package pearls;

import pearls.*;
import java.util.*;
import java.io.*;
import java.net.*;

/***************************************************************************

    <p><i>ScriptureViewerModel:</i> Makes all data fetches to the 
    scripture display file and performs the searches for the ScriptureViewer 
    application -- <b>includes network indirection</b>.</p>


    <p><i>Client/Server Boundary</i></p>

    <p><i>Launch Directions:</i> This application can be launched
    standalone as an application or it can be launched as a client to 
    make calls to a server for data fetches. </p> 

    <p><i>Boundary:</i> The boundary between the client side of the
    application and the server side of the application is in each model
    object.  Models know how to fetch data directly from a file system or 
    fetch data through a web server to another instance of itself running 
    as a CGI behind the web server.</p>

    * <pre>
    *    Launched Standalone:
    *    --------------------
    *  
    *    +----------+                
    *    |          |    +-------+
    *    |          |    |       |     Data
    *    |   View   |    | Model |     on a  
    *    |          |    |       |  File System
    *    |          |    +-------+
    *    +----------+                
    *                  client/server
    *  
    *    ---------------------------------------------------------------------
    *    when getFile() is called...
    *  
    *                     getFile ("kjv.txt")
    *                     return (byte[] file)
    *  
    *  
    *    ---------------------------------------------------------------------
    *    when getChapter() is called...
    *  
    *                     getChapter ("col3:3:2:(kjv)")
    *                    [getFile ("kjv.txt")]
    *                     findVerseOffset("col3:3:2(kjv)")
    *                     return (int iImageOffset)
    *                     getChapter (int iImageOffset)
    *                     return (PBuffer chapter)
    *  
    *  
    *    ---------------------------------------------------------------------
    *    when the class is instantiated...
    *  
    *                     getBookOffsets()
    *  
    *  
    *    ---------------------------------------------------------------------
    *    when showRef() is called...
    *  
    *      showRef ("col3:5:0(*)")
    *  
    *                     getChapter ("col3:1:2(kjv)") <-- also sets curr posn
    *                     return (byte[] chapter)
    *  
    *      parse data
    *  
    *                     static parseVerse (byte[], offset)
    *                     return (int[][] inflection offset&length)
    *  
    *      loading textPane
    *      varying fonts
    *  
    *  
    *    ---------------------------------------------------------------------
    *    when showNextChapter() is called...
    *  
    *                     getChapter ("col3:1:2(kjv)") <-- also sets curr posn
    *                     return (byte[] chapter)
    *                     getNextChapter ()
    *                     return (byte[] chapter)
    *  
    *  
    *    ---------------------------------------------------------------------
    *    when showPrevChapter() is called...
    *  
    *                     getChapter ("col3:1:2(kjv)") <-- also sets curr posn
    *                     return (byte[] chapter)
    *                     getPrevChapter ()
    *                     return (byte[] chapter)
    *  
    *  
    *    ---------------------------------------------------------------------
    *    when showNextVerse() is called...
    *  
    *                     getChapter ("col3:1:2(kjv)") <-- also sets curr posn
    *                     return (byte[] chapter)
    *                     setLocation ("col3:2:0(*)")
    *                   
    *      incr offset to 
    *        next verse
    *      parse thru data 
    *        of the new verse
    *  
    *                     static parseVerse (byte[], offset)
    *                     return (int[][] inflection offset&length)
    *  
    *      loading textPane
    *      varying fonts
    *  
    *  
    *  
    * ------------------------------------------------------------------------
    *  
    *    Launched as a Client:
    *    ---------------------
    *  
    *    +----------+                    +------+
    *    |          |    +-------+       |      |    +-------+
    *    |          |    |       |       | Web  |    |       |     Data
    *    |   View   |----| Model |- - - -|Server|----| Model |     on a  
    *    |          |    |       |       |      |    |       |  File System
    *    |          |    +-------+       |      |    +-------+
    *    +----------+                    +------+
    *                      client                      server
    *  
    *                      fetch() - - - - - - - - - - get()
    *  
    *  
    *    ---------------------------------------------------------------------
    *    when getFile() is called...
    *  
    *                     getFile ("kjv.txt") 
    *                     fetch ("getFile kjv.txt")
    *  
    *                                                 get ("getFile kjv.txt")
    *                                                 getFile ("kjv.txt")
    *                                                 return (byte[] file)
    *  
    *  
    *    ---------------------------------------------------------------------
    *    when getChapter() is called...
    *  
    *                     getChapter ("col3:3:2(kjv)")
    *                     fetch ("getChapter col3:3:2(kjv)")
    *  
    *                                                 get ("getChapter col3:3:2(kjv)")
    *                                                 getChapter ("col3:3:2:(kjv)")
    *                                                [getFile ("kjv.txt")]
    *                                                 findVerseOffset("col3:3:2(kjv)")
    *                                                 return (int iImageOffset)
    *                                                 getChapter (int iImageOffset)
    *                                                 return (PBuffer chapter)
    *                                                 get() copies array over wire
    *
    *                     fetch() creates PBuffer
    *                       with new byte[]
    *                     return (PBuffer chapter)
    *  
    *  
    *    ---------------------------------------------------------------------
    *    when this class is instantiated...
    *  
    *                     url=urlIn                   getBookOffsets()
    *  
    *  
    *    ---------------------------------------------------------------------
    *    when showRef() is called...
    *  
    *      showRef ("col3:5:0(*)")
    *  
    *                     getChapter ("col3:1:2(kjv)")
    *                     fetch ("getChapter col3:1:2(kjv)")
    *  
    *                                                 get ("getChapter col3:1:2(kjv)")
    *                                                 getChapter ("col3:1:2(kjv)")
    *                                                 return (byte[] chapter)
    *  
    *      parse thru data
    *  
    *                     static parseVerse (byte[], offset)
    *                     return (int[][] inflection offset&length)
    *  
    *      loading textPane
    *      varying fonts and colors
    *  
    *    ---------------------------------------------------------------------
    *  
    * </pre>

    <p><i>User State:</i> A PUserState object is an object that holds all
    state related to a specific user of the model.  It holds items like IP of
    user, current image offset, current location, time last accessed, etc.</p>


    <p><i>Development Environment</i></p>

      <li>Compiled under JDK 1.30</li>

    <p><i>History</i></p>

    * <pre>
    *    $Log: ScriptureViewerModel.java,v $
    *    Revision 1.12  2001/09/15 00:57:41  noelenete
    *    Added the WEB version.
    *
    *    Revision 1.11  2001/09/02 04:48:29  noelenete
    *    Got the Original view and Gk & Hb models working.
    *
    *    Revision 1.10  2001/07/19 17:56:59  noelenete
    *    Added code to integrate RSV & DBY
    *
    *    Revision 1.9  2001/03/19 00:01:06  noelenete
    *    Renamed WordStudyView to LexView.
    *
    *    Revision 1.8  2001/01/28 17:42:09  noelenete
    *    Fixed Greek scan bug.
    *
    *    Revision 1.7  2001/01/21 01:58:12  noelenete
    *    First version of Greek searching.
    *
    *    Revision 1.6  2001/01/20 06:41:23  noelenete
    *    Added WordStudyView and updated GenApplicationHere.
    *
    *    Revision 1.5  2001/01/18 18:56:53  noelenete
    *    One step closer to stable Greek.
    *
    *    Revision 1.4  2001/01/14 00:13:37  noelenete
    *    Created a new Revision class.
    *
    *    Revision 1.3  2000/12/30 01:36:56  noelenete
    *    First display of Greek characters.
    *
    *    Revision 1.2  2000/11/23 00:19:22  noelenete
    *    Source code format.
    *
    *    Revision 1.1.1.1  2000/07/08 23:22:47  noelenete
    *    Initial import of PEARLS code into SourceForge.
    *
    *    Revision 1.0  2000/07/08 09:21:00  NoelEnete
    *    Created the class.
    * </pre>

 ***************************************************************************/
