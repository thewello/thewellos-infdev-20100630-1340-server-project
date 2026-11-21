package net.thewellodev.server.log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** Server logging: console, logs/latest.log and the GUI. System.out and System.err are captured too. */
public final class LogSetup {

    /** Log listeners such as the GUI. */
    public interface LogListener {
        void onLog(Level level, String formattedLine);
    }

    private static final List<LogListener> listeners = new CopyOnWriteArrayList<LogListener>();
    private static PrintStream originalOut;
    private static Logger logger;

    private LogSetup() {
    }

    public static void addListener(LogListener l) {
        listeners.add(l);
    }

    public static Logger getLogger() {
        return logger;
    }

    public static final class LineFormatter extends Formatter {
        private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");

        @Override
        public synchronized String format(LogRecord r) {
            StringBuilder sb = new StringBuilder();
            sb.append('[').append(fmt.format(new Date(r.getMillis()))).append(' ');
            sb.append(levelName(r.getLevel())).append("]: ");
            sb.append(formatMessage(r));
            if (r.getThrown() != null) {
                StringWriter sw = new StringWriter();
                r.getThrown().printStackTrace(new PrintWriter(sw));
                sb.append('\n').append(sw.toString().trim());
            }
            return sb.append('\n').toString();
        }
    }

    public static String levelName(Level l) {
        if (l == Level.SEVERE) {
            return "ERROR";
        }
        if (l == Level.WARNING) {
            return "WARNING";
        }
        if (l == Level.INFO) {
            return "INFO";
        }
        return l.getName();
    }

    public static Logger setup(File dir) {
        originalOut = System.out;
        final LineFormatter formatter = new LineFormatter();
        logger = Logger.getLogger("InfdevServer");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        Handler console = new Handler() {
            public void publish(LogRecord r) {
                if (!isLoggable(r)) {
                    return;
                }
                originalOut.print(formatter.format(r));
                originalOut.flush();
            }

            public void flush() {
                originalOut.flush();
            }

            public void close() {
            }
        };
        console.setLevel(Level.INFO);
        logger.addHandler(console);

        File logs = new File(dir, "logs");
        logs.mkdirs();
        try {
            File latest = new File(logs, "latest.log");
            if (latest.exists() && latest.length() > 0) {
                String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date(latest.lastModified()));
                latest.renameTo(new File(logs, stamp + ".log"));
            }
            final Writer fw = new OutputStreamWriter(new FileOutputStream(latest, false), "UTF-8");
            Handler file = new Handler() {
                public synchronized void publish(LogRecord r) {
                    try {
                        fw.write(formatter.format(r));
                        fw.flush();
                    } catch (IOException ignored) {
                    }
                }

                public void flush() {
                }

                public synchronized void close() {
                    try {
                        fw.close();
                    } catch (IOException ignored) {
                    }
                }
            };
            file.setLevel(Level.ALL);
            logger.addHandler(file);
        } catch (IOException e) {
            originalOut.println("Could not open log file: " + e);
        }

        Handler gui = new Handler() {
            public void publish(LogRecord r) {
                if (!isLoggable(r) || listeners.isEmpty()) {
                    return;
                }
                String line = formatter.format(r);
                for (LogListener l : listeners) {
                    l.onLog(r.getLevel(), line);
                }
            }

            public void flush() {
            }

            public void close() {
            }
        };
        gui.setLevel(Level.INFO);
        logger.addHandler(gui);

        System.setOut(new PrintStream(new LoggerStream(logger, Level.INFO), true));
        System.setErr(new PrintStream(new LoggerStream(logger, Level.WARNING), true));
        return logger;
    }

    /** Passes what is written to System.out/err on to the log, line by line. */
    private static final class LoggerStream extends OutputStream {
        private final Logger log;
        private final Level level;
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

        LoggerStream(Logger log, Level level) {
            this.log = log;
            this.level = level;
        }

        @Override
        public synchronized void write(int b) {
            if (b == '\n') {
                flushLine();
            } else if (b != '\r') {
                buf.write(b);
            }
        }

        private void flushLine() {
            String s;
            try {
                s = buf.toString("UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                s = buf.toString();
            }
            buf.reset();
            if (s.length() > 0) {
                log.log(level, s);
            }
        }

        @Override
        public synchronized void flush() {
            if (buf.size() > 0) {
                flushLine();
            }
        }
    }
}
