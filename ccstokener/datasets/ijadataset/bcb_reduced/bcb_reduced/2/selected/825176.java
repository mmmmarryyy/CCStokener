package org.ael.battlenet.wow.test.realm;

import java.io.IOException;
import java.net.URL;
import org.ael.battlenet.ServerZone;
import org.ael.battlenet.jackson.deserializer.JacksonDeserializer;
import org.ael.battlenet.wow.realm.RealmStatusQuery;
import org.ael.battlenet.wow.realm.RealmStatusResponse;
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
public class RealmStatusFetchTest {

    @Test
    public void web_apache_httpclient_fetch() throws ClientProtocolException, IOException {
        RealmStatusQuery query = new RealmStatusQuery(ServerZone.European);
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(query.getUrl());
        HttpResponse httpPayload = httpclient.execute(httpget);
        if (httpPayload.getEntity() != null && httpPayload.getStatusLine().getStatusCode() == 200) {
            String content = Utils.consumeInputStream(httpPayload.getEntity().getContent());
            RealmStatusResponse response = JacksonDeserializer.getInstance().deserializeRealmStatus(content);
            Utils.dumpRealmStatus(response);
        }
    }

    @Test
    public void web_java_http_fetch() throws ClientProtocolException, IOException {
        RealmStatusQuery query = new RealmStatusQuery(ServerZone.European);
        RealmStatusResponse response = JacksonDeserializer.getInstance().deserializeRealmStatus(new URL(query.getUrl()));
        Utils.dumpRealmStatus(response);
    }

    @Test
    public void web_java_http_parametrized_fetch() throws ClientProtocolException, IOException {
        RealmStatusQuery query = new RealmStatusQuery(ServerZone.European);
        query.addServer("Eldre'Thalas");
        query.addServer("Varimathras");
        RealmStatusResponse response = JacksonDeserializer.getInstance().deserializeRealmStatus(new URL(query.getUrl()));
        Utils.dumpRealmStatus(response);
    }
}
