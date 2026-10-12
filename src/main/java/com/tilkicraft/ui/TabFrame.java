package com.tilkicraft.ui;

/**
 * Assembles the forest TAB frame from pack pieces. The vanilla list centres
 * zero-width header/footer lines on the screen centre, puts rows 9 px apart
 * below the header (at most 20 per column, columns 5 px apart) and draws the
 * footer right below the rows, so every piece is placed relative to the centre
 * with signed space glyphs and a per-row ascent. Rows, heads, names and ping
 * icons are drawn after the header, on top of the frame.
 */
final class TabFrame {
    static final int HEADER_ART_LINES = 6, COLUMN_GAP = 5, ROW = 9;
    private final HudLayout layout;
    private final char negative, positive;
    private final int margin, maxRows;

    TabFrame(HudLayout layout) {
        this.layout = layout;
        negative = (char)layout.number("tab.space.negative");
        positive = (char)layout.number("tab.space.positive");
        margin = layout.number("tab.interiorMargin");
        maxRows = layout.number("tab.maxRows");
    }

    /** Column layout of the vanilla list for n entries. */
    static int[] grid(int entries) {
        int rows = entries, cols = 1;
        while (rows > 20) { cols++; rows = (entries + cols - 1) / cols; }
        return new int[]{Math.max(1, rows), cols};
    }

    static int slotWidth(int nameWidth, boolean heads) { return (heads ? 9 : 0) + nameWidth + 13; }

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
        void put(HudLayout.Piece piece, int at) { move(at); out.append(piece.glyph()); x += piece.width() + 1; }
        void tile(HudLayout.Piece piece, int from, int to) {
            for (int at = from; at < to; at += piece.width()) put(piece, Math.min(at, to - piece.width()));
        }
        String close() { move(0); return out.toString(); }
    }

    private int total(int cols, int slot) { return cols * slot + (cols - 1) * COLUMN_GAP; }

    /** Header: art line, blank lines over the art, then the server title and the online count on the sign. */
    String header(int entries, int slot, String title, String online) {
        int[] grid = grid(entries);
        int rows = Math.min(grid[0], maxRows), total = total(grid[1], slot);
        int left = -(total / 2) - margin, right = -(total / 2) + total + margin;
        Pen pen = new Pen();
        pen.tile(layout.piece("canopy"), left - 20, right + 18);
        pen.tile(layout.piece("beam_tile"), left, right);
        pen.put(layout.piece("beam_left"), left - 26);
        pen.put(layout.piece("beam_right"), right);
        HudLayout.Piece crest = layout.piece("crest");
        pen.put(crest, -crest.width() / 2);
        pen.tile(layout.piece("head_wood"), left, right);
        pen.put(layout.piece("head_post_left"), left - 20);
        pen.put(layout.piece("head_post_right"), right);
        HudLayout.Piece sign = layout.piece("sign");
        pen.put(sign, -sign.width() / 2);
        for (int r = 0; r < rows; r++) {
            pen.tile(layout.piece("row_wood." + r), left, right);
            pen.put(layout.piece("row_post_left." + r), left - 20);
            pen.put(layout.piece("row_post_right." + r), right);
        }
        return pen.close() + "\n".repeat(HEADER_ART_LINES) + title + "\n" + online;
    }

    /** Footer: art line, then the welcome and command lines on the bottom sign. */
    String footer(int entries, int slot, String welcome, String links) {
        int total = total(grid(entries)[1], slot);
        int left = -(total / 2) - margin, right = -(total / 2) + total + margin;
        Pen pen = new Pen();
        pen.tile(layout.piece("foot_wood"), left, right);
        pen.put(layout.piece("foot_post_left"), left - 20);
        pen.put(layout.piece("foot_post_right"), right);
        HudLayout.Piece sign = layout.piece("foot_sign");
        pen.put(sign, -sign.width() / 2);
        pen.tile(layout.piece("foot_tile"), left, right);
        pen.put(layout.piece("foot_left"), left - 26);
        pen.put(layout.piece("foot_right"), right);
        return pen.close() + "\n" + welcome + "\n" + links;
    }
}
