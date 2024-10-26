package ee.fctwister.pages;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.apache.tapestry.annotations.*;
import org.apache.tapestry.event.PageBeginRenderListener;
import org.apache.tapestry.event.PageEvent;
import org.apache.tapestry.html.BasePage;
import ee.fctwister.DAO.StatsDAO;
import ee.fctwister.DAO.TableDAO;
import ee.fctwister.DAO.TeamDAO;
import ee.fctwister.DTO.PlayerDTO;
import ee.fctwister.DTO.PlayerStatsDTO;

public abstract class Team extends BasePage implements PageBeginRenderListener {

    public static int CAPTAIN = 8;

    Calendar currentDate = new GregorianCalendar();

    private int currentYear = currentDate.get(Calendar.YEAR);

    public abstract PlayerDTO getPlayer();

    public abstract void setPlayer(PlayerDTO player);

    public abstract PlayerDTO getPlayerSelected();

    public abstract void setPlayerSelected(PlayerDTO player);

    public abstract PlayerStatsDTO getPlayerStatsYear();

    public abstract void setPlayerStatsYear(PlayerStatsDTO stats);

    public abstract PlayerStatsDTO getPlayerStatsComplete();

    public abstract void setPlayerStatsComplete(PlayerStatsDTO stats);

    public abstract String getPlayerBirthday();

    public abstract void setPlayerBirthday(String date);

    public abstract String getPlayerPosition();

    public abstract void setPlayerPosition(String pos);

    public abstract String getPlayerPos();

    public abstract void setPlayerPos(String pos);

    public abstract String getPlayerName();

    public abstract void setPlayerName(String name);

    public abstract int getStatsYear();

    public abstract void setStatsYear(int selection);

    public abstract int getPlayerPosSelected();

    public abstract void setPlayerPosSelected(int pos);

    public abstract boolean getTeamStatsSelected();

    public abstract void setTeamStatsSelected(boolean selection);

    public abstract String getPositionMenuContainerClass();

    public abstract void setPositionMenuContainerClass(String posMenu);

    public abstract String getTeamDataContainerClass();

    public abstract void setTeamDataContainerClass(String teamData);

    public abstract String getPositionMenuContainerLowerClass();

    public abstract void setPositionMenuContainerLowerClass(String posMenu);

    public abstract String getPlayerPosSelectedClass();

    public abstract void setPlayerPosSelectedClass(String pos);

    @Persist("flash")
    public abstract int getSelectedPlayerID();

    public abstract void setSelectedPlayerID(int id);

    @Persist("flash")
    public abstract int getSelectedPlayerYear();

    public abstract void setSelectedPlayerYear(int year);

    @Persist("flash")
    public abstract int getSelectedPlayerPos();

    public abstract void setSelectedPlayerPos(int pos);

    @Persist("flash")
    public abstract boolean getFormAction();

    public abstract void setFormAction(boolean flag);

    public abstract ArrayList<Integer> getYearsListLeft();

    public abstract void setYearsListLeft(ArrayList<Integer> yearsList);

    public abstract ArrayList<Integer> getYearsListRight();

    public abstract void setYearsListRight(ArrayList<Integer> yearsList);

    public ArrayList<PlayerDTO> getPlayers() {
        try {
            return ee.fctwister.DAO.TeamDAO.getPlayers(getPlayerPosSelected());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getPlayerImage() {
        int id = getPlayerSelected().getId();
        String player = "images/team/" + id + ".jpg";
        String noPlayer = "images/team/no_picture.jpg";
        String url = ee.fctwister.index.MainBorder.IMG_URL + player;
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("HEAD");
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) return player; else return noPlayer;
        } catch (Exception e) {
            e.printStackTrace();
            return noPlayer;
        }
    }

