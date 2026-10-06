package dev.redstones.mediaplayerinfo.impl.win;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import lombok.Generated;

public class WindowsMediaSession implements IMediaSession {
   private final MediaInfo media;
   private final String owner;
   private final int index;
   public static int cycle = -1;

   public WindowsMediaSession(MediaInfo media, String owner, int index) {
      this.media = media;
      this.owner = owner;
      this.index = index;
   }

   @Override
   public MediaInfo getMedia() {
      return this.media;
   }

   @Override
   public String getOwner() {
      return this.owner;
   }

   @Override
   public native void play();

   @Override
   public native void pause();

   @Override
   public native void playPause();

   @Override
   public native void stop();

   @Override
   public native void next();

   @Override
   public native void previous();

   @Override
   public native void swapCycle();

   @Override
   public native int getCycleType();

   // ДОБАВЛЕНО: перемотка. Пока не реализована в MediaPlayerInfo.dll —
   // вызов бросит UnsatisfiedLinkError, пока символ не появится в нативной
   // библиотеке (нужны исходники DLL, чтобы добавить это на C++/Rust стороне).
   @Override
   public native void seekTo(long positionMs);

   @Generated
   public static int getCycle() {
      return cycle;
   }

   @Generated
   public static void setCycle(int cycle) {
      WindowsMediaSession.cycle = cycle;
   }
}