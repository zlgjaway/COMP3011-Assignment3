
package comp3011;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.bytedeco.javacv.Frame;
import org.junit.jupiter.api.Test;

class FramePeppererTest {

    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;
    private static final int CHANNELS = 3;
    private static final int STRIDE = WIDTH * CHANNELS;

    private InfoVideo createVideoInfo() {
        return new InfoVideo(
                "pepper-test-video",
                100,
                WIDTH,
                HEIGHT,
                Frame.DEPTH_UBYTE,
                CHANNELS,
                STRIDE,
                24.0,
                24,
                41667,
                0);
    }

    private Frame createFrame() {
        Frame frame = new Frame(
                WIDTH,
                HEIGHT,
                Frame.DEPTH_UBYTE,
                CHANNELS);

        ByteBuffer pixels = (ByteBuffer) frame.image[0];
        for (int i = 0; i < pixels.capacity(); i++) {
            pixels.put(i, (byte) 200);
        }

        return frame;
    }

    private byte[] copyPixels(Frame frame) {
        ByteBuffer pixels = (ByteBuffer) frame.image[0];
        byte[] copy = new byte[pixels.capacity()];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = pixels.get(i);
        }
        return copy;
    }


	@Test
	void peppererProducesDeterministicOutputForSameFrame() throws Exception {
	    FramePepperer pepperer = new FramePepperer();
	    pepperer.initialise(createVideoInfo());
	
	    Frame firstFrame = createFrame();
	    Frame secondFrame = createFrame();
	
	    InfoFrame info = new InfoFrame(10, 416670);
	
	    pepperer.process(firstFrame, info);
	    byte[] firstOutput = copyPixels(firstFrame);
	
	    // Reuse the same processor and frame number.
	    pepperer.process(secondFrame, info);
	    byte[] secondOutput = copyPixels(secondFrame);
	
	    assertArrayEquals(firstOutput, secondOutput);
	}


	@Test
	void peppererDarkensAtLeastSomePixels() throws Exception {
	    FramePepperer pepperer = new FramePepperer();
	    pepperer.initialise(createVideoInfo());

	    Frame frame = createFrame();
	    byte[] original = copyPixels(frame);

	    // Try several frames because some deterministic seeds may
	    // generate zero visible pepper marks.
	    boolean pixelsChanged = false;

	    for (int frameNumber = 0; frameNumber < 100; frameNumber++) {
	        frame = createFrame();

	        pepperer.process(
	                frame,
	                new InfoFrame(frameNumber, frameNumber * 41667L));

	        if (!Arrays.equals(original, copyPixels(frame))) {
	            pixelsChanged = true;
	            break;
	        }
	    }

	    assertTrue(pixelsChanged,
	            "Pepperer should darken at least some pixels across multiple frames");
	}
}
