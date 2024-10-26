package spidr.export;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.axis.client.*;

/**
 * The class is used to export data from ionosphere database using
 * command line call of the SPIDR web service
 */
public class IonoClient {

    /**
   * Makes web service call
   * @param siteUrl - URL of spidr web site
   * @param login - SPIDR login
   * @param password - SPIDR password
   * @param station - Ionospheric station code
   * @param element - Ionospheric parameter, may be null for all
   * @param day - Day ID formatted as YYYYMMDD
   * @return fileName � File name of the downloaded IIWG file
   * @throws Exception
   */
    public static String webService(String siteUrl, String login, String password, String station, String element, String day) throws Exception {
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
        String url = (String) call.invoke(new Object[] { station, element, day });
        String fileName = null;
        if (url == null) {
            throw new Exception("Error: result URL is null");
        } else {
            System.err.println("Info: result URL is " + url);
            URL dataurl = new URL(url);
            String filePath = dataurl.getFile();
            if (filePath == null) {
                throw new Exception("Error: data file name is null");
            } else {
                fileName = filePath.substring(filePath.lastIndexOf("/") < 0 ? 0 : filePath.lastIndexOf("/") + 1);
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

    /**
   * Usage: java spidr.export.IonoClient
   * @param args command line arguments properties-file stationCode dayId
   */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java spidr.export.IonoClient properties-file stationCode dayId");
            return;
        }
        System.err.println("Info: Start of spidr.export.IonoClient");
        long curTime = (new java.util.Date()).getTime();
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(new File(args[0])));
        } catch (Exception e) {
            System.err.println("Error: can't load properties file: " + e.toString());
            System.exit(1);
        }
        String siteUrl = prop.getProperty("siteUrl");
        if (siteUrl == null) {
            System.err.println("Error: undefined parameter 'service'");
            System.exit(1);
        }
        String login = prop.getProperty("login");
        if (login == null) {
            System.err.println("Error: undefined parameter 'login'");
            System.exit(1);
        }
        String password = prop.getProperty("password");
        if (password == null) {
            System.err.println("Error: undefined parameter 'password'");
            System.exit(1);
        }
        String stationCode = args[1];
        String day = args[2];
        System.err.println("Info: export IIWG data for station '" + stationCode + "' date " + day);
        String fileName = null;
        try {
            System.err.println("Info: web service call ...");
            fileName = webService(siteUrl, login, password, stationCode, null, day);
            System.out.println(fileName);
        } catch (Exception e) {
            System.err.println("Error: can't load data: " + e.toString());
            System.exit(1);
        }
        curTime = (new java.util.Date()).getTime() - curTime;
        System.err.println("Info: finish (" + (float) curTime / 1000 + " sec)");
    }
}
