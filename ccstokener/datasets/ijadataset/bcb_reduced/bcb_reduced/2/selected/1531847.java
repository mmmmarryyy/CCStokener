package dscript;

import java.io.*;
import java.net.URL;
import dscript.preprocess.LineSplit;

public class ConsoleDusty extends Thread implements Killable {

    static {
        ActionHashIterator.USE_BEST_MATCH = false;
    }

    public static boolean using_alt_lang = false;

    public static String urlpath = "";

    private static String nun = ":NO_COMMAND:";

    private static String[] altlang = new String[] { nun, nun, nun, nun, nun, nun, nun, nun, nun, nun, nun, nun, nun, nun, nun, nun, nun, nun, nun, nun, nun };

    private static String FS = "";

    static {
        if (!DApplet.using_as_applet) {
            FS = System.getProperty("file.separator");
        }
    }

    private VarContainer vc;

    private ActionContainer ac;

    private ThingTypeContainer ttc;

    private boolean QUIET = false;

    private Output OUT;

    private InputStream IN;

    private boolean close_on_exit = true;

    private boolean running = false;

    private static final String BENCHMARK_COMMAND = ":benchmark";

    private static final String HELP_MESSAGE = new String("\nType in your line of code and hit enter to run it..\nType:\n :q to exit\n :r to reset variables\n :quiet to toggle quiet true/false\n :l to list variables\n :code to list source-code entered\n :save>{filename} to save\n :load>{filename} to load\n :run to run source in memory\n :clear to clear source and vars\n :? to list these instructions");

    private static final String FEEDBACK_MESSAGE = new String("We would greatly appreciate your feedback,\nplease email dustyscript-devel@lists.sourceforge.net");

    private String langstring = "";

    public ConsoleDusty() {
        try {
            OUT = new Output();
            IN = System.in;
        } catch (Exception e) {
            System.err.println("in ConsoleDusty() ->failed at creating streams! Cannot continue");
            System.exit(1);
        }
        ttc = new ThingTypeContainer(OUT, IN);
        ac = new ActionContainer(OUT);
        vc = new VarContainer(OUT);
        close_on_exit = true;
    }

    public ConsoleDusty(Output out, InputStream i) {
        OUT = out;
        try {
            ConsolePipeController cpc = new ConsolePipeController(i, 200);
            IN = cpc.getInputStream();
            cpc.start();
        } catch (Exception eek) {
            IN = i;
        }
        ttc = new ThingTypeContainer(OUT, IN);
        ac = new ActionContainer(OUT);
        vc = new VarContainer(OUT);
        close_on_exit = true;
    }

    public ConsoleDusty(Output out, InputStream i, boolean coe) {
        OUT = out;
        try {
            ConsolePipeController cpc = new ConsolePipeController(i, 200);
            IN = cpc.getInputStream();
            cpc.start();
        } catch (Exception eek) {
            IN = i;
        }
        ttc = new ThingTypeContainer(OUT, IN);
        ac = new ActionContainer(OUT);
        vc = new VarContainer(OUT);
        close_on_exit = coe;
    }

    public void kill() {
        running = false;
    }

    public ConsoleDusty setQuiet(boolean b) {
        QUIET = b;
        return this;
    }

