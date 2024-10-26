package com.sebulli.fakturama.webshopimport;

import static com.sebulli.fakturama.Translate._;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.sebulli.fakturama.Activator;
import com.sebulli.fakturama.Workspace;
import com.sebulli.fakturama.actions.MarkOrderAsAction;
import com.sebulli.fakturama.data.Data;
import com.sebulli.fakturama.data.DataSetContact;
import com.sebulli.fakturama.data.DataSetDocument;
import com.sebulli.fakturama.data.DataSetItem;
import com.sebulli.fakturama.data.DataSetPayment;
import com.sebulli.fakturama.data.DataSetProduct;
import com.sebulli.fakturama.data.DataSetShipping;
import com.sebulli.fakturama.data.DataSetVAT;
import com.sebulli.fakturama.data.UniDataSet;
import com.sebulli.fakturama.editors.ProductEditor;
import com.sebulli.fakturama.logger.Logger;
import com.sebulli.fakturama.misc.DataUtils;
import com.sebulli.fakturama.misc.DocumentType;

/**
 * Web shop import manager This class provides the functionality to connect to
 * the web shop and import the data, which is transmitted as a XML File
 * 
 * @author Gerd Bartelt
 * 
 */
public class WebShopImportManager extends Thread implements IRunnableWithProgress {

    /**
	 * Runs the reading of a http stream in an extra thread.
	 * So it can be interrupted by clicking the cancel button. 
	 * 
	 * @author Gerd Bartelt
	 */
    public class InterruptConnection implements Runnable {

        private URLConnection conn;

        private InputStream inputStream = null;

        private boolean isFinished = false;

        private boolean isError = false;

        /**
	     * Constructor. Creates a new connection to use it in an extra thread
	     * 
	     * @param conn
	     * 			The connection
	     */
        public InterruptConnection(URLConnection conn) {
            this.conn = conn;
        }

        /**
	     * Return whether the reading was successful
	     * 
	     * @return
	     * 		True, if the stream was read completely
	     */
        public boolean isFinished() {
            return isFinished;
        }

        /**
	     * Return whether the was an error
	     * 
	     * @return
	     * 		True, if there was an error
	     */
        public boolean isError() {
            return isError;
        }

        /**
	     * Returns a reference to the input stream
	     * 
	     * @return
	     * 		Reference to the input stream
	     */
        public InputStream getInputStream() {
            return inputStream;
        }

        /**
	     * Start reading the input stream 
	     */
        public void run() {
            try {
                inputStream = conn.getInputStream();
                isFinished = true;
            } catch (IOException e) {
                isError = true;
            }
        }
    }

    private DocumentBuilderFactory factory = null;

    private DocumentBuilder builder = null;

    private Document document = null;

    private String importXMLContent = "";

    private static Properties orderstosynchronize = null;

    private String runResult = "";

    private String shopURL = "";

    private String productImagePath = "";

    private IProgressMonitor monitor;

    private int worked;

    private boolean getProducts;

    private boolean getOrders;

    private Boolean useEANasItemNr = false;

    /**
	 * Sets the progress of the job in percent
	 * 
	 * @param percent
	 */
    void setProgress(int percent) {
        if (percent > worked) {
            monitor.worked(percent - worked);
            worked = percent;
        }
    }

    /**
	 * Prepare the web shop import to request products and orders.
	 */
    public void prepareGetProductsAndOrders() {
        getProducts = true;
        getOrders = true;
    }

