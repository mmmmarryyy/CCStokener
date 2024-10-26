package com.bluemarsh.jswat.ui.console;

import com.bluemarsh.jswat.ContextManager;
import com.bluemarsh.jswat.Log;
import com.bluemarsh.jswat.PathManager;
import com.bluemarsh.jswat.Session;
import com.bluemarsh.jswat.SourceSource;
import com.bluemarsh.jswat.VMConnection;
import com.bluemarsh.jswat.command.CommandManager;
import com.bluemarsh.jswat.event.SessionEvent;
import com.bluemarsh.jswat.event.SessionListener;
import com.bluemarsh.jswat.ui.AbstractAdapter;
import com.bluemarsh.jswat.ui.Bundle;
import com.bluemarsh.jswat.ui.NoOpenViewException;
import com.bluemarsh.jswat.ui.StartupRunner;
import com.bluemarsh.jswat.view.View;
import com.sun.jdi.Location;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class ConsoleAdapter connects the Session with the user interface of
 * JSwat. It builds out the major interface components, connects them to
 * the Session and managers, and handles some user input. This subclass
 * of the <code>UIAdapter</code> class builds out a console interface,
 * which runs entirely based on <code>stdout</code> and
 * <code>stdin</code>.
 *
 * @author  Nathan Fiedler
 */
public class ConsoleAdapter extends AbstractAdapter {

    /** Session we are associated with. */
    private Session ourSession;

    /** Log to which messages are printed. */
    private Log statusLog;

    /** This output stream supports printing the command prompt. */
    private ConsoleOutputStream outputStream;

    /** Handles the output from the debuggee VM. */
    private ConsoleOutputAdapter outputAdapter;

    /** Map of SourceSource to String[] instances. The String arrays
     * hold the lines read in from the SourceSource. */
    private HashMap loadedFiles;

    /** The command input prompt string, must not be null. */
    private String inputPrompt;

    /**
     * Constructs a ConsoleAdapter.
     */
    public ConsoleAdapter() {
        loadedFiles = new HashMap();
        updateInputPrompt(null);
    }

    /**
     * In a graphical environment, bring the primary debugger window
     * forward so the user can see it. This is called primarily when a
     * debugger event has occurred and the debugger may be hidden behind
     * the debuggee application window.
     */
    public void bringForward() {
    }

    /**
     * Construct the appropriate user interface and connect all the
     * pieces together. The result should be a fully functional
     * interface that is ready to be used.
     */
    public void buildInterface() {
        statusLog.start(Thread.NORM_PRIORITY);
        outputStream = new ConsoleOutputStream(System.out);
        statusLog.attach(outputStream);
        outputAdapter = new ConsoleOutputAdapter(statusLog);
        ourSession.addListener(outputAdapter);
    }

    /**
     * Indicate if this interface adapter has the ability to find a
     * string in the currently selected source view.
     *
     * @return  always returns false.
     */
    public boolean canFindString() {
        return false;
    }

    /**
     * Indicate if this interface adapter has the ability to show source
     * files in a manner appropriate for the user to read.
     *
     * @return  always returns false.
     */
    public boolean canShowFile() {
        return true;
    }

    /**
     * Deconstruct the user interface such that all components are made
     * invisible and prepared for non-use.
     */
    public void destroyInterface() {
        ourSession.removeListener(outputAdapter);
        statusLog.detach(outputStream);
    }

    /**
     * This is called when there are no more open Sessions. The adapter
     * should take the appropriate action at this time. In most cases
     * that will be to exit the JVM.
     */
    public void exit() {
        System.exit(0);
    }

    /**
     * Search for the given string in the currently selected source
     * view. The search should continue from the last successful match,
     * and wrap around to the beginning when the end is reached. This
     * implementation throws <code>UnsupportedOperationException</code>
     * since the console adapter does not support views.
     *
     * @param  query       string to look for.
     * @param  ignoreCase  true to ignore case.
     * @return  true if string was found.
     * @throws  NoOpenViewException
     *          if there is not source view opened.
     */
    public boolean findString(String query, boolean ignoreCase) throws NoOpenViewException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the currently active view in JSwat. This implementation
     * throws <code>UnsupportedOperationException</code> since the
     * console adapter does not support views.
     *
     * @return  selected view, or null if none selected.
     */
    public View getSelectedView() {
        throw new UnsupportedOperationException();
    }

