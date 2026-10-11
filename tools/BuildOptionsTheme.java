import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Applies the same wood/orange theme to controls used by the options screens. */
class BuildOptionsTheme {
    static final Path ROOT=Path.of("resourcepack/assets/minecraft/textures/gui/sprites/widget");
    static BufferedImage panel(int width,int height,boolean highlighted,boolean disabled) {
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        g.setColor(new Color(15,9,5));g.fillRect(0,0,width,height);
        g.setColor(disabled?new Color(93,72,55):highlighted?new Color(255,178,55):new Color(203,91,18));g.fillRect(1,1,width-2,height-2);
        g.setColor(disabled?new Color(45,37,31):new Color(45,24,13));g.fillRect(3,3,width-6,height-6);
        g.setColor(new Color(104,51,20));for(int y=5;y<height-4;y+=4)g.drawLine(5,y,width-6,y);
        g.setColor(new Color(255,202,88,highlighted?220:120));g.drawLine(4,3,width-5,3);g.dispose();return image;
    }
    static BufferedImage handle(boolean highlighted) {
        BufferedImage image=panel(8,20,highlighted,false);Graphics2D g=image.createGraphics();g.setColor(new Color(255,197,70));g.fillRect(3,5,2,10);g.dispose();return image;
    }
    static BufferedImage checkbox(boolean selected,boolean highlighted) {
        BufferedImage image=panel(20,20,highlighted,false);if(selected){Graphics2D g=image.createGraphics();g.setStroke(new BasicStroke(3));g.setColor(new Color(255,184,50));g.drawLine(5,10,9,14);g.drawLine(9,14,16,5);g.dispose();}return image;
    }
    static void write(String name,BufferedImage image)throws Exception{ImageIO.write(image,"png",ROOT.resolve(name+".png").toFile());}
    static void metadata(String name,int width,int height,int border)throws Exception{Files.writeString(ROOT.resolve(name+".png.mcmeta"),"{\"gui\":{\"scaling\":{\"type\":\"nine_slice\",\"width\":"+width+",\"height\":"+height+",\"border\":"+border+"}}}\n");}
    public static void main(String[] args)throws Exception{
        Files.createDirectories(ROOT);
        write("slider",panel(200,20,false,false));write("slider_highlighted",panel(200,20,true,false));metadata("slider",200,20,1);metadata("slider_highlighted",200,20,1);
        write("slider_handle",handle(false));write("slider_handle_highlighted",handle(true));metadata("slider_handle",8,20,2);metadata("slider_handle_highlighted",8,20,2);
        write("checkbox",checkbox(false,false));write("checkbox_highlighted",checkbox(false,true));write("checkbox_selected",checkbox(true,false));write("checkbox_selected_highlighted",checkbox(true,true));
        write("text_field",panel(200,20,false,false));write("text_field_highlighted",panel(200,20,true,false));metadata("text_field",200,20,2);metadata("text_field_highlighted",200,20,2);
        System.out.println("Themed sliders, handles, checkboxes and text fields.");
    }
}
