package com.xdatasystem.contactsimporter.hyves;

import com.xdatasystem.contactsimporter.*;
import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.http.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class HyvesImporter extends ContactListImporterImpl {

    public HyvesImporter(String username, String password) {
        super(username, password);
    }

    public String getContactListURL() {
        return "http://www.hyves.nl/berichten/contacts/?letter=";
    }

    public String getLoginURL() {
        return C.HYVES_LOGIN_ADDRESS;
    }

    protected void login(DefaultHttpClient client) throws Exception {
        String content = readInputStream(doGet(client, "http://www.hyves.nl", ""), C.HYVES_ENCODE);
        int index = content.indexOf("id=\"loginform\"");
        if (index == -1) throwProtocolChanged();
        content = content.substring(index);
        String test = "action=\"";
        index = content.indexOf(test);
        if (index == -1) throwProtocolChanged();
        content = content.substring(index + test.length());
        test = "\"";
        index = content.indexOf(test);
        if (index == -1) throwProtocolChanged();
        String loginUrl = content.substring(0, index);
        NameValuePair data[] = { new BasicNameValuePair("auth_username", getUsername()), new BasicNameValuePair("auth_password", getPassword()), new BasicNameValuePair("btnLogin", "Ok"), new BasicNameValuePair("login_initialPresence", "offline"), new BasicNameValuePair("auth_currentUrl", "http://www.hyves.nl/berichten/contacts/") };
        content = readInputStream(doPost(client, loginUrl, data, "http://www.hyves.nl/"), C.HYVES_ENCODE);
        if (content.contains("combination is unknown")) throw new AuthenticationException("Username and password do not match"); else return;
    }

    private String getExtraField(DefaultHttpClient client) throws ContactListImporterException, IOException, URISyntaxException, InterruptedException, HttpException {
        getLogger().info("Retrieve first hyves contacts page");
        String content = readInputStream(doGet(client, "http://www.hyves.nl/berichten/contacts/", "http://www.hyves.nl"), C.HYVES_ENCODE);
        String prePattern = "extra: '";
        int index = content.indexOf(prePattern);
        if (index == -1) throw new ContactListImporterException("Hyves changed protocols, the extra field could not be found");
        String newContent = content.substring(index + prePattern.length());
        index = newContent.indexOf("'");
        if (index == -1) {
            throw new ContactListImporterException("Hyves changed protocols, the extra field could not be found");
        } else {
            extraField = newContent.substring(0, index);
            return content;
        }
    }

    protected List parseContacts(InputStream contactsContent) throws Exception {
        return null;
    }

    protected List getAndParseContacts(DefaultHttpClient client, String host) throws Exception {
        getLogger().info("Retrieving contactlist");
        List contacts = new ArrayList(80);
        for (int pageChar = 97; pageChar < 125; pageChar++) addContacts(client, contacts, (char) pageChar);
        return contacts;
    }

    protected void addContacts(DefaultHttpClient client, List contacts, char pageChar) throws ContactListImporterException, URISyntaxException, InterruptedException, HttpException, IOException {
        String listUrl = (new StringBuilder(String.valueOf(getContactListURL()))).append(pageChar).toString();
        getLogger().info((new StringBuilder("Retrieve hyves contacts page ")).append(listUrl).toString());
        HttpGet get = new HttpGet(listUrl);
        HttpResponse resp = client.execute(get, client.getDefaultContext());
        parseAndAdd(readInputStream(resp.getEntity().getContent(), C.HYVES_ENCODE), contacts);
    }

    private void parseAndAdd(String content, List contacts) throws IOException, ContactListImporterException {
        getLogger().info("Parsing hyves contacts page");
        if (content.contains("You have to log in")) {
            throw new ContactListImporterException("Login was not succesfull");
        } else {
            return;
        }
    }

    private void throwProtocolChanged() throws ContactListImporterException {
        throw new ContactListImporterException("Hyves changed it's protocol, cannot import contactslist");
    }

    private String extraField;
}
