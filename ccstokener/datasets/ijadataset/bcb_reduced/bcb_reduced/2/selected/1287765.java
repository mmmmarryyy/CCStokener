package fr.imag.adele.escoffier.script.plugin;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import org.osgi.framework.BundleContext;
import org.apache.felix.shell.ShellService;

public class RunImpl {

    private BundleContext m_context = null;

    private ShellService shellService = null;

    public boolean[] m_eps = new boolean[3];

    public final int STOP_ID = 0;

    public final int ECHO_ID = 1;

    public final int PROMPT_ID = 2;

    public RunImpl(BundleContext bundleContext, ShellService shell) {
        m_context = bundleContext;
        shellService = shell;
    }

    /**
	 * Method name : loadScript
	 *
	 * Description : loads a script from file or the "interweb"
	 *
	 * @param PrintStream out
	 * @param PrintStream err
	 */
    public void loadScript(PrintStream out, PrintStream err, URL url) {
        URLConnection urlcnx;
        BufferedReader br;
        try {
            urlcnx = url.openConnection();
            br = new BufferedReader(new InputStreamReader(urlcnx.getInputStream()));
            String line = new String();
            boolean moreLines = true;
            while (moreLines) {
                line = (String) br.readLine();
                if (line != null) {
                    out.println(line);
                } else {
                    moreLines = false;
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace(err);
            err.flush();
            return;
        }
    }

    /**
	 * Method name : execute
	 *
	 * Description : slightly adapted execute method
	 *
	 * @param String s
	 * @param PrintStream out
	 * @param PrintStream err
	 */
    public void execute(String s, PrintStream out, PrintStream err) {
        BufferedReader br = new BufferedReader(new java.io.StringReader(s));
        boolean stopOnError = m_eps[STOP_ID];
        boolean echoCommand = m_eps[ECHO_ID];
        boolean prompt = m_eps[PROMPT_ID];
        if (shellService == null) {
            err.println("No ShellService available !");
            err.flush();
            return;
        }
        ByteArrayOutputStream baerr = new ByteArrayOutputStream();
        PrintStream pbaerr = new PrintStream(baerr);
        try {
            String commandLine;
            while ((commandLine = br.readLine()) != null) {
                commandLine = commandLine.trim();
                if (commandLine.equals("")) continue;
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
            ioe.printStackTrace(err);
            err.flush();
            return;
        } catch (Exception e) {
            e.printStackTrace(err);
            err.flush();
            return;
        } finally {
            if (br != null) try {
                br.close();
            } catch (IOException e) {
            }
        }
    }
}
