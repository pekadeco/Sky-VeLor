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
