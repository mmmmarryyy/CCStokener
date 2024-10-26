package multivalent;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.io.*;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Timer;
import javax.imageio.ImageIO;
import phelps.net.URIs;

/**
	Execute application startup, and act as a central repository for resources shared among all browser windows.

	<p>Shared resources:
	<ul>
	<li>{@link #getPreference(String,String) preferences} - name-value pairs that are saved to disk and shared among all browsers.
		The system preferences are read in first, then the user-specific preferences, which can override.
	<li>{@link #getBrowser(String) list} of {@link Browser} instances, addressable by name
	<li>{@link #getCache() Cache}
	<li>{@link #getTimer() Timer}, as used by the cursor and blinking text
	<li>{@link #remapBehavior(String) behavior remapping} - updates behavior use to third party additions, according to table in <tt>Preferences.txt</tt>.
	</ul>

	Specification and implementation versions are available from the {@link java.lang.Package}.

	@version $Revision: 1022 $ $Date: 2007-06-06 03:55:55 -0400 (Wed, 06 Jun 2007) $
*/
public final class Multivalent {

    public static final boolean DEVEL = false;

    public static final String VERSION = "hacked_for_docco_release";

    /** General monitoring flag.  <code>true</code> while developing, <code>false</code> when compile for distribution to users. */
    public static final boolean MONITOR = false;

    /**
	<!--Invoke {link #destroy()} in the preferred way, one that can be cancelled.-->
	Safely destroy browsers and all their documents, and Destroy .
	<p><tt>"EXIT"</tt>: <tt>arg=</tt> {@link java.util.HashMap} <var>attributes</var>, <tt>in=</tt> {@link multivalent.INode} <var>root of tree</var>, <tt>out=</tt><var>unused</var>.
  */
    public static final String MSG_EXIT = "EXIT";

    public static final String FILENAME_PREFERENCES = "Preferences.txt";

    public static final String PREF_CACHEDIR = "CACHEDIR";

    /** URI of Multivalent home WWW site. */
    public static final URI HOME_SITE = URI.create("http://www.cs.berkeley.edu/~phelps/Multivalent/");

    /** Singleton instance. */
    private static Multivalent instance_ = null;

    private static boolean standalone_ = false;

    /** ClassLoader with JARs found in same directory. */
    private static ClassLoader cl_;

    /** Tables mapping suffix/MIME type to media adaptor name. */
    CHashMap defadaptor_, adaptor_ = new CHashMap(100);

    HashMap defberemap_, beremap_ = new HashMap(100);

    CHashMap defpref_, pref_ = new CHashMap(100);

    List browsers_ = new ArrayList(5);

    Cache cache_ = null;

    Timer timer_ = null;

    private Multivalent() {
    }

    /**
	Returns singleton instance (use instead of a constructor), from which preferences and other state can be accessed.
	<!-- Make all methods static? -->
  */
    public static Multivalent getInstance() {
        if (instance_ == null) {
            if (standalone_) System.out.println("VERSION = " + VERSION);
            cl_ = new URLClassLoader(findJARs());
            instance_ = new Multivalent();
            instance_.readTables();
        }
        return instance_;
    }

    public static Multivalent getInstance(ClassLoader classLoader) {
        if (instance_ == null) {
            if (standalone_) System.out.println("VERSION = " + VERSION);
            cl_ = classLoader;
            instance_ = new Multivalent();
            instance_.readTables();
        }
        return instance_;
    }

