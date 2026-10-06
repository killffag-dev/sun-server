package naryn.sun.systems.file;

import java.io.File;
import lombok.Generated;
import naryn.sun.systems.file.api.FileInfo;

public abstract class ClientFile {
   public final FileInfo infoAnnotation = this.getClass().getAnnotation(FileInfo.class);
   public final File file = initFile();

   private File initFile() {
      File target = new File(FileManager.DIRECTORY, this.infoAnnotation.name() + "." + this.infoAnnotation.fileType());
      File legacy = new File(FileManager.DIRECTORY, this.infoAnnotation.name() + ".rock");
      if (!target.exists() && legacy.exists()) {
         legacy.renameTo(target);
      }
      return target;
   }

   public abstract void write();

   public abstract void read();

   @Generated
   public FileInfo getInfoAnnotation() {
      return this.infoAnnotation;
   }

   @Generated
   public File getFile() {
      return this.file;
   }
}
