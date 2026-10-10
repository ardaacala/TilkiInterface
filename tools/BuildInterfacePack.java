import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.*;
import javax.imageio.ImageIO;

class BuildInterfacePack {
    static final Path ROOT=Path.of("resourcepack");
    static BufferedImage logo;
    static void write(String path,String text) throws Exception {
        Path destination=ROOT.resolve(path); Files.createDirectories(destination.getParent()); Files.writeString(destination,text);
    }
    static void png(String path,BufferedImage image) throws Exception {
        Path destination=ROOT.resolve(path); Files.createDirectories(destination.getParent()); ImageIO.write(image,"png",destination.toFile());
    }
    static BufferedImage button(boolean highlight,boolean disabled) {
        BufferedImage image=new BufferedImage(200,20,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();
        g.setColor(new Color(12,9,7)); g.fillRect(0,0,200,20);
        g.setColor(disabled?new Color(72,66,61):highlight?new Color(255,176,62):new Color(168,87,23)); g.fillRect(1,1,198,18);
        g.setPaint(new GradientPaint(0,2,disabled?new Color(44,41,38):highlight?new Color(99,51,22):new Color(49,33,24),0,17,new Color(24,20,18)));
        g.fillRect(2,2,196,16);
        g.setColor(disabled?new Color(68,61,54):new Color(230,123,30)); g.fillRect(3,3,2,14);
        g.setColor(new Color(255,255,255,highlight?35:15)); g.drawLine(6,3,195,3);
        g.dispose(); return image;
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=1) throw new IllegalArgumentException("Original TilkiCraft logo PNG required");
        BufferedImage source=ImageIO.read(Path.of(args[0]).toFile());
        if(source==null) throw new IllegalArgumentException("Invalid PNG");
        logo=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=logo.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double ratio=Math.min(128d/source.getWidth(),128d/source.getHeight());int w=(int)Math.round(source.getWidth()*ratio),h=(int)Math.round(source.getHeight()*ratio);
        g.drawImage(source,(128-w)/2,(128-h)/2,w,h,null);g.dispose();
        // Explicit width anchor preserves predictable advances for transparent logo edges.
        logo.setRGB(127,0,0x01ffffff);
        png("assets/tilkiinterface/textures/font/logo.png",logo);
        write("pack.mcmeta","{\"pack\":{\"pack_format\":46,\"description\":\"TilkiCraft - orange fox interface | 1.21.4\"}}\n");
        write("assets/tilkiinterface/font/ui.json","""
            {"providers":[
              {"type":"bitmap","file":"tilkiinterface:font/logo.png","height":64,"ascent":44,"chars":["\uEA00"]},
              {"type":"bitmap","file":"tilkiinterface:font/logo.png","height":28,"ascent":28,"chars":["\uEA01"]}
            ]}
            """);
        for(String font:new String[]{"default","uniform"})
            write("assets/minecraft/font/"+font+".json","{\"providers\":[{\"type\":\"reference\",\"id\":\"tilkiinterface:ui\"}]}\n");
        // Keep ESC's native controls and their original actions. Only the heading is branded.
        for(String language:new String[]{"tr_tr","en_us","en_gb"})
            write("assets/minecraft/lang/"+language+".json","{\"menu.game\":\"\\uEA00\"}\n");
        String[] states={"button","button_highlighted","button_disabled"};
        for(int i=0;i<states.length;i++) {
            String path="assets/minecraft/textures/gui/sprites/widget/"+states[i]+".png";
            png(path,button(i==1,i==2));
            write(path+".mcmeta","{\"gui\":{\"scaling\":{\"type\":\"nine_slice\",\"width\":200,\"height\":20,\"border\":2}}}\n");
        }
        png("pack.png",logo);
        Files.createDirectories(Path.of("dist"));
        Path zip=Path.of("dist/TilkiInterface-ResourcePack.zip");
        try(var out=new ZipOutputStream(Files.newOutputStream(zip));var files=Files.walk(ROOT)) {
            for(Path file:files.filter(Files::isRegularFile).sorted().toList()) {
                ZipEntry entry=new ZipEntry(ROOT.relativize(file).toString().replace('\\','/'));entry.setTime(0);
                out.putNextEntry(entry);Files.copy(file,out);out.closeEntry();
            }
        }
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(zip)));
        Files.writeString(Path.of("dist/resourcepack-sha1.txt"),hash+"\n");
        preview();System.out.println("Interface pack SHA1: "+hash);
    }
    static void preview() throws Exception {
        BufferedImage image=new BufferedImage(1200,720,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0,0,new Color(73,92,95),0,720,new Color(21,27,28)));g.fillRect(0,0,1200,720);
        g.setColor(new Color(0,0,0,100));g.fillRect(0,0,1200,720);
        g.setColor(new Color(220,139,51));g.setFont(new Font("SansSerif",Font.BOLD,14));g.drawString("TILKICRAFT • TASARIM ÖNİZLEMESİ (oyun ekran görüntüsü değildir)",24,30);
        g.drawImage(logo,380,53,128,128,null);
        String[] labels={"Oyuna Dön","Gelişimler       |       İstatistikler","Geri Bildirim     |     Hata Bildir","Seçenekler       |       Dünyayı Paylaş","Bağlantıyı Kes"};
        g.setFont(new Font("SansSerif",Font.PLAIN,17));
        for(int i=0;i<labels.length;i++) {
            int y=195+i*52;g.drawImage(button(i==0,false),244,y,400,40,null);g.setColor(i==0?new Color(255,195,99):Color.WHITE);
            int textWidth=g.getFontMetrics().stringWidth(labels[i]);g.drawString(labels[i],444-textWidth/2,y+26);
        }
        g.setColor(new Color(15,12,10,210));g.fillRoundRect(855,180,310,420,8,8);
        g.drawImage(logo,957,92,106,106,null);
        g.setColor(new Color(232,132,40));g.fillRect(855,200,3,380);
        String[] rows={"10/10/26 17:30","","Çevrimiçi    24/100","Ping           35 ms","","Oyuncu       Arda","Rütbe          Oyuncu","Bakiye        1.4M","Seviye        118","","Öldürme     24","Ölüm           6","Süre            12sa 40dk","","TilkiCraft • Skyblock"};
        g.setFont(new Font("SansSerif",Font.PLAIN,16));
        for(int i=0;i<rows.length;i++) {g.setColor(i==0?new Color(159,145,128):i==14?new Color(255,164,68):Color.WHITE);g.drawString(rows[i],880,220+i*25);}
        g.setColor(new Color(209,120,31));g.fillRoundRect(312,55,500,5,4,4);g.setColor(Color.WHITE);g.drawString("◆ 1.4M  |  Oyuncu  |  TilkiCraft  |  24/100",365,48);
        g.dispose();Files.createDirectories(Path.of("art"));ImageIO.write(image,"png",Path.of("art/interface-preview.png").toFile());
    }
}
