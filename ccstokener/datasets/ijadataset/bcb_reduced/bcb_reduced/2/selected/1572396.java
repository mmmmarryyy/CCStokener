package Qaop;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

public class Qaop extends Applet implements Runnable, KeyListener, FocusListener, ComponentListener, WindowListener {

    protected Spectrum spectrum;

    private Loader loader;

    private Image img;

    private Dimension size;

    private int posx, posy;

    public static final int MODE_48K = 0;

    public static final int MODE_128K = 1;

    public synchronized void init() {
        showStatus(getAppletInfo());
        addKeyListener(this);
        addFocusListener(this);
        InputStream pin = resource("/qaop128.ini");
        Properties prop = new Properties();
        try {
            prop.load(pin);
        } catch (Exception ex) {
            showStatus("Can't read /qaop128.ini");
        }
        String _ini_mode = (String) prop.get("mode");
        int ini_mode = (_ini_mode != null && _ini_mode.equals("48")) ? MODE_48K : MODE_128K;
        boolean ini_keymatrix = ini_param((String) prop.get("keymatrix"), true);
        String ini_arrows = (String) prop.get("arrows");
        boolean ini_ay = ini_param((String) prop.get("ay"), false);
        boolean ini_muted = ini_param((String) prop.get("muted"), false);
        String smode = param("mode");
        int mode = smode != null ? (smode.equals("48") ? MODE_48K : MODE_128K) : ini_mode;
        spectrum = new Spectrum(mode);
        spectrum.setKeymatrix(ini_keymatrix);
        if (param("focus", true)) addComponentListener(this);
        String rom = param("rom");
        if (rom == null) {
            InputStream in = resource("/rom/spectrum.rom");
            if (in == null || tomem(spectrum.rom48k, 0, 16384, in) != 0) showStatus("Can't read /rom/spectrum.rom");
        }
        String rom128 = param("rom128");
        if (mode != MODE_48K && rom128 == null) {
            InputStream in = resource("/rom/128.rom");
            if (in == null || tomem(spectrum.rom128k, 0, 16384, in) != 0) showStatus("Can't read /rom/128.rom");
        }
        loader = new Loader(this, rom, param("if1rom"), rom128);
        loader.load(param("load"));
        loader.tape(param("tape"));
        String a = param("arrows");
        if (a != null) spectrum.setArrows(a); else if (ini_arrows != null) spectrum.setArrows(ini_arrows);
        if (param("ay", ini_ay)) spectrum.ay(true);
        if (param("muted", ini_muted)) spectrum.mute(true);
        spectrum.start();
        loader.start();
    }

    public synchronized void destroy() {
        spectrum.interrupt();
        loader.interrupt();
        spectrum.audio.close();
    }

    public String getAppletInfo() {
        String version = Qaop.class.getPackage().getImplementationVersion() == null ? "" : Qaop.class.getPackage().getImplementationVersion();
        return "Qaop128 " + version + "- ZX Spectrum emulator Java applet";
    }

    static final String info[][] = { { "rom", "filename", "alternative ROM image" }, { "if1rom", "filename", "enable Interface1; use this ROM" }, { "tape", "filename", "tape file" }, { "load", "filename", "snapshot or tape to load" }, { "focus", "yes/no", "grab focus on start" }, { "arrows", "keys", "define arrow keys" }, { "ay", "yes/no", "with AY" }, { "mute", "yes/no", "muted to start" }, { "mode", "48/128", "spectrum 48K/128K" }, { "rom128", "filename", "alternative 128K ROM image" } };

    public String[][] getParameterInfo() {
        return info;
    }

    public Dimension getPreferredSize() {
        return new Dimension(Spectrum.W, Spectrum.H);
    }

    public synchronized void reset() {
        spectrum.reset();
    }

    public void load(String name) {
        if (loader != null) loader.load(name);
    }

    public void tape(String name) {
        if (loader != null) loader.tape(name);
    }

    public void focus() {
        requestFocus();
    }

    private void resized(Dimension d) {
        size = d;
        int s = d.width >= 512 && d.height >= 384 ? 2 : 1;
        if (spectrum.scale() != s) {
            img = null;
            spectrum.scale(s);
            img = createImage(spectrum);
        }
        posx = (d.width - spectrum.width) / 2;
        posy = (d.height - spectrum.height) / 2;
        dl_image = null;
        loader.reshape(d);
    }

    public void paint(Graphics g) {
        update(g);
    }

    public synchronized void update(Graphics g) {
        Dimension d = getSize();
        if (!d.equals(size)) resized(d);
        if (loader.flength > 0) {
            paint_dl(g);
            return;
        }
        g.drawImage(img, posx, posy, this);
    }

