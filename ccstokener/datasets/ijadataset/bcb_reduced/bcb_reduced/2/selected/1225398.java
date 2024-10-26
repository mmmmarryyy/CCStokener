package org.artspline.webresource.junit;

import junit.framework.TestCase;
import org.artspline.webresource.ejb.session.storagemanager.StorageManager;
import org.artspline.webresource.exception.ResourceAlreadyExistException;
import org.artspline.webresource.exception.ResourceNotFoundException;
import org.artspline.webresource.exception.DirectoryIsNotEmpltyException;
import org.artspline.webresource.transport.dao.ResourceDao;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Collection;
import java.util.Vector;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by: jhm
 * Date: 14.01.2004
 * Time: 1:02:10
 */
public class ResourceManagerTest extends TestCase {

    private ResourceManagerTestWrapper wrapper = ResourceManagerTestWrapper.getInstance();

    public ResourceManagerTest(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        wrapper.clearAll();
    }

    public void testMimeTranscoding() throws Exception {
        BASE64Encoder encoder = new BASE64Encoder();
        String encodedString = encoder.encodeBuffer(ResourceManagerTestWrapper.originalData.getBytes());
        BASE64Decoder decoder = new BASE64Decoder();
        String restoredData = new String(decoder.decodeBuffer(encodedString));
        assertEquals(ResourceManagerTestWrapper.originalData, restoredData);
        ResourceDao origDao = new ResourceDao(null, ResourceDao.FileType, "testName", "Test file for JUnit", "text/pline", "text/pline", new Long(ResourceManagerTestWrapper.originalData.getBytes().length), wrapper.getAttributes(), ResourceManagerTestWrapper.originalData.getBytes());
        restoredData = new String(origDao.getResourceContentBypes());
        assertEquals(ResourceManagerTestWrapper.originalData, restoredData);
        BufferedReader contentReader = new BufferedReader(new InputStreamReader(origDao.getResourceContentStream()));
        restoredData = "";
        while (contentReader.ready()) {
            restoredData += contentReader.readLine();
            if (contentReader.ready()) restoredData += "\n";
        }
        assertEquals(ResourceManagerTestWrapper.originalData, restoredData);
        origDao = new ResourceDao(null, ResourceDao.FileType, "testName", "Test file for JUnit", "text/pline", "text/pline", new Long(ResourceManagerTestWrapper.originalData.getBytes().length), wrapper.getAttributes(), new ByteArrayInputStream(ResourceManagerTestWrapper.originalData.getBytes()));
        restoredData = new String(origDao.getResourceContentBypes());
        assertEquals(ResourceManagerTestWrapper.originalData, restoredData);
        contentReader = new BufferedReader(new InputStreamReader(origDao.getResourceContentStream()));
        restoredData = "";
        while (contentReader.ready()) {
            restoredData += contentReader.readLine();
            if (contentReader.ready()) restoredData += "\n";
        }
        assertEquals(ResourceManagerTestWrapper.originalData, restoredData);
    }

