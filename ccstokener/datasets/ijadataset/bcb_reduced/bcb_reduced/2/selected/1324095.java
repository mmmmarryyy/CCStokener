package eu.fbk.hlt.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

/**
 * System Library
 * @author milen
 *
 */
public class Sys {

    /**
	 * System verbosity level. If increased will show more system messages.
	 */
    public static int verbose_level = 0;

    /**
	 * The name of the variable used to denote the path for an application in the
	 * configuration file.
	 */
    public static final String ROOT_PATH_VAR_NAME = "EDITS_PATH";

    public static void setVerbose(int verbose_level) {
        Sys.verbose_level = verbose_level;
    }

    /**
	 * Plugin types Recognized by the system
	 */
    public enum ModuleType {

        TEXT_ANNOTATOR, DISTANCE_ALGORITHM, COST_SCHEME, ENTAILMENT_ENGINE, RULES_REPOSITORY, EXTRACTOR, FUNCTION_EXECUTOR
    }

    ;

    public static String toString(ModuleType type) {
        return type.toString().toLowerCase().replace("_", "-");
    }

    /**
	 * Returns true if an option with a certain name exists in the arguments
	 * @param name
	 * @param args
	 * @return
	 */
    public static boolean getBooleanOption(String name, String[] args) {
        if (args == null || args.length == 0) return false;
        for (String a : args) {
            if (a.equals("-" + name) || a.equals("--" + name)) return true;
        }
        return false;
    }

    /**
	 * Returns an option with a certain name from the arguments
	 * @param name
	 * @param args
	 * @return
	 */
    public static String getOption(String name, String[] args) {
        if (args == null || args.length == 0) return null;
        for (String a : args) {
            if (a.startsWith("-" + name + "=") || a.startsWith("--" + name + "=")) return a.substring(a.indexOf("=") + 1);
        }
        if (getBooleanOption(name, args)) return "";
        return null;
    }

    /**
	 * Reads the path to a tool from the system variables
	 * @return
	 */
    public static String getSystemPath() {
        String path = System.getenv(ROOT_PATH_VAR_NAME);
        if (path == null) {
            System.err.println(ROOT_PATH_VAR_NAME + " variable not defined!");
            System.exit(1);
        }
        path = path.trim();
        if (path.length() == 0) {
            System.err.println(ROOT_PATH_VAR_NAME + " variable is empty!");
            System.exit(1);
        }
        if (!path.endsWith("/")) path = path + "/";
        File f = new File(path);
        if (!f.exists()) {
            System.err.println(ROOT_PATH_VAR_NAME + "HLT_TOOL_PATH is not a valid path");
            System.exit(1);
        }
        if (!f.exists()) {
            System.err.println(ROOT_PATH_VAR_NAME + "HLT_TOOL_PATH is not a valid path");
            System.exit(1);
        }
        if (!f.isDirectory()) {
            System.err.println(ROOT_PATH_VAR_NAME + "HLT_TOOL_PATH is not a directory");
            System.exit(1);
        }
        return path;
    }

    /**
	 * Loads a readme file
	 * @param filename
	 * @return
	 * @throws FileIOException
	 */
    public static String loadReadme(String filename) {
        try {
            URL url = ClassLoader.getSystemClassLoader().getResource(filename);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
            String line = null;
            StringBuilder vud = new StringBuilder();
            while ((line = in.readLine()) != null) {
                vud.append(line + "\n");
            }
            in.close();
            return vud.toString();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    /**
	 * Converts a stack trace into a string.
	 * @param aThrowable
	 * @return
	 */
    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }

    /**
	 * Prints a system message if the level is more or equals to the verbosity level
	 * @param str
	 * @param level
	 */
    public static void println(String str, int level) {
        if (level > verbose_level) return;
        System.out.println(str);
    }

    /**
	 * Prints a system message if the level is more or equals to the verbosity level
	 * @param str
	 * @param level
	 */
    public static void print(String str, int level) {
        if (level > verbose_level) return;
        System.out.print(str);
    }
}
