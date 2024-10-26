package org.lindenb.tinytools;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Collator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * NCBIMailing
 * @author pierre
 *
 */
public class NCBIMailing {

    private NCBIMailing() {
    }

    /**
	 * Author
	 * @author pierre
	 *
	 */
    private static class Author {

        String ArticleTitle = "";

        String journal = "";

        int count = 0;

        String Year = "";

        @SuppressWarnings("unused")
        String Suffix = "";

        String LastName = "";

        String FirstName = "";

        @SuppressWarnings("unused")
        String MiddleName = "";

        @SuppressWarnings("unused")
        String Initials = "";

        String Affiliation = "";

        @Override
        public String toString() {
            return FirstName + " " + LastName;
        }
    }

    /** map mail to author */
    private HashMap<String, Author> mail2author = new HashMap<String, Author>();

    /** max number or pubmed entries to return */
    private int max_return = 500;

    /** are we debugging */
    private boolean debugging = false;

    /**
	 * got some problem with the DTD of the NCBI. This reader ignores the second line
	 * of the returned XML
	 * @author pierre
	 *
	 */
    private class IgnoreLine2 extends Reader {

        Reader delegate;

        boolean found = false;

        IgnoreLine2(Reader delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            int c = this.delegate.read();
            if (c == -1) return c;
            if (c == '\n' && !found) {
                while ((c = this.delegate.read()) != -1) {
                    if (c == '\n') break;
                }
                found = true;
                return this.read();
            }
            return c;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (found) return this.delegate.read(cbuf, off, len);
            int i = 0;
            while (i < len) {
                int c = read();
                if (c == -1) return (i == 0 ? -1 : i);
                cbuf[off + i] = (char) c;
                ++i;
            }
            return i;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    /** parse the &lt;Author&gt; tag */
    private Author parseAuthor(XMLEventReader reader) throws IOException, XMLStreamException {
        XMLEvent evt;
        Author author = new Author();
        while (!(evt = reader.nextEvent()).isEndDocument()) {
            if (evt.isEndElement()) {
                return author;
            }
            if (!evt.isStartElement()) continue;
            String tag = evt.asStartElement().getName().getLocalPart();
            String content = reader.getElementText().trim();
            if (tag.equals("LastName")) {
                author.LastName = content;
            } else if (tag.equals("FirstName") || tag.equals("ForeName")) {
                author.FirstName = content;
            } else if (tag.equals("Initials")) {
                author.Initials = content;
            } else if (tag.equals("MiddleName")) {
                author.MiddleName = content;
            } else if (tag.equals("CollectiveName")) {
                return null;
            } else if (tag.equals("Suffix")) {
                author.Suffix = content;
            } else {
                debug("###ignoring " + tag + "=" + content);
            }
        }
        throw new IOException("Cannot parse Author");
    }

    /** add an author */
    private void addAuthor(String mail, Author a) {
        for (String m : this.mail2author.keySet()) {
            Author p = this.mail2author.get(m);
            if (p.FirstName.equals(a.FirstName) && p.LastName.equals(a.LastName)) {
                return;
            }
        }
        Author prev = this.mail2author.get(mail);
        if (prev == null || (prev != null && prev.FirstName.length() < a.FirstName.length())) {
            a.count += 1 + (prev == null ? 0 : prev.count);
            this.mail2author.put(mail, a);
        }
    }

    /** creates a new reader */
    private XMLEventReader newReader(URL url) throws IOException {
        debug(url);
        XMLInputFactory f = XMLInputFactory.newInstance();
        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
        f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
        f.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        XMLEventReader reader = null;
        try {
            reader = f.createXMLEventReader(new IgnoreLine2(new InputStreamReader(url.openStream())));
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
        return reader;
    }

    /** do our stuff */
    private void mail(String term) throws IOException, XMLStreamException {
        URL url = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term=" + URLEncoder.encode(term, "UTF-8") + "&retstart=0&retmax=" + max_return + "&usehistory=y&retmode=xml&email=plindenbaum_at_yahoo.fr&tool=mail");
        XMLEventReader reader = newReader(url);
        XMLEvent evt;
        String QueryKey = null;
        String WebEnv = null;
        int countId = 0;
        while (!(evt = reader.nextEvent()).isEndDocument()) {
            if (!evt.isStartElement()) continue;
            String tag = evt.asStartElement().getName().getLocalPart();
            if (tag.equals("QueryKey")) {
                QueryKey = reader.getElementText().trim();
            } else if (tag.equals("WebEnv")) {
                WebEnv = reader.getElementText().trim();
            } else if (tag.equals("Id")) {
                ++countId;
            }
        }
        reader.close();
        if (countId == 0) return;
        url = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&WebEnv=" + URLEncoder.encode(WebEnv, "UTF-8") + "&query_key=" + URLEncoder.encode(QueryKey, "UTF-8") + "&retmode=xml&retmax=" + max_return + "&email=plindenbaum_at_yahoo.fr&tool=mail");
        Collator collator = Collator.getInstance(Locale.FRANCE);
        collator.setStrength(Collator.PRIMARY);
        reader = newReader(url);
        String Affiliation = null;
        String Year = null;
        String ArticleTitle = null;
        String Journal = null;
        int count = 0;
        int countArticles = 0;
        this.mail2author = new HashMap<String, Author>(max_return);
        Vector<Author> authors = new Vector<Author>();
        while (!(evt = reader.nextEvent()).isEndDocument()) {
            if (evt.isStartElement()) {
                String tag = evt.asStartElement().getName().getLocalPart();
                if (tag.equals("PubmedArticle")) {
                    debug(++countArticles);
                } else if (tag.equals("Affiliation")) {
                    Affiliation = reader.getElementText().trim();
                } else if (tag.equals("Author")) {
                    Author author = parseAuthor(reader);
                    if (author != null) {
                        authors.addElement(author);
                    }
                } else if (tag.equals("ArticleTitle")) {
                    ArticleTitle = reader.getElementText().trim();
                } else if (tag.equals("MedlineTA")) {
                    Journal = reader.getElementText().trim();
                } else if (tag.equals("Year") && Year == null) {
                    Year = reader.getElementText().trim();
                }
            } else if (evt.isEndElement()) {
                String tag = evt.asEndElement().getName().getLocalPart();
                if (tag.equals("PubmedArticle")) {
                    if (Affiliation != null) {
                        Affiliation = Affiliation.replaceAll("\\([ ]*at[ ]*\\)", "@");
                    }
                    if (!authors.isEmpty() && Affiliation != null && Affiliation.indexOf('@') != -1) {
                        for (String mail : Affiliation.split("[ \t\\:\\<,\\>\\(\\)]")) {
                            mail.replaceAll("\\{\\}", "");
                            if (mail.endsWith(".")) mail = mail.substring(0, mail.length() - 1);
                            int index = mail.indexOf('@');
                            if (index == -1) continue;
                            String mailPrefix = mail.substring(0, index).toLowerCase();
                            boolean found = false;
                            for (Author a : authors) {
                                if (mailPrefix.contains(a.LastName.toLowerCase()) || collator.compare(mailPrefix, a.LastName) == 0) {
                                    ++count;
                                    a.journal = Journal;
                                    a.ArticleTitle = ArticleTitle;
                                    a.Year = Year;
                                    a.Affiliation = Affiliation;
                                    addAuthor(mail.toLowerCase(), a);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                for (Author a : authors) {
                                    if (a.FirstName.length() > 1 && (mailPrefix.contains(a.FirstName.toLowerCase()) || collator.compare(mailPrefix, a.FirstName) == 0)) {
                                        ++count;
                                        a.journal = Journal;
                                        a.ArticleTitle = ArticleTitle;
                                        a.Affiliation = Affiliation;
                                        a.Year = Year;
                                        addAuthor(mail.toLowerCase(), a);
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            if (found) break;
                            debug("\nFailed to find author:\nMail:" + mail + "\nAffiliation: " + Affiliation + "\nAuthors: " + authors.toString() + "\n");
                        }
                    }
                    Affiliation = null;
                    ArticleTitle = null;
                    Year = null;
                    Journal = null;
                    authors.clear();
                }
            }
        }
        reader.close();
        for (String s : mail2author.keySet()) {
            Author a = mail2author.get(s);
            System.out.println(a.FirstName + "\t" + a.LastName + "\t" + s + "\t" + (a.Year == null || a.Year.equals("") ? "" : "(" + a.Year + ")") + "\t" + a.journal + "\t" + a.ArticleTitle + "\t" + a.Affiliation);
        }
        debug(count + " " + mail2author.size());
    }

    private void debug(Object o) {
        if (!debugging) return;
        System.err.println(o);
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        NCBIMailing prg = new NCBIMailing();
        int optind = 0;
        while (optind < args.length) {
            if (args[optind].equals("-n")) {
                prg.max_return = Integer.parseInt(args[++optind]);
            } else if (args[optind].equals("-d")) {
                prg.debugging = !prg.debugging;
            } else if (args[optind].equals("--")) {
                ++optind;
                break;
            } else if (args[optind].startsWith("-")) {
                System.err.print("Unknown option :" + args[optind]);
                return;
            } else {
                break;
            }
            ++optind;
        }
        if (optind + 1 != args.length) {
            System.err.print("Usage:\n\t{-n max-return } {-d} \"Valid Term\"\n-d is for debugging\n");
            return;
        }
        try {
            prg.mail(args[optind]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
