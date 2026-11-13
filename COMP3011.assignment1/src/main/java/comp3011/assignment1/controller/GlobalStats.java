
package comp3011.assignment1.controller;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class GlobalStats {

    private final AtomicLong inputTokens = new AtomicLong(0);
    private final AtomicLong outputTokens = new AtomicLong(0);

    // Cumulative subtitle audio duration, stored in microseconds.
    private final AtomicLong audioDurationMicros =
            new AtomicLong(0);

    public void addInputTokens(long tokens) {
        inputTokens.addAndGet(tokens);
    }

    public void addOutputTokens(long tokens) {
        outputTokens.addAndGet(tokens);
    }

    public long getInputTokens() {
        return inputTokens.get();
    }

    public long getOutputTokens() {
        return outputTokens.get();
    }

    public void addAudioSeconds(double seconds) {

        if (!Double.isFinite(seconds) || seconds <= 0) {
            return;
        }

        long micros = Math.round(seconds * 1_000_000);

        audioDurationMicros.addAndGet(micros);
    }

    public double getAudioSeconds() {
        return audioDurationMicros.get() / 1_000_000.0;
    }
}
