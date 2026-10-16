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
