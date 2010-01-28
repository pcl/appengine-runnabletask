package taskhandler

import javax.servlet.http._

class TaskHandlerServlet extends HttpServlet {
  override def doGet(request: HttpServletRequest, response: HttpServletResponse) = {
    handle(request, response)
  }

  override def doPost(request: HttpServletRequest, response: HttpServletResponse) = {
    handle(request, response)
  }

  def handle(request: HttpServletRequest, response: HttpServletResponse) = {
    val handler = TaskHandler.deserialize(request.getParameter("h")).asInstanceOf[TaskHandler]
    val message = TaskHandler.deserialize(request.getParameter("m"))
    val responseCode = handler.handle(message)
    response.setStatus(responseCode.intValue)
    response.setContentType("text/plain")
    response.getWriter.write("status: " + responseCode.intValue)
  }
}
