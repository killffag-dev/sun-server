package naryn.sun.systems.modules.modules.visuals.particle;

import java.util.Random;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

public final class HitParticleRenderer {

   public enum Shape {
      STARS,
      HEARTS,
      ORBS,
      RHOMBS,
      SUN,
      SHURIKEN,
      SNOWFLAKE,
      SAKURA
   }

   private static final Shape[] ALL_SHAPES = Shape.values();

   private HitParticleRenderer() {
   }

   public static Shape getRandomShape(Random random) {
      return ALL_SHAPES[random.nextInt(ALL_SHAPES.length)];
   }

   public static void renderParticle(Matrix4f m, BufferBuilder buf, Shape shape, float size, float r, float g, float b, float alpha) {
      switch (shape) {
         case STARS -> drawStar(m, buf, size, r, g, b, alpha);
         case HEARTS -> drawHeart(m, buf, size, r, g, b, alpha);
         case ORBS -> drawOrb(m, buf, size, r, g, b, alpha);
         case RHOMBS -> drawRhomb(m, buf, size, r, g, b, alpha);
         case SUN -> drawSun(m, buf, size, r, g, b, alpha);
         case SHURIKEN -> drawShuriken(m, buf, size, r, g, b, alpha);
         case SNOWFLAKE -> drawSnowflake(m, buf, size, r, g, b, alpha);
         case SAKURA -> drawSakura(m, buf, size, r, g, b, alpha);
      }
   }

