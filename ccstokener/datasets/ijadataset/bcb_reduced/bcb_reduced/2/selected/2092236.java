package com.objectwave.templateMerge;

import java.io.*;
import java.util.*;

/**
 *  Read a flatfile and convert it's contexts to a MergeTemplate object,
 *  possibly containing some MergeTemplates itself.  This is useful for
 *  defining a file which will contain text & token tags, with a couple of
 *  extra tags suported:
 *    beginBlock ... [ beginBlock ... endBlock] ... endBlock
 *  and
 *    include <filename>
 *
 *  The beginBlock/endBlock tags are to accomodate the "collection" token types.
 *  For instance, we may want to iterate all Equipment objects in a given
 *  Quote.lease.equipmentSummary, so instead of having to define several
 *  different templates/files (preEquipment, iterEquipment, postEquipment),
 *  we can simply define a file as:
 *
 *    ... pre-equipment text ...
 *    beginBlock
 *      ... iterEquipment text ...
 *    endBlock
 *    ... post-equipment text ...
 *
 *    Furthermore, we can nest these structures.  Suppose each Equipment
 *  object had a collection of Parts:
 *
 *    ... preEquipment ...
 *    beginBlock
 *      ... iterEquipmentPreParts ...
 *      beginBlock
 *         ... iterParts ...
 *      endBlock
 *      ... iterEquipmentPostParts ...
 *    endBlock
 *    ... postEquipment ...
 *
 *    @note I'm not sure that this nesting capability is fully supported
 *          by this object. It hasn't been tested.
 *
 *      The "importURL file://otherStuff.txt" command is relatively obvious: it'll
 *    replace the "importURL file://otherStuff.txt" with the full text of the secified URL 
 *    The filename is either a combination of 1 or more of [a-zA-Z0-9.$_-\/:], or it's
 *    anything enclosed by "'s.
 *
 */
public class TemplatePreprocessor {

    public static boolean debugOn = false;

    InformationToken tokens[] = null;

    String beginTag = "BeginTokenBlock";

    String endTag = "EndTokenBlock";

    String importTag = "ImportURL";

    int fileSize = 0;

    public TemplatePreprocessor() {
        debugOn = true;
    }

    /**
	* Method for debuggin purposes only.  If the public data member "debugOn" is true, then
	* it'll print the passed string w/ a cute li'l prefix.
	*/
    protected void dout(String s) {
        if (debugOn == true) System.out.println("TemplatePreprocessor: " + s);
    }

    /**
	 * Return the next non-whitespace character (not a space, tab, newline, for formfeed).
	 */
    protected int eatWhitespace(char[] array, int index) {
        for (; index < array.length; ++index) if (" \t\n\r".indexOf(array[index]) < 0) break;
        return index;
    }

    /**
	 *  Find the next endTag, after matching nested pairs of beginTag, endTag.  If
	 *  beginPos is at the beginning of a beginTag, that begin tag is ignored for the
	 *  purposes of nested-pair matching.
	 */
    protected int findEndTag(char[] array, int beginPos, int endPos) {
        if (beginPos + beginTag.length() < array.length && beginTag.equals(new String(array, beginPos, beginTag.length()))) {
            beginPos += beginTag.length();
        }
        dout("findEndTag");
        int nestingDepth = 1;
        int pos = beginPos;
        for (; ; ) {
            int beginTagPos = findFirstOf(array, beginTag, pos, endPos);
            int endTagPos = findFirstOf(array, endTag, pos, endPos);
            if (beginTagPos > 0 && beginTagPos < endTagPos) {
                dout("Increase nesting depth by 1");
                ++nestingDepth;
                pos = beginTagPos + beginTag.length();
            } else if (endTagPos > 0) {
                dout("Decreasing nesting depth by 1");
                --nestingDepth;
                if (nestingDepth == 0) {
                    dout(">>>>>>> Matched tag @ position " + endTagPos + " <<<<<<<");
                    return endTagPos;
                }
                pos = endTagPos + endTag.length();
            } else {
                return -1;
            }
        }
    }

