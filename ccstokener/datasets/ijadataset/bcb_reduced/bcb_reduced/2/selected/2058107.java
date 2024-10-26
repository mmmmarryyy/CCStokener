package net.sourceforge.mpcreader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author java
 */
public class MpcReader {

    private static final int BAR = 30;

    private BufferedReader input_source = null;

    private BufferedWriter output_file = null;

    private File tmpfs = null;

    private CmdData cmd = new CmdData();

    private String types[] = null;

    private String output_files[] = null;

    private String input_files[] = null;

    private void cleanup() {
        try {
            if (output_file != null) {
                output_file.close();
            }
            if (input_source != null) {
                input_source.close();
            }
        } catch (IOException ex) {
        }
        if (!cmd.isStoreOnly()) {
            if (tmpfs != null) {
                tmpfs.delete();
            }
        }
    }

    private void exit(String msg) {
        error(msg + CmdData.ERR_TERM);
        cleanup();
        System.exit(0);
    }

    /**
     * prepare MPC data
     * copy file from URL/file to temporary file
     */
    private void prepare() {
        if (cmd.isStoreOnly()) {
            if (!cmd.isMerger() || cmd.isSplit()) {
                tmpfs = new File(cmd.getOutput());
            }
        } else {
            try {
                tmpfs = File.createTempFile("mpc_", ".dat");
            } catch (IOException ex) {
                exit(CmdData.ERR_INPUT_CREATE + tmpfs.getPath());
            }
        }
        if (cmd.fromUrl()) {
            readURL();
        } else {
            readFile();
        }
    }

    /**
     * print text to screen with new line
     * @param txt
     */
    private void out(String txt) {
        out(txt, true);
    }

    /**
     * print text to screen
     * @param txt
     * @param new_line
     */
    private void out(String txt, boolean new_line) {
        if (cmd.isVerbose()) {
            if (new_line) {
                System.out.println(txt);
            } else {
                System.out.print(txt);
            }
        }
    }

    /**
     * print error to screen
     * @param txt
     */
    private void error(String txt) {
        System.out.println("\n---\n" + txt + "\n---\n");
    }

    /**
     * command line help output
     */
    private void cmdLine() {
        out("\nUsage: java -jar mpcreader [options]");
        out("--type:comet       type of MPC data");
        out("                   usage: number or short version eg. '--type:2' or '--type:critical'");
        out("                   posible types are:");
        String mpcn[] = cmd.getMpcTypeNames();
        for (int i = 0; i < mpcn.length; i++) {
            out("                   " + mpcn[i]);
        }
        out("                   or a comma separated list e.g '--type:comet,bright,distant'");
        out("                   minor planet data will be merged to one single file (see '--output')");
        out("                   default: '--type:comet' ('--type:0')\n");
        out("--soft:kstars      download MPC data for this software.");
        out("                   usage: number or long version eg. --soft:2' or '--soft:guide'");
        out("                   posible types are:");
        mpcn = cmd.getMpcSoftTypeNames();
        for (int i = 0; i < mpcn.length; i++) {
            out("                   " + mpcn[i]);
        }
        out("                   data is only converted for kstars(A). for all other software packages the data is already converted.");
        out("                   default: '--soft:kstars' (converts MPC data for kstars)\n");
        out("--input:PATH2FILE  input filename in MPC format (http://www.cfa.harvard.edu/iau/Ephemerides/Soft00.html)");
        out("                   if '--type' is a comma separated list, '--input' must be a list too or the conversion will fail");
        out("                   if '--type' is 'all', '--input' must be a list with 'comet,bright,critical,distance,unusual'-data files");
        out("                   e. g. '--type:comet,bright,distant --input:comets.dat,bright.dat,distant.dat'");
        out("                   default: none\n");
        out("--url:URL2FILE     url to webaddress in MPC format (see http://www.cfa.harvard.edu/iau/Ephemerides/Soft00.html)");
        out("                   if '--input' and '--url' is used data from '--input' is taken");
        out("                   overrides --dlbase");
        out("                   default: '--url:http://www.minorplanetcenter.org/iau/Ephemerides'\n");
        out("--dlbase:minor     select standard download url.");
        out("                   usage: number or long version eg. --dlbase:0' or '--dlbase:mpc'");
        out("                   posible types are:");
        out("                   0 mpc       http://www.minorplanetcenter.org/iau/Ephemerides");
        out("                   1 harvard   http://www.cfa.harvard.edu/iau/Ephemerides");
        out("                   default: '--dlbase:0' ('dlbase:minor') (\n");
        out("--output:PATH2FILE output filename");
        out("                   if '--type:all' or '--soft:kstarsA' is used, a comma separated list should be given");
        out("                   the first parameter is always the comet file, the second is used for the asteroid data");
        out("                   e. g. '--output:comets.dat,allasteroids.dat'");
        out("                   default: 'asteroids.dat' or 'comets.dat' for multiple input and '--soft:kstarsA'");
        out("                   otherwise same filename as internet data file\n");
        out("--split:false      do not merge files of minor planets [false|true]");
        out("                   default: '--split:false'\n");
        out("--kstarsonly:false shortcut for --type:all --output:~/.kde4/share/apps/kstars/comets.dat,~/.kde4/share/apps/kstars/asteroids.dat");
        out("                   default: '--kstarsonly:false'\n");
        out("--verbose:true     verbose output [false|true].");
        out("                   default: '--verbose:true'");
        out("--help             this screen\n");
        out("\n");
        System.exit(0);
    }

