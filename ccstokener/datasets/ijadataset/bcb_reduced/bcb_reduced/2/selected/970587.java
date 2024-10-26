package org.jcrom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.File;
import java.net.URL;
import java.util.logging.LogManager;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.core.TransientRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Thanks to Leander for identifying this problem, and submitting the
 * unit test.
 */
public class TestReferenceHistory {

    private Repository repo;

    private Session session;

    @Before
    public void setUpRepository() throws Exception {
        repo = (Repository) new TransientRepository();
        session = repo.login(new SimpleCredentials("a", "b".toCharArray()));
        ClassLoader loader = TestReferenceHistory.class.getClassLoader();
        URL url = loader.getResource("logger.properties");
        if (url == null) {
            url = loader.getResource("/logger.properties");
        }
        LogManager.getLogManager().readConfiguration(url.openStream());
    }

    @After
    public void tearDownRepository() throws Exception {
        session.logout();
        deleteDir(new File("repository"));
        new File("repository.xml").delete();
        new File("derby.log").delete();
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    /**
     * .
     * 
     * @throws Exception
     */
    @Test
    public final void testReferenceHistoryMaxDepth() throws Exception {
        Jcrom jcrom = new Jcrom(false, true);
        jcrom.map(HierarchyNode.class);
        jcrom.map(Folder.class);
        jcrom.map(FolderReference.class);
        jcrom.map(Document.class);
        final Node junitNode = this.session.getRootNode().addNode("junit");
        Folder directoryA = new Folder();
        directoryA.setName("Directory_A");
        Folder directoryA1 = new Folder();
        directoryA1.setName("Directory_A_1");
        directoryA.getChildren().add(directoryA1);
        Folder directoryA2 = new Folder();
        directoryA2.setName("Directory_A_2");
        directoryA.getChildren().add(directoryA2);
        Document document1 = new Document();
        document1.setName("Document_1");
        directoryA2.getChildren().add(document1);
        Document document2 = new Document();
        document2.setName("Document_2");
        directoryA2.getChildren().add(document2);
        jcrom.addNode(junitNode, directoryA);
        session.save();
        FolderReference folderReference = new FolderReference();
        folderReference.setName(directoryA2.getName());
        folderReference.setReference(directoryA2);
        directoryA1.getChildren().add(folderReference);
        Node directoryA1Node = session.getNodeByUUID(directoryA1.getUuid());
        jcrom.updateNode(directoryA1Node, directoryA1);
        session.save();
        session.logout();
        jcrom = new Jcrom(false, true);
        jcrom.map(HierarchyNode.class);
        jcrom.map(Folder.class);
        jcrom.map(FolderReference.class);
        jcrom.map(Document.class);
        session = repo.login(new SimpleCredentials("a", "b".toCharArray()));
        Node directoryANode = session.getNodeByUUID(directoryA.getUuid());
        Folder folder = jcrom.fromNode(Folder.class, directoryANode, "*", 3);
        assertEquals("Wrong child count of Directory_A.", 2, folder.getChildren().size());
        for (HierarchyNode h : folder.getChildren()) {
            if (h.getName().equals("Directory_A_1")) {
                Folder f = (Folder) h;
                assertEquals("Wrong child count of Directory_A_1.", 1, f.getChildren().size());
                FolderReference fr = (FolderReference) f.getChildren().get(0);
                assertNotNull("Reference is NULL.", fr.getReference());
                assertEquals("Wrong child count for folder reference.", 0, fr.getChildren().size());
            } else if (h.getName().equals("Directory_A_2")) {
                Folder f = (Folder) h;
                assertEquals("Wrong child count of Directory_A_2.", 2, f.getChildren().size());
            }
        }
    }
}
