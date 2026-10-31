package com.arena.pekaclient.config;

import java.util.Properties;

/** Config migration/normalisation proof for top-level font state. */
public final class FontSettingsTests {
    private static int passed,failed;
    public static void main(String[] args){defaults();migratesOldChatFont();roundTripAndRejectsPath();done();}
    private static void defaults(){FontSettings.load(new Properties());check("default is vanilla",FontSettings.MINECRAFT_DEFAULT.equals(FontSettings.selected)&&!FontSettings.enabled&&FontSettings.applyToEverything);}
    private static void migratesOldChatFont(){Properties p=new Properties();p.setProperty("chat.qol.globalFont.enabled","true");p.setProperty("chat.qol.globalFont.name","Rounded");p.setProperty("chat.qol.globalFont.scale.ui","130");FontSettings.load(p);check("old state migrates",FontSettings.enabled&&FontSettings.ROUNDED.equals(FontSettings.selected)&&FontSettings.scale==130);check("migration flagged",FontSettings.isMigrationPending());}
    private static void roundTripAndRejectsPath(){FontSettings.enabled=true;FontSettings.selected="External:face.otf";FontSettings.applyToEverything=false;FontSettings.scale=140;Properties p=new Properties();FontSettings.save(p);FontSettings.load(p);check("native state round trips",FontSettings.enabled&&!FontSettings.applyToEverything&&"External:face.otf".equals(FontSettings.selected));FontSettings.selected="External:../../bad.ttf";FontSettings.normalise();check("unsafe path fails closed",FontSettings.MINECRAFT_DEFAULT.equals(FontSettings.selected));}
    private static void check(String n,boolean ok){if(ok)passed++;else{failed++;System.out.println("FAIL "+n);}}private static void done(){System.out.println("font settings: passed "+passed+", failed "+failed);if(failed>0)System.exit(1);}
}
