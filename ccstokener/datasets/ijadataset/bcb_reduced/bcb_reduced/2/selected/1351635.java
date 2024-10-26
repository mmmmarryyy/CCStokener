package JavaTron;

import java.net.*;
import java.io.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.*;

/**
 * Implements the AudioTron api.  The AudioTron object calls all api command
 * requests on a seperate thread.  All calls to the AudioTron object API's are
*  non blocking.
 * <p>
 * State is notified via a AudioTronListener object.  If you want state use
 * an AudioTronState object instead.
 *
 * @author Taylor Gautier
 * @version $Revision: 1.1 $ 
 */
public class AudioTron {

    public final Integer ON = new Integer(1);

    public final Integer OFF = new Integer(-1);

    public final Integer TOGGLE = new Integer(0);

    private final String USER_AGENT = "JavaTron/" + Version.version;

    public static final int NONE = 0;

    public static final int COMMAND = 1;

    public static final int INFO = 2;

    public static final int CLOCK = 3;

    public static final int SET_CLOCK = 4;

    public static final int GLOBAL = 5;

    public static final int TOC = 6;

    public static final int NAP = 7;

    public static final int DELQUE = 8;

    public static final int ADDNEW = 9;

    public static final int SEARCH = 10;

    public static final int MSG = 11;

    public static final int ALARM = 12;

    public static final int LAST_STATUS = ALARM;

    private int status = NONE;

    protected Hashtable statusMap = new Hashtable();

    private CommandThread commandThread;

    protected boolean showAddress = true;

    protected boolean showUnparsedOutput = false;

    protected boolean showGet = false;

    protected boolean showPost = true;

    protected boolean showAuth = false;

    protected boolean showExtra = false;

    protected StringBuffer getBuffer;

    InputStreamReader isr = null;

    /**
   * Describes an interface that an object needs to adhere to 
   * for it to be a playable object by an AT
   */
    public interface Playable {

        public String getType();

        public String getFile();

        public boolean isPlayable();
    }

    public static interface Parser {

        /**
     * Called when the parsing sequence begins - error is null if
     * connection was made, !null if there is an error connecting
     *
     * @param error null if connection made, !null if not made, value
     *              indicates the error
     */
        public void begin(String error);

        /**
     * Called when the parsing sequence ends.  If an error has occurred
     * the error flag is set
     *
     * @param error set if an error has occurred.
     */
        public void end(boolean error);

        /**
     * 
    /**
     * Called for each line to parse
     *
     * @return true if this method handled the parse
     *
     * @throws an exception if there is an error in parsing and
     *         the data operation should be aborted
     */
        public boolean parse(String content);
    }

    /**
   * A helper class that makes creating a parser class easy
   */
    public abstract static class AbstractParser implements Parser {

        @Override
        public void begin(String error) {
        }

        @Override
        public void end(boolean error) {
        }

        @Override
        public boolean parse(String content) {
            return true;
        }
    }

    /**
   * Zero parameter version of constructor -- default settings for address,
   * username, and password.  Sets up the thread and other initializations.
   */
    public AudioTron() {
        this(null);
    }

    /**
   * One parameter version of constructor -- allows setting of AudioTron
   * address (default username and password)
   *
   * @param server the AudioTron ip address (defaults to 192.168.0.10) 
   */
    public AudioTron(String server) {
        this(server, null);
    }

    /**
   * Two parameter version of constructor -- allows setting of AudioTron
   * address and username (default password).
   *
   * @param server the AudioTron ip address (defaults to 192.168.0.10) 
   * @param username the username to use when accessing AudioTron pages
   *                 (defaults to 'admin')
   */
    public AudioTron(String server, String username) {
        this(server, username, null);
    }