public class ScriptureViewerModel extends Dripper {

    public static final String msVer = "@(#) $Id: ScriptureViewerModel.java,v 1.12 2001/09/15 00:57:41 noelenete Exp $";

    public static final int MAX_RESULTS = 1000;

    public static final int NUM_VERSES_IN_BIBLE = 31102;

    public static final int MODEL_INIT_EVENT = 1;

    public static final int MODEL_UPDATE_EVENT = 2;

    protected static ScriptureViewerModel[] maModel = new ScriptureViewerModel[PLocation.NUM_VERSIONS];

    protected int miVersion;

    protected Vector vModelListeners;

    protected byte[] abScriptureData;

    protected int[] aiMatchStartOffset;

    protected int iNumMatches;

    protected URL urlBase;

    protected byte[] abBuffer;

    /**   Needed for GreekScriptureViewerModel subclass.
 *
 */
    protected ScriptureViewerModel() {
        init();
    }

    /**   Use this constructor when the application was launched from a 
 *    command line as an application.  Other classes should use a form
 *    of getModel() to get an instance of this class so that only one
 *    instance is created for each scripture image file.
 *
 */
    protected ScriptureViewerModel(int iVersionIn) {
        miVersion = iVersionIn;
        init();
    }

    /**   Use this constructor when the application was launched from a web
 *    browser as an applet.  Other classes should use a form of getModel() 
 *    to get an instance of this class so that only one instance is created 
 *    for each scripture image file
 *
 */
    protected ScriptureViewerModel(int iVersionIn, URL urlIn) {
        miVersion = iVersionIn;
        urlBase = urlIn;
        init();
    }

