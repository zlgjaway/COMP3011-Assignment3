
/*
 * Starter code supplied for Adelaide University COMP3011 Assignment 2.
 * Students are free to modify this file for assessment purposes.
 *
 * Authors:
 *   1. Simon Ratcliffe, in collaboration with GPT-5.6 Terra
 *   2. <student NhatLongTa and a1944226>
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bytedeco.javacv.Frame;

/**
 * Draws moving vertical scratches to simulate damaged film stock. Some scratches are one pixel wide, some are two. They
 * move at different rates horizontally and come and go from the film stock.
 *
 * <p>
 * {@link #generateScratches()} creates a deterministic set of scratches over
 * the video timeline. {@link #process(Frame, InfoFrame)} finds the scratch
 * segment visible in the current frame and draws it as a black vertical line.
 * </p>
 */
public class FrameScratcher extends FrameProcessor {
    private static final int FRAMES_PER_NEW_SCRATCH = 48;
    private static final double MEAN_SCRATCH_LENGTH = 96;
    private static final double THICK_SCRATCH_PROBABILITY = 0.15;
    private static final int SCRATCH_MAX_TRAVEL = 32;

    private static int nextInstanceIndex;

    private final int instanceIndex;
    private final List<Scratch> scratches = new ArrayList<>();
    private int mediaSeed;

    public FrameScratcher() {
        super();
        instanceIndex = nextInstanceIndex++;
    }

    @Override
    public void initialise(InfoVideo videoInfo) throws Exception {
        super.initialise(videoInfo);
        mediaSeed = saltedMediaSeed(videoInfo.mediaName(), instanceIndex);
        generateScratches();
    }

    @Override
    public void process(Frame frame, InfoFrame info) throws Exception {
        if (frame.image == null || frame.image.length == 0 || frame.image[0] == null) {
            return;
        }

        if (!(frame.image[0] instanceof ByteBuffer pixels)) {
            throw new Exception("FrameScratcher requires byte-backed image frames");
        }

        int depth = videoInfo.imageDepth();
        int channels = videoInfo.imageChannels();

        if (depth != Frame.DEPTH_UBYTE && depth != Frame.DEPTH_BYTE) {
            throw new Exception("FrameScratcher requires 8-bit image frames");
        }

        if (channels < 1) {
            throw new Exception("FrameScratcher requires at least one image channel");
        }

        int width = videoInfo.imageWidth();
        int height = videoInfo.imageHeight();
        int stride = videoInfo.imageStride();

        double frameStart = info.frameNumber();
        double frameEnd = frameStart + 1.0;

        for (Scratch scratch : scratches) {
            double scratchStart = Math.max(scratch.startFrame(), frameStart);
            double scratchEnd = Math.min(scratch.endFrame(), frameEnd);

            if (scratchStart < scratchEnd) {
                int yStart = frameOffsetToY(scratchStart - frameStart, height);
                int yEnd = frameOffsetToYCeiling(scratchEnd - frameStart, height);
                int x = scratchXAtFrameStart(scratch, frameStart, width);

                drawVerticalLine(pixels, x, yStart, yEnd, scratch.thick(),
                        width, height, channels, stride);
            }
        }
    }

    private void generateScratches() {
        scratches.clear();

        if (videoInfo.totalVideoFrames() <= 0 || videoInfo.imageWidth() <= 0) {
            return;
        }

        Random random = new Random(mediaSeed);
        double scratchStartProbability = 1.0 / FRAMES_PER_NEW_SCRATCH;
        int width = videoInfo.imageWidth();
        int totalFrames = videoInfo.totalVideoFrames();

        for (int frameNumber = 0; frameNumber < totalFrames; frameNumber++) {
            if (random.nextDouble() < scratchStartProbability) {
                double startFrame = frameNumber;
                double scratchLength = nextExponential(random, MEAN_SCRATCH_LENGTH);
                double endFrame = Math.min(totalFrames, startFrame + scratchLength);
                int xStart = random.nextInt(width);
                int xEnd = xStart + randomScratchTravel(random);
                boolean thick = random.nextDouble() < THICK_SCRATCH_PROBABILITY;

                xEnd = clamp(xEnd, 0, width - 1);
                scratches.add(new Scratch(startFrame, endFrame, xStart, xEnd, thick));
            }
        }
    }

    private double nextExponential(Random random, double mean) {
        return -mean * Math.log(1.0 - random.nextDouble());
    }

    private int frameOffsetToY(double frameOffset, int height) {
        int y = (int) Math.floor(frameOffset * height);
        return clamp(y, 0, height);
    }

    private int frameOffsetToYCeiling(double frameOffset, int height) {
        int y = (int) Math.ceil(frameOffset * height);
        return clamp(y, 0, height);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private int randomScratchTravel(Random random) {
        return random.nextInt(2 * SCRATCH_MAX_TRAVEL + 1) - SCRATCH_MAX_TRAVEL;
    }

    private int scratchXAtFrameStart(Scratch scratch, double frameStart, int width) {
        double scratchDuration = scratch.endFrame() - scratch.startFrame();

        if (scratchDuration <= 0) {
            return scratch.xStart();
        }

        double progress = (frameStart - scratch.startFrame()) / scratchDuration;
        progress = Math.max(0.0, Math.min(progress, 1.0));
        double x = scratch.xStart() + progress * (scratch.xEnd() - scratch.xStart());

        return clamp((int) Math.round(x), 0, width - 1);
    }

    private void drawVerticalLine(
            ByteBuffer pixels,
            int x,
            int yStart,
            int yEnd,
            boolean thick,
            int width,
            int height,
            int channels,
            int stride) {

        drawSinglePixelVerticalLine(pixels, x, yStart, yEnd, channels, stride);

        if (thick && width > 1) {
            int secondX = x + 1 < width ? x + 1 : x - 1;
            drawSinglePixelVerticalLine(pixels, secondX, yStart, yEnd, channels, stride);
        }
    }

    private void drawSinglePixelVerticalLine(
            ByteBuffer pixels,
            int x,
            int yStart,
            int yEnd,
            int channels,
            int stride) {

        int pixelOffset = x * channels;

        for (int y = yStart; y < yEnd; y++) {
            int base = y * stride + pixelOffset;

            for (int channel = 0; channel < channels; channel++) {
                pixels.put(base + channel, (byte) 0);
            }
        }
    }

    private record Scratch(
            double startFrame,
            double endFrame,
            int xStart,
            int xEnd,
            boolean thick) {
    }
}
