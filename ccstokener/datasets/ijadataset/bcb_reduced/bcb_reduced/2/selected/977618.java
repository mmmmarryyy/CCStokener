package org.deft.extension.test;

import static org.junit.Assert.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import org.deft.extension.test.testutil.TestUtil;
import org.deft.extension.tools.tokentrimmer.TokenAutoTrimmer;
import org.deft.repository.XfsrFormatManager;
import org.deft.repository.XmlFileSystemRepository;
import org.deft.repository.XmlToFormatContentConverter;
import org.deft.repository.ast.TreeNode;
import org.deft.repository.ast.decoration.DecoratorSelection;
import org.deft.repository.ast.decoration.Format;
import org.deft.repository.query.Query;
import org.deft.repository.util.XmlUtil;
import org.junit.Test;
import org.w3c.dom.Document;

public class TrimTest {

    @Test
    public void testTrim() throws Exception {
        TreeNode ast = TestUtil.readFileInAST("resources/SimpleTestFile.java");
        DecoratorSelection ds = new DecoratorSelection();
        XmlFileSystemRepository rep = new XmlFileSystemRepository();
        XmlToFormatContentConverter converter = new XmlToFormatContentConverter(rep);
        URI url = new File("resources/javaDefaultFormats.xml").toURI();
        InputStream is = url.toURL().openStream();
        converter.convert(is);
        File f = new File("resources/javaDefaultFormats.xml").getAbsoluteFile();
        converter.convert(f);
        String string = new File("resources/query.xml").getAbsolutePath();
        Document qDoc = XmlUtil.loadXmlFromFile(string);
        Query query = new Query(qDoc);
        Format format = XfsrFormatManager.getInstance().getFormats("java", "signature only");
        TokenAutoTrimmer.create("Java", "resources/java.autotrim");
        Document doc = rep.getXmlContentTree(ast, query, format, ds).getOwnerDocument();
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><sourcecode>main(String[])</sourcecode>";
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        XmlUtil.outputXml(doc, bout);
        String actual = bout.toString();
        assertEquals(expected, actual);
    }
}
