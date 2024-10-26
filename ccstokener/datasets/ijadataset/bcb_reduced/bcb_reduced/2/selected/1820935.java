package net.sf.freesimrc.networking.whazzup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Vector;
import net.sf.freesimrc.Preference;

public class WhazzupParser {

    private String whazzupfile;

    private Preference pref;

    public Vector<WhazzupServer> servers;

    public Vector<WhazzupClient> clients;

    public WhazzupParser() {
        pref = new Preference();
        this.whazzupfile = pref.getPref("WhazzupFile");
        servers = new Vector<WhazzupServer>();
        clients = new Vector<WhazzupClient>();
    }

    public void getServers() {
        String line;
        servers.clear();
        try {
            URL url = new URL(whazzupfile);
            URLConnection con = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            boolean serverLine = false;
            while ((line = reader.readLine()) != null) {
                if ((serverLine) && line.startsWith(";")) serverLine = false;
                if ((serverLine) && (line.startsWith("!"))) serverLine = false;
                if (serverLine) makeServer(line);
                if (line.startsWith("!SERVERS")) serverLine = true;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void makeServer(String line) {
        WhazzupServer ws = new WhazzupServer();
        try {
            String[] spl = line.split(":");
            ws.setIdent(spl[0]);
            ws.setHost(spl[1]);
            ws.setLocation(spl[2]);
            ws.setServername(spl[3]);
            if (spl[4].equals("1")) ws.setClientsAllowed(true); else ws.setClientsAllowed(false);
            ws.setMaxClients(new Integer(spl[5]).intValue());
        } catch (ArrayIndexOutOfBoundsException ex) {
        }
        servers.add(ws);
    }

    public void getClients() {
        String line;
        clients.clear();
        try {
            URL url = new URL(whazzupfile);
            URLConnection con = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            boolean clientLine = false;
            while ((line = reader.readLine()) != null) {
                if ((clientLine) && line.startsWith(";")) clientLine = false;
                if ((clientLine) && (line.startsWith("!"))) clientLine = false;
                if (clientLine) makeClient(line);
                if (line.startsWith("!CLIENTS")) clientLine = true;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void makeClient(String line) {
        WhazzupClient wc = new WhazzupClient();
        try {
            String[] s = line.split(":");
            wc.setCallsign(s[0]);
            wc.setVID(s[1]);
            wc.setRealname(s[2]);
            wc.setClientType(s[3]);
            wc.setFrequency(s[4]);
            wc.setLatitude(s[5]);
            wc.setLongitude(s[6]);
            wc.setAltitude(s[7]);
            wc.setGroundSpeed(s[8]);
            wc.setPlannedAircraft(s[9]);
            wc.setPlannedTAS(s[10]);
            wc.setPlannedDepAirport(s[11]);
            wc.setPlannedAltitude(s[12]);
            wc.setPlannedDestAirport(s[13]);
            wc.setServer(s[14]);
            wc.setProtRevision(s[15]);
            wc.setRating(s[16]);
            wc.setTransponder(s[17]);
            wc.setFacilityType(s[18]);
            wc.setVisualRange(s[19]);
            wc.setPlannedRevision(s[20]);
            wc.setPlannedFlightType(s[21]);
            wc.setPlannedDepTime(s[22]);
            wc.setPlannedActDepTime(s[23]);
            wc.setPlannedHrsEnroute(s[24]);
            wc.setPlannedMinEnroute(s[25]);
            wc.setPlannedHrsFuel(s[26]);
            wc.setPlannedMinFuel(s[27]);
            wc.setPlannedAltn(s[28]);
            wc.setPlannedRemarks(s[29]);
            wc.setPlannedRoute(s[30]);
            wc.setPlannedDepAirportLat(s[31]);
            wc.setPlannedDepAirportLon(s[32]);
            wc.setPlannedDestAirportLat(s[33]);
            wc.setPlannedDestAirportLon(s[34]);
            wc.setAtisMessage(s[35]);
            wc.setTimeLastAtisReceived(s[36]);
            wc.setTimeConnected(s[37]);
            wc.setClientSoftwareName(s[38]);
            wc.setClientSoftwareVersion(s[39]);
            wc.setAdminRating(s[40]);
            wc.setAtcOrPilotRating(s[41]);
            wc.setPlannedAltn2(s[42]);
            wc.setPlannedTypeOfFlight(s[43]);
            wc.setPlannedPersonsOnBoard(s[44]);
        } catch (ArrayIndexOutOfBoundsException ex) {
        }
        clients.add(wc);
    }

    public WhazzupClient getClient(String callsign) {
        Iterator<WhazzupClient> iter = clients.iterator();
        while (iter.hasNext()) {
            WhazzupClient w = iter.next();
            if (w.getCallsign().toUpperCase().equals(callsign.toUpperCase())) return w;
        }
        return null;
    }
}
