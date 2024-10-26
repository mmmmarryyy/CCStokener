package ar.com.ironsoft.javaopenauth.oauth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import ar.com.ironsoft.javaopenauth.dto.Contact;
import ar.com.ironsoft.javaopenauth.dto.TokenOAuthWrap;
import ar.com.ironsoft.javaopenauth.exceptions.InvalidOAuthTokenException;
import ar.com.ironsoft.javaopenauth.exceptions.JavaOpenAuthException;
import ar.com.ironsoft.javaopenauth.utils.OAuthUtils;

public class HotmailOAuthStrategyWRAP extends OAuthStrategyWRAP {

    private static String STRATEGY_ID = "hotmail";

    public HotmailOAuthStrategyWRAP() {
    }

    public HotmailOAuthStrategyWRAP(String filename) {
        this.initKeysFromProperties(filename, STRATEGY_ID);
    }

    @Override
    public String getVerificationCodeURL() {
        String url = getOauthProperties().getProperty("hotmail.url.verification_code");
        url = url.replace("{0}", getConsumerKey()).replace("{1}", getCallbackUrl());
        return url;
    }

    @Override
    public TokenOAuthWrap getTokens(HttpServletRequest request) {
        return getTokens(request.getParameter("wrap_verification_code"));
    }

    @Override
    public TokenOAuthWrap getTokens(String verificationCode) {
        TokenOAuthWrap token = new TokenOAuthWrap();
        try {
            URL url = new URL(getTokenURL());
            URLConnection conn = url.openConnection();
            String data = URLEncoder.encode("wrap_verification_code", "UTF-8") + "=" + URLEncoder.encode(verificationCode, "UTF-8");
            data += "&" + URLEncoder.encode("wrap_client_id", "UTF-8") + "=" + URLEncoder.encode(getConsumerKey(), "UTF-8");
            data += "&" + URLEncoder.encode("wrap_client_secret", "UTF-8") + "=" + URLEncoder.encode(getConsumerSecret(), "UTF-8");
            data += "&" + URLEncoder.encode("wrap_callback", "UTF-8") + "=" + getCallbackUrl();
            data += "&" + URLEncoder.encode("idtype", "UTF-8") + "=" + URLEncoder.encode("CID", "UTF-8");
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                Map<String, String> params = OAuthUtils.buildMapFromQueryResponse(line);
                token.setExpirationTime(Integer.parseInt(params.get("wrap_access_token_expires_in")));
                token.setRefreshToken(params.get("wrap_refresh_token"));
                token.setAccessToken(params.get("wrap_access_token"));
                token.setUserId(params.get("uid"));
                token.setSecretKey(params.get("skey"));
            }
            rd.close();
        } catch (Exception e) {
            throw new JavaOpenAuthException("Error when accessing hotmail request token url", e);
        }
        return token;
    }

    @Override
    public String getTokenURL() {
        return getOauthProperties().getProperty("hotmail.url.access_token");
    }

    @Override
    public String getContactsURL(String userId) {
        String url = getOauthProperties().getProperty("hotmail.url.all_contacts");
        url = url.replace("{0}", userId);
        return url;
    }

    @Override
    public String getContactsListJson(String accessToken, String userId) {
        String response = null;
        try {
            URL url = new URL(getContactsURL(userId));
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "WRAP access_token=" + accessToken);
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                response = line;
            }
            rd.close();
        } catch (Exception e) {
            throw new JavaOpenAuthException("Error when accessing hotmail contact lists", e);
        }
        JSONObject jsonObject = JSONObject.fromObject(response);
        Integer code = OAuthUtils.getIntegerFromJson(jsonObject, "Code");
        if (code != null && code == 1062) {
            throw new InvalidOAuthTokenException("Invalid hotmail token, you need to refresh");
        }
        return response;
    }

    @Override
    public List<Contact> getContactsList(String accessToken, String userId) {
        String json = getContactsListJson(accessToken, userId);
        List<Contact> contacts = new ArrayList<Contact>();
        JSONObject jsonObject = JSONObject.fromObject(json);
        JSONArray entries = OAuthUtils.getArrayFromJson(jsonObject, "Entries");
        if (entries == null) throw new JavaOpenAuthException("The response doesnt have entries");
        for (int i = 0; i < entries.size(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            Contact c = new Contact();
            c.setFirstname(OAuthUtils.getStringFromJson(entry, "FirstName"));
            c.setLastname(OAuthUtils.getStringFromJson(entry, "LastName"));
            c.setFullname(c.getFirstname() + " " + c.getLastname());
            c.setThumbnailLink(OAuthUtils.getStringFromJson(entry, "ThumbnailImageLink"));
            c.setTitle(OAuthUtils.getStringFromJson(entry, "Title"));
            contacts.add(c);
        }
        return contacts;
    }

    @Override
    public String getRefreshTokenURL() {
        return getOauthProperties().getProperty("hotmail.url.refresh_token");
    }

    @Override
    public TokenOAuthWrap getNewToken(TokenOAuthWrap token) {
        try {
            URL url = new URL(getRefreshTokenURL());
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Content Type", "application/x-www-form-urlencoded");
            String data = URLEncoder.encode("wrap_refresh_token", "UTF-8") + "=" + token.getRefreshToken();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            String responseData = "";
            token.setAccessToken("");
            while ((line = rd.readLine()) != null) {
                responseData += line;
            }
            Map<String, String> params = OAuthUtils.buildMapFromQueryResponse(responseData);
            token.setExpirationTime(Integer.parseInt(params.get("wrap_access_token_expires_in")));
            token.setAccessToken(params.get("wrap_access_token"));
            rd.close();
        } catch (Exception e) {
            throw new JavaOpenAuthException("Error when accessing hotmail request token url", e);
        }
        return token;
    }
}