    public void run() {
        if (!using_alt_lang) {
            OUT.println("Welcome to Dustyscript!\n" + HELP_MESSAGE + "\n\n" + FEEDBACK_MESSAGE);
        } else {
            OUT.println(altlang[0] + '\n' + altlang[1] + "\n\n" + altlang[2]);
        }
        String source = "";
        running = true;
        while (running) {
            OUT.print("\ndustycommand>>");
            String s = Ask.ask(IN);
            s.trim();
            while (!s.endsWith(";") && s.endsWith(" ")) {
                s = s.substring(0, s.length() - 1);
            }
            if (s.equals(":q")) {
                break;
            }
            if (s.equals(":r")) {
                vc = new VarContainer(OUT);
                ac = new ActionContainer(OUT);
                if (!using_alt_lang) {
                    OUT.println("->reset variables");
                } else {
                    OUT.println("->" + altlang[4]);
                }
                continue;
            }
            if (s.equals(":l")) {
                vc.dump();
                ac.dump();
                if (!using_alt_lang) {
                    OUT.println("->list.");
                } else {
                    OUT.println("->" + altlang[5]);
                }
                continue;
            }
            if (s.equals(":code") || s.equals(altlang[6])) {
                if (!using_alt_lang) {
                    OUT.println(source + "->source");
                } else {
                    OUT.println(source + "->" + altlang[7]);
                }
                continue;
            }
            if (s.equals(":?")) {
                if (!using_alt_lang) {
                    OUT.println(HELP_MESSAGE);
                } else {
                    OUT.println(altlang[1]);
                }
                continue;
            }
            if (s.equals(":clear") || s.equals(altlang[8])) {
                source = "";
                vc = new VarContainer(OUT);
                ac = new ActionContainer(OUT);
                ttc = new ThingTypeContainer(OUT, IN);
                Use.clear();
                if (!using_alt_lang) {
                    OUT.println("->reset source and variables");
                } else {
                    OUT.println("->" + altlang[4]);
                }
                continue;
            }
            if (s.equals(":quiet") || s.equals(altlang[9])) {
                QUIET = !QUIET;
                if (!using_alt_lang) {
                    OUT.println("Toggled 'quiet' to " + QUIET);
                } else {
                    OUT.println(altlang[10] + QUIET);
                }
                continue;
            }
            if (s.equals(BENCHMARK_COMMAND) || s.equals(altlang[11])) {
                StatementProcessor.BENCHMARK = !StatementProcessor.BENCHMARK;
                if (!using_alt_lang) {
                    OUT.println("Toggled 'benchmark' to " + StatementProcessor.BENCHMARK);
                } else {
                    OUT.println(altlang[12] + StatementProcessor.BENCHMARK);
                }
                continue;
            }
            if (s.startsWith(":lang>")) {
                if (s.equals(":lang>")) {
                    continue;
                }
                if (s.equals(":lang>ENGLISH")) {
                    langstring = "";
                    using_alt_lang = false;
                    OUT.println("Using language 'english'");
                    continue;
                }
                String lngs = s.substring(6, s.length());
                constructLanguage(lngs, OUT);
                langstring = "/*LANG:" + lngs + "*/\n";
                if (!using_alt_lang) {
                    OUT.println("Using language '" + lngs + "'");
                } else {
                    OUT.println(altlang[13] + " '" + lngs + "'");
                }
                continue;
            }
            if (s.startsWith(":save>") || s.startsWith(altlang[14])) {
                String filename = s.substring(6, s.length());
                save(filename, source);
                continue;
            }
            if (s.startsWith(":load>") || s.startsWith(altlang[15])) {
                String filename = s.substring(6, s.length());
                source = load(filename, OUT);
                continue;
            }
            if (s.equals(":run") || s.equals(altlang[16])) {
                vc = new VarContainer(OUT);
                ac = new ActionContainer(OUT);
                ttc = new ThingTypeContainer(OUT, IN);
                if (!using_alt_lang) {
                    OUT.println("Running:\n" + source + "\n");
                } else {
                    OUT.println(altlang[17] + ":\n" + source + "\n");
                }
                process(langstring + source);
                continue;
            }
            if (s.endsWith(";") == false) {
                if (!using_alt_lang) {
                    OUT.println("->line must end in with a ';'");
                } else {
                    OUT.println("->" + altlang[18]);
                }
                continue;
            }
            OUT.println("");
            source = source + s + "\n";
            process(langstring + s);
            s = "";
        }
        String outmssg = "";
        if (!using_alt_lang) {
            outmssg = "\n\nThank you for using Dustyscript! \n " + StatementProcessor.VERSION + "\n~goodbye\n\n" + FEEDBACK_MESSAGE + "\n";
        } else {
            outmssg = "\n\n" + altlang[19] + " \n " + StatementProcessor.VERSION + "\n~" + altlang[20] + "\n\n" + altlang[2] + "\n";
        }
        System.err.println(outmssg);
        if (DApplet.using_as_applet) {
            OUT.println(outmssg);
        }
        if (close_on_exit && !DApplet.using_as_applet) {
            System.exit(0);
        }
    }

