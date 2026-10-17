package com.arena.pekaclient.render.itemanimation;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/**
 * Forge 1.8.9 coremod entry point for the narrowly scoped Item Animation bytecode hook.
 *
 * <p>The normal Forge hand event happens before EntityRenderer resets the hand matrices and has no paired post
 * event, so it cannot safely surround the real first-person renderer. This plugin instead wires two balanced,
 * method-local callbacks into Minecraft's own renderer. It does not replace ItemRenderer or mutate final fields.
 */
@IFMLLoadingPlugin.Name("Sky VeLor Item Animation")
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions({"com.arena.pekaclient.render.itemanimation"})
public final class ItemAnimationLoadingPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        // This status class deliberately has no Minecraft references, so the launcher diagnostic cannot force an
        // early ItemRenderer/Minecraft load before Forge installs the exclusion and transformer.
        ItemAnimationHookStatus.noteCoremodRegistered();
        return new String[]{ItemAnimationTransformer.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        // No Minecraft classes are loaded during coremod setup.
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
