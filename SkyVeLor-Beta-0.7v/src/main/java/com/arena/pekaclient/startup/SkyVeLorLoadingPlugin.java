package com.arena.pekaclient.startup;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.io.File;
import java.util.Map;

/**
 * The early hook that makes Sky VeLor's loading screen possible.
 *
 * Forge loads coremods during the launchwrapper launch, before {@code Minecraft.startGame()} and therefore before
 * {@code SplashProgress.start()} reads {@code config/splash.properties}. That is the only point at which the
 * splash screen can still be changed, so {@link EarlySplash} is applied from here.
 *
 * This plugin deliberately does no bytecode work at all: no ASM transformers, no access transformers, no setup
 * class, no mod container of its own. It only writes two files in the game directory - the splash config and the
 * wordmark - which is why it can be shipped without putting the normal startup path at risk. The jar is still a
 * regular Forge mod as well; the manifest's {@code FMLCorePluginContainsFMLMod} tells Forge to load it as one.
 */
public class SkyVeLorLoadingPlugin implements IFMLLoadingPlugin {

    /** Forge puts the game directory here before calling {@link #injectData(Map)}. */
    private static final String KEY_MC_DIR = "mcLocation";

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    /**
     * Applies the Sky VeLor splash. Everything is wrapped: a coremod that throws at this point stops the game
     * from launching at all, and a loading screen is never worth that.
     */
    @Override
    public void injectData(Map<String, Object> data) {
        try {
            Object dir = data == null ? null : data.get(KEY_MC_DIR);
            if (dir instanceof File) {
                EarlySplash.install((File) dir);
            }
        } catch (Throwable ignored) {
            // Never break the launch for a splash screen.
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