    /**
	 * Prepare the web shop import to change the state of an order.
	 */
    public void prepareChangeState() {
        getProducts = false;
        getOrders = false;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        this.monitor = monitor;
        runResult = "";
        String address = Activator.getDefault().getPreferenceStore().getString("WEBSHOP_URL");
        String user = Activator.getDefault().getPreferenceStore().getString("WEBSHOP_USER");
        String password = Activator.getDefault().getPreferenceStore().getString("WEBSHOP_PASSWORD");
        Integer maxProducts = Activator.getDefault().getPreferenceStore().getInt("WEBSHOP_MAX_PRODUCTS");
        Boolean onlyModifiedProducts = Activator.getDefault().getPreferenceStore().getBoolean("WEBSHOP_ONLY_MODIFIED_PRODUCTS");
        useEANasItemNr = Activator.getDefault().getPreferenceStore().getBoolean("WEBSHOP_USE_EAN_AS_ITEMNR");
        Boolean useAuthorization = Activator.getDefault().getPreferenceStore().getBoolean("WEBSHOP_AUTHORIZATION_ENABLED");
        String authorizationUser = Activator.getDefault().getPreferenceStore().getString("WEBSHOP_AUTHORIZATION_USER");
        String authorizationPassword = Activator.getDefault().getPreferenceStore().getString("WEBSHOP_AUTHORIZATION_PASSWORD");
        if (address.isEmpty()) {
            runResult = _("Web shop URL is not set.");
            return;
        }
        if (!address.toLowerCase().startsWith("http://") && !address.toLowerCase().startsWith("https://") && !address.toLowerCase().startsWith("file://")) address = "http://" + address;
        readOrdersToSynchronize();
        factory = DocumentBuilderFactory.newInstance();
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        try {
            worked = 0;
            URLConnection conn = null;
            monitor.beginTask(_("Connection to web shop"), 100);
            monitor.subTask(_("Connected to:") + " " + address);
            setProgress(10);
            URL url = new URL(address);
            conn = url.openConnection();
            conn.setDoInput(true);
            conn.setConnectTimeout(4000);
            if (!address.toLowerCase().startsWith("file://")) {
                conn.setDoOutput(true);
                if (useAuthorization) {
                    String encodedPassword = Base64Coder.encodeString(authorizationUser + ":" + authorizationPassword);
                    conn.setRequestProperty("Authorization", "Basic " + encodedPassword);
                }
                OutputStream outputStream = null;
                outputStream = conn.getOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                setProgress(20);
                String postString = "username=" + URLEncoder.encode(user, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8");
                String actionString = "";
                if (getProducts) actionString += "_products";
                if (getOrders) actionString += "_orders";
                if (!actionString.isEmpty()) actionString = "&action=get" + actionString;
                postString += actionString;
                postString += "&setstate=" + orderstosynchronize.toString();
                if (maxProducts > 0) {
                    postString += "&maxproducts=" + maxProducts.toString();
                }
                if (onlyModifiedProducts) {
                    String lasttime = Data.INSTANCE.getProperty("lastwebshopimport", "");
                    if (!lasttime.isEmpty()) postString += "&lasttime=" + lasttime.toString();
                }
                writer.write(postString);
                writer.flush();
                writer.close();
            }
            String line;
            setProgress(30);
            importXMLContent = "";
            InterruptConnection interruptConnection = new InterruptConnection(conn);
            new Thread(interruptConnection).start();
            while (!monitor.isCanceled() && !interruptConnection.isFinished() && !interruptConnection.isError()) ;
            if (!interruptConnection.isFinished()) {
                ((HttpURLConnection) conn).disconnect();
                if (interruptConnection.isError()) {
                    runResult = _("Error while connecting to webserver.");
                }
                return;
            }
            if (interruptConnection.isError()) {
                ((HttpURLConnection) conn).disconnect();
                runResult = _("Error reading web shop data.");
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(interruptConnection.getInputStream()));
            monitor.subTask(_("Loading Data"));
            double progress = worked;
            String filename = Activator.getDefault().getPreferenceStore().getString("GENERAL_WORKSPACE");
            File logFile = null;
            BufferedWriter bos = null;
            if (!filename.isEmpty()) {
                filename += "/Log/";
                File directory = new File(filename);
                if (!directory.exists()) directory.mkdirs();
                filename += "WebShopImport.log";
                logFile = new File(filename);
                if (logFile.exists()) logFile.delete();
                bos = new BufferedWriter(new FileWriter(logFile, true));
            }
            StringBuffer sb = new StringBuffer();
            while (((line = reader.readLine()) != null) && (!monitor.isCanceled())) {
                sb.append(line);
                sb.append("\n");
                progress += (50 - progress) * 0.01;
                setProgress((int) progress);
            }
            importXMLContent = sb.toString();
            if (bos != null) bos.write(importXMLContent);
            if (bos != null) bos.close();
            if (!monitor.isCanceled()) {
                if (!importXMLContent.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")) {
                    runResult = _("No webshop data:") + "\n" + address + importXMLContent;
                    return;
                }
                ByteArrayInputStream importInputStream = new ByteArrayInputStream(importXMLContent.getBytes());
                document = builder.parse(importInputStream);
                NodeList ndList = document.getElementsByTagName("webshopexport");
                if (ndList.getLength() != 0) {
                    orderstosynchronize = new Properties();
                } else {
                    runResult = importXMLContent;
                }
                ndList = document.getElementsByTagName("error");
                if (ndList.getLength() > 0) {
                    runResult = ndList.item(0).getTextContent();
                }
            } else {
            }
            reader.close();
            if (runResult.isEmpty()) interpretWebShopData(monitor);
            String now = DataUtils.DateAsISO8601String();
            Data.INSTANCE.setProperty("lastwebshopimport", now);
            monitor.done();
        } catch (SAXException e) {
            runResult = "Error parsing XML content:\n" + e.getLocalizedMessage() + "\n" + importXMLContent;
        } catch (Exception e) {
            runResult = _("Error opening:") + "\n" + address + "\n";
            runResult += "Message:" + e.getLocalizedMessage() + "\n";
            if (e.getStackTrace().length > 0) runResult += "Trace:" + e.getStackTrace()[0].toString() + "\n";
            if (!importXMLContent.isEmpty()) runResult += "\n\n" + importXMLContent;
        }
    }

    /**
	 * Remove the HTML tags from the result
	 * 
	 * @return The formated run result string
	 */
    public String getRunResult() {
        return runResult.replaceAll("\\<.*?\\>", "");
    }

    /**
	 * Read the list of all orders, which are out of sync with the web shop from
	 * the file system
	 * 
	 */
    public static void readOrdersToSynchronize() {
        Reader reader = null;
        orderstosynchronize = new Properties();
        try {
            reader = new FileReader(Activator.getDefault().getPreferenceStore().getString("GENERAL_WORKSPACE") + "/orders2sync.txt");
            orderstosynchronize.load(reader);
        } catch (IOException e) {
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * Save the list of all orders, which are out of sync with the web shop to
	 * file system
	 * 
	 */
    public static void saveOrdersToSynchronize() {
        Writer writer = null;
        if (orderstosynchronize.isEmpty()) return;
        try {
            writer = new FileWriter(Activator.getDefault().getPreferenceStore().getString("GENERAL_WORKSPACE") + "/orders2sync.txt");
            orderstosynchronize.store(writer, "OrdersNotInSyncWithWebshop");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * Update the progress of an order
	 * 
	 * @param uds
	 *            The UniDataSet with the new progress value
	 */
    public static void updateOrderProgress(UniDataSet uds, String comment, boolean notify) {
        int orderId = uds.getIntValueByKey("webshopid");
        int progress = uds.getIntValueByKey("progress");
        int webshopState;
        readOrdersToSynchronize();
        if (progress >= MarkOrderAsAction.SHIPPED) webshopState = 3; else if (progress >= MarkOrderAsAction.PROCESSING) webshopState = 2; else webshopState = 1;
        String value = Integer.toString(webshopState);
        comment = comment.replace("%2C", "%26comma%3B");
        comment = comment.replace("%3D", "%26equal%3B");
        if (notify) value += "*" + comment;
        orderstosynchronize.setProperty(Integer.toString(orderId), value);
        saveOrdersToSynchronize();
    }

    /**
	 * Mark all orders as "in sync" with the web shop
	 */
    public static void allOrdersAreInSync() {
        orderstosynchronize = new Properties();
        File f = new File(Activator.getDefault().getPreferenceStore().getString("GENERAL_WORKSPACE") + "/orders2sync.txt");
        f.delete();
    }

    /**
	 * Get an attribute's value and return an empty string, if the attribute is
	 * not specified
	 * 
	 * @param attributes
	 *            Attributes node
	 * @param name
	 *            Name of the attribute
	 * @return Attributes value
	 */
    private static String getAttributeAsString(NamedNodeMap attributes, String name) {
        Attr attribute;
        String value = "";
        attribute = (Attr) attributes.getNamedItem(name);
        if (attribute != null) {
            value = attribute.getValue();
        }
        return value;
    }

    /**
	 * Get an attribute's value and return -1 if the attribute is
	 * not specified
	 * 
	 * @param attributes
	 *            Attributes node
	 * @param name
	 *            Name of the attribute
	 * @return Attributes value
	 */
    private static int getAttributeAsID(NamedNodeMap attributes, String name) {
        int id = -1;
        String s = getAttributeAsString(attributes, name);
        try {
            if (!s.isEmpty()) {
                id = Integer.valueOf(s);
            }
        } catch (Exception e) {
        }
        return id;
    }

    /**
	 * Returns the text of a specified child node.
	 * 
	 * @param parentNode
	 *            The parent node.
	 * @param name
	 *            Name of the child
	 * @return The text, or an empty string
	 */
    private static String getChildTextAsString(Node parentNode, String name) {
        String retVal = "";
        for (int index = 0; index < parentNode.getChildNodes().getLength(); index++) {
            Node child = parentNode.getChildNodes().item(index);
            if (child.getNodeName().equals(name)) retVal = child.getTextContent();
        }
        return retVal;
    }

    /**
	 * Convert the payment method to a readable (and localized) text.
	 * 
	 * @param intext
	 *            order status
	 * @return payment method as readable (and localized) text
	 */
    public String getPaymentMethodText(String intext) {
        String paymentstatustext = intext;
        if (intext.equalsIgnoreCase("cod")) paymentstatustext = _("Cash_on_Delivery"); else if (intext.equalsIgnoreCase("prepayment")) paymentstatustext = _("Prepayment"); else if (intext.equalsIgnoreCase("creditcard")) paymentstatustext = _("Credit_Card"); else if (intext.equalsIgnoreCase("check")) paymentstatustext = _("Check");
        return paymentstatustext;
    }

    /**
	 * Parse an XML node and create a new product for each product entry
	 * 
	 * @param productNode
	 *            The node with the products to import
	 */
    public void createProductFromXMLOrderNode(Node productNode) {
        String productModel;
        String productName;
        String productCategory;
        String productNet;
        String productGross;
        String productVatPercent;
        String productVatName;
        String productDescription;
        String productImage;
        String pictureName;
        String productQuantity;
        String productEAN;
        String productQUnit;
        int productID;
        NamedNodeMap attributes = productNode.getAttributes();
        productNet = getAttributeAsString(attributes, "net");
        productGross = getAttributeAsString(attributes, "gross");
        productVatPercent = getAttributeAsString(attributes, "vatpercent");
        productQuantity = getAttributeAsString(attributes, "quantity");
        productID = getAttributeAsID(attributes, "id");
        productModel = getChildTextAsString(productNode, "model");
        productName = getChildTextAsString(productNode, "name");
        productCategory = getChildTextAsString(productNode, "category");
        productVatName = getChildTextAsString(productNode, "vatname");
        productImage = getChildTextAsString(productNode, "image");
        productEAN = getChildTextAsString(productNode, "ean");
        productQUnit = getChildTextAsString(productNode, "qunit");
        productDescription = "";
        for (int index = 0; index < productNode.getChildNodes().getLength(); index++) {
            Node productChild = productNode.getChildNodes().item(index);
            if (productChild.getNodeName().equals("short_description")) productDescription += productChild.getTextContent();
        }
        Double vatPercentDouble = 0.0;
        try {
            vatPercentDouble = Double.valueOf(productVatPercent).doubleValue() / 100;
        } catch (Exception e) {
        }
        Double priceNet = 0.0;
        try {
            if (!productNet.isEmpty()) {
                priceNet = Double.valueOf(productNet).doubleValue();
            }
            if (!productGross.isEmpty()) {
                priceNet = Double.valueOf(productGross).doubleValue() / (1 + vatPercentDouble);
            }
        } catch (Exception e) {
        }
        DataSetVAT vat = Data.INSTANCE.getVATs().addNewDataSetIfNew(new DataSetVAT(productVatName, "", productVatName, vatPercentDouble));
        int vatId = vat.getIntValueByKey("id");
        DataSetProduct product;
        String shopCategory = Activator.getDefault().getPreferenceStore().getString("WEBSHOP_PRODUCT_CATEGORY");
        if (!shopCategory.isEmpty()) if (!shopCategory.endsWith("/")) shopCategory += "/";
        if (useEANasItemNr) {
            if (!productEAN.isEmpty()) productModel = productEAN;
        }
        if (productModel.isEmpty() && !productName.isEmpty()) productModel = productName;
        if (productName.isEmpty() && !productModel.isEmpty()) productName = productModel;
        pictureName = "";
        if (!productImage.isEmpty()) {
            pictureName = ProductEditor.createPictureName(productName, productModel);
            downloadImageFromUrl(monitor, shopURL + productImagePath + productImage, Workspace.INSTANCE.getWorkspace() + Workspace.productPictureFolderName, pictureName);
        }
        Double quantity = 1.0;
        try {
            quantity = Double.valueOf(productQuantity).doubleValue();
        } catch (Exception e) {
        }
        product = new DataSetProduct(productName, productModel, shopCategory + productCategory, productDescription, priceNet, vatId, "", pictureName, quantity, productID, productQUnit);
        if (Data.INSTANCE.getProducts().isNew(product)) {
            Data.INSTANCE.getProducts().addNewDataSet(product);
        } else {
            DataSetProduct existingProduct = Data.INSTANCE.getProducts().getExistingDataSet(product);
            existingProduct.setStringValueByKey("category", product.getStringValueByKey("category"));
            existingProduct.setStringValueByKey("name", product.getStringValueByKey("name"));
            existingProduct.setStringValueByKey("itemnr", product.getStringValueByKey("itemnr"));
            existingProduct.setStringValueByKey("description", product.getStringValueByKey("description"));
            existingProduct.setDoubleValueByKey("price1", product.getDoubleValueByKey("price1"));
            existingProduct.setIntValueByKey("vatid", product.getIntValueByKey("vatid"));
            existingProduct.setStringValueByKey("picturename", product.getStringValueByKey("picturename"));
            existingProduct.setDoubleValueByKey("quantity", product.getDoubleValueByKey("quantity"));
            existingProduct.setIntValueByKey("webshopid", product.getIntValueByKey("webshopid"));
            existingProduct.setStringValueByKey("qunit", product.getStringValueByKey("qunit"));
            Data.INSTANCE.getProducts().updateDataSet(existingProduct);
        }
    }

    /**
	 * Download an image and save it to the file system
	 * 
	 * @param address
	 *            The URL of the image
	 * @param filePath
	 *            The folder to store the image
	 * @param fileName
	 *            The filename of the image
	 */
    public void downloadImageFromUrl(IProgressMonitor monitor, String address, String filePath, String fileName) {
        if (address.isEmpty() || filePath.isEmpty() || fileName.isEmpty()) return;
        URLConnection conn = null;
        URL url;
        try {
            File outputFile = new File(filePath + fileName);
            if (outputFile.exists()) return;
            url = new URL(address);
            conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setConnectTimeout(4000);
            File directory = new File(filePath);
            if (!directory.exists()) directory.mkdirs();
            InterruptConnection interruptConnection = new InterruptConnection(conn);
            new Thread(interruptConnection).start();
            while (!monitor.isCanceled() && !interruptConnection.isFinished() && !interruptConnection.isError()) ;
            if (!interruptConnection.isFinished) {
                ((HttpURLConnection) conn).disconnect();
                return;
            }
            InputStream content = (InputStream) interruptConnection.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(content));
            BufferedInputStream bis = new BufferedInputStream(content);
            FileOutputStream fos = new FileOutputStream(outputFile);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int current = 0;
            while ((current = bis.read()) != -1) {
                byteArrayOutputStream.write((byte) current);
            }
            fos.write(byteArrayOutputStream.toByteArray());
            byteArrayOutputStream.close();
            fos.close();
            in.close();
        } catch (MalformedURLException e) {
            Logger.logError(e, _("Malformated URL:") + " " + address);
        } catch (IOException e) {
            Logger.logError(e, _("Error downloading picture from:") + " " + address);
        }
    }

    /**
	 * Parse an XML node and create a new order for each order entry
	 * 
	 * @param orderNode
	 *            The node with the orders to import
	 */
    public void createOrderFromXMLOrderNode(Node orderNode) {
        String firstname;
        String id;
        String genderString;
        int genderInt = 0;
        String deliveryGenderString;
        int deliveryGenderInt = 0;
        String lastname;
        String company;
        String street;
        String zip;
        String city;
        String country;
        String phone;
        String email;
        String delivery_firstname;
        String delivery_lastname;
        String delivery_company;
        String delivery_street;
        String delivery_zip;
        String delivery_city;
        String delivery_country;
        String itemQuantity;
        String itemDescription;
        String itemModel;
        String itemName;
        String itemGross;
        String itemCategory;
        String itemVatpercent;
        String itemVatname;
        String itemQUnit;
        int productID;
        String order_id;
        String order_date;
        String paymentCode;
        String paymentName;
        String order_total;
        Double order_totalDouble = 0.0;
        String shipping_vatpercent;
        String shipping_vatname;
        String shipping_name;
        String shipping_gross;
        String commentDate;
        String comment;
        String commentText;
        int documentId;
        boolean noVat = true;
        String noVatName = "";
        NamedNodeMap attributes = orderNode.getAttributes();
        order_id = getAttributeAsString(attributes, "id");
        order_date = getAttributeAsString(attributes, "date");
        if (!Data.INSTANCE.getDocuments().isNew(new DataSetDocument(DocumentType.ORDER, order_id, DataUtils.DateAsISO8601String(order_date)))) return;
        DataSetDocument dataSetDocument = Data.INSTANCE.getDocuments().addNewDataSet(new DataSetDocument(DocumentType.ORDER));
        documentId = dataSetDocument.getIntValueByKey("id");
        dataSetDocument.setStringValueByKey("name", order_id);
        dataSetDocument.setStringValueByKey("webshopid", order_id);
        dataSetDocument.setStringValueByKey("webshopdate", DataUtils.DateAsISO8601String(order_date));
        NodeList childnodes = orderNode.getChildNodes();
        for (int childnodeIndex = 0; childnodeIndex < childnodes.getLength(); childnodeIndex++) {
            Node childnode = childnodes.item(childnodeIndex);
            attributes = childnode.getAttributes();
            if (childnode.getNodeName().equalsIgnoreCase("contact")) {
                id = getAttributeAsString(attributes, "id");
                genderString = getChildTextAsString(childnode, "gender");
                firstname = getChildTextAsString(childnode, "firstname");
                lastname = getChildTextAsString(childnode, "lastname");
                company = getChildTextAsString(childnode, "company");
                street = getChildTextAsString(childnode, "street");
                zip = getChildTextAsString(childnode, "zip");
                city = getChildTextAsString(childnode, "city");
                country = getChildTextAsString(childnode, "country");
                deliveryGenderString = getChildTextAsString(childnode, "delivery_gender");
                delivery_firstname = getChildTextAsString(childnode, "delivery_firstname");
                delivery_lastname = getChildTextAsString(childnode, "delivery_lastname");
                delivery_company = getChildTextAsString(childnode, "delivery_company");
                delivery_street = getChildTextAsString(childnode, "delivery_street");
                delivery_zip = getChildTextAsString(childnode, "delivery_zip");
                delivery_city = getChildTextAsString(childnode, "delivery_city");
                delivery_country = getChildTextAsString(childnode, "delivery_country");
                phone = getChildTextAsString(childnode, "phone");
                email = getChildTextAsString(childnode, "email");
                if (genderString.equals("m")) genderInt = 1;
                if (genderString.equals("f")) genderInt = 2;
                if (deliveryGenderString.equals("m")) deliveryGenderInt = 1;
                if (deliveryGenderString.equals("f")) deliveryGenderInt = 2;
                String shopCategory = Activator.getDefault().getPreferenceStore().getString("WEBSHOP_CONTACT_CATEGORY");
                DataSetContact contact = Data.INSTANCE.getContacts().addNewDataSetIfNew(new DataSetContact(-1, false, shopCategory, genderInt, "", firstname, lastname, company, street, zip, city, country, deliveryGenderInt, "", delivery_firstname, delivery_lastname, delivery_company, delivery_street, delivery_zip, delivery_city, delivery_country, "", "", "", "", "", "", id, "", "", Data.INSTANCE.getPropertyAsInt("standardpayment"), 0, phone, "", "", email, "", "", 0, 0.0, 0));
                contact.setIntValueByKey("gender", genderInt);
                contact.setStringValueByKey("firstname", firstname);
                contact.setStringValueByKey("name", lastname);
                contact.setStringValueByKey("company", company);
                contact.setStringValueByKey("street", street);
                contact.setStringValueByKey("zip", zip);
                contact.setStringValueByKey("city", city);
                contact.setStringValueByKey("country", country);
                contact.setIntValueByKey("delivery_gender", deliveryGenderInt);
                contact.setStringValueByKey("delivery_firstname", delivery_firstname);
                contact.setStringValueByKey("delivery_name", delivery_lastname);
                contact.setStringValueByKey("delivery_company", delivery_company);
                contact.setStringValueByKey("delivery_street", delivery_street);
                contact.setStringValueByKey("delivery_zip", delivery_zip);
                contact.setStringValueByKey("delivery_city", delivery_city);
                contact.setStringValueByKey("delivery_country", delivery_country);
                contact.setStringValueByKey("nr", id);
                Data.INSTANCE.getContacts().updateDataSet(contact);
                dataSetDocument.setIntValueByKey("addressid", contact.getIntValueByKey("id"));
                dataSetDocument.setStringValueByKey("address", contact.getAddress(false));
                dataSetDocument.setStringValueByKey("deliveryaddress", contact.getAddress(true));
                dataSetDocument.setStringValueByKey("addressfirstline", contact.getName(false));
            }
        }
        comment = "";
        for (int childnodeIndex = 0; childnodeIndex < childnodes.getLength(); childnodeIndex++) {
            Node childnode = childnodes.item(childnodeIndex);
            attributes = childnode.getAttributes();
            if (childnode.getNodeName().equalsIgnoreCase("comment")) {
                commentDate = DataUtils.DateAndTimeAsLocalString(getAttributeAsString(attributes, "date"));
                commentText = childnode.getTextContent();
                if (!comment.isEmpty()) comment += "\n";
                comment += commentDate + " :\n";
                comment += commentText + "\n";
            }
        }
        String itemString = "";
        for (int childnodeIndex = 0; childnodeIndex < childnodes.getLength(); childnodeIndex++) {
            Node childnode = childnodes.item(childnodeIndex);
            attributes = childnode.getAttributes();
            if (childnode.getNodeName().equalsIgnoreCase("item")) {
                itemQuantity = getAttributeAsString(attributes, "quantity");
                itemGross = getAttributeAsString(attributes, "gross");
                itemVatpercent = getAttributeAsString(attributes, "vatpercent");
                productID = getAttributeAsID(attributes, "productid");
                itemModel = getChildTextAsString(childnode, "model");
                itemName = getChildTextAsString(childnode, "name");
                itemCategory = getChildTextAsString(childnode, "category");
                itemVatname = getChildTextAsString(childnode, "vatname");
                itemQUnit = getChildTextAsString(childnode, "qunit");
                Double vat_percentDouble = 0.0;
                try {
                    vat_percentDouble = Double.valueOf(itemVatpercent).doubleValue() / 100;
                } catch (Exception e) {
                }
                if (vat_percentDouble > 0.0) noVat = false; else {
                    if (noVatName.isEmpty() && !itemVatname.isEmpty()) noVatName = itemVatname;
                }
                Double priceNet = 0.0;
                try {
                    priceNet = Double.valueOf(itemGross).doubleValue() / (1 + vat_percentDouble);
                } catch (Exception e) {
                }
                DataSetVAT vat = Data.INSTANCE.getVATs().addNewDataSetIfNew(new DataSetVAT(itemVatname, "", itemVatname, vat_percentDouble));
                int vatId = vat.getIntValueByKey("id");
                DataSetProduct product;
                String shopCategory = Activator.getDefault().getPreferenceStore().getString("WEBSHOP_PRODUCT_CATEGORY");
                if (!shopCategory.isEmpty()) if (!shopCategory.endsWith("/")) shopCategory += "/";
                if (itemModel.isEmpty() && !itemName.isEmpty()) itemModel = itemName;
                if (itemName.isEmpty() && !itemModel.isEmpty()) itemName = itemModel;
                itemDescription = "";
                for (int index = 0; index < childnode.getChildNodes().getLength(); index++) {
                    Node itemChild = childnode.getChildNodes().item(index);
                    if (itemChild.getNodeName().equals("attribute")) {
                        attributes = itemChild.getAttributes();
                        if (!itemDescription.isEmpty()) itemDescription += ", ";
                        itemDescription += getChildTextAsString(itemChild, "option") + ": ";
                        itemDescription += getChildTextAsString(itemChild, "value");
                    }
                }
                product = new DataSetProduct(itemName, itemModel, shopCategory + itemCategory, itemDescription, priceNet, vatId, "", "", 1.0, productID, itemQUnit);
                DataSetProduct newOrExistingProduct = Data.INSTANCE.getProducts().addNewDataSetIfNew(product);
                product.setStringValueByKey("picturename", newOrExistingProduct.getStringValueByKey("picturename"));
                DataSetItem item = Data.INSTANCE.getItems().addNewDataSet(new DataSetItem(Double.valueOf(itemQuantity), product));
                item.setIntValueByKey("owner", documentId);
                Data.INSTANCE.getItems().updateDataSet(item);
                if (!itemString.isEmpty()) itemString += ",";
                itemString += item.getStringValueByKey("id");
            }
        }
        for (int childnodeIndex = 0; childnodeIndex < childnodes.getLength(); childnodeIndex++) {
            Node childnode = childnodes.item(childnodeIndex);
            attributes = childnode.getAttributes();
            if (childnode.getNodeName().equalsIgnoreCase("shipping")) {
                shipping_name = getChildTextAsString(childnode, "name");
                shipping_gross = getAttributeAsString(attributes, "gross");
                shipping_vatpercent = getAttributeAsString(attributes, "vatpercent");
                shipping_vatname = getChildTextAsString(childnode, "vatname");
                Double shippingvat_percentDouble = 0.0;
                try {
                    shippingvat_percentDouble = Double.valueOf(shipping_vatpercent).doubleValue() / 100;
                } catch (Exception e) {
                }
                Double shippingGross = 0.0;
                try {
                    shippingGross = Double.valueOf(shipping_gross).doubleValue();
                } catch (Exception e) {
                }
                String shopCategory = Activator.getDefault().getPreferenceStore().getString("WEBSHOP_SHIPPING_CATEGORY");
                DataSetVAT vat = Data.INSTANCE.getVATs().addNewDataSetIfNew(new DataSetVAT(shipping_vatname, "", shipping_vatname, shippingvat_percentDouble));
                int vatId = vat.getIntValueByKey("id");
                DataSetShipping shipping = Data.INSTANCE.getShippings().addNewDataSetIfNew(new DataSetShipping(shipping_name, shopCategory, shipping_name, shippingGross, vatId, 1));
                dataSetDocument.setIntValueByKey("shippingid", shipping.getIntValueByKey("id"));
                dataSetDocument.setDoubleValueByKey("shipping", shippingGross);
                dataSetDocument.setStringValueByKey("shippingname", shipping_name);
                dataSetDocument.setStringValueByKey("shippingdescription", shipping.getStringValueByKey("description"));
                dataSetDocument.setDoubleValueByKey("shippingvat", shippingvat_percentDouble);
                dataSetDocument.setStringValueByKey("shippingvatdescription", vat.getStringValueByKey("description"));
                String s = "";
                if (order_id.length() <= 5) s = "00000".substring(order_id.length(), 5);
                s += order_id;
                dataSetDocument.setStringValueByKey("customerref", _("Web shop No.") + " " + s);
            }
        }
        for (int childnodeIndex = 0; childnodeIndex < childnodes.getLength(); childnodeIndex++) {
            Node childnode = childnodes.item(childnodeIndex);
            attributes = childnode.getAttributes();
            if (childnode.getNodeName().equalsIgnoreCase("payment")) {
                order_total = getAttributeAsString(attributes, "total");
                paymentCode = getAttributeAsString(attributes, "type");
                paymentName = getChildTextAsString(childnode, "name");
                try {
                    order_totalDouble = Double.valueOf(order_total).doubleValue();
                } catch (Exception e) {
                }
                DataSetPayment payment = Data.INSTANCE.getPayments().addNewDataSetIfNew(new DataSetPayment(paymentName, "", paymentName + " (" + paymentCode + ")", 0.0, 0, 0, "Zahlung dankend erhalten.", "", false));
                dataSetDocument.setIntValueByKey("paymentid", payment.getIntValueByKey("id"));
                dataSetDocument.setStringValueByKey("paymentname", paymentName);
                dataSetDocument.setStringValueByKey("paymentdescription", payment.getStringValueByKey("description"));
            }
        }
        dataSetDocument.setIntValueByKey("progress", 10);
        dataSetDocument.setStringValueByKey("date", DataUtils.DateAsISO8601String(order_date));
        comment = dataSetDocument.getStringValueByKey("message") + comment;
        dataSetDocument.setStringValueByKey("message", comment);
        dataSetDocument.setStringValueByKey("items", itemString);
        dataSetDocument.setDoubleValueByKey("total", order_totalDouble);
        if (noVat) {
            dataSetDocument.setBooleanValueByKey("novat", true);
            dataSetDocument.setStringValueByKey("novatname", noVatName);
            DataSetVAT v = Data.INSTANCE.getVATs().getDatasetByName(noVatName);
            if (v != null) dataSetDocument.setStringValueByKey("novatdescription", v.getStringValueByKey("description"));
        }
        Data.INSTANCE.getDocuments().updateDataSet(dataSetDocument);
        dataSetDocument.calculate();
        Double calcTotal = dataSetDocument.getSummary().getTotalGross().asDouble();
        if (!DataUtils.DoublesAreEqual(order_totalDouble, calcTotal)) {
            String error = _("Order" + ":");
            error += " " + order_id + "\n";
            error += _("Total sum from web shop:");
            error += "\n" + DataUtils.DoubleToFormatedPriceRound(order_totalDouble) + "\n";
            error += _("is not equal to the calculated one:");
            error += "\n" + DataUtils.DoubleToFormatedPriceRound(calcTotal) + "\n";
            error += _("Please check this!");
            runResult = error;
        }
    }

    /**
	 * Interpret the complete node of all orders and import them
	 */
    public void interpretWebShopData(IProgressMonitor monitor) {
        shopURL = "";
        productImagePath = "";
        allOrdersAreInSync();
        if (document == null) return;
        NodeList ndList;
        ndList = document.getElementsByTagName("webshop");
        if (ndList.getLength() == 1) {
            Node webshop = ndList.item(0);
            shopURL = getAttributeAsString(webshop.getAttributes(), "url");
        }
        ndList = document.getElementsByTagName("products");
        if (ndList.getLength() == 1) {
            Node products = ndList.item(0);
            productImagePath = getAttributeAsString(products.getAttributes(), "imagepath");
        }
        ndList = document.getElementsByTagName("product");
        for (int productIndex = 0; productIndex < ndList.getLength(); productIndex++) {
            monitor.subTask(_("Loading product image") + " " + Integer.toString(productIndex + 1) + "/" + Integer.toString(ndList.getLength()));
            setProgress(50 + 40 * (productIndex + 1) / ndList.getLength());
            Node product = ndList.item(productIndex);
            createProductFromXMLOrderNode(product);
            if (monitor.isCanceled()) return;
        }
        monitor.subTask(_("Importing orders"));
        setProgress(95);
        ndList = document.getElementsByTagName("order");
        for (int orderIndex = 0; orderIndex < ndList.getLength(); orderIndex++) {
            Node order = ndList.item(orderIndex);
            createOrderFromXMLOrderNode(order);
        }
        saveOrdersToSynchronize();
    }
}
