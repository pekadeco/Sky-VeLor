package com.arena.pekaclient.gui;

import com.arena.pekaclient.PekaVersion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Packaged-class regression proof for the merged Lunaria release contract. */
public final class ReleaseClaimsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        publicReleaseIdentityIsLunaria();
        retainedArenaSurfacesArePackaged();
        retainedFriendSurfacesArePackaged();
        settingsRoutesExposeEveryRetainedSurface();
        commandsKeepTheCanonicalAndCompatibilityRoutes();
        compatibilityPersistenceAndLoadingArtworkRemainPackaged();
        obsoleteDuplicateRenderersStayAbsent();
        done();
    }

    private static void publicReleaseIdentityIsLunaria() {
        check("requested display version remains the single public release version",
                "Beta-0.10.9.4v".equals(PekaVersion.NAME));
        check("requested public title is packaged", "The Sky's Lunaria Mod".equals(PekaVersion.DISPLAY_NAME));
        check("Lunaria metadata entry point is packaged", classExists("com.arena.pekaclient.PekaClientMod")
                && classText("com.arena.pekaclient.PekaClientMod").contains("lunaria"));
    }

    private static void retainedArenaSurfacesArePackaged() {
        String[] required = {
                "com.arena.pekaclient.assist.ShortcutController",
                "com.arena.pekaclient.itemlockswap.ItemLockSwapController",
                "com.arena.pekaclient.itemlockswap.ItemLockSwapSettings",
                "com.arena.pekaclient.dungeon.DungeonMapOverlay",
                "com.arena.pekaclient.dungeon.DungeonMapPlayerHeadMotion",
                "com.arena.pekaclient.gui.DungeonHudEditorScreen",
                "com.arena.pekaclient.assist.dungeon.DungeonScoreCalculator",
                "com.arena.pekaclient.assist.dungeon.DungeonScoreTracker",
                "com.arena.pekaclient.hud.DungeonScoreHud",
                "com.arena.pekaclient.assist.dungeon.RoomClearAlertController",
                "com.arena.pekaclient.hud.RoomClearAlertHud",
                "com.arena.pekaclient.assist.dungeon.DungeonChestCloser",
                "com.arena.pekaclient.assist.visual.DeadMobRenderFilter",
                "com.arena.pekaclient.assist.visual.AspectTeleportTransitionRenderer",
                "com.arena.pekaclient.gui.ItemAnimationEditorScreen"
        };
        check("required Arena shortcut, inventory, dungeon, HUD, visual and animation surfaces remain", allExist(required));
    }

    private static void retainedFriendSurfacesArePackaged() {
        String[] required = {
                "com.arena.pekaclient.config.FontSettings",
                "com.arena.pekaclient.render.GlobalFontManager",
                "com.arena.pekaclient.render.ExternalFontLocations",
                "com.arena.pekaclient.feature.ChatFontPolicy",
                "com.arena.pekaclient.gui.ChatEditorScreen",
                "com.arena.pekaclient.gui.ChatEditorLayout",
                "com.arena.pekaclient.gui.ChatUiModel",
                "com.arena.pekaclient.dungeon.DungeonProgressTracker",
                "com.arena.pekaclient.dungeon.solver.terminals.TerminalGuiLayer",
                "com.arena.pekaclient.feature.inventory.SlotLockStore"
        };
        check("retained friend font, chat editor, progress, terminal and slot-lock surfaces are packaged", allExist(required));
    }

    private static void settingsRoutesExposeEveryRetainedSurface() {
        ReferenceSettingsCatalog.Setting font = ReferenceSettingsCatalog.find("font-settings");
        ReferenceSettingsCatalog.Setting chatEditor = ReferenceSettingsCatalog.find("chat-editor");
        check("font controls are visible in the Lunaria Misc catalog", font != null && font.hasChildren()
                && ReferenceSettingsCatalog.find("font-selection") != null
                && ReferenceSettingsCatalog.find("font-scale") != null
                && ReferenceSettingsCatalog.find("font-lunaria-only") != null
                && ReferenceSettingsCatalog.ownerOf("font-settings") == ReferenceSettingsCatalog.Tab.SYSTEM);
        check("Chat Editor is a visible named destination", chatEditor != null && chatEditor.isEditor()
                && "open-chat-editor".equals(chatEditor.editorActionId));
        String screen = classText("com.arena.pekaclient.gui.AppleSettingsScreen");
        check("settings screen opens the retained detailed Chat Editor", screen.contains("open-chat-editor")
                && screen.contains("ChatEditorScreen"));
        check("all required primary settings routes remain complete", ReferenceSettingsCatalog.routesAreComplete());
    }

    private static void commandsKeepTheCanonicalAndCompatibilityRoutes() {
        String command = classText("com.arena.pekaclient.command.SvCommand");
        String dungeon = classText("com.arena.pekaclient.command.SvDungeonCommand");
        String actions = classText("com.arena.pekaclient.command.ClientActions");
        check("/lunaria is canonical while /sv and /velor remain aliases", command.contains("lunaria")
                && command.contains("sv") && command.contains("velor"));
        check("dungeon debug and legacy direct dungeon command stay available", command.contains("dungeon")
                && command.contains("debug") && dungeon.contains("svdungeon"));
        check("chat command opens the retained detailed editor after chat closes", actions.contains("ChatEditorScreen")
                && !actions.contains("openChatSettings"));
    }

    private static void compatibilityPersistenceAndLoadingArtworkRemainPackaged() {
        String settings = classText("com.arena.pekaclient.config.AppleSettings");
        String configFiles = classText("com.arena.pekaclient.config.ConfigFiles");
        String slots = classText("com.arena.pekaclient.feature.inventory.SlotLockStore");
        String fonts = classText("com.arena.pekaclient.render.ExternalFontLocations");
        String artwork = classText("com.arena.pekaclient.gui.LunariaArtwork");
        String terrain = classText("com.arena.pekaclient.gui.LunariaDownloadingTerrainScreen");
        check("main settings use Lunaria writes with legacy compatibility reads", settings.contains("lunaria.properties")
                && settings.contains("skyvelor.properties") && settings.contains("FontSettings")
                && configFiles.contains("Lunaria"));
        check("slot locks use Lunaria current precedence with a legacy fallback", slots.contains("lunaria")
                && slots.contains("skyvelor"));
        check("external fonts use current-first plus legacy fallback without file mutation", fonts.contains("lunaria/fonts")
                && fonts.contains("skyvelor/fonts") && fonts.contains("findReadableFile"));
        check("loading artwork uses the supplied cover image and scaled source crop", artwork.contains("lunaria_loading_background")
                && artwork.contains("drawScaledCustomSizeModalRect"));
        check("native terrain lifecycle uses a post-draw Lunaria overlay", terrain.contains("Downloading terrain")
                && terrain.contains("Terrain progress is not exposed"));
    }

    private static void obsoleteDuplicateRenderersStayAbsent() {
        check("obsolete duplicate first-person renderer ownership remains removed",
                !classExists("com.arena.pekaclient.render.CustomItemRenderer")
                        && !classExists("com.arena.pekaclient.render.FirstPersonItemSetup"));
    }

    private static boolean allExist(String[] names) {
        for (String name : names) if (!classExists(name)) return false;
        return true;
    }

    private static boolean classExists(String binaryName) {
        return ReleaseClaimsTests.class.getClassLoader().getResource(binaryName.replace('.', '/') + ".class") != null;
    }

    private static String classText(String binaryName) {
        InputStream input = ReleaseClaimsTests.class.getClassLoader()
                .getResourceAsStream(binaryName.replace('.', '/') + ".class");
        if (input == null) return "";
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            return new String(output.toByteArray(), "ISO-8859-1");
        } catch (IOException ignored) {
            return "";
        } finally {
            try { input.close(); } catch (IOException ignored) { }
        }
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("release claims: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
