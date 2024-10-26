package org.josso.tooling.gshell.core.commands.builtins;

import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.command.annotation.Requirement;
import org.apache.geronimo.gshell.command.CommandExecutor;
import org.apache.geronimo.gshell.clp.Argument;
import org.codehaus.plexus.util.IOUtil;
import org.josso.tooling.gshell.core.support.JOSSOCommandSupport;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;

/**
 * Read and execute commands from a file/url in the current shell environment.
 *
 * @version $Rev$ $Date$
 */
@CommandComponent(id = "gshell-builtins:source", description = "Load a file/url into the current shell")
public class SourceCommand extends JOSSOCommandSupport {

    @Requirement
    private CommandExecutor executor;

    @Argument(required = true, description = "Source file")
    private String source;

    protected Object doExecute() throws Exception {
        URL url;
        try {
            url = new URL(source);
        } catch (MalformedURLException e) {
            url = new File(source).toURI().toURL();
        }
        BufferedReader reader = openReader(url);
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String tmp = line.trim();
                if (tmp.length() == 0 || tmp.startsWith("#")) {
                    continue;
                }
                executor.execute(line);
            }
        } finally {
            IOUtil.close(reader);
        }
        return SUCCESS;
    }

    private BufferedReader openReader(final Object source) throws IOException {
        BufferedReader reader;
        if (source instanceof File) {
            File file = (File) source;
            log.info("Using source file: " + file);
            reader = new BufferedReader(new FileReader(file));
        } else if (source instanceof URL) {
            URL url = (URL) source;
            log.info("Using source URL: " + url);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
        } else {
            String tmp = String.valueOf(source);
            try {
                URL url = new URL(tmp);
                log.info("Using source URL: " + url);
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
            } catch (MalformedURLException ignore) {
                File file = new File(tmp);
                log.info("Using source file: " + file);
                reader = new BufferedReader(new FileReader(tmp));
            }
        }
        return reader;
    }
}
