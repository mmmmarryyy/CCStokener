import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class StatusChecker extends TimerTask {

    private static final String REQUEST_PARAM = "/?STATUS";

    private final String requestURL;

    private final String username;

    private final String password;

    private final String smtpServer;

    private final String senderAddress;

    private final String recipientsAddress;

    private final String subject;

    private final String text;

    private final String command;

    private final int timeout;

    private final int exceptionInterval;

    private int counter;

    StatusChecker(TreeMap<String, String> settings) {
        requestURL = "http://" + settings.get("ip") + ":" + settings.get("port") + REQUEST_PARAM;
        this.senderAddress = settings.get("senderAddress");
        this.recipientsAddress = settings.get("recipientAddress");
        this.smtpServer = settings.get("smtpHost");
        this.username = settings.get("username");
        this.password = settings.get("password");
        this.timeout = Integer.parseInt(settings.get("timeout"));
        this.exceptionInterval = Integer.parseInt(settings.get("exceptionInterval"));
        this.subject = settings.get("subject");
        this.text = settings.get("text");
        this.command = settings.get("command");
    }

    public static void main(String[] args) {
        TreeMap<String, String> settings = PropertiesFileReader.readServerConfigFile();
        int interval = Integer.parseInt(settings.get("interval"));
        StatusChecker sc = new StatusChecker(settings);
        Timer t = new Timer();
        t.schedule(sc, interval, interval);
    }

    public void run() {
        System.out.println("Checking masterserver...");
        sendRequest();
    }

    private void sendRequest() {
        try {
            URL url = new URL(requestURL);
            URLConnection con = url.openConnection();
            con.setReadTimeout(timeout);
            checkStatus(getResponse(con));
        } catch (IOException e) {
            System.err.println("I/O Error while open connection");
            e.printStackTrace();
        }
    }

    /**
     * This method checks the availability of the server
     * 
     * if the server is not available via HTTP (timeout), failover solution is
     * invoked. The notification will be sent to the admin via email
     * 
     * @param con
     *            URLConnection
     */
    private String getResponse(URLConnection con) {
        BufferedReader br = null;
        String statusLine = "";
        int shellExitStatus = 0;
        String line = null;
        try {
            br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            while ((line = br.readLine()) != null) {
                statusLine = line;
            }
            br.close();
        } catch (IOException e) {
            try {
                if (isLinuxSystem()) {
                    ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
                    Process shell = builder.start();
                    br = new BufferedReader(new InputStreamReader(shell.getInputStream()));
                    shellExitStatus = shell.waitFor();
                    while ((line = br.readLine()) != null) {
                        System.out.println(line);
                    }
                    System.out.println();
                    br.close();
                } else {
                    if (isWindowsSystem()) {
                        Process shell = Runtime.getRuntime().exec("cmd /c" + "\"" + command + "\"");
                        br = new BufferedReader(new InputStreamReader(shell.getInputStream()));
                        shellExitStatus = shell.waitFor();
                        while ((line = br.readLine()) != null) {
                            System.out.println(line + "\n");
                        }
                        System.out.println();
                        br.close();
                    } else {
                        System.out.println("StatusChecker: OS not known.");
                    }
                }
                if (!smtpServer.equals("")) {
                    sendEMail(null, null);
                }
                System.exit(0);
            } catch (Exception e1) {
                System.err.println("StatusChecker: Error while call " + "extern Program.");
                e1.printStackTrace();
            }
        }
        return statusLine;
    }

    /**
     * This method analyzes response status of the server (quantity of
     * exceptions) If while the specified period server throws exceptions
     * (default 15 min) the notification will be sent to the admin via email,
     * but failover will be not invoked
     * 
     * @param statusLine
     *            server status response
     */
    private void checkStatus(String statusLine) {
        String temp = null;
        if (counter == exceptionInterval) {
            counter = 0;
            if (statusLine.length() > 0) {
                temp = statusLine.substring(statusLine.indexOf(':') + 2, statusLine.indexOf('O') - 1);
                int currentSqlError = Integer.parseInt(temp);
                temp = statusLine.substring(statusLine.lastIndexOf(':') + 2);
                int currentError = Integer.parseInt(temp);
                if (currentError < 1 && currentSqlError < 1) {
                    System.out.println("Masterserver is running correct.");
                } else {
                    if (!smtpServer.equals("")) sendEMail(subject, text);
                }
            }
        } else {
            counter++;
        }
        System.out.println(statusLine);
    }

    private void sendEMail(String subject, String text) {
        String subj;
        String txt;
        if (subject == null && text == null) {
            subj = "WebtrackingServer: Error on master WebtrackingServer";
            txt = "No connection to WebtrackingServer." + " Switched to slave WebtrackingServer." + " Please check masterserver and restart them." + " After that you must restart the StatusChecker because " + "it shutdown after slave server is take over the response.";
        } else {
            subj = subject;
            txt = text;
        }
        new SendMailHandler().sendMail(smtpServer, username, password, senderAddress, recipientsAddress, subj, txt);
    }

    private boolean isWindowsSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.indexOf("windows") >= 0;
    }

    private boolean isLinuxSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.indexOf("linux") >= 0;
    }
}