    /**
   * Three parameter version of constructor -- allows setting of AudioTron
   * address, username, and password.
   *
   * @param server the AudioTron ip address (defaults to 192.168.0.10) 
   * @param username the username to use when accessing AudioTron pages
   *                 (defaults to 'admin')
   * @param password the password to use when accessing the AudioTron pages
   *                 (defaults to 'admin')
   */
    public AudioTron(String server, String username, String password) {
        setDefaults();
        if (server != null) Configuration.setProperty(Configuration.KEY_SERVER, server);
        if (username != null) Configuration.setProperty(Configuration.KEY_USERNAME, username);
        if (password != null) Configuration.setProperty(Configuration.KEY_PASSWORD, password);
        statusMap.put(new Integer(COMMAND), "Cmd");
        statusMap.put(new Integer(INFO), "Info");
        statusMap.put(new Integer(CLOCK), "Get Clock");
        statusMap.put(new Integer(SET_CLOCK), "Set Clock");
        statusMap.put(new Integer(GLOBAL), "Global");
        statusMap.put(new Integer(TOC), "Save TOC");
        statusMap.put(new Integer(NAP), "Set Nap");
        statusMap.put(new Integer(ALARM), "Set Alarm");
        statusMap.put(new Integer(DELQUE), "Removing");
        statusMap.put(new Integer(ADDNEW), "Adding");
        statusMap.put(new Integer(SEARCH), "Seaching");
        statusMap.put(new Integer(MSG), "Display");
        HttpURLConnection.setFollowRedirects(false);
        commandThread = new CommandThread(this);
        new MSNTPDaemon();
    }

    public void setDefaults() {
        Properties defaults = new Properties();
        String SP = System.getProperty("file.separator");
        defaults.setProperty(Configuration.KEY_SERVER, "192.168.0.10");
        defaults.setProperty(Configuration.KEY_USERNAME, "admin");
        defaults.setProperty(Configuration.KEY_PASSWORD, JTP.encrypt("admin"));
        defaults.setProperty(Configuration.KEY_BASE_M3U_FILE, System.getProperty("user.home") + SP + "Desktop" + SP + "test.m3u");
        defaults.setProperty(Configuration.KEY_DEFAULT_PLAYLIST_PATH, System.getProperty("user.home"));
        defaults.setProperty(Configuration.KEY_TV_BGCOLOR, "0,0,0");
        defaults.setProperty(Configuration.KEY_TV_FONT, "Arial");
        defaults.setProperty(Configuration.KEY_TV_FONT_COLOR, "100,100,100");
        defaults.setProperty(Configuration.KEY_TV_FONT_SIZE, "20");
        Configuration.setDefault(defaults);
    }

    public String getServer() {
        return Configuration.getProperty(Configuration.KEY_SERVER);
    }

    public String getUsername() {
        return Configuration.getProperty(Configuration.KEY_USERNAME);
    }

    public String getPassword() {
        return (new String(JTP.decrypt(Configuration.getProperty(Configuration.KEY_PASSWORD))));
    }

    public String getBaseM3U() {
        return Configuration.getProperty(Configuration.KEY_BASE_M3U_FILE);
    }

    public String getPlaylistDefaultPath() {
        return Configuration.getProperty(Configuration.KEY_DEFAULT_PLAYLIST_PATH);
    }

    public void start() {
    }

    /**
  * Tell the AudioTron to Stop
  */
    public void stop() {
        command("stop");
    }

    /**
   * Tell the AudioTron to Play
   */
    public void play() {
        command("play");
    }

    /**
   * Tell the AudioTron to go back (Prev)
   */
    public void prev() {
        command("prev");
    }

    /**
   * Tell the AudioTron to go forward (Next)
   */
    public void next() {
        command("next");
    }

    /**
   * Tell the AudioTron to toggle the Pause setting
   */
    public void pause() {
        pause(TOGGLE);
    }

    /**
   * Tell the AudioTron to Pause
   *
   * @param state the state to set the setting to.  Must be one of 
   *              {@link #ON}, {@link #OFF}, {@link #TOGGLE}.
   */
    public void pause(Integer state) {
        command("pause", state.toString());
    }

    /**
   * Tell the AudioTron to clear the playlist
   */
    public void clear() {
        clear(true);
    }

    /**
   * Tell the AudioTron to clear the playlist
   */
    public void clear(boolean update) {
        command("clear");
    }

    /**
   * Tell the AudioTron to toggle the Random setting
   */
    public void random() {
        random(TOGGLE);
    }

    /**
   * Tell the AudioTron to set the Random setting
   *
   * @param state the state to set the setting to.  Must be one of 
   *              {@link #ON}, {@link #OFF}, {@link #TOGGLE}.
   */
    public void random(Integer state) {
        command("random", state.toString());
    }

