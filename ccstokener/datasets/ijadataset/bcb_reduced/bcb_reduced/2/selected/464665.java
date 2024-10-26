package blastRobot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.biojava.bio.Annotation;
import org.biojava.bio.program.sax.blastxml.BlastXMLParserFacade;
import org.biojava.bio.program.ssbind.BlastLikeSearchBuilder;
import org.biojava.bio.program.ssbind.SeqSimilarityAdapter;
import org.biojava.bio.search.SearchContentHandler;
import org.biojava.bio.search.SeqSimilaritySearchHit;
import org.biojava.bio.search.SeqSimilaritySearchResult;
import org.biojava.bio.seq.db.DummySequenceDB;
import org.biojava.bio.seq.db.DummySequenceDBInstallation;
import org.biojava.bio.symbol.SymbolList;
import org.xml.sax.InputSource;

public class BlastSearch {

    private SymbolList sequence;

    private String database = "";

    private String program = "";

    private String rid = "";

    private List results = new ArrayList();

    public BlastSearch() {
    }

    public BlastSearch(SymbolList sequence) throws Exception {
        this.sequence = sequence;
    }

    public BlastSearch(SymbolList sequence, String database, String program) throws Exception {
        this.sequence = sequence;
        this.database = database;
        this.program = program;
    }

    public SymbolList getSequence() {
        return sequence;
    }

    public void setSequence(SymbolList sequence) {
        this.sequence = sequence;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getRID() {
        return rid;
    }

    public void setRID(String rid) {
        this.rid = rid;
    }

    public void search() throws Exception {
        URL searchurl = new URL("" + "http://www.ncbi.nlm.nih.gov/blast/Blast.cgi" + "?CMD=Put" + "&DATABASE=" + this.database + "&PROGRAM=" + this.program + "&QUERY=" + this.sequence.seqString());
        BufferedReader reader = new BufferedReader(new InputStreamReader(searchurl.openStream(), "UTF-8"));
        String line = "";
        while ((line = reader.readLine()) != null) {
            if (line.contains("Request ID")) this.rid += line.substring(70, 81);
        }
        reader.close();
    }

    public void parseResult() throws Exception {
        URL searchurl = new URL("" + "http://www.ncbi.nlm.nih.gov/blast/Blast.cgi" + "?CMD=Get" + "&FORMAT_TYPE=XML" + "&RID=" + this.rid);
        BlastXMLParserFacade blast = new BlastXMLParserFacade();
        SeqSimilarityAdapter adapter = new SeqSimilarityAdapter();
        blast.setContentHandler(adapter);
        SearchContentHandler builder = new BlastLikeSearchBuilder(results, new DummySequenceDB("queries"), new DummySequenceDBInstallation());
        adapter.setSearchContentHandler(builder);
        blast.parse(new InputSource(searchurl.openStream()));
    }

    public String waveOutput() {
        String waveOutput = "\nBlast-Result:\n";
        for (Iterator i = results.iterator(); i.hasNext(); ) {
            SeqSimilaritySearchResult result = (SeqSimilaritySearchResult) i.next();
            Annotation anno = result.getAnnotation();
            for (Iterator j = anno.keys().iterator(); j.hasNext(); ) {
                Object key = j.next();
                Object property = anno.getProperty(key);
                waveOutput += (key + " : " + property) + "\n";
            }
            waveOutput += ("\nHits: \n");
            for (Iterator k = result.getHits().iterator(); k.hasNext(); ) {
                SeqSimilaritySearchHit hit = (SeqSimilaritySearchHit) k.next();
                waveOutput += ("\t match: " + hit.getSubjectID());
                waveOutput += ("\t\t\t" + hit.getEValue()) + "\n";
            }
        }
        return waveOutput;
    }
}
