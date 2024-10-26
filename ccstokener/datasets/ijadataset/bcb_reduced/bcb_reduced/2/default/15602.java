import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.After;
import org.junit.Before;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.persistent.appfabric.acs.ACSTokenProvider;
import com.persistent.appfabric.acs.Credentials;
import com.persistent.appfabric.acs.Credentials.TOKEN_TYPE;
import com.persistent.appfabric.common.DotNetServicesEnvironment;
import com.persistent.appfabric.common.LoggerUtil;
import com.persistent.appfabric.sample.salesdataservice.AtomParserForTestCase;
import com.persistent.appfabric.sample.salesdataservice.dao.impl.SalesOrderDaoImpl;
import com.persistent.appfabric.sample.salesdataservice.TestSalesOrderService;
import com.persistent.appfabric.sample.salesdataservice.util.DBManager;

public class TestSalesOrderData extends TestCase {

    private static DBManager dbManager = new DBManager();

    Connection connection = null;

    Statement statement = null;

    ResultSet resultSet = null;

    SalesOrderDaoImpl salesDao = null;

    private Locale locale = new Locale("en", "US");

    private ResourceBundle rb = ResourceBundle.getBundle("config/sdkprop", locale);

    private String solutionName = null;

    private String serviceName = null;

    private String acmHostName = null;

    private String httpWebProxyServer = null;

    private int httpWebProxyPort = 0;

    public TestSalesOrderData(String name) {
        super(name);
    }

    @Before
    public void setUp() throws Exception {
        connection = dbManager.connect();
        statement = connection.createStatement();
        salesDao = new SalesOrderDaoImpl();
        solutionName = rb.getString("SIMPLE_SOLUTION_NAME");
        serviceName = rb.getString("SIMPLE_SERVICE_NAME");
        acmHostName = DotNetServicesEnvironment.getACMHostName();
        try {
            httpWebProxyServer = rb.getString("HTTP_PROXY_SERVER_NAME");
            httpWebProxyPort = Integer.parseInt(rb.getString("HTTP_PROXY_PORT"));
        } catch (Exception e) {
            httpWebProxyServer = null;
            httpWebProxyPort = 0;
        }
    }

    @After
    public void tearDown() throws Exception {
        connection = dbManager.connect();
        statement = connection.createStatement();
        salesDao = new SalesOrderDaoImpl();
    }

    /**
	 * This method will test for SalesOrderData service. This will test the
	 * sales data per sales person. If data is not retrieved successfully an
	 * error message will be displayed.
	 * 
	 * @throws IOException
	 *             , HttpException
	 * 
	 */
    @org.junit.Test
    public void checkGetSalesOrder() throws IOException, SQLException, Exception {
        Long salesPersonId = new Long(1);
        Long productId = new Long(1);
        String query = "SELECT c.salesOrderId, b.productId, d.regionname ,b.productname, b.cost ,c.orderQty , c.orderDate FROM salesperson a, product b , salesorder c , region d WHERE b.productId = c.productId and a.salesPersonId = c.salesPersonId  and a.regionId = d.regionId " + "and a.salesPersonId =" + salesPersonId + " and b.productId =" + productId;
        resultSet = statement.executeQuery(query);
        List<TestSalesOrderService> testSalesOrder = populateTestSalesOrderObject(resultSet);
        StringBuilder queryString = new StringBuilder();
        queryString.append("/salesPersonId/" + salesPersonId);
        queryString.append("/productId/" + productId);
        String urlForSalesData = "http://localhost:8080/SalesOrderService/SalesOrder/SalesOrderData";
        urlForSalesData = urlForSalesData + queryString;
        String responseString = null;
        responseString = getScenarioData(urlForSalesData);
        List<TestSalesOrderService> actualSalesOrderList = getSalesOrderInfo(responseString);
        for (int i = 0; i < testSalesOrder.size(); i++) {
            TestSalesOrderService s1 = testSalesOrder.get(i);
            TestSalesOrderService s2 = actualSalesOrderList.get(i);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date strDate1 = dateFormat.parse(s2.getOrderDate());
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            String strDate = df.format(strDate1);
            assertEquals(s1.getProductName(), s2.getProductName());
            assertEquals(s1.getOrderDate(), strDate);
            assertEquals(s1.getOrderQuantity(), s2.getOrderQuantity());
            assertEquals(s1.getRegionName(), s2.getRegionName());
            assertEquals(s1.getProductID(), s2.getProductID());
            assertEquals(s1.getSalesOrderID(), s2.getSalesOrderID());
            assertEquals(s1.getCost(), s2.getCost());
        }
    }

