package eu.planets_project.ifr.core.storage.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.BeforeClass;
import org.junit.Test;
import eu.planets_project.ifr.core.storage.api.DataRegistry;
import eu.planets_project.ifr.core.storage.api.DataRegistryFactory;
import eu.planets_project.ifr.core.storage.api.DigitalObjectManager;
import eu.planets_project.ifr.core.storage.api.DataRegistry.DigitalObjectManagerNotFoundException;
import eu.planets_project.ifr.core.storage.api.DigitalObjectManager.DigitalObjectNotFoundException;
import eu.planets_project.ifr.core.storage.api.DigitalObjectManager.DigitalObjectNotStoredException;
import eu.planets_project.services.datatypes.Content;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.DigitalObjectContent;
import eu.planets_project.services.utils.test.TestFile;

/**
 * @author CFWilson
 *
 */
public class DataRegistryTests {

    private static TestFile[] testFiles = new TestFile[] { TestFile.HTML, TestFile.RTF, TestFile.TXT, TestFile.XML };

    private static DataRegistry dataReg = null;

    /**
	 * @throws java.lang.Exception
	 */
    @BeforeClass
    public static void setUp() throws Exception {
        DataRegistryTests.dataReg = DataRegistryFactory.getDataRegistry();
    }

    @SuppressWarnings("boxing")
    @Test
    public final void testPreLoadList() {
        assertEquals("DataRegistry.list(null).size() expected to equal DataRegistry.countDigitalObjectManagers", DataRegistryTests.dataReg.list(null).size(), DataRegistryTests.dataReg.countDigitalObjectMangers());
        for (URI uri : DataRegistryTests.dataReg.list(null)) {
            try {
                assertEquals("Expected DataRegistryTests.dataReg.list(uri) to equal " + "DataRegistryTests.dataReg.getDigitalObjectManager(uri).list(uri)", DataRegistryTests.dataReg.list(uri), DataRegistryTests.dataReg.getDigitalObjectManager(uri).list(uri));
            } catch (DigitalObjectManagerNotFoundException e) {
                e.printStackTrace();
                fail("URI from list(uri) " + uri + " threw DigitalObjectManagerNotFoundException " + e.getMessage());
            }
        }
    }

    /**
	 * Test consistency of hasDigitalObjectManager()
	 * 
	 * Test method for {@link eu.planets_project.ifr.core.storage.api.DataRegistryImpl#hasDigitalObjectManager(java.net.URI)}.
	 */
    @Test
    public final void testPreLoadHasDigitalObjectManager() {
        for (URI uri : DataRegistryTests.dataReg.list(null)) {
            assertTrue("Expected hasDigitalObjectManager(uri) true for " + uri, DataRegistryTests.dataReg.hasDigitalObjectManager(uri));
        }
    }

    /**
	 * Test consistency of hasDigitalObjectManager()
	 * 
	 * Test method for {@link eu.planets_project.ifr.core.storage.api.DataRegistryImpl#hasDigitalObjectManager(java.net.URI)}.
	 * @throws DigitalObjectManagerNotFoundException 
	 */
    @Test
    public final void testPreLoadIsWriteable() {
        for (URI uri : DataRegistryTests.dataReg.list(null)) {
            try {
                assertEquals("Expected DataRegistry.isWritable(uri) to equal DataRegisty.getDigitalObjectManager(uri).isWritable(uri) for " + uri, DataRegistryTests.dataReg.isWritable(uri), DataRegistryTests.dataReg.getDigitalObjectManager(uri).isWritable(uri));
            } catch (DigitalObjectManagerNotFoundException e) {
                e.printStackTrace();
                fail("URI from list(uri) " + uri + " threw DigitalObjectManagerNotFoundException " + e.getMessage());
            }
        }
    }

    /**
	 * Test method for {@link eu.planets_project.ifr.core.storage.api.DataRegistryImpl#getDigitalObjectManager(java.net.URI)}.
	 * @throws DigitalObjectManagerNotFoundException 
	 */
    @Test
    public final void testPreLoadGetDigitalObjectManager() {
        for (URI uri : DataRegistryTests.dataReg.list(null)) {
            try {
                assertNotNull("DataRegistry.getDigitalObjectManager(uri) for " + uri + " should not be null", DataRegistryTests.dataReg.getDigitalObjectManager(uri));
            } catch (DigitalObjectManagerNotFoundException e) {
                e.printStackTrace();
                fail("URI from list(uri) " + uri + " threw DigitalObjectManagerNotFoundException " + e.getMessage());
            }
        }
    }

