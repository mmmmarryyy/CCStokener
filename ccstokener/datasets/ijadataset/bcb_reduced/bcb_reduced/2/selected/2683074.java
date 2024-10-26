package aleksandar.djuric.online;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import aleksandar.djuric.common.TimeManagement;
import aleksandar.djuric.entities.Attribute;
import aleksandar.djuric.entities.Player;
import aleksandar.djuric.entities.Team;

public class FIDataFetcher {

    private static final String fidUrl = "http://footballidentity.com";

    private static final String myLeaguePage = "/WorldAndCompetition" + "/StatisticsAndAwards/LeagueSpace.aspx";

    private static final String teamPage = "/Team/TeamGeneral/General/U71.aspx?id=";

    private static final String teamPlayersPage = "/Team/TeamGeneral/General/TeamSummaryPlayers.aspx?id=";

    private static final String myTeamPlayersPage = "/Team/TeamGeneral/TeamSpacePlayers.aspx";

    private static final String playerPage = "/Player/PlayerGeneral/General/U59.aspx?id=";

    private static final String matchStatsPage = "/WorldAndCompetition/StatisticsAndAwards/U84Players.aspx?id=";

    private DefaultHttpClient client;

    private ResponseHandler<String> responseHandler;

    private BufferedReader in;

    private String page;

    private String tmp;

    private HttpGet get;

    private String username;

    private String password;

    private int myId;

    private String myLeague;

    private String viewState;

    private String eventValidation;

    public FIDataFetcher(String username, String password) {
        ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager();
        connectionManager.setDefaultMaxPerRoute(20);
        client = new DefaultHttpClient(connectionManager);
        responseHandler = new BasicResponseHandler();
        this.username = username;
        this.password = password;
    }