    /**
 */
    private void init() {
        abBuffer = new byte[16384];
        vModelListeners = new Vector();
        if (urlBase == null) {
            getScriptureImage();
            aiMatchStartOffset = new int[MAX_RESULTS];
            iNumMatches = 0;
        }
    }

    /**
 */
    public static void main(String[] args) {
        int i;
        URL url;
        String s;
        byte[] ab;
        PBuffer pbuffer;
        PLocation location;
        PLocationCollection locations;
        ScriptureViewerModel x;
        x = ScriptureViewerModel.getModel(PLocation.VERSION_KJV);
        ab = x.getFile("index.html");
        System.out.println("Client fetch:\n*-------------------------------------*");
        for (i = 0; i < ab.length; i++) {
            System.out.print((char) ab[i]);
        }
        System.out.println("*-------------------------------------*");
        location = new PLocation("gen1:2:0(kjv)");
        i = x.findVerseOffset(location, x.abScriptureData);
        System.out.println(location + " offset=" + i + " (should be 86)");
        System.out.println("*-------------------------------------*");
        s = "john3:16:0(kjv)";
        location = new PLocation(s);
        i = x.findVerseOffset(location, x.abScriptureData);
        pbuffer = x.getChapter(i);
        System.out.println("chapter for " + s + " is " + pbuffer);
        System.out.println(new String(pbuffer.getByteArray(), pbuffer.getOffset(), pbuffer.getLength()));
        System.out.println("*-------------------------------------*");
    }

    /**   Add a view (or other object) which needs to be notified of
 *    state changes in the model.
 *
 */
    public void addModelListener(PModelListener listenerIn) {
        vModelListeners.addElement(listenerIn);
    }

    /**   Compares two byte arrays for the equality of data over the range
 *    specified.  Since the bytes are compared by value, this can be used
 *    as a case sensitive compare.
 *
 */
    public boolean compare(byte[] abSearchIn, byte[] abIn, int iOffsetIn) {
        int i;
        if (iOffsetIn + abSearchIn.length >= abIn.length) {
            return (false);
        }
        for (i = 0; i < abSearchIn.length; i++) {
            if (abSearchIn[i] != abIn[iOffsetIn + i]) {
                return (false);
            }
        }
        return (true);
    }

