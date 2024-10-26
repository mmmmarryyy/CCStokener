package edu.cmu.cs.bungee.client.query;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.zip.InflaterInputStream;
import javax.swing.SwingUtilities;
import edu.cmu.cs.bungee.javaExtensions.MyResultSet;
import edu.cmu.cs.bungee.javaExtensions.Util;

final class ServletInterface {

    private static final boolean printOps = false;

    private final String host;

    private final String sessionID;

    private final String databaseDesc;

    final int facetCount;

    final int itemCount;

    final String itemDescriptionFields;

    final String label;

    final String doc;

    final boolean isEditable;

    private final MyResultSet initPerspectives;

    private final MyResultSet init;

    /**
	 * status of most recent servlet response
	 */
    private String status;

    private DescAndImage descAndImage;

    /**
	 * This caches answers for two functions: itemIndex: what is the itemOffset
	 * of item? offsetItems: what are the items for a range of offsets?
	 * 
	 * The answers are cached when calling itemIndex, itemIndexFromURL, and
	 * updateOnItems.
	 * 
	 */
    private ItemInfo itemInfo;

    private final class DescAndImage {

        DescAndImage(int _item, ResultSet _info) {
            item = _item;
            info = _info;
        }

        final int item;

        final ResultSet info;
    }

    private final class ItemInfo {

        final int item;

        final int minIndex;

        final ResultSet itemOffsets;

        final int itemIndex;

        ItemInfo(int _item, int _itemIndex, int _minIndex, ResultSet _itemOffsets) {
            item = _item;
            minIndex = _minIndex;
            itemOffsets = _itemOffsets;
            itemIndex = _itemIndex;
        }

        int maxIndex() {
            int _maxIndex = minIndex;
            if (itemOffsets != null) {
                try {
                    _maxIndex += MyResultSet.nRows(itemOffsets);
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
            return _maxIndex;
        }

        @Override
        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("<ItemInfo");
            if (itemIndex > 0) {
                buf.append(" onItems[").append(itemIndex).append("] = ").append(item);
            }
            if (itemOffsets != null) {
                String records = "";
                records = MyResultSet.valueOfDeep(itemOffsets, MyResultSet.INT, 5);
                buf.append(" range ").append(minIndex).append("-").append(maxIndex()).append("\n").append(records);
            }
            buf.append(">");
            return buf.toString();
        }
    }

    ServletInterface(String codeBase, String dbName) {
        System.out.print(codeBase + " " + dbName + " " + (new Date().toString()));
        host = codeBase;
        String[] args = { dbName };
        if (dbName == null || dbName.length() == 0) args = null;
        DataInputStream in = getStream("CONNECT", args);
        if (in != null) {
            sessionID = MyResultSet.readString(in);
            System.out.println(" session = '" + sessionID + "'");
            databaseDesc = MyResultSet.readString(in);
            facetCount = MyResultSet.readInt(in);
            itemCount = MyResultSet.readInt(in);
            itemDescriptionFields = MyResultSet.readString(in);
            label = MyResultSet.readString(in);
            doc = MyResultSet.readString(in);
            String isEditableVal = MyResultSet.readString(in);
            assert isEditableVal == null || isEditableVal.length() == 0 || isEditableVal.equalsIgnoreCase("N") || isEditableVal.equalsIgnoreCase("Y") : "Suspicious globals.isEditable value: " + isEditableVal;
            isEditable = "Y".equalsIgnoreCase(isEditableVal);
            initPerspectives = new MyResultSet(in, MyResultSet.STRING_STRING_STRING_INT_INT_INT_INT);
            init = new MyResultSet(in, MyResultSet.INT);
            closeNcatch(in, "CONNECT", args);
        } else {
            if (status == null) status = "Could not connect to " + Util.join(args);
            sessionID = null;
            label = null;
            itemDescriptionFields = null;
            itemCount = -1;
            isEditable = false;
            initPerspectives = null;
            init = null;
            facetCount = -1;
            doc = null;
            databaseDesc = null;
        }
    }

    void close() {
        dontGetStream("CLOSE", null);
    }

    String errorMessage() {
        return status;
    }

