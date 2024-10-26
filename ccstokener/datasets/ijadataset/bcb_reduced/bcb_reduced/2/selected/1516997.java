package net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import util.DateUtil;

public class YahooSpy extends Observable implements IQuoteSpy, Comparable<IQuoteSpy> {

    public int compareTo(IQuoteSpy o) {
        return String.CASE_INSENSITIVE_ORDER.compare(description, o.toString());
    }

    private String url, text = "";

    private InputStream stream = null;

    private String name, description = "";

    private double price = 0;

    private Date date = null;

    private static final String patternQuote = "</small><big><b>([0-9]*,[0-9]*) &#[0-9]*;</b></big>&nbsp;&nbsp;";

    private static final String link = "http://it.finance.yahoo.com/q?s=";

    private static final String patternDateOpen = "Orario:</td><td class=\"yfnc_tabledata1\">(([0-9]*:[0-9]*)) </td>";

    private static final String patternDateFlag = "Orario:";

    private YahooSpy(String url) throws IOException {
        this.url = url.substring(0);
    }

    public void run() {
        refresh();
        spyCurrentPrice();
        spyDate();
        setChanged();
        notifyObservers();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean refresh() {
        try {
            synchronized (text) {
                stream = (new URL(url)).openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                text = sb.toString();
            }
            price = 0;
            date = null;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public double spyCurrentPrice() {
        if (price != 0) return price;
        BufferedReader reader = new BufferedReader(new StringReader(text));
        String line;
        Pattern p = Pattern.compile(patternQuote);
        try {
            while ((line = reader.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.find()) {
                    price = Double.parseDouble(m.group(1).replace(",", "."));
                    return price;
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public Date spyDate() {
        if (date != null) return date;
        BufferedReader reader = new BufferedReader(new StringReader(text));
        String line;
        Pattern p = Pattern.compile(patternDateOpen);
        Pattern flag = Pattern.compile(patternDateFlag);
        try {
            while ((line = reader.readLine()) != null) {
                Matcher m = flag.matcher(line);
                if (m.find()) {
                    Matcher m2 = p.matcher(line);
                    if (m2.find()) {
                        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
                        try {
                            Date d = new Date(format.parse(m2.group(1)).getTime());
                            Date out = new Date(System.currentTimeMillis());
                            d.setYear(out.getYear());
                            d.setMonth(out.getMonth());
                            d.setDate(out.getDate());
                            return d;
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else return new Date(0, 0, 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static YahooSpy create(String quoteCode) throws IOException {
        String urlString = link + quoteCode.toUpperCase();
        YahooSpy y = new YahooSpy(urlString);
        y.name = quoteCode;
        return y;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return description;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IQuoteSpy) return description.equals(obj.toString()) || name.equals(((IQuoteSpy) obj).getName()); else return false;
    }
}
