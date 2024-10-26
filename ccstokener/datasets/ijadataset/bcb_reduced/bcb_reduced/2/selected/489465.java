package net.sf.dozer.util.mapping.util;

import java.net.URL;
import net.sf.dozer.util.mapping.AbstractDozerTest;
import net.sf.dozer.util.mapping.classmap.Mappings;

/**
 * @author garsombke.franz
 */
public class XMLParserTest extends AbstractDozerTest {

    public void testParse() throws Exception {
        XMLParser parser = new XMLParser();
        ResourceLoader loader = new ResourceLoader();
        URL url = loader.getResource("dozerBeanMapping.xml");
        Mappings mappings = parser.parse(url.openStream());
        assertNotNull(mappings);
    }
}
