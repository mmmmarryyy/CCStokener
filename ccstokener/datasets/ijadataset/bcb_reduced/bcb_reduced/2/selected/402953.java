package com.cyberkinetx.ecr.country;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import com.cyberkinetx.ecr.CurrencyUnit;
import com.cyberkinetx.ecr.util.DataUrlResolver;
import com.cyberkinetx.ecr.util.DomainName;

/**
 * The country object for exchange currency data for Europe/Czech Republic
 * @author Andrei Erimicioi <erani.mail@gmail.com>
 */
public class CzechRepublic extends AbstractCountry {

    @Override
    public void parse() throws IOException {
        URL url = new URL(getDataUrl());
        URLConnection con = url.openConnection();
        BufferedReader bStream = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String s = bStream.readLine();
        bStream.readLine();
        while ((s = bStream.readLine()) != null) {
            String[] tokens = s.split("\\|");
            CurrencyUnit unit = new CurrencyUnit(tokens[3], Float.valueOf(tokens[4]), Integer.valueOf(tokens[2]));
            set.add(unit);
        }
    }

    private String getDataUrl() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        sdf.setTimeZone(TimeZone.getDefault());
        return (new DataUrlResolver()).getDataUrl(DomainName.CZECH_REPUBLIC) + sdf.format(cal.getTime());
    }
}
