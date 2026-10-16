import os, textwrap
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'stubs')

FILES = {}

def add(path, code):
    FILES[path] = textwrap.dedent(code).lstrip('\n')

# ---------------- net.minecraft.client.gui ----------------
add('net/minecraft/client/gui/Gui.java', '''
package net.minecraft.client.gui;

/** Faithful subset of Gui for Minecraft 1.8.9 (MCP stable_22 names). */
public class Gui {
    /** 1.8.9 sorts the corners, then draws one quad with alpha blending. */
    public static void drawRect(int left, int top, int right, int bottom, int color) {
        if (left < right) { int i = left; left = right; right = i; }
        if (top < bottom) { int j = top; top = bottom; bottom = j; }
        com.arena.pekaclient.verify.Raster.rect(left, top, right, bottom, color);
    }

    protected void drawCenteredString(net.minecraft.client.gui.FontRenderer fr, String text, int x, int y, int color) {
        fr.drawString(text, x - fr.getStringWidth(text) / 2, y, color);
    }

    public void drawString(net.minecraft.client.gui.FontRenderer fr, String text, int x, int y, int color) {
        fr.drawString(text, x, y, color);
    }
}
''')

add('net/minecraft/client/gui/GuiScreen.java', '''
package net.minecraft.client.gui;

import java.io.IOException;

/** Faithful subset of GuiScreen for Minecraft 1.8.9. */
public class GuiScreen extends Gui {
    public int width;
    public int height;
    public FontRenderer fontRendererObj = new FontRenderer();
    public net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            this.mc.displayGuiScreen((GuiScreen) null);
            if (this.mc.currentScreen == null) {
                this.mc.setIngameFocus();
            }
        }
    }

    public void initGui() { }
    public void onGuiClosed() { }
    public boolean doesGuiPauseGame() { return true; }
    public void updateScreen() { }
}
''')

add('net/minecraft/client/gui/FontRenderer.java', '''
package net.minecraft.client.gui;

/** Faithful subset of FontRenderer. drawString keeps the 1.8.9 ARGB colour handling. */
public class FontRenderer {
    public int FONT_HEIGHT = 9;

    public int drawString(String text, int x, int y, int color) {
        return this.drawString(text, (float) x, (float) y, color, false);
    }

    public int drawString(String text, float x, float y, int color, boolean dropShadow) {
        if (text == null) { text = "null"; }
        com.arena.pekaclient.verify.Raster.text(text, x, y, color, dropShadow);
        return (int) x + this.getStringWidth(text);
    }

    public int getStringWidth(String text) {
        return text == null ? 0 : text.length() * 6 - 1;
    }
}
''')

# ---------------- renderer ----------------
add('net/minecraft/client/renderer/GlStateManager.java', '''
package net.minecraft.client.renderer;

/** Records the modelview scale so the pixel canvas matches what OpenGL would rasterise. */
public class GlStateManager {
    private static final java.util.ArrayDeque<float[]> STACK = new java.util.ArrayDeque<float[]>();
    private static float sx = 1.0F;
    private static float sy = 1.0F;

    public static void pushMatrix() { STACK.push(new float[] { sx, sy }); }
    public static void popMatrix() { if (!STACK.isEmpty()) { float[] t = STACK.pop(); sx = t[0]; sy = t[1]; } }
    public static void scale(float x, float y, float z) { sx *= x; sy *= y; }
    public static void translate(float x, float y, float z) { }
    public static void color(float r, float g, float b, float a) { }
    public static void color(float r, float g, float b) { }
    public static float scaleX() { return sx; }
    public static float scaleY() { return sy; }
    public static void enableTexture2D() { }
    public static void disableTexture2D() { }
    public static void enableBlend() { }
    public static void disableBlend() { }
    public static void enableAlpha() { }
    public static void disableAlpha() { }
    public static void enableLighting() { }
    public static void disableLighting() { }
    public static void tryBlendFuncSeparate(int a, int b, int c, int d) { }
    public static void reset() { STACK.clear(); sx = 1.0F; sy = 1.0F; }
}
''')

