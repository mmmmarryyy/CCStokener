package org.eclipse.swordfish.tooling.ui.wizards;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.cxf.helpers.DOMUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.ui.templates.AbstractTemplateSection;
import org.eclipse.swordfish.tooling.ui.Activator;
import org.eclipse.swordfish.tooling.ui.Messages;
import org.eclipse.swordfish.tooling.ui.helper.ErrorUtil;
import org.eclipse.swordfish.tooling.ui.wizards.actions.CxfEndpointGenerationJob;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * class PluginContentWsdlWizardSection
 * 
 * @author amarkevich
 */
public class PluginContentWsdlWizardSection extends AbstractTemplateSection {

    private static final int PAGE_COUNT = 1;

    private PluginContentWsdlWizardPage page;

    private static final String CXF_ENDPOINT_FILENAME = "META-INF/spring/cxf-endpoint.xml";

    @Override
    public void addPages(Wizard wizard) {
        page = new PluginContentWsdlWizardPage();
        wizard.addPage(page);
        markPagesAdded();
    }

    /**
	 * {@inheritDoc}
	 */
    protected void updateModel(IProgressMonitor monitor) throws CoreException {
    }

    /**
	 * {@inheritDoc}
	 */
    public String getUsedExtensionPoint() {
        return null;
    }

    /**
	 * {@inheritDoc}
	 */
    protected ResourceBundle getPluginResourceBundle() {
        return Platform.getResourceBundle(Activator.getDefault().getBundle());
    }

    /**
	 * {@inheritDoc}
	 */
    public String[] getNewFiles() {
        return new String[0];
    }

    @Override
    public int getNumberOfWorkUnits() {
        return super.getNumberOfWorkUnits() + PAGE_COUNT;
    }

    /**
	 * {@inheritDoc}
	 */
    public String getLabel() {
        return null;
    }

    /**
	 * {@inheritDoc}
	 */
    public WizardPage getPage(int pageIndex) {
        if (pageIndex == 0) {
            return page;
        }
        return null;
    }

    /**
	 * {@inheritDoc}
	 */
    public int getPageCount() {
        return PAGE_COUNT;
    }

    @Override
    protected void generateFiles(IProgressMonitor monitor) throws CoreException {
        super.generateFiles(monitor);
        String pathToWsdl = page.getPathToWSDL();
        try {
            monitor.setTaskName("PluginContentWsdlGenerationOperation");
            IFolder srcFolder = getSourceFolder(monitor);
            PluginContentWsdlGenerationOperation generationOperation = new PluginContentWsdlGenerationOperation(pathToWsdl, project, srcFolder, false);
            generationOperation.run(monitor);
            List<String> warnings = generationOperation.getWarnings();
            if (!warnings.isEmpty()) {
                StringBuilder message = new StringBuilder();
                for (String warning : warnings) {
                    message.append(warning).append("\n");
                }
                ErrorDialog.openError(page.getShell(), null, null, new Status(IStatus.WARNING, Activator.getDefault().getPluginId(), message.toString()));
            }
            Document document = null;
            final URL url = new URL("file", null, pathToWsdl);
            InputStream stream = url.openStream();
            if (stream != null) {
                try {
                    document = DOMUtils.readXml(stream);
                } finally {
                    stream.close();
                }
            }
            String serviceURL = "http://localhost:8197/" + generationOperation.getServiceName() + "/";
            List<Element> elementList = DOMUtils.findAllElementsByTagNameNS(document.getDocumentElement(), "http://schemas.xmlsoap.org/wsdl/", "port");
            for (Element el : elementList) {
                Element soapAddress = DOMUtils.findAllElementsByTagNameNS(el, "http://schemas.xmlsoap.org/wsdl/soap/", "address").iterator().next();
                serviceURL = soapAddress.getAttribute("location");
            }
            monitor.setTaskName("CxfEndpointGenerationJob");
            IPath path = project.getFullPath().append(CXF_ENDPOINT_FILENAME);
            CxfEndpointGenerationJob cxfEndpointJob = new CxfEndpointGenerationJob(generationOperation.getServiceName(), generationOperation.getNameSpace(), generationOperation.getImplementorName(), serviceURL, path);
            IStatus result = cxfEndpointJob.runInWorkspace(monitor);
            if (!result.isOK()) {
                throw new CoreException(result);
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            IStatus status = ErrorUtil.getErrorStatus(Messages.ERROR_JAXWS_PROVIDER_GENERATION, cause);
            throw new CoreException(status);
        } catch (InterruptedException e) {
        } catch (SAXException e) {
            Throwable cause = e.getCause();
            IStatus status = ErrorUtil.getErrorStatus(Messages.ERROR_JAXWS_PROVIDER_GENERATION, cause);
            throw new CoreException(status);
        } catch (ParserConfigurationException e) {
            Throwable cause = e.getCause();
            IStatus status = ErrorUtil.getErrorStatus(Messages.ERROR_JAXWS_PROVIDER_GENERATION, cause);
            throw new CoreException(status);
        } catch (IOException e) {
            Throwable cause = e.getCause();
            IStatus status = ErrorUtil.getErrorStatus(Messages.ERROR_JAXWS_PROVIDER_GENERATION, cause);
            throw new CoreException(status);
        }
    }
}
