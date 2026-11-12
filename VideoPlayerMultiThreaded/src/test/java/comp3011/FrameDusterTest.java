
package comp3011;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;

import org.bytedeco.javacv.Frame;
import org.junit.jupiter.api.Test;

class FrameDusterTest {

    @Test
    void dusterProducesDeterministicOutputForSameFrame() throws Exception {
        FrameDuster duster = new FrameDuster();
        duster.initialise(createVideoInfo());

        Frame firstFrame = createFrame();
        Frame secondFrame = createFrame();

        InfoFrame info = new InfoFrame(10, 41667);

        duster.process(firstFrame, info);
        duster.process(secondFrame, info);

        assertArrayEquals(
                getPixels(firstFrame),
                getPixels(secondFrame));
    }

    

    private InfoVideo createVideoInfo() {
        return new InfoVideo(
                "dust-test-video",
                1000,                  // totalVideoFrames
                320,                   // imageWidth
                240,                   // imageHeight
                Frame.DEPTH_UBYTE,     // imageDepth
                3,                     // imageChannels
                960,                   // imageStride
                24.0,                  // frameRate
                24,                    // intFrameRate
                41667,                 // frameDurationUs
                0                      // pixelFormat
        );
    }

    private Frame createFrame() {
        Frame frame = new Frame(
                320,
                240,
                Frame.DEPTH_UBYTE,
                3);

        ByteBuffer pixels = (ByteBuffer) frame.image[0];
        for (int i = 0; i < pixels.capacity(); i++) {
            pixels.put(i, (byte) 200);
        }

        return frame;
    }

    private byte[] getPixels(Frame frame) {
        ByteBuffer pixels = (ByteBuffer) frame.image[0];
        byte[] result = new byte[pixels.capacity()];

        for (int i = 0; i < result.length; i++) {
            result[i] = pixels.get(i);
        }

        return result;
    }

    private boolean allPixelsAreOriginal(byte[] pixels) {
        for (byte pixel : pixels) {
            if (Byte.toUnsignedInt(pixel) != 200) {
                return false;
            }
        }
        return true;
    }
}
