package runnabletask;

import javax.servlet.http.*;

/**
 * A servlet that can handle requests posted by {@link TaskQueueExecutor}.
 *
 * @see TaskQueueExecutor
 */
public class RunnableTaskServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        handle(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)  {
        handle(request, response);
    }

    private void handle(HttpServletRequest request, HttpServletResponse response) {
        if (!allowRunnableExecution(request))
            throw new IllegalStateException("This servlet is only accessible "
                + "from the Google App Engine task queue.");

        String retryCountHeader = request.getHeader("X-AppEngine-TaskRetryCount");
        int retryCount = Integer.parseInt(retryCountHeader);

        Runnable runnable = (Runnable) TaskQueueExecutor.deserialize(request.getParameter("r"));
        TaskQueueExecutor.setCurrentRetryCount(retryCount);

        try {
            runnable.run();
            response.setStatus(200);
        } finally {
          TaskQueueExecutor.setCurrentRetryCount(-1);
        }
    }

    private boolean allowRunnableExecution(HttpServletRequest request) {
        // Google AppEngine strips out certain headers when provided by the HTTP client, so we
        // can rely on their presence to indicate that a request comes from the task queue.
        // For details, see http://groups.google.com/group/google-appengine/msg/78aa0461361efc35
        if (isDevelopmentServer(request))
            return true;
        
        return request.getHeader("X-AppEngine-TaskRetryCount") != null;
    }

    private boolean isDevelopmentServer(HttpServletRequest request) {
        return request.getHeader("X-Google-DevAppserver-SkipAdminCheck") != null;
    }
}
