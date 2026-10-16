package com.arena.pekaclient.hud;

import com.arena.pekaclient.render.PlayerPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;

import java.util.Properties;

/**
 * Player doll HUD: a live 3D model of the local player, drawn with vanilla player rendering.
 *
 * The model shows the armour and the held item exactly as the game renders them, because it is the same
 * renderer. Settings: scale, position, background, border and opacity (the shared element style), plus
 * the facing angle and an optional slow spin. Dragging the preview in the hub's HUD tab sets the angle.
 *
 * The doll only reads the local player. It needs a loaded world, so with no world it draws nothing but its
 * panel. It never changes the entity-rendering pipeline.
 */
public final class PlayerDollHud extends HudElement {

    /** Local width and height at scale 1. The figure's feet sit 8 px above the bottom edge. */
    public static final int WIDTH = 48;
    public static final int HEIGHT = 68;
    private static final float FEET_INSET = 8f;
    /** Pixels per block. 26 gives a figure about 47 px tall at scale 1. */
    private static final float BLOCK_PX = 26f;
    /** Spin speed in degrees per second when spin is on. */
    private static final float SPIN_DEG_PER_SEC = 24f;

    /** Facing angle in degrees, -180 .. 180. 0 is the vanilla default. */
    public int angle;
    /** When true the doll turns slowly on its own, on top of the angle. */
    public boolean spin;
    /** Off resets the angle to 0 and locks the Angle slider and the drag on the preview. */
    public boolean angleOn = true;

    public PlayerDollHud() {
        super("doll", "Player doll",
                new HudPlacement(HudPlacement.Anchor.START, 6, HudPlacement.Anchor.END, 6), false);
    }

    /** The facing angle used for drawing: the chosen angle plus the spin offset, in -180 .. 180. */
    public float effectiveAngle() {
        float a = angle;
        if (spin) {
            a += (float) ((System.nanoTime() / 1.0e9) * SPIN_DEG_PER_SEC % 360.0);
        }
        return PlayerPreview.normalise(a);
    }

    /** Adds a horizontal drag to the angle, in degrees. The result wraps into -180 .. 180. */
    public void dragBy(float degrees) {
        if (!angleOn) {
            return;
        }
        angle = Math.round(PlayerPreview.normalise(angle + degrees));
    }

    @Override
    public float width() {
        return WIDTH;
    }

    @Override
    public float height() {
        return HEIGHT;
    }

    @Override
    protected void drawContent() {
        drawPanel(WIDTH, HEIGHT);
        drawDoll(WIDTH * 0.5f, HEIGHT - FEET_INSET, BLOCK_PX, effectiveAngle());
    }

    /**
     * Draws the local player at the given feet position and facing. Shared with the hub preview, so the
     * preview and the HUD show exactly the same model.
     */
    public static void drawDoll(float cx, float feetY, float blockPx, float yawDeg) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase player = mc.thePlayer;
        if (player == null || mc.theWorld == null) {
            return;
        }
        PlayerPreview.draw(cx, feetY, blockPx, yawDeg, player);
    }

    @Override
    protected void resetExtra() {
        angle = 0;
        spin = false;
        angleOn = true;
    }

    @Override
    protected void loadExtra(Properties p, String k) {
        angle = clamp(intOf(p, k + "angle", 0), -180, 180);
        spin = bool(p, k + "spin", false);
        angleOn = bool(p, k + "angleOn", true);
    }

    @Override
    protected void saveExtra(Properties p, String k) {
        p.setProperty(k + "angle", String.valueOf(angle));
        p.setProperty(k + "spin", String.valueOf(spin));
        p.setProperty(k + "angleOn", String.valueOf(angleOn));
    }
}
