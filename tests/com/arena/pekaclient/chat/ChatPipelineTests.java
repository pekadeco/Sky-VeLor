package com.arena.pekaclient.chat;

import java.util.ArrayList;
import java.util.List;

/** Proves shared line/wrap/hit/scroll behavior used by Default Chat, Chat 1 and Chat 2. */
public final class ChatPipelineTests {
    private static int passed,failed;
    public static void main(String[] args){wrappedBlockHitsAtRightEdge();blankRowHits();scrollAndFade();done();}
    private static ChatPipeline.Wrapper wrapper(){return new ChatPipeline.Wrapper(){public List<String> wrap(String text,int width){List<String> out=new ArrayList<String>();for(int i=0;i<text.length();i+=Math.max(1,width/5))out.add(text.substring(i,Math.min(text.length(),i+Math.max(1,width/5))));if(out.isEmpty())out.add("");return out;}};}
    private static void wrappedBlockHitsAtRightEdge(){ChatPipeline p=new ChatPipeline();ChatPipeline.Message m=new ChatPipeline.Message(null,"This is a deliberately wrapped message for a shared chat pipeline","plain",1,0);p.add(m);p.layout(10,10,200,90,30,10,0,wrapper());ChatPipeline.Block b=p.blocks().get(0);check("message wrapped",b.rows.size()>1);ChatPipeline.Hit h=p.hit(209,Math.min(99,b.bounds.y+b.bounds.h-3));check("full row width right-edge hover",h!=null&&h.message()==m);check("block gapless",b.bounds.h==b.rows.size()*10);}
    private static void blankRowHits(){ChatPipeline p=new ChatPipeline();ChatPipeline.Message m=new ChatPipeline.Message(null,"","",2,0);p.add(m);p.layout(0,0,100,40,50,10,0,wrapper());check("blank creates line",p.blocks().get(0).rows.size()==1);check("blank is hittable",p.hit(99,35)!=null);}
    private static void scrollAndFade(){ChatPipeline p=new ChatPipeline();for(int i=0;i<30;i++)p.add(new ChatPipeline.Message(null,"1234567890","x",i,0));p.layout(0,0,90,30,90,10,0,wrapper());check("scroll max",p.scroll().maximum()>0);p.wheel(-1,false,2f);float t=p.scroll().target();p.wheel(0,false,2f);check("wheel only once",p.scroll().target()==t);p.update(.1f,false);check("wheel moves",p.scroll().value()==t);ChatPipeline.Message m=p.messages().get(0);check("fade after age",p.fadeAlpha(m,300,10)==0f);p.setOpen(true);check("open chat does not fade",p.fadeAlpha(m,300,10)==1f);}
    private static void check(String n,boolean ok){if(ok)passed++;else{failed++;System.out.println("FAIL "+n);}}private static void done(){System.out.println("chat pipeline: passed "+passed+", failed "+failed);if(failed>0)System.exit(1);}
}