    public void save(String file, String src) {
        if (DApplet.using_as_applet) {
            OUT.println("Save has been disabled for web.");
            return;
        }
        String tosave = "";
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '\n') {
                if (i == 0) {
                    tosave = tosave + c;
                    continue;
                }
                if (src.charAt((i - 1)) != '\r') {
                    tosave = tosave + "\r\n";
                } else {
                    tosave = tosave + "\n";
                }
                continue;
            }
            tosave = tosave + c;
        }
        file = System.getProperty("user.dir") + FS + "users" + FS + file;
        try {
            FileWriter fw = new FileWriter(new File(file));
            fw.write(tosave);
            fw.close();
            if (!QUIET) {
                OUT.println("->saved " + file + "!");
            }
        } catch (Exception e) {
            OUT.println("->save failed for " + file, 1);
        }
    }

    private static String appletLoad(String file, Output OUT) {
        if (!urlpath.endsWith("/")) {
            urlpath += '/';
        }
        if (!urlpath.startsWith("http://")) {
            urlpath = "http://" + urlpath;
        }
        String url = "";
        if (file.equals("languages.txt")) {
            url = urlpath + file;
        } else {
            url = urlpath + "users/" + file;
        }
        try {
            StringBuffer sb = new StringBuffer(2000);
            BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            String a;
            while ((a = br.readLine()) != null) {
                sb.append(a).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            OUT.println("load failed for file->" + file);
        }
        return "";
    }

    public static String load(String file, Output OUT) {
        if (DApplet.using_as_applet) {
            return appletLoad(file, OUT);
        }
        if (file.equals("languages.txt")) {
            file = System.getProperty("user.dir") + FS + file;
        } else {
            file = System.getProperty("user.dir") + FS + "users" + FS + file;
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String src = "";
            String s;
            while ((s = br.readLine()) != null) {
                src = src + s + "\n";
            }
            return src;
        } catch (Exception e) {
            OUT.println("->load failed for " + file, 1);
        }
        return "";
    }

    public void process(String source) {
        StatementProcessor sp = new StatementProcessor(source, vc, ac, ttc, OUT, IN);
        sp.suppress();
        sp.setUserDebug(!QUIET);
        try {
            sp.run();
        } catch (Exception e) {
            OUT.println("Error external to dustyscript.");
            OUT.println("What follows is called a 'stack trace'!");
            OUT.println(e.toString());
            e.printStackTrace();
        }
    }

    public static void constructLanguage(String lang, Output OUT) {
        String data = "";
        try {
            data = load("languages.txt", OUT);
            LineSplit line = new LineSplit();
            data = line.ssplit(lang, data);
            if (data.equals("")) {
                return;
            }
            for (int i = 0; i <= 20; i++) {
                fillArray(i, line, data);
            }
        } catch (Exception e) {
            OUT.println("perhaps the author of this language plugin has not provided Console bindings (error)");
        }
    }

    public static void fillArray(int index, LineSplit line, String data) {
        String s = line.ssplit("" + index, data);
        if (!s.equals("")) {
            try {
                altlang[index] = s;
            } catch (Exception e) {
            }
        }
    }

    public static void main(String[] args) {
        boolean quiet = false;
        if (args.length > 0 && args[0].equals("-quiet")) {
            quiet = true;
        }
        new ConsoleDusty().setQuiet(quiet).run();
    }
}
