package com.tilkicraft.ui;

import java.util.List;

/**
 * Assembles the forest TAB frame from pack glyphs. The vanilla list centres each
 * header/footer line on the screen centre, puts rows 9 px apart below the header (at
 * most 20 per column, columns 5 px apart) and draws the footer right below the rows.
 * The art lines have zero net width: signed space glyphs move the pen to each tile.
 * Rows, heads, names and ping icons are drawn after the header, on top of the frame.
 */
public final class TabFrame {
    static final int COLUMN_GAP = 5;
    private final HudLayout layout;
    private final char negative, positive;
    private final int margin, maxRows, headerLines, titleLine, onlineLine, footerLines, footerTextLine;

    public TabFrame(HudLayout layout) {
        this.layout = layout;
        negative = (char)layout.number("space.negative");
        positive = (char)layout.number("space.positive");
        margin = layout.number("tab.margin");
        maxRows = layout.number("tab.maxRows");
        headerLines = layout.number("tab.headerLines");
        titleLine = layout.number("tab.titleLine");
        onlineLine = layout.number("tab.onlineLine");
        footerLines = layout.number("tab.footerLines");
        footerTextLine = layout.number("tab.footerTextLine");
    }

    /** Rows and columns of the vanilla list for n entries. */
    public static int[] grid(int entries) {
        int rows = entries, cols = 1;
        while (rows > 20) { cols++; rows = (entries + cols - 1) / cols; }
        return new int[]{Math.max(1, rows), cols};
    }

    public static int slotWidth(int nameWidth, boolean heads) { return (heads ? 9 : 0) + nameWidth + 13; }

    /** The frame art matches this name width (TilkiTags pads TAB names to it). */
    public int nameWidth() { return layout.number("tab.nameWidth"); }

    /** Writes glyphs at absolute x positions relative to the line centre. */
    private final class Pen {
        final StringBuilder out = new StringBuilder();
        int x;
        void move(int to) {
            int delta = to - x;
            char base = delta < 0 ? negative : positive;
            int left = Math.abs(delta);
            for (int bit = 7; bit >= 0; bit--) while (left >= (1 << bit)) { out.append((char)(base + bit)); left -= 1 << bit; }
            x = to;
        }
        void put(List<HudLayout.Tile> tiles, int origin) {
            for (HudLayout.Tile tile : tiles) { move(origin + tile.x()); out.append(tile.glyph()); x += tile.width() + 1; }
        }
        String close() { move(0); return out.toString(); }
    }

    private static int total(int cols, int slot) { return cols * slot + (cols - 1) * COLUMN_GAP; }
    private int interiorLeft(int cols, int slot) { return -(total(cols, slot) / 2) - margin; }
    private static String variant(int cols, boolean heads) { return Math.min(4, cols) + "." + (heads ? 1 : 0); }

    /** Lines of the given count; the art goes on the first, texts on their lines. */
    private static String lines(int count, String art, int[] at, String... texts) {
        StringBuilder out = new StringBuilder(art);
        for (int line = 1; line < count; line++) {
            out.append('\n');
            String text = " ";
            for (int i = 0; i < at.length; i++) if (at[i] == line) text = texts[i];
            out.append(text);
        }
        return out.toString();
    }

    /** Header: posts for the current row count, top beam with corners and the fox sign, title and online count. */
    public String header(int entries, boolean heads, String title, String online) {
        int[] grid = grid(entries);
        int rows = Math.min(grid[0], maxRows), slot = slotWidth(nameWidth(), heads), cols = grid[1];
        int left = interiorLeft(cols, slot), right = left + total(cols, slot) + 2 * margin;
        Pen pen = new Pen();
        pen.put(layout.tiles("tab.post.left." + rows), left);
        pen.put(layout.tiles("tab.post.right." + rows), right);
        pen.put(layout.tiles("tab.header." + variant(cols, heads)), 0);
        return lines(headerLines, pen.close(), new int[]{titleLine, onlineLine}, title, online);
    }

    /** Footer: bottom beam with corners and the support sign, then the text line. */
    public String footer(int entries, boolean heads, String text) {
        Pen pen = new Pen();
        pen.put(layout.tiles("tab.footer." + variant(grid(entries)[1], heads)), 0);
        return lines(footerLines, pen.close(), new int[]{footerTextLine}, text);
    }
}