add('net/minecraft/client/gui/ScaledResolution.java', '''
package net.minecraft.client.gui;

public class ScaledResolution {
    private final int scaledWidth;
    private final int scaledHeight;

    public ScaledResolution(int width, int height, int scaleFactor) {
        this.scaledWidth = width / scaleFactor;
        this.scaledHeight = height / scaleFactor;
    }

    public int getScaledWidth() { return this.scaledWidth; }
    public int getScaledHeight() { return this.scaledHeight; }
    public int getScaleFactor() { return 2; }
}
''')

# ---------------- inventory ----------------
add('net/minecraft/util/IChatComponent.java', '''
package net.minecraft.util;

public interface IChatComponent {
    String getUnformattedText();
    String getFormattedText();
}
''')

add('net/minecraft/util/ChatComponentText.java', '''
package net.minecraft.util;

public class ChatComponentText implements IChatComponent {
    private final String text;

    public ChatComponentText(String text) { this.text = text; }

    public String getUnformattedText() { return this.text; }
    public String getFormattedText() { return this.text; }
}
''')

add('net/minecraft/inventory/IInventory.java', '''
package net.minecraft.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

public interface IInventory {
    int getSizeInventory();
    ItemStack getStackInSlot(int index);
    IChatComponent getDisplayName();
    String getName();
    int getInventoryStackLimit();
}
''')

add('net/minecraft/inventory/InventoryBasic.java', '''
package net.minecraft.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

/** A simple chest inventory, used by the verification harness. */
public class InventoryBasic implements IInventory {
    private final String title;
    private final ItemStack[] items;

    public InventoryBasic(String title, int size) {
        this.title = title;
        this.items = new ItemStack[size];
    }

    public int getSizeInventory() { return this.items.length; }
    public ItemStack getStackInSlot(int index) { return this.items[index]; }
    public IChatComponent getDisplayName() { return new ChatComponentText(this.title); }
    public String getName() { return this.title; }
    public int getInventoryStackLimit() { return 64; }

    public void set(int index, ItemStack stack) { this.items[index] = stack; }
}
''')

add('net/minecraft/inventory/Slot.java', '''
package net.minecraft.inventory;

import net.minecraft.item.ItemStack;

public class Slot {
    public int xDisplayPosition;
    public int yDisplayPosition;
    public int slotNumber;
    private ItemStack stack;

    public Slot(int slotNumber, int x, int y) {
        this.slotNumber = slotNumber;
        this.xDisplayPosition = x;
        this.yDisplayPosition = y;
    }

    public ItemStack getStack() { return this.stack; }
    public void putStack(ItemStack stack) { this.stack = stack; }
    public boolean getHasStack() { return this.stack != null; }
}
''')

add('net/minecraft/inventory/Container.java', '''
package net.minecraft.inventory;

import java.util.ArrayList;
import java.util.List;

public class Container {
    public int windowId;
    public List<Slot> inventorySlots = new ArrayList<Slot>();

    protected Slot addSlotToContainer(Slot slot) {
        slot.slotNumber = this.inventorySlots.size();
        this.inventorySlots.add(slot);
        return slot;
    }

    public Slot getSlot(int index) { return this.inventorySlots.get(index); }
    public int getInventorySize() { return this.inventorySlots.size(); }
}
''')

add('net/minecraft/inventory/ContainerChest.java', '''
package net.minecraft.inventory;

/** Faithful subset of ContainerChest: windowId, the slot list, and the lower chest inventory. */
public class ContainerChest extends Container {
    private final IInventory lowerChestInventory;
    private final int numRows;

    public ContainerChest(IInventory lowerChestInventory) {
        this.lowerChestInventory = lowerChestInventory;
        int size = lowerChestInventory.getSizeInventory();
        this.numRows = size / 9;
        for (int i = 0; i < size; i++) {
            this.addSlotToContainer(new Slot(i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }
    }

    public IInventory getLowerChestInventory() { return this.lowerChestInventory; }
    public int getNumRows() { return this.numRows; }
}
''')

