package runnabletask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executor;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Builder;
import org.apache.commons.codec.binary.Base64;

import static runnabletask.RunnableTaskServlet.*;

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

    private static final Base64 base64 = new Base64();

    public void execute(Runnable r) {
        byte[] runnableBytes = serialize(r);
        byte[] encodedRunnableBytes = base64.encode(runnableBytes);
        TaskOptions task = Builder.url(_url)
            .param(RUNNABLE_PARAMETER_NAME, encodedRunnableBytes);
        try {
            _queue.add(task);
        } catch (IllegalArgumentException e) {
            // Assume that this means the task is too big. We're handling
            // this via a try-catch block instead of comparing the byte array
            // size to the advertised 10240 limit because the limit seems to
            // be significantly off.
            // TODO if a runnable gets created but the add fails, db will fill up
            task = Builder.url(_url)
                .param(RUNNABLE_ID_PARAMETER_NAME, storeRunnable(runnableBytes));
            _queue.add(task);
        }
    }

    private byte[] serialize(Object obj) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(out);
            oout.writeObject(obj);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Object decodeAndDeserialize(String serialized)
        throws IOException, ClassNotFoundException {
        byte[] bytes = base64.decode(serialized);
        return deserialize(bytes);
    }

    static Object deserialize(byte[] bytes)
        throws IOException, ClassNotFoundException {
        InputStream in = new ByteArrayInputStream(bytes);
        ObjectInputStream oin = new ObjectInputStream(in);
        return oin.readObject();
    }

    public static final String RUNNABLE_TASK_KIND = TaskQueueExecutor.class.getName() + ":runnable";

    private byte[] storeRunnable(byte[] runnableBytes) {
        Entity entity = new Entity(RUNNABLE_TASK_KIND);
        entity.setUnindexedProperty("runnableBytes", new Blob(runnableBytes));
        entity.setProperty("timestamp", new Date());
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(entity);
        return base64.encode(serialize(entity.getKey()));
    }

    private static final ThreadLocal<Integer> tl = new ThreadLocal<Integer>();
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