    /**
	 *  Provide a custom method to find substrings in a char array.  This is to save the
	 *  memory costs of building a String and doing it that way.  This method can also expect
	 *  the tagPrefix substring to preceed any of these strings.  The returned index is the
	 *  index of the first character of "findMe".
	 *  
	 *  Use of the custom for(int j...) {...} loop instead of use of findMe.equals(...) 
	 *  is a good optimization.  It makes (end-begin-findMe.length())*findMe.length() fewer
	 *  array comparisons.
	 */
    protected int findFirstOf(char[] array, String findMe, int beginIndex, int endIndex) {
        int i;
        for (i = beginIndex; i < array.length; ++i) {
            i = eatWhitespace(array, i);
            if (i + findMe.length() <= endIndex) {
                int j;
                for (j = 0; j < findMe.length(); ++j) if (findMe.charAt(j) != array[i + j]) break;
                if (j == findMe.length()) return i;
                if (array[i + j] == findMe.charAt(0)) --i;
                i += j;
            }
        }
        return -1;
    }

    public String getBeginTag() {
        return beginTag;
    }

    public String getEndTag() {
        return endTag;
    }

    public String getImportTag() {
        return importTag;
    }

    public InformationToken[] getTokens() {
        return tokens;
    }

    /**
	 * (attempt to) import files indicated by the importTag.  The 
	 * importTag expects a single argument, either a word or a "-delimited
	 * string.  Any number of whitespace characters can be present between the 
	 * importTag and the argument.  Using the default importTag value "importURL", 
	 * a line of the source file may look like this:
	 *
	 *    importURL   "c:\program files\common files\vendor.htm"   \n
	 *
	 *   The imported file has importURL run on it, too, so inclusions can be 
	 * recursive.  If there's a problem reading the indicated file due to some
	 * problem with the argument or whatever, a warning will be issued and nothing
	 * will be substituted.  That is, the tag and argument will be eaten and be 
	 * replaced by the empty string "".
	 */
    public char[] importURLs(char array[]) {
        dout(" ----> importURLs");
        StringBuffer result = new StringBuffer(array.length);
        int pos = 0;
        for (; ; ) {
            int tagPos = findFirstOf(array, importTag, pos, array.length);
            if (tagPos > pos) result.append(array, pos, tagPos - pos);
            if (tagPos < 0) {
                result.append(array, pos, array.length - pos);
                dout("Didn't find any import tag after tagPos.");
                break;
            }
            int argPos = tagPos + importTag.length();
            int argEnd = -1;
            String importURL = "";
            argPos = eatWhitespace(array, argPos);
            if (argPos < 0) System.err.println("Missing argument to " + importTag + "."); else if (array[argPos] == '\"' && argPos + 1 != importTag.length()) {
                argEnd = findFirstOf(array, "\"", argPos + 1, array.length);
                if (argEnd < 0) System.err.println("Couldn't match \" as argument to " + importTag); else importURL = new String(array, argPos + 1, argEnd - argPos - 1);
            } else {
                argEnd = argPos;
                while (argEnd < array.length && (array[argEnd] != ' ' && array[argEnd] != '\t' && array[argEnd] != '\n' && array[argEnd] != '\r')) {
                    ++argEnd;
                }
                importURL = new String(array, argPos, argEnd - argPos);
            }
            dout("importURL = \"" + importURL + "\"");
            if (importURL.length() == 0) {
                System.err.println("Import filename is empty. Cannot import.");
                continue;
            }
            try {
                StringBuffer importBuffer = readURL(new java.net.URL(importURL));
                char importedChars[] = new char[importBuffer.length()];
                importBuffer.getChars(0, importBuffer.length(), importedChars, 0);
                importedChars = importURLs(importedChars);
                result.append(importedChars);
            } catch (IOException e) {
                System.err.println("Error importing URL \"" + importURL + "\"");
            }
            pos = argEnd + 1;
        }
        char resultChars[] = new char[result.length()];
        result.getChars(0, result.length(), resultChars, 0);
        dout(" <---- importURLs");
        return resultChars;
    }

    public int lastProcessingFileSize() {
        return fileSize;
    }

