import com.google.gson.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** Adds a pause-menu frame to the centered menu.game title glyph. */
class BuildEscFrame {
    static final Path ROOT = Path.of("resourcepack");
    static final String GLYPH = "\uEA03";

    static BufferedImage frame(Path artwork, Path logoPath) throws Exception {
        BufferedImage source = ImageIO.read(artwork.toFile());
        BufferedImage logo = ImageIO.read(logoPath.toFile());
        if (source == null || logo == null) throw new IllegalArgumentException("Invalid frame or logo PNG");
        BufferedImage result = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        // Keep a 212-pixel clear center for the native 204-pixel-wide pause buttons.
        // Compress only the decorative side margins of the generated square artwork.
        int left = source.getWidth() * 14 / 100;
        int right = source.getWidth() * 86 / 100;
        for (int y = 0; y < result.getHeight(); y++) {
            int sy = y * source.getHeight() / result.getHeight();
            for (int x = 0; x < result.getWidth(); x++) {
                int sx;
                if (x < 22) sx = x * left / 22;
                else if (x < 234) sx = left + (x - 22) * (right - left) / 212;
                else sx = right + (x - 234) * (source.getWidth() - right) / 22;
                result.setRGB(x, y, source.getRGB(Math.min(sx, source.getWidth()-1), sy));
            }
        }
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        // The existing TilkiCraft logo remains the menu's focal point, in front of the top rail.
        g.drawImage(logo, 89, 0, 78, 78, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        String welcome = "TILKICRAFT'A HOŞ GELDİN!";
        FontMetrics metrics = g.getFontMetrics();
        int tx = (256 - metrics.stringWidth(welcome)) / 2;
        g.setColor(new Color(19, 10, 5));g.drawString(welcome, tx+1, 231);
        g.setColor(new Color(255, 170, 48));g.drawString(welcome, tx, 230);
        g.dispose();
        // Force full-cell glyph width without adding visible side pixels.
        result.setRGB(0, 0, 0x01ffffff);
        result.setRGB(255, 0, 0x01ffffff);
        return result;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Frame source and TilkiCraft logo PNG required");
        Path texture = ROOT.resolve("assets/tilkiinterface/textures/font/esc_frame.png");
        Files.createDirectories(texture.getParent());
        BufferedImage image = frame(Path.of(args[0]), Path.of(args[1]));
        ImageIO.write(image, "png", texture.toFile());
        Path definition = ROOT.resolve("assets/tilkiinterface/font/ui.json");
        JsonObject font = JsonParser.parseString(Files.readString(definition)).getAsJsonObject();
        JsonArray providers = new JsonArray();
        for (JsonElement entry : font.getAsJsonArray("providers")) {
            JsonObject provider = entry.getAsJsonObject();
            if (provider.has("file") && provider.get("file").getAsString().equals("tilkiinterface:font/esc_frame.png")) continue;
            providers.add(entry);
        }
        JsonObject frameProvider = new JsonObject();
        frameProvider.addProperty("type", "bitmap");
        frameProvider.addProperty("file", "tilkiinterface:font/esc_frame.png");
        frameProvider.addProperty("height", 256);
        frameProvider.addProperty("ascent", 44);
        JsonArray chars = new JsonArray(); chars.add(GLYPH); frameProvider.add("chars", chars);
        providers.add(frameProvider);
        font.add("providers", providers);
        Files.writeString(definition, font.toString());
        for (String lang : new String[]{"tr_tr", "en_us", "en_gb"}) {
            Path path = ROOT.resolve("assets/minecraft/lang/"+lang+".json");
            JsonObject translation = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            translation.addProperty("menu.game", GLYPH);
            Files.writeString(path, translation.toString());
        }
        Path zip = Path.of("dist/TilkiInterface-ResourcePack.zip");
        Files.createDirectories(zip.getParent());
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip)); var files = Files.walk(ROOT)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                ZipEntry entry = new ZipEntry(ROOT.relativize(file).toString().replace('\\','/'));
                entry.setTime(0); out.putNextEntry(entry); Files.copy(file, out); out.closeEntry();
            }
        }
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(zip)));
        Files.writeString(Path.of("dist/resourcepack-sha1.txt"), hash+"\n");
        BufferedImage preview = new BufferedImage(768, 768, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = preview.createGraphics();
        g.setColor(new Color(33, 41, 48)); g.fillRect(0, 0, 768, 768);
        g.drawImage(image, 0, 0, 768, 768, null);
        g.dispose();
        ImageIO.write(preview, "png", Path.of("art/esc-frame-preview.png").toFile());
        System.out.println("ESC frame pack SHA1: "+hash+"; "+Files.size(zip)+" bytes");
    }
}
