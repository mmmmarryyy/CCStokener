package gov.usda.gdpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 *
 * @author  terryc
 */
public class Import {

    private static final Logger myLogger = Logger.getLogger(Import.class);

    /** Creates a new instance of Import */
    private Import() {
    }

    /**
     */
    public static GenotypeTable readFasta(String filename) throws FileNotFoundException, IOException {
        if (filename.startsWith("http")) {
            URL url = new URL(filename);
            return readFasta(new BufferedReader(new InputStreamReader(url.openStream())), filename);
        } else {
            return readFasta(new BufferedReader(new FileReader(filename)), filename);
        }
    }

    /**
     */
    public static GenotypeTable readFasta(File file) throws FileNotFoundException, IOException {
        return readFasta(new BufferedReader(new FileReader(file)), file.getName());
    }

    public static GenotypeTable readFasta(Reader input, String name) throws FileNotFoundException, IOException {
        BufferedReader reader = null;
        if (input instanceof BufferedReader) {
            reader = (BufferedReader) input;
        } else {
            reader = new BufferedReader(input);
        }
        List result = new ArrayList();
        String line = null;
        Taxon taxon = null;
        GenotypeExperiment exp = null;
        line = reader.readLine();
        boolean sequence = false;
        while (line != null) {
            line = line.trim();
            if (line.startsWith(";")) {
                line = reader.readLine();
            } else if (line.startsWith(">")) {
                StringTokenizer tokens = new StringTokenizer(line);
                String taxaName = tokens.nextToken();
                if (taxaName.length() == 1) {
                    taxaName = tokens.nextToken();
                } else {
                    taxaName = taxaName.substring(1).trim();
                }
                Map taxaProps = new HashMap();
                taxaProps.put(TaxonProperty.DATA_SOURCE, GDPCConstants.NO_DATA_SOURCE);
                taxaProps.put(TaxonProperty.ID, GDPCConstants.UNKNOWN_IDENTIFIER);
                taxaProps.put(TaxonProperty.ACCESSION, taxaName);
                taxon = Taxon.getInstance(taxaProps);
                Map expProps = new HashMap();
                expProps.put(GenotypeExperimentProperty.DATA_SOURCE, GDPCConstants.NO_DATA_SOURCE);
                expProps.put(GenotypeExperimentProperty.ID, GDPCConstants.UNKNOWN_IDENTIFIER);
                expProps.put(GenotypeExperimentProperty.POLY_TYPE, GenotypeExperimentProperty.POLY_TYPE_SEQUENCE);
                expProps.put(GenotypeExperimentProperty.NAME, name);
                exp = GenotypeExperiment.getInstance(expProps);
                sequence = true;
                line = reader.readLine();
            } else if (sequence) {
                StringBuilder builder = new StringBuilder();
                while ((line != null) && (!line.startsWith(">")) && (!line.startsWith(";"))) {
                    line = line.trim().toUpperCase();
                    builder.append(line);
                    line = reader.readLine();
                }
                AlleleList alleles = AlleleList.getInstance(new String[] { builder.toString() });
                Map props = new HashMap();
                props.put(GenotypeProperty.TAXON, taxon);
                props.put(GenotypeProperty.GENOTYPE_EXPERIMENT, exp);
                props.put(GenotypeProperty.ALLELE_LIST, alleles);
                Genotype genotype = Genotype.getInstance(props);
                result.add(genotype);
                sequence = false;
            } else {
                myLogger.error("readFasta: file: " + name + " invalid format.");
                throw new IllegalArgumentException("Import: readFasta: invalid format.");
            }
        }
        return DefaultGenotypeTable.getInstance(DefaultGenotypeGroup.getInstance(result));
    }

    public static Object readJavaSerialized(File file) throws FileNotFoundException, IOException, ClassNotFoundException {
        myLogger.info("openFile: file: " + file.toString());
        FileInputStream in = new FileInputStream(file);
        ObjectInputStream s = new ObjectInputStream(in);
        return s.readObject();
    }

