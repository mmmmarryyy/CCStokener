package gnu.javax.print.ipp;

import gnu.javax.print.PrintFlavorException;
import gnu.javax.print.ipp.attribute.job.JobId;
import gnu.javax.print.ipp.attribute.job.JobUri;
import gnu.javax.print.ipp.attribute.printer.DocumentFormat;
import gnu.javax.print.ipp.attribute.supported.OperationsSupported;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.print.CancelablePrintJob;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.HashPrintJobAttributeSet;
import javax.print.attribute.PrintJobAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.PrinterURI;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.event.PrintJobAttributeListener;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;

/**
 * Implementation of the DocPrintJob interface. Implementation is 
 * specific to the <code>IppPrintService</code> implementation.
 * 
 * @author Wolfgang Baer (WBaer@gmx.de)
 */
public class DocPrintJobImpl implements CancelablePrintJob {

    /** The print service this job is bound to. */
    private IppPrintService service;

    /** The set of print job listeners. */
    private HashSet printJobListener = new HashSet();

    /** The print job attributes listeners. */
    private ArrayList attributesListener = new ArrayList();

    /** The print job attributes listeners associated attribute set. */
    private ArrayList attributesListenerAttributes = new ArrayList();

    /** The username. */
    private String username;

    /** The password of the user. */
    private String password;

    /** Returned job uri. */
    private JobUri jobUri = null;

    /** Returned job id. */
    private JobId jobId = null;

    /** The requesting-username for later canceling */
    private RequestingUserName requestingUser;

    /** The print job sets. */
    private PrintJobAttributeSet oldSet = new HashPrintJobAttributeSet();

    private PrintJobAttributeSet currentSet = new HashPrintJobAttributeSet();

    /** 
   * State variable if we already started printing.
   */
    private boolean printing = false;

    /**
   * Constructs a DocPrintJobImpl instance bound to the given print service.
   * 
   * @param service the print service instance.
   * @param user the user of this print service.
   * @param passwd the password of the user.
   */
    public DocPrintJobImpl(IppPrintService service, String user, String passwd) {
        this.service = service;
        username = user;
        password = passwd;
    }

    /**
   * @see DocPrintJob#addPrintJobAttributeListener(PrintJobAttributeListener, PrintJobAttributeSet)
   */
    public void addPrintJobAttributeListener(PrintJobAttributeListener listener, PrintJobAttributeSet attributes) {
        if (listener == null) return;
        attributesListener.add(listener);
        attributesListenerAttributes.add(attributes);
    }

    /**
   * @see DocPrintJob#addPrintJobListener(PrintJobListener)
   */
    public void addPrintJobListener(PrintJobListener listener) {
        if (listener == null) return;
        printJobListener.add(listener);
    }

    /**
   * @see javax.print.DocPrintJob#getAttributes()
   */
    public PrintJobAttributeSet getAttributes() {
        return AttributeSetUtilities.unmodifiableView(currentSet);
    }

    /**
   * @see javax.print.DocPrintJob#getPrintService()
   */
    public PrintService getPrintService() {
        return service;
    }

