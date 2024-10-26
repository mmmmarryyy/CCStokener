package jmxcollector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class JMXCollector implements JMXCollectorMBean {

    public static final int NAGIOS_OK = 0;

    public static final String NAGIOS_OK_MSG = "JMXCollector OK - ";

    public static final int NAGIOS_WARNING = 1;

    public static final String NAGIOS_WARNING_MSG = "JMXCollector WARNING - ";

    public static final int NAGIOS_CRITICAL = 2;

    public static final String NAGIOS_CRITICAL_MSG = "JMXCollector CRITICAL - ";

    public static final int NAGIOS_UNKNOWN = 3;

    public static final String NAGIOS_UNKNOWN_MSG = "JMXCollector UNKNOWN - ";

    private static String configxml;

    private static int QueryThreadPoolSize;

    private static ScheduledThreadPoolExecutor QueryThreadPool;

    private static int SessionThreadPoolSize;

    private static ScheduledThreadPoolExecutor SessionThreadPool;

    private static AtomicBoolean stopping = new AtomicBoolean(false);

    private static Vector<JMXSession> OwnJMXSessions;

    private LinkedBlockingQueue<Object> JMXSessionResultsQueue;

    private LinkedList<Object> Results;

    private static JMXCollector JMXCollectorInstance;

    private static Vector<ScheduledFuture<?>> OwnJMXSessionFutures;

    private static boolean runonce;

    private static LinkedList<String> NagiosResults;

    private static Date NextWrite;

    private static Logger logger = Logger.getLogger(JMXCollector.class.getName());

    public void setQueryThreadPoolSize(int size) {
        QueryThreadPoolSize = size;
    }

    public void setSessionThreadPoolSize(int size) {
        SessionThreadPoolSize = size;
    }

    private JMXCollector() {
        JMXSessionResultsQueue = new LinkedBlockingQueue<Object>();
        Results = new LinkedList<Object>();
        OwnJMXSessions = new Vector<JMXSession>();
        OwnJMXSessionFutures = new Vector<ScheduledFuture<?>>();
        NagiosResults = new LinkedList<String>();
    }

    public static JMXCollector getInstance() {
        if (JMXCollectorInstance == null) {
            JMXCollectorInstance = new JMXCollector();
        }
        return JMXCollectorInstance;
    }

    public void AddSessionData(JMXSession jmxsess) {
        OwnJMXSessions.add(jmxsess);
    }

    public ScheduledThreadPoolExecutor getQueryThreadPool() {
        return QueryThreadPool;
    }

    public LinkedBlockingQueue<Object> getJMXSessionResultsQueue() {
        return JMXSessionResultsQueue;
    }

    private void InitializeThreadPools() {
        this.SessionThreadPool = new ScheduledThreadPoolExecutor(SessionThreadPoolSize);
        this.SessionThreadPool.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.QueryThreadPool = new ScheduledThreadPoolExecutor(QueryThreadPoolSize);
        this.QueryThreadPool.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    public static void startCollect() {
        JMXCollector collector = JMXCollector.getInstance();
        logger.info("Collector: SessionThreadPoolSize=" + collector.SessionThreadPoolSize + ", QueryThreadPoolSize=" + collector.QueryThreadPoolSize + ".");
        for (JMXSession js : OwnJMXSessions) {
            js.execute(collector.QueryThreadPool);
            OwnJMXSessionFutures.add(collector.SessionThreadPool.scheduleWithFixedDelay(js, 0, 2, TimeUnit.SECONDS));
        }
        NextWrite = new Date();
        logger.info("Collector: started");
        while (!stopping.get()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                stopping.set(true);
            }
            collector.collectResults();
            collector.writeResults();
        }
        logger.info("Writes to Resultfile stopped.");
        System.exit(0);
    }

    public static void runonce() {
        JMXCollector collector = JMXCollector.getInstance();
        System.out.println("Collector: Run all query once");
        for (JMXSession js : OwnJMXSessions) {
            js.runonce();
        }
        System.out.println("Nagios results will be:");
        collector.collectResults();
        collector.writeResults();
    }

    public static void checkNagiosResultFile() {
        Settings set = Settings.getInstance();
        File f = new File(set.nagiosResultFile);
        if (f.exists()) {
            if (f.canWrite()) {
            } else {
                System.out.println("Nagios external command file is not writable.");
                System.out.println("Make sure that the user running JMXCollector has write access to this file.");
                System.exit(1);
            }
        } else {
            System.out.println("Nagios external command file does not exists. Maybe Nagios is not running.");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        JMXCollector collector = JMXCollector.getInstance();
        logger.setLevel(Level.ALL);
        Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);
        Options opt = collector.checkParameters(args);
        Configuration cfg = Configuration.getInstance();
        try {
            cfg.Load(configxml);
        } catch (JMXCollectorException e) {
            System.out.println("Error while processing configuration: " + e.getMessage());
            System.exit(1);
        }
        collector.InitializeThreadPools();
        collector.InitializeMBeanServer();
        if (runonce) {
            runonce();
            System.out.println("Collector: stopped");
            System.exit(0);
        }
        checkNagiosResultFile();
        logger.finest("Adding shutdownhook");
        Runtime.getRuntime().addShutdownHook(new ShutdownThread(collector));
        startCollect();
    }

    private void InitializeMBeanServer() {
        logger.finest("Initializing MBean server");
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        Object mbean = this;
        try {
            mbs.registerMBean(mbean, new ObjectName("jmxcollector:type=RuntimeStats"));
        } catch (MalformedObjectNameException e) {
            logger.warning("Cannot create MBean object. " + e.getMessage());
        } catch (InstanceAlreadyExistsException e) {
            logger.warning("Cannot register MBean. " + e.getMessage());
        } catch (MBeanRegistrationException e) {
            logger.warning("Cannot register MBean. " + e.getMessage());
        } catch (NotCompliantMBeanException e) {
            logger.warning("Cannot register MBean. " + e.getMessage());
        }
    }

    public static String ComposeNagiosPassiveResult(JMXQueryResult res) {
        String preRes;
        String midRes;
        String postRes;
        preRes = "[" + String.valueOf(res.ResultTime.getTime() / 1000L) + "] PROCESS_SERVICE_CHECK_RESULT;" + res.qry.NagiosResultHostname + ";" + res.qry.NagiosResultServicename + ";";
        int nagiosState;
        if (res.qry.NagiosResultCritical > res.qry.NagiosResultWarning) {
            if (res.Value >= res.qry.NagiosResultCritical) {
                nagiosState = NAGIOS_CRITICAL;
            } else if (res.Value >= res.qry.NagiosResultWarning) {
                nagiosState = NAGIOS_WARNING;
            } else {
                nagiosState = NAGIOS_OK;
            }
        } else {
            if (res.Value <= res.qry.NagiosResultCritical) {
                nagiosState = NAGIOS_CRITICAL;
            } else if (res.Value <= res.qry.NagiosResultWarning) {
                nagiosState = NAGIOS_WARNING;
            } else {
                nagiosState = NAGIOS_OK;
            }
        }
        if (nagiosState == NAGIOS_OK) {
            midRes = String.valueOf(NAGIOS_OK) + ";" + NAGIOS_OK_MSG;
        } else if (nagiosState == NAGIOS_WARNING) {
            midRes = String.valueOf(NAGIOS_WARNING) + ";" + NAGIOS_WARNING_MSG;
        } else {
            midRes = String.valueOf(NAGIOS_CRITICAL) + ";" + NAGIOS_CRITICAL_MSG;
        }
        postRes = res.qry.objname + " " + res.qry.attribute + " " + res.qry.key + "=" + String.valueOf(res.Value) + "|'" + res.qry.getNagiosResultPerfDataLabel() + "'=" + String.valueOf(res.Value) + res.qry.getNagiosResultPerfDataUOM() + ";" + res.qry.NagiosResultWarning + ";" + res.qry.NagiosResultCritical + ";;\n";
        return preRes + midRes + postRes;
    }

    public static String ComposeNagiosPassiveResult(JMXQueryExceptionResult res) {
        String preRes;
        String midRes;
        String postRes;
        preRes = "[" + String.valueOf(res.ResultTime.getTime() / 1000L) + "] PROCESS_SERVICE_CHECK_RESULT;" + res.qry.NagiosResultHostname + ";" + res.qry.NagiosResultServicename + ";";
        if (res.E.getClass().getName() == "jmxcollector.JMXCollectorExcpetion") {
            midRes = String.valueOf(NAGIOS_UNKNOWN) + ";" + NAGIOS_UNKNOWN_MSG + res.E.getMessage();
        } else midRes = String.valueOf(NAGIOS_UNKNOWN) + ";" + NAGIOS_UNKNOWN_MSG + res.E.toString();
        postRes = "\n";
        return preRes + midRes + postRes;
    }

    public void collectResults() {
        JMXSessionResultsQueue.drainTo(Results);
        for (Object o : Results) {
            if (o.getClass().getName() == "jmxcollector.JMXQueryResult") {
                NagiosResults.add(ComposeNagiosPassiveResult((JMXQueryResult) o));
            } else if (o.getClass().getName() == "jmxcollector.JMXQueryExceptionResult") {
                NagiosResults.add(ComposeNagiosPassiveResult((JMXQueryExceptionResult) o));
            }
        }
        Results.clear();
    }

    public void writeResults() {
        if (!runonce) {
            if (new Date().getTime() > NextWrite.getTime()) {
                Settings set = Settings.getInstance();
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.SECOND, set.writeInterval);
                NextWrite = cal.getTime();
                cal = null;
                if (NagiosResults.size() > 0) {
                    logger.finest("Writing results to Nagios external command file");
                    File resfile = new File(set.nagiosResultFile);
                    if (resfile.canWrite()) {
                        try {
                            FileWriter fw = new FileWriter(resfile, true);
                            Iterator<String> i = NagiosResults.iterator();
                            while (i.hasNext()) {
                                fw.append(i.next());
                            }
                            fw.flush();
                            fw.close();
                        } catch (IOException e) {
                            logger.warning("Write Error: " + e.getMessage());
                        }
                        for (String s : NagiosResults) {
                            System.out.print("Collector: NAGIOS RESULT will be: " + s);
                        }
                        NagiosResults.clear();
                    } else {
                        logger.warning("Cannot write to Nagios External Command File. " + NagiosResults.size() + " result(s) are lost.");
                        NagiosResults.clear();
                    }
                }
            }
        } else {
            for (String s : NagiosResults) {
                System.out.print("Collector: NAGIOS RESULT will be: " + s);
            }
            NagiosResults.clear();
        }
    }

    public void stop() {
        stopping.set(true);
        logger.info("Collector: Stopping...");
        for (JMXSession js : OwnJMXSessions) {
            js.stop();
        }
        if (!runonce) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e1) {
            }
            logger.fine("Collector: Cancelling session threads");
            for (ScheduledFuture<?> schFuture : OwnJMXSessionFutures) {
                schFuture.cancel(false);
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            logger.fine("Collector: Killing session threads");
            for (ScheduledFuture<?> schFuture : OwnJMXSessionFutures) {
                schFuture.cancel(true);
            }
            logger.fine("Shutdown session thread pool.");
            SessionThreadPool.shutdownNow();
            logger.fine("Shutdown query thread pool.");
            QueryThreadPool.shutdownNow();
        }
        logger.info("Collector: Stopped");
    }

    private static void showHelp(Options opt) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("JMXCollector", opt, true);
        System.exit(1);
    }

    private static void showVersion() {
        System.out.println("JMXColletor version 1.1");
        System.exit(1);
    }

    private static void outputResource(URL url) {
        try {
            Reader r = new InputStreamReader(url.openStream());
            StringBuilder str = new StringBuilder();
            char[] buffer = new char[1024];
            for (int len = r.read(buffer); len != -1; len = r.read(buffer)) {
                str.append(buffer, 0, len);
            }
            System.out.println(str.toString());
        } catch (IOException e) {
            logger.warning("Error: " + e.getMessage());
        }
    }

    private void ValidateConfiguration(String filename) throws SAXParseException, SAXException, IOException, Exception {
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        URL schemaLocation = getClass().getResource("Configuration.xsd");
        Schema schema = factory.newSchema(schemaLocation);
        Validator validator = schema.newValidator();
        Source source = new StreamSource(filename);
        validator.validate(source);
    }

    private Options checkParameters(String[] args) {
        Options opt = new Options();
        opt.addOption("h", "help", false, "print this help message");
        opt.addOption(OptionBuilder.withLongOpt("config-file").withDescription("filename of the main configuration xml file").hasArgs(1).withArgName("configxml").create("C"));
        opt.addOption("v", "version", false, "print version information and exit");
        opt.addOption(OptionBuilder.withLongOpt("get-configfile-schema").withDescription("get the configuration XSD").create());
        opt.addOption(OptionBuilder.withLongOpt("test").withDescription("validate configuration").create());
        opt.addOption(OptionBuilder.withLongOpt("run-once").withDescription("run all configured JMX query once and print the results (ideal for testing)").create("1"));
        opt.addOption(OptionBuilder.withLongOpt("debug").withDescription("print debugging information to standard output").create());
        CommandLineParser parser = new PosixParser();
        CommandLine cmdline = null;
        try {
            cmdline = parser.parse(opt, args);
        } catch (ParseException e) {
            System.out.println("Wrong arguments");
            showHelp(opt);
        }
        if (cmdline.hasOption("help")) {
            showHelp(opt);
        }
        if (cmdline.hasOption("version")) {
            showVersion();
        }
        if (cmdline.hasOption("get-configfile-schema")) {
            outputResource(getClass().getResource("Configuration.xsd"));
            System.exit(0);
        }
        if (!cmdline.hasOption("config-file")) {
            System.out.println("Wrong arguments: configuration file not set");
            showHelp(opt);
        } else {
            configxml = cmdline.getOptionValue("config-file");
        }
        if (cmdline.hasOption("run-once")) {
            runonce = true;
        } else {
            runonce = false;
        }
        if (cmdline.hasOption("debug")) {
            logger.setLevel(Level.ALL);
            Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);
        } else {
            logger.setLevel(Level.INFO);
            Logger.getLogger("").getHandlers()[0].setLevel(Level.INFO);
        }
        System.out.println("Validating main configuration file: " + configxml + " ...");
        try {
            this.ValidateConfiguration(configxml);
        } catch (SAXParseException e) {
            System.out.println("ERROR on line: " + e.getLineNumber() + " at column: " + e.getColumnNumber());
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (SAXException e) {
            System.out.println("Parsing error: " + e.getMessage());
            System.exit(2);
        } catch (IOException e) {
            System.out.println("IO Error: " + e.getMessage());
            System.exit(3);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(4);
        }
        System.out.println("Configuration file is valid.");
        if (cmdline.hasOption("test")) {
            System.exit(0);
        }
        return opt;
    }

    /** MBean functions ***/
    @Override
    public int getActiveQueryThreadCount() {
        return QueryThreadPool.getActiveCount();
    }

    @Override
    public int getActiveSessionThreadCount() {
        return SessionThreadPool.getActiveCount();
    }

    @Override
    public int getSessionThreadPoolSize() {
        return SessionThreadPool.getCorePoolSize();
    }

    @Override
    public int getQueryThreadPoolSize() {
        return QueryThreadPool.getCorePoolSize();
    }

    @Override
    public int getLargestQueryThreadCount() {
        return QueryThreadPool.getLargestPoolSize();
    }

    @Override
    public int getLargestSessionThreadCount() {
        return SessionThreadPool.getLargestPoolSize();
    }

    @Override
    public long getTotalStartedQuery() {
        return QueryThreadPool.getTaskCount();
    }

    @Override
    public int getCollectorInternalQueueSize() {
        return JMXSessionResultsQueue.size();
    }

    @Override
    public synchronized ArrayList<String> validateConfiguration() {
        ArrayList<String> res = new ArrayList<String>();
        res.add("Validating main configuration file: " + configxml + " ... \n");
        try {
            this.ValidateConfiguration(configxml);
            res.add("Configuration file is valid.");
        } catch (SAXParseException e) {
            res.add("ERROR on line: " + e.getLineNumber() + " at column: " + e.getColumnNumber());
            res.add(e.getMessage());
        } catch (SAXException e) {
            res.add("Parsing error: " + e.getMessage());
        } catch (IOException e) {
            res.add("IO Error: " + e.getMessage());
        } catch (Exception e) {
            res.add("Error: " + e.getMessage());
        }
        return res;
    }

    @Override
    public synchronized ArrayList<String> reloadConfiguration() {
        ArrayList<String> res = new ArrayList<String>();
        res.add("Not yet implemented...");
        return res;
    }
}
