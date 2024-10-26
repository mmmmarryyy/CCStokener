package net.sf.fraglets.cca;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import org.apache.log4j.Logger;

/**
 * Perform modification check based on a plain history file.
 * 
 * @author  Klaus Rennecke
 * @version $Revision: 1.15 $
 */
public class CVSHistory implements SourceControl {

    /** The properties set for ANT. */
    private Hashtable properties = new Hashtable();

    /** The property name for the property to set on modifications. */
    private String onModifiedName;

    /** The property name for the property to set on deletions. */
    private String onDeletedName;

    /** The file name of the history file to read. */
    private String historyFileName;

    /** The URL where to fetch the history. */
    private String historyUrl;

    /** The URL of the viewcvs gateway. */
    private String viewcvsUrl;

    /** The default encoding for log entries. */
    private String defaultEncoding;

    /** The module name to check for. */
    private List modules = new ArrayList();

    /** The committers with their personal charset setting. */
    private List committers;

    /** Map from committer name to charset. */
    private HashMap encodings;

    /** Logger for this class. */
    private static Logger log = Logger.getLogger(CVSHistory.class);

    /** Flag indicating that a deletion was seen. */
    private boolean deletionSeen;

    /** Minimum line length for a valid history entry. */
    private static final int MINIMUM_HISTORY_LINE = 10;

    /** Time conversion factor from un*x time to java time. */
    private static final int TIME_MULTIPLIER = 1000;

    /** End index of the timestamp. */
    private static final int TIMESTAMP_END = 9;

    /** Radix of the history timestamp. */
    private static final int TIMESTAMP_RADIX = 16;

    /**
     * Get the modifications recorded in the history file.
     * @param lastBuild the last build
     * @param now the check time
     */
    public List getModifications(Date lastBuild, Date now) {
        List result = new ArrayList();
        try {
            Reader fin;
            if (historyFileName != null) {
                fin = new FileReader(historyFileName);
            } else {
                if (defaultEncoding != null) {
                    fin = new InputStreamReader(new URL(historyUrl).openStream(), defaultEncoding);
                } else {
                    fin = new InputStreamReader(new URL(historyUrl).openStream());
                }
            }
            try {
                BufferedReader in = new BufferedReader(fin);
                String line;
                while ((line = in.readLine()) != null) {
                    parseModification(line, result, lastBuild, now);
                }
                if (onModifiedName != null && result.size() > 0) {
                    properties.put(onModifiedName, "true");
                }
                if (onDeletedName != null && deletionSeen) {
                    properties.put(onDeletedName, "true");
                }
            } finally {
                fin.close();
            }
        } catch (IOException e) {
            log.error(Messages.getString("CVSHistory.Failed_to_read_history_file"), e);
        }
        return result;
    }

    /**
     * @param line the line to parse
     * @param result the list where to put results
     * @param lastBuild lower bound
     * @param now upper bound
     * @param cvslog the CVS log map, updated as necessary
     */
    private void parseModification(String line, List result, Date lastBuild, Date now) throws MalformedURLException, UnsupportedEncodingException, IOException {
        if (line.length() >= MINIMUM_HISTORY_LINE) {
            String type;
            switch(line.charAt(0)) {
                case 'A':
                    type = "added";
                    break;
                case 'M':
                    type = "modified";
                    break;
                case 'R':
                    type = "deleted";
                    break;
                default:
                    return;
            }
            String timestamp = line.substring(1, TIMESTAMP_END);
            long time = Long.parseLong(timestamp, TIMESTAMP_RADIX) * TIME_MULTIPLIER;
            if (time < lastBuild.getTime()) {
                return;
            } else if (time > now.getTime()) {
                return;
            }
            try {
                StringTokenizer tok = new StringTokenizer(line.substring(TIMESTAMP_END + 1), "|", true);
                String userName = tok.nextToken();
                tok.nextToken();
                tok.nextToken();
                tok.nextToken();
                String folderName = tok.nextToken();
                Module module = matchModule(folderName);
                if (module == null) {
                    return;
                }
                tok.nextToken();
                String revision = tok.nextToken();
                tok.nextToken();
                String fileName = tok.nextToken();
                String comment = fetchLog(folderName + '/' + fileName, revision, module.getBranch(), getEncoding(userName));
                if (comment == null) {
                    return;
                }
                Modification modification = new Modification();
                modification.type = type;
                modification.modifiedTime = new Date(time);
                modification.userName = userName;
                modification.folderName = folderName;
                modification.fileName = fileName;
                modification.revision = revision;
                modification.comment = comment;
                if (type.equals("deleted")) {
                    deletionSeen = true;
                }
                result.add(modification);
            } catch (NoSuchElementException e) {
                log.warn(Messages.getString("CVSHistory.Invalid_history_format") + line + "\"");
            }
        }
    }