    /** 
   * Tell the AudioTron to toggle the Repeat setting
   */
    public void repeat() {
        repeat(TOGGLE);
    }

    /** 
   * Tell the AudioTron to set the Repeat setting
   *
   * @param state the state to set the setting to.  Must be one of 
   *              {@link #ON}, {@link #OFF}, {@link #TOGGLE}.
   */
    public void repeat(Integer state) {
        command("repeat", state.toString());
    }

    /**
   * Tell the AudioTron to toggle the Mute setting
   */
    public void mute() {
        mute(TOGGLE);
    }

    /**
   * Tell the AudioTron to set the Mute setting
   *
   * @param state the state to set the setting to.  Must be one of 
   *              {@link #ON}, {@link #OFF}, {@link #TOGGLE}.
   */
    public void mute(Integer state) {
        command("mute", state.toString());
    }

    /**
   * Tell the AudioTron to go to a new index
   *
   * @param index the index to go to
   */
    public void gotoIndex(int index) {
        command("goto", new Integer(index).toString());
    }

    /**
   * Tell the AudioTron to go to a different position within a song
   *
   * @param pos the new song position
   */
    public void setSongPosition(int pos) {
        command("position", new Integer(pos).toString());
    }

    /**
   * Tell the AudioTron to set the volume to the specified value
   *
   * @param volume the new volume
   */
    public void setVolume(int volume) {
        command("volume", new Integer(volume).toString());
    }

    public void setMsgOff() {
        command("msgoff");
    }

    /**
   * Get the clock ctrl page
   *
   * @param parser the parser that will parse the result
   */
    public void getClockCtrl(Parser parser) {
        GetCommand command = new GetCommand("/clockctrl.asp", null, parser, CLOCK);
        addCommand(command, 0);
    }

    /**
   * Enable nap timer
   */
    public void setNap(int minutes, boolean softWake) {
        Vector commandArgs = new Vector();
        commandArgs.add("naptime");
        commandArgs.add(new Integer(minutes).toString());
        commandArgs.add("softenable");
        commandArgs.add(softWake ? "On" : "Off");
        GetCommand command = new GetCommand("/goform/webSetNapForm", commandArgs, null, NAP);
        addCommand(command, 0);
    }

    public void setAlarm(int index, boolean enabled, boolean softEnabled, int hour, int min, boolean am, BitSet days, int volume) {
        if (index < 0 || index > 1) {
            return;
        }
        Vector commandArgs = new Vector();
        commandArgs.add("alarmenable");
        commandArgs.add(enabled ? "ON" : "OFF");
        commandArgs.add("alarmindex");
        commandArgs.add(new Integer(index).toString());
        if (enabled) {
            commandArgs.add("softenable");
            commandArgs.add(softEnabled ? "ON" : "OFF");
            commandArgs.add("alarmhour");
            commandArgs.add(new Integer(hour).toString());
            commandArgs.add("alarmmin");
            commandArgs.add(new Integer(min).toString());
            commandArgs.add("ampmctrl");
            commandArgs.add(am ? "1" : "2");
            if (days.get(0)) {
                commandArgs.add("alarmmon");
                commandArgs.add("ON");
            }
            if (days.get(1)) {
                commandArgs.add("alarmtue");
                commandArgs.add("ON");
            }
            if (days.get(2)) {
                commandArgs.add("alarmwed");
                commandArgs.add("ON");
            }
            if (days.get(3)) {
                commandArgs.add("alarmthu");
                commandArgs.add("ON");
            }
            if (days.get(4)) {
                commandArgs.add("alarmfri");
                commandArgs.add("ON");
            }
            if (days.get(5)) {
                commandArgs.add("alarmsat");
                commandArgs.add("ON");
            }
            if (days.get(6)) {
                commandArgs.add("alarmsun");
                commandArgs.add("ON");
            }
            commandArgs.add("volumectrl");
            commandArgs.add(new Integer(volume).toString());
        }
        GetCommand command = new GetCommand("/goform/webSetAlarmForm", commandArgs, null, ALARM);
        addCommand(command, 0);
    }

