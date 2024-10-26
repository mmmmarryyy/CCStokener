package com.uglygreencar.tribalwars;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

/***
 * 
 * /map/village.txt - /map/village.txt.gz

This file contains information about the villages. The data are available in the following order:

$id, $name, $x, $y, $tribe, $points, $bonus



/map/tribe.txt - /map/tribe.txt.gz

This file contains information about the players. The data are available in the following order:

$id, $name, $ally, $villages, $points, $rank



/map/ally.txt - /map/ally.txt.gz

This file contains information about Tribal Wars. The data are sorted in the following order:

$id, $name, $tag, $members, $villages, $points, $all_points, $rank
 * 
 * @author nathaniel
 *
 */
public abstract class DataReader {

    protected abstract String getURL();

    protected abstract void processLine(String line);

    protected void readFile() {
        BufferedReader in = null;
        InputStreamReader inputStreamReader = null;
        try {
            URL url;
            try {
                url = new URL(this.getURL());
            } catch (Exception e) {
                e.printStackTrace();
                url = null;
            }
            URLConnection connection = url.openConnection();
            GZIPInputStream gzipInputStream = new GZIPInputStream(connection.getInputStream());
            inputStreamReader = new InputStreamReader(gzipInputStream);
            in = new BufferedReader(inputStreamReader);
            while (in.ready()) {
                this.processLine(in.readLine());
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (inputStreamReader != null) inputStreamReader.close();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }
}
