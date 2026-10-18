package com.arena.pekaclient.chat;

/** Pure clipboard fallback/verification policy. Platform backends are supplied by ChatClipboard. */
public final class ClipboardChain {
    public interface Backend { String name(); boolean write(String text) throws Exception; String read() throws Exception; }
    public static final class Result { public final boolean success; public final String backend; public final String text; Result(boolean ok,String via,String value){success=ok;backend=via;text=value;} }
    private ClipboardChain() { }
    public static Result copy(String raw, Backend[] backends) {
        String text=ChatText.clean(raw); if(text.length()==0)return new Result(false,"empty",text);
        if(backends!=null)for(Backend b:backends)if(b!=null)try{if(b.write(text)&&text.equals(ChatText.clean(b.read())))return new Result(true,b.name(),text);}catch(Throwable ignored){}
        return new Result(false,"unavailable",text);
    }
}
