package pl.umk.webclient.gridbeans;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import javax.xml.namespace.QName;
import org.jdom.adapters.XercesDOMAdapter;
import org.w3c.dom.Element;
import pl.umk.webclient.WebClientSettings;
import com.intel.gpe.client2.Client;
import com.intel.gpe.client2.gridbeans.GridBeanJob;
import com.intel.gpe.client2.gridbeans.InternalGridBean;
import com.intel.gpe.clients.api.Application;
import com.intel.gpe.gridbeans.ApplicationImpl;
import com.intel.gpe.gridbeans.GridBeanException;
import com.intel.gpe.gridbeans.IGridBean;
import com.intel.util.sets.Pair;
import com.intel.util.xml.ElementUtil;

/**
 * @author Rafal Osowicki (rrafcio@mat.umk.pl)
 */
public class WebGridBean implements InternalGridBean {

    private WebGridBeanMetadata metadata;

    private String inputJspFile = "";

    private String outputJspFile = "";

    private String icon = WebClientSettings.NO_ICON_PICTURE;

    private String name;

    private String descriptor;

    private String description;

    private String version;

    private String author = "unknown";

    private String releaseDate = "unknown";

    private Application application;

    private List<Pair<String, String>> params;

    private List<Pair<String, String>> htmlParams;

    private IGridBean model;

    public WebGridBean(String descriptor) throws GridBeanException {
        this.descriptor = descriptor;
        try {
            URL url = new URL(WebClientSettings.getGridbeanPath() + descriptor + "/" + WebGridBeanConstants.GRIDBEAN_DESCRIPTION);
            InputStream gbdInput = new BufferedInputStream(url.openStream());
            Element root = new XercesDOMAdapter().getDocument(gbdInput, false).getDocumentElement();
            String appName = ElementUtil.getChildValueString(root, WebGridBeanConstants.NAMESPACE, WebGridBeanConstants.APPLICATION_NAME, null);
            String appVersion = ElementUtil.getChildValueString(root, WebGridBeanConstants.NAMESPACE, WebGridBeanConstants.APPLICATION_VERSION, null);
            application = new ApplicationImpl(appName, appVersion);
            name = ElementUtil.getChildValueString(root, WebGridBeanConstants.NAMESPACE, WebGridBeanConstants.NAME, null);
            version = ElementUtil.getChildValueString(root, WebGridBeanConstants.NAMESPACE, WebGridBeanConstants.VERSION, null);
            inputJspFile = ElementUtil.getChildValueString(root, WebGridBeanConstants.NAMESPACE, WebGridBeanConstants.INPUT_GUI, null);
            outputJspFile = ElementUtil.getChildValueString(root, WebGridBeanConstants.NAMESPACE, WebGridBeanConstants.OUTPUT_GUI, null);
            String tmp = ElementUtil.getChildValueString(root, WebGridBeanConstants.NAMESPACE, WebGridBeanConstants.ICON_FILE, null);
            if (tmp != null) icon = tmp;
            tmp = ElementUtil.getChildValueString(root, WebGridBeanConstants.NAMESPACE, WebGridBeanConstants.AUTHOR, null);
            if (tmp != null) author = tmp;
            tmp = ElementUtil.getChildValueString(root, WebGridBeanConstants.NAMESPACE, WebGridBeanConstants.RELEASED, null);
            if (tmp != null) releaseDate = tmp;
            tmp = ElementUtil.getChildValueString(root, WebGridBeanConstants.NAMESPACE, WebGridBeanConstants.DESCRIPTION, null);
            if (tmp != null) description = tmp;
            gbdInput.close();
        } catch (Exception e) {
            throw new GridBeanException("Cannot load GridBean definition.", e);
        }
        long id = System.currentTimeMillis();
        metadata = new WebGridBeanMetadata(id, name, version, releaseDate, author, application.getName(), application.getApplicationVersion(), inputJspFile, outputJspFile, icon, description);
    }

    public IGridBean getModel() {
        if (model == null) model = new GenericWebGridBeanModel(params, application);
        return model;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public Object getPluginInstance(QName type, Client parent) {
        return null;
    }

    public GridBeanJob getJobInstance(String tmpDirectory) throws GridBeanException {
        return new GridBeanJob(tmpDirectory, getModel().getClass(), this);
    }

    public Application getSupportedApplication() {
        return application;
    }

    public WebGridBeanMetadata getMetadata() {
        return metadata;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public void setParams(List<Pair<String, String>> params) {
        this.params = params;
    }

    public List<Pair<String, String>> getHtmlParams() {
        return htmlParams;
    }

    public void setHtmlParams(List<Pair<String, String>> htmlParams) {
        this.htmlParams = htmlParams;
    }

    public List<Pair<String, String>> getParams() {
        return params;
    }
}