    /**
	*  A main method to test to the class to some degree.
	*/
    public static void main(String args[]) {
        if (args.length != 2) {
            System.err.println("Expected 2 args: find arg1 in arg2.");
            return;
        }
        TemplatePreprocessor tpp = new TemplatePreprocessor();
        tpp.debugOn = true;
        char array[] = new char[args[1].length()];
        args[1].getChars(0, args[1].length(), array, 0);
        String find = args[0];
        System.out.println("Find \"" + find + "\"");
        int here = tpp.findFirstOf(array, find, 0, array.length);
        System.out.println("Found \"" + find + "\" at position " + here + " of \"" + array + "\"");
    }

    /**
	 * Create a MergeTemplate from the subarray from beginPos to endPos-1.
	 *
	 * The data will presented in the form
	 *
	 *   ... text section #0 ...
	 *   [ beginTag ... sub-template #1 text ... endTag ]
	 *   ... text section #1 ...
	 *   [ beginTag ... sub-template #2 text ... endTag ]
	 *   ... text section #2 ...
	 *   [ beginTag ... sub-template #n text ... endTag ]
	 *   ... text section #n...
	 *
	 *   Each sub-template will have the above format.  This method processes this
	 *   information to yield a structured MergeTemplate.  Here's a sample of the template
	 *   structure, where n = 3 and only sub-template #2 has an additional sub-template:
	 *
	 *   template [ prefix=section#0, suffix = null ]
	 *      |
	 *      `----> template [ prefix=sub-template#1 text, suffix = null ]
	 *      `----> template [ prefix=section#1 text, suffix = null ]
	 *      `----> template [ prefix=sub-template#2 text, suffix = null ]
	 *      |         |
	 *      |         `----> template [prefix=sub-template#2a text, suffix = null]
	 *      `----> template [ prefix=section#2 text, suffix = null ]
	 *      `----> template [ prefix=sub-template#3 text, suffix = null ]
	 *      `----> template [ prefix=section#3 text, suffix = null ]
	 */
    protected MergeTemplate processArray(char array[], int beginPos, int endPos) throws IOException {
        dout(" --> processArray[" + beginPos + "," + endPos + "]");
        MergeTemplate template = new MergeTemplate();
        int lhsPos = findFirstOf(array, beginTag, beginPos, endPos);
        int preBodyLen = (lhsPos < 0 ? endPos : lhsPos) - beginPos;
        String preBody = new String(array, beginPos, preBodyLen);
        template.setPreBody(preBody);
        Vector tokensUsed = new Vector();
        for (int i = 0; tokens != null && i < tokens.length; ++i) {
            if (tokens[i] != null && preBody.indexOf(tokens[i].getTokenString()) >= 0) {
                tokensUsed.addElement(tokens[i]);
            }
        }
        dout("Found " + tokensUsed.size() + " token" + (tokensUsed.size() == 1 ? "" : "s") + " in the text body.");
        if (tokensUsed.size() > 0) {
            InformationToken tokens[] = new InformationToken[tokensUsed.size()];
            for (int i = 0; i < tokensUsed.size(); ++i) tokens[i] = (InformationToken) tokensUsed.elementAt(i);
            template.setPreTokens(tokens);
        }
        while (lhsPos > -1 && lhsPos < endPos) {
            lhsPos += beginTag.length();
            dout("Find \"" + endTag + "\".");
            int rhsPos = findEndTag(array, lhsPos, endPos);
            if (rhsPos < 0) throw new IOException("Invalid file format: missing end marker.");
            if (rhsPos > endPos) throw new IOException("Invalid file format: " + endTag + " is beyond legal bounds.");
            dout("Setting child template [block]");
            MergeTemplate childTemplate = processArray(array, lhsPos, rhsPos);
            template.addTemplate(childTemplate);
            lhsPos = findFirstOf(array, beginTag, rhsPos + endTag.length(), endPos);
            if (lhsPos < 0) lhsPos = endPos;
            if (lhsPos > rhsPos) {
                dout("Setting child template [string]");
                childTemplate = processArray(array, rhsPos + endTag.length(), lhsPos);
                template.addTemplate(childTemplate);
            }
        }
        dout(" <-- processArray[" + beginPos + "," + endPos + "]");
        System.out.println("");
        return template;
    }