# ---------------- items, blocks, nbt ----------------
add('net/minecraft/item/Item.java', '''
package net.minecraft.item;

import net.minecraft.block.Block;

public class Item {
    private final String name;

    public Item(String name) { this.name = name; }

    public String getUnlocalizedName() { return this.name; }
    public String toString() { return this.name; }

    public static Item getItemFromBlock(Block block) { return block == null ? null : block.item; }
}
''')

add('net/minecraft/item/ItemStack.java', '''
package net.minecraft.item;

import net.minecraft.nbt.NBTTagCompound;

public class ItemStack {
    public int stackSize;
    private final Item item;
    private final int metadata;
    private String displayName;
    private NBTTagCompound tag;

    public ItemStack(Item item, int stackSize, int metadata) {
        this(item, stackSize, metadata, null);
    }

    public ItemStack(Item item, int stackSize, int metadata, String displayName) {
        this.item = item;
        this.stackSize = stackSize;
        this.metadata = metadata;
        this.displayName = displayName;
    }

    public Item getItem() { return this.item; }
    public int getMetadata() { return this.metadata; }
    public String getDisplayName() { return this.displayName == null ? "" : this.displayName; }
    public void setStackDisplayName(String name) { this.displayName = name; }
    public NBTTagCompound getTagCompound() { return this.tag; }
    public void setTagCompound(NBTTagCompound tag) { this.tag = tag; }
    public int getMaxStackSize() { return 64; }
    public ItemStack copy() { return new ItemStack(this.item, this.stackSize, this.metadata, this.displayName); }
}
''')

add('net/minecraft/block/Block.java', '''
package net.minecraft.block;

import net.minecraft.item.Item;

public class Block {
    final Item item;
    private final String name;

    public Block(String name, Item item) {
        this.name = name;
        this.item = item;
    }

    public String getUnlocalizedName() { return this.name; }
    public String toString() { return this.name; }
}
''')

add('net/minecraft/init/Blocks.java', '''
package net.minecraft.init;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

/** The glass family used by the Click in order terminal. */
public class Blocks {
    public static final Block stained_glass_pane = new Block("stained_glass_pane", new Item("item.stained_glass_pane"));
    public static final Block stained_glass = new Block("stained_glass", new Item("item.stained_glass"));
    public static final Block glass_pane = new Block("glass_pane", new Item("item.glass_pane"));
    public static final Block chest = new Block("chest", new Item("item.chest"));

    public static void register() { }
}
''')

add('net/minecraft/nbt/NBTTagCompound.java', '''
package net.minecraft.nbt;

import java.util.HashMap;
import java.util.Map;

public class NBTTagCompound {
    private final Map<String, Object> map = new HashMap<String, Object>();

    public NBTTagCompound() { }

    public void setString(String key, String value) { this.map.put(key, value); }
    public void setTag(String key, NBTTagCompound value) { this.map.put(key, value); }
    public void setTag(String key, NBTTagList value) { this.map.put(key, value); }
    public void setBoolean(String key, boolean value) { this.map.put(key, Boolean.valueOf(value)); }

    public boolean hasKey(String key, int type) {
        Object o = this.map.get(key);
        if (o == null) { return false; }
        return (type == 10 && o instanceof NBTTagCompound) || (type == 9 && o instanceof NBTTagList)
                || (type == 8 && o instanceof String);
    }

    public NBTTagCompound getCompoundTag(String key) {
        Object o = this.map.get(key);
        return o instanceof NBTTagCompound ? (NBTTagCompound) o : null;
    }

    public NBTTagList getTagList(String key, int type) {
        Object o = this.map.get(key);
        return o instanceof NBTTagList ? (NBTTagList) o : null;
    }

    public String getString(String key) {
        Object o = this.map.get(key);
        return o instanceof String ? (String) o : "";
    }

    public int hashCode() {
        int h = 1;
        for (Map.Entry<String, Object> e : this.map.entrySet()) {
            h = h * 31 + e.getKey().hashCode();
            h = h * 31 + (e.getValue() == null ? 0 : e.getValue().hashCode());
        }
        return h;
    }

    public String toString() { return this.map.toString(); }
}
''')

