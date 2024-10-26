package asdtester.asd;

import asdtester.asd.ASDGrammarSuccessor;
import asdtester.asd.ASDInputException;
import java.io.*;
import java.net.*;
import java.util.*;

/**
   Instances can create an ASDGrammar from a character file.
   @author James A. Mason
   @version 1.05 2000 Mar 24-26, 30; Apr 26, 28; 2001 Feb 5-7;
   Oct 1; Nov 20, 23; 2004 Jan 20, 29
 */
public class ASDGrammarReader {

    /**
      Initializes a new ASDGrammarReader on a given file or URL.
      Throws an IOException or malformedURLException if the file
      or URL can't be opened.
      By default, pixel coordinates of grammar nodes are NOT loaded.
      @param fileName the name of the file or URL
    */
    public ASDGrammarReader(String fileName) throws IOException, MalformedURLException {
        this(fileName, false);
    }

    /**
      Initializes a new ASDGrammarReader on a given file or URL.
      Throws an IOException or malformedURLException if the file
      or URL can't be opened.
      @param fileName the name of the file or URL
      @param includeCoords indicates whether or not to include
      pixel coordinates in the grammar loaded, if they are
      present.  They are needed by the graphical grammar editor
      but not by the parser.
    */
    public ASDGrammarReader(String fileName, boolean includeCoords) throws IOException, MalformedURLException {
        includePixelCoords = includeCoords;
        fileName = fileName.trim();
        urlConnection = null;
        urlStream = null;
        if (fileName.substring(0, 5).equalsIgnoreCase("http:")) {
            URL fileURL = new URL(fileName);
            urlConnection = (HttpURLConnection) fileURL.openConnection();
            urlStream = urlConnection.getInputStream();
            reader = new ASDTokenReader(new BufferedReader(new InputStreamReader(urlStream)));
        } else reader = new ASDTokenReader(new FileReader(fileName));
    }

    /**
      Closes the InputStream used by the ASDGrammarReader,
      if the InputStream was opened from a URL.
    */
    public void close() throws IOException {
        if (urlConnection != null) {
            urlStream.close();
            urlConnection.disconnect();
        }
    }

    /**
      Gets an ASD grammar from the file.
      Throws an ASDInputException if the grammar representation
      in the file is ill-formed.
      @return a HashMap of words and ArrayLists of ASDGrammarNodes
      that represent their instances.
    */
    public HashMap getGrammar() throws IOException, ASDInputException {
        HashMap result = new HashMap(DEFAULT_CAPACITY);
        ArrayList wordEntry;
        while (true) {
            wordEntry = getWordEntry();
            if (wordEntry == null) break;
            result.put(wordEntry.get(0), wordEntry.get(1));
        }
        reader.close();
        return result;
    }

    /**
      Gets the next word entry from the file.
      @return an ArrayList consisting of a "word" String and a
       ArrayList of instances for it, each an ASDGrammarNode;
       or null if there are no more word entries in the file.
    */
    private ArrayList getWordEntry() throws IOException, ASDInputException {
        ArrayList result = new ArrayList();
        String word;
        ArrayList instances = new ArrayList();
        ASDGrammarNode node;
        currentToken = reader.getToken();
        if (currentToken.length() == 0) return null;
        if (!currentToken.equals("(")) {
            reader.close();
            throw new ASDInputException("missing ( at beginning of a word entry");
        }
        word = reader.getToken();
        if (word.equals("(") || word.equals(")")) {
            reader.close();
            throw new ASDInputException("missing word in a word entry");
        }
        result.add(word);
        currentToken = reader.getToken();
        if (!currentToken.equals("(")) {
            reader.close();
            throw new ASDInputException("missing ( around list of instance entries\n" + "for word " + word);
        }
        while (true) {
            node = getInstanceEntry(word);
            if (node == null) break;
            instances.add(node);
        }
        result.add(instances);
        reader.getRightParenthesis();
        return result;
    }