    public void testStorageCreation() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        StorageManager manager = wrapper.managerHome.create();
        assertNotNull("StorageManager instance souldn't be null", manager);
    }

    public void testResourceStore() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        ResourceDao resourceDao = wrapper.generateResourceDao();
        ResourceDao insertionResult = wrapper.create("/", resourceDao);
        assertEquals("Insertion result should be equvivalent", insertionResult.getName(), resourceDao.getName());
        assertEquals("Insertion result should be equvivalent", insertionResult.getDescription(), resourceDao.getDescription());
    }

    public void testResourceFinding() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        for (int i = 0; i < 10; i++) {
            ResourceDao dao = wrapper.generateResourceDao();
            wrapper.create("/", dao);
        }
        for (Iterator iterator = wrapper.pathes.iterator(); iterator.hasNext(); ) {
            String path = (String) iterator.next();
            ResourceDao found = null;
            try {
                found = wrapper.manager.findResource(path);
            } catch (ResourceNotFoundException e) {
                assertTrue("Resource <" + path + "> not found", true);
            }
            assertNotNull("Found resource should be not null", found);
            assertEquals("The found resource should have at least the same name", "/" + found.getName(), path);
        }
        ResourceDao wrongNameResult = null;
        boolean isThrowed = false;
        try {
            wrongNameResult = wrapper.manager.findResource("/1212TestFile");
        } catch (ResourceNotFoundException e) {
            isThrowed = true;
        }
        assertNull("Should be null", wrongNameResult);
        assertTrue("Exception should be throwed in resource unexisting case", isThrowed);
    }

    public void testResourceDuplicate() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        boolean isThrows = false;
        ResourceDao resourceDao = wrapper.generateResourceDao();
        ResourceDao insertionResult = wrapper.create("/", resourceDao);
        assertEquals("Insertion insertionResult should be equvivalent", insertionResult.getName(), resourceDao.getName());
        assertTrue(insertionResult.getDescription().equals(resourceDao.getDescription()));
        try {
            ResourceDao insertionResult2 = wrapper.create("/", resourceDao);
            assertEquals("Insertion insertionResult should be equvivalent", insertionResult2.getName(), resourceDao.getName());
            assertTrue(insertionResult2.getDescription().equals(resourceDao.getDescription()));
        } catch (ResourceAlreadyExistException e) {
            isThrows = true;
        }
        assertTrue("Exception sould be throwed", isThrows);
    }

    public void testResourceDelete() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        boolean isThrows = false;
        ResourceDao resourceDao = wrapper.generateResourceDao();
        ResourceDao insertionResult = wrapper.create("/", resourceDao);
        try {
            wrapper.remove("/" + insertionResult.getName());
        } catch (ResourceNotFoundException e) {
            isThrows = true;
        }
        assertTrue("Unsuccessfull deletion", !isThrows);
        try {
            wrapper.remove("/" + insertionResult.getName());
        } catch (ResourceNotFoundException e) {
            isThrows = true;
        }
        assertTrue("Removing of unexisting resuorce should be throwed", isThrows);
    }

    public void testResourceMakeDir() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        ResourceDao dir = wrapper.generateResourceDir();
        ResourceDao insertionResult = wrapper.create("/", dir);
        assertEquals("Result dir should be dir", insertionResult.getType(), ResourceDao.DirType);
    }

    public void testResourceStoringInVariouseDirs() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        ResourceDao dir = wrapper.generateResourceDir();
        ResourceDao insertionResult = wrapper.create("/", dir);
        assertEquals("Result dir should be dir", insertionResult.getType(), ResourceDao.DirType);
        ResourceDao file = wrapper.generateResourceDao();
        insertionResult = wrapper.create("/" + dir.getName(), file);
        assertEquals("Result resource should be file", insertionResult.getType(), ResourceDao.FileType);
        assertEquals("Result resource should be equals with original", insertionResult.getName(), file.getName());
        file = wrapper.generateResourceDao();
        insertionResult = wrapper.create("/" + dir.getName(), file);
        assertEquals("Result dir should be file", insertionResult.getType(), ResourceDao.FileType);
        assertEquals("Result resource should be equals with original", insertionResult.getName(), file.getName());
        ResourceDao dir2 = wrapper.generateResourceDir();
        insertionResult = wrapper.create("/" + dir.getName(), dir2);
        assertEquals("Result dir should be dir", insertionResult.getType(), ResourceDao.DirType);
        file = wrapper.generateResourceDao();
        insertionResult = wrapper.create("/" + dir.getName() + "/" + dir2.getName(), file);
        assertEquals("Result dir should be file", insertionResult.getType(), ResourceDao.FileType);
        assertEquals("Result resource should be equals with original", insertionResult.getName(), file.getName());
        file = wrapper.generateResourceDao();
        insertionResult = wrapper.create("/" + dir.getName() + "/" + dir2.getName(), file);
        assertEquals("Result dir should be file", insertionResult.getType(), ResourceDao.FileType);
        assertEquals("Result resource should be equals with original", insertionResult.getName(), file.getName());
    }

    public void testResourceListDir() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        Collection storedIds = new Vector();
        ResourceDao dir = wrapper.generateResourceDir();
        ResourceDao insertionResult = wrapper.create("/", dir);
        assertEquals("Result dir should be dir", insertionResult.getType(), ResourceDao.DirType);
        ResourceDao file = wrapper.generateResourceDao();
        insertionResult = wrapper.create("/" + dir.getName(), file);
        assertEquals("Result resource should be file", insertionResult.getType(), ResourceDao.FileType);
        assertEquals("Result resource should be equals with original", insertionResult.getName(), file.getName());
        storedIds.add("/" + dir.getName() + "/" + insertionResult.getName());
        file = wrapper.generateResourceDao();
        insertionResult = wrapper.create("/" + dir.getName(), file);
        assertEquals("Result dir should be file", insertionResult.getType(), ResourceDao.FileType);
        assertEquals("Result resource should be equals with original", insertionResult.getName(), file.getName());
        storedIds.add("/" + dir.getName() + "/" + insertionResult.getName());
        file = wrapper.generateResourceDao();
        insertionResult = wrapper.create("/" + dir.getName(), file);
        assertEquals("Result dir should be file", insertionResult.getType(), ResourceDao.FileType);
        assertEquals("Result resource should be equals with original", insertionResult.getName(), file.getName());
        storedIds.add("/" + dir.getName() + "/" + insertionResult.getName());
        Collection resourceList = wrapper.manager.listResources("/" + dir.getName());
        for (Iterator iterator = resourceList.iterator(); iterator.hasNext(); ) {
            Object obj = iterator.next();
            assertTrue("Resulting collection should contains integer ids", obj instanceof String);
        }
        for (Iterator iterator = storedIds.iterator(); iterator.hasNext(); ) {
            String path = (String) iterator.next();
            assertTrue("Stored list shold contains path", resourceList.contains(path));
        }
    }

    public void testResourceMakeDirDuplicate() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        ResourceDao dir = wrapper.generateResourceDir();
        ResourceDao insertionResult = wrapper.create("/", dir);
        assertEquals("Result dir should be dir", insertionResult.getType(), ResourceDao.DirType);
        boolean isThrowed = false;
        try {
            insertionResult = wrapper.create("/", dir);
            assertEquals("Result dir should be dir", insertionResult.getType(), ResourceDao.DirType);
        } catch (ResourceAlreadyExistException e) {
            isThrowed = true;
        }
        assertTrue("ResourceAlreadyExist Exception sould be throwed", isThrowed);
    }

    public void testResourceDeleteNotEmptyDir() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        ResourceDao dir = wrapper.generateResourceDir();
        ResourceDao insertionResult = wrapper.create("/", dir);
        assertEquals("Result dir should be dir", insertionResult.getType(), ResourceDao.DirType);
        ResourceDao file = wrapper.generateResourceDao();
        insertionResult = wrapper.create("/" + dir.getName(), file);
        assertEquals("Result resource should be file", insertionResult.getType(), ResourceDao.FileType);
        assertEquals("Result resource should be equals with original", insertionResult.getName(), file.getName());
        boolean isThrowed = false;
        try {
            wrapper.remove("/" + dir.getName());
        } catch (DirectoryIsNotEmpltyException e) {
            isThrowed = true;
        }
        assertTrue("DirectoryIsNotEmplty Exception sould be throwed", isThrowed);
    }

    public void testResourceDeleteEmptyDir() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        ResourceDao dir = wrapper.generateResourceDir();
        ResourceDao insertionResult = wrapper.create("/", dir);
        assertEquals("Result dir should be dir", insertionResult.getType(), ResourceDao.DirType);
        boolean isThrowed = false;
        try {
            wrapper.remove("/" + dir.getName());
        } catch (DirectoryIsNotEmpltyException e) {
            isThrowed = true;
        }
        assertTrue("DirectoryIsNotEmplty Exception sould not be throwed", !isThrowed);
    }

    public void testWebResourceAvailable() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        boolean throwed = false;
        try {
            URL url = new URL(wrapper.baseServletUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            assertEquals(connection.getResponseCode(), 200);
        } catch (Exception e) {
            throwed = true;
        }
        assertTrue("Servlet is not available", !throwed);
    }

    public void testWebResourceGetResource() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        boolean throwed = false;
        ResourceDao dao = wrapper.generateResourceDao();
        wrapper.create("/", dao);
        String path = (String) wrapper.pathes.lastElement();
        try {
            URL url = new URL(wrapper.baseServletUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            assertEquals(200, connection.getResponseCode());
            String sUrl = wrapper.baseServletUrl + path;
            sUrl = sUrl.replaceAll("/" + path, path);
            url = new URL(sUrl);
            connection = (HttpURLConnection) url.openConnection();
            assertEquals(200, connection.getResponseCode());
            String contentType = connection.getContentType();
            int contentLength = connection.getContentLength();
            assertEquals("Content length should be equals", (dao.getContentLength() != null) ? dao.getContentLength().intValue() : 0, contentLength);
            assertEquals("Content type should be equals", dao.getContentType(), contentType);
        } catch (Exception e) {
            throwed = true;
        }
        assertTrue("Servlet is not available", !throwed);
    }

    public void testWebResourceNotFind() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        boolean throwed = false;
        ResourceDao dao = wrapper.generateResourceDao();
        wrapper.create("/", dao);
        String path = (String) wrapper.pathes.lastElement();
        try {
            URL url = new URL(wrapper.baseServletUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            assertEquals(200, connection.getResponseCode());
            String sUrl = wrapper.baseServletUrl + path;
            sUrl = sUrl.replaceAll("/" + path, path);
            url = new URL(sUrl + "asasas");
            connection = (HttpURLConnection) url.openConnection();
            assertEquals(404, connection.getResponseCode());
        } catch (Exception e) {
            throwed = true;
        }
        assertTrue("Servlet is not available", !throwed);
    }

    public void testWebResourceDirFind() throws Exception {
        assertTrue("JNDI is required", wrapper.isJNDIAvailable);
        boolean throwed = false;
        ResourceDao dir = wrapper.generateResourceDir();
        wrapper.create("/", dir);
        ResourceDao file = wrapper.generateResourceDao();
        wrapper.create("/" + dir.getName(), file);
        String path = (String) wrapper.pathes.lastElement();
        try {
            URL url = new URL(wrapper.baseServletUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            assertEquals(200, connection.getResponseCode());
            String sUrl = wrapper.baseServletUrl + path;
            sUrl = sUrl.replaceAll("/" + path, path);
            url = new URL(sUrl);
            connection = (HttpURLConnection) url.openConnection();
            assertEquals(200, connection.getResponseCode());
        } catch (Exception e) {
            throwed = true;
        }
        assertTrue("Servlet is not available", !throwed);
    }
}
