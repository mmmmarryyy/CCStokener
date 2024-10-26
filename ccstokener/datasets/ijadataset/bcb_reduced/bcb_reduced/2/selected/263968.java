package com.cyberkinetx.ecr.countries;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import org.dom4j.DocumentException;
import com.cyberkinetx.ecr.ResultUnit;

public class CzechRepublic extends AbstractCountry {

    private static final String DATA_URL = "http://www.cnb.cz/en/" + "financial_markets/foreign_exchange_market/exchange_rate_fixing/" + "daily.txt?date=";

    @Override
    public void parse() throws DocumentException, IOException {
        URL url = new URL(getDataUrl());
        URLConnection con = url.openConnection();
        BufferedReader bStream = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String s = bStream.readLine();
        bStream.readLine();
        while ((s = bStream.readLine()) != null) {
            String[] tokens = s.split("\\|");
            ResultUnit unit = new ResultUnit(tokens[3], Float.valueOf(tokens[4]), Integer.valueOf(tokens[2]));
            set.add(unit);
        }
    }

    private String getDataUrl() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        sdf.setTimeZone(TimeZone.getDefault());
        return DATA_URL + sdf.format(cal.getTime());
    }
}
