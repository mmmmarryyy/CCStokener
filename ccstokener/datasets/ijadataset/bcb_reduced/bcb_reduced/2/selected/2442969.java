package libomv;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Set;
import java.util.prefs.Preferences;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import libomv.StructuredData.OSD;
import libomv.StructuredData.OSDArray;
import libomv.StructuredData.OSDMap;
import libomv.StructuredData.OSD.OSDFormat;
import libomv.StructuredData.OSD.OSDType;
import libomv.StructuredData.OSDString;
import libomv.assets.AssetManager;
import libomv.capabilities.CapsMessage;
import libomv.inventory.InventoryManager;
import libomv.utils.Helpers;

public class GridClient {

    public class GridInfo implements Cloneable {

        public String gridnick;

        public String gridname;

        public String platform;

        public String loginuri;

        public String loginpage;

        public String helperuri;

        public String website;

        public String support;

        public String register;

        public String passworduri;

        public int version;

        public transient boolean saveSettings;

        public transient boolean savePassword;

        public transient String firstname;

        public transient String lastname;

        public transient String startLocation;

        private transient String password;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            if (password.length() != 35 && !password.startsWith("$1$")) {
                password = Helpers.MD5Password(password);
            }
            this.password = password;
        }

        @Override
        public GridInfo clone() {
            try {
                return (GridInfo) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new Error("This should not occur since we implement Cloneable");
            }
        }

        /**
		 * Merge in the grid info for all null fields in our record
		 * 
		 * @param info The info to merge in
		 */
        public void merge(GridInfo info) {
            saveSettings |= info.saveSettings;
            savePassword = saveSettings && (info.savePassword || savePassword);
            if (firstname == null) firstname = info.firstname;
            if (lastname == null) lastname = info.lastname;
            if (password == null) password = info.password;
            if (startLocation == null) startLocation = info.startLocation;
            if (version < info.version) {
                if (gridnick == null || version >= 0) gridnick = info.gridnick;
                if (gridname == null || version >= 0) gridname = info.gridname;
                if (platform == null || version >= 0) platform = info.platform;
                if (loginuri == null || version >= 0) loginuri = info.loginuri;
                if (loginpage == null || version >= 0) loginpage = info.loginpage;
                if (helperuri == null || version >= 0) helperuri = info.helperuri;
                if (website == null || version >= 0) website = info.website;
                if (support == null || version >= 0) support = info.support;
                if (register == null || version >= 0) register = info.register;
                version = info.version;
            }
            if (!equals(info)) version++;
        }

        public String dump() {
            return String.format("Nick: %s, Name: %s, Platform: %s, Ver: %d\n" + "loginuri: %s, loginpage: %s, website: %s, support: %s\n", gridnick, gridname, platform, version, loginuri, loginpage, website, support);
        }

        @Override
        public int hashCode() {
            int hash = 0;
            String string = null;
            for (int i = 0; i < 9; i++) {
                switch(i) {
                    case 0:
                        string = gridnick;
                        break;
                    case 1:
                        string = gridname;
                        break;
                    case 2:
                        string = loginuri;
                        break;
                    case 3:
                        string = loginpage;
                        break;
                    case 4:
                        string = helperuri;
                        break;
                    case 5:
                        string = website;
                        break;
                    case 6:
                        string = support;
                        break;
                    case 7:
                        string = register;
                        break;
                    case 8:
                        string = platform;
                        break;
                }
                if (string != null) hash ^= string.hashCode();
            }
            return hash;
        }

        @Override
        public boolean equals(Object info) {
            return (info != null && info instanceof GridInfo) ? equals((GridInfo) info) : false;
        }

