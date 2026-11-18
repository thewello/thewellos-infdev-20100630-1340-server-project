package net.thewellodev.server.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/** One class loader per plugin jar; it can also see the classes of other plugins. */
public class PluginClassLoader extends URLClassLoader {

    private final SimplePluginManager manager;

    public PluginClassLoader(File jar, ClassLoader parent, SimplePluginManager manager) throws MalformedURLException {
        super(new URL[]{jar.toURI().toURL()}, parent);
        this.manager = manager;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(name, true);
    }

    Class<?> findClass(String name, boolean global) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            if (global) {
                Class<?> c = manager.findClassFromOtherPlugins(name, this);
                if (c != null) {
                    return c;
                }
            }
            throw e;
        }
    }
}
