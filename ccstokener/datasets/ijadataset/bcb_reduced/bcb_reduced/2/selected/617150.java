package org.zmpp.swingui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import javax.swing.JApplet;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.zmpp.iff.FormChunk;
import org.zmpp.iff.WritableFormChunk;
import org.zmpp.instructions.DefaultInstructionDecoder;
import org.zmpp.io.FileInputStream;
import org.zmpp.io.IOSystem;
import org.zmpp.io.InputStream;
import org.zmpp.io.OutputStream;
import org.zmpp.io.TranscriptOutputStream;
import org.zmpp.vm.DefaultMachineConfig;
import org.zmpp.vm.InstructionDecoder;
import org.zmpp.vm.Machine;
import org.zmpp.vm.MachineConfig;
import org.zmpp.vm.MachineImpl;
import org.zmpp.vm.MemoryOutputStream;
import org.zmpp.vm.SaveGameDataStore;
import org.zmpp.vm.StatusLine;
import org.zmpp.vm.StoryFileHeader;

/**
 * This is the applet class for ZMPP.
 * 
 * @author Wei-ju Wu
 * @version 1.0
 */
public class ZmppApplet extends JApplet implements InputStream, StatusLine, SaveGameDataStore, IOSystem {

    private static final long serialVersionUID = 1L;

    private JLabel global1ObjectLabel;

    private JLabel statusLabel;

    private TextViewport viewport;

    private Machine machine;

    private LineEditorImpl lineEditor;

    private GameThread currentGame;

    public void init() {
        machine = openStoryFile();
        lineEditor = new LineEditorImpl(machine.getServices().getStoryFileHeader(), machine.getServices().getZsciiEncoding());
        viewport = new TextViewport(machine, lineEditor);
        viewport.setPreferredSize(new Dimension(640, 480));
        viewport.setMinimumSize(new Dimension(400, 300));
        viewport.setBackground(Color.WHITE);
        viewport.setForeground(Color.BLACK);
        if (machine.getServices().getStoryFileHeader().getVersion() <= 3) {
            JPanel statusPanel = new JPanel(new GridLayout(1, 2));
            JPanel status1Panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JPanel status2Panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            statusPanel.add(status1Panel);
            statusPanel.add(status2Panel);
            global1ObjectLabel = new JLabel(" ");
            statusLabel = new JLabel(" ");
            status1Panel.add(global1ObjectLabel);
            status2Panel.add(statusLabel);
            getContentPane().add(statusPanel, BorderLayout.NORTH);
        }
        getContentPane().add(viewport, BorderLayout.CENTER);
        addKeyListener(lineEditor);
        viewport.addKeyListener(lineEditor);
        viewport.addMouseListener(lineEditor);
        initMachine();
    }

    public void start() {
        currentGame = new GameThread(machine, viewport);
        currentGame.start();
    }

    private void initMachine() {
        FileInputStream fileIs = new FileInputStream(this, machine.getServices().getZsciiEncoding());
        machine.setInputStream(0, this);
        machine.setInputStream(1, fileIs);
        machine.setOutputStream(1, this.getOutputStream());
        machine.selectOutputStream(1, true);
        TranscriptOutputStream transcriptStream = new TranscriptOutputStream(this, machine.getServices().getZsciiEncoding());
        machine.setOutputStream(2, transcriptStream);
        machine.selectOutputStream(2, false);
        machine.setOutputStream(3, new MemoryOutputStream(machine));
        machine.selectOutputStream(3, false);
        machine.setStatusLine(this);
        machine.setScreen(viewport);
        machine.setSaveGameDataStore(this);
    }

    private Machine openStoryFile() {
        String source = getParameter("storyfile");
        java.io.InputStream inputstream = null;
        try {
            URL url = new URL(getDocumentBase(), source);
            inputstream = url.openStream();
            MachineConfig config = new DefaultMachineConfig(inputstream);
            StoryFileHeader fileheader = config.getStoryFileHeader();
            if (fileheader.getVersion() < 1 || fileheader.getVersion() == 6) {
                JOptionPane.showMessageDialog(null, "Story file version 6 is not supported.", "Story file read error", JOptionPane.ERROR_MESSAGE);
                stop();
            }
            Machine machine = new MachineImpl();
            InstructionDecoder decoder = new DefaultInstructionDecoder();
            machine.initialize(config, decoder);
            return machine;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (inputstream != null) inputstream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public void updateStatusScore(final String objectName, final int score, final int steps) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                global1ObjectLabel.setText(objectName);
                statusLabel.setText(score + "/" + steps);
            }
        });
    }

    public void updateStatusTime(final String objectName, final int hours, final int minutes) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                global1ObjectLabel.setText(objectName);
                statusLabel.setText(hours + ":" + minutes);
            }
        });
    }

    public OutputStream getOutputStream() {
        return viewport;
    }

    private WritableFormChunk savegame;

    public boolean saveFormChunk(WritableFormChunk formchunk) {
        savegame = formchunk;
        return true;
    }

    public FormChunk retrieveFormChunk() {
        return savegame;
    }

    public Writer getTranscriptWriter() {
        return new OutputStreamWriter(System.out);
    }

    public Reader getInputStreamReader() {
        File currentdir = new File(System.getProperty("user.dir"));
        JFileChooser fileChooser = new JFileChooser(currentdir);
        fileChooser.setDialogTitle("Set input stream file ...");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                return new FileReader(fileChooser.getSelectedFile());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public void close() {
    }

    public void cancelInput() {
        lineEditor.cancelInput();
    }

    public short getZsciiChar() {
        enterEditMode();
        short zsciiChar = lineEditor.nextZsciiChar();
        leaveEditMode();
        return zsciiChar;
    }

    private void enterEditMode() {
        if (!lineEditor.isInputMode()) {
            viewport.resetPagers();
            lineEditor.setInputMode(true);
        }
    }

    private void leaveEditMode() {
        lineEditor.setInputMode(false);
    }
}