   public static void drawStar(Matrix4f m, BufferBuilder buf, float size, float r, float g, float b, float alpha) {
      if (alpha <= 0.0F) return;
      float half = size * 0.12F;
      for (int i = 0; i < 4; i++) {
         double angle = Math.PI * 0.5 * i;
         float tx = (float) (Math.cos(angle) * size);
         float ty = (float) (Math.sin(angle) * size);
         float px = (float) (Math.cos(angle + Math.PI * 0.5) * half);
         float py = (float) (Math.sin(angle + Math.PI * 0.5) * half);

         buf.vertex(m, px, py, 0.0F).color(r, g, b, alpha);
         buf.vertex(m, -px, -py, 0.0F).color(r, g, b, alpha);
         buf.vertex(m, tx, ty, 0.0F).color(r, g, b, 0.0F);
      }

      float core = size * 0.22F;
      buf.vertex(m, 0.0F, core, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
      buf.vertex(m, core, 0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
      buf.vertex(m, -core, 0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);

      buf.vertex(m, 0.0F, -core, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
      buf.vertex(m, -core, 0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
      buf.vertex(m, core, 0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
   }

   public static void drawHeart(Matrix4f m, BufferBuilder buf, float size, float r, float g, float b, float alpha) {
      if (alpha <= 0.0F) return;
      int segs = 20;
      float cx = 0.0F;
      float cy = -size * 0.12F;
      for (int i = 0; i < segs; i++) {
         double t1 = (Math.PI * 2.0 * i) / segs;
         double t2 = (Math.PI * 2.0 * (i + 1)) / segs;

         float st1 = (float) Math.sin(t1);
         float x1 = (st1 * st1 * st1) * size;
         float y1 = -(float) (13.0 * Math.cos(t1) - 5.0 * Math.cos(2.0 * t1) - 2.0 * Math.cos(3.0 * t1) - Math.cos(4.0 * t1)) / 16.0F * size;

         float st2 = (float) Math.sin(t2);
         float x2 = (st2 * st2 * st2) * size;
         float y2 = -(float) (13.0 * Math.cos(t2) - 5.0 * Math.cos(2.0 * t2) - 2.0 * Math.cos(3.0 * t2) - Math.cos(4.0 * t2)) / 16.0F * size;

         buf.vertex(m, cx, cy, 0.0F).color(r, g, b, alpha);
         buf.vertex(m, x1, y1, 0.0F).color(r, g, b, alpha * 0.9F);
         buf.vertex(m, x2, y2, 0.0F).color(r, g, b, alpha * 0.9F);
      }
   }

   public static void drawOrb(Matrix4f m, BufferBuilder buf, float size, float r, float g, float b, float alpha) {
      if (alpha <= 0.0F) return;
      int segs = 14;
      for (int i = 0; i < segs; i++) {
         double a1 = (Math.PI * 2.0 * i) / segs;
         double a2 = (Math.PI * 2.0 * (i + 1)) / segs;
         float x1 = (float) Math.cos(a1) * size;
         float y1 = (float) Math.sin(a1) * size;
         float x2 = (float) Math.cos(a2) * size;
         float y2 = (float) Math.sin(a2) * size;

         buf.vertex(m, 0.0F, 0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
         buf.vertex(m, x1, y1, 0.0F).color(r, g, b, 0.0F);
         buf.vertex(m, x2, y2, 0.0F).color(r, g, b, 0.0F);
      }
   }

   public static void drawRhomb(Matrix4f m, BufferBuilder buf, float size, float r, float g, float b, float alpha) {
      if (alpha <= 0.0F) return;
      float w = size * 0.65F;
      float h = size;

      buf.vertex(m, 0.0F, h, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
      buf.vertex(m, -w, 0.0F, 0.0F).color(r, g, b, alpha);
      buf.vertex(m, w, 0.0F, 0.0F).color(r, g, b, alpha);

      buf.vertex(m, 0.0F, -h, 0.0F).color(r * 0.65F, g * 0.65F, b * 0.65F, alpha);
      buf.vertex(m, w, 0.0F, 0.0F).color(r, g, b, alpha);
      buf.vertex(m, -w, 0.0F, 0.0F).color(r, g, b, alpha);
   }

   public static void drawSun(Matrix4f m, BufferBuilder buf, float size, float r, float g, float b, float alpha) {
      if (alpha <= 0.0F) return;
      float baseHalf = size * 0.09F;
      for (int i = 0; i < 8; i++) {
         double ang = (Math.PI * 2.0 * i) / 8.0;
         float len = (i % 2 == 0) ? size : size * 0.62F;
         float tx = (float) Math.cos(ang) * len;
         float ty = (float) Math.sin(ang) * len;
         float px = (float) Math.cos(ang + Math.PI * 0.5) * baseHalf;
         float py = (float) Math.sin(ang + Math.PI * 0.5) * baseHalf;

         buf.vertex(m, px, py, 0.0F).color(r, g, b, alpha);
         buf.vertex(m, -px, -py, 0.0F).color(r, g, b, alpha);
         buf.vertex(m, tx, ty, 0.0F).color(r, g, b, 0.0F);
      }

      float core = size * 0.28F;
      int segs = 12;
      for (int i = 0; i < segs; i++) {
         double a1 = (Math.PI * 2.0 * i) / segs;
         double a2 = (Math.PI * 2.0 * (i + 1)) / segs;
         buf.vertex(m, 0.0F, 0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
         buf.vertex(m, (float) Math.cos(a1) * core, (float) Math.sin(a1) * core, 0.0F).color(r, g, b, alpha * 0.85F);
         buf.vertex(m, (float) Math.cos(a2) * core, (float) Math.sin(a2) * core, 0.0F).color(r, g, b, alpha * 0.85F);
      }
   }

   public static void drawShuriken(Matrix4f m, BufferBuilder buf, float size, float r, float g, float b, float alpha) {
      if (alpha <= 0.0F) return;
      float hub = size * 0.22F;
      for (int i = 0; i < 4; i++) {
         double a = (Math.PI * 0.5 * i);
         float tipX = (float) Math.cos(a) * size;
         float tipY = (float) Math.sin(a) * size;

         double aLead = a + 0.45;
         float lx = (float) Math.cos(aLead) * hub;
         float ly = (float) Math.sin(aLead) * hub;

         double aTrail = a - 0.38;
         float tx = (float) Math.cos(aTrail) * hub;
         float ty = (float) Math.sin(aTrail) * hub;

         buf.vertex(m, tipX, tipY, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
         buf.vertex(m, lx, ly, 0.0F).color(r, g, b, alpha);
         buf.vertex(m, 0.0F, 0.0F, 0.0F).color(r, g, b, alpha);

         buf.vertex(m, tipX, tipY, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
         buf.vertex(m, 0.0F, 0.0F, 0.0F).color(r * 0.55F, g * 0.55F, b * 0.55F, alpha);
         buf.vertex(m, tx, ty, 0.0F).color(r * 0.55F, g * 0.55F, b * 0.55F, alpha);
      }
   }

   public static void drawSnowflake(Matrix4f m, BufferBuilder buf, float size, float r, float g, float b, float alpha) {
      if (alpha <= 0.0F) return;
      float s = size * 0.78F;

      // 1. Центральное шестиугольное ядро со светящимся центром
      float core = s * 0.18F;
      for (int i = 0; i < 6; i++) {
         double a1 = (Math.PI * 2.0 * i) / 6.0;
         double a2 = (Math.PI * 2.0 * (i + 1)) / 6.0;
         buf.vertex(m, 0.0F, 0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
         buf.vertex(m, (float) Math.cos(a1) * core, (float) Math.sin(a1) * core, 0.0F).color(r, g, b, alpha * 0.9F);
         buf.vertex(m, (float) Math.cos(a2) * core, (float) Math.sin(a2) * core, 0.0F).color(r, g, b, alpha * 0.9F);
      }

      float stemW = s * 0.038F;

      // 2. Шесть основных лучей с ромбовидными наконечниками и боковыми веточками
      for (int i = 0; i < 6; i++) {
         double ang = (Math.PI * 2.0 * i) / 6.0;
         float cos = (float) Math.cos(ang);
         float sin = (float) Math.sin(ang);
         float perpX = -sin * stemW;
         float perpY = cos * stemW;

         float startX = cos * core;
         float startY = sin * core;
         float midX = cos * s * 0.72F;
         float midY = sin * s * 0.72F;

         drawQuad(m, buf,
            startX + perpX, startY + perpY, r, g, b, alpha * 0.85F,
            startX - perpX, startY - perpY, r, g, b, alpha * 0.85F,
            midX - perpX, midY - perpY, r, g, b, alpha * 0.95F,
            midX + perpX, midY + perpY, r, g, b, alpha * 0.95F
         );

         // Внутренние веточки (V-образные на 0.42 от размера под углом 60°)
         float b1x = cos * s * 0.42F;
         float b1y = sin * s * 0.42F;
         float barb1Len = s * 0.20F;
         drawBarb(m, buf, b1x, b1y, ang + Math.PI / 3.0, barb1Len, stemW * 0.8F, r, g, b, alpha);
         drawBarb(m, buf, b1x, b1y, ang - Math.PI / 3.0, barb1Len, stemW * 0.8F, r, g, b, alpha);

         // Внешние веточки (V-образные на 0.68 от размера под углом 60°)
         float b2x = cos * s * 0.68F;
         float b2y = sin * s * 0.68F;
         float barb2Len = s * 0.16F;
         drawBarb(m, buf, b2x, b2y, ang + Math.PI / 3.0, barb2Len, stemW * 0.75F, r, g, b, alpha);
         drawBarb(m, buf, b2x, b2y, ang - Math.PI / 3.0, barb2Len, stemW * 0.75F, r, g, b, alpha);

         // Ромбовидный наконечник основного луча
         float tipW = s * 0.09F;
         float tpx = -sin * tipW;
         float tpy = cos * tipW;
         float waistX = cos * s * 0.86F;
         float waistY = sin * s * 0.86F;
         float endX = cos * s;
         float endY = sin * s;

         drawDiamond(m, buf,
            midX, midY, r, g, b, alpha * 0.9F,
            waistX + tpx, waistY + tpy, 1.0F, 1.0F, 1.0F, alpha,
            endX, endY, 1.0F, 1.0F, 1.0F, alpha * 0.8F,
            waistX - tpx, waistY - tpy, 1.0F, 1.0F, 1.0F, alpha
         );
      }

      // 3. Шесть промежуточных лучей (между основными, формируют округлый силуэт)
      float subCore = core * 0.8F;
      for (int i = 0; i < 6; i++) {
         double angMid = (Math.PI * 2.0 * i) / 6.0 + Math.PI / 6.0;
         float cos = (float) Math.cos(angMid);
         float sin = (float) Math.sin(angMid);
         float perpX = -sin * stemW * 0.7F;
         float perpY = cos * stemW * 0.7F;

         float startX = cos * subCore;
         float startY = sin * subCore;
         float baseDiamondX = cos * s * 0.32F;
         float baseDiamondY = sin * s * 0.32F;

         drawQuad(m, buf,
            startX + perpX, startY + perpY, r, g, b, alpha * 0.7F,
            startX - perpX, startY - perpY, r, g, b, alpha * 0.7F,
            baseDiamondX - perpX, baseDiamondY - perpY, r, g, b, alpha * 0.8F,
            baseDiamondX + perpX, baseDiamondY + perpY, r, g, b, alpha * 0.8F
         );

         float subTipW = s * 0.065F;
         float tpx = -sin * subTipW;
         float tpy = cos * subTipW;
         float waistX = cos * s * 0.44F;
         float waistY = sin * s * 0.44F;
         float endX = cos * s * 0.54F;
         float endY = sin * s * 0.54F;

         drawDiamond(m, buf,
            baseDiamondX, baseDiamondY, r, g, b, alpha * 0.75F,
            waistX + tpx, waistY + tpy, 1.0F, 1.0F, 1.0F, alpha * 0.9F,
            endX, endY, 1.0F, 1.0F, 1.0F, alpha * 0.7F,
            waistX - tpx, waistY - tpy, 1.0F, 1.0F, 1.0F, alpha * 0.9F
         );
      }
   }

   private static void drawBarb(Matrix4f m, BufferBuilder buf, float x, float y, double angle, float len, float w, float r, float g, float b, float alpha) {
      float cos = (float) Math.cos(angle);
      float sin = (float) Math.sin(angle);
      float px = -sin * w;
      float py = cos * w;

      float tipX = x + cos * len;
      float tipY = y + sin * len;

      buf.vertex(m, x + px, y + py, 0.0F).color(r, g, b, alpha * 0.85F);
      buf.vertex(m, x - px, y - py, 0.0F).color(r, g, b, alpha * 0.85F);
      buf.vertex(m, tipX, tipY, 0.0F).color(1.0F, 1.0F, 1.0F, alpha * 0.9F);
   }

   private static void drawQuad(Matrix4f m, BufferBuilder buf,
                                float x1, float y1, float r1, float g1, float b1, float a1,
                                float x2, float y2, float r2, float g2, float b2, float a2,
                                float x3, float y3, float r3, float g3, float b3, float a3,
                                float x4, float y4, float r4, float g4, float b4, float a4) {
      buf.vertex(m, x1, y1, 0.0F).color(r1, g1, b1, a1);
      buf.vertex(m, x2, y2, 0.0F).color(r2, g2, b2, a2);
      buf.vertex(m, x3, y3, 0.0F).color(r3, g3, b3, a3);

      buf.vertex(m, x1, y1, 0.0F).color(r1, g1, b1, a1);
      buf.vertex(m, x3, y3, 0.0F).color(r3, g3, b3, a3);
      buf.vertex(m, x4, y4, 0.0F).color(r4, g4, b4, a4);
   }

   private static void drawDiamond(Matrix4f m, BufferBuilder buf,
                                   float x1, float y1, float r1, float g1, float b1, float a1,
                                   float x2, float y2, float r2, float g2, float b2, float a2,
                                   float x3, float y3, float r3, float g3, float b3, float a3,
                                   float x4, float y4, float r4, float g4, float b4, float a4) {
      buf.vertex(m, x1, y1, 0.0F).color(r1, g1, b1, a1);
      buf.vertex(m, x2, y2, 0.0F).color(r2, g2, b2, a2);
      buf.vertex(m, x3, y3, 0.0F).color(r3, g3, b3, a3);

      buf.vertex(m, x1, y1, 0.0F).color(r1, g1, b1, a1);
      buf.vertex(m, x3, y3, 0.0F).color(r3, g3, b3, a3);
      buf.vertex(m, x4, y4, 0.0F).color(r4, g4, b4, a4);
   }


   public static void drawSakura(Matrix4f m, BufferBuilder buf, float size, float r, float g, float b, float alpha) {
      if (alpha <= 0.0F) return;
      float[][] outline = {
         {0.0F, -size * 0.6F},
         {-size * 0.32F, -size * 0.2F},
         {-size * 0.42F, size * 0.18F},
         {-size * 0.22F, size * 0.55F},
         {0.0F, size * 0.38F},
         {size * 0.22F, size * 0.55F},
         {size * 0.42F, size * 0.18F},
         {size * 0.32F, -size * 0.2F}
      };

      for (int i = 0; i < outline.length; i++) {
         float[] p1 = outline[i];
         float[] p2 = outline[(i + 1) % outline.length];
         buf.vertex(m, 0.0F, 0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
         buf.vertex(m, p1[0], p1[1], 0.0F).color(r, g, b, alpha * 0.9F);
         buf.vertex(m, p2[0], p2[1], 0.0F).color(r, g, b, alpha * 0.9F);
      }
   }
}
