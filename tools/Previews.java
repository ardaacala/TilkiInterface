import com.tilkicraft.ui.HudLayout;
import com.tilkicraft.ui.TabFrame;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Real-size previews of the v3 interface. They are simulations, not game screenshots:
 * the vanilla pause grid, sidebar and player list are re-implemented from the client
 * code (1.21.4–26.x), and every string is rendered glyph by glyph from the generated
 * font (art tiles, pen-move spaces, moved ASCII fonts, text shadow, bold).
 */
final class Previews {
    static final Path OUT = BuildUiV3.DERIVED.resolve("previews");
    static final int[] COLORS = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
    static HudLayout layout;

    static void all() throws IOException {
        Files.createDirectories(OUT);
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/hud-layout.properties"))) { layout = new HudLayout(in); }
        int[][] esc = {{1920, 1080, 4}, {1920, 1080, 3}, {1366, 768, 3}, {1366, 768, 2}, {1280, 720, 3}};
        for (int[] e : esc) for (boolean modern : new boolean[]{false, true}) {
            Screen s = new Screen(e[0], e[1], e[2]); pause(s, "tr_tr", modern); s.save("esc_" + (modern ? "26.3_" : "1.21.4_") + e[0] + "x" + e[1] + "_gui" + e[2]);
        }
        Screen en = new Screen(1920, 1080, 3); pause(en, "en_us", true); en.save("esc_26.3_1920x1080_gui3_en");
        int[][] tabs = {{6, 1920, 1080, 3}, {34, 1920, 1080, 3}, {75, 1920, 1080, 2}, {75, 1920, 1080, 3}, {34, 1366, 768, 2}};
        for (int[] t : tabs) { Screen s = new Screen(t[1], t[2], t[3]); tab(s, t[0], true); s.save("tab_" + t[0] + "_players_" + t[1] + "x" + t[2] + "_gui" + t[3]); }
        Screen noHeads = new Screen(1920, 1080, 3); tab(noHeads, 12, false); noHeads.save("tab_12_players_no_heads_1920x1080_gui3");
        int[][] huds = {{1920, 1080, 3}, {1920, 1080, 4}, {1366, 768, 2}};
        for (int[] h : huds) { Screen s = new Screen(h[0], h[1], h[2]); sidebar(s, 13); s.save("hud_" + h[0] + "x" + h[1] + "_gui" + h[2]); }
        Screen shortHud = new Screen(1920, 1080, 3); sidebar(shortHud, 7); shortHud.save("hud_7_lines_1920x1080_gui3");
        edges();
        buttons();
    }

