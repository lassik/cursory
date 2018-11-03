package cursory;

import java.lang.StringBuilder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        m.put(0x08, "Backspace");
        m.put(0x09, "Tab");
        m.put(0x0a, "Return");
        m.put(0x0d, "Return");
        m.put(0x7f, "Backspace");
        specialControlMap = m;
    }

    private static final Map<String, String> escapeMap;
    static {
        Map<String, String> m = new HashMap<String, String>();

        m.put("", "Escape");
        m.put("[15~", "F5");
        m.put("[17~", "F6");
        m.put("[18~", "F7");
        m.put("[19~", "F8");
        m.put("[1;10A", "Alt-Shift-Up");
        m.put("[1;10B", "Alt-Shift-Down");
        m.put("[1;10C", "Alt-Shift-Right");
        m.put("[1;10D", "Alt-Shift-Left");
        m.put("[1;2A", "Shift-Up");
        m.put("[1;2B", "Shift-Down");
        m.put("[1;2C", "Shift-Right");
        m.put("[1;2D", "Shift-Left");
        m.put("[1;2F", "Shift-End");
        m.put("[1;2H", "Shift-Home");
        m.put("[1;2P", "PrintScreen");
        m.put("[1;5A", "Control-Alt-Up");
        m.put("[1;5B", "Control-Alt-Down");
        m.put("[1;5C", "Control-Alt-Right");
        m.put("[1;5D", "Control-Alt-Left");
        m.put("[1;6A", "Control-Shift-Up");
        m.put("[1;6B", "Control-Shift-Down");
        m.put("[1;6C", "Control-Shift-Right");
        m.put("[1;6D", "Control-Shift-Left");
        m.put("[20~", "F9");
        m.put("[21~", "F10");
        m.put("[23~", "F11");
        m.put("[24~", "F12");
        m.put("[3~", "Delete");
        m.put("[5~", "PageUp");
        m.put("[6~", "PageDown");
        m.put("[A", "Up");
        m.put("[B", "Down");
        m.put("[C", "Right");
        m.put("[D", "Left");
        m.put("[e", "F19");
        m.put("[F", "End");
        m.put("[f", "F20");
        m.put("[g", "F21");
        m.put("[h", "F22");
        m.put("[H", "Home");
        m.put("[i", "F23");
        m.put("[j", "F24");
        m.put("[k", "F25");
        m.put("[l", "F26");
        m.put("[m", "F27");
        m.put("[n", "F28");
        m.put("[o", "F29");
        m.put("[p", "F30");
        m.put("[q", "F31");
        m.put("[r", "F32");
        m.put("[s", "F33");
        m.put("[t", "F34");
        m.put("[u", "F35");
        m.put("[v", "F36");
        m.put("[w", "F37");
        m.put("[x", "F38");
        m.put("[y", "F39");
        m.put("[z", "F40");
        m.put("[Z", "Shift-Tab");
        m.put("[{", "F48");
        m.put("OF", "End");
        m.put("OH", "Home");
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
            if (byt == ':') {
                byt = ';';
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
        return new Event("specialkey", "Control-" + which);
    }

    private Event utf8Rune(int byt) throws Exception {
        int rune, cont;
        if (byt < 0) {
            return new Event();
        } else if (byt < 0b10000000) {
            rune = byt;
            cont = 0;
        } else if (byt < 0b11000000) {
            return new Event();
        } else if (byt < 0b11100000) {
            rune = byt & 0b00011111;
            cont = 1;
        } else if (byt < 0b11110000) {
            rune = byt & 0b00001111;
            cont = 2;
        } else if (byt < 0b11111000) {
            rune = byt & 0b00000111;
            cont = 3;
        } else {
            return new Event();
        }
        for (; cont > 0; cont--) {
            byt = readByteWithTimeout();
            if (byt < 0b10000000) {
                return new Event();
            } else if (byt < 0b11000000) {
                rune = (rune << 6) | byt & 0b00111111;
            } else {
                return new Event();
            }
        }
        return new Event("rune", new String(Character.toChars(rune)));
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
            return utf8Rune(byt);
        } else if (byt == 0x7f) {
            return control(byt);
        } else {
            return utf8Rune(byt);
        }
    }

    private boolean isOrdinaryChar(char c) {
        if (c < 0x20) {
            return false;
        } else if (c < 0x7f) {
            return true;
        } else if (c == 0x7f) {
            return false;
        } else {
            return true;
        }
    }

    private void writeOrdinaryChar(char c) {
        if (!isOrdinaryChar(c)) {
            c = '?';
        }
        System.out.print(c);
    }

    private void writeOrdinaryText(String s) {
        for (int i = 0; i < s.length(); i++) {
            writeOrdinaryChar(s.charAt(i));
        }
    }

    private void writeEscape(String esc) {
        System.out.print("\u001b");
        writeOrdinaryText(esc);
    }

    private void writeEscapeNumbers(int a, int b, char ch) {
        if ((a >= 0) && (b >= 0)) {
            System.out.print("\u001b");
            writeOrdinaryText("[" + String.valueOf(a) + ";" +
                              String.valueOf(b) + String.valueOf(ch));
        }
    }

    public XY getCursorPos() throws Exception {
        writeEscape("[6n");
        System.out.flush();
        byte[] escbuf = new byte[16];
        int len = System.in.read(escbuf);
        if (len < 1) {
            return null;
        }
        String esc = new String(escbuf).substring(0, len);
        Pattern p = Pattern.compile("\u001b\\[(\\d+);(\\d+)R");
        Matcher m = p.matcher(esc);
        if (!m.matches()) {
            return null;
        }
        int x = Integer.parseInt(m.group(1));
        int y = Integer.parseInt(m.group(2));
        return new XY(x, y);
    }

    private static final Map<String, Integer> colorMap;
    static {
        Map<String, Integer> m = new HashMap<String, Integer>();
        m.put("black", 0);
        m.put("red", 1);
        m.put("green", 2);
        m.put("yellow", 3);
        m.put("blue", 4);
        m.put("magenta", 5);
        m.put("cyan", 6);
        m.put("white", 7);
        //
        m.put("default", 9);
        colorMap = m;
    }

    public String colorEscape(int base, String colorName) {
        Integer index = colorMap.get(colorName);
        if (index == null) {
            return null;
        }
        return "[" + String.valueOf(base + index) + "m";
    }

    private static final Map<String, String> boxCharMap;
    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("horz", "q\u2500\u2550");
        m.put("vert", "x\u2502\u2551");
        m.put("cross", "n\u253c\u256c");
        m.put("corner-top-left", "l\u250c\u2554");
        m.put("corner-top-right", "k\u2510\u2557");
        m.put("corner-bottom-left", "m\u2514\u255a");
        m.put("corner-bottom-right", "j\u2518\u255d");
        boxCharMap = m;
    }

    private char boxChar(String charName, String style, boolean vt100) {
        int i;
        if (vt100) {
            i = 0;
        } else if ((style == null) || !style.equals("double")) {
            i = 1;
        } else {
            i = 2;
        }
        String chars = boxCharMap.get(charName);
        if (chars == null) {
            return ' ';
        }
        return chars.charAt(i);
    }

    public void render(Iterable<RenderAction> actions) {
        for (RenderAction action : actions) {
            switch (action.actionType) {
            case "clearToLineEnd":
                writeEscape("[2K");
                break;
            case "clearToScreenEnd":
                writeEscape("[J");
                break;
            case "setBackgroundColor":
                writeEscape(colorEscape(40, action.s));
                break;
            case "setForegroundColor":
                writeEscape(colorEscape(30, action.s));
                break;
            case "goAbs":
                if (action.x == 0 && action.y == 0) {
                    writeEscape("[H");
                } else {
                    writeEscapeNumbers(1 + action.y, 1 + action.x, 'H');
                }
                break;
            case "text":
                writeOrdinaryText(action.s);
                break;
            case "boxChar":
                char c = boxChar(action.s, action.t, true);
                writeEscape("(0");
                for (int i = 0; i < action.x; i++) {
                    writeOrdinaryChar(c);
                }
                writeEscape("(B");
                break;
            default:
                break;
            }
        }
        System.out.flush();
    }
}
