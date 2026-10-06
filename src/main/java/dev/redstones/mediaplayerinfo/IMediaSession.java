package dev.redstones.mediaplayerinfo;

public interface IMediaSession {
   String getOwner();

   MediaInfo getMedia();

   void play();

   void pause();

   void playPause();

   void stop();

   void next();

   void previous();

   void swapCycle();

   int getCycleType();

   /**
    * Перемотка на конкретную позицию в мс. Требует поддержки в нативной
    * реализации (WindowsMediaSession/MediaPlayerInfo.dll) — если символ там
    * не экспортирован, вызов бросит UnsatisfiedLinkError, это нужно ловить
    * на стороне вызывающего кода.
    */
   void seekTo(long positionMs);
}