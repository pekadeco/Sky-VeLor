package com.arena.pekaclient.config;

import com.arena.pekaclient.hud.HudManager;
import java.io.File;
import java.nio.file.Files;

/** Tests the same checked write paths called by the settings screen, without starting Minecraft. */
public final class ExplicitSaveTests {
    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("lunaria-explicit-save").toFile();
        try {
            if (!AppleSettings.saveChecked(dir)) throw new AssertionError("settings write failed");
            if (ConfigFiles.read(new File(dir, AppleSettings.FILE_NAME)).isEmpty())
                throw new AssertionError("settings file empty");
            if (!HudManager.saveChecked(dir)) throw new AssertionError("HUD write failed");
            if (ConfigFiles.read(new File(dir, HudManager.FILE_NAME)).isEmpty())
                throw new AssertionError("HUD file empty");
            File blocked = new File(dir, "not-a-directory");
            Files.write(blocked.toPath(), new byte[]{1});
            if (AppleSettings.saveChecked(blocked)) throw new AssertionError("settings failure not reported");
            if (HudManager.saveChecked(blocked)) throw new AssertionError("HUD failure not reported");
            System.out.println("explicit save: success, data and failure reporting passed");
        } finally {
            for (File file : dir.listFiles()) file.delete();
            dir.delete();
        }
    }
}
