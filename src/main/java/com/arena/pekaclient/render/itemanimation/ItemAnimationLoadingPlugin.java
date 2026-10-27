package com.arena.pekaclient.render.itemanimation;

import com.arena.pekaclient.assist.visual.AspectTeleportCameraTransformer;
import com.arena.pekaclient.assist.visual.VisualAssistTransformer;
import com.arena.pekaclient.itemlockswap.ItemLockSwapHookStatus;
import com.arena.pekaclient.itemlockswap.ItemLockSwapTransformer;
import com.arena.pekaclient.render.blockparticles.BlockParticleHookStatus;
import com.arena.pekaclient.render.blockparticles.BlockParticleTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/**
 * Forge 1.8.9 coremod entry point for narrow Item Animation/particle/Aspect render hooks plus the fail-closed Item
 * Lock / Swap packet guards and its one GuiContainer foreground draw callback. None replaces a vanilla renderer,
 * container, inventory, or network handler.
 */
@IFMLLoadingPlugin.Name("Sky VeLor Visual Hooks")
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions({"com.arena.pekaclient.render.itemanimation",
        "com.arena.pekaclient.render.blockparticles", "com.arena.pekaclient.itemlockswap",
        "com.arena.pekaclient.assist.visual", "com.arena.pekaclient.assist.inventory"})
public final class ItemAnimationLoadingPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        // Both status classes deliberately have no Minecraft references, so launcher diagnostics cannot force an
        // early ItemRenderer/EffectRenderer load before Forge installs exclusions and transformers.
        ItemAnimationHookStatus.noteCoremodRegistered();
        BlockParticleHookStatus.noteCoremodRegistered();
        ItemLockSwapHookStatus.noteCoremodRegistered();
        return new String[]{ItemAnimationTransformer.class.getName(), BlockParticleTransformer.class.getName(),
                ItemLockSwapTransformer.class.getName(), VisualAssistTransformer.class.getName(),
                AspectTeleportCameraTransformer.class.getName()};
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
