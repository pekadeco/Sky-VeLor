package com.arena.pekaclient.dungeon.solver;

import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

/** Local-only sound playback for terminal feedback. */
public final class DungeonSolverSound {
    private DungeonSolverSound() { }

    public static void playSound(BlockPos ignored, String sound, float volume, float pitch) {
        playGlobalSound(sound, volume, pitch);
    }

    public static void playSound(int x, int y, int z, String sound, float volume, float pitch) {
        playGlobalSound(sound, volume, pitch);
    }

    public static void playGlobalSound(String sound, float volume, float pitch) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null || sound == null || sound.trim().isEmpty()) return;
        try {
            mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                    sound.trim(), volume, pitch, false);
        } catch (RuntimeException ignored) {
            // Some resource packs register no matching sound event; solver logic must still work.
        }
    }
}
