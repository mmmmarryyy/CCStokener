package net.sourceforge.mapcraft.map.tilesets.database;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import java.sql.*;
import net.sourceforge.mapcraft.map.MapException;
import net.sourceforge.mapcraft.xml.MapXML;

/**
 * Create a new map in the database.
 * @author Samuel Penn
 */
class MapBuilder {

    static void create(String name, Properties properties, Connection cx) throws SQLException {
        createIndex(name, properties, cx);
        createTerrain(name, properties, cx);
    }

    /**
	 * Create an index for this map in the list of maps for this database.
	 * TODO: Currently no checking for duplicates is performed. This is a BUG.
	 * 
	 * @param name
	 * @param properties
	 * @param cx
	 * @throws SQLException
	 */
    private static void createIndex(String name, Properties properties, Connection cx) throws SQLException {
        int width = 64;
        int height = 64;
        int scale = 1;
        String shape = "hexagonal";
        String template = "standard";
        String description = name;
        width = Integer.parseInt(properties.getProperty("width", "64"));
        height = Integer.parseInt(properties.getProperty("height", "64"));
        scale = Integer.parseInt(properties.getProperty("scale", "1"));
        shape = properties.getProperty("shape", "hexagonal");
        template = properties.getProperty("template", "standard");
        description = properties.getProperty("description", name);
        StringBuffer query = new StringBuffer("insert into mapcraft (");
        query.append("name, description, shape, template, ");
        query.append("width, height, scale) values (");
        query.append("'").append(name).append("', ");
        query.append("'").append(description).append("', ");
        query.append("'").append(shape).append("', ");
        query.append("'").append(template).append("', ");
        query.append(width).append(", ");
        query.append(height).append(", ");
        query.append(scale).append(")");
        Statement stmnt = cx.createStatement();
        stmnt.executeUpdate(query.toString());
    }

    /**
	 * Create a default set of terrain entries for this map.
	 * 
	 * @param name
	 * @param properties
	 * @param cx
	 * @throws SQLException
	 */
    private static void createTerrain(String name, Properties properties, Connection cx) throws SQLException {
    }

    public static MapXML getTerrainInfo(URL url) {
        URL infoFile = null;
        MapXML map = null;
        try {
            infoFile = new URL(url.toString() + "/terrain.xml");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            map = new MapXML(infoFile);
            System.out.println(map.getAuthor());
            System.out.println(map.getTileShape());
        } catch (MapException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Returns a list of all valid terrain setups found at the given URL.
     * Terrain sets are returned as a Hashtable keyed by the name, and
     * containing an object of type MapXML.
     * 
     * @param url       Base URL to look for terrain resources.
     * @return          Hashtable of MapXML objects.
     */
    public static Hashtable getTerrainList(URL url) {
        Hashtable table = new Hashtable();
        String document = null;
        try {
            InputStream is = url.openStream();
            byte[] buffer = new byte[10240];
            int count = 0;
            while (is.available() > 0) {
                count += is.read(buffer, count, buffer.length - count);
            }
            String s = new String(buffer);
            int idx = 0;
            while ((idx = s.indexOf("<A HREF=", idx)) > 0) {
                int start = s.indexOf("\"", idx);
                int end = s.indexOf("\"", start + 1);
                String name = s.substring(start + 1, end - 1);
                if (name.startsWith("?") || name.startsWith("/")) {
                } else {
                    System.out.println(name);
                    URL infoUrl = new URL(url.toString() + "/" + name);
                    MapXML map = getTerrainInfo(infoUrl);
                    table.put(name, map);
                }
                idx = end;
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return table;
    }

    public static void main(String[] args) throws Exception {
        URL url = new URL("http://mapcraft.sourceforge.net/resources/terrainsets");
        getTerrainList(url);
    }
}
