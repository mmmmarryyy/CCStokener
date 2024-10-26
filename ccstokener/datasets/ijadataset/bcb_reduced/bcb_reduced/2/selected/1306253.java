package com.trackerdogs.websources.statistics;

import java.lang.*;
import java.util.*;
import java.net.*;
import java.io.*;
import com.trackerdogs.websources.statistics.*;
import com.trackerdogs.websources.*;
import com.trackerdogs.search.*;
import com.trackerdogs.*;

public class Evaluator {

    private static final int NO_RESULTS = 7;

    static Statistics evaluate(Vector searchEngines, Vector queries) {
        return null;
    }

    public static void main(String[] args) {
        Date beginTime = new Date();
        System.out.println("Evaluating Search Engines...");
        DataManager.importStatistics();
        Statistics stats = DataManager.getStatistics();
        Vector searchEngines = DataManager.getWebSources();
        Vector keywords = generateKeywords();
        if (stats == null) {
            System.out.println("generating new statistics");
            stats = new Statistics();
        } else {
            System.out.println("updating statistics");
        }
        if (args.length == 0) {
            Iterator it = searchEngines.iterator();
            while (it.hasNext()) {
                evaluateWebSource((WebSource) it.next(), keywords, stats);
            }
        } else {
            int at = searchEngines.indexOf(new WebSource(args[0], null));
            if (at != -1) {
                evaluateWebSource((WebSource) searchEngines.elementAt(at), keywords, stats);
            } else {
                System.out.println("No such search engine");
            }
        }
        DataManager.exportStatistics(stats);
        reloadServerCache();
        System.out.println("Total execution time: " + (((new Date()).getTime()) - beginTime.getTime()) / 1000 + " sec");
    }

    public static void evaluateWebSource(WebSource se, Vector keywords, Statistics stats) {
        if (se.getStatus() == WebSource.STATUS_FOUND || se.getStatus() == WebSource.STATUS_FORM) {
            return;
        }
        int noErrors = 0;
        int noTests = 0;
        System.out.println(se.getSEName());
        Iterator itKey = keywords.iterator();
        while (itKey.hasNext()) {
            Keywords key = (Keywords) itKey.next();
            System.out.println(" " + key.toString(true, true));
            WebSourcePage page = new WebSourcePage(se, key);
            Results res = new Results(key);
            page.fetchResultsIn(res);
            while (!page.allResultsExtracted() && !page.errorOccured()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            }
            noTests++;
            if (page.errorOccured()) {
                System.out.println("    error occured");
                noErrors++;
            }
            if (page.isAlive()) {
                System.out.println("eRRoR: PAGE STILL ALIVE !!");
            }
            System.out.println("    average score: " + page.getAverageScore());
            System.out.println("    time         : " + page.getTotalTime());
            stats.addPageScore(se.getSEName(), 0, page.getAverageScore());
        }
        if (noTests == noErrors) {
            stats.setError(se.getSEName(), "error");
        } else {
            stats.setError(se.getSEName(), null);
        }
        if (stats.getPageScore(se.getSEName(), 0) > 0) {
            se.setStatus(WebSource.STATUS_WORKS);
        }
        if (stats.getError(se.getSEName()) != null) {
            se.setStatus(WebSource.STATUS_ERROR);
        }
        System.out.println();
        DataManager.exportStatistics(stats);
    }

    private static void reloadServerCache() {
        try {
            URL url = new URL("http://localhost:8080/servlet/trackerdogs.Reload");
            InputStream is = url.openStream();
            System.out.println("Web server reload");
        } catch (IOException ex) {
            System.out.println("Error: Web Server offline");
        }
    }

    public static Vector generateKeywords() {
        Vector searches = DataManager.getSearches();
        Vector keywords = new Vector();
        Vector self = new Vector();
        Iterator searchesIt = searches.iterator();
        while (searchesIt.hasNext()) {
            String key = (String) searchesIt.next();
            keywords.addElement(new Keywords(key, Keywords.OPERATORS));
        }
        self.add(new Keywords("test", Keywords.ONE_OF_WORDS));
        self.add(new Keywords("meta search", Keywords.ONE_OF_WORDS));
        self.add(new Keywords("book", Keywords.ONE_OF_WORDS));
        self.add(new Keywords("boek", Keywords.ALL_OF_WORDS));
        self.add(new Keywords("new york", Keywords.ALL_OF_WORDS));
        self.add(new Keywords("adadffedaaa", Keywords.ONE_OF_WORDS));
        self.add(new Keywords("paper", Keywords.ALL_OF_WORDS));
        self.add(new Keywords("chat", Keywords.ONE_OF_WORDS));
        self.add(new Keywords("Koen Witters", Keywords.ONE_OF_WORDS));
        self.add(new Keywords("free mp3 music", Keywords.ONE_OF_WORDS));
        int i = 0;
        while (keywords.size() > NO_RESULTS) {
            keywords.removeElementAt(0);
        }
        while (keywords.size() < NO_RESULTS) {
            keywords.addElement(self.elementAt(i));
            i++;
        }
        return keywords;
    }
}