    /**
     * Perform any initialization that requires a Session instance. This is
     * called after the object is constructed and before
     * <code>buildInterface()</code> is called.
     *
     * @param  session  session to associate with.
     */
    public void init(Session session) {
        ourSession = session;
        statusLog = session.getStatusLog();
    }

    /**
     * Called when the Session initialization has completed.
     */
    public void initComplete() {
        CommandManager cmdman = (CommandManager) ourSession.getManager(CommandManager.class);
        String err = StartupRunner.runRCFiles(cmdman);
        if (err != null) {
            statusLog.writeln(err);
        }
        statusLog.writeln(Bundle.getString("initialMsg"));
        new ConsoleInputAdapter(System.in, cmdman);
    }

    /**
     * Loads the contents of the given source into an array of String
     * objects.
     *
     * @param  src  source to load into memory.
     * @return  array of Strings (without line terminators), or null
     *          if source has no input stream.
     * @throws  IOException
     *          if something goes wrong.
     */
    protected String[] loadFile(SourceSource src) throws IOException {
        InputStream is = src.getInputStream();
        if (is == null) {
            return null;
        }
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        ArrayList lines = new ArrayList(100);
        String line = br.readLine();
        while (line != null) {
            lines.add(line);
            line = br.readLine();
        }
        br.close();
        return (String[]) lines.toArray(new String[lines.size()]);
    }

    /**
     * Refresh the display to reflect changes in the program. Generally
     * this means refreshing the panels.
     */
    public void refreshDisplay() {
        loadedFiles.clear();
    }

    /**
     * Save any settings to the appropriate places, the program is about
     * to terminate.
     */
    public void saveSettings() {
    }

    /**
     * Show the given file in the appropriate view and make the given
     * line visible in that view.
     *
     * @param  src    source to be displayed.
     * @param  line   one-based line to be made visible, or zero for
     *                a reasonable default.
     * @param  count  number of lines to display, or zero for a
     *                reasonable default. Some adapters will ignore
     *                this value if, for instance, they utilize a
     *                scrollable view.
     * @return  true if successful, false if error.
     */
    public boolean showFile(SourceSource src, int line, int count) {
        String[] lines = (String[]) loadedFiles.get(src);
        if (lines == null) {
            if (!src.exists()) {
                return false;
            }
            try {
                lines = loadFile(src);
                if (lines == null) {
                    statusLog.writeln(Bundle.getString("couldntMapSrcFile") + " (" + src.getLongName() + ")");
                    return false;
                }
            } catch (IOException ioe) {
                statusLog.writeStackTrace(ioe);
                return false;
            }
            loadedFiles.put(src, lines);
        }
        ContextManager conman = (ContextManager) ourSession.getManager(ContextManager.class);
        Location loc = conman.getCurrentLocation();
        int currentLine = -1;
        if (loc != null) {
            PathManager pathman = (PathManager) ourSession.getManager(PathManager.class);
            try {
                SourceSource source = pathman.mapSource(loc.declaringType());
                if (source.equals(src)) {
                    currentLine = loc.lineNumber();
                }
            } catch (IOException ioe) {
            }
        }
        if (line <= 0) {
            line = 1;
            if (count <= 0) {
                statusLog.writeln(Bundle.getString("console.showFirstLines"));
                count = 10;
            }
        } else if (count <= 0) {
            count = 1;
        }
        int max = Math.min(line + count, lines.length + 1);
        for (int ii = line; ii < max; ii++) {
            statusLog.write(String.valueOf(ii));
            statusLog.write(": ");
            if (ii == currentLine) {
                statusLog.write("===>");
            } else {
                statusLog.write("    ");
            }
            statusLog.writeln(lines[ii - 1]);
        }
        return true;
    }