    /**
      Gets the next instance entry for a given word from the
      file, and returns a corresponding new ASDGrammarNode, or
      returns null if there are no more instances of the word
      in the grammar.
      The instance entry must have all of the information needed
      for a well-formed ASDGrammarNode;
      otherwise an ASDInputException is thrown.
    */
    private ASDGrammarNode getInstanceEntry(String word) throws IOException, ASDInputException {
        String instance;
        boolean begins;
        ArrayList beginsTypes = null;
        ArrayList successors = null;
        ArrayList successorTypes = null;
        String phraseType = null;
        String semanticValue = null;
        String semanticAction = null;
        boolean endOfEntry = false;
        short xCoord = 0;
        short yCoord = 0;
        currentToken = reader.getToken();
        if (currentToken.equals(")")) return null;
        instance = reader.getPseudoWord();
        currentToken = reader.getToken();
        if (currentToken.equalsIgnoreCase("nil") || currentToken.equalsIgnoreCase("null") || currentToken.equalsIgnoreCase("false")) begins = false; else if (currentToken.equalsIgnoreCase("t") || currentToken.equalsIgnoreCase("true")) {
            begins = true;
            beginsTypes = null;
        } else if (!currentToken.equals("(")) {
            reader.close();
            throw new ASDInputException("missing parenthesis at start of 'begins' field\n" + "for word " + word + " instance " + instance);
        } else {
            currentToken = reader.getToken();
            if (currentToken.equals(")")) begins = false; else {
                char ch;
                begins = true;
                beginsTypes = new ArrayList();
                do {
                    ch = currentToken.charAt(0);
                    if (ch == '(' || ch == '"' || ch == '\'' || Character.isDigit(ch)) {
                        reader.close();
                        throw new ASDInputException("expected phrase type name missing " + "in 'begins' field\nfor word " + word + " instance " + instance + "\nInstead found a token beginning with " + ch);
                    }
                    beginsTypes.add(currentToken);
                    currentToken = reader.getToken();
                    if (currentToken.length() == 0) {
                        reader.close();
                        throw new ASDInputException("missing ) at end of 'begins' list\n" + "for word " + word + " instance " + instance);
                    }
                } while (!currentToken.equals(")"));
            }
        }
        currentToken = reader.getToken();
        if (currentToken.equals(")") || currentToken.length() == 0) {
            reader.close();
            throw new ASDInputException("successors field missing\nfor word " + word + " instance " + instance);
        } else if (currentToken.equals("(")) {
            successors = new ArrayList();
            currentToken = reader.getToken();
            while (!currentToken.equals(")")) {
                if (!currentToken.equals("(")) {
                    reader.close();
                    throw new ASDInputException("missing " + "( at start of a (word instance ... ) " + "entry\nin successors list for word " + word + " instance " + instance);
                }
                ASDGrammarSuccessor successor = new ASDGrammarSuccessor(reader.getPseudoWord(), reader.getPseudoWord());
                if (includePixelCoords) {
                    short xEdgeCoord = 0;
                    short yEdgeCoord = 0;
                    currentToken = reader.getToken();
                    char ch = currentToken.charAt(0);
                    if (Character.isDigit(ch)) {
                        try {
                            xEdgeCoord = Short.parseShort(currentToken);
                        } catch (NumberFormatException e) {
                            reader.close();
                            throw new ASDInputException("invalid edge pixel " + "coordinate \"" + currentToken + "\"\n" + "in successors list for word " + word + " instance " + instance);
                        }
                        currentToken = reader.getToken();
                        try {
                            yEdgeCoord = Short.parseShort(currentToken);
                        } catch (NumberFormatException e) {
                            reader.close();
                            throw new ASDInputException("invalid or missing edge " + "pixel coordinate\nin successors list for word " + word + " instance " + instance);
                        }
                        successor.setXCoordinate(xEdgeCoord);
                        successor.setYCoordinate(yEdgeCoord);
                        currentToken = reader.getToken();
                    }
                } else for (int j = 0; j < 3; ++j) {
                    currentToken = reader.getToken();
                    if (currentToken.equals(")")) break;
                }
                if (!currentToken.equals(")")) {
                    reader.close();
                    throw new ASDInputException("missing " + ") at end of (word instance ... ) entry\n" + "in successors list for word " + word + " instance " + instance);
                }
                successors.add(successor);
                currentToken = reader.getToken();
            }
        } else phraseType = currentToken;
        currentToken = reader.getToken();
        if (currentToken.equals(")")) {
            endOfEntry = true;
            if (successors != null) successorTypes = new ArrayList(0);
        } else if (currentToken.equalsIgnoreCase("t")) ; else {
            successorTypes = new ArrayList();
            if (currentToken.equalsIgnoreCase("nil") || currentToken.equalsIgnoreCase("null")) ; else if (currentToken.equals("(")) {
                currentToken = reader.getToken();
                char ch;
                while (!currentToken.equals(")")) {
                    ch = currentToken.charAt(0);
                    if (ch == '(' || ch == '"' || ch == '\'' || Character.isDigit(ch)) {
                        reader.close();
                        throw new ASDInputException("expected phrase type name missing " + "or starts with digit character " + "in 'successorTypes' field\nfor word " + word + " instance " + instance);
                    }
                    successorTypes.add(currentToken);
                    currentToken = reader.getToken();
                    if (currentToken.length() == 0) {
                        reader.close();
                        throw new ASDInputException("missing ) " + " at end of 'successorTypes' list\n" + "for word " + word + " instance " + instance);
                    }
                }
            } else if (currentToken.length() > 0) {
                char ch = currentToken.charAt(0);
                if (ch != '\"' && ch != '\'') {
                    reader.close();
                    throw new ASDInputException("missing quote " + "at beginning of semantic value field\n" + "for word " + word + " instance " + instance);
                }
                semanticValue = currentToken.substring(1, currentToken.length() - 1);
            }
        }
        if (!endOfEntry) {
            currentToken = reader.getToken();
            if (!currentToken.equals(")")) {
                char ch = currentToken.charAt(0);
                if (currentToken.equalsIgnoreCase("nil") || currentToken.equalsIgnoreCase("null")) ; else if (ch != '\"' && ch != '\'') {
                    reader.close();
                    throw new ASDInputException("missing quote " + "at beginning of semantic action field\n" + "for word " + word + " instance " + instance);
                } else semanticAction = currentToken.substring(1, currentToken.length() - 1);
                if (includePixelCoords) {
                    xCoord = 0;
                    yCoord = 0;
                    currentToken = reader.getToken();
                    if (currentToken.length() == 0) {
                        reader.close();
                        throw new ASDInputException("unexpected end of grammar file " + "in entry for word " + word + "instance " + instance);
                    }
                    if (Character.isDigit(currentToken.charAt(0))) {
                        try {
                            xCoord = Short.parseShort(currentToken);
                        } catch (NumberFormatException e) {
                            reader.close();
                            throw new ASDInputException("invalid edge pixel " + "coordinate \"" + currentToken + "\"\n" + "in entry for word " + word + " instance " + instance);
                        }
                        currentToken = reader.getToken();
                        if (currentToken.length() == 0) {
                            reader.close();
                            throw new ASDInputException("unexpected end of grammar file" + " in entry for word " + word + "instance " + instance);
                        }
                        try {
                            yCoord = Short.parseShort(currentToken);
                        } catch (NumberFormatException e) {
                            reader.close();
                            throw new ASDInputException("invalid or missing " + "pixel coordinate\nin entry for word " + word + " instance " + instance);
                        }
                        currentToken = reader.getToken();
                    }
                } else for (int j = 0; j < 3; ++j) {
                    currentToken = reader.getToken();
                    if (currentToken.equals(")")) break;
                }
                if (!currentToken.equals(")")) {
                    reader.close();
                    throw new ASDInputException("missing " + ") at end of a word instance entry\n" + "for word " + word + " instance " + instance);
                }
            }
        }
        ASDGrammarNode result = new ASDGrammarNode(word, instance, begins, beginsTypes, successors, successorTypes, phraseType, semanticValue, semanticAction);
        if (includePixelCoords) {
            result.setXCoordinate(xCoord);
            result.setYCoordinate(yCoord);
        }
        return result;
    }

    private static final int DEFAULT_CAPACITY = 101;

    HttpURLConnection urlConnection;

    InputStream urlStream = null;

    private asdtester.asd.ASDTokenReader reader;

    private String currentToken;

    private boolean includePixelCoords;
}
