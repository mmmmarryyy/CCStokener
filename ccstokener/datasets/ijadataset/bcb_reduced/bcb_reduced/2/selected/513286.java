package iwork.icrafter.services.devices;

import iwork.icrafter.system.*;
import iwork.icrafter.util.*;
import iwork.eheap2.*;
import java.io.*;
import java.net.*;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.print.event.*;

public class PrinterServiceImpl extends ICrafterService implements PrinterService {

    PrintService printer = null;

    EventHeap eh = null;

    public void init() throws ICrafterException {
        System.out.println("Initting service ..");
        printer = PrintServiceLookup.lookupDefaultPrintService();
        System.out.println("Obtained handle to default print service ..");
        eh = new EventHeap(getInitParameter("heapMachine"));
        System.out.println("Obtained handle to EventHeap ..");
    }

    public int print(String type, String url) throws PrinterException {
        return print(type, url, null);
    }

    public int print(String type, RData rdata) throws PrinterException {
        return print(type, rdata, null);
    }

    public int print(String type, String url, String attrs) throws PrinterException {
        try {
            return print(type, (new URL(url)).openStream(), attrs);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PrinterException(e);
        }
    }

    public int print(String type, RData printData, String attrs) throws PrinterException {
        try {
            return print(type, printData.getDataStream(), attrs);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PrinterException(e);
        }
    }

    private int print(String type, InputStream is, String attrs) throws PrinterException {
        try {
            PrintRequestAttributeSet aset = null;
            if (attrs == null) aset = new HashPrintRequestAttributeSet(); else aset = convertAttributes(attrs);
            DocPrintJob printJob = printer.createPrintJob();
            Doc doc = new SimpleDoc(is, getDocFlavor(type), null);
            int jobid = (int) (Math.random() * Integer.MAX_VALUE);
            printJob.print(doc, aset);
            PrintJobAdapter listener = new PrintListener(getName(), eh, jobid);
            printJob.addPrintJobListener(listener);
            return jobid;
        } catch (Exception e) {
            e.printStackTrace();
            throw new PrinterException(e);
        }
    }

    private DocFlavor getDocFlavor(String type) {
        DocFlavor flav = null;
        if (type.equalsIgnoreCase("text/plain")) {
            flav = DocFlavor.INPUT_STREAM.TEXT_PLAIN_US_ASCII;
        } else if (type.equalsIgnoreCase("text/html")) {
            flav = DocFlavor.INPUT_STREAM.TEXT_HTML_US_ASCII;
        } else if (type.equalsIgnoreCase("image/gif")) {
            flav = DocFlavor.INPUT_STREAM.GIF;
        } else if (type.equalsIgnoreCase("image/jpeg")) {
            flav = DocFlavor.INPUT_STREAM.JPEG;
        } else if (type.equalsIgnoreCase("application/vnd.hp-PCL")) {
            flav = DocFlavor.INPUT_STREAM.PCL;
        } else if (type.equalsIgnoreCase("application/pdf")) {
            flav = DocFlavor.INPUT_STREAM.PDF;
        } else if (type.equalsIgnoreCase("application/postscript")) {
            flav = DocFlavor.INPUT_STREAM.POSTSCRIPT;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
        return flav;
    }

    private PrintRequestAttributeSet convertAttributes(String attrs) throws IllegalAccessException, NoSuchFieldException {
        PrinterAttributes pattrs = new PrinterAttributes();
        XMLSerializer.fromXML(pattrs, attrs);
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        if (pattrs.chromaticity == 2) {
            aset.add(Chromaticity.COLOR);
        } else {
            aset.add(Chromaticity.MONOCHROME);
        }
        aset.add(new Copies(pattrs.copies));
        if (pattrs.fidelity == 2) {
            aset.add(Fidelity.FIDELITY_FALSE);
        } else {
            aset.add(Fidelity.FIDELITY_TRUE);
        }
        if (pattrs.sides == 2) {
            aset.add(Sides.ONE_SIDED);
        } else {
            aset.add(Sides.DUPLEX);
        }
        if (pattrs.media == 2) {
            aset.add(MediaName.ISO_A4_TRANSPARENT);
        } else if (pattrs.media == 3) {
            aset.add(MediaName.NA_LETTER_WHITE);
        } else {
            aset.add(MediaName.ISO_A4_WHITE);
        }
        if (pattrs.orientation == 2) {
            aset.add(OrientationRequested.PORTRAIT);
        } else {
            aset.add(OrientationRequested.LANDSCAPE);
        }
        return aset;
    }

    public static void main(String[] args) {
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
