package com.google.code.guidatv.server.service.impl.italy.generalistic.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.code.guidatv.model.Transmission;
import com.google.code.guidatv.server.service.GuidaTvException;

public class DNERealTimeTransmissionDaoImpl implements DNERealTimeTransmissionDao {

    private final Logger log = Logger.getLogger(getClass().getName());

    private String pattern = "http://www.realtimetv.it/guidatv/guidatv.php?type={0}&day={1,number,0000}{2,number,00}{3,number,00}";

    public List<Transmission> getTransmissions(Date day) {
        MessageFormat format = new MessageFormat(pattern);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"));
        cal.setTime(day);
        List<Transmission> transmissions = new ArrayList<Transmission>();
        cal.add(Calendar.DATE, -1);
        loadTransmissions("notte", day, cal.getTime(), format, transmissions);
        loadTransmissions("mattina", day, day, format, transmissions);
        loadTransmissions("pomeriggio", day, day, format, transmissions);
        loadTransmissions("sera", day, day, format, transmissions);
        return transmissions;
    }

    private void loadTransmissions(String type, Date day, Date urlDay, MessageFormat format, List<Transmission> transmissions) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome"));
        cal.setTime(urlDay);
        String urlString = format.format(new Object[] { type, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DATE) });
        Reader reader = null;
        try {
            URL url = new URL(urlString);
            InputStream is = url.openStream();
            reader = new InputStreamReader(is, "UTF-8");
            StringBuilder builder = new StringBuilder();
            int character;
            while ((character = reader.read()) > 0) {
                builder.append((char) character);
            }
            JSONObject jsonObject = new JSONObject(builder.toString());
            JSONArray jsonArray = jsonObject.getJSONArray("contents");
            DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getInstance();
            numberFormat.applyPattern("00");
            boolean sameDay = true;
            for (int i = 0; i < jsonArray.length() && sameDay; i++) {
                JSONObject transmissionJson = jsonArray.getJSONObject(i);
                String timeString = transmissionJson.getString("time");
                String hour = timeString.substring(0, 2);
                String minutes = timeString.substring(3, 5);
                int hours = numberFormat.parse(hour).intValue();
                int minute = numberFormat.parse(minutes).intValue();
                cal.setTime(day);
                cal.set(Calendar.HOUR_OF_DAY, hours);
                cal.set(Calendar.MINUTE, minute);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                transmissions.add(new Transmission(transmissionJson.getString("name"), transmissionJson.getString("description"), cal.getTime(), null, null));
            }
        } catch (MalformedURLException e) {
            throw new GuidaTvException(e);
        } catch (IOException e) {
            throw new GuidaTvException(e);
        } catch (ParseException e) {
            throw new GuidaTvException(e);
        } catch (JSONException e) {
            log.log(Level.FINE, "Exception when getting schedule", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
