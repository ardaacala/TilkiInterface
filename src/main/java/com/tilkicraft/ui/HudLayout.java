package com.tilkicraft.ui;

import java.io.*;
import java.util.*;

/** Glyph characters and sizes written by tools/BuildHudTab.java (hud-layout.properties). */
final class HudLayout {
    record Piece(char glyph, int width) {}
    private final Properties values = new Properties();

    HudLayout(InputStream in) throws IOException { values.load(new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)); }

    static HudLayout load() {
        try (InputStream in = HudLayout.class.getResourceAsStream("/hud-layout.properties")) {
            return in == null ? null : new HudLayout(in);
        } catch (IOException ex) { return null; }
    }

    int number(String key) { return Integer.parseInt(Objects.requireNonNull(values.getProperty(key), key)); }
    String text(String key) { return Objects.requireNonNull(values.getProperty(key), key); }
    char icon(String name) { return (char)number("icon." + name); }
    Piece piece(String key) {
        String[] parts = Objects.requireNonNull(values.getProperty("piece." + key), key).split(",");
        return new Piece((char)Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
