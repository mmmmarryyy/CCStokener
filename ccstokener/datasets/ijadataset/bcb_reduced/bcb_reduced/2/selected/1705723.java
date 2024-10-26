package com.keppardo.dyndns;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;
import com.keppardo.dyndns.auth.EncryptUserData;
import com.keppardo.dyndns.auth.User;
import com.keppardo.dyndns.reader.FreeDNSXmlReader;

public class StartChecker implements Job {

    static String GET_IP = "http://ip.dnsexit.com";

    static String URL_DNS = "http://freedns.afraid.org/api/?action=getdyndns&style=xml&sha=";

    static boolean sendEmail = false;

    static MailSender mailSender = null;

    private static String pwdMail;

    private static String usrMail;

    private static String from;

    private static String to;

    private static String smtpPort;

    /**
	 * @param args
	 * @throws Exception
	 */
    public static void main(String[] args) throws Exception {
        try {
            CommandLineParser parser = new GnuParser();
            CommandLine commandLine = parser.parse(createOptions(), args);
            if (commandLine.hasOption("help")) {
                printHelp();
                return;
            }
            if (commandLine.hasOption("mail")) {
                sendEmail = true;
                if (!commandLine.hasOption("to") || !commandLine.hasOption("from") || !commandLine.hasOption("mailUser") || !commandLine.hasOption("mailPassword")) {
                    printHelp();
                    return;
                }
                to = commandLine.getOptionValue("to");
                from = commandLine.getOptionValue("from");
                usrMail = commandLine.getOptionValue("mailUser");
                pwdMail = commandLine.getOptionValue("mailPassword");
                smtpServer = commandLine.getOptionValue("smtpServer");
                smtpPort = commandLine.getOptionValue("smtpPort");
            }
            if (!commandLine.hasOption("user")) {
                System.out.println("Specificare nome utente");
                printHelp();
                return;
            }
            boolean checkAllHosts = false;
            if (!commandLine.hasOption("hostname")) {
                checkAllHosts = true;
            }
            String[] hosts = null;
            if (!checkAllHosts) {
                String hostname = commandLine.getOptionValue("hostname");
                hosts = new String[] { hostname };
                if (hostname.indexOf(",") != -1) {
                    hosts = StringUtils.split(hostname, ",");
                }
            }
            String userName = commandLine.getOptionValue("user");
            File f = new File(userName + ".ser");
            User user;
            if (f.exists() && !commandLine.hasOption("password")) {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
                user = (User) in.readObject();
                in.close();
            } else {
                if (!commandLine.hasOption("password")) {
                    System.out.println("Password is missing");
                    return;
                }
                if (!commandLine.hasOption("hostname")) {
                    System.out.println("Host is missing");
                    return;
                }
                user = new User();
                String password = commandLine.getOptionValue("password");
                user.setPwd(password);
                user.setUsername(userName);
                ObjectSerializer.serialize(user, userName + ".ser");
            }
            for (int i = 0; i < hosts.length; i++) {
                String h = hosts[i];
                if (user.getDomainsData().get(h) == null) {
                    user.addIp(h, "");
                }
            }
            SchedulerFactory factory = new StdSchedulerFactory();
            Scheduler scheduler = factory.getScheduler();
            scheduler.start();
            JobDetail jobDetail = new JobDetail("Dynamic DNS update", null, StartChecker.class);
            jobDetail.getJobDataMap().put("params", args);
            jobDetail.getJobDataMap().put("user", user);
            jobDetail.getJobDataMap().put("hosts", hosts);
            Trigger trigger = TriggerUtils.makeSecondlyTrigger(90);
            trigger.setStartTime(new Date());
            trigger.setName("myTrigger");
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    private static void printHelp() {
        HelpFormatter help = new HelpFormatter();
        help.printHelp("java -jar dynDns.jar", createOptions());
    }

    private static Options createOptions() {
        Option help = new Option("help", "print this message");
        Option user = OptionBuilder.withArgName("user").hasArg().withDescription("User name").create("user");
        Option password = OptionBuilder.withArgName("password").hasArg().withDescription("The password").create("password");
        Option host = OptionBuilder.withArgName("host1[,host2,...]").hasArg().withDescription("The host or virtualhost name").create("hostname");
        Option to = OptionBuilder.withArgName("email address").hasArg().withDescription("Indirizzo email a cui spedire l'ip").create("to");
        Option from = OptionBuilder.withArgName("email").hasArg().withDescription("Mittente").create("from");
        Option mailUser = OptionBuilder.withArgName("nome utente account email").hasArg().withDescription("User name per spedire la mail").create("mailUser");
        Option pwdMail = OptionBuilder.withArgName("password account email").hasArg().withDescription("User name per spedire la mail").create("mailPassword");
        Option smtpServer = OptionBuilder.withArgName("Server smtp").hasArg().withDescription("Smtp server").create("smtpServer");
        Option smtpPort = OptionBuilder.withArgName("Port Server smtp").hasArg().withDescription("Smtp server port (Default 587)").create("smtpPort");
        Option optMail = new Option("mail", "spedisci il cambio di ip via email");
        Options options = new Options();
        options.addOption(help);
        options.addOption(user);
        options.addOption(password);
        options.addOption(host);
        options.addOption(from);
        options.addOption(to);
        options.addOption(mailUser);
        options.addOption(pwdMail);
        options.addOption(optMail);
        options.addOption(smtpServer);
        options.addOption(smtpPort);
        return options;
    }

    private static String getActualIp() throws IOException {
        URL url = new URL(GET_IP);
        URLConnection con = url.openConnection();
        InputStream in = con.getInputStream();
        byte[] b = new byte[512];
        int ch = 0;
        StringBuffer sb = new StringBuffer();
        while ((ch = in.read(b)) >= 0) {
            sb.append(new String(b, 0, ch));
        }
        return sb.toString();
    }

    private static String smtpServer;

    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        try {
            JobDetail jobDetail = ctx.getJobDetail();
            JobDataMap dataMap = jobDetail.getJobDataMap();
            User user = (User) dataMap.get("user");
            String[] hosts = (String[]) dataMap.get("hosts");
            EncryptUserData enc = new EncryptUserData(user);
            String hash = enc.getAccountHash();
            String ipNew = getActualIp().trim();
            System.out.println("IP is " + ipNew);
            Hashtable<String, String> domains = user.getDomainsData();
            URL url = new URL(URL_DNS + hash);
            System.out.println(URL_DNS + hash);
            URLConnection urlConnection = url.openConnection();
            InputStream inStream = urlConnection.getInputStream();
            List items = FreeDNSXmlReader.domainListItems(IOUtils.toByteArray(inStream));
            boolean ipIsChanged = false;
            boolean messageSent = false;
            for (int i = 0; i < hosts.length; i++) {
                String h = hosts[i];
                String oldIpHost = domains.get(h).trim();
                System.out.println("For host " + h + " old ip is " + oldIpHost);
                if (!ipNew.equals(oldIpHost)) {
                    ipIsChanged = true;
                }
                for (Iterator<Element> iter = items.iterator(); iter.hasNext(); ) {
                    Element itemEl = iter.next();
                    String domain = itemEl.elementText("host");
                    if (domain.equals(h)) {
                        String ipMemo = itemEl.elementText("address");
                        if (ipIsChanged || !ipMemo.equals(ipNew)) {
                            if (!messageSent) {
                                MailSender mailSender = new MailSender(to, from, usrMail, pwdMail);
                                mailSender.setSmtpServer(smtpServer);
                                messageSent = true;
                                if (smtpPort != null) {
                                    mailSender.setSmtpPort(smtpPort);
                                }
                                mailSender.sendMail(ipNew);
                            }
                            String updateString = itemEl.elementText("url");
                            updateDNS(domain, updateString);
                            user.addIp(domain, ipNew);
                        }
                    }
                }
            }
            ObjectSerializer.serialize(user, user.getUsername() + ".ser");
            System.out.println("##########################################################");
            System.out.println("                          INFO                           ");
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG, Locale.ITALY);
            String date = dateFormat.format(ctx.getNextFireTime());
            System.out.println(" Next fire time: " + date);
            System.out.println("##########################################################");
            System.out.println();
            System.out.println();
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateDNS(String hostName, String updateString) throws IOException {
        System.out.println("Update ip for host " + hostName + ".....");
        System.out.println(updateString);
        URL url = new URL(updateString);
        URLConnection urlConnection = url.openConnection();
        InputStream inStream = urlConnection.getInputStream();
        System.out.println("***************************************");
        System.out.println(IOUtils.toString(inStream));
        System.out.println("***************************************");
    }
}