    // ------------------------------------------------------------------ screen and text
    static final class Screen {
        final BufferedImage image; final Graphics2D g; final int scale, width, height;
        final Map<String, BufferedImage> tinted = new HashMap<>();
        Screen(int w, int h, int scale) {
            this.scale = scale; image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            width = (int)Math.ceil(w / (double)scale); height = (int)Math.ceil(h / (double)scale);
            g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            // A dim forest-like backdrop (not a game capture): sky, tree line, grass.
            Random random = new Random(7);
            for (int y = 0; y < h; y++) { float t = y / (float)h; g.setColor(new Color(lerp(96, 46, t), lerp(140, 84, t), lerp(170, 60, t))); g.fillRect(0, y, w, 1); }
            for (int i = 0; i < 70; i++) {
                int x = random.nextInt(w), top = h / 3 + random.nextInt(h / 4), size = 40 + random.nextInt(120);
                g.setColor(new Color(24 + random.nextInt(30), 60 + random.nextInt(50), 24 + random.nextInt(24)));
                g.fillRect(x - size / 2, top, size, size); g.setColor(new Color(70, 48, 26)); g.fillRect(x - size / 12, top + size, size / 6, h);
            }
            g.setColor(new Color(58, 110, 40)); g.fillRect(0, h * 4 / 5, w, h);
        }
        static int lerp(int a, int b, float t) { return Math.round(a + (b - a) * t); }
        void fill(double x0, double y0, double x1, double y1, int argb) {
            g.setColor(new Color(argb, true));
            int a = (int)Math.round(x0 * scale), b = (int)Math.round(y0 * scale);
            g.fillRect(a, b, (int)Math.round(x1 * scale) - a, (int)Math.round(y1 * scale) - b);
        }
        void blit(BufferedImage texture, double x, double y, double w, double h, int rgb, int alpha) {
            String key = System.identityHashCode(texture) + ":" + rgb + ":" + alpha;
            BufferedImage t = tinted.computeIfAbsent(key, k -> tint(texture, rgb, alpha));
            int a = (int)Math.round(x * scale), b = (int)Math.round(y * scale);
            g.drawImage(t, a, b, (int)Math.round((x + w) * scale) - a, (int)Math.round((y + h) * scale) - b, null);
        }
        static BufferedImage tint(BufferedImage texture, int rgb, int alpha) {
            BufferedImage out = new BufferedImage(texture.getWidth(), texture.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < texture.getHeight(); y++) for (int x = 0; x < texture.getWidth(); x++) {
                int p = texture.getRGB(x, y);
                int a = (p >>> 24) * alpha / 255, r = ((p >> 16) & 255) * ((rgb >> 16) & 255) / 255, gg = ((p >> 8) & 255) * ((rgb >> 8) & 255) / 255, b = (p & 255) * (rgb & 255) / 255;
                out.setRGB(x, y, a << 24 | r << 16 | gg << 8 | b);
            }
            return out;
        }
        /** Draws like vanilla drawString (shadow first, then the text); returns the advance. */
        int text(String s, double x, double y, int rgb, boolean shadow) {
            if (shadow) render(s, x + 1, y + 1, rgb, true, true);
            return render(s, x, y, rgb, false, true);
        }
        int width(String s) { return render(s, 0, 0, 0xffffff, false, false); }
        int render(String s, double x, double y, int base, boolean shadowPass, boolean draw) {
            int color = base, pen = 0; boolean bold = false;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '§' && i + 1 < s.length()) {
                    char code = Character.toLowerCase(s.charAt(++i));
                    if (code == 'x' && i + 12 < s.length()) {
                        StringBuilder hex = new StringBuilder();
                        for (int k = 0; k < 6; k++) hex.append(s.charAt(i + 2 + 2 * k));
                        color = Integer.parseInt(hex.toString(), 16); bold = false; i += 12; continue;
                    }
                    int index = "0123456789abcdef".indexOf(code);
                    if (index >= 0) { color = COLORS[index]; bold = false; } else if (code == 'l') bold = true; else if (code == 'r') { color = base; bold = false; }
                    continue;
                }
                int rgb = shadowPass ? (color & 0xFCFCFC) >> 2 : color;
                Integer space = BuildUiV3.spaceAdvance.get(c);
                if (space != null) { pen += space; continue; }
                if (c == ' ') { pen += 4; continue; }
                int advance;
                BuildUiV3.Glyph glyph = BuildUiV3.font.get(c);
                BuildUiV3.AsciiFont moved = null;
                for (BuildUiV3.AsciiFont f : BuildUiV3.asciiFonts) if (c > f.base() + 32 && c < f.base() + 127) moved = f;
                if (glyph != null) {
                    BufferedImage t = glyph.texture();
                    double scaleY = glyph.height() / (double)t.getHeight();
                    if (draw) for (int k = 0; k <= (bold ? 1 : 0); k++) blit(t, x + pen + k, y + 7 - glyph.ascent(), t.getWidth() * scaleY, glyph.height(), rgb, 255);
                    advance = (int)(0.5 + measured(t) * scaleY) + 1;
                } else {
                    char ch = moved != null ? (char)(c - moved.base()) : BuildShared.base(c);
                    double top = moved != null ? y + 7 - moved.ascent() : y;
                    int tint = moved != null ? multiply(rgb, moved.tint()) : rgb;
                    BufferedImage cell = BuildShared.atlas.getSubimage(ch % 16 * 16, ch / 16 * 16, 16, 16);
                    if (draw) for (int k = 0; k <= (bold ? 1 : 0); k++) {
                        if (c == 'ı') blit(cell.getSubimage(0, 4, 16, 12), x + pen + k, top + 2, 8, 6, tint, 255);
                        else blit(cell, x + pen + k, top, 8, 8, tint, 255);
                        if (c == 'ö' || c == 'ü' || c == 'Ö' || c == 'Ü') { dot(x + pen + k + 1, top - 1, tint); dot(x + pen + k + 4, top - 1, tint); }
                        if (c == 'İ') dot(x + pen + k + 1, top - 2, tint);
                        if (c == 'ş' || c == 'Ş' || c == 'ç' || c == 'Ç') dot(x + pen + k + 2, top + 8, tint);
                    }
                    advance = BuildShared.advance(ch);
                }
                pen += advance + (bold ? 1 : 0);
            }
            return pen;
        }
        void dot(double x, double y, int rgb) { fill(x, y, x + 1, y + 1, 0xff000000 | rgb); }
        static int multiply(int a, int b) { return (((a >> 16) & 255) * ((b >> 16) & 255) / 255) << 16 | (((a >> 8) & 255) * ((b >> 8) & 255) / 255) << 8 | (a & 255) * (b & 255) / 255; }
        void save(String name) throws IOException { ImageIO.write(image, "png", OUT.resolve(name + ".png").toFile()); }
    }
    static int measured(BufferedImage t) {
        for (int x = t.getWidth() - 1; x >= 0; x--) for (int y = 0; y < t.getHeight(); y++) if ((t.getRGB(x, y) >>> 24) != 0) return x + 1;
        return 0;
    }

    // ------------------------------------------------------------------ pause menu
    static BufferedImage sprite(String name) throws IOException { return ImageIO.read(BuildUiV3.ROOT.resolve("assets/minecraft/textures/gui/sprites/widget/" + name + ".png").toFile()); }
    /** Nine-slice button (border 7/3): the 186 px middle is tiled like vanilla. */
    static void button(Screen s, int x, int y, int w, String sprite, String label) throws IOException {
        BufferedImage t = sprite(sprite);
        s.blit(t.getSubimage(0, 0, 7, 20), x, y, 7, 20, 0xffffff, 255);
        for (int at = 7; at < w - 7; at += 186) { int n = Math.min(186, w - 7 - at); s.blit(t.getSubimage(7, 0, n, 20), x + at, y, n, 20, 0xffffff, 255); }
        s.blit(t.getSubimage(193, 0, 7, 20), x + w - 7, y, 7, 20, 0xffffff, 255);
        int width = s.width(label);
        s.text(label, x + w / 2 - width / 2, y + 6, 0xffffff, true);
    }
    /**
     * PauseScreen.createPauseMenu: a 2-column grid, cell padding 4 (bottom 0), the first
     * button with 50 px top padding, aligned at 25% of the free height. 1.21.4 has text
     * buttons in row 3; 26.3 has four 20 px icon buttons there and World Options in row 4.
     */
    static void pause(Screen s, String language, boolean modern) throws IOException {
        s.fill(0, 0, s.width, s.height, 0x60101010);
        String label = layout.text("esc.label." + language);
        boolean tr = language.equals("tr_tr");
        int cx = s.width / 2, top = (int)((s.height - 166) * 0.25f) + 50;
        if (s.width(label) != 0) throw new IllegalStateException("ESC label must have zero width, got " + s.width(label));
        button(s, cx - 102, top, 204, "button", label);
        button(s, cx - 102, top + 24, 98, "button", tr ? "Gelişmeler" : "Advancements");
        button(s, cx + 4, top + 24, 98, "button", tr ? "İstatistikler" : "Statistics");
        if (modern) {
            String[] icons = {"pause_menu/bug", "pause_menu/social_interactions", "friends/friends", "pause_menu/player_reporting"};
            String sprites = System.getProperty("vanilla.sprites");
            for (int i = 0; i < 4; i++) {
                int x = cx - 46 + i * 24;
                button(s, x, top + 48, 20, "button", "");
                Path icon = sprites == null ? null : Path.of(sprites, icons[i] + ".png");
                if (icon != null && Files.exists(icon)) { BufferedImage t = ImageIO.read(icon.toFile()); s.blit(t, x + 2.5, top + 50.5, 15, 15, 0xffffff, 255); }
            }
            button(s, cx - 102, top + 72, 98, "button_highlighted", tr ? "Ayarlar" : "Options...");
            button(s, cx + 4, top + 72, 98, "button", tr ? "Dünya Ayarları" : "World Options...");
        } else {
            button(s, cx - 102, top + 48, 98, "button", tr ? "Geri Bildirim" : "Send Feedback");
            button(s, cx + 4, top + 48, 98, "button", tr ? "Hata Bildir" : "Report Bugs");
            button(s, cx - 102, top + 72, 98, "button_highlighted", tr ? "Ayarlar" : "Options...");
            button(s, cx + 4, top + 72, 98, "button", tr ? "Oyuncu Bildir" : "Player Reporting");
        }
        button(s, cx - 102, top + 96, 204, "button", tr ? "Sunucudan Ayrıl" : "Disconnect");
        caption(s, "Simulation of the " + (modern ? "26.3" : "1.21.4") + " pause layout · " + s.image.getWidth() + "×" + s.image.getHeight() + " GUI " + s.scale + " (" + s.width + "×" + s.height + ") · hover on " + (tr ? "Ayarlar" : "Options"));
    }
    static void caption(Screen s, String text) {
        s.g.setColor(new Color(0, 0, 0, 170)); s.g.fillRect(0, s.image.getHeight() - 26, s.image.getWidth(), 26);
        s.g.setColor(Color.WHITE); s.g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15)); s.g.drawString(text, 8, s.image.getHeight() - 8);
    }

    // ------------------------------------------------------------------ TAB
    static final String[] NAMES = {"ArdaAlaca", "TilkiKral", "Mavi_Kus", "Elif2010", "Kartal", "Duman", "Gokce", "Pixel_Usta", "Yaprak", "Kerem_TR", "SuperSu", "Bulut"};
    static void tab(Screen s, int entries, boolean heads) {
        TabFrame frame = new TabFrame(layout);
        int nameWidth = layout.number("tab.nameWidth");
        int slot = TabFrame.slotWidth(nameWidth, heads);
        String header = frame.header(entries, heads, "§x§F§F§E§9§C§2§lSKYBLOCK", "§fÇevrimiçi: §a" + entries + " §8/ §f100");
        String footer = frame.footer(entries, heads, "§6/discord §8• §6/kurallar");
        int[] grid = TabFrame.grid(entries);
        int rows = grid[0], cols = grid[1];
        // PlayerTabOverlay.render
        int slotWidth = Math.min(cols * slot, s.width - 50) / cols, xxo = s.width / 2 - (slotWidth * cols + (cols - 1) * 5) / 2, yy = 10;
        int maxWidth = slotWidth * cols + (cols - 1) * 5;
        String[] headerLines = header.split("\n", -1), footerLines = footer.split("\n", -1);
        for (String line : headerLines) maxWidth = Math.max(maxWidth, s.width(line));
        for (String line : footerLines) maxWidth = Math.max(maxWidth, s.width(line));
        s.fill(s.width / 2 - maxWidth / 2 - 1, yy - 1, s.width / 2 + maxWidth / 2 + 1, yy + headerLines.length * 9, 0x80000000);
        for (String line : headerLines) { s.text(line, s.width / 2 - s.width(line) / 2, yy, 0xffffff, true); yy += 9; }
        yy++;
        s.fill(s.width / 2 - maxWidth / 2 - 1, yy - 1, s.width / 2 + maxWidth / 2 + 1, yy + rows * 9, 0x80000000);
        for (int i = 0; i < entries; i++) {
            int col = i / rows, row = i % rows, xo = xxo + col * slotWidth + col * 5, yo = yy + row * 9;
            s.fill(xo, yo, xo + slotWidth, yo + 8, 0x20FFFFFF);
            int x = xo;
            if (heads) { s.fill(xo, yo, xo + 8, yo + 8, 0xff000000 | (0x406080 + i * 0x0a1307) & 0xffffff); x += 9; }
            String name = NAMES[i % NAMES.length] + (i >= NAMES.length ? String.valueOf(i) : "");
            String rank = i == 0 ? "§6§lTİLKİ " : i % 5 == 1 ? "§bVIP " : i % 7 == 2 ? "§dMEGA " : "";
            s.text(rank + "§f" + name, x, yo, 0xffffff, true);
            String level = "§7" + (3 + i * 7 % 60);
            s.text(level, x + nameWidth - s.width(level), yo, 0xffffff, true);
            try {
                BufferedImage ping = ImageIO.read(BuildUiV3.ROOT.resolve("assets/minecraft/textures/gui/sprites/icon/ping_" + (5 - i % 5) + ".png").toFile());
                s.blit(ping, xo + slotWidth - 11, yo, 10, 8, 0xffffff, 255);
            } catch (IOException ignored) { }
        }
        yy += rows * 9 + 1;
        s.fill(s.width / 2 - maxWidth / 2 - 1, yy - 1, s.width / 2 + maxWidth / 2 + 1, yy + footerLines.length * 9, 0x80000000);
        for (String line : footerLines) { s.text(line, s.width / 2 - s.width(line) / 2, yy, 0xffffff, true); yy += 9; }
        caption(s, "Simulated player list · " + entries + " players (" + cols + "×" + rows + ")" + (heads ? "" : ", no heads") + " · " + s.image.getWidth() + "×" + s.image.getHeight() + " GUI " + s.scale
            + (yy > s.height ? " · taller than the screen (" + yy + " > " + s.height + ")" : ""));
    }

    // ------------------------------------------------------------------ sidebar
    static void sidebar(Screen s, int count) {
        String[] lines = {"§7" + "13/10/26 21:40", " ", "ON §fÇevrimiçi §7» §e34/100", "PI §fPing §7» §e42 ms", " ", "PL §fOyuncu §7» §fArdaAlaca",
            "RA §fRütbe §7» §6§lTİLKİ", "MO §fBakiye §7» §e12.4K", "IS §fAda Seviyesi §7» §e57", " ", "TI §fSüre §7» §e48sa 12dk", " ", "FO §6TilkiCraft §8• §fSkyblock"};
        String[] icons = {"ON", "online", "PI", "ping", "PL", "player", "RA", "rank", "MO", "money", "IS", "island", "TI", "time", "FO", "fox"};
        List<String> shown = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String line = lines[i];
            for (int k = 0; k < icons.length; k += 2) line = line.replace(icons[k] + " ", "§f" + layout.icon(icons[k + 1]) + " ");
            shown.add(line.isBlank() ? "§f" + layout.text("sidebar.separator") : line);
        }
        String title = "§f" + layout.text("sidebar.title." + count);
        int titleWidth = s.width(title), biggest = titleWidth;
        for (String line : shown) biggest = Math.max(biggest, s.width(line));
        // Hud.displayScoreboardSidebar (numbers hidden)
        int n = shown.size(), bottom = s.height / 2 + n * 9 / 3, left = s.width - biggest - 3, right = s.width - 1, firstTop = bottom - n * 9;
        s.fill(left - 2, firstTop - 9 - 1, right, firstTop - 1, 0x66000000);
        s.fill(left - 2, firstTop - 1, right, bottom, 0x4C000000);
        s.text(title, left + biggest / 2 - titleWidth / 2, firstTop - 9, 0xffffff, false);
        for (int i = 0; i < n; i++) s.text(shown.get(i), left, firstTop + i * 9, 0xffffff, false);
        caption(s, "Simulated sidebar · " + n + " lines · panel " + biggest + " px (title " + titleWidth + ") · " + s.image.getWidth() + "×" + s.image.getHeight() + " GUI " + s.scale);
    }

    // ------------------------------------------------------------------ strips and buttons
    static void edges() throws IOException {
        String[] names = {"edge_left", "edge_right", "edge_top", "edge_bottom"};
        boolean[] vertical = {true, true, false, false};
        int[] probe = {1700, 1140, 100, 100};
        double[] thickness = {5.44, 5.44, 6.0, 6.0};
        int zoom = 3;
        for (int i = 0; i < 4; i++) {
            BuildUiV3.Strip strip = new BuildUiV3.Strip(names[i], BuildUiV3.load(names[i] + ".png"), vertical[i], thickness[i], probe[i]);
            double period = strip.period / (double)BuildUiV3.TX;
            // Row 1: four raw repeats (seam check). Rows 2–5: leaf-free fills of several lengths.
            double[] lengths = {4 * period, 4 * period, 37, 90, 151};
            int across = strip.acrossTexels(), gap = 8;
            int along = (int)Math.round(4 * period * BuildUiV3.TX);
            BufferedImage sheet = vertical[i] ? new BufferedImage((across + gap) * lengths.length * zoom, along * zoom + 40, BufferedImage.TYPE_INT_RGB)
                                              : new BufferedImage(along * zoom + 40, (across + gap) * lengths.length * zoom, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = sheet.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            for (int y = 0; y < sheet.getHeight(); y += 8) for (int x = 0; x < sheet.getWidth(); x += 8) { g.setColor(((x + y) / 8) % 2 == 0 ? new Color(70, 70, 76) : new Color(56, 56, 62)); g.fillRect(x, y, 8, 8); }
            for (int r = 0; r < lengths.length; r++) {
                BufferedImage img;
                if (r == 0) {
                    int n = along; img = vertical[i] ? new BufferedImage(across, n, BufferedImage.TYPE_INT_ARGB) : new BufferedImage(n, across, BufferedImage.TYPE_INT_ARGB);
                    for (int t = 0; t < n; t++) for (int j = 0; j < across; j++) { int p = strip.pixel(t, j); if (vertical[i]) img.setRGB(j, t, p); else img.setRGB(t, j, p); }
                } else img = strip.fill(lengths[r]);
                int offset = r * (across + gap) * zoom;
                if (vertical[i]) g.drawImage(img, offset, 0, img.getWidth() * zoom, img.getHeight() * zoom, null);
                else g.drawImage(img, 0, offset, img.getWidth() * zoom, img.getHeight() * zoom, null);
                if (r == 0) { // period boundaries in red
                    g.setColor(Color.RED);
                    for (int k = 1; k < 4; k++) { int at = (int)Math.round(k * strip.period * zoom); if (vertical[i]) g.fillRect(offset, at, across * zoom, 1); else g.fillRect(at, offset, 1, across * zoom); }
                }
            }
            ImageIO.write(sheet, "png", OUT.resolve("tiling_" + names[i] + ".png").toFile());
        }
    }
    static void buttons() throws IOException {
        int zoom = 4;
        BufferedImage sheet = new BufferedImage(204 * zoom + 20, 3 * 28 * zoom, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(40, 50, 40)); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        String[] names = {"button", "button_highlighted", "button_disabled"};
        for (int i = 0; i < 3; i++) g.drawImage(sprite(names[i]), 10, 10 + i * 28 * zoom, 200 * zoom, 20 * zoom, null);
        ImageIO.write(sheet, "png", OUT.resolve("buttons_x4.png").toFile());
    }
}
