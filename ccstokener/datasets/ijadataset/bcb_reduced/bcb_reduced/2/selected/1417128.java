package util;

import objects.*;
import server.OGSserver;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class GameInfo {

    public static void saveInfo(Galaxy galaxy, boolean post) {
        String type = OGSserver.getProperty("Server.SaveInfo", "off").toLowerCase();
        if (type.equalsIgnoreCase("off")) return;
        try {
            Profiler.getProfiler().start("saveinfo", galaxy.getName(), galaxy.getTurn());
            String path = "";
            if (type.equalsIgnoreCase("txt")) path = saveInfoTxt(galaxy); else if (type.equalsIgnoreCase("xml")) {
                path = saveInfoXML(galaxy);
                saveGameStatus(galaxy);
                saveMapXML(galaxy);
                saveGameInfoXML(galaxy);
                saveAuthXML(galaxy);
                if (galaxy.getTurn() >= 0 && !post) saveTurnStatisticsXML(galaxy);
            } else Galaxy.logger.severe("GameInfo: unknown method for saving game info" + type);
            if ((path != null) && (path.length() > 0)) informWebServer(path);
        } catch (Exception err) {
            Galaxy.logger.log(Level.SEVERE, "Save game info " + galaxy.getName(), err);
        } finally {
            Profiler.getProfiler().stop("saveinfo");
        }
    }

    private static void informWebServer(String path) throws Exception {
        if (!Utils.parseBoolean(OGSserver.getProperty("Server.SaveInfo.InformWebServer", "no"))) return;
        URL url = new URL(OGSserver.getProperty("Server.SaveInfo.InformWebServer.URL") + URLEncoder.encode(path, "UTF-8"));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.getInputStream().read();
        conn.disconnect();
    }

    private static String saveInfoTxt(Galaxy galaxy) throws IOException {
        File file = new File("info" + File.separator + galaxy.getName() + ".inf");
        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file), OGSserver.getProperty("Server.Charset"));
        StringBuilder sb = new StringBuilder(128 * galaxy.getRaces().length);
        sb.append(galaxy.getName()).append(' ').append(galaxy.state.ordinal()).append(' ').append(galaxy.getTurn()).append(' ').append(galaxy.getPlanets().length).append(' ').append(Utils.toReport(galaxy.getGeometry().size)).append(' ').append(galaxy.getRaces().length).append("\r\n");
        for (Race race : galaxy.getRaces()) {
            sb.append(race.getName()).append(' ').append(race.living).append(' ').append(race.getPassword()).append(' ').append(race.getEncoding().name()).append(' ').append(race.getAddresses().toString(true)).append(' ');
            TechBlock techs = race.getTechBlock();
            for (int j = 0; j < TechBlock.LENGTH; ++j) sb.append(Utils.toReport(techs.getTech(j))).append(' ');
            sb.append(Utils.toReport(race.totalPop())).append(' ').append(Utils.toReport(race.totalInd())).append(' ').append(race.totalPlanets()).append(' ').append(Utils.toReport(galaxy.receivedVotes(race))).append("\r\n");
        }
        osw.append(sb);
        osw.flush();
        osw.close();
        return file.getAbsolutePath();
    }

    private static String saveInfoXML(Galaxy galaxy) throws Exception {
        File file = new File("info" + File.separator + galaxy.getName() + ".xml.gz");
        org.w3c.dom.Document doc;
        if (file.canRead()) {
            javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(new GZIPInputStream(new FileInputStream(file), 0x10000));
        } else {
            doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation().createDocument("", "game", null);
            doc.getDocumentElement().setAttribute("name", galaxy.getName());
        }
        org.w3c.dom.Element root = doc.getDocumentElement();
        root.setAttribute("size", Utils.toReport(galaxy.getGeometry().size));
        root.setAttribute("planets", String.valueOf(galaxy.getPlanets().length));
        root.setAttribute("turn", String.valueOf(galaxy.getTurn()));
        root.setAttribute("state", String.valueOf(galaxy.state.ordinal()));
        org.w3c.dom.NodeList v = root.getElementsByTagName("turn");
        for (int i = 0; i < v.getLength(); ++i) {
            org.w3c.dom.Element e = (org.w3c.dom.Element) v.item(i);
            if (e.hasAttribute("num") && Integer.parseInt(e.getAttribute("num")) > galaxy.getTurn()) root.removeChild(e);
        }
        org.w3c.dom.Element turn = null;
        for (int i = 0; i < v.getLength(); ++i) {
            org.w3c.dom.Element e = (org.w3c.dom.Element) v.item(i);
            if (e.hasAttribute("num") && Integer.parseInt(e.getAttribute("num")) == galaxy.getTurn()) {
                turn = e;
                break;
            }
        }
        if (turn == null) {
            turn = doc.createElement("turn");
            root.appendChild(turn);
            turn.setAttribute("num", String.valueOf(galaxy.getTurn()));
            turn.setAttribute("date", String.valueOf(galaxy.saveDate.getTime()));
            turn.setAttribute("updated", String.valueOf(System.currentTimeMillis()));
            boolean isDismount = galaxy.props.getProperty("Galaxy.DismountScience") != null;
            for (Race race : galaxy.getRaces()) {
                org.w3c.dom.Element r = doc.createElement("race");
                turn.appendChild(formatRaceToXML(race, r));
                r.setAttribute("s-planets", r.getAttribute("planets"));
                r.setAttribute("s-pop", r.getAttribute("pop"));
                r.setAttribute("s-ind", r.getAttribute("ind"));
                r.setAttribute("s-votes", r.getAttribute("votes"));
                if (isDismount) {
                    r.setAttribute("s-drive", r.getAttribute("drive"));
                    r.setAttribute("s-weapons", r.getAttribute("weapons"));
                    r.setAttribute("s-shields", r.getAttribute("shields"));
                    r.setAttribute("s-cargo", r.getAttribute("cargo"));
                }
                if ((galaxy.state == Galaxy.State.FINAL || galaxy.state == Galaxy.State.ARCHIVE) && (!galaxy.getWinners().isEmpty())) if (galaxy.getWinners().contains(race)) r.setAttribute("winner", "true");
            }
        } else {
            org.w3c.dom.NodeList v2 = turn.getElementsByTagName("race");
            ArrayList<org.w3c.dom.Element> v3 = new ArrayList<org.w3c.dom.Element>();
            for (int j = 0; j < v2.getLength(); ++j) v3.add((org.w3c.dom.Element) v2.item(j));
            for (Race race : galaxy.getRaces()) {
                org.w3c.dom.Element r = null;
                for (int j = 0; j < v3.size(); ++j) {
                    org.w3c.dom.Element x = v3.get(j);
                    if (x.getAttribute("name").equals(race.getName())) {
                        r = x;
                        v3.remove(j);
                        break;
                    }
                }
                if (r == null) {
                    r = doc.createElement("race");
                    turn.appendChild(r);
                }
                formatRaceToXML(race, r);
            }
            for (org.w3c.dom.Element e : v3) turn.removeChild(e);
            turn.setAttribute("updated", String.valueOf(System.currentTimeMillis()));
        }
        javax.xml.transform.Transformer tr = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        OutputStream os = new GZIPOutputStream(new FileOutputStream(file), 0x10000);
        tr.transform(new javax.xml.transform.dom.DOMSource(doc), new javax.xml.transform.stream.StreamResult(os));
        os.close();
        return file.getAbsolutePath();
    }

    private static org.w3c.dom.Element formatRaceToXML(Race race, org.w3c.dom.Element r) {
        r.setAttribute("name", race.getName());
        if (race.getTeam() != null) r.setAttribute("team", race.getTeam().getName());
        r.setAttribute("email", race.getAddresses().toString(true));
        r.setAttribute("password", race.getPassword());
        r.setAttribute("enc", race.getEncoding().name());
        r.setAttribute("living", String.valueOf(race.living));
        TechBlock techs = race.getTechBlock();
        r.setAttribute("drive", Utils.toReport(techs.drive()));
        r.setAttribute("weapons", Utils.toReport(techs.weapons()));
        r.setAttribute("shields", Utils.toReport(techs.shields()));
        r.setAttribute("cargo", Utils.toReport(techs.cargo()));
        r.setAttribute("planets", String.valueOf(race.totalPlanets()));
        r.setAttribute("pop", Utils.toReport(race.totalPop()));
        r.setAttribute("ind", Utils.toReport(race.totalInd()));
        r.setAttribute("votes", Utils.toReport(race.getGalaxy().receivedVotes(race)));
        return r;
    }

    private static void saveGameStatus(Galaxy galaxy) throws javax.xml.parsers.ParserConfigurationException, javax.xml.transform.TransformerException, IOException {
        File dir = new File("info" + File.separator + galaxy.getName());
        dir.mkdirs();
        OutputStream os = new FileOutputStream(dir + File.separator + "status");
        OutputStreamWriter out = new OutputStreamWriter(os, "UTF-8");
        out.write(galaxy.getName() + ' ' + galaxy.state + ' ' + galaxy.getTurn() + '\n');
        out.flush();
        os.close();
    }

    private static void saveGameInfoXML(Galaxy galaxy) throws javax.xml.parsers.ParserConfigurationException, javax.xml.transform.TransformerException, IOException {
        org.w3c.dom.Element root = XmlHelper.createXML("game");
        root.setAttribute("name", galaxy.getName());
        if (galaxy.racesCount > 0) root.setAttribute("races-count", String.valueOf(galaxy.racesCount));
        root.setAttribute("info", galaxy.getGalaxyInfo());
        root.setAttribute("geometry", galaxy.getGeometry().name());
        String gamepass = galaxy.props.getProperty("Galaxy.JoinPassword");
        if (gamepass != null) root.setAttribute("private", "true");
        if (galaxy.getTeams() != null) {
            root.setAttribute("teams", galaxy.props.getProperty("Galaxy.Teams", "no"));
            if (galaxy.teamSize > 0) {
                if (galaxy.racesCount > 0) root.setAttribute("teams-count", String.valueOf(galaxy.racesCount / galaxy.teamSize));
                root.setAttribute("teams-size", String.valueOf(galaxy.teamSize));
            }
        }
        List<Race> winners = (galaxy.state == Galaxy.State.FINAL | galaxy.state == Galaxy.State.ARCHIVE) ? galaxy.getWinners() : new ArrayList<Race>();
        if (galaxy.getTeams() != null) for (Team team : galaxy.getTeams()) {
            org.w3c.dom.Element teamElem = XmlHelper.createElement(root, "team");
            teamElem.setAttribute("name", team.getName());
            if (team.getPassword() != null) teamElem.setAttribute("private", "true");
            boolean winner = false;
            boolean living = false;
            for (Race race : team.getRaces()) {
                org.w3c.dom.Element raceElem = XmlHelper.createElement(teamElem, "race");
                raceElem.setAttribute("name", race.getName());
                if (winners.contains(race)) {
                    winner = true;
                    raceElem.setAttribute("status", "winner");
                } else if (race.living) {
                    living = true;
                    raceElem.setAttribute("status", "active");
                } else raceElem.setAttribute("status", "dead");
            }
            if (winner) teamElem.setAttribute("status", "winner"); else if (living) teamElem.setAttribute("status", "active"); else teamElem.setAttribute("status", "dead");
        } else for (Race race : galaxy.getRaces()) {
            org.w3c.dom.Element raceElem = XmlHelper.createElement(root, "race");
            raceElem.setAttribute("name", race.getName());
            if (winners.contains(race)) raceElem.setAttribute("status", "winner"); else if (race.living) raceElem.setAttribute("status", "active"); else raceElem.setAttribute("status", "died");
        }
        File dir = new File("info" + File.separator + galaxy.getName());
        dir.mkdirs();
        XmlHelper.save(root, dir + File.separator + "info.xml");
    }

    private static void saveAuthXML(Galaxy galaxy) throws javax.xml.parsers.ParserConfigurationException, javax.xml.transform.TransformerException, IOException {
        org.w3c.dom.Element root = XmlHelper.createXML("game");
        root.setAttribute("name", galaxy.getName());
        String gamepass = galaxy.props.getProperty("Galaxy.JoinPassword");
        if (gamepass != null) root.setAttribute("join-password", gamepass);
        if (galaxy.getTeams() != null) for (Team team : galaxy.getTeams()) saveTeamAuthXML(team, root); else for (Race race : galaxy.getRaces()) saveRaceAuthXML(race, root);
        File dir = new File("info" + File.separator + galaxy.getName());
        dir.mkdirs();
        XmlHelper.save(root, dir + File.separator + "players.xml");
    }

    private static void saveTeamAuthXML(Team team, org.w3c.dom.Element parent) {
        org.w3c.dom.Element elem = XmlHelper.createElement(parent, "team");
        elem.setAttribute("name", team.getName());
        if (team.getPassword() != null) elem.setAttribute("join-password", team.getPassword());
        for (Race race : team.getRaces()) saveRaceAuthXML(race, elem);
    }

    private static void saveRaceAuthXML(Race race, org.w3c.dom.Element parent) {
        org.w3c.dom.Element elem = XmlHelper.createElement(parent, "race");
        elem.setAttribute("name", race.getName());
        elem.setAttribute("password", race.getPassword());
        for (GamerAddress ga : race.getAddresses().addresses()) {
            org.w3c.dom.Element addressElem = XmlHelper.createElement(elem, "email");
            addressElem.setAttribute("address", ga.getString());
            if (!ga.isCanReceive(GamerAddressList.ALL)) {
                ArrayList<String> type = new ArrayList<String>();
                if (ga.isCanReceive(GamerAddressList.MAIL)) type.add("mail");
                if (ga.isCanReceive(GamerAddressList.REPORT)) type.add("report");
                if (ga.isCanReceive(GamerAddressList.ANSWER)) type.add("answer");
                addressElem.setAttribute("type", Utils.join(type.toArray(new String[0])));
            }
            if (ga.getConfirmCode() != null) addressElem.setAttribute("unconfirmed", "true");
        }
        elem.setAttribute("report-charset", race.getEncoding().name());
        elem.setAttribute("report-name", race.repName);
    }

    private static void saveMapXML(Galaxy galaxy) throws javax.xml.parsers.ParserConfigurationException, javax.xml.transform.TransformerException, IOException {
        org.w3c.dom.Element root = XmlHelper.createXML("map");
        root.setAttribute("geometry", galaxy.getGeometry().name());
        root.setAttribute("size", Utils.toReport(galaxy.getGeometry().size));
        for (Planet planet : galaxy.getPlanets()) {
            Position pos = planet.getPosition();
            org.w3c.dom.Element elem = XmlHelper.createElement(root, "planet");
            elem.setAttribute("id", Integer.toString(planet.getNumber()));
            elem.setAttribute("x", Utils.toReport(pos.getX()));
            elem.setAttribute("y", Utils.toReport(pos.getY()));
        }
        File dir = new File("info" + File.separator + galaxy.getName());
        dir.mkdirs();
        XmlHelper.save(root, dir + File.separator + "map.xml");
    }

    private static void saveTurnStatisticsXML(Galaxy galaxy) throws javax.xml.parsers.ParserConfigurationException, javax.xml.transform.TransformerException, IOException {
        org.w3c.dom.Element root = XmlHelper.createXML("turn");
        root.setAttribute("number", String.valueOf(galaxy.getTurn()));
        root.setAttribute("date", galaxy.saveDate.toString());
        root.setAttribute("size", Utils.toReport(galaxy.getGeometry().size));
        root.setAttribute("planets", String.valueOf(galaxy.getPlanets().length));
        double totalPop = 0;
        double totalInd = 0;
        double totalVotes = 0;
        for (Race race : galaxy.getRaces()) {
            totalPop += race.totalPop();
            totalInd += race.totalInd();
            totalVotes += race.votes();
        }
        root.setAttribute("population", Utils.toReport(totalPop));
        root.setAttribute("industry", Utils.toReport(totalInd));
        root.setAttribute("votes", Utils.toReport(totalVotes));
        if (galaxy.getTeams() != null) for (Team team : galaxy.getTeams()) saveTeamStatisticsXML(team, root); else for (Race race : galaxy.getRaces()) saveRaceStatisticsXML(race, root);
        File dir = new File("info" + File.separator + galaxy.getName() + File.separator + "statistics");
        dir.mkdirs();
        XmlHelper.saveZ(root, dir + File.separator + galaxy.getTurn() + ".xml");
    }

    private static void saveTeamStatisticsXML(Team team, org.w3c.dom.Element parent) {
        boolean living = false;
        for (Race race : team.getRaces()) if (race.living) {
            living = true;
            break;
        }
        if (!living) return;
        org.w3c.dom.Element elem = XmlHelper.createElement(parent, "team");
        elem.setAttribute("name", team.getName());
        TechBlock techs = new TechBlock();
        for (Race race : team.getRaces()) techs = TechBlock.max(techs, race.getTechBlock());
        elem.setAttribute("drive", Utils.toReport(techs.drive()));
        elem.setAttribute("weapons", Utils.toReport(techs.weapons()));
        elem.setAttribute("shields", Utils.toReport(techs.shields()));
        elem.setAttribute("cargo", Utils.toReport(techs.cargo()));
        double totalPop = 0;
        double totalInd = 0;
        int totalPlanets = 0;
        double totalVotes = 0;
        for (Race race : team.getRaces()) {
            totalPop += race.totalPop();
            totalInd += race.totalInd();
            totalPlanets += race.getOwnedPlanets().size();
            totalVotes += race.votes();
        }
        elem.setAttribute("population", Utils.toReport(totalPop));
        elem.setAttribute("industry", Utils.toReport(totalInd));
        elem.setAttribute("planets", String.valueOf(totalPlanets));
        elem.setAttribute("votes", Utils.toReport(totalVotes));
        for (Race race : team.getRaces()) saveRaceStatisticsXML(race, elem);
    }

    private static void saveRaceStatisticsXML(Race race, org.w3c.dom.Element parent) {
        if (!race.living) return;
        org.w3c.dom.Element elem = XmlHelper.createElement(parent, "race");
        elem.setAttribute("name", race.getName());
        TechBlock techs = race.getTechBlock();
        elem.setAttribute("drive", Utils.toReport(techs.drive()));
        elem.setAttribute("weapons", Utils.toReport(techs.weapons()));
        elem.setAttribute("shields", Utils.toReport(techs.shields()));
        elem.setAttribute("cargo", Utils.toReport(techs.cargo()));
        elem.setAttribute("population", Utils.toReport(race.totalPop()));
        elem.setAttribute("industry", Utils.toReport(race.totalInd()));
        elem.setAttribute("planets", String.valueOf(race.totalPlanets()));
        elem.setAttribute("votes", Utils.toReport(race.getGalaxy().receivedVotes(race)));
    }
}
