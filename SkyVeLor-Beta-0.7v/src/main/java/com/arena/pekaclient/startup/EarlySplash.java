package com.arena.pekaclient.startup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Makes Forge's loading screen the Sky VeLor loading screen.
 *
 * Why this class exists: Forge 1.8.9 draws the very first screen itself. In
 * {@code FMLClientHandler.beginMinecraftLoading} the first call is {@code SplashProgress.start()}, which reads
 * {@code config/splash.properties} from the game directory and then paints the splash on its own thread while
 * Minecraft loads. That splash is what replaces the plain Mojang screen. Everything a normal mod does - the
 * {@code @Mod} constructor, {@code preInit} - happens later, so a mod cannot change that screen from its own
 * init: by then the splash is already on screen (that is why Sky VeLor's own loading screen used to appear only
 * at the main menu).
 *
 * So the properties file has to exist before Minecraft starts. {@link SkyVeLorLoadingPlugin}, the coremod entry
 * point, runs during the launchwrapper launch and calls {@link #install(File)} with the game directory. From
 * then on the loading screen is Sky VeLor: its own wordmark, its own colours, drawn in place of the Mojang and
 * Forge screen, before any mod code runs.
 *
 * The splash is configured only through the keys Forge itself reads, so nothing is patched and nothing is
 * transformed. Three things are deliberately left alone:
 *  - {@code enabled} is never written, so Forge keeps its own decision (off by default on macOS, and off under
 *    OptiFine unless OptiFine reports compatibility),
 *  - a value the player already changed is never overwritten,
 *  - anything that goes wrong here is swallowed, because a loading-screen change must never stop the game from
 *    starting.
 */
public final class EarlySplash {

    /** Where Forge looks for the splash config, relative to the game directory. */
    public static final String CONFIG_PATH = "config/splash.properties";
    /** The extra resource pack Forge loads splash textures from, relative to the game directory. */
    public static final String PACK_PATH = "resources";
    /** The wordmark inside the mod jar and inside the splash pack. */
    public static final String LOGO_RESOURCE = "assets/pekaclient/textures/gui/skyvelor_splash.png";
    /** The resource location Forge is told to draw. */
    public static final String LOGO_LOCATION = "pekaclient:textures/gui/skyvelor_splash.png";

    /** Sky VeLor's splash colours. The background is the loading screen's deep blue. */
    static final String[][] BRANDING = {
            {"background", "0x0B2350"},
            {"font", "0xFFFFFF"},
            {"bar", "0x0A84FF"},
            {"barBorder", "0x3D78D6"},
            {"barBackground", "0x0A1730"},
            {"logoTexture", LOGO_LOCATION},
            {"rotate", "false"},
    };

    /**
     * The stock values Forge writes when it creates the file. A key that still holds one of these was never
     * chosen by the player, so Sky VeLor may replace it. Any other value is the player's and is kept.
     */
    static final Map<String, String> STOCK = new TreeMap<String, String>();

    static {
        STOCK.put("background", "0xFFFFFF");
        STOCK.put("font", "0x000000");
        STOCK.put("bar", "0xCB3D35");
        STOCK.put("barBorder", "0xC0C0C0");
        STOCK.put("barBackground", "0xFFFFFF");
        STOCK.put("logoTexture", "textures/gui/title/mojang.png");
        STOCK.put("rotate", "false");
    }

    private EarlySplash() {
    }

    /**
     * Merges the Sky VeLor splash settings into an existing property set and returns the result.
     * Pure, so the merge rule is unit-tested. A key is written only when it is missing or still holds Forge's
     * own default; a value the player chose is left exactly as it is. {@code enabled} is never touched.
     */
    public static Properties merge(Properties existing) {
        Properties out = new Properties();
        if (existing != null) {
            for (String name : existing.stringPropertyNames()) {
                out.setProperty(name, existing.getProperty(name));
            }
        }
        for (String[] pair : BRANDING) {
            String key = pair[0];
            String value = pair[1];
            String current = out.getProperty(key);
            if (current == null || current.trim().isEmpty()) {
                out.setProperty(key, value);
                continue;
            }
            String stock = STOCK.get(key);
            if (stock != null && stock.equalsIgnoreCase(current.trim())) {
                out.setProperty(key, value);
            }
        }
        return out;
    }

    /**
     * Applies the splash branding in the game directory. Never throws: the caller is the coremod, and a failure
     * here must not stop the game. Returns true when the config file was written.
     */
    public static boolean install(File gameDir) {
        if (gameDir == null) {
            return false;
        }
        try {
            extractLogo(gameDir);
            return writeConfig(gameDir);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Writes the wordmark into the splash resource pack Forge loads. No-op when it is already current. */
    static void extractLogo(File gameDir) throws IOException {
        File target = new File(new File(gameDir, PACK_PATH), LOGO_RESOURCE);
        byte[] bytes = readBundledLogo();
        if (bytes == null || bytes.length == 0) {
            return;
        }
        if (target.isFile() && target.length() == bytes.length) {
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            return;
        }
        OutputStream out = null;
        try {
            out = new FileOutputStream(target);
            out.write(bytes);
        } finally {
            close(out);
        }
    }

    /** The wordmark as shipped in the mod jar, or null when this build has none. */
    static byte[] readBundledLogo() {
        InputStream in = null;
        try {
            in = EarlySplash.class.getClassLoader().getResourceAsStream(LOGO_RESOURCE);
            if (in == null) {
                in = EarlySplash.class.getResourceAsStream("/" + LOGO_RESOURCE);
            }
            if (in == null) {
                return null;
            }
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream(4096);
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) > 0) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        } catch (Throwable t) {
            return null;
        } finally {
            close(in);
        }
    }

    /** Reads the splash config, merges the branding in and writes it back. */
    static boolean writeConfig(File gameDir) throws IOException {
        File configFile = new File(gameDir, CONFIG_PATH);
        Properties existing = new Properties();
        if (configFile.isFile()) {
            InputStream in = null;
            try {
                in = new FileInputStream(configFile);
                existing.load(in);
            } finally {
                close(in);
            }
        }
        Properties merged = merge(existing);
        if (existing.equals(merged)) {
            return false;
        }
        File parent = configFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            return false;
        }
        OutputStream out = null;
        try {
            out = new FileOutputStream(configFile);
            merged.store(out, "Sky VeLor loading screen (merged into Forge's own splash settings)");
        } finally {
            close(out);
        }
        return true;
    }

    private static void close(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
                // closing must never fail the caller
            }
        }
    }
}
