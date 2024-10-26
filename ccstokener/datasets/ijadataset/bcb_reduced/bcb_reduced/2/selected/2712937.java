package server.mwcyclopscomm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import common.House;
import common.Planet;
import common.CampaignData;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPersonalPilotQueues;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.operations.ShortOperation;
import server.campaign.pilot.SPilot;
import server.campaign.pilot.skills.SPilotSkill;

/**
  * Main Communcations calls for the MW Server to Cyclops
  * This is initialized in CampaignMain and accessed from there
  * This class routes all calls to their subclass to create the mesasges
  * to fit Cyclops' protocol
  * 
  * @author Torren Nov 10, 2005
  *
  */
public class MWCyclopsComm extends Thread {

    Socket socket;

    String ip = "muposerver.dyndns.org";

    String serverName = "Test Server";

    String stringURL = "http://muposerver.dyndns.org/devel/cyclops/XMLRPC/";

    boolean debug = false;

    int port = 80;

    private int queueSize = 100;

    private Vector<String> messageQueue = new Vector<String>(10, 1);

    public MWCyclopsComm(String ip, String servername, String URL, boolean debug) {
        super("MW Cyclops Comm Thread");
        this.ip = ip;
        this.serverName = servername;
        this.stringURL = URL;
        this.debug = debug;
    }

    /**
      * After much reflection I found that depending on the third party
      * DB to have good connections was a mistake
      * I've decided to thread the comm portion and put all messages into
      * a queue and every 125ms that queue is check and a message pulled
      * from it if there are any. This will keep the server from lagging
      * while waiting for a response from Cyclops.
      * 
      *  Torren.
      */
    @Override
    public synchronized void run() {
        try {
            while (true) {
                this.wait(125);
                if (!messageQueue.isEmpty()) sendMessage(messageQueue.remove(0), debug);
            }
        } catch (Exception ex) {
            CampaignData.mwlog.errLog("Error while trying to sleep in MWCyclopsComm.run()");
            CampaignData.mwlog.errLog(ex);
        }
    }

    /**
      * Sends a query to Cyclops to see it its up and running
      * the response is parsed and sent to mod mail.
      *
      */
    public void checkup() {
        String message = "";
        message += MWCyclopsUtils.methodCallStart();
        message += MWCyclopsUtils.methodName("cyclops.checkup");
        message += MWCyclopsUtils.methodCallEnd();
        String version = sendMessage(message, true);
        int start = version.indexOf("<boolean>") + 9;
        int end = version.indexOf("</boolean>");
        if (version.substring(start, end).equals("1")) version = "Cyclops is UP"; else version = "Cyclops is DOWN";
        CampaignMain.cm.doSendModMail("Cyclops check: ", version);
    }

    /**
      * Send a request for the cyclops version
      * the response is parased and sent to mod mail
      *
      */
    public void version() {
        String message = "";
        message += MWCyclopsUtils.methodCallStart();
        message += MWCyclopsUtils.methodName("cyclops.version");
        message += MWCyclopsUtils.methodCallEnd();
        String version = sendMessage(message, true);
        int start = version.indexOf("<string>") + 8;
        int end = version.indexOf("</string>");
        CampaignMain.cm.doSendModMail("Cyclops Version: ", version.substring(start, end));
    }

    /**
      * send a request to reset the cyclops DB
      * A response is only logged when debug mode is on
      *
      */
    public void reset() {
        String message = "";
        message += MWCyclopsUtils.methodCallStart();
        message += MWCyclopsUtils.methodName("cyclops.reset");
        message += MWCyclopsUtils.methodCallEnd();
        String version = sendMessage(message, true);
        int start = version.indexOf("<boolean>") + 9;
        int end = version.indexOf("</boolean>");
        if (version.substring(start, end).equals("1")) version = "Cyclops DataBase Reset Complete"; else version = "Cyclops DataBase Reset failed";
        CampaignMain.cm.doSendModMail("Cyclops reset: ", version);
    }