    /**
     * Creates a Taxon with the given name, and ID and DATA_SOURCE set to
     * unknown
     */
    public static Taxon taxonCreator(String fileName, Identifier id, String name) {
        Taxon taxa = null;
        Map taxaProps = new HashMap();
        taxaProps.put(TaxonProperty.ID, id);
        taxaProps.put(TaxonProperty.DATA_SOURCE, fileName);
        taxaProps.put(TaxonProperty.ACCESSION, name);
        taxa = Taxon.getInstance(taxaProps);
        return taxa;
    }

    public static TaxonParent femaleParentCreator(String fileName, String[][] idTable, String femaleParentName, String childName) {
        TaxonParent femaleParent = null;
        Map taxaFemaleParentProps = new HashMap();
        taxaFemaleParentProps.put(TaxonParentProperty.CHILD, taxonCreator(fileName, idGrabber(childName, idTable), childName));
        taxaFemaleParentProps.put(TaxonParentProperty.PARENT, taxonCreator(fileName, idGrabber(femaleParentName, idTable), femaleParentName));
        taxaFemaleParentProps.put(TaxonParentProperty.ROLE, TaxonParentProperty.ROLE_FEMALE);
        taxaFemaleParentProps.put(TaxonParentProperty.RECURRENT, 0);
        femaleParent = TaxonParent.getInstance(taxaFemaleParentProps);
        return femaleParent;
    }

    public static TaxonParent maleParentCreator(String fileName, String[][] idTable, String maleParentName, String childName) {
        TaxonParent maleParent = null;
        Map taxaMaleParentProps = new HashMap();
        taxaMaleParentProps.put(TaxonParentProperty.CHILD, taxonCreator(fileName, idGrabber(childName, idTable), childName));
        taxaMaleParentProps.put(TaxonParentProperty.PARENT, taxonCreator(fileName, idGrabber(maleParentName, idTable), maleParentName));
        taxaMaleParentProps.put(TaxonParentProperty.ROLE, TaxonParentProperty.ROLE_MALE);
        taxaMaleParentProps.put(TaxonParentProperty.RECURRENT, 0);
        maleParent = TaxonParent.getInstance(taxaMaleParentProps);
        return maleParent;
    }

    public static TaxonParentGroup merge(List list1, TaxonParent[] list2) {
        int length1 = list1.size();
        int length2 = list2.length;
        List list3 = new ArrayList(length1 + length2);
        for (int i = 0; i < length1; i++) {
            list3.add(i, list1.get(i));
        }
        for (int j = length1; j < length1 + length2; j++) {
            list3.add(j, list2[j - length1]);
        }
        DefaultTaxonParentGroup group = new DefaultTaxonParentGroup(list3);
        return group;
    }

    public static TaxonParent[] FullGroup(String[] taxaNames, int numRows, String fileName, String[][] idTable) {
        boolean isIndividual = false;
        int numParentsAdded = 0;
        int counter = 0;
        String parent = null;
        String[] parentsToBeAdded = new String[taxaNames.length];
        for (int position = 0; position < taxaNames.length; position++) {
            isIndividual = false;
            if ((position % 3) != 0) {
                parent = taxaNames[position];
                for (int position1 = 0; position1 < taxaNames.length; position1++) {
                    if (((position1 % 3 == 0) && (taxaNames[position1].compareTo(parent) == 0)) || parent.compareTo("-999") == 0) {
                        isIndividual = true;
                    }
                }
                if ((isIndividual == false) && (position % 3 != 0)) {
                    boolean isAlreadyAdded = false;
                    for (int i = 0; i < position; i++) {
                        if (parentsToBeAdded[i].compareTo(taxaNames[position]) == 0) {
                            isAlreadyAdded = true;
                        }
                    }
                    if (isAlreadyAdded == false) {
                        parentsToBeAdded[position] = taxaNames[position];
                        numParentsAdded++;
                    }
                } else {
                    parentsToBeAdded[position] = "-999";
                }
            } else {
                parentsToBeAdded[position] = "-999";
            }
        }
        TaxonParent[] arrAddedParents = new TaxonParent[2 * numParentsAdded];
        for (int i = 0; (i < arrAddedParents.length) && (counter < arrAddedParents.length); i++) {
            if (parentsToBeAdded[i].compareTo("-999") != 0) {
                arrAddedParents[counter] = femaleParentCreator(fileName, idTable, "-999", parentsToBeAdded[i]);
                arrAddedParents[counter + 1] = maleParentCreator(fileName, idTable, "-999", parentsToBeAdded[i]);
                counter = counter + 2;
            }
        }
        return arrAddedParents;
    }

