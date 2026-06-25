package tools;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

public class IconRenderer {

    private static final int TARGET_SIZE = 256;

    public BufferedImage generateIsometricIcon(BufferedImage top, BufferedImage left, BufferedImage right, String type,
                                               float brightTop, float brightLeft, float brightRight) {

        // Fallbacks
        if (top == null) top = sideFallback(left, right);
        if (left == null) left = sideFallback(top, right);
        if (right == null) right = sideFallback(top, left);
        if (top == null) return new BufferedImage(TARGET_SIZE, TARGET_SIZE, BufferedImage.TYPE_INT_ARGB);

        int r = top.getWidth();
        int hr = r / 2;

        BufferedImage result = new BufferedImage(TARGET_SIZE, TARGET_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        double scale = 16.0;
        
        double cx = 8.0 * scale, cy = 8.0 * scale;
        double w = 8.0 * scale, h = 4.0 * scale, d = 8.0 * scale;
        double ov = 0.15 * scale;
        double res = r;

        AffineTransform atTop = new AffineTransform((w+ov)/res, (h+ov)/res, -(w+ov)/res, (h+ov)/res, cx, cy - 2*h);
        AffineTransform atLeft = new AffineTransform((w+ov)/res, h/res, 0, (d+ov)/res, cx - w, cy - h);
        AffineTransform atRight = new AffineTransform((w+ov)/res, -h/res, 0, (d+ov)/res, cx - ov, cy - ov);
        AffineTransform oldTransform = g2d.getTransform();

        // 1. PFLANZEN & FACKELN: Flach und OHNE Schatten rendern
        if (type.contains("cross") || type.contains("torch")) {
            g2d.drawImage(left, 0, 0, TARGET_SIZE, TARGET_SIZE, null);
            g2d.dispose();
            return result;
        }

        // -- NEU: TUREN (Doors) als flaches 2D-Sprite rendern --
        if (type.contains("door") && !type.contains("trapdoor")) {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            int drawW = (int)(7 * scale);
            int drawH = (int)(7 * scale); // Top und Bottom jeweils 7 Pixel hoch -> 14 Pixel gesamt
            int xOffset = (int)(4 * scale); // Mittig zentriert: (16 - 7) / 2 = 4

            if (top != null) {
                // Obere Halfte auf Y = 1 zeichnen (1 Pixel Abstand nach oben)
                g2d.drawImage(top, xOffset, (int)(1 * scale), drawW, drawH, null);
            }
            if (left != null) {
                // Untere Halfte direkt darunter auf Y = 8 (1 + 7) zeichnen
                g2d.drawImage(left, xOffset, (int)(8 * scale), drawW, drawH, null);
            }

            g2d.dispose();
            return result;
        }

        // 2. BLOCKE: Helligkeit anwenden (Schatten)
        BufferedImage imgTop = applyBrightness(top, brightTop);
        BufferedImage imgLeft = applyBrightness(left, brightLeft);
        BufferedImage imgRight = applyBrightness(right, brightRight);

        // -- NEU: TRAPDOORS (Fallturen) --
        if (type.contains("trapdoor")) {
            AffineTransform topTrapdoor = new AffineTransform(atTop);

            // Dicke ist 4/16, wir verschieben die Top-Face also um 12/16 nach unten.
            double shift = 8.0 * (12.0 / 16.0);
            topTrapdoor.preConcatenate(AffineTransform.getTranslateInstance(0, shift));

            // Wir berechnen die Dicke in Pixeln (bei 16x16 Texturen sind das 4 Pixel)
            int thick = Math.max(1, (int) Math.round(r * (4.0 / 16.0)));
            int clipY = r - thick;

            // Wir clippen die Seiten so, dass nur die untersten ~3 Pixel gezeichnet werden
            drawClipped(g2d, atLeft, imgLeft, new Rectangle(0, clipY - 1, r, thick + 1), oldTransform);
            drawClipped(g2d, atRight, imgRight, new Rectangle(0, clipY - 1, r, thick + 1), oldTransform);
            drawClipped(g2d, topTrapdoor, imgTop, null, oldTransform);

            // -- NEU: KISTEN (Chests) --
        } else if (type.contains("chest")) {
            // Skalierung auf 14/16 (entspricht exakt deiner BoundingBox)
            double sf = 14.0 / 16.0;
            double cw = w * sf;
            double ch = h * sf;
            double cd = d * sf;

            // Y-Shift: Wir schieben das Modell um 1 Pixel nach unten,
            double yShift = d - cd;

            AffineTransform chestTop = new AffineTransform((cw+ov)/res, (ch+ov)/res, -(cw+ov)/res, (ch+ov)/res, cx, cy - 2*ch + yShift);
            AffineTransform chestLeft = new AffineTransform((cw+ov)/res, ch/res, 0, (cd+ov)/res, cx - cw, cy - ch + yShift);
            AffineTransform chestRight = new AffineTransform((cw+ov)/res, -ch/res, 0, (cd+ov)/res, cx - ov, cy - ov + yShift);

            drawClipped(g2d, chestTop, imgTop, null, oldTransform);
            drawClipped(g2d, chestLeft, imgLeft, null, oldTransform);
            drawClipped(g2d, chestRight, imgRight, null, oldTransform);

            // -- SLABS --
        } else if (type.contains("slab")) {
            AffineTransform topSlab = new AffineTransform(atTop);
            topSlab.preConcatenate(AffineTransform.getTranslateInstance(0, 8.0 / 2.0));
            drawClipped(g2d, atLeft, imgLeft, new Rectangle(0, hr - 1, r, r - hr + 1), oldTransform);
            drawClipped(g2d, atRight, imgRight, new Rectangle(0, hr - 1, r, r - hr + 1), oldTransform);
            drawClipped(g2d, topSlab, imgTop, null, oldTransform);

        } else if (type.contains("stairs")) {
            AffineTransform rightInner = new AffineTransform(atRight);
            rightInner.preConcatenate(AffineTransform.getTranslateInstance(-8.0/2.0, -4.0/2.0));
            AffineTransform topLow = new AffineTransform(atTop);
            topLow.preConcatenate(AffineTransform.getTranslateInstance(0, 8.0 / 2.0));

            drawClipped(g2d, atLeft, imgLeft, null, oldTransform);
            drawClipped(g2d, rightInner, imgRight, new Rectangle(0, 0, r, hr + 1), oldTransform);
            drawClipped(g2d, atRight, imgRight, new Rectangle(0, hr - 1, r, r - hr + 1), oldTransform);
            drawClipped(g2d, atTop, imgTop, new Rectangle(0, 0, hr + 1, r), oldTransform);
            drawClipped(g2d, topLow, imgTop, new Rectangle(hr - 1, 0, r - hr + 1, r), oldTransform);

        } else {
            drawClipped(g2d, atTop, imgTop, null, oldTransform);
            drawClipped(g2d, atLeft, imgLeft, null, oldTransform);
            drawClipped(g2d, atRight, imgRight, null, oldTransform);
        }

        g2d.dispose();
        return result;
    }

    private BufferedImage sideFallback(BufferedImage a, BufferedImage b) {
        if (a != null) return a;
        if (b != null) return b;
        return null;
    }

    private void drawClipped(Graphics2D g2d, AffineTransform at, BufferedImage img, Rectangle clip, AffineTransform old) {
        g2d.setTransform(at);
        g2d.setClip(clip);
        g2d.drawImage(img, 0, 0, null);
        g2d.setTransform(old);
        g2d.setClip(null);
    }

    private BufferedImage applyBrightness(BufferedImage src, float factor) {
        if (factor == 1.0f) return src;
        BufferedImage argb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics(); g.drawImage(src, 0, 0, null); g.dispose();
        RescaleOp op = new RescaleOp(new float[]{factor, factor, factor, 1.0f}, new float[]{0, 0, 0, 0}, null);
        return op.filter(argb, null);
    }
}
