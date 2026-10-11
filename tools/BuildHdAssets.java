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
 static final Map<String,String> textures=new HashMap<>();
 static final Properties advances=new Properties();
 static BufferedImage master;
 // Artwork covers the client's dark panel margin without increasing text advance.
 static final int FRAME_OVERHANG=16, TAB_TOP_EXTENSION=48;
 static String space(int amount) { return String.valueOf((char)(0xee40+amount)); }
 static String framed(BufferedImage image,int verticalOffset) throws Exception {
  return rewind(FRAME_OVERHANG)+layout(image,verticalOffset)+rewind(FRAME_OVERHANG);
 }
 static BufferedImage frame(int w,int h,boolean bottomOnly) {
  BufferedImage result=new BufferedImage(w*2,h*2,BufferedImage.TYPE_INT_ARGB);Graphics2D g=result.createGraphics();
  g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
  int c=320, d=(bottomOnly?48:32)*2, mw=master.getWidth(),mh=master.getHeight();
  // Nine-slice corners and rails, keeping the center fully transparent.
  for(int x=0;x<3;x++)for(int y=0;y<3;y++) {
   if(x==1&&y==1)continue;
   int sx=x==0?0:x==1?c:mw-c, sy=y==0?0:y==1?c:mh-c;
   int sw=x==1?mw-2*c:c,sh=y==1?mh-2*c:c;
   int dx=x==0?0:x==1?d:w*2-d,dy=y==0?0:y==1?d:h*2-d;
   int dw=x==1?w*2-2*d:d,dh=y==1?h*2-2*d:d;

   if(x==1) {
    int count=bottomOnly?4:(dw<=384?3:4);
    for(int segment=0;segment<count;segment++) {
     int left=dx+dw*segment/count,right=dx+dw*(segment+1)/count;
     g.drawImage(master,left,dy,right,dy+dh,sx,sy,sx+sw,sy+sh,null);
    }
   } else if(y==1) {
    int count=Math.max(2,(dh+63)/64-3);
    for(int segment=0;segment<count;segment++) {
     int top=dy+dh*segment/count,bottom=dy+dh*(segment+1)/count;
     g.drawImage(master,dx,top,dx+dw,bottom,sx,sy,sx+sw,sy+sh,null);
    }
   } else g.drawImage(master,dx,dy,dx+dw,dy+dh,sx,sy,sx+sw,sy+sh,null);
  }
  g.dispose();return result;
 }
 static String layout(BufferedImage image,int verticalOffset) throws Exception {
  int width=image.getWidth()/2,height=image.getHeight()/2;StringBuilder text=new StringBuilder();
  for(int y=0;y<height;y+=64) {
   for(int x=0;x<width;x+=64) {
    int w=Math.min(64,width-x),h=Math.min(64,height-y);boolean visible=false;
    BufferedImage tile=new BufferedImage(w*2,h*2,BufferedImage.TYPE_INT_ARGB);
    for(int yy=0;yy<h*2;yy++)for(int xx=0;xx<w*2;xx++) {
     int pixel=image.getRGB(x*2+xx,y*2+yy);
     tile.setRGB(xx,yy,pixel);visible|=(pixel>>>24)!=0;
    }
    if(!visible) { text.append(space(w));continue; }
    tile.setRGB(w*2-1,0,0x01ffffff); // fixed advance, even with transparent edges
    var digest=MessageDigest.getInstance("SHA-256");
    for(int pixel:tile.getRGB(0,0,w*2,h*2,null,0,w*2))digest.update(java.nio.ByteBuffer.allocate(4).putInt(pixel).array());
    String pixels=HexFormat.of().formatHex(digest.digest());
    String key=pixels+":"+h+":"+(y+verticalOffset);
    String character=glyphs.get(key);
    if(character==null) {
     character=new String(Character.toChars(next++));glyphs.put(key,character);
     String name=textures.get(pixels);
     if(name==null) { name="tile_"+textures.size()+".png";textures.put(pixels,name);ImageIO.write(tile,"png",TEX.resolve(name).toFile()); }
     advances.setProperty(Integer.toHexString(next-1),""+(w+1));
     JsonObject provider=new JsonObject();provider.addProperty("type","bitmap");provider.addProperty("file","tilkiinterface:font/hd/"+name);provider.addProperty("height",h);provider.addProperty("ascent",7-y-verticalOffset);
     JsonArray chars=new JsonArray();chars.add(character);provider.add("chars",chars);providers.add(provider);
    }
    text.append(character).append('\uEA08');
   }
   if(y+64<height)text.append(rewind(width));
  }
  return text.append('\uEA09').toString();
 }
 static String rewind(int width) { return String.valueOf((char)(0xee00+width/16)); }
 public static void main(String[] args) throws Exception {
  master=ImageIO.read(Path.of(args[0]).toFile());Files.createDirectories(TEX);
  Path mascotPath=ROOT.resolve("assets/tilkiinterface/textures/font/mascot.png");
  BufferedImage mascot=ImageIO.read(mascotPath.toFile());
  BufferedImage raised=new BufferedImage(128,256,BufferedImage.TYPE_INT_ARGB);
  Graphics2D mg=raised.createGraphics();mg.drawImage(mascot,0,0,128,128,0,0,mascot.getWidth(),Math.min(mascot.getHeight(),mascot.getWidth()),null);mg.dispose();
  ImageIO.write(raised,"png",mascotPath.toFile());
  try(var old=Files.list(TEX)) { for(Path file:old.filter(p->p.getFileName().toString().matches("tile_[0-9]+\\.png")).toList()) Files.delete(file); }
  for(int width:new int[]{192,208,224,256}) layouts.setProperty("hud."+width,framed(frame(width+FRAME_OVERHANG*2,188,false),-28));
  for(int variant=0;variant<10;variant++) {
   int width=192+64*variant;
   for(int rows=1;rows<=20;rows++)layouts.setProperty("tab."+variant+"."+rows,
     framed(frame(width+FRAME_OVERHANG*2,168+rows*9+TAB_TOP_EXTENSION,true),-TAB_TOP_EXTENSION));
   layouts.setProperty("bottom."+variant,""); // The complete frame includes its bottom rail.
  }
  JsonObject spacing=new JsonObject();spacing.addProperty("\uEA08",-1);spacing.addProperty("\uEA09",1);spacing.addProperty("\uEA07",36);
  spacing.addProperty(rewind(FRAME_OVERHANG),-FRAME_OVERHANG);
  for(int i=0;i<=192;i++)spacing.addProperty(space(i),i);
  for(int w=128;w<=768+FRAME_OVERHANG*2;w+=16)spacing.addProperty(rewind(w),-w);
  JsonObject spaces=new JsonObject();spaces.addProperty("type","space");spaces.add("advances",spacing);providers.add(spaces);
  for(String character:spacing.keySet()) advances.setProperty(Integer.toHexString(character.codePointAt(0)),spacing.get(character).getAsString());
  Path font=ROOT.resolve("assets/tilkiinterface/font/ui.json");JsonObject current=JsonParser.parseString(Files.readString(font)).getAsJsonObject();JsonArray kept=new JsonArray();
  for(JsonElement element:current.getAsJsonArray("providers")) {
   JsonObject p=element.getAsJsonObject();String file=p.has("file")?p.get("file").getAsString():"";
   if(file.contains("sidebar_frame")||file.contains("/tab_")||file.contains("/hd/"))continue;
   if(file.endsWith("/mascot.png")) { p.addProperty("height",56);p.addProperty("ascent",56); }
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
  BufferedImage preview=new BufferedImage(1152,820,BufferedImage.TYPE_INT_ARGB);Graphics2D g=preview.createGraphics();
  g.setColor(new Color(25,24,22));g.fillRect(0,0,1152,820);
  g.drawImage(frame(256+FRAME_OVERHANG*2,188,false),0,20,576,376,null);
  g.drawImage(frame(320+FRAME_OVERHANG*2,159+TAB_TOP_EXTENSION,true),512,20,704,414,null);
  g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.PLAIN,16));
  g.drawString("HUD: 32 px corners / 36 px text inset",74,110);
  g.drawString("TAB: 48 px corners",660,140);
  g.drawString("Title and player list have reserved space",620,180);
  g.dispose();ImageIO.write(preview,"png",Path.of("art/frame-layout-preview.png").toFile());
 }
}