    public static TaxonParentGroup readThreeColumnPedigree(String filename) throws FileNotFoundException, IOException {
        return readThreeColumnPedigree(new File(filename));
    }

    public static Identifier idGrabber(String id, String[][] idTable) {
        int length = idTable.length;
        boolean inTable = false;
        int row = 0;
        String current = null;
        for (int i = 0; i < length; i++) {
            current = idTable[i][0];
            if ((id.compareTo(current) == 0) && (id.compareTo("-999") != 0)) {
                inTable = true;
                row = i;
            }
        }
        if (inTable) {
            return new Identifier(Integer.toString(row + 2));
        } else {
            return new Identifier(Integer.toString(0));
        }
    }

    public static TaxonParentGroup readThreeColumnPedigree(File file) throws FileNotFoundException, IOException {
        Identifier identifier = null;
        String fileName = file.getName();
        int id = 1;
        int counter = 0;
        int counter2 = 0;
        int counter3 = 0;
        String line = null;
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line1 = reader.readLine();
        line1 = line1.trim();
        StringTokenizer line1Tokens = new StringTokenizer(line1);
        int numRows = Integer.parseInt(line1Tokens.nextToken());
        int numColumns = Integer.parseInt(line1Tokens.nextToken());
        int numHeadings = Integer.parseInt(line1Tokens.nextToken());
        BufferedReader reader2 = new BufferedReader(new FileReader(file));
        reader2.readLine();
        for (int i = 0; i < numHeadings; i++) {
            String tmp = reader.readLine();
            String tmp2 = reader2.readLine();
        }
        String[][] idTable = new String[numRows - 1][2];
        while ((line = reader2.readLine()) != null) {
            StringTokenizer tokens = new StringTokenizer(line);
            String name = tokens.nextToken();
            idTable[counter3][0] = name;
            String tmpInt = Integer.toString(counter3 + 1);
            idTable[counter3][1] = tmpInt;
            counter3++;
        }
        String[] strArray = new String[3 * (numRows - 1)];
        List taxa = new ArrayList(2 * (numRows - numHeadings));
        while ((line = reader.readLine()) != null) {
            id++;
            line = line.trim();
            StringTokenizer tokens = new StringTokenizer(line);
            while (tokens.hasMoreTokens()) {
                String tmpIdentifier = Integer.toString(id);
                identifier = new Identifier(tmpIdentifier);
                String taxaName = tokens.nextToken();
                strArray[counter] = taxaName;
                counter++;
                String taxaFemaleParentName = tokens.nextToken();
                strArray[counter] = taxaFemaleParentName;
                femaleParentCreator(fileName, idTable, taxaFemaleParentName, taxaName);
                taxa.add(femaleParentCreator(fileName, idTable, taxaFemaleParentName, taxaName));
                counter++;
                counter2++;
                String taxaMaleParentName = tokens.nextToken();
                maleParentCreator(fileName, idTable, taxaMaleParentName, taxaName);
                strArray[counter] = taxaMaleParentName;
                taxa.add(maleParentCreator(fileName, idTable, taxaMaleParentName, taxaName));
                counter++;
                counter2++;
            }
        }
        TaxonParent[] missingParentList = FullGroup(strArray, numRows, fileName, idTable);
        return merge(taxa, missingParentList);
    }

