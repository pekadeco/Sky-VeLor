package com.arena.pekaclient.gui;

/**
 * The only geometry authority for Sky VeLor-owned settings/editor/chat panels. This class intentionally has no
 * Minecraft imports, so widths, clipping, scroll and panel bounds are proven headlessly before the renderer uses
 * them. Coordinates are GUI-scaled pixels.
 */
public final class GuiLayoutToolkit {
    public static final float GAP = 6f;
    public static final float PAD = 10f;

    private GuiLayoutToolkit() { }

    public static final class Rect {
        public float x, y, w, h;
        public Rect set(float x, float y, float w, float h) {
            this.x = x; this.y = y; this.w = Math.max(0f, w); this.h = Math.max(0f, h); return this;
        }
        public boolean contains(float px, float py) { return px >= x && py >= y && px < x + w && py < y + h; }
        public boolean intersects(Rect other) { return other != null && x < other.x + other.w && x + w > other.x && y < other.y + other.h && y + h > other.y; }
    }

    public interface TextMetrics { float width(String value); float height(); }

    public static float rowHeight(float fontHeight, int textLines, float controlHeight, float padding) {
        return Math.max(Math.max(1f, fontHeight) * Math.max(1, textLines), Math.max(1f, controlHeight)) + Math.max(0f, padding) * 2f;
    }

    /** Guarantees a label column (ellipsis-ready) and a non-overlapping right control even on a tiny GUI. */
    public static void splitRow(Rect row, float pad, float gap, float wantedControlW, float controlH, Rect label, Rect control) {
        float p = Math.max(0f, pad), innerX = row.x + p, innerW = Math.max(1f, row.w - p * 2f);
        float g = Math.max(3f, gap);
        float controlW = Math.min(Math.max(1f, wantedControlW), Math.max(1f, innerW - g - Math.min(42f, innerW * .42f)));
        float ch = Math.min(Math.max(1f, controlH), Math.max(1f, row.h));
        label.set(innerX, row.y, Math.max(0f, innerW - controlW - g), row.h);
        control.set(innerX + innerW - controlW, row.y + (row.h - ch) * .5f, controlW, ch);
    }

    public static String ellipsize(String value, float maxWidth, TextMetrics metrics) {
        if (value == null || value.length() == 0 || metrics == null || maxWidth <= 0f) return "";
        if (metrics.width(value) <= maxWidth) return value;
        String dots = "...";
        if (metrics.width(dots) > maxWidth) return "";
        int low = 0, high = value.length();
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (metrics.width(value.substring(0, mid)) + metrics.width(dots) <= maxWidth) low = mid; else high = mid - 1;
        }
        return value.substring(0, low) + dots;
    }

    /** A single wheel event updates only target once; rendering eases value toward it if enabled. */
    public static final class Scroll {
        private float value, target, maximum;
        public float value() { return value; }
        public float target() { return target; }
        public float maximum() { return maximum; }
        public void setMaximum(float max) { maximum = Math.max(0f, max); value = clamp(value, 0f, maximum); target = clamp(target, 0f, maximum); }
        public void reset() { value = target = 0f; }
        public void setImmediate(float next) { value = target = clamp(next, 0f, maximum); }
        public void wheel(float notches, boolean fine, float multiplier, float normalStep) {
            if (notches == 0f) return;
            float step = Math.max(1f, normalStep) * (fine ? .25f : 1f) * clamp(multiplier, .25f, 6f);
            target = clamp(target - Math.signum(notches) * step, 0f, maximum);
        }
        public void update(float seconds, boolean smooth) {
            target = clamp(target, 0f, maximum);
            if (!smooth) { value = target; return; }
            float k = 1f - (float)Math.exp(-Math.max(0f, Math.min(.1f, seconds)) * 16f);
            value += (target - value) * k;
            if (Math.abs(target - value) < .05f) value = target;
            value = clamp(value, 0f, maximum);
        }
    }

    /** Shared animated/collapsible section geometry. Bodies are physically zero-height when closed. */
    public static final class Sections {
        public final Rect viewport = new Rect();
        public final Rect[] headers;
        public final Rect[] bodies;
        public float contentHeight, maxScroll, scroll;
        public Sections(int count) {
            headers = new Rect[Math.max(0, count)]; bodies = new Rect[headers.length];
            for (int i=0;i<headers.length;i++) { headers[i]=new Rect(); bodies[i]=new Rect(); }
        }
        public void layout(float x, float y, float w, float h, float headerH, float gap, float[] bodyHeights, float[] openness, float requestedScroll) {
            viewport.set(x,y,Math.max(1f,w),Math.max(1f,h));
            float total=0f, hh=Math.max(1f,headerH), gg=Math.max(0f,gap);
            for (int i=0;i<headers.length;i++) { total += hh; float bh=at(bodyHeights,i)*clamp(at(openness,i),0f,1f); if (bh>0) total+=gg+bh; if(i+1<headers.length) total+=gg; }
            contentHeight=total; maxScroll=Math.max(0f,total-viewport.h); scroll=clamp(requestedScroll,0f,maxScroll);
            float cursor=viewport.y-scroll;
            for (int i=0;i<headers.length;i++) { headers[i].set(viewport.x,cursor,viewport.w,hh); cursor+=hh; float bh=at(bodyHeights,i)*clamp(at(openness,i),0f,1f); if(bh>0)cursor+=gg; bodies[i].set(viewport.x+PAD,cursor,Math.max(1f,viewport.w-PAD*2f),bh); cursor+=bh; if(i+1<headers.length)cursor+=gg; }
        }
        private static float at(float[] a,int i){return a==null||i<0||i>=a.length?0f:Math.max(0f,a[i]);}
    }

    /** Keeps every draggable panel edge reachable. */
    public static void clampPanel(Rect out, float wantedX, float wantedY, float width, float height, float screenW, float screenH) {
        float w = Math.max(1f, Math.min(Math.max(1f,width),Math.max(1f,screenW)));
        float h = Math.max(1f, Math.min(Math.max(1f,height),Math.max(1f,screenH)));
        out.set(clamp(wantedX,0f,Math.max(0f,screenW-w)),clamp(wantedY,0f,Math.max(0f,screenH-h)),w,h);
    }
    public static float clamp(float v,float min,float max){return max<min?min:(v<min?min:(v>max?max:v));}
}
