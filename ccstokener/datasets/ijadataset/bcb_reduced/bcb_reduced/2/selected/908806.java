package gov.sns.apps.diagnostics.blmview;

import gov.sns.application.*;
import java.net.URL;
import gov.sns.ca.ChannelFactory;
import gov.sns.tools.scan.SecondEdition.MonitoredPV;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import gov.sns.tools.xml.XmlDataAdaptor;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import gov.sns.apps.diagnostics.blmview.mvc.ModelEvent;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

/**
 * TemplateDocument
 *
 * @author  somebody
 */
class MPSstudyDocument extends XalDocument {

    public static ChannelFactory chf = ChannelFactory.defaultFactory();

    private BLMsController bController;

    private BLMsTableModel btModel;

    private PlotPanel pView;

    private WindowPanel wp;

    private final Timer globalTimer = new Timer();

    private Timer localTimer;

    private static MonitoredPV timingPV;

    private static boolean timerInitialized = false;

    private String windowMode;

    private boolean externalTimingAvailable;

    /**
	 * Method getBController
	 *
	 */
    public BLMsController getBController() {
        return bController;
    }

    /**
	 * Method getPlotView
	 *
	 * @return   a  Component
	 */
    public PlotPanel getPlotView() {
        return pView;
    }

    /**
	 * Returns BtModel
	 *
	 * @return    a  BLMsTableModel
	 */
    public BLMsTableModel getBtModel() {
        return btModel;
    }

    static {
        chf.init();
    }

    private String dataRootName = "MPS_Study";

    private BLMsModel bModel;

    BLMsModel getBLMmodel() {
        return bModel;
    }

    /** Create a new empty document */
    public MPSstudyDocument(String winmode) {
        this(null, winmode);
    }

    /**
     * Create a new document loaded from the URL file
     * @param url The URL of the file to load into the new document.
     */
    public MPSstudyDocument(java.net.URL url, String winmode) {
        windowMode = winmode;
        initializeMVC();
        if (url == null) {
            url = this.getClass().getResource("resources/linac.diagml");
        }
        setSource(url);
        readDocument(url);
        if (timingPV == null) {
            String s = getTimingPVName();
            if (s == null) externalTimingAvailable = false; else {
                externalTimingAvailable = true;
                timingPV = MonitoredPV.getMonitoredPV("Timing");
                timingPV.setChannelName(s);
            }
        }
    }

    private static double REFRESHRATE = 1.0;

    public WindowPanel getWP() {
        return wp;
    }

    /**
	 * Method initializeMVC
	 *
	 */
    private void initializeMVC() {
        bModel = new BLMsModel();
        bController = new BLMsController(bModel);
        wp = new WindowPanel(bModel, bController);
        initializeGlobalTimer();
        startTiming();
    }

    /**
     * Make a main window by instantiating the my custom window.
     */
    @Override
    public void makeMainWindow() {
        mainWindow = new BLMSumWindow(this);
    }

