package org.opennms.netmgt.provision.service.dns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opennms.netmgt.config.modelimport.ModelImport;
import org.opennms.netmgt.dao.castor.CastorUtils;

public class HandlerTest {

    private static final String DNS_URL = "dns://127.0.0.1:53/localhost";

    @Before
    public void registerFactory() {
        try {
            new URL(DNS_URL);
        } catch (MalformedURLException e) {
            URL.setURLStreamHandlerFactory(new DnsUrlFactory());
        }
    }

    @Test
    @Ignore
    public void dwOpenConnectionURL() throws IOException {
        URL url = new URL(DNS_URL);
        InputStream is = url.openConnection().getInputStream();
        Assert.assertNotNull("input stream is null", is);
        ModelImport mi = CastorUtils.unmarshalWithTranslatedExceptions(ModelImport.class, is);
        Assert.assertTrue("Number of nodes in Model Import > 1", 1 == mi.getNodeCount());
        Assert.assertTrue("NodeLabel isn't localhost", "localhost".equals(mi.getNode(0).getNodeLabel()));
        Assert.assertTrue("127.0.0.1".equals(mi.getNode(0).getInterface(0).getIpAddr()));
    }

    @Test
    public void dwParseURL() throws MalformedURLException {
        String urlString = "dns://localhost:53/opennms";
        URL dnsUrl = null;
        dnsUrl = new URL(urlString);
        try {
            dnsUrl = new URL(urlString);
            assertNotNull(dnsUrl);
            assertEquals(urlString, dnsUrl.toString());
            assertEquals("localhost", dnsUrl.getHost());
            assertEquals(53, dnsUrl.getPort());
            assertEquals(DnsRequisitionUrlConnection.PROTOCOL, dnsUrl.getProtocol());
        } catch (MalformedURLException e) {
            fail(e.getLocalizedMessage());
        }
    }
}
