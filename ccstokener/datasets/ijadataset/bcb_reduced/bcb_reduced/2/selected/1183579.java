package org.xmlcml.cml.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.base.CMLConstants;
import org.xmlcml.cml.base.CMLElements;
import org.xmlcml.cml.element.CMLCml;
import org.xmlcml.cml.element.CMLMolecule;
import org.xmlcml.cml.element.CMLPeak;
import org.xmlcml.cml.element.CMLPeakList;
import org.xmlcml.cml.element.CMLPeakStructure;
import org.xmlcml.cml.element.CMLSpectrum;
import org.xmlcml.euclid.Util;

public class SpectrumFixture {

    protected String peakStructureFile1 = "peakStructure1" + CMLConstants.XML_SUFF;

    protected String peakStructureFile1NoSchema = "peakStructure1-noSchema" + CMLConstants.XML_SUFF;

    protected String peakStructureFile2 = "peakStructure2" + CMLConstants.XML_SUFF;

    protected String peakStructureFile2Schema = "peakStructure2Schema" + CMLConstants.XML_SUFF;

    protected String testfile = "spectrum";

    protected String testfile1 = "spectrum1.xml";

    protected String testfile2 = "spectrum2.xml";

    protected String testfile3 = "spectrum3.xml";

    protected String testfile4 = "spectrum4.xml";

    protected String testfile5 = "spectrum5.xml";

    protected String testCompoundFile1 = "spectrum_and_structure1.xml";

    private URL makeSpectrumInputStreamContainer(int num) throws IOException {
        return Util.getResource(Fixture.SIMPLE_RESOURCE + CMLConstants.U_S + "spectrum" + num + CMLConstants.XML_SUFF);
    }

    protected CMLSpectrum readSpectrum(int num) throws Exception {
        CMLSpectrum spectrum = null;
        URL spurl = makeSpectrumInputStreamContainer(num);
        InputStream in = spurl.openStream();
        spectrum = (CMLSpectrum) new CMLBuilder().build(in).getRootElement();
        in.close();
        return spectrum;
    }

    /**
	 * gets the spectrum out of the peakStructure.xml test file
	 * 
	 * @return the spectrum
	 */
    public CMLSpectrum getSpectrum() throws Exception {
        CMLSpectrum spectrum = null;
        InputStream in = Util.getInputStreamFromResource(Fixture.SIMPLE_RESOURCE + CMLConstants.U_S + peakStructureFile1);
        CMLCml cml = (CMLCml) new CMLBuilder().build(in).getRootElement();
        spectrum = (CMLSpectrum) cml.getChildCMLElements(CMLSpectrum.TAG).get(0);
        return spectrum;
    }

    /**
	 * gets peaks from peakStructure.xml example file.
	 * 
	 * @return the peaks
	 * @throws Exception
	 */
    CMLElements<CMLPeak> getPeaks() throws Exception {
        CMLSpectrum spectrum = getSpectrum();
        CMLPeakList peakList = spectrum.getPeakListElements().get(0);
        CMLElements<CMLPeak> peaks = peakList.getPeakElements();
        return peaks;
    }

    /**
	 * gets peak structures from peakStructure.xml example files.
	 * 
	 * @param num
	 *            only 1 works. gets peaksStructure[1] on Hb.
	 * @return the peakStructures
	 * @throws Exception
	 */
    public CMLElements<CMLPeakStructure> getPeakStructures(int num) throws Exception {
        CMLPeak peak = getPeaks().get(num);
        return peak.getPeakStructureElements();
    }

    /**
	 * gets the molecule out of the peakStructure.xml test file
	 * 
	 * @return the spectrum
	 */
    public CMLMolecule getMolecule() throws Exception {
        CMLMolecule molecule = null;
        InputStream in = Util.getInputStreamFromResource(Fixture.SIMPLE_RESOURCE + CMLConstants.U_S + peakStructureFile1);
        CMLCml cml = (CMLCml) new CMLBuilder().build(in).getRootElement();
        in.close();
        molecule = (CMLMolecule) cml.getChildCMLElements(CMLMolecule.TAG).get(0);
        return molecule;
    }
}
