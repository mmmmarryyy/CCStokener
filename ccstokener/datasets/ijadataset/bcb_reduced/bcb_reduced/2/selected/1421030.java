package uk.ac.ebi.pridemod.slimmod.tab;

import uk.ac.ebi.pridemod.slimmod.model.SlimModCollection;
import uk.ac.ebi.pridemod.slimmod.model.SlimModification;
import uk.ac.ebi.pridemod.slimmod.model.Specificity;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by IntelliJ IDEA.
 * User: yperez
 * Date: 20/07/11
 * Time: 10:48
 * To change this template use File | Settings | File Templates.
 */
public final class ReadTabSlim {

    private static final String delimiter = "\t";

    private static final String delimiterSpecificity = ",";

    /**
     * Template method that calls {@link #processLine(String)}.
     */
    public static SlimModCollection parseSlimModification(URL url) {
        try {
            Scanner scanner = new Scanner(url.openStream());
            return parseSlimModification(scanner);
        } catch (IOException e) {
            throw new IllegalStateException("Error reading slim file: " + e.getMessage(), e);
        }
    }

    public static SlimModCollection parseSlimModification(String fileName) {
        try {
            Scanner scanner = new Scanner(new FileReader(new File(fileName)));
            return parseSlimModification(scanner);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Error reading slim file: " + e.getMessage(), e);
        }
    }

    public static SlimModCollection parseSlimModification(Scanner scanner) {
        SlimModCollection returnModCollection = new SlimModCollection();
        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.charAt(0) != '#') {
                    SlimModification mod = processLine(line);
                    returnModCollection.add(mod);
                }
            }
        } finally {
            scanner.close();
        }
        return returnModCollection;
    }

    protected static SlimModification processLine(String aLine) {
        String slimdatavalue[] = aLine.split(delimiter);
        SlimModification slimNode = new SlimModification(slimdatavalue[0], Double.parseDouble(slimdatavalue[1]), Integer.parseInt(slimdatavalue[2]), slimdatavalue[3], slimdatavalue[4], parseSpecificityCollection(slimdatavalue[5]));
        return slimNode;
    }

    private static Specificity parseSpecificity(String s) {
        s = s.replaceAll("\\(", " ");
        s = s.replaceAll("\\)", "");
        s = s.trim();
        String valueAA[] = s.split(" ");
        Specificity.AminoAcid aminoacid = Specificity.parseAminoAcid(valueAA[0]);
        Specificity.Position position = Specificity.parsePositon(valueAA[1]);
        return new Specificity(aminoacid, position);
    }

    private static List<Specificity> parseSpecificityCollection(String s) {
        String specificity[] = s.split(delimiterSpecificity);
        List<Specificity> returnSpecificityList = new ArrayList<Specificity>();
        for (int i = 0; i < specificity.length; i++) {
            returnSpecificityList.add(parseSpecificity(specificity[i]));
        }
        return returnSpecificityList;
    }
}
