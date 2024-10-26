package com.bitgate.util.scheduler;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import com.bitgate.util.constants.Constants;
import com.bitgate.util.debug.Debug;
import com.bitgate.util.services.Vend;
import com.bitgate.util.thread.ThreadController;
import com.bitgate.util.thread.ThreadManager;

/**
 * This is the URL Request class that performs a URL request.  It connects to a given URL, and pings that URL,
 * sending data to it as necessary.
 *
 * @author Kenji Hollis &lt;kenji@nuklees.com&gt;
 * @version $Id: //depot/nuklees/util/scheduler/Server.java#11 $
 */
class URLRequestThread extends ThreadController {

    private String urlSite;

    /**
     * The constructor.
     *
     * @param url The URL to request.
     */
    public URLRequestThread(String url) {
        this.urlSite = url;
    }

    /**
     * This function performs the URL request.  This is called from a thread.start function.
     */
    public void process() {
        URL urlObj = null;
        URLConnection conn = null;
        InputStream istream = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            urlObj = new URL(urlSite);
            conn = urlObj.openConnection();
            istream = conn.getInputStream();
        } catch (Exception e) {
            Debug.inform("Unable to retrieve URL '" + urlSite + "': " + e.getMessage());
            return;
        }
        byte dat[];
        dat = new byte[32768];
        while (true) {
            int bytesRead;
            try {
                bytesRead = istream.read(dat);
                bos.write(dat, 0, bytesRead);
            } catch (Exception e) {
                Debug.inform("Unable to read data from URL Stream: " + e.getMessage());
                break;
            }
            if (bytesRead <= 0) {
                break;
            }
        }
        String retstr = bos.toString();
        try {
            istream.close();
        } catch (Exception e) {
            Debug.inform("Unable to close input stream: " + e.getMessage());
        }
        Debug.inform("Request return='" + retstr + "'");
        setControllerInactive();
    }
}

/**
 * This is the Chronological Timer thread that performs the firing of URL requests on a periodical basis.
 *
 * @author &lt;kenji@nuklees.com&gt;
 * @version $Id: //depot/nuklees/util/scheduler/Server.java#11 $
 */
class CronThread extends ThreadController {

    private String[] cronTime;

    private String cronAction;

    /**
     * This is the constructor.
     *
     * @param cronTime An array of strings containing the chronological time entries for the requested cronAction.
     * @param cronAction The action to perform (URL to request) after the time period elapses.
     */
    public CronThread(String[] cronTime, String cronAction) {
        this.cronTime = cronTime;
        this.cronAction = cronAction;
    }

    /**
     * Returns a numeric representation of the day of the week, sunday being 0, saturday being 6.
     *
     * @param cron 3-letter day of the week.
     * @return int value of the day of the week.
     */
    private int getNumericDayOfWeek(String cron) {
        if (Character.isDigit(cron.charAt(0))) {
            if (cron.equals("sun")) {
                return 0;
            }
            if (cron.equals("mon")) {
                return 1;
            }
            if (cron.equals("tue")) {
                return 2;
            }
            if (cron.equals("wed")) {
                return 3;
            }
            if (cron.equals("thu")) {
                return 4;
            }
            if (cron.equals("fri")) {
                return 5;
            }
            if (cron.equals("sat")) {
                return 6;
            }
        }
        return Integer.parseInt(cron);
    }

