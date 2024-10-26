package src.network;

import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import src.engine.PICAIUInputQueue;
import src.network.PICAIUDBConnection;
import src.network.PICAIURecruittedUnit;

/**
 * This class represents a single connection to a client on the other end of the network. It stores
 * data about that client and can communicate with it directly. It also checks for incoming messages
 * and processes them accordingly. It extends thread to allows a short amount of sleeping so that
 * it doesn't hog system resources and also implements the interface PICAIUCanPing so that it can
 * house a PICAIUPingService to ping the client and check for a connection.
 */
public class PICAIUServerPersona extends Thread implements PICAIUCanPing {

    private Socket client;

    private PrintWriter out;

    private PICAIUServer server;

    private boolean running = true;

    private boolean connected = false;

    private long sleepTimeMillis = 10;

    private PICAIUPingService pingService;

    private boolean sendPing = false;

    private String userName;

    private int money = 200;

    private int id = -1;

    private String verificationCode;

    private PICAIUGame game;

    ArrayList<PICAIURecruittedUnit> recruitted = new ArrayList<PICAIURecruittedUnit>();

    HashMap<Long, PICAIURecruittedUnit> recruittedRefs = new HashMap<Long, PICAIURecruittedUnit>();

    private int placed = 0;

    PICAIUInputQueue outputQueue = new PICAIUInputQueue();

    /**
   * Creates a new PICAIUServerPersona with the specifed socket and parent server.
   */
    public PICAIUServerPersona(Socket clientIn, PICAIUServer serverIn) {
        client = clientIn;
        server = serverIn;
        pingService = new PICAIUPingService(this);
        try {
            out = new PrintWriter(client.getOutputStream(), true);
            sendDataToClient("CNCT");
        } catch (Exception e) {
            server.clearPersona(this);
            System.out.println(e);
            System.out.println("Failed to connect to client at: " + client.getInetAddress());
        }
        init();
        Thread t = new Thread(this);
        t.start();
    }

    /**
   * This method sets up the persona for gameplay
   */
    protected void init() {
        sendDataToClient("OKGO");
        createReferences();
        for (PICAIURecruittedUnit u : recruitted) {
            u.initAI(game);
        }
    }

    /**
   * Generates a special verification code to email to the user and compare against
   */
    protected void generateVerificationCode() {
        verificationCode = "";
        for (int i = 0; i < 4; i++) {
            verificationCode += (int) (Math.random() * 10);
        }
        verificationCode += "-";
        for (int i = 0; i < 4; i++) {
            verificationCode += (int) (Math.random() * 10);
        }
        verificationCode += "-";
        for (int i = 0; i < 4; i++) {
            verificationCode += (int) (Math.random() * 10);
        }
    }

    /**
   * Emails a message to the user with their information needed to login as well as their special verification
   * code to test against
   */
    protected boolean emailRecover(String userIn, String emailIn) {
        try {
            if (server.VERBOSE) {
                System.out.println("RECOVERING INFO FROM: " + PICAIUServer.MYHOST);
            }
            URL url = new URL(PICAIUServer.MYHOST + "recover.php?user=" + userIn + "&server=PICAIU" + "&email=" + emailIn + "&ver=" + verificationCode + "&authPass=" + server.getAuthPass());
            URLConnection conn = url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            String status = sb.toString();
            System.out.println("Email Recover Status=" + status);
            if (status.equals("recovered correctly!")) {
                return true;
            }
            return true;
        } catch (Exception e) {
            System.out.println(e);
        }
        return false;
    }

    /**
   * Takes a string and appends to it the control string, "t ", to designate that a text-based message is being sent
   * so that the clients can display it in the text fields of the GUI.
   * @param message the string to send after appending the text control string, "t ".
   */
    public void sendMessageToClient(String message) {
        if (isConnected()) {
            try {
                out.println("t " + message);
            } catch (Exception e) {
                System.out.println(e);
                System.out.println("Failed to send message to client: " + message);
            }
        }
    }

    /**
   * Takes a string and appends to it the control string, "d ", to designate that a data message is being sent
   * so that the clients can take the appropriate action.
   * @param message the string to send after appending the data control string, "d ".
   */
    public void sendDataToClient(String message) {
        if (isConnected()) {
            try {
                out.println("d " + message);
            } catch (Exception e) {
                System.out.println(e);
                System.out.println("Failed to send message to client: " + message);
            }
        }
    }

