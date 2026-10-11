import com.google.gson.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;

/** Keeps the ESC logo and panel icons, replacing frame glyphs with two small logos. */
class BuildLogoAssets {
 static final Path ROOT=Path.of("resourcepack");
 static BufferedImage scaled(Path source,int width,int height) throws Exception {
  BufferedImage input=ImageIO.read(source.toFile());
  if(input==null)throw new IllegalArgumentException("Invalid image: "+source);
  BufferedImage output=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
  Graphics2D g=output.createGraphics();
  g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
  g.drawImage(input,0,0,width,width,null);g.dispose();
  output.setRGB(width-1,0,0x01ffffff); // predictable glyph advance
  return output;
 }
 static JsonObject bitmap(String file,int height,int ascent,String character) {
  JsonObject provider=new JsonObject();provider.addProperty("type","bitmap");
  provider.addProperty("file","tilkiinterface:font/"+file);
  provider.addProperty("height",height);provider.addProperty("ascent",ascent);
  JsonArray chars=new JsonArray();chars.add(character);provider.add("chars",chars);
  return provider;
 }
 public static void main(String[] args) throws Exception {
  if(args.length!=2)throw new IllegalArgumentException("Fox head and TAB logo PNG required");
  Path textures=ROOT.resolve("assets/tilkiinterface/textures/font");
  ImageIO.write(scaled(Path.of(args[0]),128,256),"png",textures.resolve("mascot.png").toFile());
  ImageIO.write(scaled(Path.of(args[1]),128,128),"png",textures.resolve("tab_logo.png").toFile());
  // The old frame textures and font providers must not inflate the second download.
  try(var files=Files.list(textures)) {
   for(Path file:files.filter(p->p.getFileName().toString().matches("(sidebar_frame.*|tab_.*)\\.png")&&!p.getFileName().toString().equals("tab_logo.png")).toList())Files.delete(file);
  }
  Path hd=textures.resolve("hd");
  if(Files.isDirectory(hd)) {
   try(var files=Files.list(hd)) {for(Path file:files.toList())Files.delete(file);}
   Files.delete(hd);
  }
  Path definition=ROOT.resolve("assets/tilkiinterface/font/ui.json");
  JsonObject font=JsonParser.parseString(Files.readString(definition)).getAsJsonObject();
  JsonArray kept=new JsonArray();
  for(JsonElement element:font.getAsJsonArray("providers")) {
   JsonObject provider=element.getAsJsonObject();
   if(provider.get("type").getAsString().equals("space")) {
    JsonObject advances=provider.getAsJsonObject("advances");
    if(advances.has("\uEA08")||advances.has("\uEA09"))continue;
   }
   if(provider.has("file")) {
    String file=provider.get("file").getAsString();
    if(file.contains("/hd/")||file.contains("sidebar_frame")||file.contains("/tab_")||file.endsWith("/mascot.png"))continue;
    // Preserve the original ESC logo glyph; TAB gets its own new image.
    if(file.endsWith("/logo.png") && provider.getAsJsonArray("chars").get(0).getAsString().equals("\uEA02"))continue;
   }
   kept.add(provider);
  }
  kept.add(bitmap("mascot.png",64,64,"\uEA01"));
  kept.add(bitmap("tab_logo.png",52,52,"\uEA02"));
  font.add("providers",kept);Files.writeString(definition,font.toString());
  Files.deleteIfExists(Path.of("src/main/resources/frame-layouts.properties"));
  Files.deleteIfExists(Path.of("src/main/resources/frame-advances.properties"));
  Files.deleteIfExists(Path.of("art/frame-layout-preview.png"));
  BufferedImage preview=new BufferedImage(900,360,BufferedImage.TYPE_INT_ARGB);
  Graphics2D g=preview.createGraphics();g.setColor(new Color(32,39,45));g.fillRect(0,0,900,360);
  g.setColor(new Color(0,0,0,140));g.fillRect(45,100,270,210);g.fillRect(400,100,455,210);
  g.drawImage(ImageIO.read(textures.resolve("mascot.png").toFile()),155,65,50,100,null);
  g.drawImage(ImageIO.read(textures.resolve("tab_logo.png").toFile()),585,45,78,78,null);
  g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.PLAIN,18));
  g.drawString("Çevrimiçi: 1/20",75,165);g.drawString("Oyuncu: ArdaAlaca",75,200);
  g.drawString("TILKICRAFT  |  1/20",528,165);g.drawString("ArdaAlaca  |  82 ms",528,215);
  g.dispose();ImageIO.write(preview,"png",Path.of("art/logo-layout-preview.png").toFile());
  Path zip=Path.of("dist/TilkiInterface-ResourcePack.zip");Files.createDirectories(zip.getParent());
  try(var out=new java.util.zip.ZipOutputStream(Files.newOutputStream(zip));var files=Files.walk(ROOT)) {
   for(Path file:files.filter(Files::isRegularFile).sorted().toList()) {
    var entry=new java.util.zip.ZipEntry(ROOT.relativize(file).toString().replace('\\','/'));
    entry.setTime(0);out.putNextEntry(entry);Files.copy(file,out);out.closeEntry();
   }
  }
  String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(zip)));
  Files.writeString(Path.of("dist/resourcepack-sha1.txt"),hash+"\n");
  System.out.println("Logo interface SHA1: "+hash+"; "+Files.size(zip)+" bytes");
 }
}
