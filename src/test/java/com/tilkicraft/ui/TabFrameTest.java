package com.tilkicraft.ui;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TabFrameTest {
    private final HudLayout layout = Objects.requireNonNull(HudLayout.load(), "hud-layout.properties");

    /** Advance of the first (art) line: pieces advance width+1, space glyphs ±2^k. */
    private int advance(String line, Map<Character, Integer> widths) {
        char negative = (char)layout.number("tab.space.negative"), positive = (char)layout.number("tab.space.positive");
        int total = 0;
        for (char c : line.toCharArray()) {
            if (c >= negative && c < negative + 8) total -= 1 << (c - negative);
            else if (c >= positive && c < positive + 8) total += 1 << (c - positive);
            else total += Objects.requireNonNull(widths.get(c), "unknown glyph " + Integer.toHexString(c)) + 1;
        }
        return total;
    }

    private Map<Character, Integer> widths() throws Exception {
        Properties values = new Properties();
        try (var in = getClass().getResourceAsStream("/hud-layout.properties")) { values.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)); }
        Map<Character, Integer> widths = new HashMap<>();
        for (String key : values.stringPropertyNames()) if (key.startsWith("piece.")) {
            String[] parts = values.getProperty(key).split(",");
            widths.put((char)Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        return widths;
    }

    @Test void frameLinesHaveZeroWidthForEveryPlayerCount() throws Exception {
        TabFrame frame = new TabFrame(layout);
        Map<Character, Integer> widths = widths();
        for (int entries : new int[]{1, 2, 9, 20, 21, 40, 41, 60, 61, 80}) for (boolean heads : new boolean[]{true, false}) {
            int slot = TabFrame.slotWidth(140, heads);
            String header = frame.header(entries, slot, "T", "O"), footer = frame.footer(entries, slot, "W", "L");
            assertEquals(0, advance(header.split("\n", -1)[0], widths), "header " + entries);
            assertEquals(0, advance(footer.split("\n", -1)[0], widths), "footer " + entries);
            // Art line, five blank lines, then the title and online lines on the sign.
            assertEquals(TabFrame.HEADER_ART_LINES + 2, header.split("\n", -1).length);
            assertEquals(layout.number("tab.headerLines"), header.split("\n", -1).length);
        }
    }

    @Test void gridMatchesTheVanillaColumnRule() {
        assertArrayEquals(new int[]{20, 1}, TabFrame.grid(20));
        assertArrayEquals(new int[]{11, 2}, TabFrame.grid(21));
        assertArrayEquals(new int[]{20, 4}, TabFrame.grid(80));
    }

    @Test void sidebarTitleIsTheZeroWidthLogo() {
        String title = layout.text("sidebar.title");
        assertEquals(3, title.length());
    }
}
