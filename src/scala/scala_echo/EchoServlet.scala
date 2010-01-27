package scala_echo
import javax.servlet.http._
import java.util._
import java.text._

class EchoServlet extends HttpServlet {
  override def doGet(request: HttpServletRequest, response: HttpServletResponse) = 
  {
    val now = new Date
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    val format = "{ 'status': 0, 'timestamp': '%s', 'request-url': '%s', 'data': '%s' }"
    
    response.setContentType("application/json")
    response.getWriter.printf(format, sdf.format(now), request.getRequestURI, request.getParameter("echo"))
  }
}
