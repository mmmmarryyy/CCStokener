package uk.ac.ebi.intact.services.search.dev;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.obo.datamodel.OBOSession;
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.dataexchange.cvutils.CvUpdater;
import uk.ac.ebi.intact.dataexchange.cvutils.CvUpdaterStatistics;
import uk.ac.ebi.intact.dataexchange.cvutils.OboUtils;
import uk.ac.ebi.intact.dataexchange.cvutils.model.IntactOntology;
import uk.ac.ebi.intact.dataexchange.cvutils.model.CvObjectOntologyBuilder;
import uk.ac.ebi.intact.dataexchange.psimi.xml.exchange.PsiExchange;
import java.net.URL;

/**
 * TODO comment this
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: IntactPopulatorBean.java 11974 2008-08-15 09:09:31Z baranda $
 */
public class IntactPopulatorBean implements InitializingBean {

    private static final Log log = LogFactory.getLog(IntactPopulatorBean.class);

    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied
     * (and satisfied BeanFactoryAware and ApplicationContextAware).
     * <p>This method allows the bean instance to perform initialization only
     * possible when all bean properties have been set and to throw an
     * exception in the event of misconfiguration.
     *
     * @throws Exception in the event of misconfiguration (such
     *                   as failure to set an essential property) or if initialization fails.
     */
    public void afterPropertiesSet() throws Exception {
        if (IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getInteractionDao().countAll() > 0) {
            log.info("The database already contains interactions. Won't populate.");
            return;
        }
        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        insertSampleData();
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();
    }

    public void createCVs() throws Exception {
        OBOSession session = OboUtils.createOBOSessionFromLatestMi();
        CvObjectOntologyBuilder builder = new CvObjectOntologyBuilder(session);
        CvUpdater updater = new CvUpdater();
        CvUpdaterStatistics stats = updater.createOrUpdateCVs(builder.getAllCvs());
        log.info("Created terms: " + stats.getCreatedCvs().size());
    }

    public void insertSampleData() throws Exception {
        URL urlToImport = new URL("ftp://ftp.ebi.ac.uk/pub/databases/intact/current/psi25/pmid/2007/10094392.xml");
        URL urlToImport2 = new URL("ftp://ftp.ebi.ac.uk/pub/databases/intact/current/psi25/pmid/2007/10220404.xml");
        PsiExchange.importIntoIntact(urlToImport.openStream());
        PsiExchange.importIntoIntact(urlToImport2.openStream());
    }
}
