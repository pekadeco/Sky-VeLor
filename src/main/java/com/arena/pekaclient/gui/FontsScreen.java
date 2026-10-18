package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.FontSettings;
import com.arena.pekaclient.render.GlobalFontManager;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

/** Global font settings. Chat QoL is intentionally not a dependency of this screen. */
public final class FontsScreen extends GuiLayoutScreen {
    private final GuiScreen parent;
    private final GuiLayoutToolkit.Rect[] controls = {
            new GuiLayoutToolkit.Rect(), new GuiLayoutToolkit.Rect(), new GuiLayoutToolkit.Rect(),
            new GuiLayoutToolkit.Rect(), new GuiLayoutToolkit.Rect()
    };

    public FontsScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        placePanel(Math.min(560f, width - 12f), Math.min(410f, height - 12f));
    }

    @Override
    public void drawScreen(int mx, int my, float partial) {
        drawDefaultBackground();
        beginPanel("Fonts", mx, my);
        float y = rowY(0f);
        row("Enable custom font", FontSettings.enabled ? "Enabled" : "Disabled", 0, y);
        y += 31f;
        row("Font", FontSettings.selected, 1, y);
        y += 31f;
        row("Scale", FontSettings.scale + "%", 2, y);
        y += 31f;
        row("Apply to everything", FontSettings.applyToEverything ? "Requested" : "Sky VeLor only", 3, y);
        y += 31f;
        row("Reset font settings", "Reset", 4, y);
        y += 38f;

        drawLabel("Live preview", content.x, y, content.w, 0xFFB8C0CF);
        FontRenderer font = GlobalFontManager.rendererFor(mc.fontRendererObj);
        UIRender.roundRect(content.x, y + 14f, content.w - 4f, 58f, 5f, 0xFF20252D, 0xFF20252D, 1f);
        UIRender.text(font, "The quick brown fox jumps over 0123456789", content.x + 8f, y + 25f,
                0xFFFFFFFF, 1f, true);
        UIRender.text(font, "Sky VeLor — formatted §aGreen §bBlue §lBold", content.x + 8f, y + 43f,
                0xFFEAEAF0, 1f, true);
        y += 82f;
        drawLabel("Built-ins: Minecraft Default, Smooth, Doodle, Rounded and Mono.", content.x, y, content.w,
                0xFFC6CBD4);
        y += 17f;
        drawLabel("External .ttf/.otf: .minecraft/config/skyvelor/fonts/", content.x, y, content.w, 0xFFC6CBD4);
        y += 17f;
        drawLabel("Bundled font notices: OFL 1.1 — Abel, Architects Daughter, Nunito and JetBrains Mono.",
                content.x, y, content.w, 0xFF9FA8B6);
        y += 17f;
        if (FontSettings.enabled && FontSettings.applyToEverything) {
            drawLabel("Note: Minecraft-owned text stays vanilla when the safe 1.8.9 bridge is unavailable.",
                    content.x, y, content.w, 0xFFFFC36B);
            y += 17f;
        }
        drawLabel("Status: " + GlobalFontManager.status(), content.x, y, content.w,
                FontSettings.enabled ? 0xFF9EE6B0 : 0xFFB8C0CF);
        finishContent(y - content.y + 25f);
        endPanel();
    }

    private void row(String label, String value, int index, float y) {
        GuiLayoutToolkit.Rect whole = new GuiLayoutToolkit.Rect().set(content.x, y, content.w, 26f);
        GuiLayoutToolkit.Rect left = new GuiLayoutToolkit.Rect();
        GuiLayoutToolkit.splitRow(whole, 4f, 6f, Math.max(100f, uiFont().getStringWidth(value) + 16f),
                20f, left, controls[index]);
        drawLabel(label, left.x, left.y + 8f, left.w, 0xFFD9DDE5);
        UIRender.roundRect(controls[index].x, controls[index].y, controls[index].w, controls[index].h,
                4f, 0xFF343B47, 0xFF343B47, 1f);
        drawLabel(value, controls[index].x + 6f, controls[index].y + 6f, controls[index].w - 12f, 0xFFFFFFFF);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;
        for (int i = 0; i < controls.length; i++) {
            if (!controls[i].contains(mx, my)) continue;
            act(i);
            return;
        }
    }

    private void act(int control) {
        if (control == 0) {
            FontSettings.enabled = !FontSettings.enabled;
        } else if (control == 1) {
            FontSettings.selected = next(FontSettings.selected);
        } else if (control == 2) {
            FontSettings.scale = FontSettings.scale >= 140 ? 70 : FontSettings.scale + 10;
        } else if (control == 3) {
            FontSettings.applyToEverything = !FontSettings.applyToEverything;
            if (FontSettings.enabled && FontSettings.applyToEverything) GlobalFontManager.appliesToAllMinecraftText();
        } else {
            FontSettings.reset();
        }
        FontSettings.normalise();
        GlobalFontManager.reload();
        AppleSettings.save();
    }

    private static String next(String selected) {
        String[] all = GlobalFontManager.availableFonts();
        if (all.length == 0) return FontSettings.MINECRAFT_DEFAULT;
        for (int i = 0; i < all.length; i++) {
            if (all[i].equals(selected)) return all[(i + 1) % all.length];
        }
        return all[0];
    }

    @Override
    protected void keyTyped(char typed, int key) throws IOException {
        if (key == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typed, key);
    }
}