add('net/minecraft/nbt/NBTTagList.java', '''
package net.minecraft.nbt;

import java.util.ArrayList;
import java.util.List;

public class NBTTagList {
    private final List<String> strings = new ArrayList<String>();

    public void appendTag(NBTTagCompound tag) { }
    public void appendString(String value) { this.strings.add(value); }
    public int tagCount() { return this.strings.size(); }
    public String getStringTagAt(int index) { return this.strings.get(index); }
    public String toString() { return this.strings.toString(); }
}
''')

add('net/minecraft/nbt/NBTTagString.java', '''
package net.minecraft.nbt;

public class NBTTagString {
    private final String data;

    public NBTTagString(String data) { this.data = data; }
    public String getString() { return this.data; }
}
''')

add('net/minecraft/util/EnumChatFormatting.java', '''
package net.minecraft.util;

public enum EnumChatFormatting {
    BLACK('0'), DARK_BLUE('1'), DARK_GREEN('2'), DARK_AQUA('3'), DARK_RED('4'), DARK_PURPLE('5'),
    GOLD('6'), GRAY('7'), DARK_GRAY('8'), BLUE('9'), GREEN('a'), AQUA('b'), RED('c'), LIGHT_PURPLE('d'),
    YELLOW('e'), WHITE('f'), OBFUSCATED('k'), BOLD('l'), STRIKETHROUGH('m'), UNDERLINE('n'), ITALIC('o'),
    RESET('r');

    private final char code;

    EnumChatFormatting(char code) { this.code = code; }

    public static String getTextWithoutFormattingCodes(String text) {
        if (text == null) { return null; }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == 0x00a7 && i + 1 < text.length()) { i++; continue; }
            sb.append(c);
        }
        return sb.toString();
    }
}
''')

# ---------------- client ----------------
add('net/minecraft/client/Minecraft.java', '''
package net.minecraft.client;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;

public class Minecraft {
    private static Minecraft instance;

    public PlayerControllerMP playerController;
    public EntityPlayerSP thePlayer;
    public GuiScreen currentScreen;
    public boolean inGameHasFocus;

    public static Minecraft getMinecraft() {
        if (instance == null) { instance = new Minecraft(); }
        return instance;
    }

    public void displayGuiScreen(GuiScreen screen) { this.currentScreen = screen; }

    public void setIngameFocus() { this.inGameHasFocus = true; }
}
''')

add('net/minecraft/client/entity/EntityPlayer.java', '''
package net.minecraft.client.entity;

public class EntityPlayer {
}
''')

add('net/minecraft/client/entity/EntityPlayerSP.java', '''
package net.minecraft.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IChatComponent;

public class EntityPlayerSP extends EntityPlayer {
    public void closeScreen() {
        Minecraft.getMinecraft().displayGuiScreen((net.minecraft.client.gui.GuiScreen) null);
    }

    public void addChatMessage(IChatComponent component) { }
}
''')

add('net/minecraft/client/multiplayer/PlayerControllerMP.java', '''
package net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.entity.EntityPlayerSP;

/** Records the window clicks that the terminal panel sends. */
public class PlayerControllerMP {
    public static final List<String> CLICKS = new ArrayList<String>();
    public static List<Runnable> HOOKS = new ArrayList<Runnable>();

    public void windowClick(int windowId, int slotId, int mouseButton, int mode, EntityPlayerSP player) {
        CLICKS.add(windowId + ":" + slotId + ":" + mouseButton + ":" + mode);
        for (int i = 0; i < HOOKS.size(); i++) {
            HOOKS.get(i).run();
        }
    }
}
''')

add('net/minecraft/client/gui/inventory/GuiChest.java', '''
package net.minecraft.client.gui.inventory;

import net.minecraft.client.gui.GuiScreen;

public class GuiChest extends GuiScreen {
    public net.minecraft.inventory.Container inventorySlots;
}
''')

# ---------------- forge ----------------
add('net/minecraftforge/client/event/GuiOpenEvent.java', '''
package net.minecraftforge.client.event;

import net.minecraft.client.gui.GuiScreen;

public class GuiOpenEvent {
    public GuiScreen gui;

    public GuiOpenEvent(GuiScreen gui) { this.gui = gui; }
}
''')

