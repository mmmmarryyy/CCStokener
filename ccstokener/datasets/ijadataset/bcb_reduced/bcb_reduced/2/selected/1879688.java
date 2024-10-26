package org.jbuzz.telnet.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URL;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TerminalHandler {

    static HashMap<String, TerminalHandler> instances;

    static DocumentBuilderFactory documentBuilderFactory;

    static DocumentBuilder documentBuilder;

    static {
        instances = new HashMap<String, TerminalHandler>();
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (Throwable t) {
        }
    }

    /**
	 * Get terminal handler corresponding to the terminal type.
	 * 
	 * @param type
	 *            type of the terminal
	 * @return terminal handler corresponding to the terminal type
	 */
    public static TerminalHandler getInstance(String type) {
        TerminalHandler handler = TerminalHandler.instances.get(type);
        if (handler == null) {
            handler = new TerminalHandler(type);
            TerminalHandler.instances.put(type, handler);
        }
        return handler;
    }

    String type;

    KeySequence keySequence;

    HashMap<String, Control> controls;

    Control getCursorControl;

    Control setCursorControl;

    Control setCursorUpControl;

    Control setCursorDownControl;

    Control setCursorRightControl;

    Control setCursorLeftControl;

    Control saveCursorControl;

    Control restoreCursorControl;

    Control scrollScreenUpControl;

    Control scrollScreenDownControl;

    Control scrollScreenRightControl;

    Control scrollScreenLeftControl;

    Control eraseScreenControl;

    Control eraseLineControl;

    TerminalHandler(String type) {
        this.type = type;
        String urlString = new StringBuffer("/org/jbuzz/telnet/terminal/").append(type).append(".xml").toString();
        URL url = TerminalHandler.class.getResource(urlString);
        InputStream inputStream = null;
        Document document = null;
        try {
            inputStream = url.openStream();
            document = documentBuilder.parse(inputStream);
        } catch (Exception e) {
            return;
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
            }
        }
        Element documentElement = document.getDocumentElement();
        String extendType = documentElement.getAttribute("extends");
        this.keySequence = new KeySequence(documentElement, extendType);
        NodeList controlElements = documentElement.getElementsByTagName("control");
        int controlElementLength = controlElements.getLength();
        if (extendType.length() > 0) {
            TerminalHandler terminalHandler = TerminalHandler.getInstance(extendType);
            this.controls = new HashMap<String, Control>(terminalHandler.controls.size() + (controlElementLength >> 1));
            for (Control control : terminalHandler.controls.values()) {
                this.controls.put(control.name, control);
            }
        } else {
            this.controls = new HashMap<String, Control>(controlElementLength);
        }
        for (int i = 0; i < controlElementLength; i++) {
            Element controlElement = (Element) controlElements.item(i);
            String name = controlElement.getAttribute("name");
            String output = controlElement.getAttribute("output");
            if (output.length() > 0) {
                String input = controlElement.getAttribute("input");
                if (input.length() == 0) {
                    this.controls.put(name, new Control(name, output));
                } else {
                    this.controls.put(name, new Control(name, output, input));
                }
            }
        }
        this.getCursorControl = this.controls.get("getCursor");
        this.setCursorControl = this.controls.get("setCursor");
        this.setCursorUpControl = this.controls.get("setCursorUp");
        this.setCursorDownControl = this.controls.get("setCursorDown");
        this.setCursorRightControl = this.controls.get("setCursorRight");
        this.setCursorLeftControl = this.controls.get("setCursorLeft");
        this.saveCursorControl = this.controls.get("saveCursor");
        this.restoreCursorControl = this.controls.get("restoreCursor");
        this.scrollScreenUpControl = this.controls.get("scrollScreenUp");
        this.scrollScreenDownControl = this.controls.get("scrollScreenDown");
        this.scrollScreenRightControl = this.controls.get("scrollScreenRight");
        this.scrollScreenLeftControl = this.controls.get("scrollScreenLeft");
        this.eraseScreenControl = this.controls.get("eraseScreen");
        this.eraseLineControl = this.controls.get("eraseLine");
    }

    /**
	 * Get type of the terminal emulator.
	 * 
	 * @return type of the terminal emulator
	 */
    public String getType() {
        return this.type;
    }

    public int[] getCursor(Terminal terminal) throws IOException {
        return null;
    }

    public void setCursor(Terminal terminal, int column, int row) throws IOException {
        this.setCursorControl.control(terminal, String.valueOf(row), String.valueOf(column));
    }

    public void setCursor(Terminal terminal, int column, int row, boolean relative) throws IOException {
        if (relative) {
            if (column > 0) {
                this.setCursorRightControl.control(terminal, String.valueOf(column));
            } else if (column < 0) {
                this.setCursorLeftControl.control(terminal, String.valueOf(-column));
            }
            if (row > 0) {
                this.setCursorDownControl.control(terminal, String.valueOf(row));
            } else if (row < 0) {
                this.setCursorUpControl.control(terminal, String.valueOf(-row));
            }
        } else {
            this.setCursorControl.control(terminal, String.valueOf(row), String.valueOf(column));
        }
    }

    public void setCursorUp(Terminal terminal, int times) throws IOException {
        this.setCursorUpControl.control(terminal, String.valueOf(times));
    }

    public void setCursorDown(Terminal terminal, int times) throws IOException {
        this.setCursorDownControl.control(terminal, String.valueOf(times));
    }

    public void setCursorRight(Terminal terminal, int times) throws IOException {
        this.setCursorRightControl.control(terminal, String.valueOf(times));
    }

    public void setCursorLeft(Terminal terminal, int times) throws IOException {
        this.setCursorLeftControl.control(terminal, String.valueOf(times));
    }

    public void saveCursor(Terminal terminal) throws IOException {
        this.saveCursorControl.control(terminal);
    }

    public void restoreCursor(Terminal terminal) throws IOException {
        this.restoreCursorControl.control(terminal);
    }

    public void scrollScreen(Terminal terminal, int column, int row) throws IOException {
        if (column > 0) {
            this.scrollScreenRightControl.control(terminal, String.valueOf(column));
        } else if (column < 0) {
            this.scrollScreenLeftControl.control(terminal, String.valueOf(-column));
        }
        if (row > 0) {
            this.scrollScreenDownControl.control(terminal, String.valueOf(row));
        } else if (row < 0) {
            this.scrollScreenUpControl.control(terminal, String.valueOf(-row));
        }
    }

    public void eraseScreen(Terminal terminal) throws IOException {
        this.eraseScreen(terminal, Terminal.ERASE_ALL);
    }

    public void eraseScreen(Terminal terminal, int erasePolicy) throws IOException {
        this.eraseScreenControl.control(terminal, String.valueOf(erasePolicy));
    }

    public void eraseLine(Terminal terminal) throws IOException {
        this.eraseLine(terminal, Terminal.ERASE_ALL);
    }

    public void eraseLine(Terminal terminal, int erasePolicy) throws IOException {
        this.eraseLineControl.control(terminal, String.valueOf(erasePolicy));
    }

    public void control(Terminal terminal, String name) throws IOException {
        Control control = this.controls.get(name);
        if (control != null) {
            control.control(terminal);
        }
    }

    public void control(Terminal terminal, String name, String[] parameters) throws IOException {
        Control control = this.controls.get(name);
        if (control != null) {
            control.control(terminal, parameters);
        }
    }

    public int handle(PushbackInputStream inputStream) throws IOException {
        int terminalKey = keySequence.mapKey(inputStream);
        if (terminalKey == Terminal.INVALID) {
            return inputStream.read() & 0xff;
        } else {
            return terminalKey;
        }
    }
}
