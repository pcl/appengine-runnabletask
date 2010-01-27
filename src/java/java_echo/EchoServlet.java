package java_echo;

import java.io.IOException;
import javax.servlet.http.*;
import java.util.Date;
import java.text.SimpleDateFormat;

public class EchoServlet extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String jsonFormat = 
            "{ 'status': 0, 'timestamp': '%s', 'request-url': '%s', 'data': '%s' }";

        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

        resp.setContentType("application/json");
        resp.getWriter().printf(jsonFormat, sdf.format(now), req.getRequestURI(),
            req.getParameter("echo"));
    }
}