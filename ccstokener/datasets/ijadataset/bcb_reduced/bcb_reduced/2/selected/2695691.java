package com.cnc.mediaconnect.common;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import android.util.Log;
import com.cnc.mediaconnect.data.Email;
import com.cnc.mediaconnect.data.Phone;
import com.cnc.mediaconnect.data.SearchItem;
import com.cnc.mediaconnect1.request.RecordRequest;
import com.cnc.mediaconnect1.request.RequestNote;
import com.cnc.mediaconnect1.response.RecordResponse;
import com.cnc.mediaconnect1.response.ResponseNote;
import com.cnc.mediaconnect1.response.items.Note;

public class XMLfunctions {

    public static final String URL_SERVER = "http://connectsoftware.com/";

    public static final String URL_LOGIN = URL_SERVER + "mapi/login?format=xml&email={0}&password={1}&submit=Submit+Query";

    public static final String URL_SEARCH_CONTACT = URL_SERVER + "mapi/contact?format=xml&token={0}&name={1}&submit=Submit+Query&page={2}&start={3}&limit={4}";

    public static final String URL_SEARCH_COMPANY = URL_SERVER + "mapi/publication?format=xml&token={0}&name={1}&submit=Submit+Query&page={2}&start={3}&limit={4}";

    public static final String URL = URL_SERVER + "mapi/upload?format=xml&token={0}&contactid={1}&submit=Submit+Query";

    public static final String URL_REGISTER = URL_SERVER + "mapi/register/post";

    public static final String URL_RECORD_CONTACT = URL_SERVER + "mapi/contact/get";

    public static final String URL_RECORD_COMPANY = URL_SERVER + "mapi/publication/get";

    public static final String URL_SESARCH_CONACT_COMPANY = URL_SERVER + "mapi/node?format=xml&token={0}&type={1}&id={2}&page={3}&submit=Submit+Query";

    public static final String URL_ADDNOTE = URL_SERVER + "mapi/node/post";

    public static final String URL_SESARCH_INTERACTION = URL_SERVER + "mapi/interaction?format=xml&token={0}&type={1}&id={2}&page={3}&submit=Submit+Query";

    public static String URL_ADDINTERACTION = URL_SERVER + "mapi/interaction/post";

    public static final String URL_SESARCH_EMPLOY = URL_SERVER + "mapi/employ?format=xml&token={0}&type={1}&id={2}&page={3}&submit=Submit+Query";

    public static final String URL_ADDEMPLOY = URL_SERVER + "mapi/employ/post";

    private static final String URL_EDIT_COOMPANY = URL_SERVER + "mapi/publication/put";

    private static final String URL_EDIT_CONTACT = URL_SERVER + "mapi/contact/put";

    public static final String URL_SEARCH_ROLODEX = URL_SERVER + "mapi/rolodex?format=xml&token={0}&type=1&contact={1}&city={2}&state={3}&company={4}&page={5}&submit=Submit+Query";

    public static final String URL_SEARCH_ROLODEX_COMPANY = URL_SERVER + "mapi/rolodex?format=xml&token={0}&type=2&contact={1}&city={2}&state={3}&company={4}&page={5}&submit=Submit+Query";

    public static final String URL_SEARCH_ALL_COMPANY = URL_SERVER + "mapi/publication/post";

    public static final String URL_SEARCH_ALL_CONTACT = URL_SERVER + "mapi/contact/post";

    public static String URL_GET_PHONE_CONTACT = URL_SERVER + "mapi/contact/getphone";