    /**
	*  Creates an inputstream based on the File object. A simple fileObject
	*  can be created like so:
	*     File file = new File("c:\autoexec.bat");
	*/
    public MergeTemplate processFile(java.io.File file) throws IOException, SecurityException {
        if (!file.isFile()) throw new FileNotFoundException(file.toString());
        FileInputStream fileStream = new FileInputStream(file);
        return processStream(fileStream);
    }

    /**
	*    This method will create the File object to be used, given a String
	*  which is presumably the filename.
	*/
    public MergeTemplate processFile(String filename) throws IOException, SecurityException {
        return processFile(new File(filename));
    }

    /**
	*  On the presumption (uh-oh) that the stream sizes involved here will be
	*  relatively small (less than 1MB), we'll read the entire stream into
	*  a StringBuffer to simplify our manipulation of the data.
	*/
    public MergeTemplate processStream(InputStream is) throws IOException {
        char[] array = readStream(is);
        dout("Array returned has length " + array.length + ".");
        fileSize = array.length;
        array = importURLs(array);
        return processArray(array, 0, array.length);
    }

    /**
	 *  Build a character array of all available data in the InputStream.  If the InputStream
	 *  is blocking and the data always gets slurped by someone else between isAvailable() and 
	 *  read(), or if there's a continuous stream of data coming from InputStream, then this
	 *  method could continue until memory runs out.  These scenarios are not worth 
	 *  accomodating since the InputStream is gonna be a file, so there will be few IO issues.
	 */
    protected char[] readStream(InputStream is) throws IOException {
        byte allBytes[] = new byte[0];
        int expectedNum = 0;
        for (; ; ) {
            expectedNum = is.available();
            dout("Expecting " + expectedNum + " more bytes.");
            if (expectedNum <= 0) break;
            byte moreBytes[] = new byte[expectedNum];
            int numBytes = is.read(moreBytes);
            if (numBytes <= 0) {
                dout("TemplatePreprocessor warning: was told to expect " + expectedNum + " bytes; got " + numBytes + " instead.");
                break;
            }
            dout("Expected " + expectedNum + " bytes, got " + numBytes + ".");
            if (numBytes < expectedNum) {
                byte newBytes[] = new byte[numBytes];
                for (int i = 0; i < moreBytes.length; ++i) newBytes[i] = moreBytes[i];
                moreBytes = newBytes;
            }
            byte newBytes[] = new byte[allBytes.length + moreBytes.length];
            int i;
            for (i = 0; i < allBytes.length; ++i) newBytes[i] = allBytes[i];
            for (int j = 0; j < moreBytes.length; ++j) newBytes[i + j] = moreBytes[j];
            allBytes = newBytes;
        }
        dout("Entire file size read: " + allBytes.length);
        char allChars[] = new char[allBytes.length];
        for (int i = 0; i < allChars.length; ++i) allChars[i] = (char) allBytes[i];
        return allChars;
    }

    /**
	* Read a full stream from a given URL, until the stream is empty or an exception
	* occurs.
	*/
    protected StringBuffer readURL(java.net.URL url) throws IOException {
        StringBuffer result = new StringBuffer(4096);
        InputStreamReader reader = new InputStreamReader(url.openStream());
        for (; ; ) {
            char portion[] = new char[4096];
            int numRead = reader.read(portion, 0, portion.length);
            if (numRead < 0) break;
            result.append(portion, 0, numRead);
        }
        dout("Read " + result.length() + " bytes.");
        return result;
    }

    public void setBeginTag(String tag) {
        beginTag = tag;
    }

    public void setEndTag(String tag) {
        endTag = tag;
    }

    public void setImportTag(String tag) {
        importTag = tag;
    }

    /**
	* Create a vector based on the InformationToken values given in
	* tokens.
	*/
    public void setTokens(InformationToken _tokens[]) {
        tokens = _tokens;
    }
}
