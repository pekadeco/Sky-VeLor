package com.arena.pekaclient.command;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.slayer.DropMessageParser;
import com.arena.pekaclient.slayer.SlayerFormat;
import com.arena.pekaclient.slayer.SlayerTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import java.util.Collections;
import java.util.List;

/** /setprice (alias /priceset) sets the manual price of the held item or a named item. Client-side only. */
public final class PriceSetCommand extends CommandBase {

    private final String commandName;

    public PriceSetCommand(String commandName) {
        this.commandName = commandName;
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.emptyList();
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + commandName + " <price> (held item, e.g. 110k) or /" + commandName + " <item name> <price>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    private static void say(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText("\u00a7b[Sky VeLor] \u00a7f" + text));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!AppleSettings.slayerPricing) {
            say(sender, "/" + commandName + " is off. Turn it on in the hub, Slayer, Prices.");
            return;
        }
        if (args.length < 1) {
            String held = heldItemName();
            say(sender, held.length() == 0 ? "No held item. Hold an item, then use /" + commandName + " <price>."
                    : "Held item: " + held + ". Set its price with /" + commandName + " <price>.");
            return;
        }
        String item;
        String priceArg;
        if (args.length == 1) {
            item = heldItemName();
            priceArg = args[0];
            if (item.length() == 0) {
                say(sender, "Hold the item you want to price, then run /" + commandName + " <price>.");
                return;
            }
        } else {
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < args.length - 1; i++) {
                if (i > 0) {
                    name.append(' ');
                }
                name.append(args[i]);
            }
            item = name.toString().trim();
            if (item.length() >= 2 && item.startsWith("\"") && item.endsWith("\"")) {
                item = item.substring(1, item.length() - 1).trim();
            }
            priceArg = args[args.length - 1];
        }
        long price;
        try {
            price = SlayerFormat.parsePrice(priceArg);
        } catch (NumberFormatException | ArithmeticException e) {
            say(sender, "Invalid price. Use a coin amount such as 110000, 110k, 1.5m or 1b.");
            return;
        }
        if (price < 0L) {
            say(sender, "Price cannot be negative.");
            return;
        }
        if (item.length() == 0) {
            say(sender, "Enter an item name. Usage: " + getCommandUsage(sender));
            return;
        }
        SlayerTracker.setItemPrice(item, price);
        say(sender, "Saved price for " + item + ": " + SlayerFormat.formatPrice(price) + " coins.");
    }

    private static String heldItemName() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            return "";
        }
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) {
            return "";
        }
        return DropMessageParser.extractDropItem(
                com.arena.pekaclient.fishing.SeaCreatureParser.stripFormatting(held.getDisplayName()).trim());
    }
}