    public static String getXML(String url) {
        String line = null;
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            line = EntityUtils.toString(httpEntity);
        } catch (Exception e) {
            line = "<results status=\"error\"><msg>Can't connect to server</msg></results>";
            line = null;
        }
        return line;
    }

    public static final Document XMLfromString(String xml) {
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            doc = db.parse(is);
        } catch (Exception e) {
            return null;
        }
        return doc;
    }

    public static final String getElementValue(Node elem) {
        Node kid;
        if (elem != null) {
            if (elem.hasChildNodes()) {
                for (kid = elem.getFirstChild(); kid != null; kid = kid.getNextSibling()) {
                    if (kid.getNodeType() == Node.TEXT_NODE) {
                        return kid.getNodeValue();
                    }
                }
            }
        }
        return "";
    }

    public static int numResults(Document doc) {
        Node results = doc.getDocumentElement();
        int res = -1;
        try {
            res = Integer.valueOf(results.getAttributes().getNamedItem("count").getNodeValue());
        } catch (Exception e) {
            res = -1;
        }
        return res;
    }

    public static String getValue(Element item, String str) {
        try {
            NodeList n = item.getElementsByTagName(str);
            return XMLfunctions.getElementValue(n.item(0));
        } catch (Exception e) {
            return "";
        }
    }

    public static String[] login(String userName, String password) {
        String _return[] = new String[3];
        String strXml = XMLfunctions.getXML(URL_LOGIN.replace("{0}", userName).replace("{1}", password));
        if (strXml != null) {
            Document document = XMLfunctions.XMLfromString(strXml);
            NodeList nodes = document.getElementsByTagName("response");
            Element e = (Element) nodes.item(0);
            _return[0] = XMLfunctions.getValue(e, "success");
            if ("true".equals(_return[0])) {
                _return[1] = XMLfunctions.getValue(e, "token");
                _return[2] = XMLfunctions.getValue(e, "Role_ID");
            }
        }
        return _return;
    }

    public static List<SearchItem> search(String token, String search, boolean isContact, int page, int start, int limit) {
        List<SearchItem> _return = new ArrayList<SearchItem>();
        String url = URL_SEARCH_CONTACT;
        if (!isContact) {
            url = URL_SEARCH_COMPANY;
        }
        url = url.replace("{0}", token);
        url = url.replace("{1}", URLEncoder.encode(search));
        url = url.replace("{2}", "" + 0);
        url = url.replace("{3}", "" + start);
        url = url.replace("{4}", "" + limit);
        String strXml = XMLfunctions.getXML(url);
        if (strXml != null) {
            Document document = XMLfunctions.XMLfromString(strXml);
            try {
                NodeList nodes = document.getElementsByTagName("item");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element e = (Element) nodes.item(i);
                    String id = XMLfunctions.getValue(e, isContact ? "id" : "MasterID");
                    String name = XMLfunctions.getValue(e, isContact ? "Name__Last__First_" : "Name");
                    SearchItem searchItem = new SearchItem();
                    searchItem.set(0, id);
                    searchItem.set(1, name);
                    _return.add(searchItem);
                }
            } catch (Exception e) {
            }
            try {
                Element e1 = (Element) document.getElementsByTagName(TOTAL_ITEM).item(0);
                SearchItem searchItem = new SearchItem();
                searchItem.set(0, getValue(e1, COUNT));
                _return.add(searchItem);
            } catch (Exception e) {
            }
        }
        return _return;
    }

    public static SearchItem register(String... args) {
        SearchItem _return = new SearchItem();
        String line = null;
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(URL_REGISTER);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
            nameValuePairs.add(new BasicNameValuePair("format", "xml"));
            nameValuePairs.add(new BasicNameValuePair("firtname", args[0]));
            nameValuePairs.add(new BasicNameValuePair("lastname", args[1]));
            nameValuePairs.add(new BasicNameValuePair("email", args[2]));
            nameValuePairs.add(new BasicNameValuePair("phone", args[3]));
            nameValuePairs.add(new BasicNameValuePair("password", args[4]));
            nameValuePairs.add(new BasicNameValuePair("confirmpassword", args[5]));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpClient.execute(httpPost);
            line = EntityUtils.toString(response.getEntity());
            Document document = XMLfunctions.XMLfromString(line);
            NodeList nodes = document.getElementsByTagName("response");
            Element e = (Element) nodes.item(0);
            _return.set(0, XMLfunctions.getValue(e, "success"));
            if ("false".endsWith(_return.get(0))) {
                _return.set(1, XMLfunctions.getValue(e, "error"));
            } else {
                _return.set(1, XMLfunctions.getValue(e, "message"));
            }
            return _return;
        } catch (Exception e) {
            line = "<results status=\"error\"><msg>Can't connect to server</msg></results>";
            line = null;
            _return.set(0, "false");
            _return.set(1, "");
        }
        return _return;
    }

    public static SearchItem loadRecord(String id, boolean isContact) {
        String line = null;
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(isContact ? URL_RECORD_CONTACT : URL_RECORD_COMPANY);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
            nameValuePairs.add(new BasicNameValuePair("format", "xml"));
            nameValuePairs.add(new BasicNameValuePair("token", Common.token));
            nameValuePairs.add(new BasicNameValuePair("id", id));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpClient.execute(httpPost);
            line = EntityUtils.toString(response.getEntity());
            Document document = XMLfunctions.XMLfromString(line);
            NodeList nodes = document.getElementsByTagName("response");
            Element e = (Element) nodes.item(0);
            String Name__Last__First_ = XMLfunctions.getValue(e, isContact ? "Name__Last__First_" : "Name");
            String phone = "";
            if (!isContact) phone = XMLfunctions.getValue(e, "Phone");
            String Email1 = XMLfunctions.getValue(e, isContact ? "Personal_Email" : "Email");
            String Home_Fax = XMLfunctions.getValue(e, isContact ? "Home_Fax" : "Fax1");
            String Address1 = XMLfunctions.getValue(e, "Address1");
            String Address2 = XMLfunctions.getValue(e, "Address2");
            String City = XMLfunctions.getValue(e, "City");
            String State = XMLfunctions.getValue(e, "State");
            String Zip = XMLfunctions.getValue(e, "Zip");
            String Country = XMLfunctions.getValue(e, "Country");
            String Profile = XMLfunctions.getValue(e, "Profile");
            String success = XMLfunctions.getValue(e, "success");
            String error = XMLfunctions.getValue(e, "error");
            SearchItem item = new SearchItem();
            item.set(1, Name__Last__First_);
            item.set(2, phone);
            item.set(3, phone);
            item.set(4, Email1);
            item.set(5, Home_Fax);
            item.set(6, Address1);
            item.set(7, Address2);
            item.set(8, City);
            item.set(9, State);
            item.set(10, Zip);
            item.set(11, Profile);
            item.set(12, Country);
            item.set(13, success);
            item.set(14, error);
            return item;
        } catch (Exception e) {
            line = "<results status=\"error\"><msg>Can't connect to server</msg></results>";
            line = null;
        }
        return null;
    }

    public static RecordResponse loadRecord(RecordRequest recordRequest) {
        RecordResponse recordResponse = new RecordResponse();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(recordRequest.isContact() ? URL_RECORD_CONTACT : URL_RECORD_COMPANY);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
            nameValuePairs.add(new BasicNameValuePair("format", "xml"));
            nameValuePairs.add(new BasicNameValuePair("token", recordRequest.getToken()));
            nameValuePairs.add(new BasicNameValuePair("id", recordRequest.getId()));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpClient.execute(httpPost);
            String line = EntityUtils.toString(response.getEntity());
            Document document = XMLfunctions.XMLfromString(line);
            NodeList nodes = document.getElementsByTagName("response");
            Element e = (Element) nodes.item(0);
            String Name__Last__First_ = XMLfunctions.getValue(e, recordRequest.isContact() ? "Name__Last__First_" : "Name");
            String phone = "";
            if (!recordRequest.isContact()) phone = XMLfunctions.getValue(e, "Phone");
            String Email1 = XMLfunctions.getValue(e, recordRequest.isContact() ? "Personal_Email" : "Email");
            String Home_Fax = XMLfunctions.getValue(e, recordRequest.isContact() ? "Home_Fax" : "Fax1");
            String Address1 = XMLfunctions.getValue(e, "Address1");
            String Address2 = XMLfunctions.getValue(e, "Address2");
            String City = XMLfunctions.getValue(e, "City");
            String State = XMLfunctions.getValue(e, "State");
            String Zip = XMLfunctions.getValue(e, "Zip");
            String Country = XMLfunctions.getValue(e, "Country");
            String Profile = XMLfunctions.getValue(e, "Profile");
            String success = XMLfunctions.getValue(e, "success");
            String error = XMLfunctions.getValue(e, "error");
            recordResponse.setName(Name__Last__First_);
            recordResponse.setPhone(phone);
            recordResponse.setEmail(Email1);
            recordResponse.setHomeFax(Home_Fax);
            recordResponse.setAddress1(Address1);
            recordResponse.setAddress2(Address2);
            recordResponse.setCity(City);
            recordResponse.setState(State);
            recordResponse.setZip(Zip);
            recordResponse.setProfile(Profile);
            recordResponse.setCountry(Country);
            recordResponse.setSuccess(success);
            recordResponse.setError(error);
        } catch (Exception e) {
        }
        return recordResponse;
    }

    public static List<SearchItem> searchNote(String token, String searchId, boolean isContact, int page, int start, int limit) {
        List<SearchItem> _return = new ArrayList<SearchItem>();
        String strXml = null;
        String url = URL_SESARCH_CONACT_COMPANY.replace("{0}", token).replace("{2}", searchId).replace("{1}", isContact ? "1" : "2").replace("{3}", "" + page);
        url += "&start=" + start + "&limit=" + limit;
        strXml = XMLfunctions.getXML(url);
        if (strXml != null) {
            Document document = XMLfunctions.XMLfromString(strXml);
            try {
                NodeList nodes = document.getElementsByTagName("item");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element e = (Element) nodes.item(i);
                    String id = XMLfunctions.getValue(e, "id");
                    String name = XMLfunctions.getValue(e, "Note_Detail");
                    SearchItem searchItem = new SearchItem();
                    searchItem.set(0, id);
                    searchItem.set(1, name);
                    _return.add(searchItem);
                }
            } catch (Exception e) {
            }
            try {
                Element e1 = (Element) document.getElementsByTagName(TOTAL_ITEM).item(0);
                SearchItem searchItem = new SearchItem();
                searchItem.set(0, getValue(e1, COUNT));
                _return.add(searchItem);
            } catch (Exception e) {
            }
        }
        return _return;
    }

    public static ResponseNote searchResponseNote(RequestNote requestNote) {
        ResponseNote responseNote = new ResponseNote();
        String strXml = null;
        String url = URL_SESARCH_CONACT_COMPANY.replace("{0}", requestNote.getToken()).replace("{2}", requestNote.getId()).replace("{1}", requestNote.isContact() ? "1" : "2").replace("{3}", "" + requestNote.getPage());
        url += "&start=" + requestNote.getStart() + "&limit=" + requestNote.getLimit();
        strXml = XMLfunctions.getXML(url);
        if (strXml != null) {
            Document document = XMLfunctions.XMLfromString(strXml);
            try {
                NodeList nodes = document.getElementsByTagName("item");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element e = (Element) nodes.item(i);
                    String id = XMLfunctions.getValue(e, "id");
                    String name = XMLfunctions.getValue(e, "Note_Detail");
                    Note note = new Note();
                    note.setId(id);
                    note.setDetail(name);
                    responseNote.getlNotes().add(note);
                }
            } catch (Exception e) {
            }
            try {
                Element e1 = (Element) document.getElementsByTagName(TOTAL_ITEM).item(0);
                responseNote.setCount(getValue(e1, COUNT));
            } catch (Exception e) {
            }
        }
        return responseNote;
    }

    public static SearchItem addNote(String note, String id, boolean isContact) {
        SearchItem searchItem = new SearchItem();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(URL_ADDNOTE);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
            nameValuePairs.add(new BasicNameValuePair("format", "xml"));
            nameValuePairs.add(new BasicNameValuePair("token", Common.token));
            nameValuePairs.add(new BasicNameValuePair("type", isContact ? "1" : "2"));
            nameValuePairs.add(new BasicNameValuePair("id", id));
            nameValuePairs.add(new BasicNameValuePair("descriptions", note));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpClient.execute(httpPost);
            String line = EntityUtils.toString(response.getEntity());
            Document document = XMLfunctions.XMLfromString(line);
            NodeList nodes = document.getElementsByTagName("response");
            Element e = (Element) nodes.item(0);
            if ("true".equals(XMLfunctions.getValue(e, "success"))) {
                searchItem.set(0, "true");
                searchItem.set(1, XMLfunctions.getValue(e, "message"));
            } else {
                searchItem.set(0, "false");
                searchItem.set(1, XMLfunctions.getValue(e, "error"));
            }
        } catch (Exception e) {
            searchItem.set(0, "false");
            searchItem.set(1, "");
        }
        return searchItem;
    }

    public static final String TOTAL_ITEM = "total_item";

    public static final String COUNT = "count";

    public static List<SearchItem> searchInteractions(String token, String searchId, boolean isContact, int page, int start, int limit) {
        List<SearchItem> _return = new ArrayList<SearchItem>();
        String strXml = null;
        String url = URL_SESARCH_INTERACTION.replace("{0}", token).replace("{2}", searchId).replace("{1}", isContact ? "1" : "2").replace("{3}", "" + page);
        url += "&start=" + start + "&limit=" + limit;
        strXml = XMLfunctions.getXML(url);
        if (strXml != null) {
            Document document = XMLfunctions.XMLfromString(strXml);
            try {
                NodeList nodes = document.getElementsByTagName("item");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element e = (Element) nodes.item(i);
                    String timestamp = XMLfunctions.getValue(e, "Timestamp");
                    String name = XMLfunctions.getValue(e, "Description");
                    SearchItem searchItem = new SearchItem();
                    searchItem.set(0, timestamp);
                    searchItem.set(1, name);
                    _return.add(searchItem);
                }
            } catch (Exception e) {
            }
            try {
                Element e1 = (Element) document.getElementsByTagName(TOTAL_ITEM).item(0);
                SearchItem searchItem = new SearchItem();
                searchItem.set(0, getValue(e1, COUNT));
                _return.add(searchItem);
            } catch (Exception e) {
            }
        }
        return _return;
    }

    public static SearchItem addInteraction(String note, String date, String id, boolean isContact) {
        SearchItem searchItem = new SearchItem();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(URL_ADDINTERACTION);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
            nameValuePairs.add(new BasicNameValuePair("format", "xml"));
            nameValuePairs.add(new BasicNameValuePair("token", Common.token));
            nameValuePairs.add(new BasicNameValuePair("type", isContact ? "1" : "2"));
            nameValuePairs.add(new BasicNameValuePair("id", id));
            nameValuePairs.add(new BasicNameValuePair("descriptions", note));
            nameValuePairs.add(new BasicNameValuePair("date", date));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpClient.execute(httpPost);
            String line = EntityUtils.toString(response.getEntity());
            Document document = XMLfunctions.XMLfromString(line);
            NodeList nodes = document.getElementsByTagName("response");
            Element e = (Element) nodes.item(0);
            if ("true".equals(XMLfunctions.getValue(e, "success"))) {
                searchItem.set(0, "true");
                searchItem.set(1, XMLfunctions.getValue(e, "message"));
            } else {
                searchItem.set(0, "false");
                searchItem.set(1, XMLfunctions.getValue(e, "error"));
            }
        } catch (Exception e) {
            searchItem.set(0, "false");
            searchItem.set(1, "");
        }
        return searchItem;
    }

    public static List<SearchItem> searchEmploy(String token, String searchId, boolean isContact, int page, int start, int limit) {
        List<SearchItem> _return = new ArrayList<SearchItem>();
        String strXml = null;
        String url = URL_SESARCH_EMPLOY.replace("{0}", token).replace("{2}", searchId).replace("{1}", isContact ? "1" : "2").replace("{3}", "" + page);
        url += "&start=" + start + "&limit=" + limit;
        strXml = XMLfunctions.getXML(url);
        if (strXml != null) {
            Document document = XMLfunctions.XMLfromString(strXml);
            try {
                NodeList nodes = document.getElementsByTagName("item");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element e = (Element) nodes.item(i);
                    String timestamp = XMLfunctions.getValue(e, "Title1");
                    String name = XMLfunctions.getValue(e, isContact ? "Name" : "Name__Last__First_");
                    String id = XMLfunctions.getValue(e, "id");
                    String idPu = XMLfunctions.getValue(e, isContact ? "pid" : "cid");
                    SearchItem searchItem = new SearchItem();
                    searchItem.set(0, timestamp);
                    searchItem.set(1, name);
                    searchItem.set(2, id);
                    searchItem.set(3, idPu);
                    _return.add(searchItem);
                }
            } catch (Exception e) {
            }
            try {
                Element e1 = (Element) document.getElementsByTagName(TOTAL_ITEM).item(0);
                SearchItem searchItem = new SearchItem();
                searchItem.set(0, getValue(e1, COUNT));
                _return.add(searchItem);
            } catch (Exception e) {
            }
        }
        return _return;
    }

    public static SearchItem addEmploy(String contactId, String publication, String job_type) {
        SearchItem searchItem = new SearchItem();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(URL_ADDEMPLOY);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
            nameValuePairs.add(new BasicNameValuePair("format", "xml"));
            nameValuePairs.add(new BasicNameValuePair("token", Common.token));
            nameValuePairs.add(new BasicNameValuePair("contact_id", contactId));
            nameValuePairs.add(new BasicNameValuePair("publication_id", publication));
            nameValuePairs.add(new BasicNameValuePair("job_type", job_type));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpClient.execute(httpPost);
            String line = EntityUtils.toString(response.getEntity());
            Document document = XMLfunctions.XMLfromString(line);
            NodeList nodes = document.getElementsByTagName("response");
            Element e = (Element) nodes.item(0);
            if ("true".equals(XMLfunctions.getValue(e, "success"))) {
                searchItem.set(0, "true");
                searchItem.set(1, XMLfunctions.getValue(e, "message"));
            } else {
                searchItem.set(0, "false");
                searchItem.set(1, XMLfunctions.getValue(e, "error"));
            }
        } catch (Exception e) {
            searchItem.set(0, "true");
            searchItem.set(1, "");
        }
        return searchItem;
    }

    public static List<SearchItem> searchAllContact() {
        List<SearchItem> _return = new ArrayList<SearchItem>();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(URL_ADDNOTE);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
            nameValuePairs.add(new BasicNameValuePair("format", "xml"));
            nameValuePairs.add(new BasicNameValuePair("token", Common.token));
            nameValuePairs.add(new BasicNameValuePair("submit", "submit"));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpClient.execute(httpPost);
            String line = EntityUtils.toString(response.getEntity());
            Document document = XMLfunctions.XMLfromString(line);
            try {
                NodeList nodes = document.getElementsByTagName("item");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element e = (Element) nodes.item(i);
                    String id = XMLfunctions.getValue(e, "id");
                    String name = XMLfunctions.getValue(e, "Name__Last__First_");
                    SearchItem searchItem = new SearchItem();
                    searchItem.set(0, id);
                    searchItem.set(1, name);
                    _return.add(searchItem);
                }
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
        return _return;
    }

    public static SearchItem editCompay(boolean isContact, String... args) {
        SearchItem searchItem = new SearchItem();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(isContact ? URL_EDIT_CONTACT : URL_EDIT_COOMPANY);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
            nameValuePairs.add(new BasicNameValuePair("format", "xml"));
            nameValuePairs.add(new BasicNameValuePair("token", Common.token));
            nameValuePairs.add(new BasicNameValuePair("id", args[0]));
            if (isContact) {
                nameValuePairs.add(new BasicNameValuePair("type", "1"));
                nameValuePairs.add(new BasicNameValuePair("email", args[1]));
                nameValuePairs.add(new BasicNameValuePair("phone1", args[2]));
                nameValuePairs.add(new BasicNameValuePair("phone2", args[3]));
                nameValuePairs.add(new BasicNameValuePair("fax", args[4]));
                nameValuePairs.add(new BasicNameValuePair("address1", args[5]));
                nameValuePairs.add(new BasicNameValuePair("address2", args[6]));
                nameValuePairs.add(new BasicNameValuePair("city", args[7]));
                nameValuePairs.add(new BasicNameValuePair("state", args[8]));
                nameValuePairs.add(new BasicNameValuePair("zip", args[9]));
                nameValuePairs.add(new BasicNameValuePair("profile", args[10]));
            } else {
                nameValuePairs.add(new BasicNameValuePair("name", args[1]));
                nameValuePairs.add(new BasicNameValuePair("email", args[2]));
                nameValuePairs.add(new BasicNameValuePair("phone", args[3]));
                nameValuePairs.add(new BasicNameValuePair("fax", args[4]));
                nameValuePairs.add(new BasicNameValuePair("address1", args[5]));
                nameValuePairs.add(new BasicNameValuePair("address2", args[6]));
                nameValuePairs.add(new BasicNameValuePair("city", args[7]));
                nameValuePairs.add(new BasicNameValuePair("state", args[8]));
                nameValuePairs.add(new BasicNameValuePair("zip", args[9]));
                nameValuePairs.add(new BasicNameValuePair("profile", args[10]));
            }
            nameValuePairs.add(new BasicNameValuePair("submit", "submit"));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpClient.execute(httpPost);
            String line = EntityUtils.toString(response.getEntity());
            Document document = XMLfunctions.XMLfromString(line);
            NodeList nodes = document.getElementsByTagName("response");
            Element e = (Element) nodes.item(0);
            if ("true".equals(XMLfunctions.getValue(e, "success"))) {
                searchItem.set(0, "true");
                searchItem.set(1, XMLfunctions.getValue(e, "message"));
            } else {
                searchItem.set(0, "false");
                searchItem.set(1, XMLfunctions.getValue(e, "error"));
            }
        } catch (Exception e) {
            searchItem.set(0, "false");
            searchItem.set(1, "");
        }
        return searchItem;
    }

    public static List<SearchItem> searchRolodex(boolean isSearchCompany, int page, String token, String strContact, String strCity, String strState, String strCompany, int limited, int start) {
        List<SearchItem> _return = new ArrayList<SearchItem>();
        String strXml = null;
        String urlSearch = URL_SEARCH_ROLODEX;
        if (isSearchCompany) urlSearch = URL_SEARCH_ROLODEX_COMPANY;
        urlSearch = urlSearch.replace("{0}", token);
        urlSearch = urlSearch.replace("{1}", URLEncoder.encode(strContact));
        urlSearch = urlSearch.replace("{2}", URLEncoder.encode(strCity));
        urlSearch = urlSearch.replace("{3}", URLEncoder.encode(strState));
        String limit = "&limit=" + limited + "&start=" + start;
        urlSearch += limit;
        if (isSearchCompany) urlSearch = urlSearch.replace("{4}", URLEncoder.encode((strCompany))); else urlSearch = urlSearch.replace("{4}", "");
        urlSearch = urlSearch.replace("{5}", "" + page);
        strXml = XMLfunctions.getXML(urlSearch);
        if (strXml != null) {
            Document document = XMLfunctions.XMLfromString(strXml);
            try {
                NodeList nodes = document.getElementsByTagName("item");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element e = (Element) nodes.item(i);
                    String id = XMLfunctions.getValue(e, "id");
                    String nameContact = XMLfunctions.getValue(e, "Name__Last__First_");
                    String city = XMLfunctions.getValue(e, "City");
                    String state = XMLfunctions.getValue(e, "State");
                    String nameComopany = XMLfunctions.getValue(e, "Name");
                    SearchItem searchItem = new SearchItem();
                    searchItem.set(0, id);
                    searchItem.set(1, nameContact);
                    searchItem.set(2, nameComopany);
                    searchItem.set(3, state);
                    searchItem.set(4, city);
                    _return.add(searchItem);
                }
            } catch (Exception e) {
            }
            try {
                Element e1 = (Element) document.getElementsByTagName(TOTAL_ITEM).item(0);
                SearchItem searchItem = new SearchItem();
                searchItem.set(0, getValue(e1, COUNT));
                _return.add(searchItem);
            } catch (Exception e) {
            }
        }
        return _return;
    }

    public static List<SearchItem> searchAll(boolean isContact) {
        List<SearchItem> _return = new ArrayList<SearchItem>();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(isContact ? URL_SEARCH_ALL_CONTACT : URL_SEARCH_ALL_COMPANY);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
            nameValuePairs.add(new BasicNameValuePair("format", "xml"));
            nameValuePairs.add(new BasicNameValuePair("token", Common.token));
            nameValuePairs.add(new BasicNameValuePair("submit", "submit"));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpClient.execute(httpPost);
            String line = EntityUtils.toString(response.getEntity());
            Document document = XMLfunctions.XMLfromString(line);
            NodeList nodes = document.getElementsByTagName("item");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                String id = XMLfunctions.getValue(e, isContact ? "id" : "MasterID");
                String name = XMLfunctions.getValue(e, isContact ? "Name__Last__First_" : "Name");
                SearchItem searchItem = new SearchItem();
                searchItem.set(0, id);
                searchItem.set(1, name);
                _return.add(searchItem);
            }
        } catch (Exception e) {
        }
        return _return;
    }

    public static List<Phone> searchAllPhone(String _id) {
        List<Phone> _return = new ArrayList<Phone>();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(URL_GET_PHONE_CONTACT);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
            nameValuePairs.add(new BasicNameValuePair("format", "xml"));
            nameValuePairs.add(new BasicNameValuePair("token", Common.token));
            nameValuePairs.add(new BasicNameValuePair("id", _id));
            nameValuePairs.add(new BasicNameValuePair("submit", "submit"));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpClient.execute(httpPost);
            String line = EntityUtils.toString(response.getEntity());
            Document document = XMLfunctions.XMLfromString(line);
            NodeList nodes = document.getElementsByTagName("item");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                String id = XMLfunctions.getValue(e, "id");
                String phone1 = XMLfunctions.getValue(e, "phone1");
                String phone2 = XMLfunctions.getValue(e, "phone2");
                String name = XMLfunctions.getValue(e, "name");
                Phone phone = new Phone();
                phone.setId(id);
                phone.setName(name);
                phone.setPhone1(phone1);
                phone.setPhone2(phone2);
                _return.add(phone);
            }
        } catch (Exception e) {
        }
        return _return;
    }

    public static List<Email> searchAllEmail(String _id) {
        List<Email> _return = new ArrayList<Email>();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://connectsoftware.com/mapi/contact/getemail");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
            nameValuePairs.add(new BasicNameValuePair("format", "xml"));
            nameValuePairs.add(new BasicNameValuePair("token", Common.token));
            nameValuePairs.add(new BasicNameValuePair("id", _id));
            nameValuePairs.add(new BasicNameValuePair("submit", "submit"));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpClient.execute(httpPost);
            String line = EntityUtils.toString(response.getEntity());
            Document document = XMLfunctions.XMLfromString(line);
            NodeList nodes = document.getElementsByTagName("item");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                String id = XMLfunctions.getValue(e, "id");
                String email1 = XMLfunctions.getValue(e, "email1");
                String email2 = XMLfunctions.getValue(e, "email2");
                String name = XMLfunctions.getValue(e, "name");
                Email email = new Email();
                email.setId(id);
                email.setEmail1(email1);
                email.setEmail2(email2);
                email.setName(name);
                _return.add(email);
            }
        } catch (Exception e) {
        }
        return _return;
    }

    public static List<String> updatePhone(List<Phone> lPhones) {
        List<String> result = new ArrayList<String>();
        String url = "http://connectsoftware.com/mapi/contact/updatephone?format=xml&token=";
        url += Common.token;
        for (int i = 0; i < lPhones.size(); i++) {
            Phone phone = lPhones.get(i);
            String add = "&id" + (i + 1) + "=" + URLEncoder.encode(phone.getId());
            add += "&phone1_id" + (i + 1) + "=" + URLEncoder.encode(phone.getPhone1());
            add += "&phone2_id" + (i + 1) + "=" + URLEncoder.encode(phone.getPhone2());
            url += add;
        }
        Log.i("ADB", url);
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httppost = new HttpGet(url);
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            String line = EntityUtils.toString(entity);
            Document document = XMLfunctions.XMLfromString(line);
            Log.i("ADB", line);
            NodeList nodes = document.getElementsByTagName("response");
            Element e = (Element) nodes.item(0);
            result.add(getValue(e, "success"));
            Element e1 = (Element) nodes.item(1);
            result.add(getValue(e1, "error"));
        } catch (Exception e) {
            result.add(null);
            result.add(null);
        }
        return result;
    }

    public static List<String> updateEmail(List<Email> lPhones) {
        List<String> result = new ArrayList<String>();
        String url = "http://connectsoftware.com/mapi/contact/updateemail?format=xml&token=";
        url += Common.token;
        for (int i = 0; i < lPhones.size(); i++) {
            Email phone = lPhones.get(i);
            String add = "&id" + (i + 1) + "=" + URLEncoder.encode(phone.getId());
            add += "&email1_id" + (i + 1) + "=" + URLEncoder.encode(phone.getEmail1());
            add += "&email2_id" + (i + 1) + "=" + URLEncoder.encode(phone.getEmail2());
            url += add;
        }
        Log.i("ADB", url);
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httppost = new HttpGet(url);
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            String line = EntityUtils.toString(entity);
            Document document = XMLfunctions.XMLfromString(line);
            Log.i("ADB", line);
            NodeList nodes = document.getElementsByTagName("response");
            Element e = (Element) nodes.item(0);
            result.add(getValue(e, "success"));
            Element e1 = (Element) nodes.item(1);
            result.add(getValue(e1, "error"));
        } catch (Exception e) {
            result.add(null);
            result.add(null);
        }
        return result;
    }
}
