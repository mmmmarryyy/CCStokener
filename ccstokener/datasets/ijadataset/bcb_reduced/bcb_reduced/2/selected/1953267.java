package cloudspace.demoitem;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.log4j.Logger;
import org.zkoss.image.AImage;
import org.zkoss.zul.A;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import cloudspace.config.ServletLocationFinder;

/**
 * The controller class for the demoItems.zul.  
 * The controller does a lot more than the name suggests here. 
 * It is also responsible for talking to the model, and capturing images 
 * for the demo projects that are added.
 * 
 * @author Sunil Kamalakar
 *
 */
public class DemoItemsController extends GenericForwardComposer {

    private static final long serialVersionUID = 5823920504173577650L;

    private static final String SERVLET_PATH = ServletLocationFinder.getServletResourcePath("/");

    private static final String DEFAULT_IMAGE_LOCATION = "images/demo/";

    private static final String DEFAULT_IMAGE_NAME = "DemoImage-";

    private static final String DEFAULT_IMAGE_EXTENTION = ".png";

    private static final String DEFAULT_PORT_URI = ":8443";

    private static Logger log = Logger.getLogger(DemoItemsController.class);

    DemoItemsModel model;

    private List<DemoItemBean> demoItems = new ArrayList<DemoItemBean>();

    private Listbox viewEditListbox;

    private Textbox addProgramURI, addTooltip;

    private Image demoImage;

    private A imageLink;

    public DemoItemsController() {
        super();
        try {
            model = new DemoItemsModel();
        } catch (Exception e) {
            String errorMessage = "An error occured in the creation of the model file for demo items. \n" + "No demo functionality will work";
            log.error(errorMessage, e);
        }
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        viewEditListbox.setItemRenderer(new DemoListItemRenderer(model, this));
        updateModelInView();
    }

    public void onSelect$viewEditListbox(SelectEvent event) throws Exception {
        Listitem selectedItem = viewEditListbox.getSelectedItem();
        if (selectedItem != null) {
            DemoItemBean bean = (DemoItemBean) selectedItem.getValue();
            setViewItemValue(bean);
        }
    }

    private void setViewItemValue(DemoItemBean bean) {
        if (bean == null) {
            bean = new DemoItemBean("", "", "", false);
        }
        addProgramURI.setValue(bean.getProgramURI());
        addTooltip.setValue(bean.getTooltip());
        try {
            imageLink.setHref("/" + bean.getImageURI());
            demoImage.setContent(new AImage(SERVLET_PATH + bean.getImageURI()));
        } catch (Exception e) {
            imageLink.setHref("/");
            demoImage.setSrc("");
        }
    }

    public void onClick$refreshItemButton(Event event) {
        updateModelInView();
    }

    public void onClick$addItemButton(Event event) {
        addDemoItem(addProgramURI.getValue(), null, addTooltip.getValue(), true);
        updateModelInView();
    }

    public void addDemoItem(String programURIStr, String imageURIStr, String tooltip, boolean visibility) {
        try {
            if (programURIStr != null && isValidURI(programURIStr, true)) {
                imageURIStr = createImageFile(programURIStr);
                if (imageURIStr != null && imageURIStr != "") {
                    addItemToModel(programURIStr, imageURIStr, tooltip, visibility);
                }
            }
        } catch (Exception e) {
            String errorMessage = "An error occured while adding a new demo item.\n" + e.getMessage();
            log.error(errorMessage, e);
            displayError(errorMessage);
        }
    }