    private Image dl_image;

    private void paint_dl(Graphics g) {
        int x = posx, y = posy;
        int sw = spectrum.width, sh = spectrum.height;
        if (dl_image == null) dl_image = createImage(sw, sh);
        Graphics g2 = dl_image.getGraphics();
        g2.drawImage(img, 0, 0, this);
        g2.translate(loader.x - x, loader.y - y);
        loader.paint(g2);
        g2.dispose();
        g.drawImage(dl_image, x, y, null);
    }

    public boolean imageUpdate(Image i, int f, int x, int y, int w, int h) {
        if ((f & FRAMEBITS) != 0) {
            repaint(posx + x, posy + y, w, h);
            return true;
        }
        if ((f & ~SOMEBITS) == 0) return true;
        return super.imageUpdate(i, f, x, y, w, h);
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        int c = e.getKeyCode();
        boolean m;
        int v;
        if (c == e.VK_DELETE && e.isControlDown()) {
            spectrum.reset();
            return;
        } else if (c == e.VK_F11) {
            spectrum.mute(m = !spectrum.muted);
            v = spectrum.volumeChg(0);
        } else if (c == e.VK_PAGE_UP || c == e.VK_PAGE_DOWN) {
            m = spectrum.muted;
            v = spectrum.volumeChg(c == e.VK_PAGE_UP ? +5 : -5);
        } else {
            keyEvent(e);
            return;
        }
        String s = "Volume: ";
        for (int i = 0; i < v; i += 4) s += "|";
        s += " " + v + "%";
        if (m) s += " (muted)";
        showStatus(s);
    }

    public void keyReleased(KeyEvent e) {
        keyEvent(e);
    }

    void keyEvent(KeyEvent e) {
        KeyEvent[] k = spectrum.keys;
        int c = e.getKeyCode();
        int j = -1;
        synchronized (k) {
            for (int i = 0; i < k.length; i++) {
                if (k[i] == null) {
                    j = i;
                    continue;
                }
                int d = k[i].getKeyCode();
                if (d == c) {
                    j = i;
                    break;
                }
            }
            if (j >= 0) k[j] = e.getID() == KeyEvent.KEY_PRESSED ? e : null;
        }
    }

    public void focusGained(FocusEvent e) {
        showStatus(getAppletInfo());
    }

    public void focusLost(FocusEvent e) {
        KeyEvent[] k = spectrum.keys;
        synchronized (k) {
            for (int i = 0; i < k.length; i++) k[i] = null;
        }
    }

    public void componentResized(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
        removeComponentListener(this);
        requestFocus();
    }

    String param(String n) {
        return param != null ? (String) param.get(n) : getParameter(n);
    }

    boolean ini_param(String val, boolean dflt) {
        if (val == null || val.length() == 0) return dflt;
        char c = Character.toUpperCase(val.charAt(0));
        return c != 'N' && c != 'F' && c != '0';
    }

    boolean param(String name, boolean dflt) {
        String p = param(name);
        if (p == null || p.length() == 0) return dflt;
        char c = Character.toUpperCase(p.charAt(0));
        return c != 'N' && c != 'F' && c != '0';
    }

    private final InputStream resource(String name) {
        return getClass().getResourceAsStream(name);
    }

    private static int tomem(int[] m, int p, int l, InputStream in) {
        do try {
            int v = in.read();
            if (v < 0) break;
            m[p++] = v;
        } catch (IOException ignored) {
            break;
        } while (--l > 0);
        return l;
    }

    protected URL url_of_file(String f) {
        if (f != null) try {
            return new URL(getDocumentBase(), f);
        } catch (MalformedURLException e) {
            showStatus(e.toString());
            System.out.println(e);
        }
        return null;
    }

    private InputStream dl_input;

    private int dl_kind;

    private boolean dl_gz;

    protected void do_load(InputStream in, int kind, boolean gz) {
        dl_input = in;
        dl_kind = kind;
        dl_gz = gz;
    }

    public void run() {
        InputStream in = dl_input;
        dl_input = null;
        try {
            if (dl_gz) in = new GZIPInputStream(in);
            switch(dl_kind) {
                case Loader.TAP:
                    load_tape(in);
                    break;
                case Loader.SNA:
                    load_sna(in);
                    break;
                case Loader.Z80:
                    load_z80(in);
                    break;
                case Loader.CART:
                    spectrum.reset();
                default:
                    load_rom(in, dl_kind);
            }
        } catch (IOException e) {
        }
    }

