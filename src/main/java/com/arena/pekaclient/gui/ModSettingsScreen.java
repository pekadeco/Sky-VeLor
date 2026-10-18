package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

/**
 * Top-level settings for cross-feature behavior. Scroll policy lives here exactly once: each active viewport receives
 * a native wheel event once and uses this same multiplier and smoothing choice.
 */
public final class ModSettingsScreen extends GuiLayoutScreen {
    private final GuiScreen parent;
    private final GuiLayoutToolkit.Rect[] controls = {
            new GuiLayoutToolkit.Rect(), new GuiLayoutToolkit.Rect(), new GuiLayoutToolkit.Rect(),
            new GuiLayoutToolkit.Rect()
    };

    public ModSettingsScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        placePanel(Math.min(540f, width - 12f), Math.min(286f, height - 12f));
    }

    @Override
    public void drawScreen(int mx, int my, float partial) {
        drawDefaultBackground();
        beginPanel("Mod Settings", mx, my);
        float naturalY = 0f;
        row("Scroll speed", multiplier(AppleSettings.effectiveScrollSpeed()), 0, rowY(naturalY));
        naturalY += 31f;
        row("Smooth scrolling", AppleSettings.smoothScrolling ? "Enabled" : "Disabled", 1, rowY(naturalY));
        naturalY += 31f;
        row("Chat QoL master", ChatQolSettings.isEnabled() ? "Enabled" : "Disabled", 2, rowY(naturalY));
        naturalY += 31f;
        row("Item Animation", AppleSettings.itemAnimationEnabled ? "Enabled" : "Disabled", 3, rowY(naturalY));
        naturalY += 42f;
        drawLabel("Shift + wheel uses fine scrolling. A wheel event is applied once only to its hovered viewport.",
                content.x, rowY(naturalY), content.w, 0xFFB9C1CD);
        naturalY += 18f;
        drawLabel("This global policy is used by settings, editors, HUD panels, and the Chat QoL renderer.",
                content.x, rowY(naturalY), content.w, 0xFFB9C1CD);
        finishContent(naturalY + 28f);
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
            if (i == 0) {
                AppleSettings.scrollSpeed = nextSpeed(AppleSettings.effectiveScrollSpeed());
                AppleSettings.scrollSpeedOn = true;
            } else if (i == 1) {
                AppleSettings.smoothScrolling = !AppleSettings.smoothScrolling;
            } else if (i == 2) {
                ChatQolSettings.enabled = !ChatQolSettings.enabled;
            } else {
                AppleSettings.itemAnimationEnabled = !AppleSettings.itemAnimationEnabled;
            }
            AppleSettings.save();
            return;
        }
    }

    private static int nextSpeed(int value) {
        return value >= 300 ? 50 : value + 25;
    }

    private static String multiplier(int value) {
        return String.format(java.util.Locale.ROOT, "%.2fx", value / 100f);
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(c, key);
    }
}
