package uk.ac.ebi.pridemod.pridemod.xml;

import org.apache.log4j.Logger;
import uk.ac.ebi.pridemod.pridemod.extractor.PrideModExtractor;
import uk.ac.ebi.pridemod.pridemod.model.PrideMod;
import uk.ac.ebi.pridemod.pridemod.model.PrideModification;
import uk.ac.ebi.pridemod.pridemod.xml.unmarshaller.PrideModUnmarshallerFactory;
import uk.ac.ebi.pridemod.slimmod.model.SlimModCollection;
import uk.ac.ebi.pridemod.slimmod.model.SlimModification;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: yperez
 * Date: 18-Jul-2011
 * Time: 12:13:31
 */
public class PrideModReader {

    private static final Logger logger = Logger.getLogger(PrideModReader.class.getName());

    /**
     * pattern to match the content of a element
     */
    private static final Pattern ELEMENT_CONTENT_PATTERN = Pattern.compile("\\s*\\<[^\\>]+\\>([^\\<]+)\\<\\/[^\\>]+\\>\\s*");

    /**
     * internal unmashaller
     */
    private Unmarshaller unmarshaller = null;

    /**
     * internal xml extractor
     */
    private PrideModExtractor extractor = null;

    private PrideMod prideMod_whole = null;

    public PrideModReader(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("Xml file to be indexed must not be null");
        }
        try {
            this.unmarshaller = PrideModUnmarshallerFactory.getInstance().initializeUnmarshaller();
            prideMod_whole = (PrideMod) unmarshaller.unmarshal(url.openStream());
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Error unmarshalling XML file: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading XML file: " + e.getMessage(), e);
        }
    }

    public SlimModCollection getSlimModCollection() {
        SlimModCollection slimModCollection = new SlimModCollection();
        for (PrideModification prideModification : prideMod_whole.getPrideModifications().getPrideModification()) {
            SlimModification slimModification = new SlimModification(prideModification.getPsiId(), prideModification.getDiffMono().doubleValue(), prideModification.getUnimodMappings().getUnimodMapping().get(0).getId().intValue(), prideModification.getPsiName(), prideModification.getPsiName(), prideModification.getSpecificityList());
            slimModCollection.add(slimModification);
        }
        return slimModCollection;
    }
}