    /**
	 * This method will test sales data for all sales persons for SalesOrderData
	 * Service. If data is not retrieved successfully an error message will be
	 * displayed.
	 * 
	 * @throws IOException
	 *             , HttpException
	 * 
	 */
    @org.junit.Test
    public void checkGetSalesOrders() throws IOException, SQLException, Exception {
        Long salesPersonId = new Long(1);
        Long productId = new Long(1);
        Long regionId = new Long(1);
        String query = "SELECT c.salesOrderId, b.productId, d.regionname ,b.productname, b.cost ,c.orderQty , c.orderDate FROM salesperson a, product b , salesorder c , region d WHERE b.productId = c.productId and a.salesPersonId = c.salesPersonId  and a.regionId = d.regionId " + "and b.productId =" + productId + " and a.regionId =" + regionId + " ORDER BY c.salesOrderId";
        resultSet = statement.executeQuery(query);
        List<TestSalesOrderService> testSalesOrder = populateTestSalesOrderObject(resultSet);
        StringBuilder queryString = new StringBuilder();
        queryString.append("/salesPersonId/" + salesPersonId);
        queryString.append("/productId/" + productId);
        queryString.append("/regionId/" + regionId);
        String urlForSalesData = "http://localhost:8080/SalesOrderService/SalesOrder/SalesOrderData";
        urlForSalesData = urlForSalesData + queryString;
        String responseString = null;
        responseString = getScenarioData(urlForSalesData);
        List<TestSalesOrderService> actualSalesOrderList = getSalesOrderInfo(responseString);
        for (int i = 0; i < testSalesOrder.size(); i++) {
            TestSalesOrderService s1 = testSalesOrder.get(i);
            TestSalesOrderService s2 = actualSalesOrderList.get(i);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date strDate1 = dateFormat.parse(s2.getOrderDate());
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            String strDate = df.format(strDate1);
            assertEquals(s1.getProductName(), s2.getProductName());
            assertEquals(s1.getOrderDate(), strDate);
            assertEquals(s1.getOrderQuantity(), s2.getOrderQuantity());
            assertEquals(s1.getRegionName(), s2.getRegionName());
            assertEquals(s1.getProductID(), s2.getProductID());
            assertEquals(s1.getSalesOrderID(), s2.getSalesOrderID());
            assertEquals(s1.getCost(), s2.getCost());
        }
    }

    /**
	 * This method will test CompanySales Service. It checks data sales data
	 * retrieved for all companies. If data is not retrieved successfully an
	 * error message will be displayed.
	 * 
	 * @throws IOException
	 *             , HttpException
	 * 
	 */
    @org.junit.Test
    public void checkGetCompanySales() throws IOException, SQLException, Exception {
        String fromYear = "2009";
        String toYear = "2010";
        String productId = "2";
        String query = "select sum(so.OrderQty) as Quantity,date_format(so.OrderDate,'%M') as Month,date_format(so.OrderDate,'%Y') as Year,so.productid  from salesorder so where " + "date_format(so.OrderDate,'%Y') between " + fromYear + " and " + toYear + " group by date_format(so.OrderDate,'%Y')";
        resultSet = statement.executeQuery(query);
        List<TestSalesOrderService> expectedCompanySalesList = new ArrayList<TestSalesOrderService>();
        while (resultSet.next()) {
            TestSalesOrderService companysalesDTO = new TestSalesOrderService();
            companysalesDTO.setOrderQuantity(resultSet.getLong("Quantity"));
            companysalesDTO.setMonth(resultSet.getString("Month"));
            companysalesDTO.setYear(resultSet.getString("Year"));
            expectedCompanySalesList.add(companysalesDTO);
        }
        StringBuilder queryString = new StringBuilder();
        queryString.append("/fromYear/" + fromYear);
        queryString.append("/toYear/" + toYear);
        String urlForSalesData = "http://localhost:8080/SalesOrderService/SalesOrder/CompanySales";
        urlForSalesData = urlForSalesData + queryString;
        String responseString = null;
        responseString = getScenarioData(urlForSalesData);
        List<TestSalesOrderService> actualCompanySalesList = getSalesOrderInfo(responseString);
        for (int i = 0; i < expectedCompanySalesList.size(); i++) {
            TestSalesOrderService s1 = expectedCompanySalesList.get(i);
            TestSalesOrderService s2 = actualCompanySalesList.get(i);
            assertEquals(s1.getOrderQuantity(), s2.getOrderQuantity());
            assertEquals(s1.getMonth(), s2.getMonth());
            assertEquals(s1.getYear(), s2.getYear());
        }
    }

