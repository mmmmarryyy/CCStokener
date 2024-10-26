package spidr.export;

import java.io.*;
import java.net.*;
import org.apache.axis.client.*;
import org.apache.commons.cli.*;

/**
 * The class is used to export data from database using
 * command line call of the SPIDR web service
 */
public class SpidrClient {

    /**
   * Makes SPIDR web service call
   *
   * @param siteUrl - web service URL\\\\\\\\\\\\\\\\\\\\\\\\\
   * @param login - web service login
   * @param password - web service password
   * @param table - data table (e.g. KPAP)
   * @param station - station code (e.g. BC840)
   * @param element - parameter, may be null for all (e.g. kp)
   * @param dayFrom - starting day ID formatted as YYYYMMDD
   * @param dayTo - ending day ID formatted as YYYYMMDD
   * @param format - export format, may be "ascii" (default), "IIWG", "WDC", or
   *   "raw" for serialized spidr.datamode.DataSequenceSet
   * @param sampling - data time step in min, if not default (minimal for the
   *   data)
   * @param filePath file path to save data on the local workstation, if not in
   *   the system workd directory
   * @return fileName � file name of the data file recieved from web service
   * @throws Exception
   */
    public static String webService(String siteUrl, String login, String password, String table, String station, String element, String dayFrom, String dayTo, String format, String sampling, String filePath) throws Exception {
        Service service = new Service();
        Call call = (Call) service.createCall();
        if (login != null) {
            call.setUsername(login);
            if (password != null) {
                call.setPassword(password);
            }
            System.err.println("Info: authentication user=" + login + " passwd=" + password + " at " + siteUrl);
        }
        call.setTargetEndpointAddress(new URL(siteUrl));
        call.setOperationName("getData");
        call.setTimeout(new Integer(60 * 1000 * 30));
        String url = (String) call.invoke(new Object[] { table, station, element, dayFrom, dayTo, format, sampling });
        String fileName = null;
        if (url == null) {
            throw new Exception("Error: result URL is null");
        } else {
            System.err.println("Info: result URL is " + url);
            URL dataurl = new URL(url);
            String filePart = dataurl.getFile();
            if (filePart == null) {
                throw new Exception("Error: file part in the data URL is null");
            } else {
                fileName = filePart.substring(filePart.lastIndexOf("/") < 0 ? 0 : filePart.lastIndexOf("/") + 1);
                if (filePath != null) {
                    fileName = filePath + fileName;
                }
                System.err.println("Info: local file name is " + fileName);
            }
            FileOutputStream file = new FileOutputStream(fileName);
            if (file == null) {
                throw new Exception("Error: file output stream is null");
            }
            InputStream strm = dataurl.openStream();
            if (strm == null) {
                throw new Exception("Error: data input stream is null");
            } else {
                int c;
                while ((c = strm.read()) != -1) {
                    file.write(c);
                }
            }
        }
        return fileName;
    }