    /**
     * This function runs the chronological time server.
     */
    public void process() {
        String minuteCron = cronTime[0];
        String hourCron = cronTime[1];
        String dayOfMonthCron = cronTime[2];
        String monthCron = cronTime[3];
        String dayOfWeekCron = cronTime[4];
        String minuteCrons[] = new String[61];
        String hourCrons[] = new String[25];
        String dayOfMonthCrons[] = new String[32];
        String monthCrons[] = new String[13];
        String dayOfWeekCrons[] = new String[8];
        int numMin = 0, numHour = 0, numDOM = 0, numMonth = 0, numDOY = 0;
        int hourCronInt;
        dayOfWeekCron = dayOfWeekCron.toLowerCase();
        if (minuteCron.indexOf(",") != -1) {
            String tempStrings[] = minuteCron.split(",");
            for (int i = 0; i < tempStrings.length; i++) {
                if (tempStrings[i].indexOf("-") != -1) {
                    int startVal = Integer.parseInt(tempStrings[i].substring(0, tempStrings[i].indexOf("-")));
                    int endVal = Integer.parseInt(tempStrings[i].substring(tempStrings[i].indexOf("-") + 1));
                    for (int x = startVal; x < endVal + 1; x++) {
                        minuteCrons[numMin++] = Integer.toString(x);
                    }
                } else {
                    minuteCrons[numMin++] = tempStrings[i];
                }
            }
            Debug.inform("Minute Entries: " + numMin);
        }
        try {
            int currentSecond = Calendar.getInstance().get(Calendar.SECOND);
            int currentMinute = Calendar.getInstance().get(Calendar.MINUTE);
            int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
            int currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
            boolean matches = true;
            if (!dayOfWeekCron.equals("*")) {
                int numericDayOfWeek = getNumericDayOfWeek(dayOfWeekCron);
                if (numericDayOfWeek != currentDayOfWeek) {
                    matches = false;
                }
            }
            if (matches) {
                if (!monthCron.equals("*")) {
                    int numericMonth = Integer.parseInt(monthCron);
                    if (numericMonth != currentMonth) {
                        matches = false;
                    }
                }
            }
            if (matches) {
                if (!dayOfMonthCron.equals("*")) {
                    int numericDayOfMonth = Integer.parseInt(dayOfMonthCron);
                    if (numericDayOfMonth != currentDay) {
                        matches = false;
                    }
                }
            }
            if (matches) {
                if (!hourCron.equals("*")) {
                    int numericHour = Integer.parseInt(hourCron);
                    if (numericHour != currentHour) {
                        matches = false;
                    }
                }
            }
            if (matches) {
                if (currentSecond == 0) {
                    if (!minuteCron.equals("*")) {
                        int numericMinute = Integer.parseInt(minuteCron);
                        if (numericMinute != currentMinute) {
                            matches = false;
                        }
                    }
                } else {
                    matches = false;
                }
            }
            if (matches) {
                URLRequestThread urlRequest = new URLRequestThread(cronAction);
                ThreadManager.getDefault().add(urlRequest);
            }
            Thread.sleep(1000);
        } catch (Exception e) {
            Debug.inform("Unable to sleep!");
        }
    }
}

/**
 * This is the Server class for the Cronological Server.
 *
 * @author Kenji Hollis &lt;kenji@nuklees.com&gt;
 * @version $Id: //depot/nuklees/util/scheduler/Server.java#11 $
 */
public class Server {

    /**
     * This function loads in the cronological table file.
     */
    public static void prepareCronEntries() {
        BufferedReader br = null;
        String line = null;
        try {
            br = new BufferedReader(new FileReader("./server.crontab"));
        } catch (Exception e) {
            return;
        }
        try {
            while ((line = br.readLine()) != null) {
                if (line == null) {
                    break;
                }
                if (line.startsWith("#") || line.equals("")) {
                    continue;
                }
                if (line.indexOf("\t") != -1) {
                    String cronEntries = null;
                    String cronAction = null;
                    String splitLine[] = line.split("\t", 2);
                    String cronTime[];
                    cronEntries = splitLine[0];
                    cronAction = splitLine[1];
                    cronTime = cronEntries.split(" ");
                    CronThread ct = new CronThread(cronTime, cronAction);
                    ThreadManager.getDefault().add(ct);
                }
            }
        } catch (Exception e) {
            Debug.inform("Cannot read line from file: " + e.getMessage());
        }
    }

    /**
     * This starts the server.
     */
    public static void main(String args[]) {
        Constants.getDefault().loadProperties("./server.properties");
        Debug.debug("Starting up.");
        if (!Vend.validate(args[0])) {
            Properties props = System.getProperties();
            props.put("mail.smtp.host", "mail.nuklees.com");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "10000");
            Session session = Session.getDefaultInstance(props, null);
            Message msg = new MimeMessage(session);
            try {
                msg.setFrom(new InternetAddress("software@nuklees.com"));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("kenji@nuklees.com, dell@nuklees.com", false));
                msg.setSubject("Activation Violation");
                msg.setHeader("X-Mailer", "Scheduler");
                msg.setText("Activation violation detected.");
                Transport.send(msg);
            } catch (Exception e) {
            }
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            System.exit(-1);
        }
        Debug.inform("Starting Web Services.");
        Vend wv = new Vend(Vend.WEB_SHOW_FILES);
        Thread t = new Thread(wv, "[Nuklees:Scheduler] Web Vender Thread - Endless");
        t.start();
        prepareCronEntries();
    }
}
