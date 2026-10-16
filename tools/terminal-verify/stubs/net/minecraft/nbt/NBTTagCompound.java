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