    /**
	 * This method will test Region service. It checks the list of Regions
	 * retrieved from the database by the service. If data is not retrieved
	 * successfully an error message will be displayed.
	 * 
	 * @throws IOException
	 *             , HttpException
	 */
    @org.junit.Test
    public void checkRegions() throws IOException, SQLException, Exception {
        String query = "SELECT * FROM Region Order By RegionID";
        resultSet = statement.executeQuery(query);
        List<TestSalesOrderService> expectedRegionList = new ArrayList<TestSalesOrderService>();
        while (resultSet.next()) {
            TestSalesOrderService regionBean = new TestSalesOrderService();
            regionBean.setRegionID(resultSet.getString("regionId"));
            regionBean.setRegionName(resultSet.getString("RegionName"));
            expectedRegionList.add(regionBean);
        }
        StringBuilder queryString = new StringBuilder();
        String urlForSalesData = "http://localhost:8080/SalesOrderService/SalesOrder/Region";
        urlForSalesData = urlForSalesData + queryString;
        String responseString = null;
        responseString = getScenarioData(urlForSalesData);
        List<TestSalesOrderService> actualRegionList = getSalesOrderInfo(responseString);
        for (int i = 0; i < expectedRegionList.size(); i++) {
            TestSalesOrderService s1 = expectedRegionList.get(i);
            TestSalesOrderService s2 = actualRegionList.get(i);
            assertEquals(s1.getRegionID(), s2.getRegionID());
            assertEquals(s1.getRegionName(), s2.getRegionName());
        }
    }

    /**
	 * This method will test Product service. It checks the list of products
	 * retrieved from the database by the service. If data is not retrieved
	 * successfully an error message will be displayed.
	 * 
	 * @throws IOException
	 *             , HttpException
	 * 
	 */
    @org.junit.Test
    public void checkProducts() throws IOException, SQLException, Exception {
        String query = "SELECT * FROM Product";
        resultSet = statement.executeQuery(query);
        List<TestSalesOrderService> expectedProductList = new ArrayList<TestSalesOrderService>();
        while (resultSet.next()) {
            TestSalesOrderService regionBean = new TestSalesOrderService();
            regionBean.setProductID(resultSet.getString("productID"));
            regionBean.setProductName(resultSet.getString("ProductName"));
            expectedProductList.add(regionBean);
        }
        StringBuilder queryString = new StringBuilder();
        String urlForSalesData = "http://localhost:8080/SalesOrderService/SalesOrder/Product";
        urlForSalesData = urlForSalesData + queryString;
        String responseString = null;
        responseString = getScenarioData(urlForSalesData);
        List<TestSalesOrderService> actualProductList = getSalesOrderInfo(responseString);
        for (int i = 0; i < expectedProductList.size(); i++) {
            TestSalesOrderService s1 = expectedProductList.get(i);
            TestSalesOrderService s2 = actualProductList.get(i);
            assertEquals(s1.getProductID(), s2.getProductID());
            assertEquals(s1.getProductName(), s2.getProductName());
        }
    }

