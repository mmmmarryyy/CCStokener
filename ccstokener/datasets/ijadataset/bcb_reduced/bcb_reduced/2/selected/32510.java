package cardwall.server.xplanner;

import dk.netl.www.xplanner.soap.XPlanner.XPlanner;
import dk.netl.www.xplanner.soap.XPlanner.XPlannerServiceLocator;
import dk.netl.www.xplanner.soap.XPlanner.XPlannerSoapBindingStub;
import junit.framework.TestCase;
import org.apache.axis.encoding.Base64;
import org.xplanner.soap.ProjectData;
import javax.xml.rpc.ServiceException;
import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author $LastChangedBy: vogensen $
 * @version $Revision: 2 $
 */
public class XplannerTest extends TestCase {

    public void testAuthorization() {
        try {
            Authenticator.setDefault(new Authenticator() {

                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("sbv@lakeside.dk", "sbv".toCharArray());
                }
            });
            URL url = new URL("http://netl.dk/xplanner/");
            URLConnection urlConnection = url.openConnection();
            System.out.println(urlConnection.getHeaderFields());
            urlConnection.connect();
            InputStreamReader ReadIn = new InputStreamReader(urlConnection.getInputStream());
            BufferedReader BufData = new BufferedReader(ReadIn);
            String UrlData = null;
            while ((UrlData = BufData.readLine()) != null) {
                System.out.println(UrlData);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testGetProjects() {
        XPlannerServiceLocator locator = new XPlannerServiceLocator();
        locator.setXPlannerEndpointAddress("http://netl.dk/xplanner/soap/XPlanner");
        XPlanner xPlanner;
        try {
            xPlanner = locator.getXPlanner();
        } catch (ServiceException e) {
            throw new RuntimeException("cannot get xplanner web service", e);
        }
        XPlannerSoapBindingStub stub = (XPlannerSoapBindingStub) xPlanner;
        stub.setUsername("sbv");
        stub.setPassword("sbv123");
        String authString = "sbv@lakeside.dk" + ":" + "sbv";
        String authorization = Base64.encode((authString).getBytes());
        stub.setHeader(null, "Authorization", authorization);
        List<ProjectData> projects = new ArrayList<ProjectData>();
        try {
            ProjectData[] array = xPlanner.getProjects();
            for (ProjectData element : array) {
                projects.add(element);
            }
        } catch (RemoteException e) {
            throw new RuntimeException("cannot find all projects", e);
        }
    }
}
