package com.objectwave.templateMerge.gui;

import com.objectwave.exception.*;
import com.objectwave.logging.MessageLog;
import com.objectwave.sourceGenerator.ClassInformation;
import com.objectwave.templateMerge.*;
import com.objectwave.utility.FileFinder;
import com.objectwave.utility.TreeCollection;
import java.awt.FileDialog;
import java.beans.*;
import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import javax.swing.tree.TreeModel;

/**
 *  The behavior of the core TemplateGui screen. This class could easily be used
 *  as a nonvisual component integrated into any system.
 *
 * @author  Dave Hoag
 * @version  $Id: TemplateGuiModel.java,v 2.1 2001/10/19 14:21:00 dave_hoag Exp $
 * @see  com.objectwave.templateMerge.gui.TemplateGuiVIF
 * @see  com.objectwave.templateMerge.gui.TemplateGui
 */
public class TemplateGuiModel implements TemplateGuiVIF, PropertyChangeListener {

    KnownTemplates workingTemplates;

    java.awt.Frame gui;

    PropertyChangeSupport eventList = new PropertyChangeSupport(this);

    javax.swing.tree.TreeModel templateTreeModel;

    String selectedTemplateName = null;

    TokenProvider tokenProvider;

    /**
	 *  KnownTemplates is a collection of known MergeTemplates.
	 */
    public TemplateGuiModel() {
        this(KnownTemplates.getDefaultInstance());
    }

    /**
	 * @param  known KnownTemplates is a collection of known MergeTemplates.
	 */
    public TemplateGuiModel(KnownTemplates known) {
        setWorkingTemplates(known);
    }

    /**
	 *  Used to generate serialized files from text template definitions. Useful
	 *  for command line builds. Changed by Zhou Cai Use importXML to get templates
	 *  from an XML file
	 *
	 * @param  args The command line arguments
	 */
    public static void main(String[] args) {
        TemplateGuiModel model = new TemplateGuiModel();
        if (args.length != 3) {
            System.out.println("Usage java com.objectwave.templateMerge.gui.TemplateGuiModel <TokenProvider> <InputFile> <OutputFile>");
        } else {
            try {
                Class c = Class.forName(args[0]);
                model.setTokenProvider((com.objectwave.templateMerge.TokenProvider) c.newInstance());
                URL url = new FileFinder().getUrl(model.getClass(), args[1]);
                InputStreamReader fr = new InputStreamReader(url.openStream());
                model.importFromXML(fr);
                model.workingTemplates.fileName = args[2];
                model.saveCurrentModel();
            } catch (Exception e) {
                MessageLog.error(TemplateGuiModel.class, "Error with token provider " + e, e);
                System.exit(1);
            }
        }
    }

    /**
	 *  Other operations will be selection dependent.
	 *
	 * @param  val The new SelectedTemplateName value
	 * @see  #requestTemplateEdit()
	 */
    public void setSelectedTemplateName(String val) {
        selectedTemplateName = val;
    }

    /**
	 * @param  gui The new TemplateGui value
	 */
    public void setTemplateGui(java.awt.Frame gui) {
        this.gui = gui;
    }

    /**
	 *  A token provider is the source of the InformationToken objects.
	 *
	 * @param  p The new TokenProvider value
	 */
    public void setTokenProvider(TokenProvider p) {
        tokenProvider = p;
    }

    /**
	 * @param  workingTemplates The new WorkingTemplates value
	 */
    public void setWorkingTemplates(KnownTemplates workingTemplates) {
        if (this.workingTemplates != workingTemplates) {
            if (this.workingTemplates != null) {
                this.workingTemplates.removePropertyChangeListener(this);
            }
            if (workingTemplates != null) {
                workingTemplates.addPropertyChangeListener(this);
            }
            this.workingTemplates = workingTemplates;
            createTreeModel();
        }
    }

    /**
	 *  Used internally to change the model. In addition to that it fires the
	 *  property change event. It is the only property that will fire this event.
	 *
	 * @param  mod The new TemplateTreeModel value
	 */
    protected void setTemplateTreeModel(javax.swing.tree.TreeModel mod) {
        TreeModel old = templateTreeModel;
        templateTreeModel = mod;
        eventList.firePropertyChange("templateTreeModel", old, mod);
    }

