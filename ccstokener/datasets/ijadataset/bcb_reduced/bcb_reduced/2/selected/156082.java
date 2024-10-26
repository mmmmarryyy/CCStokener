package net.sf.buildbox.buildrobot.webhook;

import net.sf.buildbox.buildrobot.api.*;
import net.sf.buildbox.buildrobot.model.CodeRoot;
import net.sf.buildbox.buildrobot.model.Settings;
import net.sf.buildbox.buildrobot.model.VcsLocation;
import net.sf.buildbox.buildrobot.model.VcsRepository;
import net.sf.buildbox.buildrobot.util.VcsRepositoryHelper;
import net.sf.buildbox.devmodel.commit.CommitEntry;
import net.sf.buildbox.devmodel.commit.CommitPath;
import org.apache.commons.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Workflow:
 * 1. initial chat
 * - client sends empty content
 * - servlet responds with latest available revision
 * 2. cyclic chat
 * - client sends result of "svn log --limit 100 --xml --with-all-revprops --revision X:HEAD" where X is the previously received revision
 * - servlet stores all the data and responds with latest available (NOT highest stored) revision
 * <p/>
 * Parameters:
 */
public class CommitLogServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(CommitLogServlet.class.getName());

    private CommitLogDao commitLogDao;

    private VcsRepositoryDao vcsRepositoryDao;

    private StructureDao structureDao;

    private BuildRobot buildrobot;

    private SettingsDao settingsDao;

    @Autowired
    @Required
    @Qualifier("commitLogDao")
    public void setCommitLogDao(CommitLogDao commitLogDao) {
        this.commitLogDao = commitLogDao;
    }

    @Autowired
    @Required
    @Qualifier("settingsDao")
    public void setSettingsDao(SettingsDao settingsDao) {
        this.settingsDao = settingsDao;
    }

    @Autowired
    @Required
    @Qualifier("vcsRepositoryDao")
    public void setVcsRepositoryDao(VcsRepositoryDao vcsRepositoryDao) {
        this.vcsRepositoryDao = vcsRepositoryDao;
    }

    @Autowired
    @Required
    @Qualifier("structureDao")
    public void setStructureDao(StructureDao structureDao) {
        this.structureDao = structureDao;
    }

    @Autowired
    @Required
    @Qualifier("buildrobot")
    public void setBuildrobot(BuildRobot buildrobot) {
        this.buildrobot = buildrobot;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        LOGGER.info("CommitLogServlet.init:");
        final WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
        final AutowireCapableBeanFactory beanFactory = ctx.getAutowireCapableBeanFactory();
        LOGGER.fine("CommitLogServlet.-aw:");
        beanFactory.autowireBean(this);
        LOGGER.fine("CommitLogServlet.-aw.");
        for (String vcsId : vcsRepositoryDao.listVcsIds(null)) {
            LOGGER.finer("found VcsRepository: " + vcsId);
        }
        LOGGER.info("CommitLogServlet.init.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String vcsId = request.getPathInfo().substring(1);
        VcsRepository vcsRepository = vcsRepositoryDao.getRepository(vcsId);
        if (vcsRepository == null) {
            final String vcsDomainId = VcsLocation.domainPart(vcsId);
            final Settings domainSettings = settingsDao.getSettings(SettingsDao.VCS_DOMAIN_ROOT + vcsDomainId);
            if (domainSettings == null) {
                LOGGER.warning(vcsId + ": no configured domain '" + vcsDomainId + "' found, cannot add ad-hoc repository");
            } else {
                final boolean canCreateReposOnDemand = "false".equals(domainSettings.getProperty("canCreateAdhocRepos"));
                if (canCreateReposOnDemand) {
                    vcsRepository = VcsRepositoryHelper.create(vcsId, domainSettings.getProperties());
                    LOGGER.info("creating ad-hoc repository: " + vcsId);
                    vcsRepositoryDao.saveRepository(vcsRepository);
                }
            }
        }
        if (vcsRepository == null) {
            response.sendError(404, "Unknown VcsRepository: " + vcsId);
            throw new ServletException("Unknown VcsRepository: " + vcsId);
        }
        try {
            final PrintWriter pw = response.getWriter();
            printVcsRepositoryInfo(pw, vcsRepository);
            printNewestCommitInfo(pw, getTargetVcsId(vcsRepository));
            pw.flush();
        } catch (Exception e) {
            response.sendError(500, e.getClass().getName() + ": " + e.getMessage());
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPut(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final PrintWriter pw = response.getWriter();
        try {
            final String vcsId = request.getPathInfo().substring(1);
            final VcsRepository vcsRepository = vcsRepositoryDao.getRepository(vcsId);
            if (vcsRepository == null) {
                throw new IllegalArgumentException("Unknown VcsRepository: " + vcsId);
            }
            printVcsRepositoryInfo(pw, vcsRepository);
            final ParsedFormParams parsedFormParams = ParsedFormParams.parse(request);
            final String targetVcsId = getTargetVcsId(vcsRepository);
            if (targetVcsId != null) {
                final List<CommitEntry> commits = getCommits(parsedFormParams, targetVcsId);
                processCommits(targetVcsId, commits);
                printNewestCommitInfo(pw, targetVcsId);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            pw.flush();
        }
    }

    private String getTargetVcsId(VcsRepository vcsRepository) {
        final String mirrorOf = vcsRepository.getProperty("mirrorOf");
        final String repoId = vcsRepository.getVcsId();
        if (mirrorOf == null) {
            return repoId;
        }
        final VcsRepository targetVcsRepository = vcsRepositoryDao.getRepository(mirrorOf);
        if (targetVcsRepository == null) {
            LOGGER.severe(mirrorOf + ": no such target repository for " + vcsRepository);
            return null;
        }
        final String targetsPreferredMirror = targetVcsRepository.getProperty("preferredMirror");
        if (targetsPreferredMirror == null) {
            LOGGER.warning("No mirror is selected as preferred for " + vcsRepository);
            return repoId;
        }
        if (targetsPreferredMirror.equals(repoId)) {
            return mirrorOf;
        }
        return null;
    }

    private Set<String> affectedPathsToProjectPaths(String vcsId, Set<String> affectedPathSet) {
        final Set<String> affectedPaths = new HashSet<String>(affectedPathSet);
        final Set<String> allProjectPaths = structureDao.listCodeRootPathsFor(vcsId);
        final Set<String> projectPaths = new HashSet<String>();
        int unmatchedCnt = 0;
        while (!affectedPaths.isEmpty()) {
            String prefix = null;
            for (Iterator<String> iterator = affectedPaths.iterator(); iterator.hasNext(); ) {
                final String affectedPath = iterator.next();
                if (prefix == null) {
                    final String matchingProjectPath = findMatchingProjectPath(allProjectPaths, affectedPath);
                    if (matchingProjectPath != null) {
                        prefix = matchingProjectPath;
                        if (projectPaths.add(matchingProjectPath)) {
                            LOGGER.info("project: " + prefix);
                        }
                    } else {
                        unmatchedCnt++;
                    }
                    iterator.remove();
                } else if (affectedPath.startsWith(prefix + "/") || affectedPath.equals(prefix)) {
                    iterator.remove();
                }
            }
        }
        if (unmatchedCnt > 0) {
            LOGGER.fine("unmatched " + unmatchedCnt + " paths in " + vcsId);
        }
        return projectPaths;
    }

    private static String findMatchingProjectPath(Set<String> allProjectPaths, String affectedPath) {
        for (String projectPath : allProjectPaths) {
            if (affectedPath.startsWith(projectPath + "/")) return projectPath;
            if (affectedPath.equals(projectPath)) return projectPath;
        }
        return null;
    }

    private void processCommits(String targetVcsId, List<CommitEntry> commits) throws IOException, FileUploadException {
        final Set<String> affectedPaths = new HashSet<String>();
        if (commits != null && !commits.isEmpty()) {
            log(targetVcsId + " - processing " + commits.size() + " commits");
            int cntCreated = 0;
            int cntUpdated = 0;
            CommitEntry newestPosted = null;
            for (CommitEntry commit : commits) {
                log(String.format("* %tFT%<tT.%<tL %s::%s - %s - %s", commit.getTimestamp(), targetVcsId, commit.getRevision(), commit.getAuthor(), commit.getMessage().trim()));
                final boolean exists = commitLogDao.getCommit(commit.getVcsId(), commit.getRevision()) != null;
                if (exists) {
                    cntUpdated++;
                } else {
                    cntCreated++;
                }
                for (CommitPath commitPath : commit.getPaths()) {
                    affectedPaths.add(commitPath.getPath());
                }
                if (newestPosted == null || commit.getTimestamp().after(newestPosted.getTimestamp())) {
                    newestPosted = commit;
                }
            }
            LOGGER.finer(String.format("Saving %d commits for %s", commits.size(), targetVcsId));
            commitLogDao.saveCommits(commits);
            final CommitEntry newestCommit = commitLogDao.getLastCommit(VcsLocation.create(targetVcsId, ""));
            final VcsRepository targetVcsRepository = vcsRepositoryDao.getRepository(targetVcsId);
            final int limit = Integer.valueOf(targetVcsRepository.getProperty("limit", "100"));
            final boolean isRecent = cntCreated > 0 && cntCreated < limit && cntUpdated == 0 && newestCommit.equals(newestPosted);
            if (isRecent) {
                final Set<String> affectedProjectPaths = affectedPathsToProjectPaths(targetVcsId, affectedPaths);
                for (String affectedProjectPath : affectedProjectPaths) {
                    final VcsLocation projectLocation = VcsLocation.create(targetVcsId, affectedProjectPath);
                    log(String.format("Scheduling build for project=%s revision=%s", projectLocation, newestPosted.getRevision()));
                    buildrobot.startBuild("commit", null, projectLocation, "");
                }
                final String webhookUrl = settingsDao.get(SettingsDao.GLOBAL_DEFAULTS_CONTEXT, "webhook.url");
                LOGGER.fine("webhookUrl=" + webhookUrl);
                if (webhookUrl != null) {
                    for (String affectedProjectPath : affectedProjectPaths) {
                        final VcsLocation projectLocation = VcsLocation.create(targetVcsId, affectedProjectPath);
                        final CodeRoot coderoot = structureDao.findCodeRootContaining(projectLocation);
                        if (coderoot != null) {
                            try {
                                invokeWebHook(webhookUrl, targetVcsRepository, coderoot, newestPosted);
                            } catch (Exception e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                    }
                }
            }
        }
    }

    private void invokeWebHook(String webhookUrl, VcsRepository targetVcsRepository, CodeRoot coderoot, CommitEntry commit) throws IOException, ProtocolException {
        final String projectName = coderoot.getBuildProperty("project.name");
        if (projectName == null) return;
        final URL url = new URL(webhookUrl);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/xml");
        connection.setDoOutput(true);
        final OutputStream os = connection.getOutputStream();
        try {
            final StringBuilder sbuffer = new StringBuilder();
            sbuffer.append("<event type='commit'>");
            sbuffer.append(String.format("  <project>%s</project>", projectName));
            sbuffer.append(String.format("  <contact>%s</contact>", commit.getAuthor()));
            sbuffer.append(String.format("  <eventTime>%tFT%<tT.%<tLZ</eventTime>", commit.getTimestamp()));
            sbuffer.append(String.format("  <component>%s</component>", coderoot.getDisplayName()));
            sbuffer.append(String.format("  <subject url='%s' code='%s'>%s</subject>", targetVcsRepository.getConnectionUrl(), commit.getRevision(), commit.getMessage()));
            sbuffer.append("</event>");
            os.write(sbuffer.toString().getBytes());
            os.flush();
            LOGGER.info("invoked webhook " + webhookUrl + " with data " + sbuffer);
        } finally {
            os.close();
            connection.disconnect();
        }
        if (connection.getResponseCode() != 201) {
            LOGGER.warning(String.format("Unexpected response: %d %s", connection.getResponseCode(), connection.getResponseMessage()));
        }
    }

    public static List<CommitEntry> getCommits(ParsedFormParams parsedFormParams, String vcsId) throws IOException, FileUploadException {
        final InputStream stream = parsedFormParams.getInputStream("SvnLogXml");
        if (stream != null) {
            final SvnLogParser svnLogParser = new SvnLogParser(vcsId);
            try {
                return svnLogParser.parseWithSax(new InputSource(stream));
            } catch (ParserConfigurationException e) {
                throw new IllegalStateException(e);
            } catch (SAXException e) {
                throw new FileUploadException("svn commit log", e);
            } finally {
                stream.close();
            }
        }
        return null;
    }

    private void printNewestCommitInfo(PrintWriter pw, String vcsId) {
        final CommitEntry newestCommit = commitLogDao.getLastCommit(VcsLocation.create(vcsId, ""));
        if (newestCommit != null) {
            pw.println(String.format("commits:newestRevision %s", newestCommit.getRevision()));
            pw.println(String.format("commits:newestTime %tFT%<tT.%<tL", newestCommit.getTimestamp()));
        }
        pw.flush();
    }

    private static void printVcsRepositoryInfo(PrintWriter pw, VcsRepository repo) {
        pw.println("repo:vcsId " + repo.getVcsId());
        pw.println("repo:vcsType " + repo.getVcsType());
        for (Map.Entry<String, String> entry : repo.getProperties().entrySet()) {
            pw.println(String.format("properties:%s %s", entry.getKey(), entry.getValue()));
        }
    }
}