    /**
     * fetch command line argument and return selcted option
     * @param arg
     * @param cmd
     * @return
     */
    private String fetchCmd(String[] arg, String cmd) {
        if (arg == null || arg.length == 0) {
            return null;
        }
        for (int i = 0; i < arg.length; i++) {
            if (arg[i].indexOf(cmd) != -1) {
                if (arg[i].equalsIgnoreCase("--help")) {
                    return "help";
                }
                if (arg[i].equalsIgnoreCase("--hint")) {
                    return "hint";
                }
                String[] s = arg[i].split(":");
                if (s.length < 2) {
                    return null;
                }
                out(s[0] + ":" + s[1] + " ", false);
                return s[1].trim();
            }
        }
        return null;
    }

    /**
     * parse command line for arguments and store options in command class
     * @param arg
     */
    private void parseCmdLine(String[] arg) {
        out("\njava -jar mpcreader ", false);
        String help = fetchCmd(arg, "help");
        if (help != null) {
            cmdLine();
            return;
        }
        String verbose = fetchCmd(arg, "verbose");
        if (verbose != null) {
            if (verbose.equalsIgnoreCase("false") || verbose.equalsIgnoreCase("no") || verbose.equalsIgnoreCase("off")) {
                cmd.setVerbose(false);
            }
        }
        String split = fetchCmd(arg, "split");
        if (split != null) {
            if (split.equalsIgnoreCase("true") || split.equalsIgnoreCase("yes") || split.equalsIgnoreCase("on")) {
                cmd.setSplit(true);
            }
        }
        String c = fetchCmd(arg, "type");
        if (c == null) {
            c = "comet";
        }
        if (c.equalsIgnoreCase("all") || c.equalsIgnoreCase("5")) {
            c = "comet,bright,critical,distant,unusual";
        }
        types = c.split(",");
        if (types.length == 0) {
            types = new String[] { c };
        }
        String url_sel = fetchCmd(arg, "dlbase");
        if (url_sel != null) {
            if (url_sel.equalsIgnoreCase("harvard") || url_sel.equalsIgnoreCase("1")) {
                cmd.setUrl(CmdData.URLSEL_HARVARD);
            } else {
                cmd.setUrl(CmdData.URLSEL_MPC);
            }
        }
        String input_url = fetchCmd(arg, "url");
        if (input_url != null) {
            try {
                if (!input_url.startsWith("http://")) {
                    input_url = "http://" + input_url;
                }
                URL u = new URL(input_url);
                cmd.setUrl(input_url, false);
            } catch (MalformedURLException ex) {
                exit(CmdData.ERR_INPUT_URL + input_url);
            }
        }
        String input = fetchCmd(arg, "input");
        if (input != null) {
            input_files = input.split(",");
            if (input_files.length == 0) {
                input_files = new String[] { input };
            }
        }
        String output = fetchCmd(arg, "output");
        if (output_files == null && !cmd.isSplit() && types.length > 1) {
            output_files = new String[] { CmdData.DEF_KSTARS_COMET_FILE, CmdData.DEF_KSTARS_MINOR_FILE };
        }
        if (output != null) {
            output_files = output.split(",");
            if (output_files.length == 1) {
                if (types.length > 1) {
                    output_files = new String[] { output, CmdData.DEF_KSTARS_MINOR_FILE };
                }
            }
        }
        String s = fetchCmd(arg, "soft");
        if (s == null) {
            s = "kstars";
        }
        if (s.equalsIgnoreCase("mpc") || s.equalsIgnoreCase("0")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_MPC);
        } else if (s.equalsIgnoreCase("skymap") || s.equalsIgnoreCase("1")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_SKYMAP);
        } else if (s.equalsIgnoreCase("guide") || s.equalsIgnoreCase("2")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_GUIDE);
        } else if (s.equalsIgnoreCase("xephem") || s.equalsIgnoreCase("3")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_XEPHEM);
        } else if (s.equalsIgnoreCase("home") || s.equalsIgnoreCase("4")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_MYSTARS);
        } else if (s.equalsIgnoreCase("mystars") || s.equalsIgnoreCase("5")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_MYSTARS);
        } else if (s.equalsIgnoreCase("thesky") || s.equalsIgnoreCase("6")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_THESKY);
        } else if (s.equalsIgnoreCase("starry") || s.equalsIgnoreCase("7")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_STARRYNIGHT);
        } else if (s.equalsIgnoreCase("deep") || s.equalsIgnoreCase("8")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_DEEPSPACE);
        } else if (s.equalsIgnoreCase("pctcs") || s.equalsIgnoreCase("9")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_PCTCS);
        } else if (s.equalsIgnoreCase("ecu") || s.equalsIgnoreCase("10")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_ECU);
        } else if (s.equalsIgnoreCase("dotp") || s.equalsIgnoreCase("11")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_DOTP);
        } else if (s.equalsIgnoreCase("mega") || s.equalsIgnoreCase("12")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_MEGASTAR);
        } else if (s.equalsIgnoreCase("skychart") || s.equalsIgnoreCase("13")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_SKYCHART);
        } else if (s.equalsIgnoreCase("voyager") || s.equalsIgnoreCase("14")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_VOYAGER);
        } else if (s.equalsIgnoreCase("skytools") || s.equalsIgnoreCase("15")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_SKYTOOLS);
        } else if (s.equalsIgnoreCase("autostar") || s.equalsIgnoreCase("16")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_AUTOSTAR);
        } else if (s.equalsIgnoreCase("kstars") || s.equalsIgnoreCase("17")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_KSTARS);
        } else if (s.equalsIgnoreCase("kstarsA") || s.equalsIgnoreCase("18")) {
            cmd.setSoftType(CmdData.SOFT_TYPE_KSTARS);
            types = "comet,bright,critical,distant,unusual".split(",");
            output_files = new String[] { CmdData.DEF_KSTARS_COMET_FILE, CmdData.DEF_KSTARS_MINOR_FILE };
        }
        String kstars_only = fetchCmd(arg, "kstarsonly");
        if (kstars_only != null) {
            if (kstars_only.equalsIgnoreCase("true") || kstars_only.equalsIgnoreCase("yes") || kstars_only.equalsIgnoreCase("on")) {
                cmd.setKStarsOnly(true);
                types = "comet,bright,critical,distant,unusual".split(",");
                output_files = new String[] { CmdData.KSTARS_DIR + File.separator + CmdData.DEF_KSTARS_COMET_FILE, CmdData.KSTARS_DIR + File.separator + CmdData.DEF_KSTARS_MINOR_FILE };
            }
        }
        String hint = fetchCmd(arg, "hint");
        if (hint != null) {
            out("\nHINT: type 'java -jar mpcreader --help' for command line options");
        }
        out("");
    }

    private void setType(String c) {
        if (c.equalsIgnoreCase("comet") || c.equalsIgnoreCase("0")) {
            cmd.setMpcType(CmdData.TYPE_COMET);
        } else if (c.equalsIgnoreCase("bright") || c.equalsIgnoreCase("1")) {
            cmd.setMpcType(CmdData.TYPE_MINOR_BRIGHT);
        } else if (c.equalsIgnoreCase("critical") || c.equalsIgnoreCase("2")) {
            cmd.setMpcType(CmdData.TYPE_MINOR_CRITICAL);
        } else if (c.equalsIgnoreCase("distant") || c.equalsIgnoreCase("3")) {
            cmd.setMpcType(CmdData.TYPE_MINOR_DISTANT);
        } else if (c.equalsIgnoreCase("unusual") || c.equalsIgnoreCase("4")) {
            cmd.setMpcType(CmdData.TYPE_MINOR_UNUSUAL);
        }
    }

    private void setOutputFile(int index_to_file) {
        if (output_files != null && !cmd.isSplit() && cmd.getMpcType() != CmdData.TYPE_COMET && output_files.length > 1) {
            cmd.setOutput(output_files[1]);
        } else if (output_files != null && cmd.getMpcType() == CmdData.TYPE_COMET) {
            cmd.setOutput(output_files[0]);
        } else {
            if (output_files != null && output_files.length > index_to_file) {
                cmd.setOutput(output_files[index_to_file]);
            } else {
                cmd.setOutput(null);
            }
        }
    }

    private void setInputFile(String c) {
        cmd.setInputFile(new File(c));
    }

    /**
     * read MPC data from URL to temporaryfile
     */
    private void readURL() {
        String url = cmd.getURLString(null);
        out("\nreading from URL:\n" + url + "\nto file\n" + tmpfs.getPath());
        URL mpc_url = null;
        try {
            mpc_url = new URL(url);
        } catch (MalformedURLException ex) {
            exit(CmdData.ERR_INPUT_URL + url);
        }
        long apx_len = 0;
        try {
            InputStream is = mpc_url.openStream();
            apx_len = is.available();
            input_source = new BufferedReader(new InputStreamReader(is));
        } catch (IOException ex) {
            exit(CmdData.ERR_INPUT_WEB + url);
        }
        copyData(input_source, tmpfs, apx_len, url);
        try {
            input_source.close();
        } catch (IOException ex) {
        }
        out("\nreading from URL done");
    }

    /**
     * read MPC data from file to temporary file
     */
    private void readFile() {
        File file = cmd.getInputFile();
        out("\nreading from file:\n" + file.getPath() + "\nto file\n" + tmpfs.getPath());
        try {
            input_source = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException ex) {
            exit(CmdData.ERR_INPUT_OPEN + file.getPath());
        }
        copyData(input_source, tmpfs, file.length(), file.getPath());
        try {
            input_source.close();
        } catch (IOException ex) {
        }
        out("\nreading from file done");
    }

    /**
     * copy data from input reader to file
     * @param in
     * @param out_file
     * @param inp
     */
    private void copyData(BufferedReader in, File out_file, long apx_len, String inp) {
        try {
            output_file = new BufferedWriter(new FileWriter(out_file, cmd.isMerger()));
        } catch (IOException ex) {
            exit(CmdData.ERR_OUTPUT_OPEN + out_file.getPath());
        }
        long a = 0;
        long x = apx_len / BAR;
        boolean verbose = cmd.isVerbose();
        Thread t = null;
        Bar b = null;
        if (verbose) {
            b = new Bar();
            t = new Thread(b);
            t.start();
        }
        while (true) {
            int c = 0;
            try {
                c = in.read();
                if (c == -1) {
                    break;
                }
            } catch (IOException ex) {
                exit(CmdData.ERR_INPUT_READ + inp);
            }
            try {
                output_file.write(c);
            } catch (IOException ex) {
                exit(CmdData.ERR_OUTPUT_WRITE + out_file);
            }
            if (verbose) {
                a++;
                if (a % x == 0) {
                    b.write();
                }
            }
        }
        out("\n", false);
        if (b != null) {
            b.stop();
        }
        try {
            output_file.close();
        } catch (IOException ex) {
        }
    }

    /**
     * convert MPC data from temp-file created in readURL() or readFile()
     */
    private void convert() {
        if (cmd.isStoreOnly()) {
            return;
        }
        File file = new File(cmd.getOutput());
        if (!cmd.isMerger() || cmd.isSplit()) {
            try {
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
            } catch (IOException ex) {
                exit(CmdData.ERR_INPUT_CREATE + file.getPath());
            }
        }
        BufferedReader input = null;
        try {
            input = new BufferedReader(new FileReader(tmpfs));
        } catch (FileNotFoundException ex) {
            exit(CmdData.ERR_INPUT_OPEN + tmpfs.getPath());
        }
        BufferedWriter bw_mpc = null;
        try {
            bw_mpc = new BufferedWriter(new FileWriter(file, cmd.isMerger()));
        } catch (IOException ex) {
            exit(CmdData.ERR_OUTPUT_OPEN + file.getPath());
        }
        out("\nstoring to file: " + file.getPath() + "\n");
        MPC mpc = cmd.getMpcConverter();
        while (true) {
            String l = null;
            String ccv = null;
            try {
                l = input.readLine();
                if (l == null) {
                    break;
                }
                if (cmd.ignore(l)) {
                    error(CmdData.ERR_INPUT_IGNORE + "\n" + l);
                    continue;
                }
                ccv = mpc.format(l);
                if (ccv == null) {
                    error(CmdData.ERR_INPUT_READ_LINE + "\n" + l);
                    continue;
                }
                out(ccv);
            } catch (Exception ex) {
                error(CmdData.ERR_INPUT_READ_LINE + "\n" + l);
                continue;
            }
            try {
                bw_mpc.write(ccv);
            } catch (IOException ex) {
                error(CmdData.ERR_OUTPUT_WRITE_LINE + "\n" + ccv);
            }
            try {
                bw_mpc.newLine();
            } catch (IOException ex) {
                error("Could not writenew line.");
            }
        }
        try {
            bw_mpc.close();
            input.close();
        } catch (IOException ex) {
        }
        out("\nstoring to file done");
    }

    /**
     * main loop
     * @param arg
     */
    public void process(String[] arg) {
        if (arg.length == 0) {
            arg = new String[] { "--type:0", "--soft:kstars", "--hint:true" };
        }
        parseCmdLine(arg);
        if (input_files != null && input_files.length != types.length) {
            exit(CmdData.ERR_TYPES_INPUTFILE);
        }
        boolean minors = false;
        for (int i = 0; i < types.length; i++) {
            setType(types[i]);
            if (cmd.getMpcType() != CmdData.TYPE_COMET) {
                minors = true;
            }
            if (input_files != null) {
                setInputFile(input_files[i]);
            }
            setOutputFile(i);
            prepare();
            convert();
            cleanup();
            if (minors) {
                cmd.setMerger(true);
            }
            out("");
        }
    }

    public static void main(String[] arg) {
        MpcReader p = new MpcReader();
        p.process(arg);
        System.exit(1);
    }

    /**
     * quick and dirty bar thread
     */
    class Bar implements Runnable {

        private int write = 0;

        private boolean run = true;

        public void write() {
            this.write++;
        }

        public void stop() {
            this.run = false;
        }

        public void run() {
            while (run) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException ex) {
                    return;
                }
                if (!run) {
                    return;
                }
                while (write-- > 0) {
                    out("+", false);
                }
            }
        }
    }
}
