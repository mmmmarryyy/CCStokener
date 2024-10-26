package com.googlecode.g2re.examples.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Manages data connection with an in-memory representation of the Sun
 * PetStore database. This class provides helper methods to load data files
 * and manage the database connection.
 * @author Brad Rydzewski
 */
public class PetStoreDataManager {

    /**
     * A shared, static database connection. Since we are using
     * a H2 database (in memory), we need to keep a connection open for 
     * the entire duration the application server is up and running - this
     * is why this is a static variable.
     */
    private static Connection conn = null;

    /**
     * Opens a connection with the in-memory database.
     */
    public void openConnection(String url, String user, String pw, String className) {
        try {
            Class.forName(className).newInstance();
            conn = DriverManager.getConnection(url, user, pw);
            conn.setAutoCommit(true);
        } catch (Exception ex) {
            System.out.println("Error opening connection");
            System.out.println(ex.toString());
            ex.printStackTrace();
        }
    }

    /**
     * Closes the database connection.
     */
    public void closeConnection() {
        try {
            conn.close();
        } catch (Exception ex) {
        }
    }

    /**
     * Reads a text file containing DDL and sample data and loads
     * it into the in-memory database using the Statement.executeUpdate
     * command.
     * @param f Flat file with sql ddl.
     */
    public void loadFile(File f) {
        Statement statement = null;
        try {
            System.out.println("Loading file: " + f.getAbsolutePath());
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            StringBuffer result = new StringBuffer();
            while ((line = br.readLine()) != null) {
                result.append(line).append("\n");
            }
            System.out.println(result.toString());
            statement = conn.createStatement();
            int loaded = -1;
            loaded = statement.executeUpdate(result.toString());
            System.out.println("rows loaded: " + loaded);
        } catch (Exception ex1) {
            System.out.println(ex1.getMessage());
            System.out.println(ex1.toString());
            ex1.printStackTrace();
            try {
                statement.close();
            } catch (Exception ex2) {
            }
        }
    }
}
