package net.thewellodev.api.util;

/** Chat colours. The Infdev font uses '&amp;' followed by a hex digit (e.g. "&amp;e" is yellow). */
public enum ChatColor {
    BLACK('0'), DARK_BLUE('1'), DARK_GREEN('2'), DARK_AQUA('3'), DARK_RED('4'), DARK_PURPLE('5'),
    GOLD('6'), GRAY('7'), DARK_GRAY('8'), BLUE('9'), GREEN('a'), AQUA('b'), RED('c'),
    LIGHT_PURPLE('d'), YELLOW('e'), WHITE('f');

    public static final char COLOR_CHAR = '&';
    private final char code;

    ChatColor(char code) {
        this.code = code;
    }

    public char getChar() {
        return code;
    }

    @Override
    public String toString() {
        return new String(new char[]{COLOR_CHAR, code});
    }

    /** Metindeki renk kodlarini siler. */
    public static String stripColor(String s) {
        return s == null ? null : s.replaceAll("&[0-9a-fA-F]", "");
    }
}
