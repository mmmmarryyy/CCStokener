package jp.web.sync.android.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import jp.web.sync.android.bean.SelfGroupInfoBean;
import jp.web.sync.android.bean.SelfInfoBean;
import jp.web.sync.android.bean.SelfLocationInfoBean;
import jp.web.sync.relax.response.ResponseXML;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.util.Log;
import android.util.Xml;

/**
 * @author m-shichi
 *
 */
public class HttpUtils {

    public static final String HTTP_HOST_NAME = "www.clockworksapple.info";

    public static final String HTTP_BASE_PATH = "/krs/server/";

    /**
	 *
	 * @param selfInfo
	 * @return
	 */
    public ResponseXML requestUserInfo(int procFlag, SelfInfoBean selfInfo) {
        StringBuilder sb = new StringBuilder(HTTP_BASE_PATH);
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        switch(procFlag) {
            case 1:
                sb.append("user/new");
                qparams.add(new BasicNameValuePair("Address", selfInfo.getMailAddr()));
                qparams.add(new BasicNameValuePair("Pass", selfInfo.getHexPassword()));
                break;
            case 2:
                sb.append("user/");
                sb.append(String.valueOf(selfInfo.getId()));
                sb.append("/signin");
                qparams.add(new BasicNameValuePair("Address", selfInfo.getMailAddr()));
                qparams.add(new BasicNameValuePair("Pass", selfInfo.getHexPassword()));
                qparams.add(new BasicNameValuePair("TerminalId", selfInfo.getTerminalId()));
                break;
            case 3:
                sb.append("user/");
                sb.append(String.valueOf(selfInfo.getId()));
                sb.append("/edit");
                qparams.add(new BasicNameValuePair("Address", selfInfo.getMailAddr()));
                qparams.add(new BasicNameValuePair("Pass", selfInfo.getHexPassword()));
                qparams.add(new BasicNameValuePair("Handle", selfInfo.getHandleName()));
                qparams.add(new BasicNameValuePair("Message", selfInfo.getMessage()));
                break;
        }
        return getResponseXml(sb.toString(), qparams);
    }

    /**
	 *
	 * @param procFlag
	 * @param selfInfo
	 * @return
	 */
    public ResponseXML requestGroupInfo(int procFlag, SelfInfoBean selfInfo, SelfGroupInfoBean selfGroupInfo) {
        StringBuilder sb = new StringBuilder(HTTP_BASE_PATH);
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        switch(procFlag) {
            case 1:
                sb.append("group/");
                sb.append(String.valueOf(selfInfo.getId()));
                sb.append("/new");
                qparams.add(new BasicNameValuePair("GroupName", selfGroupInfo.getGroupName()));
                break;
            case 2:
                sb.append("group/");
                sb.append(String.valueOf(selfInfo.getId()));
                sb.append("/enter");
                qparams.add(new BasicNameValuePair("GroupId", String.valueOf(selfGroupInfo.getId())));
                break;
            case 3:
                sb.append("group/");
                sb.append(String.valueOf(selfInfo.getId()));
                sb.append("/list");
                break;
            default:
                break;
        }
        return getResponseXml(sb.toString(), qparams);
    }

    /**
	 *
	 * @param selfInfo
	 * @param selfGroupInfo
	 * @param selfLocationInfo
	 * @return
	 */
    public ResponseXML requestLocationInfo(SelfInfoBean selfInfo, SelfGroupInfoBean selfGroupInfo, SelfLocationInfoBean selfLocationInfo) {
        StringBuilder sb = new StringBuilder(HTTP_BASE_PATH);
        sb.append("location/");
        sb.append(String.valueOf(selfInfo.getId()));
        sb.append("/list");
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair("GroupId", String.valueOf(selfGroupInfo.getId())));
        qparams.add(new BasicNameValuePair("Lattitude", String.valueOf(selfLocationInfo.getLattitude())));
        qparams.add(new BasicNameValuePair("Longitude", String.valueOf(selfLocationInfo.getLongitude())));
        return getResponseXml(sb.toString(), qparams);
    }

    /**
	 *
	 * @param host
	 * @param path
	 * @param qparams
	 * @return
	 */
    private ResponseXML getResponseXml(String path, List<NameValuePair> qparams) {
        HttpClient httpclient = null;
        InputStream in = null;
        ResponseXML res = null;
        try {
            httpclient = new DefaultHttpClient();
            URI uri = URIUtils.createURI("http", HTTP_HOST_NAME, -1, path, URLEncodedUtils.format(qparams, "UTF-8"), null);
            HttpGet httpget = new HttpGet(uri);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            in = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder buf = new StringBuilder();
            String str;
            while ((str = reader.readLine()) != null) {
                buf.append(str);
                buf.append(System.getProperty("line.separator"));
            }
            reader.close();
            res = new ResponseXML(buf.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return res;
    }

    /**
	 *
	 * @param host
	 * @param path
	 * @param qparams
	 * @return
	 */
    private ResponseXML postResponseXml(String path, List<NameValuePair> qparams) {
        HttpClient httpclient = null;
        InputStream in = null;
        ResponseXML res = null;
        try {
            httpclient = new DefaultHttpClient();
            URI uri = URIUtils.createURI("http", HTTP_HOST_NAME, -1, path, URLEncodedUtils.format(qparams, "UTF-8"), null);
            HttpPost httpPost = new HttpPost(uri);
            HttpResponse response = httpclient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            in = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder buf = new StringBuilder();
            String str;
            while ((str = reader.readLine()) != null) {
                buf.append(str);
                buf.append(System.getProperty("line.separator"));
            }
            reader.close();
            res = new ResponseXML(buf.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return res;
    }

    /**
	 *
	 * @param xml
	 * @return
	 */
    @SuppressWarnings("unused")
    private ResponseXML parseResponse(String xml) {
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(new StringReader(xml));
        } catch (XmlPullParserException ex) {
            ex.printStackTrace();
        }
        try {
            int eventType;
            eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    Log.d("XmlPullParserSample", "Start document");
                } else if (eventType == XmlPullParser.END_DOCUMENT) {
                    Log.d("XmlPullParserSample", "End document");
                } else if (eventType == XmlPullParser.START_TAG) {
                    Log.d("XmlPullParserSample", "Start tag " + parser.getName());
                } else if (eventType == XmlPullParser.END_TAG) {
                    Log.d("XmlPullParserSample", "End tag " + parser.getName());
                } else if (eventType == XmlPullParser.TEXT) {
                    Log.d("XmlPullParserSample", "Text " + parser.getText());
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.d("XmlPullParserSample", "Error");
        }
        return null;
    }
}
