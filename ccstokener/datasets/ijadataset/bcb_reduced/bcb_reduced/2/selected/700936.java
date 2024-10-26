package de.psychomatic.mp3db.dblayer;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.torque.Torque;
import org.apache.torque.TorqueException;

/**
 * @author Kykal
 */
public class DB {

    private static DB getInstance() {
        if (_instance == null) {
            synchronized (DB.class) {
                if (_instance == null) {
                    _instance = new DB();
                }
            }
        }
        return _instance;
    }

    public static void initDB() throws IOException {
        getInstance().init();
    }

    public static List createDB() {
        return getInstance().createTables();
    }

    public static boolean isInit() {
        return _init;
    }

    public static List clearDB() {
        return getInstance().clearTables();
    }

    public static List dropDB() {
        return getInstance().dropTables();
    }

    public static List checkDB() {
        return getInstance().checkTables();
    }

    private static DB _instance;

    private static boolean _init = false;

    private DB() {
    }

    private void init() throws IOException {
        if (Torque.isInit()) {
            String adapter = Torque.getConfiguration().getString("database.mp3db.adapter");
            if (adapter != null && adapter.trim().length() > 0) loadDBConfig(adapter);
        }
    }

    private void loadDBConfig(String adapter) throws IOException {
        URL url = getClass().getClassLoader().getResource("adapter/" + adapter + ".properties");
        _props = new Properties();
        _props.load(url.openStream());
        _init = true;
    }

    private List createTables() {
        List exceptions = new ArrayList();
        exceptions.addAll(executeSQL("mediafile.create"));
        exceptions.addAll(executeSQL("cd.create"));
        exceptions.addAll(executeSQL("album.create"));
        exceptions.addAll(executeSQL("covers.create"));
        return exceptions;
    }

    private List dropTables() {
        List exceptions = new ArrayList();
        exceptions.addAll(executeSQL("mediafile.drop"));
        exceptions.addAll(executeSQL("cd.drop"));
        exceptions.addAll(executeSQL("album.drop"));
        exceptions.addAll(executeSQL("covers.drop"));
        return exceptions;
    }

    private List clearTables() {
        List exceptions = new ArrayList();
        exceptions.addAll(executeSQL("mediafile.clear"));
        exceptions.addAll(executeSQL("cd.clear"));
        exceptions.addAll(executeSQL("album.clear"));
        exceptions.addAll(executeSQL("covers.clear"));
        return exceptions;
    }

    private List checkTables() {
        List exceptions = new ArrayList();
        exceptions.addAll(executeSQL("mediafile.check"));
        exceptions.addAll(executeSQL("cd.check"));
        exceptions.addAll(executeSQL("album.check"));
        exceptions.addAll(executeSQL("covers.check"));
        return exceptions;
    }

    private List executeSQL(String property) {
        List errors = new ArrayList();
        String sqlString = _props.getProperty(property);
        if (sqlString.endsWith(";")) sqlString = sqlString.substring(0, sqlString.length() - 1);
        String[] sqls = sqlString.split(";");
        Connection con;
        try {
            con = Torque.getConnection();
            Statement stmt = con.createStatement();
            String sql = null;
            for (int i = 0; i < sqls.length; i++) {
                sql = sqls[i].trim();
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    _log.error(e);
                    if (!sql.toLowerCase().startsWith("drop")) errors.add(e);
                }
            }
            stmt.close();
            Torque.closeConnection(con);
        } catch (TorqueException e) {
            _log.error("executeSQL(String)", e);
            errors.add(e);
        } catch (SQLException e) {
            _log.error("executeSQL(String)", e);
            errors.add(e);
        }
        return errors;
    }

    private Properties _props;

    private Logger _log = Logger.getLogger(DB.class);
}
