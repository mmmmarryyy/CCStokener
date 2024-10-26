package com.tredart.yahoo.dataimport.parsers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class YahooFileHandler {

    private static final int YEAR_ADJUSTMENT = 1900;

    private final URL url;

    public YahooFileHandler(Date startDate, Date endDate, String code, EPriceFrequency frequency) {
        Object[] args = { code, startDate.getMonth(), startDate.getDate(), startDate.getYear() + YEAR_ADJUSTMENT, endDate.getMonth(), endDate.getDate(), endDate.getYear() + YEAR_ADJUSTMENT, frequency.getCode() };
        String urlString = String.format("http://ichart.yahoo.com/table.csv?s=%s&a=%02d&b=%02d&c=%04d&d=%02d&e=%02d&f=%04d&g=%s&ignore=.csv", args);
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new YahooDownloadException(e);
        }
    }

    public String downloadFile() {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            try {
                final File outFile = File.createTempFile("csvdata", ".csv");
                final BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
                try {
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        writer.write(line);
                        writer.newLine();
                    }
                } finally {
                    writer.flush();
                    writer.close();
                }
                return outFile.getAbsolutePath();
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new YahooDownloadException(e);
        }
    }
}