    /**   Compares two byte arrays for the case insensitive equality of character 
 *    data over the range specified.  The abSearchReverseCaseIn byte array is
 *    the same size as the abSearchIn array but each character's case has been
 *    replaced with the opposite case -- upper case replaced with lower case 
 *    and lower case replaced with upper case.
 *
 */
    public boolean compareIgnoreCase(byte[] abSearchIn, byte[] abSearchReverseCaseIn, byte[] abIn, int iOffsetIn) {
        int i;
        int iOffset;
        for (i = 0; i < abSearchIn.length; i++) {
            iOffset = iOffsetIn + i;
            if ((abSearchIn[i] != abIn[iOffset]) && (abSearchReverseCaseIn[i] != abIn[iOffset])) {
                return (false);
            }
        }
        return (true);
    }

    /**   Call this method to dispatch an event to all registered
 *    listeners.
 *
 */
    protected void dispatchModelEvent(int iEventIDIn, Object objArgIn) {
        int i;
        PModelListener listener;
        i = vModelListeners.size() - 1;
        while (i >= 0) {
            listener = (PModelListener) vModelListeners.elementAt(i--);
            listener.handleModelEvent(iEventIDIn, objArgIn);
        }
    }

    /**   This method constructs an HTTP GET request using the past request string
 *    and passes the result back to the requester.  The ultimate method this
 *    ends up invoking is the get() method of this class in an instance of this
 *    class that is running behind the web server.  
 *
 *    If there is a problem, this returns null.
 *
 */
    private PBuffer fetch(String sReqIn) {
        URL url;
        byte[] ab;
        int iBytesRead;
        InputStream is;
        String sUrlEncodedReq;
        ByteArrayOutputStream baos;
        try {
            sUrlEncodedReq = SConnection.stringToHttp("/pearls.ScriptureViewerModel?" + sReqIn);
            url = new URL(urlBase, sUrlEncodedReq);
            is = url.openStream();
            baos = new ByteArrayOutputStream();
            synchronized (abBuffer) {
                while ((iBytesRead = is.read(abBuffer)) >= 0) {
                    baos.write(abBuffer, 0, iBytesRead);
                }
            }
            is.close();
            ab = baos.toByteArray();
            return (new PBuffer(ab, 0, ab.length));
        } catch (Exception e) {
            System.out.println("Error fetching " + sReqIn + ": " + e);
        }
        return (null);
    }

    /**   Returns the offset in the memory image of the Bible text file
 *    for the 1st word of the verse that is identified in the location
 *    -- includes <b>network indirection</b>.  Even if the location 
 *    specifies a word number in the verse, this does not return the 
 *    offset of that word.  It returns the offset of the 1st word of 
 *    the verse.
 *
 *    On error, this returns -1.
 *
 */
    public int findVerseOffset(PLocation locationIn, byte[] abSearchBibleIn) {
        int i;
        int iPtr;
        String s;
        PBuffer pbuffer;
        int iVerse;
        byte[] abSearch;
        try {
            if (urlBase != null) {
                pbuffer = fetch("findVerseOffset " + locationIn.toFullyQualifiedString());
                return (pbuffer.toInt());
            }
        } catch (Exception e) {
            System.out.println("Error findVerseOffset(): " + e);
            return (-1);
        }
        abSearch = locationIn.toSearchString().getBytes();
        iVerse = 0;
        iPtr = 0;
        for (iVerse = 0; true; iVerse++) {
            while (abSearchBibleIn[iPtr] != '[') {
                iPtr++;
                if (iPtr >= abSearchBibleIn.length) break;
            }
            iPtr++;
            if (iPtr >= abSearchBibleIn.length) break;
            if (compare(abSearch, abSearchBibleIn, iPtr)) {
                while (abSearchBibleIn[iPtr] != ']') {
                    iPtr++;
                }
                iPtr += 3;
                return (iPtr);
            }
        }
        return (-1);
    }

