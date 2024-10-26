package uk.ac.ebi.intact.plugins.reactome;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.model.CvDatabase;
import uk.ac.ebi.intact.model.CvTopic;
import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * TODO comment this!
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: ReactomeExport.java 11323 2008-04-15 15:00:50Z baranda $
 */
public class ReactomeExport {

    private static final String NEW_LINE = System.getProperty("line.separator");

    private static final Log log = LogFactory.getLog(ReactomeExport.class);

    private ReactomeExport() {
    }

    public static List<ReactomeBean> createReactomXrefsFromIntactList() throws ReactomeException {
        CvTopic curatedComplex = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.CURATED_COMPLEX);
        if (curatedComplex == null) {
            throw new IllegalStateException("Could not find CvTopic by shortlabel: " + CvTopic.CURATED_COMPLEX);
        }
        Collection<CvDatabase> databases = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getCvObjectDao(CvDatabase.class).getByXrefLike(CvDatabase.REACTOME_COMPLEX_PSI_REF);
        if (databases == null || databases.isEmpty()) {
            throw new IllegalStateException("Could not find CvDatabase( reactome complex ) by Xref: " + CvDatabase.REACTOME_COMPLEX_PSI_REF);
        }
        CvDatabase reactome = databases.iterator().next();
        Connection connection = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().connection();
        final String sql = "SELECT i.ac as interactionAC, x.primaryId as reactomeID\n" + "FROM ia_interactor i, ia_interactor_xref x, ia_annotation a, ia_int2annot i2a\n" + "WHERE i.objclass LIKE '%Interaction%' AND\n" + "      i.ac = x.parent_ac AND\n" + "      x.database_ac = '" + reactome.getAc() + "' AND\n" + "      i.ac = i2a.interactor_ac AND\n" + "      i2a.annotation_ac = a.ac AND\n" + "      a.topic_ac = '" + curatedComplex.getAc() + "'";
        log.debug("sql = " + sql);
        QueryRunner queryRunner = new QueryRunner();
        ResultSetHandler handler = new BeanListHandler(ReactomeBean.class);
        List<ReactomeBean> xrefsFromIntact = null;
        try {
            xrefsFromIntact = (List) queryRunner.query(connection, sql, handler);
        } catch (SQLException e) {
            throw new ReactomeException(e);
        }
        return xrefsFromIntact;
    }

    public static void exportToReactomeFile(List<ReactomeBean> reactomeBeans, File outputFile) throws ReactomeException {
        if (reactomeBeans == null) {
            throw new ReactomeException("Could not retreive any data about Reactome Xrefs in IntAct.");
        }
        log.debug("IntAct maintain " + reactomeBeans.size() + " Xref" + (reactomeBeans.size() > 1 ? "s" : "") + " to Reactome.");
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
            log.debug(reactomeBeans.size() + " Reactome xref" + (reactomeBeans.size() > 1 ? "s" : "") + " found.");
            for (ReactomeBean reactomeBean : reactomeBeans) {
                out.write(reactomeBean.toSingleLine());
                out.write(NEW_LINE);
            }
            out.close();
            log.debug("File closed.");
        } catch (IOException e) {
            throw new ReactomeException("Problems creating file", e);
        }
    }

    public static ReactomeValidationReport areXrefsFromIntactValid(List<ReactomeBean> xrefsFromIntact, URL url) throws ReactomeException {
        if (url != null) {
            throw new NullPointerException("url cannot be null");
        }
        List<ReactomeBean> xrefsFromReactome = null;
        try {
            xrefsFromReactome = getUrlData(url);
        } catch (IOException e) {
            throw new ReactomeException("Problem getting reactome list from URL: " + url, e);
        }
        log.debug("Reactome maintain " + xrefsFromReactome.size() + " Xref" + (xrefsFromReactome.size() > 1 ? "s" : "") + " to IntAct.");
        Collection<String> reactomeIDs = CollectionUtils.subtract(getReactomeIDs(xrefsFromIntact), getReactomeIDs(xrefsFromReactome));
        ReactomeValidationReport report = new ReactomeValidationReport();
        report.setNonExistingReactomeIdsInIntact(reactomeIDs);
        if (log.isErrorEnabled() && !reactomeIDs.isEmpty()) {
            log.error("We have found " + reactomeIDs.size() + " Reactome ID that are used in IntAct but not existing in Reactome anymore.");
            for (String reactomeId : reactomeIDs) {
                log.error(reactomeId);
            }
        } else {
            log.debug("All Reactome Xref maintained in IntAct are Live in Reactome.");
        }
        Collection<String> intactIDs = CollectionUtils.subtract(getIntactIDs(xrefsFromReactome), getIntactIDs(xrefsFromIntact));
        report.setNonExistingIntactAcsInReactome(intactIDs);
        if (log.isErrorEnabled() && !intactIDs.isEmpty()) {
            log.error("We have found " + intactIDs.size() + " Intact Interaction AC that are used in Reactome but not existing in IntAct anymore.");
            for (String reactomeId : intactIDs) {
                log.error(reactomeId);
            }
        } else {
            log.debug("All Reactome Xref maintained in Reactome are Live in IntAct.");
        }
        return report;
    }

    /**
     * Retreives web content from a URL.
     *
     * @param url the URL we want to download data from.
     *
     * @return the content as a String.
     */
    public static List<ReactomeBean> getUrlData(URL url) throws IOException {
        List<ReactomeBean> beans = new ArrayList<ReactomeBean>(256);
        log.debug("Retreiving content for: " + url);
        StringBuffer content = new StringBuffer(4096);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String str;
        while ((str = in.readLine()) != null) {
            if (str.startsWith("#")) {
                continue;
            }
            StringTokenizer stringTokenizer = new StringTokenizer(str, "\t");
            String InteractionAc = stringTokenizer.nextToken();
            String reactomeId = stringTokenizer.nextToken();
            ReactomeBean reactomeBean = new ReactomeBean();
            reactomeBean.setReactomeID(reactomeId);
            reactomeBean.setInteractionAC(InteractionAc);
            beans.add(reactomeBean);
        }
        in.close();
        return beans;
    }

    /**
     * Extract IntAct IDs from from the given list.
     *
     * @param xrefsFromIntact the given list of Xrefs
     *
     * @return a new instance of List containing all IntAct IDs.
     */
    private static List<String> getIntactIDs(List<ReactomeBean> xrefsFromIntact) {
        List ids = new ArrayList(xrefsFromIntact.size());
        for (ReactomeBean reactomeBean : xrefsFromIntact) {
            ids.add(reactomeBean.getInteractionAC());
        }
        return ids;
    }

    /**
     * Extract reactome IDs from from the given list.
     *
     * @param reactomeBeans the given list of Xrefs
     *
     * @return a new instance of List containing all reactome IDs.
     */
    private static List<String> getReactomeIDs(List<ReactomeBean> reactomeBeans) {
        List ids = new ArrayList(reactomeBeans.size());
        for (ReactomeBean reactomeBean : reactomeBeans) {
            ids.add(reactomeBean.getReactomeID());
        }
        return ids;
    }
}
