package com.tilkicraft.ui;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TabFrameTest {
    private final HudLayout layout = Objects.requireNonNull(HudLayout.load(), "hud-layout.properties");

    /** Advance of an art line: tiles advance width+1, space glyphs ±2^k. */
    private int advance(String line, Map<Character, Integer> widths) {
        char negative = (char)layout.number("space.negative"), positive = (char)layout.number("space.positive");
        int total = 0;
        for (char c : line.toCharArray()) {
            if (c >= negative && c < negative + 8) total -= 1 << (c - negative);
            else if (c >= positive && c < positive + 8) total += 1 << (c - positive);
            else total += Objects.requireNonNull(widths.get(c), "unknown glyph " + Integer.toHexString(c)) + 1;
        }
        return total;
    }

    /** Widths of every TAB art tile in the layout. */
    private Map<Character, Integer> widths() throws Exception {
        Properties values = new Properties();
        try (var in = getClass().getResourceAsStream("/hud-layout.properties")) { values.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)); }
        Map<Character, Integer> widths = new HashMap<>();
        for (String key : values.stringPropertyNames()) if (key.startsWith("tab.") && values.getProperty(key).contains(",")) {
            for (HudLayout.Tile tile : layout.tiles(key)) widths.put(tile.glyph(), tile.width());
        }
        return widths;
    }

    @Test void frameLinesHaveZeroWidthForEveryPlayerCount() throws Exception {
        TabFrame frame = new TabFrame(layout);
        Map<Character, Integer> widths = widths();
        for (int entries : new int[]{1, 2, 9, 20, 21, 40, 41, 60, 61, 80}) for (boolean heads : new boolean[]{true, false}) {
            String header = frame.header(entries, heads, "T", "O"), footer = frame.footer(entries, heads, "L");
            String[] headerLines = header.split("\n", -1), footerLines = footer.split("\n", -1);
            assertEquals(0, advance(headerLines[0], widths), "header " + entries);
            assertEquals(0, advance(footerLines[0], widths), "footer " + entries);
            assertEquals(layout.number("tab.headerLines"), headerLines.length);
            assertEquals("T", headerLines[layout.number("tab.titleLine")]);
            assertEquals("O", headerLines[layout.number("tab.onlineLine")]);
            assertEquals(layout.number("tab.footerLines"), footerLines.length);
            assertEquals("L", footerLines[layout.number("tab.footerTextLine")]);
        }
    }

    @Test void postsExistForEveryRowCount() {
        for (int rows = 1; rows <= layout.number("tab.maxRows"); rows++) {
            assertFalse(layout.tiles("tab.post.left." + rows).isEmpty());
            assertFalse(layout.tiles("tab.post.right." + rows).isEmpty());
        }
    }

    @Test void gridMatchesTheVanillaColumnRule() {
        assertArrayEquals(new int[]{20, 1}, TabFrame.grid(20));
        assertArrayEquals(new int[]{11, 2}, TabFrame.grid(21));
        assertArrayEquals(new int[]{20, 4}, TabFrame.grid(80));
    }

    @Test void sidebarTitlesHaveFrameAndLogoForEveryLineCount() {
        char negative = (char)layout.number("space.negative"), positive = (char)layout.number("space.positive");
        for (int lines = 1; lines <= layout.number("sidebar.maxLines"); lines++) {
            int glyphs = 0;
            for (char c : layout.text("sidebar.title." + lines).toCharArray()) if (c < negative || c >= positive + 8) glyphs++;
            assertTrue(glyphs >= 2, "frame and logo glyphs for " + lines + " lines");
        }
        assertTrue(layout.number("sidebar.content") < layout.number("sidebar.width"));
    }
}