    /**
     * Show a help screen written in HTML. This is may be implemented
     * like the <code>showURL()</code> method, but should have buttons
     * for navigating the help content.
     *
     * @param  url  help screen to be shown to the user.
     */
    public void showHelp(URL url) {
    }

    /**
     * Show a message in an appropriate location.
     *
     * @param  type  one of the message types defined in this class.
     * @param  msg   message to be shown to the user.
     */
    public void showMessage(int type, String msg) {
        if (type == MESSAGE_ERROR) {
            msg = Bundle.getString("msg.error.prefix") + ' ' + msg;
        } else if (type == MESSAGE_WARNING) {
            msg = Bundle.getString("msg.warn.prefix") + ' ' + msg;
        }
        if (statusLog != null) {
            statusLog.writeln(msg);
        } else {
            System.out.println(msg);
        }
    }

    /**
     * Show a URL in a reasonable manner. This will likely involve using
     * a <code>JEditorPane</code> or some similar class to display the
     * file referenced by the <code>URL</code>.
     *
     * @param  url    URL to be shown to the user.
     * @param  title  title for the window showing the URL, if any.
     */
    public void showURL(URL url, String title) {
        try {
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            char[] buf = new char[8192];
            StringWriter sw = new StringWriter();
            int bytesRead = isr.read(buf);
            while (bytesRead > 0) {
                sw.write(buf, 0, bytesRead);
                bytesRead = isr.read(buf);
            }
            isr.close();
            String s = sw.toString();
            s = s.replaceAll("<[^>]+>", "");
            s = s.trim();
            statusLog.writeln(s);
        } catch (IOException ioe) {
            statusLog.writeln("Error reading URL in showURL(): " + ioe.getMessage());
        }
    }

    /**
     * Change the prompt displayed beside the command input field.
     *
     * @param  prompt  new input prompt, or null to display default.
     */
    public void updateInputPrompt(String prompt) {
        if (prompt == null) {
            prompt = "> ";
        }
        inputPrompt = prompt;
    }

    /**
     * <p>Class ConsoleInputAdapter adapts the standard input stream to
     * the CommandManager.</p>
     *
     * @author  Nathan Fiedler
     */
    protected class ConsoleInputAdapter implements Runnable {

        /** Where input is sent. */
        private CommandManager commandManager;

        /** Where input comes from. */
        private BufferedReader inputReader;

        /**
         * Constructs a ConsoleInputAdapter to read from the given input
         * stream and send the input to the given command manager.
         *
         * @param  input   input stream.
         * @param  cmdman  CommandManager to send input to.
         */
        public ConsoleInputAdapter(InputStream input, CommandManager cmdman) {
            inputReader = new BufferedReader(new InputStreamReader(input));
            commandManager = cmdman;
            Thread th = new Thread(this);
            th.setPriority(Thread.NORM_PRIORITY);
            th.start();
        }

