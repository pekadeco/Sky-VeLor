package com.arena.pekaclient.config;

import java.util.Properties;

/** Global font state, deliberately independent from Chat QoL. */
public final class FontSettings {
    public static final String PREFIX = "fonts.";
    public static final String MINECRAFT_DEFAULT = "Minecraft Default";
    public static final String SMOOTH = "Smooth";
    public static final String DOODLE = "Doodle";
    public static final String ROUNDED = "Rounded";
    public static final String MONO = "Mono";
    public static boolean enabled;
    public static String selected = MINECRAFT_DEFAULT;
    /** Fresh font selections apply to the whole Minecraft client unless user selects Lunaria only. */
    public static boolean applyToEverything = true;
    public static int scale = 100;
    private static boolean migrationPending;
    private FontSettings() { }
    public static void reset(){ enabled=false; selected=MINECRAFT_DEFAULT; applyToEverything=true; scale=100; migrationPending=false; }
    public static void load(Properties p) {
        if (p == null) p = new Properties(); reset();
        boolean nativeKeys = hasPrefix(p,PREFIX);
        boolean old = p.containsKey("chat.qol.globalFont.enabled") || p.containsKey("chat.qol.font.enabled");
        enabled = bool(p,PREFIX+"enabled",bool(p,"chat.qol.globalFont.enabled",bool(p,"chat.qol.font.enabled",false)));
        selected = normalise(text(p,PREFIX+"selected",text(p,"chat.qol.globalFont.name",text(p,"chat.qol.font.name",MINECRAFT_DEFAULT))));
        applyToEverything = bool(p,PREFIX+"applyEverything",p.containsKey("chat.qol.globalFont.allMinecraft") ? bool(p,"chat.qol.globalFont.allMinecraft",true):true);
        scale = integer(p,PREFIX+"scale",integer(p,"chat.qol.globalFont.scale.ui",100,70,140),70,140);
        migrationPending = !nativeKeys && old;
    }
    public static void save(Properties p) { if(p==null)return; normalise(); p.setProperty(PREFIX+"enabled",Boolean.toString(enabled)); p.setProperty(PREFIX+"selected",selected); p.setProperty(PREFIX+"applyEverything",Boolean.toString(applyToEverything)); p.setProperty(PREFIX+"scale",Integer.toString(scale)); migrationPending=false; }
    public static boolean isMigrationPending(){return migrationPending;}
    public static void normalise(){ selected=normalise(selected); scale=Math.max(70,Math.min(140,scale)); }
    public static String normalise(String value) {
        if(value==null)return MINECRAFT_DEFAULT; String v=value.trim();
        if(SMOOTH.equalsIgnoreCase(v)||"Compact".equalsIgnoreCase(v))return SMOOTH;
        if(DOODLE.equalsIgnoreCase(v))return DOODLE;
        if(ROUNDED.equalsIgnoreCase(v))return ROUNDED;
        if(MONO.equalsIgnoreCase(v))return MONO;
        if(MINECRAFT_DEFAULT.equalsIgnoreCase(v)||"Unicode".equalsIgnoreCase(v))return MINECRAFT_DEFAULT;
        if(v.startsWith("External:")){String n=v.substring(9);String l=n.toLowerCase(java.util.Locale.ROOT);if(n.length()>=5&&n.length()<=96&&n.indexOf('/')<0&&n.indexOf('\\')<0&&n.indexOf("..")<0&&(l.endsWith(".ttf")||l.endsWith(".otf")))return "External:"+n;}
        return MINECRAFT_DEFAULT;
    }
    private static boolean hasPrefix(Properties p,String prefix){for(Object key:p.keySet())if(key!=null&&key.toString().startsWith(prefix))return true;return false;}
    private static String text(Properties p,String key,String fallback){String v=p.getProperty(key,fallback);return v==null?fallback:v.trim();}
    private static boolean bool(Properties p,String key,boolean fallback){String v=p.getProperty(key); return v==null?fallback:("true".equalsIgnoreCase(v.trim())?true:("false".equalsIgnoreCase(v.trim())?false:fallback));}
    private static int integer(Properties p,String key,int fallback,int min,int max){try{return Math.max(min,Math.min(max,Integer.parseInt(p.getProperty(key,Integer.toString(fallback)).trim())));}catch(RuntimeException e){return fallback;}}
}