    /**
   * Retrieve the global info page
   */
    public void getGlobalInfo(Parser parser) {
        Vector commandArgs = new Vector();
        commandArgs.add("type");
        commandArgs.add("global");
        GetCommand command = new GetCommand("/apigetinfo.asp", commandArgs, parser, GLOBAL);
        addCommand(command, 0);
    }

    /**
   * Retrieve a TOC file
   */
    public void getTOC(String share, Parser parser) {
        Vector commandArgs = new Vector();
        commandArgs.add("share");
        commandArgs.add(share);
        GetCommand command = new GetCommand("/apidumptoc.asp", commandArgs, parser, TOC);
        addCommand(command, 0);
    }

    /**
   * Tell the AT to set it's clock from the given server
   *
   * @param timeServer the NTP time server to set the clock from
   * @param parser the parser to use to parse the result
   */
    public void setTimeFromTimeServer(String timeServer, Parser parser) {
        Vector commandArgs = new Vector();
        commandArgs.add("timeserver");
        commandArgs.add(timeServer);
        commandArgs.add("settime button");
        commandArgs.add("Set Time from Time Server");
        GetCommand command = new GetCommand("/goform/webSetTimeNTPForm", commandArgs, parser, SET_CLOCK);
        addCommand(command, 0);
    }

    public void getFilteredInfo(String filter, String filterValue, Parser parser) {
        getFilteredInfo(filter, filterValue, null, parser);
    }

    public void getFilteredInfo(String filter, String filterValue, String start, Parser parser) {
        getFilteredInfo(filter, filterValue, start, 0, parser);
    }

    public void getFilteredInfo(String filter, String filterValue, int count, Parser parser) {
        getFilteredInfo(filter, filterValue, null, 0, parser);
    }

    public void getFilteredInfo(String filter, String filterValue, String start, int count, Parser parser) {
        String f = "f" + filter.toLowerCase();
        getInfo("file", f, filterValue, start, count, parser);
    }

    /**
   * Tell the audiotron to retrieve info listing (query song database)
   */
    public void getInfo(String type, Parser parser) {
        getInfo(type, null, null, null, 0, parser);
    }

    public void getInfo(String type, String start, Parser parser) {
        getInfo(type, start, 0, parser);
    }

    public void getInfo(String type, int count, Parser parser) {
        getInfo(type, null, count, parser);
    }

    public void getInfo(String type, String start, int count, Parser parser) {
        getInfo(type, null, null, start, count, parser);
    }

    private void getInfo(String type, String filter, String filterValue, String start, int count, Parser parser) {
        Vector commandArgs = new Vector();
        commandArgs.add("type");
        commandArgs.add(type);
        if (filter != null) {
            commandArgs.add(filter);
            commandArgs.add(filterValue);
        }
        if (start != null) {
            commandArgs.add("this");
            commandArgs.add(start);
        }
        if (count > 0) {
            commandArgs.add("count");
            commandArgs.add(new Integer(count).toString());
        }
        GetCommand command = new GetCommand("/apigetinfo.asp", commandArgs, parser, INFO);
        addCommand(command, 1);
    }

    public void queueFile(Object playable) {
        try {
            queueFile((Playable) playable);
        } catch (ClassCastException cce) {
        }
    }

    public void queueFile(Object[] playable) {
        try {
            for (int i = 0; i < playable.length; i++) {
                queueFile((Playable) playable[i]);
            }
        } catch (ClassCastException cce) {
        }
    }

    /**
   * Tell the audiotron to queue a file
   *
   * type is the type of file to queue
   * file is the file to queue
   *
   * @param playable A Playable object type, the file to add to the queue
   */
    public void queueFile(Playable playable) {
        if (!playable.isPlayable()) {
            return;
        }
        Vector commandArgs = new Vector();
        commandArgs.add("type");
        commandArgs.add(playable.getType());
        commandArgs.add("file");
        commandArgs.add(playable.getFile());
        GetCommand command = new GetCommand("/apiqfile.asp", commandArgs, null);
        addCommand(command, 0);
    }

    public void playFile(Object playable) {
        try {
            playFile((Playable) playable);
        } catch (ClassCastException cce) {
        }
    }

