package naryn.sun.systems.modules.modules.visuals.fpsping;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.framework.objects.BorderRadius;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class FpsPingLogo {

    private static final Identifier IDENTIFIER = Identifier.of("sun", "textures/hud_watermark_icon");
    private static boolean loaded = false;
    private static boolean failed = false;

    private FpsPingLogo() {
    }

    public static Identifier getTexture() {
        if (loaded) {
            return IDENTIFIER;
        }
        if (failed) {
            return null;
        }

        InputStream is = openIconStream();
        if (is != null) {
            try (is) {
                NativeImage image = NativeImage.read(is);
                NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                MinecraftClient.getInstance().getTextureManager().registerTexture(IDENTIFIER, texture);
                loaded = true;
                return IDENTIFIER;
            } catch (Throwable t) {
                failed = true;
            }
        } else {
            failed = true;
        }
        return null;
    }

    private static InputStream openIconStream() {
        InputStream is = FpsPingLogo.class.getResourceAsStream("/assets/sun/icon.png");
        if (is != null) {
            return is;
        }

        is = FpsPingLogo.class.getResourceAsStream("/assets/sun/icon.jfif");
        if (is != null) {
            return is;
        }

        File fPng = new File("src/main/resources/assets/sun/icon.png");
        if (fPng.exists()) {
            try {
                return new FileInputStream(fPng);
            } catch (Exception ignored) {
            }
        }

        File fJfif = new File("src/main/resources/assets/sun/icon.jfif");
        if (fJfif.exists()) {
            try {
                return new FileInputStream(fJfif);
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    public static void draw(CustomDrawContext context, float x, float y, float size) {
        Identifier id = getTexture();
        if (id != null) {
            context.drawRoundedTexture(id, x, y, size, size, BorderRadius.all(2.5F));
        }
    }
}
