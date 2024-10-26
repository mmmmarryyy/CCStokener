package au.edu.uq.itee.eresearch.dimer.core.util;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PubmedDownloader {

    public static class Result {

        public String title;

        public String abstractText;

        public List<String> authors;

        public String journalTitle;

        public String journalVolume;

        public String journalIssue;

        public String journalPubDate;

        public String bookTitle;

        public String bookPublisher;

        public String bookPubDate;
    }

    public static Result download(String pubmedID) throws Exception {
        InputStream inputStream = null;
        try {
            URL url = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id=" + pubmedID);
            inputStream = new BufferedInputStream(url.openStream());
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            Element root = document.getDocumentElement();
            Result result = new Result();
            Element pubmedArticle = getElement(root, "PubmedArticle");
            Element article = getElement(pubmedArticle, "Article");
            result.title = getText(article, "ArticleTitle");
            Element abstractElement = getElement(article, "Abstract");
            if (abstractElement != null) {
                result.abstractText = getText(abstractElement, "AbstractText");
            }
            result.authors = new ArrayList<String>();
            NodeList authors = article.getElementsByTagName("Author");
            for (int i = 0; i < authors.getLength(); i++) {
                Element author = (Element) authors.item(i);
                String lastName = getText(author, "LastName");
                String initials = getText(author, "Initials");
                String collectiveName = getText(author, "CollectiveName");
                if ((lastName != null) && (initials != null)) {
                    result.authors.add(lastName + " " + initials);
                } else if (collectiveName != null) {
                    result.authors.add(collectiveName);
                }
            }
            Element journal = getElement(article, "Journal");
            Element book = getElement(article, "Book");
            if (journal != null) {
                result.journalTitle = getText(journal, "Title");
                Element journalIssue = getElement(journal, "JournalIssue");
                if (journalIssue != null) {
                    result.journalVolume = getText(journalIssue, "Volume");
                    result.journalIssue = getText(journalIssue, "Issue");
                    result.journalPubDate = parsePubDate(journalIssue, "PubDate");
                }
            } else if (book != null) {
                result.bookTitle = getText(book, "Title");
                result.bookPublisher = getText(book, "Publisher");
                result.bookPubDate = parsePubDate(book, "PubDate");
            }
            return result;
        } catch (Exception exception) {
            throw new Exception("Exception downloading PubMed data", exception);
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
            }
        }
    }

    private static Element getElement(Element parent, String name) {
        NodeList elementList = parent.getElementsByTagName(name);
        return (elementList.getLength() > 0) ? (Element) elementList.item(0) : null;
    }

    private static String getText(Element parent, String name) {
        Element element = getElement(parent, name);
        return (element != null) ? element.getTextContent() : null;
    }

    private static String parsePubDate(Element parent, String name) {
        Element element = getElement(parent, name);
        if (element == null) {
            return null;
        }
        String medlineDate = getText(element, "MedlineDate");
        if (medlineDate != null) {
            return medlineDate;
        }
        String year = getText(element, "Year");
        String month = getText(element, "Month");
        String day = getText(element, "Day");
        String season = getText(element, "Season");
        StringBuffer result = new StringBuffer();
        result.append(year);
        if (season != null) {
            result.append(" ");
            result.append(season);
        } else if (month != null) {
            result.append(" ");
            result.append(month);
            if (day != null) {
                result.append(" ");
                result.append(day);
            }
        }
        return result.toString();
    }
}