    /**
     * Fetch the log entry for the given file revision, returning its
     * commit message. If the revision does not exist on the given
     * branch, null is returned.
     * 
     * @param fileName the file name to fetch.
     * @param revision a revision to fetch.
     * @param branch the branch to select, or null.
     * @param cvslog the CVS log map, updated as necessary.
     * @return the commit message, or null.
     */
    private String fetchLog(String fileName, String revision, String branch, String encoding) throws MalformedURLException, UnsupportedEncodingException, IOException {
        if (viewcvsUrl == null) {
            return "";
        }
        InputStream in;
        try {
            if (branch != null) {
                in = new URL(viewcvsUrl + urlquote(fileName, false) + "?only_with_tag=" + urlquote(branch, true)).openStream();
            } else {
                in = new URL(viewcvsUrl + urlquote(fileName, false)).openStream();
            }
        } catch (IOException e) {
            log.warn(Messages.getString("CVSHistory.logNotFound1") + fileName + Messages.getString("CVSHistory.logNotFound2") + branch, e);
            return null;
        }
        if (!(in instanceof BufferedInputStream)) {
            in = new BufferedInputStream(in);
        }
        try {
            for (; ; ) {
                skipTag(in, "hr");
                if (!"Revision".equals(readToken(in))) {
                    continue;
                }
                if (!revision.equals(readToken(in))) {
                    continue;
                }
                return readBlock(in, "pre", encoding);
            }
        } catch (EOFException e) {
        } finally {
            in.close();
        }
        return null;
    }

    private static void skipTo(InputStream in, String mark) throws IOException {
        int end = mark.length();
        int scan = 0;
        while (scan < end) {
            int c = in.read();
            if (c == mark.charAt(scan)) {
                scan += 1;
            } else if (c == -1) {
                throw new EOFException();
            } else {
                scan = 0;
            }
        }
    }

    private static boolean matchText(InputStream in, String text) throws IOException {
        int end = text.length();
        int scan = 0;
        in.mark(end);
        while (scan < end) {
            int c = in.read();
            if (c == text.charAt(scan)) {
                scan += 1;
            } else if (c == -1) {
                throw new EOFException();
            } else {
                in.reset();
                return false;
            }
        }
        return true;
    }

    private static String readToken(InputStream in) throws IOException {
        StringBuffer buffer = new StringBuffer();
        for (; ; ) {
            in.mark(1);
            int c = in.read();
            if (c == -1) {
                break;
            } else if (Character.isWhitespace((char) c)) {
                if (buffer.length() > 0) {
                    break;
                }
            } else if (c == '<') {
                if (buffer.length() > 0) {
                    break;
                }
                skipTo(in, ">");
            } else {
                buffer.append((char) c);
            }
        }
        if (buffer.length() > 0) {
            in.reset();
            return buffer.toString();
        } else {
            throw new EOFException();
        }
    }