    private void load_rom(InputStream in, int kind) throws IOException {
        int m[] = new int[0x8000];
        if (tomem(m, 0, kind & 0xFFF0, in) != 0) {
            showStatus("Rom image truncated");
            return;
        }
        if (kind == Loader.IF1ROM) {
            System.arraycopy(m, 0, m, 0x4000, 0x4000);
            spectrum.if1rom = m;
            return;
        }
        if (kind == Loader.ROM) spectrum.rom48k = m;
        if (kind == Loader.ROM128) spectrum.rom128k = m;
    }

    private void load_tape(InputStream in) {
        byte data[] = null;
        int pos = 0;
        for (; ; ) try {
            byte buf[] = new byte[pos + 512];
            int n = in.read(buf, pos, 512);
            if (n < 512) {
                if (n <= 0) break;
                byte buf2[] = new byte[pos + n];
                System.arraycopy(buf, pos, buf2, pos, n);
                buf = buf2;
            }
            if (data != null) System.arraycopy(data, 0, buf, 0, pos);
            data = buf;
            pos += n;
            spectrum.tape(data, false);
            Thread.yield();
        } catch (IOException e) {
            break;
        }
        if (data != null) spectrum.tape(data, true);
    }

    private static int get8(DataInputStream i) throws IOException {
        return i.readUnsignedByte();
    }

    private static int get16(DataInputStream i) throws IOException {
        int b = i.readUnsignedByte();
        return b | i.readUnsignedByte() << 8;
    }

    private void poke_stream(DataInputStream in, int pos, int len) throws IOException {
        do {
            spectrum.mem(pos++, get8(in));
        } while (--len > 0);
    }

    /**
         * write int array to memory
         * @param a int[]
         * @param pos int
         * @param len int
         */
    private void poke_array(int[] a, int pos, int len) {
        for (int i = 0; i < len; i++) {
            spectrum.mem(pos + i, a[i]);
        }
    }

    private void load_sna(InputStream ins) throws IOException {
        DataInputStream in = new DataInputStream(ins);
        spectrum.reset();
        Z80 cpu = spectrum.cpu;
        cpu.i(get8(in));
        cpu.hl(get16(in));
        cpu.de(get16(in));
        cpu.bc(get16(in));
        cpu.af(get16(in));
        cpu.exx();
        cpu.ex_af();
        cpu.hl(get16(in));
        cpu.de(get16(in));
        cpu.bc(get16(in));
        cpu.iy(get16(in));
        cpu.ix(get16(in));
        cpu.ei(get8(in) != 0);
        cpu.r(get8(in));
        cpu.af(get16(in));
        cpu.sp(get16(in));
        cpu.im(get8(in));
        spectrum.out(254, get8(in));
        int[] ram49152 = new int[49152];
        tomem(ram49152, 0, 49152, in);
        try {
            cpu.pc(get16(in));
            System.out.println("It is 128K .SNA");
            int port7ffd = get8(in);
            int trdosrom = get8(in);
            spectrum.out(32765, port7ffd);
            poke_array(ram49152, 16384, 49152);
            for (int i = 0; i < 8; i++) {
                if (i == 2 || i == 5 || (i == (port7ffd & 0x03))) continue;
                tomem(spectrum.get_rambank(i), 0, 16384, in);
            }
        } catch (EOFException e) {
            System.out.println("It is 48K .SNA");
            spectrum.rom = spectrum.rom48k;
            poke_array(ram49152, 16384, 49152);
            int sp = cpu.sp();
            cpu.pc(spectrum.mem16(sp));
            cpu.sp((char) (sp + 2));
            spectrum.mem16(sp, 0);
        }
    }

