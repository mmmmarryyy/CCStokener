package org.dynamo.database.ui.console;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * ConsoleWriter objects passed in to the ConsoleAppender as Writer object, it
 * internally writes to the MessageConsole using the MessageConsoleStream
 * provided by org.eclipse.ui.console package.
 */
public class ConsoleWriter extends PrintWriter {

    public static boolean isLogForOSGI;

    public Console _Console;

    private MessageConsoleStream writer;

    public static final int MSG_INFORMATION = 1;

    public static final int MSG_ERROR = 4;

    public static final int MSG_WARNING = 2;

    public static String CONSOLE_NAME = "org.dynamo.database.reverse";

    public static StringBuffer logFile;

    /**
	 * @param out
	 * @param autoFlush
	 */
    public ConsoleWriter(OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
        init();
    }

    /**
	 * @param out
	 */
    public ConsoleWriter(Writer out) {
        super(out);
        init();
    }

    /**
	 * @param out
	 * @param autoFlush
	 */
    public ConsoleWriter(Writer out, boolean autoFlush) {
        super(out, autoFlush);
        init();
    }

    /**
	 * @param out
	 */
    public ConsoleWriter(OutputStream out) {
        super(out);
        init();
    }

    /**
	 * No Arg Ctor.
	 */
    public ConsoleWriter() {
        super(System.out);
        init();
    }

    /**
	 * 
	 */
    private void init() {
        _Console = new Console(CONSOLE_NAME, ImageDescriptor.createFromImage(getImage()));
        Font newFont = new Font(_Console.getFont().getDevice(), "Courier New", 10, 0);
        _Console.setFont(newFont);
        ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { _Console });
        writer = _Console.newMessageStream();
        _Console.setConsoleWriter(this);
        logFile = new StringBuffer();
    }

    private MessageConsoleStream getNewMessageConsoleStream(int msgKind) {
        Color c;
        switch(msgKind) {
            case MSG_INFORMATION:
                c = new Color(Display.getDefault(), 0, 0, 0);
                break;
            case MSG_ERROR:
                c = new Color(Display.getDefault(), 255, 0, 0);
                break;
            case MSG_WARNING:
                c = new Color(Display.getDefault(), 0, 0, 255);
                break;
            default:
                c = new Color(Display.getDefault(), 0, 0, 0);
                break;
        }
        final MessageConsoleStream msgConsoleStream = _Console.newMessageStream();
        final Color cc = c;
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                msgConsoleStream.setColor(cc);
            }
        });
        return msgConsoleStream;
    }

    private Image getImage() {
        try {
            URL url = (URL) Activator.getDefault().getBundle().findEntries("icons", "dtu.gif", false).nextElement();
            return new Image(Display.getDefault(), url.openStream());
        } catch (IOException ex) {
            DTULogger.error(ex.getMessage(), ex);
        }
        return null;
    }

    public void println() {
        writer.println();
    }

    private void log(String text) {
        writer.print(text);
    }

    private void logln(String text) {
        writer.println(text);
    }

    public void print(boolean b) {
        print(String.valueOf(b));
    }

    public void print(char c) {
        print(String.valueOf(c));
    }

    public void print(char[] s) {
        print(String.valueOf(s));
    }

    public void print(double d) {
        print(String.valueOf(d));
    }

    public void print(float f) {
        print(String.valueOf(f));
    }

    public void print(int i) {
        print(String.valueOf(i));
    }

    public void print(long l) {
        print(String.valueOf(l));
    }

    public void print(Object obj) {
        print(String.valueOf(obj));
    }

    public void print(String s) {
        log(String.valueOf(s));
    }

    public void println(boolean x) {
        println(String.valueOf(x));
    }

    public void println(char x) {
        println(String.valueOf(x));
    }

    public void println(char[] x) {
        println(String.valueOf(x));
    }

    public void println(double x) {
        println(String.valueOf(x));
    }

    public void println(float x) {
        println(String.valueOf(x));
    }

    public void println(int x) {
        println(String.valueOf(x));
    }

    public void println(long x) {
        println(String.valueOf(x));
    }

    public void println(Object x) {
        this.println(String.valueOf(x));
    }

    public void println(String x) {
        this.logln(x);
    }

    public void write(char[] buf, int off, int len) {
        if (buf != null && buf.length >= len && off != -1 && off <= len) {
            StringBuffer buffer = new StringBuffer();
            for (int i = off; i < len; i++) {
                buffer.append(buf[i]);
            }
            write(buffer.toString());
        }
    }

    public void write(char[] buf) {
        this.print(buf);
    }

    public void write(int c) {
        this.print(c);
    }

    public void write(String s, int off, int len) {
        if (s != null && s.length() >= len && off != -1 && off <= len) {
            StringBuffer buffer = new StringBuffer();
            for (int i = off; i < len; i++) {
                buffer.append(s.charAt(i));
            }
            this.write(buffer.toString());
        }
    }

    public void write(String s) {
        this.print(s);
    }

    public void print(String text, int severity) {
        try {
            MessageConsoleStream msgConsoleStream = getNewMessageConsoleStream(severity);
            if (!isLogForOSGI) {
                msgConsoleStream.write(text);
            }
        } catch (IOException e) {
            DTULogger.error(e.getMessage(), e);
        }
    }
}
