
package comp3011;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.bytedeco.javacv.Frame;

/**
 * Assignment 3: Draws test subtitles onto video frames.
 * This first version uses fixed text to test rendering.
 */
public class FrameSubtitler extends FrameProcessor {

    @Override
    public void process(Frame frame, InfoFrame info) throws Exception {

        if (frame.image == null || frame.image.length == 0) {
            return;
        }

        if (!(frame.image[0] instanceof ByteBuffer pixels)) {
            return;
        }

        int width = videoInfo.imageWidth();
        int height = videoInfo.imageHeight();
        int channels = videoInfo.imageChannels();
        int stride = videoInfo.imageStride();

        if (channels < 1 || width <= 0 || height <= 0) {
            return;
        }

        if (videoInfo.imageDepth() != Frame.DEPTH_UBYTE
                && videoInfo.imageDepth() != Frame.DEPTH_BYTE) {
            return;
        }

        // Create a transparent layer for subtitle text.
        BufferedImage overlay = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = overlay.createGraphics();

        try {
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int fontSize = Math.max(16, width / 35);
            g.setFont(new Font("SansSerif", Font.BOLD, fontSize));

            String text = "Subtitle Test";

            int textWidth = g.getFontMetrics().stringWidth(text);
            int x = Math.max(0, (width - textWidth) / 2);
            int y = height - Math.max(20, height / 12);

            // Black outline for readability.
            g.setColor(Color.BLACK);

            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    g.drawString(text, x + dx, y + dy);
                }
            }

            // White subtitle text.
            g.setColor(Color.WHITE);
            g.drawString(text, x, y);

        } finally {
            g.dispose();
        }

        // Copy only the subtitle pixels into the video frame.
        // White/black values work with RGB, BGR and grayscale.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int argb = overlay.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;

                if (alpha == 0) {
                    continue;
                }

                int color = (argb >>> 16) & 0xFF;
                int base = y * stride + x * channels;

                if (base < 0 || base + channels > pixels.limit()) {
                    continue;
                }

                // Blend with the existing frame.
                int count = channels == 4 ? 3 : channels;

                for (int c = 0; c < count; c++) {
                    int original = pixels.get(base + c) & 0xFF;

                    int blended =
                            (color * alpha
                            + original * (255 - alpha)) / 255;

                    pixels.put(base + c, (byte) blended);
                }
            }
        }
    }
}