    private void load_z80(InputStream ins) throws IOException {
        DataInputStream in = new DataInputStream(ins);
        spectrum.reset();
        Z80 cpu = spectrum.cpu;
        cpu.a(get8(in));
        cpu.f(get8(in));
        cpu.bc(get16(in));
        cpu.hl(get16(in));
        int pc = get16(in);
        cpu.pc(pc);
        cpu.sp(get16(in));
        cpu.i(get8(in));
        int f1 = get16(in);
        cpu.r(f1 & 0x7F | f1 >> 1 & 0x80);
        f1 >>>= 8;
        if (f1 == 0xFF) f1 = 0;
        spectrum.out(0xFE, f1 >> 1 & 7);
        cpu.de(get16(in));
        cpu.exx();
        cpu.ex_af();
        cpu.bc(get16(in));
        cpu.de(get16(in));
        cpu.hl(get16(in));
        cpu.a(get8(in));
        cpu.f(get8(in));
        cpu.exx();
        cpu.ex_af();
        cpu.iy(get16(in));
        cpu.ix(get16(in));
        int v = get8(in);
        cpu.iff((v == 0 ? 0 : 1) | (get8(in) == 0 ? 0 : 2));
        cpu.im(get8(in));
        if (pc != 0) {
            if ((f1 & 0x20) != 0) uncompress_z80(in, 16384, 49152); else poke_stream(in, 16384, 49152);
            return;
        }
        int l = get16(in);
        cpu.pc(get16(in));
        int hm = get8(in);
        if (hm > 1) System.out.println("Unsupported model: #" + hm);
        get8(in);
        if (get8(in) == 0xFF) {
            if (spectrum.if1rom != null) {
            }
        }
        if ((get8(in) & 4) != 0 && spectrum.ay_enabled && l >= 23) {
            spectrum.ay_idx = (byte) (get8(in) & 15);
            for (int i = 0; i < 16; i++) spectrum.ay_write(i, get8(in));
            l -= 17;
        }
        in.skip(l - 6);
        for (; ; ) {
            l = get16(in);
            int a;
            switch(get8(in)) {
                case 8:
                    a = 0x4000;
                    break;
                case 4:
                    a = 0x8000;
                    break;
                case 5:
                    a = 0xC000;
                    break;
                default:
                    in.skip(l);
                    continue;
            }
            if (l == 0xFFFF) poke_stream(in, a, 16384); else uncompress_z80(in, a, 16384);
        }
    }

    private int uncompress_z80(DataInputStream in, int pos, int count) throws IOException {
        int end = pos + count;
        int n = 0;
        loop: do {
            int v = get8(in);
            n++;
            if (v != 0xED) {
                spectrum.mem(pos++, v);
                continue;
            }
            v = get8(in);
            n++;
            if (v != 0xED) {
                spectrum.mem16(pos, v << 8 | 0xED);
                pos += 2;
                continue;
            }
            int l = get8(in);
            v = get8(in);
            n += 2;
            while (l > 0) {
                spectrum.mem(pos++, v);
                if (pos >= end) break loop;
                l--;
            }
        } while (pos < end);
        return n;
    }

    private static Hashtable param;

    public URL getDocumentBase() {
        try {
            return super.getDocumentBase();
        } catch (Exception e) {
        }
        try {
            return new URL("file", "", "");
        } catch (Exception e) {
        }
        return null;
    }

    public void showStatus(String s) {
        try {
            super.showStatus(s);
        } catch (Exception e) {
            System.out.println(s);
        }
    }

    public static void main(String args[]) {
        Hashtable p = new Hashtable();
        for (int n = 0; n < args.length; n++) {
            String a = args[n];
            int i = a.indexOf('=');
            if (i >= 0) p.put(a.substring(0, i), a.substring(i + 1)); else p.put("load", a);
        }
        param = p;
        Frame f = new Frame("Qaop");
        Qaop a = new Qaop();
        f.add(a);
        f.addWindowListener(a);
        Insets b = f.getInsets();
        Dimension d = a.getPreferredSize();
        f.setSize(d.width + b.left + b.right, d.height + b.top + b.bottom);
        a.init();
        f.show();
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        destroy();
        System.exit(0);
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }
}

class Loader extends Thread {

    private Qaop qaop;

    Loader(Qaop qaop, String rom, String if1rom, String rom128) {
        this.rom = rom;
        this.if1rom = if1rom;
        this.qaop = qaop;
        this.rom128 = rom128;
    }

    static final int ROM = 0x4000, IF1ROM = 0x2001, CART = 0x4002, ROM128 = 0x4003, TAP = 3, SNA = 4, Z80 = 5;

    private String rom, rom128, if1rom, run, tape;

    synchronized void load(String name) {
        run = name;
        notify();
    }

    synchronized void tape(String name) {
        tape = name;
        notify();
    }

    protected int flength, floaded;

    private String text;

    static final Font font = new Font("SansSerif", 0, 14);

    static final Color fg = Color.white;

    static Color shadow = Color.black, bg;

    static {
        try {
            bg = new Color(0x66999999, true);
            shadow = new Color(0x80111111, true);
        } catch (Throwable t) {
        }
    }

    protected int x, y;

    static final int w = 160, h = 45;

    void reshape(Dimension d) {
        x = (d.width - w) / 2;
        y = (d.height - h) / 2;
    }