    /**
     * Save the document to the specified URL.
     * @param url The URL to which the document should be saved.
     */
    @Override
    public void saveDocumentAs(URL url) {
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(url.getPath())));
            out.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n");
            out.write("<MPS_Study title=\"MPS calculator " + url.getFile() + "\">\n");
            bModel.write(out);
            out.write("</MPS_Study>");
            out.flush();
            out.close();
        } catch (Exception e) {
            System.out.println("Some shit happened to the stream!");
        }
    }

    public void readDocument(URL url) {
        stopTiming();
        try {
            String xmlData = "";
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = "";
            boolean cont = true;
            while (cont) {
                line = in.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '%') {
                    xmlData = xmlData + line + System.getProperty("line.separator");
                }
                if (line.length() > 1 && line.charAt(0) == '%' && line.charAt(1) == '=') {
                    cont = false;
                }
            }
            XmlDataAdaptor readAdp = null;
            readAdp = XmlDataAdaptor.adaptorForString(xmlData, false);
            if (readAdp != null) {
                XmlDataAdaptor mpsfileData_Adaptor = readAdp.childAdaptor(dataRootName);
                if (mpsfileData_Adaptor != null) {
                    setTitle(mpsfileData_Adaptor.stringValue("title"));
                    java.util.Iterator<XmlDataAdaptor> plotIt = mpsfileData_Adaptor.childAdaptorIterator("Plot");
                    while (plotIt.hasNext()) {
                        XmlDataAdaptor pvDA = plotIt.next();
                        String name = pvDA.stringValue("name");
                        String xMin = pvDA.stringValue("xmin");
                        String xMax = pvDA.stringValue("xmax");
                        String step = pvDA.stringValue("step");
                        System.out.println(name + " " + xMax + " " + xMin + " " + step);
                        bModel.setPlotAxes(name, xMin, xMax, step);
                    }
                    java.util.Iterator<XmlDataAdaptor> timingIt = mpsfileData_Adaptor.childAdaptorIterator("TimingPV");
                    while (timingIt.hasNext()) {
                        XmlDataAdaptor pvDA = timingIt.next();
                        String name = pvDA.stringValue("name");
                        bModel.setTimingPVName(name);
                    }
                    java.util.Iterator<XmlDataAdaptor> trigIt = mpsfileData_Adaptor.childAdaptorIterator("Trigger");
                    while (trigIt.hasNext()) {
                        XmlDataAdaptor pvDA = trigIt.next();
                        String name = pvDA.stringValue("name");
                        String type = pvDA.stringValue("type");
                        bModel.addTrigger(name, type);
                    }
                    java.util.Iterator<XmlDataAdaptor> blmIt = mpsfileData_Adaptor.childAdaptorIterator("BLMdevice");
                    while (blmIt.hasNext()) {
                        XmlDataAdaptor pvDA = blmIt.next();
                        String name = pvDA.stringValue("name");
                        String section = pvDA.stringValue("section");
                        String mpschan = pvDA.stringValue("mpschan");
                        String devType = pvDA.stringValue("devicetype");
                        String location = pvDA.stringValue("locationz");
                        double locz = 0;
                        try {
                            locz = Double.parseDouble(location);
                        } catch (NumberFormatException e) {
                            locz = 0.0;
                        }
                        if (devType == null) bModel.addBLM(new IonizationChamber(name, section, mpschan, locz)); else if (devType.equals("ND")) bModel.addBLM(new NeutronDetector(name, section, mpschan, locz)); else if (devType.equals("IC")) bModel.addBLM(new IonizationChamber(name, section, mpschan, locz));
                    }
                }
            }
            in.close();
        } catch (IOException exception) {
            System.out.println("Fatal error. Something wrong with input file. Stop.");
        }
        startTiming();
    }

    @Override
    protected void willClose() {
    }

    private void cleanUp() {
        System.out.println("Cleaning up...");
        stopTiming();
        globalTimer.cancel();
        bModel.cleanUp();
        if (Application.getApp().getDocuments().isEmpty()) {
        }
    }

    private void runOnEvent(int id) {
        if (mainWindow == null) return;
        bModel.calculateAll(((AbstractBLMWindow) mainWindow).getFirstVisible(), ((AbstractBLMWindow) mainWindow).getLastVisible());
        ModelEvent me = new ModelEvent(this, id, "", "Timing");
        bModel.notifyChanged(me);
    }

    private void dropEvent(String s) {
        System.out.println(s);
    }

    static final int LOCALTIMING = 0;

    static final int EXTERNALTIMING = 1;

    private int timingMode = LOCALTIMING;

    private String getTimingPVName() {
        return bModel.getTimingPVname();
    }

    @Override
    protected void customizeCommands(Commander commander) {
        AbstractAction timingAction, dtlAction, cclAction, sclAction, linacAction;
        timingAction = new AbstractAction("check-timing") {

            /**
			 * Invoked when an action occurs.
			 */
            public void actionPerformed(ActionEvent e) {
                changeTiming();
            }
        };
        timingAction.setEnabled(true);
        commander.registerAction(timingAction);
        ArrayList<String> allRegions = new ArrayList<String>();
        allRegions.add("dtl");
        allRegions.add("ccl");
        allRegions.add("scl");
        allRegions.add("linac");
        allRegions.add("hli");
        allRegions.add("ring");
        for (int i = 0; i < allRegions.size(); i++) {
            final String name = allRegions.get(i);
            AbstractAction newAction = new AbstractAction(name + "-menu") {

                public void actionPerformed(ActionEvent e) {
                    Application.getApp().openDocument(this.getClass().getResource("resources/" + name + ".diagml"));
                }
            };
            newAction.setEnabled(true);
            commander.registerAction(newAction);
        }
    }

    private void initializeGlobalTimer() {
        globalTimer.schedule(new globalTimerTask(), 0, (int) (50.0));
    }

    ActionListener pvListener;

    private void startTiming() {
        if (timingMode == LOCALTIMING) {
            localTimer = new Timer();
            localTimer.schedule(new LocalTimerTask(), 0, (int) (REFRESHRATE * 1000.0));
        } else if (timingMode == EXTERNALTIMING) {
            pvListener = new ActionListener() {

                private int counter = 0;

                public void actionPerformed(ActionEvent e) {
                    setNewTrigger(true);
                    counter++;
                    if (counter % 20 == 0) System.out.println(getTitle() + " PVTrigger" + counter);
                }
            };
            timingPV.addValueListener(pvListener);
        }
    }

    private void changeTiming() {
        stopTiming();
        if (timingMode == LOCALTIMING && externalTimingAvailable) {
            ((JButton) (getMainWindow().getToolBar().getComponent(1))).setText("LocalTiming");
            timingMode = EXTERNALTIMING;
            System.out.println("Switched to external timing " + getTimingPVName());
        } else if (timingMode == EXTERNALTIMING) {
            ((JButton) (getMainWindow().getToolBar().getComponent(1))).setText("ExternalTiming");
            timingMode = LOCALTIMING;
            System.out.println("Switched to local timing ");
        }
        startTiming();
    }

    private void stopTiming() {
        if (timingMode == LOCALTIMING) {
            localTimer.cancel();
        } else if (timingMode == EXTERNALTIMING) {
            timingPV.removeValueListener(pvListener);
        }
    }

    private Boolean busyCalculating = false;

    private void setBusyCalculating(boolean p0) {
        synchronized (busyCalculating) {
            busyCalculating = p0;
        }
    }

    private boolean getBusyCalculating() {
        synchronized (busyCalculating) {
            return busyCalculating;
        }
    }

    private Boolean newExternalTrigger = false;

    private void setNewTrigger(boolean p0) {
        synchronized (newExternalTrigger) {
            newExternalTrigger = p0;
        }
    }

    private boolean getNewTrigger() {
        synchronized (newExternalTrigger) {
            return newExternalTrigger;
        }
    }

    class LocalTimerTask extends TimerTask {

        int counter = 0;

        @Override
        public void run() {
            setNewTrigger(true);
            counter++;
            if (counter % 20 == 0) System.out.println(getTitle() + " LocalTrigger " + counter);
        }
    }

    class globalTimerTask extends TimerTask {

        private int counter = 0;

        @Override
        public void run() {
            if (getNewTrigger()) {
                counter++;
                if (getBusyCalculating()) {
                    dropEvent("Trigger " + counter + " dropped");
                    return;
                }
                setBusyCalculating(true);
                runOnEvent(0);
                setBusyCalculating(false);
                setNewTrigger(false);
                if (counter % 20 == 0) System.out.println(getTitle() + " trigger " + counter);
            } else {
                return;
            }
        }
    }
}
