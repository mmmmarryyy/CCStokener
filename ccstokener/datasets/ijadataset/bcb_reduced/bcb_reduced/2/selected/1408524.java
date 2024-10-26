package com.apelon.matchpack.lexicon.dictionary;

import com.apelon.matchpack.MatchPackException;
import com.apelon.matchpack.lexicon.LexiconEntry;
import com.apelon.matchpack.lexicon.WordEntry;
import com.apelon.matchpack.spellcheck.SpellCheck;
import com.apelon.matchpack.spellcheck.algorithm.EditDistance;
import com.apelon.matchpack.spellcheck.algorithm.PhoneticAlgorithm;
import com.apelon.matchpack.spellcheck.algorithm.PhoneticAlgorithmEngine;
import java.io.*;
import java.net.URL;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *  This class loads words from lex_dictionary and save them into memory
 */
public class LexiconSpellCheckSoundCode implements SpellCheck {

    protected static Map dictionary;

    protected static Map apelonAdditions;

    protected static Map wordDictionary;

    protected static PhoneticAlgorithm algorithm;

    private static Map variantsDictionary;

    private PreparedStatement selectLexDictionaryStat;

    protected static final String NEWLINE = System.getProperty("line.separator");

    protected static final int MAX_EDIT_DISTANCE = 100;

    protected static final String SOUND_CODE = "ABKTEFHIJLMNOPRSTUXY";

    private static final String ApelonAdditonFileName = "apelon_additions.dic";

    public LexiconSpellCheckSoundCode() throws MatchPackException {
        try {
            PhoneticAlgorithmEngine pae = PhoneticAlgorithmEngine.getInstance();
            String name = "com.apelon.matchpack.spellcheck.algorithm.Metaphone";
            algorithm = pae.getPhoneticAlgorithm(name);
        } catch (Exception e) {
            throw new MatchPackException(e.getMessage());
        }
    }

    /**
     * This method reads the words from the table
     *
     * @param obj JDBC connection
     * @throws MatchPackException thrown if there is an error during the
     *    construction of the memory table.
     */
    public synchronized void loadResource(Object obj) throws MatchPackException {
        if (dictionary != null) {
            return;
        }
        if (wordDictionary != null) {
            return;
        }
        try {
            dictionary = loadURL("lex.dic");
            wordDictionary = loadURL("word.dic");
            loadApelonAdditons();
        } catch (Exception e) {
            throw new MatchPackException("reading dictioanry: lex.dic: " + e.getMessage());
        }
        return;
    }

