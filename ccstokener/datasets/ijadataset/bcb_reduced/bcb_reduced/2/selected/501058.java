package com.streamsicle;

import com.streamsicle.fluid.InteractiveStream;
import com.streamsicle.fluid.Server;
import com.streamsicle.gui.*;
import com.streamsicle.songinfo.SongInfo;
import com.streamsicle.util.Constants;
import com.streamsicle.util.Utils;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;
import java.net.*;
import com.streamsicle.fluid.StreamingEngineException;
import com.streamsicle.fluid.ExstreamerDetector;

/**
 *  The main Streamsicle servlet.
 *
 *@author     Matt Hall
 *@author     John Watkinson
 *@created    October 8, 2002
 */
public class Streamsicle extends HttpServlet {

    public static final String UPGRADE_CHECK_URL = "http://streamsicle.com/upgrade/20.html";

    String upgradeLink = "";

    public static final String ACTION_VIEW_GENRE = "viewGenre";

    public static final String ACTION_VIEW_ARTIST = "viewArtist";

    public static final String ACTION_VIEW_ALBUM = "viewAlbum";

    public static final String ACTION_VIEW_ALL_GENRES = "viewGenres";

    public static final String ACTION_ADD_SONG = "add";

    public static final String PARAM_ALBUM = "album";

    public static final String PARAM_ARTIST = "artist";

    public static final String LOOK_DIR = "/look_default";

    public static final String MAIN_JSP = LOOK_DIR + "/main.jsp";

    public static final String REFRESH_JSP = LOOK_DIR + "/index.jsp";

    public static final String BUSY_JSP = LOOK_DIR + "/busy.jsp";

    public static final String REQUEST_JSP = LOOK_DIR + "/request.jsp";

    public static final String ID3_JSP = LOOK_DIR + "/id3TreeInc.jsp";

    public static final String DIRLIST_REQUEST_JSP = LOOK_DIR + "/newrequest.jsp";

    public static final String SEARCH_REQUEST_JSP = LOOK_DIR + "/searchrequest.jsp";

    public static final String SONG_INFO_JSP = LOOK_DIR + "/songInfo.jsp";

    public static final String CONFIGURE_JSP = LOOK_DIR + "/configure.jsp";

    public static final String MAIN_SERVLET = LOOK_DIR + "/servlet/Streamsicle";

    public static final String STREAMSICLE_CONFIG_FILE = "WEB-INF/conf/streamsicle.conf";

    public static final String LOG_CONFIG_FILE = "WEB-INF/conf/log4j.properties";

    public static final String ATTRIB_DIR_ID = "dirID";

    public static final String PARAM_DIR_ID = "dirID";

    public static final String PARAM_SEARCH_STRING = "searchstring";

    public static final String ATTRIB_SEARCH_STRING = "searchstring";

    public static final String ATTRIB_LEFT_TAB = "leftTab";

    public static final String PARAM_LEFT_TAB = "leftTab";

    public static final String ATTRIB_RIGHT_TAB = "rightTab";

    public static final String PARAM_RIGHT_TAB = "rightTab";

    public static final String CONFIG_PROPERTY = "streamsicle.playdir";

    public static final String CONFIG_FLAG = "UNCONFIGURED";

    public static final String GUI_PROPERTY = "streamsicle.gui";

    public static final String GUI_FLAG = "true";

    public static final String PARAM_GENRE = "genre";

    public static final String PARAM_SONG = "song";

    private InteractiveStream stream = null;

    static Category log = Category.getInstance(Streamsicle.class);

    private Server server;

    private Properties props;

    private Long lastReloadTime = new Long(System.currentTimeMillis());

    public static final String ATTRIBUTE_STREAM = "stream";

    public static final String ATTRIBUTE_ID3MODE = "id3Mode";

    public static final String ATTRIBUTE_ID3PARAM = "id3Param";

    public static final String ID3MODE_ALL_GENRES = "viewingGenres";