    /**
	 *  Useful when displaying the way a template will look.
	 *
	 * @param  selection The name of a known template.
	 * @param  fullView Should subtemplates be expanded?
	 * @return  The BufferForTemplate value
	 */
    public String getBufferForTemplate(String selection, boolean fullView) {
        MergeTemplate temp = getTemplateNamed(selection);
        if (temp == null) {
            return null;
        }
        return temp.browseLayoutBuffer(fullView).toString();
    }

    /**
	 *  The currently selected template. This is used for template context
	 *  operations.
	 *
	 * @return  String a Known template name.
	 */
    public String getSelectedTemplateName() {
        return selectedTemplateName;
    }

    /**
	 *  Return the TreeModel. This model was created via the 'createTreeModel'
	 *  method.
	 *
	 * @return  The TemplateTreeModel value
	 * @see  #createTreeModel
	 */
    public TreeModel getTemplateTreeModel() {
        return templateTreeModel;
    }

    /**
	 *  The tokenProvider contains all of the information tokens that can used in
	 *  the templates. This is useful for pick lists as well as converting a 'text'
	 *  defined template into the actual objects.
	 *
	 * @return  The TokenProvider value
	 */
    public TokenProvider getTokenProvider() {
        if (tokenProvider == null) {
            String className = System.getProperty("tokenProvider");
            if (className != null) {
                try {
                    Class tokenClass = Class.forName(className);
                    tokenProvider = (TokenProvider) tokenClass.newInstance();
                } catch (Exception ex) {
                    MessageLog.debug(this, "Failed to get configured token provider " + className + " , using default. ", ex);
                }
            }
            if (tokenProvider == null) {
                tokenProvider = new ClassInformation();
            }
        }
        return tokenProvider;
    }

    /**
	 * @return  The knownTemplate collection that this model is building/managing.
	 */
    public KnownTemplates getWorkingTemplates() {
        return workingTemplates;
    }

    /**
	 *  Find the template object with the given name.
	 *
	 * @param  name
	 * @return  The TemplateNamed value
	 */
    protected MergeTemplate getTemplateNamed(String name) {
        return getWorkingTemplates().getTemplate(name);
    }

