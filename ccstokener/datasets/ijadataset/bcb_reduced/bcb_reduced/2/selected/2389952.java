package org.geogurus.gas.forms;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.geogurus.tools.DataManager;
import org.geogurus.tools.sql.ConPool2;

/**
 *
 * @author nicolas
 */
public class CheckConfigurationForm extends org.apache.struts.action.ActionForm {

    private String mapserverURL;

    private String mapserverVersion;

    private String shp2pgsql;

    private String pgsql2shp;

    private int mapserverClassLimit;

    private int tmpMapsDeletionPeriod;

    private int tempMapsDeletionTimeRange;

    private String gasSymbolFile;

    private String gasDbReproj;

    public String getMapserverURL() {
        return mapserverURL;
    }

    public void setMapserverURL(String mapserverURL) {
        this.mapserverURL = mapserverURL;
    }

    public String getShp2pgsql() {
        return shp2pgsql;
    }

    public void setShp2pgsql(String shp2pgsql) {
        this.shp2pgsql = shp2pgsql;
    }

    public String getPgsql2shp() {
        return pgsql2shp;
    }

    public void setPgsql2shp(String pgsql2shp) {
        this.pgsql2shp = pgsql2shp;
    }

    public String getGasSymbolFile() {
        return gasSymbolFile;
    }

    public void setGasSymbolFile(String gasSymbolFile) {
        this.gasSymbolFile = gasSymbolFile;
    }

    public String getGasDbReproj() {
        return gasDbReproj;
    }

    public void setGasDbReproj(String gasDbReproj) {
        this.gasDbReproj = gasDbReproj;
    }

    public String getMapserverVersion() {
        return mapserverVersion;
    }

    public void setMapserverVersion(String mapserverVersion) {
        this.mapserverVersion = mapserverVersion;
    }

    /**
     *
     */
    public CheckConfigurationForm() {
        super();
        setMapserverURL(DataManager.getProperty("MAPSERVERURL"));
        setShp2pgsql(DataManager.getProperty("SHP2PGSQL"));
        setPgsql2shp(DataManager.getProperty("PGSQL2SHP"));
        setGasSymbolFile(DataManager.getProperty("GAS_SYMBOL_FILE"));
        setGasDbReproj(DataManager.getProperty("GAS_DB_REPROJ"));
        setMapserverVersion(DataManager.getProperty("MAPSERVERVERSION"));
    }

    /**
     * @see validate in ActionForm
     * @param mapping
     * @param request
     * @return
     */
    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();
        ActionMessage aMsg = null;
        if (getMapserverURL() == null) {
            errors.add("mapserverURL", new ActionMessage("error.mapserverURL.required"));
        } else if ((aMsg = checkURL(getMapserverURL())) != null) {
            errors.add("mapserverURL", aMsg);
        }
        if (getShp2pgsql() == null) {
            errors.add("shp2pgsql", new ActionMessage("error.shp2pgsql.required"));
        } else if ((aMsg = checkExecutablePath(getShp2pgsql())) != null) {
            errors.add("shp2pgsql", aMsg);
        }
        if (getPgsql2shp() == null) {
            errors.add("pgsql2shp", new ActionMessage("error.pgsql2shp.required"));
        } else if ((aMsg = checkExecutablePath(getPgsql2shp())) != null) {
            errors.add("pgsql2shp", aMsg);
        }
        if (getGasDbReproj() == null) {
            errors.add("gasDbReproj", new ActionMessage("error.gasDbReproj.required"));
        } else if ((aMsg = checkPostGisDb(getGasDbReproj())) != null) {
            errors.add("gasDbReproj", aMsg);
        }
        return errors;
    }

    /**
     * checks given path to test if it is a valid executable program.
     * For the moment, only tests if given path is a valid file
     * @param path the executable path to check
     * @return null if given string is a valid executable, an ActionMessage with appropriate message
     *         in case of error
     */
    private ActionMessage checkExecutablePath(String path) {
        if (path == null) {
            return new ActionMessage("error.executable.null");
        }
        File f = new File(path);
        if (!f.isFile()) {
            return new ActionMessage("error.executable.notfile");
        }
        return null;
    }

    /**
     * Checks the given URL to see if it is valid
     * 
     * @param u the url to check, given as a String
     * @return null if given URL is valid, an ActionMessage with appropriate message
     *         in case of error
     */
    private ActionMessage checkURL(String u) {
        if (u == null) {
            return new ActionMessage("error.mapserverurl.null");
        }
        URL url = null;
        try {
            url = new URL(u);
            url.openStream();
        } catch (Exception e) {
            return new ActionMessage("error.mapserverurl.invalid", e.getMessage());
        }
        return null;
    }

    /**
     * Checks if the given database properties are valid, ie allows to connect to a PostGis DB
     * containing spatial_ref_sys entries
     * <br>
     * parameters string is : &lt;host&gt;,&lt;port&gt;,&lt;db&gt;,&lt;user&gt;,&lt;pwd&gt;
     * @param dbParams the Database parameters string as configured in serverlist.properties.
     * 
     * @return null if given string allows to connect to a database, an ActionMessage with appropriate message
     *         in case of error
     */
    private ActionMessage checkPostGisDb(String dbParams) {
        if (dbParams == null) {
            return new ActionMessage("error.postgisparams.null");
        }
        Connection c = null;
        try {
            StringTokenizer tok = new StringTokenizer(dbParams, ",");
            if (tok.countTokens() != 5) {
                return new ActionMessage("error.postgisparams.invalid", dbParams);
            }
            String host = tok.nextToken();
            String dbport = tok.nextToken();
            String dbname = tok.nextToken();
            String user = tok.nextToken();
            String pwd = tok.nextToken();
            ConPool2 cp = ConPool2.getInstance();
            c = cp.getConnection(cp.getConnectionURI(host, dbport, dbname, user, pwd, ConPool2.DBTYPE_POSTGRES));
            if (c == null) {
                return new ActionMessage("error.postgisparams.invalid", dbParams);
            }
            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("select count(*) from spatial_ref_sys");
            if (!rs.next()) {
                return new ActionMessage("error.postgisparams.notspatial", "");
            }
        } catch (SQLException e) {
            return new ActionMessage("error.postgisparams.notspatial", e.getMessage());
        }
        return null;
    }

    /**
     * Tests if given String points to a valid file
     * @param file the file path to test
     * @return null if file exists, an actionMessage otherwise
     */
    private ActionMessage checkFile(String file) {
        if (file == null) {
            return new ActionMessage("error.file.null");
        }
        File f = null;
        try {
            f = new File(file);
            if (f.exists()) {
                return null;
            } else {
                ActionMessage aMsg = new ActionMessage("error.file.notfound", file);
                return aMsg;
            }
        } catch (Exception e) {
            ActionMessage aMsg = new ActionMessage("error.file.exception", e.getMessage());
            return aMsg;
        }
    }
}
