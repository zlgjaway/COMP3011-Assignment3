
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
	
	private volatile java.util.List<SubtitleClient.Subtitle> subtitles =
	        java.util.List.of();

	
	private volatile boolean enabled = true;

	public void setEnabled(boolean enabled) {
	    this.enabled = enabled;
	}
	
	public void setSubtitles(
	        java.util.List<SubtitleClient.Subtitle> subtitles) {

	    this.subtitles = java.util.List.copyOf(subtitles);
	}

    @Override
    public void process(Frame frame, InfoFrame info) throws Exception {
    	
    	if (!enabled) {
    	    return;
    	}

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

            // Get the current video playback time in seconds.
            double currentTime = info.timestampUs() / 1_000_000.0;

            // Find the subtitle matching the current time.
            String text = "";

            for (SubtitleClient.Subtitle subtitle : subtitles) {

                if (currentTime >= subtitle.start()
                        && currentTime < subtitle.end()) {

                    text = subtitle.text();
                    break;
                }
            }

            // No subtitle for this frame.
            if (text.isEmpty()) {
                return;
            }

	         // Wrap subtitles into a maximum of two lines,
	         // with no more than 42 characters per line.
             java.util.List<String> lines;

             if (text.contains("\n")) {
                 lines = java.util.Arrays.asList(text.split("\n", -1));
             } else {
                 lines = wrapSubtitle(text);
             }
	
	         int lineHeight = g.getFontMetrics().getHeight();
	         int bottomY = height - Math.max(20, height / 12);
	         int firstY = bottomY - (lines.size() - 1) * lineHeight;
	
	         for (int i = 0; i < lines.size(); i++) {
	             String line = lines.get(i);
	
	             int textWidth = g.getFontMetrics().stringWidth(line);
	             int x = Math.max(0, (width - textWidth) / 2);
	             int y = firstY + i * lineHeight;
	
	             // Draw black outline.
	             g.setColor(Color.BLACK);
	
	             for (int dx = -2; dx <= 2; dx++) {
	                 for (int dy = -2; dy <= 2; dy++) {
	                     g.drawString(line, x + dx, y + dy);
	                 }
	             }
	
	             // Draw white subtitle text.
	             g.setColor(Color.WHITE);
	             g.drawString(line, x, y);
	         }

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
    
    private java.util.List<String> wrapSubtitle(String text) {

        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : text.trim().split("\\s+")) {

            // Split exceptionally long words so no line exceeds 42 characters.
            while (word.length() > 42) {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                }

                lines.add(word.substring(0, 42));
                word = word.substring(42);
            }

            if (word.isEmpty()) {
                continue;
            }

            int proposedLength = currentLine.length()
                    + (currentLine.isEmpty() ? 0 : 1)
                    + word.length();

            if (proposedLength > 42) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
            }

            if (!currentLine.isEmpty()) {
                currentLine.append(" ");
            }

            currentLine.append(word);
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        // Display at most two lines.
        if (lines.size() > 2) {
            return lines.subList(0, 2);
        }

        return lines;
    }
    
    
}
