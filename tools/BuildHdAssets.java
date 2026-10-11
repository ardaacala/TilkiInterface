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
 static BufferedImage hudLogo,tabLogo;
 // Artwork covers the client's dark panel margin without increasing text advance.
 static final int FRAME_OVERHANG=16, TAB_TOP_EXTENSION=48;
 static String space(int amount) { return String.valueOf((char)(0xee40+amount)); }
 static String framed(BufferedImage image,int verticalOffset) throws Exception {
  return rewind(FRAME_OVERHANG)+layout(image,verticalOffset)+rewind(FRAME_OVERHANG);
 }
 static BufferedImage frame(int w,int h,boolean tab) {
  BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
  g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
  // Continuous rails keep a growing player list visually calm.
  g.setColor(new Color(17,13,15,215));
  g.fillRect(2,3,5,h-6);g.fillRect(w-7,3,5,h-6);
  g.fillRect(3,2,w-6,5);g.fillRect(3,h-7,w-6,5);
  g.setColor(new Color(255,138,31,235));
  g.fillRect(3,3,w-6,2);g.fillRect(3,h-5,w-6,2);
  g.fillRect(3,4,2,h-8);g.fillRect(w-5,4,2,h-8);
  g.setColor(new Color(255,207,91,205));
  g.fillRect(6,6,w-12,1);g.fillRect(6,h-7,w-12,1);
  g.fillRect(6,7,1,h-14);g.fillRect(w-7,7,1,h-14);
  g.setColor(new Color(70,36,28,170));
  g.fillRect(8,8,w-16,1);g.fillRect(8,h-9,w-16,1);
  g.fillRect(8,9,1,h-18);g.fillRect(w-9,9,1,h-18);
  BufferedImage logo=tab?tabLogo:hudLogo;
  int size=tab?62:46,left=(w-size)/2,crestHeight=tab?36:30;
  g.setColor(new Color(17,13,15,235));g.fillRect(left-5,2,size+10,crestHeight);
  g.setColor(new Color(255,138,31));
  g.fillRect(left-5,2,size+10,2);
  g.fillRect(left-5,2,2,crestHeight);g.fillRect(left+size+3,2,2,crestHeight);
  g.setColor(new Color(255,207,91));g.fillRect(left-2,5,size+4,1);
  g.drawImage(logo,left,tab?0:-1,size,size,null);
  g.setColor(new Color(255,174,51));
  for(int x:new int[]{3,w-11})for(int y:new int[]{3,h-11}) {
   g.fillRect(x,y,8,2);g.fillRect(x,y,2,8);
  }
  g.setColor(new Color(255,207,91,190));
  g.fillRect(3,h/2-3,4,6);g.fillRect(w-7,h/2-3,4,6);
  g.dispose();return image;
 }
 static String layout(BufferedImage image,int verticalOffset) throws Exception {
  int width=image.getWidth(),height=image.getHeight();StringBuilder text=new StringBuilder();
  for(int y=0;y<height;y+=64) {
   for(int x=0;x<width;x+=64) {
    int w=Math.min(64,width-x),h=Math.min(64,height-y);boolean visible=false;
    BufferedImage tile=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
    for(int yy=0;yy<h;yy++)for(int xx=0;xx<w;xx++) {
     int pixel=image.getRGB(x+xx,y+yy);
     tile.setRGB(xx,yy,pixel);visible|=(pixel>>>24)!=0;
    }
    if(!visible) { text.append(space(w));continue; }
    tile.setRGB(w-1,0,0x01ffffff); // fixed advance, even with transparent edges
    var digest=MessageDigest.getInstance("SHA-256");
    for(int pixel:tile.getRGB(0,0,w,h,null,0,w))digest.update(java.nio.ByteBuffer.allocate(4).putInt(pixel).array());
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
  hudLogo=ImageIO.read(Path.of(args[0]).toFile());tabLogo=ImageIO.read(Path.of(args[1]).toFile());Files.createDirectories(TEX);
  Path mascotPath=ROOT.resolve("assets/tilkiinterface/textures/font/mascot.png");
  BufferedImage mascot=ImageIO.read(mascotPath.toFile());
  BufferedImage raised=new BufferedImage(128,256,BufferedImage.TYPE_INT_ARGB);
  Graphics2D mg=raised.createGraphics();mg.drawImage(mascot,0,0,128,128,0,0,mascot.getWidth(),Math.min(mascot.getHeight(),mascot.getWidth()),null);mg.dispose();
  ImageIO.write(raised,"png",mascotPath.toFile());
  Path legacy=ROOT.resolve("assets/tilkiinterface/textures/font");
  try(var old=Files.list(legacy)) {
   for(Path file:old.filter(p->p.getFileName().toString().matches("(sidebar_frame.*|tab_.*)\\.png")).toList())Files.delete(file);
  }
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
    if(p.getAsJsonObject("advances").size()==0)continue;
   }
   kept.add(p);
  }
  kept.addAll(providers);current.add("providers",kept);Files.writeString(font,current.toString());
  Files.createDirectories(Path.of("src/main/resources"));try(var out=Files.newOutputStream(Path.of("src/main/resources/frame-layouts.properties"))) { layouts.store(out,"HD atlas-safe frame layouts"); }
  try(var out=Files.newOutputStream(Path.of("src/main/resources/frame-advances.properties"))) { advances.store(out,"Logical advances for layout validation"); }
  for(String name:java.util.List.of("default","uniform"))Files.writeString(ROOT.resolve("assets/minecraft/font/"+name+".json"),"{\"providers\":[{\"type\":\"reference\",\"id\":\"tilkiinterface:ui\"}]}");
  Path zip=Path.of("dist/TilkiInterface-ResourcePack.zip");
  try(var out=new java.util.zip.ZipOutputStream(Files.newOutputStream(zip));var walk=Files.walk(ROOT)) { for(Path file:walk.filter(Files::isRegularFile).sorted().toList()) {var entry=new java.util.zip.ZipEntry(ROOT.relativize(file).toString().replace('\\','/'));entry.setTime(0);out.putNextEntry(entry);Files.copy(file,out);out.closeEntry();} }
  String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(zip)));Files.writeString(Path.of("dist/resourcepack-sha1.txt"),hash+"\n");
  System.out.println("HD interface SHA1: "+hash);
  System.out.println("HD tiles: "+glyphs.size()+"; layouts: "+layouts.size());
  BufferedImage preview=new BufferedImage(1152,820,BufferedImage.TYPE_INT_ARGB);Graphics2D g=preview.createGraphics();
  g.setColor(new Color(25,24,22));g.fillRect(0,0,1152,820);
  g.drawImage(frame(256+FRAME_OVERHANG*2,188,false),40,100,472,382,null);
  g.drawImage(frame(320+FRAME_OVERHANG*2,159+TAB_TOP_EXTENSION,true),560,100,528,311,null);
  g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.PLAIN,16));
  g.drawString("HUD",52,80);g.drawString("TAB player list",572,80);
  g.drawString("Date and server data",105,245);
  g.drawString("Players",785,245);
  g.dispose();ImageIO.write(preview,"png",Path.of("art/frame-layout-preview.png").toFile());
 }
}
