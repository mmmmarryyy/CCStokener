package controller;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import entities.Band;
import entities.Event;
import entities.Venue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class LollaWrapper {

    /**
	 * A date format ("yyyy-MM-dd HH:mm:ss Z") used mainly for updated_on fields.
	 */
    private final SimpleDateFormat updatedOnDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    /**
	 * A date format ("EEE, dd MMM yyyy HH:mm:ss Z") used mainly for start_time and end_time.
	 */
    private final SimpleDateFormat otherDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

    /**
	 * All api calls need to start with this.
	 */
    private final String API_URL_START = "http://api.dostuffmedia.com/";

    /**
	 * All api calls need to have this at some point in them.
	 */
    private final String API_URL_END = ".json?key=";

    /**
	 * The api key used for all calls.
	 */
    private final String apiKey;

    /**
	 * The gson object used for parsing through JSON files.
	 */
    private final Gson gson;

    /**
	 * Basic and only constructor for the LollaWrapper.
	 *
	 * @param apiKey Takes in an api key, that will be used for all api calls.
	 */
    public LollaWrapper(String apiKey) {
        this.apiKey = apiKey;
        GsonBuilder b = new GsonBuilder();
        b.registerTypeAdapter(Event.class, new JsonDeserializer<Event>() {

            @Override
            public Event deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
                JsonObject eventObj = arg0.getAsJsonObject();
                Gson g = new Gson();
                Event e = g.fromJson(arg0, Event.class);
                List<Long> bandIds = new ArrayList<Long>();
                if (e.getTitle() != null) {
                    if (eventObj.getAsJsonObject("bands").get("band").isJsonArray()) {
                        List<Band> bands;
                        String jsonString = eventObj.get("bands").toString();
                        jsonString = jsonString.substring(jsonString.indexOf(":") + 1, jsonString.length() - 1);
                        bands = g.fromJson(jsonString, new TypeToken<List<Band>>() {
                        }.getType());
                        for (Band band : bands) {
                            bandIds.add(band.getId());
                        }
                    } else {
                        String jsonString = eventObj.get("bands").toString();
                        jsonString = jsonString.substring(jsonString.indexOf(":") + 1, jsonString.length() - 1);
                        Band single = g.fromJson(jsonString, Band.class);
                        bandIds = new ArrayList<Long>();
                        bandIds.add(single.getId());
                    }
                    e.setBandIds(bandIds);
                    Venue venue = g.fromJson(eventObj.get("venue"), Venue.class);
                    e.setVenueId(venue.getId());
                }
                return e;
            }
        });
        this.gson = b.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    /**
	 * Takes in a filler for the api call, and forms a full api url.
	 *
	 * @param filler Examples include "events" and "events/[ID]"
	 * @return The completed API url.
	 */
    public String createFullAPIUrl(String filler) {
        return API_URL_START + filler + API_URL_END + apiKey;
    }

    private String getJsonString(String url) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()), 8192);
        String line = reader.readLine();
        String jsonString = "";
        while (line != null) {
            jsonString += line;
            line = reader.readLine();
        }
        jsonString = jsonString.substring(jsonString.indexOf(":") + 1, jsonString.length() - 1);
        return jsonString;
    }

    /**
	 * Takes in a api call url and creates an object of generic type.
	 *
	 * @param <T>
	 * @param url  API call url.
	 * @param type Type of entity it will return
	 * @return
	 * @throws JsonSyntaxException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
    public <T> T makeCall(String url, Class<T> type) throws JsonSyntaxException, IOException {
        return getGson().fromJson(getJsonString(url), type);
    }

    /**
	 * @return the aPI_URL_START
	 */
    public String getAPI_URL_START() {
        return API_URL_START;
    }

    /**
	 * @return the aPI_URL_END
	 */
    public String getAPI_URL_END() {
        return API_URL_END;
    }

    /**
	 * @return the apiKey
	 */
    public String getApiKey() {
        return apiKey;
    }

    /**
	 * @return the gson
	 */
    public Gson getGson() {
        return gson;
    }

    /**
	 * @return the updatedAtDateFormat
	 */
    public SimpleDateFormat getUpdatedOnDateFormat() {
        return updatedOnDateFormat;
    }

    /**
	 * @return the otherDateFormat
	 */
    public SimpleDateFormat getOtherDateFormat() {
        return otherDateFormat;
    }
}
