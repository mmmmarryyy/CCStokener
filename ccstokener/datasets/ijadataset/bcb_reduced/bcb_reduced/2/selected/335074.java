package net.sf.jaer.jaerappletviewer;

import net.sf.jaer.aemonitor.AEPacketRaw;
import net.sf.jaer.chip.AEChip;
import ch.unizh.ini.jaer.chip.retina.Tmpdiff128;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.eventio.AEInputStream;
import net.sf.jaer.eventio.AEUnicastInput;
import net.sf.jaer.eventio.AEUnicastSettings;
import net.sf.jaer.graphics.AEChipRenderer;
import net.sf.jaer.graphics.ChipCanvas;
import net.sf.jaer.util.EngineeringFormat;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Random;
import java.util.logging.*;
import javax.swing.border.TitledBorder;

/**
 * Applet that plays events in a web browser from
 * a network and file input streams.
 * <p>
 * Note that applets have limited permissions and certain permissions
 * must be granted on the server for this applet to be run.
 * Either the applet jar must be signed and have permission granted to run the code 
 * by the browser, or the java.policy file in java/lib/security can be edited on
 * the server to have the following permissions granted for jAER.jar
 * 
 * 
<pre>
grant codeBase "http://localhost:8080/jaer/dist/jAER.jar" {
permission java.io.FilePermission "<<ALL FILES>>", "read";
permission java.lang.RuntimePermission "preferences";
permission java.util.PropertyPermission "user.dir", "read";
permission java.awt.AWTPermission "setAppletStub";
permission java.net.SocketPermission "www.ini.uzh.ch:80", "connect";
permission java.net.SocketPermission "www.ini.uzh.ch:80", "resolve";
 * };

</pre>
 * 
 * 
 * 
 * @author  tobi delbruck/mert yentur
 */
public class JAERAppletViewer extends javax.swing.JApplet {

    AEChip liveChip, recordedChip;

    ChipCanvas liveCanvas, recordedCanvas;

    Logger log = Logger.getLogger("JAERAppletViewer");

    EngineeringFormat fmt = new EngineeringFormat();

    volatile String fileSizeString = "";

    AEInputStream aeRecordedInputStream;

    AEUnicastInput aeLiveInputStream;

    AEInputStream his;

    private int packetTime = 40000;

    volatile boolean stopflag = false;

    private long frameDelayMs = 40;

    private int unicastInputPort = AEUnicastSettings.ARC_TDS_STREAM_PORT + 1;

    private String dataFileListURL = "http://www.ini.uzh.ch/~tobi/propaganda/retina/dataFileURLList.txt";

    Random random = new Random();

    @Override
    public String getAppletInfo() {
        return "jAERAppletViewer";
    }

    private void setCanvasDefaults(ChipCanvas canvas) {
        canvas.setOpenGLEnabled(true);
    }

    /** Initializes the applet JAERAppletViewer */
    public synchronized void init() {
        log.info("applet init");
        liveChip = new Tmpdiff128();
        liveChip.setName("Live DVS");
        liveCanvas = liveChip.getCanvas();
        liveChip.getRenderer().setColorScale(2);
        liveChip.getRenderer().setColorMode(AEChipRenderer.ColorMode.GrayLevel);
        recordedChip = new Tmpdiff128();
        recordedChip.setName("Recorded DVS");
        recordedCanvas = recordedChip.getCanvas();
        recordedChip.getRenderer().setColorScale(2);
        recordedChip.getRenderer().setColorMode(AEChipRenderer.ColorMode.GrayLevel);
        initComponents();
        livePanel.add(liveCanvas.getCanvas(), BorderLayout.CENTER);
        recordedPanel.add(recordedCanvas.getCanvas(), BorderLayout.CENTER);
        setCanvasDefaults(liveCanvas);
        setCanvasDefaults(recordedCanvas);
    }

    @Override
    public synchronized void start() {
        super.start();
        log.info("applet starting with dataFileListURL=" + dataFileListURL + " unicastInputPort=" + unicastInputPort);
        openNextStreamFile();
        openNetworkInputStream();
        repaint();
    }

