package org.learnaholic.application;

import java.awt.Frame;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;
import org.learnaholic.application.model.MemoryItems;
import org.learnaholic.application.model.MemoryItemsEvent;
import org.learnaholic.application.model.MemoryItemsImpl;
import org.learnaholic.application.model.MemoryListener;
import org.learnaholic.application.model.TestDialogDefinition;
import org.learnaholic.application.model.xml.ItemListParser;
import org.learnaholic.application.model.xml.ItemListWriter;
import org.learnaholic.application.model.xml.LearnaholicXmlException;
import org.learnaholic.application.test.TestImpl;
import org.learnaholic.application.ui.LearnaholicUi;

/**
 * 
 */
public class LearnaholicImpl implements Learnaholic {

    /** The singleton instance */
    private static LearnaholicImpl instance;

    /** The application UI */
    private final LearnaholicUi ui;

    /** The application model */
    private final MemoryItems model;

    /** The application model */
    private TestDialogDefinition testDialogDefinition;

    /** The model listener */
    private final MemoryEventHandler memoryEventHandler = new MemoryEventHandler();

    private final ItemListWriter itemListWriter = new ItemListWriter();

    private final ItemListParser itemListParser = new ItemListParser();

    public static void initialise(LearnaholicUi ui) {
        instance = new LearnaholicImpl(ui);
    }

    /**
	 * Singleton getter.
	 * 
	 * @return the singleton instance.
	 */
    public static Learnaholic getInstance() {
        if (null == instance) {
            throw new NullPointerException("Call initialise(ui) first.");
        }
        return instance;
    }

    /**
	 * Sole private constructor.
	 */
    private LearnaholicImpl(LearnaholicUi ui) {
        this.ui = ui;
        model = new MemoryItemsImpl();
        model.addMemoryListener(memoryEventHandler);
    }

    /**
	 * Releases the resource.
	 */
    public static void dispose() {
        instance = null;
    }

    /**
	 * @param list
	 */
    public void loadList(MemoryItems items) {
        model.putAll(items);
    }

    public MemoryItems getModel() {
        return model;
    }

    /**
	 * 
	 */
    public void start() {
        InputStream listData = null;
        InputStream resultData = null;
        try {
            URL listUrl = new URL("http://i-gnoramus.googlecode.com/svn/trunk/I-gnoramus/prototype/res/ibis_french.xml");
            listData = listUrl.openStream();
            URL resultUrl = new URL("http://i-gnoramus.googlecode.com/svn/trunk/I-gnoramus/prototype/res/ibis_results.xml");
            resultData = resultUrl.openStream();
            loadXmlList(listData, resultData);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                listData.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                resultData.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        InputStream testDialogDefinitionData = null;
        try {
            URL url = new URL("http://i-gnoramus.googlecode.com/svn/trunk/I-gnoramus/prototype/res/testdialog.xml");
            testDialogDefinitionData = url.openStream();
            testDialogDefinition = itemListParser.loadTestDialogDefinition(testDialogDefinitionData);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                testDialogDefinitionData.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadXmlList(InputStream xmlList, InputStream resultsXml) {
        try {
            loadList(itemListParser.loadXml(xmlList, resultsXml));
        } catch (LearnaholicXmlException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 
	 */
    public void save() {
        try {
            FileWriter fileWriter = new FileWriter("res/results.xml");
            itemListWriter.write(fileWriter, model);
            fileWriter.flush();
            fileWriter.close();
            setBlurred(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 
	 */
    public void stop() {
        System.exit(0);
    }

    public void startTest(Set<String> items) {
        try {
            new TestImpl((Frame) ui, model, testDialogDefinition).startTest(model.getSubMap(items));
        } catch (LearnaholicException e) {
            e.printStackTrace();
        }
    }

    /**
	 * The model event handler.
	 */
    private class MemoryEventHandler implements MemoryListener {

        public void itemsAdded(MemoryItemsEvent evt) {
            ui.addItems(evt.getItems());
        }

        public void itemsRemoved(MemoryItemsEvent evt) {
            ui.removeItems(evt.getItems());
        }
    }

    /**
	 * 
	 */
    public void setBlurred(boolean blurred) {
        ui.setBlurred(blurred);
    }
}