    /**
   * Tell the audiotron to play a file (same as queueFile but clears
   * the playlist first)
   *
   * type is the type of file to queue
   * file is the file to queue
   * @param playable A Playable Object - a file, or list
   */
    public void playFile(Playable playable) {
        if (!playable.isPlayable()) {
            return;
        }
        Vector list = new Vector();
        list.addElement(playable);
        playFile(list.elements());
    }

    /**
   * Method to remove a Song from the Audiotron's Active Play Queue
   * The only way to do this is through the web interface, with a POST
   * request. Probably should usa a Vector for consistency, but I'm 
   * still learning how all this put together.
   * 
   * @author JSC
   * @param args A HashMap of the POST form data
   */
    public void dequeueFiles(HashMap args) {
        Vector commandArgs = new Vector();
        int i = 0;
        Set set = args.entrySet();
        Iterator idx = set.iterator();
        while (idx.hasNext()) {
            Map.Entry me = (Map.Entry) idx.next();
            commandArgs.add(i, me.getKey());
            commandArgs.add(i + 1, me.getValue());
            i += 2;
        }
        PostCommand command = new PostCommand("/goform/webQuePlayForm", commandArgs, null, DELQUE);
        if (showExtra) {
            System.out.println("Dequeueing ....");
        }
        addCommand(command, 0);
    }

    /**
   * Tell the audiotron to play a file (same as queueFile but clears
   * the playlist first)
   *
   * @param type an array of the type of file to queue
   * @param file and array of the file to queue
   */
    public void playFile(Enumeration list) {
        boolean queued = false;
        while (list.hasMoreElements()) {
            Playable p = (Playable) list.nextElement();
            if (p.isPlayable() && !queued) {
                queued = true;
                stop();
                clear(false);
            }
            queueFile(p);
        }
        if (queued) {
            play();
        }
    }

    /**
   * Method that tell the Audiotron to add a single file to its library -JSC
   * @param songLoc the SMB URL of the file to add
   */
    public void addToLibrary(String songLoc) {
        Vector commandArgs = new Vector();
        commandArgs.add("type");
        commandArgs.add("file");
        commandArgs.add("file");
        commandArgs.add(songLoc);
        GetCommand command = new GetCommand("/apiaddfile.asp", commandArgs, null, ADDNEW);
        if (showExtra) {
            System.out.println("Adding " + songLoc);
        }
    }

    /**
   * The POST request that tells the Audiotron to search for new files - JSC
   * TODO: need feedback...this is a LONG operation
   */
    public void search() {
        Vector commandArgs = new Vector();
        commandArgs.add("Hosts");
        commandArgs.add("Press to Check");
        PostCommand command = new PostCommand("/goform/CheckNewFilesForm", commandArgs, null, SEARCH);
        if (showExtra) {
            System.out.println("Searching for new Files.");
        }
        addCommand(command, 0);
    }

    /**
   * POST's winXP/2000 network auth information to AT
   * @param username
   * @param password
   */
    public void setXPAuth(String username, String password) {
        Vector commandArgs = new Vector();
        commandArgs.add("ntuser");
        commandArgs.add(username);
        commandArgs.add("ntpass");
        commandArgs.add(password);
        commandArgs.add("NTSubmit");
        commandArgs.add("Save NT/2000/XP Settings");
        PostCommand command = new PostCommand("/goform/NTForm", commandArgs, null);
        addCommand(command, 0);
    }

    /**
 * POST's win98 share password to the AT
 * @param password
 */
    public void setWin98Auth(String password) {
        Vector commandArgs = new Vector();
        commandArgs.add("sharepass");
        commandArgs.add(password);
        commandArgs.add("sharesubmit");
        commandArgs.add("Save Share Password");
        PostCommand command = new PostCommand("goform/ShareForm", commandArgs, null);
        addCommand(command, 0);
    }

    /**
   * Gets a web page from the Audiotron
   * 
   * @param address - The URL to GET
   * @param commandArgs - Arguments for a GET request
   * @param parser - An HtmlParser to parse/return the output
   */
    public void getPage(String address, Vector commandArgs, JTP.HtmlParser parser) {
        GetCommand command = new GetCommand(address, commandArgs, parser, INFO);
        if (showExtra) {
            System.out.println("Getting " + address);
        }
        addCommand(command, 0);
    }

