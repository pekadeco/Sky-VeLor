package com.arena.pekaclient.render;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Generates a high-definition, 16x16 vanilla-ascii-compatible atlas from a TTF/OTF face. */
public final class FontAtlasGenerator {
    public static final int DEFAULT_CELL_SIZE=128;
    private FontAtlasGenerator(){}
    public static final class Atlas {
        public final BufferedImage image; public final int cellSize, baseline, fontPixelSize; public final int[] left=new int[256],right=new int[256],top=new int[256],bottom=new int[256];
        Atlas(BufferedImage i,int c,int b,int fp){image=i;cellSize=c;baseline=b;fontPixelSize=fp;for(int n=0;n<256;n++){left[n]=top[n]=c;right[n]=bottom[n]=-1;}}
        public boolean hasInk(int c){c&=255;return right[c]>=left[c]&&bottom[c]>=top[c];}
        public boolean isClipped(int c){c&=255;return hasInk(c)&&c!=32&&(left[c]<=0||top[c]<=0||right[c]>=cellSize-1||bottom[c]>=cellSize-1);}
        public byte[] png() throws IOException {ByteArrayOutputStream o=new ByteArrayOutputStream();ImageIO.write(image,"PNG",o);return o.toByteArray();}
    }
    public static Atlas generate(Font requested,int desiredCell,int scalePercent){
        if(requested==null)throw new IllegalArgumentException("font");int cell=Math.max(32,Math.min(256,desiredCell)),percent=Math.max(70,Math.min(140,scalePercent));BufferedImage image=new BufferedImage(cell*16,cell*16,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();try{g.setComposite(AlphaComposite.Clear);g.fillRect(0,0,image.getWidth(),image.getHeight());g.setComposite(AlphaComposite.SrcOver);g.setColor(Color.WHITE);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);Font face=fit(requested,cell,percent,g);g.setFont(face);FontMetrics m=g.getFontMetrics();int pad=Math.max(3,cell/24),base=pad+m.getAscent();Atlas atlas=new Atlas(image,cell,base,Math.round(face.getSize2D()));for(int code=32;code<256;code++){if(code==127)continue;char ch=(char)code;int x=(code&15)*cell,y=(code>>4)*cell;GlyphVector glyph=face.createGlyphVector(g.getFontRenderContext(),new char[]{ch});if(glyph.getGlyphCode(0)==face.getMissingGlyphCode())continue;Rectangle2D visual=glyph.getVisualBounds();float shift=visual.getX()<pad?pad-(float)visual.getX():pad;g.drawString(String.valueOf(ch),x+shift,y+base);scan(atlas,code);}return atlas;}finally{g.dispose();}}
    private static Font fit(Font f,int cell,int percent,Graphics2D g){Font c=f.deriveFont(Font.PLAIN,Math.max(6f,Math.min(cell*.82f,cell*.72f*percent/100f)));int guard=Math.max(3,cell/24);for(int n=0;n<24;n++){g.setFont(c);FontMetrics m=g.getFontMetrics();int widest=0;for(char ch=33;ch<127;ch++)widest=Math.max(widest,m.charWidth(ch));if(m.getHeight()<=cell-guard*2&&widest<=cell-guard*2)return c;c=f.deriveFont(Font.PLAIN,Math.max(5f,c.getSize2D()*.92f));}return c;}
    private static void scan(Atlas a,int code){int bx=(code&15)*a.cellSize,by=(code>>4)*a.cellSize;for(int y=0;y<a.cellSize;y++)for(int x=0;x<a.cellSize;x++)if(((a.image.getRGB(bx+x,by+y)>>>24)&255)!=0){a.left[code]=Math.min(a.left[code],x);a.right[code]=Math.max(a.right[code],x);a.top[code]=Math.min(a.top[code],y);a.bottom[code]=Math.max(a.bottom[code],y);}}
    public static BufferedImage preview(Atlas a,String title){BufferedImage out=new BufferedImage(1024,760,BufferedImage.TYPE_INT_ARGB);Graphics2D g=out.createGraphics();try{g.setColor(new Color(22,24,29));g.fillRect(0,0,1024,760);g.setColor(Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,22));g.drawString(title==null?"Font atlas":title,22,32);g.drawImage(a.image,22,76,534,588,0,0,a.image.getWidth(),a.image.getHeight(),null);g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,22));g.drawString("Aa Bb Cc 0123456789",565,120);g.drawString("The quick brown fox",565,162);g.drawString("[]{}() <> !? @#$%",565,204);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));g.drawString("cell="+a.cellSize+" baseline="+a.baseline+" face="+a.fontPixelSize,565,250);}finally{g.dispose();}return out;}
}
