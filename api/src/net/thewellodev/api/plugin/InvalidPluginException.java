package net.thewellodev.api.plugin;

/** Thrown when a plugin cannot be loaded. */
public class InvalidPluginException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidPluginException(String message) {
        super(message);
    }

    public InvalidPluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
