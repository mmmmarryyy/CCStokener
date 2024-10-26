package bbalc.core;

import java.util.*;
import java.net.*;
import java.io.*;
import bbalc.tools.*;

/**
 * represents a BloodBowl team with all its abilities and values
 */
public class HTMLTeam extends Team {

    private ArrayList htmlTeam;

    private boolean isOK;

    /**
	 * the constructor creates a team from the given html file list.
	 * @param team an list storing an html team file
	**/
    public HTMLTeam(ArrayList team) {
        super();
        this.isOK = false;
        this.htmlTeam = team;
        parseHTML(this.htmlTeam);
    }

    /**
	 * the constructor creates a team from the given html file.
	 * @param filename the filename to load
	**/
    public HTMLTeam(String filename) {
        super();
        this.isOK = false;
        htmlTeam = load(filename);
        if (htmlTeam != null) parseHTML(htmlTeam);
    }

    /**
	 * the constructor creates a team from the given URL.
	 * @param url the URL to load
	**/
    public HTMLTeam(URL url) {
        super();
        this.isOK = false;
        htmlTeam = loadHTML(url);
        parseHTML(htmlTeam);
    }

    /**
	 * adds a new line to the HTML team list
	 * @param line a String
	 */
    public void addHTMLTeamLine(String line) {
        if (htmlTeam != null) {
            htmlTeam.add(line);
        }
    }

    /**
	 * creates a IPlayer object from this line.
	 * @param line a String
	 */
    private void createPlayer(String line) {
        String[] playerData = getTagContent(line, 16);
        for (int n = 0; n < playerData.length; n++) {
            if (playerData[n].startsWith("&nbsp")) {
                if ((n == 1) && (n == 2) && (n == 7)) playerData[n] = ""; else {
                    if ((n == 7) || (n == 8)) playerData[n] = ""; else playerData[n] = "0";
                }
            }
        }
        if (!playerData[1].equals("0")) {
            Player myPlayer = new Player(playerData);
            addPlayer(myPlayer);
        }
    }

    /**
	 * returns the HTML team list.
	 * @return ArrayList a HTML team 
	 */
    public ArrayList getHTMLTeam() {
        return this.htmlTeam;
    }

    /**
	 * returns the content between <TD(...)> and </TD>.
	 * @param line a String
	 * @param size number of splits
	 * @return list of Strings
	 */
    private String[] getTagContent(String line, int size) {
        String[] ret = new String[size];
        int start = 0;
        int stop = 0;
        for (int n = 0; n < size; n++) {
            start = line.indexOf("<TD", start);
            start = line.indexOf(">", start);
            stop = line.indexOf("</TD>", start);
            String tmp = line.substring(start + 1, stop);
            ret[n] = tmp;
        }
        return ret;
    }

    /**
	 * loads an HTML team from file
	 * @param filename a String
	 * @return a html team list
	 */
    private ArrayList load(String filename) {
        ArrayList content = new ArrayList();
        String line = "";
        try {
            BufferedReader mystream = new BufferedReader(new FileReader(filename));
            while (line != null) {
                line = mystream.readLine();
                if (line != null) content.add(line);
            }
            this.isOK = true;
        } catch (Exception e) {
            content = null;
            System.err.println("HTMLTeam.load> " + e);
            throw new RuntimeException();
        }
        return content;
    }

    public boolean isValid() {
        return this.isOK;
    }

    /**
	 * loads an HTML team from the URL
	 * @param url a URL
	 * @return a html team list
	 */
    private ArrayList loadHTML(URL url) {
        ArrayList res = new ArrayList();
        try {
            URLConnection myCon = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(myCon.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                res.add(inputLine);
            }
            in.close();
        } catch (Exception e) {
            System.out.println("url> " + url);
        }
        return res;
    }

    /**
	 * creates a ITeam object from the HTML team list
	 * @param myTeam the team list
	 */
    public void parseHTML(ArrayList myTeam) {
        for (int n = 0; n < myTeam.size(); n++) {
            String line = (String) myTeam.get(n);
            if ((line.startsWith("<TR alignn=\"center\">")) || (line.startsWith("<TR ALIGN=CENTER><TD>"))) createPlayer(line);
            if ((line.startsWith("<TR align=\"center\"><TD align=")) || (line.startsWith("<TR ALIGN=CENTER><TD ALIGN="))) {
                String[] content = getTagContent(line, 4);
                if (content[0].equals("Team Name:")) {
                    if (content[1].startsWith("&nbsp")) content[1] = "";
                    if (content[3].startsWith("&nbsp")) content[3] = "0";
                    setName(content[1]);
                    setReRolls(IOTools.string2int(content[3]));
                } else if (content[0].equals("Race:")) {
                    if (content[1].startsWith("&nbsp")) content[1] = "";
                    if (content[3].startsWith("&nbsp")) content[3] = "0";
                    setRace(content[1]);
                    setFanFactor(IOTools.string2int(content[3]));
                } else if (content[0].equals("Team Rating:")) {
                    if (content[1].startsWith("&nbsp")) content[1] = "0";
                    if (content[3].startsWith("&nbsp")) content[3] = "0";
                    setTeamRating(IOTools.string2int(content[1]));
                    setAssistantCoaches(IOTools.string2int(content[3]));
                } else if (content[0].equals("Treasury:")) {
                    if (content[1].startsWith("&nbsp")) content[1] = "0";
                    if (content[3].startsWith("&nbsp")) content[3] = "0";
                    setTreasury(IOTools.string2int(content[1]));
                    setCheerleaders(IOTools.string2int(content[3]));
                } else if (content[0].equals("Coach:")) {
                    if (content[1].startsWith("&nbsp")) content[1] = "";
                    if (content[3].startsWith("&nbsp")) content[3] = "0";
                    setCoach(content[1]);
                    setApothecary(IOTools.string2int(content[3]));
                }
            }
        }
    }
}
