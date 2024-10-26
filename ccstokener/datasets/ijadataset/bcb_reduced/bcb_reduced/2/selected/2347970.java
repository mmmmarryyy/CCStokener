package eulergui.parser.n3.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import n3_project.exceptions.parser.ParsingException;
import net.sf.parser4j.parser.entity.parsenode.status.IParseNodeInErrorStatus;
import net.sf.parser4j.parser.entity.parsenode.status.impl.DefaultParseNodeInErrorStatus;
import eulergui.util.ReaderUtils;

/**
 * @author Jean-Marc Vanel jeanmarc.vanel@gmail.com
 *
 */
public class MissingPrefixesCompletion {

    /**
	 * @param parsingException
	 * @return
	 */
    public static String makePrefixDeclarationsWithPrefix_cc(ParsingException parsingException) {
        Set<String> missingPrefixes = getMissingPrefixes(parsingException);
        return makePrefixDeclarationsWithPrefix_cc(missingPrefixes);
    }

    /**
	 * @param parsingException
	 */
    private static Set<String> getMissingPrefixes(ParsingException parsingException) {
        Set<String> result = new HashSet<String>();
        Pattern patt = Pattern.compile(".*no prefix for \"(\\w+):\".*", Pattern.DOTALL);
        int lineNumber = 0;
        int columnNumber = 0;
        List<IParseNodeInErrorStatus> eErrorStatusList = parsingException.getN3ParseResult().getParseResult().getErrorStatusList();
        for (IParseNodeInErrorStatus iParseNodeInErrorStatus : eErrorStatusList) {
            lineNumber = iParseNodeInErrorStatus.getBeginLineNumber();
            columnNumber = iParseNodeInErrorStatus.getBeginColumnNumber();
            System.err.print("ResultManagement.displayOriginalSourceWithErrors(): " + lineNumber + ":" + columnNumber + " - " + iParseNodeInErrorStatus.getEndLineNumber() + ":" + iParseNodeInErrorStatus.getEndColumnNumber());
            if (iParseNodeInErrorStatus instanceof DefaultParseNodeInErrorStatus) {
                String errorMessage = ((DefaultParseNodeInErrorStatus) iParseNodeInErrorStatus).getErrorMessage();
                System.err.print(" " + errorMessage);
                Matcher m = patt.matcher(errorMessage);
                if (m.matches() && m.groupCount() > 0) {
                    String prefix = m.group(1);
                    result.add(prefix);
                }
            }
            System.err.println();
        }
        return result;
    }

    /** * @param missingPrefix missing N3 Prefix with or without the ": */
    public static String makePrefixDeclarationsWithPrefix_cc(String missingPrefix) {
        Set<String> set = new HashSet<String>();
        set.add(missingPrefix.replaceAll(": *$", ""));
        return makePrefixDeclarationsWithPrefix_cc(set);
    }

    /**
	 * @param missingPrefixes missing N3 Prefixes without the ":
	 * @return
	 */
    private static String makePrefixDeclarationsWithPrefix_cc(Set<String> missingPrefixes) {
        StringWriter sb = new StringWriter();
        for (Iterator<String> iterator = missingPrefixes.iterator(); iterator.hasNext(); ) {
            String prefix = (String) iterator.next();
            sb.append(prefix);
            if (iterator.hasNext()) {
                sb.append(',');
            }
        }
        String missingPrefixesForPrefix_cc = sb.toString();
        String prefixDeclarations = "";
        if (missingPrefixes.size() > 0) {
            try {
                String urlString = "http://prefix.cc/" + missingPrefixesForPrefix_cc + ".file.n3";
                URL url = new URL(urlString);
                URLConnection conn = url.openConnection();
                conn.setRequestProperty("accept", "application/rdf+n3, application/rdf-turtle, application/rdf-n3," + "text/rdf+n3");
                InputStream openStream = conn.getInputStream();
                StringWriter output = new StringWriter();
                ReaderUtils.copyReader("# From prefix.cc\n", new InputStreamReader(openStream), output);
                prefixDeclarations = output.toString();
                Logger.getLogger("prefix.cc").info("makePrefixDeclarationsWithPrefix_cc() : From prefix.cc:\n" + prefixDeclarations);
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return prefixDeclarations;
    }
}
