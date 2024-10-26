package gsa_snp.tool;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 *
 * @author Jin Kim
 */
public class GeneSetMaker {

    private Map<Integer, Term> map;

    private class Term {

        /** ID in gene ontology */
        private Integer id;

        /** */
        private GeneSet geneSet;

        /** The list of corresponding GOs */
        private List<Integer> correspondents = new ArrayList<Integer>();

        public Term(Integer id, GeneSet geneSet) {
            this.id = id;
            this.geneSet = geneSet;
        }
    }

    GeneSetMaker(String ontologyURL) throws MalformedURLException, IOException {
        URL url = new URL(ontologyURL);
        URLConnection urlc = url.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
        map = new HashMap<Integer, Term>();
        Term term;
        while ((term = getTerm(br)) != null) {
            map.put(term.id, term);
        }
        br.close();
    }

    private Term getTerm(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null && !line.startsWith("[Term]")) {
        }
        if (line == null) {
            return null;
        } else {
            Integer id = Integer.valueOf(br.readLine().trim().substring(7));
            String name = br.readLine().substring(6).trim();
            Term term = new Term(id, new GeneSet(id, name));
            while ((line = br.readLine()) != null && line.length() > 0) {
                if (line.startsWith("is_a: GO:") || line.startsWith("relationship: part_of GO:")) {
                    int startIndex = line.indexOf("GO:") + 3;
                    int endIndex = line.indexOf(" !");
                    Integer i = Integer.parseInt(line.substring(startIndex, endIndex));
                    term.correspondents.add(i);
                }
            }
            return term;
        }
    }

    Collection<GeneSet> run(String annotationURL) throws MalformedURLException, IOException {
        URL url = new URL(annotationURL);
        URLConnection urlc = url.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(new java.util.zip.GZIPInputStream(urlc.getInputStream())));
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.startsWith("!")) {
                String[] records = line.split("\t");
                Integer startId = Integer.valueOf(records[4].substring(3));
                String geneSymbol = records[2];
                ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
                queue.push(startId);
                while (!queue.isEmpty()) {
                    Integer id = queue.pop();
                    Term term = map.get(id);
                    term.geneSet.add(geneSymbol);
                    for (Integer i : term.correspondents) {
                        queue.push(i);
                    }
                }
            }
        }
        ArrayList<GeneSet> list = new ArrayList<GeneSet>(map.size());
        for (Term term : map.values()) {
            list.add(term.geneSet);
        }
        return list;
    }
}
