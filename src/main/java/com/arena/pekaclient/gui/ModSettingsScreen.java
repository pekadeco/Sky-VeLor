package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.gui.GuiScreen;
import java.io.IOException;

/** Top-level settings for cross-feature behavior. Wheel speed is defined here once, not in individual editors. */
public final class ModSettingsScreen extends GuiLayoutScreen {
    private final GuiScreen parent; private final GuiLayoutToolkit.Rect[] controls={new GuiLayoutToolkit.Rect(),new GuiLayoutToolkit.Rect(),new GuiLayoutToolkit.Rect(),new GuiLayoutToolkit.Rect(),new GuiLayoutToolkit.Rect()};
    public ModSettingsScreen(GuiScreen parent){this.parent=parent;}
    @Override public void initGui(){placePanel(Math.min(540,width-12),Math.min(350,height-12));}
    @Override public void drawScreen(int mx,int my,float partial){drawDefaultBackground();beginPanel("Mod Settings",mx,my);float y=rowY(0);row("Chat scroll speed",mult(AppleSettings.effectiveChatScrollSpeed()),0,y);y+=31;row("GUI scroll speed",mult(AppleSettings.effectiveGuiScrollSpeed()),1,y);y+=31;row("Smooth scrolling",AppleSettings.smoothScrolling?"Enabled":"Disabled",2,y);y+=31;row("Chat QoL master",ChatQolSettings.enabled?"Enabled":"Disabled",3,y);y+=31;row("Held item animation",AppleSettings.heldItemEnabled?"Enabled":"Disabled",4,y);y+=42;drawLabel("Shift + wheel uses fine scrolling. Each wheel event is applied once to the hovered viewport.",content.x,y,content.w,0xFFB9C1CD);y+=18;drawLabel("Defaults: Chat 2.0x, GUI 2.0x, smooth scrolling on.",content.x,y,content.w,0xFFB9C1CD);finishContent(y-content.y+28);endPanel();}
    private void row(String label,String value,int index,float y){GuiLayoutToolkit.Rect whole=new GuiLayoutToolkit.Rect().set(content.x,y,content.w,26),left=new GuiLayoutToolkit.Rect();GuiLayoutToolkit.splitRow(whole,4,6,Math.max(100,uiFont().getStringWidth(value)+16),20,left,controls[index]);drawLabel(label,left.x,left.y+8,left.w,0xFFD9DDE5);UIRender.roundRect(controls[index].x,controls[index].y,controls[index].w,controls[index].h,4,0xFF343B47,0xFF343B47,1);drawLabel(value,controls[index].x+6,controls[index].y+6,controls[index].w-12,0xFFFFFFFF);}
    @Override protected void mouseClicked(int mx,int my,int button)throws IOException{super.mouseClicked(mx,my,button);if(button!=0)return;for(int i=0;i<controls.length;i++)if(controls[i].contains(mx,my)){if(i==0)AppleSettings.chatScrollSpeed=next(AppleSettings.effectiveChatScrollSpeed());else if(i==1)AppleSettings.guiScrollSpeed=next(AppleSettings.effectiveGuiScrollSpeed());else if(i==2)AppleSettings.smoothScrolling=!AppleSettings.smoothScrolling;else if(i==3)ChatQolSettings.enabled=!ChatQolSettings.enabled;else AppleSettings.heldItemEnabled=!AppleSettings.heldItemEnabled;AppleSettings.save();return;}}
    private int next(int value){return value>=600?25:value+25;}private String mult(int value){return String.format(java.util.Locale.ROOT,"%.2fx",value/100f);}
    @Override protected void keyTyped(char c,int key)throws IOException{if(key==1){mc.displayGuiScreen(parent);return;}super.keyTyped(c,key);}
}