    void paint(Graphics g) {
        if (bg != null) {
            g.setColor(bg);
            g.fillRoundRect(0, 0, w, h, 6, 6);
        }
        double perc = (double) floaded / flength;
        if (perc > 1) perc = 1;
        String t = text;
        int tx, ty;
        tx = ty = 0;
        if (t != null) {
            g.setFont(font);
            FontMetrics m = g.getFontMetrics();
            tx = (w - m.stringWidth(t)) / 2;
            ty = 20 - m.getDescent();
        }
        g.setColor(shadow);
        g.translate(1, 1);
        for (boolean f = false; ; f = true) {
            g.drawRect(6, 21, w - 13, 17);
            g.fillRect(8, 21 + 2, (int) (perc * (w - 16)), 17 - 3);
            if (t != null && tx >= 0) g.drawString(t, tx, ty);
            if (f) return;
            g.setColor(fg);
            g.translate(-1, -1);
        }
    }

    private static Hashtable cache = new Hashtable();

    public void run() {
        try {
            if (rom != null) {
                URL url = qaop.url_of_file(rom);
                download(url, ROM);
            }
            if (if1rom != null) {
                URL url = qaop.url_of_file(if1rom);
                download(url, IF1ROM);
            }
            if (rom128 != null) {
                URL url = qaop.url_of_file(rom128);
                download(url, ROM128);
            }
            for (; ; ) {
                URL url;
                synchronized (this) {
                    url = qaop.url_of_file(run);
                    run = null;
                }
                if (url != null) {
                    qaop.spectrum.stop_loading();
                    download(url, 0);
                }
                qaop.spectrum.pause(false);
                synchronized (this) {
                    url = qaop.url_of_file(tape);
                    tape = null;
                }
                if (url != null) {
                    qaop.spectrum.tape(null, false);
                    download(url, TAP);
                }
                synchronized (this) {
                    if (run == null && tape == null) wait();
                }
            }
        } catch (InterruptedException e) {
        }
    }

    private void download(URL url, int kind) throws InterruptedException {
        String f = url.getFile();
        int i = f.lastIndexOf('/');
        if (i >= 0) f = f.substring(i + 1);
        text = f;
        f = f.toUpperCase();
        boolean gz = f.endsWith(".GZ");
        if (gz) f = f.substring(0, f.length() - 3);
        if (kind == 0) {
            if (f.endsWith(".SNA")) kind = SNA; else if (f.endsWith(".Z80")) kind = Z80; else if (f.endsWith(".TAP")) kind = TAP; else if (f.endsWith(".ROM")) kind = CART; else {
                qaop.showStatus("Unknown format: " + text);
                return;
            }
            Spectrum s = qaop.spectrum;
            s.pause(true);
            if (kind == TAP) {
                s.tape(null, false);
                s.autoload();
                s.pause(false);
            }
        }
        Thread t = new Thread(qaop);
        byte data[] = (byte[]) cache.get(url);
        if (data != null) try {
            qaop.do_load(new ByteArrayInputStream(data), kind, gz);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            t.start();
        } catch (Exception e) {
            cache.remove(url);
            data = null;
        }
        if (data == null) {
            floaded = 0;
            flength = 1;
            PipedOutputStream pipe = new PipedOutputStream();
            try {
                qaop.do_load(new PipedInputStream(pipe), kind, gz);
                t.start();
                real_download(url, pipe);
            } catch (FileNotFoundException e) {
                String m = "File not found: " + url;
                qaop.showStatus(m);
                System.out.println(m);
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                qaop.showStatus(e.toString());
            } finally {
                try {
                    pipe.close();
                } catch (IOException e) {
                }
            }
        }
        t.join();
        flength = 0;
        text = null;
        qaop.repaint();
        System.gc();
    }

    private void real_download(URL url, OutputStream pipe) throws Exception {
        ByteArrayOutputStream d = new ByteArrayOutputStream();
        InputStream s = null;
        qaop.repaint(x, y, w, h);
        try {
            URLConnection con = url.openConnection();
            int l = con.getContentLength();
            flength = l > 0 ? l : 1 << 16;
            s = con.getInputStream();
            if (con instanceof java.net.HttpURLConnection) {
                int c = ((java.net.HttpURLConnection) con).getResponseCode();
                if (c < 200 || c > 299) throw new FileNotFoundException();
            }
            byte buf[] = new byte[4096];
            for (; ; ) {
                if (interrupted()) throw new InterruptedException();
                int n = s.available();
                if (n < 1) n = 1; else if (n > buf.length) n = buf.length;
                n = s.read(buf, 0, n);
                if (n <= 0) break;
                d.write(buf, 0, n);
                pipe.write(buf, 0, n);
                floaded += n;
                qaop.repaint(x, y, w, h);
            }
            cache.put(url, d.toByteArray());
        } finally {
            if (s != null) try {
                s.close();
            } catch (IOException e) {
            }
        }
    }
}