    /**
	 *  Add yourself to be notified of property changes. As of 11/12/97 this only
	 *  occurred when setTemplateTreeModel().
	 *
	 * @param  l The feature to be added to the PropertyChangeListener attribute
	 * @see  #setTemplateTreeModel
	 */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        eventList.addPropertyChangeListener(l);
    }

    /**
	 *  Write ascii code to the buffer containing the MergeTemplate definitions.
	 *  Usefull if you don't want to rely upon serialized objects.
	 *
	 * @param  buff The output buffer upon which to write the ascii version of the
	 *      templates.
	 * @exception  IOException Any exceptions related to writing to the parameter.
	 */
    public void dumpToText(java.io.BufferedWriter buff) throws IOException {
        Enumeration e = workingTemplates.getList().elements();
        MergeTemplateWriter.dumpToText(e, buff);
    }

    /**
	 *  Write the templates into the the buffer of an XML file. Usefull if you
	 *  don't want to rely upon serialized objects.
	 *
	 * @param  buff BufferedWriter The output buffer of an XML file
	 * @throws  ConfigurationException Receive the exception from
	 *      MergeTemplateWriter
	 * @author  Zhou Cai
	 */
    public void dumpToXML(java.io.BufferedWriter buff) throws ConfigurationException {
        Enumeration e = workingTemplates.getList().elements();
        MergeTemplateWriter.dumpToXML(e, buff);
    }

    /**
	 *  Read ascii code from the buffer containing the MergeTemplate definitions.
	 *  Usefull if you don't want to rely upon serialized objects.
	 *
	 * @param  buff
	 * @exception  IOException
	 * @see  dumpToText
	 */
    public void importFromText(java.io.BufferedReader buff) throws IOException {
        if (workingTemplates != null) {
            workingTemplates.removePropertyChangeListener(this);
        }
        MergeTemplateWriter.setTokenList(getTokenProvider().getKnownTokens());
        KnownTemplates temps = MergeTemplateWriter.importFromText(buff);
        if (workingTemplates == null) {
            workingTemplates = temps;
        } else {
            Enumeration e = temps.getList().elements();
            while (e.hasMoreElements()) {
                workingTemplates.addTemplate((MergeTemplate) e.nextElement());
            }
        }
        workingTemplates.addPropertyChangeListener(this);
        createTreeModel();
    }

    /**
	 *  Read ascii code from the buffer containing the MergeTemplate definitions.
	 *  Usefull if you don't want to rely upon serialized objects.
	 *
	 * @param  xmlFile FileReader The input stream of XML file
	 * @see  dumpToText
	 * @throws  ConfigurationException If any error happens in the XML file
	 *      processing
	 * @auther  Zhou Cai
	 */
    public void importFromXML(final Reader xmlFile) throws ConfigurationException {
        if (workingTemplates != null) {
            workingTemplates.removePropertyChangeListener(this);
        }
        MergeTemplateWriter.setTokenList(getTokenProvider().getKnownTokens());
        KnownTemplates temps = MergeTemplateWriter.importFromXML(xmlFile);
        if (workingTemplates == null) {
            workingTemplates = temps;
        } else {
            Enumeration e = temps.getList().elements();
            while (e.hasMoreElements()) {
                workingTemplates.addTemplate((MergeTemplate) e.nextElement());
            }
        }
        workingTemplates.addPropertyChangeListener(this);
        createTreeModel();
    }

    /**
	 *  Load the model from an ObjectStream. Relies on Java serialization.
	 */
    public void loadModel() {
        FileDialog fd = new FileDialog(gui, "Select file to load.");
        fd.setMode(FileDialog.LOAD);
        fd.show();
        String file = fd.getFile();
        if (file == null) {
            return;
        }
        String fileName = fd.getDirectory() + file;
        try {
            setWorkingTemplates(KnownTemplates.readFile(fileName));
            KnownTemplates.setDefaultInstance(workingTemplates);
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    /**
	 *  Change the workingTemplatesInstance.
	 */
    public void newModel() {
        setWorkingTemplates(new KnownTemplates());
    }

    /**
	 *  Only called when our knownSourceTemplates have been modified.
	 *
	 * @param  evt
	 */
    public void propertyChange(PropertyChangeEvent evt) {
        createTreeModel();
    }

    /**
	 *  No longer listen for property change events.
	 *
	 * @param  l
	 */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        eventList.removePropertyChangeListener(l);
    }

    /**
	 *  Create a new template.
	 */
    public void requestNewTemplate() {
        TemplateModGui scrn = new TemplateModGui(null, workingTemplates);
        scrn.setTokenProvider(getTokenProvider());
        scrn.setVisible(true);
    }

    /**
	 *  Edit the selected template.
	 */
    public void requestTemplateDelete() {
        MergeTemplate temp = getTemplateNamed(getSelectedTemplateName());
        if (temp == null) {
            return;
        }
        workingTemplates.remove(temp);
        createTreeModel();
    }

    /**
	 *  Edit the selected template.
	 */
    public void requestTemplateEdit() {
        MergeTemplate temp = getTemplateNamed(getSelectedTemplateName());
        if (temp == null) {
            return;
        }
        TemplateModGui scrn = new TemplateModGui(temp, workingTemplates);
        scrn.setTokenProvider(getTokenProvider());
        scrn.setVisible(true);
    }

    /**
	 *  Using serialization, save the current templates.
	 */
    public void saveCurrentModel() {
        if (workingTemplates == null) {
            return;
        }
        if (workingTemplates.fileName != null) {
            try {
                workingTemplates.save();
            } catch (IOException e) {
            }
            return;
        } else {
            FileDialog fd = new FileDialog(gui, "Enter File Name");
            fd.setMode(FileDialog.SAVE);
            fd.show();
            String file = fd.getFile();
            if (file == null) {
                return;
            }
            String fileName = fd.getDirectory() + file;
            workingTemplates.fileName = fileName;
            try {
                workingTemplates.save();
            } catch (IOException e) {
            }
        }
    }

    /**
	 *  Using the KnownMergeTemplates found in 'workingTemplates', create a
	 *  TreeModel. Often used by screens to display the model.
	 */
    protected void createTreeModel() {
        if (workingTemplates == null) {
            setTemplateTreeModel(new TreeCollection());
            return;
        }
        TreeCollection newTreeModel = null;
        newTreeModel = workingTemplates.asTreeCollection();
        Enumeration e = newTreeModel.elements();
        while (e.hasMoreElements()) {
            Object obj = e.nextElement();
            MergeTemplate temp = (MergeTemplate) obj;
            newTreeModel.replace(temp, temp.getTemplateName());
        }
        setTemplateTreeModel(newTreeModel);
    }
}
