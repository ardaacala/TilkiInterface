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
