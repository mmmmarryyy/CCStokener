package ch.almana.mcclient.rcp.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import ch.almana.mcclient.rcp.application.Activator;

public class TsHandler {

    class RunPlayer implements IRunnableWithProgress {

        private static final long MIN_WATCH_TIME = 10 * 60 * 1000;

        private Process process;

        private String command;

        private long wachTime;

        public RunPlayer(String command) {
            this.command = command;
        }

        public Process getProcess() {
            return process;
        }

        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            try {
                long start = System.currentTimeMillis();
                process = exec(command);
                wachTime = System.currentTimeMillis() - start;
            } catch (IOException e) {
                throw new InvocationTargetException(e);
            }
            int exitVal = process.waitFor();
            Activator.getDefault().log("exit: " + exitVal, exitVal > 0 ? Status.WARNING : Status.OK);
        }

        public void setCommand(String command) {
            this.command = command;
        }

        private Process exec(String command) throws IOException {
            Process p = Runtime.getRuntime().exec(command);
            printStream(p.getInputStream());
            printStream(p.getErrorStream());
            return p;
        }

        public boolean getWatched() {
            return getProcess().exitValue() == 0 && wachTime > MIN_WATCH_TIME;
        }
    }

    ;

    private static final String MEDIACENTER_BASE_URL = "http://mediacenter.sf.tv/index.php?asx=1&vid=tagesschau";

    private static final String MEDIACEMTER_APPEND_URL = "-0";

    private static final String LINK_TAG_NAME = "REF";

    private static final String LINK_ATTR_NAME = "href";

    private static final String DATE_FORMAT = "yyyyMMdd";

    private static final String PLAYER_LNX = "/usr/bin/xine";

    private static final String PLAYER_MAC = "/Applications/VLC.app/Contents/MacOS/VLC";

    private static TsHandler instance;

    public static TsHandler getInstance() {
        if (instance == null) {
            instance = new TsHandler();
        }
        return instance;
    }

    public boolean executeTs(Date date) throws MalformedURLException, IOException, ParserConfigurationException, SAXException, InvocationTargetException, InterruptedException {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        if (cal.get(Calendar.HOUR_OF_DAY) < 20) {
            cal.add(Calendar.DATE, -1);
        }
        String streamUrl = getUrlFromDate(cal.getTime());
        System.out.println("Stream URL: " + streamUrl);
        return playUrl(streamUrl);
    }

    public String getUrlFromDate(Date date) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
        String dateString;
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        dateString = sdf.format(date);
        String streamUrl = getUrlFromDate(dateString);
        return streamUrl;
    }

    public String getUrlFromDate(String dateString) {
        String adress = MEDIACENTER_BASE_URL + dateString + dateString + MEDIACEMTER_APPEND_URL;
        String streamUrl = "";
        try {
            streamUrl = getStreamUrl(adress);
        } catch (Exception e) {
            Activator.getDefault().log("Could not get stream url", e, Status.WARNING);
        }
        return streamUrl;
    }

    public boolean playUrl(String streamUrl) throws InvocationTargetException, InterruptedException {
        String playerCmd = null;
        if ((new File(PLAYER_LNX)).exists()) {
            playerCmd = PLAYER_LNX;
        } else if ((new File(PLAYER_MAC)).exists()) {
            playerCmd = PLAYER_MAC;
        }
        if (playerCmd == null) {
            Activator.getDefault().log("No player found exiting", Status.ERROR);
            return false;
        }
        RunPlayer runPlayer = new RunPlayer(playerCmd + " " + streamUrl);
        Activator.getDefault().getWorkbench().getProgressService().run(true, false, runPlayer);
        return runPlayer.getWatched();
    }

    private void printStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        int i;
        while ((i = reader.read()) != -1) {
            System.out.write(i);
        }
    }

    private String getStreamUrl(String adress) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
        URL url = new URL(adress);
        InputStream is = url.openStream();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(is);
        Node linkTag = doc.getElementsByTagName(LINK_TAG_NAME).item(0);
        String StreamUrl = linkTag.getAttributes().getNamedItem(LINK_ATTR_NAME).getNodeValue();
        return StreamUrl;
    }
}
