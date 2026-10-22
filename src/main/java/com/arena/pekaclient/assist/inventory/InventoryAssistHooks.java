package com.arena.pekaclient.assist.inventory;

import com.arena.pekaclient.config.AssistSettings;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiny entry points woven into the vanilla tooltip method. The hook only edits the fresh, local tooltip list that
 * vanilla just assembled for this frame; it never mutates an ItemStack, server data, or an inventory container.
 */
public final class InventoryAssistHooks {

    private static final int TOOLTIP_VISIBLE_LINES = 10;
    private static int tooltipOffset;
    private static int lastLineCount;

    private InventoryAssistHooks() {
    }

    /** Called from a GuiScreen draw-tooltip entry hook. Mapping-neutral Object arguments are intentional. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void prepareTooltip(Object screen, Object candidateLines) {
        if (!(screen instanceof GuiContainer) || !(candidateLines instanceof List)
                || (!AssistSettings.romanNumerals && !AssistSettings.tooltipScroll)) {
            return;
        }
        List lines = (List) candidateLines;
        if (lines.isEmpty()) {
            tooltipOffset = 0;
            lastLineCount = 0;
            return;
        }
        ArrayList<String> rewritten = null;
        if (AssistSettings.romanNumerals) {
            rewritten = new ArrayList<String>(lines.size());
            for (int index = 0; index < lines.size(); index++) {
                Object value = lines.get(index);
                if (!(value instanceof String)) return; // A foreign tooltip shape stays exactly vanilla.
                rewritten.add(RomanNumeralPolicy.replaceLevels((String) value));
            }
        }
        List<String> effective = rewritten == null ? lines : rewritten;
        lastLineCount = effective.size();
        if (AssistSettings.tooltipScroll && effective.size() > TOOLTIP_VISIBLE_LINES) {
            tooltipOffset = TooltipWindow.clampOffset(tooltipOffset, effective.size(), TOOLTIP_VISIBLE_LINES);
            effective = TooltipWindow.visible(effective, tooltipOffset, TOOLTIP_VISIBLE_LINES);
        } else {
            tooltipOffset = 0;
        }
        try {
            lines.clear();
            lines.addAll(effective);
        } catch (UnsupportedOperationException ignored) {
            // A foreign immutable tooltip list must remain entirely vanilla, rather than risking a render crash.
        }
    }

    /** Only the inventory controller calls this for a real wheel event over a live container slot. */
    public static void scrollTooltip(int wheelDelta) {
        if (!AssistSettings.tooltipScroll || wheelDelta == 0 || lastLineCount <= TOOLTIP_VISIBLE_LINES) return;
        int direction = wheelDelta < 0 ? 1 : -1;
        tooltipOffset = TooltipWindow.clampOffset(tooltipOffset + direction, lastLineCount, TOOLTIP_VISIBLE_LINES);
    }

    public static void resetTooltipScroll() {
        tooltipOffset = 0;
        lastLineCount = 0;
    }

    public static int tooltipOffset() {
        return tooltipOffset;
    }
}
