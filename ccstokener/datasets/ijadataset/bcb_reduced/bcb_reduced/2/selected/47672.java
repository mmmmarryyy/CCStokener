package webelements.simpleparser;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import webelements.parser.*;
import webelements.datastructure.*;

public class IncludeKeywordResolver extends KeywordResolver {

    public boolean resolve(String parameters, Reader in, Writer out, DataFieldResolver dataFieldResolver, int[] arrayPositioner) throws IOException {
        PrintWriter printOut = new PrintWriter(out);
        URL url = new URL(parameters);
        Reader urlIn = new InputStreamReader(url.openStream());
        int ch = urlIn.read();
        while (ch != -1) {
            out.write(ch);
            ch = urlIn.read();
        }
        out.flush();
        return false;
    }
}
