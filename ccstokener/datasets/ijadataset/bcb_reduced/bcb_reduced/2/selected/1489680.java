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
 * The country object for exchange currency data for Europe/Lithuania
 * @author Andrei Erimicioi <erani.mail@gmail.com>
 */
public class Lithuania extends AbstractCountry {

    @Override
    public void parse() throws IOException {
        URL url = new URL(getDataUrl());
        URLConnection con = url.openConnection();
        BufferedReader bStream = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String str;
        while ((str = bStream.readLine()) != null) {
            String[] tokens = str.split(",");
            CurrencyUnit unit = new CurrencyUnit(tokens[1], Float.valueOf(tokens[3]), Integer.valueOf(tokens[2]));
            this.set.add(unit);
        }
    }

    private String getDataUrl() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        sdf.setTimeZone(TimeZone.getDefault());
        String tokens[] = sdf.format(cal.getTime()).split("/");
        return (new DataUrlResolver()).getDataUrl(DomainName.LITHUANIA).replace("{month}", tokens[0]).replace("{day}", tokens[1]).replace("{year}", tokens[2]);
    }
}