    /**
      * send an optimize command to cyclopse
      * basicly defrags the DB
      *
      */
    public void optimize() {
        String message = "";
        message += MWCyclopsUtils.methodCallStart();
        message += MWCyclopsUtils.methodName("cyclops.optimize");
        message += MWCyclopsUtils.methodCallEnd();
        String version = sendMessage(message, true);
        int start = version.indexOf("<boolean>") + 9;
        int end = version.indexOf("</boolean>");
        if (version.substring(start, end).equals("1")) version = "Cyclops DataBase Optimize Complete"; else version = "Cyclops DataBase Optimize failed";
        CampaignMain.cm.doSendModMail("Cyclops Optimize: ", version);
    }

    /**
      * send an optimize command to cyclopse
      * basicly defrags the DB
      *
      */
    public void postmaxsize() {
        String message = "";
        message += MWCyclopsUtils.methodCallStart();
        message += MWCyclopsUtils.methodName("cyclops.postmaxsize");
        message += MWCyclopsUtils.methodCallEnd();
        String version = sendMessage(message, true);
        int start = version.indexOf("<int>") + 5;
        int end = version.indexOf("</int>");
        CampaignMain.cm.doSendModMail("Cyclops Postmaxsize: ", version.substring(start, end));
    }

    /**
      * send a house.write() command cyclopse
      * @param house
      */
    public void houseWrite(SHouse house) {
        String message = "";
        message = MWCyclopsHouse.houseWrite(house);
        messageQueue.add(message);
    }

    public void houseWriteFromList(Collection<House> houses) {
        String message = "";
        message = MWCyclopsHouse.houseWriteFromList(houses);
        messageQueue.add(message);
    }

    /**
      * send a planet.write() command to cyclopse
      * @param planet
      */
    public void planetWrite(SPlanet planet) {
        String message = "";
        message = MWCyclopsPlanet.planetWrite(planet);
        messageQueue.add(message);
    }

    public void planetWriteFromList(Collection<Planet> planets) {
        String message = "";
        Collection<Planet> planetList = new Vector<Planet>(queueSize + 1, 1);
        for (Planet planet : planets) {
            planetList.add(planet);
            if (planetList.size() >= queueSize) {
                message = MWCyclopsPlanet.planetWriteFromList(planetList);
                messageQueue.add(message);
                planetList.clear();
            }
        }
        if (!planetList.isEmpty()) {
            message = MWCyclopsPlanet.planetWriteFromList(planetList);
            messageQueue.add(message);
        }
    }

    /**
      * send a player.write() command to cyclops
      * @param player
      */
    public void playerWrite(SPlayer player) {
        String message = "";
        message = MWCyclopsPlayer.playerWrite(player);
        messageQueue.add(message);
    }

    public void unitTemplateWrite(SUnit unit) {
        String message = "";
        message = MWCyclopsUnitTemplate.unitTemplateWrite(unit);
        messageQueue.add(message);
    }

    public void unitTemplateWriteFromList(Vector<SUnit> units) {
        String message = "";
        Vector<SUnit> unitList = new Vector<SUnit>(queueSize + 1, 1);
        for (SUnit unit : units) {
            unitList.add(unit);
            if (unitList.size() >= queueSize) {
                message = MWCyclopsUnitTemplate.unitTemplateWriteFromList(unitList);
                messageQueue.add(message);
                unitList.clear();
            }
        }
        if (!unitList.isEmpty()) {
            message = MWCyclopsUnitTemplate.unitTemplateWriteFromList(unitList);
            messageQueue.add(message);
        }
    }

    public void skillWrite(SPilotSkill skill) {
        String message = "";
        message = MWCyclopsSkill.skillWrite(skill);
        messageQueue.add(message);
    }

    public void skillWriteFromList(Hashtable<Integer, SPilotSkill> skills) {
        String message = "";
        message = MWCyclopsSkill.skillWriteFromList(skills);
        messageQueue.add(message);
    }

