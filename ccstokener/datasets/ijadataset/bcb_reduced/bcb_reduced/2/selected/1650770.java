package rbsla.utils.contractQsc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import rbsla.environment.ECAConstraints;
import ws.prova.RListUtils;
import ws.prova.reference.ComplexTermImpl;

/**
 * 
 * Klasse mit den Methoden des Test Vertrags QSC
 * 
 */
public class Qsc {

    public static void main(String[] args) {
        InetAddress iaHost = null;
        int iPort = 80;
        try {
            if (0 >= args.length) throw new Exception("Parameter missing!");
            iaHost = InetAddress.getByName(args[0]);
            if (1 < args.length) iPort = Integer.parseInt(args[1]);
            long tm = System.currentTimeMillis();
            Socket so = new Socket(iaHost, iPort);
            so.close();
            tm = System.currentTimeMillis() - tm;
            System.out.println("Connection ok (port " + iPort + ", time = " + tm + " ms). \n" + "Host Address = " + iaHost.getHostAddress() + "\n" + "Host Name    = " + iaHost.getHostName());
            System.exit(0);
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            System.exit(1);
        }
    }

    /**
	 * checks if a server is available
	 * 
	 * @param srv
	 *            host name
	 * @param port
	 *            port number
	 * @return true if the server is reachable, false if not
	 */
    public Boolean isAvailable(String srv, Integer port) {
        try {
            int p = port.intValue();
            Socket so = new Socket(srv, p);
            return Boolean.TRUE;
        } catch (Exception ex) {
            return Boolean.FALSE;
        }
    }

    /**
	 * gets the round trip delay of a package
	 * 
	 * @param srv
	 *            host name
	 * @param port
	 *            port number
	 * @return ms of the round trip
	 */
    public Integer getRT(String srv, Integer port) {
        try {
            int p = port.intValue();
            long tm = System.currentTimeMillis();
            Socket so = new Socket(srv, p);
            so.close();
            tm = System.currentTimeMillis() - tm;
            return new Integer(2 * (int) tm);
        } catch (Exception ex) {
            return new Integer(-1);
        }
    }

    public Integer getRT(String u) {
        try {
            long tm = System.currentTimeMillis();
            URL url = new URL(u);
            URLConnection connect = url.openConnection();
            connect.connect();
            tm = System.currentTimeMillis() - tm;
            return new Integer(2 * (int) tm);
        } catch (IOException e) {
            return new Integer(-1);
        }
    }

    public Boolean testWebserver(String u) {
        try {
            URL url = new URL(u);
            URLConnection connect = url.openConnection();
            connect.connect();
            return Boolean.TRUE;
        } catch (IOException e) {
            return Boolean.FALSE;
        }
    }

    public Integer getRoundTrip(String u) {
        try {
            long tm = System.currentTimeMillis();
            URL url = new URL(u);
            URLConnection connect = url.openConnection();
            connect.connect();
            tm = System.currentTimeMillis() - tm;
            return new Integer(2 * (int) tm);
        } catch (IOException e) {
            return new Integer(-1);
        }
    }

    public Boolean resetKB() {
        String standard = ":-eval(consult('rules/ContractLog/ec.prova')).\n" + ":-eval(consult('rules/ContractLog/datetime.prova')).\n" + ":-eval(consult('rules/ContractLog/timeinterval.prova')).\n";
        try {
            FileOutputStream out = new FileOutputStream("./rules/ContractLog/bsp2/kb.prova");
            for (int i = 0; i < standard.length(); i++) {
                out.write((byte) standard.charAt(i));
            }
            out.close();
        } catch (Exception fEx) {
            return Boolean.TRUE;
        }
        return Boolean.TRUE;
    }

