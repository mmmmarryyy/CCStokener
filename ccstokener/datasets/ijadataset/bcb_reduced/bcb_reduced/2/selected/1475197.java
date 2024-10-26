package com.michaelbelyakov1967.projects.VICZONE;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.michaelbelyakov1967.gps.GPSMessenger;
import com.michaelbelyakov1967.gps.GPSMessage;
import com.michaelbelyakov1967.gps.GPSData;
import com.michaelbelyakov1967.util.Connector;
import com.michaelbelyakov1967.util.TuttiFrutti;
import com.michaelbelyakov1967.util.StreamPreparator;

public class T801 extends GPSMessenger implements Runnable {

    protected VicZoneMessage current;

    protected boolean registred;

    protected boolean debug;

    protected boolean inCanada;

    private static HashMap<String, PreparedStatement> statements = new HashMap<String, PreparedStatement>();

    static boolean saveIntoDB(String dbName, GPSData msg, int id) {
        if (bundle == null) bundle = new T801().bundle;
        try {
            PreparedStatement gstmt = statements.get(dbName);
            if (gstmt == null) {
                gstmt = Connector.connect(bundle.getString("dbLogin"), bundle.getString("dbPassword"), "rus_" + dbName, Connector.Type.POSTGRESQL).prepareStatement(bundle.getString("saveQuery"));
                statements.put(dbName, gstmt);
            }
            gstmt.setTimestamp(1, new java.sql.Timestamp(msg.getDate().getTime()));
            gstmt.setInt(2, new Integer(id));
            gstmt.setInt(3, msg.getLatGrades());
            gstmt.setDouble(4, msg.getLatMinutes());
            gstmt.setInt(5, msg.getLonGrades());
            gstmt.setDouble(6, msg.getLonMinutes());
            gstmt.setBoolean(7, msg.isValid());
            gstmt.setBoolean(8, msg.isNorth());
            gstmt.setBoolean(9, msg.isEast());
            gstmt.setDouble(10, msg.getSpeed());
            gstmt.setDouble(11, msg.getDirection());
            gstmt.setBoolean(12, msg.getMilProv());
            gstmt.setLong(13, msg.getMileage());
            gstmt.executeUpdate();
            return true;
        } catch (SQLException ex) {
            System.out.println(ex);
            return false;
        }
    }

    private int timeout;

    private Socket sock;

    public T801(Socket sck) {
        this();
        sock = sck;
    }

    public T801() {
        super();
        Timer tm = new Timer();
        timeout = new Integer(bundle.getString("timeout"));
        debug = new Boolean(bundle.getString("debug"));
        inCanada = new Boolean(bundle.getString("inCanada"));
        tm.schedule(new TimerTask() {

            public void run() {
                try {
                    if (sock != null && !sock.isClosed()) sock.close();
                    System.out.println("Timeout.");
                } catch (Exception ex1) {
                }
            }
        }, timeout);
    }

    public GPSMessage create(InputStream is) throws IOException {
        String s = "";
        char ch = '?';
        while (ch != '^') {
            if (ch == '*') {
                s = "";
            }
            ch = (char) is.read();
            s += ch;
        }
        current = new VicZoneMessage(s, this);
        return current;
    }

    public void save(GPSMessage msg) {
        if (debug && current != null) {
            System.out.println("> *" + current.toString());
            System.out.println("  ID=" + getVehicle());
            System.out.println(" ORG=" + getOrganisation());
            if (msg.hasGPS()) System.out.println("   Has GPS: " + msg.getGPSData());
        }
        if (msg.hasGPS()) {
            new Thread(new Runnable() {

                public void run() {
                    try {
                        saveIntoDB("" + getOrganisation(), current.getGPSData(), getVehicle());
                        if (!inCanada) {
                            URL url = new URL(bundle.getString("httpSaver") + "?id=" + getVehicle() + "&gps=" + current.getGPSData() + "&org=" + getOrganisation());
                            if (debug) System.out.println(" Data saved: " + StreamPreparator.stream2String(url.openStream()));
                        }
                    } catch (Exception ex) {
                        System.out.println("Exception on URL saving: " + ex);
                    }
                }
            }).start();
        }
    }

    public void write(OutputStream os) throws IOException {
        if (current == null) return;
        String s = current.toString();
        String answ = "*" + TuttiFrutti.getGPSTimeString();
        if (s.indexOf("UB05") > 0) {
            if (registred) return;
            answ += "DX00UB051^";
            registred = true;
        } else if (s.indexOf("HSO") > 0) {
            answ += "DB01HSO^";
        }
        if (debug) System.out.println("< " + answ);
        os.write(answ.getBytes());
        os.flush();
        current = null;
    }

    public static void main(String[] args) {
        if (args == null || args.length != 1) {
            System.out.println("Bad args.");
            System.exit(1);
        }
        try {
            ServerSocket serv = new ServerSocket(new Integer(args[0]));
            System.out.println("Listen on " + args[0] + ".");
            while (true) {
                Socket sock = serv.accept();
                System.out.println("==================== Accepted. ====================");
                new Thread(new T801(sock)).start();
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public void run() {
        VicZoneMessage.setPattern(bundle.getString("regex"));
        while (true) {
            if (sock.isClosed()) break;
            try {
                save(create(sock.getInputStream()));
                Thread.sleep(100);
                write(sock.getOutputStream());
            } catch (Exception ex) {
                System.out.println("Exception on save/write: " + ex);
                System.exit(1);
            }
        }
    }
}

class VicZoneMessage extends GPSMessage {

    private InputStream input;

    private static Pattern pattern;

    static void setPattern(String s) {
        if (pattern == null) pattern = Pattern.compile(s);
    }

    VicZoneMessage(String s, GPSMessenger m) {
        super(s, m);
    }

    protected void parseData(String s) {
        try {
            input = new ByteArrayInputStream(s.getBytes());
            date = getDate(input);
            down = isDownlink(input);
            category = (char) input.read();
            command = TuttiFrutti.readInt(input, 2);
            if (!down && category == 'B' && command == 5) {
                String p = s.substring(16);
                try {
                    org = Integer.parseInt(p.substring(0, 12));
                    vehicle = Integer.parseInt(p.substring(12, 15));
                } catch (NumberFormatException ex) {
                    vehicle = 1;
                }
            }
            Matcher m = pattern.matcher(body);
            if (m.find()) {
                ;
                String p = m.group(0);
                gps = new VicZoneGPSData(p);
            }
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    private static Calendar calendar = Calendar.getInstance();

    private Date getDate(InputStream is) throws IOException {
        calendar.set(Calendar.YEAR, TuttiFrutti.readInt(is, 2));
        calendar.set(Calendar.MONTH, TuttiFrutti.readInt(is, 2));
        calendar.set(Calendar.DATE, TuttiFrutti.readInt(is, 2));
        calendar.set(Calendar.HOUR_OF_DAY, TuttiFrutti.readInt(is, 2));
        calendar.set(Calendar.MINUTE, TuttiFrutti.readInt(is, 2));
        calendar.set(Calendar.SECOND, TuttiFrutti.readInt(is, 2));
        return calendar.getTime();
    }

    private boolean isDownlink(InputStream is) throws IOException {
        return 'D' == (char) is.read();
    }
}
