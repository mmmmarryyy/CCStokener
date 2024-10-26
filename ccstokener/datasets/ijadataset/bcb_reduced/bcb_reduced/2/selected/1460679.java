package net.sf.zorobot.util;

import java.net.*;
import java.io.*;

public class StartAsker implements Asker {

    public String ask(String s) {
        try {
            String result = null;
            URL url = new URL("http://start.csail.mit.edu/startfarm.cgi?query=" + URLEncoder.encode(s, "UTF-8"));
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
            connection.setDoOutput(false);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String inputLine;
            int state = 0;
            StringBuilder sb = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                switch(state) {
                    case 0:
                        if (inputLine.indexOf("===> ") >= 0) state = 1;
                        break;
                    case 1:
                        sb.append(inputLine);
                        sb.append(" ");
                        break;
                    case 2:
                }
            }
            in.close();
            if (sb.length() > 0) {
                result = sb.toString();
                String arr[] = result.split("<(hr|HR)>");
                int numOfLine[] = new int[arr.length];
                String scriptUrl = null;
                for (int i = 0; i < arr.length; i++) {
                    int srcIdx = arr[i].indexOf("script src");
                    if (srcIdx > 0) {
                        int endScrpt = arr[i].indexOf("'", srcIdx + 12);
                        if (endScrpt > 0) {
                            scriptUrl = arr[i].substring(srcIdx + 12, endScrpt);
                        }
                    }
                    arr[i] = arr[i].replaceAll("<([Pp]|li|dd|dt|br|BR|/p|/P)>", "\n");
                    arr[i] = arr[i].replaceAll("<IMG SRC=\"([^\"]+)\".+?>", "$1");
                    arr[i] = arr[i].replaceAll("<image src=\"([^\"]+)\".+?>", "$1");
                    arr[i] = arr[i].replaceAll("<.+?>", "");
                    arr[i] = arr[i].replaceAll(" \\(source:.+?\\)", "");
                    arr[i] = arr[i].replaceAll("(Source:.+?\n|Source:.+?$)", "");
                    arr[i] = arr[i].replaceAll("&quot;", "'");
                    arr[i] = arr[i].replaceAll("&nbsp;", " ");
                    arr[i] = stripUnicode(arr[i]);
                    String line[] = arr[i].split("\n");
                    numOfLine[i] = line.length;
                    StringBuilder finRes = new StringBuilder();
                    for (int k = 0; k < line.length; k++) {
                        if (line[k].indexOf("START dialog") < 0) {
                            String lineToAdd = line[k].trim();
                            if (lineToAdd.length() > 0) {
                                if (finRes.length() > 0) finRes.append(" ");
                                finRes.append(lineToAdd);
                                char lastChar = lineToAdd.charAt(lineToAdd.length() - 1);
                                if (lastChar != '.' && (lastChar < 'A' || (lastChar > 'Z' && lastChar < 'a') || lastChar > 'z')) finRes.append(".");
                            }
                        }
                    }
                    arr[i] = finRes.toString().replaceAll("\\s+", " ");
                    System.out.println("result ---> " + arr[i]);
                }
                int shortest = 0;
                int curS = numOfLine[0];
                for (int i = 1; i < arr.length; i++) {
                    if (numOfLine[i] < curS) {
                        shortest = i;
                        curS = numOfLine[i];
                    }
                }
                result = arr[shortest];
                if (result.indexOf("You asked me to add the following assertion") >= 0 || result.indexOf("SynTactic Analysis using Reversible Transformations") >= 0 || result.indexOf("START Project") >= 0 || (result.indexOf("MIT") >= 0 && result.indexOf("Artificial Intelligence") >= 0)) result = null;
                if (result.indexOf("Below are the current conditions for") >= 0) {
                    if (scriptUrl != null) {
                        System.out.println(scriptUrl);
                        String wheatherString = getWheatherString(scriptUrl);
                        if (wheatherString != null) result = wheatherString;
                    }
                }
            } else {
                System.out.println("result ---> none!");
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getWheatherString(String url) {
        String rst = getResultStr(url);
        try {
            StringBuilder result = new StringBuilder();
            rst = cutAfter(rst, "TABLE");
            rst = cutAfter(rst, "GENXHcityInfo");
            rst = cutAfter(rst, 5);
            String city = getBefore(rst, "</B>");
            rst = cutAfter(rst, "GENXHobs1");
            rst = cutAfter(rst, 2);
            String degree = getBefore(rst, "&deg");
            rst = cutAfter(rst, "GENXHobs2");
            rst = cutAfter(rst, 2);
            String whea = getBefore(rst, "</FONT>");
            result.append("The current weather for ");
            result.append(city);
            result.append(": ");
            result.append(whea);
            result.append(" (");
            result.append(degree);
            result.append("°" + "F)");
            return result.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public String getResultStr(String urls) {
        try {
            String result = null;
            URL url = new URL(urls);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
            connection.setDoOutput(false);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String inputLine;
            int state = 0;
            StringBuilder sb = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                sb.append(inputLine);
            }
            in.close();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public String cutAfter(String toCut, int x) throws Exception {
        if (x >= toCut.length()) throw new Exception();
        return toCut.substring(x);
    }

    public String cutAfter(String toCut, String mark) throws Exception {
        int x = toCut.indexOf(mark);
        if (x < 0) throw new Exception();
        return toCut.substring(x + mark.length());
    }

    public String getBefore(String toGet, String mark) throws Exception {
        int x = toGet.indexOf(mark);
        if (x < 0) throw new Exception();
        return toGet.substring(0, x);
    }

    public String stripUnicode(String ss) {
        String arr[] = ss.split(";");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].endsWith("&amp")) {
                if (i + 1 < arr.length && arr[i + 1].startsWith("#")) {
                    if (arr[i].length() > 4) sb.append(arr[i].substring(0, arr[i].length() - 4));
                    sb.append((char) Integer.parseInt(arr[i + 1].substring(1)));
                    i++;
                }
            } else {
                sb.append(arr[i]);
            }
        }
        return sb.toString();
    }

    public static void main(String[] ar) {
        String str = "";
        for (int i = 0; i < ar.length; i++) {
            if (i > 0) str += " ";
            str += ar[i];
        }
        System.out.println(new StartAsker().ask(str));
    }
}
