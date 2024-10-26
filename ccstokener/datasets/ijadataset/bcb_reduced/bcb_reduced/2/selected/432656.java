package org.taak.module.core;

import java.net.URL;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import org.taak.*;
import org.taak.error.*;
import org.taak.function.*;
import org.taak.util.*;

/**
 * The built in function to capitalize the first letter in a string.
 */
public class Read extends UnaryFunction {

    public static final Read INSTANCE = new Read();

    public static final String NAME = "read";

    public Object apply(Context context, Object arg) {
        Object result = null;
        if (arg == null) {
            throw new NullArgument("Null argument to read().");
        }
        if (arg instanceof String) {
            String filename = (String) arg;
            File file = new File(filename);
            if (!file.exists()) {
                throw new FileNotFound("File not found: " + filename);
            }
            if (!file.isFile()) {
                throw new BadArgument("Argument to read() is not a file: " + filename);
            }
            if (!file.canRead()) {
                throw new BadArgument("File cannot be read: " + filename);
            }
            try {
                StringBuffer buffer = new StringBuffer();
                BufferedReader input = new BufferedReader(new FileReader(file));
                String sep = System.getProperty("line.separator");
                String line = null;
                for (; ; ) {
                    line = input.readLine();
                    if (line == null) {
                        break;
                    }
                    buffer.append(line);
                    buffer.append(sep);
                }
                result = buffer.toString();
                input.close();
            } catch (Exception e) {
                throw new IOError("Error reading " + filename);
            }
        } else if (arg instanceof URL) {
            URL url = (URL) arg;
            try {
                StringBuffer buffer = new StringBuffer();
                InputStreamReader isr = new InputStreamReader(url.openStream());
                BufferedReader input = new BufferedReader(isr);
                String sep = System.getProperty("line.separator");
                String line = null;
                for (; ; ) {
                    line = input.readLine();
                    if (line == null) {
                        break;
                    }
                    buffer.append(line);
                    buffer.append(sep);
                }
                result = buffer.toString();
                input.close();
            } catch (Exception e) {
                throw new IOError("Error reading " + url);
            }
        } else {
            throw new BadArgument("Bad argument to read()");
        }
        return result;
    }

    public String getName() {
        return NAME;
    }
}