    private void loadApelonAdditons() throws Exception {
        try {
            apelonAdditions = loadURL(ApelonAdditonFileName);
            Iterator iter = apelonAdditions.keySet().iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                ArrayList apelon_additons = (ArrayList) apelonAdditions.get(key);
                List lex_entries = (List) dictionary.get(key);
                lex_entries.addAll(apelon_additons);
                dictionary.put(key, lex_entries);
            }
        } catch (Exception e) {
        }
    }

    public String serializeLexicon() throws Exception {
        URL url = this.getClass().getResource("lex.dic");
        String path = url.getPath();
        String jar_path = path.substring(path.indexOf("/") + 1, path.indexOf("!"));
        String internal_package = path.substring(path.indexOf("!/") + 2, path.lastIndexOf("/"));
        ZipInputStream in = new ZipInputStream(new FileInputStream(new File(jar_path)));
        String new_version = jar_path.substring(0, jar_path.indexOf(".jar")) + ".zip";
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File(new_version)));
        ZipEntry entry = null;
        while ((entry = in.getNextEntry()) != null) {
            if (entry.getName().indexOf(ApelonAdditonFileName) != -1) continue;
            out.putNextEntry(entry);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
        if (apelonAdditions != null) {
            out.putNextEntry(new ZipEntry(internal_package + "/" + ApelonAdditonFileName));
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bout);
            os.writeObject(apelonAdditions);
            out.write(bout.toByteArray(), 0, bout.size());
            os.close();
        }
        out.closeEntry();
        out.close();
        return new_version;
    }

    protected Map loadURL(String name) throws Exception {
        long start = System.currentTimeMillis();
        URL url = this.getClass().getResource(name);
        ObjectInputStream si = new ObjectInputStream(url.openStream());
        Map dict = (Map) si.readObject();
        long end = System.currentTimeMillis();
        si.close();
        return dict;
    }

    private Map getDictionary() {
        return dictionary;
    }

    private void storeDictionary(Map map, String key, WordEntry entry) {
        Object o = map.get(key);
        List l = null;
        if (o == null) {
            l = new ArrayList();
        } else {
            l = (List) o;
        }
        l.add(entry);
        map.put(key, l);
    }

    /**
     * This method finds the best candidate of words from the memory table.
     * Currently, the words whose edit distance is less than 1 will be returned.
     *
     * @param word input word
     * @return candidate words
     * @throws MatchPackException thrown if there is an error
     *    in double-metaphone.
     */
    public String[] getSuggestions(String word, int suggestionLimit) throws MatchPackException {
        final String upperWord = word.toUpperCase();
        String[] code = algorithm.getCodes(upperWord);
        HashSet set = new HashSet();
        for (int i = 0; i < code.length; i++) {
            getBestCandidates(upperWord, code[i], MAX_EDIT_DISTANCE, set);
        }
        String[] result = (String[]) set.toArray(new String[0]);
        return getTopResult(result, upperWord, suggestionLimit);
    }

    public String[] getSuggestions(String word) throws MatchPackException {
        return getSuggestions(word, 3);
    }

    protected static String[] getTopResult(String[] result, final String upperWord, int suggestionLimit) {
        Arrays.sort(result, new Comparator() {

            public int compare(Object s1, Object s2) {
                int s1Distance = EditDistance.computeDistance((String) s1, upperWord);
                int s2Distance = EditDistance.computeDistance((String) s2, upperWord);
                return (s1Distance - s2Distance);
            }

            public boolean equals(Object s1) {
                String s = (String) s1;
                return this.equals(s1);
            }
        });
        int minLength = (suggestionLimit > result.length) ? result.length : suggestionLimit;
        String[] top = new String[minLength];
        System.arraycopy(result, 0, top, 0, minLength);
        return top;
    }

    /**
     * This method finds the word with the same sound code in the table.
     * Then it computes the edit distance of words from the table.
     * If the edit distance is smaller than the given distance, the words
     * will be added into set.
     *
     * @param word input word
     * @param code double metaphone code
     * @param editdistance edit distance between the given word and
     *          word from the table whose sound code is same as the sound code of
     *          given word
     * @param set set contains the words from the memory.
     */
    protected void getBestCandidates(String word, String code, int editdistance, Set set) {
        List wordList = (List) wordDictionary.get(code);
        if (wordList == null) {
            return;
        }
        int size = wordList.size();
        for (int i = 0; i < size; i++) {
            String candidate = (String) wordList.get(i);
            if (isBaseNormal(candidate)) {
                int distance = EditDistance.computeDistance(word, candidate);
                if (distance == 0) {
                    set.clear();
                    set.add(candidate);
                    return;
                }
                set.add(candidate);
            }
        }
    }

    /**
     * This method returns the base for given word. If there is no base
     * form, word itself is returned.
     *
     * @param word the variants
     * @return word entries which cotains bases
     * @throws MatchPackException
     */
    public WordEntry[] getBases(String word) throws MatchPackException {
        String upperWord = word.toUpperCase();
        List l = (List) dictionary.get(upperWord);
        if (l == null) {
            boolean status = isNumer(word);
            WordEntry entry = null;
            if (status) {
                entry = new WordEntry(word, LexiconEntry.NUMBER_CAT, word);
            } else {
                entry = new WordEntry(word, LexiconEntry.NOTEXIST_CAT, word);
            }
            return new WordEntry[] { entry };
        }
        return (WordEntry[]) l.toArray(new WordEntry[0]);
    }

    public boolean addNewBaseForWord(String word, String newBase) {
        List l_tmp = null;
        newBase = newBase.toUpperCase();
        String upperWord = word.toUpperCase();
        List l = (List) dictionary.get(upperWord);
        if (l == null) l = new ArrayList();
        Iterator iter = l.iterator();
        while (iter.hasNext()) {
            WordEntry we = (WordEntry) iter.next();
            if (we.getBase().equalsIgnoreCase(newBase)) return false;
        }
        boolean status = isNumer(word);
        WordEntry entry = null;
        if (status) {
            entry = new WordEntry(word, LexiconEntry.NUMBER_CAT, newBase);
        } else {
            l_tmp = (List) dictionary.get(newBase);
            String cat = LexiconEntry.NOTEXIST_CAT;
            if (l_tmp.size() > 0) cat = ((WordEntry) l_tmp.get(0)).getCategory();
            entry = new WordEntry(word, cat, newBase);
        }
        l.add(entry);
        dictionary.put(upperWord, l);
        return addToApelonAdditions(entry);
    }

    public boolean removeBaseForWord(String word, String remove) {
        boolean got_it = false;
        String upperWord = word.toUpperCase();
        remove = remove.toUpperCase();
        List l = (List) apelonAdditions.get(upperWord);
        if (l == null || l.size() == 0) return got_it;
        Iterator iter = l.iterator();
        while (iter.hasNext()) {
            WordEntry we = (WordEntry) iter.next();
            if (we.getBase().equals(remove)) {
                iter.remove();
                got_it = true;
                break;
            }
        }
        apelonAdditions.put(upperWord, l);
        return got_it;
    }

    private boolean addToApelonAdditions(WordEntry we) {
        if (apelonAdditions == null) apelonAdditions = new HashMap(100);
        String upperWord = we.getWord().toUpperCase();
        List l = (List) apelonAdditions.get(upperWord);
        if (l == null) l = new ArrayList();
        Iterator iter = l.iterator();
        while (iter.hasNext()) {
            WordEntry we_tmp = (WordEntry) iter.next();
            if (we_tmp.getBase().equalsIgnoreCase(we.getBase())) return false;
        }
        l.add(we);
        apelonAdditions.put(upperWord, l);
        return true;
    }

    protected boolean isBaseNormal(String word) {
        try {
            WordEntry[] entry = getBases(word);
            if (entry.length == 0) {
                return false;
            }
            for (int i = 0; i < entry.length; i++) {
                if (entry[i].getForm() != WordEntry.NORMAL_FORM) {
                    return false;
                }
            }
        } catch (MatchPackException mpe) {
            return false;
        }
        return true;
    }

    /**
     * This method is detect if the number is valid format
     * @param word
     * @return true if the word is number otherwise false
     */
    private boolean isNumer(String word) {
        boolean result = false;
        try {
            Double.parseDouble(word);
            result = true;
        } catch (Exception e) {
            result = false;
        } finally {
            return result;
        }
    }

    /**
     * This method returns variants of the given base. If there is no variant
     * 0-length of word entry is returned.
     *
     * @param word the base form
     * @return word entries which contains the variants
     * @throws MatchPackException
     * @see com.apelon.matchpack.lexicon.WordEntry
     */
    public WordEntry[] getVariants(String word) throws MatchPackException {
        String upperWord = word.toUpperCase();
        if (variantsDictionary == null) {
            try {
                long start = System.currentTimeMillis();
                URL url = this.getClass().getResource("varlex.dic");
                ObjectInputStream si = new ObjectInputStream(url.openStream());
                variantsDictionary = (Map) si.readObject();
                long end = System.currentTimeMillis();
                System.out.println("loaded " + (end - start) + "ms");
                si.close();
            } catch (Exception e) {
                throw new MatchPackException("cannot load: varlex.dic " + e.getMessage());
            }
        }
        List l = (List) variantsDictionary.get(upperWord);
        if (l == null) {
            return new WordEntry[0];
        }
        return (WordEntry[]) l.toArray(new WordEntry[0]);
    }

    protected String[] insertion(String source, String insertString) {
        String[] result = new String[(source.length() + 1) * insertString.length()];
        int count = 0;
        for (int i = 0; i <= source.length(); i++) {
            String first = source.substring(0, i);
            String second = source.substring(i, source.length());
            for (int j = 0; j < insertString.length(); j++) {
                StringBuffer temp = new StringBuffer();
                temp.append(first);
                temp.append(insertString.charAt(j));
                temp.append(second);
                result[count++] = temp.toString();
            }
        }
        return result;
    }

    protected String[] deletion(String source) {
        String[] result = new String[source.length()];
        int count = 0;
        for (int i = 0; i < source.length(); i++) {
            StringBuffer temp = new StringBuffer(source);
            temp.deleteCharAt(i);
            result[count++] = temp.toString();
        }
        return result;
    }

    protected String[] replacement(String source, String insertString) {
        String[] result = new String[source.length() * insertString.length()];
        int count = 0;
        for (int i = 0; i < source.length(); i++) {
            for (int j = 0; j < insertString.length(); j++) {
                StringBuffer temp = new StringBuffer(source);
                temp.setCharAt(i, insertString.charAt(j));
                result[count++] = temp.toString();
            }
        }
        return result;
    }

    protected String[] transposition(String source) {
        String[] result = new String[source.length() - 1];
        int count = 0;
        for (int i = 0; i < source.length() - 1; i++) {
            StringBuffer temp = new StringBuffer(source);
            char current = temp.charAt(i);
            char next = temp.charAt(i + 1);
            temp.setCharAt(i, next);
            temp.setCharAt(i + 1, current);
            result[count++] = temp.toString();
        }
        return result;
    }

    private void testManipulation() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter the string:");
        String input = in.readLine();
        if (input == null || input.length() == 0) {
            return;
        }
        String insert = "#";
        String[] result = insertion(input, insert);
        for (int i = 0; i < result.length; i++) {
            System.out.println("insert: " + result[i]);
        }
        result = deletion(input);
        for (int i = 0; i < result.length; i++) {
            System.out.println("deletion: " + result[i]);
        }
        result = replacement(input, insert);
        for (int i = 0; i < result.length; i++) {
            System.out.println("replacement: " + result[i]);
        }
        result = transposition(input);
        for (int i = 0; i < result.length; i++) {
            System.out.println("transposition: " + result[i]);
        }
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        LexiconSpellCheckSoundCode ldt = new LexiconSpellCheckSoundCode();
        ldt.loadResource(null);
        String input = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.println("/nEnter the choice:");
            System.out.println("To exit: press enter");
            System.out.println("To get variants: 1");
            input = in.readLine();
            if (input == null || input.length() == 0) {
                return;
            }
            int choice = 0;
            if (choice == 1) {
                System.out.println("Enter the base");
                System.out.println("To exit: press enter");
                input = in.readLine();
                if (input == null || input.length() == 0) {
                    return;
                }
                WordEntry[] entries = ldt.getVariants(input);
                for (int i = 0; i < entries.length; i++) {
                    System.out.println("variants: " + entries[i].getWord() + " category: " + entries[i].getCategory() + " form: " + entries[i].getForm());
                }
            }
        }
    }
}
