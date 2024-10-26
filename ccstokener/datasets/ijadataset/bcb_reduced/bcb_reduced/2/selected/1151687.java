package gov.sns.apps.pvbrowser;

import gov.sns.application.XalDocument;
import gov.sns.application.XalDocumentListener;
import gov.sns.application.XalWindow;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

/**
 * Provides a document for the application.
 * 
 * @author Chris Fowlkes
 */
public class PVBrowserSearchDocument extends XalDocument implements XalDocumentListener {

    /**
   * The data for the document.
   */
    private Properties data;

    /**
   * Holds the open PV documents.
   */
    private ArrayList openPVDocuments = new ArrayList();

    /**
   * Holds the model used to connect to the database.
   */
    private PVBrowserModel model;

    /**
   * Creates a new <CODE>PVBrowserSearchDocument</CODE>.
   * 
   * @param applicationModel The <CODE>PVBrowserModel</CODE> containing data shared by the application.
   */
    public PVBrowserSearchDocument(PVBrowserModel applicationModel) {
        setModel(applicationModel);
        data = new Properties();
    }

    /**
   * Creates a new <CODE>PVBrowserDocument</CODE> from the given 
   * <CODE>URL</CODE>.
   * 
   * @param url The <CODE>URL</CODE> from which to create the document.
   * @param applicationModel The <CODE>PVBrowserModel</CODE> containing data shared by the application.
   */
    public PVBrowserSearchDocument(URL url, PVBrowserModel applicationModel) {
        this(applicationModel);
        if (url != null) {
            try {
                data.loadFromXML(url.openStream());
                loadOpenPVsFromData();
                setHasChanges(false);
                setSource(url);
            } catch (java.io.IOException exception) {
                System.err.println(exception);
                displayWarning("Open Failed!", exception.getMessage(), exception);
            }
        }
    }

    /**
   * Loads the open PVs from the data.
   */
    private void loadOpenPVsFromData() {
        String openCountString = data.getProperty("openPVCount");
        if (openCountString != null) {
            int openPVCount = Integer.valueOf(openCountString).intValue();
            for (int i = 0; i < openPVCount; i++) {
                StringBuffer property = new StringBuffer("openPV");
                property.append(i);
                String pvPropertyName = property.toString();
                String pvName = data.getProperty(pvPropertyName);
                BrowserPV pv = new BrowserPV(pvName);
                PVBrowserPVDocument document = new PVBrowserPVDocument(pv, this);
                openPVDocuments.add(document);
            }
        }
    }

    /**
   * Creates the window for the document. 
   */
    @Override
    protected void makeMainWindow() {
        mainWindow = new PVBrowserSearchFrame(this);
    }

    /**
   * Saves the document to a URL.
   * 
   * @param url The URL to which this document should be saved.
   */
    @Override
    public void saveDocumentAs(URL url) {
        try {
            saveOpenPVsToData();
            File file = new File(url.getPath());
            if (!file.exists()) file.createNewFile();
            data.storeToXML(new FileOutputStream(file), null);
            setHasChanges(false);
        } catch (IOException exception) {
            System.err.println(exception);
            displayWarning("Save Failed!", "save to file '" + url.getPath() + "' failed.", exception);
        }
    }

    /**
   * Saves the open PV Documents to the data.
   */
    private void saveOpenPVsToData() {
        int openPVCount = openPVDocuments.size();
        data.setProperty("openPVCount", String.valueOf(openPVCount));
        for (int i = 0; i < openPVCount; i++) {
            StringBuffer property = new StringBuffer("openPV");
            property.append(i);
            String documentPropertyName = property.toString();
            PVBrowserPVDocument pvDocument = (PVBrowserPVDocument) openPVDocuments.get(i);
            data.setProperty(documentPropertyName, pvDocument.getPV().getID());
            property.append("x");
            XalWindow pvWindow = pvDocument.getMainWindow();
            Point pvWindowLocation = pvWindow.getLocation();
            data.setProperty(property.toString(), String.valueOf(pvWindowLocation.x));
            property = new StringBuffer(documentPropertyName);
            property.append("y");
            data.setProperty(property.toString(), String.valueOf(pvWindowLocation.y));
            Dimension pvWindowSize = pvWindow.getSize();
            property = new StringBuffer(documentPropertyName);
            property.append("width");
            data.setProperty(property.toString(), String.valueOf(pvWindowSize.width));
            property = new StringBuffer(documentPropertyName);
            property.append("height");
            data.setProperty(property.toString(), String.valueOf(pvWindowSize.height));
        }
    }

