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
    static final String BOTTOM_GLYPH = "\uEA06";
    static final String REWIND_GLYPH = "\uEA0A";

    static BufferedImage frame(Path artwork) throws Exception {
        BufferedImage source = ImageIO.read(artwork.toFile());
        if (source == null) throw new IllegalArgumentException("Invalid frame PNG");
        BufferedImage result = new BufferedImage(256, 278, BufferedImage.TYPE_INT_ARGB);
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
        // Force full-cell glyph width without adding visible side pixels.
        result.setRGB(0, 0, 0x01ffffff);
        result.setRGB(255, 0, 0x01ffffff);
        return result;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("ESC frame source PNG required");
        Path texture = ROOT.resolve("assets/tilkiinterface/textures/font/esc_frame.png");
        Path bottomTexture = ROOT.resolve("assets/tilkiinterface/textures/font/esc_frame_bottom.png");
        Files.createDirectories(texture.getParent());
        BufferedImage image = frame(Path.of(args[0]));
        BufferedImage top=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB),bottom=new BufferedImage(256,22,BufferedImage.TYPE_INT_ARGB);
        Graphics2D topGraphics=top.createGraphics();topGraphics.drawImage(image,0,0,null);topGraphics.dispose();
        Graphics2D bottomGraphics=bottom.createGraphics();bottomGraphics.drawImage(image,0,0,256,22,0,256,256,278,null);bottomGraphics.dispose();
        top.setRGB(255,0,0x01ffffff);bottom.setRGB(255,0,0x01ffffff);
        ImageIO.write(top, "png", texture.toFile());ImageIO.write(bottom,"png",bottomTexture.toFile());
        Path definition = ROOT.resolve("assets/tilkiinterface/font/ui.json");
        JsonObject font = JsonParser.parseString(Files.readString(definition)).getAsJsonObject();
        JsonArray providers = new JsonArray();
        for (JsonElement entry : font.getAsJsonArray("providers")) {
            JsonObject provider = entry.getAsJsonObject();
            if (provider.has("file") && provider.get("file").getAsString().contains("esc_frame")) continue;
            if(provider.get("type").getAsString().equals("space") && provider.getAsJsonObject("advances").has(REWIND_GLYPH))provider.getAsJsonObject("advances").remove(REWIND_GLYPH);
            providers.add(entry);
        }
        JsonObject frameProvider = new JsonObject();
        frameProvider.addProperty("type", "bitmap");
        frameProvider.addProperty("file", "tilkiinterface:font/esc_frame.png");
        frameProvider.addProperty("height", 256);
        frameProvider.addProperty("ascent", 44);
        JsonArray chars = new JsonArray(); chars.add(GLYPH); frameProvider.add("chars", chars);
        providers.add(frameProvider);
        JsonObject bottomProvider=new JsonObject();bottomProvider.addProperty("type","bitmap");bottomProvider.addProperty("file","tilkiinterface:font/esc_frame_bottom.png");bottomProvider.addProperty("height",22);bottomProvider.addProperty("ascent",-212);
        JsonArray bottomChars=new JsonArray();bottomChars.add(BOTTOM_GLYPH);bottomProvider.add("chars",bottomChars);providers.add(bottomProvider);
        JsonObject rewindProvider=new JsonObject();rewindProvider.addProperty("type","space");JsonObject advances=new JsonObject();advances.addProperty(REWIND_GLYPH,-257);rewindProvider.add("advances",advances);providers.add(rewindProvider);
        font.add("providers", providers);
        Files.writeString(definition, font.toString());
        for (String lang : new String[]{"tr_tr", "en_us", "en_gb"}) {
            Path path = ROOT.resolve("assets/minecraft/lang/"+lang+".json");
            JsonObject translation = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            translation.addProperty("menu.game", GLYPH+REWIND_GLYPH+BOTTOM_GLYPH);
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
        BufferedImage preview = new BufferedImage(768, 834, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = preview.createGraphics();
        g.setColor(new Color(33, 41, 48)); g.fillRect(0, 0, 768, 768);
        g.drawImage(image, 0, 0, 768, 834, null);
        g.dispose();
        ImageIO.write(preview, "png", Path.of("art/esc-frame-preview.png").toFile());
        System.out.println("ESC frame pack SHA1: "+hash+"; "+Files.size(zip)+" bytes");
    }
}
