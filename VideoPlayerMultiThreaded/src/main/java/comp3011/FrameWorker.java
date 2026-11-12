


package comp3011;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Serial background worker for expensive video-frame preparation.
 *
 * Tasks submitted to this worker run on a dedicated daemon thread.
 * Only one task executes at a time, protecting stateful frame processors
 * from concurrent execution by multiple preparation tasks.
 *
 * Callers remain responsible for coordinating access to shared playback
 * resources and discarding results made stale by seeking or stopping.
 */
final class FrameWorker implements AutoCloseable {

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(task -> {
                Thread thread = new Thread(task, "video-frame-worker");
                thread.setDaemon(true);
                return thread;
            });

    /**
     * Submit a task for serial execution on the background worker.
     */
    Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    /**
     * Stop accepting new tasks and interrupt running work where possible.
     */
    void shutdownNow() {
        executor.shutdownNow();
    }

    @Override
    public void close() {
        shutdownNow();
    }
}
