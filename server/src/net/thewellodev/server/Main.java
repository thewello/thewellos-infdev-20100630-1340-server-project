package net.thewellodev.server;

import net.thewellodev.server.gui.ServerGui;
import net.thewellodev.server.log.LogSetup;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TheWello's Infdev Server entry point.
 *
 * <pre>
 * java -Xmx1G -jar TheWellosInfdevServer.jar          (with the control window)
 * java -Xmx1G -jar TheWellosInfdevServer.jar nogui    (console only)
 * </pre>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        boolean gui = !GraphicsEnvironment.isHeadless();
        for (String a : args) {
            if (a.equalsIgnoreCase("nogui") || a.equalsIgnoreCase("--nogui")) {
                gui = false;
            }
        }
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        final Logger logger = LogSetup.setup(dir);
        final TWServer server = new TWServer(dir, logger);
        ServerGui window = null;
        if (gui) {
            try {
                window = ServerGui.open(server);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Could not open the window, continuing in console mode", t);
            }
        }
        final ServerGui fw = window;

        Thread consoleThread = new Thread("TW console") {
            public void run() {
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
                    String line;
                    while ((line = r.readLine()) != null) {
                        server.submitConsoleInput(line);
                    }
                } catch (Exception ignored) {
                }
            }
        };
        consoleThread.setDaemon(true);
        consoleThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread("TW shutdown") {
            public void run() {
                if (server.isRunning()) {
                    server.shutdown();
                    long until = System.currentTimeMillis() + 15000;
                    while (!server.isStopped() && System.currentTimeMillis() < until) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
        });

        Thread serverThread = new Thread("Server thread") {
            public void run() {
                try {
                    server.start();
                    if (fw != null) {
                        fw.onServerStarted();
                    }
                    logger.info("Tip: lines starting with '/' are commands (/help); other lines are sent to players as [Server] messages.");
                    server.run();
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, "The server could not start!", t);
                    if (fw != null) {
                        fw.onServerFailed(t);
                    } else {
                        System.exit(1);
                    }
                    return;
                }
                if (fw == null) {
                    System.exit(0);
                }
            }
        };
        serverThread.start();
    }
}
