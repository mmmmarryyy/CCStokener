package com.razie.pub.lightsoa.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import razie.JAS;
import razie.base.ActionItem;
import razie.base.ActionToInvoke;
import com.razie.pub.assets.JavaAssetMgr;
import com.razie.pub.base.ExecutionContext;
import com.razie.pub.base.log.Log;
import com.razie.pub.http.test.TestLightBase;
import com.razie.pub.lightsoa.HttpAssetSoaBinding;

/**
 * setup a light server with soa assets management and test a few calls to a sample asset
 * 
 * @author razvanc99
 */
public class TestLightServerSoaAssets extends TestLightBase {

    static String PK = TestLocalSoaAssets.PLAYERKEY.toUrlEncodedString();

    public void setUp() {
        if (server == null) {
            ExecutionContext.DFLT_CTX.enter();
            super.setUp();
            JavaAssetMgr.init(new razie.assets.InventoryAssetMgr());
            JAS.manage(new SampleAsset("1"));
            JAS.manage(new SampleAsset2("2"));
            HttpAssetSoaBinding soa = new HttpAssetSoaBinding();
            soa.register(SampleAsset.class);
            cmdGET.registerSoa(soa);
        }
    }

    /**
    * test the SOA simple echo via the proper URL reader (check implementation as a proper http
    * server)
    */
    public void testSoaEchoUrl() throws IOException, InterruptedException {
        URL url = new URL("http://localhost:" + PORT + "/lightsoa/asset/" + PK + "/play?movie=" + TestLocalSoaAssets.MOVIEKEY.toUrlEncodedString());
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String result = in.readLine();
        in.close();
        assertTrue(result.contains(TestLocalSoaAssets.MOVIEKEY.getId()));
    }

    /**
     */
    public void testSoaEchoAction() throws IOException, InterruptedException {
        ActionToInvoke action = razie.JAS.aati("http://localhost:" + PORT, TestLocalSoaAssets.PLAYERKEY, new ActionItem("play"), "movie", TestLocalSoaAssets.MOVIEKEY);
        String result = (String) action.act(null);
        assertTrue(result.contains(TestLocalSoaAssets.MOVIEKEY.getId()));
    }

    static final Log logger = Log.factory.create(TestLightServerSoaAssets.class.getName());
}
