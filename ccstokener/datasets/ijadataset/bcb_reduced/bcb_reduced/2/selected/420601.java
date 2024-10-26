package com.microfly.job.yahoo;

import com.microfly.exception.NpsException;
import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.Vector;
import java.util.Currency;
import java.util.Locale;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;

/**
 * CurrencyConverter
 *   �ṩyahoo�Ļ��ʲ�ѯ����
 *
 * a new publishing system
 * Copyright: Copyright (c) 2007
 *
 * @author jialin
 * @version 1.0
 *
*/
public class CurrencyConverter {

    private Vector currency_infos = new Vector();

    private static final String URL_YAHOO_QUOTE = "http://download.finance.yahoo.com/d/quotes.csv";

    private static final String FORMAT = "f=sl1d1t1&e=.csv";

    public CurrencyConverter() {
    }

    public void AddCurrency(String from, String to) {
        try {
            Currency.getInstance(from);
            Currency.getInstance(to);
        } catch (Exception e) {
            com.microfly.util.DefaultLog.error_noexception(e);
            return;
        }
        ExchangeRate currency = new ExchangeRate(from, to);
        currency_infos.add(currency);
    }

    public Vector Get() throws Exception {
        String query_str = BuildYahooQueryString();
        if (query_str == null) return null;
        Vector result = new Vector();
        HttpURLConnection urlc = null;
        try {
            URL url = new URL(URL_YAHOO_QUOTE + "?" + query_str + "&" + FORMAT);
            urlc = (HttpURLConnection) url.openConnection();
            urlc.setRequestMethod("GET");
            urlc.setDoOutput(true);
            urlc.setDoInput(true);
            urlc.setUseCaches(false);
            urlc.setAllowUserInteraction(false);
            urlc.setRequestProperty("Content-type", "text/html;charset=UTF-8");
            if (urlc.getResponseCode() == 200) {
                InputStream in = urlc.getInputStream();
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    String msg = null;
                    while ((msg = reader.readLine()) != null) {
                        ExchangeRate rate = ParseYahooData(msg);
                        if (rate != null) result.add(rate);
                    }
                } finally {
                    if (reader != null) try {
                        reader.close();
                    } catch (Exception e1) {
                    }
                    if (in != null) try {
                        in.close();
                    } catch (Exception e1) {
                    }
                }
                return result;
            }
        } finally {
            if (urlc != null) try {
                urlc.disconnect();
            } catch (Exception e) {
            }
        }
        return null;
    }

    private String BuildYahooQueryString() {
        String query = null;
        if (currency_infos == null || currency_infos.size() == 0) return null;
        for (Object obj : currency_infos) {
            ExchangeRate currency = (ExchangeRate) obj;
            if (query == null) query = "s="; else query += "+";
            query += currency.ToYahooQueryString();
        }
        return query;
    }

    private ExchangeRate ParseYahooData(String msg) {
        if (msg == null || msg.trim().length() == 0) return null;
        String[] datas = msg.split(",");
        String from = datas[0].substring(1, 4);
        String to = datas[0].substring(4, 7);
        BigDecimal rate = new BigDecimal(datas[1]);
        String update_dateonly = datas[2].substring(1, datas[2].length() - 1);
        String update_timeonly = datas[3].substring(1, datas[3].length() - 1);
        String s_updatedate = null;
        if (!"N/A".equalsIgnoreCase(update_dateonly)) {
            s_updatedate = update_dateonly;
            if (!"N/A".equalsIgnoreCase(update_timeonly)) {
                s_updatedate += " " + update_timeonly.toUpperCase();
            }
        }
        Date update_date = null;
        if (s_updatedate != null) {
            String date_format = "MM/dd/yyyy h:mma";
            SimpleDateFormat sdf = new SimpleDateFormat(date_format, Locale.US);
            try {
                update_date = sdf.parse(s_updatedate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ExchangeRate(from, to, rate, update_date);
    }

    public class ExchangeRate {

        private String from = null;

        private String to = null;

        private BigDecimal rate = null;

        private Date update_date = null;

        public ExchangeRate(String from, String to) {
            this.from = from;
            this.to = to;
        }

        public ExchangeRate(String from, String to, BigDecimal rate, Date update_date) {
            this.from = from;
            this.to = to;
            this.rate = rate;
            this.update_date = update_date;
        }

        public String ToYahooQueryString() {
            return from + to + "=X";
        }

        public String GetFrom() {
            return from;
        }

        public String GetTo() {
            return to;
        }

        public BigDecimal GetRate() {
            return rate;
        }

        public Date GetDate() {
            return update_date;
        }
    }

    public static void main(String[] args) throws Exception {
        CurrencyConverter converter = new CurrencyConverter();
        converter.AddCurrency("USD", "CNY");
        converter.AddCurrency("EUR", "CNY");
        Vector rates = converter.Get();
        for (Object obj : rates) {
            ExchangeRate rate = (ExchangeRate) obj;
            System.out.println(rate.GetFrom());
            System.out.println(rate.GetTo());
            System.out.println(rate.GetRate());
            System.out.println(rate.GetDate());
        }
    }
}
