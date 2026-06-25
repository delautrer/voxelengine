package tools;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BlockIconRenderer {

    private static final int TARGET_SIZE = 300;
    // Pitch anpassen, damit man von schräg oben schaut
    private double pitch = -Math.PI / 6;
    private double yaw = -Math.PI / 4;

    public BufferedImage generate3DBlockIcon(BufferedImage top, BufferedImage bot, BufferedImage front, BufferedImage back, BufferedImage left, BufferedImage right, String type) {
        BufferedImage result = new BufferedImage(TARGET_SIZE, TARGET_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Fallbacks (mostly for cubes if missing some)
        if (front == null && right != null) front = right;
        if (back == null && right != null) back = right;
        if (left == null && right != null) left = right;
        if (top == null && right != null) top = right;
        if (bot == null) bot = top;
        if (right == null) return result; // nothing to render

        double scale = 55.0;
        int cx = TARGET_SIZE / 2;
        int cy = TARGET_SIZE / 2;

        List<Face> faces = new ArrayList<>();

        if (type.contains("cross") || type.contains("plant") || type.contains("flower") || type.equals("grass") || type.equals("poppy") || type.equals("sandy_grass") || type.equals("dandelion") || type.contains("mushroom") || type.equals("tall_grass") || type.equals("fern") || type.equals("dead_bush") || type.contains("tulip") || type.equals("blue_orchid") || type.equals("allium") || type.equals("azure_bluet") || type.equals("fairy_bell") || type.equals("mavvinilia") || type.equals("sugar_cane") || type.contains("sapling")) {
            // Perfektes diagonales Kreuz für Pflanzen
            faces.add(createFace(new Vector3(-0.5,-1,-0.5), new Vector3(0.5,-1,0.5), new Vector3(0.5,1,0.5), new Vector3(-0.5,1,-0.5), right, 0.0));
            faces.add(createFace(new Vector3(0.5,-1,-0.5), new Vector3(-0.5,-1,0.5), new Vector3(-0.5,1,0.5), new Vector3(0.5,1,-0.5), right, 0.0));
        } else if (type.contains("torch")) {
            // Fackel 2x10x2
            faces.add(createFace(new Vector3(-1/16.0,-1,-1/16.0), new Vector3(1/16.0,-1,-1/16.0), new Vector3(1/16.0,-1,1/16.0), new Vector3(-1/16.0,-1,1/16.0), crop(top!=null?top:right, 7, 0, 9, 2), 0.0)); // Top
            faces.add(createFace(new Vector3(-1/16.0,10/16.0-1,1/16.0), new Vector3(1/16.0,10/16.0-1,1/16.0), new Vector3(1/16.0,10/16.0-1,-1/16.0), new Vector3(-1/16.0,10/16.0-1,-1/16.0), crop(bot!=null?bot:right, 7, 14, 9, 16), 0.5)); // Bot
            faces.add(createFace(new Vector3(-1/16.0,-1,1/16.0), new Vector3(1/16.0,-1,1/16.0), new Vector3(1/16.0,10/16.0-1,1/16.0), new Vector3(-1/16.0,10/16.0-1,1/16.0), crop(front, 7, 6, 9, 16), 0.2)); // Front
            faces.add(createFace(new Vector3(1/16.0,-1,-1/16.0), new Vector3(-1/16.0,-1,-1/16.0), new Vector3(-1/16.0,10/16.0-1,-1/16.0), new Vector3(1/16.0,10/16.0-1,-1/16.0), crop(back, 7, 6, 9, 16), 0.2)); // Back
            faces.add(createFace(new Vector3(1/16.0,-1,1/16.0), new Vector3(1/16.0,-1,-1/16.0), new Vector3(1/16.0,10/16.0-1,-1/16.0), new Vector3(1/16.0,10/16.0-1,1/16.0), crop(right, 7, 6, 9, 16), 0.35)); // Right
            faces.add(createFace(new Vector3(-1/16.0,-1,-1/16.0), new Vector3(-1/16.0,-1,1/16.0), new Vector3(-1/16.0,10/16.0-1,1/16.0), new Vector3(-1/16.0,10/16.0-1,-1/16.0), crop(left, 7, 6, 9, 16), 0.35)); // Left
        } else if (type.contains("chest")) {
            addBox(faces, 1, 0, 1, 15, 14, 15, top, bot, front, back, left, right);
        } else if (type.contains("door") && !type.contains("trapdoor")) {
            scale = 40.0;
            cy += 40;
            addBox(faces, 0, 0, 0, 16, 32, 3, top, bot, front, back, left, right);
        } else if (type.contains("trapdoor")) {
            addBox(faces, 0, 0, 0, 16, 3, 16, top, bot, front, back, left, right);
        } else if (type.contains("stairs")) {
            // Treppe soll nach unten rechts schauen ("in meine richtung rechts").
            // Dafür muss die volle Höhe hinten links (z=0..8) sein, und die Stufe vorne rechts (z=8..16).
            addBox(faces, 0, 0, 0, 16, 16, 8, top, bot, front, back, left, right); // Back half full height
            addBox(faces, 0, 0, 8, 16, 8, 16, top, bot, front, back, left, right); // Front half bottom
        } else if (type.contains("slab")) {
            addBox(faces, 0, 0, 0, 16, 8, 16, top, bot, front, back, left, right);  // Nur Bodenplatte
        } else {
            addBox(faces, 0, 0, 0, 16, 16, 16, top, bot, front, back, left, right); // Voller Block
        }

        // 3D Transformation
        double cosY = Math.cos(yaw), sinY = Math.sin(yaw), cosP = Math.cos(pitch), sinP = Math.sin(pitch);
        for (Face f : faces) {
            for (Vector3 v : f.vertices) {
                double x1 = v.x * cosY - v.z * sinY, z1 = v.x * sinY + v.z * cosY;
                v.tx = x1; v.ty = v.y * cosP - z1 * sinP; v.tz = v.y * sinP + z1 * cosP;
            }
            f.depth = (f.vertices[0].tz + f.vertices[1].tz + f.vertices[2].tz + f.vertices[3].tz) / 4.0;
        }

        faces.sort(Comparator.comparingDouble(f -> f.depth));
        for (Face f : faces) drawFace(g2d, f, cx, cy, scale);

        g2d.dispose();
        return result;
    }

    private void addBox(List<Face> faces, double x1, double y1, double z1, double x2, double y2, double z2, BufferedImage top, BufferedImage bot, BufferedImage front, BufferedImage back, BufferedImage left, BufferedImage right) {
        double rx1 = (x1 - 8)/8.0, rx2 = (x2 - 8)/8.0;
        double ry1 = -(y2 - 8)/8.0, ry2 = -(y1 - 8)/8.0; // Y invertiert für Screen
        double rz1 = (z1 - 8)/8.0, rz2 = (z2 - 8)/8.0;

        faces.add(createFace(new Vector3(rx1,ry1,rz1), new Vector3(rx2,ry1,rz1), new Vector3(rx2,ry1,rz2), new Vector3(rx1,ry1,rz2), crop(top!=null?top:right, x1, z1, x2, z2), 0.0)); // Top
        faces.add(createFace(new Vector3(rx1,ry2,rz2), new Vector3(rx2,ry2,rz2), new Vector3(rx2,ry2,rz1), new Vector3(rx1,ry2,rz1), crop(bot!=null?bot:right, x1, 16-z2, x2, 16-z1), 0.5)); // Bot
        faces.add(createFace(new Vector3(rx1,ry1,rz2), new Vector3(rx2,ry1,rz2), new Vector3(rx2,ry2,rz2), new Vector3(rx1,ry2,rz2), crop(front!=null?front:right, x1, Math.max(0, 16-y2), x2, 16-y1), 0.2)); // Front
        faces.add(createFace(new Vector3(rx2,ry1,rz1), new Vector3(rx1,ry1,rz1), new Vector3(rx1,ry2,rz1), new Vector3(rx2,ry2,rz1), crop(back!=null?back:right, 16-x2, Math.max(0, 16-y2), 16-x1, 16-y1), 0.2)); // Back
        faces.add(createFace(new Vector3(rx2,ry1,rz2), new Vector3(rx2,ry1,rz1), new Vector3(rx2,ry2,rz1), new Vector3(rx2,ry2,rz2), crop(right!=null?right:right, 16-z2, Math.max(0, 16-y2), 16-z1, 16-y1), 0.35)); // Right
        faces.add(createFace(new Vector3(rx1,ry1,rz1), new Vector3(rx1,ry1,rz2), new Vector3(rx1,ry2,rz2), new Vector3(rx1,ry2,rz1), crop(left!=null?left:right, z1, Math.max(0, 16-y2), z2, 16-y1), 0.35)); // Left
    }

    private BufferedImage crop(BufferedImage img, double u1, double v1, double u2, double v2) {
        if (img == null) return null;
        int x = (int) u1, y = (int) v1, w = (int) (u2 - u1), h = (int) (v2 - v1);
        if (w <= 0 || h <= 0) return img;
        double sX = img.getWidth() / 16.0, sY = img.getHeight() / 16.0;
        int cx = Math.max(0, (int)(x*sX));
        int cy = Math.max(0, (int)(y*sY));
        int cw = Math.max(1, (int)(w*sX));
        int ch = Math.max(1, (int)(h*sY));
        if (cx + cw > img.getWidth()) cw = img.getWidth() - cx;
        if (cy + ch > img.getHeight()) ch = img.getHeight() - cy;
        return img.getSubimage(cx, cy, cw, ch);
    }

    private Face createFace(Vector3 v0, Vector3 v1, Vector3 v2, Vector3 v3, BufferedImage img, double shadeValue) {
        Face f = new Face(); f.vertices = new Vector3[]{v0, v1, v2, v3}; f.img = img; f.shadeValue = shadeValue; return f;
    }

    private void drawFace(Graphics2D g2d, Face f, int cx, int cy, double scale) {
        int[] xP = new int[4], yP = new int[4];
        for (int i=0; i<4; i++) { xP[i] = (int)(cx + f.vertices[i].tx * scale); yP[i] = (int)(cy + f.vertices[i].ty * scale); }

        if (f.shadeValue >= 0 && (xP[1]-xP[0])*(yP[2]-yP[0]) - (yP[1]-yP[0])*(xP[2]-xP[0]) <= 0) return; // Backface Culling

        Polygon poly = new Polygon(xP, yP, 4);
        if (f.img != null) {
            double w = f.img.getWidth(), h = f.img.getHeight();
            if (w > 0 && h > 0) {
                AffineTransform at = new AffineTransform((xP[1]-xP[0])/w, (yP[1]-yP[0])/w, (xP[3]-xP[0])/h, (yP[3]-yP[0])/h, xP[0], yP[0]);
                Shape old = g2d.getClip(); g2d.setClip(poly); g2d.drawImage(f.img, at, null);
                if (f.shadeValue > 0) {
                    g2d.setColor(new Color(0,0,0, (int)(255 * f.shadeValue)));
                    g2d.fillPolygon(poly);
                }
                g2d.setClip(old);
            }
        }
    }

    private static class Vector3 {
        double x, y, z, tx, ty, tz;
        Vector3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    }

    private static class Face {
        Vector3[] vertices;
        double depth;
        BufferedImage img;
        double shadeValue;
    }
}
