package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.hud.HudPlacement;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * Dedicated centered, solid editor for dungeon-only HUD targets. It deliberately owns a short element picker rather
 * than the generic editor's full HUD canvas: Score/Stats, Room Clear Alert, and the existing Dungeon Map stay in one
 * predictable place while their saved positions/scales remain independent.
 */
public final class DungeonHudEditorScreen extends AppleScreen {
    private static final float SNAP = 4F;
    private static final float PANEL_PAD = 10F;
    private static final float PANEL_H = 136F;
    private static final float CONTROL_H = 19F;

    private enum Target {
        SCORE("Score / Stats", HudManager.DUNGEON_SCORE),
        ALERT("Room Alert", HudManager.ROOM_CLEAR_ALERT),
        MAP("Dungeon Map", null);

        final String label;
        final HudElement element;

        Target(String label, HudElement element) {
            this.label = label;
            this.element = element;
        }
    }

    private final GuiScreen parent;
    private Target selected = Target.SCORE;
    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;
    private boolean dragging;
    private float grabX;
    private float grabY;
    private int guides;
    private boolean dirty;

    public DungeonHudEditorScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        HudManager.ensureLoaded();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        persist();
        UIRender.restoreVanillaGuiState();
    }

    private void layout() {
        panelW = Math.max(1F, Math.min(450F, width - 12F));
        panelH = Math.max(108F, Math.min(PANEL_H, height - 12F));
        panelX = (width - panelW) * .5F;
        panelY = (height - panelH) * .5F;
    }

    private void persist() {
        if (!dirty) return;
        dirty = false;
        HudManager.save();
        AppleSettings.save();
    }

    private void change() {
        dirty = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        layout();
        drawDefaultBackground();
        UIRender.beginFrame();
        try {
            UIRender.rect(0F, 0F, width, height, Theme.veil);
        } finally {
            UIRender.endFrame();
        }

        HudManager.renderDungeonEditor(new ScaledResolution(Minecraft.getMinecraft()), mouseX, mouseY);
        drawDungeonMapPreview(mouseX, mouseY);

        UIRender.beginFrame();
        try {
            drawSelection();
            drawGuides();
            drawSolidPanel(mouseX, mouseY);
        } finally {
            UIRender.endFrame();
        }
    }

    /** Editor-only visual placeholder for the real map's existing saved placement. Runtime map rendering remains bare. */
    private void drawDungeonMapPreview(int mouseX, int mouseY) {
        int size = mapSize();
        int left = mapLeft(size);
        int top = mapTop(size);
        UIRender.beginFrame();
        try {
            int fill = UIRender.withAlpha(Theme.card, .92F);
            UIRender.rect(left, top, size, size, fill);
            for (int index = 1; index < 4; index++) {
                float x = left + size * index / 4F;
                float y = top + size * index / 4F;
                int line = UIRender.withAlpha(Theme.textSecondary, .32F);
                UIRender.line(x, top, x, top + size, 1F, line);
                UIRender.line(left, y, left + size, y, 1F, line);
            }
            UIRender.text(fr(), "MAP", left + 5F, top + 5F, Theme.textPrimary, .72F, false);
            if (selected == Target.MAP || inside(mouseX, mouseY, left, top, size, size)) {
                UIRender.roundRectRing(left - 2F, top - 2F, size + 4F, size + 4F, 3F, 1F, Theme.accent);
            }
        } finally {
            UIRender.endFrame();
        }
    }

    private void drawSelection() {
        if (selected == Target.MAP) return;
        HudElement element = selected.element;
        if (element == null) return;
        float x = element.leftFor(width) - 3F;
        float y = element.topFor(height) - 3F;
        float w = element.pixelWidth() + 6F;
        float h = element.pixelHeight() + 6F;
        UIRender.roundRectRing(x, y, w, h, 3F, 1F, Theme.accent);
        UIRender.circle(x, y, 2F, Theme.accent);
        UIRender.circle(x + w, y + h, 2F, Theme.accent);
    }

    private void drawGuides() {
        if (guides == 0) return;
        if ((guides & HudPlacement.GUIDE_CENTER_X) != 0) UIRender.rect(width * .5F - .5F, 0F, 1F, height, Theme.accent);
        if ((guides & HudPlacement.GUIDE_CENTER_Y) != 0) UIRender.rect(0F, height * .5F - .5F, width, 1F, Theme.accent);
        if ((guides & HudPlacement.GUIDE_LEFT) != 0) UIRender.rect(0F, 0F, 1F, height, Theme.accent);
        if ((guides & HudPlacement.GUIDE_RIGHT) != 0) UIRender.rect(width - 1F, 0F, 1F, height, Theme.accent);
        if ((guides & HudPlacement.GUIDE_TOP) != 0) UIRender.rect(0F, 0F, width, 1F, Theme.accent);
        if ((guides & HudPlacement.GUIDE_BOTTOM) != 0) UIRender.rect(0F, height - 1F, width, 1F, Theme.accent);
    }

    private void drawSolidPanel(int mouseX, int mouseY) {
        // This editor is intentionally solid rather than a glass overlay: placement targets remain readable over
        // active dungeon content at every configured GUI opacity.
        UIRender.roundRectBorder(panelX, panelY, panelW, panelH, Theme.radius,
                Theme.ITEM_ANIMATION_PANEL, Theme.ITEM_ANIMATION_PANEL, Theme.ITEM_ANIMATION_PANEL_BORDER);
        FontRenderer font = fr();
        UIRender.text(font, "Dungeon HUD Editor", panelX + PANEL_PAD, panelY + 10F, Theme.textPrimary, 1F, false);
        UIRender.text(font, "Close", panelX + panelW - 44F, panelY + 10F, Theme.textSecondary, .78F, false);
        UIRender.line(panelX + PANEL_PAD, panelY + 24F, panelX + panelW - PANEL_PAD, panelY + 24F, 1F,
                Theme.expandedDivider);

        float pickerY = panelY + 31F;
        float pickerW = (panelW - PANEL_PAD * 2F - 8F) / Target.values().length;
        for (int index = 0; index < Target.values().length; index++) {
            Target target = Target.values()[index];
            float x = panelX + PANEL_PAD + index * (pickerW + 4F);
            drawButton(x, pickerY, pickerW, CONTROL_H, fit(font, target.label, Math.max(1F, pickerW - 8F)),
                    target == selected, mouseX, mouseY);
        }

        float y = pickerY + CONTROL_H + 10F;
        String targetName = selected == Target.MAP ? "Dungeon Map" : selected.element.name;
        UIRender.text(font, fit(font, targetName, Math.max(1F, panelW - 160F)), panelX + PANEL_PAD, y + 5F,
                Theme.textSecondary, .75F, false);
        float toggleW = 48F;
        float toggleX = panelX + panelW - PANEL_PAD - toggleW;
        drawButton(toggleX, y, toggleW, CONTROL_H, selectedEnabled() ? "On" : "Off", selectedEnabled(), mouseX, mouseY);

        y += 26F;
        float sizeX = panelX + PANEL_PAD;
        drawButton(sizeX, y, 28F, CONTROL_H, "-", false, mouseX, mouseY);
        drawButton(sizeX + 33F, y, 28F, CONTROL_H, "+", false, mouseX, mouseY);
        String size = selected == Target.MAP ? "Map size " + AppleSettings.effectiveDungeonMapScale() + "%"
                : "Size " + Math.round(selected.element.scale * 100F) + "%";
        UIRender.text(font, fit(font, size, Math.max(1F, panelW - 200F)), sizeX + 68F, y + 5F,
                Theme.textSecondary, .75F, false);
        float resetW = 74F;
        drawButton(panelX + panelW - PANEL_PAD - resetW, y, resetW, CONTROL_H, "Reset target", false, mouseX, mouseY);

        String instruction = selected == Target.MAP ? "Drag the map preview to move it. Size uses the existing map scale."
                : "Drag the selected preview to move it. Use - and + to resize it.";
        UIRender.text(font, fit(font, instruction, Math.max(1F, panelW - PANEL_PAD * 2F)), panelX + PANEL_PAD,
                panelY + panelH - 17F, Theme.textSecondary, .68F, false);
    }

    private void drawButton(float x, float y, float w, float h, String label, boolean active, int mouseX, int mouseY) {
        boolean hot = inside(mouseX, mouseY, x, y, w, h);
        int fill = active ? Theme.accent : Theme.buttonFill;
        int text = active ? Theme.windowTop : (hot ? Theme.textPrimary : Theme.textSecondary);
        UIRender.roundRectBorder(x, y, w, h, h * .5F, fill, fill, active ? Theme.accent : Theme.border);
        float scale = .68F;
        UIRender.text(fr(), label, x + (w - fr().getStringWidth(label) * scale) * .5F, y + 5F, text, scale, false);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) return;
        if (inside(mouseX, mouseY, panelX + panelW - 52F, panelY, 52F, 24F)) {
            close();
            return;
        }
        float pickerY = panelY + 31F;
        float pickerW = (panelW - PANEL_PAD * 2F - 8F) / Target.values().length;
        for (int index = 0; index < Target.values().length; index++) {
            float x = panelX + PANEL_PAD + index * (pickerW + 4F);
            if (inside(mouseX, mouseY, x, pickerY, pickerW, CONTROL_H)) {
                selected = Target.values()[index];
                guides = 0;
                return;
            }
        }
        float rowY = pickerY + CONTROL_H + 10F;
        if (inside(mouseX, mouseY, panelX + panelW - PANEL_PAD - 48F, rowY, 48F, CONTROL_H)) {
            setSelectedEnabled(!selectedEnabled());
            return;
        }
        rowY += 26F;
        if (inside(mouseX, mouseY, panelX + PANEL_PAD, rowY, 28F, CONTROL_H)) {
            resizeSelected(-1);
            return;
        }
        if (inside(mouseX, mouseY, panelX + PANEL_PAD + 33F, rowY, 28F, CONTROL_H)) {
            resizeSelected(1);
            return;
        }
        if (inside(mouseX, mouseY, panelX + panelW - PANEL_PAD - 74F, rowY, 74F, CONTROL_H)) {
            resetSelected();
            return;
        }
        // After the concrete controls, let a selected preview remain draggable even where its saved position passes
        // behind the centered panel on a compact screen. Controls above always retain click priority.
        if (selected == Target.MAP) {
            int size = mapSize();
            int left = mapLeft(size);
            int top = mapTop(size);
            if (inside(mouseX, mouseY, left, top, size, size)) {
                dragging = true;
                grabX = mouseX - left;
                grabY = mouseY - top;
            }
            return;
        }
        HudElement element = selected.element;
        if (element != null && inside(mouseX, mouseY, element.leftFor(width), element.topFor(height),
                element.pixelWidth(), element.pixelHeight())) {
            dragging = true;
            grabX = mouseX - element.leftFor(width);
            grabY = mouseY - element.topFor(height);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (!dragging) return;
        if (selected == Target.MAP) {
            int size = mapSize();
            guides = AppleSettings.setDungeonMapPosition(width, height, size, Math.round(mouseX - grabX),
                    Math.round(mouseY - grabY), true, (int) SNAP);
        } else if (selected.element != null) {
            HudElement element = selected.element;
            guides = element.place.setFromAbsolute(width, height, element.pixelWidth(), element.pixelHeight(),
                    Math.round(mouseX - grabX), Math.round(mouseY - grabY), true, (int) SNAP);
        }
        change();
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        if (!dragging) return;
        dragging = false;
        guides = 0;
        persist();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) close();
    }

    private boolean selectedEnabled() {
        return selected == Target.MAP ? AppleSettings.dungeonMap : selected.element != null && selected.element.enabled;
    }

    private void setSelectedEnabled(boolean value) {
        if (selected == Target.MAP) AppleSettings.dungeonMap = value;
        else if (selected.element != null) selected.element.enabled = value;
        change();
        persist();
    }

    private void resizeSelected(int direction) {
        if (selected == Target.MAP) {
            AppleSettings.dungeonMapScaleOn = true;
            AppleSettings.dungeonMapScale = Math.max(AppleSettings.DUNGEON_MAP_SCALE_MIN,
                    Math.min(AppleSettings.DUNGEON_MAP_SCALE_MAX, AppleSettings.dungeonMapScale + direction * 5));
        } else if (selected.element != null) {
            selected.element.scale = Math.max(HudElement.MIN_SCALE,
                    Math.min(HudElement.MAX_SCALE, selected.element.scale + direction * .10F));
            selected.element.scaleOn = true;
        }
        change();
        persist();
    }

    private void resetSelected() {
        if (selected == Target.MAP) {
            AppleSettings.resetDungeonMapPosition();
            AppleSettings.dungeonMapScaleOn = true;
            AppleSettings.dungeonMapScale = 100;
            AppleSettings.dungeonMapZoom = 100;
        } else if (selected.element != null) {
            selected.element.resetAll();
        }
        change();
        persist();
    }

    private int mapSize() {
        return AppleSettings.dungeonMapPixelSize(width, height);
    }

    private int mapLeft(int size) {
        return AppleSettings.dungeonMapLeft(width, height, size);
    }

    private int mapTop(int size) {
        return AppleSettings.dungeonMapTop(width, height, size);
    }

    private void close() {
        persist();
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }
}