    /**
   * Gets the text for the PV name search field.
   * 
   * @return The text that should appear in the PV name search field.
   */
    public String getPVName() {
        return data.getProperty("pvName");
    }

    /**
   * Sets the text that appears in the PV name search field.
   * 
   * @param pvName The text from the PV name search field.
   */
    public void setPVName(String pvName) {
        if (!compare(pvName, getPVName())) {
            data.setProperty("pvName", pvName);
            setHasChanges(true);
        }
    }

    /**
   * Sets the values for the list associated with the given list name.
   * 
   * @param listName The name of the list for which to return the values.
   * @param values The values for the list for the given name.
   */
    public void setListValues(String listName, String[] values) {
        if (!compare(values, getListValues(listName))) saveArrayProperty(listName, values);
    }

    /**
   * Gets the values for the list associated with the given list name.
   * 
   * @param listName The name of the list for which to return the values.
   */
    public String[] getListValues(String listName) {
        return retrieveArrayProperty(listName);
    }

    /**
   * Sets the value of the all property.
   * 
   * @param all The value of the all property.
   */
    public void setAll(boolean all) {
        if (all ^ isAll()) {
            data.setProperty("all", String.valueOf(all));
            setHasChanges(true);
        }
    }

    /**
   * Gets the value of the all property.
   * 
   * @return
   */
    public boolean isAll() {
        return Boolean.valueOf(data.getProperty("all", "true")).booleanValue();
    }

    /**
   * Stores the given array in the instance of <CODE>Properties</CODE> that 
   * holds the data for the document. It can be retrieved by passing the given
   * property name to the <CODE>retrieveArrayProperty</CODE> method.
   * 
   * @param propertyName The key to use to save the array.
   * @param values The array to save.
   */
    private void saveArrayProperty(String propertyName, String[] values) {
        StringBuffer key = new StringBuffer(propertyName);
        key.append("Count");
        data.setProperty(key.toString(), String.valueOf(values.length));
        for (int i = 0; i < values.length; i++) {
            key = new StringBuffer(propertyName);
            key.append(i);
            data.setProperty(key.toString(), values[i]);
        }
        setHasChanges(true);
    }

    /**
   * Gets the saved array for the given key.
   * 
   * @param propertyName The key with which the array was saved.
   * @return The array saved with the given key.
   */
    private String[] retrieveArrayProperty(String propertyName) {
        StringBuffer key = new StringBuffer(propertyName);
        key.append("Count");
        String countString = data.getProperty(key.toString());
        if (countString == null) return null;
        int count = Integer.valueOf(countString);
        String[] values = new String[count];
        for (int i = 0; i < count; i++) {
            key = new StringBuffer(propertyName);
            key.append(i);
            values[i] = data.getProperty(key.toString());
        }
        return values;
    }

    /**
   * Checks to see if the two given arrays are equal. This method does ignores 
   * the order of the items and checks for <CODE>null</CODE>.
   * 
   * @param stringArray1 The first array to compare.
   * @param stringArray2 The second array to compare.
   * @return <CODE>true</CODE> if the arrays are equal or both <CODE>null</CODE>, <CODE>false</CODE> otherwise.
   */
    public boolean compare(String[] stringArray1, String[] stringArray2) {
        boolean arrayOneEmpty = stringArray1 == null || stringArray1.length == 0;
        boolean arrayTwoEmpty = stringArray2 == null || stringArray2.length == 0;
        if (arrayOneEmpty && arrayTwoEmpty) return true;
        if (arrayOneEmpty ^ arrayTwoEmpty) return false;
        if (stringArray1 == stringArray2) return true;
        if (stringArray1.length != stringArray2.length) return false;
        String[] sortedStringArray1 = new String[stringArray1.length];
        System.arraycopy(stringArray1, 0, sortedStringArray1, 0, stringArray1.length);
        Arrays.sort(sortedStringArray1);
        String[] sortedStringArray2 = new String[stringArray2.length];
        System.arraycopy(stringArray2, 0, sortedStringArray2, 0, stringArray2.length);
        Arrays.sort(sortedStringArray2);
        return Arrays.equals(sortedStringArray1, sortedStringArray2);
    }

    /**
   * Compares two instances of <CODE>String</CODE>, checking for 
   * <CODE>null</CODE>.
   * 
   * @param string1 The first <CODE>String</CODE> to compare.
   * @param string2 The Second <CODE>String</CODE> to compare.
   * @return <CODE>true</CODE> if the instances of <CODE>String</CODE> are equal or both are <CODE>null</CODE>, <CODE>false</CODE> otherwise.
   */
    private boolean compare(String string1, String string2) {
        if (string1 == null) return string2 == null; else return string1.equals(string2);
    }