    /**
   * Sends the given string to all clients as a text-based message
   */
    public void sendMessageToAllClients(String message) {
        server.sendLobbyMessageToAll(message);
    }

    /**
   * Sends the given string to all clients as a data-based message
   */
    public void sendDataToAllClients(String message) {
        server.sendDataToAll(message);
    }

    /**
   * Creates a text-based list of all users connected and sends it to the client associated with this persona
   */
    protected void sendUserList() {
        sendDataToClient(server.getUserList());
    }

    /**
   * Creates a text-based list of all the existing games and sends it to the client associated with this persona
   */
    protected void sendGamesList() {
        sendDataToClient(server.getGamesList());
    }

    /**
   * This method loops and pings the client as well as checks for new incoming TCP connections
   */
    public void run() {
        try {
            BufferedReader serverIn = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String stringIn;
            while (running) {
                pingService.update();
                if (serverIn.ready() && ((stringIn = serverIn.readLine()) != null)) {
                    interpret(stringIn);
                }
                processOutgoing();
                sleep(sleepTimeMillis);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
   * This method clears out this persona. It is likely called from the server. It will set it to stop looping so
   * that it can finish as a thread as well as clear out the ping service associated with this persona
   */
    protected void clear() {
        running = false;
        if (PICAIUServer.VERBOSE) {
            System.out.println("Stopping ServerPersona (" + getUserName() + ")");
        }
        if (pingService != null) {
            pingService.clear();
        }
        server.clearStartedGames(this);
    }

    /**
   * Sets the username of this client after receiving it over the network.
   * @param nameIn the desired username for the client
   */
    protected void setUserName(String nameIn) {
        userName = nameIn;
    }

    /**
   * This method returns the user name associated with this persona/client.
   */
    protected String getUserName() {
        return userName;
    }

    /**
   * This method sets whether or not this persona believes it is connected to a client.
   */
    protected void setConnected(boolean connectedIn) {
        connected = connectedIn;
    }

    /**
   * This is where most of the work is done by the persona. Any incoming TCP message over the network
   * from the client is interpreted here and processed. A text string will be sent out to all connected
   * users and a data string will control various aspects of this connection.
   * @param message the incoming TCP string
   */
    public String interpret(String message) {
        if (PICAIUServer.VERBOSE) {
            System.out.println("Received Message: " + message);
        }
        if (message.startsWith("t ")) {
            message = message.substring(2, message.length());
            if (message.startsWith("/me ")) {
                server.sendLobbyMessageToAll("*" + getUserName() + " " + message.substring(4, message.length()));
            } else {
                server.sendLobbyMessageToAll(getUserName() + ": " + message);
            }
            return message;
        } else {
            String[] data = message.substring(2, message.length()).split("#");
            if (data[0].equals("SETNAME")) {
                PICAIUDBConnection conn = server.grabDBConn(this);
                if (conn == null) {
                    sendDataToClient("ERR#DB2");
                } else if (data.length > 2 && conn.verifyUserInDB(data[1], data[2], this)) {
                    server.addPersona(this);
                    sendUserList();
                    sendGamesList();
                    server.clearDuplicates(this);
                    sendDataToAllClients("RMVUSR#" + data[1]);
                    sendMessageToAllClients(getUserName() + " is Now Connected to the Server");
                    sendDataToAllClients("JOIN#" + getUserName());
                    sendDataToClient("AUTHOK#" + data[1] + "#" + PICAIUServer.UNITRETURNRATE);
                    setConnected(true);
                } else {
                }
                server.putBackDBConn(conn);
            } else if (data[0].equals("GETMYMONEY")) {
                sendDataToClient("SETMYMONEY#" + getMoney());
            } else if (data[0].equals("TYPES")) {
                sendDataToClient("TYPES#" + getGameTypes());
            } else if (data[0].equals("CREATEGAME")) {
                String pass = "";
                if (data.length > 2) {
                    pass = data[2];
                }
                createGame(data[1], pass);
                sendDataToAllClients("RMVUSR#" + userName);
            } else if (data[0].equals("JOINGM")) {
                String pass = "";
                if (data.length > 2) {
                    pass = data[2];
                }
                joinGame(data[1], pass);
            } else if (data[0].equals("DSCNT")) {
                if (data[1].equals(getUserName())) {
                    server.disconnectClient(this);
                }
            } else if (data[0].equals("PONG")) {
                pingService.pongReceived();
            } else if (data[0].equals("LEFTPREGM")) {
                returnUnits();
                removeGame();
            } else if (data[0].equals("READY")) {
                game.setReady(this);
            } else if (data[0].equals("REC")) {
                int type = 0;
                if (data[1].equals("TAN")) {
                    type = 1;
                } else if (data[1].equals("ROC")) {
                    type = 2;
                }
                int amount = Integer.parseInt(data[2]);
                if (amount > this.money) {
                    amount = money;
                }
                if (amount < 1 || recruitted.size() >= server.MAXUNITS) {
                    return "";
                }
                PICAIUDBConnection conn = server.grabDBConn(this);
                PICAIURecruittedUnit unit = conn.recruitUnit(type, amount, this);
                if (unit == null) {
                    server.putBackDBConn(conn);
                    return "";
                }
                money -= unit.worth;
                this.sendDataToClient("SETMYMONEY#" + money);
                this.sendDataToClient("REC#" + unit.id + "#" + unit.type + "#" + unit.worth + "#" + unit.health + "#" + unit.energy + "#" + unit.vision + "#" + unit.accuracy + "#" + unit.id);
                recruitted.add(unit);
                game.sendToOtherInfo(this);
                server.putBackDBConn(conn);
            } else if (data[0].equals("PING")) {
                if (PICAIUServer.VERBOSE) {
                    System.out.println("SENDING PONG to client");
                }
                sendDataToClient("PONG");
            } else if (data[0].equals("CREATE")) {
                String create;
                PICAIUDBConnection conn = server.grabDBConn(this);
                if ((create = conn.createNewUser(data[1], data[2], data[3])).equals("##OK##")) {
                    sendDataToClient("CREATEOK#" + data[1]);
                } else {
                    sendDataToClient("CREATEFAIL#" + create);
                }
                server.putBackDBConn(conn);
            } else if (data[0].equals("RCVR")) {
                String recover;
                PICAIUDBConnection conn = server.grabDBConn(this);
                if ((recover = conn.recover(data[1], this)).equals("##OK##")) {
                    sendDataToClient("RCVROK#" + data[1]);
                } else {
                    sendDataToClient("RCVRFAIL#" + recover);
                }
                server.putBackDBConn(conn);
            } else if (data[0].equals("VER")) {
                if (data[1].equals(verificationCode)) {
                    if (isVerbose()) {
                        System.out.println("changing password");
                    }
                    PICAIUDBConnection conn = server.grabDBConn(this);
                    String change = conn.changePassword(data[2], data[3]);
                    server.putBackDBConn(conn);
                    if (change.equals("##OK##")) {
                        sendDataToClient("VEROK#");
                    } else {
                        sendDataToClient("VERFAIL#" + change);
                    }
                } else {
                    sendDataToClient("VERFAIL# Invalid verification code  ");
                }
            } else if (data[0].equals("SELL")) {
                long id = Long.parseLong(data[1]);
                PICAIURecruittedUnit unit = null;
                for (PICAIURecruittedUnit u : recruitted) {
                    if (u.id == id) {
                        unit = u;
                        break;
                    }
                }
                this.money += (int) (unit.worth * server.UNITRETURNRATE);
                PICAIUDBConnection conn = server.grabDBConn(this);
                conn.putSoldierBack(id);
                server.putBackDBConn(conn);
                sendDataToClient("SETMYMONEY#" + getMoney());
                sendDataToClient("RMUNIT#" + data[1] + "#" + unit.getType());
                recruitted.remove(unit);
            } else if (data[0].equals("CONTUNTRSFR")) {
                sendInitialUnitsToClient();
            } else if (data[0].equals("GETGMSLIST")) {
                sendGamesList();
            } else if (data[0].equals("FP")) {
                if (placed < recruitted.size()) {
                    for (PICAIURecruittedUnit u : recruitted) {
                        if (u.id == Long.parseLong(data[1])) {
                            u.setX(Float.parseFloat(data[2]));
                            u.setY(Float.parseFloat(data[3]));
                            placed++;
                        }
                    }
                }
                if (placed == recruitted.size()) {
                    game.setFinishedPlacing(this);
                }
            } else if (data[0].equals("ITR")) {
                game.setFinishedRetrievingEnemyTroops(this);
            } else if (data[0].equals("BOU")) {
                game.addBounty(data[1], data[2], data[3], data[4], data[5], data[6]);
            }
            return data[0];
        }
    }

    /**
   * This method is necessary if this class has a pingService. The service will call this
   * to ping the server as necessary.
   * Overridden from PICAIUCanPing
   */
    @Override
    public void ping() {
        if (PICAIUServer.VERBOSE) {
            System.out.println("SENDING PING to server");
        }
        sendDataToClient("PING");
    }

    /**
   * Overridden from PICAIUCanPing interface and used to notify clientComm that a pong has not been received
   * within the time constraints.
   * Overridden from PICAIUCanPing
   */
    @Override
    public void pongNotReceived() {
        if (game.isRunning()) {
            return;
        }
        disconnect();
    }

    /**
   * Used to sever client/server relationship. It is likely that this is only called when a pong has not been received.
   */
    public void disconnect() {
        sendDataToClient("DSCNT");
        server.disconnectClient(this);
    }

    /**
   * Returns whether the persona believes it is connected to a client
   */
    public boolean isConnected() {
        return true;
    }

    /**
   * Returns whether debugging output should be displayed to the command line.
   * Overridden from PICAIUCanPing
   */
    @Override
    public boolean isVerbose() {
        return PICAIUServer.VERBOSE;
    }

    protected String getMoney() {
        return "" + money;
    }

    private String getGameTypes() {
        return server.getGameTypes();
    }

    private void createGame(String amount, String password) {
        if (money >= Integer.parseInt(amount.substring(0, amount.indexOf("-")))) {
            sendDataToClient("CREATEGAME");
            game = server.addGame(this, amount, password);
            sendDataToAllClients("ADDGAME#" + userName + "#" + amount);
        } else {
            sendDataToClient("CREATEGAMEFAILED");
        }
    }

    private void joinGame(String name, String pass) {
        PICAIUGame g = server.getGame(name);
        if (g.getPassword().equals(pass) && money >= g.getMinGameAmount()) {
            g.playerJoins(this);
        }
    }

    public void setMoney(int mon) {
        if (mon < 200) {
            money = 200;
        } else {
            money = mon;
        }
    }

    void setGame(PICAIUGame g) {
        game = g;
    }

    protected boolean isInGame() {
        return game != null;
    }

    protected void removeGame() {
        if (game == null) {
            return;
        }
        game.notifyOtherOfLeaving(this);
        game = null;
    }

    protected void setId(int idNext) {
        id = idNext;
    }

    protected int getPersonaId() {
        return id;
    }

    protected void returnUnits() {
        PICAIUDBConnection conn = server.grabDBConn(this);
        for (PICAIURecruittedUnit unit : recruitted) {
            conn.putSoldierBack(unit.id);
            this.money += (int) (unit.worth * server.UNITRETURNRATE);
        }
        recruitted.clear();
        sendDataToClient("SETMYMONEY#" + getMoney());
        server.putBackDBConn(conn);
    }

    protected int getNumberOfUnitsTotal() {
        return recruitted.size();
    }

    private void sendInitialUnitsToClient() {
        for (PICAIURecruittedUnit u : game.getRedPersona().recruitted) {
            sendDataToClient("AIU#R#" + u.getBasicDataString());
        }
        for (PICAIURecruittedUnit u : game.getBluePersona().recruitted) {
            sendDataToClient("AIU#B#" + u.getBasicDataString());
        }
    }

    String getTeam() {
        return game.getTeam(this);
    }

    void createReferences() {
        for (PICAIURecruittedUnit u : recruitted) {
            recruittedRefs.put(u.id, u);
        }
    }

    PICAIURecruittedUnit getUnit(String id) {
        return recruittedRefs.get(Long.parseLong(id));
    }

    boolean isRedTeam() {
        if (game.getRedPersona() == this) {
            return true;
        }
        return false;
    }

    private void processOutgoing() {
        ConcurrentLinkedQueue events = outputQueue.getQueue();
        while (!events.isEmpty()) {
            Object e = events.poll();
            sendDataToClient((String) e);
        }
    }

    void sendCCDataToClient(String string) {
        outputQueue.add(string);
    }

    PICAIUGame getGame() {
        return game;
    }
}