        public boolean equals(GridInfo info) {
            String string1 = null, string2 = null;
            for (int i = 0; i < 9; i++) {
                switch(i) {
                    case 0:
                        string1 = gridnick;
                        string2 = info.gridnick;
                        break;
                    case 1:
                        string1 = gridname;
                        string2 = info.gridname;
                        break;
                    case 2:
                        string1 = loginuri;
                        string2 = info.loginuri;
                        break;
                    case 3:
                        string1 = loginpage;
                        string2 = info.loginpage;
                        break;
                    case 4:
                        string1 = helperuri;
                        string2 = info.helperuri;
                        break;
                    case 5:
                        string1 = website;
                        string2 = info.website;
                        break;
                    case 6:
                        string1 = support;
                        string2 = info.support;
                        break;
                    case 7:
                        string1 = register;
                        string2 = info.register;
                        break;
                    case 8:
                        string1 = platform;
                        string2 = info.platform;
                        break;
                }
                if (string1 == null || !string1.equals(string2)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return gridname + " (" + gridnick + ")";
        }
    }

    private static final String listUri = "http://libomv-java.sourceforge.net/grids/default_grids.xml";

    private static HashMap<String, GridInfo> gridlist = new HashMap<String, GridInfo>();

    private static int listversion = 0;

    private String defaultGrid = null;

    private static final String GRIDINFO = "gridinfo";

    private static final String GRID_LIST = "gridlist";

    private static final String DEFAULT_GRID = "defaultGrid";

    private static final String DEFAULT_GRID_NAME1 = "osgrid";

    private static final String DEFAULT_GRID_NAME2 = "secondlife";

    private static final String GRID_INFO_PROTOCOL = "get_grid_info";

    private static final String DEFAULT_GRIDS_VERSION = "default_grids_version";

    private static final String DEFAULT_GRIDS_LIST = "/res/default_grids.xml";

    public NetworkManager Network;

    public LoginManager Login;

    public CapsMessage Messages;

    public AgentThrottle Throttle;

    public Settings Settings;

    public AgentManager Self;

    public AvatarManager Avatars;

    public FriendsManager Friends;

    public GroupManager Groups;

    public GridManager Grid;

    public AssetManager Assets;

    public InventoryManager Inventory;

    public SoundManager Sound;

    public AppearanceManager Appearance;

    public ParcelManager Parcels;

    public ObjectManager Objects;

    public DirectoryManager Directory;

    public Statistics Stats;

    public GridClient() throws Exception {
        this(new Settings());
    }

    public GridClient(Settings settings) throws Exception {
        initializeGridList();
        Login = new LoginManager(this);
        Network = new NetworkManager(this);
        Messages = new CapsMessage();
        Settings = settings.Startup(this);
        Self = new AgentManager(this);
        Friends = new FriendsManager(this);
        Groups = new GroupManager(this);
        Grid = new GridManager(this);
        if (Settings.SEND_AGENT_THROTTLE) Throttle = new AgentThrottle(this);
        if (Settings.ENABLE_AVATAR_MANAGER) Avatars = new AvatarManager(this);
        if (Settings.ENABLE_ASSET_MANAGER) Assets = new AssetManager(this);
        if (Settings.ENABLE_INVENTORY_MANAGER) Inventory = new InventoryManager(this);
        if (Settings.ENABLE_SOUND_MANAGER) Sound = new SoundManager(this);
        if (Settings.ENABLE_PARCEL_MANAGER) Parcels = new ParcelManager(this);
        if (Settings.ENABLE_OBJECT_MANAGER) Objects = new ObjectManager(this);
        if (Settings.ENABLE_DIRECTORY_MANAGER) Directory = new DirectoryManager(this);
        Stats = new Statistics();
    }

    public GridInfo[] getGridInfos() {
        GridInfo[] grids = new GridInfo[gridlist.size()];
        return gridlist.values().toArray(grids);
    }

    public Set<String> getGridNames() {
        return gridlist.keySet();
    }

    public GridInfo getGrid(String grid) {
        return gridlist.get(grid);
    }

    public GridInfo getDefaultGrid() {
        if (defaultGrid == null || defaultGrid.isEmpty()) setDefaultGrid((String) null);
        return gridlist.get(defaultGrid);
    }

    public void setDefaultGrid(GridInfo grid) {
        setDefaultGrid(grid != null ? grid.gridnick : null);
    }

    public void setDefaultGrid(String gridnick) {
        if (gridnick != null && gridlist.containsKey(gridnick)) {
            defaultGrid = gridnick;
        } else if (gridlist.containsKey(DEFAULT_GRID_NAME1)) {
            defaultGrid = DEFAULT_GRID_NAME1;
        } else if (gridlist.containsKey(DEFAULT_GRID_NAME2)) {
            defaultGrid = DEFAULT_GRID_NAME2;
        } else if (!gridlist.isEmpty()) {
            defaultGrid = gridlist.keySet().iterator().next();
        } else {
            defaultGrid = Helpers.EmptyString;
        }
    }

    public GridInfo addGrid(GridInfo info) {
        return gridlist.put(info.gridnick, info);
    }

    public GridInfo removeGrid(String grid) {
        GridInfo info = gridlist.remove(grid);
        if (info != null) {
            if (grid.equals(defaultGrid)) {
                setDefaultGrid((String) null);
            }
        }
        return info;
    }

    public void clearGrids() {
        gridlist.clear();
    }

    /**
	 * Retrieves the GridInfo settings from the grid user server, when the
	 * server supports the GridInfo protocol.
	 * 
	 * @param loginuri
	 *            The HTTP address of the user server
	 * @return a filled in GridInfo if the call was successful, null otherwise
	 * @throws Exception
	 */
    public GridInfo queryGridInfo(GridInfo grid) throws Exception {
        GridInfo info = null;
        HttpClient client = new DefaultHttpClient();
        HttpGet getMethod = new HttpGet(new URI(grid.loginuri + GRID_INFO_PROTOCOL));
        try {
            HttpResponse response = client.execute(getMethod);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream stream = entity.getContent();
                    String charset = null;
                    if (entity.getContentType() != null) {
                        HeaderElement values[] = entity.getContentType().getElements();
                        if (values.length > 0) {
                            NameValuePair param = values[0].getParameterByName("charset");
                            if (param != null) {
                                charset = param.getValue();
                            }
                        }
                    }
                    if (charset == null) {
                        charset = HTTP.DEFAULT_CONTENT_CHARSET;
                    }
                    XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                    parser.setInput(stream, charset);
                    parser.nextTag();
                    parser.require(XmlPullParser.START_TAG, null, GRIDINFO);
                    if (!parser.isEmptyElementTag()) {
                        parser.nextTag();
                        info = parseRecord(parser);
                    }
                }
            }
        } finally {
            getMethod.abort();
        }
        if (info != null) {
            info.merge(grid);
        }
        return info;
    }

    private GridInfo parseRecord(XmlPullParser parser) throws XmlPullParserException, IOException {
        GridInfo info = new GridInfo();
        info.version = -1;
        parser.require(XmlPullParser.START_TAG, null, null);
        do {
            if (parser.isEmptyElementTag()) {
                parser.nextTag();
            } else {
                String name = parser.getName();
                if (name.equals("gridnick")) {
                    info.gridnick = parser.nextText().trim();
                } else if (name.equals("gridname")) {
                    info.gridname = parser.nextText().trim();
                } else if (name.equals("platform")) {
                    info.platform = parser.nextText().trim();
                } else if (name.equals("login") || name.equals("loginuri")) {
                    info.loginuri = parser.nextText().trim();
                } else if (name.equals("welcome") || name.equals("loginpage")) {
                    info.loginpage = parser.nextText().trim();
                } else if (name.equals("economy") || name.equals("helperuri")) {
                    info.helperuri = parser.nextText().trim();
                } else if (name.equals("about") || name.equals("website")) {
                    info.website = parser.nextText().trim();
                } else if (name.equals("help") || name.equals("support")) {
                    info.support = parser.nextText().trim();
                } else if (name.equals("register") || name.equals("account")) {
                    info.register = parser.nextText().trim();
                } else if (name.equals("password")) {
                    info.passworduri = parser.nextText().trim();
                } else if (name.equals("firstname")) {
                    info.firstname = parser.nextText().trim();
                    info.saveSettings = true;
                } else if (name.equals("lastname")) {
                    info.lastname = parser.nextText().trim();
                    info.saveSettings = true;
                } else if (name.equals("startLocation")) {
                    info.startLocation = parser.nextText().trim();
                } else if (name.equals("userpassword")) {
                    info.password = parser.nextText().trim();
                    info.savePassword = true;
                } else {
                    parser.nextTag();
                }
            }
        } while (parser.nextTag() == XmlPullParser.START_TAG);
        return info;
    }

    private void initializeGridList() throws IOException, ParseException, IllegalStateException, URISyntaxException, IllegalArgumentException, IllegalAccessException {
        boolean modified = setList(loadSettings(), false);
        modified |= setList(loadDefaults(), true);
        modified |= setList(downloadList(), true);
        if (modified) saveList();
    }

    private boolean setList(OSD list, boolean merge) throws IllegalArgumentException, IllegalAccessException {
        if (list == null || list.getType() != OSDType.Array) return false;
        if (!merge) {
            gridlist.clear();
            listversion = 0;
        }
        boolean modified = false;
        int version = 0;
        OSDArray array = (OSDArray) list;
        for (int i = 0; i < array.size(); i++) {
            OSDMap map = (OSDMap) array.get(i);
            if (map.containsKey(DEFAULT_GRIDS_VERSION)) {
                version = map.get(DEFAULT_GRIDS_VERSION).AsInteger();
                if (version <= listversion) {
                    return false;
                }
            } else {
                GridInfo newinfo = new GridInfo();
                map.deserializeMembers(newinfo);
                GridInfo oldinfo = gridlist.get(newinfo.gridname);
                if (!merge || oldinfo == null || oldinfo.version < newinfo.version) {
                    gridlist.put(newinfo.gridnick, newinfo);
                    modified = true;
                }
            }
            if (modified) listversion = version;
        }
        return modified;
    }

    private void saveList() throws IllegalArgumentException, IllegalAccessException, IOException {
        OSDArray array = new OSDArray();
        OSDMap map = new OSDMap();
        map.put(DEFAULT_GRIDS_VERSION, OSD.FromInteger(listversion));
        array.add(map);
        for (GridInfo info : gridlist.values()) {
            OSDMap members = OSD.serializeMembers(info);
            if (info.saveSettings) {
                members.put("firstname", OSDString.FromString(info.firstname));
                members.put("lastname", OSDString.FromString(info.lastname));
                members.put("startLocation", OSDString.FromString(info.startLocation));
                if (info.savePassword) {
                    members.put("userpassword", OSDString.FromString(info.password));
                }
            }
            array.add(members);
        }
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        prefs.put(GRID_LIST, array.serializeToString(OSDFormat.Xml));
        prefs.put(DEFAULT_GRID, defaultGrid);
    }

    private OSD loadDefaults() throws IOException, ParseException {
        OSD osd = null;
        InputStream stream = getClass().getResourceAsStream(DEFAULT_GRIDS_LIST);
        if (stream != null) {
            try {
                osd = OSD.parse(stream, Helpers.UTF8_ENCODING);
            } finally {
                stream.close();
            }
        }
        return osd;
    }

    private OSD loadSettings() throws IOException, ParseException {
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        defaultGrid = prefs.get(DEFAULT_GRID, Helpers.EmptyString);
        return OSD.parse(prefs.get(GRID_LIST, Helpers.EmptyString));
    }

    private OSD downloadList() throws IOException, IllegalStateException, ParseException, URISyntaxException {
        OSD osd = null;
        HttpClient client = new DefaultHttpClient();
        HttpGet getMethod = new HttpGet(new URI(listUri));
        try {
            HttpResponse response = client.execute(getMethod);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
            }
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream stream = entity.getContent();
                String charset = null;
                if (entity.getContentType() != null) {
                    HeaderElement values[] = entity.getContentType().getElements();
                    if (values.length > 0) {
                        NameValuePair param = values[0].getParameterByName("charset");
                        if (param != null) {
                            charset = param.getValue();
                        }
                    }
                }
                if (charset == null) {
                    charset = HTTP.DEFAULT_CONTENT_CHARSET;
                }
                osd = OSD.parse(stream, charset);
            }
        } finally {
            getMethod.abort();
        }
        return osd;
    }

    public String dumpGridlist() {
        String string = String.format("Version: %d, Default: %s\n", listversion, defaultGrid);
        for (GridInfo info : gridlist.values()) {
            string += info.dump();
        }
        return string;
    }

    @Override
    public String toString() {
        return Self.getName();
    }

    public void Tick(long millis) throws Exception {
        Thread.sleep(millis);
    }

    public void Tick() throws Exception {
        Thread.sleep(0);
    }
}
