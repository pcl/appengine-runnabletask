package taskhandler

import com.google.appengine.api.labs.taskqueue.Queue
import com.google.appengine.api.labs.taskqueue.QueueFactory
import com.google.appengine.api.labs.taskqueue.TaskOptions.Builder

import java.io._

import org.apache.commons.codec.binary.Base64

/**
 * <p>A <code>TaskHandler</code> provides an actor-inspired means of 
 * using the Google App Engine task queue functionality. It provides an actor-like 
 * programming model for executing tasks, without the hassle of creating web endpoints
 * for each task. Of course, this is a much more tightly-coupled approach than the
 * general-purpose facilities provided by the Google App Engine functionality.</p>
 *
 * <p>A TaskHandler differs from a normal Scala actor in that it exists within the Google App 
 * Engine task queue, rather than in a running process. This means that a task handler cannot
 * be started or stopped -- it just is. It also means that task handlers do not loop and poll
 * their own mailboxes; Google App Engine + the task handler framework will automatically create
 * handler instances as needed to handle messages in the queue.</p>
 *
 * <p>To use this framework, you must register the <code>TaskHandlerServlet</code> class to handle
 * the URI <code>/tasks/taskhandler</code> in your application.</p>
 */
@serializable
trait TaskHandler {
  /**
   * <p>Handles execution of the task. Since Google App Engine is responsible for
   * managing the lifecycle of the task handler, this is a short-lived method that
   * processes a single message and then exit.</p>
   *
   * @return an HTTP response code as defined by the standard Google App Engine
   * task queue rules (i.e., response code between 200 and 299 to indicate success). 
   */
  def handle(msg: Any): java.lang.Integer

  def enqueue(msg: Any) = {
    var q : Queue = null
    if (queue eq null)
      q = QueueFactory.getDefaultQueue
    else
      q = QueueFactory.getQueue(queue)
    q.add(Builder.url("/tasks/taskhandler")
          .param("h", TaskHandler.serialize(this))
          .param("m", TaskHandler.serialize(msg)))
  }

  /**
   * @return the name of the Google App Engine queue that this task handler should use for 
   * execution. The default implementation returns <code>null</code>, causing the dispatch
   * to use the default queue.
   */
  protected def queue(): String = null
}

object TaskHandler {
  /**
   * <p>This function is used for the definition of task handlers. Since task handlers
   * are executed in a remote process space, the function body must be serializable.</p>
   *
   * <p>The following example demonstrates its usage:</p><pre>
   * import taskhandler.TaskHandler._
   * ...
   * val h = task {
   *   ...
   * }
   * h enqueue "msg"
   * </pre>
   *
   * @param  f     the code block to be executed by the newly created actor
   * @return       the newly created handler.
   */
  def task(f: PartialFunction[Any, java.lang.Integer]): TaskHandler = {
    val handler = new TaskHandler {
      def handle(msg: Any): java.lang.Integer = f(msg)
    }
    handler
  }

  val base64 = new Base64()

  def serialize(obj : Any): Array[byte] = {
    val out = new ByteArrayOutputStream
    val oout = new ObjectOutputStream(out)
    oout.writeObject(obj)
    return base64.encode(out.toByteArray)
  }

  def deserialize(serialized : String): Any = {
    val bytes = base64.decode(serialized)
    val in = new ByteArrayInputStream(bytes)
    val oin = new ObjectInputStream(in)
    oin.readObject
  }
}