    /**   This is the method that the fetch() method is ultimately calling.
 *    The fetch() method constructs an HTTP GET request that gets sent to
 *    a web server.  A second instance of this class is available behind
 *    the web server to receive this call.  Any CGI parameters are passed 
 *    in the array of strings parameter.
 *
 */
    public void get(SConnection connIn, String[] asArgsIn) {
        int i;
        int iSize;
        byte[] ab = new byte[0];
        String s;
        String sReq;
        PLocation location;
        DataOutputStream dos;
        ByteArrayOutputStream baos;
        PLocationCollection locations;
        sReq = SConnection.httpToString(asArgsIn[0]);
        if (sReq.startsWith("getFile")) {
            ab = getFile(sReq.substring(8));
        } else if (sReq.startsWith("findVerseOffset")) {
            location = new PLocation(sReq.substring(16));
            ab = getScriptureImage();
            if (ab == null) {
                ab = new byte[0];
            } else {
                i = findVerseOffset(location, ab);
                ab = String.valueOf(i).getBytes();
            }
        } else if (sReq.startsWith("search")) {
            s = sReq.substring(7);
            ab = getScriptureImage();
            if (ab == null) {
                ab = new byte[0];
            } else {
                try {
                    locations = search(s);
                    iSize = locations.size();
                    baos = new ByteArrayOutputStream();
                    dos = new DataOutputStream(baos);
                    dos.writeInt(iSize);
                    for (i = 0; i < iSize; i++) {
                        dos.writeInt(locations.imageOffsetAt(i));
                    }
                    for (i = 0; i < iSize; i++) {
                        s = locations.searchStringAt(i);
                        dos.writeShort((short) s.length());
                        dos.writeBytes(s);
                    }
                    ab = baos.toByteArray();
                } catch (Exception e) {
                    System.out.println("*** Error, search() network indirection: " + e);
                }
            }
        } else {
            System.out.println("*** Error: unknown http request -->" + sReq + "<--");
        }
        connIn.putMime(200, "application/octet-stream");
        connIn.put(ab, 0, ab.length);
    }

    /**
 */
    public PBuffer getChapter(PLocation locationIn) {
        PBuffer pbuffer;
        int iVerseOffset;
        iVerseOffset = findVerseOffset(locationIn, abScriptureData);
        pbuffer = getChapter(iVerseOffset);
        return (pbuffer);
    }

    /**   Given an offset into a memory image of a scripture file, this method
 *    identifies the borders of the chapter that encloses this position and
 *    returns a PBuffer of the chapter.
 *
 */
    protected PBuffer getChapter(int iImageOffsetIn) {
        int i;
        int iPtr;
        int iBegin = 0;
        int iEnd;
        int iSize;
        byte[] abChapterRef;
        if (abScriptureData == null) return (null);
        iBegin = startOfLine(iImageOffsetIn);
        iPtr = iBegin;
        while (abScriptureData[iPtr] != ':') {
            iPtr++;
        }
        iSize = iPtr - iBegin;
        abChapterRef = new byte[iSize];
        System.arraycopy(abScriptureData, iBegin, abChapterRef, 0, iSize);
        iPtr = iBegin;
        while (iPtr > 0) {
            iPtr = prevLine(iPtr);
            if (iPtr == 0) break;
            if (!compare(abChapterRef, abScriptureData, iPtr)) {
                iPtr = nextLine(iPtr);
                break;
            }
        }
        iBegin = iPtr;
        while (iPtr < abScriptureData.length) {
            iPtr = nextLine(iPtr);
            if (iPtr == abScriptureData.length) break;
            if (!compare(abChapterRef, abScriptureData, iPtr)) {
                break;
            }
        }
        iEnd = iPtr;
        return (new PBuffer(abScriptureData, iBegin, iEnd - iBegin));
    }

