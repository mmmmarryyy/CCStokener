package org.openscience.jchempaint.inchi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.MoleculeSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IMoleculeSet;

/**
 * Class to read an InChI file which expected to be some text file
 * with InChI=.... lines in there. These lines are fed into the StdInChIParser 
 * 
 * @author markr
 *
 */
public class StdInChIReader {

    /**
     * Read the InChI=.. lines from a give text file containing InChI(s)
     * @param url
     * @return chemModel with molecule set with molecule(s) created using InChI
     * @throws CDKException
     */
    public static IChemModel readInChI(URL url) throws CDKException {
        IChemModel chemModel = new ChemModel();
        try {
            IMoleculeSet moleculeSet = new MoleculeSet();
            chemModel.setMoleculeSet(moleculeSet);
            StdInChIParser parser = new StdInChIParser();
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.toLowerCase().startsWith("inchi=")) {
                    IAtomContainer atc = parser.parseInchi(line);
                    moleculeSet.addAtomContainer(atc);
                }
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new CDKException(e.getMessage());
        }
        return chemModel;
    }
}
