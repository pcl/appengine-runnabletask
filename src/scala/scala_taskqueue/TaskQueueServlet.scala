package scala_taskqueue

import servlet._
import javax.servlet.http._
import runnabletask.TaskQueueExecutor.RUNNABLE_DB_KIND
import runnabletask.scala.TaskQueueExecutor
import runnabletask.scala.TaskQueueExecutor._
import com.google.appengine.api.datastore.{Query, DatastoreServiceFactory}

class TaskQueueServlet extends JsonHttpServlet {
  override def doGet(request: HttpServletRequest, response: HttpServletResponse) = 
  {
    val executor = new TaskQueueExecutor

    val foo = "message received!"
    executor execute {
      Console.println("runnable: " + foo)
    }

    executor execute {
      if (retryCount == 0) {
        Console.println("failing first run")
        throw new Exception("failing first run")
      } else {
        Console.println("passing run " + retryCount)
      }
    }

    // try a value bigger than the advertised 10*1024 limit.
    val bigValue = new Array[Byte](20*1024)
    executor execute {
      Console.println("running bigRunnable")
      Console.println("big value length: " + bigValue.length)

      // check that the database gets cleaned up in another task, since
      // the cleanup doesn't happen until after the task executes
      val nestedExecutor = new TaskQueueExecutor
      nestedExecutor execute {
        val ds = DatastoreServiceFactory.getDatastoreService
        val q = ds.prepare(new Query(RUNNABLE_DB_KIND))
        if (q.countEntities > 1)
          throw new Exception("Too many entities found. Expected 1; found " + q.countEntities)
      }
    }

    writeJsonPayload(request, response, "enqueued messages")
  }
}
