package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.FontSettings;
import com.arena.pekaclient.render.GlobalFontManager;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import java.io.IOException;

/** Top-level font screen; it is not a Chat QoL sub-page. */
public final class FontsScreen extends GuiLayoutScreen {
    private final GuiScreen parent; private final GuiLayoutToolkit.Rect[] controls={new GuiLayoutToolkit.Rect(),new GuiLayoutToolkit.Rect(),new GuiLayoutToolkit.Rect(),new GuiLayoutToolkit.Rect(),new GuiLayoutToolkit.Rect()};
    public FontsScreen(GuiScreen parent){this.parent=parent;}
    @Override public void initGui(){placePanel(Math.min(570,width-12),Math.min(410,height-12));}
    @Override public void drawScreen(int mx,int my,float partial){drawDefaultBackground();beginPanel("Fonts",mx,my);float y=rowY(0);row("Enable custom font",FontSettings.enabled?"Enabled":"Disabled",0,y);y+=31;row("Font",FontSettings.selected,1,y);y+=31;row("Scale",FontSettings.scale+"%",2,y);y+=31;row("Apply to everything",FontSettings.applyToEverything?"Minecraft":"Sky VeLor only",3,y);y+=31;row("Reset font settings","Reset",4,y);y+=38;drawLabel("Live preview",content.x,y,content.w,0xFFB8C0CF);FontRenderer f=GlobalFontManager.rendererFor(mc.fontRendererObj);UIRender.roundRect(content.x,y+14,content.w-4,58,5,0xFF20252D,0xFF20252D,1);UIRender.text(f,"The quick brown fox jumps over 0123456789",content.x+8,y+25,0xFFFFFFFF,1f,true);UIRender.text(f,"Sky VeLor — formatted §aGreen §bBlue §lBold",content.x+8,y+43,0xFFEAEAF0,1f,true);y+=82;drawLabel("Built-ins: Minecraft Default, Smooth, Doodle, Rounded, Mono.",content.x,y,content.w,0xFFC6CBD4);y+=17;drawLabel("External .ttf/.otf: .minecraft/config/skyvelor/fonts/",content.x,y,content.w,0xFFC6CBD4);y+=17;drawLabel("Bundled font notices: OFL 1.1 — Abel, Architects Daughter, Nunito, JetBrains Mono.",content.x,y,content.w,0xFF9FA8B6);y+=22;drawLabel("Status: "+GlobalFontManager.status(),content.x,y,content.w,FontSettings.enabled?0xFF9EE6B0:0xFFB8C0CF);finishContent(y-content.y+25);endPanel();}
    private void row(String label,String value,int index,float y){GuiLayoutToolkit.Rect whole=new GuiLayoutToolkit.Rect().set(content.x,y,content.w,26),left=new GuiLayoutToolkit.Rect();GuiLayoutToolkit.splitRow(whole,4,6,Math.max(100,uiFont().getStringWidth(value)+16),20,left,controls[index]);drawLabel(label,left.x,left.y+8,left.w,0xFFD9DDE5);UIRender.roundRect(controls[index].x,controls[index].y,controls[index].w,controls[index].h,4,0xFF343B47,0xFF343B47,1);drawLabel(value,controls[index].x+6,controls[index].y+6,controls[index].w-12,0xFFFFFFFF);}
    @Override protected void mouseClicked(int mx,int my,int button)throws IOException{super.mouseClicked(mx,my,button);if(button!=0)return;for(int i=0;i<controls.length;i++)if(controls[i].contains(mx,my)){act(i);return;}}
    private void act(int i){if(i==0)FontSettings.enabled=!FontSettings.enabled;else if(i==1)FontSettings.selected=next(FontSettings.selected);else if(i==2)FontSettings.scale=FontSettings.scale>=140?70:FontSettings.scale+10;else if(i==3)FontSettings.applyToEverything=!FontSettings.applyToEverything;else FontSettings.reset();FontSettings.normalise();AppleSettings.save();GlobalFontManager.reload();}
    private String next(String selected){String[] all=GlobalFontManager.availableFonts();if(all.length==0)return FontSettings.MINECRAFT_DEFAULT;for(int i=0;i<all.length;i++)if(all[i].equals(selected))return all[(i+1)%all.length];return all[0];}
    @Override protected void keyTyped(char c,int key)throws IOException{if(key==1){mc.displayGuiScreen(parent);return;}super.keyTyped(c,key);}
}