    /**
	 * This method will create salesorders. It checks whether the sales order is
	 * successfully created or not.
	 * 
	 */
    @org.junit.Test
    public void checkCreateSalesOrder() throws Exception {
        long salesPersonId = 11;
        long productId = 12;
        long orderQty = 15;
        String urlForSalesData = "http://localhost:8080/SalesOrderService/SalesOrder/SalesOrderData";
        DataOutputStream outputStream = null;
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        DataInputStream input = null;
        StringBuffer sBuf = new StringBuffer();
        URL url = null;
        try {
            url = new URL(urlForSalesData);
            HttpURLConnection httpUrlConnection;
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setRequestMethod("PUT");
            httpUrlConnection.setUseCaches(false);
            httpUrlConnection.setDoInput(true);
            httpUrlConnection.setDoOutput(true);
            httpUrlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpUrlConnection.setRequestProperty("Content-Language", "en-US");
            httpUrlConnection.setRequestProperty("Accept", "*/*");
            httpUrlConnection.setRequestProperty("Authorization", "Required");
            String name = rb.getString("WRAP_NAME");
            String password = rb.getString("WRAP_PASSWORD");
            Credentials simpleAuthCrentials = new Credentials(TOKEN_TYPE.SimpleApiAuthToken, name, password);
            ACSTokenProvider tokenProvider = new ACSTokenProvider(httpWebProxyServer, httpWebProxyPort, simpleAuthCrentials);
            String requestUriStr1 = "https://" + solutionName + "." + acmHostName + "/" + serviceName;
            String appliesTo1 = rb.getString("SIMPLEAPI_APPLIES_TO");
            String token = tokenProvider.getACSToken(requestUriStr1, appliesTo1);
            httpUrlConnection.addRequestProperty("token", "WRAPv0.9 " + token);
            httpUrlConnection.addRequestProperty("solutionName", solutionName);
            StringBuilder postData = new StringBuilder();
            postData.append("productId=" + URLEncoder.encode(Long.toString(productId), "UTF-8"));
            postData.append("&");
            postData.append("orderQty=" + URLEncoder.encode(Long.toString(orderQty), "UTF-8"));
            postData.append("&");
            postData.append("salesPersonId=" + URLEncoder.encode(Long.toString(salesPersonId), "UTF-8"));
            outputStream = new DataOutputStream(httpUrlConnection.getOutputStream());
            outputStream.writeBytes(postData.toString());
            outputStream.flush();
            inputStream = httpUrlConnection.getInputStream();
            if (httpUrlConnection.getResponseCode() == HttpServletResponse.SC_UNAUTHORIZED) {
                return;
            }
            input = new DataInputStream(inputStream);
            bufferedReader = new BufferedReader(new InputStreamReader(input));
            String str;
            while (null != ((str = bufferedReader.readLine()))) {
                sBuf.append(str);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String queryAfterInsert = "SELECT * FROM SalesOrder WHERE ProductID= " + productId + " AND OrderQty= " + orderQty + " AND SalesPersonID= " + salesPersonId;
        resultSet = statement.executeQuery(queryAfterInsert);
        List<TestSalesOrderService> expectedProductList = new ArrayList<TestSalesOrderService>();
        while (resultSet.next()) {
            TestSalesOrderService salesOrderBean = new TestSalesOrderService();
            salesOrderBean.setOrderDate(resultSet.getString("orderDate"));
            salesOrderBean.setOrderQuantity(resultSet.getLong("orderQty"));
            salesOrderBean.setRegionName(resultSet.getString("SalesPersonID"));
            salesOrderBean.setProductID(resultSet.getString("productId"));
            salesOrderBean.setSalesOrderID(resultSet.getString("salesOrderId"));
            expectedProductList.add(salesOrderBean);
        }
        if (expectedProductList.size() != 0) {
            assertNotNull("Insertion fails", expectedProductList);
        } else {
            throw new AssertionError("No Record found in the database.");
        }
    }

    /**
	 * This method will test UpdateSalesOrders service. It checks whether the
	 * sales order is successfully created or not.
	 * 
	 * @throws IOException
	 *             , HttpException
	 * 
	 */
    @org.junit.Test
    public void checkUpdateSalesOrders() throws IOException, SQLException, Exception {
        long salesPersonId = 1;
        long productId = 1;
        long orderQty = 13;
        long salesOrderId = 1;
        String query = "SELECT * FROM SalesOrder WHERE SalesOrderID= " + salesOrderId;
        resultSet = statement.executeQuery(query);
        List<TestSalesOrderService> expectedProductList = new ArrayList<TestSalesOrderService>();
        while (resultSet.next()) {
            TestSalesOrderService salesOrderBean = new TestSalesOrderService();
            salesOrderBean.setOrderQuantity(13);
            salesOrderBean.setProductID("1");
            salesOrderBean.setSalesOrderID("1");
            expectedProductList.add(salesOrderBean);
        }
        String urlForSalesData = "http://localhost:8080/SalesOrderService/SalesOrder/SalesOrderData";
        DataOutputStream outputStream = null;
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        DataInputStream input = null;
        StringBuffer sBuf = new StringBuffer();
        URL url = null;
        try {
            url = new URL(urlForSalesData);
            HttpURLConnection httpUrlConnection;
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setRequestMethod("POST");
            httpUrlConnection.setUseCaches(false);
            httpUrlConnection.setDoInput(true);
            httpUrlConnection.setDoOutput(true);
            httpUrlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpUrlConnection.setRequestProperty("Content-Language", "en-US");
            httpUrlConnection.setRequestProperty("Accept", "*/*");
            httpUrlConnection.setRequestProperty("Authorization", "Required");
            String name = rb.getString("WRAP_NAME");
            String password = rb.getString("WRAP_PASSWORD");
            Credentials simpleAuthCrentials = new Credentials(TOKEN_TYPE.SimpleApiAuthToken, name, password);
            ACSTokenProvider tokenProvider = new ACSTokenProvider(httpWebProxyServer, httpWebProxyPort, simpleAuthCrentials);
            String requestUriStr1 = "https://" + solutionName + "." + acmHostName + "/" + serviceName;
            String appliesTo1 = rb.getString("SIMPLEAPI_APPLIES_TO");
            String token = tokenProvider.getACSToken(requestUriStr1, appliesTo1);
            httpUrlConnection.addRequestProperty("token", "WRAPv0.9 " + token);
            httpUrlConnection.addRequestProperty("solutionName", solutionName);
            StringBuilder postData = new StringBuilder();
            postData.append("productId=" + URLEncoder.encode(Long.toString(productId), "UTF-8"));
            postData.append("&");
            postData.append("orderQty=" + URLEncoder.encode(Long.toString(orderQty), "UTF-8"));
            postData.append("&");
            postData.append("salesOrderId=" + URLEncoder.encode(Long.toString(salesOrderId), "UTF-8"));
            outputStream = new DataOutputStream(httpUrlConnection.getOutputStream());
            outputStream.writeBytes(postData.toString());
            outputStream.flush();
            inputStream = httpUrlConnection.getInputStream();
            if (httpUrlConnection.getResponseCode() == HttpServletResponse.SC_UNAUTHORIZED) {
                return;
            }
            input = new DataInputStream(inputStream);
            bufferedReader = new BufferedReader(new InputStreamReader(input));
            String str;
            while (null != ((str = bufferedReader.readLine()))) {
                sBuf.append(str);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String queryAfterUpdate = "SELECT * FROM SalesOrder WHERE SalesOrderID= " + salesOrderId;
        resultSet = statement.executeQuery(queryAfterUpdate);
        List<TestSalesOrderService> actualProductList = new ArrayList<TestSalesOrderService>();
        while (resultSet.next()) {
            TestSalesOrderService salesOrderBean = new TestSalesOrderService();
            salesOrderBean.setOrderQuantity(resultSet.getLong("orderQty"));
            salesOrderBean.setProductID(resultSet.getString("productId"));
            salesOrderBean.setSalesOrderID(resultSet.getString("salesOrderId"));
            actualProductList.add(salesOrderBean);
        }
        for (int i = 0; i < expectedProductList.size(); i++) {
            TestSalesOrderService s1 = expectedProductList.get(i);
            TestSalesOrderService s2 = actualProductList.get(i);
            assertSame(s1.getOrderQuantity(), s2.getOrderQuantity());
            assertTrue(s1.getProductID().equalsIgnoreCase(s2.getProductID()));
        }
    }

    /**
	 * This method will parse the XML input in the form of string and get the
	 * node values of the XML and set it in SalesOrder bean.
	 * 
	 * @param fileName
	 *            XML string to be parsed
	 * @return List<TestSalesOrderService> list of TestSalesOrderServices
	 * 
	 */
    public List<TestSalesOrderService> getSalesOrderInfo(String responseString) throws Exception {
        AtomParserForTestCase atomParser = new AtomParserForTestCase();
        List<TestSalesOrderService> salesOrderServiceBean = new ArrayList<TestSalesOrderService>();
        salesOrderServiceBean = atomParser.parseString(responseString);
        return salesOrderServiceBean;
    }

    /**
	 * This method will populate the list with the data from the result set.
	 * 
	 * @throws IOException
	 * 
	 * @return List<TestSalesOrderService> list of TestSalesOrderServices
	 */
    private List<TestSalesOrderService> populateTestSalesOrderObject(ResultSet resultSet) throws SQLException {
        List<TestSalesOrderService> reList = new ArrayList<TestSalesOrderService>();
        while (resultSet.next()) {
            TestSalesOrderService salesOrderBean = new TestSalesOrderService();
            salesOrderBean.setProductName(resultSet.getString("productname"));
            salesOrderBean.setOrderDate(resultSet.getString("orderDate"));
            salesOrderBean.setOrderQuantity(resultSet.getLong("orderQty"));
            salesOrderBean.setRegionName(resultSet.getString("regionname"));
            salesOrderBean.setProductID(resultSet.getString("productId"));
            salesOrderBean.setSalesOrderID(resultSet.getString("salesOrderId"));
            salesOrderBean.setCost(resultSet.getString("cost"));
            reList.add(salesOrderBean);
        }
        return reList;
    }

    /**
	 * This method will convert an XML node element to string.
	 * 
	 * @param ale
	 *            node element
	 * @param tagName
	 *            name of the node to be converted into String
	 *@return String
	 * 
	 */
    public String getTextValue(Element ele, String tagName) {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            Element el = (Element) nl.item(0);
            textVal = el.getFirstChild().getNodeValue();
        }
        return textVal;
    }

    public String getScenarioData(String urlForSalesData) throws IOException, Exception {
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        DataInputStream input = null;
        StringBuffer sBuf = new StringBuffer();
        Proxy proxy;
        if (httpWebProxyServer != null && Integer.toString(httpWebProxyPort) != null) {
            SocketAddress address = new InetSocketAddress(httpWebProxyServer, httpWebProxyPort);
            proxy = new Proxy(Proxy.Type.HTTP, address);
        } else {
            proxy = null;
        }
        proxy = null;
        URL url;
        try {
            url = new URL(urlForSalesData);
            HttpURLConnection httpUrlConnection;
            if (proxy != null) httpUrlConnection = (HttpURLConnection) url.openConnection(proxy); else httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setDoInput(true);
            httpUrlConnection.setRequestMethod("GET");
            String name = rb.getString("WRAP_NAME");
            String password = rb.getString("WRAP_PASSWORD");
            Credentials simpleAuthCrentials = new Credentials(TOKEN_TYPE.SimpleApiAuthToken, name, password);
            ACSTokenProvider tokenProvider = new ACSTokenProvider(httpWebProxyServer, httpWebProxyPort, simpleAuthCrentials);
            String requestUriStr1 = "https://" + solutionName + "." + acmHostName + "/" + serviceName;
            String appliesTo1 = rb.getString("SIMPLEAPI_APPLIES_TO");
            String token = tokenProvider.getACSToken(requestUriStr1, appliesTo1);
            httpUrlConnection.addRequestProperty("token", "WRAPv0.9 " + token);
            httpUrlConnection.addRequestProperty("solutionName", solutionName);
            httpUrlConnection.connect();
            if (httpUrlConnection.getResponseCode() == HttpServletResponse.SC_UNAUTHORIZED) {
                List<TestSalesOrderService> salesOrderServiceBean = new ArrayList<TestSalesOrderService>();
                TestSalesOrderService response = new TestSalesOrderService();
                response.setResponseCode(HttpServletResponse.SC_UNAUTHORIZED);
                response.setResponseMessage(httpUrlConnection.getResponseMessage());
                salesOrderServiceBean.add(response);
            }
            inputStream = httpUrlConnection.getInputStream();
            input = new DataInputStream(inputStream);
            bufferedReader = new BufferedReader(new InputStreamReader(input));
            String str;
            while (null != ((str = bufferedReader.readLine()))) {
                sBuf.append(str);
            }
            String responseString = sBuf.toString();
            return responseString;
        } catch (MalformedURLException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
    }

    /**
	 * This method will convert an XML node element to integer.
	 * 
	 * @param ale
	 *            node element
	 * @param tagName
	 *            name of the node to be converted into integer value
	 * @return long
	 */
    public long getIntValue(Element ele, String tagName) {
        return Long.parseLong(getTextValue(ele, tagName));
    }

    public static Test suite() throws IOException {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSalesOrderData("checkRegions"));
        suite.addTest(new TestSalesOrderData("checkProducts"));
        suite.addTest(new TestSalesOrderData("checkGetSalesOrder"));
        suite.addTest(new TestSalesOrderData("checkGetSalesOrders"));
        suite.addTest(new TestSalesOrderData("checkGetCompanySales"));
        suite.addTest(new TestSalesOrderData("checkCreateSalesOrder"));
        suite.addTest(new TestSalesOrderData("checkUpdateSalesOrders"));
        return suite;
    }
}
