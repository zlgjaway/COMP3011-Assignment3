/*
 * Starter code supplied for Adelaide University COMP3011 Assignment 2.
 * Students are free to modify this file for assessment purposes.
 * 
 * Authors:
 *   1. Simon Ratcliffe, in collaboration with GPT-5.6 Terra
 *   2. <student name and student number insert here upon modification>
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011;

import java.nio.ByteBuffer;

import org.bytedeco.javacv.Frame;

/**
 * Simulates bright light bleeding into nearby pixels, with a warm colour bias.
 * This one is expensive in CPU time!
 *
 * <p>
 * For each frame, {@link #buildBrightMask(ByteBuffer)} identifies bright
 * regions, {@link #blurBrightMask()} spreads their influence, and
 * {@link #applyBleed(ByteBuffer)} adds the resulting coloured glow.
 * </p>
 */
public class FrameBleeder extends FrameProcessor {
    private static final int MASK_BLOCK_SIZE = 4;
    private static final int BLUR_RADIUS = 3;
    private static final int BLEED_LUMINANCE_THRESHOLD = 150;
    private static final double BLEED_GAMMA = 1.25;
    private static final double BLEED_MASK_GAIN = 2.4;
    private static final double RED_BLEED_STRENGTH = 105.0;
    private static final double GREEN_BLEED_STRENGTH = 62.0;
    private static final double BLUE_BLEED_STRENGTH = 18.0;

    private int maskWidth;
    private int maskHeight;
    private double[] brightMask;
    private double[] horizontalBlurMask;
    private double[] blurredMask;
    private int[] xMaskLeft;
    private double[] xMaskBlend;
    private int[] yMaskTop;
    private double[] yMaskBlend;

    public FrameBleeder() {
        super();
    }

    @Override
    public void initialise(InfoVideo videoInfo) throws Exception {
        super.initialise(videoInfo);

        maskWidth = (videoInfo.imageWidth() + MASK_BLOCK_SIZE - 1) / MASK_BLOCK_SIZE;
        maskHeight = (videoInfo.imageHeight() + MASK_BLOCK_SIZE - 1) / MASK_BLOCK_SIZE;
        brightMask = new double[maskWidth * maskHeight];
        horizontalBlurMask = new double[brightMask.length];
        blurredMask = new double[brightMask.length];
        buildSamplingLookups();
    }

    @Override
    public void process(Frame frame, InfoFrame info) throws Exception {
        if (frame.image == null || frame.image.length == 0 || frame.image[0] == null) {
            return;
        }

        if (!(frame.image[0] instanceof ByteBuffer pixels)) {
            throw new Exception("FrameBleeder requires byte-backed image frames");
        }

        if (videoInfo.imageDepth() != Frame.DEPTH_UBYTE && videoInfo.imageDepth() != Frame.DEPTH_BYTE) {
            throw new Exception("FrameBleeder requires 8-bit image frames");
        }

        if (videoInfo.imageChannels() < 3) {
            throw new Exception("FrameBleeder requires at least three image channels");
        }

        buildBrightMask(pixels);
        blurBrightMask();
        applyBleed(pixels);
    }

    private void buildSamplingLookups() {
        xMaskLeft = new int[videoInfo.imageWidth()];
        xMaskBlend = new double[videoInfo.imageWidth()];
        yMaskTop = new int[videoInfo.imageHeight()];
        yMaskBlend = new double[videoInfo.imageHeight()];

        buildAxisSamplingLookups(videoInfo.imageWidth(), maskWidth, xMaskLeft, xMaskBlend);
        buildAxisSamplingLookups(videoInfo.imageHeight(), maskHeight, yMaskTop, yMaskBlend);
    }

    private void buildAxisSamplingLookups(int imageSize, int maskSize, int[] maskStarts, double[] maskBlends) {
        if (imageSize <= 1 || maskSize <= 1) {
            for (int position = 0; position < imageSize; position++) {
                maskStarts[position] = 0;
                maskBlends[position] = 0.0;
            }
            return;
        }

        for (int position = 0; position < imageSize; position++) {
            double maskPosition = (position + 0.5) / MASK_BLOCK_SIZE - 0.5;
            maskPosition = clamp(maskPosition, 0.0, maskSize - 1.0);

            int maskStart = (int) Math.floor(maskPosition);
            if (maskStart >= maskSize - 1) {
                maskStart = maskSize - 2;
                maskPosition = maskSize - 1;
            }

            maskStarts[position] = maskStart;
            maskBlends[position] = maskPosition - maskStart;
        }
    }