    public static final String ID3MODE_ALL_ARTISTS = "viewingArtists";

    public static final String ID3MODE_ALL_ALBUMS = "viewingAlbums";

    public static final String ID3MODE_ARTIST = "viewingArtist";

    public static final String ID3MODE_ALBUM = "viewingAlbum";

    public static final String ID3MODE_GENRE = "viewingGenre";

    /**
     *  Gets the stream attribute of the Streamsicle object
     *
     *@return    The stream value
     */
    public InteractiveStream getStream() {
        return stream;
    }

    /**
     *  Gets the properties attribute of the Streamsicle object
     *
     *@return    The properties value
     */
    public Properties getProperties() {
        return props;
    }

    /**
     *  Gets the server attribute of the Streamsicle object
     *
     *@return    The server value
     */
    public Server getServer() {
        return server;
    }

    /**
     *  Description of the Method
     */
    public void reconfigure() {
        try {
            FileInputStream propsFile = new FileInputStream(getServletContext().getRealPath(STREAMSICLE_CONFIG_FILE));
            Properties newProps = new Properties();
            newProps.load(propsFile);
            propsFile.close();
            Utils.replaceProperties(props, newProps);
            stream.reconfigure();
        } catch (IOException e) {
            log.error("Problem with config file.");
            e.printStackTrace();
        }
    }

