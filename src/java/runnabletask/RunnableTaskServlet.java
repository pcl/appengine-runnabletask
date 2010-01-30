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

        int retryCount = 0;
        String devServerHeader = request.getHeader("X-Google-DevAppserver-SkipAdminCheck");
        // Only inspect the retry count header if we're not running in the dev server,
        // which does not set that header.
        if (devServerHeader == null) {
            String retryCountHeader = request.getHeader("X-AppEngine-TaskRetryCount");
            if (retryCountHeader == null) {
                // the request must not be from the task queue. Empirically, it seems
                // that GAE strips out this header when specified in a curl request
                throw new IllegalStateException("This servlet is only accessible "
                    + "from the Google App Engine task queue.");
            } else {
                retryCount = Integer.parseInt(retryCountHeader);
            }
        }

        Runnable runnable = (Runnable) TaskQueueExecutor.deserialize(request.getParameter("r"));
        TaskQueueExecutor.setCurrentRetryCount(retryCount);

        try {
            runnable.run();
            response.setStatus(200);
        } finally {
          TaskQueueExecutor.setCurrentRetryCount(-1);
        }
    }
}
