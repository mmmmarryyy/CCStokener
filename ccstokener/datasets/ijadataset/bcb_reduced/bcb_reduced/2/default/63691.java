import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Scanner;
import java.io.*;
import java.net.*;

public class AttributeSchemaMatcher {

    Database db1;

    Database db2;

    Hashtable<String, Double> types;

    Hashtable<String, String> classes;

    MappingDataStructure mapping1;

    MappingDataStructure mapping2;

    public AttributeSchemaMatcher(Database db1, Database db2) {
        this.db1 = db1;
        this.db2 = db2;
        mapping1 = new MappingDataStructure(db1.getDatabaseName());
        mapping2 = new MappingDataStructure(db2.getDatabaseName());
        types = new Hashtable<String, Double>();
        classes = new Hashtable<String, String>();
        types.put("float" + "_" + "integer", 0.8);
        types.put("integer" + "_" + "text", 0.1);
        types.put("binary" + "_" + "integer", 0.5);
        types.put("float" + "_" + "text", 0.1);
        types.put("binary" + "_" + "float", 0.5);
        types.put("smalldatetime" + "_" + "int", 0.1);
        classes.put("float", "float");
        classes.put("real", "float");
        classes.put("money", "float");
        classes.put("smallmoney", "float");
        classes.put("int", "integer");
        classes.put("bigint", "integer");
        classes.put("smallint", "integer");
        classes.put("tinyint", "integer");
        classes.put("numeric", "integer");
        classes.put("varchar", "text");
        classes.put("char", "text");
        classes.put("nvarchar", "text");
        classes.put("nchar", "text");
        classes.put("text", "text");
        classes.put("binary", "binary");
        classes.put("smalldatetime", "date");
    }

    private Double probability(String dataType1, String dataType2) {
        String A, B;
        if (dataType1 == null || dataType2 == null) {
            return 0.0;
        }
        dataType1 = classes.get(dataType1);
        dataType2 = classes.get(dataType2);
        if (dataType1.charAt(0) < dataType2.charAt(0)) {
            A = dataType1;
            B = dataType2;
        } else {
            A = dataType2;
            B = dataType1;
        }
        if (A.equals(B)) {
            return 1.0;
        }
        if (types.get(A + "_" + B) != null) return Double.parseDouble(types.get(A + "_" + B).toString()); else return 0.0;
    }

    /**
	 * As defined in Automatic Schema Matching for Data Warehousing It first
	 * eliminates all non alphanumeric characters from it
	 * 
	 * @param a
	 *            pair of attribute names
	 * @param b
	 * @return linguistic similarity
	 * 
	 */
    public double getLinguisticSimilarity(String a, String b) {
        int n = 0;
        double l = Math.max(a.length(), b.length());
        try {
            Vector<String> letters = new Vector<String>();
            for (Character letter : a.toCharArray()) {
                letters.add(letter + "");
            }
            for (Character c : b.toCharArray()) {
                if (letters.contains(c + "")) {
                    n++;
                }
            }
        } catch (ArithmeticException e) {
            return 0;
        }
        return n / l;
    }

    /**
	 * Out idea using free online thesaurus
	 * 
	 * @return
	 */
    public double getSemanticSimilarity(String a, String b) {
        URL url;
        URLConnection con = null;
        String str = "";
        String output = "";
        DataInputStream input;
        Vector<String> synonyms = new Vector<String>();
        a = a.toLowerCase();
        b = b.toLowerCase();
        try {
            url = new URL("http://freethesaurus.net/s.php?q=" + a);
            con = url.openConnection();
            input = new DataInputStream(con.getInputStream());
            while (null != ((str = input.readLine()))) {
                output += str;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        Scanner reader = new Scanner(output);
        String result = "";
        boolean print = false;
        while (reader.hasNext()) {
            String pick = reader.next();
            if (pick.indexOf("class=\"syn\">") >= 0) {
                print = true;
            }
            if (print && pick.indexOf("</div>") >= 0) {
                break;
            }
            if (print) {
                result += pick;
            }
        }
        Scanner parser = new Scanner(result);
        parser.useDelimiter("<spanonmouseover([^>]*)>");
        while (parser.hasNext()) {
            String str1[] = parser.next().split("<");
            if (str1[0].indexOf("class") < 0) {
                synonyms.add(str1[0].toLowerCase());
            }
        }
        if (synonyms.contains(b)) {
            System.out.println(a + " and " + b + " are synonyms ");
            return 1.0;
        }
        return 0.0;
    }

    /**
	 * Data type matching
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
    public double getStructuralSimilarity(String a, String b) {
        try {
            String relation1 = mapping1.getRelationByAttribute(b);
            String relation2 = mapping2.getRelationByAttribute(a);
            String type1 = db1.getDataType(relation1, b);
            String type2 = db2.getDataType(relation2, a);
            return probability(type1, type2);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
	 * Returns the average of the three similarity functions
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
    public double getSimilarityForIntegratedMatching(String a, String b) {
        return (getLinguisticSimilarity(a, b) + getSemanticSimilarity(a, b) + getStructuralSimilarity(a, b)) / 3;
    }

    /**
	 * Returns the average of the similarity functions described by Li et al
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
    public double getSimilarityForElementBasedMatching(String a, String b) {
        return (getLinguisticSimilarity(a, b) + getStructuralSimilarity(a, b)) / 2;
    }
}
