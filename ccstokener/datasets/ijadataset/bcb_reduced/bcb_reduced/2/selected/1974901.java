package org.ael.battlenet.wow.test.character;

import java.io.IOException;
import org.ael.battlenet.ServerZone;
import org.ael.battlenet.jackson.deserializer.JacksonDeserializer;
import org.ael.battlenet.wow.character.CharacterQuery;
import org.ael.battlenet.wow.character.CharacterQueryOption;
import org.ael.battlenet.wow.character.CharacterResponse;
import org.ael.battlenet.wow.test.util.Utils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.unitils.UnitilsJUnit4TestClassRunner;

@RunWith(UnitilsJUnit4TestClassRunner.class)
public class CharacterFetchTest {

    @Test
    public void web_apache_httpclient_fetch() throws ClientProtocolException, IOException {
        CharacterQuery query = new CharacterQuery(ServerZone.European, "eldre'thalas", "thapps");
        query.addOption(CharacterQueryOption.Achievements);
        query.addOption(CharacterQueryOption.Companions);
        query.addOption(CharacterQueryOption.HunterPets);
        query.addOption(CharacterQueryOption.Items);
        query.addOption(CharacterQueryOption.Mounts);
        query.addOption(CharacterQueryOption.Reputation);
        query.addOption(CharacterQueryOption.Stats);
        query.addOption(CharacterQueryOption.Talents);
        query.addOption(CharacterQueryOption.Titles);
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(query.getUrl());
        HttpResponse httpPayload = httpclient.execute(httpget);
        if (httpPayload.getEntity() != null && httpPayload.getStatusLine().getStatusCode() == 200) {
            String content = Utils.consumeInputStream(httpPayload.getEntity().getContent());
            System.out.println(content);
            CharacterResponse response = JacksonDeserializer.getInstance().deserializeCharacter(content);
            Utils.dumpCharacter(response);
        } else {
            System.out.println("http status code :" + httpPayload.getStatusLine().getStatusCode());
            String content = Utils.consumeInputStream(httpPayload.getEntity().getContent());
            CharacterResponse response = JacksonDeserializer.getInstance().deserializeCharacter(content);
            Utils.dumpCharacter(response);
        }
    }
}
