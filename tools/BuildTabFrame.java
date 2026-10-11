import com.google.gson.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** Builds sparse, player-count-aware TAB frames from the ESC artwork. */
class BuildTabFrame {
    static final Path ROOT=Path.of("resourcepack");
    static final Path TEX=ROOT.resolve("assets/tilkiinterface/textures/font/tab_frame");
    static final JsonArray PROVIDERS=new JsonArray();
    static final Properties LAYOUTS=new Properties();
    static int next=0xf1000, textureIndex=0;

    static void patch(Graphics2D g,BufferedImage source,int dx1,int dy1,int dx2,int dy2,int sx1,int sy1,int sx2,int sy2) {
        g.drawImage(source,dx1,dy1,dx2,dy2,sx1,sy1,sx2,sy2,null);
    }
    static BufferedImage compose(BufferedImage source,BufferedImage logo,int width,int rows) {
        int height=76+rows*9, smx=190, smy=220, dmx=28, dmy=38;
        BufferedImage out=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int sw=source.getWidth(),sh=source.getHeight();
        patch(g,source,0,0,dmx,dmy,0,0,smx,smy);
        patch(g,source,dmx,0,width-dmx,dmy,smx,0,sw-smx,smy);
        patch(g,source,width-dmx,0,width,dmy,sw-smx,0,sw,smy);
        patch(g,source,0,dmy,dmx,height-dmy,0,smy,smx,sh-smy);
        patch(g,source,width-dmx,dmy,width,height-dmy,sw-smx,smy,sw,sh-smy);
        patch(g,source,0,height-dmy,dmx,height,0,sh-smy,smx,sh);
        patch(g,source,dmx,height-dmy,width-dmx,height,smx,sh-smy,sw-smx,sh);
        patch(g,source,width-dmx,height-dmy,width,height,sw-smx,sh-smy,sw,sh);
        g.setComposite(AlphaComposite.Clear);g.fillRect(dmx,dmy,width-dmx*2,height-dmy*2);g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(logo,(width-58)/2,-2,58,58,null);
        g.dispose();
        // A compact pixel palette keeps the single resource-pack download small.
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            int pixel=out.getRGB(x,y),a=pixel>>>24;
            if(a<32)out.setRGB(x,y,0);
            else {int r=(pixel>>16)&255,green=(pixel>>8)&255,b=pixel&255;out.setRGB(x,y,0xff000000|((r>>4)<<20)|((green>>4)<<12)|((b>>4)<<4));}
        }
        return out;
    }
    static String addLayout(BufferedImage image,String key) throws Exception {
        StringBuilder text=new StringBuilder();
        for(int x=0;x<image.getWidth();x+=256) {
            int width=Math.min(256,image.getWidth()-x);
            BufferedImage tile=image.getSubimage(x,0,width,image.getHeight());
            BufferedImage copy=new BufferedImage(width,image.getHeight(),BufferedImage.TYPE_INT_ARGB);
            copy.getGraphics().drawImage(tile,0,0,null);copy.setRGB(width-1,0,0x01ffffff);
            String file="tab_frame/frame_"+(textureIndex++)+".png";ImageIO.write(copy,"png",ROOT.resolve("assets/tilkiinterface/textures/font").resolve(file).toFile());
            String glyph=new String(Character.toChars(next++));
            JsonObject provider=new JsonObject();provider.addProperty("type","bitmap");provider.addProperty("file","tilkiinterface:font/"+file);
            provider.addProperty("height",copy.getHeight());provider.addProperty("ascent",52);
            JsonArray chars=new JsonArray();chars.add(glyph);provider.add("chars",chars);PROVIDERS.add(provider);
            text.append(glyph).append('\uEA08');
        }
        LAYOUTS.setProperty(key,text.toString());return text.toString();
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("Frame source and TilkiCraft logo PNG required");
        BufferedImage source=ImageIO.read(Path.of(args[0]).toFile()),logo=ImageIO.read(Path.of(args[1]).toFile());
        if(source==null||logo==null)throw new IllegalArgumentException("Invalid PNG");Files.createDirectories(TEX);
        try(var old=Files.list(TEX)){for(Path file:old.toList())Files.delete(file);}
        for(int players=1;players<=80;players++) {
            int columns=(players+19)/20,rows=(players+columns-1)/columns,width=128+columns*128;
            String key=columns+"."+rows;if(!LAYOUTS.containsKey(key))addLayout(compose(source,logo,width,rows),key);
        }
        Path definition=ROOT.resolve("assets/tilkiinterface/font/ui.json");JsonObject font=JsonParser.parseString(Files.readString(definition)).getAsJsonObject();JsonArray kept=new JsonArray();
        for(JsonElement element:font.getAsJsonArray("providers")) {
            JsonObject provider=element.getAsJsonObject();String file=provider.has("file")?provider.get("file").getAsString():"";
            if(!file.contains("/tab_frame/"))kept.add(provider);
        }
        kept.addAll(PROVIDERS);font.add("providers",kept);Files.writeString(definition,font.toString());
        try(var output=Files.newOutputStream(Path.of("src/main/resources/tab-frame-layouts.properties"))){LAYOUTS.store(output,"TAB frame layouts by columns and rows");}
        Path zip=Path.of("dist/TilkiInterface-ResourcePack.zip");
        try(var out=new ZipOutputStream(Files.newOutputStream(zip));var files=Files.walk(ROOT)){for(Path file:files.filter(Files::isRegularFile).sorted().toList()){ZipEntry entry=new ZipEntry(ROOT.relativize(file).toString().replace('\\','/'));entry.setTime(0);out.putNextEntry(entry);Files.copy(file,out);out.closeEntry();}}
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(zip)));Files.writeString(Path.of("dist/resourcepack-sha1.txt"),hash+"\n");
        BufferedImage preview=compose(source,logo,384,12);ImageIO.write(preview,"png",Path.of("art/tab-frame-preview.png").toFile());
        System.out.println("TAB layouts: "+LAYOUTS.size()+"; textures: "+textureIndex+"; SHA1: "+hash+"; "+Files.size(zip)+" bytes");
    }
}
