import com.google.gson.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Tiles HD artwork into atlas-safe glyphs; layouts retain logical screen sizes. */
class BuildHdAssets {
 static final Path ROOT=Path.of("resourcepack"), TEX=ROOT.resolve("assets/tilkiinterface/textures/font/hd");
 static final JsonArray providers=new JsonArray(); static final Properties layouts=new Properties();
 static final Map<String,String> glyphs=new HashMap<>(); static int next=0xf0000;
 static final Properties advances=new Properties();
 static BufferedImage master;
 static String space(int amount) { return String.valueOf((char)(0xee40+amount)); }
 static BufferedImage frame(int w,int h,boolean bottomOnly) {
  BufferedImage result=new BufferedImage(w*4,h*4,BufferedImage.TYPE_INT_ARGB);Graphics2D g=result.createGraphics();
  g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
  int c=master.getWidth()/7, d=32, mw=master.getWidth(),mh=master.getHeight();
  // Nine-slice corners and rails, keeping the center fully transparent.
  for(int x=0;x<3;x++)for(int y=0;y<3;y++) {
   if(x==1&&y==1)continue;
   int sx=x==0?0:x==1?c:mw-c, sy=y==0?0:y==1?c:mh-c;
   int sw=x==1?mw-2*c:c,sh=y==1?mh-2*c:c;
   int dx=x==0?0:x==1?d:w*4-d,dy=y==0?0:y==1?d:h*4-d;
   int dw=x==1?w*4-2*d:d,dh=y==1?h*4-2*d:d;
   if(bottomOnly && y!=2)continue;
   g.drawImage(master,dx,dy,dx+dw,dy+dh,sx,sy,sx+sw,sy+sh,null);
  }
  g.dispose();return result;
 }
 static String layout(BufferedImage image,boolean removeBottom) throws Exception {
  int width=image.getWidth()/4,height=image.getHeight()/4;StringBuilder text=new StringBuilder();
  for(int y=0;y<height;y+=32) {
   for(int x=0;x<width;x+=32) {
    int w=Math.min(32,width-x),h=Math.min(32,height-y);boolean visible=false;
    BufferedImage tile=new BufferedImage(w*4,h*4,BufferedImage.TYPE_INT_ARGB);
    for(int yy=0;yy<h*4;yy++)for(int xx=0;xx<w*4;xx++) {
     int pixel=image.getRGB(x*4+xx,y*4+yy);
     if(removeBottom && y*4+yy>=(height-8)*4)pixel=0;
     tile.setRGB(xx,yy,pixel);visible|=(pixel>>>24)!=0;
    }
    if(!visible) { text.append(space(w));continue; }
    tile.setRGB(w*4-1,0,0x01ffffff); // fixed advance, even with transparent edges
    var digest=MessageDigest.getInstance("SHA-256");
    for(int pixel:tile.getRGB(0,0,w*4,h*4,null,0,w*4))digest.update(java.nio.ByteBuffer.allocate(4).putInt(pixel).array());
    String key=HexFormat.of().formatHex(digest.digest())+":"+h+":"+y;
    String character=glyphs.get(key);
    if(character==null) {
     character=new String(Character.toChars(next++));glyphs.put(key,character);String name="tile_"+(next-1)+".png";ImageIO.write(tile,"png",TEX.resolve(name).toFile());
     advances.setProperty(Integer.toHexString(next-1),""+(w+1));
     JsonObject provider=new JsonObject();provider.addProperty("type","bitmap");provider.addProperty("file","tilkiinterface:font/hd/"+name);provider.addProperty("height",h);provider.addProperty("ascent",7-y);
     JsonArray chars=new JsonArray();chars.add(character);provider.add("chars",chars);providers.add(provider);
    }
    text.append(character).append('\uEA08');
   }
   if(y+32<height)text.append(rewind(width));
  }
  return text.append('\uEA09').toString();
 }
 static String rewind(int width) { return String.valueOf((char)(0xee00+width/16)); }
 public static void main(String[] args) throws Exception {
  master=ImageIO.read(Path.of(args[0]).toFile());Files.createDirectories(TEX);
  for(int width:new int[]{128,144,160,192}) layouts.setProperty("hud."+width,layout(frame(width,136,false),false));
  for(int variant=0;variant<10;variant++) {
   int width=192+64*variant;
   for(int rows=1;rows<=20;rows++)layouts.setProperty("tab."+variant+"."+rows,layout(frame(width,64+rows*9,false),true));
   // Bottom rail is a shallow texture with its own corners, no extra visible panel.
   BufferedImage full=frame(width,16,false);BufferedImage bottom=full.getSubimage(0,32,width*4,32);
   layouts.setProperty("bottom."+variant,layout(bottom,false));
  }
  JsonObject spacing=new JsonObject();spacing.addProperty("\uEA08",-1);spacing.addProperty("\uEA09",1);spacing.addProperty("\uEA07",10);
  for(int i=0;i<=192;i++)spacing.addProperty(space(i),i);
  for(int w=128;w<=768;w+=16)spacing.addProperty(rewind(w),-w);
  JsonObject spaces=new JsonObject();spaces.addProperty("type","space");spaces.add("advances",spacing);providers.add(spaces);
  for(String character:spacing.keySet()) advances.setProperty(Integer.toHexString(character.codePointAt(0)),spacing.get(character).getAsString());
  Path font=ROOT.resolve("assets/tilkiinterface/font/ui.json");JsonObject current=JsonParser.parseString(Files.readString(font)).getAsJsonObject();JsonArray kept=new JsonArray();
  for(JsonElement element:current.getAsJsonArray("providers")) {
   JsonObject p=element.getAsJsonObject();String file=p.has("file")?p.get("file").getAsString():"";
   if(file.contains("sidebar_frame")||file.contains("/tab_"))continue;
   if(p.get("type").getAsString().equals("space")) {
    for(String key:new ArrayList<>(p.getAsJsonObject("advances").keySet())) if(spacing.has(key))p.getAsJsonObject("advances").remove(key);
   }
   kept.add(p);
  }
  kept.addAll(providers);current.add("providers",kept);Files.writeString(font,current.toString());
  Files.createDirectories(Path.of("src/main/resources"));try(var out=Files.newOutputStream(Path.of("src/main/resources/frame-layouts.properties"))) { layouts.store(out,"HD atlas-safe frame layouts"); }
  try(var out=Files.newOutputStream(Path.of("src/main/resources/frame-advances.properties"))) { advances.store(out,"Logical advances for layout validation"); }
  for(String style:java.util.List.of("modern","classic","fantasy")) {
   String definition="{\"providers\":[{\"type\":\"space\",\"advances\":{\" \":4}},{\"type\":\"ttf\",\"file\":\"tilkiinterface:fonts/"+style+".ttf\",\"size\":9,\"oversample\":4,\"shift\":[0,0]}]}";
   Files.writeString(ROOT.resolve("assets/tilkiinterface/font/text_"+style+".json"),definition);
  }
  for(String name:java.util.List.of("default","uniform"))Files.writeString(ROOT.resolve("assets/minecraft/font/"+name+".json"),"{\"providers\":[{\"type\":\"reference\",\"id\":\"tilkiinterface:ui\"},{\"type\":\"reference\",\"id\":\"tilkiinterface:text_modern\"}]}");
  Path zip=Path.of("dist/TilkiInterface-ResourcePack.zip");
  try(var out=new java.util.zip.ZipOutputStream(Files.newOutputStream(zip));var walk=Files.walk(ROOT)) { for(Path file:walk.filter(Files::isRegularFile).sorted().toList()) {var entry=new java.util.zip.ZipEntry(ROOT.relativize(file).toString().replace('\\','/'));entry.setTime(0);out.putNextEntry(entry);Files.copy(file,out);out.closeEntry();} }
  String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(zip)));Files.writeString(Path.of("dist/resourcepack-sha1.txt"),hash+"\n");
  System.out.println("HD interface SHA1: "+hash);
  System.out.println("HD tiles: "+glyphs.size()+"; layouts: "+layouts.size());
 }
}
