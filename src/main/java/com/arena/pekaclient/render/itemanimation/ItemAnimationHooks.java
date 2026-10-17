package com.arena.pekaclient.render.itemanimation;

import com.arena.pekaclient.feature.ItemAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * Runtime callbacks woven directly into Minecraft 1.8.9's {@code ItemRenderer}.
 *
 * <p>The callbacks run only from the first-person ItemRenderer methods. They use no reflection, allocate no
 * objects per frame, and never modify EntityLivingBase, attack speed, Haste, packets, world rendering, or any
 * gameplay state. Primitive setting values are refreshed on each live render/update so sliders and typed values
 * apply immediately.
 */
public final class ItemAnimationHooks {

    private static final float ANCHOR_X = 0.56F;
    private static final float ANCHOR_Y = -0.52F;
    private static final float ANCHOR_Z = -0.72F;
    private static final float VANILLA_SWING_TICKS = 6.0F;

    // Fixed stack bits cover any theoretical nested ItemRenderer calls without growing an array on the render path.
    private static int renderDepth;
    private static long pushedMatrixBits;

    private static boolean cachedEnabled;
    private static float cachedX;
    private static float cachedY;
    private static float cachedZ;
    private static float cachedSize = 1.0F;
    private static float cachedSpeed = 1.0F;
    private static boolean cachedRemoveHasteEffect;
    private static boolean cachedAutoFixHitSlash;
    private static int cachedHitSlashStyle;

    private static boolean sawVanillaSwing;
    private static boolean visualSwingActive;
    private static boolean visualSwingComplete;
    private static int visualSwingStartTick;
    private static float lastRawSwingProgress;
    private static volatile float lastSwingProgress;

    private static volatile boolean renderHookReached;
    private static volatile boolean equipHookReached;
    private static volatile long renderCalls;
    private static volatile long equipCalls;
    private static volatile long lastRenderNanos;
    private static volatile long lastEquipNanos;
    private static volatile float lastX;
    private static volatile float lastY;
    private static volatile float lastZ;
    private static volatile float lastSize = 1.0F;
    private static volatile float lastSpeed = 1.0F;

    private ItemAnimationHooks() {
    }

    /**
     * Called immediately after ItemRenderer's vanilla outer {@code pushMatrix()}, before any map/item/arm branch.
     * The matching callback is woven immediately before vanilla's {@code popMatrix()} (and defensively before an
     * unexpected return), so a custom push can never leak into the next frame.
     */
    public static void beforeFirstPersonRender(Object renderedStack, float partialTicks) {
        refreshSettings();
        ItemStack held = renderedStack instanceof ItemStack ? (ItemStack) renderedStack : null;
        boolean map = isMap(held);
        boolean offsets = cachedX != 0.0F || cachedY != 0.0F || cachedZ != 0.0F;
        // ItemRenderer's own itemToRender field is passed by the transformer. Null arms and map rendering remain
        // exactly vanilla; scale and offsets apply only to the non-map held-item branch.
        boolean scaleHeldItem = held != null && !map && cachedSize != 1.0F;
        boolean apply = cachedEnabled && held != null && !map && (offsets || scaleHeldItem);

        renderHookReached = true;
        renderCalls++;
        lastRenderNanos = System.nanoTime();
        lastX = cachedX;
        lastY = cachedY;
        lastZ = cachedZ;
        lastSize = cachedSize;
        ItemAnimationHookStatus.onRenderCallback();

        int slot = renderDepth++;
        if (slot >= 0 && slot < Long.SIZE && apply) {
            pushedMatrixBits |= 1L << slot;
            GlStateManager.pushMatrix();
            // Offset stays outside the anchor scale, so X/Y/Z retain their exact user-facing meaning.
            GlStateManager.translate(cachedX, cachedY, cachedZ);
            if (scaleHeldItem) {
                // T(anchor) * S * T(-anchor): resize about the vanilla held-item anchor, not the screen origin.
                GlStateManager.translate(ANCHOR_X, ANCHOR_Y, ANCHOR_Z);
                GlStateManager.scale(cachedSize, cachedSize, cachedSize);
                GlStateManager.translate(-ANCHOR_X, -ANCHOR_Y, -ANCHOR_Z);
            }
        }
    }

