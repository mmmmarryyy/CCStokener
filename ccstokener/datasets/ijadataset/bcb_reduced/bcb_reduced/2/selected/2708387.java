package net.sf.rptserver;

import net.sf.rptserver.lang.*;
import net.sf.rptserver.util.*;
import net.sf.rptserver.rpc.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.xmlrpc.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.apache.xpath.*;
import net.sf.rptserver.report.*;
import org.quartz.*;
import org.quartz.impl.*;

public class Main extends ReportServerObject {

    private String basedir = null;

    private Scheduler sched = null;

    private Main() {
        super();
    }

    public static void main(String args[]) throws Exception {
        Main main = new Main();
        if (args.length > 0 && args[0] != null) {
            MainConfig.setDefaultFile(new File(args[0]));
        }
        main.run();
    }

    public void run() throws Exception {
        Properties buildprops = new Properties();
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            URL url = cl.getResource("build.properties");
            InputStream is = url.openStream();
            ;
            buildprops.load(is);
        } catch (Exception ex) {
            log.error("Problem getting build props", ex);
        }
        System.out.println("Report Server v" + buildprops.getProperty("version", "unknown") + "-" + buildprops.getProperty("build", "unknown"));
        validate();
        if (log.isInfoEnabled()) {
            log.info("Starting Report Server v" + buildprops.getProperty("version", "unknown") + "-" + buildprops.getProperty("build", "unknown"));
        }
        MainConfig config = MainConfig.newInstance();
        basedir = config.getBaseDirectory();
        if (log.isInfoEnabled()) {
            log.info("basedir = " + basedir);
        }
        SchedulerFactory schedFact = new StdSchedulerFactory();
        sched = schedFact.getScheduler();
        NodeList reports = config.getReports();
        for (int x = 0; x < reports.getLength(); x++) {
            try {
                if (log.isInfoEnabled()) {
                    log.info("Adding report at index " + x);
                }
                Node report = reports.item(x);
                runReport(report);
            } catch (Exception ex) {
                if (log.isErrorEnabled()) {
                    log.error("Can't add a report at report index " + x, ex);
                }
            }
        }
        addStatsJob();
        sched.start();
        WebServer webserver = new WebServer(8080);
        webserver.setParanoid(false);
        webserver.start();
    }

    private void runReport(Node n_report) throws Exception {
        Config config = Config.newInstance(n_report);
        ReportFactory rf = new ReportFactory();
        Report report = rf.getReport(n_report);
        if (log.isInfoEnabled()) {
            log.info("Adding report job " + report);
        }
        JobDataMap map = report.getJobDetail().getJobDataMap();
        map.put("basedir", basedir);
        map.put("report", report);
        ArrayList triggers = report.getTriggers();
        for (int x = 0; x < triggers.size(); x++) {
            Trigger trigger = (Trigger) triggers.get(x);
            if (x <= 0) {
                sched.scheduleJob(report.getJobDetail(), trigger);
            } else {
                sched.scheduleJob(trigger);
            }
        }
    }

    private void addStatsJob() throws Exception {
        JobDetail jobDetail = new JobDetail("_rptserver_serverstats", "_rptserver", ServerStatsJob.class);
        Trigger trigger = new CronTrigger("_rptserver_serverstats_trigger", "_rptserver", "0 * * * * ?");
        sched.scheduleJob(jobDetail, trigger);
    }

    /** This will validate that everything is set up properly */
    private void validate() throws Exception {
        MainConfig config = MainConfig.newInstance();
        String basedir = config.getBaseDirectory();
        if (basedir == null || basedir.length() <= 0) {
            throw new ConfigException("missing or empty <basedir/>");
        }
        File f_basedir = new File(basedir);
        if (!f_basedir.exists()) {
            throw new ConfigException("missing basedir: " + basedir);
        }
        if (!f_basedir.isDirectory()) {
            throw new ConfigException("basedir not a directory: " + basedir);
        }
        File runtime = new File(basedir + "/rtlib/rptserver_rt.jar");
        if (!runtime.exists()) {
            throw new ConfigException("can't locate runtime library at: " + runtime);
        }
    }
}
