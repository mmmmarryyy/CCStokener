package pub.db.command;

import pub.db.*;
import pub.utils.*;
import pub.pubfetch.PubSourceVivifier;
import pub.pubfetch.MedlineMapConverter;
import java.sql.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class AddPubmedArticle {

    private String _pid;

    private int _article_id;

    private int _entered_by;

    private pub.db.PubConnection _conn;

    public AddPubmedArticle() {
        _entered_by = 8;
        _pid = "";
        _article_id = 0;
        _conn = null;
    }

    public void setConnection(pub.db.PubConnection conn) {
        this._conn = conn;
    }

    public void setPubmedId(String id) {
        _pid = id;
    }

    public void setEnteredBy(int id) {
        _entered_by = id;
    }

    public int getArticleId() {
        return _article_id;
    }

    private boolean isArticleAlreadyThere(Map m) {
        return (getArticleIdFromArticleData(m).length() > 0);
    }

    public String getArticleIdFromArticleData(Map m) {
        ArticleTable table = new ArticleTable(_conn);
        return "" + table.getArticleIdFromPubmedId("" + m.get("pubmed_id"));
    }

    private String getPubsource(pubfetch.MedlineMap articleMap) {
        PubSourceVivifier vivifier = new PubSourceVivifier();
        vivifier.setMapToVivify(articleMap);
        vivifier.setConnection(_conn);
        vivifier.setUserId("" + _entered_by);
        vivifier.vivify();
        String id = vivifier.getPubSourceId();
        return id;
    }

    public void execute() throws ArticleAlreadyExists, InsertionFailed, IOException {
        pubfetch.MedlineMap articleMap = readMedlineMapFromSite();
        addArticle(articleMap);
    }

    private pubfetch.MedlineMap readMedlineMapFromSite() throws IOException {
        String pubmed_fetch_url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed";
        pubfetch.PubmedFetcher fetcher = new pubfetch.PubmedFetcher();
        String url = pubmed_fetch_url + "&id=" + _pid + "&retmode=MEDLINE&rettype=MEDLINE";
        pubfetch.MedlineMap amap = null;
        try {
            InputStream is = (new URL(url)).openStream();
            fetcher.extractMapsFromInputStream(is);
            List documents = fetcher.documents();
            amap = (pubfetch.MedlineMap) documents.get(0);
        } catch (pubfetch.FetchError fe) {
            throw new IOException("fetch error for " + url);
        } catch (MalformedURLException me) {
            throw new IOException("malformed url from :" + url);
        } catch (IOException ie) {
            throw new IOException("couldn't read site:" + url);
        }
        return amap;
    }

    private void addArticle(pubfetch.MedlineMap articleMap) {
        String pubsourceId = getPubsource(articleMap);
        AddArticle cmd = new AddArticle(_conn);
        cmd.setTitle(articleMap.getFirst("TI"));
        cmd.setAbstractText(articleMap.getFirst("AB"));
        cmd.setPageStart(articleMap.getFirst("PG"));
        cmd.setYear((articleMap.getYear()));
        cmd.setPubSourceId(pubsourceId);
        cmd.setAuthors(articleMap.getAuthors());
        cmd.setType(articleMap.getType());
        cmd.setIssue(articleMap.getFirst("IP"));
        cmd.setVolume(articleMap.getFirst("VI"));
        cmd.setPubmedId(articleMap.getFirst("PMID"));
        cmd.setAgricolaId(articleMap.getFirst("OID"));
        cmd.setUpdatedBy("" + _entered_by);
        try {
            cmd.execute();
            _article_id = Integer.parseInt(cmd.getArticleId());
        } catch (NumberFormatException ne) {
        }
    }

    private String convertPubmedPublicationType(String ptype) {
        if (StringUtils.isSubstringOf("article", ptype.toLowerCase())) {
            return "research_article";
        } else if (ptype.equalsIgnoreCase("technical report")) {
            return "research_article";
        } else if (StringUtils.isSubstringOf("review", ptype.toLowerCase())) {
            return "review";
        } else {
            return "unknown";
        }
    }

    /**********************************************/
    public static class InsertionFailed extends Exception {

        public InsertionFailed(Exception e) {
            super(e);
        }
    }

    public static class ArticleAlreadyExists extends Exception {

        private int article_id;

        public ArticleAlreadyExists(int article_id) {
            this.article_id = article_id;
        }

        public int getArticleId() {
            return article_id;
        }
    }
}