    /**
   * Issue a command
   *
   * @param command the command to execute
   */
    private void command(String command) {
        command(command, null);
    }

    /**
   * Issue a command.  This method queues the command onto the priority
   * queue.  It is non-blocking.
   *
   * @param command the command to execute
   * @param state additional args
   */
    private void command(String cmd, String state) {
        Vector commandArgs = new Vector();
        commandArgs.add("cmd");
        commandArgs.add(cmd);
        if (state != null) {
            commandArgs.add("arg");
            commandArgs.add(state);
        }
        GetCommand command = new GetCommand("/apicmd.asp", commandArgs, null);
        addCommand(command, 0);
    }

    /**
   * A Command that does a get
   */
    protected class GetCommand extends Command {

        String address;

        Vector args;

        Parser parser;

        /**
    * Get command constructor
     *
     * @param address_ the (web page) address of the command
     * @param args_ args to the command
     * @param parser the parser to parse the result
     */
        public GetCommand(String address_, Vector args_, Parser parser_) {
            this(address_, args_, parser_, COMMAND);
        }

        /**
     * Get command constructor
     *
     * @param address_ the (web page) address of the command
     * @param args_ args to the command
     * @param parser_ the parser to parse the result
     * @param status what status this command represents
    */
        public GetCommand(String address_, Vector args_, Parser parser_, int status) {
            super(status);
            address = address_;
            args = args_;
            parser = parser_;
        }

        @Override
        public void invoke() {
            try {
                get(address, args, parser);
            } catch (IOException ioe) {
            }
        }
    }

    /**
   * A command that does a POST
   *
   */
    protected class PostCommand extends Command {

        String address;

        Vector args;

        Parser parser;

        /**
	* Post command constructor
	*
	* @param address the (web page) address of the command
	* @param args args to the command
	* @param parser the parser to parse the result
	*/
        public PostCommand(String address_, Vector args_, Parser parser_) {
            this(address_, args_, parser_, COMMAND);
        }

        /**
     * Post command constructor
     *
     * @param address_ the (web page) address of the command
     * @param args_ args to the command
     * @param parser_ the parser to parse the result
     * @param status what status this command represents
    */
        public PostCommand(String address_, Vector args_, Parser parser_, int status) {
            super(status);
            address = address_;
            args = args_;
            parser = parser_;
        }

        @Override
        public void invoke() {
            try {
                post(address, args, parser);
            } catch (IOException ioe) {
            }
        }
    }

    /**
   * Create URI arguments
   *
   * @param buffer the buffer to append the output string to
   * @param args the input arguments
   */
    private void createURIArgs(StringBuffer buffer, Vector args) throws UnsupportedEncodingException {
        for (int i = 0; i < args.size(); i += 2) {
            if (i > 0) {
                buffer.append("&");
            }
            buffer.append(URLEncoder.encode(args.elementAt(i).toString(), "UTF-8"));
            buffer.append("=");
            buffer.append(URLEncoder.encode(args.elementAt(i + 1).toString(), "UTF-8"));
        }
    }