    @Override
    public synchronized void stop() {
        super.stop();
        log.info("applet stop, setting stopflag=true and closing input stream");
        stopflag = true;
        try {
            if (aeRecordedInputStream != null) {
                aeRecordedInputStream.close();
                aeRecordedInputStream = null;
            }
            if (aeLiveInputStream != null) {
                aeLiveInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    int lastFileNumber = 0;

    BufferedReader dataFileListReader = null;

    private boolean printedMissingDataFileListWarningAlready = false;

    private String getNextFileName() {
        String fileName = null;
        try {
            if (dataFileListReader == null) {
                dataFileListReader = new BufferedReader(new InputStreamReader(new URL(dataFileListURL).openStream()));
                log.info("opened dataFileListReader from " + dataFileListURL);
                dataFileListReader.mark(3000);
            }
            int n = random.nextInt(20);
            for (int i = 0; i < n; i++) {
                try {
                    fileName = dataFileListReader.readLine();
                    log.info("read next data file line " + fileName);
                    if (fileName == null) throw new EOFException("null filename");
                } catch (EOFException eof) {
                    dataFileListReader.reset();
                }
            }
        } catch (IOException e2) {
            if (!printedMissingDataFileListWarningAlready) {
                log.warning("while opening list of data file URLs " + dataFileListURL + " : " + e2.toString());
                printedMissingDataFileListWarningAlready = true;
            }
        }
        return fileName;
    }

    private void openNextStreamFile() {
        log.info("opening next data file from URL stream");
        String fileName = getNextFileName();
        if (fileName == null) {
            return;
        }
        try {
            log.info("opening data file url input stream from " + fileName);
            URL url = new URL(fileName);
            InputStream is = new BufferedInputStream(url.openStream());
            aeRecordedInputStream = new AEInputStream(is);
            stopflag = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openNetworkInputStream() {
        try {
            if (aeLiveInputStream != null) {
                aeLiveInputStream.close();
            }
            aeLiveInputStream = new AEUnicastInput();
            aeLiveInputStream.setPort(unicastInputPort);
            aeLiveInputStream.set4ByteAddrTimestampEnabled(AEUnicastSettings.ARC_TDS_4_BYTE_ADDR_AND_TIMESTAMPS);
            aeLiveInputStream.setAddressFirstEnabled(AEUnicastSettings.ARC_TDS_ADDRESS_BYTES_FIRST_ENABLED);
            aeLiveInputStream.setSequenceNumberEnabled(AEUnicastSettings.ARC_TDS_SEQUENCE_NUMBERS_ENABLED);
            aeLiveInputStream.setSwapBytesEnabled(AEUnicastSettings.ARC_TDS_SWAPBYTES_ENABLED);
            aeLiveInputStream.setTimestampMultiplier(AEUnicastSettings.ARC_TDS_TIMESTAMP_MULTIPLIER);
            aeLiveInputStream.open();
            log.info("opened AEUnicastInput " + aeLiveInputStream);
            stopflag = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    EventPacket emptyPacket = new EventPacket();

    public synchronized void paint(Graphics g) {
        super.paint(g);
        if (stopflag) {
            log.info("stop set, not painting again or calling repaint");
            return;
        }
        if (aeLiveInputStream != null) {
            AEPacketRaw aeRaw = aeLiveInputStream.readPacket();
            if (aeRaw != null) {
                EventPacket ae = liveChip.getEventExtractor().extractPacket(aeRaw);
                if (ae != null) {
                    liveChip.getRenderer().render(ae);
                    liveChip.getCanvas().paintFrame();
                    ((TitledBorder) livePanel.getBorder()).setTitle("Live: " + aeRaw.getNumEvents() + " events");
                } else {
                    ((TitledBorder) livePanel.getBorder()).setTitle("Live: " + "null packet");
                }
            }
        }
        if (aeRecordedInputStream != null) {
            try {
                AEPacketRaw aeRaw = aeRecordedInputStream.readPacketByTime(packetTime);
                if (aeRaw != null) {
                    EventPacket ae = liveChip.getEventExtractor().extractPacket(aeRaw);
                    if (ae != null) {
                        recordedChip.getRenderer().render(ae);
                        recordedChip.getCanvas().paintFrame();
                        ((TitledBorder) recordedPanel.getBorder()).setTitle("Recorded: " + aeRaw.getNumEvents() + " events");
                    }
                }
            } catch (EOFException eof) {
                log.info("EOF on file " + aeRecordedInputStream);
                openNextStreamFile();
            } catch (IOException e) {
                log.warning(e.toString());
            }
        } else {
            recordedChip.getRenderer().render(emptyPacket);
            recordedChip.getCanvas().paintFrame();
        }
        try {
            Thread.currentThread().sleep(frameDelayMs);
        } catch (InterruptedException e) {
        }
        repaint();
    }

    /** This method is called from within the init() method to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jTextField2 = new javax.swing.JTextField();
        livePanel = new javax.swing.JPanel();
        recordedPanel = new javax.swing.JPanel();
        jTextField2.setText("jTextField2");
        setBackground(new java.awt.Color(0, 0, 0));
        setName("jAERAppletViewer");
        setStub(null);
        addComponentListener(new java.awt.event.ComponentAdapter() {

            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridLayout(2, 1));
        livePanel.setBackground(new java.awt.Color(0, 0, 0));
        livePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Live - the INI kitchen", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 11), new java.awt.Color(255, 255, 255)));
        livePanel.setLayout(new java.awt.BorderLayout());
        getContentPane().add(livePanel);
        recordedPanel.setBackground(new java.awt.Color(0, 0, 0));
        recordedPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Recorded DVS data from various sources", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 11), new java.awt.Color(255, 255, 255)));
        recordedPanel.setLayout(new java.awt.BorderLayout());
        getContentPane().add(recordedPanel);
    }

    private void formComponentResized(java.awt.event.ComponentEvent evt) {
    }

    private javax.swing.JTextField jTextField2;

    private javax.swing.JPanel livePanel;

    private javax.swing.JPanel recordedPanel;
}
