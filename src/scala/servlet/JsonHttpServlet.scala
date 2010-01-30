package servlet

import javax.servlet.http._
import java.util._
import java.text._

class JsonHttpServlet extends HttpServlet {
  def writeJsonPayload(request: HttpServletRequest, response: HttpServletResponse, payload: Any) {
    val now = new Date
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    val format = "{ 'status': 0, 'timestamp': '%s', 'request-url': '%s', 'data': '%s' }\n"

    response.setContentType("application/json")
    response.getWriter.printf(format, sdf.format(now), request.getRequestURI, payload.toString)
  }
}
