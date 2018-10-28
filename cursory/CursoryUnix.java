package cursory;

import java.lang.StringBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Structure;

public class CursoryUnix extends Cursory {

    public static class TermiosNative {
        static final String libc = "c";
        static { Native.register(libc); }

        public static final short POLLIN = 1;
        public static final int NCCS = 20;
        public static final int TCSAFLUSH = 2;
        public static final int TIOCGWINSZ = 1074295912;

        public static class PollfdStruct extends Structure {
            protected List<String> getFieldOrder() {
                return Arrays.asList(new String[] {
                    "fd",
                    "events",
                    "revents",
                });
            }
            public int fd;
            public short events;
            public short revents;
        }

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

        public static native int poll(PollfdStruct pollfd, int nfds,
                                      int timeout) throws LastErrorException;
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

    private static final int TIMEOUT_MS = 50;
    private TermiosNative.PollfdStruct pollfd =
        new TermiosNative.PollfdStruct();

    // TODO: Support fd's other than stdin.

    private int readByte() throws Exception { return System.in.read(); }

    private int readByteWithTimeout() throws Exception {
        if (System.in.available() < 1) {
            pollfd.fd = 0;
            pollfd.events = TermiosNative.POLLIN;
            if (TermiosNative.poll(pollfd, 1, TIMEOUT_MS) < 1) {
                return -1;
            }
        }
        return readByte();
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

    private static final Map<Integer, String> specialControlMap;
    static {
        Map<Integer, String> m = new HashMap<Integer, String>();
        m.put(0x08, "backspace");
        m.put(0x09, "tab");
        m.put(0x0a, "return");
        m.put(0x0d, "return");
        m.put(0x7f, "backspace");
        specialControlMap = m;
    }

    private static final Map<String, String> escapeMap;
    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("", "escape");
        m.put("[15~", "F5");
        m.put("[17~", "F6");
        m.put("[18~", "F7");
        m.put("[19~", "F8");
        m.put("[1;2P", "printscreen");
        m.put("[20~", "F9");
        m.put("[21~", "F10");
        m.put("[3~", "delete");
        m.put("[5~", "pageup");
        m.put("[6~", "pagedown");
        m.put("[A", "up");
        m.put("[B", "down");
        m.put("[C", "right");
        m.put("[D", "left");
        m.put("[F", "end");
        m.put("[H", "home");
        m.put("OF", "end");
        m.put("OH", "home");
        m.put("OP", "F1");
        m.put("OQ", "F2");
        m.put("OR", "F3");
        m.put("OS", "F4");
        escapeMap = m;
    }

    private String readEscape() throws Exception {
        int byt = readByteWithTimeout();
        if (byt == -1) {
            return "";
        }
        if ((byt != 'O') && (byt != '[')) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append((char)byt);
        do {
            byt = readByteWithTimeout();
            if ((byt == -1) || (sb.length() > 8)) {
                return null;
            }
            sb.append((char)byt);
        } while ((byt == ';') || ((byt >= '0') && (byt <= '9')));
        return sb.toString();
    }

    private Event escape() throws Exception {
        String which = escapeMap.get(readEscape());
        if (which != null) {
            return new Event("specialkey", which);
        }
        return new Event();
    }

    private Event control(int byt) {
        String which = specialControlMap.get(byt);
        if (which != null) {
            return new Event("specialkey", which);
        }
        which = String.valueOf((char)(0x40 + byt));
        return new Event("controlkey", which);
    }

    public Event readEvent() throws Exception {
        int byt = readByte();
        if (byt < 0x01) {
            return new Event();
        } else if (byt < 0x1b) {
            return control(byt);
        } else if (byt == 0x1b) {
            return escape();
        } else if (byt < 0x20) {
            return new Event();
        } else if (byt < 0x7f) {
            return new Event("char", String.valueOf((char)byt));
        } else {
            return control(byt);
        }
    }
}
