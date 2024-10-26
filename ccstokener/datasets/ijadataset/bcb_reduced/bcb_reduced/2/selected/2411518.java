package console.commando;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.util.StringList;

/**
 * An EditAction which is intended to be used in the Console Commando.
 * Associated with an .xml file which may be inside a jar, or may be in the
 * user dir.
 * 
 */
public class CommandoCommand extends EditAction {

    /**
	 * 
	 * @return true if userdefined
	 */
    public boolean isUser() {
        return (url == null);
    }

    /**
	 * 
	 * @return true for user commands that override a command 
	 *     with the same name in the jar. 
	 */
    public boolean isOverriding() {
        if (!isUser()) return false;
        String defaultCommands = jEdit.getProperty("commando.default");
        StringList sl = StringList.split(defaultCommands, " ");
        String cmdName = name.replace("commando.", "");
        return sl.contains(cmdName);
    }

    public static CommandoCommand create(URL url) {
        String l = shortLabel(url.getPath());
        CommandoCommand retval = new CommandoCommand(l, url.getPath());
        retval.url = url;
        return retval;
    }

    public static CommandoCommand create(String path) {
        String l = shortLabel(path);
        File f = new File(path);
        if (f.canRead()) {
            return new CommandoCommand(l, path);
        } else throw new RuntimeException("path: " + path + " abs: " + f.getAbsolutePath());
    }

    /**
	 * @return the short label - for button text
	 */
    public String getShortLabel() {
        return label;
    }

    /**
	 * @param path
	 *                an absolute path to a resource
	 * @return the short label on for a button text
	 */
    static String shortLabel(String path) {
        Matcher m = p.matcher(path);
        m.find();
        String name = m.group(1);
        name = name.replace('_', ' ');
        return name;
    }

    private CommandoCommand(String shortLabel, String path) {
        super("commando." + shortLabel);
        label = shortLabel;
        this.path = path;
        this.propertyPrefix = getName() + '.';
        jEdit.setTemporaryProperty(getName() + ".label", label);
    }

    public String getPropertyPrefix() {
        return propertyPrefix;
    }

    public void invoke(View view) {
        new CommandoDialog(view, getName());
    }

    public String getCode() {
        return "new console.commando.CommandoDialog(view,\"" + getName() + "\");";
    }

    protected Reader openStream() throws IOException {
        if (url != null) {
            return new BufferedReader(new InputStreamReader(url.openStream()));
        } else {
            return new BufferedReader(new FileReader(path));
        }
    }

    private URL url = null;

    private String label;

    private String path;

    private String propertyPrefix;

    private static final String pattern = "([^\\\\\\./]+)\\.xml$";

    private static final Pattern p = Pattern.compile(pattern);
}