    /** Balances exactly one matrix pushed by {@link #beforeFirstPersonRender(Object, float)}. */
    public static void afterFirstPersonRender() {
        if (renderDepth <= 0) {
            // Protect vanilla's matrix stack if another coremod produced an unexpected callback ordering.
            return;
        }
        int slot = --renderDepth;
        if (slot >= 0 && slot < Long.SIZE && (pushedMatrixBits & (1L << slot)) != 0L) {
            pushedMatrixBits &= ~(1L << slot);
            GlStateManager.popMatrix();
        }
    }

    /**
     * Replaces only the local swing value used by ItemRenderer. A new visual timeline starts when vanilla starts
     * swinging and advances from game ticks plus partial ticks at {@code speed}; it never writes EntityLivingBase
     * swing fields. The optional Haste removal is likewise a fixed renderer timeline, not a potion mutation.
     */
    public static float visualSwingProgress(float vanillaProgress, float partialTicks) {
        refreshSettings();
        if (!cachedEnabled) {
            clearVisualSwing();
            lastRawSwingProgress = vanillaProgress;
            lastSwingProgress = vanillaProgress;
            return vanillaProgress;
        }

        float preview = previewSwingProgress();
        if (preview >= 0.0F) {
            float styledPreview = applyHitSlashStyle(preview);
            lastRawSwingProgress = preview;
            lastSwingProgress = styledPreview;
            return styledPreview;
        }

        boolean customVisual = cachedRemoveHasteEffect || cachedSpeed != 1.0F || cachedHitSlashStyle != 0;
        if (!customVisual || Float.isNaN(cachedSpeed) || Float.isInfinite(cachedSpeed)) {
            clearVisualSwing();
            lastRawSwingProgress = vanillaProgress;
            lastSwingProgress = vanillaProgress;
            return vanillaProgress;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc == null ? null : mc.thePlayer;
        if (player == null) {
            clearVisualSwing();
            lastRawSwingProgress = vanillaProgress;
            lastSwingProgress = vanillaProgress;
            return vanillaProgress;
        }

        boolean vanillaSwinging = player.isSwingInProgress;
        if (vanillaSwinging && !sawVanillaSwing) {
            visualSwingStartTick = player.ticksExisted;
            visualSwingActive = true;
            visualSwingComplete = false;
            lastRawSwingProgress = 0.0F;
            lastSwingProgress = 0.0F;
        }
        sawVanillaSwing = vanillaSwinging;

        if (visualSwingComplete) {
            if (vanillaSwinging) {
                lastRawSwingProgress = 1.0F;
                lastSwingProgress = 1.0F;
                return 1.0F;
            }
            visualSwingComplete = false;
            lastRawSwingProgress = vanillaProgress;
            lastSwingProgress = vanillaProgress;
            return vanillaProgress;
        }
        if (!visualSwingActive) {
            lastRawSwingProgress = vanillaProgress;
            lastSwingProgress = vanillaProgress;
            return vanillaProgress;
        }

        float elapsedTicks = (player.ticksExisted - visualSwingStartTick) + partialTicks;
        float visual = elapsedTicks * cachedSpeed / VANILLA_SWING_TICKS;
        if (visual >= 1.0F) {
            visualSwingActive = false;
            visualSwingComplete = true;
            lastRawSwingProgress = 1.0F;
            lastSwingProgress = 1.0F;
            return 1.0F;
        }
        if (visual <= 0.0F) {
            lastRawSwingProgress = 0.0F;
            lastSwingProgress = 0.0F;
            return 0.0F;
        }
        if (cachedAutoFixHitSlash && vanillaSwinging && visual < lastRawSwingProgress) {
            // Fix only the local float consumed by ItemRenderer; player swing timing and Haste stay untouched.
            visual = lastRawSwingProgress;
        }
        float styled = applyHitSlashStyle(visual);
        lastRawSwingProgress = visual;
        lastSwingProgress = styled;
        return styled;
    }

    /** Renderer-only preview progression. It has no input, packet, player-state, or potion side effect. */
    private static float previewSwingProgress() {
        long started = ItemAnimation.getPreviewSwingStartedNanos();
        if (started < 0L) {
            return -1.0F;
        }
        float elapsedTicks = (System.nanoTime() - started) / 50000000.0F;
        float visual = elapsedTicks * cachedSpeed / VANILLA_SWING_TICKS;
        if (visual >= 1.0F) {
            ItemAnimation.clearPreviewSwing();
            return 1.0F;
        }
        return Math.max(0.0F, visual);
    }

    /** Applies a first-person-only shape to the local ItemRenderer swing value. */
    private static float applyHitSlashStyle(float progress) {
        if (cachedHitSlashStyle == 1) {
            return Math.min(1.0F, progress * 1.12F);
        }
        if (cachedHitSlashStyle == 2) {
            return (float) Math.sin(progress * Math.PI * 0.5D);
        }
        return progress;
    }

    private static void clearVisualSwing() {
        sawVanillaSwing = false;
        visualSwingActive = false;
        visualSwingComplete = false;
    }

    /**
     * Called after vanilla computes ItemRenderer.equippedProgress. This changes only the visible interpolation;
     * it does not alter attack cooldown, swinging state, Haste, packets, inventory state, or use actions.
     */
    public static float scaleEquipProgress(float previous, float vanillaCurrent) {
        refreshSettings();
        equipHookReached = true;
        equipCalls++;
        lastEquipNanos = System.nanoTime();
        lastSpeed = cachedSpeed;

        if (!cachedEnabled || cachedSpeed == 1.0F || Float.isNaN(cachedSpeed)
                || Float.isInfinite(cachedSpeed)) {
            return vanillaCurrent;
        }
        float adjusted = previous + (vanillaCurrent - previous) * cachedSpeed;
        // ItemRenderer's visible equipped progress is bounded by vanilla's [0, 1] interval.
        return Math.max(0.0F, Math.min(1.0F, adjusted));
    }

    /** Reads every live setting into primitive fields; no snapshot object/reflection is created on the render path. */
    private static void refreshSettings() {
        cachedEnabled = ItemAnimation.isEnabled();
        cachedX = cachedEnabled ? ItemAnimation.getConfiguredX() : 0.0F;
        cachedY = cachedEnabled ? ItemAnimation.getConfiguredY() : 0.0F;
        cachedZ = cachedEnabled ? ItemAnimation.getConfiguredZ() : 0.0F;
        cachedSize = cachedEnabled ? ItemAnimation.getConfiguredSize() : 1.0F;
        cachedSpeed = cachedEnabled ? ItemAnimation.getConfiguredSpeed() : 1.0F;
        cachedRemoveHasteEffect = cachedEnabled && ItemAnimation.isHasteEffectRemoved();
        cachedAutoFixHitSlash = cachedEnabled && ItemAnimation.isAutoFixHitSlash();
        cachedHitSlashStyle = cachedEnabled ? ItemAnimation.getHitSlashStyle() : 0;
    }

    private static boolean isMap(ItemStack held) {
        return held != null && (held.getItem() == Items.filled_map || held.getItem() == Items.map);
    }

    public static boolean hasRenderHookRun() {
        return renderHookReached;
    }

    public static boolean hasEquipHookRun() {
        return equipHookReached;
    }

    public static long getRenderCalls() {
        return renderCalls;
    }

    public static int getRenderCallsLastSecond() {
        return ItemAnimationHookStatus.getRenderCallsLastSecond();
    }

    public static long getEquipCalls() {
        return equipCalls;
    }

    public static long getLastRenderNanos() {
        return lastRenderNanos;
    }

    public static long getLastEquipNanos() {
        return lastEquipNanos;
    }

    public static float getLastX() {
        return lastX;
    }

    public static float getLastY() {
        return lastY;
    }

    public static float getLastZ() {
        return lastZ;
    }

    public static float getLastSize() {
        return lastSize;
    }

    public static float getLastSpeed() {
        return lastSpeed;
    }

    public static float getVisualSwingProgress() {
        return lastSwingProgress;
    }
}
