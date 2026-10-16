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
