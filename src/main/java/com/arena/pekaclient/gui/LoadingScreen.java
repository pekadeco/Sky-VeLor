package com.arena.pekaclient.gui;

import com.arena.pekaclient.PekaVersion;
import com.arena.pekaclient.render.Anim;
import com.arena.pekaclient.render.GlobalFontManager;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import com.arena.pekaclient.startup.StartupTasks;
import com.arena.pekaclient.widget.AppleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * Sky VeLor loading screen: a black-to-deep-blue gradient, the wordmark, the version, a small spinner, and a
 * progress bar that shows the real count of finished startup tasks.
 *
 * The screen is shown only while {@link StartupTasks} has work to run. It runs one task per game tick, and
 * closes itself when the last task has finished. If a task fails, the error is shown in red and the screen
 * stays until the player presses Continue. A failure is never shown as a finished startup.
 *
 * Limitation: this replaces only Sky VeLor's own startup work. The Mojang and Forge splash shown before
 * Minecraft's own loading starts is drawn before any mod loads, so a mod cannot replace it without a
 * core hook. That splash is not changed.
 */
public final class LoadingScreen extends GuiScreen {

    private static final int BLACK = 0xFF000000;
    /** Bottom of the gradient. Deep blue, the Sky VeLor brand colour. */
    private static final int DEEP_BLUE = 0xFF0B2350;
    private static final int DOTS = 12;
    private static final float BAR_W = 180f;

    private final GuiScreen returnTo;
    private final Anim fade = new Anim(0f);
    private final AppleButton continueBtn = new AppleButton("Continue", 110f, 26f, true, this::finish);
    private long startNs;

    public LoadingScreen(GuiScreen returnTo) {
        this.returnTo = returnTo;
        fade.to(1f);
    }

    @Override
    public void initGui() {
        super.initGui();
        if (startNs == 0L) {
            startNs = System.nanoTime();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        fade.update(1f / 20f, 6f);
        continueBtn.update(1f / 20f);
        if (StartupTasks.hasWork()) {
            // One real task per tick. Its result is only counted when it has completed.
            StartupTasks.runNext();
        }
        if (!StartupTasks.hasWork() && !StartupTasks.failed()) {
            finish();
        }
    }

    private void finish() {
        Minecraft.getMinecraft().displayGuiScreen(returnTo);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        FontRenderer fr = GlobalFontManager.rendererFor(Minecraft.getMinecraft().fontRendererObj);
        float a = fade.get();
        float cx = width * 0.5f;
        float cy = height * 0.5f;

        UIRender.beginFrame();
        UIRender.alphaMul = 1f;
        // Vertical gradient: black at the top, deep blue at the bottom.
        UIRender.roundRect(0f, 0f, width, height, 0f, BLACK, DEEP_BLUE, 1f);

        // Wordmark, centred, with the version under it.
        String name = PekaVersion.DISPLAY_NAME;
        float wordW = fr.getStringWidth(name) * 3f;
        UIRender.text(fr, name, cx - wordW * 0.5f, cy - 40f, UIRender.withAlpha(0xFFFFFFFF, a), 3f, false);
        String ver = PekaVersion.NAME;
        UIRender.text(fr, ver, cx - fr.getStringWidth(ver) * 0.5f, cy - 16f,
                UIRender.withAlpha(0xFF9CC3FF, a), 1f, false);

        // Spinner: twelve dots on a ring, with a moving bright head. Motion only, no progress meaning.
        float t = (System.nanoTime() - startNs) / 1.0e9f;
        int head = (int) (t * DOTS * 1.2f) % DOTS;
        float ring = 14f;
        for (int i = 0; i < DOTS; i++) {
            double ang = Math.PI * 2.0 * i / DOTS;
            float dx = cx + ring * (float) Math.cos(ang);
            float dy = cy + 12f + ring * (float) Math.sin(ang);
            int age = (head - i + DOTS) % DOTS;
            float alpha = (1f - age / (float) DOTS) * a;
            UIRender.circle(dx, dy, 1.8f, UIRender.withAlpha(0xFFFFFFFF, Math.max(0.12f, alpha)));
        }

        // Progress: the real count of finished tasks.
        int total = StartupTasks.total();
        int done = StartupTasks.completed();
        float barY = cy + 50f;
        UIRender.roundRect(cx - BAR_W * 0.5f, barY, BAR_W, 3f, 1.5f, 0x33FFFFFF, 0x33FFFFFF, 0.5f);
        if (total > 0) {
            float frac = Math.min(1f, done / (float) total);
            UIRender.roundRect(cx - BAR_W * 0.5f, barY, BAR_W * frac, 3f, 1.5f, Theme.accent, Theme.accent, 0.5f);
        }

        String status;
        int statusCol;
        if (StartupTasks.failed()) {
            status = StartupTasks.failureMessage();
            statusCol = 0xFFFF7A70;
        } else if (StartupTasks.hasWork()) {
            status = StartupTasks.currentLabel() + " (" + (done + 1) + " of " + total + ")";
            statusCol = 0xFFA1A1A8;
        } else {
            status = "Ready";
            statusCol = 0xFFA1A1A8;
        }
        float maxW = Math.max(120f, width - 24f);
        String shown = GuiLayoutToolkit.ellipsize(status, maxW, new GuiLayoutToolkit.TextMetrics() { public float width(String value) { return fr.getStringWidth(value); } public float height() { return fr.FONT_HEIGHT; } });
        UIRender.text(fr, shown, cx - fr.getStringWidth(shown) * 0.5f, barY + 12f, statusCol, 1f, false);

        String foot = "Forge client for Minecraft 1.8.9";
        UIRender.text(fr, foot, cx - fr.getStringWidth(foot) * 0.5f, height - 22f, 0xFF8E8E93, 1f, false);
        UIRender.endFrame();

        if (StartupTasks.failed()) {
            continueBtn.x = cx - continueBtn.w * 0.5f;
            continueBtn.y = barY + 34f;
            UIRender.beginFrame();
            continueBtn.draw(mx, my);
            UIRender.endFrame();
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button == 0 && StartupTasks.failed() && continueBtn.contains(mx, my)) {
            finish();
        }
    }

    @Override
    protected void keyTyped(char c, int code) throws IOException {
        // Escape only leaves the screen once a failure is on display. Otherwise startup work keeps running.
        if (code == Keyboard.KEY_ESCAPE && StartupTasks.failed()) {
            finish();
        }
    }
}
