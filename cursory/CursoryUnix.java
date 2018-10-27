package cursory;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Structure;

public class CursoryUnix extends Cursory {

    public static class TermiosNative {
        static final String libc = "c";
        static { Native.register(libc); }

        public static final int NCCS = 20;
        public static final int TCSAFLUSH = 2;
        public static final int TIOCGWINSZ = 1074295912;

        public static class WinsizeStruct extends Structure {
            protected List<String> getFieldOrder() {
                return Arrays.asList(new String[] {
                    "ws_row",
                    "ws_col",
                    "ws_xpixel",
                    "ws_ypixel",
                });
            }
            public short ws_row;
            public short ws_col;
            public short ws_xpixel;
            public short ws_ypixel;
        }

        public static class TermiosStruct extends Structure {
            protected List<String> getFieldOrder() {
                return Arrays.asList(new String[] {
                    "c_iflag",
                    "c_oflag",
                    "c_cflag",
                    "c_lflag",
                    "c_cc",
                    "c_ispeed",
                    "c_ospeed",
                });
            }
            public long c_iflag;
            public long c_oflag;
            public long c_cflag;
            public long c_lflag;
            public byte[] c_cc = new byte[NCCS];
            public long c_ispeed;
            public long c_ospeed;
        }

        public static native int tcgetattr(int fd, TermiosStruct termios_p)
            throws LastErrorException;
        public static native int tcsetattr(int fd, int optional_actions,
                                           TermiosStruct termios_p)
            throws LastErrorException;
        public static native void cfmakeraw(TermiosStruct termios_p);
        public static native int isatty(int fd);
        public static native int ioctl(int fd, long request,
                                       WinsizeStruct ttysize_p)
            throws LastErrorException;
    }

    public static boolean isatty(int fileDescriptor) throws Exception {
        return TermiosNative.isatty(fileDescriptor) != 0;
    }

    private int fd;
    private TermiosNative.TermiosStruct origMode = null;

    public CursoryUnix(int fileDescriptor) throws Exception {
        this.fd = fileDescriptor;
        this.origMode = new TermiosNative.TermiosStruct();
        if (TermiosNative.tcgetattr(this.fd, this.origMode) == -1)
            throw new Exception("tcgetattr");
    }

    protected void useMode(TermiosNative.TermiosStruct mode)
        throws Exception {
        if (TermiosNative.tcsetattr(this.fd, TermiosNative.TCSAFLUSH, mode) ==
            -1) {
            throw new Exception("tcsetattr");
        }
    }

    public void enableRawMode() throws Exception {
        TermiosNative.TermiosStruct rawMode =
            new TermiosNative.TermiosStruct();
        TermiosNative.cfmakeraw(rawMode);
        useMode(rawMode);
    }

    public void restoreMode() throws Exception { useMode(origMode); }

    public XY getSize() throws Exception {
        TermiosNative.WinsizeStruct ws = new TermiosNative.WinsizeStruct();
        if (TermiosNative.ioctl(this.fd, TermiosNative.TIOCGWINSZ, ws) ==
            -1) {
            throw new Exception("TIOCGWINSZ");
        }
        return new XY(ws.ws_col, ws.ws_row);
    }

    public Event readEvent() throws Exception {
        Event e = new Event();
        int byt = System.in.read(); // TODO: Support fd's other than stdin.
        if (byt == -1) {
            e.eventType = "";
        } else {
            e.eventType = "char";
            e.which = new String(new byte[] {(byte)byt});
        }
        return e;
    }
}
