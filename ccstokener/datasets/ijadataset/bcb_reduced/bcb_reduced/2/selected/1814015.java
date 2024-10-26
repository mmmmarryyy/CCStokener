package net.sf.gridarta.textedit.scripteditor;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import net.sf.gridarta.textedit.textarea.JEditTextArea;
import net.sf.gridarta.utils.IOUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class implements a popup window which shows all python methods in the
 * 'CFPython' package. <p/> As JPopupMenus are not scrollable, the
 * implementation of the combo box popup menu (standard UI) is used here. This
 * is not the perfect approach as it imposes some unwanted limitations. However,
 * the "perfect approach" would require full coding of a JWindow rendered as a
 * popup menu - which is an extremely time consuming task.
 * @author <a href="mailto:andi.vogl@gmx.net">Andreas Vogl</a>
 */
public class CFPythonPopup extends JComboBox {

    /**
     * Python menu definitions.
     */
    @NotNull
    private static final String PYTHON_MENU_FILE = "cfpython_menu.def";

    /**
     * The Logger for printing log messages.
     */
    @NotNull
    private static final Category log = Logger.getLogger(CFPythonPopup.class);

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * List of menu entries (all CFPython commands).
     * @serial
     */
    @Nullable
    private static String[] menuEntries = null;

    /**
     * The popup menu.
     * @serial
     */
    @NotNull
    private final JPopupMenu menu;

    /**
     * Whether this menu has been fully initialized.
     * @serial
     */
    private boolean isReady = false;

    /**
     * The caret position in the document where this popup was opened.
     * @serial
     */
    private int caretPos = 0;

    /**
     * Creates a new instance.
     * @param scriptEditControl the script edit control to forward to
     */
    public CFPythonPopup(@NotNull final ScriptEditControl scriptEditControl) {
        setBackground(Color.white);
        if (menuEntries == null) {
            loadCommandList();
        }
        menu = new CFPythonPopupMenu(this);
        if (menuEntries != null) {
            for (final String menuEntry : menuEntries) {
                addItem(" " + menuEntry);
            }
        }
        addActionListener(new MenuActionListener(this, scriptEditControl));
        if (menuEntries != null && menuEntries.length > 0) {
            isReady = true;
        }
        setRequestFocusEnabled(true);
    }

    /**
     * Load the list of CFPython commands from the data file.
     */
    private static void loadCommandList() {
        final URL url;
        try {
            url = IOUtils.getResource(null, PYTHON_MENU_FILE);
        } catch (final FileNotFoundException ex) {
            log.error("File '" + PYTHON_MENU_FILE + "': " + ex.getMessage());
            return;
        }
        final List<String> cmdList = new ArrayList<String>();
        try {
            final InputStream inputStream = url.openStream();
            try {
                final Reader reader = new InputStreamReader(inputStream, IOUtils.MAP_ENCODING);
                try {
                    final BufferedReader bufferedReader = new BufferedReader(reader);
                    try {
                        while (true) {
                            final String inputLine = bufferedReader.readLine();
                            if (inputLine == null) {
                                break;
                            }
                            final String line = inputLine.trim();
                            if (line.length() > 0 && !line.startsWith("#")) {
                                final int k = line.indexOf('(');
                                if (k > 0) {
                                    cmdList.add(line.substring(0, k) + "()");
                                } else {
                                    log.error("Parse error in " + url + ":");
                                    log.error("   \"" + line + "\" missing '()'");
                                    cmdList.add(line + "()");
                                }
                            }
                        }
                        Collections.sort(cmdList, String.CASE_INSENSITIVE_ORDER);
                        if (!cmdList.isEmpty()) {
                            menuEntries = cmdList.toArray(new String[cmdList.size()]);
                        }
                    } finally {
                        bufferedReader.close();
                    }
                } finally {
                    reader.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (final FileNotFoundException ex) {
            log.error("File '" + url + "' not found: " + ex.getMessage());
        } catch (final EOFException ignored) {
        } catch (final UnsupportedEncodingException ex) {
            log.error("Cannot decode file '" + url + "': " + ex.getMessage());
        } catch (final IOException ex) {
            log.error("Cannot read file '" + url + "': " + ex.getMessage());
        }
    }

    /**
     * Returns whether this popup menu has been fully initialized and is ready
     * for use.
     * @return <code>true</code> if initialized, otherwise <code>false</code>
     */
    public boolean isInitialized() {
        return isReady;
    }

    /**
     * Set the caret position where this menu has been invoked.
     * @param pos caret position in the document
     */
    public void setCaretPosition(final int pos) {
        caretPos = pos;
        menu.requestFocus();
        ScriptEditControl.registerActivePopup(this);
    }

    @NotNull
    public JPopupMenu getMenu() {
        return menu;
    }

    /**
     * Subclass MenuActionListener handles the action events for the menu
     * items.
     */
    private class MenuActionListener implements ActionListener {

        @NotNull
        private final ScriptEditControl control;

        @NotNull
        private final CFPythonPopup popup;

        private boolean ignore = false;

        MenuActionListener(@NotNull final CFPythonPopup popup, @NotNull final ScriptEditControl control) {
            this.popup = popup;
            this.control = control;
        }

        @Override
        public void actionPerformed(@NotNull final ActionEvent e) {
            if (!ignore) {
                String method = popup.getSelectedItem().toString();
                method = method.substring(0, method.indexOf('(')).trim() + "()";
                final JEditTextArea activeTextArea = control.getActiveTextArea();
                if (activeTextArea != null) {
                    try {
                        activeTextArea.getDocument().insertString(caretPos, method, null);
                    } catch (final BadLocationException ex) {
                        log.error("BadLocationException", ex);
                    }
                }
                ignore = true;
                popup.setSelectedIndex(0);
                ignore = false;
                popup.getMenu().setVisible(false);
            }
        }
    }
}
