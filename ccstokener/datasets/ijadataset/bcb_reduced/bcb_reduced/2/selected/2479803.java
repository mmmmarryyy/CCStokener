package biocreative.genenormalization.experiment;

import java.io.*;
import java.net.*;

public class PubmedRetriever {

    String baseURL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml";

    File outputDir = null;

    public PubmedRetriever(File dir) {
        outputDir = dir;
    }

    public boolean retrieveByPMID(String pmid) {
        try {
            URL url = new URL(baseURL + "&id=" + pmid.trim());
            BufferedReader xml = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            StringBuffer title_sb = new StringBuffer();
            while ((line = xml.readLine()) != null) {
                if (line.indexOf("<ArticleTitle>") != -1) {
                    title_sb.delete(0, title_sb.length());
                    title_sb.append(line.substring(line.indexOf("<ArticleTitle>") + 14, line.length() - 15));
                } else if (line.indexOf("<AbstractText>") != -1) {
                    PrintWriter article = new PrintWriter(new FileWriter(new File(outputDir.getPath() + File.separatorChar + pmid + ".txt")));
                    article.println(title_sb);
                    article.println(line.substring(line.indexOf("<AbstractText>") + 14, line.length() - 15));
                    article.close();
                    break;
                }
            }
            xml.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String args[]) {
        PubmedRetriever retriever = new PubmedRetriever(new File(args[0]));
        try {
            BufferedReader list = new BufferedReader(new FileReader(new File(args[1])));
            String line = null;
            while ((line = list.readLine()) != null) {
                retriever.retrieveByPMID(line.trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
