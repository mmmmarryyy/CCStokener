package jmIrcWordGamesBot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import org.jibble.pircbot.DccFileTransfer;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

public class JmIrcWordGamesBot extends PircBot {

    protected String boggleChannel, scrabbleChannel;

    protected final String[] words;

    protected String[] channels;

    private boolean overwriteFlag, debug;

    private int roll;

    private Vector hangmanUsers, scrabbleGames, dictUsers;

    private Hashtable definitions, dictUserStages, pendingApprovals, currentlyOnline, wrdindx;

    private HashMap msgStore;

    public static final int TYPE_SCRABBLE = 0, TYPE_BOGGLE = 1, MSG_LIMIT = 12;

    private char[][] boggleGrid;

    private boolean boggleGame, boggleTimeout, boggleStarted;

    private Timer timer;

    private Vector boggleUsers, boggleUserWordList, boggleWinners, crosswords, ignoreList;

    private Hashtable boggleWords, antiFloodTable, antiFloodTimerTable;

    private int boggleHighScore;

    private BoggleCountdown boggleCountdown;

    TEAEncryptor tea;

    private static final char[][] cubes = { { 'B', 'B', 'A', 'O', 'O', 'J' }, { 'P', 'O', 'H', 'C', 'A', 'S' }, { 'E', 'Y', 'T', 'L', 'T', 'R' }, { 'L', 'R', 'E', 'I', 'X', 'D' }, { 'M', 'U', 'Q', 'H', 'I', 'N' }, { 'U', 'M', 'C', 'O', 'I', 'T' }, { 'S', 'I', 'O', 'S', 'T', 'E' }, { 'N', 'R', 'N', 'Z', 'L', 'H' }, { 'T', 'S', 'T', 'I', 'D', 'Y' }, { 'F', 'P', 'K', 'A', 'F', 'S' }, { 'E', 'E', 'H', 'N', 'G', 'W' }, { 'E', 'U', 'S', 'E', 'N', 'I' }, { 'E', 'Y', 'L', 'D', 'V', 'R' }, { 'V', 'E', 'H', 'W', 'T', 'R' }, { 'A', 'G', 'A', 'E', 'N', 'E' }, { 'O', 'W', 'T', 'O', 'A', 'T' } };

