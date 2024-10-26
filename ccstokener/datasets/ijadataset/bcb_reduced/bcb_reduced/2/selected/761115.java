package org.jsresources.apps.ripper;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import org.tritonus.share.sampled.*;

public class GrabEncodePanel extends JPanel implements Constants, TrackList.Listener {

    static {
        try {
            URL.setURLStreamHandlerFactory(new org.tritonus.sampled.cdda.CddaURLStreamHandlerFactory());
        } catch (Throwable t) {
            System.err.println("Please make sure to have tritonus_share.jar in your CLASSPATH !");
            t.printStackTrace();
        }
    }

    private static int BUFFER_SIZE = 20 * 2352;

    private static boolean DEBUG = false;

    private TrackListPanel trackListPanel;

    private ProgressPanel progressPanel;

    private ButtonPanel buttonPanel;

    private JLabel statusLabel;

    private TrackList trackList;

    private AsynchronousAudioInputStream aais;

    private SimpleAudioPlayer simp;

    private long currTrackLengthInMs;

    private MP3Encoder mp3encoder;

    private AudioFileFormat.Type fileType = org.tritonus.share.sampled.AudioFileTypes.getType("MP3", "mp3");

    private AudioFormat.Encoding encoding = Encodings.getEncoding("MPEG1L3");