    /**
     * @param listOfTaxa list of all table information in Table Order
     * Requires: taxon is the information of a COMPLETE table.
     * @return
     */
    public static int[][] threeColumnToNumerical(String[] listOfTaxa) {
        String[] uniqueTaxaNull = new String[listOfTaxa.length / 3];
        int index1 = 0;
        boolean occurs;
        uniqueTaxaNull[0] = listOfTaxa[0];
        for (int i = 0; i < listOfTaxa.length; i++) {
            occurs = false;
            if ((i % 3) == 0) {
                for (int j = 0; j < index1; j++) {
                    if ((uniqueTaxaNull[j].compareTo(listOfTaxa[i]) == 0) && (listOfTaxa[i].compareTo("-999") != 0)) {
                        occurs = true;
                    }
                }
                if (occurs == false) {
                    uniqueTaxaNull[index1] = listOfTaxa[i];
                    index1++;
                }
            }
        }
        String[] uniqueTaxa = new String[index1];
        for (int i = 0; i < index1; i++) {
            uniqueTaxa[i] = uniqueTaxaNull[i];
        }
        String[][] nidMatrix = new String[uniqueTaxa.length][2];
        for (int i = 0; i < uniqueTaxa.length; i++) {
            int tmp = i + 1;
            String val = String.valueOf(tmp);
            nidMatrix[i][0] = uniqueTaxa[i];
            nidMatrix[i][1] = val;
        }
        int[][] nidTable = new int[nidMatrix.length][3];
        for (int i = 0; i < listOfTaxa.length; i++) {
            String tmp = listOfTaxa[i];
            int column = 0;
            if (i % 3 == 0) {
                column = 0;
            } else if ((i % 3) == 1) {
                column = 1;
            } else {
                column = 2;
            }
            double itemp = i;
            Double rowTemp = Math.floor(itemp / 3.0);
            int row = rowTemp.intValue();
            if (tmp.compareTo("-999") == 0) {
                nidTable[row][column] = 0;
            } else {
                for (int j = 0; j < uniqueTaxa.length; j++) {
                    int compare = tmp.compareTo(uniqueTaxa[j]);
                    if (compare == 0) {
                        int myInt = 0;
                        try {
                            myInt = Integer.parseInt(nidMatrix[j][1]);
                        } catch (NumberFormatException e) {
                        }
                        nidTable[row][column] = myInt;
                    }
                }
            }
        }
        return nidTable;
    }

    public static int[][] threeColumnToNumerical(File file) throws FileNotFoundException, IOException {
        TaxonParentGroup list = readThreeColumnPedigree(file);
        String[] listOfTaxa = Convert.convertTaxonParentsToTableOrder(list);
        return threeColumnToNumerical(listOfTaxa);
    }

    /**
     * nidTable is K x 3 matrix in which first column is sorted in increasing
     * order, beginning
     * at 1
     *
     * @return "reverse" distance
     */
    public static int getHeightRecursive(int[][] nidTable, int individual, int height) {
        if (individual == 0) {
            return height;
        } else {
            int female = nidTable[individual - 1][1];
            int male = nidTable[individual - 1][2];
            if ((female == 0) && (male == 0)) {
                return height;
            } else {
                return Math.max(getHeightRecursive(nidTable, male, height + 1), getHeightRecursive(nidTable, female, height + 1));
            }
        }
    }