    public void unitWrite(SUnit unit, String Player, String House) {
        String message = "";
        message = MWCyclopsUnit.unitWrite(unit, Player, House);
        messageQueue.add(message);
    }

    public void unitWriteFromList(List<SUnit> units, String Player, String House) {
        String message = "";
        message = MWCyclopsUnit.unitWriteFromList(units, Player, House);
        messageQueue.add(message);
    }

    public void unitDestroy(String unitid, String reason, String opid, String destroyingPlayer, String destroyingUnit) {
        String message = "";
        message = MWCyclopsUnit.unitDestroy(unitid, reason, opid, destroyingPlayer, destroyingUnit);
        messageQueue.add(message);
    }

    public void unitChangeOwnerShip(String unitID, String Player, String House, String opID, String reason) {
        String message = "";
        message = MWCyclopsUnit.unitChangeOwnership(unitID, Player, House, opID, reason);
        messageQueue.add(message);
    }

    public void pilotWrite(SPilot pilot, String player) {
        String message = "";
        if (pilot.getName().equalsIgnoreCase("vacant") || pilot.getGunnery() == 99 || pilot.getPiloting() == 99) return;
        message = MWCyclopsPilot.pilotWrite(pilot, player);
        messageQueue.add(message);
    }

    public void pilotWriteFromList(SPersonalPilotQueues pilots, String player) {
        String message = "";
        message = MWCyclopsPilot.pilotWriteFromList(pilots, player);
        messageQueue.add(message);
    }

    public void pilotKill(SPilot pilot, String opid) {
        String message = "";
        message = MWCyclopsPilot.pilotKill(pilot, opid);
        messageQueue.add(message);
    }

    public void pilotRetire(SPilot pilot) {
        String message = "";
        message = MWCyclopsPilot.pilotRetire(pilot);
        messageQueue.add(message);
    }

    /**
      *The start of an op 
      */
    public void opWrite(ShortOperation op) {
        String message = "";
        message = MWCyclopsOp.opWrite(op);
        messageQueue.add(message);
    }

    /**
      *The start of an op 
      */
    public void opConclude(ShortOperation op) {
        String message = "";
        message = MWCyclopsOp.opConclude(op);
        messageQueue.add(message);
    }

    /**
      *The start of an op 
      */
    public void opCancel(ShortOperation op) {
        String message = "";
        if (op.getOpCyclopsID() == null) return;
        message = MWCyclopsOp.opCancel(op);
        messageQueue.add(message);
    }

    /**
      * Main method call to send all messages to cyclops
      * @param message
      * @param log
      * @return
      */
    public String sendMessage(String message, boolean log) {
        StringBuilder ret;
        try {
            URL url = new URL(this.stringURL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("User-Agent", serverName);
            urlConnection.setRequestProperty("Host", ip);
            urlConnection.setRequestProperty("Content-type", "text/xml");
            urlConnection.setRequestProperty("Content-length", Integer.toString(message.length()));
            PrintWriter _out = new PrintWriter(urlConnection.getOutputStream());
            if (log) {
                CampaignData.mwlog.infoLog("Sending Message: " + MWCyclopsUtils.formatMessage(message));
            } else CampaignData.mwlog.infoLog("Sending Message: " + message);
            _out.println(message);
            _out.flush();
            _out.close();
            ret = new StringBuilder();
            if (log) {
                BufferedReader _in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String input;
                while ((input = _in.readLine()) != null) ret.append(input + "\n");
                CampaignData.mwlog.infoLog(ret.toString());
                _in.close();
            } else {
                BufferedReader _in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                while (_in.readLine() != null) {
                }
                _in.close();
            }
            _out.close();
            urlConnection.disconnect();
            return ret.toString();
        } catch (Exception ex) {
            CampaignData.mwlog.errLog(ex);
        }
        return "";
    }
}