    public GrabEncodePanel() {
        trackList = new TrackList();
        setLayout(new BorderLayout());
        add("North", new LogoPanel());
        trackListPanel = new TrackListPanel();
        add("Center", trackListPanel);
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new StripeLayout(0, 5, 0, 5, 5));
        southPanel.add(new JSeparator());
        progressPanel = new ProgressPanel();
        southPanel.add(progressPanel);
        southPanel.add(new JSeparator());
        buttonPanel = new ButtonPanel();
        southPanel.add(buttonPanel);
        southPanel.add(new JSeparator());
        statusLabel = new JLabel("");
        statusLabel.setOpaque(true);
        southPanel.add(statusLabel);
        add("South", southPanel);
        trackList.setListener(this);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                refresh();
            }
        });
    }

    protected void beginWait() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    protected void endWait() {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    protected void paintImmediately(JComponent comp) {
        comp.update(comp.getGraphics());
    }

    protected void status() {
        status(null, false);
    }

    protected synchronized void status(String text) {
        status(text, false);
    }

    protected synchronized void status(String text, boolean immediately) {
        if (text == null || text.length() == 0) {
            text = "Ready";
        }
        statusLabel.setText(text);
        if (immediately) {
            paintImmediately(statusLabel);
        }
    }

    protected void handleException(Throwable t) {
        String s = t.getMessage();
        if (s == null || s == "") {
            s = t.toString();
        }
        JOptionPane.showMessageDialog(null, s);
        status(s);
    }

    public void close() {
        stop();
    }

    public void refresh() {
        if (trackList != null) {
            try {
                beginWait();
                status("Refreshing track list...", true);
                try {
                    trackList.setInputStream(getCDDADir());
                } finally {
                    endWait();
                    status();
                }
            } catch (Exception e) {
                handleException(e);
            }
        }
    }

    public void trackListChanged(TrackList trackList) {
        if (trackList.getCount() > 0) {
            buttonPanel.setState(buttonPanel.STOPPED);
        } else {
            buttonPanel.setState(buttonPanel.NOTHING);
        }
        trackListPanel.refresh();
    }

    private static class LogoPanel extends JLabel {

        LogoPanel() {
            super(APP_NAME);
            setHorizontalAlignment(SwingConstants.CENTER);
        }
    }

    private InputStream getCDDADir() throws Exception {
        URL url = new URL("cdda:/dev/cdrom");
        return url.openStream();
    }

    private String getSelectedFilename() throws Exception {
        int i = trackListPanel.getSelection(0);
        if (i >= 0) {
            return trackListPanel.getFilename(i);
        }
        throw new Exception("Please select a track!");
    }

    private AudioInputStream getCDDAStream() throws Exception {
        InputStream is = null;
        int i = trackListPanel.getSelection(0);
        if (i >= 0) {
            URL url = new URL("cdda://dev/cdrom#" + (i + 1));
            is = url.openStream();
            currTrackLengthInMs = trackList.getDurationInMs(i);
        } else {
            throw new Exception("Please select a track!");
        }
        return (AudioInputStream) is;
    }

    private synchronized void play() {
        try {
            if (aais != null || simp != null) {
                stop();
            }
            beginWait();
            try {
                status("Getting CDDA AudioInputStream...", true);
                AudioInputStream cddaStream = getCDDAStream();
                progressPanel.startProgress();
                status("Constructing AsynchronousAudioInputStream...", true);
                aais = new AsynchronousAudioInputStream(cddaStream, BUFFER_SIZE);
                status("Constructing SimpleAudioPlayer...", true);
                simp = new SimpleAudioPlayer(aais);
                status("Starting SimpleAudioPlayer...", true);
                simp.start();
                buttonPanel.setState(buttonPanel.PLAYING);
            } finally {
                endWait();
                status();
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    private synchronized void stop() {
        beginWait();
        try {
            if (aais != null) {
                try {
                    status("Closing AsynchronousAudioInputStream...", true);
                    aais.close();
                } catch (Exception e) {
                    handleException(e);
                }
                aais = null;
            }
            if (simp != null) {
                status("Closing SimpleAudioPlayer...", true);
                simp.stopPlaying();
                simp = null;
            }
            progressPanel.stopProgress();
            buttonPanel.setState(buttonPanel.STOPPED);
        } finally {
            status();
            endWait();
        }
    }

    private synchronized void encode() {
        try {
            beginWait();
            try {
                status("Getting CDDA AudioInputStream...", true);
                AudioInputStream cddaStream = getCDDAStream();
                progressPanel.startProgress();
                status("Constructing AsynchronousAudioInputStream...", true);
                aais = new AsynchronousAudioInputStream(cddaStream, BUFFER_SIZE);
                status("Getting converted Stream...", true);
                AudioInputStream mp3Stream = AudioSystem.getAudioInputStream(encoding, aais);
                status("Creating MP3 encoder...", true);
                mp3encoder = new MP3Encoder(mp3Stream, getSelectedFilename());
                status("Starting MP3 encoder...", true);
                mp3encoder.start();
            } finally {
                endWait();
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    private class TrackListPanel extends JPanel {

        private final int COLUMNS = 3;

        private final String[] COLUMN_TITLES = { "#", "Title", "Duration" };

        private JTable table;

        public TrackListPanel() {
            super();
            this.setLayout(new BorderLayout());
            TableModel dataModel = new AbstractTableModel() {

                public int getColumnCount() {
                    return COLUMNS;
                }

                public int getRowCount() {
                    return getTrackCount();
                }

                public Object getValueAt(int r, int c) {
                    if (c == 0) {
                        return String.valueOf(r + 1);
                    } else if (c == 1) {
                        return getTrackName(r);
                    } else {
                        return getDurationStr(r);
                    }
                }

                public String getColumnName(int c) {
                    return COLUMN_TITLES[c];
                }

                public Class getColumnClass(int c) {
                    return getValueAt(0, c).getClass();
                }

                public boolean isCellEditable(int r, int c) {
                    return false;
                }

                public void setValueAt(Object obj, int r, int c) {
                }
            };
            table = new JTable(dataModel);
            table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            table.setPreferredScrollableViewportSize(new Dimension(250, 250));
            table.setCellSelectionEnabled(false);
            table.setColumnSelectionAllowed(false);
            table.setRowSelectionAllowed(true);
            table.setRowSelectionAllowed(true);
            JScrollPane sp = new JScrollPane(table);
            sp.setHorizontalScrollBarPolicy(sp.HORIZONTAL_SCROLLBAR_NEVER);
            this.add(sp);
        }

        public int getSelection(int startIndex) {
            for (int i = startIndex; i < getTrackCount(); i++) {
                if (table.isRowSelected(i)) {
                    return i;
                }
            }
            return -1;
        }

        public void refresh() {
            table.repaint();
        }

        public int getTrackCount() {
            if (trackList == null) {
                return 0;
            }
            return trackList.getCount();
        }

        public String getTrackName(int track) {
            if (trackList == null) {
                return "<none>";
            }
            return Util.formatNumber(trackList.getTrack(track).getID(), 2);
        }

        public String getFilename(int track) {
            return Util.formatNumber(trackList.getTrack(track).getID(), 2) + "." + fileType.getExtension();
        }

        public String getDurationStr(int track) {
            if (trackList == null) {
                return "";
            }
            return Util.formatMinSec(trackList.getDurationInMs(track));
        }
    }

    private class ProgressPanel extends JPanel implements Runnable {

        private JProgressBar progressBar;

        private JLabel percent;

        private JLabel time;

        private boolean running;

        public ProgressPanel() {
            super();
            this.setLayout(new GridLayout(1, 2));
            progressBar = new JProgressBar(0, 100);
            progressBar.setValue(0);
            this.add(progressBar);
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(1, 2));
            percent = new JLabel();
            panel.add(percent);
            time = new JLabel();
            panel.add(time);
            this.add(panel);
        }

        public synchronized void startProgress() {
            Thread thread = new Thread(this);
            running = true;
            thread.start();
        }

        public synchronized void stopProgress() {
            running = false;
        }

        public void run() {
            while (running) {
                if (aais != null) {
                    progressBar.setValue((int) (aais.getPositionInMs() * 100 / currTrackLengthInMs));
                    percent.setText("Buffer: " + aais.getBufferPercent() + "%");
                    time.setText(Util.formatMinSec(aais.getPositionInMs()));
                } else {
                    progressBar.setValue(0);
                    percent.setText("<none>");
                    time.setText("<none>");
                }
                synchronized (this) {
                    try {
                        this.wait(50);
                    } catch (InterruptedException ie) {
                    }
                }
            }
        }
    }

    private class ButtonPanel extends JPanel implements ActionListener {

        public final int NOTHING = 0;

        public final int STOPPED = 1;

        public final int PLAYING = 2;

        public final int ENCODING = 3;

        private JButton bPlay;

        private JButton bEncode;

        private JButton bStop;

        private JButton bRefresh;

        private JButton addButton(String caption) {
            JButton result = new JButton(caption);
            result.addActionListener(this);
            this.add(result);
            return result;
        }

        public ButtonPanel() {
            super();
            this.setLayout(new GridLayout(1, 4));
            bPlay = addButton("Play");
            bEncode = addButton("Encode");
            bStop = addButton("Stop");
            bRefresh = addButton("Refresh");
        }

        public void setState(int state) {
            bPlay.setEnabled(state == STOPPED);
            bEncode.setEnabled(state == STOPPED);
            bStop.setEnabled(state != STOPPED && state != NOTHING);
            bRefresh.setEnabled(state == STOPPED);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == bPlay) {
                play();
            } else if (e.getSource() == bStop) {
                stop();
            } else if (e.getSource() == bRefresh) {
                refresh();
            } else if (e.getSource() == bEncode) {
                encode();
            }
        }
    }

    private class MP3Encoder extends Thread {

        private AudioInputStream mp3Stream;

        private String filename;

        public MP3Encoder(AudioInputStream mp3Stream, String filename) {
            this.mp3Stream = mp3Stream;
            this.filename = filename;
        }

        public void run() {
            status("Encoding to MP3 in progress...", true);
            buttonPanel.setState(buttonPanel.ENCODING);
            try {
                AudioSystem.write(mp3Stream, fileType, new File(filename));
            } catch (Exception e) {
                handleException(e);
            }
            GrabEncodePanel.this.stop();
        }
    }
}
