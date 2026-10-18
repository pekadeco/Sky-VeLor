package com.arena.pekaclient.render;

import java.awt.Font;
import java.awt.image.BufferedImage;

/** HD atlas regression: printable glyphs must fit their padded cells and preview output must be valid. */
public final class FontAtlasGeneratorTests {
    private static int passed,failed;
    public static void main(String[] args)throws Exception{FontAtlasGenerator.Atlas a=FontAtlasGenerator.generate(new Font(Font.SANS_SERIF,Font.PLAIN,12),128,140);check("atlas dimensions",a.image.getWidth()==2048&&a.image.getHeight()==2048);int clipped=0,ink=0;for(int c=33;c<127;c++){if(a.hasInk(c))ink++;if(a.isClipped(c))clipped++;}check("ascii has ink",ink>80);check("no padded ascii clipping",clipped==0);BufferedImage p=FontAtlasGenerator.preview(a,"Atlas test");check("preview dimensions",p.getWidth()==1024&&p.getHeight()==760);check("png emitted",a.png().length>100);System.out.println("font atlas: passed "+passed+", failed "+failed);if(failed>0)System.exit(1);}
    private static void check(String n,boolean ok){if(ok)passed++;else{failed++;System.out.println("FAIL "+n);}}
}