    /**
	 * Test method for {@link eu.planets_project.ifr.core.storage.api.DataRegistryImpl#storeAsNew(eu.planets_project.services.datatypes.DigitalObject)}.
	 */
    @Test
    public final void testPreLoadStoreAsNewDefault() {
        try {
            DigitalObjectManager dom = DataRegistryTests.dataReg.getDefaultDigitalObjectManager();
            this.testStoreAsNew(null, dom);
        } catch (DigitalObjectManagerNotFoundException e) {
            e.printStackTrace();
            fail("DataRegistry.getDefaultDigitalObjectManager threw DigitalObjectManagerNotFoundException " + e.getMessage());
        }
    }

    /**
	 * Test method for {@link eu.planets_project.ifr.core.storage.api.DataRegistryImpl#storeAsNew(java.net.URI, eu.planets_project.services.datatypes.DigitalObject)}.
	 */
    @Test
    public final void testPreLoadStoreAsNew() {
        for (URI uri : DataRegistryTests.dataReg.list(null)) {
            try {
                DigitalObjectManager dom = DataRegistryTests.dataReg.getDigitalObjectManager(uri);
                this.testStoreAsNew(uri, dom);
            } catch (DigitalObjectManagerNotFoundException e) {
                e.printStackTrace();
                fail("URI from list(uri) " + uri + " threw DigitalObjectManagerNotFoundException " + e.getMessage());
            }
        }
    }

    /**
	 * Test method for {@link eu.planets_project.ifr.core.storage.api.DataRegistryImpl#getDigitalObjectManager(java.net.URI)}.
	 * @throws DigitalObjectManagerNotFoundException 
	 */
    @Test
    public final void testPreLoadDeleteDigitalObjectManager() {
        for (URI uri : DataRegistryTests.dataReg.list(null)) {
            try {
                DataRegistryTests.dataReg.deleteDigitalObjectManager(uri);
            } catch (DigitalObjectManagerNotFoundException e) {
                e.printStackTrace();
                fail("URI from list(uri) " + uri + " threw DigitalObjectManagerNotFoundException " + e.getMessage());
            }
        }
        assertEquals("Expected DataRegistry.countDigitalObjectMangers() to be zero", DataRegistryTests.dataReg.countDigitalObjectMangers(), 0);
    }

    private void testStoreAsNew(URI uri, DigitalObjectManager dom) {
        if (!dom.isWritable(null)) return;
        for (TestFile file : DataRegistryTests.testFiles) {
            try {
                File testFile = new File(file.getLocation());
                URI purl = testFile.toURI();
                String name = testFile.getName();
                System.out.println("PURL is " + file.getLocation());
                DigitalObjectContent content = Content.byReference(purl.toURL().openStream());
                System.out.println("created content " + content);
                DigitalObject object = new DigitalObject.Builder(content).permanentUri(purl).title(purl.toString()).build();
                System.out.println("created object " + object);
                URI theLoc = null;
                if (uri != null) theLoc = DataRegistryTests.dataReg.storeAsNew(new URI(uri.toString() + "/" + name), object); else theLoc = DataRegistryTests.dataReg.storeAsNew(object);
                System.out.println("got theLoc = " + theLoc);
                DigitalObjectContent expectCont = Content.byReference(purl.toURL().openStream());
                DigitalObject expectObj = new DigitalObject.Builder(expectCont).build();
                DigitalObject retObject = dom.retrieve(theLoc);
                assertEquals("Retrieve Digital Object content doesn't match that stored", expectObj.getContent(), retObject.getContent());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                fail("Couldn't get URl from URI ");
            } catch (IOException e) {
                e.printStackTrace();
                fail("IOException accessing file");
            } catch (DigitalObjectNotStoredException e) {
                e.printStackTrace();
                fail("Couldn't store digital object");
            } catch (DigitalObjectNotFoundException e) {
                e.printStackTrace();
                fail("Couldn't retrieve stored object");
            } catch (URISyntaxException e) {
                e.printStackTrace();
                fail("Couldn't create URI for" + uri.toString() + " file " + file);
            }
        }
    }
}
