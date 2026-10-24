package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.UIRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Shared runtime layout shell for every new settings/editor surface. It centralises panel clamping, clipping and wheel handling. */
public abstract class GuiLayoutScreen extends AppleScreen {
    protected final GuiLayoutToolkit.Rect panel=new GuiLayoutToolkit.Rect(), content=new GuiLayoutToolkit.Rect();
    protected final GuiLayoutToolkit.Scroll scroll=new GuiLayoutToolkit.Scroll();
    protected float contentHeight; protected int titleHeight=28; protected boolean dragging; protected float dragDX,dragDY; private long lastNanos=System.nanoTime();
    protected FontRenderer uiFont(){return Minecraft.getMinecraft().fontRendererObj;}
    protected void placePanel(float wantedW,float wantedH){GuiLayoutToolkit.clampPanel(panel,(width-wantedW)*.5f,(height-wantedH)*.5f,wantedW,wantedH,width,height);content.set(panel.x+10,panel.y+titleHeight+7,Math.max(1,panel.w-20),Math.max(1,panel.h-titleHeight-17));}
    protected void beginPanel(String title,int mouseX,int mouseY){long n=System.nanoTime();float dt=Math.min(.1f,Math.max(0,(n-lastNanos)/1000000000f));lastNanos=n;scroll.update(dt,AppleSettings.smoothScrolling);UIRender.beginFrame();UIRender.shadow(panel.x,panel.y,panel.w,panel.h,Theme.RADIUS_CARD);UIRender.roundRectBorder(panel.x,panel.y,panel.w,panel.h,Theme.RADIUS_CARD,Theme.SURFACE_POPUP,Theme.SURFACE_POPUP,Theme.BORDER);UIRender.text(uiFont(),GuiLayoutToolkit.ellipsize(title,panel.w-48,metrics()),panel.x+11,panel.y+10,Theme.TEXT_PRIMARY,1f,true);UIRender.scissorBegin(content.x,content.y,content.w,content.h,new net.minecraft.client.gui.ScaledResolution(Minecraft.getMinecraft()).getScaleFactor());}
    protected void endPanel(){UIRender.scissorEnd();if(scroll.maximum()>0){float rail=content.h,thumb=Math.max(18,rail*Math.min(1f,content.h/(content.h+scroll.maximum()))),y=content.y+(rail-thumb)*(scroll.value()/scroll.maximum());UIRender.roundRect(panel.x+panel.w-6,y,2,thumb,1,UIRender.withAlpha(Theme.TEXT_TERTIARY,.6f),UIRender.withAlpha(Theme.TEXT_TERTIARY,.6f),1f);}UIRender.endFrame();}
    protected float rowY(float naturalY){return content.y+naturalY-scroll.value();}
    protected void finishContent(float naturalHeight){contentHeight=Math.max(0,naturalHeight);scroll.setMaximum(Math.max(0,contentHeight-content.h));}
    protected void drawLabel(String value,float x,float y,float maxW,int color){UIRender.text(uiFont(),GuiLayoutToolkit.ellipsize(value,maxW,metrics()),x,y,color,1f,false);}
    protected GuiLayoutToolkit.TextMetrics metrics(){final FontRenderer f=uiFont();return new GuiLayoutToolkit.TextMetrics(){public float width(String value){return f.getStringWidth(value);}public float height(){return f.FONT_HEIGHT;}};}
    protected boolean contentContains(int mx,int my){return content.contains(mx,my);}
    @Override public void handleMouseInput() throws java.io.IOException {super.handleMouseInput();int wheel=Mouse.getEventDWheel();if(wheel!=0&&contentContains(Mouse.getEventX()*width/Minecraft.getMinecraft().displayWidth,height-Mouse.getEventY()*height/Minecraft.getMinecraft().displayHeight-1)){scroll.wheel(wheel,Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)||Keyboard.isKeyDown(Keyboard.KEY_RSHIFT),AppleSettings.guiScrollMultiplier(),20f);}}
    @Override protected void mouseClicked(int mx,int my,int button)throws java.io.IOException{super.mouseClicked(mx,my,button);if(button==0&&my>=panel.y&&my<panel.y+titleHeight){dragging=true;dragDX=mx-panel.x;dragDY=my-panel.y;}}
    @Override protected void mouseClickMove(int mx,int my,int button,long time){super.mouseClickMove(mx,my,button,time);if(dragging&&button==0){GuiLayoutToolkit.clampPanel(panel,mx-dragDX,my-dragDY,panel.w,panel.h,width,height);content.set(panel.x+10,panel.y+titleHeight+7,Math.max(1,panel.w-20),Math.max(1,panel.h-titleHeight-17));}}
    @Override protected void mouseReleased(int mx,int my,int button){super.mouseReleased(mx,my,button);if(button==0)dragging=false;}
}
