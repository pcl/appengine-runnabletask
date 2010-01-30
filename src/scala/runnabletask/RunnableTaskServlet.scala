package runnabletask

import javax.servlet.http._

class RunnableTaskServlet extends HttpServlet {
  override def doGet(request: HttpServletRequest, response: HttpServletResponse) = {
    handle(request, response)
  }

  override def doPost(request: HttpServletRequest, response: HttpServletResponse) = {
    handle(request, response)
  }

  def handle(request: HttpServletRequest, response: HttpServletResponse) = {
    val runnable = TaskQueueExecutor.deserialize(request.getParameter("r")).asInstanceOf[Runnable]

    val retryCountHeader = request.getHeader("X-AppEngine-TaskRetryCount")
    if (retryCountHeader == null)
      TaskQueueExecutor.setCurrentRetryCount(0)
    else
      TaskQueueExecutor.setCurrentRetryCount(Integer.parseInt(retryCountHeader))

    try {
      runnable.run
      response.setStatus(200)
    } catch {
      case e =>
        e.printStackTrace(response.getWriter)
        response.setStatus(500)
    } finally {
      response.setContentType("text/plain")
      TaskQueueExecutor.setCurrentRetryCount(-1)
    }
  }
}
