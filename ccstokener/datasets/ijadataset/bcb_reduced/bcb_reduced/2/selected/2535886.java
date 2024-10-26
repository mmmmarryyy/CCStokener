package ar.edu.unicen.exa.server.serverLogic;

import java.io.Serializable;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;
import common.datatypes.PlayerStat;
import common.datatypes.PlayerStats;
import common.datatypes.Quest;
import common.datatypes.QuestState;
import common.datatypes.Ranking;
import common.datatypes.Skin;
import common.datatypes.social.Friendship;
import common.datatypes.social.PlayerRelationalState;
import common.datatypes.social.ProfileData;
import common.datatypes.social.SearchResponse;
import common.datatypes.social.SocialEvent;
import common.datatypes.social.UserIdentifier;

/**
 * La clase implementa la interface de acceso al modelo de logica del juego.
 * Este modelo debera proveer soporte para el almacenamiento persistente y
 * recuperacion de datos relevantes para la mecanica del juego, que abarca
 * puntos tales como almacenamiento de Players, 2DGames, Quest, estado de
 * quests, puntuacion de stats, etc.<BR/>
 * 
 * La clase sigue el patron de diseño <I>Sinleton</I>.
 * 
 * @author Cabrea Emilio Facundo &lt;cabrerafacundo at gmail dot com&gt;
 * @author Crego Facundo &lt;facundo.crego at gmail dot com&gt;
 * @author Nicol�s G�mez &lt;nicolas.e.gomez at gmail dot com&gt;
 * @encoding UTF-8
 * 
 *           TODO Extraer una interface a partir de esto. Interface Name IModel.
 * 
 */
public final class ModelAccess {

    /**
	 * Instancia de la clase.
	 */
    private static ModelAccess instance = new ModelAccess();

    /**
	 * @return La instancia singleton de la clase.
	 */
    public static ModelAccess getInstance() {
        return instance;
    }

    private Statement stmt;

    private Connection con;

    /**
	 * Constructor por defecto de la clase, inicializa el estado interno de la
	 * misma.
	 */
    private ModelAccess() {
        try {
            Properties prop = new Properties();
            URL url = ModelAccess.class.getClassLoader().getResource("u3dserver.properties");
            prop.load(url.openStream());
            String database = prop.getProperty("database", "//localhost/2bsoft");
            String user = prop.getProperty("user", "root");
            String password = prop.getProperty("password", "");
            System.out.println("Intentando cargar el conector...");
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("Conectando a la base...");
            con = DriverManager.getConnection("jdbc:mysql:" + database, user, password);
            stmt = con.createStatement();
            System.out.println("Conexion a BD establecida");
        } catch (SQLException ex) {
            System.out.println("Error de mysql: " + ex.getLocalizedMessage() + " " + ex.getErrorCode());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Se produjo un error inesperado: " + e.getMessage());
        }
    }

