package runnabletask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.concurrent.Executor;

import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Builder;
import org.apache.commons.codec.binary.Base64;

/**
 * An {@link Executor} that runs {@link Runnable}s via Google App Engine's
 * task queue API.
 *
 * See the package documentation for example usage. 
 */
public class TaskQueueExecutor implements Executor {

    private Queue _queue;
    private String _url;

    /**
     * Creates a new TaskQueueExecutor using the default queue and
     * bound to <code>/tasks/RunnableTaskServlet</code>.
     */
    public TaskQueueExecutor() {
        this(null, null);
    }

    /**
     * Creates a new TaskQueueExecutor using the named <code>queue</code>
     * and bound to <code>/tasks/RunnableTaskServlet</code>.
     */
    public TaskQueueExecutor(String queue) {
        this(queue, null);
    }

    /**
     * Creates a new TaskQueueExecutor using the named <code>queue</code>
     * and bound to <code>url</code>.
     *
     * @param url   The relative URL that the {@link RunnableTaskServlet} is
     *              bound to. Only the URL's path is used. If <code>null</code>,
     *              uses the default location (<code>/tasks/RunnableTaskServlet</code>).
     */
    public TaskQueueExecutor(String queue, URL url)
    {
        if (queue == null)
          _queue = QueueFactory.getDefaultQueue();
        else
          _queue = QueueFactory.getQueue(queue);

        if (url == null)
            _url = "/tasks/RunnableTaskServlet";
        else
            _url = url.getPath();
    }

    public void execute(Runnable r) {
        _queue.add(Builder.url(_url).param("r", serialize(r)));
    }

    private static Base64 base64 = new Base64();

    private static byte[] serialize(Object obj) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(out);
            oout.writeObject(obj);
            return base64.encode(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Object deserialize(String serialized) {
        try {
            byte[] bytes = base64.decode(serialized);
            InputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream oin = new ObjectInputStream(in);
            return oin.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static ThreadLocal<Integer> tl = new ThreadLocal<Integer>();
    static {
        tl.set(-1);
    }

    /**
     * the retry count for the currently-executing task, or -1 if no task is
     * currently executing
     */
    public static int retryCount() {
        return tl.get();
    }

    static void setCurrentRetryCount(int count) {
        tl.set(count);
    }
}
