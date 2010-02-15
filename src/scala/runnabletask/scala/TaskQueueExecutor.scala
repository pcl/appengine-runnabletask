package runnabletask.scala

class TaskQueueExecutor extends runnabletask.TaskQueueExecutor {

  /**
   * <p>Converts the provided block to a serializable {@link Runnable}, 
   * and executes it.</p>
   *
   * Usage:
   * <pre>
   * new TaskQueueExecutor execute { work work work }
   * </pre> 
   */
  def execute(body: => Unit): Unit = {
    val runnable = new SerializableRunnable {
      def run() = body
    }
    execute(runnable)
  }
}

object TaskQueueExecutor {
  def retryCount(): Int = {
    runnabletask.TaskQueueExecutor.retryCount
  }
}