    private static String readBlock(InputStream in, String tagName, String encoding) throws IOException {
        skipTag(in, tagName);
        byte[] buffer = new byte[1024];
        int pos = 0;
        for (; ; ) {
            int c = in.read();
            if (c == -1) {
                break;
            } else if (c == '<') {
                if (matchText(in, "/" + tagName)) {
                    skipTo(in, ">");
                    break;
                }
            }
            try {
                buffer[pos] = (byte) c;
            } catch (ArrayIndexOutOfBoundsException e) {
                int more = buffer.length * 2;
                byte[] grow = new byte[more];
                System.arraycopy(buffer, 0, grow, 0, buffer.length);
                buffer = grow;
                buffer[pos] = (byte) c;
            }
            pos += 1;
        }
        if (encoding != null) {
            return new String(buffer, 0, pos, encoding);
        } else {
            return new String(buffer, 0, pos);
        }
    }

    private static void skipWhite(InputStream in) throws IOException {
        do {
            in.mark(1);
        } while (Character.isWhitespace((char) in.read()));
        in.reset();
    }

    private static void skipTag(InputStream in, String tagName) throws IOException {
        String found;
        do {
            found = readTag(in, "<");
            skipTo(in, ">");
        } while (!tagName.equals(found));
    }

    private static String readTag(InputStream in, String start) throws IOException {
        skipTo(in, start);
        skipWhite(in);
        StringBuffer buffer = new StringBuffer();
        for (; ; ) {
            in.mark(1);
            int c = in.read();
            if (c == -1) {
                break;
            } else if (Character.isLetter((char) c)) {
                buffer.append((char) c);
            } else {
                break;
            }
        }
        in.reset();
        return buffer.toString();
    }

