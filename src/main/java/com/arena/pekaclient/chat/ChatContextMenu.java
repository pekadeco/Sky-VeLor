package com.arena.pekaclient.chat;

/** Small shared state machine: menu commands never fire until matching mouse release. */
public final class ChatContextMenu {
    public static final String[] OPTIONS={"Copy to clipboard","Copy to chat box"};
    private int pressed=-1;
    public boolean press(int option){if(option<0||option>=OPTIONS.length)return false;pressed=option;return true;}
    /** Returns chosen option only when release is over the same option, otherwise -1. */
    public int release(int option){int was=pressed;pressed=-1;return was>=0&&was==option?was:-1;}
    public void clear(){pressed=-1;}
    public boolean armed(){return pressed>=0;}
}
