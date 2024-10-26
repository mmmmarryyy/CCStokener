package org.xmlcml.cml.converters.dictionary;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import nu.xom.Document;
import org.apache.commons.io.IOUtils;
import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.element.CMLCml;
import org.xmlcml.cml.element.CMLDictionary;

/**
 *
 * @author Weerapong
 */
public class DictionaryCollection {

    private Map<String, CMLDictionary> dictNSIndex = new HashMap<String, CMLDictionary>();

    public static final String PropertyLocation = "org/xmlcml/cml/converters/compchem/property.xml";

    public static final String CompChemLocation = "org/xmlcml/cml/converters/compchem/compchemDict.xml";

    public static final String UnitsLocation = "org/xmlcml/cml/converters/compchem/unitTypeDict.xml";

    public static final String MoleculeLocation = "org/xmlcml/cml/converters/compchem/molecule.xml";

    public void loadDictionary(String filepath) {
        CMLDictionary dictionary = null;
        URL url = getClass().getClassLoader().getResource(filepath);
        System.out.println(url.getPath());
        if (url == null) {
            throw new RuntimeException("BUG: can't load dictionary (check resource directories are on the classpath: " + filepath);
        }
        InputStream inputStream = null;
        try {
            inputStream = url.openStream();
            Document cmldoc = new CMLBuilder().build(inputStream);
            CMLCml cml = (CMLCml) cmldoc.getRootElement();
            dictionary = (CMLDictionary) cml.getFirstCMLChild(CMLDictionary.TAG);
            if (dictionary == null) {
                throw new RuntimeException("Failed to find dictionary element in " + filepath);
            } else {
                dictNSIndex.put(cml.getNamespaceURI(), dictionary);
            }
        } catch (Exception e) {
            throw new RuntimeException("BUG: could not read/parse dictionary: " + filepath, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public CMLDictionary getDictionary(String namespace) {
        return dictNSIndex.get(namespace);
    }
}