    public void playerSelectActionBase(int id, int year) {
        try {
            setPlayer(getPlayerById(id));
            setPlayerSelected(getPlayerById(id));
            setPlayerBirthday(createBirthday(Long.toString(getPlayerSelected().getIdNr())));
            setPlayerPosition(createPosition(getPlayerSelected().getPos()));
            setPlayerName(getPlayerSelected().getFirst() + " " + getPlayerSelected().getLast());
            setPlayerPosSelected(getSelectedPlayerPos());
            setSelectedPlayerPos(getSelectedPlayerPos());
            setPlayerStatsYear(StatsDAO.getStats(year, id));
            setPlayerStatsComplete(StatsDAO.getStats(0, id));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playerSelectAction(int id) {
        try {
            playerSelectActionBase(id, currentYear);
            setFormAction(true);
            setSelectedPlayerID(id);
            setSelectedPlayerYear(currentYear);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void yearSelectAction(int year) {
        try {
            playerSelectActionBase(getSelectedPlayerID(), year);
            setFormAction(true);
            setSelectedPlayerID(getSelectedPlayerID());
            setSelectedPlayerYear(year);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cupSelectAction() {
        try {
            playerSelectActionBase(getSelectedPlayerID(), -1);
            setFormAction(true);
            setSelectedPlayerID(getSelectedPlayerID());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void teamStatsAction() {
        try {
            setTeamStatsSelected(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void teamListAction() {
        try {
            setTeamStatsSelected(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playerPosSelectAction(String pos) {
        try {
            setSelectedPlayerPos(Integer.parseInt(pos));
            setFormAction(true);
            setSelectedPlayerID(getSelectedPlayerID());
            setSelectedPlayerYear(getSelectedPlayerYear());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PlayerDTO getPlayerById(int id) {
        try {
            return TeamDAO.getPlayerById(id);
        } catch (Exception e) {
            return null;
        }
    }

    private String createBirthday(String idNr) {
        try {
            return idNr.substring(5, 7) + "." + idNr.substring(3, 5) + ".19" + idNr.substring(1, 3);
        } catch (Exception e) {
            e.printStackTrace();
            return "Teadmata";
        }
    }

    private String createPosition(int pos) {
        try {
            return TeamDAO.getPlayerPositionName(pos);
        } catch (Exception e) {
            e.printStackTrace();
            return "Teadmata";
        }
    }

    public boolean isCaptain() {
        try {
            if (getPlayerSelected().getNr() == CAPTAIN) return true; else return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getLeagueName() {
        try {
            return TableDAO.getTableName(ee.fctwister.index.MainBorder.getLeagueIndex(getStatsYear()));
        } catch (Exception e) {
            e.printStackTrace();
            return "N/A";
        }
    }

    private void initPersistentParams() {
        setSelectedPlayerID(0);
        setSelectedPlayerYear(0);
        setSelectedPlayerPos(0);
    }

    public void pageBeginRender(PageEvent event) {
        if (!getFormAction()) {
            initPersistentParams();
            setPositionMenuContainerClass("positionmenucontainer");
            setPositionMenuContainerLowerClass("positionmenucontainerlower");
            setTeamDataContainerClass("teamdatacontainer");
        } else {
            setFormAction(false);
            ArrayList<Integer> yearsLeft = new ArrayList<Integer>();
            ArrayList<Integer> yearsRight = new ArrayList<Integer>();
            int iter = 0;
            int history = ee.fctwister.index.MainBorder.PLAYER_STATS_HISTORY_YEARS;
            for (int i = currentYear; i > currentYear - history; i--) {
                if (iter++ < history / 2) yearsLeft.add(i); else yearsRight.add(i);
            }
            setYearsListLeft(yearsLeft);
            setYearsListRight(yearsRight);
            if (getSelectedPlayerYear() == 0) setSelectedPlayerYear(currentYear);
            if (getSelectedPlayerID() == 0) {
                setPlayerSelected(null);
                setPositionMenuContainerClass("positionmenucontainer");
                setPositionMenuContainerLowerClass("positionmenucontainerlower");
                setTeamDataContainerClass("teamdatacontainer");
            } else if (getPlayerSelected() == null) {
                playerSelectActionBase(getSelectedPlayerID(), getSelectedPlayerYear());
                setPositionMenuContainerClass("positionmenucontaineronplayer");
                setPositionMenuContainerLowerClass("positionmenucontainerloweronplayer");
                setTeamDataContainerClass("teamdatacontaineronplayer");
            } else {
                setPositionMenuContainerClass("positionmenucontaineronplayer");
                setPositionMenuContainerLowerClass("positionmenucontainerloweronplayer");
                setTeamDataContainerClass("teamdatacontaineronplayer");
            }
            if (getSelectedPlayerPos() != 0) setPlayerPosSelected(getSelectedPlayerPos());
        }
    }
}
