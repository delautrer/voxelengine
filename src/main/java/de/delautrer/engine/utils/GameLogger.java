package de.delautrer.engine.utils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GameLogger {

    public static void init(boolean isDev) {
        try {
            PrintStream consoleOut = System.out;
            PrintStream consoleErr = System.err;

            PrintStream fileOut = null;

            if (!isDev) {
                File logDir = GamePaths.LOGS_DIR.toFile();
                if (!logDir.exists()) logDir.mkdirs();

                String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                File logFile = new File(logDir, dateStr + ".log");

                fileOut = new PrintStream(new FileOutputStream(logFile, true));
                fileOut.println("\n=========================================");
                fileOut.println("GAME STARTED: " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
                fileOut.println("=========================================");
            }

            // Wir leiten JEDE Konsolenausgabe durch unseren eigenen Formatierer
            System.setOut(new PrintStream(new LogStream(isDev ? consoleOut : null, fileOut, "INFO"), true));
            System.setErr(new PrintStream(new LogStream(isDev ? consoleErr : null, fileOut, "ERROR"), true));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class LogStream extends OutputStream {
        private final PrintStream console;
        private final PrintStream file;
        private final String prefix;
        private boolean isNewLine = true;
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

        public LogStream(PrintStream console, PrintStream file, String prefix) {
            this.console = console;
            this.file = file;
            this.prefix = prefix;
        }

        @Override
        public void write(int b) throws IOException {
            if (isNewLine && b != '\n' && b != '\r') {
                String timePrefix = "[" + timeFormat.format(new Date()) + "] [" + prefix + "] ";
                if (console != null) console.print(timePrefix);
                if (file != null) file.print(timePrefix);
                isNewLine = false;
            }

            if (console != null) console.write(b);
            if (file != null) file.write(b);

            if (b == '\n') {
                isNewLine = true;
            }
        }
    }
}