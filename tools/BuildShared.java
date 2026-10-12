import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;

/** Image and text helpers shared by the HUD/TAB pack tools (compile together with javac). */
class BuildShared {
    static BufferedImage atlas;

    /** Makes the flat dark background of a reference image transparent (flood fill from the border). */
    static BufferedImage cutout(BufferedImage source) {
        int width = source.getWidth(), height = source.getHeight(), background = source.getRGB(4, 4);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        boolean[] outside = new boolean[width * height];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int x = 0; x < width; x++) { queue.add(new int[]{x, 0}); queue.add(new int[]{x, height - 1}); }
        for (int y = 0; y < height; y++) { queue.add(new int[]{0, y}); queue.add(new int[]{width - 1, y}); }
        while (!queue.isEmpty()) {
            int[] p = queue.removeFirst(); int x = p[0], y = p[1];
            if (x < 0 || y < 0 || x >= width || y >= height || outside[y * width + x]) continue;
            int pixel = source.getRGB(x, y);
            if (Math.max(Math.abs(((pixel >> 16) & 255) - ((background >> 16) & 255)), Math.max(Math.abs(((pixel >> 8) & 255) - ((background >> 8) & 255)), Math.abs((pixel & 255) - (background & 255)))) > 14) continue;
            outside[y * width + x] = true;
            queue.add(new int[]{x + 1, y}); queue.add(new int[]{x - 1, y}); queue.add(new int[]{x, y + 1}); queue.add(new int[]{x, y - 1});
        }
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) if (!outside[y * width + x]) result.setRGB(x, y, source.getRGB(x, y) | 0xff000000);
        return result;
    }

    /** Box-filter downscale with premultiplied alpha. */
    static BufferedImage areaScale(BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        double sx = source.getWidth() / (double)width, sy = source.getHeight() / (double)height;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            double a = 0, r = 0, gr = 0, b = 0, total = 0;
            for (int yy = (int)(y * sy); yy < Math.min(source.getHeight(), Math.ceil((y + 1) * sy)); yy++) {
                double wy = Math.min(yy + 1, (y + 1) * sy) - Math.max(yy, y * sy);
                for (int xx = (int)(x * sx); xx < Math.min(source.getWidth(), Math.ceil((x + 1) * sx)); xx++) {
                    double weight = wy * (Math.min(xx + 1, (x + 1) * sx) - Math.max(xx, x * sx));
                    int pixel = source.getRGB(xx, yy); double alpha = ((pixel >>> 24) / 255.0) * weight;
                    a += alpha; r += ((pixel >> 16) & 255) * alpha; gr += ((pixel >> 8) & 255) * alpha; b += (pixel & 255) * alpha; total += weight;
                }
            }
            int alpha = (int)Math.round(255 * a / total);
            if (alpha < 26) continue;
            result.setRGB(x, y, (alpha << 24) | ((int)Math.round(r / a) << 16) | ((int)Math.round(gr / a) << 8) | (int)Math.round(b / a));
        }
        return result;
    }

    static char base(char c) {
        return switch (c) { case 'ö' -> 'o'; case 'ü' -> 'u'; case 'ş' -> 's'; case 'Ş' -> 'S'; case 'ç' -> 'c'; case 'Ç' -> 'C'; case 'ğ' -> 'g'; case 'İ' -> 'I'; case 'ı' -> 'i'; case '•' -> '*'; case '»' -> '>'; default -> c < 128 ? c : '?'; };
    }

    static int measured(char c) {
        if (c == ' ') return 0;
        int code = base(c), cx = code % 16 * 16, cy = code / 16 * 16;
        for (int x = 15; x >= 0; x--) for (int y = 0; y < 16; y++) if ((atlas.getRGB(cx + x, cy + y) >>> 24) != 0) return x + 1;
        return 0;
    }

    static int advance(char c) { return c == ' ' ? 4 : (int)(0.5 + measured(c) * 0.5) + 1; }

    static int width(String value) { int w = 0; for (char c : value.toCharArray()) w += advance(c); return w; }

    /** Preview text with the pack's 2x ASCII atlas at 'scale' device pixels per GUI pixel. */
    static void drawText(Graphics2D g, String value, int x, int y, int scale, int rgb) {
        double unit = scale / 2.0;
        g.setColor(new Color(rgb));
        for (char c : value.toCharArray()) {
            if (c != ' ') {
                int code = base(c), cx = code % 16 * 16, cy = code / 16 * 16;
                for (int yy = 0; yy < 16; yy++) for (int xx = 0; xx < 16; xx++) {
                    if ((atlas.getRGB(cx + xx, cy + yy) >>> 24) == 0) continue;
                    if (c == 'ı' && yy < 4) continue;
                    g.fillRect(x + (int)(xx * unit), y + (int)(yy * unit), (int)Math.ceil(unit), (int)Math.ceil(unit));
                }
                int width = measured(c);
                if (c == 'ö' || c == 'ü') { g.fillRect(x + (int)(2 * unit), y, (int)(2 * unit), (int)(2 * unit)); g.fillRect(x + (int)((width - 4) * unit), y, (int)(2 * unit), (int)(2 * unit)); }
                if (c == 'İ') g.fillRect(x + (int)((width / 2 - 1) * unit), y - (int)(2 * unit), (int)(2 * unit), (int)(2 * unit));
                if (c == 'ş' || c == 'Ş' || c == 'ç' || c == 'Ç') g.fillRect(x + (int)((width / 2 - 1) * unit), y + (int)(16 * unit), (int)(2 * unit), (int)(2 * unit));
            }
            x += advance(c) * scale;
        }
    }
}
