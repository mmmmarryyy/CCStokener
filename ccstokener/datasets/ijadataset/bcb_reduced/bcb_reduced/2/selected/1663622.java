package net.sourceforge.fraglets.aotools.codec;

import net.sourceforge.fraglets.aotools.model.BaseNanoCluster;
import net.sourceforge.fraglets.aotools.model.BaseNanoList;
import net.sourceforge.fraglets.aotools.model.Body;
import com.jclark.xml.parse.ApplicationException;
import com.jclark.xml.parse.CharacterDataEvent;
import com.jclark.xml.parse.EndElementEvent;
import com.jclark.xml.parse.EntityManagerImpl;
import com.jclark.xml.parse.OpenEntity;
import com.jclark.xml.parse.NotWellFormedException;
import com.jclark.xml.parse.StartElementEvent;
import com.jclark.xml.parse.base.ApplicationImpl;
import com.jclark.xml.parse.base.Parser;
import com.jclark.xml.parse.base.ParserImpl;
import java.io.IOException;
import java.net.URL;

/** Decoder for XML encoded nano lists.
 *
 * @author kre
 * @version $Revision: 1.3 $
 */
public class NanoListDecoder extends ApplicationImpl {

    /** The list where to add nano clusters. */
    private BaseNanoList list;

    /** Inter-element state. */
    private String nanoName;

    private String nanoSkill;

    private int nanoType;

    private int nanoBodyLocation;

    private String propertyName;

    private StringBuffer buffer;

    private char copyBuffer[];

    /** Creates new NanoListDecoder
     * @param list the list where decoded nanos should be added
     */
    public NanoListDecoder(BaseNanoList list) {
        this.list = list;
        this.buffer = new StringBuffer();
    }

    /** Decode the XML file at <var>url</var>.
     * @param url the URL to parse
     * @throws ApplicationException on parser failures
     * @throws IOException on IO failures
     * @throws NotWellFormedException on XML format errors
     */
    public void decode(URL url) throws ApplicationException, IOException, NotWellFormedException {
        Parser parser = new ParserImpl();
        parser.setApplication(this);
        parser.parseDocument(new OpenEntity(url.openStream(), url.toExternalForm(), url));
    }

    /** Convert a text description of a nano type to numeric
     * representation.
     *
     * @param text description
     * @return the numeric representation
     */
    public static int toNanoType(String text) {
        int scan = BaseNanoCluster.NANO_TYPES.length;
        while (--scan >= 0) {
            if (text.equals(BaseNanoCluster.NANO_TYPES[scan])) {
                return scan;
            }
        }
        throw new IllegalArgumentException("unknown nano type: " + text);
    }

    /** Convert a text description of a nano type to numeric
     * representation.
     *
     * @param text description
     * @return the numeric representation
     */
    public static int toBodyLoc(String text) {
        int scan = Body.SLOT_NAMES.length;
        while (--scan >= 0) {
            if (text.equals(Body.SLOT_NAMES[scan])) {
                return scan;
            }
        }
        throw new IllegalArgumentException("unknown body location: " + text);
    }

    /** Event handler for start element.
     * @param ev the start element event
     */
    public void startElement(StartElementEvent ev) {
        String name = ev.getName();
        if (name.equals(NanoListTags.BASE_NANO_CLUSTER)) {
            clearState();
            nanoName = ev.getAttributeValue(NanoListTags.NAME);
        } else if (name.equals(NanoListTags.PROPERTY)) {
            propertyName = ev.getAttributeValue(NanoListTags.NAME);
            buffer.setLength(0);
        }
    }

    /** Event handler for character data (i.e. property values).
     * @param ev character event
     */
    public void characterData(CharacterDataEvent ev) {
        if (copyBuffer == null || ev.getLengthMax() > copyBuffer.length) {
            copyBuffer = new char[ev.getLengthMax()];
        }
        int n = ev.copyChars(copyBuffer, 0);
        buffer.append(copyBuffer, 0, n);
    }

    /** Event handler for end element.
     * @param ev end element event
     */
    public void endElement(EndElementEvent ev) {
        String name = ev.getName();
        if (name.equals(NanoListTags.BASE_NANO_CLUSTER)) {
            if (nanoName == null || nanoSkill == null || nanoType == -1 || nanoBodyLocation == -1) {
                throw new IllegalArgumentException("missing property");
            } else {
                BaseNanoCluster nano = new BaseNanoCluster(nanoName, nanoSkill, nanoType, nanoBodyLocation);
                list.addNano(nano);
            }
        } else if (name.equals(NanoListTags.PROPERTY)) {
            String value = buffer.toString();
            if (propertyName.equals(NanoListTags.BODY_LOC)) {
                nanoBodyLocation = toBodyLoc(value);
            } else if (propertyName.equals(NanoListTags.SKILL)) {
                nanoSkill = value;
            } else if (propertyName.equals(NanoListTags.TYPE)) {
                nanoType = toNanoType(value);
            } else {
                System.err.println("unrecognized property: " + propertyName);
            }
            buffer.setLength(0);
        }
    }

    /** Clear parser state between elements.
     */
    protected void clearState() {
        nanoName = null;
        nanoSkill = null;
        nanoType = -1;
        nanoBodyLocation = -1;
        propertyName = null;
    }

    /** Testing method reading one input file and printing the
     * resulting list.
     * @param args priovide exactly one name of an XML input file
     * @throws IOException on IO failures
     * @throws ApplicationException on parser failures
     */
    public static void main(String args[]) throws IOException, ApplicationException {
        Parser parser = new ParserImpl();
        BaseNanoList list = BaseNanoList.getBaseNanoList();
        parser.setApplication(new NanoListDecoder(list));
        try {
            parser.parseDocument(EntityManagerImpl.openFile(args[0]));
        } catch (NotWellFormedException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        list.print();
    }
}
