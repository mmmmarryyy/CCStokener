package dscript;

import java.io.*;
import java.net.URL;
import java.util.HashMap;

public class Use {

    public static String urlpath = "";

    private static HashMap hm = new HashMap();

    public static String base = "";

    public static String fs = "";

    public static boolean using_as_applet;

    static {
        if (!DApplet.using_as_applet) {
            base = System.getProperty("user.dir");
            fs = System.getProperty("file.separator");
        }
    }

    public static String usrspath = base + fs + "users" + fs;

    public static String libspath = base + fs + "libraries" + fs;

    private static final String[] ss = new String[0];

    public static void clear() {
        hm.clear();
    }

    public static boolean exec(String fname, VarContainer vc, ActionContainer ac, ThingTypeContainer ttc, Output OUT, InputStream IN, boolean AT) {
        return exec(fname, vc, ac, ttc, OUT, IN, AT, null, ss);
    }

    public static boolean exec(String fname, VarContainer vc, ActionContainer ac, ThingTypeContainer ttc, Output OUT, InputStream IN, boolean AT, Statement state, String[] arggies) {
        if (hm.containsKey(fname)) {
            return true;
        }
        hm.put(fname, null);
        using_as_applet = DApplet.using_as_applet;
        if (using_as_applet) {
            boolean ea = exec_applet(fname, vc, ac, ttc, OUT, IN, AT, state, arggies);
            if (!ea) {
                hm.remove(fname);
            }
            return ea;
        }
        StringBuffer src = new StringBuffer(2400);
        String s;
        String userfile = "";
        if (fname.startsWith("dusty_")) {
            userfile = libspath + fname;
        } else {
            userfile = usrspath + fname;
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(userfile)));
            while ((s = br.readLine()) != null) {
                src.append(s);
                src.append('\n');
            }
            br.close();
        } catch (Exception e) {
            OUT.println(new DSOut(DSOut.ERR_OUT, -1, "Dustyscript failed at reading the file'" + fname + "'\n\t...for 'use' statement"), vc, AT);
            hm.remove(fname);
            return false;
        }
        fork(src, vc, ac, ttc, OUT, IN, AT, state, arggies);
        return true;
    }

    public static void fork(StringBuffer src, VarContainer vc, ActionContainer ac, ThingTypeContainer ttc, Output OUT, InputStream IN, boolean AT, Statement state, String[] arggies) {
        StatementProcessor sp = new StatementProcessor(src.toString(), vc, ac, ttc, OUT, IN);
        if (state != null) {
            sp.processArgArray(arggies, state);
        } else {
            sp.processArgArray(arggies);
        }
        sp.suppress();
        sp.setAttempting(AT);
        sp.run();
    }

    public static boolean exec_applet(String fname, VarContainer vc, ActionContainer ac, ThingTypeContainer ttc, Output OUT, InputStream IN, boolean AT, Statement state, String[] arggies) {
        if (!urlpath.endsWith("/")) {
            urlpath = urlpath + '/';
        }
        if (!urlpath.startsWith("http://")) {
            urlpath = "http://" + urlpath;
        }
        String url = urlpath;
        if (fname.startsWith("dusty_")) {
            url = url + "libraries/" + fname;
        } else {
            url = url + "users/" + fname;
        }
        StringBuffer src = new StringBuffer(2400);
        try {
            String s;
            BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            while ((s = br.readLine()) != null) {
                src.append(s).append('\n');
            }
            br.close();
        } catch (Exception e) {
            OUT.println(new DSOut(DSOut.ERR_OUT, -1, "Dustyscript failed at reading the file'" + fname + "'\n\t...for 'use' statement"), vc, AT);
            return false;
        }
        fork(src, vc, ac, ttc, OUT, IN, AT, state, arggies);
        return true;
    }
}
