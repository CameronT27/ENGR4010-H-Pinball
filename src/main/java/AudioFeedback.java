import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public final class AudioFeedback implements AutoCloseable {
    private static final float SAMPLE_RATE = 44100.0f;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private final ExecutorService sounds = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "pinball-audio");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean unavailableWarningShown = new AtomicBoolean();

    public void update(GameSimulation simulation, InputState input) {
        if (input.leftFlipperPressedThisFrame() || input.rightFlipperPressedThisFrame()) {
            playTone(220.0, 0.06, 0.20);
        }
        if (simulation.consumeLaunchEvent()) {
            playTone(330.0, 0.16, 0.28);
        }
        if (simulation.consumeBumperEvent()) {
            playTone(660.0, 0.10, 0.30);
        }
        if (simulation.consumeKickerEvent()) {
            playTone(520.0, 0.08, 0.22);
        }
        if (simulation.consumeDrainEvent()) {
            playTone(110.0, 0.24, 0.28);
        }
    }

    private void playTone(double frequency, double duration, double volume) {
        sounds.submit(() -> {
            int sampleCount = (int) (SAMPLE_RATE * duration);
            byte[] samples = new byte[sampleCount * 2];
            for (int index = 0; index < sampleCount; index++) {
                double envelope = 1.0 - index / (double) sampleCount;
                short sample = (short) (Math.sin(2.0 * Math.PI * frequency * index / SAMPLE_RATE)
                        * Short.MAX_VALUE * volume * envelope);
                samples[index * 2] = (byte) (sample & 0xff);
                samples[index * 2 + 1] = (byte) (sample >> 8);
            }
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
                try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                    line.open(FORMAT);
                    line.start();
                    line.write(samples, 0, samples.length);
                    line.drain();
                }
            } catch (LineUnavailableException | IllegalArgumentException exception) {
                if (unavailableWarningShown.compareAndSet(false, true)) {
                    System.err.println("Audio feedback is unavailable: " + exception.getMessage());
                }
            }
        });
    }

    @Override
    public void close() {
        sounds.shutdownNow();
    }
}
