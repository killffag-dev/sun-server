package naryn.sun.utility.sounds;

import naryn.sun.systems.file.FileManager;
import net.minecraft.util.Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ClientSoundManager {
    private static final ClientSoundManager INSTANCE = new ClientSoundManager();
    private final SoundConfig config = new SoundConfig();

    private long lastScrollTime = 0L;
    private long lastTypingTime = 0L;

    private ClientSoundManager() {
    }

    public static ClientSoundManager getInstance() {
        return INSTANCE;
    }

    public SoundConfig getConfig() {
        return config;
    }

    public File getSoundsDirectory() {
        return new File(FileManager.DIRECTORY, "sounds");
    }

    public void playScroll() {
        if (!config.isEnabled(SoundType.SCROLL)) return;
        long now = System.currentTimeMillis();
        if (now - lastScrollTime < 30L) return;
        lastScrollTime = now;

        float volume = config.getMasterVolume() * config.getVolume(SoundType.SCROLL);
        if (volume <= 0.001F) return;

        if (config.isCustomSoundsEnabled()) {
            File custom = new File(getSoundsDirectory(), "scroll.wav");
            if (custom.exists()) {
                CustomSoundPlayer.playFile(custom, volume);
                return;
            }
        }
        ClientSounds.SCROLL.play(volume, 1.0F);
    }

    public void playTyping() {
        if (!config.isEnabled(SoundType.TYPING)) return;
        long now = System.currentTimeMillis();
        if (now - lastTypingTime < 20L) return;
        lastTypingTime = now;

        float volume = config.getMasterVolume() * config.getVolume(SoundType.TYPING);
        if (volume <= 0.001F) return;

        if (config.isCustomSoundsEnabled()) {
            File custom = new File(getSoundsDirectory(), "typing.wav");
            if (custom.exists()) {
                CustomSoundPlayer.playFile(custom, volume);
                return;
            }
        }
        float pitch = 0.95F + (float) Math.random() * 0.1F;
        ClientSounds.TYPING.play(volume, pitch);
    }

    public void playModuleToggle(boolean enabled) {
        if (!config.isEnabled(SoundType.MODULE_TOGGLE)) return;
        float volume = config.getMasterVolume() * config.getVolume(SoundType.MODULE_TOGGLE);
        if (volume <= 0.001F) return;

        if (config.isCustomSoundsEnabled()) {
            String target = enabled ? "toggle_on.wav" : "toggle_off.wav";
            File custom = new File(getSoundsDirectory(), target);
            if (!custom.exists()) {
                custom = new File(getSoundsDirectory(), "toggle.wav");
            }
            if (custom.exists()) {
                CustomSoundPlayer.playFile(custom, volume);
                return;
            }
        }

        if (enabled) {
            ClientSounds.TOGGLE_ON.play(volume, 1.0F);
        } else {
            ClientSounds.TOGGLE_OFF.play(volume, 1.0F);
        }
    }

    public void playButtonClick() {
        if (!config.isEnabled(SoundType.BUTTON_CLICK)) return;
        float volume = config.getMasterVolume() * config.getVolume(SoundType.BUTTON_CLICK);
        if (volume <= 0.001F) return;

        if (config.isCustomSoundsEnabled()) {
            File custom = new File(getSoundsDirectory(), "click.wav");
            if (custom.exists()) {
                CustomSoundPlayer.playFile(custom, volume);
                return;
            }
        }
        ClientSounds.CLICK.play(volume, 1.0F);
    }

    public void playMenuOpen() {
        if (!config.isEnabled(SoundType.MENU_OPEN)) return;
        float volume = config.getMasterVolume() * config.getVolume(SoundType.MENU_OPEN);
        if (volume <= 0.001F) return;

        if (config.isCustomSoundsEnabled()) {
            File custom = new File(getSoundsDirectory(), "open.wav");
            if (custom.exists()) {
                CustomSoundPlayer.playFile(custom, volume);
                return;
            }
        }
        ClientSounds.OPEN.play(volume, 1.0F);
    }

    public void playOneShot(String soundId, float volume, float pitch) {
        float finalVolume = config.getMasterVolume() * volume;
        if (finalVolume <= 0.001F) return;

        if (config.isCustomSoundsEnabled()) {
            File custom = new File(getSoundsDirectory(), soundId + ".wav");
            if (custom.exists()) {
                CustomSoundPlayer.playFile(custom, finalVolume);
                return;
            }
        }

        new ClientSoundInstance(soundId, finalVolume, pitch).play(finalVolume, pitch);
    }

    public void playSoundPreview(SoundType type) {
        switch (type) {
            case SCROLL -> {
                float volume = config.getMasterVolume() * config.getVolume(SoundType.SCROLL);
                if (config.isCustomSoundsEnabled()) {
                    File custom = new File(getSoundsDirectory(), "scroll.wav");
                    if (custom.exists()) {
                        CustomSoundPlayer.playFile(custom, volume);
                        return;
                    }
                }
                ClientSounds.SCROLL.play(volume, 1.0F);
            }
            case TYPING -> {
                float volume = config.getMasterVolume() * config.getVolume(SoundType.TYPING);
                if (config.isCustomSoundsEnabled()) {
                    File custom = new File(getSoundsDirectory(), "typing.wav");
                    if (custom.exists()) {
                        CustomSoundPlayer.playFile(custom, volume);
                        return;
                    }
                }
                ClientSounds.TYPING.play(volume, 1.0F);
            }
            case MODULE_TOGGLE -> {
                float volume = config.getMasterVolume() * config.getVolume(SoundType.MODULE_TOGGLE);
                if (config.isCustomSoundsEnabled()) {
                    File custom = new File(getSoundsDirectory(), "toggle_on.wav");
                    if (!custom.exists()) custom = new File(getSoundsDirectory(), "toggle.wav");
                    if (custom.exists()) {
                        CustomSoundPlayer.playFile(custom, volume);
                        return;
                    }
                }
                ClientSounds.TOGGLE_ON.play(volume, 1.0F);
            }
            case BUTTON_CLICK -> {
                float volume = config.getMasterVolume() * config.getVolume(SoundType.BUTTON_CLICK);
                if (config.isCustomSoundsEnabled()) {
                    File custom = new File(getSoundsDirectory(), "click.wav");
                    if (custom.exists()) {
                        CustomSoundPlayer.playFile(custom, volume);
                        return;
                    }
                }
                ClientSounds.CLICK.play(volume, 1.0F);
            }
            case MENU_OPEN -> {
                float volume = config.getMasterVolume() * config.getVolume(SoundType.MENU_OPEN);
                if (config.isCustomSoundsEnabled()) {
                    File custom = new File(getSoundsDirectory(), "open.wav");
                    if (custom.exists()) {
                        CustomSoundPlayer.playFile(custom, volume);
                        return;
                    }
                }
                ClientSounds.OPEN.play(volume, 1.0F);
            }
        }
    }

    public void openSoundsFolder() {
        File dir = getSoundsDirectory();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File readme = new File(dir, "README.txt");
        if (!readme.exists()) {
            try (FileWriter writer = new FileWriter(readme)) {
                writer.write("""
                        ======================================================
                        SUN Client - Custom Sounds / Пользовательские звуки
                        ======================================================
                        Поместите сюда файлы формата .wav для замены звуков интерфейса:
                        
                        - scroll.wav       : Звук скролла
                        - typing.wav       : Звук печатания в поиске
                        - toggle_on.wav    : Звук включения модуля
                        - toggle_off.wav   : Звук выключения модуля (или toggle.wav для обоих)
                        - click.wav        : Звук клика по кнопкам UI
                        - open.wav         : Звук открытия меню
                        
                        Не забудьте включить тумблер "Пользовательские звуки" в разделе GUI Settings!
                        ======================================================
                        """);
            } catch (IOException ignored) {
            }
        }
        Util.getOperatingSystem().open(dir);
    }
}
