package runnabletask;

import java.io.Serializable;

/**
 * Convenience interface that implements both {@link Serializable} and
 * {@link Runnable}. Use this to create anonymous inner classes for use
 * with {@link TaskQueueExecutor}.
 *
 * @see TaskQueueExecutor
 */
public interface SerializableRunnable
    extends Serializable, Runnable {
}
