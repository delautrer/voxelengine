package de.delautrer.engine.utils;

import de.delautrer.Constants;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Stream;

public class ResourceUtils {

    public static List<String> listResourceFolder(String folderPath) {
        return listResources(folderPath, null);
    }

    public static List<String> listResources(String folderPath, String extension) {
        List<String> result = new ArrayList<>();
        String prefix = folderPath.startsWith("/") ? folderPath.substring(1) : folderPath;
        if (!prefix.endsWith("/")) prefix += "/";

        try {
            if (Constants.IS_DEV) {
                URL url = ResourceUtils.class.getResource("/" + prefix);
                if (url != null) {
                    Path rootPath = Paths.get(url.toURI());
                    if (Files.exists(rootPath)) {
                        try (Stream<Path> walk = Files.walk(rootPath)) {
                            walk.filter(Files::isRegularFile).forEach(p -> {
                                String rel = rootPath.relativize(p).toString().replace('\\', '/');
                                if (extension == null || rel.endsWith(extension)) {
                                    result.add(rel);
                                }
                            });
                        }
                    }
                }
            } else {
                File jarFile = new File(ResourceUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                try (ZipFile zip = new ZipFile(jarFile)) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (!entry.isDirectory() && name.startsWith(prefix)) {
                            String rel = name.substring(prefix.length());
                            if (extension == null || rel.endsWith(extension)) {
                                result.add(rel);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ResourceUtils] Error listing resources: " + folderPath);
            e.printStackTrace();
        }
        return result;
    }

    public static String readResourceToString(String resourcePath) throws IOException {
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        try (InputStream is = ResourceUtils.class.getResourceAsStream(path)) {
            if (is == null) throw new FileNotFoundException("Resource not found: " + resourcePath);
            byte[] bytes = is.readAllBytes();
            int offset = 0;
            if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
                offset = 3;
            }
            return new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
        }
    }

    public static InputStream getResourceAsStream(String resourcePath) {
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        return ResourceUtils.class.getResourceAsStream(path);
    }

    public static boolean hasResource(String resourcePath) {
        return getResourceAsStream(resourcePath) != null;
    }

    public static Reader readResourceToReader(String resourcePath) throws IOException {
        return new StringReader(readResourceToString(resourcePath));
    }
}