package com.abich.eve.evecalc.alloys;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

public class Alloy {

    private static final String ALLOYS_URL = "http://www.abich.com/evecalc/alloys.properties";

    private HashMap<String, Integer> minerals = new HashMap<String, Integer>();

    public static String[] mineralTypes = { "tritanium", "pyerite", "isogen", "zydrine", "mexallon", "nocxium", "morphite", "megacyte" };

    private String name;

    private String typeID;

    private static Properties properties;

    private long amount;

    public Alloy(String typeID) {
        Properties prop = getProperties();
        this.typeID = typeID;
        name = prop.getProperty(typeID + ".name");
        readAmounts(prop);
    }

    /**
	 * @param prop
	 */
    private void readAmounts(Properties prop) {
        for (int i = 0; i < mineralTypes.length; i++) {
            String mineralAmount = prop.getProperty(name + "." + mineralTypes[i], "0");
            minerals.put(mineralTypes[i], parse(mineralAmount));
        }
    }

    private int parse(String amount) {
        try {
            Number a = NumberFormat.getIntegerInstance().parse(amount);
            return a.intValue();
        } catch (ParseException e) {
            return 0;
        }
    }

    private Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
            try {
                InputStream resStream = this.getClass().getResourceAsStream("alloys.properties");
                if (resStream == null) {
                    URL url = new URL(ALLOYS_URL);
                    try {
                        resStream = url.openStream();
                        properties.load(resStream);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    properties.load(resStream);
                }
            } catch (IOException e) {
            }
        }
        return properties;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final long getAmount() {
        return amount;
    }

    public final void setAmount(long amount) {
        this.amount = amount;
    }

    public final HashMap<String, Long> getMinerals() {
        HashMap<String, Long> list = new HashMap<String, Long>();
        for (int i = 0; i < mineralTypes.length; i++) {
            String mineral = mineralTypes[i];
            Long number = new Long(minerals.get(mineral)) * amount;
            System.out.println(mineral + ":" + number);
            list.put(mineral, number);
        }
        return list;
    }

    public static HashMap<String, Long> addMinerals(HashMap<String, Long> inMap1, HashMap<String, Long> inMap2) {
        HashMap<String, Long> outMap = new HashMap<String, Long>();
        for (Iterator<String> iter = inMap1.keySet().iterator(); iter.hasNext(); ) {
            String key = iter.next();
            Long long1 = inMap1.get(key);
            Long long2 = inMap2.get(key);
            long1 = long1 == null ? 0 : long1;
            long2 = long2 == null ? 0 : long2;
            outMap.put(key, long1 + long2);
        }
        return outMap;
    }

    public String getTypeID() {
        return typeID;
    }
}
