package naryn.sun.systems.friends;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import naryn.sun.Sun;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.utility.game.EntityUtility;
import naryn.sun.utility.game.MessageUtility;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;
import ru.kotopushka.compiler.sdk.annotations.Compile;

public class FriendManager implements IMinecraft {
   private final List<String> friends = new ArrayList<>();

   public void add(String name) {
      if (Sun.getInstance().getTargetManager().getTarget().contains(name)) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.target")));
      } else if (this.friends.contains(name)) {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.exists", name)));
      } else if (name.equalsIgnoreCase(mc.getSession().getUsername())) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.self")));
      } else {
         this.friends.add(name);
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.added", name)));
         if (EntityUtility.isInGame()) {
            Sun.getInstance().getFileManager().writeFile("client");
         }
      }
   }

   public void remove(String name) {
      if (this.friends.contains(name)) {
         this.friends.remove(name);
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.removed", name)));
      } else {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.not_exists", name)));
      }

      Sun.getInstance().getFileManager().writeFile("client");
   }

   @Compile
   public void clear() {
      if (this.friends.isEmpty()) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.empty")));
      } else {
         this.friends.clear();
         MessageUtility.info(Text.of("Список друзей успешно очищен!"));
         Sun.getInstance().getFileManager().writeFile("client");
      }
   }

   public List<String> listFriends() {
      return Collections.unmodifiableList(this.friends);
   }

   public boolean isFriend(String name) {
      if (name == null || name.isEmpty()) {
         return false;
      }
      for (String friend : this.friends) {
         if (friend.equalsIgnoreCase(name)) {
            return true;
         }
      }
      return false;
   }
}