    /**
     *  Description of the Method
     *
     *@exception  Exception  Description of the Exception
     */
    protected void initialize() throws Exception {
        final ServletContext context = getServletContext();
        String guiProp = null;
        try {
            FileInputStream propsFile = new FileInputStream(context.getRealPath(STREAMSICLE_CONFIG_FILE));
            props = new Properties();
            props.load(propsFile);
            propsFile.close();
            String configProp = props.getProperty(CONFIG_PROPERTY);
            guiProp = props.getProperty(GUI_PROPERTY);
            if ((configProp == null) || (CONFIG_FLAG.equals(configProp))) {
                if (GUI_FLAG.equals(guiProp)) {
                    ConfigurationWizard wiz = new ConfigurationWizard(context.getRealPath(STREAMSICLE_CONFIG_FILE));
                    try {
                        wiz.run();
                    } catch (Exception e) {
                        log.error("Problem with configuration: ");
                    }
                    propsFile = new FileInputStream(context.getRealPath(STREAMSICLE_CONFIG_FILE));
                    props.load(propsFile);
                    propsFile.close();
                    setConfigured(true);
                } else {
                    setConfigured(false);
                }
            } else {
                setConfigured(true);
            }
        } catch (IOException e) {
            log.error("Problem with config file: " + e);
        }
        if (isConfigured()) {
            try {
                URL url = new URL(UPGRADE_CHECK_URL);
                URLConnection connection = url.openConnection();
                Object contents = connection.getContent();
                if (contents instanceof InputStream) {
                    if (connection.getHeaderField(0).indexOf("4") == -1) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream) contents));
                        String line;
                        while (null != (line = reader.readLine())) {
                            upgradeLink += line;
                        }
                    }
                    ((InputStream) contents).close();
                }
            } catch (Exception e) {
            }
            if (props.getProperty("streamsicle.requesttree.showsongtimes").toLowerCase().equals("true")) {
                getServletContext().setAttribute(Constants.SHOW_SONG_TIMES_ATTRIBUTE, "true");
            }
            int maxClients = -1;
            try {
                maxClients = Integer.parseInt(System.getProperty("streamsicle.maxclients"));
            } catch (NumberFormatException nfe) {
            }
            getServletContext().setAttribute(Constants.MAX_CLIENTS_ATTRIBUTE, new Integer(maxClients));
            setReloading(true);
            Thread initThread = new Thread(new Runnable() {

                public void run() {
                    server = new Server(context, props);
                    log.debug("Trying to initialize streaming engine.");
                    try {
                        server.initialize();
                    } catch (StreamingEngineException e) {
                        log.debug("Had an engine exception.");
                        setReloading(false);
                        setConfigured(false, e.toString());
                        return;
                    }
                    setConfigured(true, "");
                    stream = server.getStream();
                    setReloading(false);
                }
            }, "Init Thread");
            initThread.start();
            if (GUI_FLAG.equals(guiProp)) {
                log.info("Initializing GUI...");
                MainWindow gui = new MainWindow(getServletContext().getRealPath(STREAMSICLE_CONFIG_FILE), this);
                gui.start();
            }
        }
    }

    /**
     *  Description of the Method
     *
     *@exception  ServletException  Description of the Exception
     */
    public void init() throws ServletException {
        final ServletContext context = getServletContext();
        PropertyConfigurator.configureAndWatch(context.getRealPath(LOG_CONFIG_FILE), 5000);
        try {
            initialize();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     *  Cleans up after the servlet is taken out of service (ie. Streamsicle is
     *  being shut down).
     */
    public void destroy() {
        server.shutdown();
    }

    /**
     *  Main service-handling method.
     *
     *@param  request               Description of the Parameter
     *@param  response              Description of the Parameter
     *@exception  ServletException  Description of the Exception
     *@exception  IOException       Description of the Exception
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!isConfigured()) {
            log.debug("Attemping to configure.");
            try {
                initialize();
            } catch (Exception x) {
                x.printStackTrace();
            }
            log.debug("Checking to see if config was successful.");
            if (!isConfigured()) {
                request.setAttribute("errorMessage", getConfigurationErrorMessage());
                request.getRequestDispatcher(CONFIGURE_JSP).forward(request, response);
                return;
            }
        }
        if (isReloading()) {
            String lastSongName = null;
            if (server != null) {
                stream = server.getStream();
            }
            if (stream != null) {
                lastSongName = stream.getLastAddedSong();
            }
            if (lastSongName != null) {
                request.setAttribute(Constants.MP3_FILE_PARSING_NAME, lastSongName);
                request.setAttribute(Constants.MP3_FILE_PARSED_NUMBER, Long.toString(stream.getNumSongsAdded()));
                request.setAttribute(Constants.MP3_FILE_COUNT_TOTAL, Long.toString(stream.getNumTotalSongs()));
                String tempDirName = stream.getParsingFromDir();
                if (tempDirName.indexOf("/") != -1) {
                    tempDirName = tempDirName.substring(tempDirName.indexOf("/"));
                }
                request.setAttribute(Constants.MP3_ROOT_DIR_PARSING_NAME, tempDirName);
            } else {
            }
            request.setAttribute("errorMessage", getConfigurationErrorMessage());
            request.getRequestDispatcher(BUSY_JSP).forward(request, response);
            return;
        }
        InetAddress address = InetAddress.getByName(request.getRemoteAddr());
        boolean valid_user = false;
        valid_user = server.verifyClientByAddress(address);
        setSessionData(request);
        String action = request.getParameter("action");
        if ("add".equals(action)) {
            int songID = Integer.parseInt(request.getParameter("song"));
            Vector vec = stream.getQueue();
            boolean found_file = false;
            for (int i = 0; i < vec.size(); i++) {
                if (songID == ((QueueItem) vec.elementAt(i)).getMP3File().getFileID()) {
                    found_file = true;
                }
            }
            if (!found_file) {
                MP3File m3uFile = stream.getMP3File(songID);
                String songName = m3uFile.getFile().getAbsolutePath();
                String t = null;
                String m3uPathName = m3uFile.getFile().getAbsolutePath().toLowerCase();
                m3uPathName = m3uPathName.substring(0, m3uPathName.lastIndexOf(java.io.File.separator) + 1);
                Vector mp3List = null;
                MP3File tFile = null;
                if (songName.endsWith(Constants.CONST_M3U_FILE_SUFFIX)) {
                    try {
                        BufferedReader in = new BufferedReader(new FileReader(songName));
                        mp3List = new Vector();
                        while ((t = in.readLine()) != null) {
                            if (t.indexOf('#') != 0) {
                                mp3List.addElement(t);
                                String searchFileName = null;
                                int index;
                                if ((index = t.lastIndexOf(java.io.File.separator)) != -1) {
                                    String searchFilePath = t.substring(index, t.length());
                                    String basePathName = t.substring(0, index + 1);
                                    index = m3uPathName.indexOf(basePathName);
                                    searchFileName = m3uPathName.substring(0, index) + t;
                                } else {
                                    searchFileName = m3uPathName + t;
                                }
                                if ((tFile = stream.getMP3File(searchFileName)) == null) {
                                } else {
                                    stream.addMedia(tFile.getFileID(), true);
                                }
                            }
                        }
                        ;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    stream.addMedia(songID, true);
                }
            }
            request.getRequestDispatcher(REFRESH_JSP).forward(request, response);
        } else if ("list".equals(action)) {
            Vector songs = stream.getList();
            String[] list = new String[songs.size()];
            int[] ids = new int[songs.size()];
            for (int i = 0; i < list.length; i++) {
                list[i] = ((MP3File) songs.elementAt(i)).getName();
                ids[i] = ((MP3File) songs.elementAt(i)).getFileID();
            }
            request.setAttribute("list", list);
            request.setAttribute("ids", ids);
            request.getRequestDispatcher(REQUEST_JSP).forward(request, response);
        } else if ("newlist".equals(action)) {
            setNewListAttributes(request);
            request.getRequestDispatcher(DIRLIST_REQUEST_JSP).forward(request, response);
        } else if ("setrandom".equals(action)) {
            String fileID = (String) request.getParameter("fileID");
            if ((fileID != null) && (valid_user)) {
                stream.setRandomRoot(new Integer(fileID));
            }
            request.getRequestDispatcher(REFRESH_JSP).forward(request, response);
        } else if ("playall".equals(action)) {
            String fileID = (String) request.getParameter("fileID");
            if ((fileID != null) && (valid_user)) {
                try {
                    stream.addMediaIn(new Integer(fileID));
                } catch (NumberFormatException e) {
                    stream.addMediaIn(new Integer(stream.getRoot().getFileID()));
                }
            }
            request.getRequestDispatcher(REFRESH_JSP).forward(request, response);
        } else if ("remove".equals(action)) {
            String stringID = (String) request.getParameter("fileID");
            int fileIDtoRemove;
            if ((stringID != null) && (valid_user)) {
                fileIDtoRemove = Integer.parseInt(stringID);
                stream.removeFromQueue(fileIDtoRemove);
            }
            request.getRequestDispatcher(REFRESH_JSP).forward(request, response);
        } else if ("totop".equals(action)) {
            String stringID = (String) request.getParameter("fileID");
            int fileIDtoMove;
            if ((stringID != null) && (valid_user)) {
                fileIDtoMove = Integer.parseInt(stringID);
                stream.toTopOfQueue(fileIDtoMove);
            }
            request.getRequestDispatcher(REFRESH_JSP).forward(request, response);
        } else if ("tobottom".equals(action)) {
            String stringID = (String) request.getParameter("fileID");
            int fileIDtoMove;
            if ((stringID != null) && (valid_user)) {
                fileIDtoMove = Integer.parseInt(stringID);
                stream.toBottomOfQueue(fileIDtoMove);
            }
            request.getRequestDispatcher(REFRESH_JSP).forward(request, response);
        } else if ("move".equals(action)) {
            String stringID = (String) request.getParameter("fileID");
            String numSpaces = (String) request.getParameter("numSpaces");
            int fileIDtoMove;
            int spaces;
            if ((stringID != null) && (valid_user)) {
                fileIDtoMove = Integer.parseInt(stringID);
                spaces = Integer.parseInt(numSpaces);
                stream.moveInQueue(fileIDtoMove, spaces);
            }
            request.getRequestDispatcher(REFRESH_JSP).forward(request, response);
        } else if ("randomizeQueue".equals(action)) {
            stream.randomizeQueue();
            request.getRequestDispatcher(REFRESH_JSP).forward(request, response);
        } else if ("clearQueue".equals(action)) {
            stream.clearQueue();
            request.getRequestDispatcher(REFRESH_JSP).forward(request, response);
        } else if ("skip".equals(action)) {
            if (valid_user) {
                stream.skip();
                request.getRequestDispatcher(REFRESH_JSP).forward(request, response);
            }
        } else if ("playlist".equals(action)) {
            String sleepTimer = request.getParameter("timer");
            response.setContentType("audio/x-scpls");
            PrintWriter out = response.getWriter();
            out.println("[playlist]");
            out.println("numberofentries=1");
            out.print("File1=http://" + stream.getHostName() + ":" + stream.getPortNumber());
            if (sleepTimer != null) {
                out.println("/timer/" + sleepTimer);
            } else {
                out.println();
            }
            out.println("Title1=" + stream.getHostDescription());
            out.println("Length1=-1");
            out.println("Version=2");
        } else if ("m3u".equals(action) || "m3u.m3u".equals(action)) {
            String sleepTimer = request.getParameter("timer");
            response.setContentType("audio/x-mpegurl");
            PrintWriter out = response.getWriter();
            out.println("#EXTM3U");
            out.println("#EXTINF:0," + stream.getHostDescription());
            out.print("http://" + stream.getHostName() + ":" + stream.getPortNumber());
            if (sleepTimer != null) {
                out.println("/timer/" + sleepTimer);
            } else {
                out.println();
            }
        } else if ("reloadFiles".equals(action)) {
            reloadFiles(request);
            request.getRequestDispatcher(REFRESH_JSP).forward(request, response);
        } else if ("info".equals(action)) {
            int fileID = Integer.parseInt(request.getParameter("fileID"));
            MP3File mp3File = stream.getMP3File(fileID);
            SongInfo songInfo = mp3File.getSongInfo();
            int rootNameLength = stream.getRoot().getFile().getPath().length();
            request.setAttribute("mp3File", mp3File);
            request.setAttribute("songInfo", songInfo);
            request.setAttribute("rootNameLength", new Integer(rootNameLength));
            request.getRequestDispatcher(SONG_INFO_JSP).forward(request, response);
        } else if ("rss".equals(action)) {
            response.setContentType("text/xml");
            PrintWriter out = response.getWriter();
            out.println("<?xml version=\"1.0\" ?>");
            out.println("<rss version=\"2.0\">");
            out.println("<channel>");
            out.println("<title>Streamsicle</title>");
            out.println("<link>http://" + stream.getHostName() + ":" + stream.getPortNumber() + "</link>");
            out.println("<description>Streamsicle - take a lick</description>");
            out.println("<item>");
            out.println("<title>Now Playing</title>");
            out.println("<description><![CDATA[" + stream.getCurrent().getName() + "]]></description>");
            out.println("</item>");
            out.println("</channel>");
            out.println("</rss>");
            out.flush();
        } else if ("startExstreamer".equals(action)) {
            String addr = (String) request.getParameter("addr");
            log.debug("Starting exstreamer at address " + addr + " by user request.");
            stream.getExDetector().startPushStreaming(addr);
            request.getRequestDispatcher(REFRESH_JSP).forward(request, response);
        } else if ("stopExstreamer".equals(action)) {
            String addr = (String) request.getParameter("addr");
            log.debug("Starting exstreamer at address " + addr + " by user request.");
            server.kickClient(addr);
            request.getRequestDispatcher(REFRESH_JSP).forward(request, response);
        } else if (ACTION_VIEW_GENRE.equals(action)) {
            String genre = request.getParameter(PARAM_GENRE);
            request.setAttribute(ATTRIBUTE_ID3MODE, ID3MODE_GENRE);
            request.setAttribute(ATTRIBUTE_ID3PARAM, genre);
            setMainAttributes(request);
            request.getRequestDispatcher(MAIN_JSP).forward(request, response);
        } else if (ACTION_VIEW_ARTIST.equals(action)) {
            String artist = request.getParameter(PARAM_ARTIST);
            request.setAttribute(ATTRIBUTE_ID3MODE, ID3MODE_ARTIST);
            request.setAttribute(ATTRIBUTE_ID3PARAM, artist);
            setMainAttributes(request);
            request.getRequestDispatcher(MAIN_JSP).forward(request, response);
        } else if (ACTION_VIEW_ALBUM.equals(action)) {
            String album = request.getParameter(PARAM_ALBUM);
            request.setAttribute(ATTRIBUTE_ID3MODE, ID3MODE_ALBUM);
            request.setAttribute(ATTRIBUTE_ID3PARAM, album);
            setMainAttributes(request);
            request.getRequestDispatcher(MAIN_JSP).forward(request, response);
        } else {
            setMainAttributes(request);
            request.getRequestDispatcher(MAIN_JSP).forward(request, response);
        }
    }

    /**
     *  Description of the Method
     *
     *@param  request               Description of the Parameter
     *@param  response              Description of the Parameter
     *@exception  ServletException  Description of the Exception
     *@exception  IOException       Description of the Exception
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     *  Sets attributes that are needed by main.jsp and its includes
     *
     *@param  request  The new mainAttributes value
     */
    public void setMainAttributes(HttpServletRequest request) {
        String current = stream.getCurrent().getName();
        request.setAttribute("current", current);
        request.setAttribute(ATTRIBUTE_STREAM, stream);
        request.setAttribute("server", server);
        request.setAttribute("upgradeLink", upgradeLink);
        HttpSession session = request.getSession(true);
        if ("requestTree".equals((String) session.getAttribute(ATTRIB_RIGHT_TAB))) {
            setNewListAttributes(request);
        } else if ("searchreq".equals((String) session.getAttribute(ATTRIB_RIGHT_TAB))) {
            setSearchAttributes(request);
        } else {
            setNewListAttributes(request);
        }
    }

    /**
     *  Sets attributes on the request that are needed by newlist.jsp and
     *  requestTreeInc.jsp.
     *
     *@param  request  The new newListAttributes value
     */
    public void setNewListAttributes(HttpServletRequest request) {
        Integer dirID = null;
        MP3File browseRoot = null;
        HttpSession session = request.getSession(true);
        String dirIDStr = request.getParameter(PARAM_DIR_ID);
        if (dirIDStr != null) {
            try {
                dirID = new Integer(dirIDStr);
                browseRoot = stream.getMP3File(dirID.intValue());
                if (browseRoot == null) {
                    dirID = null;
                } else {
                    if (!browseRoot.getFile().isDirectory()) {
                        browseRoot = stream.getRoot();
                    }
                    session.setAttribute(ATTRIB_DIR_ID, dirID);
                }
            } catch (NumberFormatException nfe) {
                dirID = null;
            }
        }
        if (dirID == null) {
            Object dirIDObj = null;
            if ((dirIDStr != null) && (Integer.parseInt(dirIDStr) == stream.getRoot().getFileID()) && stream.getRoot().virtualDirectory) {
            } else {
                dirIDObj = session.getAttribute(ATTRIB_DIR_ID);
            }
            if (dirIDObj != null && dirIDObj instanceof Integer) {
                dirID = (Integer) dirIDObj;
                browseRoot = stream.getMP3File(dirID.intValue());
                if (browseRoot == null) {
                    browseRoot = stream.getRoot();
                    session.setAttribute(ATTRIB_DIR_ID, new Integer(browseRoot.getFileID()));
                }
            } else {
                browseRoot = stream.getRoot();
                dirID = new Integer(browseRoot.getFileID());
                session.setAttribute(ATTRIB_DIR_ID, dirID);
            }
        }
        String[] fileList = new String[browseRoot.getMP3FileChildren().size()];
        int[] fileIDList = new int[browseRoot.getMP3FileChildren().size()];
        String[] dirList = new String[browseRoot.getDirectoryChildren().size()];
        int[] dirIDList = new int[browseRoot.getDirectoryChildren().size()];
        for (int i = 0; i < fileList.length; i++) {
            Vector fileKids = browseRoot.getMP3FileChildren();
            fileList[i] = ((MP3File) fileKids.elementAt(i)).getName();
            fileIDList[i] = ((MP3File) fileKids.elementAt(i)).getFileID();
        }
        for (int i = 0; i < dirList.length; i++) {
            Vector dirKids = browseRoot.getDirectoryChildren();
            dirList[i] = ((MP3File) dirKids.elementAt(i)).getName();
            dirIDList[i] = ((MP3File) dirKids.elementAt(i)).getFileID();
        }
        request.setAttribute(Constants.REQUEST_NEW_FILE_LIST, fileList);
        request.setAttribute(Constants.REQUEST_NEW_FILE_ID_LIST, fileIDList);
        request.setAttribute(Constants.REQUEST_NEW_DIR_LIST, dirList);
        request.setAttribute(Constants.REQUEST_NEW_DIR_ID_LIST, dirIDList);
        request.setAttribute(Constants.REQUEST_FILE_LIST, browseRoot.getMP3FileChildren());
        request.setAttribute(Constants.REQUEST_DIR_LIST, browseRoot.getDirectoryChildren());
        String currdir = null;
        if (!browseRoot.isVirtualDirectory()) {
            currdir = browseRoot.getFile().getAbsolutePath();
            if (currdir.indexOf(browseRoot.getFile().separator) != -1) {
                currdir = currdir.substring(currdir.indexOf(browseRoot.getFile().separator));
            }
        }
        Integer upID = new Integer(browseRoot.getParent().getFileID());
        request.setAttribute(Constants.PARAM_CURR_DIR, browseRoot);
        request.setAttribute(Constants.PARAM_UP_DIR, upID);
    }

    /**
     *  Sets the searchAttributes attribute of the Streamsicle object
     *
     *@param  request  The new searchAttributes value
     */
    public void setSearchAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String searchstring = request.getParameter(PARAM_SEARCH_STRING);
        String sessionString = (String) session.getAttribute(ATTRIB_SEARCH_STRING);
        Long searchExecuted = (Long) session.getAttribute(Constants.SESSION_SEARCH_TIME_EXECUTED);
        if (lastReloadTime == null) {
            lastReloadTime = new Long(0);
        }
        if (searchExecuted == null) {
            searchExecuted = new Long(1);
        }
        if ((searchstring != null) || (sessionString == null) || (lastReloadTime.compareTo(searchExecuted) > 0)) {
            if ((lastReloadTime.compareTo(searchExecuted) > 0) && (searchstring == null)) {
                if (sessionString != null) {
                    searchstring = sessionString;
                }
            }
            if (searchstring != null) {
                session.setAttribute(ATTRIB_SEARCH_STRING, searchstring);
            } else {
                searchstring = "";
            }
            Vector songs = stream.getList();
            Vector mp3Files = new Vector();
            Vector mp3Dirs = new Vector();
            if (!"".equals(searchstring)) {
                for (int i = 0; i < songs.size(); i++) {
                    if (((MP3File) songs.elementAt(i)).getFile().isDirectory()) {
                        if ((((MP3File) songs.elementAt(i)).getFile().getName().toLowerCase().indexOf(searchstring.toLowerCase()) >= 0)) {
                            mp3Dirs.addElement(songs.elementAt(i));
                        }
                    } else {
                        if (((MP3File) songs.elementAt(i)).getFile().getName().toLowerCase().indexOf(searchstring.toLowerCase()) >= 0) {
                            mp3Files.addElement(songs.elementAt(i));
                        } else if (((MP3File) songs.elementAt(i)).getAlbum().toLowerCase().indexOf(searchstring.toLowerCase()) >= 0) {
                            mp3Files.addElement(songs.elementAt(i));
                        } else if (((MP3File) songs.elementAt(i)).getArtist().toLowerCase().indexOf(searchstring.toLowerCase()) >= 0) {
                            mp3Files.addElement(songs.elementAt(i));
                        }
                    }
                }
            }
            session.setAttribute(Constants.SESSION_SEARCH_DIR_LIST, mp3Dirs);
            session.setAttribute(Constants.SESSION_SEARCH_FILE_LIST, mp3Files);
            session.setAttribute(Constants.SESSION_SEARCH_TIME_EXECUTED, new Long(System.currentTimeMillis()));
            request.setAttribute(Constants.REQUEST_SEARCH_DIR_LIST, mp3Dirs);
            request.setAttribute(Constants.REQUEST_SEARCH_FILE_LIST, mp3Files);
        } else {
            request.setAttribute(Constants.SESSION_SEARCH_DIR_LIST, session.getAttribute(Constants.SESSION_SEARCH_DIR_LIST));
            request.setAttribute(Constants.SESSION_SEARCH_FILE_LIST, session.getAttribute(Constants.SESSION_SEARCH_FILE_LIST));
        }
    }

    /**
     *  Sets the sessionData attribute of the Streamsicle object
     *
     *@param  request  The new sessionData value
     */
    private void setSessionData(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String leftTab = request.getParameter(PARAM_LEFT_TAB);
        if (leftTab != null) {
            session.setAttribute(ATTRIB_LEFT_TAB, leftTab);
        }
        String rightTab = request.getParameter(PARAM_RIGHT_TAB);
        if (rightTab != null) {
            session.setAttribute(ATTRIB_RIGHT_TAB, rightTab);
        }
    }

    private boolean reloading = false;

    /**
     *  Gets the reloading attribute of the Streamsicle object
     *
     *@return    The reloading value
     */
    public boolean isReloading() {
        return reloading;
    }

    /**
     *  Sets the reloading attribute of the Streamsicle object
     *
     *@param  value  The new reloading value
     */
    public void setReloading(boolean value) {
        reloading = value;
    }

    private boolean configured = false;

    private String configureErrorMessage = "";

    /**
     *  Gets the configured attribute of the Streamsicle object
     *
     *@return    The configured value
     */
    public boolean isConfigured() {
        return configured;
    }

    /**
     * Return any error messages from the latest configuration attempt
     * @return
     */
    public String getConfigurationErrorMessage() {
        return configureErrorMessage;
    }

    /**
     *  Sets the configured attribute of the Streamsicle object
     *
     *@param  value  The new configured value
     */
    public void setConfigured(boolean value) {
        configured = value;
    }

    /**
     * Sets the configured attribute of the Streamsicle object and sets
     * an error message to display on the configuration screen.
     * @param value
     * @param errorMessage
     */
    public void setConfigured(boolean value, String errorMessage) {
        log.debug("Setting configured to " + value + " and error message to " + errorMessage);
        configureErrorMessage = errorMessage;
        setConfigured(value);
    }

    /**
     *  Description of the Method
     *
     *@param  request  Description of the Parameter
     */
    public void reloadFiles(HttpServletRequest request) {
        String enteredPassword = request.getParameter("password");
        String adminPassword = props.getProperty("streamsicle.adminPassword");
        if (adminPassword == null || adminPassword.trim().equals("") || (enteredPassword != null && enteredPassword.trim().equals(adminPassword.trim()))) {
            doReload();
            lastReloadTime = new Long(System.currentTimeMillis());
        }
    }

    /**
     *  Description of the Method
     */
    public void doReload() {
        final InteractiveStream interactiveStream = stream;
        if (interactiveStream != null) {
            setReloading(true);
            Thread reloadThread = new Thread(new Runnable() {

                public void run() {
                    interactiveStream.initPlayDir();
                    setReloading(false);
                }
            }, "Reload Thread");
            reloadThread.start();
        }
    }
}
