package net.thewellodev.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/** server.properties */
public class ServerConfig {

    private final File file;
    private final Properties props = new Properties();
    private boolean dirty;

    public ServerConfig(File file) {
        this.file = file;
        if (file.exists()) {
            try {
                InputStream in = new FileInputStream(file);
                try {
                    props.load(in);
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                System.err.println("Could not read server.properties: " + e);
            }
        } else {
            dirty = true;
        }
    }

    public String getString(String key, String def) {
        String v = props.getProperty(key);
        if (v == null) {
            props.setProperty(key, def);
            dirty = true;
            return def;
        }
        return v.trim();
    }

    public int getInt(String key, int def) {
        try {
            return Integer.parseInt(getString(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public boolean getBoolean(String key, boolean def) {
        return Boolean.parseBoolean(getString(key, String.valueOf(def)));
    }

    public void set(String key, String value) {
        props.setProperty(key, value);
        dirty = true;
    }

    public void saveIfDirty() {
        if (!dirty) {
            return;
        }
        try {
            OutputStream o = new FileOutputStream(file);
            try {
                props.store(o, "TheWelloDev server settings (inf-20100630-1340)");
            } finally {
                o.close();
            }
            dirty = false;
        } catch (IOException e) {
            System.err.println("server.properties yazilamadi: " + e);
        }
    }
}