    /**
     * Quote the given string for use in a URL.
     * @param string a string.
     * @return the quoted string.
     */
    private String urlquote(String string, boolean query) {
        byte[] data;
        try {
            data = string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.toString());
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            char c = (char) (data[i] & 0xff);
            if (c == '/' || c == '-' || c == '_' || c == '.' || c == '*' || (c < 127 && Character.isLetterOrDigit(c))) {
                buffer.append(c);
            } else if (query && c == ' ') {
                buffer.append('+');
            } else {
                buffer.append('%');
                if (c < 16) {
                    buffer.append('0');
                }
                buffer.append(Integer.toHexString(c));
            }
        }
        return buffer.toString();
    }

    /**
     * Check of the given folder name is in the set of modules configured.
     * @param folderName the folder name to check.
     * @return the module for the given folder, or null.
     */
    public Module matchModule(String folderName) {
        int scan = modules.size();
        if (scan == 0) {
            return Module.DEFAULT_MODULE;
        }
        while (--scan >= 0) {
            Module module = (Module) modules.get(scan);
            String moduleName = module.getName();
            if (!folderName.startsWith(moduleName)) {
                continue;
            } else if (folderName.length() > moduleName.length() && folderName.charAt(moduleName.length()) != '/') {
                continue;
            } else {
                return module;
            }
        }
        return null;
    }

    /**
     * @see net.sourceforge.cruisecontrol.SourceControl#validate()
     */
    public void validate() throws CruiseControlException {
        if (historyFileName == null) {
            if (historyUrl == null) {
                throw new CruiseControlException(Messages.getString("CVSHistory.history_required"));
            } else {
                try {
                    new URL(historyUrl).toExternalForm();
                } catch (MalformedURLException e) {
                    throw new CruiseControlException(Messages.getString("CVSHistory.Malformed_historyurl"), e);
                }
            }
        } else if (historyUrl != null) {
            throw new CruiseControlException(Messages.getString("CVSHistory.Either_historyfilename_or_historyurl"));
        } else if (!new File(historyFileName).isFile()) {
            log.warn(Messages.getString("CVSHistory.History_file") + historyFileName + Messages.getString("CVSHistory.does_not_exist"));
        }
        if (viewcvsUrl != null) {
            try {
                new URL(viewcvsUrl).toExternalForm();
            } catch (MalformedURLException e) {
                throw new CruiseControlException(Messages.getString("CVSHistory.Malformed_viewcvsurl"), e);
            }
        }
        if (committers != null) {
            int scan = committers.size();
            while (--scan >= 0) {
                Committer committer = (Committer) committers.get(scan);
                try {
                    committer.getName().getBytes(committer.getEncoding());
                } catch (UnsupportedEncodingException e) {
                    throw new CruiseControlException(Messages.getString("CVSHistory.Unsupported_encoding"), e);
                } catch (NullPointerException e) {
                    throw new CruiseControlException(Messages.getString("CVSHistory.Missing_committer_name"), e);
                }
            }
        }
        if (defaultEncoding != null) {
            try {
                defaultEncoding.getBytes(defaultEncoding);
            } catch (UnsupportedEncodingException e) {
                throw new CruiseControlException(Messages.getString("CVSHistory.Unsupported_encoding"), e);
            }
        }
    }

    /**
     * @see net.sourceforge.cruisecontrol.SourceControl#getProperties()
     */
    public Hashtable getProperties() {
        return properties;
    }

    /**
     * Set the property name for the property to set on modification.
     * @see net.sourceforge.cruisecontrol.SourceControl#setProperty(java.lang.String)
     */
    public void setProperty(String property) {
        onModifiedName = property;
    }

    /**
     * Set the property name for the property to set on deletions.
     * @see net.sourceforge.cruisecontrol.SourceControl#setPropertyOnDelete(java.lang.String)
     */
    public void setPropertyOnDelete(String property) {
        onDeletedName = property;
    }

    /**
     * @return the history file name
     */
    public String getHistoryFileName() {
        return historyFileName;
    }

    /**
     * @param string the new history file name
     */
    public void setHistoryFileName(String string) {
        historyFileName = string;
    }

    /**
     * @return the history url
     */
    public String getHistoryUrl() {
        return historyUrl;
    }

    /**
     * @param string the new history url
     */
    public void setHistoryUrl(String string) {
        historyUrl = string;
    }

    /**
     * @return the viewcvs URL.
     */
    public String getViewcvsUrl() {
        return viewcvsUrl;
    }

    /**
     * @param string the new viewcvs URL.
     */
    public void setViewcvsUrl(String string) {
        if (string == null || string.endsWith("/")) {
            viewcvsUrl = string;
        } else {
            viewcvsUrl = string + '/';
        }
    }

    /**
     * @param string the new module name
     */
    public void setModule(String string) {
        createModule().setName(string);
    }

    /**
     * @param encoding the default charset encoding.
     */
    public void setEncoding(String encoding) {
        this.defaultEncoding = encoding;
    }

    /**
     * Create a new sub-element for a module.
     * @return the new module sub-element.
     */
    public Module createModule() {
        Module result = new Module();
        modules.add(result);
        return result;
    }

    /**
     * Create a new sub-element for a committer.
     * @return the new committer sub-element.
     */
    public synchronized Committer createCommitter() {
        if (committers == null) {
            committers = new ArrayList();
        }
        Committer result = new Committer();
        committers.add(result);
        encodings = null;
        return result;
    }

    private synchronized String getEncoding(String committerName) {
        if (committerName == null || committers == null) {
            return defaultEncoding;
        }
        if (encodings == null) {
            encodings = new HashMap();
            int scan = committers.size();
            while (--scan >= 0) {
                Committer committer = (Committer) committers.get(scan);
                encodings.put(committer.getName(), committer.getEncoding());
            }
        }
        String result = (String) encodings.get(committerName);
        if (result == null) {
            return defaultEncoding;
        } else {
            return result;
        }
    }

    /**
     * Bean implementation for the module sub-element.
     * @since 01.03.2004
     * @author Klaus Rennecke
     * @version $Revision: 1.15 $
     */
    public static class Module {

        public static final Module DEFAULT_MODULE = new Module();

        /** The module name. */
        private String name;

        /** The branch name. */
        private String branch;

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param string the new name
         */
        public void setName(String string) {
            name = string;
        }

        /**
         * @return the branch.
         */
        public String getBranch() {
            return branch;
        }

        /**
         * @param string the new branch.
         */
        public void setBranch(String string) {
            branch = string;
        }
    }

    public static class Committer {

        private String name;

        private String encoding;

        public String getEncoding() {
            return encoding;
        }

        public String getName() {
            return name;
        }

        public void setEncoding(String string) {
            encoding = string;
        }

        public void setName(String string) {
            name = string;
        }
    }
}