    /**
   * Send a get request to the AudioTron server
   *
   * @param address the uri to use
   * @param args the arguments to use
   * @param parser an optional line-by-line parser to use instead of buffering
   *               the whole response
   *
   * @return null on success, a string on error.  The string describes
   *         the error.
   *
   * @throws IOException if the process was interrupted
   */
    protected String get(String address, Vector args, Parser parser) throws IOException {
        String ret = null;
        StringBuffer myAddress = new StringBuffer();
        Object[] methodArgs = new Object[1];
        try {
            myAddress.append(address);
            if (args != null) {
                myAddress.append('?');
                createURIArgs(myAddress, args);
            }
            if (showAddress) {
                System.out.println(myAddress.toString());
            }
            HttpURLConnection conn = getConnection(myAddress.toString());
            if (conn.getResponseCode() != 200 && conn.getResponseCode() != 302) {
                try {
                    ret = conn.getResponseMessage();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                if (ret == null) {
                    ret = "Unknown Error";
                }
                if (parser != null) {
                    parser.begin(ret);
                }
                return ret;
            }
            isr = new InputStreamReader(conn.getInputStream());
            BufferedReader reader = new BufferedReader(isr);
            String s;
            getBuffer = new StringBuffer();
            if (parser != null) {
                parser.begin(null);
            }
            while ((s = reader.readLine()) != null) {
                if (showGet) {
                    System.out.println(s);
                }
                getBuffer.append(s);
                getBuffer.append("\n");
                if (parser == null) {
                } else {
                    if (!parser.parse(s)) {
                        return "Parse Error";
                    }
                }
            }
            if (parser == null && showUnparsedOutput) {
                System.out.println(getBuffer);
            }
            if (parser != null) {
                parser.end(false);
            }
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            if (parser != null) {
                parser.end(true);
            }
            throw (ioe);
        } finally {
            try {
                isr.close();
            } catch (Exception e) {
            }
        }
        return ret;
    }

    /**
   * Method to execute a POST Request to the Audiotron - JSC
   * 
   * @param address - The requested page
   * @param args - The POST data
   * @param parser - A Parser Object fit for parsing the response
   *
   * @return null on success, a string on error.  The string describes
   *         the error.
   *
   * @throws IOException
   */
    protected String post(String address, Vector args, Parser parser) throws IOException {
        String ret = null;
        URL url;
        HttpURLConnection conn;
        String formData = new String();
        for (int i = 0; i < args.size(); i += 2) {
            if (showPost) {
                System.out.print("POST: " + args.get(i).toString() + " = ");
                System.out.println(args.get(i + 1).toString());
            }
            formData += URLEncoder.encode(args.get(i).toString(), "UTF-8") + "=" + URLEncoder.encode(args.get(i + 1).toString(), "UTF-8");
            if (i + 2 != args.size()) formData += "&";
        }
        try {
            url = new URL("http://" + getServer() + address);
            if (showAddress || showPost) {
                System.out.println("POST: " + address);
                System.out.println("POST: " + formData);
            }
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            String auth = getUsername() + ":" + getPassword();
            conn.setRequestProperty("Authorization", "Basic " + B64Encode(auth.getBytes()));
            if (showAuth) {
                System.out.println("POST: AUTH: " + auth);
            }
            conn.setRequestProperty("Content Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", "" + Integer.toString(formData.getBytes().length));
            conn.setRequestProperty("Content-Language", "en-US");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(formData);
            wr.flush();
            wr.close();
            if (conn.getResponseCode() != 200 && conn.getResponseCode() != 302) {
                try {
                    ret = conn.getResponseMessage();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                if (ret == null) {
                    ret = "Unknown Error";
                }
                if (parser != null) {
                    parser.begin(ret);
                }
                return ret;
            }
            isr = new InputStreamReader(conn.getInputStream());
            BufferedReader reader = new BufferedReader(isr);
            String s;
            getBuffer = new StringBuffer();
            if (parser != null) {
                parser.begin(null);
            }
            while ((s = reader.readLine()) != null) {
                if (showGet) {
                    System.out.println(s);
                }
                getBuffer.append(s);
                getBuffer.append("\n");
                if (parser == null) {
                } else {
                    if (!parser.parse(s)) {
                        return "Parse Error";
                    }
                }
            }
            if (parser == null && showUnparsedOutput) {
                System.out.println(getBuffer);
            }
            if (parser != null) {
                parser.end(false);
            }
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            if (parser != null) {
                parser.end(true);
            }
            throw (ioe);
        } finally {
            try {
                isr.close();
            } catch (Exception e) {
            }
        }
        return ret;
    }

    /**
   * Initiate a HttpURLConnection to the AudioTron.  Includes
   * setting the appropriate headers such as authentication.
   *
   * @param address the full http address to connect to
   * 
   * @return an HttpURLConnection that is not connected
   *
   * @throws MalformedURLException if the address parameter is bad
   * @throws IOException if there is a problem opening the connection
   */
    private HttpURLConnection getConnection(String address) throws MalformedURLException, IOException {
        StringBuffer urlBuffer = new StringBuffer();
        urlBuffer.append("http://");
        urlBuffer.append(getServer());
        urlBuffer.append(address);
        URL url = new URL(urlBuffer.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        String auth = getUsername() + ":" + getPassword();
        conn.setRequestProperty("Authorization", "Basic " + B64Encode(auth.getBytes()));
        if (showAuth) {
            System.out.println("AUTH: " + auth);
        }
        return conn;
    }

    /**
   * BASE64 Encode a byte array
   * Function to replace sun.misc.BASE64 stuff
   * uses jakarta commons codec
   * @param s - the input to be encoded
   * @return a Base64 Encoded String fit for web work
   */
    protected String B64Encode(byte[] s) {
        byte[] t = Base64.encodeBase64(s);
        String h = "";
        for (int i = 0; i < t.length; i++) {
            h += (char) t[i];
        }
        if (showExtra) {
            System.out.println("Encode: " + h);
        }
        return (h);
    }

    /**
   * Add a command to execute.  Does so non-blocking.  Just calls
   * the CommandThreads' addCommand method.
   *
   * @param command the callback method to invoke.
   * @param priority the priority of this command
   */
    protected void addCommand(Command command, int priority) {
        commandThread.addCommand(command, priority);
    }

    /**
   * Remove a command from the execution queue.
   *
   * @param command the callback method to remove
   */
    protected void removeCommand(Command command) {
        commandThread.removeCommand(command);
    }

    /**
  * Get the status of the currently executing command
  */
    protected int getCommandStatus() {
        return commandThread.getCommandStatus();
    }

    /**
  * Return a text description of the status
  */
    public String getStatus() {
        Object obj = statusMap.get(new Integer(getCommandStatus()));
        if (obj != null) {
            return obj.toString();
        }
        return "";
    }

    /**
   * Call at the beginning a command
   */
    protected synchronized void startCommand(Command command) {
    }

    /**
   * Call at the end of a a command.
   */
    protected synchronized void endCommand(Command command) {
    }

    /**
   * Implements the command processing thread.  This thread dequeues
   * items from the priority queue (which are assumed to be of type
   * Command) and invokes them.
   *
   */
    private class CommandThread implements Runnable, PriorityQueue.ElementInsertionListener {

        private PriorityQueue queue;

        private AudioTron at;

        private PriorityQueue.PrioritizedElement current;

        public CommandThread(AudioTron at_) {
            at = at_;
            queue = new PriorityQueue(3);
            queue.addElementInsertionListener(this);
            Thread t = new Thread(this);
            t.setDaemon(true);
            t.start();
        }

        /**
     * ElementInsertionListener implementation
     */
        @Override
        public void elementInserted(Object element, int priority) {
            if (current != null && current.priority > priority) {
                try {
                    isr.close();
                } catch (Exception e) {
                }
            }
        }

        /**
     * Add a command to execute.  Does so non-blocking.
     *
     * @param wm the callback method to invoke.
     * @param the priority of this command
     */
        public synchronized void addCommand(Command command, int priority) {
            queue.add(command, priority);
            notify();
        }

        /**
     * Remove a command from the execution queue.
     *
     * @param wm the callback method to remove
     */
        public void removeCommand(Command command) {
            queue.remove(command);
        }

        /**
     * Return the status value of the currently executing command
     */
        public int getCommandStatus() {
            try {
                return ((Command) current.element).status;
            } catch (NullPointerException npe) {
            }
            return NONE;
        }

        /**
     * The run method which is the start point of the thread.
     */
        @Override
        public void run() {
            Command command = null;
            for (; ; ) {
                synchronized (this) {
                    while (queue.isEmpty()) {
                        try {
                            wait();
                        } catch (InterruptedException ie) {
                        }
                    }
                    current = (PriorityQueue.PrioritizedElement) queue.remove();
                }
                try {
                    command = (Command) current.element;
                    startCommand(command);
                    command.invoke();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    current = null;
                    if (command != null) {
                        endCommand(command);
                    }
                    command = null;
                }
            }
        }
    }

    /**
   * Command object is the basis for executing "commands" from an
   * ordered, priority queue on a seperate "command thread" for
   * asynchronous behavior.  See Design Patterns (Command Pattern).
   */
    public abstract static class Command {

        int status;

        public Command(int status_) {
            status = status_;
        }

        public abstract void invoke() throws Exception;
    }
}
