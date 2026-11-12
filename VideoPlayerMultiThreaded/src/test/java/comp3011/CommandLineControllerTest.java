
package comp3011;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommandLineControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void helpOptionExitsWithoutError() {
        CommandLineController controller =
                new CommandLineController(new String[]{"--help"});

        assertEquals(0, controller.getExitCode());
        assertNull(controller.getErrorMessage());
        assertFalse(controller.shouldLaunchApplication());
    }

    @Test
    void unknownOptionIsRejected() {
        CommandLineController controller =
                new CommandLineController(new String[]{"-q"});

        assertEquals(1, controller.getExitCode());
        assertEquals("Unknown option: -q",
                controller.getErrorMessage());
        assertFalse(controller.shouldLaunchApplication());
    }

    @Test
    void stackedEffectsAreCreatedInCorrectOrder() throws Exception {
        Path video = Files.createFile(tempDir.resolve("test.mp4"));

        CommandLineController controller =
                new CommandLineController(
                        new String[]{"-nw", video.toString()});

        assertEquals(0, controller.getExitCode());
        assertTrue(controller.shouldLaunchApplication());

        List<String> effectNames = controller.getFrameProcessors()
                .stream()
                .map(processor -> processor.getClass().getSimpleName())
                .toList();

        assertEquals(
                List.of("FrameNumberer", "FrameBlackAndWhiter"),
                effectNames);
    }

    @Test
    void repeatedEffectsArePreserved() throws Exception {
        Path video = Files.createFile(tempDir.resolve("repeat.mp4"));

        CommandLineController controller =
                new CommandLineController(
                        new String[]{"-nwn", video.toString()});

        List<String> effectNames = controller.getFrameProcessors()
                .stream()
                .map(processor -> processor.getClass().getSimpleName())
                .toList();

        assertEquals(
                List.of(
                        "FrameNumberer",
                        "FrameBlackAndWhiter",
                        "FrameNumberer"),
                effectNames);
    }
    
    @Test
    void allRequiredLongOptionsAreAccepted() throws Exception {
        Path video = Files.createFile(tempDir.resolve("all-effects.mp4"));

        CommandLineController controller = new CommandLineController(
                new String[]{
                        "--number-frames",
                        "--scratch-frames",
                        "--flicker-frames",
                        "--black-and-white",
                        "--yellow-frames",
                        "--vignette-frames",
                        "--dust-frames",
                        "--jitter-frames",
                        "--mottle-frames",
                        "--bleed-frames",
                        "--pepper-frames",
                        video.toString()
                });

        assertEquals(0, controller.getExitCode());
        assertTrue(controller.shouldLaunchApplication());

        List<String> effectNames = controller.getFrameProcessors()
                .stream()
                .map(processor -> processor.getClass().getSimpleName())
                .toList();

        assertEquals(List.of(
                "FrameNumberer",
                "FrameScratcher",
                "FrameFlickerer",
                "FrameBlackAndWhiter",
                "FrameYellower",
                "FrameVignetter",
                "FrameDuster",
                "FrameJitterer",
                "FrameMottler",
                "FrameBleeder",
                "FramePepperer"
        ), effectNames);
    }

    @Test
    void mixedShortAndLongOptionsPreserveOrderAndRepetitions()
            throws Exception {
        Path video = Files.createFile(tempDir.resolve("mixed-effects.mp4"));

        CommandLineController controller = new CommandLineController(
                new String[]{
                        "-jf",
                        "--dust-frames",
                        "-d",
                        "--number-frames",
                        video.toString()
                });

        assertEquals(0, controller.getExitCode());

        List<String> effectNames = controller.getFrameProcessors()
                .stream()
                .map(processor -> processor.getClass().getSimpleName())
                .toList();

        assertEquals(List.of(
                "FrameJitterer",
                "FrameFlickerer",
                "FrameDuster",
                "FrameDuster",
                "FrameNumberer"
        ), effectNames);
    }

    @Test
    void completeStackedExamplePreservesOrderAndDuplicates()
            throws Exception {
        Path video = Files.createFile(tempDir.resolve("full-example.mp4"));

        CommandLineController controller = new CommandLineController(
                new String[]{"-nssnfwyvdjmbp", video.toString()});

        assertEquals(0, controller.getExitCode());

        List<String> effectNames = controller.getFrameProcessors()
                .stream()
                .map(processor -> processor.getClass().getSimpleName())
                .toList();

        assertEquals(List.of(
                "FrameNumberer",
                "FrameScratcher",
                "FrameScratcher",
                "FrameNumberer",
                "FrameFlickerer",
                "FrameBlackAndWhiter",
                "FrameYellower",
                "FrameVignetter",
                "FrameDuster",
                "FrameJitterer",
                "FrameMottler",
                "FrameBleeder",
                "FramePepperer"
        ), effectNames);
    }
}
