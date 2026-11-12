package comp3011;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class FrameWorkerTest {

    @Test
    void runsTasksOnDedicatedDaemonThread() throws Exception {
        try (WorkerFixture fixture = new WorkerFixture()) {
            AtomicReference<Thread> executingThread = new AtomicReference<>();
            CountDownLatch completed = new CountDownLatch(1);

            fixture.worker.submit(() -> {
                executingThread.set(Thread.currentThread());
                completed.countDown();
            });

            assertTrue(completed.await(2, TimeUnit.SECONDS),
                    "Worker task did not finish in time");

            Thread workerThread = executingThread.get();
            assertTrue(workerThread != null,
                    "Task should record its executing thread");
            assertEquals("video-frame-worker", workerThread.getName());
            assertTrue(workerThread.isDaemon(),
                    "Worker should not keep the JVM alive");
            assertNotEquals(Thread.currentThread(), workerThread,
                    "Frame preparation must not execute on the calling/UI thread");
        }
    }

    @Test
    void executesSubmittedTasksSeriallyOnTheSameWorker() throws Exception {
        try (WorkerFixture fixture = new WorkerFixture()) {
            AtomicInteger activeTasks = new AtomicInteger();
            AtomicInteger maximumActiveTasks = new AtomicInteger();
            AtomicReference<String> firstThread = new AtomicReference<>();
            AtomicReference<String> secondThread = new AtomicReference<>();
            CountDownLatch bothCompleted = new CountDownLatch(2);

            fixture.worker.submit(() -> {
                int active = activeTasks.incrementAndGet();
                maximumActiveTasks.accumulateAndGet(active, Math::max);
                firstThread.set(Thread.currentThread().getName());

                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    activeTasks.decrementAndGet();
                    bothCompleted.countDown();
                }
            });

            fixture.worker.submit(() -> {
                int active = activeTasks.incrementAndGet();
                maximumActiveTasks.accumulateAndGet(active, Math::max);
                secondThread.set(Thread.currentThread().getName());
                activeTasks.decrementAndGet();
                bothCompleted.countDown();
            });

            assertTrue(bothCompleted.await(2, TimeUnit.SECONDS),
                    "Worker tasks did not finish in time");
            assertEquals(1, maximumActiveTasks.get(),
                    "Tasks must not execute concurrently");
            assertEquals(firstThread.get(), secondThread.get(),
                    "Tasks should share one worker thread");
            assertEquals("video-frame-worker", firstThread.get());
        }
    }

    private static final class WorkerFixture implements AutoCloseable {
        private final FrameWorker worker = new FrameWorker();

        @Override
        public void close() {
            worker.shutdownNow();
        }
    }
}