    /** Find other JARs in same directory to add to CLASSPATH. */
    private static URL[] findJARs() {
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                String lcname = name.toLowerCase();
                return lcname.endsWith(".jar") && !lcname.startsWith("multivalent") && new File(dir, name).canRead();
            }
        };
        String jar = URIs.decode(Multivalent.class.getResource("Multivalent.class").toString());
        String top;
        if (jar.startsWith("jar:")) {
            jar = jar.substring("jar:file:".length(), jar.indexOf('!'));
            top = jar.substring(0, jar.lastIndexOf('/') + 1);
        } else {
            int inx = jar.lastIndexOf('/');
            inx = jar.lastIndexOf('/', inx - 1);
            jar = jar.substring("file:".length(), inx + 1);
            top = jar;
        }
        List urls = new ArrayList(20);
        if (standalone_) System.out.println("Searching for JARs in " + top);
        try {
            File[] f = new File(top).listFiles(filter);
            for (int i = 0, imax = f.length; i < imax; i++) {
                urls.add(f[i].toURI().toURL());
                if (standalone_) System.out.println("\t" + f[i]);
            }
        } catch (MalformedURLException canthappen) {
            System.err.println(canthappen);
            System.err.println("Move to different directory");
        }
        return (URL[]) urls.toArray(new URL[0]);
    }

    public Map getGenreMap() {
        return adaptor_;
    }

    /** Used by {@link Behavior#getInstance}.  Maybe move there, or less likely move Behavior.getInstance here. */
    public ClassLoader getJARsClassLoader() {
        return cl_;
    }

    /** File cache. */
    public Cache getCache() {
        return cache_;
    }

    /** Heartbeat timer calls observers every 100 ms. */
    public Timer getTimer() {
        if (timer_ == null) timer_ = new java.util.Timer();
        return timer_;
    }

    /**
	Returns preferred behavior according to substitution map in <tt>Preferences.txt</tt>.
	Clients can ask for the preferred "Hyperlink" and get the lastest-greatest, which was written after the client.
	Replacements must subclass what they replace and should generally recognize as many attributes as applicable.
	{@link Behavior#getInstance(String, String, Map, Layer)} remaps all behavior names.
  */
    public String remapBehavior(String bename) {
        assert bename != null;
        String remap = (String) beremap_.get(bename);
        return (remap != null ? remap : bename);
    }

    /**
	Returns preference under passed <var>key</var>,
	or if it doesn't exists sets value with <var>defaultval</var> and establishes this as the future preference value.
	Keys are case insensitive.
  */
    public final String getPreference(String key, String defaultval) {
        assert key != null;
        String val = (String) pref_.get(key);
        if (val == null && defaultval != null) {
            val = defaultval;
            putPreference(key, val);
        }
        return val;
    }

    public final void putPreference(String key, String val) {
        assert key != null;
        pref_.put(key, val);
    }

    public final void removePreference(String key) {
        assert key != null;
        pref_.remove(key);
    }

    public final Iterator prefKeyIterator() {
        return pref_.keySet().iterator();
    }

    /**
	Reads preferences, system or user, overwriting existing settings.
	TO DO: preserve comments through read/write cycle.
  */
    void readPreferences(InputStream prefin) {
        BufferedReader prefr = new BufferedReader(new InputStreamReader(prefin));
        StreamTokenizer st = new StreamTokenizer(prefr);
        st.eolIsSignificant(true);
        st.resetSyntax();
        st.whitespaceChars(0, ' ');
        st.wordChars(' ' + 1, 0x7e);
        st.commentChar('#');
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar('"');
        try {
            String key, val;
            for (int token = st.nextToken(); token != StreamTokenizer.TT_EOF; ) {
                if (token == StreamTokenizer.TT_EOL) {
                    token = st.nextToken();
                    continue;
                }
                String cmd = st.sval;
                if (cmd != null) cmd = cmd.intern();
                st.nextToken();
                key = st.sval;
                st.nextToken();
                val = st.sval;
                if ("mediaadaptor" == cmd) {
                    adaptor_.put(key.toLowerCase(), val);
                } else if ("remap" == cmd) {
                    beremap_.put(key, val);
                } else if ("set" == cmd) {
                    putPreference(key, val);
                }
                do {
                    token = st.nextToken();
                } while (token != StreamTokenizer.TT_EOL && token != ';' && token != StreamTokenizer.TT_EOF);
            }
            prefr.close();
        } catch (IOException ignore) {
            System.err.println("can't read prefs " + ignore);
        }
    }

    void writePreferences() {
        try {
            File pref = getCache().mapTo(null, FILENAME_PREFERENCES, Cache.USER);
            System.out.println("writing preferences to " + pref);
            Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pref)));
            w.write("# If line begins with '#', command is commented out\n\n\n");
            writePrefTable(w, adaptor_, defadaptor_, "mediaadaptor");
            w.write("# ... otherwise interpreted as ASCII\n\n\n");
            writePrefTable(w, beremap_, defberemap_, "remap");
            w.write("\n\n\n");
            writePrefTable(w, pref_, defpref_, "set");
            w.write("\n\n\n");
            w.close();
        } catch (IOException ioe) {
            System.err.println("Couldn't write Preferences: " + ioe);
        }
    }

    /**
	Writes preferences: name-value, commenting out if value is same as default.
	Used for various tables: media adaptor, remap, set.
  */
    void writePrefTable(Writer w, Map cur, Map def, String cmd) throws IOException {
        assert w != null && cur != null && def != null && cmd != null;
        Object[] keys = cur.keySet().toArray();
        Arrays.sort(keys);
        for (int i = 0, imax = keys.length; i < imax; i++) {
            String key = (String) keys[i], val = (String) cur.get(key), defval = (String) def.get(key);
            if (val.equals(defval)) w.write("#");
            String quote = (val.indexOf(' ') == -1 ? "" : "\"");
            w.write(cmd + "\t" + key + "\t" + quote + val + quote + "\n");
        }
    }

    /** Returns {@link multivalent.Browser} with given name.	If no such browser, create new one. */
    public Browser getBrowser(String name) {
        return getBrowser(name, "System");
    }

    /**
	Returns {@link multivalent.Browser} with given name, with passed URL to system behaviors hub.  If no such browser, create new one.
	This, not Browser's constructor, is the way to create new instances.
  */
    public Browser getBrowser(String name, String systemHub) {
        assert systemHub != null;
        if (name != null && !"_NEW".equals(name)) {
            for (int i = 0, imax = browsers_.size(); i < imax; i++) {
                Browser br = (Browser) browsers_.get(i);
                if (name.equals(br.getName())) return br;
            }
        }
        Browser newbr = new Browser(name, systemHub, standalone_);
        browsers_.add(newbr);
        if (standalone_) {
        }
        return newbr;
    }

    public Iterator browsersIterator() {
        return browsers_.iterator();
    }

    /**
	Used by {@link Browser#destroy()} to remove Browser instance from Multivalent's list.
	If remove last Browser, destroy whole application.
  */
    void removeBrowser(Browser br) {
        assert browsers_.indexOf(br) != -1;
        browsers_.remove(br);
        if (browsers_.size() == 0) destroy();
    }

    /** Read system preferences and user preferences, and establish cache. */
    private void readTables() {
        String[] s = ImageIO.getReaderMIMETypes();
        for (int i = 0, imax = s.length; i < imax; i++) adaptor_.put(s[i].toLowerCase(), "RawImage");
        s = ImageIO.getReaderFormatNames();
        for (int i = 0, imax = s.length; i < imax; i++) {
            String key = s[i].toLowerCase();
            if (adaptor_.get(key) == null) adaptor_.put(key, "RawImage");
        }
        if (adaptor_.get("jpeg2000") != null && adaptor_.get("jp2") == null) adaptor_.put("jp2", "RawImage");
        try {
            Map seen = new HashMap(13);
            for (Enumeration e = cl_.getResources("sys/" + FILENAME_PREFERENCES); e.hasMoreElements(); ) {
                URL url = (URL) e.nextElement();
                if (seen.get(url) != null) continue; else seen.put(url, url);
                readPreferences(url.openStream());
            }
        } catch (IOException ioe) {
            System.err.println("startup: " + ioe);
        }
        defadaptor_ = (CHashMap) adaptor_.clone();
        defberemap_ = (HashMap) beremap_.clone();
        defpref_ = (CHashMap) pref_.clone();
        String home = System.getProperty("user.home");
        String username = System.getProperty("user.name");
        if (!home.endsWith(username)) home += File.separatorChar + username;
        home += File.separatorChar + ".Multivalent";
        File homedir = new File(home), userpref = new File(homedir, FILENAME_PREFERENCES);
        if (standalone_) System.out.println("HOME = " + home);
        if (userpref.exists()) try {
            readPreferences(new FileInputStream(userpref));
        } catch (IOException ioe) {
            System.out.println("couldn't read user prefs: " + ioe);
        } else if (!homedir.exists()) homedir.mkdirs();
        String cache = getPreference(PREF_CACHEDIR, System.getProperty("java.io.tmpdir"));
        File cachedir = new File(cache);
        if (!cachedir.exists()) cachedir.mkdirs();
        cache_ = new Cache(home, cache, adaptor_);
    }

    /**
	System shutdown, in this sequence: shuts down all browsers, writes preferences, <code>System.exit(0)</code>.
	Rather than invoking directly, behaviors should send the {@link #MSG_EXIT "EXIT"} semantic event.
  */
    void destroy() {
        for (int i = 0, imax = browsers_.size(); i < imax; i++) ((Browser) browsers_.get(i)).destroy();
        writePreferences();
        File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        String[] ftmp = tmpdir.list();
        for (int i = 0, imax = ftmp.length; i < imax; i++) {
            String n = ftmp[i];
            if ((n.startsWith("font") && n.endsWith(".ttf")) || (n.startsWith("+~JF")) || (n.startsWith("imageio") && n.endsWith(".tmp"))) {
                new File(tmpdir, n).delete();
            }
        }
        System.exit(0);
    }

    /**
	Starts up Multivalent Browser, automatically loading any files given as command line args.

	The command line option <code>-version</code> reports the version and exits.
	(Since the browser is usually started by double clicking an icon, general command line options are not used, in favor of a Preferences file.)
  */
    public static void main(String[] argv) {
        if (argv.length > 0 && argv[0].startsWith("-v")) {
            System.out.println("Multivalent Browser v" + VERSION);
            System.out.println("Home web site: " + HOME_SITE);
            System.exit(0);
        }
        standalone_ = true;
        Multivalent m = getInstance();
        Browser br = m.getBrowser("STARTUP");
        br.eventq(SystemEvents.MSG_GO_HOME, null);
        File pwd = new File(".");
        for (int i = 0, imax = argv.length; i < imax; i++) {
            String v = argv[i];
            URI uri = null;
            File f = new File(pwd, v);
            if (f.canRead()) uri = f.toURI(); else try {
                uri = new URI(v);
            } catch (URISyntaxException e) {
                System.out.println("not a File or URI: " + v);
            }
            if (uri != null) br.eventq(Document.MSG_OPEN, uri);
        }
    }
}
