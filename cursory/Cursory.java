package cursory;

import com.sun.jna.Platform;

public abstract class Cursory implements AutoCloseable {

    public class Event {
        public String eventType;
        public String which;
        public Event(String eventType, String which) {
            this.eventType = eventType;
            this.which = which;
        }
        public Event() { this("", ""); }
    }

    public class XY {
        public int x;
        public int y;
        public XY(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    abstract public XY getSize() throws Exception;
    abstract public void restoreMode() throws Exception;
    abstract public void enableRawMode() throws Exception;
    abstract public void render(Iterable<RenderAction> actions);
    public void close() throws Exception { restoreMode(); }

    public static boolean isTerminal(int fileDescriptor) throws Exception {
        if (Platform.isMac()) {
            return CursoryUnix.isatty(fileDescriptor);
        } else {
            throw new Exception("Unsupported platform");
        }
    }

    public static Cursory getTerminal(int fileDescriptor) throws Exception {
        if (Platform.isMac()) {
            return new CursoryUnix(fileDescriptor);
        } else {
            throw new Exception("Unsupported platform");
        }
    }
}
