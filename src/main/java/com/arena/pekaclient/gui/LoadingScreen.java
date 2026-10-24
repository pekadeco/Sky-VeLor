package com.arena.pekaclient.gui;

import com.arena.pekaclient.PekaVersion;
import com.arena.pekaclient.render.Anim;
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

    private static final int BLACK = Theme.LOADING_BLACK;
    /** Bottom of the gradient. Deep blue, the Sky VeLor brand colour. */
    private static final int DEEP_BLUE = Theme.LOADING_DEEP_BLUE;
    private static final int DOTS = 12;
    private static final float BAR_W = 180f;

    private final GuiScreen returnTo;
    private final Anim fade = new Anim(0f);
    private final AppleButton continueBtn = new AppleButton("Continue", 110f, 26f, true, this::finish);
    /** Full scaled viewport, kept as a shared-layout rect so the loading composition never assumes 854x480. */
    private final GuiLayoutToolkit.Rect safeViewport = new GuiLayoutToolkit.Rect();
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
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        float a = fade.get();
        GuiLayoutToolkit.clampPanel(safeViewport, 0f, 0f, width, height, width, height);
        float cx = safeViewport.x + safeViewport.w * 0.5f;
        // A 105px composition fits even the smallest Minecraft scaled viewport. Larger screens retain the
        // original centred visual weight; short screens hide only nonessential footer/status chrome.
        float blockH = Math.min(105f, Math.max(86f, safeViewport.h - 12f));
        float top = Math.max(safeViewport.y + 6f, safeViewport.y + (safeViewport.h - blockH) * .5f);
        float wordY = top;
        float versionY = wordY + 26f;
        float spinnerY = versionY + 29f;
        float barY = Math.min(safeViewport.bottom() - 34f, spinnerY + 18f);
        float barW = Math.min(BAR_W, Math.max(40f, safeViewport.w - 24f));

        UIRender.beginFrame();
        UIRender.alphaMul = 1f;
        // Vertical gradient: black at the top, deep blue at the bottom.
        UIRender.roundRect(0f, 0f, width, height, 0f, BLACK, DEEP_BLUE, 1f);

        // Wordmark, centred, with the version under it.
        String name = PekaVersion.DISPLAY_NAME;
        float wordScale = Math.min(3f, Math.max(.75f, (safeViewport.w - 16f) / Math.max(1f, fr.getStringWidth(name))));
        float wordW = fr.getStringWidth(name) * wordScale;
        UIRender.text(fr, name, cx - wordW * 0.5f, wordY, UIRender.withAlpha(Theme.LOADING_WORDMARK, a), wordScale, false);
        String ver = GuiLayoutToolkit.ellipsize(PekaVersion.NAME, Math.max(1f, safeViewport.w - 24f),
                metrics(fr));
        UIRender.text(fr, ver, cx - fr.getStringWidth(ver) * 0.5f, versionY,
                UIRender.withAlpha(Theme.LOADING_VERSION, a), 1f, false);

        // Spinner: twelve dots on a ring, with a moving bright head. Motion only, no progress meaning.
        float t = (System.nanoTime() - startNs) / 1.0e9f;
        int head = (int) (t * DOTS * 1.2f) % DOTS;
        float ring = 14f;
        for (int i = 0; i < DOTS; i++) {
            double ang = Math.PI * 2.0 * i / DOTS;
            float dx = cx + ring * (float) Math.cos(ang);
            float dy = spinnerY + ring * (float) Math.sin(ang);
            int age = (head - i + DOTS) % DOTS;
            float alpha = (1f - age / (float) DOTS) * a;
            UIRender.circle(dx, dy, 1.8f, UIRender.withAlpha(Theme.LOADING_WORDMARK, Math.max(0.12f, alpha)));
        }

        // Progress: the real count of finished tasks.
        int total = StartupTasks.total();
        int done = StartupTasks.completed();
        UIRender.roundRect(cx - barW * 0.5f, barY, barW, 3f, 1.5f, Theme.LOADING_TRACK, Theme.LOADING_TRACK, 0.5f);
        if (total > 0) {
            float frac = Math.min(1f, done / (float) total);
            UIRender.roundRect(cx - barW * 0.5f, barY, barW * frac, 3f, 1.5f, Theme.ACCENT_BLUE, Theme.ACCENT_BLUE, 0.5f);
        }

        String status;
        int statusCol;
        if (StartupTasks.failed()) {
            status = StartupTasks.failureMessage();
            statusCol = Theme.LOADING_ERROR;
        } else if (StartupTasks.hasWork()) {
            status = StartupTasks.currentLabel() + " (" + (done + 1) + " of " + total + ")";
            statusCol = Theme.LOADING_STATUS;
        } else {
            status = "Ready";
            statusCol = Theme.LOADING_STATUS;
        }
        if (!(StartupTasks.failed() && safeViewport.h < 150f)) {
            float maxW = Math.max(1f, safeViewport.w - 24f);
            String shown = GuiLayoutToolkit.ellipsize(status, maxW, metrics(fr));
            UIRender.text(fr, shown, cx - fr.getStringWidth(shown) * 0.5f, barY + 12f, statusCol, 1f, false);
        }

        if (safeViewport.h >= 150f) {
            String foot = GuiLayoutToolkit.ellipsize("Forge client for Minecraft 1.8.9",
                    Math.max(1f, safeViewport.w - 24f), metrics(fr));
            UIRender.text(fr, foot, cx - fr.getStringWidth(foot) * 0.5f, safeViewport.bottom() - 22f,
                    Theme.LOADING_DIM, 1f, false);
        }
        UIRender.endFrame();

        if (StartupTasks.failed()) {
            continueBtn.x = Math.max(safeViewport.x, Math.min(cx - continueBtn.w * 0.5f,
                    safeViewport.right() - continueBtn.w));
            continueBtn.y = Math.min(barY + (safeViewport.h < 150f ? 16f : 34f),
                    safeViewport.bottom() - continueBtn.h);
            UIRender.beginFrame();
            continueBtn.draw(mx, my);
            UIRender.endFrame();
        }
    }

    private static GuiLayoutToolkit.TextMetrics metrics(final FontRenderer fr) {
        return new GuiLayoutToolkit.TextMetrics() {
            public float width(String value) { return fr.getStringWidth(value); }
            public float height() { return fr.FONT_HEIGHT; }
        };
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
