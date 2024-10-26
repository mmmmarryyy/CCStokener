package tufts.vue;

import tufts.Util;
import static tufts.Util.*;
import tufts.vue.gui.GUI;
import static tufts.vue.gui.GUI.dragName;
import tufts.vue.NodeTool.NodeModeTool;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.Shape;
import java.awt.Point;
import java.awt.Image;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.*;
import java.io.File;
import java.io.FileInputStream;
import static java.awt.dnd.DnDConstants.*;
import java.net.*;

/**
 * Handle the dropping of drags mediated by host operating system onto the map.
 *
 * We currently handling the dropping of File lists, LWComponent lists,
 * Resource lists, and text (a String).
 *
 * @version $Revision: 1.135 $ / $Date: 2010-02-03 19:17:41 $ / $Author: mike $  
 */
public class MapDropTarget implements java.awt.dnd.DropTargetListener {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MapDropTarget.class);

    private static final boolean DropImagesAsNodes = true;

    private static final int DROP_FILE_LIST = 1;

    private static final int DROP_NODE_LIST = 2;

    private static final int DROP_RESOURCE_LIST = 3;

    private static final int DROP_TEXT = 4;

    private static final int DROP_ONTOLOGY_TYPE = 5;

    private static final int DROP_GENERAL_HANDLER = 6;

    public static final int ALL_DROP_TYPES = DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE | DnDConstants.ACTION_LINK;

    public static final int ACCEPTABLE_DROP_TYPES = DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE | DnDConstants.ACTION_LINK;

    private boolean CenterNodesOnDrop = true;

    private final MapViewer mViewer;

    private DropHandler mActiveHandler;

    public MapDropTarget(MapViewer viewer) {
        mViewer = viewer;
    }

    public static final String DROP_REJECT = "DROP_REJECT";

    public static final String DROP_ACCEPT_NODE = "DROP_ACCEPT_NODE";

    public static final String DROP_ACCEPT_DATA = "DROP_ACCEPT_DATA";

    public static final String DROP_RESOURCE_RESET = "DROP_RESOURCE_RESET";

    public static class DropIndication {

        private static final DropIndication REJECTED = new DropIndication();

        static DropIndication last;

        public final Object type;

        /** This is the action we want to ACCEPT: it may be different from the
          * user-indicated drop action, and may not even be one of the availble source
          * actions, tho we can accept any action we like, and the drag/drop cursor
          * should be set appropriately */
        public final int acceptedAction;

        /** This accepted hit target, which will depend on the drop action and drop target */
        public final LWComponent hit;

        public DropIndication(Object t, int action, LWComponent target) {
            type = t;
            acceptedAction = action;
            hit = target;
            last = this;
        }

        public static DropIndication rejected() {
            return REJECTED;
        }

        private DropIndication() {
            type = DROP_REJECT;
            acceptedAction = ACTION_NONE;
            hit = null;
            last = this;
        }

        boolean isAccepted() {
            return type != DROP_REJECT;
        }

        /** @return true if type and hit are the same */
        boolean isSame(DropIndication di) {
            return di.type == type && di.hit == hit;
        }

        java.awt.Color getColor() {
            if (type == DROP_RESOURCE_RESET) return VueConstants.COLOR_INDICATION_ALTERNATE; else return VueConstants.COLOR_INDICATION;
        }

        @Override
        public String toString() {
            return "Indication[" + type + "; " + dropName(acceptedAction) + "; hit=" + LWComponent.tag(hit) + "]";
        }
    }

    /** DropTargetListener */
    public void dragEnter(DropTargetDragEvent e) {
        final Transferable transfer = e.getTransferable();
        if (transfer.isDataFlavorSupported(DropHandler.DataFlavor)) mActiveHandler = extractData(transfer, DropHandler.DataFlavor, DropHandler.class);
        final DropIndication di = getIndication(e);
        if (DEBUG.DND) out("dragEnter: " + dragName(e) + "; handler=" + mActiveHandler + " " + di);
        e.acceptDrag(di.acceptedAction);
    }

    /** DropTargetListener */
    public void dragOver(DropTargetDragEvent e) {
        final DropIndication di = getIndication(e);
        if (DEBUG.DND) out("dragOver: " + dragName(e) + " " + di);
        if (di.isAccepted()) {
            e.acceptDrag(di.acceptedAction);
            mViewer.setIndicated(di);
        } else {
            mViewer.clearIndicated();
            e.rejectDrag();
        }
    }

    /** DropTargetListener */
    public void dropActionChanged(DropTargetDragEvent e) {
        if (DEBUG.DND) out("dropActionChanged: " + dragName(e));
        dragOver(e);
    }

    /** DropTargetListener */
    public void dragExit(DropTargetEvent e) {
        if (DEBUG.DND) out("dragExit: " + e);
    }

    /** DropTargetListener */
    public void drop(DropTargetDropEvent e) {
        try {
            GUI.activateWaitCursor();
            final DropIndication di = getIndication(e);
            if (DEBUG.DND) out(TERM_GREEN + "\nDROP: " + Util.tag(e) + "\n\t     sourceActions: " + dropName(e.getSourceActions()) + "\n\t        dropAction: " + dropName(e.getDropAction()) + "\n\t        dropAccept: " + dropName(di.acceptedAction) + "\n\t          location: " + e.getLocation() + TERM_CLEAR);
            e.acceptDrop(di.acceptedAction);
            boolean success = processTransferable(e.getTransferable(), e);
            if (DEBUG.DND) out(TERM_CYAN + "processTransferable: success=" + success + TERM_CLEAR);
            e.dropComplete(success);
            mViewer.clearIndicated();
        } finally {
            GUI.clearWaitCursor();
        }
    }

    private static final Object POSSIBLE_RESOURCE = new Object();

    private DropIndication getIndication(DropTargetDragEvent e) {
        return getIndication(e, e.getSourceActions(), e.getDropAction(), dropToMapLocation(e.getLocation()));
    }

    private DropIndication getIndication(DropTargetDropEvent e) {
        return getIndication(e, e.getSourceActions(), e.getDropAction(), dropToMapLocation(e.getLocation()));
    }

    private DropIndication getIndication(final DropTargetEvent e, final int sourceAbleActions, final int dropAction, final Point2D.Float mapLoc) {
        int dropAccept = dropAction;
        if (dropAction == ACTION_MOVE && (sourceAbleActions & ACTION_COPY) != 0) {
            dropAccept = ACTION_COPY;
        } else if (dropAction == ACTION_NONE) {
            dropAccept = ACTION_LINK;
        }
        final PickContext pc = mViewer.getPickContext(mapLoc.x, mapLoc.y);
        pc.dropping = POSSIBLE_RESOURCE;
        final LWComponent hit = LWTraversal.PointPick.pick(pc);
        LWComponent dropHit = null;
        if (mActiveHandler != null) {
            return mActiveHandler.getIndication(hit, dropAccept);
        } else {
            if (hit instanceof LWImage) {
                final LWImage image = (LWImage) hit;
                if (image.isNodeIcon()) {
                    dropHit = hit.getParent();
                } else if (image.hasImageError() || dropAccept == ACTION_LINK) {
                    dropHit = hit;
                    dropAccept = ACTION_LINK;
                }
            } else if (hit instanceof LWLink) {
                dropHit = hit;
                dropAccept = ACTION_LINK;
            } else if (hit != null) {
                if (hit.supportsChildren() && hit != mViewer.getFocal()) {
                    if (hit instanceof LWSlide) ; else if (hit instanceof LWGroup) ; else dropHit = hit;
                }
            }
        }
        final DropIndication report;
        Object type = DROP_ACCEPT_NODE;
        if (dropAccept == ACTION_LINK) {
            if ((hit instanceof LWNode || hit instanceof LWImage) && hit.hasResource()) {
                type = DROP_RESOURCE_RESET;
            } else {
            }
        }
        if (type != null) report = new DropIndication(type, dropAccept, hit); else report = new DropIndication();
        return report;
    }

    public static class DropContext {

        public final Transferable transfer;

        public final Point2D.Float location;

        public final MapViewer viewer;

        public LWComponent hit;

        public LWContainer hitParent;

        public final boolean isLinkAction;

        public List items;

        public final String text;

        private float nextX;

        private float nextY;

        public List select = new java.util.ArrayList();

        DropContext(Transferable t, Point2D.Float mapLocation, MapViewer viewer, List items, String text, LWComponent hit, boolean isLinkAction) {
            this.transfer = t;
            this.location = mapLocation;
            this.viewer = viewer;
            this.items = items;
            this.text = text;
            this.hit = hit;
            if (hit != null && hit.supportsChildren()) hitParent = (LWContainer) hit; else hitParent = null;
            this.isLinkAction = isLinkAction;
            if (mapLocation != null) {
                nextX = mapLocation.x;
                nextY = mapLocation.y;
            }
            if (DEBUG.DND) System.out.println("DropContext: loc: " + Util.fmt(mapLocation) + "\n             hit: " + hit + "\n       hitParent: " + hitParent);
        }

        Point2D nextDropLocation() {
            Point2D p = new Point.Float(nextX, nextY);
            nextX += 15;
            nextY += 15;
            return p;
        }

        /**
         * Track top-level nodes created and added to map as we processed the drop.
         * Note that nodes are created and added to the map as the drop is processed in
         * case we fail we can get some partial results.  This lets us set the selection
         * to everything that was dropped at the end.
         */
        void add(LWComponent c) {
            select.add(c);
        }
    }

    private static final Object DATA_FAILURE = new Object();

    public static <A> A extractData(Transferable transfer, DataFlavor flavor, Class<A> clazz) {
        final Object data = extractData(transfer, flavor);
        if (clazz.isInstance(data)) {
            return clazz.cast(data);
        } else {
            Log.warn("Transfer data expecting type " + clazz + "; found: " + Util.tags(data));
            return null;
        }
    }

    public static Object extractData(Transferable transfer, DataFlavor flavor) {
        Log.info("extractData " + flavor);
        Object data = DATA_FAILURE;
        try {
            data = transfer.getTransferData(flavor);
        } catch (UnsupportedFlavorException ex) {
            Util.printStackTrace(ex, "TRANSFER: Transfer lied about supporting flavor " + "\"" + flavor.getHumanPresentableName() + "\" " + flavor);
        } catch (java.io.IOException ex) {
            Util.printStackTrace(ex, "TRANSFER: data no longer available");
        }
        return data;
    }

    private static final Pattern HTML_Fragment = Pattern.compile(".*<!--StartFragment-->(.*)<!--EndFragment-->", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern IMG_Tag = Pattern.compile(".*<img\\s+.*\\bsrc=\"([^\"]*)", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /** @return the string matched by the first group in the given Pattern, or null if no match */
    private static final String extractText(Pattern pattern, String text) {
        final Matcher m = pattern.matcher(text);
        String s = null;
        if (m.lookingAt()) return m.group(1); else return null;
    }

    /**
     * Process any transferrable: @param e can be null if don't have a drop event
     * (e.g., could use to process clipboard contents as well as drop events)
     * A sucessful result will be newly created items on the map.
     * @return true if succeeded
     */
    public boolean processTransferable(Transferable transfer, DropTargetDropEvent e) {
        Point dropLocation = null;
        Point2D.Float mapLocation = null;
        int dropAction = DnDConstants.ACTION_COPY;
        if (e != null) {
            dropLocation = e.getLocation();
            dropAction = DropIndication.last.acceptedAction;
            mapLocation = dropToMapLocation(dropLocation);
            if (DEBUG.DND) out("processTransferable: " + Util.tag(e) + "\n\t       description: " + GUI.dropName(e) + "\n\t        dropAction: " + dropName(e.getDropAction()) + "\n\t        dropAccept: " + DropIndication.last + "\n\t     dropScreenLoc: " + Util.fmt(dropLocation) + "\n\t        dropMapLoc: " + Util.fmt(mapLocation));
        } else {
            if (DEBUG.DND) out("processTransferable: (no drop event) transfer=" + transfer);
        }
        final boolean isLinkAction = (dropAction == ACTION_LINK);
        LWComponent dropTarget = null;
        Point2D.Float hitLocation = null;
        if (dropLocation != null) {
            dropTarget = DropIndication.last.hit;
            if (DEBUG.DND) out("dropTarget=" + dropTarget + " in " + mViewer);
            if (dropTarget != null) {
                if (!dropTarget.supportsChildren() && !isLinkAction) {
                    if (DEBUG.DND) out("dropTarget: doesn't support children: " + dropTarget);
                    return false;
                }
                hitLocation = mapToLocalLocation(mapLocation, dropTarget);
                if (DEBUG.DND) out("dropTarget hit location: " + Util.fmt(hitLocation));
            } else {
                if (mViewer.getFocal() instanceof LWMap == false) {
                    if (DEBUG.DND) out("warning: drop to non-map focal " + mViewer.getFocal());
                    hitLocation = mapToLocalLocation(mapLocation, mViewer.getFocal());
                }
            }
        } else {
            if (mViewer != null) {
                dropLocation = mViewer.getLastMousePressPoint();
                mapLocation = dropToFocalLocation(dropLocation);
            }
        }
        if (hitLocation == null) hitLocation = mapLocation;
        DataFlavor foundFlavor = null;
        Object foundData = null;
        DropHandler foundHandler = null;
        String dropText = null;
        List dropItems = null;
        int dropType = 0;
        if (DEBUG.DND && DEBUG.META) dumpFlavors(transfer);
        final DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
        final DataFlavor URLFlavor = findFlavor(dataFlavors, "application/x-java-url", java.net.URL.class);
        final DataFlavor HTMLTextFlavor = findFlavor(dataFlavors, "text/html", java.lang.String.class);
        URL found_HTTP_URL = null;
        if (HTMLTextFlavor != null) {
            final String htmlText = extractData(transfer, HTMLTextFlavor, String.class);
            if (htmlText != null) {
                final String fragment = extractText(HTML_Fragment, htmlText);
                if (fragment != null) {
                    Log.debug("FOUND HTML FRAGMENT [" + fragment + "]");
                    final String imgSrc = extractText(IMG_Tag, fragment);
                    if (imgSrc != null) {
                        Log.debug("FOUND IMG SRC=[" + imgSrc + "]");
                        if (imgSrc != null && imgSrc.toLowerCase().startsWith("http")) {
                            URL url = null;
                            try {
                                url = new java.net.URL(imgSrc);
                            } catch (Throwable t) {
                                Log.debug("invalid URL: " + imgSrc + "; " + t);
                            }
                            found_HTTP_URL = url;
                        }
                    }
                }
            }
        }
        try {
            if (URLFlavor != null && found_HTTP_URL == null) {
                URL url = null;
                try {
                    url = extractData(transfer, URLFlavor, URL.class);
                } catch (Throwable t) {
                    Log.warn("failure extracting " + URLFlavor, t);
                }
                if (url != null) {
                    if ("http".equals(url.getProtocol())) {
                        found_HTTP_URL = url;
                        Log.debug("FOUND HTTP URL FLAVOR/DATA: " + URLFlavor + "; URL=" + url);
                    }
                }
            }
            if (transfer.isDataFlavorSupported(DropHandler.DataFlavor)) {
                foundFlavor = DropHandler.DataFlavor;
                foundHandler = extractData(transfer, foundFlavor, DropHandler.class);
                dropType = DROP_GENERAL_HANDLER;
            } else if (transfer.isDataFlavorSupported(edu.tufts.vue.ontology.ui.TypeList.DataFlavor) && (dropAction == DnDConstants.ACTION_LINK)) {
                dropType = DROP_ONTOLOGY_TYPE;
                foundData = extractData(transfer, edu.tufts.vue.ontology.ui.TypeList.DataFlavor);
            } else if (transfer.isDataFlavorSupported(LWComponent.DataFlavor)) {
                foundFlavor = LWComponent.DataFlavor;
                foundData = extractData(transfer, foundFlavor);
                dropType = DROP_NODE_LIST;
                dropItems = (List) foundData;
            } else if (transfer.isDataFlavorSupported(Resource.DataFlavor)) {
                foundFlavor = Resource.DataFlavor;
                foundData = extractData(transfer, foundFlavor);
                if (foundData == null) throw new IllegalStateException("null resource found");
                dropType = DROP_RESOURCE_LIST;
                if (foundData instanceof List) dropItems = (List) foundData; else dropItems = Collections.singletonList(foundData);
            } else if (found_HTTP_URL != null && !found_HTTP_URL.getHost().equals("images.google.com")) {
                dropType = DROP_TEXT;
                final String http_url = found_HTTP_URL.toString();
                if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    final String txt = extractData(transfer, DataFlavor.stringFlavor, String.class);
                    if (txt.length() > http_url.length() && txt.startsWith(http_url)) {
                        foundData = txt;
                        dropText = txt;
                        foundFlavor = DataFlavor.stringFlavor;
                    }
                }
                if (dropText == null) {
                    foundFlavor = URLFlavor;
                    foundData = found_HTTP_URL;
                    dropText = http_url;
                }
            } else if (transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                foundFlavor = DataFlavor.javaFileListFlavor;
                foundData = extractData(transfer, foundFlavor);
                dropType = DROP_FILE_LIST;
                dropItems = (List) foundData;
            } else if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                foundFlavor = DataFlavor.stringFlavor;
                foundData = extractData(transfer, foundFlavor);
                dropType = DROP_TEXT;
                dropText = (String) foundData;
            } else {
                if (DEBUG.Enabled) {
                    System.out.println("TRANSFER: found no supported dataFlavors");
                    dumpFlavors(transfer);
                }
                return false;
            }
        } catch (ClassCastException ex) {
            Util.printStackTrace(ex, "TRANSFER: Transfer data did not match expected type:" + "\n\tflavor=" + foundFlavor + "\n\t  type=" + foundData.getClass());
            return false;
        } catch (Throwable t) {
            Util.printStackTrace(t, "TRANSFER: data extraction failure");
            return false;
        }
        if (foundData == DATA_FAILURE) return false;
        if (DEBUG.Enabled) {
            String size = "";
            Object firstInBag = null;
            String bagEntry0 = "";
            if (foundData instanceof Collection) {
                Collection bag = (Collection) foundData;
                size = " (Collection size " + bag.size() + ")";
                if (bag.size() > 0) {
                    final Object o = bag.iterator().next();
                    firstInBag = o;
                    bagEntry0 = "\n\tdata[0]: " + Util.tags(firstInBag);
                }
            }
            Log.debug(TERM_CYAN + "\nTRANSFER: Found a supported DataFlavor among the " + dataFlavors.length + " available;" + "\n\t flavor: " + foundFlavor + "\n\tdataTag: " + Util.tag(foundData) + size + (DEBUG.META ? ("\n\tdataRaw: [" + foundData + "]") : "") + bagEntry0 + TERM_CLEAR);
        }
        DropContext drop = new DropContext(transfer, hitLocation, mViewer, dropItems, dropText, dropTarget, isLinkAction);
        boolean success = false;
        if (dropItems != null && dropItems.size() > 1) ; else CenterNodesOnDrop = true;
        try {
            switch(dropType) {
                case DROP_GENERAL_HANDLER:
                    return processDroppedHandler(drop, foundHandler);
                case DROP_FILE_LIST:
                    success = processDroppedFileList(drop);
                    break;
                case DROP_NODE_LIST:
                    success = processDroppedNodes(drop);
                    break;
                case DROP_RESOURCE_LIST:
                    success = processDroppedResourceList(drop);
                    break;
                case DROP_TEXT:
                    success = processDroppedText(drop);
                    break;
                case DROP_ONTOLOGY_TYPE:
                    success = processDroppedOntologyType(drop, foundData);
                    break;
                default:
                    throw new Error("unknown drop type " + dropType);
            }
            completeDrop(drop);
        } catch (Throwable t) {
            Util.printStackTrace(t, "drop processing failed");
        }
        mViewer.getMap().getUndoManager().mark("Drop");
        return success;
    }

    public static void completeDrop(DropContext drop) {
        if (drop.items != null && drop.items.size() > 0) {
            drop.viewer.grabVueApplicationFocus("drop", null);
        }
        if (drop.select != null && drop.select.size() > 0) {
            drop.viewer.selectionSet(drop.select);
        }
    }

    protected boolean processDroppedText(DropContext drop) {
        if (DEBUG.DND) out("processDroppedText");
        String[] rows = drop.text.split("\n");
        URL foundURL = null;
        Map properties = new HashMap();
        if (rows.length < 3) {
            foundURL = makeURL(rows[0]);
            if (rows.length > 1) {
                properties.put("title", rows[1]);
            }
        }
        if (foundURL != null && foundURL.getQuery() != null) {
            foundURL = decodeSearchEngineLightBoxURL(foundURL, properties);
        }
        if (foundURL != null) {
            boolean processed = true;
            boolean overwriteResource = drop.isLinkAction;
            if (drop.hit != null) {
                if (overwriteResource) {
                    drop.hit.setResource(foundURL.toString());
                    ((URLResource) drop.hit.getResource()).scanForMetaDataAsync(drop.hit);
                } else if (drop.hitParent != null) {
                    drop.hitParent.dropChild(createNodeAndResource(drop, null, foundURL.toString(), properties, drop.location));
                } else {
                    processed = false;
                }
            }
            if (drop.hit == null || !processed) createNodeAndResource(drop, null, foundURL.toString(), properties, drop.location);
        } else {
            drop.add(createTextNode(drop.text, drop.location));
        }
        return true;
    }

    public abstract static class DropHandler {

        public static final java.awt.datatransfer.DataFlavor DataFlavor = tufts.vue.gui.GUI.makeDataFlavor(DropHandler.class);

        public abstract boolean handleDrop(DropContext drop);

        public abstract DropIndication getIndication(LWComponent target, int requestAction);
    }

    private boolean processDroppedHandler(DropContext drop, DropHandler handler) {
        if (DEBUG.Enabled) out("processDroppedHandler: " + Util.tags(handler));
        VUE.activateWaitCursor();
        boolean success = false;
        try {
            success = handler.handleDrop(drop);
        } catch (Throwable t) {
            Log.error("dropHandler failed: " + Util.tags(handler), t);
        } finally {
            VUE.clearWaitCursor();
        }
        return success;
    }

    public static void addNodesToMap(DropContext drop) {
        if (DEBUG.Enabled) Log.debug("addNodesToMap: " + Util.tags(drop.items));
        if (drop.hitParent != null && !(drop.hitParent instanceof LWMap)) {
            drop.hitParent.addChildren(drop.items, LWComponent.ADD_DROP);
        } else {
            drop.viewer.getDropFocal().addChildren(drop.items, LWComponent.ADD_DROP);
        }
    }

    private boolean processDroppedNodes(DropContext drop) {
        if (DEBUG.DND) out("processDroppedNodes");
        if (CenterNodesOnDrop) setCenterAt(drop.items, drop.location); else setLocation(drop.items, drop.location);
        addNodesToMap(drop);
        drop.select.addAll(drop.items);
        return true;
    }

    private boolean processDroppedResourceList(DropContext drop) {
        if (DEBUG.DND) out("processDroppedResourceList");
        if (drop.items.size() == 1 && drop.hit != null && drop.isLinkAction) {
            drop.hit.setResource((Resource) drop.items.get(0));
        } else {
            Iterator i = drop.items.iterator();
            while (i.hasNext()) {
                Resource resource = (Resource) i.next();
                if (drop.hitParent != null && !drop.isLinkAction) {
                    drop.hitParent.dropChild(createNode(drop, resource, drop.nextDropLocation()));
                } else {
                    createNode(drop, resource, drop.nextDropLocation());
                }
            }
        }
        return true;
    }

    private boolean processDroppedOntologyType(DropContext drop, Object foundData) {
        if (DEBUG.DND) out("processDroppedType");
        edu.tufts.vue.metadata.VueMetadataElement ele = new edu.tufts.vue.metadata.VueMetadataElement();
        ele.setObject(foundData);
        drop.hit.getMetadataList().getMetadata().add(ele);
        edu.tufts.vue.metadata.ui.OntologicalMembershipPane.getGlobal().refresh();
        return true;
    }

    private boolean processDroppedFileList(DropContext drop) {
        if (DEBUG.DND) out("processDroppedFileList");
        for (Object o : drop.items) {
            try {
                processDroppedFile((File) o, drop);
            } catch (Throwable t) {
                Log.error("processing dropped file " + Util.tags(o), t);
            }
        }
        return true;
    }

    private void processDroppedFile(File file, DropContext drop) {
        String resourceSpec = file.getPath();
        String path = file.getPath();
        Map props = new HashMap();
        if ((DEBUG.IO || DEBUG.DND) && !file.exists()) examineBadFile(file);
        if (path.toLowerCase().endsWith(".url")) {
            String url = convertWindowsURLShortCutToURL(file);
            if (url != null) {
                resourceSpec = url;
                String resourceName;
                if (file.getName().length() > 4) resourceName = file.getName().substring(0, file.getName().length() - 4); else resourceName = file.getName();
                props.put("title", resourceName);
            }
        } else if (path.endsWith(".textClipping") || path.endsWith(".webloc") || path.endsWith(".fileloc")) {
            if (drop.transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String unicodeString;
                try {
                    unicodeString = (String) drop.transfer.getTransferData(DataFlavor.stringFlavor);
                    if (DEBUG.Enabled) out("*** GOT MAC REDIRECT DATA [" + unicodeString + "]");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (drop.hit != null) {
            if (drop.isLinkAction || drop.hit instanceof LWLink) {
                drop.hit.setResource(resourceSpec);
            } else if (drop.hitParent != null) {
                drop.hitParent.dropChild(createNodeAndResource(drop, file, resourceSpec, props, drop.nextDropLocation()));
            }
        } else {
            createNodeAndResource(drop, file, resourceSpec, props, drop.nextDropLocation());
        }
    }

    private LWComponent createNodeAndResource(DropContext drop, File file, String resourceSpec, Map properties, Point2D where) {
        final Resource resource;
        if (file != null) resource = mViewer.getMap().getResourceFactory().get(file); else resource = mViewer.getMap().getResourceFactory().get(resourceSpec);
        if (DEBUG.DND) out("createNodeAndResource " + resourceSpec + " " + properties + " where=" + where);
        LWComponent c = createNode(drop, resource, properties, where, true);
        return c;
    }

    private LWComponent createNode(DropContext drop, Resource resource, Point2D where) {
        return createNode(drop, resource, Collections.EMPTY_MAP, where, false);
    }

    private static int MaxNodeTitleLen = VueResources.getInt("node.title.maxDefaultChars", 50);

    private LWComponent createNode(DropContext drop, Resource resource, Map properties, Point2D where, boolean newResource) {
        if (DEBUG.DND) Log.debug(drop + "; createNode " + resource + " " + properties + " where=" + where);
        if (properties == null) properties = Collections.EMPTY_MAP;
        final boolean dropImagesAsNodes = DropImagesAsNodes && !drop.isLinkAction && !(drop.hitParent instanceof LWSlide) && !(drop.hit == null && mViewer.getFocal() instanceof LWSlide);
        LWComponent node;
        String displayName = (String) properties.get("title");
        if (displayName == null) displayName = makeNodeTitle(resource);
        String shortName = displayName;
        if (shortName.length() > MaxNodeTitleLen) shortName = shortName.substring(0, MaxNodeTitleLen) + "...";
        LWImage lwImage = null;
        if (resource.isImage()) {
            int suggestWidth = -1, suggestHeight = -1;
            if (DEBUG.DND || DEBUG.IMAGE) Log.debug(drop + "; IMAGE DROP " + resource + " " + properties);
            String ws = (String) properties.get("width");
            String hs = (String) properties.get("height");
            if (ws != null && hs != null) {
                suggestWidth = Integer.parseInt(ws);
                suggestHeight = Integer.parseInt(hs);
                if (suggestWidth > 0 && suggestHeight > 0) {
                    resource.setProperty(Resource.IMAGE_WIDTH, ws);
                    resource.setProperty(Resource.IMAGE_HEIGHT, hs);
                }
            }
        }
        if (dropImagesAsNodes) {
            shortName = Util.formatLines(shortName, VueResources.getInt("dataNode.labelLength"));
            node = NodeModeTool.createNewNode(shortName);
            node.setResource(resource);
        } else {
            lwImage = LWImage.create(resource);
            if (resource != null) lwImage.setLabel(makeNodeTitle(resource));
            node = lwImage;
        }
        if (where != null) addNodeToFocal(node, where);
        drop.add(node);
        return node;
    }

    private LWComponent createTextNode(String text, Point2D where) {
        return addNodeToFocal(NodeModeTool.createTextNode(text), where);
    }

    private LWComponent addNodeToFocal(final LWComponent node, Point2D where) {
        if (DEBUG.DND) Log.debug("addNodeToFocal: " + node + "; where=" + where + "; centerAt=" + CenterNodesOnDrop);
        if (CenterNodesOnDrop) node.setCenterAt(where); else node.setLocation(where);
        mViewer.getFocal().dropChild(node);
        return node;
    }

    /**
     * for nodes dropped directly into the layer (not another node in the layer), if it
     * looks "crowded", push out all the other nodes on the layer to make more room;
     */
    public static void makeRoomFor(final LWComponent node) {
        node.addCleanupTask(new Runnable() {

            public void run() {
                if (!node.getParent().isTopLevel()) {
                    if (DEBUG.Enabled) Log.debug("ignoring push for non-top0level final parent of " + node);
                    return;
                }
                final LWMap.Layer layer = node.getLayer();
                if (layer != null && layer.getChildren() != null) {
                    final Rectangle2D.Float clearRegion = Util.grow(node.getMapBounds(), 24);
                    for (LWComponent n : layer.getChildren()) {
                        if (n == node || n.getParent() != layer) continue;
                        if (clearRegion.intersects(n.getMapBounds())) {
                            Actions.PushOut.act(node);
                            break;
                        }
                    }
                }
            }
        });
    }

    private static String dropName(int dropAction) {
        return GUI.dropName(dropAction);
    }

    private void dumpFlavors(Transferable transfer) {
        final DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
        Log.debug("TRANSFERABLE: " + transfer + " has " + dataFlavors.length + " dataFlavors:");
        for (int i = 0; i < dataFlavors.length; i++) {
            DataFlavor flavor = dataFlavors[i];
            String name = flavor.getHumanPresentableName();
            if (flavor.getMimeType().toString().startsWith(name + ";")) name = ""; else name = "\"" + name + "\"";
            System.out.format("flavor %2d %-16s %s", i, name, flavor.getMimeType());
            try {
                Object data = transfer.getTransferData(flavor);
                System.out.println(" [" + data + "]");
                if (DEBUG.META) {
                    if (flavor.getHumanPresentableName().equals("text/uri-list")) readTextFlavor(flavor, transfer);
                }
            } catch (Exception ex) {
                System.out.println("\tEXCEPTION: getTransferData: " + ex);
            }
        }
    }

    private DataFlavor findFlavor(DataFlavor[] dataFlavors, String mimeType, Class repClass) {
        for (DataFlavor flavor : dataFlavors) {
            if (flavor.isMimeTypeEqual(mimeType) && flavor.getRepresentationClass() == repClass) return flavor;
        }
        return null;
    }

    /** attempt to make a URL from a string: return null if malformed */
    private static URL makeURL(String s) {
        try {
            return new URL(s);
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    /**
     * URL's dragged from the image search page of most search engines include query
     * fields that allow us to locate the original source of the image, as well as
     * width and height
     *
     * @param url a URL that at least know has a query
     * @param properties a map to put found properties into (e.g., width, height)
     */
    private static URL decodeSearchEngineLightBoxURL(final URL url, Map properties) {
        final String query = url.getQuery();
        if (DEBUG.IMAGE || DEBUG.IO || DEBUG.DND) Log.debug("DECODE QUERY: host " + url.getHost() + " query " + url.getQuery());
        Map data = VueUtil.getQueryData(query);
        if (DEBUG.DND) {
            String[] pairs = query.split("&");
            for (int i = 0; i < pairs.length; i++) {
                System.out.println("\tquery pair " + pairs[i]);
            }
        }
        final String host = url.getHost();
        final String s = url.toString();
        final String urlWithoutQuery;
        final int questionMarkIndex = s.indexOf('?');
        if (questionMarkIndex > 0) urlWithoutQuery = s.substring(0, questionMarkIndex); else urlWithoutQuery = s;
        String imageURL = (String) data.get("imgurl");
        if (imageURL == null) imageURL = (String) data.get("image_url");
        if (imageURL == null && host.endsWith(".msn.com") || host.endsWith(".live.com")) imageURL = (String) data.get("iu");
        if (imageURL == null && host.endsWith(".netscape.com")) imageURL = (String) data.get("img");
        if (imageURL == null && host.endsWith(".ask.com")) imageURL = (String) data.get("u");
        URL redirectURL = null;
        if (imageURL == null && host.endsWith(".flickr.com")) {
            try {
                return new URL(urlWithoutQuery);
            } catch (Throwable t) {
                return url;
            }
        }
        if (imageURL == null) imageURL = (String) data.get("url");
        if (imageURL != null && ("images.google.com".equals(host) || "search.live.com".equals(host) || "images.search.yahoo.com".equals(host) || "rds.yahoo.com".equals(host) || "search.lycos.com".equals(host) || "tm.ask.com".equals(host) || "search.msn.com".equals(host) || "search.netscape.com".equals(host) || host.endsWith("mamma.com"))) {
            imageURL = Util.decodeURL(imageURL);
            if (DEBUG.IMAGE || DEBUG.IO || DEBUG.DND) Log.debug("redirect to image search url " + imageURL);
            if (imageURL.indexOf(':') < 0) imageURL = "http://" + imageURL;
            redirectURL = makeURL(imageURL);
            if (redirectURL == null && !imageURL.startsWith("http://")) redirectURL = makeURL("http://" + imageURL);
            if (DEBUG.IMAGE || DEBUG.IO || DEBUG.DND) Log.debug("redirect got URL " + redirectURL);
            if (url != null) {
                String w = (String) data.get("w");
                String h = (String) data.get("h");
                if (w == null || h == null) {
                    w = (String) data.get("wd");
                    h = (String) data.get("ht");
                    if (w == null || h == null) {
                        w = (String) data.get("image_width");
                        h = (String) data.get("image_height");
                        if (w == null || h == null) {
                            w = (String) data.get("width");
                            h = (String) data.get("height");
                        }
                    }
                }
                if (w != null && h != null && properties != null) {
                    properties.put("width", w);
                    properties.put("height", h);
                }
            }
        }
        if (redirectURL == null) return url; else return redirectURL;
    }

    private static final Pattern URL_Line = Pattern.compile(".*^URL=([^\r\n]+).*", Pattern.MULTILINE | Pattern.DOTALL);

    public static String convertWindowsURLShortCutToURL(File file) {
        String url = null;
        try {
            if (DEBUG.DND) Log.debug("Searching for URL in: " + file);
            FileInputStream is = new FileInputStream(file);
            byte[] buf = new byte[2048];
            int len = is.read(buf);
            is.close();
            String str = new String(buf, 0, len);
            if (DEBUG.DND) System.out.println("*** size=" + str.length() + "[" + str + "]");
            Matcher m = URL_Line.matcher(str);
            if (m.lookingAt()) {
                url = m.group(1);
                if (url != null) url = url.trim();
                if (DEBUG.DND) System.out.println("*** FOUND URL [" + url + "]");
                int i = url.indexOf("|/");
                if (i > -1) {
                    url = url.substring(0, i) + ":" + url.substring(i + 1);
                    Log.debug("PATCHED URL [" + url + "]");
                }
            }
        } catch (Exception e) {
            Log.debug(e);
        }
        return url;
    }

    private Point2D.Float dropToFocalLocation(Point p) {
        return dropToFocalLocation(p.x, p.y);
    }

    private Point2D.Float mapToLocalLocation(Point2D.Float mapLocation, LWComponent local) {
        return (Point2D.Float) local.transformMapToZeroPoint(mapLocation, new Point2D.Float());
    }

    private Point2D.Float dropToFocalLocation(int x, int y) {
        final Point2D.Float mapLoc = (Point2D.Float) mViewer.screenToFocalPoint(x, y);
        return mapLoc;
    }

    private Point2D.Float dropToMapLocation(Point p) {
        final Point2D.Float mapLoc = mViewer.screenToMapPoint(p.x, p.y);
        return mapLoc;
    }

    static String makeNodeTitle(Resource resource) {
        if (resource.getTitle() != null) return resource.getTitle();
        String title = resource.getProperty("title");
        if (title != null) return title;
        String spec = resource.getSpec();
        String name = Util.decodeURL(spec);
        int slashIdx = name.lastIndexOf('/');
        if (slashIdx == name.length() - 1) {
            return name;
        } else {
            if (slashIdx > 0) {
                name = name.substring(slashIdx + 1);
                int dotIdx = name.lastIndexOf('.');
                if (dotIdx > 0) name = name.substring(0, dotIdx);
                name = name.replace('_', ' ');
                name = name.replace('.', ' ');
                name = name.replace('-', ' ');
                name = Util.upperCaseWords(name);
            }
        }
        return name;
    }

    /**
     * Given a collection of LWComponent's, center them as a group at the given map location.
     */
    public static void setCenterAt(Collection<LWComponent> nodes, Point2D.Float mapLocation) {
        if (DEBUG.DND) Log.debug("setCenterAt " + mapLocation + "; " + Util.tags(nodes));
        java.awt.geom.Rectangle2D.Float bounds = LWMap.getBounds(nodes.iterator());
        float dx = mapLocation.x - (bounds.x + bounds.width / 2);
        float dy = mapLocation.y - (bounds.y + bounds.height / 2);
        translate(nodes, dx, dy);
    }

    /**
     * Given a collection of LWComponent's, place the upper left hand corner of the group at the given location.
     */
    public static void setLocation(List<LWComponent> nodes, Point2D.Float mapLocation) {
        if (nodes.size() == 1) {
            if (nodes.get(0).getParent() == null) nodes.get(0).setLocation(mapLocation);
        } else {
            java.awt.geom.Rectangle2D.Float bounds = LWMap.getBounds(nodes.iterator());
            float dx = mapLocation.x - bounds.x;
            float dy = mapLocation.y - bounds.y;
            translate(nodes, dx, dy);
        }
    }

    private static void translate(Collection<LWComponent> nodes, float dx, float dy) {
        for (LWComponent c : nodes) {
            if (c.getParent() == null) c.translate(dx, dy);
        }
    }

    private void out(String s) {
        Log.debug(s);
    }

    private String readTextFlavor(DataFlavor flavor, Transferable transfer) {
        java.io.Reader reader = null;
        String value = null;
        try {
            reader = flavor.getReaderForText(transfer);
            char buf[] = new char[512];
            int got = reader.read(buf);
            value = new String(buf, 0, got);
            if (DEBUG.DND && DEBUG.META) System.out.println("\t" + Util.tags(value));
            if (reader.read() != -1) System.out.println("[there was more data in the reader]");
        } catch (Exception e) {
            System.err.println("readTextFlavor: " + e);
        }
        return value;
    }

    private static void examineBadFile(File file) {
        examineBadFile(file, true);
    }

    private static void examineBadFile(File file, boolean descend) {
        Log.warn("BAD DROPPED FILE:" + "\n\t      file: " + file + "\n\t      name: " + Util.tags(file.getName()) + "\n\t    exists: false" + "\n\t   canRead: " + file.canRead());
        URI uri = null;
        String nameUTF = null;
        String nameMac = null;
        File fileUTF = null;
        File fileMac = null;
        File uriUTF = null;
        File uriMac = null;
        try {
            uri = file.toURI();
            nameUTF = java.net.URLEncoder.encode(file.getName(), "UTF-8");
            nameMac = java.net.URLEncoder.encode(file.getName(), "MacRoman");
            fileUTF = new File(file.getParent(), nameUTF);
            fileMac = new File(file.getParent(), nameMac);
            URI utf, mac;
            utf = new URI("file://" + file.getParent() + File.separator + nameUTF);
            mac = new URI("file://" + file.getParent() + File.separator + nameMac);
            Log.debug("URIUTF " + utf);
            Log.debug("URIMac " + mac);
            uriUTF = new File(utf);
            uriMac = new File(mac);
            URL url = new URL(utf.toString());
            Log.debug("URL: " + url);
            URLConnection c = url.openConnection();
            Log.debug("URL-CONTENT: " + Util.tags(c.getContent()));
        } catch (Throwable t) {
            Log.error("meta-debug", t);
        }
        Log.warn("BAD DROPPED FILE ANALYSIS:" + "\n\t     toURI: " + uri + "\n\t    asUTF8: " + Util.tags(nameUTF) + "\n\tasMacRoman: " + Util.tags(nameMac) + "\n\t   fileUTF: " + fileUTF + "\n\t existsUTF: " + fileUTF.exists() + "\n\t   fileMac: " + fileMac + "\n\t existsMac: " + fileMac.exists() + "\n\t    uriUTF: " + uriUTF + "\n\t existsUTF: " + uriUTF.exists() + "\n\t    uriMac: " + uriMac + "\n\t existsMac: " + uriMac.exists() + "\n\t(probably contains unicode character(s) unhandled by java: this is a java bug)");
    }

    private static final String MIME_TYPE_MAC_URLN = "application/x-mac-ostype-75726c6e";
}