    /**
	 * Retorna el Skin relacionado al identificador de jugador pasado como
	 * parametro.
	 * 
	 * @param player_nick
	 *            El nick del jugador para el que se desea obtener el skin.
	 * @return El Skin relacionado al identificador del jugador.
	 */
    public Skin getSkin(final String player_nick) {
        try {
            ResultSet res = stmt.executeQuery("select * from player where player_nick = '" + player_nick + "'");
            if (res.next()) {
                Skin avatar = new Skin();
                avatar.setSkin(res.getString("player_avatar"));
                return avatar;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Retorna el conjunto de identificadores de juegos 2D que estan disponibles
	 * para el identificador del jugador pasado como parametro.
	 * 
	 * @param player_nick
	 *            El nick del jugador para el cual se desea conocer los juegos
	 *            disponibles.
	 * @return Un conjunto con los identificadores de juegos 2D disponibles para
	 *         el jugador.
	 */
    public Set<String> getAvailableGames(final String player_nick) {
        Set<String> salida = new HashSet<String>();
        try {
            String idPlayer = " (Select ID_Player from player where player_nick = '" + player_nick + "')";
            ResultSet res = stmt.executeQuery("select mg.ID_game from minigame mg inner join gamesbuyed gb on ( mg.ID_game = gb.ID_game ) where gb.ID_player = " + idPlayer);
            while (res.next()) salida.add(res.getString("ID_game"));
            return salida;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return salida;
    }

    /**
	 * Realiza las acciones correspondientes para marcar como "comprado" el
	 * juego para el jugador.
	 * 
	 * @param id2DGame
	 *            El identificador del juego a comprar.
	 * @param player_nick
	 *            El nick del jugado que compra el juego.
	 */
    public void buy2DGame(final String id2DGame, final String player_nick) {
        try {
            String idPlayer = " (Select ID_Player from player where player_nick = '" + player_nick + "')";
            stmt.executeUpdate("insert into gamesbuyed (ID_game, ID_player) VALUES ('" + id2DGame + "', " + idPlayer + " )");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Construye una instancia de la clase en base a la informacion almacenada
	 * par el identificador de Quest pasado.
	 * 
	 * @param idQuest
	 *            El identificador de la quest que se desea obtener.
	 * @return una instancia de la clase en base a la informacion almacenada par
	 *         el identificador de Quest pasado.
	 */
    public Quest getQuest(final String idQuest) {
        try {
            ResultSet res = stmt.executeQuery("select * from quest where ID_quest = '" + idQuest + "'");
            Quest q = new Quest();
            if (res.next()) {
                q.setIdQuest(res.getString("ID_quest"));
                q.setMoneyReward(res.getFloat("quest_moneyReward"));
                q.setPointsReward(res.getInt("quest_pointsReward"));
                q.setStates(getQuestStates(res.getInt("ID_quest")));
            }
            return q;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Obtiene el conjunto de Quests disponibles para el jugador pasado.
	 * 
	 * @param player_nick
	 *            El nick del jugador.
	 * @return El conjunto de Quests disponibles par ael jugador pasado.
	 */
    public Set<Quest> getAvailableQuests(final String player_nick) {
        Set<Quest> set = new HashSet<Quest>();
        try {
            String idPlayer = " (Select ID_Player from player where player_nick = '" + player_nick + "')";
            ResultSet res = stmt.executeQuery("select * from quest as quest1 where not exists( " + "select 1 from questactivation as qact where not exists (" + "select 1 from questfinished where questfinished.ID_quest = qact.ID_quest AND questfinished.ID_player = " + idPlayer + " ) and" + "(qact.ID_quest_unlocks = quest1.ID_quest)) and " + "(not exists (select 1 from questincourse where questincourse.ID_player =" + idPlayer + " AND questincourse.ID_quest = quest1.ID_quest)" + "and not exists (select 1 from questfinished where questfinished.ID_player = " + idPlayer + " AND questfinished.ID_quest = quest1.ID_quest)" + ")");
            while (res.next()) {
                Quest q = new Quest();
                q.setIdQuest(res.getString("ID_quest"));
                q.setMoneyReward(res.getFloat("quest_moneyReward"));
                q.setPointsReward(res.getInt("quest_pointsReward"));
                q.setStates(getQuestStates(res.getInt("ID_quest")));
                set.add(q);
            }
            return set;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return set;
    }

    /**
	 * @return Un conjunto con los identificadores de los juegos 2D de todos
	 *         aquellos juegos que se puedan comprar.
	 */
    public Set<String> getBuyables2DGames() {
        ResultSet result;
        Set<String> set = new HashSet<String>();
        try {
            result = stmt.executeQuery("select * from minigameforbuy");
            while (result.next()) {
                String id = result.getString("ID_game");
                set.add(id);
            }
            return set;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return set;
    }

    /**
	 * 
	 * @param id2DGame
	 *            Identificador del juego 2D
	 * @param player_nick
	 *            Nick del juegador.
	 * @return La cantidad de veces que el juegador ah jugado el juego 2D.
	 */
    public int getPlayedTimes(final String id2DGame, final String player_nick) {
        ResultSet result;
        try {
            String idPlayer = " (Select ID_Player from player where player_nick = '" + player_nick + "')";
            result = stmt.executeQuery("select count(*) as total from playerhistory where ID_player = " + idPlayer + " and ID_game= '" + id2DGame + "'");
            if (result.next()) return result.getInt("total");
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
	 * @param player_nick
	 *            Nick del jugador.
	 * @return La puntuacion global del jugador pasado como parametro.
	 */
    public int getGlobalScore(final String player_nick) {
        ResultSet result;
        try {
            int points;
            result = stmt.executeQuery("Select * from player where player_nick = '" + player_nick + "'");
            if (result.next()) {
                points = result.getInt("player_points");
                return points;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
	 * @param player_nick
	 *            Nick del jugador
	 * @return Conjunto de quests que esta haciendo actualmente el jugador.
	 */
    public Set<Quest> getCurrentQuests(final String player_nick) {
        ResultSet result;
        Set<Quest> set = new HashSet<Quest>();
        try {
            String idPlayer = " (Select ID_Player from player where player_nick = '" + player_nick + "')";
            result = stmt.executeQuery("select * from (questincourse as qi join quest as q on qi.ID_quest = q.ID_quest) join queststate as qs on qi.ID_quest=qs.ID_quest where ID_player = " + idPlayer);
            while (result.next()) {
                Quest q = new Quest();
                q.setActualState(result.getInt("order1"));
                q.setIdQuest(String.valueOf(result.getInt("ID_quest")));
                q.setMoneyReward(result.getFloat("quest_moneyReward"));
                q.setPointsReward(result.getInt("quest_pointsReward"));
                q.setStates(getQuestStates(result.getInt("ID_quest")));
                set.add(q);
            }
            return set;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Dummy implementation. Check always if login == password.
	 * 
	 * @param password
	 *            La contraseña.
	 * @param player_nick
	 *            El Nick del jugador
	 * 
	 * @return {@code true} si la contraseña suministrada para el usuario es la
	 *         que se tiene almancenada. {@code false} en caso contrario.
	 */
    public boolean checkPlayer(final String password, final String player_nick) {
        try {
            ResultSet res = stmt.executeQuery("Select count(*) as total from player where player_password = MD5('" + password + "') and player_nick = '" + player_nick + "'");
            res.next();
            return res.getInt("total") > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
	 * Agrega el puntaje pasado como parametro al modelo, realizando todas las
	 * acciones corresponidentes.<BR/>
	 * Estas acciones consiten en actualizar los stats para el jugador dueño
	 * del puntaje, asi como tmb el historial de puntajes para el minijuego
	 * asociado y chequear si es el puntaje mas alto.
	 * 
	 * @param score
	 *            El puntaje a agregar al modelo.
	 */
    public void add2DGameScore(final PlayerStats score) {
        String player_nick = score.getIdPlayer();
        String idPlayer = "(Select ID_Player from player where player_nick ='" + player_nick + "')";
        Hashtable<String, Object> pointsTable = score.getStatsPerIdPlayerStat();
        System.out.println(pointsTable.size());
        try {
            Enumeration<String> e = pointsTable.keys();
            while (e.hasMoreElements()) {
                String ID_stat_name = e.nextElement();
                String ID_stat = "(Select ID_stat from stat where stat_name ='" + ID_stat_name + "')";
                ResultSet playerstats = stmt.executeQuery("Select * from playerstat where ID_stat = " + ID_stat + " and ID_player=" + idPlayer + "");
                if (playerstats.next()) {
                    double playerstatvalue = playerstats.getDouble("playerstat_value");
                    if (playerstatvalue + Double.parseDouble(((Integer) pointsTable.get(ID_stat_name)).toString()) > getStatMaxValue(ID_stat)) playerstatvalue = getStatMaxValue(ID_stat); else if (playerstatvalue + Double.parseDouble(((Integer) pointsTable.get(ID_stat_name)).toString()) < getStatMinValue(ID_stat)) playerstatvalue = getStatMinValue(ID_stat); else playerstatvalue += Double.parseDouble(((Integer) pointsTable.get(ID_stat_name)).toString());
                    stmt.executeUpdate("update playerstat set playerstat_value = '" + playerstatvalue + "' where ID_stat= " + ID_stat + " and ID_player= '" + idPlayer + "'");
                } else stmt.executeUpdate("insert into playerstat (ID_stat,ID_player,playerstat_value) values (" + ID_stat + "," + idPlayer + ",'" + pointsTable.get(ID_stat_name) + "')");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * @param IDGame
	 * @param IDStat
	 * @return
	 */
    public double getRewardStatValue(final String IDGame, final int IDStat) {
        try {
            ResultSet result = stmt.executeQuery("Select * from rewardstat where ID_game= '" + IDGame + "' and ID_stat = " + IDStat);
            if (result.next()) {
                double value = result.getFloat("rewardstat_value");
                return value;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private double getStatMaxValue(final String IDStat) {
        try {
            ResultSet result = stmt.executeQuery("Select * from stat where ID_stat = " + IDStat);
            if (result.next()) return result.getFloat("stat_maxvalue");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private double getStatMinValue(final String IDStat) {
        try {
            ResultSet result = stmt.executeQuery("Select * from stat where ID_stat = " + IDStat);
            if (result.next()) return result.getFloat("stat_minvalue");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
	 * Cambia el puntaje global asociado al jugador por el pasado como
	 * parametro. PLAYER_POINTS
	 * 
	 * @param player_name
	 *            Nick del jugador.
	 * @param globalScore
	 *            el puntaje global.
	 */
    public void setGlobalScore(final String player_nick, final int globalScore) {
        try {
            stmt.executeUpdate("update player set player_points = " + globalScore + " where player_nick = '" + player_nick + "'");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * @param id2DGame
	 *            Identificador del juego2D
	 * @return El ranking para el juego 2D pasado como parametro.
	 */
    public Ranking getRanking(final String id2DGame) {
        try {
            ResultSet set = stmt.executeQuery("select count(*) as total from (select ID_player,MAX(pointsEarned) as points from playerhistory where ID_game = '" + id2DGame + "' group by ID_player order by points DESC) a");
            int total;
            if (set.next()) {
                total = set.getInt("total");
                Ranking rank = new Ranking(total, id2DGame);
                ResultSet set2 = stmt.executeQuery("select ID_player,MAX(pointsEarned) as points from playerhistory where ID_game = '" + id2DGame + "' group by ID_player order by points DESC");
                int position = 0;
                while (set2.next()) {
                    rank.putRanking(position, set2.getString("ID_player"), set2.getInt("points"));
                    position++;
                }
                return rank;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Agrega la quest al conjunto de quest que estan siendo jugadas por el
	 * jugador actualmente.
	 * 
	 * @param player_nick
	 *            Nick del jugador.
	 * @param idQuest
	 *            Identificador de la quest.
	 */
    public void startQuest(final String player_nick, final String idQuest) {
        try {
            String idPlayer = " (Select ID_Player from player where player_nick = '" + player_nick + "')";
            String orderinicial = " (select MIN(order1) from queststate where ID_quest = " + Integer.valueOf(idQuest).intValue() + " order by order1) ";
            stmt.executeUpdate("insert into questincourse (ID_player,order1,ID_quest) values (" + idPlayer + "," + orderinicial + "," + Integer.valueOf(idQuest).intValue() + ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Agrega la quest al conjunto de quest que ya han sido terminadas por el
	 * jugador.
	 * 
	 * @param player_nick
	 *            Nick del jugador.
	 * @param idQuest
	 *            Identificador de la quest.
	 */
    public void finishQuest(final String player_nick, final String idQuest) {
        try {
            String idPlayer = " (Select ID_Player from player where player_nick = '" + player_nick + "')";
            stmt.executeUpdate("insert into questfinished (ID_player,ID_quest) values (" + idPlayer + "," + Integer.valueOf(idQuest).intValue() + ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Saca la quest al conjunto de quest que estan siendo jugadas por el
	 * jugador actualmente.
	 * 
	 * @param player_nick
	 *            Nick del jugador.
	 * @param idQuest
	 *            Identificador de la quest.
	 */
    public void abortQuest(final String player_nick, final String idQuest) {
        try {
            String idPlayer = " (Select ID_Player from player where player_nick = '" + player_nick + "')";
            stmt.executeUpdate("delete from questincourse where ID_player = " + idPlayer + " and ID_quest = " + Integer.valueOf(idQuest).intValue());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Cambia el estado de la quest en curso pasada como parametro indicandole
	 * que avance al siguiente estado de la misma.
	 * 
	 * @param player_nick
	 *            Nick del jugador.
	 * @param idQuest
	 *            Identificador de la quest.
	 * @param order
	 *            El proximo estado de la quest.
	 */
    public void nextQuestState(final String player_nick, final String idQuest, final String order) {
        try {
            String idPlayer = " (Select ID_Player from player where player_nick = '" + player_nick + "')";
            stmt.executeUpdate("update questincourse set order1 = " + Integer.valueOf(order).intValue() + " where ID_player = " + idPlayer + " and ID_quest = " + Integer.valueOf(idQuest).intValue());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 
	 * @param player_nick
	 *            Nick del jugador.
	 * @return el conjunto de PlayerStats del jugador pasado como parametro.
	 */
    public Set<PlayerStat> getPlayerStats(final String player_nick) {
        ResultSet res;
        Set<PlayerStat> salida = new HashSet<PlayerStat>();
        try {
            res = stmt.executeQuery("" + "Select * from playerstat ps" + "	inner join stat s on (s.ID_stat = ps.ID_stat) inner join player p on ps.ID_Player = p.ID_Player " + "where p.player_nick = '" + player_nick + "'");
            while (res.next()) {
                PlayerStat aux = new PlayerStat();
                aux.setId(res.getString("stat_name"));
                aux.setValue(res.getFloat("playerstat_value"));
                salida.add(aux);
            }
            return salida;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return salida;
    }

    /**
	 * @param player_nick
	 *            Nick del jugador.
	 * @return El dinero del jugador pasado como parametro.
	 */
    public float getMoney(final String player_nick) {
        try {
            ResultSet result = stmt.executeQuery("Select * from player where player_nick = '" + player_nick + "'");
            if (result.next()) {
                float money = result.getFloat("player_money");
                return money;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
	 * Setea el dinero del jugador y lo perciste en el modelo.
	 * 
	 * @param player_nick
	 *            Nick del jugador.
	 * @param money
	 *            El dinero a setear.
	 */
    public void setMoney(final String player_nick, final float money) {
        try {
            stmt.executeUpdate("update player set player_money = " + money + " where player_nick= '" + player_nick + "'");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Retorna el costo de un juego determinado.
	 * 
	 * @param id2DGame
	 *            identificador del juego que nos interesa consultar.
	 * 
	 * @return valor del juego especificado.
	 */
    public float get2DGamePrice(final String id2DGame) {
        try {
            ResultSet result = stmt.executeQuery("Select * from minigameforbuy where ID_game = '" + id2DGame + "'");
            if (result.next()) return result.getFloat("game_cost");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private QuestState[] getQuestStates(int ID_quest) {
        ResultSet statesNumber;
        try {
            Statement s = con.createStatement();
            statesNumber = s.executeQuery("Select count(*) as total from queststate where ID_quest = " + ID_quest);
            int number = 0;
            if (statesNumber.next()) number = statesNumber.getInt("total");
            ResultSet statesResult = s.executeQuery("Select * from queststate where ID_quest = " + ID_quest);
            QuestState[] states = new QuestState[number];
            int i = 0;
            while (statesResult.next()) {
                QuestState qs = new QuestState();
                qs.setId2DGame(statesResult.getString("queststate_2dgameassociated"));
                qs.setIdQuestState(String.valueOf(ID_quest));
                qs.setStartStage(statesResult.getInt("queststate_initialstage"));
                qs.setTimeToPlay(statesResult.getDouble("queststate_timetoplay"));
                states[i] = qs;
            }
            return states;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getAction(final String Id_AccessPoint) {
        try {
            ResultSet result = stmt.executeQuery("Select Id_Action from actionmapping natural join actionaccess where Id_AccessPoint = '" + Id_AccessPoint + "'");
            if (result.next()) return result.getString("Id_Action");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Hashtable<String, Object> getActionParameters(final String Id_AccessPoint) {
        Hashtable<String, Object> parameters = new Hashtable<String, Object>();
        try {
            ResultSet result = stmt.executeQuery("Select * from actionparameters natural join actionaccess where Id_AccessPoint = '" + Id_AccessPoint + "'");
            while (result.next()) parameters.put(result.getString("Parameter"), result.getString("Value"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return parameters;
    }

    public String getRecommendedAction(final String Id_AccessPoint) {
        try {
            ResultSet result = stmt.executeQuery("SELECT * FROM ((actionaccess NATURAL JOIN recommendedaction) JOIN actionmapping ON Id_SuperActionRecommended = actionmapping.Id_SuperAction) WHERE actionaccess.Id_AccessPoint = '" + Id_AccessPoint + "'");
            if (result.next()) return result.getString("Id_Action");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Hashtable<String, Object> getRecommendedActionParameters(final String Id_AccessPoint) {
        Hashtable<String, Object> parameters = new Hashtable<String, Object>();
        try {
            ResultSet result = stmt.executeQuery("SELECT * FROM (( actionaccess NATURAL JOIN recommendedaction )JOIN actionparameters ON Id_SuperActionRecommended = actionparameters.Id_SuperAction)WHERE actionaccess.Id_AccessPoint = '" + Id_AccessPoint + "'");
            while (result.next()) parameters.put(result.getString("Parameter"), result.getString("Value"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return parameters;
    }

    public boolean verifyRestrictions(String Id_AccessPoint, String id_Player) throws SQLException {
        boolean answ = true;
        try {
            ResultSet action = stmt.executeQuery("SELECT * FROM actionaccess WHERE ( Id_AccessPoint = '" + Id_AccessPoint + "');");
            String Id_SuperAction;
            ResultSet playerStats;
            if (action.next()) {
                Id_SuperAction = (new Integer(action.getInt("Id_SuperAction"))).toString();
                ResultSet name = stmt.executeQuery("SELECT * FROM  player WHERE ( player_name = '" + id_Player + "');");
                name.next();
                String playerId = (new Integer(name.getInt("ID_player"))).toString();
                Hashtable<String, String> actionRestricted = getActionRestricted(Id_SuperAction);
                Enumeration<String> enumm = actionRestricted.keys();
                while (enumm.hasMoreElements()) {
                    String ID_stat = enumm.nextElement();
                    playerStats = stmt.executeQuery("SELECT * FROM playerstat WHERE (( ID_stat = '" + ID_stat + "') AND (Id_Player = '" + playerId + "'));");
                    if (playerStats.next()) {
                        if ((playerStats.getDouble("playerstat_value")) < (Double.parseDouble(actionRestricted.get(ID_stat)))) answ = false;
                    } else answ = false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return answ;
    }

    private Hashtable<String, String> getActionRestricted(String Id_SuperAction) {
        Hashtable<String, String> answ = new Hashtable<String, String>();
        try {
            ResultSet actionRestricted = stmt.executeQuery("SELECT * FROM actionrestricted WHERE ( Id_SuperAction ='" + Id_SuperAction + "' );");
            while (actionRestricted.next()) answ.put((new Integer(actionRestricted.getInt("ID_stat"))).toString(), (new Double(actionRestricted.getDouble("cant_min_value"))).toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return answ;
    }

    /**
	 * @author Gasti, Fido Devuelve los datos de perfil de un usuario
	 * @param player_name
	 *            nombre del usuario usado para la busca en la tabla Player de
	 *            la BD
	 * @return los Datos de Perfil del jugador que corresponde con el nombre de
	 *         usuario
	 */
    public ProfileData getProfileData(ProfileData profileData) {
        try {
            String player_name = profileData.getName();
            ResultSet result = stmt.executeQuery("Select * from player where player_name = '" + player_name + "'");
            if (result.next()) {
                String idPlayer = result.getString("ID_player");
                String name = result.getString("player_name");
                String lastname = result.getString("player_lastname");
                String nick = result.getString("player_nick");
                String city = result.getString("player_city");
                String country = result.getString("player_country");
                String avatar = result.getString("player_avatar");
                String telephone = result.getString("player_telephone");
                Date birthday = result.getDate("player_birthdate");
                String email = result.getString("player_email");
                int id = Integer.parseInt(idPlayer);
                ProfileData playerInfo = new ProfileData(id, name, lastname, nick, city, country, avatar, telephone, birthday, email);
                return playerInfo;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Retorna la lista de amigos para un jugador en particular
	 * 
	 * @author mati, Fido, Cito
	 * @param playerName
	 *            Nombre del jugador
	 * @param cant
	 * @param from
	 * @param filter
	 * @return Amigos del jugador levantados de la base de datos
	 */
    public SearchResponse getFriendsList(SearchResponse searchResponse) {
        try {
            String playerName = searchResponse.getUserName();
            String filter = searchResponse.getFilter();
            int from = searchResponse.getFrom();
            int cant = searchResponse.getPerPage();
            int playerID = this.getPlayerIdentifier(playerName);
            ResultSet result = stmt.executeQuery("SELECT p.player_name, p.ID_player " + "FROM player_friendship f join player p on p.ID_player = f.ID_friend " + "WHERE f.ID_player = '" + playerID + "' " + "and p.player_name LIKE '%" + filter + "%' " + "order by player_name " + "LIMIT " + from + "," + cant + " ");
            SearchResponse friendsList = new SearchResponse(playerName, filter, from, cant);
            ArrayList<Serializable> amigos = new ArrayList<Serializable>();
            while (result.next()) {
                String name = result.getString("player_name");
                String idStr = result.getString("ID_player");
                int id = Integer.parseInt(idStr);
                amigos.add(new UserIdentifier(id, name));
            }
            friendsList.setResults(amigos);
            ResultSet lista = stmt.executeQuery("Select count(*) cuenta " + "FROM player_friendship f join player p on p.ID_player = f.ID_friend " + "WHERE f.ID_player = '" + playerID + "' " + "and p.player_name LIKE '%" + filter + "%' ");
            lista.next();
            friendsList.setTotal(lista.getInt("cuenta"));
            return friendsList;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Crea una solicitud de amistad
	 * 
	 * @param playerNameFrom
	 * @param playerNameTo
	 * 
	 */
    public void addFriendshipRequest(Friendship friendship) {
        try {
            String playerNameFrom = friendship.getPlayerNameFrom();
            String playerNameTo = friendship.getPlayerNameTo();
            stmt.executeUpdate("INSERT INTO friend_request (ID_player_to,ID_player_from) values (" + getPlayerIdentifier(playerNameTo) + "," + getPlayerIdentifier(playerNameFrom) + ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Retorna el identificador de un player dado el nombre del mismo, si no es
	 * valido retorna -1
	 * 
	 * @param playerName
	 * @return
	 */
    public int getPlayerIdentifier(String playerName) {
        try {
            ResultSet result = stmt.executeQuery("Select * from player where player_name = '" + playerName + "'");
            if (result.next()) return result.getInt("ID_player");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
	 * @author Cito Eval�a si existe una solicitud de amistad entre los dos
	 *         players pasados como parametro. En caso de existir, devuelve el
	 *         id de misma. En caso negativo, -1
	 * @param playerNameFrom
	 * @param playerNameTo
	 * @return
	 */
    public int isFriendshipRequested(Friendship friendship) {
        String playerNameFrom = friendship.getPlayerNameFrom();
        String playerNameTo = friendship.getPlayerNameTo();
        String consulta = "SELECT id_friend_request " + "from friend_request " + "where " + "id_player_from = " + getPlayerIdentifier(playerNameFrom) + " " + "and " + "id_player_to = " + getPlayerIdentifier(playerNameTo);
        try {
            ResultSet result = stmt.executeQuery(consulta);
            if (result.next()) return result.getInt("id_friend_request");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
	 * @author Cito Eval�a si un player es amigo de otro
	 * @param playerNameFrom
	 * @param playerNameTo
	 * @return
	 */
    public PlayerRelationalState isFriend(Friendship friendship) {
        String playerNameFrom = friendship.getPlayerNameFrom();
        String playerNameTo = friendship.getPlayerNameTo();
        int idFrom = getPlayerIdentifier(playerNameFrom);
        int idTo = getPlayerIdentifier(playerNameTo);
        String consulta = "select * " + "from player_friendship " + "where " + "id_player = " + idFrom + " and " + "id_friend = " + idTo + "";
        try {
            ResultSet result = stmt.executeQuery(consulta);
            if (result.next()) return PlayerRelationalState.FRIENDS; else {
                consulta = "select * from friend_request " + "where id_player_to=" + idTo + " and " + "id_player_from=" + idFrom + "";
                result = stmt.executeQuery(consulta);
                if (result.next()) return PlayerRelationalState.WAITING_CONFIRMATION; else {
                    consulta = "select * from friend_request " + "where id_player_to=" + idFrom + " and " + "id_player_from=" + idTo + "";
                    result = stmt.executeQuery(consulta);
                    if (result.next()) return PlayerRelationalState.PENDING_CONFIRMATION;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return PlayerRelationalState.NO_FRIEND;
    }

    /**
	 * Borra la solicitud de amistad de la base de datos asociada al Id de la
	 * solicitud
	 * 
	 * @param idFriendRequest
	 *            Id de la solicitud de amistad a eliminar
	 * 
	 */
    public boolean removeFriendshipRequest(Friendship friendship) {
        try {
            int idFriendRequest = friendship.getIdFriendRequest();
            stmt.executeUpdate("DELETE FROM friend_request WHERE ID_friend_request= '" + idFriendRequest + "'");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
	 * @author mati
	 * @param playerName
	 *            nombre del jugador del cual se pretende obtener la lista de
	 *            solicitudes
	 * @param cant
	 * @param from
	 * @param filter
	 * @return lista de solicitudes (lista de Friendship)
	 */
    public SearchResponse getFriendsRequestList(SearchResponse searchResponse) {
        try {
            String playerName = searchResponse.getUserName();
            String filter = searchResponse.getFilter();
            int from = searchResponse.getFrom();
            int cant = searchResponse.getPerPage();
            SearchResponse fRL = new SearchResponse(playerName, filter, from, cant);
            ResultSet lista = stmt.executeQuery("select f.ID_friend_request, p2.player_name " + "from (player p1 join friend_request f " + "on p1.ID_player=f.ID_player_to) " + "join player p2 " + "on p2.ID_player=f.ID_player_from " + "where p1.player_name ='" + playerName + "' " + "and p2.player_name LIKE '%" + filter + "%' " + "order by p2.player_name " + "LIMIT " + from + ", " + cant);
            ArrayList<Serializable> reqs = new ArrayList<Serializable>();
            while (lista.next()) {
                int id_solicitud = lista.getInt("ID_friend_request");
                String name = lista.getString("player_name");
                Friendship req = new Friendship(id_solicitud, name, playerName);
                reqs.add(req);
            }
            fRL.setResults(reqs);
            lista = stmt.executeQuery("Select count(*) cuenta " + "from (player p1 join friend_request f " + "on p1.ID_player=f.ID_player_to) " + "join player p2 " + "on p2.ID_player=f.ID_player_from " + "where p1.player_name ='" + playerName + "' " + "and p2.player_name LIKE '%" + filter + "%' ");
            lista.next();
            fRL.setTotal(lista.getInt("cuenta"));
            return fRL;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * @author Sole, Cito Confirma la solicitud de amistad de la base de datos
	 *         asociada al Id de la solicitud e inserta una tupla en la tabla
	 *         player_friendship indicando una nueva relacion de amistad entre
	 *         el usuario que envi� la solicitud y el usuario que la acepta.
	 * @param idFriendRequest
	 *            , friend_to, friend_from Id de la solicitud de amistad a
	 *            eliminar, nombre del usuario que envia la solicitud de
	 *            amistad, nombre del usuario que recibe la solcitud de amistad
	 * 
	 * 
	 */
    public boolean confirmFriendshipRequest(Friendship f) {
        try {
            int idFriendRequest = f.getIdFriendRequest();
            ResultSet friendship = stmt.executeQuery("SELECT * FROM friend_request WHERE ID_friend_request= '" + idFriendRequest + "'");
            if (friendship.next()) {
                int friend_to = friendship.getInt("ID_player_to");
                int friend_from = friendship.getInt("ID_player_from");
                ResultSet es_amigo1 = stmt.executeQuery("SELECT * FROM player_friendship WHERE ID_friend ='" + friend_to + "' and ID_player='" + friend_from + "'");
                if (!es_amigo1.next()) {
                    stmt.executeUpdate("insert into player_friendship (ID_player,ID_friend) values (" + friend_to + "," + friend_from + ")");
                    stmt.executeUpdate("insert into player_friendship (ID_player,ID_friend) values (" + friend_from + "," + friend_to + ")");
                } else return false;
                stmt.executeUpdate("DELETE FROM friend_request WHERE ID_friend_request= '" + idFriendRequest + "'");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
	 * @author Maximiliano Torre, Fido, Cito
	 * @param filter
	 * @param playerName
	 *            nombre del jugador por el cual se filtra la busqueda
	 * @param from
	 *            desde que tupla devuelve la consulta
	 * @param to
	 *            hasta que tupla devuelve la consulta
	 * @return lista de usuarios
	 */
    public SearchResponse searchForUsers(SearchResponse searchResponse) {
        try {
            String userSearching = searchResponse.getUserName();
            String filter = searchResponse.getFilter();
            int pFrom = searchResponse.getFrom();
            int perPage = searchResponse.getPerPage();
            String condicion = "FROM player " + "WHERE player_name like '%" + filter + "%' " + "OR player_lastname like '%" + filter + "%' " + "OR player_nick like '%" + filter + "%' ";
            String s = "SELECT ID_player, player_name " + condicion + "" + "order by player_name " + "LIMIT " + pFrom + "," + perPage + " ";
            ResultSet result = stmt.executeQuery(s);
            ArrayList<Serializable> amigos = new ArrayList<Serializable>();
            while (result.next()) {
                String name = result.getString("player_name");
                String idStr = result.getString("ID_player");
                int id = Integer.parseInt(idStr);
                amigos.add(new UserIdentifier(id, name));
            }
            ResultSet total = stmt.executeQuery("Select count(*) cuenta " + condicion);
            total.next();
            String t = total.getString("cuenta");
            int t1 = Integer.parseInt(t);
            SearchResponse userList = new SearchResponse(userSearching, filter, pFrom, perPage);
            userList.setTotal(t1);
            userList.setResults(amigos);
            return userList;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Elimina una relacion de amistad.
	 * 
	 * @param playerName
	 * @param playerName2
	 * @return
	 */
    public Boolean removeFriendship(Friendship friendship) {
        try {
            String playerName = friendship.getPlayerNameFrom();
            String playerName2 = friendship.getPlayerNameTo();
            int id1 = getPlayerIdentifier(playerName);
            int id2 = getPlayerIdentifier(playerName2);
            stmt.executeUpdate("DELETE FROM player_friendship WHERE (id_player = " + id1 + " " + "and id_friend = " + id2 + ") or " + "(id_player = " + id2 + " " + "and id_friend = " + id1 + ")");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getRecord(String playerName, String juego) {
        try {
            int idPlayer = this.getPlayerIdentifier(playerName);
            ResultSet result = stmt.executeQuery("select max(pointsEarned) from playerhistory p where ( p.ID_game= '" + juego + "' and id_player= " + idPlayer + " )");
            if (result.next()) return result.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public SearchResponse getSocialEvent(SearchResponse searchRequest) {
        String playerName = searchRequest.getUserName();
        String filter = searchRequest.getFilter();
        int pFrom = searchRequest.getFrom();
        int perPage = searchRequest.getPerPage();
        int idPlayer = this.getPlayerIdentifier(playerName);
        String s = "SELECT * from player_event_notification " + "where ID_player_to= '" + idPlayer + "' " + "order by ID_player_to " + "LIMIT " + pFrom + "," + perPage + " ";
        ResultSet result;
        try {
            result = stmt.executeQuery(s);
            ArrayList<Serializable> eventos = new ArrayList<Serializable>();
            while (result.next()) {
                int id = result.getInt("ID_player_event_notification");
                String from = result.getString("ID_player_from");
                String to = result.getString("ID_player_to");
                String descrip = result.getString("description");
                eventos.add(new SocialEvent(id, from, to, descrip));
            }
            ResultSet total = stmt.executeQuery("Select count(*) cuenta from player_event_notification " + "where ID_player_to= '" + idPlayer + "'");
            total.next();
            String t = total.getString("cuenta");
            int t1 = Integer.parseInt(t);
            SearchResponse userList = new SearchResponse(playerName, filter, pFrom, perPage);
            userList.setTotal(t1);
            userList.setResults(eventos);
            return userList;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addSocialEvent(SocialEvent event) {
        try {
            stmt.executeUpdate("INSERT INTO player_event_notification (ID_player_to , ID_player_from , description) values ( " + getPlayerIdentifier(event.getPlayerNameTo()) + "," + getPlayerIdentifier(event.getPlayerNameFrom()) + " , '" + event.getDescription() + "')");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * @author Nadia vega
	 * @param idUser
	 * @return
	 */
    public ArrayList<UserIdentifier> getFriends(String idUser) {
        try {
            int playerID = this.getPlayerIdentifier(idUser);
            ResultSet result = stmt.executeQuery("SELECT p.player_name, p.ID_player " + "FROM player_friendship f join player p on p.ID_player = f.ID_friend " + "WHERE f.ID_player = '" + playerID + "' ");
            ArrayList<UserIdentifier> amigos = new ArrayList<UserIdentifier>();
            while (result.next()) {
                String name = result.getString("player_name");
                String idStr = result.getString("ID_player");
                int id = Integer.parseInt(idStr);
                amigos.add(new UserIdentifier(id, name));
            }
            return amigos;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * @author Nadia vega, Maximiliano Torre
	 * @param idUser
	 * @return
	 */
    public String getPlayerNameById(String id) {
        try {
            ResultSet result = stmt.executeQuery("SELECT p.player_name " + "FROM player p  " + "WHERE p.ID_player = " + id + " ");
            String name = "";
            while (result.next()) name = result.getString("player_name");
            return name;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * @author Nadia vega, Maximiliano Torre
	 * @param idUser
	 * @return
	 */
    public String getQuestDescription(final String idQuest) {
        try {
            ResultSet res = stmt.executeQuery("select quest_description from quest where ID_quest = '" + idQuest + "'");
            String description = "";
            if (res.next()) description = res.getString("quest_description");
            return description;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * @author Nadia Vega
	 * @param idUser
	 * @return
	 */
    public ArrayList<UserIdentifier> getFriendInQuest(String namePlayer, String idQuest) {
        try {
            int playerID = this.getPlayerIdentifier(namePlayer);
            ResultSet result = stmt.executeQuery("SELECT DISTINCT p.player_name, p.ID_player" + "" + " FROM (player_friendship f join player p on p.ID_player = f.ID_friend)" + "join questfinished q on p.ID_player=q.ID_player " + "WHERE f.ID_player = " + playerID + " and ID_quest <=" + idQuest);
            ArrayList<UserIdentifier> amigos = new ArrayList<UserIdentifier>();
            while (result.next()) {
                String name = result.getString("player_name");
                String idStr = result.getString("ID_player");
                int id = Integer.parseInt(idStr);
                amigos.add(new UserIdentifier(id, name));
            }
            return amigos;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<UserIdentifier> getFriendLessQuest(String namePlayer, String idQuest) {
        try {
            int playerID = this.getPlayerIdentifier(namePlayer);
            ResultSet result = stmt.executeQuery("SELECT DISTINCT p.player_name, p.ID_player" + "" + " FROM (player_friendship f join player p on p.ID_player = f.ID_friend)" + "join questfinished q on p.ID_player=q.ID_player " + "WHERE f.ID_player = " + playerID + " and ID_quest <=" + idQuest);
            ArrayList<UserIdentifier> amigos = new ArrayList<UserIdentifier>();
            while (result.next()) {
                String name = result.getString("player_name");
                String idStr = result.getString("ID_player");
                int id = Integer.parseInt(idStr);
                amigos.add(new UserIdentifier(id, name));
            }
            return amigos;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
