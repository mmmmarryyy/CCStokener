package net.sf.hdkp.attendance;

import java.io.*;
import java.net.*;
import java.util.Date;

public abstract class Parser {

    private final URL url;

    private LineNumberReader reader;

    private String line;

    private Attendance attendance;

    public Parser(URL url) {
        this.url = url;
    }

    protected void parse(Attendance attendance) {
        this.attendance = attendance;
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Firefox/2.0");
            connection.connect();
            final InputStream is = connection.getInputStream();
            try {
                reader = new LineNumberReader(new BufferedReader(new InputStreamReader(is)));
                readLine();
                if (skipToStart()) {
                    while (hasLine()) {
                        if (!parseNext()) {
                            break;
                        }
                    }
                }
            } finally {
                is.close();
                connection.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract boolean skipToStart();

    protected abstract boolean parseNext();

    protected Member getOrCreateMember(String name) {
        return attendance.getOrCreateMember(name);
    }

    protected Raid getOrCreateRaid(Date date) {
        return attendance.getOrCreateRaid(date);
    }

    protected Raid findRaid(Date date) {
        return attendance.findRaid(date);
    }

    protected URL url() {
        return url;
    }

    protected String line() {
        return line;
    }

    protected void readLine() {
        try {
            line = reader.readLine();
        } catch (IOException e) {
            line = null;
        }
    }

    protected boolean skipTo(String pattern, String abortPattern) {
        while (hasLine() && !matches(pattern)) {
            readLine();
            if (matches(abortPattern)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasLine() {
        return line != null;
    }

    private boolean matches(String pattern) {
        return line != null && pattern != null && line.matches(".*" + pattern + ".*");
    }

    protected void dumpRest() {
        while (hasLine()) {
            System.out.println(line);
            readLine();
        }
    }
}
