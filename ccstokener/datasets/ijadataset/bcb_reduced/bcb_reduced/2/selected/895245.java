package com.javaclimber.web20fundamentals.meetup.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.javaclimber.web20fundamentals.meetup.client.MeetupNetworkingException;
import com.javaclimber.web20fundamentals.meetup.client.MeetupResponse;
import com.javaclimber.web20fundamentals.meetup.client.MeetupService;
import com.javaclimber.web20fundamentals.meetup.client.Profile;
import com.javaclimber.web20fundamentals.meetup.client.Result;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class MeetupServiceImpl extends RemoteServiceServlet implements MeetupService {

    public MeetupResponse lookupMeetupInfo(String apiKey, String groupId) throws MeetupNetworkingException {
        String json = "";
        try {
            String today = new SimpleDateFormat("MMddyyyy").format(new Date());
            String eventQuery = "http://api.meetup.com/events.json/?after=" + today + "&group_id=" + groupId + "&key=" + apiKey;
            System.out.println(eventQuery);
            json = callPage(eventQuery);
            System.out.println(json);
            String key = "\"id\":\"";
            String eventId = json.substring(json.indexOf(key) + key.length());
            System.out.println("eventId=" + eventId);
            eventId = eventId.substring(0, eventId.indexOf("\""));
            System.out.println("eventId=" + eventId);
            json = callPage("http://api.meetup.com/rsvps.json/?event_id=" + eventId + "&key=" + apiKey);
        } catch (IOException e) {
            e.printStackTrace();
            throw new MeetupNetworkingException(e);
        }
        MeetupResponse eventData = parseJSON(json);
        removeNoRSVP(eventData);
        return eventData;
    }

    private void removeNoRSVP(MeetupResponse eventData) {
        Result[] results = eventData.getResults();
        if (results == null) results = new Result[0];
        List<Result> yesList = new ArrayList<Result>();
        for (int i = 0; i < results.length; i++) {
            if ("yes".equals(results[i].getResponse())) yesList.add(results[i]);
        }
        eventData.setResults(yesList.toArray(new Result[yesList.size()]));
    }

    private MeetupResponse parseJSON(String json) throws MeetupNetworkingException {
        ObjectMapper mapper = new ObjectMapper();
        MeetupResponse eventData = null;
        try {
            eventData = (MeetupResponse) mapper.readValue(new StringReader(json), MeetupResponse.class);
            eventData = (MeetupResponse) mapper.readValue(json, MeetupResponse.class);
        } catch (JsonParseException e) {
            System.out.println(json);
            e.printStackTrace();
            throw new MeetupNetworkingException(e);
        } catch (JsonMappingException e) {
            System.out.println(json);
            e.printStackTrace();
            throw new MeetupNetworkingException(e);
        } catch (IOException e) {
            System.out.println(json);
            e.printStackTrace();
            throw new MeetupNetworkingException(e);
        }
        return eventData;
    }

    private String callPage(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        BufferedReader reader = null;
        StringBuilder result = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        } finally {
            if (reader != null) reader.close();
        }
        return result.toString();
    }

    public Profile lookupProfile(String urlLink) throws MeetupNetworkingException {
        String pageData = null;
        try {
            pageData = callPage(urlLink);
        } catch (IOException e) {
            e.printStackTrace();
            throw new MeetupNetworkingException(e);
        }
        return parseProfileData(pageData);
    }

    private Profile parseProfileData(String pageData) {
        Profile profile = new Profile();
        String tagStart = "<img class=\"D_memberProfilePhoto\" alt=\"\" class=\"photo\" src=\"";
        int profileImageIndex = pageData.indexOf(tagStart);
        if (profileImageIndex >= 0) {
            String subPage = pageData.substring(profileImageIndex + tagStart.length());
            String img = subPage.substring(0, subPage.indexOf("\""));
            profile.setImage(img);
        } else {
            return null;
        }
        tagStart = "<h4>Bio</h4>";
        profileImageIndex = pageData.indexOf(tagStart);
        if (profileImageIndex >= 0) {
            String subPage = pageData.substring(profileImageIndex + tagStart.length());
            String bio = subPage.substring(0, subPage.indexOf("<"));
            profile.setBio(bio);
        }
        return profile;
    }
}
