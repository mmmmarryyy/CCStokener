package org.paradise.etrc.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import org.paradise.etrc.data.Stop;
import org.paradise.etrc.data.Train;

/**
 * 从www.huochepiao.com网站获取信息
 */
public class NetInfoHuoChePiao {

    static final String URL_getTrainInfoByName = "http://www.huochepiao.com/Search/chaxun/resultc.asp?txtCheCi=";

    static final String Pattern_TrainInfoData1 = "<td align=\"center\" height=\"18\">";

    static final String Pattern_TrainInfoData2 = "<td height=\"18\" align=\"center\">";

    static final String URL_getStationInfoByName = "http://www.huochepiao.com/Search/chaxun/resultz.asp?txtChezhan=";

    public String[] getStationData(String stationName) {
        BufferedReader in = null;
        try {
            HttpURLConnection connection = connect(URL_getStationInfoByName + stationName);
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            int count = 0;
            Vector trainNames = new Vector();
            while ((inputLine = in.readLine()) != null) {
                int index1 = inputLine.indexOf("txtCheCi=");
                int index2 = inputLine.indexOf("\">", index1);
                if (index1 >= 0) {
                    String trainName = inputLine.substring(index1 + 9, index2);
                    if (count % 2 == 0) {
                        trainNames.add(trainName);
                    }
                    count++;
                }
            }
            String arrStationNames[] = new String[count / 2];
            for (int i = 0; i < count / 2; i++) {
                arrStationNames[i] = (String) trainNames.get(i);
            }
            return arrStationNames;
        } catch (Exception e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    /**
	 * 从火车票网获取指定车次的停站信息
	 * 需要事先设定好上下行车次（如果只有一个就设一个）
	 * 或者设定全称－－对于三车次以上的情形，目前的临时解决办法
	 * @param train
	 */
    public void getTrainData(Train train) {
        train.stopNum = 0;
        BufferedReader in = null;
        try {
            HttpURLConnection connection = connect(URL_getTrainInfoByName + train.getTrainName());
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            int count = 0;
            String stName = "";
            String stArrive = "";
            String stLeave = "";
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith(Pattern_TrainInfoData1) || inputLine.startsWith(Pattern_TrainInfoData2)) {
                    count++;
                    if (count < 6) continue;
                    String content = paserTrainInfoLine(inputLine);
                    switch((count - 6) % 7) {
                        case 3:
                            stName = content;
                            break;
                        case 4:
                            stArrive = content;
                            break;
                        case 5:
                            stLeave = content;
                            train.appendStop(Stop.makeStop(stName, stArrive, stLeave, true));
                            break;
                        default:
                            System.out.println(inputLine);
                    }
                }
            }
            if (train.stopNum != 0) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String paserTrainInfoLine(String inputLine) {
        int len = inputLine.length();
        String tail = inputLine.substring(31, len);
        len = tail.length();
        String content = tail.substring(0, len - 5);
        if (content.startsWith("<a")) content = paserTrainInfoStationName(content);
        return content;
    }

    private String paserTrainInfoStationName(String line) {
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
        String sta[] = new NetInfoHuoChePiao().getStationData("西安");
        for (int i = 0; i < sta.length; i++) {
            String names[] = sta[i].split("/");
            Train train = new Train();
            train.trainNameFull = sta[i];
            for (int j = 0; j < names.length; j++) {
                if (Train.isDownName(names[j])) train.trainNameDown = names[j]; else train.trainNameUp = names[j];
            }
            new NetInfoHuoChePiao().getTrainData(train);
            System.out.print(train);
        }
    }
}
