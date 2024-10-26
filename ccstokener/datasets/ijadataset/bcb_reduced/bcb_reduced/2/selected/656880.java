package org.ael.battlenet.wow.test.guild;

import java.io.IOException;
import org.ael.battlenet.ServerZone;
import org.ael.battlenet.jackson.deserializer.JacksonDeserializer;
import org.ael.battlenet.wow.guild.GuildQuery;
import org.ael.battlenet.wow.guild.GuildQueryOption;
import org.ael.battlenet.wow.guild.GuildResponse;
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
public class GuildFetchTest {

    @Test
    public void web_apache_httpclient_fetch() throws ClientProtocolException, IOException {
        GuildQuery query = new GuildQuery(ServerZone.European, "EldreThalas", "Hors%20dEux");
        query.addOption(GuildQueryOption.Achievements);
        query.addOption(GuildQueryOption.Members);
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(query.getUrl());
        HttpResponse httpPayload = httpclient.execute(httpget);
        if (httpPayload.getEntity() != null && httpPayload.getStatusLine().getStatusCode() == 200) {
            String content = Utils.consumeInputStream(httpPayload.getEntity().getContent());
            System.out.println(query.getUrl());
            System.out.println(content);
            GuildResponse response = JacksonDeserializer.getInstance().getMapper().readValue(content, GuildResponse.class);
        }
    }
}
