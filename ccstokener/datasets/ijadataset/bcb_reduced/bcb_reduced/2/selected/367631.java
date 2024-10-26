package fr.imag.adele.escoffier.script.command;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.shell.Command;
import org.apache.felix.shell.ShellService;
import org.osgi.framework.BundleContext;
import fr.imag.adele.escoffier.script.util.SubstituteUtility;
import fr.imag.adele.escoffier.shell.ShellContext;

/**
 * @author Clement Escoffier <clement.escoffier@gmail.com>
 * 
 */
@Component(immediate = true)
@Provides
public class RunCmdImpl implements Command {

    private static final String HELP_OPTION = "-help";

    private static final String STOP_OPTION = "-s";

    private static final String PROMPT_OPTION = "-p";

    private static final String ECHO_OPTION = "-e";

    private static final String NOVAR_OPTION = "-nonvar";

    private BundleContext bundleContext;

    @Requires
    private ShellService shellService;

    @Requires
    ShellContext shellContext;

    public RunCmdImpl(BundleContext context) {
        bundleContext = context;
    }

    public String getName() {
        return "run";
    }

    public String getUsage() {
        return "run [-help]";
    }

    public String getShortDescription() {
        return "run a script downloaded from an url";
    }

    private void printUsage(PrintStream out) {
        out.println(getName() + "[options] <url>");
        out.println(HELP_OPTION + "\t- Show this help message.");
        ;
        out.println(ECHO_OPTION + "\t- echo commands.");
        out.println(PROMPT_OPTION + "\t- prompt each command.");
        out.println(STOP_OPTION + "\t- stop on error.");
        out.println(NOVAR_OPTION + "\t- disable variables substitution.");
        return;
    }

    public void execute(String s, PrintStream out, PrintStream err) {
        StringTokenizer st = new StringTokenizer(s, " ");
        if (st.countTokens() == 1) {
            printUsage(out);
            return;
        }
        st.nextToken();
        String token = null;
        boolean stopOnError = false;
        boolean echoCommand = false;
        boolean prompt = false;
        boolean novar = false;
        String urlstr = null;
        while (st.hasMoreTokens()) {
            token = st.nextToken().trim();
            if (token.equals(STOP_OPTION)) {
                stopOnError = true;
            } else if (token.equals(ECHO_OPTION)) {
                echoCommand = true;
            } else if (token.equals(PROMPT_OPTION)) {
                prompt = true;
            } else if (token.equals(NOVAR_OPTION)) {
                novar = true;
            } else if (token.equals(HELP_OPTION)) {
                break;
            } else {
                urlstr = token;
            }
        }
        if (urlstr == null) {
            printUsage(out);
            return;
        }
        URL url;
        try {
            url = new URL(urlstr);
        } catch (MalformedURLException e) {
            e.printStackTrace(err);
            err.flush();
            return;
        }
        URLConnection urlcnx;
        BufferedReader br;
        try {
            urlcnx = url.openConnection();
            br = new BufferedReader(new InputStreamReader(urlcnx.getInputStream()));
        } catch (IOException ioe) {
            err.println(ioe.getMessage());
            err.flush();
            return;
        }
        ByteArrayOutputStream baerr = new ByteArrayOutputStream();
        PrintStream pbaerr = new PrintStream(baerr);
        try {
            BundleContextPropertiesWrapper bundleContextPropertiesWrapper = new BundleContextPropertiesWrapper(bundleContext);
            String commandLine;
            while ((commandLine = br.readLine()) != null) {
                commandLine = commandLine.trim();
                if (commandLine.equals("")) continue;
                if (!novar) {
                    commandLine = SubstituteUtility.substitute(commandLine, System.getProperties());
                    commandLine = SubstituteUtility.substitute(commandLine, bundleContextPropertiesWrapper);
                    commandLine = SubstituteUtility.substitute(commandLine, shellContext.getMap());
                }
                if (prompt) out.print("-->");
                if (echoCommand) out.print(commandLine);
                if (prompt || echoCommand) out.println();
                shellService.executeCommand(commandLine, out, pbaerr);
                pbaerr.flush();
                byte[] ba = baerr.toByteArray();
                if (ba.length != 0) {
                    err.println(new String(ba));
                    err.flush();
                    if (stopOnError) break;
                    baerr.reset();
                }
            }
        } catch (IOException ioe) {
            err.println(ioe.getMessage());
            err.flush();
            return;
        } catch (Exception e) {
            err.println(e.getMessage());
            err.flush();
            return;
        } finally {
            if (br != null) try {
                br.close();
            } catch (IOException e) {
            }
        }
    }

    private boolean debug = false;

    protected final void trace(String msg) {
        if (debug) {
            System.err.println(getClass().getName() + ":" + msg);
        }
    }
}
