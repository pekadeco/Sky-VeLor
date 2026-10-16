package net.minecraft.nbt;

public class NBTTagString {
    private final String data;

    public NBTTagString(String data) { this.data = data; }
    public String getString() { return this.data; }
}
