package com.cyberkinetx.ecr.countries;

import java.io.BufferedInputStream;
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

/**
 * The country object for exchange currency data for Europe/Lithuania
 * @author Andrei Erimicioi <erani.mail@gmail.com>
 */
public class Lithuania extends AbstractCountry {

    private String XMLAddress = "http://www.lb.lt/exchange/Results.asp?Lang=E&id=5513&ord=1&dir=ASC&M={month}&D={day}&Y={year}&DD=D&S=csv&x=68283";

    public Lithuania() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        sdf.setTimeZone(TimeZone.getDefault());
        String tokens[] = sdf.format(cal.getTime()).split("/");
        this.XMLAddress = this.XMLAddress.replace("{month}", tokens[0]).replace("{day}", tokens[1]).replace("{year}", tokens[2]);
    }

    @Override
    public void parse() throws IOException, DocumentException {
        URL url = new URL(this.XMLAddress);
        URLConnection con = url.openConnection();
        BufferedReader bStream = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String str;
        while ((str = bStream.readLine()) != null) {
            String[] tokens = str.split(",");
            ResultUnit unit = new ResultUnit(tokens[1], Float.valueOf(tokens[3]), Integer.valueOf(tokens[2]));
            this.set.add(unit);
        }
    }
}