    public JmIrcWordGamesBot(String[] channels, String name) {
        if (channels[0] != null) this.channels = channels; else this.channels = new String[] { "#wordgames" };
        setName("ScrabbleBot");
        setAutoNickChange(true);
        setVersion("ELSBOT");
        setMessageDelay(1000);
        debug = true;
        tea = new TEAEncryptor();
        roll = 0;
        hangmanUsers = new Vector();
        scrabbleChannel = "#wordgames";
        scrabbleGames = new Vector();
        pendingApprovals = new Hashtable();
        boggleChannel = "#wordgames";
        boggleUsers = new Vector();
        boggleUserWordList = new Vector();
        boggleWinners = new Vector();
        boggleWords = new Hashtable();
        boggleTimeout = false;
        boggleStarted = false;
        boggleHighScore = 0;
        boggleCountdown = new BoggleCountdown();
        dictUsers = new Vector();
        definitions = new Hashtable();
        dictUserStages = new Hashtable();
        words = loadWordList("wordlist.txt");
        wrdindx = new Hashtable(25);
        wrdindx = createWrdIndx(wrdindx);
        crosswords = loadCrosswordFiles();
        overwriteFlag = false;
        msgStore = new HashMap();
        antiFloodTable = new Hashtable();
        antiFloodTimerTable = new Hashtable();
        ignoreList = new Vector();
        currentlyOnline = new Hashtable();
        timer = new Timer();
        timer.schedule(boggleCountdown, 0, 1000);
    }

    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("logs\\" + channel + ".log", true));
            out.write(System.currentTimeMillis() + ":" + sender + ":" + message + "\r\n");
            out.close();
        } catch (IOException e) {
        }
        if (!this.getNick().equals(sender) && !ignoreList.contains(sender)) {
            if (message.equalsIgnoreCase("roll")) {
                User[] users = getUsers(channel);
                do {
                    roll = (int) (Math.round(Math.random() * (users.length - 1)));
                } while (users[roll].equals(this.getNick()));
                sendMessage(channel, sender + ": roll " + users[roll].getNick());
            } else if (message.equalsIgnoreCase("time")) {
                String time = new java.util.Date().toString();
                sendMessage(channel, sender + ": The time is now " + time);
            } else if ((message.equalsIgnoreCase("new boggle game")) || (message.equalsIgnoreCase("join boggle"))) approveModUser(sender, message);
        } else {
            kick(channel, sender);
        }
    }

    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("logs\\" + sender + ".log", true));
            out.write(System.currentTimeMillis() + ":" + message + "\r\n");
            out.close();
        } catch (IOException e) {
        }
        if (!ignoreList.contains(sender)) {
            doAntiFlood(sender);
            if (message.startsWith("!s")) parseScrabble(sender, message); else if (getHangman(sender) != null && message.startsWith("!h")) playHangman(sender, message); else if (message.toLowerCase().startsWith("translate")) translate(sender, message); else if (message.toLowerCase().startsWith("define")) dictLookup(sender, message); else if (message.equalsIgnoreCase("hangman")) startHangman(sender); else if ((message.toLowerCase().startsWith("new scrabble game")) || (message.toLowerCase().startsWith("join scrabble game"))) approveModUser(sender, message); else if (message.equalsIgnoreCase("start scrabble")) startScrabble(sender); else if (message.equalsIgnoreCase("leave scrabble")) leaveScrabble(sender); else if (message.startsWith("!b")) parseBoggle(sender, message); else if (message.toLowerCase().startsWith("download crossword")) getCrossword(sender, message, crosswords); else if (message.toLowerCase().startsWith("tell")) storeMsg(sender, message); else if (message.equalsIgnoreCase("list crosswords")) listCrosswords(sender, crosswords); else if (message.equalsIgnoreCase("enable overwrite")) setOverwrite(true, sender); else if (message.equalsIgnoreCase("disable overwrite")) setOverwrite(false, sender); else if (message.equalsIgnoreCase("overwrite")) getOverwrite(sender); else if (message.equalsIgnoreCase("check messages")) checkMsgs(sender, msgStore); else if ((message.equalsIgnoreCase("new boggle game")) || (message.equalsIgnoreCase("join boggle"))) sendMessage(sender, "Join a boggle game in the " + boggleChannel + " channel"); else if (message.toLowerCase().equals("y")) {
                if (!sendDefinition(sender)) playHangman(sender, message);
            } else if (message.toLowerCase().equals("n")) {
                if (!cleanUpDefinition(sender)) playHangman(sender, message);
            } else if (getHangman(sender) != null) playHangman(sender, message);
            if (message.toLowerCase().startsWith("highscores")) {
                highScores(sender, message);
            }
        }
    }

    protected void onDisconnect() {
        try {
            reconnect();
            for (int i = 0; i < channels.length; i++) {
                joinChannel(channels[i]);
            }
            joinChannel(scrabbleChannel);
            joinChannel(boggleChannel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onIncomingFileTransfer(DccFileTransfer transfer) {
        File file = new File("crosswords\\" + transfer.getFile().getName());
        String filename = file.getName();
        String[] nameAndExtension = splitString(filename, ".");
        if (nameAndExtension[nameAndExtension.length - 1].equalsIgnoreCase("ECW") && transfer.getSize() != -1 && transfer.getSize() < 5120) {
            if (!file.exists()) {
                transfer.receive(file, false);
            } else {
                if (overwriteFlag) {
                    file.delete();
                    transfer.receive(file, false);
                    sendMessage(transfer.getNick(), file.getName() + " already exists, but has been overwritten.  To disable file overwriting send me a private message " + "\"disable overwrite\" (Only approved nicks such as yours can do this).");
                } else {
                    sendMessage(transfer.getNick(), file.getName() + " already exists.  To enable file overwriting send me a private message " + "\"enable overwrite\" (Only approved nicks such as yours can do this).");
                }
            }
        } else sendMessage(transfer.getNick(), "Transfer denied");
    }

    protected void onFileTransferFinished(DccFileTransfer transfer, Exception e) {
        crosswords.addElement(loadCrosswordFile(transfer.getFile().getPath()));
    }

    protected void onJoin(String channel, String sender, String login, String hostname) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("logs\\" + channel + ".log", true));
            out.write(System.currentTimeMillis() + ":" + sender + " joined the channel\r\n");
            out.close();
        } catch (IOException e) {
        }
        addDBUser(channel, sender);
        checkMsgs(sender, msgStore);
    }

    protected void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {
        String code = notice.substring(10, notice.length() - 1);
        if (pendingApprovals.containsKey(sourceNick) && code.equals(tea.getCipherRPL())) {
            String msg = (String) pendingApprovals.get(sourceNick);
            if (msg.toLowerCase().startsWith("new scrabble game")) createScrabble(sourceNick, msg); else if (msg.toLowerCase().startsWith("join scrabble game")) joinScrabble(sourceNick, msg); else if (msg.equalsIgnoreCase("new boggle game")) createBoggle(sourceNick); else if (msg.equalsIgnoreCase("join boggle")) joinBoggle(sourceNick);
        }
    }

    protected void onPart(String channel, String sender, String login, String hostname) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("logs\\" + channel + ".log", true));
            out.write(System.currentTimeMillis() + ":" + sender + " left the channel\r\n");
            out.close();
        } catch (IOException e) {
        }
        ((UserLogins) currentlyOnline.get(sender)).setChannelLogout(channel);
        boolean leftAllChannels = true;
        for (int i = 0; i < channels.length; i++) {
            if (userExistsInChannel(sender, channels[i])) {
                leftAllChannels = false;
                break;
            }
        }
        if (leftAllChannels) {
            doDBUserQuit(channel, sender);
        }
        leaveScrabble(sender);
        removeBogglePlayer(sender);
    }

    protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("logs\\" + sourceNick + ".log", true));
            out.write(System.currentTimeMillis() + ":" + sourceNick + ":Quit->" + reason + "\r\n");
            out.close();
        } catch (IOException e) {
        }
        leaveScrabble(sourceNick);
        removeBogglePlayer(sourceNick);
        String channel = findUserChannel(sourceNick);
        doDBUserQuit(channel, sourceNick);
    }

    protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
        leaveScrabble(oldNick);
        removeBogglePlayer(oldNick);
        String channel = findUserChannel(newNick);
        doDBUserQuit(channel, oldNick);
        addDBUser(channel, newNick);
    }

    protected void onUserList(String channel, User[] users) {
        for (int j = 0; j < users.length; j++) {
            addDBUser(channel, users[j].getNick());
        }
    }

    private Hashtable createWrdIndx(Hashtable h) {
        if (debug) System.out.println("Creating word list hash:");
        int j = 0;
        h.put("a", new Integer(0));
        for (int i = 0; i < words.length - 2; i++) {
            String s = words[i].substring(0, 1);
            String t = words[i + 1].substring(0, 1);
            if (!s.equalsIgnoreCase(t)) {
                h.put(t, new Integer(i + 1));
                j++;
            }
        }
        return h;
    }

    private String findUserChannel(String nick) {
        for (int i = 0; i < channels.length; i++) {
            User[] u = getUsers(channels[i]);
            for (int j = 0; j < u.length; j++) {
                if (nick.equals(u[j].getNick())) return channels[i];
            }
        }
        return null;
    }

    protected void addDBUser(String channel, String sender) {
        if (!currentlyOnline.containsKey(sender)) {
            currentlyOnline.put(sender, new UserLogins(channel, sender));
        } else {
            ((UserLogins) currentlyOnline.get(sender)).addChannelLogin(channel);
        }
    }

    private void doDBUserQuit(String channel, String sender) {
        UserLogins ul = (UserLogins) currentlyOnline.get(sender);
        String[] s = new String[ul.channelLogins.size() + 3];
        s[0] = "CREATE TABLE IF NOT EXISTS " + sender + "Connections (loginTime BIGINT, logoutTime BIGINT)";
        s[1] = "CREATE TABLE IF NOT EXISTS " + sender + "Joins (channel VARCHAR(25), joinTime BIGINT, leaveTime BIGINT)";
        s[2] = "INSERT INTO " + sender + "Connections VALUES (" + ul.loginTime + "," + System.currentTimeMillis() + ")";
        for (int i = 0; i < ul.channelLogins.size(); i++) {
            ChannelLoginTimes clt = (ChannelLoginTimes) ul.channelLogins.elementAt(i);
            if (clt.logoutTime == -1) {
                s[i + 3] = "INSERT INTO " + sender + "Joins VALUES ('" + clt.name + "'," + clt.loginTime + "," + System.currentTimeMillis() + ")";
            } else {
                s[i + 3] = "INSERT INTO " + sender + "Joins VALUES ('" + clt.name + "'," + clt.loginTime + "," + clt.logoutTime + ")";
            }
        }
    }

    private void startHangman(String sender) {
        if (getHangman(sender) == null) {
            timer.schedule(new HangmanTimer(sender), 900000);
            int rnd = (int) (Math.round(Math.random() * (words.length - 1)));
            HangmanUser hu = new HangmanUser(sender, words[rnd]);
            hangmanUsers.addElement(hu);
            drawGallows(hu);
        }
    }

    private void playHangman(String sender, String message) {
        HangmanUser hu;
        if ((hu = getHangman(sender)) != null) {
            if (message.length() == 1) {
                for (int j = 0; j < hu.hangmanGuess.length; j++) {
                    if (hu.word.substring(j, j + 1).equalsIgnoreCase(message)) {
                        hu.hangmanGuess[j] = message.charAt(0);
                        hu.guessBoolean = true;
                    }
                }
                if (hu.guessBoolean) hu.guessBoolean = false; else hu.guessNo--;
                drawGallows(hu);
            } else {
                sendMessage(sender, "Please guess a letter");
            }
        }
    }

    private void drawGallows(HangmanUser hu) {
        String hangmanGuessString = "";
        if (hu.guessNo <= 0) {
            hu.guessNo = 0;
            hangmanGuessString = hu.word;
            hangmanUsers.remove(hu);
        } else {
            for (int j = 0; j < hu.hangmanGuess.length; j++) {
                hangmanGuessString += hu.hangmanGuess[j];
            }
            if (!hangmanGuessString.contains("-")) {
                hangmanGuessString = "You guessed it!: " + hangmanGuessString;
                hangmanUsers.remove(hu);
            }
        }
        sendMessage(hu.getNick(), hangmanGuessString.toLowerCase() + ":" + hu.guessNo);
    }

    private HangmanUser getHangman(String nick) {
        for (int i = 0; i < hangmanUsers.size(); i++) {
            HangmanUser x = (HangmanUser) hangmanUsers.elementAt(i);
            if (x.getNick().equals(nick)) return x;
        }
        return null;
    }

    private void createScrabble(String nick, String message) {
        if (playerExistsAt(nick, scrabbleGames, TYPE_SCRABBLE) != -1) sendMessage(nick, "Your nick is already in a scrabble game"); else if (gameExistsAt(message.substring(18), scrabbleGames, TYPE_SCRABBLE) != -1) {
            sendMessage(nick, "A game by that name already exists");
        } else {
            if (message.length() <= 18) sendMessage(nick, "To create a new scrabble game type: NEW SCRABBLE GAME <name>"); else {
                if (userExistsInChannel(nick, scrabbleChannel)) {
                    Scrabble sg = new Scrabble(message.substring(18));
                    sg.addPlayer(nick);
                    scrabbleGames.addElement(sg);
                    sendMessage(nick, "Scrabble game " + sg.getName() + " created");
                } else {
                    sendMessage(nick, "Please join channel " + scrabbleChannel + " and try again");
                }
            }
        }
    }

    private void startScrabble(String nick) {
        int gameNo = playerExistsAt(nick, scrabbleGames, TYPE_SCRABBLE);
        if (gameNo > -1) {
            Scrabble sg = (Scrabble) scrabbleGames.elementAt(gameNo);
            GameUser[] sgusers = sg.getPlayers();
            if (!nick.equals(sgusers[0].getNick())) sendMessage(nick, "Only the scrabble game creator can start the game"); else {
                sg.setStarted(true);
                for (int i = 0; i < sgusers.length; i++) {
                    if (sgusers[i] != null) {
                        String letters = "";
                        for (int j = 0; j < 7; j++) letters += sg.pickUp();
                        sendMessage(sgusers[i].getNick(), "!s?!begin?" + letters + "?null?" + sgusers[0].getNick());
                    }
                }
            }
        } else sendMessage(nick, "You are not in a scrabble game");
    }

    private void joinScrabble(String nick, String message) {
        if (userExistsInChannel(nick, scrabbleChannel)) {
            if (playerExistsAt(nick, scrabbleGames, TYPE_SCRABBLE) != -1) sendMessage(nick, "Your nick is already in a scrabble game"); else {
                if (message.length() <= 19) sendMessage(nick, "To join a scrabble game type: JOIN SCRABBLE GAME <name>"); else {
                    String gamename = message.substring(19);
                    int pos = gameExistsAt(gamename, scrabbleGames, TYPE_SCRABBLE);
                    if (pos == -1) sendMessage(nick, "I am not running any scrabble games by the name of " + gamename + " at this moment"); else {
                        Scrabble game = (Scrabble) scrabbleGames.elementAt(pos);
                        if (!game.isStarted()) {
                            if (!game.addPlayer(nick)) sendMessage(nick, "That game is already full"); else {
                                sendMessage(nick, "You have joined game " + game.getName());
                                GameUser[] gameusers = game.getPlayers();
                                for (int i = 0; i < gameusers.length; i++) {
                                    if (gameusers[i] != null && !gameusers[i].getNick().equals(nick)) sendMessage(gameusers[i].getNick(), nick + " has joined scrabble game " + game.getName());
                                }
                            }
                        } else sendMessage(nick, "That game has already started.");
                    }
                }
            }
        } else {
            sendMessage(nick, "Please join channel " + scrabbleChannel + " and try again");
        }
    }

    private void leaveScrabble(String nick) {
        int i = playerExistsAt(nick, scrabbleGames, TYPE_SCRABBLE);
        if (i > -1) {
            Scrabble sg = (Scrabble) scrabbleGames.elementAt(i);
            GameUser[] sgusers = sg.getPlayers();
            boolean empty = true;
            boolean creatorLeaving = false;
            int leavingPlayer = -1;
            for (int x = 0; x < sgusers.length; x++) {
                if (sgusers[x] != null) {
                    if (!sg.isStarted()) {
                        sendMessage(sgusers[x].getNick(), nick + " has left scrabble");
                    }
                    if (sgusers[x].getNick().equals(nick)) {
                        leavingPlayer = x;
                        if (x == 0) {
                            creatorLeaving = true;
                        }
                    } else empty = false;
                }
            }
            if (empty || (creatorLeaving && !sg.isStarted())) {
                sendMessage(nick, "Scrabble game " + sg.getName() + " has been removed");
                scrabbleGames.removeElementAt(i);
            } else if (sg.isStarted()) {
                if (nick.equals(sgusers[sg.getPlayerTurn()].getNick())) {
                    sgusers[sg.getPlayerTurn()] = null;
                    sg.incrPlayerTurn();
                    for (int x = 0; x < sgusers.length; x++) {
                        if (sgusers[x] != null) {
                            sendMessage(sgusers[x].getNick(), "!s??0?" + nick + " !x?" + sgusers[sg.getPlayerTurn()].getNick());
                        }
                    }
                } else {
                    if (leavingPlayer != -1) {
                        sgusers[leavingPlayer] = null;
                    }
                    sg.incrPlayerTurn();
                    for (int x = 0; x < sgusers.length; x++) {
                        if (sgusers[x] != null) {
                            sendMessage(sgusers[x].getNick(), nick + " has left scrabble");
                        }
                    }
                }
                sendMessage(nick, "You have left scrabble");
            } else {
                if (leavingPlayer != -1) {
                    sgusers[leavingPlayer] = null;
                }
            }
        }
    }

    private void parseScrabble(String nick, String message) {
        boolean invalid = false;
        String incorrectWords = "";
        int p = playerExistsAt(nick, scrabbleGames, TYPE_SCRABBLE);
        if (p > -1) {
            Scrabble sg = (Scrabble) scrabbleGames.elementAt(p);
            GameUser[] sgusers = sg.getPlayers();
            int prevUserTurn = sg.getPlayerTurn();
            String[] tempCodes = splitString(message, "?");
            if (sgusers[prevUserTurn] != null && sgusers[prevUserTurn].getNick().equals(nick)) {
                String[] scrabbleWords;
                if (tempCodes[1].equals("!c")) {
                    if (!tempCodes[2].equals("!p")) {
                        scrabbleWords = tempCodes[2].split("~");
                        for (int i = 0; i < scrabbleWords.length; i++) {
                            if (!wordIsValid(scrabbleWords[i])) {
                                invalid = true;
                                if (incorrectWords.equals("")) incorrectWords = scrabbleWords[i]; else incorrectWords += " " + scrabbleWords[i];
                            }
                        }
                        if (!invalid) {
                            String[] s = new String[scrabbleWords.length];
                            for (int i = 0; i < scrabbleWords.length; i++) {
                                s[i] = "INSERT INTO scrabbleWords VALUES ('" + scrabbleWords[i] + "', 1) ON DUPLICATE KEY UPDATE Frequency = Frequency + 1";
                            }
                        }
                        for (int i = 0; i < sgusers.length; i++) {
                            if (sgusers[i] != null && sgusers[i].getNick().equals(nick)) sgusers[i].setScore(0);
                        }
                    } else if (tempCodes[3].equals("")) {
                        int count = 0;
                        for (int i = 0; i < sgusers.length; i++) {
                            if (sgusers[i] != null) count++;
                        }
                        boolean allPassed = true;
                        for (int i = 0; i < sgusers.length; i++) {
                            if (sgusers[i] != null && sgusers[i].getNick().equals(nick)) sgusers[i].setScore(-1); else if (sgusers[i] != null && sgusers[i].getScore() != -1) {
                                if (allPassed) allPassed = false;
                            }
                        }
                        if (allPassed) {
                            for (int i = 0; i < sgusers.length; i++) {
                                if (sgusers[i] != null) sendMessage(sgusers[i].getNick(), "!s?!e");
                            }
                            return;
                        }
                    } else {
                        for (int i = 0; i < sgusers.length; i++) {
                            if (sgusers[i] != null && sgusers[i].getNick().equals(nick)) {
                                sgusers[i].setScore(0);
                            }
                        }
                    }
                    sg.updatePlayers(sgusers);
                    if (invalid) {
                        sendMessage(nick, "!s?!c?" + incorrectWords);
                        invalid = false;
                        return;
                    } else {
                        String[] newGridRefs = tempCodes[3].split("~");
                        String newLetters = "";
                        for (int i = 0; i < newGridRefs.length; i++) {
                            if (!sg.tileBag.isEmpty()) newLetters += sg.pickUp();
                        }
                        int score = Integer.parseInt(tempCodes[4]);
                        sg.incrPlayerTurn();
                        for (int i = 0; i < sgusers.length; i++) {
                            if (sgusers[i] != null) {
                                if (sgusers[i].getNick().equals(nick)) {
                                    sendMessage(nick, "!s?!c!valid?" + score + "?" + newLetters + "?" + sgusers[sg.getPlayerTurn()].getNick());
                                } else sendMessage(sgusers[i].getNick(), "!s" + "?" + tempCodes[3] + "?" + score + "?" + sgusers[prevUserTurn].getNick() + "?" + sgusers[sg.getPlayerTurn()].getNick());
                            }
                        }
                    }
                }
            }
            if (tempCodes[1].equals("!q")) {
                int count = 0;
                if (tempCodes.length == 3) {
                    for (int i = 0; i < sgusers.length; i++) {
                        if (sgusers[i] != null) {
                            if (sgusers[i].endMsgRecieved == true) {
                                if (debug) System.out.println("EndMsg already Recieved:" + sgusers[i].getNick());
                                count++;
                            } else if (sgusers[i].getNick().equals(nick)) {
                                sgusers[i].setScore(Integer.valueOf(tempCodes[2]).intValue());
                                sgusers[i].endMsgRecieved = true;
                                count++;
                            }
                        } else {
                            count++;
                        }
                    }
                    if (count == 4) {
                        Vector winners = new Vector();
                        Vector v = new Vector();
                        int highscore = 0;
                        for (int i = 0; i < 4; i++) {
                            if (sgusers[i] != null && sgusers[i].getScore() > 0) {
                                if (sgusers[i].getScore() > highscore) {
                                    winners.removeAllElements();
                                    highscore = sgusers[i].getScore();
                                }
                                if (sgusers[i].getScore() >= highscore) {
                                    winners.addElement(sgusers[i]);
                                }
                                v.addElement(sgusers[i]);
                            }
                        }
                        String[] s = new String[v.size()];
                        String str = "";
                        for (int i = 0; i < s.length; i++) {
                            boolean winner = false;
                            GameUser gu = ((GameUser) v.elementAt(i));
                            for (int j = 0; j < winners.size(); j++) {
                                if (gu.equals((GameUser) winners.elementAt(j))) {
                                    s[i] = "INSERT INTO personal VALUES ('" + gu.getNick() + "',NULL,NULL,1,0,0,0,0,1,0," + gu.getScore() + ",0," + gu.getScore() + ",0) ON DUPLICATE KEY UPDATE ScrabbleGames = ScrabbleGames +1,ScrabbleWins = ScrabbleWins + 1, ScrabbleHighScore = GREATEST(ScrabbleHighScore," + gu.getScore() + "), ScrabbleFullScore = ScrabbleFullScore + " + gu.getScore();
                                    str += gu.getNick() + " ";
                                    winner = true;
                                }
                            }
                            if (winner == false) {
                                s[i] = "INSERT INTO personal VALUES ('" + gu.getNick() + "',NULL,NULL,1,0,0,0,0,0,0," + gu.getScore() + ",0," + gu.getScore() + ",0) ON DUPLICATE KEY UPDATE ScrabbleGames = ScrabbleGames + 1, ScrabbleHighScore = GREATEST(ScrabbleHighScore," + gu.getScore() + "), ScrabbleFullScore = ScrabbleFullScore + " + gu.getScore();
                            }
                        }
                        sendMessage(scrabbleChannel, sg.getName() + " has ended.  " + str + "won the game with " + highscore + " points.");
                        scrabbleGames.removeElementAt(p);
                    }
                } else {
                    leaveScrabble(nick);
                }
            }
        }
    }

    private int gameExistsAt(String name, Vector games, int type) {
        if (type == TYPE_SCRABBLE) {
            for (int i = 0; i < games.size(); i++) {
                Scrabble scrabble = (Scrabble) games.elementAt(i);
                if (name.equalsIgnoreCase(scrabble.getName())) return i;
            }
        }
        return -1;
    }

    private int playerExistsAt(String nick, Vector games, int type) {
        if (type == TYPE_SCRABBLE) {
            for (int i = 0; i < games.size(); i++) {
                Scrabble scrabble = (Scrabble) games.elementAt(i);
                GameUser[] gus = scrabble.getPlayers();
                for (int j = 0; j < gus.length; j++) {
                    if (gus[j] != null) {
                        if (nick.equals(gus[j].getNick())) return i;
                    }
                }
            }
        } else if (type == TYPE_BOGGLE) {
            for (int i = 0; i < games.size(); i++) {
                if (nick.equals((String) boggleUsers.elementAt(i))) return i;
            }
        }
        return -1;
    }

    private boolean wordIsValid(String word) {
        for (int i = ((Integer) wrdindx.get(word.substring(0, 1).toLowerCase())).intValue(); i < words.length; i++) {
            if (word.equalsIgnoreCase(words[i])) return true;
        }
        return false;
    }

    private void createBoggle(String sender) {
        if (!boggleGame) {
            if (userExistsInChannel(sender, boggleChannel)) {
                sendMessage(boggleChannel, "boggle will start in 1 minute");
                joinBoggle(sender);
                boggleTimeout = false;
                boggleCountdown.initialiseBoggleCountdown();
                boggleGame = true;
            } else sendMessage(sender, "Please join " + boggleChannel + " and try again");
        } else sendMessage(sender, "boggle game already started");
    }

    private void joinBoggle(String sender) {
        if (playerExistsAt(sender, boggleUsers, TYPE_BOGGLE) != -1) sendMessage(sender, "Your nick is already registered for this game"); else {
            if (boggleStarted) {
                sendMessage(sender, "Boggle has already started");
            } else if (userExistsInChannel(sender, boggleChannel)) {
                boggleUsers.addElement(sender);
                sendMessage(sender, "You have joined boggle");
            } else sendMessage(sender, "Please join " + boggleChannel + " and try again");
        }
    }

    private void startBoggle(Vector boggleUsers) {
        boggleStarted = true;
        boggleGrid = setBoggleGrid();
        String gridString = "";
        for (int i = 0; i < boggleGrid.length; i++) {
            for (int j = 0; j < boggleGrid[i].length; j++) gridString += boggleGrid[i][j];
        }
        for (int i = 0; i < boggleUsers.size(); i++) sendMessage((String) boggleUsers.elementAt(i), "!b?" + gridString);
    }

    private void parseBoggle(String sender, String message) {
        String[] tokens = splitString(message, "?");
        if (tokens.length == 2 && playerExistsAt(sender, boggleUsers, TYPE_BOGGLE) != -1) {
            if (debug) System.out.println("Checking reply " + tokens[1]);
            if (tokens[1].equals("!q")) {
                removeBogglePlayer(sender);
            } else if (allBoggleRepliesDone(sender, tokens[1]) && !boggleTimeout) {
                sendBoggleReplies();
            }
        }
    }

    private void removeBogglePlayer(String nick) {
        if (!boggleWords.containsKey(nick) && boggleUsers.contains(nick)) {
            boggleUsers.removeElement(nick);
            if (debug) System.out.println(nick + " removed from boggle game");
            if (boggleUsers.size() == 0 && boggleWords.size() == 0) {
                boggleGame = false;
                boggleTimeout = false;
                boggleStarted = false;
                sendMessage(boggleChannel, "boggle has ended");
            }
        }
    }

    private void sendBoggleReplies() {
        doBoggleReplies(boggleUsers, boggleWords);
        boggleUsers.removeAllElements();
        boggleWinners.removeAllElements();
        boggleWords.clear();
        boggleGame = false;
        boggleTimeout = false;
        boggleStarted = false;
        boggleHighScore = 0;
    }

    private boolean allBoggleRepliesDone(String sender, String words) {
        if (!boggleWords.containsKey(sender)) {
            boggleWords.put(sender, words);
            if (debug) System.out.println("player reply added to Hashtable");
        }
        for (int i = 0; i < boggleUsers.size(); i++) {
            if (!boggleWords.containsKey((String) boggleUsers.elementAt(i))) {
                return false;
            }
        }
        if (debug) System.out.println("All boggle replies recieved");
        return true;
    }

    private void doBoggleReplies(Vector boggleUsers, Hashtable boggleWords) {
        if (!boggleUsers.isEmpty()) {
            String[] words;
            for (int i = 0; i < boggleUsers.size(); i++) {
                if (boggleWords.containsKey((String) boggleUsers.elementAt(i))) {
                    words = splitString((String) boggleWords.get(boggleUsers.elementAt(i)), " ");
                    for (int j = 0; j < words.length; j++) this.boggleUserWordList.addElement(words[j]);
                }
            }
            for (int i = 0; i < boggleUsers.size(); i++) {
                String boggleUser = (String) boggleUsers.elementAt(i);
                if (boggleWords.containsKey(boggleUser)) {
                    String reply = checkBoggleWords((String) boggleWords.get(boggleUsers.elementAt(i)), boggleUser);
                    sendMessage((String) boggleUsers.elementAt(i), reply);
                } else if (debug) System.out.println("Invalid call to doBoggleReplies(): No such boggle user in Hashtable:" + ((String) boggleUsers.elementAt(i)));
            }
            String boggleStatsMsg = "Boggle has ended. ";
            if (boggleWinners.size() > 0) {
                boggleStatsMsg += " " + (String) boggleWinners.elementAt(0);
                Vector v = new Vector();
                for (int i = 1; i < boggleWinners.size(); i++) {
                    String winner = (String) boggleWinners.elementAt(i);
                    boggleStatsMsg += ", " + winner;
                    v.addElement("UPDATE personal SET BoggleWins = BoggleWins + 1 WHERE Nick = '" + winner + "'");
                }
                String[] s = new String[v.size()];
                for (int i = 0; i < s.length; i++) {
                    s[i] = (String) v.elementAt(i);
                }
                boggleStatsMsg += " won the game.  High score was " + boggleHighScore;
            }
            sendMessage(boggleChannel, boggleStatsMsg);
        }
    }

    private boolean hasDuplicate(String word, Vector wordList) {
        int count = 0;
        for (int i = 0; i < wordList.size(); i++) {
            if (word.equalsIgnoreCase((String) wordList.elementAt(i))) {
                count++;
            }
            if (count > 1) return true;
        }
        return false;
    }

    private String checkBoggleWords(String message, String boggleUser) {
        if (debug) System.out.println("checking boggle words...");
        String[] words = splitString(message, " ");
        String reply = "!b?";
        int score = 0;
        Vector validWords = new Vector();
        for (int i = 0; i < words.length; i++) {
            if (wordIsValid(words[i])) {
                if (hasDuplicate(words[i], boggleUserWordList)) {
                    reply += "2";
                } else {
                    reply += "1";
                    int chars = words[i].length();
                    switch(chars) {
                        case 3:
                        case 4:
                            score++;
                            break;
                        case 5:
                            score += 2;
                            break;
                        case 6:
                            score += 3;
                            break;
                        case 7:
                            score += 5;
                            break;
                        default:
                            break;
                    }
                    if (chars >= 8) score += 11;
                }
                validWords.addElement(words[i]);
            } else reply += "0";
        }
        String[] s = new String[validWords.size() + 1];
        for (int i = 0; i < validWords.size(); i++) {
            s[i] = "INSERT INTO bogglewords VALUES ('" + (String) validWords.elementAt(i) + "',1) ON DUPLICATE KEY UPDATE Frequency = Frequency + 1";
        }
        s[validWords.size()] = "INSERT INTO personal VALUES ('" + boggleUser + "',NULL,NULL,0,1,0,0,0,0,0,0," + score + ",0," + score + ") " + "ON DUPLICATE KEY UPDATE BoggleGames = BoggleGames + 1, BoggleHighScore = GREATEST(BoggleHighScore," + score + "), BoggleFullScore = boggleFullScore + " + score;
        updateHighestBoggleScore(score, boggleUser);
        reply += "?" + score;
        return reply;
    }

    private void updateHighestBoggleScore(int score, String boggleUser) {
        if (score > boggleHighScore) {
            boggleHighScore = score;
            boggleWinners.removeAllElements();
            boggleWinners.addElement(boggleUser);
        } else if (score == boggleHighScore) {
            boggleWinners.addElement(boggleUser);
        }
    }

    private char[][] setBoggleGrid() {
        char[][] grid = new char[4][4];
        Vector unused = new Vector();
        for (int i = 0; i < cubes.length; i++) unused.addElement(new Integer(i));
        Random r = new Random();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                int cube = Math.abs(r.nextInt() % unused.size());
                Integer loc = (Integer) unused.elementAt(cube);
                int face = Math.abs(r.nextInt() % 6);
                grid[i][j] = cubes[loc.intValue()][face];
                unused.removeElementAt(cube);
            }
        }
        return grid;
    }

    private void listCrosswords(String sender, Vector cwords) {
        String s = "Available Crosswords: ";
        for (int i = 0; i < cwords.size(); i++) {
            s += ((Crossword) cwords.elementAt(i)).title + ", ";
        }
        s = s.substring(0, s.length() - 2);
        sendMessage(sender, s);
    }

    private void getCrossword(String sender, String message, Vector crosswords) {
        String[] msg = splitString(message, " ");
        if (msg.length > 1) {
            String temp = "";
            for (int i = 2; i < msg.length; i++) {
                temp += msg[i] + " ";
            }
            temp = temp.substring(0, temp.length() - 1);
            for (int i = 0; i < crosswords.size(); i++) {
                Crossword c = (Crossword) crosswords.elementAt(i);
                if (temp.equalsIgnoreCase(c.title)) {
                    sendCrossword(sender, c);
                    String[] s = { "INSERT INTO personal VALUES ('" + sender + "', NULL, NULL, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0) ON DUPLICATE KEY UPDATE crosswords = crosswords + 1" };
                    return;
                }
            }
        }
        sendMessage(sender, "Wrong name entered.  Type \"list crosswords\" for a list of " + "available crosswords.");
    }

    private void sendCrossword(String recipient, Crossword c) {
        for (int i = 0; i < c.msgParts.length; i++) {
            sendMessage(recipient, c.msgParts[i]);
        }
    }

    private Vector loadCrosswordFiles() {
        File crosswordDir = new File("crosswords/");
        String[] files = crosswordDir.list();
        Vector cwords = new Vector();
        for (int i = 0; i < files.length; i++) {
            cwords.addElement(loadCrosswordFile(crosswordDir.getName() + "/" + files[i]));
        }
        return cwords;
    }

    private Crossword loadCrosswordFile(String filename) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String str;
            String title = "";
            int width = 0, height = 0;
            int direction = 9;
            Vector downWords = new Vector();
            Vector acrossWords = new Vector();
            while ((str = in.readLine()) != null) {
                if (!str.startsWith(";")) {
                    if (str.startsWith("Title:  ")) {
                        str = str.replaceFirst("Title:  ", "");
                        title = str;
                    } else if (str.startsWith("Width:  ")) {
                        str = str.replaceFirst("Width:  ", "");
                        str.trim();
                        try {
                            width = Integer.valueOf(str).intValue();
                        } catch (NumberFormatException nfe) {
                            nfe.printStackTrace();
                            return null;
                        }
                    } else if (str.startsWith("Height:  ")) {
                        str = str.replaceFirst("Height:  ", "");
                        str.trim();
                        try {
                            height = Integer.valueOf(str).intValue();
                        } catch (NumberFormatException nfe) {
                            nfe.printStackTrace();
                            return null;
                        }
                    } else if (str.startsWith("*")) {
                        if (str.contains("Across words")) direction = CrosswordUnit.ACROSS; else if (str.contains("Down words")) direction = CrosswordUnit.DOWN;
                    } else if (!str.equals("") && !str.startsWith("CodePage:  ") && !str.startsWith("Copyright")) {
                        try {
                            int startX;
                            int startY;
                            String[] categories = splitString(str, ":  ");
                            String[] startCoords = splitString(categories[1], ", ");
                            startX = Integer.valueOf(startCoords[0]).intValue();
                            startY = Integer.valueOf(startCoords[1]).intValue();
                            if (direction == CrosswordUnit.ACROSS) acrossWords.addElement(new CrosswordUnit(categories[0], startX, startY, categories[2])); else if (direction == CrosswordUnit.DOWN) downWords.addElement(new CrosswordUnit(categories[0], startX, startY, categories[2]));
                        } catch (NumberFormatException nfe) {
                            nfe.printStackTrace();
                            return null;
                        }
                    }
                }
            }
            CrosswordUnit[] aw = new CrosswordUnit[acrossWords.size()];
            for (int i = 0; i < aw.length; i++) aw[i] = (CrosswordUnit) acrossWords.elementAt(i);
            CrosswordUnit[] dw = new CrosswordUnit[downWords.size()];
            for (int i = 0; i < dw.length; i++) dw[i] = (CrosswordUnit) downWords.elementAt(i);
            Crossword crossword = new Crossword(title, width, height, aw, dw);
            in.close();
            if (width != 0 && height != 0) {
                if (debug) System.out.println("Crossword successfully loaded from " + filename);
                return crossword;
            } else return null;
        } catch (IOException ioe) {
            if (debug) System.out.println("File IO error on " + filename);
            ioe.printStackTrace();
            return null;
        }
    }

    private void approveModUser(String sender, String msg) {
        if (!pendingApprovals.contains(sender)) {
            pendingApprovals.put(sender, msg);
            tea.setNumber();
            tea.encipher();
            sendCTCPCommand(sender, "PASS" + tea.getCipherMsg());
        }
    }

    private String[] loadWordList(String filename) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String str;
            Vector v = new Vector();
            while ((str = in.readLine()) != null) {
                v.addElement(str);
            }
            String[] words = new String[v.size()];
            v.toArray(words);
            in.close();
            if (debug) System.out.println("Word list " + filename + " successfully loaded");
            return words;
        } catch (IOException ioe) {
            if (debug) System.out.println("File IO error");
            return null;
        }
    }

    private void UpdateDB(String[] SQL) {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
        }
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/jmirc_els_mod?user=root&password=eagle15");
            Statement stmt = conn.createStatement();
            for (int i = 0; i < SQL.length; i++) {
                stmt.executeUpdate(SQL[i]);
            }
            stmt.close();
            conn.close();
        } catch (SQLException ex) {
            if (debug) System.out.println("SQLException: " + ex.getMessage());
            if (debug) System.out.println("SQLState: " + ex.getSQLState());
            if (debug) System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    private void setOverwrite(boolean b, String sender) {
        if (b) {
            overwriteFlag = true;
            sendMessage(sender, "File overwriting enabled");
        } else {
            overwriteFlag = false;
            sendMessage(sender, "File overwriting disabled");
        }
    }

    private void getOverwrite(String sender) {
        sendMessage(sender, "overwrite set to " + overwriteFlag + ".  Type \"enable/disable overwrite\" to set value");
    }

    private void storeMsg(String sender, String message) {
        String[] msg = splitString(message, " ");
        if (msg.length > 2) {
            message = "Message from " + sender + ": " + message.replace(msg[0] + " " + msg[1] + " ", "");
            String key = msg[1].toLowerCase();
            ArrayList list = (ArrayList) msgStore.get(key);
            if (list == null) {
                list = new ArrayList();
                msgStore.put(key, list);
            }
            if (list.size() <= 20) {
                list.add(message);
                sendMessage(sender, "Message to " + msg[1] + " stored");
            } else sendMessage(sender, msg[1] + "'s message log is full");
        }
    }

    private void checkMsgs(String sender, HashMap msgs) {
        String key = sender.toLowerCase();
        ArrayList list = (ArrayList) msgs.get(key);
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                String message = (String) list.get(i);
                sendMessage(sender, message);
            }
            msgs.put(key, null);
        }
    }

    private void highScores(String sender, String message) {
        String[] highScores = new String[3];
        String[] x = message.split(" ");
        if (x.length < 2) {
            return;
        }
        String game = x[1];
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
        }
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/jmirc_els_mod?user=root&password=eagle15");
            if (game.equalsIgnoreCase("scrabble")) {
                String query = "SELECT nick, scrabblewins, scrabbleHighscore FROM personal ORDER BY scrabblewins DESC, scrabblehighscore DESC";
                int i = 0;
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                while (rs.next() && i < 3) {
                    String s = rs.getString("nick");
                    int n = rs.getInt("scrabblewins");
                    int o = rs.getInt("scrabblehighscore");
                    highScores[i] = (i + 1) + ". " + s + ": wins:" + n + " highest score:" + o;
                    i++;
                }
                rs.close();
                stmt.close();
            } else if (game.equalsIgnoreCase("boggle")) {
                String query = "SELECT nick, bogglewins, boggleHighscore FROM personal ORDER BY bogglewins DESC, bogglehighscore DESC";
                int i = 0;
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                while (rs.next() && i < 3) {
                    String s = rs.getString("nick");
                    int n = rs.getInt("bogglewins");
                    int o = rs.getInt("bogglehighscore");
                    highScores[i] = (i + 1) + ". " + s + ": wins:" + n + " highest score:" + o;
                    i++;
                }
                rs.close();
                stmt.close();
            }
            conn.close();
        } catch (SQLException ex) {
            if (debug) System.out.println("SQLException: " + ex.getMessage());
            if (debug) System.out.println("SQLState: " + ex.getSQLState());
            if (debug) System.out.println("VendorError: " + ex.getErrorCode());
        }
        for (int i = 0; i < 3; i++) {
            if (highScores[i] != null) {
                sendMessage(sender, highScores[i]);
            } else return;
        }
    }

    private void translate(String sender, String message) {
        StringTokenizer st = new StringTokenizer(message, " ");
        message = message.replaceFirst(st.nextToken(), "");
        String typeCode = st.nextToken();
        message = message.replaceFirst(typeCode, "");
        try {
            String data = URLEncoder.encode(message, "UTF-8");
            URL url = new URL("http://babelfish.altavista.com/babelfish/tr?doit=done&urltext=" + data + "&lp=" + typeCode);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                if (line.contains("input type=hidden name=\"q\"")) {
                    String[] tokens = line.split("\"");
                    sendMessage(sender, tokens[3]);
                }
            }
            wr.close();
            rd.close();
        } catch (Exception e) {
        }
    }

    private void dictLookup(String sender, String message) {
        String[] messageParts = message.split(" ");
        if (messageParts.length == 2) {
            try {
                Socket socket = new Socket("dict.org", 2628);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                reader.readLine();
                writer.write("DEFINE gcide \"" + messageParts[1] + "\"\r\n");
                writer.flush();
                Vector def = new Vector();
                String section = "";
                String line = null;
                while ((line = reader.readLine()) != null) {
                    if (debug) System.out.println(line);
                    if (line.startsWith("552")) {
                        sendMessage(sender, "No match for " + messageParts[1]);
                        break;
                    }
                    if (line.startsWith("250")) break;
                    if (!line.startsWith("15")) {
                        line = line.trim();
                        if (line.equals("") || line.equals(".")) {
                            String[] sections = checkMsgContentSize(section);
                            for (int i = 0; i < sections.length; i++) def.addElement(sections[i]);
                            section = "";
                        } else if (line.startsWith("(")) {
                            String[] sections = checkMsgContentSize(section);
                            for (int i = 0; i < sections.length; i++) def.addElement(sections[i]);
                            section = line + " ";
                        } else section += line + " ";
                    }
                }
                socket.close();
                dictUsers.addElement(sender);
                Integer part = new Integer(0);
                dictUserStages.put(sender, part);
                String[] definition = new String[def.size()];
                for (int i = 0; i < def.size(); i++) definition[i] = (String) def.elementAt(i);
                definitions.put(sender, definition);
                timer.schedule(new DictUseTimer(sender), 600000);
                sendDefinition(sender);
                String[] s = { "INSERT INTO personal VALUES ('" + sender + "', NULL, NULL, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0) ON DUPLICATE KEY UPDATE Dictlookup = Dictlookup + 1", "INSERT INTO dictwords VALUES ('" + messageParts[1] + "', 1) ON DUPLICATE KEY UPDATE Frequency = Frequency + 1" };
            } catch (Exception e) {
                sendMessage(sender, "Error getting definition");
                e.printStackTrace();
            }
        } else sendMessage(sender, "To use the dictionary type DEFINE <word>");
    }

    protected static String[] checkMsgContentSize(String message) {
        if (message.length() <= 400) {
            return new String[] { message };
        } else {
            String[] messageParts = new String[message.length() / 400 + 1];
            for (int i = 0; i < messageParts.length; i++) {
                int j = (i + 1) * 400;
                if (j >= message.length()) messageParts[i] = message.substring(i * 400); else messageParts[i] = message.substring(i * 400, j);
            }
            return messageParts;
        }
    }

    private boolean sendDefinition(String sender) {
        if (dictUsers.contains(sender)) {
            int part = ((Integer) dictUserStages.get(sender)).intValue();
            String[] definition = (String[]) definitions.get(sender);
            if (part >= 0 && part < definition.length) {
                if (part != definition.length - 1) {
                    definition[part] += "...continue? (Y/N)";
                    sendMessage(sender, definition[part]);
                    part++;
                    dictUserStages.put(sender, new Integer(part));
                } else {
                    definition[part] += "<End of definition>";
                    sendMessage(sender, definition[part]);
                    cleanUpDefinition(sender);
                }
            }
            return true;
        }
        return false;
    }

    private boolean cleanUpDefinition(String sender) {
        if (dictUsers.contains(sender)) {
            dictUsers.remove(sender);
            dictUserStages.remove(sender);
            definitions.remove(sender);
            if (debug) System.out.println("Dict definition for " + sender + " cleaned up");
            return true;
        }
        return false;
    }

    private boolean userExistsInChannel(String nick, String channel) {
        User[] users = getUsers(channel);
        for (int i = 0; i < users.length; i++) {
            if (users[i].getNick().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    private void doAntiFlood(String sender) {
        if (!antiFloodTable.containsKey(sender)) {
            Integer i = new Integer(1);
            antiFloodTable.put(sender, i);
            Timer t = new Timer();
            t.schedule(new AntiBotFlooder(sender), 20000);
            antiFloodTimerTable.put(sender, t);
            if (debug) System.out.println(sender + " timer started");
        } else {
            int i = ((Integer) antiFloodTable.get(sender)).intValue();
            if (i >= MSG_LIMIT) {
                ignoreList.addElement(sender);
            } else {
                i++;
                if (debug) System.out.println("MSG_LIMIT:" + MSG_LIMIT + " Msg#: " + i);
                antiFloodTable.put(sender, new Integer(i));
            }
        }
    }

    private void resetAntiBotFlooder(String nick) {
        Timer t = (Timer) antiFloodTimerTable.get(nick);
        t.cancel();
        antiFloodTimerTable.remove(nick);
        antiFloodTable.remove(nick);
        if (debug) System.out.println(nick + " timer reset");
    }

    private static String[] splitString(String str, String delims) {
        if (str == null) return null; else if (str.equals("") || delims == null || delims.length() == 0) return new String[] { str };
        String[] s;
        Vector v = new Vector();
        int pos, newpos;
        pos = 0;
        newpos = str.indexOf(delims, pos);
        while (newpos != -1) {
            v.addElement(str.substring(pos, newpos));
            pos = newpos + delims.length();
            newpos = str.indexOf(delims, pos);
        }
        v.addElement(str.substring(pos));
        s = new String[v.size()];
        for (int i = 0; i < s.length; i++) {
            s[i] = (String) v.elementAt(i);
        }
        return s;
    }

    protected static int pow(int base, int exp) {
        if (exp == 0) return 1; else if (exp > 0) {
            long result = base;
            for (int i = 1; i < exp; i++) {
                if (result >= Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
                result *= base;
            }
            return (int) result;
        } else {
            int result = 1;
            for (int i = 1; i < exp; i++) {
                result /= base;
                if (result == 0) return 0;
            }
            return result;
        }
    }

    private class BoggleCountdown extends TimerTask {

        public static final int delay = 60;

        public static final int gameTime = 180;

        public static final int timeout = 90;

        private int seconds;

        public BoggleCountdown() {
            seconds = -1;
        }

        public void initialiseBoggleCountdown() {
            seconds = delay + gameTime + timeout;
        }

        public void run() {
            if (seconds >= 0) seconds--;
            if (seconds == 0 + gameTime + timeout) {
                sendMessage(boggleChannel, "Boggle has started with " + boggleUsers.size() + " players.");
                startBoggle(boggleUsers);
            } else if (seconds == 0 + 0 + 0) {
                boggleTimeout = true;
                sendBoggleReplies();
                boggleStarted = false;
            }
        }
    }

    private class AntiBotFlooder extends TimerTask {

        private String nick;

        public AntiBotFlooder(String nick) {
            this.nick = nick;
        }

        public void run() {
            resetAntiBotFlooder(nick);
        }
    }

    private class DictUseTimer extends TimerTask {

        private String nick;

        public DictUseTimer(String nick) {
            this.nick = nick;
        }

        public void run() {
            cleanUpDefinition(nick);
        }
    }

    private class HangmanTimer extends TimerTask {

        private String nick;

        public HangmanTimer(String nick) {
            this.nick = nick;
        }

        public void run() {
            hangmanUsers.removeElement(nick);
            if (debug) if (debug) System.out.println(nick + " removed from hangman - timeout");
        }
    }
}
