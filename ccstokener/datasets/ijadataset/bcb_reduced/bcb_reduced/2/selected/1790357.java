package net.updater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import net.IQuoteHistorical;
import net.YahooHistorical;
import exception.MyDBException;
import util.DateUtil;
import util.GeneralProcedures;
import util.IndiciProcedures;
import util.MyDBConnection;
import util.StoricoProcedures;
import util.YahooProcedures;

public class HistoricalUpdaterYahoo extends Observable implements IHistoricalUpdater {

    public Date getFrom() {
        return from;
    }

    public Date getTo() {
        return to;
    }

    private static final String csvLinkTemplate = "http://ichart.yahoo.com/table.csv?s=#DYNAMICSTRING#&g=d&ignore=.csv";

    private final Pattern regexDate = Pattern.compile("([0-9][0-9]?-([a-z][a-z][a-z])-[0-9][0-9]),", Pattern.CASE_INSENSITIVE);

    private final Pattern regexVal = Pattern.compile("(0-9]*\\.[0-9]*),");

    private Date from, to;

    private ArrayList<IQuoteHistorical> list = new ArrayList<IQuoteHistorical>();

    private String yahooCode = "";

    private String log = "";

    private double status = 0;

    private int id = -1;

    private HistoricalTables table;

    private String name = "";

    public HistoricalUpdaterYahoo(int mainID, HistoricalTables table, Date from, Date to) {
        this.from = from;
        this.to = to;
        id = mainID;
        this.table = table;
    }

    public HistoricalUpdaterYahoo(String name, HistoricalTables table, Date from, Date to) {
        this.from = from;
        this.to = to;
        this.name = name;
        this.table = table;
    }

    public void CompleteFields(Connection c) throws SQLException, IOException, MyDBException {
        log = "Connecting to DB...";
        setChanged();
        notifyObservers();
        if (table == HistoricalTables.STORICO) {
            if (id != -1) {
                if (yahooCode.equalsIgnoreCase("")) yahooCode = YahooProcedures.getUrlString(c, id);
                if (name == "") name = GeneralProcedures.getAzioneName(c, id);
            } else if (!name.equalsIgnoreCase("")) {
                if (id == -1) id = GeneralProcedures.getAzioneID(c, name);
                if (yahooCode.equalsIgnoreCase("")) yahooCode = YahooProcedures.getUrlString(c, id);
            }
        } else if (table == HistoricalTables.STORICO_INDICI) {
            if (id != -1) {
                if (yahooCode.equalsIgnoreCase("")) yahooCode = YahooProcedures.getUrlIndexString(c, id);
                if (name.equalsIgnoreCase("")) name = IndiciProcedures.getNome(c, id);
            } else if (!name.equalsIgnoreCase("")) {
                if (id == -1) id = IndiciProcedures.getID(c, name);
                if (yahooCode.equalsIgnoreCase("")) yahooCode = YahooProcedures.getUrlIndexString(c, id);
            }
        }
    }

    public double getCurrentStatus() {
        return status;
    }

    public String getLog() {
        return log;
    }

    public void run() {
        BufferedReader reader = null;
        log = "Downloading... " + name;
        setChanged();
        notifyObservers();
        try {
            Date marker = to;
            int previousSize = 0;
            list.clear();
            do {
                previousSize = list.size();
                URL url = new URL(createLink(from, marker));
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    try {
                        IQuoteHistorical quote = parse(line + ",");
                        if (quote != null && !list.contains(quote)) list.add(quote); else System.err.println(line);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if (list.size() > 0) marker = list.get(list.size() - 1).getData();
            } while (marker.after(from) && previousSize != list.size());
            log = "download Completed!";
        } catch (MalformedURLException e) {
            e.printStackTrace();
            log = e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
            log = e.getMessage();
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
                e.printStackTrace();
                log = e.getMessage();
            }
        }
        setChanged();
        notifyObservers();
    }

    private YahooHistorical parse(String line) throws ParseException {
        Iterator<IDateParser> chain = DateParserBuilder.getChain();
        IDateParser parser = null;
        boolean found = false;
        while (chain.hasNext() && !found) {
            parser = chain.next();
            found = parser.canParse(line);
        }
        if (!found) return null;
        java.util.Date date = parser.parseDate();
        if (date == null) return null;
        ;
        YahooHistorical record = new YahooHistorical();
        record.setData(new Date(date.getTime()));
        String[] fields = line.split(",");
        if (fields.length != 7) throw new ParseException("This line cannot be parsed: " + line, 0);
        record.setOpenPrice(Double.parseDouble(fields[1]));
        record.setHighPrice(Double.parseDouble(fields[2]));
        record.setLowPrice(Double.parseDouble(fields[3]));
        record.setClosePrice(Double.parseDouble(fields[4]));
        record.setVolume(Double.parseDouble(fields[5]));
        record.setRectClosePrice(Double.parseDouble(fields[6]));
        record.setTable(table);
        record.setId(id);
        return record;
    }

    private String createLink(Date from, Date to) {
        String dynamic = yahooCode + "&a=" + from.getMonth() + "&b=" + from.getDate() + "&c=" + (from.getYear() + 1900);
        dynamic += "&d=" + to.getMonth() + "&e=" + to.getDate() + "&f=" + (to.getYear() + 1900);
        String link = csvLinkTemplate.replace("#DYNAMICSTRING#", dynamic);
        return link;
    }

    public static void main(String[] args) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy");
        Date from = null;
        Date to = null;
        MyDBConnection myC = new MyDBConnection();
        try {
            myC.init();
            from = new Date(sdf.parse("18-01-07").getTime());
            to = new Date(System.currentTimeMillis());
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        for (int i = 1; i < 34; i++) {
            try {
                HistoricalUpdaterYahoo up = new HistoricalUpdaterYahoo(i, HistoricalTables.STORICO, from, to);
                up.run();
                Iterator<IQuoteHistorical> iter = up.getSeries();
                if (up.table == HistoricalTables.STORICO_INDICI) System.err.println("Sto swappando... " + IndiciProcedures.getNome(myC.getMyConnection(), i)); else System.err.println("Sto swappando... " + GeneralProcedures.getAzioneName(myC.getMyConnection(), i));
                StoricoProcedures.swapSeries(myC.getMyConnection(), iter);
                myC.getMyConnection().commit();
                System.err.println("Finito!");
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (MyDBException e) {
                e.printStackTrace();
            }
        }
        myC.close();
    }

    public Iterator<IQuoteHistorical> getSeries() {
        return list.iterator();
    }

    public void swapToDB(Connection c) {
        log = "Saving " + name + " to DB...";
        setChanged();
        notifyObservers();
        try {
            if (list.size() == 0) {
                JOptionPane.showMessageDialog(null, "no serie downloaded for " + name, "", JOptionPane.WARNING_MESSAGE);
                return;
            }
            StoricoProcedures.swapSeries(c, getSeries());
            log = "Series " + name + " saved";
        } catch (SQLException e) {
            e.printStackTrace();
            log = e.getMessage();
        }
        setChanged();
        notifyObservers();
    }
}
