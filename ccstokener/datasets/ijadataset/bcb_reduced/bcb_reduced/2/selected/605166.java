package org.paradise.etrc.test;

import java.net.*;
import java.io.*;
import org.paradise.etrc.data.Stop;
import org.paradise.etrc.data.Train;

public class NetTest {

    public NetTest() {
    }

    static final String URL_trainName = "http://www.huochepiao.com/Search/chaxun/resultc.asp?txtCheCi=";

    static final String Pattern_data1 = "<td align=\"center\" height=\"18\">";

    static final String Pattern_data2 = "<td height=\"18\" align=\"center\">";

    public void getTrainData(Train train) {
        BufferedReader in = null;
        try {
            HttpURLConnection connection = connect(URL_trainName + train.getTrainName());
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            int count = 0;
            String stName = "";
            String stArrive = "";
            String stLeave = "";
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith(Pattern_data1) || inputLine.startsWith(Pattern_data2)) {
                    count++;
                    if (count < 6) continue;
                    String content = paserLine(inputLine);
                    switch((count - 6) % 7) {
                        case 3:
                            System.out.print(content + " = (");
                            stName = content;
                            break;
                        case 4:
                            System.out.print(content + ",");
                            stArrive = content;
                            break;
                        case 5:
                            System.out.println(content + ")");
                            stLeave = content;
                            train.appendStop(Stop.makeStop(stName, stArrive, stLeave, true));
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String paserLine(String inputLine) {
        int len = inputLine.length();
        String tail = inputLine.substring(31, len);
        len = tail.length();
        String content = tail.substring(0, len - 5);
        if (content.startsWith("<a")) content = paserStationName(content);
        return content;
    }

    private String paserStationName(String line) {
        String[] str1 = line.split("<");
        return str1[1].split(">")[1];
    }

    private HttpURLConnection connect(String urlString) throws MalformedURLException, IOException {
        HttpURLConnection httpConn = (HttpURLConnection) (new URL(urlString)).openConnection();
        httpConn.setDoOutput(true);
        httpConn.setRequestMethod("POST");
        httpConn.addRequestProperty("Connection", "Keep-Alive");
        httpConn.addRequestProperty("Content-Type", "text/xml");
        return httpConn;
    }

    public static void main(String[] args) {
        Train train = new Train();
        train.trainNameDown = "T757";
        train.trainNameUp = "T756";
        new NetTest().getTrainData(train);
        System.out.println(train);
    }
}
