package cursory;

public class RenderAction {
    protected String actionType;
    protected String s;
    protected String t;
    protected int x;
    protected int y;

    private RenderAction(String actionType, String s, String t, int x,
                         int y) {
        this.actionType = actionType;
        this.s = s;
        this.t = t;
        this.x = x;
        this.y = y;
    }

    public static RenderAction clearToLineEnd() {
        return new RenderAction("clearToLineEnd", null, null, 0, 0);
    }

    public static RenderAction clearToScreenEnd() {
        return new RenderAction("clearToScreenEnd", null, null, 0, 0);
    }

    public static RenderAction goAbs(int x, int y) {
        return new RenderAction("goAbs", null, null, x, y);
    }

    public static RenderAction setBackgroundColor(String color) {
        return new RenderAction("setBackgroundColor", color, null, 0, 0);
    }

    public static RenderAction setForegroundColor(String color) {
        return new RenderAction("setForegroundColor", color, null, 0, 0);
    }

    public static RenderAction text(String s) {
        return new RenderAction("text", s, null, 0, 0);
    }

    public static RenderAction boxChar(String s, String style, int x) {
        return new RenderAction("boxChar", s, style, x, 0);
    }
}