    /**
   * Shows the given PV.
   * 
   * @param pv The PV to show.
   * @param dataType The DBD data type of the PV to show.
   * @param parentPV The parent of the PV.
   */
    public void showPV(BrowserPV pv, String dataType, String parentPV) {
        ((PVBrowserSearchFrame) getMainWindow()).showPV(pv, dataType, parentPV);
    }

    /**
   * Adds the document to the open document list.
   * 
   * @param document The <CODE>PVBrowserPVDocument</CODE> being opened.
   */
    public void addOpenPVDocument(PVBrowserPVDocument document) {
        openPVDocuments.add(document);
        setHasChanges(true);
        document.addXalDocumentListener(this);
    }

    /**
   * Gets the number of PV documents open.
   * 
   * @return The number of instances of <CODE>PVBrowserPVDocument</CODE> that are open.
   */
    public int getOpenPVDocumentCount() {
        return openPVDocuments.size();
    }

    /**
   * Returns the <CODE>PVBrowserPVDocument</CODE> at the given index.
   * 
   * @param index The index of the <CODE>PVBrowserPVDocument</CODE> to return.
   * @return The <CODE>PVBrowserPVDocument</CODE> at the given index.
   */
    public PVBrowserPVDocument getOpenPVDocumentAt(int index) {
        return (PVBrowserPVDocument) openPVDocuments.get(index);
    }

    /**
   * Called when the title of one of the open PV documents changes. This method 
   * does nothing.
   * 
   * @param document The <CODE>XalDocument</CODE> whose title changed.
   * @param newTitle The new title of the document.
   */
    public void titleChanged(XalDocument document, String newTitle) {
    }

    /**
   * Called when the has changes flag of one of the open PV documents changes. 
   * This method does nothing.
   * 
   * @param document The <CODE>XalDocument</CODE> whose title changed.
   * @param newHasChangesStatus The new title of the document.
   */
    public void hasChangesChanged(XalDocument document, boolean newHasChangesStatus) {
    }

    /**
   * Called before one of the open PV documents closes. This method does 
   * nothing.
   * 
   * @param document The <CODE>XalDocument</CODE> being closed.
   */
    public void documentWillClose(XalDocument document) {
    }

    /**
   * Called after one of the open PV documents closes.
   * 
   * @param document The <CODE>XalDocument</CODE> that was closed.
   */
    public void documentHasClosed(XalDocument document) {
        openPVDocuments.remove(document);
    }

    /**
   * Finds the location of the window at the given index when last saved.
   * 
   * @param index The index of the open PV document of which to find the last location.
   * @return The location the open PV document was at the given index was at when the document was last saved.
   */
    public Point lookupLastPVWindowLocation(int index) {
        int x = findOpenPVIntValue(index, "x");
        int y = findOpenPVIntValue(index, "y");
        return new Point(x, y);
    }

    /**
   * Finds the size of the window at the given index when last saved.
   * 
   * @param index The index of the open PV document of which to find the last size.
   * @return The size of the open PV document at the given index when the document was last saved.
   */
    public Dimension lookupLastPVWindowSize(int index) {
        int width = findOpenPVIntValue(index, "width");
        int height = findOpenPVIntValue(index, "height");
        return new Dimension(width, height);
    }

    /**
   * Finds the value in the stored data with a property named openPVindexvalue 
   * where index and value are replaced with the values passed into the method.
   * The value is converted and returned as an <CODE>int</CODE>.
   * 
   * @param index The index of the value to return.
   * @param value The name of the value at the given index to return.
   * @return The value for the given parameters as an <CODE>int</CODE>.
   */
    private int findOpenPVIntValue(int index, String value) {
        StringBuffer property = new StringBuffer("openPV");
        property.append(index);
        property.append(value);
        String propertyName = property.toString();
        String propertyValue = data.getProperty(propertyName);
        Integer integerValue = Integer.valueOf(propertyValue);
        return integerValue.intValue();
    }

    /**
   * Sets the <CODE>PVBrowserModel</CODE> used to do searches in the RDB.
   * 
   * @param model The model used to do searches.
   */
    public void setModel(PVBrowserModel model) {
        this.model = model;
    }

    /**
   * Gets the <CODE>PVBrowserSearchModel</CODE> used to do searches in the RDB.
   * 
   * @return The model used to do searches.
   */
    public PVBrowserModel getModel() {
        return model;
    }
}
