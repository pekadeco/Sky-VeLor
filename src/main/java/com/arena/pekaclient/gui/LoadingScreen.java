package com.arena.pekaclient.gui;

import com.arena.pekaclient.PekaVersion;
import com.arena.pekaclient.render.Anim;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.startup.StartupTasks;
import com.arena.pekaclient.widget.AppleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * Lunaria's post-mod startup screen. The supplied illustration is cover-fit behind a truthful task-progress bar.
 * Forge's pre-mod splash has already run before this class can exist; this screen owns only Lunaria startup work.
 */
public final class LoadingScreen extends GuiScreen {
    private static final float BAR_MAX_W = 260F;
    private final GuiScreen returnTo;
    private final Anim progressAnim = new Anim(0F);
    private final AppleButton continueBtn = new AppleButton("Continue", 110F, 26F, true, this::finish);

    public LoadingScreen(GuiScreen returnTo) {
        this.returnTo = returnTo;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (StartupTasks.hasWork()) StartupTasks.runNext();
        if (!StartupTasks.hasWork() && !StartupTasks.failed()) finish();
    }

    private void finish() {
        Minecraft.getMinecraft().displayGuiScreen(returnTo);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        float centerX = width * .5F;
        float panelW = Math.min(BAR_MAX_W + 44F, Math.max(220F, width - 32F));
        float panelH = 124F;
        float panelX = centerX - panelW * .5F;
        float panelY = Math.max(22F, height * .61F - panelH * .5F);

        // Cover-fit from the center: no distortion on narrow Pojav or ultrawide screens.
        LunariaArtwork.drawLoadingCover(0, 0, width, height, 1F);
        UIRender.beginFrame();
        try {
            // A quiet dark scrim keeps the full-bleed art visible while preserving readable progress text.
            UIRender.roundRect(0F, 0F, width, height, 0F, 0x33070B22, 0x66140B33, 1F);
            UIRender.roundRect(panelX - 3F, panelY - 3F, panelW + 6F, panelH + 6F, 16F,
                    0x552462C8, 0x664A1E9E, 1F);
            UIRender.roundRect(panelX, panelY, panelW, panelH, 14F, 0xD91A2048, 0xD91E163D, 1F);
        } finally {
            UIRender.endFrame();
        }

        float logoW = Math.min(206F, Math.max(132F, width * .34F));
        LunariaArtwork.drawLogo(centerX, panelY - Math.min(132F, height * .20F), logoW, 1F);

        int total = StartupTasks.total();
        int completed = StartupTasks.completed();
        float exactProgress = total <= 0 ? (StartupTasks.hasWork() ? 0F : 1F)
                : Math.max(0F, Math.min(1F, completed / (float) total));
        progressAnim.to(exactProgress);
        progressAnim.update(1F / 60F, 10F);
        int percent = Math.round(exactProgress * 100F);
        String step = StartupTasks.failed() ? StartupTasks.failureMessage()
                : StartupTasks.hasWork() ? StartupTasks.currentLabel() : "Ready";
        if (step == null || step.length() == 0) step = "Starting";
        String shown = font.trimStringToWidth(step, Math.max(80, (int) panelW - 34));
        String status = shown + " \u2022 " + percent + "%";

        float barX = panelX + 18F;
        float barY = panelY + 73F;
        float barW = panelW - 36F;
        UIRender.beginFrame();
        try {
            UIRender.text(font, PekaVersion.NAME, centerX - font.getStringWidth(PekaVersion.NAME) * .5F,
                    panelY + 17F, 0xFFB8C8FF, .9F, false);
            UIRender.text(font, status, centerX - font.getStringWidth(status) * .5F, panelY + 43F,
                    StartupTasks.failed() ? 0xFFFFB3BF : 0xFFF1EEFF, .84F, false);
            // Soft blue-to-purple glow, then the readable rounded track and real completed-task fill.
            UIRender.roundRect(barX - 4F, barY - 4F, barW + 8F, 15F, 7F, 0x303CA8FF, 0x305E45D6, 1F);
            UIRender.roundRect(barX, barY, barW, 7F, 3.5F, 0x99383C72, 0x99383C72, 1F);
            float fill = barW * Math.max(0F, Math.min(1F, progressAnim.get()));
            if (fill > 0F) UIRender.roundRect(barX, barY, fill, 7F, 3.5F, 0xFF66C7FF, 0xFFA875FF, 1F);
            String detail = total > 0 ? completed + " of " + total + " startup tasks" : "Lunaria Mod for Minecraft 1.8.9";
            UIRender.text(font, detail, centerX - font.getStringWidth(detail) * .5F, barY + 17F,
                    0xFFBAC0DF, .70F, false);
        } finally {
            UIRender.endFrame();
        }

        if (StartupTasks.failed()) {
            continueBtn.x = centerX - continueBtn.w * .5F;
            continueBtn.y = panelY + 96F;
            UIRender.beginFrame();
            try {
                continueBtn.draw(mouseX, mouseY);
            } finally {
                UIRender.endFrame();
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button == 0 && StartupTasks.failed() && continueBtn.contains(mouseX, mouseY)) finish();
    }

    @Override
    protected void keyTyped(char character, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE && StartupTasks.failed()) finish();
    }
}
