package runnabletask

import java.util.concurrent.Executor
import com.google.appengine.api.labs.taskqueue.{Queue, QueueFactory}
import com.google.appengine.api.labs.taskqueue.TaskOptions.Builder
import org.apache.commons.codec.binary.Base64
import java.io.{ObjectInputStream, ByteArrayInputStream, ByteArrayOutputStream, ObjectOutputStream}

/**
 * An {@link Executor} that runs {@link Runnable}s via Google App Engine's
 * task queue API.
 */
class TaskQueueExecutor(queue: String, url: String) extends Executor {

  def this() = this(null)

  def this(queue: String) = this(queue, "/tasks/RunnableTaskServlet")

  def execute(r: Runnable) = {
    var q : Queue = null
    if (queue eq null)
      q = QueueFactory.getDefaultQueue
    else
      q = QueueFactory.getQueue(queue)
    q.add(Builder.url(url).param("r", TaskQueueExecutor.serialize(r)))
  }
}

object TaskQueueExecutor {
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

  private val tl = new ThreadLocal[Int]
  tl.set(-1)

  /**
   * the retry count for the currently-executing task, or -1 if no task is
   * currently executing
   */
  def retryCount(): Int = tl.get

  private[runnabletask] def setCurrentRetryCount(r: Int) = tl.set(r)
}

@serializable
trait SerializableRunnable extends Runnable {
}

object SerializableRunnable {

  def runnable(body: => Unit): Runnable = {
    val runnable = new SerializableRunnable {
      def run() = body
    }
    runnable
  }
}
