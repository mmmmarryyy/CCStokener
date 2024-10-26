package org.rascalli.util.felix.shell.impl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.shell.Command;
import org.apache.felix.shell.ShellService;
import org.osgi.framework.BundleContext;
import org.rascalli.util.felix.shell.ShellContext;
import org.rascalli.util.felix.shell.util.SubstituteUtility;

@Component(public_factory = true, immediate = true)
@Provides
public class ExecCommand implements Command {

    private final Log log = LogFactory.getLog(getClass());

    public static final String AUTO_EXEC_SCRIPT_URL = "org.rascalli.util.felix.shell.core.autoexec";

    private static final String HELP_OPTION = "-help";

    private static final String STOP_OPTION = "-s";

    private static final String PROMPT_OPTION = "-p";

    private static final String ECHO_OPTION = "-e";

    private static final String NOVAR_OPTION = "-nonvar";

    private final BundleContext bundleContext;

    @Requires
    private ShellService shellService;

    @Requires
    private ShellContext shellContext;

    @Property(value = "file:${user.dir}/../autoexec.fs")
    private String defaultAutoExecScript;

    public ExecCommand(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public String getName() {
        return "exec";
    }

    public String getUsage() {
        return getName() + " [-help]";
    }

    public String getShortDescription() {
        return "execute a script downloaded from an url";
    }

    private void printUsage(PrintStream out) {
        out.println(getName() + "[options] <url>");
        out.println(HELP_OPTION + "\t- Show this help message.");
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
        urlstr = SubstituteUtility.substitute(urlstr, shellContext);
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
            err.println("cannot read script file '" + urlstr + "': " + ioe.getMessage());
            err.flush();
            return;
        }
        ByteArrayOutputStream baerr = new ByteArrayOutputStream();
        PrintStream pbaerr = new PrintStream(baerr);
        try {
            String commandLine;
            int lineNumber = 0;
            while ((commandLine = br.readLine()) != null) {
                ++lineNumber;
                commandLine = commandLine.trim();
                if (commandLine.equals("")) continue;
                if (!novar) {
                    commandLine = SubstituteUtility.substitute(commandLine, shellContext);
                }
                if (prompt) out.print("-->");
                if (echoCommand) out.print(commandLine);
                if (prompt || echoCommand) out.println();
                shellService.executeCommand(commandLine, out, pbaerr);
                pbaerr.flush();
                byte[] ba = baerr.toByteArray();
                if (ba.length != 0) {
                    err.println("error in line " + lineNumber + ": " + new String(ba));
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

    @Validate
    public void start() {
        String startupScript = bundleContext.getProperty(AUTO_EXEC_SCRIPT_URL);
        if (startupScript == null) {
            startupScript = defaultAutoExecScript;
        }
        if (startupScript != null && startupScript.length() > 0) {
            if (log.isInfoEnabled()) {
                log.info("executing script '" + startupScript + "'");
            }
            try {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintStream stream = new PrintStream(out);
                execute("exec " + startupScript, stream, stream);
                String outStr = out.toString();
                if (outStr.length() > 0) {
                    if (log.isInfoEnabled()) {
                        log.info("output of autoexec script:\n" + out);
                    }
                }
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("error executing startup script", e);
                }
            }
        }
    }
}
