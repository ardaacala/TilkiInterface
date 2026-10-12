import com.google.gson.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import javax.imageio.ImageIO;

/**
 * Builds the wooden pause menu around the vanilla 1.21 pause buttons.
 *
 * The title (menu.game) sits at a fixed y=40 while the button grid is aligned
 * to 25% of the screen height, so a title frame drifts as the GUI scale changes.
 * The frame is therefore the label of "Back to Game": a label whose advances sum
 * to zero is drawn unclipped at the button centre, so the frame always moves
 * with the buttons. Only button rectangles are transparent; every native button
 * keeps its own sprite, hover state, text and action.
 */
class BuildEscMenu {
    static final Path ROOT = Path.of("resourcepack");
    static final int S = 3, W = 264, COLS = 4, TILE = W / COLS;
    static final int[] ROWS = {84, 76, 76};
    static final int H = 236, TOP = 68, CX = W / 2;
    // Pause grid (1.21): 204/98 px buttons, 20 px tall, 4 px gaps, five rows.
    static final int[][] BUTTONS = {
        {30, TOP, 204, 20},
        {30, TOP + 24, 98, 20}, {136, TOP + 24, 98, 20},
        {30, TOP + 48, 98, 20}, {136, TOP + 48, 98, 20},
        {30, TOP + 72, 98, 20}, {136, TOP + 72, 98, 20},
        {30, TOP + 96, 204, 20}};
    static final char TILE_BASE = '', TEXT = '', BACK_ONE = '', HALF = '', FULL = '', TEXT_LEFT = '', TEXT_RIGHT = '';
    static final String LABEL = "Oyuna Dön";
    static BufferedImage atlas;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: BuildEscMenu <esc-menu-frame-source.png> <ascii.png>");
        BufferedImage source = ImageIO.read(Path.of(args[0]).toFile());
        atlas = ImageIO.read(Path.of(args[1]).toFile());
        if (source == null || atlas == null || atlas.getWidth() != 256) throw new IllegalArgumentException("Invalid frame or ASCII atlas");
        BufferedImage frame = frame(source);
        Path textures = ROOT.resolve("assets/tilkiinterface/textures/font/esc_menu");
        Files.createDirectories(textures);
        try (var old = Files.list(textures)) { for (Path file : old.toList()) Files.delete(file); }
        Files.deleteIfExists(ROOT.resolve("assets/tilkiinterface/textures/font/esc_frame.png"));
        Files.deleteIfExists(ROOT.resolve("assets/tilkiinterface/textures/font/esc_frame_bottom.png"));

        JsonArray added = new JsonArray();
        StringBuilder label = new StringBuilder().append(HALF);
        int y = 0;
        for (int row = 0; row < ROWS.length; row++) {
            // Glyph top = baseline + 7 - ascent; the label baseline is button top + 6.
            int ascent = 6 + 7 - (y - TOP);
            if (ascent > ROWS[row]) throw new IllegalStateException("Row "+row+" ascent exceeds its height");
            for (int col = 0; col < COLS; col++) {
                BufferedImage tile = frame.getSubimage(col * TILE * S, y * S, TILE * S, ROWS[row] * S);
                BufferedImage copy = new BufferedImage(tile.getWidth(), tile.getHeight(), BufferedImage.TYPE_INT_ARGB);
                copy.getGraphics().drawImage(tile, 0, 0, null);
                // Minecraft measures advance from the last non-transparent column.
                if ((copy.getRGB(copy.getWidth() - 1, 0) >>> 24) == 0) copy.setRGB(copy.getWidth() - 1, 0, 0x01ffffff);
                String name = "r"+row+"c"+col+".png";
                ImageIO.write(copy, "png", textures.resolve(name).toFile());
                char glyph = (char)(TILE_BASE + row * COLS + col);
                added.add(bitmap("tilkiinterface:font/esc_menu/"+name, ROWS[row], ascent, glyph));
                label.append(glyph).append(BACK_ONE);
            }
            label.append(row == ROWS.length - 1 ? HALF : FULL);
            y += ROWS[row];
        }
        BufferedImage text = text(LABEL);
        ImageIO.write(text, "png", textures.resolve("return_text.png").toFile());
        added.add(bitmap("tilkiinterface:font/esc_menu/return_text.png", 8, 7, TEXT));
        int textAdvance = (int)(0.5 + text.getWidth() / 2.0) + 1, left = textAdvance / 2;
        label.append(TEXT_LEFT).append(TEXT).append(TEXT_RIGHT);
        JsonObject spaces = new JsonObject(); spaces.addProperty("type", "space");
        JsonObject advances = new JsonObject();
        advances.addProperty(String.valueOf(BACK_ONE), -1);
        advances.addProperty(String.valueOf(HALF), -W / 2);
        advances.addProperty(String.valueOf(FULL), -W);
        advances.addProperty(String.valueOf(TEXT_LEFT), -left);
        advances.addProperty(String.valueOf(TEXT_RIGHT), -(textAdvance - left));
        spaces.add("advances", advances);
        added.add(spaces);

