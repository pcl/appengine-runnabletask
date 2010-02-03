package runnabletask;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

/**
 * A servlet that can handle requests posted by {@link TaskQueueExecutor}.
 *
 * @see TaskQueueExecutor
 */
public class RunnableTaskServlet extends HttpServlet {

    static final String RUNNABLE_PARAMETER_NAME = "r";
    static final String RUNNABLE_ID_PARAMETER_NAME = "r_id";

    private static final ThreadLocal<Key> _entityKeyThreadLocal = new ThreadLocal<Key>();

    /**
     * A set of keys to stored runnables that have been successfully executed,
     * but unsuccessfully deleted.
     */
    private static final Queue<Key> _keysToCleanUp = new ConcurrentLinkedQueue<Key>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        try {
            handle(request, response);
        } catch (ClassNotFoundException e) {
            throw new ServletException(e);
        } catch (EntityNotFoundException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        try {
            handle(request, response);
        } catch (ClassNotFoundException e) {
            throw new ServletException(e);
        } catch (EntityNotFoundException e) {
            throw new ServletException(e);
        }
    }

    private void handle(HttpServletRequest request, HttpServletResponse response)
        throws ClassNotFoundException, IOException, EntityNotFoundException {
        if (!allowRunnableExecution(request))
            throw new IllegalStateException("This servlet is only accessible "
                + "from the Google App Engine task queue.");

        int retryCount;
        if (isDevelopmentServer(request)) {
            retryCount = 0;
        } else {
            String retryCountHeader = request.getHeader("X-AppEngine-TaskRetryCount");
            retryCount = Integer.parseInt(retryCountHeader);
        }

        Runnable runnable;
        boolean dbNeedsCleanup = false;
        String runnableParam = request.getParameter(RUNNABLE_PARAMETER_NAME);
        if (runnableParam != null) {
            runnable = (Runnable) TaskQueueExecutor.decodeAndDeserialize(runnableParam);
        } else {
            String runnableIdParam = request.getParameter(RUNNABLE_ID_PARAMETER_NAME);
            if (runnableIdParam == null) {
                throw new IllegalStateException(
                    "Neither a Runnable nor a Runnable ID was provided in "
                        + "the request parameters.");
            }

            runnable = loadRunnable(runnableIdParam);
            dbNeedsCleanup = true;
        }

        TaskQueueExecutor.setCurrentRetryCount(retryCount);
        try {
            runnable.run();
            response.setStatus(200);
            if (dbNeedsCleanup)
                cleanUpEntity();
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

    private Runnable loadRunnable(String encodedEntityKey)
        throws ClassNotFoundException, IOException, EntityNotFoundException {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key key = (Key) TaskQueueExecutor.decodeAndDeserialize(encodedEntityKey);
        Entity entity = datastore.get(key);
        _entityKeyThreadLocal.set(key);
        Text encodedRunnable = (Text) entity.getProperty("encodedRunnableBytes");
        return (Runnable) TaskQueueExecutor.decodeAndDeserialize(encodedRunnable.getValue());
    }

    private void cleanUpEntity() {
        Key key = _entityKeyThreadLocal.get();
        if (key != null) {
            try {
                DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
                datastore.delete(key);
            } catch (Exception e) {
                // TODO logging
                _keysToCleanUp.add(key);
            }
        }

        // Delete any keys that have been marked for clean-up but that failed to
        // be removed from the set. This is done outside of a synchronized block
        // because it's not expected to happen often.
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        for (key = _keysToCleanUp.poll(); key != null; key = _keysToCleanUp.poll()) {
            try {
                datastore.delete(key);
                _keysToCleanUp.remove(key);
            } catch (Exception e) {
                // ignore
                // TODO logging
            }
        }
    }
}