    public void onClick$updateItemButton(Event event) {
        Listitem selectedItem = viewEditListbox.getSelectedItem();
        DemoItemBean bean = null;
        DemoItemBean newBean = null;
        String newProgramURI = addProgramURI.getValue();
        String newImageURI = "";
        String newTooltip = addTooltip.getValue();
        boolean newVisibility = false;
        boolean deletePreviousImage = false;
        if (selectedItem != null) {
            bean = (DemoItemBean) selectedItem.getValue();
            newImageURI = bean.getImageURI();
            newVisibility = bean.getVisibility();
        }
        try {
            if (bean != null) {
                if (isValidURI(newProgramURI, true)) {
                    if (!bean.getProgramURI().equals(newProgramURI)) {
                        newImageURI = createImageFile(newProgramURI);
                        deletePreviousImage = true;
                    }
                    newBean = new DemoItemBean(newProgramURI, newImageURI, newTooltip, newVisibility);
                    model.updateDemoItemInFile(bean, newBean);
                    updateModelInView();
                    if (deletePreviousImage) {
                        File imageFileToDelete = new File(ServletLocationFinder.getServletResourcePath("/") + bean.getImageURI());
                        if (imageFileToDelete != null && imageFileToDelete.exists()) {
                            imageFileToDelete.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            String errorMessage = "An error occured while updating the values of demo item.\n" + e.getMessage();
            log.error(errorMessage, e);
            displayError(errorMessage);
        }
    }

    public boolean isValidURI(String programURIStr, boolean enableValidityCheck) throws Exception {
        boolean retVal = true;
        programURIStr = addPortToURI(programURIStr);
        try {
            if (enableValidityCheck) {
                doesURLExists(programURIStr);
            }
            URL url = new URL(programURIStr);
            @SuppressWarnings("unused") URI uri = new URI(url.toString());
        } catch (Exception e) {
            String errorMessage = "An error occured while parsing/fetching the URL:\n " + e.getMessage();
            log.error(errorMessage, e);
            retVal = false;
            displayError(errorMessage);
        }
        return retVal;
    }

    /**
	 * This method checks if the URL exists. It sends out a HTTP request for the same.
	 * @param programURIStr
	 * @return
	 * @throws Exception
	 */
    private boolean doesURLExists(String programURIStr) throws Exception {
        boolean retVal = true;
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        } };
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier allHostsValid = new HostnameVerifier() {

            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        URL url = new URL(programURIStr);
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.setConnectTimeout(30000);
        urlConn.connect();
        log.debug("HTTP Status code: " + urlConn.getResponseCode() + " for URL: " + programURIStr);
        if (HttpURLConnection.HTTP_OK != urlConn.getResponseCode()) {
            urlConn.disconnect();
            throw new Exception("HTTP status code " + urlConn.getResponseCode() + " for " + programURIStr);
        }
        urlConn.disconnect();
        return retVal;
    }

    private String createImageFile(String programURIStr) {
        StringBuilder imageURI = new StringBuilder();
        imageURI.append(DEFAULT_IMAGE_LOCATION);
        imageURI.append(DEFAULT_IMAGE_NAME + System.currentTimeMillis());
        imageURI.append(DEFAULT_IMAGE_EXTENTION);
        try {
            execImageCapture(programURIStr, imageURI.toString());
        } catch (Exception e) {
            String errorMessage = "Not able to capture the image for file: " + imageURI.toString() + "\n" + e.getMessage();
            log.error(errorMessage, e);
            imageURI = null;
            displayError(errorMessage);
        }
        if (imageURI != null) {
            return imageURI.toString();
        }
        return "";
    }

    public void updateModelInView() {
        demoItems = loadDataFromModel();
        if (demoItems != null) {
            ListModel list = new ListModelList(demoItems);
            viewEditListbox.setModel(list);
        }
        setViewItemValue(null);
    }

    private String addItemToModel(String programURIStr, String imageURIStr, String tooltip, boolean visibility) {
        String retVal = "";
        try {
            model.addDemoItemToFile(new DemoItemBean(programURIStr, imageURIStr, tooltip, visibility));
        } catch (Exception e) {
            String errorMessage = "An Exception occured while adding an entry to the demo file\n" + e.getMessage();
            log.error(errorMessage, e);
            displayError(errorMessage);
        }
        return retVal;
    }

    private List<DemoItemBean> loadDataFromModel() {
        List<DemoItemBean> demoItems = null;
        try {
            demoItems = model.readDemoFile();
        } catch (Exception e) {
            String errorMessage = "An Exception occured while trying to read the demo file\n" + e.getMessage();
            log.error(errorMessage, e);
            displayError(errorMessage);
        }
        return demoItems;
    }

    public void execImageCapture(String programURI, String imageURI) throws Exception {
        ScreenshotCapturer.captureScreenshot(addPortToURI(programURI), SERVLET_PATH + imageURI);
    }

    private String addPortToURI(String uri) throws Exception {
        String port = "";
        URL url = null;
        try {
            url = new URL(uri);
            port = String.valueOf(url.getPort());
        } catch (MalformedURLException e) {
        }
        if (url != null && port != "" && !uri.contains(port)) return uri.replaceFirst(url.getHost(), url.getHost() + DEFAULT_PORT_URI); else return uri;
    }

    private void displayError(String errorMessage) {
        try {
            Messagebox.show(errorMessage, "Error", Messagebox.OK, Messagebox.ERROR);
        } catch (Exception e) {
            log.error("An error occurred when displaying the error ", e);
        }
    }
}

class DemoListItemRenderer implements ListitemRenderer {

    private DemoItemsModel model;

    private DemoItemsController controller;

    public DemoListItemRenderer(DemoItemsModel model, DemoItemsController controller) {
        this.model = model;
        this.controller = controller;
    }

    @Override
    public void render(Listitem row, Object data) throws Exception {
        if (data == null) return;
        DemoItemBean demoItem = (DemoItemBean) data;
        Listcell programCell = new Listcell();
        A programLink = new A(demoItem.getProgramURI());
        programLink.setHref(demoItem.getProgramURI());
        programLink.setTarget("_blank");
        programCell.appendChild(programLink);
        row.appendChild(programCell);
        row.appendChild(new Listcell(demoItem.getTooltip()));
        Listcell visibilityCell = new Listcell();
        Checkbox visibilityCheckbox = new Checkbox();
        visibilityCheckbox.setChecked(demoItem.getVisibility());
        visibilityCheckbox.setParent(visibilityCell);
        visibilityCheckbox.addEventListener(Events.ON_CHECK, new EventListener() {

            public void onEvent(Event event) throws Exception {
                Checkbox visibilityCheckbox = (Checkbox) event.getTarget();
                Listitem item = (Listitem) event.getTarget().getParent().getParent();
                boolean newVisibilityValue = visibilityCheckbox.isChecked();
                if (item != null) {
                    DemoItemBean bean = (DemoItemBean) item.getValue();
                    if (bean.getVisibility() != newVisibilityValue) {
                        DemoItemBean newBean = new DemoItemBean(bean.getProgramURI(), bean.getImageURI(), bean.getTooltip(), newVisibilityValue);
                        model.updateDemoItemInFile(bean, newBean);
                        controller.updateModelInView();
                    }
                }
            }
        });
        row.appendChild(visibilityCell);
        row.setValue(demoItem);
        Listcell operationsCell = new Listcell();
        Button button = new Button(null, "/images/icons/Delete-Icon.png");
        button.setParent(operationsCell);
        button.addEventListener(Events.ON_CLICK, new EventListener() {

            public void onEvent(Event event) throws Exception {
                Listitem item = (Listitem) event.getTarget().getParent().getParent();
                String programURI = "";
                if (item != null && item.getValue() != null) {
                    DemoItemBean bean = (DemoItemBean) item.getValue();
                    programURI = bean.getProgramURI();
                    if (programURI != null && programURI != "") {
                        model.removeDemoItemFromFile(bean);
                        File imageFileToDelete = new File(ServletLocationFinder.getServletResourcePath("/") + bean.getImageURI());
                        if (imageFileToDelete != null && imageFileToDelete.exists()) {
                            imageFileToDelete.delete();
                        }
                        controller.updateModelInView();
                    }
                }
            }
        });
        Button imageCapture = new Button(null, "/images/icons/Image-Capture.png");
        imageCapture.setParent(operationsCell);
        imageCapture.addEventListener(Events.ON_CLICK, new EventListener() {

            public void onEvent(Event event) throws Exception {
                Listitem item = (Listitem) event.getTarget().getParent().getParent();
                String programURI = "";
                String imageURI = "";
                if (item != null && item.getValue() != null) {
                    programURI = ((DemoItemBean) item.getValue()).getProgramURI();
                    imageURI = ((DemoItemBean) item.getValue()).getImageURI();
                    if (programURI != null && programURI != "") {
                        if (controller.isValidURI(programURI, true)) {
                            controller.execImageCapture(programURI, imageURI);
                            controller.updateModelInView();
                        }
                    }
                }
            }
        });
        row.appendChild(operationsCell);
    }
}