        Path definition = ROOT.resolve("assets/tilkiinterface/font/ui.json");
        JsonObject font = JsonParser.parseString(Files.readString(definition)).getAsJsonObject();
        JsonArray providers = new JsonArray();
        for (JsonElement entry : font.getAsJsonArray("providers")) {
            JsonObject provider = entry.getAsJsonObject();
            if (provider.has("file") && provider.get("file").getAsString().matches("tilkiinterface:font/esc_(frame|menu/).*")) continue;
            if (provider.has("advances") && provider.getAsJsonObject("advances").has(String.valueOf(BACK_ONE))) continue;
            providers.add(entry);
        }
        providers.addAll(added);
        font.add("providers", providers);
        Files.writeString(definition, font.toString());

        Map<String, String> turkish = new LinkedHashMap<>();
        turkish.put("gui.advancements", "Gelişmeler");
        turkish.put("gui.stats", "İstatistikler");
        turkish.put("menu.sendFeedback", "Geri Bildirim");
        turkish.put("menu.reportBugs", "Hata Bildir");
        turkish.put("menu.options", "Ayarlar");
        turkish.put("menu.playerReporting", "Oyuncu Bildir");
        turkish.put("menu.disconnect", "Sunucudan Ayrıl");
        for (String lang : new String[]{"tr_tr", "en_us", "en_gb"}) {
            Path path = ROOT.resolve("assets/minecraft/lang/"+lang+".json");
            JsonObject translation = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            // The fixed-position title would float away from the buttons; hide it.
            translation.addProperty("menu.game", "");
            translation.addProperty("menu.returnToGame", label.toString());
            if (lang.equals("tr_tr")) turkish.forEach(translation::addProperty);
            Files.writeString(path, translation.toString());
        }
        buttons();
        writeZip();
        preview(frame);
    }

    static JsonObject bitmap(String file, int height, int ascent, char glyph) {
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "bitmap");
        provider.addProperty("file", file);
        provider.addProperty("height", height);
        provider.addProperty("ascent", ascent);
        JsonArray chars = new JsonArray(); chars.add(String.valueOf(glyph)); provider.add("chars", chars);
        return provider;
    }

    // ---------------------------------------------------------------- frame art
    static BufferedImage frame(BufferedImage source) {
        BufferedImage canvas = new BufferedImage(W * S, H * S, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        Random random = new Random(26);
        board(g, random, 18, 58, W - 36, 132);
        post(g, random, 8, 54, 12, 150);
        post(g, random, W - 20, 54, 12, 150);
        beam(g, random, 2, 52, W - 4, 12);
        // Artwork from the transparent concept sheet (1254 px square).
        // Foxes and the logo sit on a leaf bed in the sheet; drop the leaves so
        // the crops do not show rectangular edges.
        place(g, leafless(source, 330, 10, 520, 285), 0, 0, 520, 285, 0.13, CX - 37, 0);        // large fox
        place(g, leafless(source, 70, 225, 230, 200), 0, 0, 230, 200, 0.15, 6, 22);             // left fox
        place(g, leafless(source, 950, 200, 235, 215), 0, 0, 235, 215, 0.15, W - 41, 20);       // right fox
        place(g, leafless(source, 280, 285, 690, 155), 0, 0, 690, 155, 0.18, CX - 64, 32);      // TilkiCraft logo
        place(g, source, 40, 395, 180, 85, 0.15, 0, 50);                 // left beam cap
        place(g, source, 1030, 395, 185, 85, 0.15, W - 28, 50);          // right beam cap
        place(g, source, 45, 470, 95, 145, 0.17, 0, 64);                 // left lantern
        place(g, source, 1115, 470, 100, 145, 0.17, W - 17, 64);         // right lantern
        place(g, source, 40, 615, 140, 115, 0.18, 2, 98);                // left leaves
        place(g, source, 1060, 600, 150, 110, 0.18, W - 29, 96);         // right leaves
        place(g, source, 1120, 740, 100, 100, 0.18, W - 20, 128);        // right leaves, lower
        place(g, source, 35, 860, 135, 125, 0.17, 0, 158);               // left moss block
        place(g, source, 1085, 875, 135, 115, 0.17, W - 23, 160);        // right moss block
        place(g, source, 40, 985, 1200, 230, 0.215, 1, 186);             // welcome sign
        g.dispose();
        // Native buttons render in these holes; nothing may cover them.
        for (int[] button : BUTTONS)
            for (int yy = button[1] * S; yy < (button[1] + button[3]) * S; yy++)
                for (int xx = button[0] * S; xx < (button[0] + button[2]) * S; xx++) canvas.setRGB(xx, yy, 0);
        return canvas;
    }

    static BufferedImage leafless(BufferedImage source, int x, int y, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int yy = 0; yy < height; yy++) for (int xx = 0; xx < width; xx++) {
            int pixel = source.getRGB(x + xx, y + yy), r = (pixel >> 16) & 255, gr = (pixel >> 8) & 255;
            if (gr <= r + 4 && (pixel >>> 24) > 0) result.setRGB(xx, yy, pixel);
        }
        // Dark outline pixels only stay next to the coloured artwork they outline.
        BufferedImage outlined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int yy = 0; yy < height; yy++) for (int xx = 0; xx < width; xx++) {
            int pixel = result.getRGB(xx, yy);
            if ((pixel >>> 24) == 0) continue;
            boolean keep = brightness(pixel) > 90;
            for (int dy = -6; dy <= 6 && !keep; dy++) for (int dx = -6; dx <= 6 && !keep; dx++) {
                int nx = xx + dx, ny = yy + dy;
                if (nx >= 0 && ny >= 0 && nx < width && ny < height && (result.getRGB(nx, ny) >>> 24) != 0 && brightness(result.getRGB(nx, ny)) > 90) keep = true;
            }
            if (keep) outlined.setRGB(xx, yy, pixel);
        }
        result = outlined;
        // Leaf outlines survive the colour test as thin fragments; keep only real shapes.
        boolean[] seen = new boolean[width * height];
        for (int start = 0; start < seen.length; start++) {
            if (seen[start] || (result.getRGB(start % width, start / width) >>> 24) == 0) continue;
            List<Integer> part = new ArrayList<>(); ArrayDeque<Integer> queue = new ArrayDeque<>(List.of(start)); seen[start] = true;
            while (!queue.isEmpty()) {
                int index = queue.removeFirst(); part.add(index);
                int px = index % width, py = index / width;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int nx = px + dx, ny = py + dy, next = ny * width + nx;
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height || seen[next] || (result.getRGB(nx, ny) >>> 24) == 0) continue;
                    seen[next] = true; queue.add(next);
                }
            }
            if (part.size() < 600) for (int index : part) result.setRGB(index % width, index / width, 0);
        }
        return result;
    }

    static int brightness(int pixel) { return Math.max((pixel >> 16) & 255, Math.max((pixel >> 8) & 255, pixel & 255)); }

    static void place(Graphics2D g, BufferedImage source, int x, int y, int width, int height, double scale, int dx, int dy) {
        int tw = (int)Math.round(width * scale * S), th = (int)Math.round(height * scale * S);
        g.drawImage(areaScale(source.getSubimage(x, y, width, height), tw, th), dx * S, dy * S, null);
    }

    static void px(Graphics2D g, int rgb, int x, int y, int w, int h) { g.setColor(new Color(rgb)); g.fillRect(x, y, w, h); }

    /** Dark horizontal planks behind the buttons. */
    static void board(Graphics2D g, Random random, int x, int y, int w, int h) {
        px(g, 0x1b0e07, x * S, y * S, w * S, h * S);
        int inner = S * 2;
        for (int plank = 0; plank * 8 < h; plank++) {
            int top = y * S + inner + plank * 8 * S, height = Math.min(8 * S, (y + h) * S - inner - top);
            if (height <= 0) break;
            int base = plank % 2 == 0 ? 0x4a2914 : 0x432411;
            px(g, base, x * S + inner, top, w * S - 2 * inner, height);
            for (int streak = 0; streak < w / 3; streak++) {
                int sx = x * S + inner + random.nextInt(w * S - 2 * inner - 30), sy = top + 2 + random.nextInt(Math.max(1, height - 4));
                px(g, random.nextBoolean() ? 0x3a1f0f : 0x5a331a, sx, sy, 12 + random.nextInt(40), 1 + random.nextInt(2));
            }
            px(g, 0x2a160b, x * S + inner, top + height - 2, w * S - 2 * inner, 2);
            px(g, 0x63391d, x * S + inner, top, w * S - 2 * inner, 1);
        }
        px(g, 0x8a4a1f, x * S + S, y * S + S, w * S - 2 * S, 1);
    }

    static void post(Graphics2D g, Random random, int x, int y, int w, int h) {
        px(g, 0x1f1008, x * S, y * S, w * S, h * S);
        px(g, 0x5e3519, x * S + S, y * S, w * S - 2 * S, h * S);
        for (int i = 0; i < h / 2; i++) {
            int sx = x * S + S + random.nextInt(w * S - 3 * S), sy = y * S + random.nextInt(h * S - 20);
            px(g, random.nextBoolean() ? 0x452612 : 0x7a4824, sx, sy, 2, 10 + random.nextInt(24));
        }
        px(g, 0x8a5730, x * S + S, y * S, 2, h * S);
    }

    static void beam(Graphics2D g, Random random, int x, int y, int w, int h) {
        px(g, 0x1f1008, x * S, y * S, w * S, h * S);
        px(g, 0x6a3c1c, x * S, y * S + S, w * S, h * S - 2 * S);
        for (int i = 0; i < w / 2; i++) {
            int sx = x * S + random.nextInt(w * S - 40), sy = y * S + S + random.nextInt(h * S - 3 * S);
            px(g, random.nextBoolean() ? 0x4c2a13 : 0x8a5328, sx, sy, 10 + random.nextInt(30), 2);
        }
        px(g, 0x9a6234, x * S, y * S + S, w * S, 2);
    }

    // ---------------------------------------------------------------- buttons
    static void buttons() throws Exception {
        Path widgets = ROOT.resolve("assets/minecraft/textures/gui/sprites/widget");
        String[] states = {"button", "button_highlighted", "button_disabled"};
        for (int state = 0; state < states.length; state++) {
            ImageIO.write(button(state == 1, state == 2), "png", widgets.resolve(states[state]+".png").toFile());
            Files.writeString(widgets.resolve(states[state]+".png.mcmeta"),
                "{\"gui\":{\"scaling\":{\"type\":\"nine_slice\",\"width\":200,\"height\":20,\"border\":4}}}\n");
        }
    }

    /** Dark wood plank with an orange rim and riveted corner brackets, 3x resolution. */
    static BufferedImage button(boolean highlighted, boolean disabled) {
        int w = 200 * S, h = 20 * S;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        Random random = new Random(7);
        int rim = disabled ? 0x6e5a49 : highlighted ? 0xffbf3f : 0xe8761c;
        int rimLight = disabled ? 0x8a7562 : highlighted ? 0xffe28a : 0xffa645;
        int rimDark = disabled ? 0x4d3f33 : highlighted ? 0xd98a1c : 0xb4500f;
        int wood = disabled ? 0x3a2f27 : highlighted ? 0x5e341a : 0x4a2814;
        px(g, 0x140a04, 0, 0, w, h);
        px(g, rim, S, S, w - 2 * S, h - 2 * S);
        px(g, rimLight, S, S, w - 2 * S, 2);
        px(g, rimDark, S, h - S - 2, w - 2 * S, 2);
        px(g, 0x1e0f06, 2 * S, 2 * S, w - 4 * S, h - 4 * S);
        px(g, wood, 2 * S + 1, 2 * S + 1, w - 4 * S - 2, h - 4 * S - 2);
        for (int i = 0; i < 70; i++) {
            int sx = 2 * S + 1 + random.nextInt(w - 4 * S - 40), sy = 2 * S + 2 + random.nextInt(h - 4 * S - 4);
            px(g, shade(wood, random.nextBoolean() ? -14 : 12), sx, sy, 14 + random.nextInt(36), 1 + random.nextInt(2));
        }
        px(g, shade(wood, 22), 2 * S + 1, 2 * S + 1, w - 4 * S - 2, 1);
        int ornament = disabled ? 0x8a7562 : highlighted ? 0xffd166 : 0xf08a2a, rivet = disabled ? 0xb0a090 : 0xffe6b0;
        for (int corner = 0; corner < 4; corner++) {
            boolean right = corner % 2 == 1, bottom = corner / 2 == 1;
            int ox = right ? w - 2 * S - 9 : 2 * S, oy = bottom ? h - 2 * S - 9 : 2 * S;
            for (int[] cell : new int[][]{{0, 0}, {3, 0}, {6, 0}, {0, 3}, {0, 6}}) {
                int cx = right ? ox + 6 - cell[0] : ox + cell[0], cy = bottom ? oy + 6 - cell[1] : oy + cell[1];
                px(g, ornament, cx, cy, 3, 3);
            }
            px(g, rivet, right ? ox + 6 : ox + 1, bottom ? oy + 6 : oy + 1, 2, 2);
        }
        g.dispose();
        return image;
    }

    static int shade(int rgb, int delta) {
        int r = Math.max(0, Math.min(255, ((rgb >> 16) & 255) + delta)), g = Math.max(0, Math.min(255, ((rgb >> 8) & 255) + delta)), b = Math.max(0, Math.min(255, (rgb & 255) + delta));
        return (r << 16) | (g << 8) | b;
    }

    // ---------------------------------------------------------------- text
    /** Renders text with the pack's 2x ASCII atlas and vanilla advances (x2). */
    static BufferedImage text(String value) {
        int width = 0;
        for (char c : value.toCharArray()) width += advance(c) * 2;
        BufferedImage image = new BufferedImage(Math.max(1, width - 2), 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        drawText(g, value, 0, 0, 2, 0xffffff);
        g.dispose();
        return image;
    }

    static char base(char c) {
        return switch (c) { case 'ö' -> 'o'; case 'ü' -> 'u'; case 'ş' -> 's'; case 'Ş' -> 'S'; case 'ç' -> 'c'; case 'Ç' -> 'C'; case 'ğ' -> 'g'; case 'İ' -> 'I'; case 'ı' -> 'i'; default -> c; };
    }

    static int measured(char c) {
        if (c == ' ') return 0;
        int code = base(c), cx = code % 16 * 16, cy = code / 16 * 16;
        for (int x = 15; x >= 0; x--) for (int y = 0; y < 16; y++) if ((atlas.getRGB(cx + x, cy + y) >>> 24) != 0) return x + 1;
        return 0;
    }

    static int advance(char c) { return c == ' ' ? 4 : (int)(0.5 + measured(c) * 0.5) + 1; }

    /** Draws at 'scale' device pixels per logical pixel (atlas is 2 px per logical pixel). */
    static void drawText(Graphics2D g, String value, int x, int y, int scale, int rgb) {
        double unit = scale / 2.0;
        for (char c : value.toCharArray()) {
            if (c != ' ') {
                int code = base(c), cx = code % 16 * 16, cy = code / 16 * 16;
                for (int yy = 0; yy < 16; yy++) for (int xx = 0; xx < 16; xx++) {
                    int pixel = atlas.getRGB(cx + xx, cy + yy);
                    if ((pixel >>> 24) == 0) continue;
                    if (c == 'ı' && yy < 4) continue;
                    g.setColor(new Color(rgb)); g.fillRect(x + (int)(xx * unit), y + (int)(yy * unit), (int)Math.ceil(unit), (int)Math.ceil(unit));
                }
                g.setColor(new Color(rgb));
                int width = measured(c);
                if (c == 'ö' || c == 'ü') { g.fillRect(x + (int)(2 * unit), y, (int)(2 * unit), (int)(2 * unit)); g.fillRect(x + (int)((width - 4) * unit), y, (int)(2 * unit), (int)(2 * unit)); }
                if (c == 'İ') g.fillRect(x + (int)((width / 2 - 1) * unit), y - (int)(2 * unit), (int)(2 * unit), (int)(2 * unit));
                if (c == 'ş' || c == 'Ş' || c == 'ç' || c == 'Ç') g.fillRect(x + (int)((width / 2 - 1) * unit), y + (int)(16 * unit), (int)(2 * unit), (int)(2 * unit));
            }
            x += advance(c) * scale;
        }
    }

    static int width(String value) { int w = 0; for (char c : value.toCharArray()) w += advance(c); return w; }

    // ---------------------------------------------------------------- packaging
    static void writeZip() throws Exception {
        Path zip = Path.of("dist/TilkiInterface-ResourcePack.zip");
        Files.createDirectories(zip.getParent());
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip)); var files = Files.walk(ROOT)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                ZipEntry entry = new ZipEntry(ROOT.relativize(file).toString().replace('\\', '/'));
                entry.setTime(0); out.putNextEntry(entry); Files.copy(file, out); out.closeEntry();
            }
        }
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(zip)));
        Files.writeString(Path.of("dist/resourcepack-sha1.txt"), hash + "\n");
        System.out.println("ESC menu pack SHA1: " + hash + "; " + Files.size(zip) + " bytes");
    }

    /** Simulates the 1.21 pause layout for the smallest, the common 1080p and a tall GUI height. */
    static void preview(BufferedImage frame) throws Exception {
        int[] heights = {240, 270, 360};
        int scale = 2, gap = 12, total = heights.length * (360 * scale + gap);
        BufferedImage preview = new BufferedImage(total, 360 * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = preview.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        px(g, 0x101418, 0, 0, total, 360 * scale);
        BufferedImage normal = button(false, false), hover = button(true, false);
        String[] labels = {null, "Gelişmeler", "İstatistikler", "Geri Bildirim", "Hata Bildir", "Ayarlar", "Oyuncu Bildir", "Sunucudan Ayrıl"};
        BufferedImage text = text(LABEL);
        int ox = 0;
        for (int h : heights) {
            int width = 360;
            px(g, 0x2f3d33, ox, 0, width * scale, h * scale);
            g.setColor(new Color(255, 255, 255, 140)); g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("GUI height " + h, ox + 8, h * scale - 8);
            int gridTop = (int)((h - 166) * 0.25f), top = gridTop + 50, cx = width / 2;
            int frameTop = top - TOP;
            for (int i = 0; i < BUTTONS.length; i++) {
                int[] b = BUTTONS[i];
                int bx = cx - CX + b[0], by = frameTop + b[1];
                nine(g, i == 3 ? hover : normal, ox + bx * scale, by * scale, b[2] * scale, b[3] * scale, scale);
                if (labels[i] != null) {
                    int tx = bx + b[2] / 2 - width(labels[i]) / 2, ty = by + 6;
                    drawText(g, labels[i], ox + (tx + 1) * scale, (ty + 1) * scale, scale, 0x3f3f3f);
                    drawText(g, labels[i], ox + tx * scale, ty * scale, scale, 0xffffff);
                }
            }
            // The frame is the first button's label: shadow first, then the glyphs.
            BufferedImage shadow = tint(frame, 0x3f3f3f);
            g.drawImage(shadow, ox + (cx - CX + 1) * scale, (frameTop + 1) * scale, W * scale, H * scale, null);
            g.drawImage(frame, ox + (cx - CX) * scale, frameTop * scale, W * scale, H * scale, null);
            int textAdvance = (int)(0.5 + text.getWidth() / 2.0) + 1;
            g.drawImage(tint(text, 0x3f3f3f), ox + (cx - textAdvance / 2 + 1) * scale, (top + 7) * scale, text.getWidth() * scale / 2, 8 * scale, null);
            g.drawImage(text, ox + (cx - textAdvance / 2) * scale, (top + 6) * scale, text.getWidth() * scale / 2, 8 * scale, null);
            if (frameTop < 0 || frameTop + H > h) throw new IllegalStateException("Frame leaves a "+h+" px tall GUI");
            px(g, 0x101418, ox, h * scale, width * scale, (360 - h) * scale);
            ox += width * scale + gap;
        }
        g.dispose();
        ImageIO.write(preview, "png", Path.of("art/esc-frame-preview.png").toFile());
    }

    static BufferedImage tint(BufferedImage image, int rgb) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int alpha = image.getRGB(x, y) >>> 24;
            if (alpha >= 26) result.setRGB(x, y, (alpha << 24) | rgb);
        }
        return result;
    }

    /** Nine-slice with border 4 and tiled edges, like the GUI sprite renderer. */
    static void nine(Graphics2D g, BufferedImage sprite, int x, int y, int w, int h, int scale) {
        int b = 4 * S, bs = 4 * scale;
        int sw = sprite.getWidth(), sh = sprite.getHeight();
        int innerW = w - 2 * bs;
        int srcInnerW = Math.min(sw - 2 * b, innerW * S / scale), srcInnerH = sh - 2 * b;
        g.drawImage(sprite, x, y, x + bs, y + bs, 0, 0, b, b, null);
        g.drawImage(sprite, x + w - bs, y, x + w, y + bs, sw - b, 0, sw, b, null);
        g.drawImage(sprite, x, y + h - bs, x + bs, y + h, 0, sh - b, b, sh, null);
        g.drawImage(sprite, x + w - bs, y + h - bs, x + w, y + h, sw - b, sh - b, sw, sh, null);
        g.drawImage(sprite, x + bs, y, x + w - bs, y + bs, b, 0, b + srcInnerW, b, null);
        g.drawImage(sprite, x + bs, y + h - bs, x + w - bs, y + h, b, sh - b, b + srcInnerW, sh, null);
        g.drawImage(sprite, x, y + bs, x + bs, y + h - bs, 0, b, b, b + srcInnerH, null);
        g.drawImage(sprite, x + w - bs, y + bs, x + w, y + h - bs, sw - b, b, sw, b + srcInnerH, null);
        g.drawImage(sprite, x + bs, y + bs, x + w - bs, y + h - bs, b, b, b + srcInnerW, b + srcInnerH, null);
    }

    /** Box-filter downscale with premultiplied alpha. */
    static BufferedImage areaScale(BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        double sx = source.getWidth() / (double)width, sy = source.getHeight() / (double)height;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            double a = 0, r = 0, gr = 0, b = 0, total = 0;
            for (int yy = (int)(y * sy); yy < Math.min(source.getHeight(), Math.ceil((y + 1) * sy)); yy++) {
                double wy = Math.min(yy + 1, (y + 1) * sy) - Math.max(yy, y * sy);
                for (int xx = (int)(x * sx); xx < Math.min(source.getWidth(), Math.ceil((x + 1) * sx)); xx++) {
                    double weight = wy * (Math.min(xx + 1, (x + 1) * sx) - Math.max(xx, x * sx));
                    int pixel = source.getRGB(xx, yy); double alpha = ((pixel >>> 24) / 255.0) * weight;
                    a += alpha; r += ((pixel >> 16) & 255) * alpha; gr += ((pixel >> 8) & 255) * alpha; b += (pixel & 255) * alpha; total += weight;
                }
            }
            int alpha = (int)Math.round(255 * a / total);
            if (alpha < 26) continue;
            result.setRGB(x, y, (alpha << 24) | ((int)Math.round(r / a) << 16) | ((int)Math.round(gr / a) << 8) | (int)Math.round(b / a));
        }
        return result;
    }
}