    public int login() {
        int returnValue = -1;
        try {
            get = new HttpGet(fidUrl + "/BasicWebsite/LogOnAndPresentation/U1.aspx");
            page = client.execute(get, responseHandler);
            in = new BufferedReader(new StringReader(page));
            tmp = "";
            while ((tmp = in.readLine()) != null) {
                if (tmp.contains("__VIEWSTATE")) {
                    viewState = tmp.substring(tmp.lastIndexOf("value=\"") + 7, tmp.lastIndexOf("\""));
                } else if (tmp.contains("__EVENTVALIDATION")) {
                    eventValidation = tmp.substring(tmp.lastIndexOf("value=\"") + 7, tmp.lastIndexOf("\""));
                }
            }
            HttpPost httpost = new HttpPost(fidUrl + "/BasicWebsite/LogOnAndPresentation/U1.aspx");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("__VIEWSTATE", viewState));
            nvps.add(new BasicNameValuePair("__EVENTVALIDATION", eventValidation));
            nvps.add(new BasicNameValuePair("logOnMain$UserName", username));
            nvps.add(new BasicNameValuePair("logOnMain$Password", password));
            nvps.add(new BasicNameValuePair("logOnMain$LoginButton", ""));
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = client.execute(httpost);
            if (response.getStatusLine().toString().equals("HTTP/1.1 302 Found")) returnValue = 0; else if (response.getStatusLine().toString().equals("HTTP/1.1 200 OK")) returnValue = 1; else returnValue = 2;
            EntityUtils.consume(response.getEntity());
        } catch (Exception e) {
            e.printStackTrace();
            returnValue = -1;
        }
        return returnValue;
    }

    public boolean getPlayer(Player player) {
        try {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            HttpGet get = new HttpGet(fidUrl + playerPage + player.getId());
            String page = client.execute(get, responseHandler);
            BufferedReader in = new BufferedReader(new StringReader(page));
            String tmp = "";
            Attribute[] attribs = new Attribute[Player.N_ATTRIBS];
            int attribNo = 0;
            player.setMyChar(false);
            while ((tmp = in.readLine()) != null) {
                if (tmp.contains("Footballidentity - Home Page")) {
                    in.close();
                    return false;
                } else if (tmp.contains("Bank Account Balance")) {
                    player.setMyChar(true);
                } else if (tmp.contains("class=\"dxgvIndentCell dxgv\"") || tmp.contains("<td class=\"cellLeft\">")) {
                    for (attribNo = 0; attribNo < attribs.length; attribNo++) {
                        if (tmp.contains(">" + Player.attribNames[attribNo] + "<")) {
                            tmp = extractValue(tmp);
                            try {
                                double value = Double.parseDouble(tmp);
                                attribs[attribNo] = new Attribute(value);
                            } catch (NumberFormatException e) {
                                attribs[attribNo] = new Attribute(tmp);
                            }
                            break;
                        }
                    }
                } else if (tmp.contains("M_M_M_C_C_C_labelCharacterCreatedDateValue")) {
                    player.setBirthDate(extractValue(tmp));
                }
            }
            player.setAttribs(attribs);
            in.close();
            if (player.isMyChar()) get = new HttpGet(fidUrl + "/Player/PlayerDevelopment/PlayerSkills.aspx"); else get = new HttpGet(fidUrl + "/WorldAndCompetition/StatisticsAndAwards" + "/WorldPlayerSkills.aspx?id=" + player.getId());
            page = client.execute(get, responseHandler);
            in = new BufferedReader(new StringReader(page));
            tmp = "";
            String viewState = "";
            String eventValidation = "";
            while ((tmp = in.readLine()) != null) {
                if (tmp.contains("__VIEWSTATE")) {
                    viewState = tmp.substring(tmp.lastIndexOf("value=\"") + 7, tmp.lastIndexOf("\""));
                } else if (tmp.contains("__EVENTVALIDATION")) {
                    eventValidation = tmp.substring(tmp.lastIndexOf("value=\"") + 7, tmp.lastIndexOf("\""));
                } else {
                    int value = 0;
                    for (int i = 0; i < Player.specialAbilities.length; i++) {
                        if (tmp.contains("\">" + Player.specialAbilities[i])) {
                            while (!(tmp = in.readLine()).contains("SARating_A")) ;
                            value = tmp.split("filledRatingStar").length - 1;
                            if (value > 0) player.addSpecialAbility(Player.specialAbilities[i], value);
                        }
                    }
                }
            }
            in.close();
            HttpPost httpost = null;
            if (player.isMyChar()) httpost = new HttpPost(fidUrl + "/Player/PlayerDevelopment/PlayerSkills.aspx"); else httpost = new HttpPost(fidUrl + "/WorldAndCompetition/StatisticsAndAwards" + "/WorldPlayerSkills.aspx?id=" + player.getId());
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("__VIEWSTATE", viewState));
            nvps.add(new BasicNameValuePair("__EVENTVALIDATION", eventValidation));
            nvps.add(new BasicNameValuePair("__CALLBACKID", "M$M$M$C$C$C$playerSkillsControl$gridFootballerSpecialAbilities"));
            nvps.add(new BasicNameValuePair("__CALLBACKPARAM", "c0:GB|20;12|PAGERONCLICK3|PN1;"));
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            page = client.execute(httpost, responseHandler);
            String[] array = page.split("class=");
            int value = 0;
            int abilityNo = -1;
            int starCounter = 0;
            for (int i = 0; i < array.length; i++) {
                for (int j = 0; j < Player.specialAbilities.length; j++) {
                    if (array[i].contains(Player.specialAbilities[j])) {
                        abilityNo = j;
                    }
                }
                if (array[i].contains("filledRatingStar")) {
                    value++;
                    starCounter++;
                } else if (array[i].contains("emptyRatingStar")) starCounter++;
                if (starCounter > 4) {
                    if (value > 0) player.addSpecialAbility(Player.specialAbilities[abilityNo], value);
                    value = 0;
                    abilityNo = -1;
                    starCounter = 0;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String extractValue(String s) {
        return s.substring(s.lastIndexOf("\">") + 2, s.indexOf("<", s.lastIndexOf("\">")));
    }

    public LinkedList<Team> getTeams() {
        LinkedList<Team> teams = new LinkedList<Team>();
        try {
            get = new HttpGet(fidUrl + myLeaguePage);
            page = client.execute(get, responseHandler);
            in = new BufferedReader(new StringReader(page));
            tmp = "";
            String name = "";
            int id = 0;
            String tmpLeagueName = null;
            while ((tmp = in.readLine()) != null) {
                if (tmp.contains("<label class=\"leagueInfoLabel\">")) tmpLeagueName = extractValue(tmp); else if (tmp.contains(teamPage)) {
                    tmp = tmp.substring(tmp.indexOf("href") + 6, tmp.indexOf("</a></td>"));
                    id = Integer.parseInt(tmp.substring(tmp.indexOf("=") + 1, tmp.indexOf("\"")));
                    name = tmp.substring(tmp.indexOf(">") + 1);
                    teams.add(new Team(name, id));
                }
            }
            in.close();
            if (tmpLeagueName != null) {
                myLeague = tmpLeagueName;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return teams;
    }

    public LinkedList<Player> getPlayers(int id) {
        LinkedList<Player> players = new LinkedList<Player>();
        try {
            if (id == myId) get = new HttpGet(fidUrl + myTeamPlayersPage); else get = new HttpGet(fidUrl + teamPlayersPage + id);
            page = client.execute(get, responseHandler);
            in = new BufferedReader(new StringReader(page));
            int number = 0;
            int pId = 0;
            String name = "";
            String position = "";
            String aPosition = "";
            boolean online = false;
            Date lastOnline = null;
            int age = 0;
            double value = 0;
            tmp = "";
            while ((tmp = in.readLine()) != null) {
                if (tmp.contains("Footballidentity - Team Space")) {
                    myId = id;
                    players = getPlayers(id);
                    break;
                } else if (tmp.contains("/Player/PlayerGeneral/General/U59.aspx?id=")) {
                    String[] s = tmp.split("[<>\"]");
                    int i = 0;
                    while (!s[i++].equals("")) ;
                    number = Integer.parseInt(s[i]);
                    while (!s[i++].contains("name=")) ;
                    online = checkOnlineStatus(Integer.parseInt(s[i]));
                    lastOnline = getLastOnline(Integer.parseInt(s[i]));
                    while (!s[++i].contains("PlayerGeneral/General/U59.aspx?id=")) ;
                    pId = Integer.parseInt(s[i].substring(s[i].indexOf("=") + 1));
                    i += 2;
                    name = s[i];
                    while (!s[++i].matches("[A-Z]+")) ;
                    position = s[i];
                    while (!s[++i].matches("^[A-Z][A-Z].*")) ;
                    aPosition = s[i];
                    while (!s[i++].equals("left")) ;
                    while (!s[i++].equals("")) ;
                    age = Integer.parseInt(s[i]);
                    while (!s[i++].equals("left")) ;
                    while (!s[i++].equals("")) ;
                    value = Double.valueOf(s[i]);
                    players.add(new Player(number, pId, name, position, aPosition, age, value));
                    players.getLast().setOnline(online);
                    players.getLast().setLastOnline(lastOnline);
                }
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return players;
    }

    public String getMyLeague() {
        return myLeague;
    }

    public void setMyLeague(String myLeague) {
        this.myLeague = myLeague;
    }

    public boolean checkOnlineStatus(int id) {
        boolean result = false;
        get = new HttpGet("http://chat.footballidentity.com/Shared/GetStatus.ashx?oid=" + id);
        try {
            String page = client.execute(get, responseHandler);
            if (page.contains("1")) result = true;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public Date getLastOnline(int id) {
        Date result = null;
        get = new HttpGet("http://chat.footballidentity.com/Shared/GetLastActivity.ashx?oid=" + id);
        try {
            String page = client.execute(get, responseHandler);
            return TimeManagement.getTM().getDate(page);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void updateOnlineStatuses(Team team) {
        try {
            if (team.getId() == myId) get = new HttpGet(fidUrl + myTeamPlayersPage); else get = new HttpGet(fidUrl + teamPlayersPage + team.getId());
            page = client.execute(get, responseHandler);
            in = new BufferedReader(new StringReader(page));
            String name = "";
            boolean online = false;
            Date lastOnline = null;
            tmp = "";
            while ((tmp = in.readLine()) != null) {
                if (tmp.contains("Footballidentity - Team Space")) {
                    myId = team.getId();
                    updateOnlineStatuses(team);
                    break;
                } else if (tmp.contains("/Player/PlayerGeneral/General/U59.aspx?id=")) {
                    int offset = 0;
                    String[] s = tmp.split("[<>\"]");
                    if (s[18].equalsIgnoreCase("Click to chat")) offset += 4;
                    if (offset != 0) {
                        online = checkOnlineStatus(Integer.parseInt(s[offset + 18]));
                        lastOnline = getLastOnline(Integer.parseInt(s[offset + 18]));
                    } else {
                        online = checkOnlineStatus(Integer.parseInt(s[20]));
                        lastOnline = getLastOnline(Integer.parseInt(s[20]));
                    }
                    name = s[offset + 30];
                    Player p = team.getPlayer(name);
                    if (p != null) {
                        p.setOnline(online);
                        p.setLastOnline(lastOnline);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getMatchStats(int id) {
        String homeOrAway = "home";
        String teamName = "";
        StringBuilder stats = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            get = new HttpGet(fidUrl + matchStatsPage + id + "&tp=" + homeOrAway);
            try {
                page = client.execute(get, responseHandler);
                in = new BufferedReader(new StringReader(page));
                tmp = "";
                String data = "";
                while ((tmp = in.readLine()) != null) {
                    if (tmp.contains("TeamCaption")) {
                        int begin = tmp.indexOf("\">") + 2;
                        int end = tmp.indexOf("<", begin);
                        teamName = tmp.substring(begin, end);
                    }
                    if (tmp.contains("/Player/PlayerGeneral/General/U59.aspx?id=") && tmp.contains("StatisticsDefence")) {
                        stats.append(teamName + "_");
                        int position = tmp.indexOf("/Player/PlayerGeneral/General/U59.aspx?id=");
                        int beginIndex = 0;
                        int endIndex = 0;
                        while ((beginIndex = tmp.indexOf(">", position) + 1) != 0) {
                            position = beginIndex + 1;
                            if ((endIndex = tmp.indexOf("<", beginIndex)) != -1) {
                                data = tmp.substring(beginIndex, endIndex);
                                if (!data.equals("")) {
                                    if (tmp.contains("StatisticsDefence")) stats.append(data + "_");
                                }
                            } else {
                                if (tmp.contains("StatisticsDefence")) stats.append("\n");
                            }
                        }
                    }
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            homeOrAway = "away";
        }
        return stats.toString();
    }

    public String attributeTraining(int attribId) {
        String returnValue = null;
        try {
            get = new HttpGet(fidUrl + "/Player/PlayerGeneral/UA1.aspx");
            page = client.execute(get, responseHandler);
            in = new BufferedReader(new StringReader(page));
            tmp = "";
            while ((tmp = in.readLine()) != null) {
                if (tmp.contains("__VIEWSTATE")) {
                    viewState = tmp.substring(tmp.lastIndexOf("value=\"") + 7, tmp.lastIndexOf("\""));
                } else if (tmp.contains("__EVENTVALIDATION")) {
                    eventValidation = tmp.substring(tmp.lastIndexOf("value=\"") + 7, tmp.lastIndexOf("\""));
                }
            }
            HttpPost httpost = new HttpPost(fidUrl + "/Player/PlayerGeneral/UA1.aspx");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("__VIEWSTATE", viewState));
            nvps.add(new BasicNameValuePair("__EVENTVALIDATION", eventValidation));
            nvps.add(new BasicNameValuePair("__EVENTTARGET", "M$M$M$C$C$C$headerPanel$trainType"));
            nvps.add(new BasicNameValuePair("M$M$M$C$C$C$headerPanel$trainType", "Attribute training"));
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = client.execute(httpost);
            if (!response.getStatusLine().toString().equals("HTTP/1.1 200 OK")) {
                EntityUtils.consume(response.getEntity());
                throw new Exception("Something got wrong (" + response.getStatusLine().toString() + ").");
            }
            String tmp = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("__CALLBACKID", "M$M$M$C$C$C$headerPanel$gridViewAttributeTraining"));
            nvps.add(new BasicNameValuePair("__CALLBACKPARAM", "c0:GB|25;14|CUSTOMCALLBACK6|" + attribId + ";"));
            while ((tmp = in.readLine()) != null) {
                if (tmp.contains("__VIEWSTATE")) {
                    nvps.add(new BasicNameValuePair("__VIEWSTATE", tmp.split("\"")[7]));
                } else if (tmp.contains("CallbackState")) {
                    nvps.add(new BasicNameValuePair("M$M$M$C$C$C$headerPanel$" + "gridViewAttributeTraining$CallbackState", tmp.split("\"")[7]));
                } else if (tmp.contains("__EVENTVALIDATION")) {
                    nvps.add(new BasicNameValuePair("__EVENTVALIDATION", tmp.split("\"")[7]));
                }
            }
            nvps.add(new BasicNameValuePair("M$M$M$C$C$C$headerPanel$trainType", "Attribute training"));
            EntityUtils.consume(response.getEntity());
            httpost = new HttpPost(fidUrl + "/Player/PlayerGeneral/UA1.aspx");
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            response = client.execute(httpost);
            if (!response.getStatusLine().toString().equals("HTTP/1.1 200 OK")) {
                EntityUtils.consume(response.getEntity());
                throw new Exception("Something got wrong.");
            }
            EntityUtils.consume(response.getEntity());
            nvps.set(0, new BasicNameValuePair("__CALLBACKID", "M$M$M$C$C$C$headerPanel"));
            nvps.set(1, new BasicNameValuePair("__CALLBACKPARAM", "c0:"));
            httpost = new HttpPost(fidUrl + "/Player/PlayerGeneral/UA1.aspx");
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            response = client.execute(httpost);
            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            tmp = in.readLine();
            String[] array = tmp.split("(\">)|(</)");
            if (array.length >= 12) returnValue = array[11];
            if (returnValue.contains("label")) if (array.length >= 17) returnValue = array[16] + ".";
            EntityUtils.consume(response.getEntity());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnValue;
    }

    public void shutDownConnection() {
        client.getConnectionManager().shutdown();
    }

    public static void main(String[] args) {
        FIDataFetcher f = new FIDataFetcher("xxx", "xxx");
        System.out.println(f.login());
        System.out.println(f.attributeTraining(87));
    }

    public static void main1(String[] args) {
        FIDataFetcher f = null;
        if (args.length == 2) f = new FIDataFetcher(args[0], args[1]); else {
            System.out.println("Incorect number of arguments provided.");
            System.out.println("Usage: java -jar fidDataFetcher.jar username password");
            System.exit(4);
        }
        PrintWriter out = null;
        File dir = null;
        Scanner in = null;
        int exit = -1;
        int teamNumber = -1;
        if (f.login() == 0) System.out.println("Successfully loged on to FID with username: " + args[0]); else {
            System.out.println("Invalid username or password.");
            System.exit(1);
        }
        LinkedList<Team> teams = f.getTeams();
        while (exit != 1) {
            exit = -1;
            if (teams != null) {
                System.out.println("Teams from " + f.getMyLeague() + " :");
                for (int i = 0; i < teams.size(); i++) {
                    System.out.println((i + 1) + ". " + teams.get(i));
                }
                System.out.println();
                teamNumber = -1;
                while (teamNumber == -1) {
                    System.out.println("Enter a team number whoose " + "players you want to download,\n" + "or 0 if u want to download all teams.");
                    System.out.println();
                    try {
                        in = new Scanner(System.in);
                        teamNumber = in.nextInt();
                        if (teamNumber < 0 || teamNumber > teams.size()) {
                            throw new Exception();
                        }
                    } catch (Exception e) {
                        System.out.println("Wrong choice, please try again.");
                        teamNumber = -1;
                    }
                }
                System.out.println("Your choice was: " + teamNumber);
                dir = new File(f.myLeague);
                if (dir.mkdir() == true) System.out.println("Created directory:\n" + dir.getAbsolutePath());
            } else {
                System.out.println("Error fetching teams.");
                System.exit(2);
            }
            for (int i = 0; i < teams.size(); i++) {
                if ((teamNumber - 1) == i || teamNumber == 0) try {
                    Team curTeam = teams.get(i);
                    out = new PrintWriter(new File(dir.getName() + "/" + curTeam.getName() + ".txt"), "UTF-8");
                    out.println(curTeam.getName());
                    out.println(curTeam.getId());
                    out.println();
                    LinkedList<Player> players = null;
                    System.out.println("Fetching players from: " + curTeam.getName());
                    players = f.getPlayers(curTeam.getId());
                    if (players != null) {
                        curTeam.addPlayers(players);
                        for (int j = 0; j < players.size(); j++) {
                            Player curPlayer = players.get(j);
                            if (f.getPlayer(curPlayer) == true) {
                                out.println(curPlayer.toStringFull());
                                out.println();
                                out.println("--------------------------" + "------------------------------");
                                out.println();
                            } else {
                                System.out.println("Error fetching player " + curPlayer.getName());
                                out.println("Error fetching player " + curPlayer.getName());
                                out.println();
                                out.println("--------------------------" + "------------------------------");
                                out.println();
                            }
                        }
                    } else {
                        System.out.println("Error fetching players from: " + curTeam.getName());
                    }
                    out.close();
                } catch (FileNotFoundException e) {
                } catch (UnsupportedEncodingException e) {
                }
            }
            System.out.println();
            System.out.println("Finished operation.");
            while (exit != 0 && exit != 1) {
                in = new Scanner(System.in);
                System.out.println("Type 1 to exit, " + "or 0 to return to team choice.");
                try {
                    exit = in.nextInt();
                } catch (Exception e) {
                    System.out.println("Wrong choice. Please try again.");
                    exit = -1;
                }
            }
        }
        in.close();
        f.shutDownConnection();
    }
}
