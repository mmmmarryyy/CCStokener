package net.sourceforge.ytdataexporter.logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Type AbstractExportLogic.
 * @author Arne Lottmann
 */
public abstract class AbstractExportLogic implements IExportLogic {

    private String _user;

    private ExportType _type;

    /**
     * @return the user
     */
    public String getUser() {
        return _user;
    }

    @Override
    public IExportLogic init(final ExportType type) {
        _type = type;
        return this;
    }

    /**
     * @param user the new user
     */
    public void setUser(final String user) {
        _user = user;
    }

    protected String getPageText(final String url) {
        StringBuilder b = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                b.append(line).append('\n');
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return b.toString();
    }

    protected ExportType getType() {
        return _type;
    }
}
