package com.tilkicraft.ui;
import java.lang.reflect.*;

/** Paper's public API first; CraftBukkit's package-private handle on Spigot. */
public final class ScoreNumbers {
    private ScoreNumbers() {}
    public static boolean hide(Object objective) {
        try {
            Class<?> format=Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat");
            objective.getClass().getMethod("numberFormat",format).invoke(objective,format.getMethod("blank").invoke(null));
            return true;
        } catch(ReflectiveOperationException ignored) { }
        try {
            Object handle=handle(objective);
            for(Method setter:handle.getClass().getMethods()) {
                if(setter.getParameterCount()!=1 || setter.getReturnType()!=void.class) continue;
                Class<?> type=setter.getParameterTypes()[0];
                if(!type.getPackageName().contains("numbers")) continue;
                for(String name:new String[]{"BlankFormat","BlankNumberFormat"}) try {
                    Class<?> blank=Class.forName(type.getPackageName()+"."+name,true,type.getClassLoader());
                    if(!type.isAssignableFrom(blank)) continue;
                    for(Field field:blank.getDeclaredFields()) if(Modifier.isStatic(field.getModifiers()) && blank.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);setter.setAccessible(true);setter.invoke(handle,field.get(null));return true;
                    }
                } catch(ClassNotFoundException ignored) { }
            }
        } catch(ReflectiveOperationException | RuntimeException ignored) { }
        return false;
    }
    static Object handle(Object objective) throws ReflectiveOperationException {
        for(Class<?> type=objective.getClass();type!=null;type=type.getSuperclass()) try {
            Method method=type.getDeclaredMethod("getHandle");method.setAccessible(true);return method.invoke(objective);
        } catch(NoSuchMethodException ignored) { }
        throw new NoSuchMethodException("CraftObjective.getHandle");
    }
}