        /**
         * Read from the input stream and send the input to the command
         * manager.
         */
        public void run() {
            try {
                while (true) {
                    statusLog.flush();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                    }
                    outputStream.printPrompt(inputPrompt);
                    String str = inputReader.readLine();
                    outputStream.needPrompt();
                    if (str.length() > 0) {
                        try {
                            commandManager.handleInput(str);
                        } catch (Exception e) {
                            statusLog.writeStackTrace(e);
                        }
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}

/**
 * Class ConsoleOutputAdapter is responsible for displaying the output
 * of a debuggee process to the Log. It reads both the standard output
 * and standard error streams from the debuggee VM. For it to operate
 * correctly it must be added as a session listener.
 *
 * @author  Nathan Fiedler
 */
class ConsoleOutputAdapter implements SessionListener {

    /** Log to send output to. */
    private Log outputLog;

    /**
     * Constructs a ConsoleOutputAdapter to output to the given Log.
     *
     * @param  log  Log to output to.
     */
    public ConsoleOutputAdapter(Log log) {
        outputLog = log;
    }

    /**
     * Called when the Session has activated. This occurs when the
     * debuggee has launched or has been attached to the debugger.
     *
     * @param  sevt  session event.
     */
    public void activated(SessionEvent sevt) {
        VMConnection vmc = sevt.getSession().getConnection();
        if (!vmc.isRemote()) {
            displayOutput(vmc.getProcess().getErrorStream());
            displayOutput(vmc.getProcess().getInputStream());
        }
    }

    /**
     * Called when the Session is about to be closed.
     *
     * @param  sevt  session event.
     */
    public void closing(SessionEvent sevt) {
    }

    /**
     * Called when the Session has deactivated. The debuggee VM is no
     * longer connected to the Session.
     *
     * @param  sevt  session event.
     */
    public synchronized void deactivated(SessionEvent sevt) {
    }

    /**
     * Create a thread that will retrieve and display any output from
     * the given input stream.
     *
     * @param  is  InputStream to read from.
     */
    protected void displayOutput(final InputStream is) {
        Thread thr = new Thread("output reader") {

            public void run() {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String line = br.readLine();
                    while (line != null) {
                        outputLog.writeln(line);
                        line = br.readLine();
                    }
                } catch (IOException ioe) {
                    outputLog.writeln(Bundle.getString("errorReadingOutput"));
                }
            }
        };
        thr.setPriority(Thread.MIN_PRIORITY);
        thr.start();
    }

    /**
     * Called after the Session has added this listener to the Session
     * listener list.
     *
     * @param  session  the Session.
     */
    public void opened(Session session) {
    }

    /**
     * Called when the debuggee is about to be resumed.
     *
     * @param  sevt  session event.
     */
    public void resuming(SessionEvent sevt) {
    }

    /**
     * Called when the debuggee has been suspended.
     *
     * @param  sevt  session event.
     */
    public void suspended(SessionEvent sevt) {
    }
}

/**
 * <p>Class ConsoleOutputStream is responsible for printing the Log
 * output to the console stream. It has an additonal operation for
 * printing a command input prompt.</p>
 *
 * @author  Nathan Fiedler
 */
class ConsoleOutputStream extends OutputStream {

    /** The output stream to which we print. */
    private OutputStream sink;

    /** True if the prompt was the last thing we printed. */
    private boolean promptPrinted;

    /** The system line separator in byte form. */
    private byte[] lineSeparator;

    /**
     * Creates an output stream filter built on top of the specified
     * underlying output stream. This output stream has an additional
     * operation for printing a given command input prompt.
     *
     * @param  out  the underlying output stream to be assigned
     *              to the field <code>this.out</code> for later
     *              use, or null if this instance is to be created
     *              without an underlying stream.
     */
    public ConsoleOutputStream(OutputStream out) {
        sink = out;
        String ls = System.getProperty("line.separator");
        lineSeparator = ls.getBytes();
    }

    /**
     * The prompt needs to be printed at some point.
     */
    public synchronized void needPrompt() {
        promptPrinted = false;
    }

    /**
     * Print the previously set command prompt to the output stream.
     * This method bypasses the Log and prints directly to the
     * underlying output stream.
     *
     * @param  prompt  comand input prompt.
     *
     * @throws  IOException
     *          if an I/O error occurs.
     */
    public void printPrompt(String prompt) throws IOException {
        synchronized (this) {
            if (!promptPrinted) {
                sink.write(prompt.getBytes());
                promptPrinted = true;
            }
        }
    }

    /**
     * Writes the specified byte to this output stream. This
     * implementation determines if the command input prompt was the
     * last thing printed. If so, a line separator is printed before the
     * given byte is sent to the underlying stream.
     *
     * @param  b  the byte.
     * @throws  IOException
     *          if an I/O error occurs.
     */
    public void write(int b) throws IOException {
        synchronized (this) {
            if (promptPrinted) {
                sink.write(lineSeparator);
                promptPrinted = false;
            }
            sink.write(b);
        }
    }
}
