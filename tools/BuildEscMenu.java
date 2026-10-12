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
    // 1x pixel art: one texture pixel per GUI pixel keeps the frame visibly pixelated.
    static final int S = 1, W = 252, COLS = 1, TILE = W / COLS;
    static final int H = 226, TOP = 68, CX = W / 2;
    static final int[] ROWS = {H};
    // Pause grid (1.21): 204/98 px buttons, 20 px tall, 4 px gaps, five rows.
    static final int[][] BUTTONS = {
        {CX - 102, TOP, 204, 20},
        {CX - 102, TOP + 24, 98, 20}, {CX + 4, TOP + 24, 98, 20},
        {CX - 102, TOP + 48, 98, 20}, {CX + 4, TOP + 48, 98, 20},
        {CX - 102, TOP + 72, 98, 20}, {CX + 4, TOP + 72, 98, 20},
        {CX - 102, TOP + 96, 204, 20}};
    // Where the button grid sits in the 1672x941 reference (art/esc-menu-reference-v2.png).
    static final double REF_LEFT = 459, REF_TOP = 370, REF_SX = (1107 - 459) / 204.0, REF_SY = (757 - 370) / 116.0;
    // 26.2+ replaces the feedback row with four centred 20 px icon buttons
    // (bug, feedback, friends, player reporting) spaced 4 px apart.
    static final int[][] ICON_BUTTONS = {
        BUTTONS[0], BUTTONS[1], BUTTONS[2],
        {CX - 46, TOP + 48, 20, 20}, {CX - 22, TOP + 48, 20, 20}, {CX + 2, TOP + 48, 20, 20}, {CX + 26, TOP + 48, 20, 20},
        BUTTONS[5], BUTTONS[6], BUTTONS[7]};
    // Resource pack format of 26.2, the first release with the icon row.
    static final int ICON_MENU_FORMAT = 88, MAX_FORMAT = 999;
    static final String OVERLAY = "tilki_icon_menu", LEGACY_OVERLAY = "tilki_legacy";
    static final char TILE_BASE = '', TEXT = '', BACK_ONE = '', HALF = '', FULL = '', TEXT_LEFT = '', TEXT_RIGHT = '';
    static final String LABEL = "Oyuna Dön";
    static BufferedImage atlas;

    public static void main(String[] args) throws Exception {
        if (args.length < 3) throw new IllegalArgumentException("Usage: BuildEscMenu <esc-menu-reference-v2.png> <ui-kit.png> <ascii.png> [extracted gui/sprites dir for preview icons]");
        BufferedImage source = ImageIO.read(Path.of(args[0]).toFile());
        kit = ImageIO.read(Path.of(args[1]).toFile());
        atlas = ImageIO.read(Path.of(args[2]).toFile());
        if (source == null || atlas == null || atlas.getWidth() != 256) throw new IllegalArgumentException("Invalid frame or ASCII atlas");
        BufferedImage frame = frame(source, BUTTONS), iconFrame = frame(source, ICON_BUTTONS);
        Path textures = ROOT.resolve("assets/tilkiinterface/textures/font/esc_menu");
        Path overlayTextures = ROOT.resolve(OVERLAY + "/assets/tilkiinterface/textures/font/esc_menu");
        for (Path dir : List.of(textures, overlayTextures)) {
            Files.createDirectories(dir);
            try (var old = Files.list(dir)) { for (Path file : old.toList()) Files.delete(file); }
        }
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
                BufferedImage tile = tile(frame, row, col, y), iconTile = tile(iconFrame, row, col, y);
                String name = "r"+row+"c"+col+".png";
                ImageIO.write(tile, "png", textures.resolve(name).toFile());
                // The overlay only carries the tiles that differ for the icon row.
                if (!same(tile, iconTile)) ImageIO.write(iconTile, "png", overlayTextures.resolve(name).toFile());
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
        Files.writeString(ROOT.resolve("pack.mcmeta"), packMetadata("TilkiCraft - orange fox interface | 1.21.4 - 26.x"));
        buttons();
        writeZip();
        preview(frame, iconFrame, args.length > 3 ? Path.of(args[3]) : null);
    }

    /**
     * Declares 1.21.4 (46) up to current releases and routes 26.2+ clients to the
     * icon-row frame. Old and new field names are both present, as required when a
     * pack spans formats below and above 65.
     */
    static String packMetadata(String description) {
        // 1.21.4 needs "formats" on every overlay, while 26.x rejects it unless some
        // overlay covers a pre-65 format; the empty legacy entry makes both accept it.
        return "{\"pack\":{\"description\":\"" + description + "\",\"pack_format\":46,\"supported_formats\":[46," + MAX_FORMAT + "],\"min_format\":46,\"max_format\":" + MAX_FORMAT + "},"
            + "\"overlays\":{\"entries\":["
            + "{\"directory\":\"" + LEGACY_OVERLAY + "\",\"formats\":[46,64],\"min_format\":46,\"max_format\":64},"
            + "{\"directory\":\"" + OVERLAY + "\",\"formats\":[" + ICON_MENU_FORMAT + "," + MAX_FORMAT + "],\"min_format\":" + ICON_MENU_FORMAT + ",\"max_format\":" + MAX_FORMAT + "}]}}\n";
    }

    static BufferedImage tile(BufferedImage frame, int row, int col, int y) {
        BufferedImage tile = frame.getSubimage(col * TILE * S, y * S, TILE * S, ROWS[row] * S);
        BufferedImage copy = new BufferedImage(tile.getWidth(), tile.getHeight(), BufferedImage.TYPE_INT_ARGB);
        copy.getGraphics().drawImage(tile, 0, 0, null);
        // Minecraft measures advance from the last non-transparent column.
        if ((copy.getRGB(copy.getWidth() - 1, 0) >>> 24) == 0) copy.setRGB(copy.getWidth() - 1, 0, 0x01ffffff);
        return copy;
    }

    static boolean same(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) for (int x = 0; x < a.getWidth(); x++) if (a.getRGB(x, y) != b.getRGB(x, y)) return false;
        return true;
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
    /**
     * The frame is the user's forest design itself: the reference is cut from its
     * dark background, its painted buttons are replaced by plain board, and it is
     * resampled to one texture pixel per GUI pixel. The fox and logo crest is
     * scaled separately so the whole frame still fits a 240 px tall GUI.
     */
    static BufferedImage frame(BufferedImage reference, int[][] holes) {
        BufferedImage art = cutout(reference);
        // Board colour from the gaps between the painted buttons.
        long r = 0, gr = 0, b = 0, n = 0;
        for (int y = 444; y <= 450; y++) for (int x = 560; x <= 1000; x++) { int p = art.getRGB(x, y); r += (p >> 16) & 255; gr += (p >> 8) & 255; b += p & 255; n++; }
        int board = 0xff000000 | (int)(r / n) << 16 | (int)(gr / n) << 8 | (int)(b / n);
        Graphics2D paint = art.createGraphics();
        paint.setColor(new Color(board, true));
        paint.fillRect((int)REF_LEFT - 6, (int)REF_TOP - 4, (int)(204 * REF_SX) + 12, (int)(116 * REF_SY) + 8);
        paint.dispose();
        BufferedImage canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        // Body: top beam down to the support sign, at the button grid's own scale.
        int beam = 56, bodyLeft = (int)Math.round(REF_LEFT - (CX - 102) * REF_SX), bodyTop = (int)Math.round(REF_TOP - (TOP - beam) * REF_SY);
        int bodyWidth = (int)Math.round(W * REF_SX), bodyHeight = (int)Math.round((H - beam) * REF_SY);
        g.drawImage(areaScale(art.getSubimage(bodyLeft, bodyTop, bodyWidth, Math.min(bodyHeight, art.getHeight() - bodyTop)), W, H - beam), 0, beam, null);
        // Crest: foxes and logo, scaled down to the 60 px above the beam.
        int crestLeft = 400, crestTop = 55, crestRight = 1180, crestBottom = bodyTop + 6;
        BufferedImage crest = art.getSubimage(crestLeft, crestTop, crestRight - crestLeft, crestBottom - crestTop);
        int crestHeight = beam + 4, crestWidth = (int)Math.round(crest.getWidth() * crestHeight / (double)crest.getHeight());
        g.drawImage(areaScale(crest, crestWidth, crestHeight), (W - crestWidth) / 2, 0, null);
        g.dispose();
        // Native buttons render in these holes; nothing may cover them.
        for (int[] button : holes)
            for (int yy = button[1] * S; yy < (button[1] + button[3]) * S; yy++)
                for (int xx = button[0] * S; xx < (button[0] + button[2]) * S; xx++) canvas.setRGB(xx, yy, 0);
        return canvas;
    }

    /** Makes the reference's flat dark background transparent (flood fill from the image border). */
    static BufferedImage cutout(BufferedImage source) {
        int width = source.getWidth(), height = source.getHeight(), background = source.getRGB(4, 4);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        boolean[] outside = new boolean[width * height];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int x = 0; x < width; x++) { queue.add(new int[]{x, 0}); queue.add(new int[]{x, height - 1}); }
        for (int y = 0; y < height; y++) { queue.add(new int[]{0, y}); queue.add(new int[]{width - 1, y}); }
        while (!queue.isEmpty()) {
            int[] p = queue.removeFirst(); int x = p[0], y = p[1];
            if (x < 0 || y < 0 || x >= width || y >= height || outside[y * width + x]) continue;
            int pixel = source.getRGB(x, y);
            if (Math.max(Math.abs(((pixel >> 16) & 255) - ((background >> 16) & 255)), Math.max(Math.abs(((pixel >> 8) & 255) - ((background >> 8) & 255)), Math.abs((pixel & 255) - (background & 255)))) > 14) continue;
            outside[y * width + x] = true;
            queue.add(new int[]{x + 1, y}); queue.add(new int[]{x - 1, y}); queue.add(new int[]{x, y + 1}); queue.add(new int[]{x, y - 1});
        }
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) if (!outside[y * width + x]) result.setRGB(x, y, source.getRGB(x, y) | 0xff000000);
        return result;
    }

    static void px(Graphics2D g, int rgb, int x, int y, int w, int h) { g.setColor(new Color(rgb)); g.fillRect(x, y, w, h); }

    // ---------------------------------------------------------------- buttons
    static void buttons() throws Exception {
        Path widgets = ROOT.resolve("assets/minecraft/textures/gui/sprites/widget");
        String[] states = {"button", "button_highlighted", "button_disabled"};
        for (int state = 0; state < states.length; state++) {
            ImageIO.write(button(state == 1, state == 2), "png", widgets.resolve(states[state]+".png").toFile());
            // Same size and border as the vanilla 26.x sprite: the 3x HD variant showed
            // as a missing texture on a 26.3 client.
            Files.writeString(widgets.resolve(states[state]+".png.mcmeta"),
                "{\"gui\":{\"scaling\":{\"type\":\"nine_slice\",\"width\":200,\"height\":20,\"border\":" + BUTTON_BORDER + "}}}\n");
        }
    }

    static final int BUTTON_BORDER = 3;

    // Normal, hover and disabled buttons in the UI kit sheet (art/ui-kit.png).
    static final int[] KIT_NORMAL = {48, 300, 394, 86}, KIT_HOVER = {463, 299, 395, 88}, KIT_DISABLED = {1294, 300, 196, 86};
    static BufferedImage kit;

    /**
     * The kit button scaled to the vanilla 20 px height (1x pixel art). Its rivet
     * ends are kept and the plain wood middle is repeated to the 200 px sprite width.
     */
    static BufferedImage button(boolean highlighted, boolean disabled) {
        int[] r = disabled ? KIT_DISABLED : highlighted ? KIT_HOVER : KIT_NORMAL;
        BufferedImage crop = kit.getSubimage(r[0], r[1], r[2], r[3]);
        BufferedImage scaled = areaScale(crop, (int)Math.round(r[2] * 20.0 / r[3]), 20);
        int w = 200, end = Math.min(24, scaled.getWidth() / 2 - 4), middle = scaled.getWidth() - 2 * end;
        BufferedImage image = new BufferedImage(w, 20, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < end; x++) { image.setRGB(x, y, scaled.getRGB(x, y)); image.setRGB(w - end + x, y, scaled.getRGB(scaled.getWidth() - end + x, y)); }
            for (int x = end; x < w - end; x++) image.setRGB(x, y, scaled.getRGB(end + (x - end) % middle, y));
        }
        return image;
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

    /**
     * Simulates both pause layouts (1.21.4 and the 26.2+ icon row) at the smallest,
     * the common 1080p and a tall GUI height. Icon sprites are read from an optional
     * extracted client directory; without it the icon buttons stay plain.
     */
    static void preview(BufferedImage frame, BufferedImage iconFrame, Path icons) throws Exception {
        int[] heights = {240, 270, 360};
        int scale = 2, gap = 12, panel = 360 * scale + gap, total = heights.length * panel;
        BufferedImage preview = new BufferedImage(total, 2 * panel, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = preview.createGraphics();
        px(g, 0x101418, 0, 0, total, 2 * panel);
        String[] oldLabels = {null, "Gelişmeler", "İstatistikler", "Geri Bildirim", "Hata Bildir", "Ayarlar", "Oyuncu Bildir", "Sunucudan Ayrıl"};
        String[] newLabels = {null, "Gelişmeler", "İstatistikler", "bug", "social_interactions", "friends", "player_reporting", "Ayarlar", "Dünya Seçenekleri...", "Sunucudan Ayrıl"};
        for (int layout = 0; layout < 2; layout++) {
            int[][] buttons = layout == 0 ? BUTTONS : ICON_BUTTONS;
            String[] labels = layout == 0 ? oldLabels : newLabels;
            for (int column = 0; column < heights.length; column++)
                pausePanel(g, layout == 0 ? frame : iconFrame, buttons, labels, icons, heights[column], column * panel, layout * panel, scale,
                    (layout == 0 ? "1.21.4" : "26.2+") + " - GUI " + heights[column]);
        }
        g.dispose();
        ImageIO.write(preview, "png", Path.of("art/esc-frame-preview.png").toFile());
    }

    static void pausePanel(Graphics2D g, BufferedImage frame, int[][] buttons, String[] labels, Path icons, int h, int ox, int oy, int scale, String caption) throws Exception {
        int width = 360;
        px(g, 0x2f3d33, ox, oy, width * scale, h * scale);
        g.setColor(new Color(255, 255, 255, 140)); g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString(caption, ox + 8, oy + h * scale - 8);
        int gridTop = (int)((h - 166) * 0.25f), top = gridTop + 50, cx = width / 2, frameTop = top - TOP;
        BufferedImage normal = button(false, false), hover = button(true, false);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int i = 0; i < buttons.length; i++) {
            int[] b = buttons[i];
            int bx = cx - CX + b[0], by = frameTop + b[1];
            nine(g, i == 3 ? hover : normal, ox + bx * scale, oy + by * scale, b[2] * scale, b[3] * scale, scale);
            if (labels[i] == null) continue;
            Path icon = icons == null ? null : icons.resolve(labels[i].equals("friends") ? "friends/friends.png" : "pause_menu/" + labels[i] + ".png");
            if (b[2] == 20) {
                if (icon != null && Files.isRegularFile(icon)) g.drawImage(ImageIO.read(icon.toFile()), ox + (bx + 2) * scale, oy + (by + 2) * scale, 15 * scale, 15 * scale, null);
                continue;
            }
            int tx = bx + b[2] / 2 - width(labels[i]) / 2, ty = by + 6;
            drawText(g, labels[i], ox + (tx + 1) * scale, oy + (ty + 1) * scale, scale, 0x3f3f3f);
            drawText(g, labels[i], ox + tx * scale, oy + ty * scale, scale, 0xffffff);
        }
        // The frame is the first button's label: shadow first, then the glyphs.
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(tint(frame, 0x3f3f3f), ox + (cx - CX + 1) * scale, oy + (frameTop + 1) * scale, W * scale, H * scale, null);
        g.drawImage(frame, ox + (cx - CX) * scale, oy + frameTop * scale, W * scale, H * scale, null);
        BufferedImage text = text(LABEL);
        int textAdvance = (int)(0.5 + text.getWidth() / 2.0) + 1;
        g.drawImage(tint(text, 0x3f3f3f), ox + (cx - textAdvance / 2 + 1) * scale, oy + (top + 7) * scale, text.getWidth() * scale / 2, 8 * scale, null);
        g.drawImage(text, ox + (cx - textAdvance / 2) * scale, oy + (top + 6) * scale, text.getWidth() * scale / 2, 8 * scale, null);
        if (frameTop < 0 || frameTop + H > h) throw new IllegalStateException("Frame leaves a "+h+" px tall GUI");
        px(g, 0x101418, ox, oy + h * scale, width * scale, (360 - h) * scale);
    }

    static BufferedImage tint(BufferedImage image, int rgb) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int alpha = image.getRGB(x, y) >>> 24;
            if (alpha >= 26) result.setRGB(x, y, (alpha << 24) | rgb);
        }
        return result;
    }

    /** Nine-slice with the button border, cropping the centre like the GUI sprite renderer. */
    static void nine(Graphics2D g, BufferedImage sprite, int x, int y, int w, int h, int scale) {
        int b = BUTTON_BORDER, bs = BUTTON_BORDER * scale;
        int sw = sprite.getWidth(), sh = sprite.getHeight();
        int innerW = w - 2 * bs;
        int srcInnerW = Math.min(sw - 2 * b, innerW / scale), srcInnerH = sh - 2 * b;
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
