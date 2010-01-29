package scala_taskqueue

import servlet._
import javax.servlet.http._
import taskhandler.TaskHandler._

class TaskQueueServlet extends JsonHttpServlet {
  override def doGet(request: HttpServletRequest, response: HttpServletResponse) = 
  {
    val t = task {
      case "message!" =>
        Console.println("message received!")
        200
      case _ =>
        Console.println("unrecognized message")
        if (self.retryCount > 1)
          200
        else
          500
    }
    t enqueue "message!"
    t enqueue "aoeu"
    writeJsonPayload(request, response, "enqueued messages")
  }
}
