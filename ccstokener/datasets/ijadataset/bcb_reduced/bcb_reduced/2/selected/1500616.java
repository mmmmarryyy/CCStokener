package net.etherstorm.jopenrpg.swing.nodehandlers;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import net.etherstorm.jopenrpg.swing.JFileEntryField;
import net.etherstorm.jopenrpg.swing.actions.DefaultAction;
import net.etherstorm.jopenrpg.util.ExceptionHandler;
import org.jdom.Element;

/**
 * 
 * 
 * 
 * $Date: 2004/04/01 02:49:02 $<br>
 * @author tedberg
 * @author $Author: tedberg $
 * @version $Revision: 1.5 $
 * @since Aug 20, 2003
 */
class EditExternalScriptAction extends DefaultAction {

    public static final String PROPERTY_HANDLER = "handler";

    protected ExternalScriptHandler _handler;

    /**
	 * 
	 */
    public EditExternalScriptAction() {
        initProperties("EditExternalScriptAction");
    }

    /**
	 * @param handler
	 */
    public EditExternalScriptAction(ExternalScriptHandler handler) {
        this();
        setHandler(handler);
    }

    /**
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
    public void actionPerformed(ActionEvent evt) {
        try {
            String args[] = new String[2];
            PreferencesNode pn = new PreferencesNode();
            args[0] = pn.getEditor();
            args[1] = getHandler().getFilename();
            Runtime.getRuntime().exec(args);
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * @return
	 */
    public ExternalScriptHandler getHandler() {
        if (_handler == null) setHandler(new ExternalScriptHandler());
        return _handler;
    }

    /**
	 * @param val
	 */
    public void setHandler(ExternalScriptHandler val) {
        try {
            if (val.equals(_handler)) return;
        } catch (Exception ex) {
            return;
        }
        ExternalScriptHandler oldval = _handler;
        _handler = val;
        firePropertyChange(PROPERTY_HANDLER, oldval, _handler);
    }
}

/**
 * 
 * 
 * 
 * $Date: 2004/04/01 02:49:02 $<br>
 * @author tedberg
 * @author $Author: tedberg $
 * @version $Revision: 1.5 $
 * @since Aug 20, 2003
 */
class EditExternalScriptHandlerAction extends DefaultAction {

    public static final String PROPERTY_HANDLER = "handler";

    protected ExternalScriptHandler _handler;

    /**
	 * 
	 */
    public EditExternalScriptHandlerAction() {
        initProperties("EditExternalScriptHandlerAction");
    }

    /**
	 * @param handler
	 */
    public EditExternalScriptHandlerAction(ExternalScriptHandler handler) {
        this();
        setHandler(handler);
    }

    /**
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
    public void actionPerformed(ActionEvent evt) {
        getHandler().doEdit();
    }

    /**
	 * @return
	 */
    public ExternalScriptHandler getHandler() {
        if (_handler == null) setHandler(new ExternalScriptHandler());
        return _handler;
    }

    /**
	 * @param val
	 */
    public void setHandler(ExternalScriptHandler val) {
        try {
            if (val.equals(_handler)) return;
        } catch (Exception ex) {
            return;
        }
        ExternalScriptHandler oldval = _handler;
        _handler = val;
        firePropertyChange(PROPERTY_HANDLER, oldval, _handler);
    }
}

/**
 * 
 * 
 * 
 * $Date: 2004/04/01 02:49:02 $<br>
 * @author tedberg
 * @author $Author: tedberg $
 * @version $Revision: 1.5 $
 * @since Aug 20, 2003
 */
public class ExternalScriptHandler extends BaseNodehandler {

    public static final String PROPERTY_FILENAME = "filename";

    protected String _filename;

    protected JPanel _panel;

    /**
	 * 
	 */
    public ExternalScriptHandler() {
        super();
        setNodeName("Script");
        setNodeImageName("gear");
        setNodeVersion("0.0.1");
        setNodeType("externalScriptHandler");
    }

    /**
	 * @param e
	 */
    public ExternalScriptHandler(Element e) {
        super(e);
    }

    /**
	 * 
	 */
    public void doEdit() {
        super.openHandler();
    }

