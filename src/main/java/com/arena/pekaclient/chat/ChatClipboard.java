package com.arena.pekaclient.chat;

import net.minecraft.client.gui.GuiScreen;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Method;

/** Clipboard chain used by every Sky VeLor chat window. Text is only reported copied after a read-back match. */
public final class ChatClipboard {
    public static final String COPY_TO_CLIPBOARD="Copy to clipboard";
    public static final String COPY_TO_CHAT_BOX="Copy to chat box";
    public interface Backend extends ClipboardChain.Backend { }
    public static final class Result { public final boolean success; public final String backend; public final String cleanText; Result(boolean ok,String via,String text){success=ok;backend=via;cleanText=text;} }
    private static Backend[] injected;
    private ChatClipboard(){}
    public static String clean(String raw){ return ChatText.clean(raw); }
    public static Result copy(String raw){Backend[] all=injected!=null?injected:new Backend[]{new GuiBackend(),new AwtBackend(),new LwjglBackend()};ClipboardChain.Result r=ClipboardChain.copy(raw,all);return new Result(r.success,r.backend,r.text);}
    /** Test seam only; production always leaves this null. */
    public static void setBackendsForTests(Backend[] value){injected=value;}
    public static String selfTestStatus(){Result r=copy("Sky §aVeLor clipboard");return r.success?"READY ("+r.backend+")":"FAILED (no writable verified clipboard)";}
    private static final class GuiBackend implements Backend {public String name(){return "GuiScreen";}public boolean write(String t){GuiScreen.setClipboardString(t);return true;}public String read(){return clean(GuiScreen.getClipboardString());}}
    private static final class AwtBackend implements Backend {public String name(){return "AWT";}public boolean write(String t){Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(t),null);return true;}public String read()throws Exception{Object value=Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);return value==null?"":clean(String.valueOf(value));}}
    /** LWJGL 2 has platform-specific clipboard methods on some backends; use reflection so headless/classic backends fail closed. */
    private static final class LwjglBackend implements Backend {private String written;public String name(){return "LWJGL";}public boolean write(String t)throws Exception{written=t;return invoke("setClipboardString",t)||invoke("setClipboard",t);}public String read()throws Exception{String r=read("getClipboardString");if(r==null)r=read("getClipboard");return r==null?"":clean(r);}private boolean invoke(String method,String value){try{Class<?> c=Class.forName("org.lwjgl.opengl.Display");Method m=c.getMethod(method,String.class);m.invoke(null,value);return true;}catch(Throwable ignored){return false;}}private String read(String method){try{Class<?> c=Class.forName("org.lwjgl.opengl.Display");Method m=c.getMethod(method);Object v=m.invoke(null);return v==null?null:String.valueOf(v);}catch(Throwable ignored){return null;}}}
}
