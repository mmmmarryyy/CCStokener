package edu.uwm.nlp.jude;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import edu.uwm.nlp.jude.internal.CRFDecoder;
import edu.uwm.nlp.jude.internal.DocumentHandlerException;
import edu.uwm.nlp.jude.internal.FileProcessor;
import edu.uwm.nlp.jude.internal.ReferenceEntity;

/**
 * @author qing
 *
 * Feb 26, 2010
 */
public class SinglePDFProcessor {

    private String model = "/home/data_user/citationdata/citationtrain9.crf";

    private CRFDecoder decoder;

    public SinglePDFProcessor() {
        decoder = new CRFDecoder(model);
    }

    public SinglePDFProcessor(String model) {
        this.model = model;
        decoder = new CRFDecoder(model);
    }

    public ArrayList<ReferenceEntity> extractPDF(String urlString) throws IOException, DocumentHandlerException {
        URL url = new URL(urlString);
        InputStream in = new BufferedInputStream(url.openStream());
        FileProcessor fp = new FileProcessor();
        String text = fp.getDocumentText(in);
        ArrayList<ReferenceEntity> rList = fp.getFullCitations(text);
        return rList;
    }

    public ArrayList parseSglCitation(String citation) {
        ArrayList total = decoder.tagField(citation);
        ArrayList result = null;
        if (total.size() > 0) result = (ArrayList) total.get(0);
        return result;
    }

    public static void main(String[] args) {
        SinglePDFProcessor sp = new SinglePDFProcessor();
        String url = "https://pantherfile.uwm.edu/hongyu/www/files/articles/AMIA.QA.2008.pdf";
        String citation = "Calvert, J.A.; Atterbury-Thomas, A.E.; Leon, C.; Forsythe, I.D.; Gachet, C.; Evans, R.J. Evidence for P2Y1, P2Y2, P2Y6 and atypical UTP-sensitive receptors coupled to rises in intracellular calcium in mouse cultured superior cervical ganglion neurons and glia. Br. J. Pharmacol. 2004;143:525�532.";
        try {
            System.out.println(sp.extractPDF(url).toString());
            System.out.println(sp.parseSglCitation(citation).toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentHandlerException e) {
            e.printStackTrace();
        }
    }
}