    /**   This method pulls the requested file on behalf of apps that were 
 *    launched in 2 possible ways:  As an application from a command line
 *    or as an applet launched from a web server -- includes <b>network 
 *    indirection</b>.  
 *
 *    sFileIn is of the format "path/filename".
 *    On error, this returns null.
 *
 */
    public byte[] getFile(String sFileIn) {
        byte[] ab;
        int iBytesRead;
        InputStream is;
        PBuffer pbuffer;
        ByteArrayOutputStream baos;
        try {
            if (urlBase != null) {
                pbuffer = fetch("getFile " + sFileIn);
                return (pbuffer.getByteArray());
            }
            is = new FileInputStream(sFileIn);
            baos = new ByteArrayOutputStream();
            synchronized (abBuffer) {
                while ((iBytesRead = is.read(abBuffer)) >= 0) {
                    baos.write(abBuffer, 0, iBytesRead);
                }
            }
            is.close();
            return (baos.toByteArray());
        } catch (Exception e) {
            System.err.println("PDataFetcher Error 5 (" + sFileIn + "): " + e);
        }
        return (null);
    }

    /**
 */
    public static ScriptureViewerModel getModel(int iVersionIn) {
        if (maModel[iVersionIn] == null) {
            switch(iVersionIn) {
                default:
                    maModel[iVersionIn] = new ScriptureViewerModel(iVersionIn);
                    break;
            }
        }
        return (maModel[iVersionIn]);
    }

    /**
 */
    public static ScriptureViewerModel getModel(int iVersionIn, URL urlIn) {
        if (maModel[iVersionIn] == null) {
            maModel[iVersionIn] = new ScriptureViewerModel(iVersionIn, urlIn);
        }
        return (maModel[iVersionIn]);
    }

    /**   This makes sure the default scripture image for this object is fetched.  
 *    If this is running as a client-side model, it fails returning
 *    null (this is designed to only be called on the server side.
 *
 */
    public byte[] getScriptureImage() {
        if (urlBase != null) return (null);
        if (abScriptureData == null) {
            abScriptureData = getFile(PLocation.getVersionFilename(miVersion));
            if (abScriptureData == null) {
                System.out.println("Error getScriptureImage(): unable to fetch " + "--->" + PLocation.getVersionFilename(miVersion) + "<---");
            }
        }
        return (abScriptureData);
    }

    /**   This method returns the verse string that identifies the
 *    position that is passed to it.  The algorithm works backward
 *    from the position and extracts the verse name from the passage
 *    data.  The search string is bracked by square brackets:
 *    [Genesis 1:2].
 *                 
 */
    public String getSearchString(byte[] abIn, int iPtr) {
        int i;
        iPtr = startOfLine(iPtr) + 1;
        i = 0;
        while (abIn[iPtr + i] != ']') {
            i++;
        }
        return (new String(abIn, iPtr, i));
    }

    /**   The passed offset is assumed to point to the first byte of a
 *    line (usually points to a '[' character) and this method returns
 *    a pointer to the first character of the next line.  If an
 *    offset equal to the length of the data array is passed to it, then 
 *    that same value is returned.  The calling code should look for 
 *    the length of the data array as a signal to break from its loop.
 *
 */
    protected int nextLine(int iOffsetIn) {
        int iPtr;
        iPtr = iOffsetIn + 1;
        while (iPtr < abScriptureData.length) {
            if ((abScriptureData[iPtr] == '[') && ((abScriptureData[iPtr - 1] == '\n') || (abScriptureData[iPtr - 1] == '\r'))) {
                return (iPtr);
            }
            iPtr++;
        }
        return (abScriptureData.length);
    }

    /**   The passed offset is assumed to point to the first byte of a
 *    line (usually points to a '[' character) and this method returns
 *    a pointer to the first character of the previous line.  If an
 *    offset of 0 is passed to it, then 0 is returned.  The calling
 *    code should look for a 0 return to break from its loop.
 *
 */
    protected int prevLine(int iOffsetIn) {
        return (startOfLine(iOffsetIn - 1));
    }