    private void buildBrightMask(ByteBuffer pixels) {
        final int width = videoInfo.imageWidth();
        final int height = videoInfo.imageHeight();
        final int channels = videoInfo.imageChannels();
        final int stride = videoInfo.imageStride();
        final int currentMaskWidth = maskWidth;
        final int currentMaskHeight = maskHeight;

        for (int maskY = 0; maskY < currentMaskHeight; maskY++) {
            final int blockY = maskY * MASK_BLOCK_SIZE;
            final int blockYEnd = Math.min(blockY + MASK_BLOCK_SIZE, height);
            final int maskRowStart = maskY * currentMaskWidth;

            for (int maskX = 0; maskX < currentMaskWidth; maskX++) {
                final int blockX = maskX * MASK_BLOCK_SIZE;
                final int blockXEnd = Math.min(blockX + MASK_BLOCK_SIZE, width);
                int maximumLuminance = 0;

                for (int y = blockY; y < blockYEnd; y++) {
                    final int rowStart = y * stride;

                    for (int x = blockX; x < blockXEnd; x++) {
                        final int base = rowStart + x * channels;

                        // JavaCV supplies decoded frames in BGR byte order.
                        final int b = Byte.toUnsignedInt(pixels.get(base));
                        final int g = Byte.toUnsignedInt(pixels.get(base + 1));
                        final int r = Byte.toUnsignedInt(pixels.get(base + 2));

                        final int luminance = (54 * r + 183 * g + 19 * b) / 256;
                        maximumLuminance = Math.max(maximumLuminance, luminance);
                    }
                }

                brightMask[maskRowStart + maskX] =
                        thresholdedBrightness(maximumLuminance);
            }
        }
    }

    private double thresholdedBrightness(int luminance) {
        if (luminance <= BLEED_LUMINANCE_THRESHOLD) {
            return 0.0;
        }

        double normalised = (luminance - BLEED_LUMINANCE_THRESHOLD) / (double) (255 - BLEED_LUMINANCE_THRESHOLD);
        return Math.pow(normalised, BLEED_GAMMA);
    }

    private void blurBrightMask() {
        blurHorizontally(brightMask, horizontalBlurMask);
        blurVertically(horizontalBlurMask, blurredMask);
    }

    private void blurHorizontally(double[] source, double[] destination) {
        final int currentMaskWidth = maskWidth;
        final int currentMaskHeight = maskHeight;

        for (int y = 0; y < currentMaskHeight; y++) {
            final int rowStart = y * currentMaskWidth;

            for (int x = 0; x < currentMaskWidth; x++) {
                double total = 0.0;
                int samples = 0;

                for (int dx = -BLUR_RADIUS; dx <= BLUR_RADIUS; dx++) {
                    final int sampleX = clamp(x + dx, 0, currentMaskWidth - 1);
                    total += source[rowStart + sampleX];
                    samples++;
                }

                destination[rowStart + x] = total / samples;
            }
        }
    }

    private void blurVertically(double[] source, double[] destination) {
        final int currentMaskWidth = maskWidth;
        final int currentMaskHeight = maskHeight;

        for (int y = 0; y < currentMaskHeight; y++) {
            final int destinationRowStart = y * currentMaskWidth;

            for (int x = 0; x < currentMaskWidth; x++) {
                double total = 0.0;
                int samples = 0;

                for (int dy = -BLUR_RADIUS; dy <= BLUR_RADIUS; dy++) {
                    final int sampleY = clamp(y + dy, 0, currentMaskHeight - 1);
                    total += source[sampleY * currentMaskWidth + x];
                    samples++;
                }

                destination[destinationRowStart + x] = total / samples;
            }
        }
    }

    private void applyBleed(ByteBuffer pixels) {
        final int width = videoInfo.imageWidth();
        final int height = videoInfo.imageHeight();
        final int channels = videoInfo.imageChannels();
        final int stride = videoInfo.imageStride();

        final int[] localXMaskLeft = xMaskLeft;
        final double[] localXMaskBlend = xMaskBlend;
        final int[] localYMaskTop = yMaskTop;
        final double[] localYMaskBlend = yMaskBlend;

        final int currentMaskWidth = maskWidth;
        final double[] currentBlurredMask = blurredMask;

        for (int y = 0; y < height; y++) {
            final int rowStart = y * stride;

            final int y0 = localYMaskTop[y];
            final int y1 = Math.min(y0 + 1, maskHeight - 1);
            final double yBlend = localYMaskBlend[y];

            final int topRowStart = y0 * currentMaskWidth;
            final int bottomRowStart = y1 * currentMaskWidth;

            for (int x = 0; x < width; x++) {
                final int x0 = localXMaskLeft[x];
                final int x1 = Math.min(x0 + 1, currentMaskWidth - 1);
                final double xBlend = localXMaskBlend[x];

                final double top = lerp(
                        currentBlurredMask[topRowStart + x0],
                        currentBlurredMask[topRowStart + x1],
                        xBlend);

                final double bottom = lerp(
                        currentBlurredMask[bottomRowStart + x0],
                        currentBlurredMask[bottomRowStart + x1],
                        xBlend);

                final double bleedStrength =
                        Math.min(1.0, lerp(top, bottom, yBlend) * BLEED_MASK_GAIN);

                if (bleedStrength <= 0.0) {
                    continue;
                }

                final int blueBoost =
                        (int) Math.round(bleedStrength * BLUE_BLEED_STRENGTH);
                final int greenBoost =
                        (int) Math.round(bleedStrength * GREEN_BLEED_STRENGTH);
                final int redBoost =
                        (int) Math.round(bleedStrength * RED_BLEED_STRENGTH);

                final int base = rowStart + x * channels;

                pixels.put(base, boostedByte(pixels.get(base), blueBoost));
                pixels.put(base + 1, boostedByte(pixels.get(base + 1), greenBoost));
                pixels.put(base + 2, boostedByte(pixels.get(base + 2), redBoost));
            }
        }
    }

    private byte boostedByte(byte value, int boost) {
        return (byte) Math.min(255, Byte.toUnsignedInt(value) + boost);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private double lerp(double a, double b, double blend) {
        return a + (b - a) * blend;
    }
}
