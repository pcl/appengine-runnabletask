package runnabletask

object SerializableRunnableFactory {

  /**
   * <p>Utility method to create a serializable {@link Runnable} for use with
   * {@link runnabletask.TaskQueueExecutor}.</p>
   *
   * Usage:
   * <pre>
   * new TaskQueueExecutor() execute <i>runnable</i> { work work work }
   * </pre> 
   */
  def runnable(body: => Unit): SerializableRunnable = {
    val runnable = new SerializableRunnable {
      def run() = body
    }
    runnable
  }
}