    public static int[][] sortID(int[][] nidTable) {
        int length = nidTable.length;
        int[] heightArray = new int[length];
        List seenParents = new ArrayList();
        for (int i = 0; i < length; i++) {
            heightArray[i] = getHeightRecursive(nidTable, i + 1, 0);
        }
        int maxHeight = 0;
        for (int i = 0; i < length; i++) {
            if (maxHeight < heightArray[i]) {
                maxHeight = heightArray[i];
            }
        }
        int[] tmp = new int[heightArray.length];
        int counter = 1;
        for (int i = 0; i <= maxHeight; i++) {
            for (int j = 0; j < length; j++) {
                if (heightArray[j] == i) {
                    tmp[j] = counter;
                    counter++;
                }
            }
        }
        int[][] idTable = new int[heightArray.length][2];
        for (int i = 0; i < heightArray.length; i++) {
            idTable[i][0] = i + 1;
            idTable[i][1] = tmp[i];
        }
        return idTable;
    }

    public static int[][] tableFiller(int[][] sortedID, int[][] nidTable) {
        int length = nidTable.length;
        int[][] completedTable = new int[length][3];
        int previous = 0;
        for (int i = 0; i < completedTable.length; i++) {
            int tmp = i + 1;
            for (int j = 0; j < completedTable.length; j++) {
                if (sortedID[j][1] == tmp) {
                    previous = sortedID[j][0];
                    for (int k = 0; k < completedTable.length; k++) {
                        for (int l = 0; l < 3; l++) {
                            if (nidTable[k][l] == previous) {
                                completedTable[k][l] = tmp;
                            } else if (nidTable[k][l] == 0) {
                                completedTable[k][l] = 0;
                            }
                        }
                    }
                }
            }
        }
        int[][] tmp = new int[completedTable.length][3];
        for (int i = 0; i < tmp.length; i++) {
            for (int j = 0; j < tmp.length; j++) {
                if (completedTable[j][0] == i + 1) {
                    for (int k = 0; k < 3; k++) {
                        tmp[i][k] = completedTable[j][k];
                    }
                }
            }
        }
        return tmp;
    }

    public static double[][] kinshipRelation(int[][] completedTable) {
        int length = completedTable.length;
        double[][] relationMatrix = new double[length + 1][length + 1];
        for (int i = 0; i < relationMatrix.length; i++) {
            for (int j = 0; j < relationMatrix.length; j++) {
                if (i == 0) {
                    relationMatrix[i][j] = j;
                } else if (j == 0) {
                    relationMatrix[i][j] = i;
                } else if (i == j) {
                    relationMatrix[i][j] = 1;
                } else {
                    relationMatrix[i][j] = 0;
                }
            }
        }
        int maleParent = 0;
        int femaleParent = 0;
        for (int i = 1; i < relationMatrix.length; i++) {
            for (int j = 1; j < relationMatrix.length; j++) {
                if (j > i) {
                    femaleParent = completedTable[j - 1][1];
                    maleParent = completedTable[j - 1][2];
                    if ((femaleParent == 0) && (maleParent == 0)) {
                        relationMatrix[i][j] = 0;
                    } else if (femaleParent == 0) {
                        relationMatrix[i][j] = .5 * relationMatrix[i][maleParent];
                    } else if (maleParent == 0) {
                        relationMatrix[i][j] = .5 * relationMatrix[i][femaleParent];
                    } else {
                        relationMatrix[i][j] = .5 * (relationMatrix[i][femaleParent] + relationMatrix[i][maleParent]);
                    }
                }
            }
        }
        for (int i = 1; i < relationMatrix.length; i++) {
            for (int j = 1; j < relationMatrix.length; j++) {
                if (i > j) {
                    relationMatrix[i][j] = relationMatrix[j][i];
                }
            }
        }
        double tmp[][] = new double[relationMatrix.length - 1][relationMatrix.length - 1];
        for (int i = 0; i < tmp.length; i++) {
            for (int j = 0; j < tmp.length; j++) {
                tmp[i][j] = relationMatrix[i + 1][j + 1];
            }
        }
        return relationMatrix;
    }
}
