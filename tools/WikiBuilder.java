package tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

public class WikiBuilder {

    public static void main(String[] args) {
        System.out.println("Building V3 Wiki...");
        
        File docsDir = new File("docs");
        if (!docsDir.exists()) docsDir.mkdirs();
        File dataDir = new File("docs/data");
        if (!dataDir.exists()) dataDir.mkdirs();
        File rendersDir = new File("docs/assets/renders");
        if (!rendersDir.exists()) rendersDir.mkdirs();
        
        try {
            // 1. Read JSON Data
            String blocksJson = readFile("src/main/resources/assets/data/blocks.json", "[]");
            String itemsJson = readFile("src/main/resources/assets/data/items.json", "[]");
            String biomesJson = readFile("src/main/resources/assets/world/biomes.json", "[]");
            String furnaceRecipesJson = readFile("src/main/resources/assets/data/furnace_recipes.json", "[]");
            String furnaceFuelsJson = readFile("src/main/resources/assets/data/furnace_fuels.json", "{}");
            
            // 2. Read Recipes & Loot Tables into arrays
            String recipesJson = gatherJsonDirectory("src/main/resources/assets/recipes");
            String lootTablesJson = gatherJsonDirectory("src/main/resources/assets/loot_tables");
            
            String mechanicsJson = gatherMarkdownDirectory("docs/content/mechanics");
            String blocksMd = gatherMarkdownDirectory("docs/content/blocks");
            String itemsMd = gatherMarkdownDirectory("docs/content/items");
            
            // 3. Generate 3D Icons
            generateIcons(blocksJson);
            generateOreItems();
            
            // 4. Inject Lore
            StringBuilder jsBuilder = new StringBuilder();
            jsBuilder.append("const WIKI_DATA = {\n");
            jsBuilder.append("  blocks: ").append(blocksJson).append(",\n");
            jsBuilder.append("  items: ").append(itemsJson).append(",\n");
            jsBuilder.append("  biomes: ").append(biomesJson).append(",\n");
            jsBuilder.append("  furnace_recipes: ").append(furnaceRecipesJson).append(",\n");
            jsBuilder.append("  furnace_fuels: ").append(furnaceFuelsJson).append(",\n");
            jsBuilder.append("  recipes: ").append(recipesJson).append(",\n");
            jsBuilder.append("  loot_tables: ").append(lootTablesJson).append(",\n");
            jsBuilder.append("  mechanics: ").append(mechanicsJson).append(",\n");
            jsBuilder.append("  blocks_md: ").append(blocksMd).append(",\n");
            jsBuilder.append("  items_md: ").append(itemsMd).append("\n");
            jsBuilder.append("};\n");
            
            try (FileWriter fw = new FileWriter("docs/data/wiki_data.js")) {
                fw.write(jsBuilder.toString());
            }
            
            System.out.println("V3 Wiki Build Complete! Open docs/index.html to view.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static String readFile(String path, String defaultVal) {
        try {
            File f = new File(path);
            if (f.exists()) {
                return new String(Files.readAllBytes(Paths.get(path)));
            }
        } catch (IOException e) {
            System.err.println("Failed to read " + path);
        }
        return defaultVal;
    }
    
    private static String gatherJsonDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) return "[]";
        
        StringBuilder sb = new StringBuilder("[\n");
        boolean first = true;
        for (File f : dir.listFiles()) {
            if (f.getName().endsWith(".json")) {
                if (!first) sb.append(",\n");
                String content = readFile(f.getAbsolutePath(), "{}");
                String id = f.getName().replace(".json", "");
                if (content.trim().startsWith("{")) {
                    content = content.replaceFirst("\\{", "{ \"_id\": \"" + id + "\", ");
                }
                sb.append(content);
                first = false;
            }
        }
        sb.append("\n]");
        return sb.toString();
    }
    
