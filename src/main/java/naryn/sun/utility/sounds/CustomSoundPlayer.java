package naryn.sun.utility.sounds;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CustomSoundPlayer {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread thread = new Thread(r, "Sun-CustomSoundPlayer");
        thread.setDaemon(true);
        return thread;
    });

    private CustomSoundPlayer() {
    }

    public static void playFile(File file, float volume) {
        if (file == null || !file.exists() || volume <= 0.001F) {
            return;
        }
        EXECUTOR.execute(() -> {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float clampedVol = Math.max(0.0001F, Math.min(1.0F, volume));
                    float dB = (float) (Math.log10(clampedVol) * 20.0);
                    dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                    gain.setValue(dB);
                }
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
                clip.start();
            } catch (Exception ignored) {
            }
        });
    }
}