    /**   Given a search string, this method returns an array of Bible image
 *    offsets that identify the first character of each occurrence of the
 *    search string in the Bible image -- includes <b>network indirection</b>. 
 *
 */
    public PLocationCollection search(String sSearchIn) {
        int i;
        int iSize;
        int iLength;
        int[] ai;
        byte[] ab;
        String s;
        String[] as;
        PBuffer pbuffer;
        PLocationCollection locations;
        ByteArrayInputStream bais;
        DataInputStream dis;
        try {
            if (urlBase != null) {
                s = SConnection.stringToHttp("search " + sSearchIn);
                pbuffer = fetch(s);
                bais = new ByteArrayInputStream(pbuffer.getByteArray(), pbuffer.getOffset(), pbuffer.getLength());
                dis = new DataInputStream(bais);
                iSize = dis.readInt();
                ai = new int[iSize];
                for (i = 0; i < iSize; i++) {
                    ai[i] = dis.readInt();
                }
                as = new String[iSize];
                ab = new byte[80];
                for (i = 0; i < iSize; i++) {
                    iLength = (int) dis.readShort() & 0x0000ffff;
                    dis.readFully(ab, 0, iLength);
                    as[i] = new String(ab, 0, iLength);
                }
                locations = new PLocationCollection(PLocation.VERSION_KJV);
                locations.setImageOffsets(ai, iSize);
                locations.setSearchStrings(as, iSize);
                return (locations);
            }
        } catch (Exception e) {
            System.out.println("Error search() using URL: " + e);
            return (null);
        }
        return (searchLocal(sSearchIn));
    }

    /**   Performs the search locally on the version in local memory.  There is
 *    no network indirection in this method.  The search() method calls this
 *    method to do the actual search.
 *
 */
    public PLocationCollection searchLocal(String sSearchIn) {
        int i;
        int iLength;
        char c, c1, c2;
        int iPtr;
        int iEnd;
        StringBuffer sb;
        String sSearchReverseCase;
        String[] asSearchStrings;
        byte[] abScripture;
        byte[] abSearch;
        byte[] abSearchReverseCase;
        PLocationCollection locations;
        sb = new StringBuffer();
        iLength = sSearchIn.length();
        for (i = 0; i < iLength; i++) {
            c = sSearchIn.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(Character.toUpperCase(c));
            }
        }
        sSearchReverseCase = sb.toString();
        abSearch = sSearchIn.getBytes();
        abSearchReverseCase = sSearchReverseCase.getBytes();
        abScripture = getScriptureImage();
        if (abScripture == null) return (null);
        iNumMatches = 0;
        iPtr = 0;
        iEnd = abScripture.length - sSearchIn.length();
        c1 = sSearchIn.charAt(0);
        c2 = sSearchReverseCase.charAt(0);
        while (iPtr < iEnd) {
            if (abScripture[iPtr] == '[') {
                while (abScripture[iPtr] != ']') {
                    iPtr++;
                }
                iPtr++;
            }
            c = (char) abScripture[iPtr];
            if (((c1 == c) || (c2 == c)) && (compareIgnoreCase(abSearch, abSearchReverseCase, abScripture, iPtr))) {
                aiMatchStartOffset[iNumMatches] = iPtr;
                iNumMatches++;
                if (iNumMatches >= MAX_RESULTS) break;
            }
            iPtr++;
        }
        asSearchStrings = new String[iNumMatches];
        for (i = 0; i < iNumMatches; i++) {
            asSearchStrings[i] = getSearchString(abScripture, aiMatchStartOffset[i]);
        }
        locations = new PLocationCollection(PLocation.VERSION_KJV);
        locations.setImageOffsets(aiMatchStartOffset, iNumMatches);
        locations.setSearchStrings(asSearchStrings, iNumMatches);
        return (locations);
    }

    /**   Accepts an offset into the buffer and returns the offset of the 
 *    first byte of the line (which is normally a '[' character).
 *
 */
    protected int startOfLine(int iOffsetIn) {
        int iPtr;
        iPtr = iOffsetIn;
        while (iPtr > 0) {
            if ((abScriptureData[iPtr] == '[') && ((abScriptureData[iPtr - 1] == '\n') || (abScriptureData[iPtr - 1] == '\r'))) {
                return (iPtr);
            }
            iPtr--;
        }
        return (0);
    }
}
