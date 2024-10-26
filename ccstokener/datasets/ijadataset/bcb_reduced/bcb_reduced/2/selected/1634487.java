package edu.calpoly.csc.plantidentification;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Scanner;
import android.content.Context;
import edu.calpoly.csc.plantidentification.objects.Identification;

public class SightingShareManager {

    private static int resultNum = 0;

    public static enum Result {

        SERVER_CONNECTION_FAILED, SERVER_REJECT, OK
    }

    public static int getResultNum() {
        return resultNum;
    }

    public static Result share(Identification mIdentification, String username, String secret) {
        String text = "";
        java.util.Date d = mIdentification.getDate();
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            URL url = new URL("http://cslvm157.csc.calpoly.edu/fieldguideservice/identification/save.php" + "?username=" + username + "&secret=" + secret + "&plantid=" + mIdentification.plantId + "&lat=" + String.format("%.0f", mIdentification.lat) + "&lng=" + String.format("%.0f", mIdentification.lng) + "&date=" + URLEncoder.encode(dateFormatGmt.format(d)));
            Scanner in = new Scanner(new InputStreamReader(url.openStream())).useDelimiter("\n");
            while (in.hasNext()) {
                text = in.next();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Result.SERVER_CONNECTION_FAILED;
        }
        if (text.equals("Okay")) return Result.OK; else return Result.SERVER_REJECT;
    }

    public static Result download(Context c, String username, int latitudeE6Min, int latitudeE6Max, int longitudeE6Min, int longitudeE6Max) {
        String text = "";
        try {
            URL url = new URL("http://cslvm157.csc.calpoly.edu/fieldguideservice/identification/get.php" + "?username=" + username + "&latmin=" + String.format("%d", latitudeE6Min) + "&latmax=" + String.format("%d", latitudeE6Max) + "&lngmin=" + String.format("%d", longitudeE6Min) + "&lngmax=" + String.format("%d", longitudeE6Max));
            Scanner in = new Scanner(new InputStreamReader(url.openStream())).useDelimiter("\n");
            while (in.hasNext()) {
                text = in.next();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Result.SERVER_CONNECTION_FAILED;
        }
        resultNum = 0;
        if (text.equals("")) return Result.OK;
        DBAdapter db = new DBAdapter(c);
        db.open();
        String[] sightings = text.split(";");
        String[] sighting;
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        DateFormat df = SimpleDateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.US);
        for (int i = 0; i < sightings.length; i++) {
            sighting = sightings[i].split("\\|");
            String date = "";
            try {
                date = df.format(dateFormatGmt.parse(sighting[5]));
            } catch (ParseException e1) {
                e1.printStackTrace();
            }
            if (db.insertSharedIdentification(sighting[1], Integer.parseInt(sighting[2]), Double.parseDouble(sighting[3]), Double.parseDouble(sighting[4]), date, sighting[0]) != -1) resultNum++;
        }
        db.close();
        return Result.OK;
    }
}