    private static String gatherMarkdownDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) return "{}";
        
        StringBuilder sb = new StringBuilder("{\n");
        boolean first = true;
        for (File f : dir.listFiles()) {
            if (f.getName().endsWith(".md")) {
                if (!first) sb.append(",\n");
                String content = readFile(f.getAbsolutePath(), "");
                // Escape string for JSON
                content = content.replace("\\", "\\\\")
                                 .replace("\"", "\\\"")
                                 .replace("\n", "\\n")
                                 .replace("\r", "");
                String id = f.getName().replace(".md", "");
                sb.append("  \"").append(id).append("\": \"").append(content).append("\"");
                first = false;
            }
        }
        sb.append("\n}");
        return sb.toString();
    }

    private static BufferedImage composite(BufferedImage base, BufferedImage overlay) {
        BufferedImage res = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = res.createGraphics();
        g.drawImage(base, 0, 0, null);
        g.drawImage(overlay, 0, 0, null);
        g.dispose();
        return res;
    }
    
    private static void generateIcons(String blocksJson) {
        System.out.println("Generating 3D Block Icons...");
        BlockIconRenderer renderer = new BlockIconRenderer();
        
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher m = p.matcher(blocksJson);
        
        BufferedImage stoneTex = readImg(new File("src/main/resources/assets/textures/block/stone.png"));
        BufferedImage dolomiteTex = readImg(new File("src/main/resources/assets/textures/block/dolomite.png"));

        int count = 0;
        while (m.find()) {
            String id = m.group(1);
            
            String blockType = id;
            int idx = blocksJson.indexOf("\"name\": \"" + id + "\"");
            if (idx == -1) idx = blocksJson.indexOf("\"name\":  \"" + id + "\"");
            if (idx != -1) {
                int typeIdx = blocksJson.indexOf("\"type\":", idx);
                int endBracket = blocksJson.indexOf("}", idx);
                if (typeIdx != -1 && typeIdx < endBracket) {
                    int start = blocksJson.indexOf("\"", typeIdx + 7) + 1;
                    int end = blocksJson.indexOf("\"", start);
                    blockType = id + " " + blocksJson.substring(start, end);
                }
            }

            try {
                BufferedImage top    = loadTexture(id, "top", "end", "all", "side", "cross", "bottom");
                BufferedImage bottom = loadTexture(id, "bottom", "all", "top", "side", "cross", "end");
                BufferedImage front  = loadTexture(id, "front", "side", "all", "cross");
                BufferedImage back   = loadTexture(id, "back", "side", "all", "cross");
                BufferedImage left   = loadTexture(id, "left", "side", "all", "cross");
                BufferedImage right  = loadTexture(id, "right", "side", "all", "cross");
                
                // Fix Ore Compositing for 3D Preview
                if (id.endsWith("_ore")) {
                    BufferedImage base = id.startsWith("dolomite_") ? dolomiteTex : stoneTex;
                    if (base != null) {
                        if (top != null) top = composite(base, top);
                        if (bottom != null) bottom = composite(base, bottom);
                        if (front != null) front = composite(base, front);
                        if (back != null) back = composite(base, back);
                        if (left != null) left = composite(base, left);
                        if (right != null) right = composite(base, right);
                    }
                }

                // Chest fallback since chests use chest/<tree>/<side>
                if (id.contains("chest") && front == null) {
                    String tree = id.replace("_chest", "");
                    if (tree.equals("chest")) tree = "oak"; // base chest
                    top = loadTexture("chest/" + tree + "/top");
                    bottom = loadTexture("chest/" + tree + "/bottom");
                    front = loadTexture("chest/" + tree + "/front");
                    back = loadTexture("chest/" + tree + "/side");
                    left = loadTexture("chest/" + tree + "/side");
                    right = loadTexture("chest/" + tree + "/side");
                }

                // Door fallback (doors have top and bottom 16x16)
                if (id.endsWith("_door")) {
                    BufferedImage doorTop = loadTexture(id, "top");
                    BufferedImage doorBot = loadTexture(id, "bottom");
                    if (doorTop != null && doorBot != null) {
                        BufferedImage doorSide = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = doorSide.createGraphics();
                        g.drawImage(doorTop, 0, 0, null);
                        g.drawImage(doorBot, 0, 16, null);
                        g.dispose();
                        top = doorSide;
                        bottom = doorSide;
                        front = doorSide;
                        back = doorSide;
                        left = doorSide;
                        right = doorSide;
                    }
                }
                
                BufferedImage icon = renderer.generate3DBlockIcon(top, bottom, front, back, left, right, blockType);
                if (icon != null) {
                    javax.imageio.ImageIO.write(icon, "PNG", new File("docs/assets/renders/" + id + ".png"));
                    count++;
                }
            } catch (Exception e) {
                System.err.println("Failed to render icon for " + id + ": " + e.getMessage());
            }
        }
        System.out.println("Generated " + count + " icons.");
    }
    
    private static BufferedImage loadTexture(String id, String... fallbacks) {
        String base = "src/main/resources/assets/textures/block/";
        
        // 1. Exact match at root
        File f = new File(base + id + ".png");
        if (f.exists()) return readImg(f);
        
        // 2. Fallback suffixes at root
        for (String suffix : fallbacks) {
            f = new File(base + id + "_" + suffix + ".png");
            if (f.exists()) return readImg(f);
        }
        
        // 3. Structured sub-folders
        // --- PLANKS: planks/birch.png (from birch_planks) ---
        if (id.endsWith("_planks") || id.endsWith("_slab") || id.endsWith("_slabs") || id.endsWith("_stairs")) {
            // extract tree name prefix
            String treePrefix = id.replaceAll("_(planks|slabs?|stairs)$", "");
            f = new File(base + "planks/" + treePrefix + ".png");
            if (f.exists()) return readImg(f);
        }
        // Legacy plank naming fallback
        String plankId = id.replace("_planks", "_plank");
        f = new File(base + "planks/" + plankId + ".png");
        if (f.exists()) return readImg(f);
        
        // --- LOGS: log/<treename>/top.png / line.png ---
        // oak_log -> log/oak/top.png
        if (id.endsWith("_log")) {
            String treeName = id.replace("_log", "");
            File topImg = new File(base + "log/" + treeName + "/top.png");
            File sideImg = new File(base + "log/" + treeName + "/line.png");
            // For top and left/right respectively – use top for top face
            // For the loadTexture call pattern we return top for "top" suffix, side for "side"
            for (String suffix : fallbacks) {
                if (suffix.equals("top") || suffix.equals("end")) { if (topImg.exists()) return readImg(topImg); }
                if (suffix.equals("side") || suffix.equals("all") || suffix.equals("left") || suffix.equals("right")) { if (sideImg.exists()) return readImg(sideImg); }
            }
            if (topImg.exists()) return readImg(topImg);
            if (sideImg.exists()) return readImg(sideImg);
        }
        
        // --- LEAVES: leaves/<treename>.png ---
        if (id.endsWith("_leaves")) {
            String treePrefix = id.replaceAll("_leaves$", "");
            f = new File(base + "leaves/" + treePrefix + ".png");
            if (f.exists()) return readImg(f);
        }
        
        // --- DOORS: door/<treename>_door_<suffix>.png ---
        for (String suffix : fallbacks) {
            f = new File(base + "door/" + id + "_" + suffix + ".png");
            if (f.exists()) return readImg(f);
        }
        // fallback: any door texture file for this id
        f = new File(base + "door/");
        if (f.exists() && f.isDirectory()) {
            for (File child : f.listFiles()) {
                if (child.getName().startsWith(id)) return readImg(child);
            }
        }
        
        // --- TRAPDOORS (uses chest top and planks) ---
        if (id.endsWith("_trapdoor")) {
            String treePrefix = id.replaceAll("_trapdoor$", "");
            for (String suffix : fallbacks) {
                if (suffix.equals("top") || suffix.equals("bottom")) {
                    f = new File(base + "chest/" + treePrefix + "/top.png");
                    if (f.exists()) return readImg(f);
                }
            }
            f = new File(base + "planks/" + treePrefix + ".png");
            if (f.exists()) return readImg(f);
        }
        
        // --- CHESTS ---
        if (id.endsWith("_chest")) {
            String treePrefix = id.replaceAll("_chest$", "");
            for (String suffix : fallbacks) {
                f = new File(base + "chest/" + treePrefix + "/" + suffix + ".png");
                if (f.exists()) return readImg(f);
            }
            f = new File(base + "chest/" + treePrefix + "/front.png");
            if (f.exists()) return readImg(f);
        }
        // --- GENERIC FALLBACK FOR SLABS & STAIRS ---
        if (id.endsWith("_stairs") || id.endsWith("_slabs") || id.endsWith("_slab")) {
            String baseId = id.replaceAll("_(stairs|slabs?)$", "");
            if (baseId.equals("cobblestone")) baseId = "cobble";
            f = new File(base + baseId + ".png");
            if (f.exists()) return readImg(f);
            for (String suffix : fallbacks) {
                f = new File(base + baseId + "_" + suffix + ".png");
                if (f.exists()) return readImg(f);
            }
        }
        
        return null;
    }
    
    private static BufferedImage readImg(File f) {
        try {
            return ImageIO.read(f);
        } catch (Exception e) {
            return null;
        }
    }

    private static void generateOreItems() {
        try {
            File itemDir = new File("docs/assets/textures/item");
            if (!itemDir.exists()) itemDir.mkdirs();
            
            BufferedImage stone = readImg(new File("src/main/resources/assets/textures/block/stone.png"));
            BufferedImage dolomite = readImg(new File("src/main/resources/assets/textures/block/dolomite.png"));
            if (stone == null || dolomite == null) return;
            
            File blockDir = new File("src/main/resources/assets/textures/block");
            if (!blockDir.exists() || !blockDir.isDirectory()) {
                System.err.println("Block directory not found!");
                return;
            }
            
            for (File oreFile : blockDir.listFiles()) {
                String name = oreFile.getName();
                // skip if it's already a dolomite ore or not an ore
                if (name.endsWith("_ore.png") && !name.startsWith("dolomite_")) {
                    BufferedImage oreOverlay = readImg(oreFile);
                    if (oreOverlay == null) continue;
                    
                    String baseName = oreFile.getName(); // e.g. coal_ore.png
                    
                    // stone variant
                    BufferedImage stoneOre = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g1 = stoneOre.createGraphics();
                    g1.drawImage(stone, 0, 0, null);
                    g1.drawImage(oreOverlay, 0, 0, null);
                    g1.dispose();
                    ImageIO.write(stoneOre, "png", new File(itemDir, baseName));
                    
                    // dolomite variant
                    BufferedImage dolomiteOre = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = dolomiteOre.createGraphics();
                    g2.drawImage(dolomite, 0, 0, null);
                    g2.drawImage(oreOverlay, 0, 0, null);
                    g2.dispose();
                    ImageIO.write(dolomiteOre, "png", new File(itemDir, "dolomite_" + baseName));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to generate ore items: " + e.getMessage());
        }
    }
}