    /**
   * @see DocPrintJob#print(Doc, PrintRequestAttributeSet)
   */
    public void print(Doc doc, PrintRequestAttributeSet attributes) throws PrintException {
        if (printing) throw new PrintException("already printing");
        printing = true;
        DocAttributeSet docAtts = doc.getAttributes();
        DocFlavor flavor = doc.getDocFlavor();
        if (flavor == null || (!service.isDocFlavorSupported(flavor))) {
            notifyPrintJobListeners(new PrintJobEvent(this, PrintJobEvent.JOB_FAILED));
            throw new PrintFlavorException("Invalid flavor", new DocFlavor[] { flavor });
        }
        HashAttributeSet mergedAtts = new HashAttributeSet();
        if (attributes != null) mergedAtts.addAll(attributes);
        if (docAtts != null) mergedAtts.addAll(docAtts);
        if (!mergedAtts.containsKey(RequestingUserName.class)) {
            mergedAtts.add(IppPrintService.REQUESTING_USER_NAME);
            requestingUser = IppPrintService.REQUESTING_USER_NAME;
        } else {
            requestingUser = (RequestingUserName) mergedAtts.get(RequestingUserName.class);
        }
        if (!mergedAtts.containsKey(JobName.class)) mergedAtts.add(IppPrintService.JOB_NAME);
        IppResponse response = null;
        try {
            PrinterURI printerUri = service.getPrinterURI();
            String printerUriStr = "http" + printerUri.toString().substring(3);
            URI uri = null;
            try {
                uri = new URI(printerUriStr);
            } catch (URISyntaxException e) {
            }
            IppRequest request = new IppRequest(uri, username, password);
            request.setOperationID((short) OperationsSupported.PRINT_JOB.getValue());
            request.setOperationAttributeDefaults();
            request.addOperationAttribute(printerUri);
            if (mergedAtts != null) {
                request.addAndFilterJobOperationAttributes(mergedAtts);
                request.addAndFilterJobTemplateAttributes(mergedAtts);
            }
            DocumentFormat format = DocumentFormat.createDocumentFormat(flavor);
            request.addOperationAttribute(format);
            String className = flavor.getRepresentationClassName();
            if (className.equals("[B")) {
                request.setData((byte[]) doc.getPrintData());
                response = request.send();
            } else if (className.equals("java.io.InputStream")) {
                InputStream stream = (InputStream) doc.getPrintData();
                request.setData(stream);
                response = request.send();
                stream.close();
            } else if (className.equals("[C")) {
                try {
                    String str = new String((char[]) doc.getPrintData());
                    request.setData(str.getBytes("utf-16"));
                    response = request.send();
                } catch (UnsupportedEncodingException e) {
                    notifyPrintJobListeners(new PrintJobEvent(this, PrintJobEvent.JOB_FAILED));
                    throw new PrintFlavorException("Invalid charset of flavor", e, new DocFlavor[] { flavor });
                }
            } else if (className.equals("java.io.Reader")) {
                try {
                    response = request.send();
                    throw new UnsupportedEncodingException("not supported yet");
                } catch (UnsupportedEncodingException e) {
                    notifyPrintJobListeners(new PrintJobEvent(this, PrintJobEvent.JOB_FAILED));
                    throw new PrintFlavorException("Invalid charset of flavor", e, new DocFlavor[] { flavor });
                }
            } else if (className.equals("java.lang.String")) {
                try {
                    String str = (String) doc.getPrintData();
                    request.setData(str.getBytes("utf-16"));
                    response = request.send();
                } catch (UnsupportedEncodingException e) {
                    notifyPrintJobListeners(new PrintJobEvent(this, PrintJobEvent.JOB_FAILED));
                    throw new PrintFlavorException("Invalid charset of flavor", e, new DocFlavor[] { flavor });
                }
            } else if (className.equals("java.net.URL")) {
                URL url = (URL) doc.getPrintData();
                InputStream stream = url.openStream();
                request.setData(stream);
                response = request.send();
                stream.close();
            } else if (className.equals("java.awt.image.renderable.RenderableImage") || className.equals("java.awt.print.Printable") || className.equals("java.awt.print.Pageable")) {
                throw new PrintException("Not yet supported.");
            } else {
                notifyPrintJobListeners(new PrintJobEvent(this, PrintJobEvent.JOB_FAILED));
                throw new PrintFlavorException("Invalid flavor", new DocFlavor[] { flavor });
            }
            notifyPrintJobListeners(new PrintJobEvent(this, PrintJobEvent.DATA_TRANSFER_COMPLETE));
        } catch (IOException e) {
            throw new PrintException("IOException occured.", e);
        }
        int status = response.getStatusCode();
        if (!(status == IppStatusCode.SUCCESSFUL_OK || status == IppStatusCode.SUCCESSFUL_OK_IGNORED_OR_SUBSTITUED_ATTRIBUTES || status == IppStatusCode.SUCCESSFUL_OK_CONFLICTING_ATTRIBUTES)) {
            notifyPrintJobListeners(new PrintJobEvent(this, PrintJobEvent.JOB_FAILED));
            throw new PrintException("Printing failed - received statuscode " + Integer.toHexString(status));
        } else {
            notifyPrintJobListeners(new PrintJobEvent(this, PrintJobEvent.JOB_COMPLETE));
        }
        List jobAtts = response.getJobAttributes();
        Map jobAttributes = (Map) jobAtts.get(0);
        jobUri = (JobUri) ((HashSet) jobAttributes.get(JobUri.class)).toArray()[0];
        jobId = (JobId) ((HashSet) jobAttributes.get(JobId.class)).toArray()[0];
    }

    /**
   * @see DocPrintJob#removePrintJobAttributeListener(PrintJobAttributeListener)
   */
    public void removePrintJobAttributeListener(PrintJobAttributeListener listener) {
        if (listener == null) return;
        int index = attributesListener.indexOf(listener);
        if (index != -1) {
            attributesListener.remove(index);
            attributesListenerAttributes.remove(index);
        }
    }

    /**
   * @see DocPrintJob#removePrintJobListener(PrintJobListener)
   */
    public void removePrintJobListener(PrintJobListener listener) {
        if (listener == null) return;
        printJobListener.remove(listener);
    }

    /**
   * @see CancelablePrintJob#cancel()
   */
    public void cancel() throws PrintException {
        if (jobUri == null) {
            throw new PrintException("print job is not yet send");
        }
        IppResponse response = null;
        try {
            IppRequest request = new IppRequest(jobUri.getURI(), username, password);
            request.setOperationID((short) OperationsSupported.CANCEL_JOB.getValue());
            request.setOperationAttributeDefaults();
            request.addOperationAttribute(jobUri);
            request.addOperationAttribute(requestingUser);
            response = request.send();
        } catch (IOException e) {
            throw new IppException("IOException occured during cancel request.", e);
        }
        int status = response.getStatusCode();
        if (!(status == IppStatusCode.SUCCESSFUL_OK || status == IppStatusCode.SUCCESSFUL_OK_IGNORED_OR_SUBSTITUED_ATTRIBUTES || status == IppStatusCode.SUCCESSFUL_OK_CONFLICTING_ATTRIBUTES)) {
            notifyPrintJobListeners(new PrintJobEvent(this, PrintJobEvent.JOB_FAILED));
            throw new PrintException("Canceling failed - received statuscode " + Integer.toHexString(status));
        } else {
            notifyPrintJobListeners(new PrintJobEvent(this, PrintJobEvent.JOB_CANCELED));
        }
    }

    private void notifyPrintJobListeners(PrintJobEvent e) {
        Iterator it = printJobListener.iterator();
        while (it.hasNext()) {
            PrintJobListener l = (PrintJobListener) it.next();
            if (e.getPrintEventType() == PrintJobEvent.DATA_TRANSFER_COMPLETE) l.printDataTransferCompleted(e); else if (e.getPrintEventType() == PrintJobEvent.JOB_CANCELED) l.printJobCanceled(e); else if (e.getPrintEventType() == PrintJobEvent.JOB_COMPLETE) l.printJobCompleted(e); else if (e.getPrintEventType() == PrintJobEvent.JOB_FAILED) l.printJobFailed(e); else if (e.getPrintEventType() == PrintJobEvent.NO_MORE_EVENTS) l.printJobNoMoreEvents(e); else l.printJobRequiresAttention(e);
        }
    }
}
