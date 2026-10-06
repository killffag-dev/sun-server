package naryn.sun.systems.modules.constructions.viewmodel;

import naryn.sun.Sun;
import naryn.sun.framework.base.CustomScreen;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.modules.modules.visuals.ViewModel;
import naryn.sun.ui.components.textfield.TextField;
import naryn.sun.ui.menu.layout.ViewModelEditor;
import naryn.sun.ui.menu.toolbar.ViewModelToolbar;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.interfaces.IScaledResolution;
import net.minecraft.client.gui.DrawContext;

public class ViewModelScreen extends CustomScreen implements IScaledResolution, IMinecraft {

   private final ViewModel viewModel;
   private final ViewModelEditor editor;

   public ViewModelScreen() {
      this.viewModel = Sun.getInstance().getModuleManager().getModule(ViewModel.class);
      this.editor = new ViewModelEditor(this.viewModel);
      ViewModelToolbar.resetState();
   }

   @Override
   protected void init() {
      super.init();
      if (!this.viewModel.isEnabled()) {
         this.viewModel.setEnabled(true);
      }
      if (mc.options != null && !mc.options.getPerspective().isFirstPerson()) {
         mc.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      float screenW = IScaledResolution.sr.getScaledWidth();
      float screenH = IScaledResolution.sr.getScaledHeight();
      if (this.editor.onMouseScrolled(mouseX, mouseY, verticalAmount, screenW, screenH)) {
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public void render(UIContext context) {
      float screenW = IScaledResolution.sr.getScaledWidth();
      float screenH = IScaledResolution.sr.getScaledHeight();

      // Отрисовка обводки выбранного предмета
      this.editor.render(context, screenW, screenH);

      // Отрисовка верхнего островка (Сбросить обе / Сбросить руку / Готово) и нижней подсказки
      ViewModelToolbar.render(context, 1.0F, screenW, screenH);
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      float screenW = IScaledResolution.sr.getScaledWidth();
      float screenH = IScaledResolution.sr.getScaledHeight();

      if (ViewModelToolbar.onMouseClicked(mouseX, mouseY, button, screenW)) {
         return;
      }
      if (ViewModelToolbar.isResetBothHovered(mouseX, mouseY, screenW)) {
         this.viewModel.resetDefaults();
         return;
      }
      if (ViewModelToolbar.isResetHandHovered(mouseX, mouseY, screenW)) {
         if (this.editor.getActiveHand() == ViewModelEditor.HandElement.MAIN_HAND) {
            this.viewModel.resetMainHand();
         } else {
            this.viewModel.resetOffHand();
         }
         return;
      }
      if (ViewModelToolbar.isDoneHovered(mouseX, mouseY, screenW)) {
         this.close();
         return;
      }

      this.editor.onMouseClicked(mouseX, mouseY, button, screenW, screenH);
   }

   @Override
   public void onMouseDragged(double mouseX, double mouseY, MouseButton button, double deltaX, double deltaY) {
      float screenW = IScaledResolution.sr.getScaledWidth();
      float screenH = IScaledResolution.sr.getScaledHeight();

      ViewModelToolbar.onMouseDragged(mouseX, mouseY, screenW, screenH);
      this.editor.onMouseDragged(mouseX, mouseY, screenW, screenH);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      ViewModelToolbar.onMouseReleased(mouseX, mouseY, button);
      this.editor.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) { // GLFW_KEY_ESCAPE
         this.close();
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
      // Пустое тело для живого отображения 3D мира и рук игрока
   }

   @Override
   public void close() {
      if (TextField.LAST_FIELD != null) {
         TextField.LAST_FIELD.setFocused(false);
      }
      ViewModelMeshTracker.reset();
      super.close();
      mc.setScreen(Sun.getInstance().getMenuScreen());
   }
}
