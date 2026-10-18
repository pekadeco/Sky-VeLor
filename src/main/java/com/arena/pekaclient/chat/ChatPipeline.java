package com.arena.pekaclient.chat;

import com.arena.pekaclient.gui.GuiLayoutToolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Shared geometry, wrapping, row hit testing, scrolling and fade math for Default Chat, Chat 1 and Chat 2. */
public final class ChatPipeline {
    public interface Wrapper { List<String> wrap(String formattedText,int width); }
    public static final class Message {
        public final Object component; public final String formatted, plain; public final int id; public final long receivedTick;
        public Message(Object component,String formatted,String plain,int id,long tick){this.component=component;this.formatted=formatted==null?"":formatted;this.plain=ChatText.clean(plain==null?this.formatted:plain);this.id=id;this.receivedTick=tick;}
    }
    public static final class Row { public final Message message; public final String formatted; public final int line; public final GuiLayoutToolkit.Rect bounds=new GuiLayoutToolkit.Rect(); Row(Message m,String f,int l){message=m;formatted=f==null?"":f;line=l;} }
    public static final class Block { public final Message message; public final List<Row> rows=new ArrayList<Row>(); public final GuiLayoutToolkit.Rect bounds=new GuiLayoutToolkit.Rect(); Block(Message m){message=m;} }
    public static final class Hit { public final Block block; public final Row row; Hit(Block b,Row r){block=b;row=r;} public Message message(){return block==null?null:block.message;} }
    private final List<Message> messages=new ArrayList<Message>(); private final List<Block> blocks=new ArrayList<Block>(); private final GuiLayoutToolkit.Scroll scroll=new GuiLayoutToolkit.Scroll(); private final GuiLayoutToolkit.Rect viewport=new GuiLayoutToolkit.Rect();
    private int limit=100, rowHeight=9, lineSpacing, width=1; private boolean open; private Hit hovered;
    public void add(Message m){if(m==null)return;messages.add(0,m);while(messages.size()>limit)messages.remove(messages.size()-1);}
    public void clear(){messages.clear();blocks.clear();scroll.reset();hovered=null;}
    public int messageCount(){return messages.size();} public List<Message> messages(){return Collections.unmodifiableList(messages);} public List<Block> blocks(){return Collections.unmodifiableList(blocks);} public GuiLayoutToolkit.Rect viewport(){return viewport;} public GuiLayoutToolkit.Scroll scroll(){return scroll;} public Hit hovered(){return hovered;} public void setLimit(int next){limit=Math.max(10,Math.min(1000,next));while(messages.size()>limit)messages.remove(messages.size()-1);} public void setOpen(boolean value){open=value;} public boolean isOpen(){return open;}
    /** Exact same method is called for all three chat targets. Newest content anchors at the viewport bottom. */
    public void layout(int x,int y,int w,int h,int scaledTextWidth,int rowHeight,int lineSpacing,Wrapper wrapper){viewport.set(x,y,Math.max(1,w),Math.max(1,h));this.width=Math.max(1,scaledTextWidth);this.rowHeight=Math.max(1,rowHeight);this.lineSpacing=Math.max(-2,lineSpacing);blocks.clear();float all=0;for(Message message:messages){Block b=new Block(message);List<String> lines=wrapper==null?Collections.singletonList(message.formatted):wrapper.wrap(message.formatted,this.width);if(lines==null||lines.isEmpty())lines=Collections.singletonList("");for(int i=0;i<lines.size();i++){Row r=new Row(message,lines.get(i),i);b.rows.add(r);}float bh=b.rows.size()*this.rowHeight+Math.max(0,b.rows.size()-1)*this.lineSpacing;b.bounds.set(x,0,w,bh);all+=bh;blocks.add(b);}scroll.setMaximum(Math.max(0,all-viewport.h));float cursor=viewport.y+viewport.h+scroll.value();for(Block b:blocks){cursor-=b.bounds.h;b.bounds.set(x,cursor,w,b.bounds.h);float ry=cursor;for(Row r:b.rows){r.bounds.set(x,ry,w,this.rowHeight);ry+=this.rowHeight+this.lineSpacing;}}}
    public void wheel(float notches,boolean shift,float multiplier){scroll.wheel(notches,shift,multiplier,rowHeight*3f);}
    public void update(float seconds,boolean smooth){scroll.update(seconds,smooth);}
    /** x intentionally spans the entire window, not only glyph pixels. */
    public Hit hit(float mouseX,float mouseY){hovered=null;if(!viewport.contains(mouseX,mouseY))return null;for(Block b:blocks)if(b.bounds.contains(mouseX,mouseY)){for(Row r:b.rows)if(r.bounds.contains(mouseX,mouseY)){hovered=new Hit(b,r);return hovered;}if(!b.rows.isEmpty()){hovered=new Hit(b,b.rows.get(0));return hovered;}}return null;}
    public boolean shouldFade(Message m,long currentTick,int fadeSeconds){if(open||fadeSeconds<=0)return false;return currentTick-m.receivedTick>fadeSeconds*20L;}
    public float fadeAlpha(Message m,long currentTick,int fadeSeconds){if(open||fadeSeconds<=0)return 1f;long age=Math.max(0,currentTick-m.receivedTick),start=Math.max(0,fadeSeconds*20L-20L);if(age<=start)return 1f;return GuiLayoutToolkit.clamp(1f-(age-start)/20f,0f,1f);}
}
