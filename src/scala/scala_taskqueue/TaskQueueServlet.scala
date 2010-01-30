package scala_taskqueue

import servlet._
import javax.servlet.http._
import runnabletask.TaskQueueExecutor
import runnabletask.TaskQueueExecutor._
import runnabletask.SerializableRunnable._

class TaskQueueServlet extends JsonHttpServlet {
  override def doGet(request: HttpServletRequest, response: HttpServletResponse) = 
  {
    val foo = "message received!"

    val r1 = runnable {
      Console.println("runnable: " + foo)
    }
    new TaskQueueExecutor() execute r1

    val r2 = runnable {
      if (retryCount == 0) {
        Console.println("failing first run")
        throw new Exception("failing first run")
      } else {
        Console.println("passing run " + retryCount)
      }
    }
    new TaskQueueExecutor(null) execute r2

    writeJsonPayload(request, response, "enqueued messages")
  }
}
