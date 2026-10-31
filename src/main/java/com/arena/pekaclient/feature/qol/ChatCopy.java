/*
 * This modified implementation follows the Better NEF / NotEnoughFakepixel behavior.
 * Maintained by ii8we; review LICENSE and NOTICE.md before use or redistribution.
 * Original code and third-party components remain subject to their applicable license terms.
 */

package com.arena.pekaclient.feature.qol;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.ChatChannelTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

/** Shift + left-click copies an entire visible chat message. Ctrl also copies formatting codes. */
public final class ChatCopy {
    private static final long FLASH_MS = 350L;
    private static long copiedAt;

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (event == null || !(event.gui instanceof GuiChat) || ChatChannelTabs.isNativeRendererActive() || !AppleSettings.chatCopyEnabled
                || Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()
                || !GuiScreen.isShiftKeyDown()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.ingameGUI == null || mc.ingameGUI.getChatGUI() == null) return;
        GuiNewChat chat = mc.ingameGUI.getChatGUI();
        if (!(chat instanceof ChatCopyAccess)) return;

        ChatLine line = ((ChatCopyAccess) chat).lunaria$getChatLineAt(Mouse.getX(), Mouse.getY());
        if (line == null) return;
        IChatComponent message = line instanceof ChatLineAccess
                ? ((ChatLineAccess) line).lunaria$getFullMessage() : null;
        if (message == null) message = line.getChatComponent();

        String text = ChatCopyTextPolicy.selectText(message.getUnformattedText(), message.getFormattedText(),
                GuiScreen.isCtrlKeyDown());
        if (text.isEmpty()) return;

        GuiScreen.setClipboardString(text);
        copiedAt = System.currentTimeMillis();
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event == null || !(event.gui instanceof GuiChat) || ChatChannelTabs.isNativeRendererActive() || !AppleSettings.chatCopyEnabled
                || !GuiScreen.isShiftKeyDown()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.ingameGUI == null || mc.ingameGUI.getChatGUI() == null) return;
        GuiNewChat chat = mc.ingameGUI.getChatGUI();
        if (!(chat instanceof ChatCopyAccess)) return;

        ChatCopyAccess access = (ChatCopyAccess) chat;
        int row = access.lunaria$getVisibleRowAt(Mouse.getX(), Mouse.getY());
        if (row < 0) return;
        int firstRow = access.lunaria$getMessageFirstRow(row);
        int lastRow = access.lunaria$getMessageLastRow(row);

        float scale = chat.getChatScale();
        int left = 2;
        int right = Math.round(2 + scale * (MathHelper.ceiling_float_int(chat.getChatWidth() / scale) + 4));
        int top = Math.round(event.gui.height - 28 - scale * (lastRow + 1) * 9);
        int bottom = Math.round(event.gui.height - 28 - scale * firstRow * 9);

        boolean flash = System.currentTimeMillis() - copiedAt < FLASH_MS;
        Gui.drawRect(left, top, right, bottom, flash ? 0x5055FF55 : 0x30FFFFFF);

        int iconBottom = Math.round(event.gui.height - 28 - scale * lastRow * 9);
        if (iconBottom - top >= 8) {
            int color = GuiScreen.isCtrlKeyDown() ? 0xFFFFAA00 : 0xFFFFFFFF;
            int x = right - 11;
            int y = top + (iconBottom - top - 9) / 2;
            Gui.drawRect(x, y, x + 5, y + 1, color);
            Gui.drawRect(x, y, x + 1, y + 6, color);
            Gui.drawRect(x + 2, y + 2, x + 8, y + 9, 0xFF202020);
            Gui.drawRect(x + 2, y + 2, x + 8, y + 3, color);
            Gui.drawRect(x + 2, y + 8, x + 8, y + 9, color);
            Gui.drawRect(x + 2, y + 2, x + 3, y + 9, color);
            Gui.drawRect(x + 7, y + 2, x + 8, y + 9, color);
            Gui.drawRect(x + 4, y + 4, x + 6, y + 5, color);
            Gui.drawRect(x + 4, y + 6, x + 6, y + 7, color);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