    void dontGetStream(String command, String[] args) {
        closeNcatch(getStream(command, args), command, args);
    }

    DataInputStream getStream(String command, String[] args) {
        long start = printOps ? new Date().getTime() : 0;
        DataInputStream in = null;
        HttpURLConnection conn = null;
        status = null;
        StringBuffer s = null;
        try {
            s = new StringBuffer();
            s.append("?command=").append(command);
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    if (args[i] != null) {
                        String arg = URLEncoder.encode(args[i], "UTF-8");
                        s.append("&arg").append(i + 1).append("=").append(arg);
                    }
                }
            }
            String actionString = flushUserActions();
            if (actionString != null) {
                assert actionString.length() > 0;
                String encodedActions = URLEncoder.encode(actionString, "UTF-8");
                assert encodedActions.length() > 0;
                if (encodedActions.length() > 0) s.append("&userActions=").append(encodedActions);
            }
            if (sessionID != null) s.append("&session=").append(sessionID);
            String url = s.toString();
            if (printOps) {
                System.out.println(URLDecoder.decode(url, "UTF-8"));
                if (SwingUtilities.isEventDispatchThread()) {
                    System.err.println("Calling ServletInterface in event dispatch thread! " + url);
                }
            }
            conn = (HttpURLConnection) (new URL(host + url)).openConnection();
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-length", "0");
            in = new DataInputStream(new InflaterInputStream(new BufferedInputStream(conn.getInputStream())));
        } catch (Throwable e) {
            if (conn != null) try {
                status = conn.getResponseMessage();
            } catch (IOException nested) {
                nested.printStackTrace();
            }
            if (status == null) status = e.toString();
        }
        if (status == null && conn != null) {
            try {
                status = conn.getResponseMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (status.equals("OK")) status = null;
        if (status != null) System.err.println("\ngetStream status: " + status + "\nin response to " + s + "\n");
        if (printOps) System.out.println(command + " took " + (new Date().getTime() - start) + "ms");
        return in;
    }

    String getString(String command, String[] args) {
        DataInputStream in = getStream(command, args);
        String result = MyResultSet.readString(in);
        closeNcatch(in, command, args);
        return result;
    }

    ResultSet getResultSet(String command, String[] args, List<Object> columnTypes) {
        DataInputStream in = getStream(command, args);
        ResultSet result = new MyResultSet(in, columnTypes);
        closeNcatch(in, command, args);
        return result;
    }

    void closeNcatch(DataInputStream s, String command, String[] args) {
        try {
            while (s.read() != -1) {
            }
            s.close();
        } catch (Throwable e) {
            Util.err("Error while closeNcatching: " + command + " " + args);
            e.printStackTrace();
        }
    }

    String getString(String command) {
        return getString(command, null);
    }

    String getString(String command, int arg) {
        String[] args = { Integer.toString(arg) };
        return getString(command, args);
    }

    ResultSet getResultSet(String command, List<Object> columnTypes, int arg1, boolean arg2) {
        String[] args = { Integer.toString(arg1), Boolean.toString(arg2) };
        return getResultSet(command, args, columnTypes);
    }

    ResultSet getResultSet(String command, List<Object> columnTypes, int arg1, boolean arg2, boolean arg3) {
        String[] args = { Integer.toString(arg1), Boolean.toString(arg2), Boolean.toString(arg3) };
        return getResultSet(command, args, columnTypes);
    }

    ResultSet getResultSet(String command, List<Object> columnTypes, int arg1) {
        String[] args = { Integer.toString(arg1) };
        return getResultSet(command, args, columnTypes);
    }

    ResultSet getResultSet(String command, List<Object> columnTypes, int arg1, int arg2) {
        String[] args = { Integer.toString(arg1), Integer.toString(arg2) };
        return getResultSet(command, args, columnTypes);
    }

    ResultSet getResultSet(String command, List<Object> columnTypes, int arg1, int arg2, int arg3) {
        String[] args = { Integer.toString(arg1), Integer.toString(arg2), Integer.toString(arg3) };
        return getResultSet(command, args, columnTypes);
    }

    ResultSet getResultSet(String command, List<Object> columnTypes, String arg1, int arg2) {
        String[] args = { arg1, Integer.toString(arg2) };
        return getResultSet(command, args, columnTypes);
    }

    ResultSet getResultSet(String command, List<Object> columnTypes, String arg1, int arg2, int arg3, int arg4) {
        String[] args = { arg1, Integer.toString(arg2), Integer.toString(arg3), Integer.toString(arg4) };
        return getResultSet(command, args, columnTypes);
    }

    ResultSet getResultSet(String command, List<Object> columnTypes, String arg1, String arg2) {
        String[] args = { arg1, arg2 };
        return getResultSet(command, args, columnTypes);
    }

    String aboutCollection() {
        return getString("ABOUT_COLLECTION");
    }

    String getItemURL(int item) {
        return getString("ITEM_URL", item);
    }

    ResultSet getCountsIgnoringFacet(String subQuery, int facetID) {
        return getResultSet("getCountsIgnoringFacet", MyResultSet.SINT_PINT, subQuery, facetID);
    }

    ResultSet getFilteredCounts(String perspectivesToAdd, String perspectivesToRemove) {
        return getResultSet("getFilteredCounts", MyResultSet.SINT_PINT, perspectivesToAdd, perspectivesToRemove);
    }

    ResultSet getFilteredCountTypes() {
        return getResultSet("getFilteredCountTypes", null, MyResultSet.SINT_PINT);
    }

    ResultSet initPerspectives() {
        return initPerspectives;
    }

    void decacheOffsets() {
        itemInfo = null;
    }

    int updateOnItems(String subQuery, int item, int table, int nNeighbors) {
        decacheOffsets();
        if (subQuery != null) {
            String[] args = { subQuery, Integer.toString(item), Integer.toString(table), Integer.toString(nNeighbors) };
            DataInputStream in = getStream("updateOnItems", args);
            int onCount = MyResultSet.readInt(in);
            if (onCount > 0 && nNeighbors > 1) {
                itemIndexInternal(in, item, nNeighbors);
            }
            closeNcatch(in, "updateOnItems", args);
            return onCount;
        } else {
            return -1;
        }
    }

    DataInputStream prefetch(Perspective facet, int type) {
        int facetID = facet.getID();
        String[] args = { Integer.toString(facetID), Integer.toString(type) };
        DataInputStream in = getStream("prefetch", args);
        return in;
    }

    ResultSet getLetterOffsets(Perspective facet, String prefix) {
        int facetID = facet.getID();
        String[] args = { Integer.toString(facetID), prefix };
        return getResultSet("getLetterOffsets", args, MyResultSet.STRING_SINT);
    }

    ResultSet init() {
        return init;
    }

    ResultSet offsetItems(int minOffset, int maxOffset, int table) {
        if (itemInfo != null && minOffset >= itemInfo.minIndex && maxOffset <= itemInfo.maxIndex()) {
            try {
                itemInfo.itemOffsets.absolute(minOffset - itemInfo.minIndex);
            } catch (SQLException e) {
                Util.err("Caching is messed up: " + itemInfo);
                e.printStackTrace();
            }
            return itemInfo.itemOffsets;
        }
        ResultSet result = getResultSet("offsetItems", MyResultSet.INT, minOffset, maxOffset, table);
        return result;
    }

    ResultSet[] getThumbs(String items, int imageW, int imageH, int quality) {
        String[] args = { items, Integer.toString(imageW), Integer.toString(imageH), Integer.toString(quality) };
        DataInputStream in = getStream("getThumbs", args);
        ResultSet[] result = new ResultSet[2];
        result[0] = new MyResultSet(in, MyResultSet.SINT_STRING_IMAGE_INT_INT);
        result[1] = new MyResultSet(in, MyResultSet.SNMINT_PINT);
        closeNcatch(in, "getThumbs", args);
        return result;
    }

    ResultSet getDescAndImage(int item, int imageW, int imageH, int quality) {
        String[] args = { Integer.toString(item), Integer.toString(imageW), Integer.toString(imageH), Integer.toString(quality) };
        DataInputStream in = getStream("getDescAndImage", args);
        descAndImage = new DescAndImage(item, new MyResultSet(in, MyResultSet.PINT_SINT_STRING_INT_INT_INT));
        ResultSet result = new MyResultSet(in, MyResultSet.STRING_IMAGE_INT_INT);
        closeNcatch(in, "getDescAndImage", args);
        return result;
    }

    ResultSet getFacetInfo(int facet, boolean isRestrictedData) {
        String[] args = { Integer.toString(facet), asBoolean(isRestrictedData) };
        DataInputStream in = getStream("getFacetInfo", args);
        ResultSet result = new MyResultSet(in, MyResultSet.PINT_SINT_STRING_INT_INT_INT);
        closeNcatch(in, "getFacetInfo", args);
        return result;
    }

    ResultSet getItemInfo(int item) {
        assert item == descAndImage.item;
        return descAndImage.info;
    }

    int itemIndex(int item, int table, int nNeighbors) {
        if (itemInfo != null && itemInfo.item == item) {
            return itemInfo.itemIndex;
        }
        String[] args = { Integer.toString(item), Integer.toString(table), Integer.toString(nNeighbors) };
        DataInputStream in = getStream("itemIndex", args);
        int result = itemIndexInternal(in, item, nNeighbors);
        closeNcatch(in, "itemIndex", args);
        return result;
    }

    int[] itemIndexFromURL(String URL, int table) {
        int nNeighbors = 0;
        String[] args = { URL, Integer.toString(table), Integer.toString(nNeighbors) };
        DataInputStream in = getStream("itemIndexFromURL", args);
        int[] result = new int[2];
        int item = MyResultSet.readInt(in);
        result[0] = item;
        result[1] = itemIndexInternal(in, item, nNeighbors);
        closeNcatch(in, "itemIndexFromURL", args);
        return result;
    }

    int itemIndexInternal(DataInputStream in, int item, int nNeighbors) {
        int minIndex = -1;
        ResultSet itemOffsets = null;
        int itemIndex = MyResultSet.readInt(in) - 1;
        if (nNeighbors > 1) {
            minIndex = MyResultSet.readInt(in);
            itemOffsets = new MyResultSet(in, MyResultSet.INT);
        }
        itemInfo = new ItemInfo(item, itemIndex, minIndex, itemOffsets);
        return itemIndex;
    }

    String[][] getDatabases() {
        String[] s = Util.splitSemicolon(databaseDesc);
        String[][] databases = new String[s.length][];
        for (int i = 0; i < s.length; i++) {
            databases[i] = Util.splitComma(s[i]);
        }
        return databases;
    }

    StringBuffer processedQueue = new StringBuffer();

    void printUserAction(String x) {
        assert x.length() > 0;
        if (processedQueue.length() > 0) processedQueue.append(";");
        processedQueue.append(x);
    }

    private String flushUserActions() {
        String result = null;
        if (processedQueue.length() > 0) {
            result = processedQueue.toString();
            processedQueue = new StringBuffer();
        }
        return result;
    }

    void reorderItems(int facetID) {
        String[] args = { Integer.toString(facetID) };
        dontGetStream("reorderItems", args);
        decacheOffsets();
    }

    void restrict() {
        dontGetStream("restrict", null);
    }

    ResultSet[] cluster(int maxClusters, int maxClusterSize, String facetRestriction, double p) {
        String[] args = { Integer.toString(maxClusters), Integer.toString(maxClusterSize), facetRestriction, Double.toString(p) };
        DataInputStream in = getStream("cluster", args);
        int nClusters = MyResultSet.readInt(in);
        ResultSet[] result = new ResultSet[nClusters];
        for (int i = 0; i < nClusters; i++) result[i] = new MyResultSet(in, MyResultSet.INT_PINT_STRING_INT_INT_INT_INT_DOUBLE_PINT_PINT);
        closeNcatch(in, "cluster", args);
        return result;
    }

    ResultSet addItemFacet(int facet, int item) {
        String[] args = { Integer.toString(facet), Integer.toString(item) };
        ResultSet result = getResultSet("addItemFacet", args, MyResultSet.SINT_INT_INT_INT_INT);
        return result;
    }

    ResultSet addItemsFacet(int facet, int table) {
        String[] args = { Integer.toString(facet), Integer.toString(table) };
        ResultSet result = getResultSet("addItemsFacet", args, MyResultSet.SINT_INT_INT_INT_INT);
        return result;
    }

    ResultSet removeItemsFacet(int facet, int table) {
        String[] args = { Integer.toString(facet), Integer.toString(table) };
        ResultSet result = getResultSet("removeItemsFacet", args, MyResultSet.SINT_INT_INT_INT_INT);
        return result;
    }

    ResultSet addChildFacet(int facet, String name) {
        String[] args = { Integer.toString(facet), name };
        ResultSet result = getResultSet("addChildFacet", args, MyResultSet.SINT_INT_INT_INT_INT);
        return result;
    }

    ResultSet removeItemFacet(int facet, int item) {
        String[] args = { Integer.toString(facet), Integer.toString(item) };
        ResultSet result = getResultSet("removeItemFacet", args, MyResultSet.SINT_INT_INT_INT_INT);
        return result;
    }

    ResultSet reparent(int parent, int child) {
        String[] args = { Integer.toString(parent), Integer.toString(child) };
        ResultSet result = getResultSet("reparent", args, MyResultSet.SINT_INT_INT_INT_INT);
        return result;
    }

    void writeback() {
        String[] args = {};
        dontGetStream("writeback", args);
    }

    void revert(String date) {
        String[] args = { date };
        dontGetStream("revert", args);
    }

    void rotate(int item, String theta) {
        String[] args = { Integer.toString(item), theta };
        dontGetStream("rotate", args);
    }

    void rename(int facetID, String newName) {
        String[] args = { Integer.toString(facetID), newName };
        dontGetStream("rename", args);
    }

    ResultSet getNames(String facets) {
        String[] args = { facets };
        ResultSet result = getResultSet("getNames", args, MyResultSet.STRING);
        return result;
    }

    void setItemDescription(int currentItem, String description) {
        String[] args = { Integer.toString(currentItem), description };
        dontGetStream("setItemDescription", args);
    }

    String[] opsSpec(String replay) {
        String[] args = { replay };
        MyResultSet result = (MyResultSet) getResultSet("opsSpec", args, MyResultSet.STRING);
        return (String[]) result.getValues(1);
    }

    String getSession() {
        return sessionID;
    }

    /**
	 * @param items
	 * @return [record_num, segment_id, start_offset, end_offset]
	 */
    ResultSet caremediaPlayArgs(String items) {
        String[] args = { items };
        ResultSet result = getResultSet("caremediaPlayArgs", args, MyResultSet.SNMINT_INT_INT);
        return result;
    }

    /**
	 * @param segments
	 * @return [segment_id, record_num]
	 */
    public ResultSet caremediaGetItems(int[] segments) {
        String[] args = { Util.join(segments) };
        ResultSet result = getResultSet("caremediaGetItems", args, MyResultSet.SINT);
        try {
            Util.print("caremediaGetItems " + Util.valueOfDeep(segments) + " " + MyResultSet.nRows(result));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public ResultSet[] onCountMatrix(String facetsOfInterest, String candidates, boolean isRestrictedData, boolean needBaseCounts) {
        String[] args = { facetsOfInterest, candidates, asBoolean(isRestrictedData), asBoolean(needBaseCounts) };
        DataInputStream in = getStream("onCountMatrix", args);
        boolean needCandidateCounts = candidates.length() > 0;
        assert needBaseCounts || needCandidateCounts;
        ResultSet[] result = { needBaseCounts ? new MyResultSet(in, MyResultSet.SNMINT_INT_INT) : null, needCandidateCounts ? new MyResultSet(in, MyResultSet.SNMINT_INT_INT) : null };
        closeNcatch(in, "onCountMatrix", args);
        return result;
    }

    private String asBoolean(boolean value) {
        return value ? "1" : "0";
    }

    ResultSet topMutInf(String facetIDs, int baseTable, int maxCandidates) {
        String[] args = { facetIDs, Integer.toString(maxCandidates), Integer.toString(baseTable) };
        return getResultSet("topCandidates", args, MyResultSet.INT);
    }
}
