package dsrwebserver.tables;

import java.awt.Point;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Scanner;
import ssmith.dbs.MySQLConnection;
import ssmith.dbs.SQLFuncs;
import ssmith.html.HTMLFunctions;
import ssmith.lang.Dates;
import ssmith.util.MemCache;
import ssmith.util.MyList;
import dsr.AppletMain;
import dsrwebserver.DSRWebServer;
import dsrwebserver.missions.AbstractMission;
import dsrwebserver.pages.dsr.leaguetable;

public final class LoginsTable extends AbstractTable {

    private static MemCache mc_display_names = new MemCache(Dates.DAY);

    public static void CreateTable(MySQLConnection dbs) throws SQLException {
        if (dbs.doesTableExist("Logins") == false) {
            dbs.runSQL("CREATE TABLE Logins (LoginID INTEGER AUTO_INCREMENT KEY, Login VARCHAR(128), Pwd VARCHAR(50), LoginCode VARCHAR(32), LastLoginDate DATETIME, PrevLoginDate DATETIME, DisplayName VARCHAR(128), Admin TINYINT, TotalTurns INTEGER, TotalDaysTakingTurns INTEGER, EmailForumReplies TINYINT, HasBeenEmailedSinceLastLogin TINYINT, Location VARCHAR(256), AboutMe VARCHAR(4096), TotalConcedes SMALLINT, Website VARCHAR(256), EmailOnTurn TINYINT, KilledOwnUnitsCount INTEGER, CampTeamName VARCHAR(128), ImportedIntoForums TINYINT, OptedOutOfEmails TINYINT, DateLastMktEmailRcvd DATETIME, Disabled TINYINT, EmailValidated TINYINT, CampInactivityPoints INTEGER, TimeLastInactivityPointsAdded DATETIME, OnHolidayText VARCHAR(1024), HasAndroid TINYINT, DateCreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
        if (dbs.doesColumnExist("Logins", "CampaignCredits") == false) {
            dbs.runSQL("ALTER TABLE Logins ADD CampaignCredits INTEGER");
        }
        if (dbs.doesColumnExist("Logins", "DateLastAwardAwarded") == false) {
            dbs.runSQL("ALTER TABLE Logins ADD DateLastAwardAwarded DATETIME");
        }
        if (dbs.doesColumnExist("Logins", "FactionID") == false) {
            dbs.runSQL("ALTER TABLE Logins ADD FactionID INTEGER");
        }
        if (dbs.doesIndexExist("Logins", "idx_Login_Pwd") == false) {
            dbs.runSQL("CREATE INDEX idx_Login_Pwd ON Logins(Login, Pwd)");
        }
    }

    public LoginsTable(MySQLConnection sqldbs) throws SQLException {
        super(sqldbs, "Logins", "LoginID");
    }

    /**
	 * This will either create the login, or return a warning as to why it could not be created.
	 * @param pwd
	 * @param displayname
	 * @param email
	 * @return
	 * @throws SQLException
	 */
    public String createLogin(String pwd, String displayname, String email) throws SQLException {
        if (email.trim().length() == 0 || pwd.trim().length() == 0 || displayname.length() == 0) {
            return "Please enter all the required details.";
        } else if (dbs.getResultSet("SELECT * FROM Logins WHERE Login = " + SQLFuncs.s2sql(email)).next()) {
            return "Sorry, one of those details has been taken.";
        } else if (dbs.getResultSet("SELECT * FROM Logins WHERE DisplayName = " + SQLFuncs.s2sql(displayname)).next()) {
            return "Sorry, one of those details has been taken.";
        } else if (displayname.indexOf("@") >= 0) {
            return "Sorry, please do not have an '@' in your name.";
        }
        int id = dbs.RunIdentityInsert_Syncd("INSERT INTO Logins (Pwd, LoginCode, DisplayName, Login, TotalTurns, TotalDaysTakingTurns, Location, AboutMe, TotalConcedes, EmailForumReplies, Website, EmailOnTurn) VALUES (" + SQLFuncs.s2sql(pwd.trim()) + ", " + System.currentTimeMillis() + ", " + SQLFuncs.s2sql(displayname) + ", " + SQLFuncs.s2sql(email) + ", 0, 0, '', '', 0, 1, '', 1)");
        try {
            WebsiteEventsTable.AddRec(dbs, " New login '" + displayname + "' has been registered.", -1);
        } catch (Exception ex) {
            DSRWebServer.HandleError(ex, true);
        }
        try {
            URL url = new URL("http://127.0.0.3:8081/importusers.php");
            Scanner in = new Scanner(url.openStream());
            while (in.hasNextLine()) {
                String line = in.nextLine();
                System.out.println(line);
            }
            in.close();
        } catch (Exception ex) {
            DSRWebServer.HandleError(ex, true);
        }
        this.selectRow(id);
        return "";
    }

    /**
	 * Pass a blank pwd to not change it.
	 * @param pwd
	 * @param displayname
	 * @param email
	 * @return
	 * @throws SQLException
	 */
    public String updateLogin(String pwd, String displayname, String email, String location, String aboutme, String website, boolean email_on_turn, String camp_team_name, String on_holiday) throws SQLException {
        if (email.trim().length() == 0 || displayname.length() == 0) {
            return "Please enter all the required details.";
        } else if (dbs.getResultSet("SELECT * FROM Logins WHERE Login = " + SQLFuncs.s2sql(email) + " AND LoginID <> " + this.getLoginID()).next()) {
            return "Sorry, one of those details has been taken.";
        } else if (dbs.getResultSet("SELECT * FROM Logins WHERE DisplayName = " + SQLFuncs.s2sql(displayname) + " AND LoginID <> " + this.getLoginID()).next()) {
            return "Sorry, one of those details has been taken.";
        }
        if (pwd.length() == 0) {
            pwd = this.getPassword();
        }
        dbs.runSQL("UPDATE Logins SET Pwd = " + SQLFuncs.s2sql(pwd.trim()) + ",  DisplayName = " + SQLFuncs.s2sql(displayname) + ", Login = " + SQLFuncs.s2sql(email) + ", Location = " + SQLFuncs.s2sql(location) + ", AboutMe = " + SQLFuncs.s2sql(aboutme) + ", Website = " + SQLFuncs.s2sql(website) + ", EmailOnTurn = " + SQLFuncs.b201(email_on_turn) + ", CampTeamName = " + SQLFuncs.s2sql(camp_team_name) + ", OnHolidayText = " + SQLFuncs.s2sql(on_holiday) + " WHERE LoginID = " + this.getLoginID());
        this.selectRow(this.getLoginID());
        return "";
    }

    public void setLastLoginDate() throws SQLException {
        dbs.runSQL("UPDATE Logins SET LastLoginDate = " + SQLFuncs.d2sql(new Date(), true) + " WHERE LoginID = " + this.getLoginID());
        this.refreshData();
    }

    public void setEmailAddressValidated() throws SQLException {
        dbs.runSQL("UPDATE Logins SET EmailValidated = 1 WHERE LoginID = " + this.getLoginID());
        this.refreshData();
    }

    public static boolean DoesLoginExistAndEnabled(MySQLConnection dbs, String login, String pwd) throws SQLException {
        ResultSet rs = dbs.getResultSet("SELECT * FROM Logins WHERE (Login = " + SQLFuncs.s2sql(login) + " OR DisplayName = " + SQLFuncs.s2sql(login) + ") AND Pwd = " + SQLFuncs.s2sql(pwd) + " AND COALESCE(Disabled, 0) <> 1");
        return rs.next();
    }

    public boolean selectUser(String login, String pwd) throws SQLException {
        rs = dbs.getResultSet("SELECT * FROM Logins WHERE (Login = " + SQLFuncs.s2sql(login) + " OR DisplayName = " + SQLFuncs.s2sql(login) + ") AND Pwd = " + SQLFuncs.s2sql(pwd));
        has_rows = rs.next();
        return has_rows;
    }

    /**
	 * This is used when the user has forgotten their password.
	 * @param login
	 * @return
	 * @throws SQLException
	 */
    public boolean selectUserByEmailAddress(String login) throws SQLException {
        rs = dbs.getResultSet("SELECT * FROM Logins WHERE Login = " + SQLFuncs.s2sql(login));
        has_rows = rs.next();
        return has_rows;
    }

    public boolean selectUserByDisplayName(String name) throws SQLException {
        rs = dbs.getResultSet("SELECT * FROM Logins WHERE DisplayName = " + SQLFuncs.s2sql(name));
        has_rows = rs.next();
        return has_rows;
    }

    public boolean checkPassword(String pwd) throws SQLException {
        return rs.getString("pwd").equalsIgnoreCase(pwd);
    }

    public String getDisplayName_Enc(boolean link) throws SQLException {
        if (link) {
            return "<a href=\"/dsr/playerspublicpage.cls?loginid=" + this.getID() + "\">" + HTMLFunctions.HTMLEncode(rs.getString("DisplayName")) + "</a>";
        } else {
            return HTMLFunctions.HTMLEncode(rs.getString("DisplayName"));
        }
    }

    public static String GetDisplayName(MySQLConnection dbs, int loginid) throws SQLException {
        if (mc_display_names.containsKey(loginid)) {
            return mc_display_names.get(loginid).toString();
        } else {
            String name = dbs.getScalarAsString("SELECT DisplayName FROM Logins WHERE LoginID = " + loginid);
            mc_display_names.put(loginid, name);
            return name;
        }
    }

    public String getLocation() throws SQLException {
        return rs.getString("Location");
    }

    public boolean isDisabled() throws SQLException {
        return rs.getInt("Disabled") > 0;
    }

    public String getWebsite() throws SQLException {
        String website = rs.getString("Website");
        if (website != null) {
            if (website.equalsIgnoreCase("null") == false) {
                if (website.toLowerCase().startsWith("http://") == false) {
                    website = "http://" + website;
                }
                return website;
            }
        }
        return "";
    }

    public String getCampTeamName() throws SQLException {
        String s = rs.getString("CampTeamName");
        if (s != null) {
            if (s.equalsIgnoreCase("null") == false) {
                return s;
            }
        }
        return "";
    }

    public String getAboutMe() throws SQLException {
        return rs.getString("AboutMe");
    }

    public String getOnHolidayText() throws SQLException {
        String s = rs.getString("OnHolidayText");
        if (s != null) {
            if (s.equalsIgnoreCase("null") == false) {
                return s;
            }
        }
        return "";
    }

    public static String GetOnHolidayText(MySQLConnection dbs, int loginid) throws SQLException {
        String s = dbs.getScalarAsString("SELECT OnHolidayText FROM Logins WHERE LoginID = " + loginid);
        if (s != null) {
            if (s.equalsIgnoreCase("null") == false) {
                return s;
            }
        }
        return "";
    }

    public String getDisplayName() throws SQLException {
        return LoginsTable.GetDisplayName(dbs, this.getID());
    }

    public String getLoginCode() throws SQLException {
        return rs.getString("LoginCode");
    }

    public boolean isAdmin() throws SQLException {
        return rs.getInt("Admin") == 1;
    }

    public int getTotalGamesFinished() throws SQLException {
        return GetTotalGamesFinished(dbs, this.getLoginID(), -1);
    }

    public static int GetTotalGamesFinished(MySQLConnection dbs, int loginid, int mission) throws SQLException {
        String sql = "SELECT Count(*) FROM Games WHERE GameStatus = " + GamesTable.GS_FINISHED + " AND " + AbstractTable.GetPlayerSubQuery(loginid) + " AND Mission <> " + AbstractMission.PRACTISE_MISSION;
        if (mission > 0) {
            sql = sql + " AND Mission = " + mission;
        }
        return dbs.getScalarAsInt(sql);
    }

    public int getGamesExperience() throws SQLException {
        return Math.max(0, this.getTotalGamesFinished() - getTotalConcedes());
    }

    public boolean canPlayInCampaign() throws SQLException {
        return getGamesExperience() >= DSRWebServer.MIN_CAMP_GAMES;
    }

    public int getTotalCurrentGames(boolean inc_practise) throws SQLException {
        String sql = "SELECT Count(*) FROM Games WHERE " + AbstractTable.GetPlayerSubQuery(this.getID()) + " AND GameStatus < " + GamesTable.GS_FINISHED;
        if (inc_practise == false) {
            sql = sql + " AND Mission <> " + AbstractMission.PRACTISE_MISSION;
        }
        return dbs.getScalarAsInt(sql);
    }

    public int getTotalVictories(boolean only_advanced, boolean ignore_concedes, boolean inc_practise, int days_ago, int mission, boolean include_camp) throws SQLException {
        String sql = "SELECT Count(*) FROM Games WHERE ";
        if (only_advanced) {
            sql = sql + " COALESCE(CanHearEnemies, 0) = 1 AND ";
        }
        sql = sql + "(";
        for (int i = 1; i <= 4; i++) {
            sql = sql + "((Player" + i + "ID = " + this.getID() + " AND WinningSide = " + i + ") OR ";
            sql = sql + "(Player" + i + "ID = " + this.getID() + " AND WinningSide2 = " + i + ")) ";
            if (i < 4) {
                sql = sql + " OR ";
            }
        }
        sql = sql + ") AND GameStatus = " + GamesTable.GS_FINISHED;
        if (inc_practise == false) {
            sql = sql + " AND COALESCE(GameType, 0) = " + GameRequestsTable.GS_NORMAL;
        }
        if (ignore_concedes) {
            sql = sql + " AND (WinType != " + GamesTable.WIN_DRAW_MUTUAL_CONCEDE + " AND WinType != " + GamesTable.WIN_OPPONENT_CONCEDED + ")";
        }
        if (days_ago > 0) {
            sql = sql + " AND DATEDIFF(CurDate(), DateFinished) <= " + days_ago;
        }
        if (mission > 0) {
            sql = sql + " AND Mission = " + mission;
        }
        if (include_camp == false) {
            sql = sql + " AND COALESCE(CampGame, 0) = 0";
        }
        return dbs.getScalarAsInt(sql);
    }

    public int getPoints() throws SQLException {
        String QUAL = leaguetable.GetQual();
        int calc_wins = 0;
        for (int s = 1; s <= 4; s++) {
            calc_wins += dbs.getScalarAsInt("SELECT SUM(PointsForWin" + s + ") FROM Games WHERE " + QUAL + " AND (WinningSide = " + s + " OR WinningSide2 = " + s + ") AND Player" + s + "ID = " + rs.getInt("LoginID"));
        }
        int calc_draws = dbs.getScalarAsInt("SELECT Count(*) FROM Games WHERE " + QUAL + " AND WinningSide < 1 AND " + AbstractTable.GetPlayerSubQuery(rs.getInt("LoginID")));
        return (calc_draws * leaguetable.DRAW_POINTS) + (calc_wins);
    }

    public Point getMinAndMaxOppRange(int pcent) throws SQLException {
        int our_pts = this.getPoints();
        int diff = (our_pts * pcent) / (100);
        Point p = new Point();
        p.x = our_pts - diff;
        if (p.x < 0) {
            p.x = 0;
        }
        p.y = our_pts + diff;
        return p;
    }

    public int getTotalDefeats() throws SQLException {
        return this.getTotalGamesFinished() - this.getTotalVictories(false, false, true, 0, -1, true) - this.getTotalDraws();
    }

    public int getTotalDraws() throws SQLException {
        String sql = "SELECT Count(*) FROM Games WHERE ";
        for (int i = 1; i <= 4; i++) {
            sql = sql + "(Player" + i + "ID = " + this.getID() + " AND WinningSide <= 0)";
            if (i < 4) {
                sql = sql + " OR ";
            }
        }
        sql = sql + " AND GameStatus = " + GamesTable.GS_FINISHED;
        return dbs.getScalarAsInt(sql);
    }

    public int getTotalConcedes() throws SQLException {
        return rs.getInt("TotalConcedes");
    }

    public int getTotalForumPosts() throws SQLException {
        return dbs.getScalarAsInt("SELECT Count(*) FROM ForumPostings WHERE UserID = " + this.getID());
    }

    public int getTotalTurns() throws SQLException {
        return rs.getInt("TotalTurns");
    }

    public Date getDateJoined() throws SQLException {
        return rs.getTimestamp("DateCreated");
    }

    public Date getLastLoginDate() throws SQLException {
        return rs.getDate("LastLoginDate");
    }

    public int getTotalDaysTakingTurns() throws SQLException {
        return rs.getInt("TotalDaysTakingTurns");
    }

    public static String GetDisplayNames_Enc(MySQLConnection dbs, MyList<Integer> loginids, boolean link) throws SQLException {
        StringBuffer str = new StringBuffer();
        for (Integer i : loginids) {
            str.append(GetDisplayName_Enc(dbs, i, link));
            str.append(", ");
        }
        str.delete(str.length() - 2, str.length());
        return str.toString();
    }

    public static String GetDisplayName_Enc(MySQLConnection dbs, int loginid, boolean link) throws SQLException {
        if (loginid > 0) {
            if (link) {
                return "<a href=\"/dsr/playerspublicpage.cls?loginid=" + loginid + "\">" + HTMLFunctions.HTMLEncode(LoginsTable.GetDisplayName(dbs, loginid)) + "</a>";
            } else {
                return HTMLFunctions.HTMLEncode(LoginsTable.GetDisplayName(dbs, loginid));
            }
        } else {
            return "[Empty]";
        }
    }

    public static String GetEmail(MySQLConnection dbs, int loginid) throws SQLException {
        String s = dbs.getScalarAsString("SELECT Login FROM Logins WHERE LoginID = " + loginid);
        return s;
    }

    public String getEmail() throws SQLException {
        return rs.getString("Login");
    }

    public String getPassword() throws SQLException {
        return rs.getString("Pwd");
    }

    public int getLoginID() throws SQLException {
        return rs.getInt("LoginID");
    }

    public void sendEmail(String subject, String msg) throws SQLException {
        DSRWebServer.SendEmail(this.getEmail(), subject, "Hello " + this.getDisplayName() + ",\n\n" + msg);
    }

    public void incTotalTurns() throws SQLException {
        dbs.runSQLUpdate("UPDATE Logins SET TotalTurns = TotalTurns + 1 WHERE LoginID = " + this.getID());
        this.refreshData();
    }

    public void setDateLastMktEmailRcvd() throws SQLException {
        dbs.runSQLUpdate("UPDATE Logins SET DateLastMktEmailRcvd = NOW() WHERE LoginID = " + this.getID());
        this.refreshData();
    }

    public static void IncKilledOwnUnitsCount(MySQLConnection dbs, int loginid) throws SQLException {
        try {
            dbs.runSQLUpdate("UPDATE Logins SET KilledOwnUnitsCount = COALESCE(KilledOwnUnitsCount, 0) + 1 WHERE LoginID = " + loginid);
        } catch (Exception ex) {
            DSRWebServer.HandleError(ex, true);
        }
    }

    public void incTotalDaysTakingTurns(int amt) throws SQLException {
        dbs.runSQLUpdate("UPDATE Logins SET TotalDaysTakingTurns = TotalDaysTakingTurns + " + amt + " WHERE LoginID = " + this.getID());
        this.refreshData();
    }

    public void sendNextTurnEmail(String mission_title, String opponents, String mission, String log, int gid) throws SQLException {
        if (this.emailOnTurn()) {
            this.sendEmail("Your Turn in " + mission_title, "It is now your turn in your game against " + opponents + " in mission " + mission + ".  Below is the game log from the most recent turn.  Please log into the website at " + AppletMain.WEBSITE_LINK + " for more details and to view the map.\n\nYou can now also view a playback of the mission at its current point at " + AppletMain.WEBSITE_LINK + "/dsr/playbackapplet/playbackpage.cls?gid=" + gid + "\n\n" + log);
        }
    }

    public void setOptedOutOfEmails(boolean b) throws SQLException {
        dbs.runSQLUpdate("UPDATE Logins SET OptedOutOfEmails = " + SQLFuncs.b201(b) + " WHERE LoginID = " + this.getID());
    }

    public boolean emailForumReplies() throws SQLException {
        return rs.getInt("EmailForumReplies") == 1;
    }

    public void addConcede() throws SQLException {
        try {
            dbs.runSQLUpdate("UPDATE Logins SET TotalConcedes = TotalConcedes + 1 WHERE LoginID = " + this.getID());
        } catch (Exception ex) {
            DSRWebServer.HandleError(ex, true);
        }
    }

    public boolean emailOnTurn() throws SQLException {
        return rs.getInt("EmailOnTurn") == 1;
    }

    public void setEmailOnTurn(boolean b) throws SQLException {
        dbs.runSQL("UPDATE Logins SET EmailOnTurn = " + SQLFuncs.b201(b) + " WHERE LoginID = " + this.getLoginID());
        this.refreshData();
    }

    public void setEmailForumReplies(boolean b) throws SQLException {
        dbs.runSQL("UPDATE Logins SET EmailForumReplies = " + SQLFuncs.b201(b) + " WHERE LoginID = " + this.getLoginID());
        this.refreshData();
    }

    public boolean isPlayerPlayingInCampaign() throws SQLException {
        String sql = "SELECT Count(*) FROM Games WHERE CampGame = 1 AND GameStatus < " + GamesTable.GS_FINISHED + " AND " + AbstractTable.GetPlayerSubQuery(this.getID()) + " LIMIT 1";
        return dbs.getScalarAsInt(sql) > 0;
    }

    public boolean doesPlayerHaveFactionRequestOpen() throws SQLException {
        String sql = "SELECT Count(*) FROM GameRequests WHERE Player1ID = " + this.getID() + " AND COALESCE(Accepted, 0) = 0 AND CampGame = 1 AND AttackingFactionID > 0 LIMIT 1";
        return dbs.getScalarAsInt(sql) > 0;
    }

    public int getPointsForWinAgainst(LoginsTable login) throws SQLException {
        return getPointsForWinAgainst(login.getPoints());
    }

    public int getPointsForWinAgainst(int opp_pts) throws SQLException {
        int our_pts = this.getPoints();
        if (our_pts > opp_pts) {
            return leaguetable.DEF_WIN_POINTS;
        } else {
            int pts = (opp_pts - our_pts) / 10;
            return leaguetable.DEF_WIN_POINTS + pts;
        }
    }

    public void incCampaignInactivity() throws SQLException {
        try {
            dbs.runSQLUpdate("UPDATE Logins SET CampInactivityPoints = COALESCE(CampInactivityPoints, 0) + 2, TimeLastInactivityPointsAdded = CURDATE() WHERE LoginID = " + this.getID() + " AND DATEDIFF(CURDATE(), TimeLastInactivityPointsAdded) > 0 AND COALESCE(CampInactivityPoints, 0) < " + this.getCampaignPointsWithoutModifiers());
        } catch (Exception ex) {
            DSRWebServer.HandleError(ex);
        }
    }

    public int getCampaignPointsWithoutModifiers() throws SQLException {
        return dbs.getScalarAsInt("SELECT SUM(RankValue) FROM CampUnits WHERE OwnerID = " + this.getID());
    }

    public static void DecCampaignInactivity(MySQLConnection dbs, int loginid) throws SQLException {
        try {
            dbs.runSQLUpdate("UPDATE Logins SET CampInactivityPoints = COALESCE(CampInactivityPoints, 0) - 1 WHERE LoginID = " + loginid + " AND COALESCE(CampInactivityPoints, 0) > 0");
        } catch (Exception ex) {
            DSRWebServer.HandleError(ex);
        }
    }

    public static void SetLoginDisabled(MySQLConnection dbs, int id, int i) throws SQLException {
        try {
            dbs.runSQLUpdate("UPDATE Logins SET Disabled = " + i + " WHERE LoginID = " + id);
        } catch (Exception ex) {
            DSRWebServer.HandleError(ex);
        }
    }

    public static boolean IsLoginDisabled(MySQLConnection dbs, int id) throws SQLException {
        return dbs.getScalarAsInt("SELECT Disabled FROM Logins WHERE LoginID = " + id) == 1;
    }

    public static int GetFactionID(MySQLConnection dbs, int id) throws SQLException {
        return dbs.getScalarAsInt("SELECT FactionID FROM Logins WHERE LoginID = " + id);
    }

    public void setHasAndroid(int i) throws SQLException {
        try {
            dbs.runSQLUpdate("UPDATE Logins SET HasAndroid = " + i + " WHERE LoginID = " + this.getID());
            this.refreshData();
        } catch (Exception ex) {
            DSRWebServer.HandleError(ex);
        }
    }

    public void setDatelastAwardAwarded() throws SQLException {
        try {
            dbs.runSQLUpdate("UPDATE Logins SET DatelastAwardAwarded = NOW() WHERE LoginID = " + this.getID());
            this.refreshData();
        } catch (Exception ex) {
            DSRWebServer.HandleError(ex);
        }
    }

    public void incCampCredits(int inc) throws SQLException {
        setCampCredits(this.getCampCredits() + inc);
    }

    public void setCampCredits(int amt) throws SQLException {
        dbs.runSQLUpdate("UPDATE Logins SET CampaignCredits = " + amt + " WHERE LoginID = " + this.getID());
        this.refreshData();
    }

    public void setFactionID(int id) throws SQLException {
        dbs.runSQLUpdate("UPDATE Logins SET FactionID = " + id + " WHERE LoginID = " + this.getID());
        this.refreshData();
    }

    public void setEmailedAboutFaction() throws SQLException {
        dbs.runSQLUpdate("UPDATE Logins SET EmailedAboutFaction = 1 WHERE LoginID = " + this.getID());
        this.refreshData();
    }

    public boolean hasAndroid() throws SQLException {
        return dbs.getScalarAsInt("SELECT HasAndroid FROM Logins WHERE LoginID = " + this.getID()) == 1;
    }

    public int getCampCredits() throws SQLException {
        return dbs.getScalarAsInt("SELECT COALESCE(CampaignCredits, 0) FROM Logins WHERE LoginID = " + this.getID());
    }

    public int getFactionID() throws SQLException {
        return dbs.getScalarAsInt("SELECT FactionID FROM Logins WHERE LoginID = " + this.getID());
    }
}
