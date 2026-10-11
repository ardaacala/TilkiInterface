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

    static BufferedImage frame(Path artwork) throws Exception {
        BufferedImage source = ImageIO.read(artwork.toFile());
        if (source == null) throw new IllegalArgumentException("Invalid frame PNG");
        // Keep the logical glyph compact, but use the full 256px atlas width so
        // the artwork stays crisp at large GUI scales without a larger layout.
        BufferedImage result = new BufferedImage(256, 235, BufferedImage.TYPE_INT_ARGB);
        // Keep a 212-pixel clear center for the native 204-pixel-wide pause buttons.
        // Compress only the decorative side margins of the generated square artwork.
        int left = source.getWidth() * 12 / 100;
        int right = source.getWidth() * 88 / 100;
        Graphics2D draw=result.createGraphics();
        draw.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        draw.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        draw.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        draw.drawImage(source,0,0,15,235,0,0,left,source.getHeight(),null);
        draw.drawImage(source,15,0,241,235,left,0,right,source.getHeight(),null);
        draw.drawImage(source,241,0,256,235,right,0,source.getWidth(),source.getHeight(),null);
        draw.dispose();
        // Force full-cell glyph width without adding visible side pixels.
        result.setRGB(0, 0, 0x01ffffff);
        result.setRGB(255, 0, 0x01ffffff);
        return result;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("ESC frame source PNG required");
        Path texture = ROOT.resolve("assets/tilkiinterface/textures/font/esc_frame.png");
        Files.createDirectories(texture.getParent());
        BufferedImage image = frame(Path.of(args[0]));
        ImageIO.write(image, "png", texture.toFile());
        Files.deleteIfExists(ROOT.resolve("assets/tilkiinterface/textures/font/esc_frame_bottom.png"));
        Path definition = ROOT.resolve("assets/tilkiinterface/font/ui.json");
        JsonObject font = JsonParser.parseString(Files.readString(definition)).getAsJsonObject();
        JsonArray providers = new JsonArray();
        for (JsonElement entry : font.getAsJsonArray("providers")) {
            JsonObject provider = entry.getAsJsonObject();
            if (provider.has("file") && provider.get("file").getAsString().contains("esc_frame")) continue;
            providers.add(entry);
        }
        JsonObject frameProvider = new JsonObject();
        frameProvider.addProperty("type", "bitmap");
        frameProvider.addProperty("file", "tilkiinterface:font/esc_frame.png");
        frameProvider.addProperty("height", 220);
        frameProvider.addProperty("ascent", 38);
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
        BufferedImage preview = new BufferedImage(720, 660, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = preview.createGraphics();
        g.setColor(new Color(33, 41, 48)); g.fillRect(0, 0, 720, 660);
        g.drawImage(image, 0, 0, 720, 660, null);
        g.dispose();
        ImageIO.write(preview, "png", Path.of("art/esc-frame-preview.png").toFile());
        System.out.println("ESC frame pack SHA1: "+hash+"; "+Files.size(zip)+" bytes");
    }
}
