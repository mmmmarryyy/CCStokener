package org.isqlviewer.ui.wizards.service;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.Collection;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.isqlviewer.ServiceReference;
import org.isqlviewer.sql.JdbcService;
import org.isqlviewer.sql.embedded.EmbeddedDatabase;
import org.isqlviewer.swing.SwingUtilities;
import org.isqlviewer.ui.laf.EnhancedListCellRenderer;
import org.isqlviewer.ui.wizards.AbstractWizardStep;
import org.isqlviewer.ui.wizards.WizardContext;
import org.isqlviewer.util.IsqlToolkit;
import org.isqlviewer.util.LocalMessages;
import org.isqlviewer.xml.ServiceDigester;
import org.xml.sax.InputSource;

public class SelectServiceStep extends AbstractWizardStep implements ListSelectionListener, MouseListener {

    private LocalMessages messages = new LocalMessages(ServiceWizard.BUNDLE_NAME);

    private DefaultListModel serviceList = new DefaultListModel();

    private int serviceSelection = -1;

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            if (serviceSelection >= 0) {
                WizardContext context = getContext();
                context.invokeNext();
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public boolean isFirst() {
        return false;
    }

    public boolean isLast() {
        return false;
    }

    @Override
    public void activate(WizardContext context) {
        super.activate(context);
        EmbeddedDatabase edb = EmbeddedDatabase.getSharedInstance();
        try {
            if (serviceList.isEmpty()) {
                Collection<ServiceReference> references = edb.getRegisteredServices();
                for (ServiceReference sr : references) {
                    serviceList.addElement(sr);
                }
            }
        } catch (SQLException error) {
            String message = messages.format("SelectServiceStep.failed_to_load_service_definitions");
            context.showErrorDialog(error, message);
        }
    }

    public boolean isValid(WizardContext context) {
        if (serviceSelection < 0) {
            return false;
        }
        ServiceReference selection = (ServiceReference) serviceList.getElementAt(serviceSelection);
        if (selection == null) {
            return false;
        }
        String function = (String) context.getAttribute(ServiceWizard.ATTRIBUTE_FUNCTION);
        context.setAttribute(ServiceWizard.ATTRIBUTE_SERVICE_REFERENCE, selection);
        URL url = selection.getResourceURL();
        InputStream inputStream = null;
        try {
            inputStream = url.openStream();
            InputSource inputSource = new InputSource(inputStream);
            JdbcService service = ServiceDigester.parseService(inputSource, IsqlToolkit.getSharedEntityResolver());
            context.setAttribute(ServiceWizard.ATTRIBUTE_SERVICE, service);
            return true;
        } catch (IOException error) {
            if (!ServiceWizard.FUNCTION_DELETE.equals(function)) {
                String loc = url.toExternalForm();
                String message = messages.format("SelectServiceStep.failed_to_load_service_from_url", loc);
                context.showErrorDialog(error, message);
            } else {
                return true;
            }
        } catch (Exception error) {
            String message = messages.format("SelectServiceStep.service_load_error", url.toExternalForm());
            context.showErrorDialog(error, message);
        }
        return false;
    }

    @Override
    public void init(WizardContext context) {
        super.init(context);
        setTitle(messages.getMessage("SelectServiceStep.title"));
        setComment(messages.getMessage("SelectServiceStep.comment"));
        setImage(SwingUtilities.loadIconResource(ServiceWizard.class, "service", 22));
        String tip = null;
        JComponent component = null;
        GridBagConstraints constraint = null;
        JPanel panel = new JPanel(new GridBagLayout());
        setView(panel);
        tip = messages.format("SelectServiceStep.list_view.tip");
        component = new JList(serviceList);
        component.addMouseListener(this);
        ((JList) component).addListSelectionListener(this);
        ((JList) component).setCellRenderer(new ServiceReferenceRenderer());
        component.setToolTipText(tip);
        constraint = ServiceWizard.constrain(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
        panel.add(new JScrollPane(component), constraint);
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e == null) {
            serviceSelection = -1;
        } else if (!e.getValueIsAdjusting()) {
            serviceSelection = e.getFirstIndex();
        }
    }

    private static class ServiceReferenceRenderer extends EnhancedListCellRenderer {

        private static final long serialVersionUID = 5174787602448379313L;

        @Override
        public String getText(Object aObject) {
            if (aObject instanceof ServiceReference) {
                return ((ServiceReference) aObject).getName();
            }
            return super.getText(aObject);
        }

        @Override
        public Icon getIcon(Object aObject) {
            if (aObject instanceof ServiceReference) {
                return SwingUtilities.loadIconResource("service", 22);
            }
            return super.getIcon(aObject);
        }
    }
}
