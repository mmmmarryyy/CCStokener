package app.igroman.gismeteo.app;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import android.content.Context;
import android.util.Log;

public class GisXML {

    protected final Context context;

    private NodeList items;

    private String[] Coords;

    private ArrayList<ArrayList<String>> data;

    public GisXML(Context pContext) {
        super();
        context = pContext;
        data = new ArrayList();
        Log.i("dev", "GisXML: ");
    }

    public void ConnectXML(String pURL) {
        Log.i("dev", "Featching by url = " + pURL);
        InputStream stream = null;
        try {
            if (Coords != null) Coords = null;
            if (data.size() > 0) {
                data.removeAll(data);
            }
            URL url = new URL(pURL);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            Log.i("dev", "response = " + Boolean.toString(connect.getResponseCode() == connect.HTTP_OK));
            stream = new BufferedInputStream(connect.getInputStream());
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document dom = builder.parse(stream);
                Element root = dom.getDocumentElement();
                items = root.getChildNodes();
                try {
                    Coords = new String[2];
                    Coords[0] = items.item(1).getChildNodes().item(1).getAttributes().getNamedItem("latitude").getNodeValue();
                    Coords[1] = items.item(1).getChildNodes().item(1).getAttributes().getNamedItem("longitude").getNodeValue();
                    Log.i("dev", Coords[0] + ", " + Coords[1]);
                } catch (Exception e) {
                    System.out.println("Failed TestXML() get coords, Exception = " + e);
                    Log.i("dev", "Failed ConnectXML() get coords, Exception = " + e);
                }
                NodeList iContent = items.item(1).getChildNodes().item(1).getChildNodes();
                for (int i = 0; i < iContent.getLength(); i++) {
                    if ((iContent.item(i) != null) && (iContent.item(i).getNodeName().equalsIgnoreCase("FORECAST"))) {
                        ArrayList<String> tmp = new ArrayList<String>();
                        Node item = iContent.item(i);
                        NamedNodeMap attrs = item.getAttributes();
                        tmp.add(attrs.getNamedItem("day").getNodeValue());
                        tmp.add(attrs.getNamedItem("month").getNodeValue());
                        tmp.add(attrs.getNamedItem("year").getNodeValue());
                        tmp.add(attrs.getNamedItem("hour").getNodeValue());
                        tmp.add(attrs.getNamedItem("tod").getNodeValue());
                        tmp.add(attrs.getNamedItem("predict").getNodeValue());
                        tmp.add(attrs.getNamedItem("weekday").getNodeValue());
                        data.add(tmp);
                        try {
                            for (int j = 0; j < iContent.item(i).getChildNodes().getLength(); j++) {
                                item = iContent.item(i).getChildNodes().item(j);
                                if ((item != null) && (!item.getNodeName().equalsIgnoreCase("#text"))) {
                                    ArrayList<String> tmpList = data.get(data.size() - 1);
                                    attrs = item.getAttributes();
                                    String tmpName = item.getNodeName();
                                    if (tmpName.equalsIgnoreCase("PHENOMENA")) {
                                        tmpList.add(attrs.getNamedItem("cloudiness").getNodeValue());
                                        tmpList.add(attrs.getNamedItem("precipitation").getNodeValue());
                                        if (attrs.getNamedItem("rpower") != null) {
                                            tmpList.add(attrs.getNamedItem("rpower").getNodeValue());
                                        } else {
                                            tmpList.add("-1");
                                        }
                                        if (attrs.getNamedItem("spower") != null) {
                                            tmpList.add(attrs.getNamedItem("spower").getNodeValue());
                                        } else {
                                            tmpList.add("-1");
                                        }
                                    } else if (tmpName.equalsIgnoreCase("PRESSURE")) {
                                        tmpList.add(attrs.getNamedItem("max").getNodeValue());
                                        tmpList.add(attrs.getNamedItem("min").getNodeValue());
                                    } else if (tmpName.equalsIgnoreCase("TEMPERATURE")) {
                                        tmpList.add(attrs.getNamedItem("max").getNodeValue());
                                        tmpList.add(attrs.getNamedItem("min").getNodeValue());
                                    } else if (tmpName.equalsIgnoreCase("WIND")) {
                                        tmpList.add(attrs.getNamedItem("max").getNodeValue());
                                        tmpList.add(attrs.getNamedItem("min").getNodeValue());
                                        tmpList.add(attrs.getNamedItem("direction").getNodeValue());
                                    } else if (tmpName.equalsIgnoreCase("RELWET")) {
                                        tmpList.add(attrs.getNamedItem("max").getNodeValue());
                                        tmpList.add(attrs.getNamedItem("min").getNodeValue());
                                    } else if (tmpName.equalsIgnoreCase("HEAT")) {
                                        tmpList.add(attrs.getNamedItem("max").getNodeValue());
                                        tmpList.add(attrs.getNamedItem("min").getNodeValue());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Failed TestXML() for j, Exception = " + e);
                            Log.i("dev", "Failed ConnectXML() for j, Exception = " + e);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Failed TestXML() parse, Exception = " + e);
                Log.i("dev", "Failed ConnectXML() parse, Exception = " + e);
            }
        } catch (Exception e) {
            System.out.println("Failed TestXML() connect, Exception = " + e);
            Log.i("dev", "Failed ConnectXML() connect, Exception = " + e);
        }
    }

    public String GetPhenomena(Integer idx) {
        String cloudiness = data.get(idx).get(7);
        String precipitation = data.get(idx).get(8);
        Log.i("dev", precipitation + "_" + cloudiness);
        if ((precipitation.equals("4")) || (precipitation.equals("5"))) {
            if (cloudiness.equals("2")) return "s"; else return "t";
        } else if ((precipitation.equals("6")) || (precipitation.equals("7"))) {
            if (cloudiness.equals("2")) return "r"; else return "o";
        } else if ((precipitation.equals("8"))) {
            return "m";
        } else if (precipitation.equals("10")) {
            if (cloudiness.equals("0")) {
                return "a";
            } else if (cloudiness.equals("1")) {
                return "c";
            } else if (cloudiness.equals("2")) {
                return "d";
            } else if (cloudiness.equals("3")) {
                return "e";
            }
        } else if (precipitation.equals("9")) {
            return "9";
        } else return "9";
        return "9";
    }

    public Integer GetColor(String Phenomia) {
        switch(Phenomia.charAt(0)) {
            case '9':
                ;
            default:
                return 0xFFFFFF00;
        }
    }

    public String GetCloudiness(Integer idx) {
        switch(data.get(idx).get(7).charAt(0)) {
            case '1':
                return "�����������";
            case '2':
                return "�������";
            case '3':
                return "��������";
        }
        return "����";
    }

    public String GetPrecipitation(Integer idx) {
        switch(data.get(idx).get(8).charAt(0)) {
            case '4':
                return "�����";
            case '5':
                return "������";
            case '6':
            case '7':
                return "����";
            case '8':
                return "";
            default:
                return "��� �������";
        }
    }

    public String GetWind(Integer idx) {
        switch(data.get(idx).get(17).charAt(0)) {
            case '0':
                return "�����";
            case '1':
                return "������-������";
            case '2':
                return "������";
            case '3':
                return "���-������";
            case '4':
                return "��";
            case '5':
                return "���-�����";
            case '6':
                return "�����";
            default:
                return "������-�����";
        }
    }

    public String GetTOD(Integer idx) {
        switch(data.get(idx).get(4).charAt(0)) {
            case '0':
                return "����";
            case '1':
                return "����";
            case '2':
                return "����";
            default:
                return "�����";
        }
    }

    public String GetDate(Integer idx, Boolean Year) {
        String d, m, y;
        d = data.get(idx).get(0);
        m = data.get(idx).get(1);
        y = data.get(idx).get(2);
        if (Year) {
            return d + "." + m + "." + y;
        } else {
            return d + "." + m;
        }
    }

    public String GetWeekDay(Integer idx) {
        switch(data.get(idx).get(6).charAt(0)) {
            case '1':
                return "�����������";
            case '2':
                return "�����������";
            case '3':
                return "�������";
            case '4':
                return "�����";
            case '5':
                return "�������";
            case '6':
                return "�������";
            default:
                return "�������";
        }
    }

    public String GetPressure(Integer idx) {
        return Integer.toString(Integer.parseInt(data.get(idx).get(12)) + ((Integer) (Integer.parseInt(data.get(idx).get(11)) - Integer.parseInt(data.get(idx).get(12))) / 2)) + "��";
    }

    public String GetWindSpeed(Integer idx) {
        return Integer.toString(Integer.parseInt(data.get(idx).get(16)) + ((Integer) (Integer.parseInt(data.get(idx).get(15)) - Integer.parseInt(data.get(idx).get(16))) / 2)) + "�/�";
    }

    public String GetTemperature(Integer idx) {
        return Integer.toString(Integer.parseInt(data.get(idx).get(14)) + ((Integer) (Integer.parseInt(data.get(idx).get(13)) - Integer.parseInt(data.get(idx).get(14))) / 2)) + "�C";
    }
}