add('net/minecraftforge/client/event/GuiScreenEvent.java', '''
package net.minecraftforge.client.event;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

public class GuiScreenEvent {
    public GuiScreen gui;

    public static class DrawScreenEvent extends GuiScreenEvent {
        public final ScaledResolution resolution;

        public DrawScreenEvent(GuiScreen gui, ScaledResolution resolution) {
            this.gui = gui;
            this.resolution = resolution;
        }

        public static class Post extends DrawScreenEvent {
            public Post(GuiScreen gui, ScaledResolution resolution) { super(gui, resolution); }
        }
    }
}
''')

add('net/minecraftforge/fml/common/eventhandler/SubscribeEvent.java', '''
package net.minecraftforge.fml.common.eventhandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent { }
''')

add('net/minecraftforge/common/MinecraftForge.java', '''
package net.minecraftforge.common;

public class MinecraftForge {
    public static final Object EVENT_BUS = new Object();
}
''')

# ---------------- log4j, lwjgl ----------------
add('org/apache/logging/log4j/Logger.java', '''
package org.apache.logging.log4j;

import java.util.ArrayList;
import java.util.List;

public class Logger {
    public static final List<String> LINES = new ArrayList<String>();
    private final String name;

    public Logger(String name) { this.name = name; }

    public void info(String message) { LINES.add("INFO " + message); }
    public void warn(String message) { LINES.add("WARN " + message); }
    public void error(String message) { LINES.add("ERROR " + message); }
    public void info(String message, Object... args) { LINES.add("INFO " + message); }
}
''')

add('org/apache/logging/log4j/LogManager.java', '''
package org.apache.logging.log4j;

public class LogManager {
    public static Logger getLogger(String name) { return new Logger(name); }
    public static Logger getLogger(Class<?> type) { return new Logger(type.getName()); }
    public static Logger getRootLogger() { return new Logger("root"); }
}
''')

add('org/lwjgl/input/Keyboard.java', '''
package org.lwjgl.input;

public class Keyboard {
    public static final int KEY_NONE = 0x00;
    public static final int KEY_ESCAPE = 0x01;
    public static final int KEY_E = 0x12;
    public static final int KEY_R = 0x13;

    public static String getKeyName(int code) { return "key" + code; }
    public static boolean isKeyDown(int code) { return false; }
    public static void enableRepeatEvents(boolean enable) { }
}
''')

# ---------------- mod internal dependencies of TerminalSolver (unchanged classes) ----------------
add('com/arena/pekaclient/hud/HudManager.java', '''
package com.arena.pekaclient.hud;

/** Stand-in for the real HUD manager: only the members TerminalSolver uses. */
public class HudManager {
    public static final ClickInOrderHud CLICK_IN_ORDER = new ClickInOrderHud();

    public static boolean visibleInGame() { return false; }
    public static void render(net.minecraft.client.gui.ScaledResolution resolution) { }
}
''')

add('com/arena/pekaclient/hud/ClickInOrderHud.java', '''
package com.arena.pekaclient.hud;

public class ClickInOrderHud {
    public boolean enabled = true;

    public void render(int width, int height, float alpha) { }
}
''')

add('com/arena/pekaclient/render/UIRender.java', '''
package com.arena.pekaclient.render;

public class UIRender {
    public static void beginFrame() { }
    public static void endFrame() { }
}
''')

add('com/arena/pekaclient/dungeon/M7Settings.java', '''
package com.arena.pekaclient.dungeon;

public class M7Settings {
    public static boolean isTerminalSolverEnabled() { return true; }
}
''')

add('com/arena/pekaclient/dungeon/M7OverlayRenderer.java', '''
package com.arena.pekaclient.dungeon;

public class M7OverlayRenderer {
    public boolean isFloorSevenScoreboardDetected() { return true; }
}
''')

for path, code in FILES.items():
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w') as f:
        f.write(code)
print('wrote %d stub files' % len(FILES))
