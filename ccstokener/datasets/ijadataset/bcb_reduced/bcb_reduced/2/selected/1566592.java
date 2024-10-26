package org.xmlcml.cml.converters.compchem.gaussian;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import nu.xom.Element;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.base.CMLElement;
import org.xmlcml.cml.converters.ConverterCommand;
import org.xmlcml.cml.converters.Type;
import org.xmlcml.cml.converters.compchem.AbstractCompchem2CMLConverter;
import org.xmlcml.cml.element.CMLCml;
import org.xmlcml.cml.element.CMLDictionary;

public class GaussianArchive2CMLConverter extends AbstractCompchem2CMLConverter {

    private static final Logger LOG = Logger.getLogger(GaussianArchive2CMLConverter.class);

    public static final String GAUSS_PREFIX = "gauss";

    public static final String GAUSS_URI = "http://wwmm.ch.cam.ac.uk/dict/gauss";

    public static final String[] typicalArgsForConverterCommand = { "-sd", "D:/projects/cost/gaussian/files", "-odir", "../gaussian.cml", "-is", "log", "-os", "gau.cml", "-aux", "D:/workspace/jumbo-converters/src/main/resources/org/xmlcml/cml/converters/compchem/gaussian/gaussianArchiveDict.xml", "-converter", "org.xmlcml.cml.converters.compchem.gaussian.GaussianArchive2CMLConverter" };

    public static final String[] testArgs = { "-quiet", "-sd", "src/test/resources/gaussian", "-odir", "../gaussian.cml", "-is", "gau", "-os", "gau.cml", "-aux", "src/main/resources/org/xmlcml/cml/converters/compchem/gaussian/gaussianArchiveDict.xml", "-converter", "org.xmlcml.cml.converters.compchem.gaussian.GaussianArchive2CMLConverter" };

    public Type getInputType() {
        return Type.GAU_ARC;
    }

    public Type getOutputType() {
        return Type.CML;
    }

    /**
	 * converts an MDL object to CML. returns cml:cml/cml:molecule
	 * 
	 * @param in input stream
	 */
    public Element convertToXML(List<String> lines) {
        CMLCml topCml = new CMLCml();
        CMLDictionary dictionary = findDictionary();
        GaussianArchiveProcessor converter = new GaussianArchiveProcessor(dictionary, this.getCommand());
        converter.setConverterLog(converterLog);
        List<CMLElement> cmlElementList = converter.readArchives(lines);
        for (CMLElement cmlElement : cmlElementList) {
            topCml.appendChild(cmlElement);
        }
        try {
            ensureId(topCml);
        } catch (RuntimeException e) {
        }
        topCml = processParamsTopMetadataNamespaces(topCml);
        return topCml;
    }

    public void addNamespaces(CMLCml topCml) {
        addCommonNamespaces(topCml);
        topCml.addNamespaceDeclaration(GAUSS_PREFIX, GAUSS_URI);
    }

    private CMLDictionary findDictionary() {
        CMLDictionary dictionary = null;
        String resourceS = "org/xmlcml/cml/converters/compchem/gaussian/gaussianArchiveDict.xml";
        URL url = getClass().getClassLoader().getResource(resourceS);
        if (url == null) {
            throw new RuntimeException("BUG: can't load gaussian archive dictionary (check resource directories are on the classpath: " + resourceS);
        }
        if (command != null && !command.isQuiet()) {
            LOG.info("URL " + url);
        }
        InputStream inputStream = null;
        try {
            inputStream = url.openStream();
            CMLCml cml = (CMLCml) new CMLBuilder().build(inputStream).getRootElement();
            dictionary = (CMLDictionary) cml.getFirstCMLChild(CMLDictionary.TAG);
            if (dictionary == null) {
                throw new RuntimeException("Failed to find dictionary element in " + resourceS);
            }
        } catch (Exception e) {
            throw new RuntimeException("BUG: could not read/parse dictionary: " + resourceS, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return dictionary;
    }

    @Override
    public int getConverterVersion() {
        return 0;
    }

    public static void main(String[] args) {
        File in = new File("c:/Users/ned24/workspace/lensfield/data/gau-files/C2H4.g03");
        File out = new File("c:/Users/ned24/workspace/lensfield/data/gau-files/C2H4.cml");
        GaussianArchive2CMLConverter cc = new GaussianArchive2CMLConverter();
        cc.setCommand(new ConverterCommand());
        cc.convert(in, out);
    }
}
