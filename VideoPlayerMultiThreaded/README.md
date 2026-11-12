# COMP3011 Assignment 2 - Java Local Project

For more information on this code and how to get started, see https://learn.adelaide.edu.au/courses/30747/assignments/94505

## Concurrency and Frame Processing

### Background frame worker

Video frame preparation is performed by a dedicated background worker, implemented by `FrameWorker`. The worker uses a single-thread executor, so submitted tasks execute sequentially rather than concurrently. Its thread is named `video-frame-worker` and is a daemon thread.

This design keeps frame preparation away from the JavaFX Application Thread, helping the user interface remain responsive while video frames are processed.

### Thread coordination

`VideoPlayerModel` coordinates background frame preparation with playback state and decoder access. Prepared frames are published to the JavaFX Application Thread for UI updates. Playback-generation tracking is used to identify and discard results that have become stale after playback state changes, such as seeking or stopping.

The worker's serial execution does not by itself make all shared state thread-safe. Access to decoder and audio resources must still be coordinated, and stale asynchronous results must be handled correctly.

### Automated tests

`FrameWorkerTest` verifies that:

- Tasks execute on a dedicated daemon thread named `video-frame-worker`.
- Submitted tasks execute serially on the same worker thread.

These tests verify the worker's execution properties. They do not replace manual playback tests or comprehensive stress tests of seeking, stopping, restarting, and audio handling.

### Manual verification

Playback, pause/resume, seeking, and audio should be checked with a representative video after concurrency changes. Rapid state changes and repeated playback sessions should also be tested to identify stale frames, UI freezes, audio problems, or unexpected exceptions.

#COMP3011 Assignment 2 – Part 2: Performance Optimisation and Verification

1. Optimisation design

The video player was optimised to reduce the processing overhead involved in applying visual effects to decoded video frames.

The main optimisation strategies were:

Parallel row processing: Independent image rows are processed concurrently in selected effects, including black-and-white conversion, vignette processing and yellow colour adjustment.

Reduced repeated calculations: Image dimensions, channel counts, row strides and other frame metadata are cached rather than repeatedly retrieved inside pixel-processing loops.

Efficient pixel access: Absolute ByteBuffer get and put operations are used to access pixel data directly without repeatedly changing buffer positions.

Reduced indexing overhead: Row offsets and pixel positions are calculated once where possible and reused during processing.

Preserved processing order: Effects are still applied sequentially in command-line order. This preserves the intended behaviour when multiple effects modify the same pixels.

The FrameWorker remains a serial background worker. Parallelism is applied within suitable image-processing operations rather than by allowing multiple frames to modify shared decoder or effect state simultaneously.

2. Parallel processing decisions

Parallel processing is most appropriate when different threads can operate on independent regions of an image.

For example, row-based processing allows different workers to process separate rows without writing to the same pixel locations.

Not every effect was parallelised. Some effects contain overlapping marks, mutable state or order-dependent pixel modifications. Parallelising those operations could introduce race conditions or change the visual result.

For these effects, optimisation focused on reducing repeated calculations and improving pixel-buffer access while preserving the original processing sequence.

3. Performance comparison methodology

A fair comparison between the original and optimised implementations should use:

The same input video and video resolution.

The same Java runtime and computer.

Identical effect configurations.

JVM warm-up before collecting measurements.

Multiple benchmark repetitions.

Average, median and 95th-percentile frame-processing times.

Playback headroom logging provides useful information about whether frames are ready before their scheduled display time. However, it is not a direct measurement of processing duration and should not be used alone to calculate a performance improvement.

Benchmark status: The optimised implementation has been tested during playback, but a controlled original-versus-optimised benchmark has not yet been completed. Therefore, no numerical speed-up claim is made.

4. Correctness and testing

The following verification has been completed:

Individual effects have been launched through the JavaFX player.

Several optimised effects have been visually checked during playback.

A combination of effects was tested using the command-line configuration -nssnfwyvdjmbp.

Command-line handling was tested, including repeated effects, mixed options and invalid options.

The serial background worker was tested to ensure submitted tasks execute serially.

Deterministic-output tests were added for selected effects.

The full combination test completed playback successfully. The latest Maven verification also completed successfully.

Pixel-equivalence status: Exact pixel-by-pixel comparison against the original implementation has not yet been completed for every effect. Visual playback checks provide useful evidence of correct behaviour, but do not replace automated pixel comparisons.

5. Final Maven verification

The latest clean verify execution produced:

Test class

Tests

Failures

Errors

CommandLineControllerTest

7

0

0

FrameDusterTest

1

0

0

FramePeppererTest

2

0

0

FrameWorkerTest

2

0

0

Total

12

0

0

The project compiled successfully, all 12 automated tests passed, and Maven generated the application JAR.

6. Limitations and future work

The current results demonstrate that the optimised application builds and runs, and that the existing automated tests pass.

Further verification is required to establish the precise performance improvement:

Benchmark the original and optimised implementations under identical conditions.

Compare output pixel arrays for each optimised effect against the original implementation.

Test additional combinations, including repeated and reordered effects.

Record measured processing times and calculate the performance difference.

Include the benchmark data and test results in the final submission.

7. Conclusion

The project applies a combination of parallel row processing and low-level image-processing optimisations. Parallel execution is used where image regions can be processed independently, while effects with overlapping writes or order-dependent behaviour remain sequential.

The latest build and automated test results are successful. The remaining work is to complete controlled performance measurements and broader pixel-output comparisons so that the effectiveness and correctness of the optimisations can be demonstrated quantitatively.