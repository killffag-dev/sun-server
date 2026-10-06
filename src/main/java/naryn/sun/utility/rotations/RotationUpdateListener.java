package naryn.sun.utility.rotations;

import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.event.impl.player.InputEvent;
import naryn.sun.systems.event.impl.render.Render3DEvent;

public class RotationUpdateListener {
   private final EventListener<ClientPlayerTickEvent> onTick = event -> Sun.getInstance().getRotationHandler().update();
   private final EventListener<Render3DEvent> onRender = event -> Sun.getInstance().getRotationHandler().updateRender(event.getTickDelta());
   private final EventListener<InputEvent> onInputEvent = event -> {
      RotationHandler rotationHandler = Sun.INSTANCE.getRotationHandler();
      RotationTask currentTask = rotationHandler.getCurrentTask();
      if (!rotationHandler.isIdling() && currentTask != null && currentTask.getMoveCorrection() == MoveCorrection.SILENT) {
         event.setYaw(Sun.getInstance().getRotationHandler().getCurrentRotation().getYaw());
      }
   };

   public RotationUpdateListener() {
      Sun.getInstance().getEventManager().subscribe(this);
   }
}
