package net.minecraft.client.renderer;

/** Records the modelview scale so the pixel canvas matches what OpenGL would rasterise. */
public class GlStateManager {
    private static final java.util.ArrayDeque<float[]> STACK = new java.util.ArrayDeque<float[]>();
    private static float sx = 1.0F;
    private static float sy = 1.0F;

    public static void pushMatrix() { STACK.push(new float[] { sx, sy }); }
    public static void popMatrix() { if (!STACK.isEmpty()) { float[] t = STACK.pop(); sx = t[0]; sy = t[1]; } }
    public static void scale(float x, float y, float z) { sx *= x; sy *= y; }
    public static void translate(float x, float y, float z) { }
    public static void color(float r, float g, float b, float a) { }
    public static void color(float r, float g, float b) { }
    public static float scaleX() { return sx; }
    public static float scaleY() { return sy; }
    public static void enableTexture2D() { }
    public static void disableTexture2D() { }
    public static void enableBlend() { }
    public static void disableBlend() { }
    public static void enableAlpha() { }
    public static void disableAlpha() { }
    public static void enableLighting() { }
    public static void disableLighting() { }
    public static void tryBlendFuncSeparate(int a, int b, int c, int d) { }
    public static void reset() { STACK.clear(); sx = 1.0F; sy = 1.0F; }
}
