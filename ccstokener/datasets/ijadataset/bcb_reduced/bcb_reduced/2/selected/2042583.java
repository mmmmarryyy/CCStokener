package edu.uah.eduardomoriana.qaopmvc.model.loader;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import edu.uah.eduardomoriana.oldcomputer.model.NeedOldComputer;
import edu.uah.eduardomoriana.oldcomputer.model.OldComputer;
import edu.uah.eduardomoriana.oldcomputer.model.loader.URLLoader;
import edu.uah.eduardomoriana.oldcomputer.model.tape.NeedTape;
import edu.uah.eduardomoriana.oldcomputer.model.tape.Tape;
import edu.uah.eduardomoriana.qaopmvc.model.NeedThreadLoader;
import edu.uah.eduardomoriana.qaopmvc.model.Spectrum;
import edu.uah.eduardomoriana.qaopmvc.model.SpectrumFacade;

public class Loader extends Thread implements URLLoader, NeedTape, NeedThreadLoader, NeedOldComputer {

    private OldComputer oldComputer;

    private URL documentBase = null;

    private Tape tapeExtern = null;

    private Runnable threadLoader;

    public String getRom() {
        return rom;
    }

    public void setRom(String rom) {
        this.rom = rom;
    }

    public String getIf1rom() {
        return if1rom;
    }

    public void setIf1rom(String if1rom) {
        this.if1rom = if1rom;
    }

    public Loader() {
        super();
    }

    private String rom, if1rom, run, tape;

    public synchronized void load(String name) {
        run = name;
        notify();
    }

    public synchronized void tape(String name) {
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

    private int x, y;

    static final int w = 160, h = 45;

    public void reshape(Dimension d) {
        x = (d.width - w) / 2;
        y = (d.height - h) / 2;
    }

    public void paint(Graphics g) {
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

    private Hashtable cache = new Hashtable();

    public URL url_of_file(String f) {
        if (f != null) try {
            URL url = new URL(documentBase, f);
            try {
                URLConnection con = url.openConnection();
                InputStream InputStream = con.getInputStream();
            } catch (IOException e) {
                url = this.getContextClassLoader().getResource(f);
            }
            return url;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void run() {
        for (int xxx = 0; xxx < 100000000; xxx++) {
            int i = 4;
            i++;
            i = i / i;
        }
        try {
            if (rom != null) {
                URL url = url_of_file(rom);
                download(url, ROM);
            }
            if (if1rom != null) {
                URL url = url_of_file(if1rom);
                download(url, IF1ROM);
            }
            for (; ; ) {
                URL url;
                synchronized (this) {
                    url = url_of_file(run);
                    run = null;
                }
                if (url != null) {
                    tapeExtern.stop_loading();
                    download(url, 0);
                }
                oldComputer.setPause(false);
                synchronized (this) {
                    url = url_of_file(tape);
                    tape = null;
                }
                if (url != null) {
                    tapeExtern.loadTape(null, false);
                    download(url, TAP);
                }
                synchronized (this) {
                    if (run == null && tape == null) wait();
                }
            }
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
    }

    private void download(URL url, int kind) throws InterruptedException {
        String file = url.getFile();
        System.out.println(file);
        int i = file.lastIndexOf('/');
        if (i >= 0) file = file.substring(i + 1);
        text = file;
        file = file.toUpperCase();
        boolean gz = file.endsWith(".GZ");
        if (gz) file = file.substring(0, file.length() - 3);
        if (kind == 0) {
            if (file.endsWith(".SNA")) kind = SNA; else if (file.endsWith(".Z80")) kind = Z80; else if (file.endsWith(".TAP")) kind = TAP; else if (file.endsWith(".ROM")) kind = CART; else {
                return;
            }
            oldComputer.setPause(true);
            if (kind == TAP) {
                tapeExtern.loadTape(null, false);
                oldComputer.autoload();
                oldComputer.setPause(false);
            }
        }
        Thread t = new Thread(threadLoader);
        byte data[] = (byte[]) cache.get(url);
        if (data != null) try {
            oldComputer.do_load(new ByteArrayInputStream(data), kind, gz);
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
                oldComputer.do_load(new PipedInputStream(pipe), kind, gz);
                t.start();
                real_download(url, pipe);
            } catch (FileNotFoundException e) {
                String m = "File not found: " + url.getPath();
                System.out.println(m);
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
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
        System.gc();
    }

    private void real_download(URL url, OutputStream pipe) throws Exception {
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        InputStream inputStream = null;
        try {
            URLConnection con = url.openConnection();
            int l = con.getContentLength();
            flength = l > 0 ? l : 1 << 16;
            inputStream = con.getInputStream();
            if (con instanceof java.net.HttpURLConnection) {
                int c = ((java.net.HttpURLConnection) con).getResponseCode();
                if (c < 200 || c > 299) throw new FileNotFoundException();
            }
            byte buf[] = new byte[4096];
            for (; ; ) {
                if (interrupted()) throw new InterruptedException();
                int n = inputStream.available();
                if (n < 1) n = 1; else if (n > buf.length) n = buf.length;
                n = inputStream.read(buf, 0, n);
                if (n <= 0) break;
                arrayOutputStream.write(buf, 0, n);
                System.out.println("escribimos el fichero en accesible");
                pipe.write(buf, 0, n);
                floaded += n;
            }
            cache.put(url, arrayOutputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int getFlength() {
        return flength;
    }

    public URL getBaseDocument() {
        return documentBase;
    }

    public void setBaseDocument(URL baseDocument) {
        this.documentBase = baseDocument;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public void setTape(Tape tape) {
        this.tapeExtern = tape;
    }

    public void setSpectrumActions(SpectrumFacade spectrumActions) {
    }

    public void setThreatLoader(Runnable thread) {
        this.threadLoader = thread;
    }

    public void setOldComputer(OldComputer computer) {
        this.oldComputer = computer;
    }
}
