package org.josso.tooling.gshell.core.commands.utils;

import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.command.IO;
import org.apache.geronimo.gshell.clp.Option;
import org.apache.geronimo.gshell.clp.Argument;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.josso.tooling.gshell.core.support.JOSSOCommandSupport;
import java.util.List;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Concatenate and print files and/or URLs.
 *
 * @version $Rev$ $Date$
 */
@CommandComponent(id = "utils:cat", description = "Concatenate and print files and/or URLs")
public class CatCommand extends JOSSOCommandSupport {

    @Option(name = "-n", description = "Number the output lines, starting at 1")
    private boolean displayLineNumbers;

    @Argument(description = "File or URL", required = true)
    private List<String> args;

    protected Object doExecute() throws Exception {
        if (args.size() == 1 && "-".equals(args.get(0))) {
            log.info("Printing STDIN");
            cat(new BufferedReader(io.in), io);
        } else {
            for (String filename : args) {
                BufferedReader reader;
                try {
                    URL url = new URL(filename);
                    log.info("Printing URL: " + url);
                    reader = new BufferedReader(new InputStreamReader(url.openStream()));
                } catch (MalformedURLException ignore) {
                    File file = new File(filename);
                    log.info("Printing file: " + file);
                    reader = new BufferedReader(new FileReader(file));
                }
                try {
                    cat(reader, io);
                } finally {
                    IOUtil.close(reader);
                }
            }
        }
        return SUCCESS;
    }

    private void cat(final BufferedReader reader, final IO io) throws IOException {
        String line;
        int lineno = 1;
        while ((line = reader.readLine()) != null) {
            if (displayLineNumbers) {
                String gutter = StringUtils.leftPad(String.valueOf(lineno++), 6);
                io.out.print(gutter);
                io.out.print("  ");
            }
            io.out.println(line);
        }
    }
}
