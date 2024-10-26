package uk.ac.manchester.cs.img.myfancytool.taverna.ui.serviceprovider;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import javax.xml.rpc.ServiceException;
import net.sf.taverna.t2.servicedescriptions.AbstractConfigurableServiceProvider;
import net.sf.taverna.t2.servicedescriptions.ConfigurableServiceProvider;
import net.sf.taverna.t2.servicedescriptions.ServiceDescription;
import net.sf.taverna.t2.servicedescriptions.ServiceDescriptionProvider;
import edu.sdsc.nbcr.opal.*;
import edu.sdsc.nbcr.opal.gui.common.GetServiceListHelper;
import edu.sdsc.nbcr.opal.gui.common.OPALService;
import org.apache.axis.message.SOAPBodyElement;

public class ExampleServiceProvider extends AbstractConfigurableServiceProvider<ExampleServiceProviderConfig> implements ConfigurableServiceProvider<ExampleServiceProviderConfig> {

    private static final URI providerId = URI.create("http://example.com/2010/service-provider/example-activity-ui");

    /**
	 * Do the actual search for services. Return using the callBack parameter.
	 */
    @SuppressWarnings("unchecked")
    public void findServiceDescriptionsAsync(FindServiceDescriptionsCallBack callBack) {
        String url;
        boolean url_valid = true;
        URI url_uri = getConfiguration().getUri();
        url = url_uri.toString();
        URLConnection urlConn_test;
        try {
            urlConn_test = (new URL(url)).openConnection();
        } catch (MalformedURLException e2) {
            url_valid = false;
            e2.printStackTrace();
            System.out.println("ERROR: Bad Opal service URL entered:" + url);
        } catch (IOException e2) {
            url_valid = false;
            e2.printStackTrace();
            System.out.println("ERROR: Bad Opal service URL entered:" + url);
        }
        if (url_uri != null && url_valid == true) {
            System.out.println("URL entered: " + url_uri);
            url = url_uri.toString();
            List<ServiceDescription> results = new ArrayList<ServiceDescription>();
            try {
                URL ws_url = new URL(url);
                URLConnection urlConn;
                DataInputStream dis;
                try {
                    urlConn = ws_url.openConnection();
                    urlConn.setDoInput(true);
                    urlConn.setUseCaches(false);
                    dis = new DataInputStream(urlConn.getInputStream());
                    String s;
                    int fpos = 0;
                    int lpos;
                    int lslash;
                    String sn;
                    String hi;
                    while ((s = dis.readLine()) != null) {
                        if (s.contains("?wsdl")) {
                            fpos = s.indexOf("\"") + 1;
                            lpos = s.indexOf("?");
                            s = s.substring(fpos, lpos);
                            if (s.startsWith("http")) s = s.substring(7);
                            lslash = s.lastIndexOf('/');
                            sn = s.substring(lslash + 1);
                            hi = s.substring(0, lslash);
                            hi = hi.replace('/', '_');
                            if (!sn.equals("Version") && !sn.equals("AdminService")) {
                                ExampleServiceDesc service = new ExampleServiceDesc();
                                s = sn + "_from_" + hi;
                                service.setExampleString(s);
                                service.setExampleUri(URI.create(url));
                                results.add(service);
                            }
                        }
                    }
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            }
            callBack.partialResults(results);
            callBack.finished();
        }
    }

    /**
	 * Icon for service provider
	 */
    public Icon getIcon() {
        return ExampleServiceIcon.getIcon();
    }

    /**
	 * Name of service provider, appears in right click for 'Remove service
	 * provider'
	 */
    public String getName() {
        return "Opal Web Service URL";
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getId() {
        return providerId.toASCIIString();
    }

    @Override
    protected List<? extends Object> getIdentifyingData() {
        return Arrays.asList(getConfiguration().getUri());
    }

    public ExampleServiceProvider() {
        super(new ExampleServiceProviderConfig());
    }
}