    private static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        PrintWriter out = new PrintWriter(System.err);
        formatter.printHelp("SpidrClient -l <url> -t <table> [-s <station>] [-e <element>] -d <YYYYMMDD>", options);
    }

    /**
   * Basically command line parser and wrapper for the web service call
   * Usage: java spidr.export.SpidrClient
   * @param args command line arguments, see usage()
   */
    public static void main(String[] args) {
        String link = null;
        String user = null;
        String passwd = null;
        String day = null;
        String dayFrom = null;
        String dayTo = null;
        int dayId = 0;
        String format = null;
        String table = null;
        String station = null;
        String element = null;
        String sampling = null;
        Option helpOpt = OptionBuilder.withArgName("help").isRequired(false).withLongOpt("help").withDescription("print this message").create("h");
        Option linkOpt = OptionBuilder.withArgName("link").hasArg().isRequired(true).withLongOpt("link").withDescription("web service url").create("l");
        Option userOpt = OptionBuilder.withArgName("user").hasArg().isRequired(false).withLongOpt("user").withDescription("web service user name").create("u");
        Option passwdOpt = OptionBuilder.withArgName("passwd").hasArg().isRequired(false).withLongOpt("password").withDescription("web service user password").create("p");
        Option tableOpt = OptionBuilder.withArgName("table").hasArg().isRequired(true).withLongOpt("table").withDescription("database table").create("t");
        Option elementOpt = OptionBuilder.withArgName("element").hasArg().isRequired(false).withLongOpt("element").withDescription("table element (parameter)").create("e");
        Option stationOpt = OptionBuilder.withArgName("station").hasArg().isRequired(false).withLongOpt("station").withDescription("station code").create("s");
        Option dayOpt = OptionBuilder.withArgName("day").hasArg().isRequired(false).withLongOpt("day").withDescription("day ID in the form YYYYMMDD").create("d");
        Option dayFromOpt = OptionBuilder.withArgName("dayfrom").hasArg().isRequired(false).withLongOpt("dayfrom").withDescription("day-from ID in the form YYYYMMDD").create("d1");
        Option dayToOpt = OptionBuilder.withArgName("dayto").hasArg().isRequired(false).withLongOpt("dayto").withDescription("day-to ID in the form YYYYMMDD").create("d2");
        Option formatOpt = OptionBuilder.withArgName("format").hasArg().isRequired(false).withLongOpt("format").withDescription("data file format, may be 'ascii', 'IIWG', 'WDC'").create("f");
        Option timestepOpt = OptionBuilder.withArgName("timestep").hasArg().isRequired(false).withLongOpt("timestep").withDescription("time step").create("ts");
        Options options = new Options();
        options.addOption(helpOpt);
        options.addOption(linkOpt);
        options.addOption(userOpt);
        options.addOption(passwdOpt);
        options.addOption(tableOpt);
        options.addOption(elementOpt);
        options.addOption(stationOpt);
        options.addOption(dayOpt);
        options.addOption(dayFromOpt);
        options.addOption(dayToOpt);
        options.addOption(formatOpt);
        options.addOption(timestepOpt);
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("l")) {
                link = line.getOptionValue("l");
            }
            if (line.hasOption("u")) {
                user = line.getOptionValue("u");
            }
            if (line.hasOption("p")) {
                passwd = line.getOptionValue("p");
            }
            if (line.hasOption("d")) {
                day = line.getOptionValue("d");
                System.err.println("Info: day option value is '" + day + "'");
                dayId = Integer.parseInt(day);
            }
            if (line.hasOption("d1")) {
                dayFrom = line.getOptionValue("d1");
                System.err.println("Info: dayFrom option value is '" + dayFrom + "'");
                dayId = Integer.parseInt(dayFrom);
            }
            if (line.hasOption("d2")) {
                dayTo = line.getOptionValue("d2");
                System.err.println("Info: dayTo option value is '" + dayTo + "'");
                dayId = Integer.parseInt(dayTo);
            }
            if (line.hasOption("t")) {
                table = line.getOptionValue("t");
            }
            if (line.hasOption("s")) {
                station = line.getOptionValue("s");
            }
            if (line.hasOption("e")) {
                element = line.getOptionValue("e");
            }
            if (line.hasOption("f")) {
                format = line.getOptionValue("f");
            }
        } catch (Exception pe) {
            usage(options);
            System.exit(1);
        }
        dayFrom = ((day == null || "".equals(day)) ? dayFrom : day);
        dayTo = ((day == null || "".equals(day)) ? dayTo : day);
        System.err.println("Info: export data for table '" + table + "' station '" + station + "' element '" + element + "' datefrom '" + dayFrom + "' dateto '" + dayTo + "' format '" + ((format == null || "".equals(format) ? "ascii" : format)) + "'");
        System.err.println("Info: Start of spidr.export.SpidrClient");
        long curTime = (new java.util.Date()).getTime();
        String fileName = null;
        try {
            System.err.println("Info: web service call ...");
            fileName = webService(link, user, passwd, table, station, element, dayFrom, dayTo, ((format == null || "".equals(format) ? "ascii" : format)), sampling, null);
            System.out.println(fileName);
        } catch (Exception e) {
            System.err.println("Error: can't load data: " + e.toString());
            System.exit(1);
        }
        curTime = (new java.util.Date()).getTime() - curTime;
        System.err.println("Info: finish (" + (float) curTime / 1000 + " sec)");
    }
}
