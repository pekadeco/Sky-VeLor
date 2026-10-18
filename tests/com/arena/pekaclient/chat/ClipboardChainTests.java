package com.arena.pekaclient.chat;

/** Mouse-up UI delegates its copy action to this verified chain; this tests fall-through and clean text. */
public final class ClipboardChainTests {
    private static int passed,failed;
    public static void main(String[] args){firstFailureFallsThrough();readbackMismatchFailsClosed();emptyAndFormatAreClean();mouseUpMenuState();done();}
    private static void firstFailureFallsThrough(){Memory good=new Memory("AWT");ClipboardChain.Backend bad=new ClipboardChain.Backend(){public String name(){return "GuiScreen";}public boolean write(String t){return false;}public String read(){return "";}};ClipboardChain.Result r=ClipboardChain.copy("A §aformatted\r message",new ClipboardChain.Backend[]{bad,good});check("fallback success",r.success&&"AWT".equals(r.backend));check("no formatting copied","A formatted message".equals(r.text));}
    private static void readbackMismatchFailsClosed(){ClipboardChain.Backend bad=new ClipboardChain.Backend(){public String name(){return "LWJGL";}public boolean write(String t){return true;}public String read(){return "other";}};ClipboardChain.Result r=ClipboardChain.copy("same",new ClipboardChain.Backend[]{bad});check("readback failure",!r.success&&"unavailable".equals(r.backend));}
    private static void emptyAndFormatAreClean(){check("empty refuses",!ClipboardChain.copy("§a",new ClipboardChain.Backend[0]).success);check("format cleaner", "hello".equals(ChatText.clean("§chello")));}
    private static void mouseUpMenuState(){ChatContextMenu menu=new ChatContextMenu();check("exact first label","Copy to clipboard".equals(ChatContextMenu.OPTIONS[0]));check("exact second label","Copy to chat box".equals(ChatContextMenu.OPTIONS[1]));menu.press(0);check("press does not execute",menu.armed());check("release same executes",menu.release(0)==0);menu.press(1);check("release elsewhere refuses",menu.release(0)<0);}
    private static final class Memory implements ClipboardChain.Backend {private String value="";private final String name;Memory(String n){name=n;}public String name(){return name;}public boolean write(String t){value=t;return true;}public String read(){return value;}}
    private static void check(String n,boolean ok){if(ok)passed++;else{failed++;System.out.println("FAIL "+n);}}private static void done(){System.out.println("clipboard chain: passed "+passed+", failed "+failed);if(failed>0)System.exit(1);}
}
