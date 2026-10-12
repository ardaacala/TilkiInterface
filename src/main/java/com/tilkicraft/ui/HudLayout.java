package com.tilkicraft.ui;

import java.io.*;
import java.util.*;

/** Glyph characters and positions written by tools/BuildUiV3.java (hud-layout.properties). */
public final class HudLayout {
    /** One art glyph: its character, x position (GUI px) and width. */
    public record Tile(char glyph, int x, int width) {}
    private final Properties values = new Properties();

    public HudLayout(InputStream in) throws IOException { values.load(new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)); }

    static HudLayout load() {
        try (InputStream in = HudLayout.class.getResourceAsStream("/hud-layout.properties")) {
            return in == null ? null : new HudLayout(in);
        } catch (IOException ex) { return null; }
    }

    public boolean has(String key) { return values.containsKey(key); }
    public int number(String key) { return Integer.parseInt(Objects.requireNonNull(values.getProperty(key), key)); }
    public String text(String key) { return Objects.requireNonNull(values.getProperty(key), key); }
    public char icon(String name) { return (char)number("icon." + name); }
    /** Tiles stored as "glyph,x,width;..." */
    public List<Tile> tiles(String key) {
        List<Tile> tiles = new ArrayList<>();
        for (String part : text(key).split(";")) {
            if (part.isEmpty()) continue;
            String[] p = part.split(",");
            tiles.add(new Tile((char)Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])));
        }
        return tiles;
    }
}