    /**
	 * 
	 * @see net.etherstorm.jopenrpg.swing.nodehandlers.BaseNode#fromXML(org.jdom.Element)
	 */
    public void fromXML(Element e) {
        super.fromXML(e);
        setFilename((String) e.getAttributeValue("filename"));
    }

    /**
	 * @return
	 */
    public String getFilename() {
        if (_filename == null) setFilename(new String());
        return _filename;
    }

    /**
	 * 
	 * @see net.etherstorm.jopenrpg.swing.nodehandlers.BaseNodehandler#getPanel()
	 */
    public JPanel getPanel() {
        if (_panel == null) _panel = new JExternalScriptHandlerEditor(this);
        return _panel;
    }

    /**
	 * 
	 * @see net.etherstorm.jopenrpg.swing.nodehandlers.BaseNodehandler#openHandler()
	 */
    public void openHandler() {
        try {
            URL url = new File(getFilename()).toURL();
            referenceManager.getPythonInterpreter().execfile(url.openStream());
            return;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            try {
                URL url = new URL(getFilename());
                referenceManager.getPythonInterpreter().execfile(url.openStream());
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        referenceManager.getPythonInterpreter().execfile(getFilename());
    }

    /**
	 * 
	 * @see net.etherstorm.jopenrpg.swing.nodehandlers.BaseNodehandler#populatePopupMenu(javax.swing.JPopupMenu)
	 */
    public void populatePopupMenu(JPopupMenu menu) {
        super.populatePopupMenu(menu);
        menu.add(new EditExternalScriptHandlerAction(this));
        menu.add(new EditExternalScriptAction(this));
    }

    /**
	 * @param val
	 */
    public void setFilename(String val) {
        try {
            if (val.equals(_filename)) return;
        } catch (Exception ex) {
            return;
        }
        String oldval = _filename;
        _filename = val;
        firePropertyChange(PROPERTY_FILENAME, oldval, _filename);
    }

    /**
	 * 
	 * @see net.etherstorm.jopenrpg.swing.nodehandlers.BaseNode#toXML()
	 */
    public Element toXML() {
        Element elem = super.toXML();
        elem.setAttribute("filename", getFilename());
        return elem;
    }
}

/**
 * 
 * 
 * 
 * $Date: 2004/04/01 02:49:02 $<br>
 * @author tedberg
 * @author $Author: tedberg $
 * @version $Revision: 1.5 $
 * @since Aug 20, 2003
 */
class JExternalScriptHandlerEditor extends JPanel implements PropertyChangeListener {

    public static final String PROPERTY_HANDLER = "handler";

    protected ExternalScriptHandler _handler;

    JButton editButton;

    JFileEntryField fileEntry;

    /**
	 * 
	 */
    public JExternalScriptHandlerEditor() {
        super(new java.awt.BorderLayout());
        Box box = Box.createVerticalBox();
        fileEntry = new JFileEntryField();
        fileEntry.setLabel("Script");
        fileEntry.setMnemonic('s');
        editButton = new JButton();
        box.add(fileEntry);
        box.add(editButton);
        add(box);
        fileEntry.addPropertyChangeListener(this);
    }

    /**
	 * @param h
	 */
    public JExternalScriptHandlerEditor(ExternalScriptHandler h) {
        this();
        setHandler(h);
        editButton.setAction(new EditExternalScriptAction(h));
    }

    /**
	 * @return
	 */
    public ExternalScriptHandler getHandler() {
        if (_handler == null) setHandler(new ExternalScriptHandler());
        return _handler;
    }

    /**
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
    public void propertyChange(PropertyChangeEvent pce) {
        if (pce.getPropertyName().equals(JFileEntryField.PROPERTY_FILENAME)) {
            getHandler().setFilename((String) pce.getNewValue());
        }
    }

    /**
	 * @param val
	 */
    public void setHandler(ExternalScriptHandler val) {
        try {
            if (val.equals(_handler)) return;
        } catch (Exception ex) {
            return;
        }
        ExternalScriptHandler oldval = _handler;
        _handler = val;
        fileEntry.setFilename(_handler.getFilename());
        firePropertyChange(PROPERTY_HANDLER, oldval, _handler);
    }
}
