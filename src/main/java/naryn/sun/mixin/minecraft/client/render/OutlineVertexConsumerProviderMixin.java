package naryn.sun.mixin.minecraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.visuals.Friends;
import naryn.sun.systems.modules.modules.visuals.Target;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OutlineVertexConsumerProvider.class)
public class OutlineVertexConsumerProviderMixin {

    @Inject(method = "draw", at = @At("HEAD"))
    private void onBeforeOutlineDraw(CallbackInfo ci) {
        Friends friends = Sun.getInstance().getModuleManager().getModule(Friends.class);
        boolean friendsActive = friends != null && friends.isEnabled() && friends.isOutline();
        Target target = Sun.getInstance().getModuleManager().getModule(Target.class);
        boolean targetActive = target != null && target.isEnabled() && target.isOutline();

        if (friendsActive || targetActive) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.worldRenderer != null && client.getFramebuffer() != null) {
                Framebuffer outlineFb = client.worldRenderer.getEntityOutlinesFramebuffer();
                if (outlineFb != null) {
                    outlineFb.copyDepthFrom(client.getFramebuffer());
                    client.getFramebuffer().beginWrite(false);
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthFunc(515);
                    RenderSystem.depthMask(false);
                    RenderSystem.enablePolygonOffset();
                    RenderSystem.polygonOffset(-0.5F, -2.0F);
                }
            }
        }
    }

    @Inject(method = "draw", at = @At("RETURN"))
    private void onAfterOutlineDraw(CallbackInfo ci) {
        Friends friends = Sun.getInstance().getModuleManager().getModule(Friends.class);
        boolean friendsActive = friends != null && friends.isEnabled() && friends.isOutline();
        Target target = Sun.getInstance().getModuleManager().getModule(Target.class);
        boolean targetActive = target != null && target.isEnabled() && target.isOutline();

        if (friendsActive || targetActive) {
            RenderSystem.polygonOffset(0.0F, 0.0F);
            RenderSystem.disablePolygonOffset();
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
            RenderSystem.depthFunc(515);
        }
    }
}
