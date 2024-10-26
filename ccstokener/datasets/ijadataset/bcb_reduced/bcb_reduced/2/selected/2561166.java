package net.sf.freesimrc.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

public class WhazzupFetcher {

    /**
	 * Use for INTVAS network
	 */
    public static final int INTVAS = 1;

    /**
	 * Use for VATSIM network
	 */
    public static final int VATSIM = 2;

    /**
	 * Use for IVAO network
	 */
    public static final int IVAO = 3;

    /**
	 * Use for GRVAC network
	 */
    public static final int GRVAC = 4;

    public static final int GLVAC = 5;

    private String intvasvoice = "http://www.intvas.net/whazzup/voice.asp";

    private String intvasmemberDeatails = "http://www.intvas.net/membername.asp";

    private String voiceaddress;

    private String rating = "0";

    public WhazzupFetcher(int network) {
        switch(network) {
            case 1:
                voiceaddress = intvasvoice;
                return;
            case 2:
                return;
            case 3:
                return;
            case 4:
                return;
            case 5:
            default:
        }
    }

    /**
	 * 
	 * 
	 * @return Vector containing IP addresses of online Network Servers.
	 */
    public Vector<String> getNetworkServersIPs(String netaddress) {
        Vector<String> result = new Vector<String>();
        boolean serverline = false;
        String line;
        String[] splitline;
        try {
            URL url = new URL(netaddress);
            URLConnection connection = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                if ((serverline) && line.startsWith(";")) {
                    serverline = false;
                }
                if (serverline) {
                    splitline = line.split(":");
                    result.add(splitline[1]);
                }
                if (line.startsWith("!SERVERS")) {
                    serverline = true;
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
	 * 
	 * 
	 * @return Vector containing the names of online voice Servers.
	 */
    public Vector<String> getVoiceServersNames() {
        Vector<String> result = new Vector<String>();
        boolean serverline = false;
        String line;
        String[] splitline;
        try {
            URL url = new URL(voiceaddress);
            URLConnection connection = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                if (serverline) {
                    splitline = line.split(":");
                    result.add(splitline[0]);
                }
                if (line.startsWith("!VOICE SERVERS")) {
                    serverline = true;
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String GetMemberName(String id) {
        String name = null;
        try {
            String line;
            URL url = new URL(intvasmemberDeatails + "?CID=" + id);
            URLConnection connection = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                name = line;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] parts = name.split(" ");
        rating = parts[2];
        return parts[0] + " " + parts[1];
    }

    public String GetMemberRating() {
        return rating;
    }
}
