package com.tilkicraft.ui;
import java.util.Locale;

public final class UiText {
    private UiText() {}
    public static int tabNameWidth(String name) {
        name=org.bukkit.ChatColor.stripColor(name);
        if(name==null)return 0;
        int width=0;
        for(int c:name.codePoints().toArray()) {
            if(c>=0xeb00 && c<=0xeb10) { width+=new int[]{55,51,40,48,54,52,34,48,46,43,46,64,33,37,56,61,36}[c-0xeb00];continue; }
            if(c>=0xea20 && c<=0xea26) { width+=9;continue; }
            if(c==' ')width+=4;
            else if(".,:;!|i".indexOf(c)>=0)width+=2;
            else if("ltI'".indexOf(c)>=0)width+=4;
            else width+=6;
        }
        return width;
    }
    /** TilkiTags' fixed TAB name width, or 0 when TilkiTags (with the TAB padding) is missing. */
    public static int fixedTabNameWidth(Object tags) {
        if (tags == null) return 0;
        try { return (int)tags.getClass().getMethod("getTabNameWidth").invoke(tags); }
        catch (ReflectiveOperationException | ClassCastException ex) { return 0; }
    }
    /** "&#RRGGBB" to the §x§R§R§G§G§B§B form that Spigot sends as a hex colour. */
    public static String hexColors(String text) {
        if (text == null) return null;
        return java.util.regex.Pattern.compile("&#([0-9a-fA-F]{6})").matcher(text).replaceAll(match -> {
            StringBuilder out = new StringBuilder("§x");
            for (char c : match.group(1).toCharArray()) out.append('§').append(c);
            return out.toString();
        });
    }
    /**
     * Client width of a formatted line: pack glyphs from the layout (sidebar icons), the
     * rest measured by TilkiTags (its widths match the TilkiCraft font) or estimated.
     */
    public static int width(String text, HudLayout layout, Object tags) {
        StringBuilder rest = new StringBuilder();
        int glyphs = 0;
        for (char c : text.toCharArray()) {
            if (layout != null && layout.has("advance." + (int)c)) glyphs += layout.number("advance." + (int)c);
            else rest.append(c);
        }
        if (tags != null) try { return glyphs + (int)tags.getClass().getMethod("getTextWidth", String.class).invoke(tags, rest.toString()); }
        catch (ReflectiveOperationException | ClassCastException ignored) { }
        return glyphs + tabNameWidth(rest.toString());
    }
    /** Cuts a formatted line to the given width, ending with "..". */
    public static String fit(String text, int pixels, java.util.function.ToIntFunction<String> measure) {
        if (measure.applyAsInt(text) <= pixels) return text;
        StringBuilder fitted = new StringBuilder();
        for (int i = 0; i < text.length();) {
            if (text.charAt(i) == '§' && i + 1 < text.length()) { fitted.append(text, i, i + 2); i += 2; continue; }
            int cp = text.codePointAt(i); String next = new String(Character.toChars(cp));
            if (measure.applyAsInt(fitted + next + "§r..") > pixels) break;
            fitted.append(next); i += Character.charCount(cp);
        }
        return fitted + "§r..";
    }
    public static String pixelSpace(int pixels) { return String.valueOf((char)(0xee40+Math.max(0,Math.min(192,pixels)))); }
    public static String fit(String text,int pixels) {
        if(tabNameWidth(text)<=pixels)return text;
        StringBuilder fitted=new StringBuilder();
        for(int i=0;i<text.length();) {
            if(text.charAt(i)=='§' && i+1<text.length()) { fitted.append(text,i,i+2);i+=2;continue; }
            int cp=text.codePointAt(i);String next=new String(Character.toChars(cp));
            if(tabNameWidth(fitted+next+"…")>pixels)break;
            fitted.append(next);i+=Character.charCount(cp);
        }
        return fitted+"…";
    }
    public static String compact(double value) {
        if (!Double.isFinite(value)) return "—";
        double amount = Math.abs(value);
        if (amount >= 1_000_000_000) return String.format(Locale.ROOT,"%.1fB",value/1_000_000_000);
        if (amount >= 1_000_000) return String.format(Locale.ROOT,"%.1fM",value/1_000_000);
        if (amount >= 1_000) return String.format(Locale.ROOT,"%.1fK",value/1_000);
        return String.format(Locale.ROOT,"%.0f",value);
    }
    public static String duration(long ticks) {
        long minutes = Math.max(0,ticks)/1200;
        return minutes/60 + "sa " + minutes%60 + "dk";
    }
    public static boolean unresolved(String value) {
        return value == null || value.isBlank() || value.matches(".*%[^%]+%.*");
    }
    public static String rankBadge(Object provider,org.bukkit.entity.Player player) {
        if(provider==null) return null;
        try {
            Object result=provider.getClass().getMethod("getRankBadge",org.bukkit.entity.Player.class,org.bukkit.entity.Player.class).invoke(provider,player,player);
            return result instanceof String badge && !badge.isBlank()?badge:null;
        } catch (ReflectiveOperationException ignored) { return null; }
    }
}