    public Boolean saveHappens(Object ev, Object t, Long ms) {
        String event = complexToString(ev);
        String time = complexToString(t);
        try {
            Class.forName(ECAConstraints._DB_DRIVER);
            Connection con = DriverManager.getConnection(ECAConstraints._DB_NAME, ECAConstraints._DB_USER, ECAConstraints._DB_PWD);
            Statement st = con.createStatement();
            String query = "INSERT INTO happens(event, date, dateMS) VALUES('" + event + "','" + time + "'," + ms + ")";
            st.executeUpdate(query);
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return Boolean.TRUE;
    }

    private String complexToString(Object ct) {
        String ret = ct.toString();
        if (ct instanceof ComplexTermImpl) {
            ret = ret.replaceAll("]", "");
            ret = ret.replaceAll("\\[", "");
            ret = ret.replaceAll("\",", "(");
            ret = ret.replaceAll(",\"", "),");
            ret = ret.replaceAll("\"", "");
            int k = 0;
            int l = 0;
            for (int i = 0; i < ret.length(); i++) {
                char tmp = ret.charAt(i);
                if (tmp == '(') k++; else if (tmp == ')') l++;
            }
            while (l < k) {
                ret += ")";
                l++;
            }
        }
        return ret;
    }

    /**
	 * TODO: methode allgemeiner schreiben
	 * @param zeile
	 * @param formatierung
	 * @return
	 */
    public Boolean saveFact(Object[] zeile, Integer formatierung) {
        int nrArgs = zeile.length;
        int form = formatierung.intValue();
        String factToInsert = "";
        if (nrArgs == 2 && form == 1) {
            factToInsert = "happens(";
            if (zeile[0] instanceof String) {
                factToInsert += zeile[0] + ",";
            } else if (zeile[0] instanceof ComplexTermImpl) {
                String tmpFact = RListUtils.term_as_string(zeile[0]);
                String[] splitted = tmpFact.split("\"");
                factToInsert += splitted[1] + "(" + splitted[3] + "),";
            }
            factToInsert += dateTimeToString(zeile[1]) + ").\n";
        }
        if (nrArgs == 3 && form < 4) {
            if (form == 2) {
                factToInsert = "initiates(";
            } else if (form == 3) {
                factToInsert = "terminates(";
            }
            if (zeile[0] instanceof String) {
                factToInsert += zeile[0] + ",";
            } else if (zeile[0] instanceof ComplexTermImpl) {
                String tmpFact = RListUtils.term_as_string(zeile[0]);
                String[] splitted = tmpFact.split("\"");
                factToInsert += splitted[1] + "(" + splitted[3] + "),";
            }
            if (zeile[1] instanceof String) {
                factToInsert += zeile[1] + ",";
            }
            factToInsert += dateTimeToString(zeile[2]) + ").\n";
        }
        if (nrArgs == 3 && form == 4) {
            factToInsert = "valueAt(";
            if (zeile[0] instanceof String) {
                factToInsert += zeile[0] + ",";
            } else if (zeile[0] instanceof ComplexTermImpl) {
                String tmpFact = RListUtils.term_as_string(zeile[0]);
                String[] splitted = tmpFact.split("\"");
                factToInsert += splitted[1] + "(" + splitted[3] + "),";
            }
            factToInsert += dateTimeToString(zeile[2]);
            if (zeile[1] instanceof Integer) {
                factToInsert += "," + zeile[1] + ").\n";
            }
        }
        try {
            FileOutputStream out = new FileOutputStream("./rules/ContractLog/bsp2/kb.prova", true);
            for (int i = 0; i < factToInsert.length(); i++) {
                out.write((byte) factToInsert.charAt(i));
            }
            out.close();
        } catch (Exception fEx) {
        }
        return Boolean.TRUE;
    }

    private String dateTimeToString(Object DT) {
        String erg = "";
        if (DT instanceof ComplexTermImpl) {
            String tmpFact = RListUtils.term_as_string(DT);
            tmpFact = tmpFact.replaceAll("\"", "");
            tmpFact = tmpFact.substring(1, tmpFact.length() - 1);
            tmpFact = tmpFact.replaceFirst(",", "(") + ")";
            erg += tmpFact;
        }
        return erg;
    }

    public Float endOfMonthCalculation(Integer year, Integer month, String ser, Integer minAval) {
        int monat = month.intValue();
        int jahr = year.intValue();
        int lastDay = 28;
        if (monat == 1 || monat == 3 || monat == 5 || monat == 7 || monat == 8 || monat == 10 || monat == 12) {
            lastDay = 31;
        } else if (monat == 4 || monat == 6 || monat == 9 || monat == 11) {
            lastDay = 30;
        }
        float avgAval = 0;
        float minA = minAval.floatValue();
        try {
            Class.forName(ECAConstraints._DB_DRIVER);
            Connection con = DriverManager.getConnection(ECAConstraints._DB_NAME, ECAConstraints._DB_USER, ECAConstraints._DB_PWD);
            Statement st = con.createStatement();
            String dFrom = jahr + "-" + monat + "-01";
            String dTo = jahr + "-" + monat + "-" + lastDay;
            String q = "SELECT AVG(stat) FROM availability a WHERE a.Host='" + ser + "' AND a.Time BETWEEN '" + dFrom + "' AND '" + dTo + "'";
            ResultSet rs = st.executeQuery(q);
            if (rs.next()) {
                avgAval = 100 * rs.getFloat(1);
            }
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new Float(avgAval - minA);
    }
}
