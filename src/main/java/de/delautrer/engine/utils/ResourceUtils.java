package de.delautrer.engine.utils;

import de.delautrer.Constants;
import java.io.File;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Stream;

public class ResourceUtils {

    public static List<String> listResourceFolder(String folderPath) {
        List<String> result = new ArrayList<>();

        // Normierung für ZIP-Suche: "assets/recipes/"
        String zipSearchPath = folderPath.startsWith("/") ? folderPath.substring(1) : folderPath;
        if (!zipSearchPath.endsWith("/")) zipSearchPath += "/";

        try {
            if (Constants.IS_DEV) {
                // --- IDE MODUS (Normale Dateien) ---
                URL url = ResourceUtils.class.getResource(folderPath.startsWith("/") ? folderPath : "/" + folderPath);
                if (url == null) return result;

                Path path = Paths.get(url.toURI());
                try (Stream<Path> walk = Files.walk(path, 1)) {
                    walk.forEach(p -> {
                        String fileName = p.getFileName().toString();
                        if (!fileName.equals(path.getFileName().toString()) && !Files.isDirectory(p)) {
                            result.add(fileName);
                        }
                    });
                }
            } else {
                // --- EXPORT MODUS (.exe / .jar) ---
                // Den physischen Ort der laufenden Datei finden
                File jarFile = new File(ResourceUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI());

                // ZipFile durchsucht das Archiv von hinten (Zentralverzeichnis) - klappt bei EXE!
                try (ZipFile zip = new ZipFile(jarFile)) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String name = entry.getName();

                        // Wenn der Pfad mit dem gesuchten Ordner beginnt
                        if (name.startsWith(zipSearchPath)) {
                            String fileName = name.substring(zipSearchPath.length());

                            // Keine Unterordner, nur direkte Dateien im Verzeichnis
                            if (!fileName.isEmpty() && !fileName.contains("/")) {
                                result.add(fileName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ResourceUtils] Error listing folder: " + folderPath);
            e.printStackTrace();
        }
        return result;
    }
}