package com.plato.etoh.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import com.google.appengine.api.datastore.Text;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.plato.etoh.client.GreetingService;
import com.plato.etoh.client.model.Admin;
import com.plato.etoh.client.model.ApplicantDTO;
import com.plato.etoh.client.model.ApplicationDTO;
import com.plato.etoh.client.model.Comment;
import com.plato.etoh.client.model.Constants_fuckme;
import com.plato.etoh.client.model.Group;
import com.plato.etoh.client.model.MonthlyStat;
import com.plato.etoh.client.model.MyQuery;
import com.plato.etoh.client.model.ResultAppListWrapper;
import com.plato.etoh.client.model.Reviewer;
import com.plato.etoh.client.model.SessionPlusLoginResult;
import com.plato.etoh.client.model.StarInfo;
import com.plato.etoh.client.model.VoteInfo;
import com.plato.etoh.client.model.YoneticiBaseInterface;
import com.plato.etoh.client.util.Messages;
import com.plato.etoh.server.dbparser.DomParserExample;
import com.plato.etoh.server.model.Applicant;
import com.plato.etoh.server.model.Application;

@SuppressWarnings("serial")
public class GreetingServiceImpl extends RemoteServiceServlet implements GreetingService {

    static int sizeOfApplications = -1;

    static int sizeOfFinishedApplications = -1;

    static int sizeOfStartedApplications = -1;

    private boolean usernameExist(String username) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Applicant.class);
        query.setFilter("email == usernameParam");
        query.declareParameters("String usernameParam");
        try {
            List<Applicant> results = (List<Applicant>) query.execute(username);
            if (results.size() > 0) {
                return true;
            }
            query = pm.newQuery(Admin.class);
            query.setFilter("email == usernameParam");
            query.declareParameters("String usernameParam");
            List<Admin> admins = (List<Admin>) query.execute(username);
            if (admins.size() > 0) {
                return true;
            }
            query = pm.newQuery(Reviewer.class);
            query.setFilter("email == usernameParam");
            query.declareParameters("String usernameParam");
            List<Reviewer> reviewers = (List<Reviewer>) query.execute(username);
            if (reviewers.size() > 0) {
                return true;
            }
            return false;
        } finally {
            query.closeAll();
        }
    }

    /**
	 * Create application and returns its id
	 */
    @Override
    public String registerApplication(String username, String password, String email, Boolean hasCompany) {
        Application application = null;
        if (usernameExist(username)) {
            return Constants_fuckme.USER_ALREADY_EXITS;
        } else {
            PersistenceManager pm = PMF.get().getPersistenceManager();
            try {
                application = new Application();
                application.setBasvuruTarihi(new Date());
                Applicant applicant = new Applicant();
                Group group = new Group();
                application = pm.makePersistent(application);
                applicant = pm.makePersistent(applicant);
                group = pm.makePersistent(group);
                application.setGroupId(group.getId());
                application.setHasCompany(hasCompany);
                group.getApplicantList().add(applicant.getId());
                group.setApplicationId(application.getId());
                applicant.setUsername(username);
                applicant.setPassword(password);
                applicant.setEmail(email);
                applicant.setGroupId(group.getId());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pm.close();
            }
            MailUtil.sendNewUserMail(username, email);
        }
        return application.getId() + "";
    }

    @Override
    public ApplicationDTO getApplication(Long applicationId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Application applicationFromDatabase = null;
        ApplicationDTO app = null;
        try {
            applicationFromDatabase = pm.getObjectById(Application.class, applicationId);
            app = DunyaTurkOlsunUtil.createApplicationDTO(applicationFromDatabase);
        } catch (Exception e) {
        } finally {
            pm.close();
        }
        return app;
    }

    @Override
    public Group getGroup(Long id) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Group grp = new Group();
        try {
            Group groupFromDatabase = pm.getObjectById(Group.class, id);
            List<Long> tempList = new ArrayList<Long>();
            for (Iterator iterator = groupFromDatabase.getApplicantList().iterator(); iterator.hasNext(); ) {
                Long type = (Long) iterator.next();
                tempList.add(new Long(type.longValue()));
            }
            grp.setApplicantList(tempList);
            grp.setId(groupFromDatabase.getId());
            grp.setApplicationId(groupFromDatabase.getApplicationId());
            grp.setGroupName(groupFromDatabase.getGroupName());
            grp.setGroupName(groupFromDatabase.getGroupName());
        } finally {
            pm.close();
        }
        return grp;
    }

    @Override
    public ApplicantDTO getApplicantById(Long id) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Applicant applicant = null;
        ApplicantDTO applicantDto = null;
        try {
            applicant = pm.getObjectById(Applicant.class, id);
            applicantDto = DunyaTurkOlsunUtil.createApplicantDTO(applicant);
        } finally {
            pm.close();
        }
        return applicantDto;
    }

    @Override
    public ApplicantDTO getApplicantByUsername(String username) {
        if (username == null) {
            username = (String) this.getThreadLocalRequest().getSession().getAttribute("username");
        }
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Applicant applicantFromDatabase = null;
        ApplicantDTO resultApplicant = null;
        try {
            Query query = pm.newQuery(Applicant.class);
            query.setFilter("username == usr");
            query.declareParameters("String usr");
            List<Applicant> results = (List<Applicant>) query.execute(username);
            applicantFromDatabase = results.get(0);
            resultApplicant = DunyaTurkOlsunUtil.createApplicantDTO(applicantFromDatabase);
        } finally {
            pm.close();
        }
        return resultApplicant;
    }

    @Override
    public String saveApplicationInfo(Group incomingGroup, ApplicantDTO incomingApplicant) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            Group group = pm.getObjectById(Group.class, incomingGroup.getId());
            group.setGroupName(incomingGroup.getGroupName().toLowerCase());
            Applicant applicantFromDatabase = pm.getObjectById(Applicant.class, incomingApplicant.getId());
            applicantFromDatabase.setName(incomingApplicant.getName().toLowerCase());
            applicantFromDatabase.setSurname(incomingApplicant.getSurname().toLowerCase());
            applicantFromDatabase.setUsername(incomingApplicant.getUsername());
            applicantFromDatabase.setPassword(incomingApplicant.getPassword());
            applicantFromDatabase.setEmail(incomingApplicant.getEmail());
            applicantFromDatabase.setPhone(incomingApplicant.getPhone());
            applicantFromDatabase.setWebSite(incomingApplicant.getWebSite());
            applicantFromDatabase.setBlog(incomingApplicant.getBlog());
            applicantFromDatabase.setTwitter(incomingApplicant.getTwitter());
            applicantFromDatabase.setFacebook(incomingApplicant.getFacebook());
            applicantFromDatabase.setFriendfeed(incomingApplicant.getFriendfeed());
            applicantFromDatabase.setGoogleProfile(incomingApplicant.getGoogleProfile());
            applicantFromDatabase.setBirthday(incomingApplicant.getBirthday());
            applicantFromDatabase.setCityLiveIn(incomingApplicant.getCityLiveIn());
            applicantFromDatabase.setEducationInfo(new Text(incomingApplicant.getEducationInfo()));
            applicantFromDatabase.setWorkExperience(new Text(incomingApplicant.getWorkExperience()));
        } finally {
            pm.close();
        }
        return "OK";
    }

    /**
	 * wrong username password: -1 admin -2 reviewer -3 basvuru tamamlandi -4
	 */
    @Override
    public SessionPlusLoginResult checkLogin(String userName, String password) {
        SessionPlusLoginResult resultObject = new SessionPlusLoginResult();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Applicant.class);
        query.setFilter("username == usernameParam");
        query.declareParameters("String usernameParam");
        try {
            List<Applicant> results = (List<Applicant>) query.execute(userName);
            if (results.size() > 0) {
                for (Applicant e : results) {
                    if (e.getPassword().equals(password)) {
                        Group group = pm.getObjectById(Group.class, e.getGroupId());
                        Application application = pm.getObjectById(Application.class, group.getApplicationId());
                        if (application.getApplciationIsFinished() != null && application.getApplciationIsFinished()) {
                            resultObject.setLoginResult(Constants_fuckme.APP_CLOSED);
                            return resultObject;
                        } else {
                            this.getThreadLocalRequest().getSession().setAttribute("type", "applicant");
                            this.getThreadLocalRequest().getSession().setAttribute("username", userName);
                            this.getThreadLocalRequest().getSession().setAttribute("userid", e.getId());
                            resultObject.setSessionId(this.getThreadLocalRequest().getSession().getId());
                            resultObject.setLoginResult(application.getId());
                            return resultObject;
                        }
                    } else if (e.getPassword() == null || e.getPassword().equalsIgnoreCase("")) {
                        resultObject.setLoginResult(Constants_fuckme.SIFRE_YARATMALI);
                        return resultObject;
                    }
                }
            } else {
                query = pm.newQuery(Admin.class);
                query.setFilter("username == usernameParam && password == sff");
                query.declareParameters("String usernameParam, String sff");
                List<Admin> resultsAdmin = (List<Admin>) query.execute(userName, password);
                if (resultsAdmin.size() > 0) {
                    this.getThreadLocalRequest().getSession().setAttribute("type", "admin");
                    this.getThreadLocalRequest().getSession().setAttribute("username", userName);
                    this.getThreadLocalRequest().getSession().setAttribute("userid", resultsAdmin.get(0).getId());
                    resultObject.setSessionId(this.getThreadLocalRequest().getSession().getId());
                    resultObject.setLoginResult(Constants_fuckme.ADMIN);
                    return resultObject;
                }
                query = pm.newQuery(Reviewer.class);
                query.setFilter("username == usernameParam && password == sff");
                query.declareParameters("String usernameParam, String sff");
                List<Reviewer> resultsReviewer = (List<Reviewer>) query.execute(userName, password);
                if (resultsReviewer.size() > 0) {
                    this.getThreadLocalRequest().getSession().setAttribute("type", "reviewer");
                    this.getThreadLocalRequest().getSession().setAttribute("username", userName);
                    this.getThreadLocalRequest().getSession().setAttribute("userid", resultsReviewer.get(0).getId());
                    resultObject.setSessionId(this.getThreadLocalRequest().getSession().getId());
                    resultObject.setLoginResult(Constants_fuckme.REVIEWER);
                    return resultObject;
                } else {
                    resultObject.setLoginResult(Constants_fuckme.WRONG_PASSWORD);
                    return resultObject;
                }
            }
        } finally {
            query.closeAll();
        }
        resultObject.setLoginResult(Constants_fuckme.WRONG_PASSWORD);
        return resultObject;
    }

    public Long checkLoginFromSession() {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Applicant.class);
        query.setFilter("username == usernameParam");
        query.declareParameters("String usernameParam");
        String usernameFromSession = (String) this.getThreadLocalRequest().getSession().getAttribute("username");
        try {
            List<Applicant> results = (List<Applicant>) query.execute(usernameFromSession);
            if (results.size() > 0) {
                for (Applicant e : results) {
                    Group group = pm.getObjectById(Group.class, e.getGroupId());
                    Application application = pm.getObjectById(Application.class, group.getApplicationId());
                    if (application.getApplciationIsFinished() != null && application.getApplciationIsFinished()) {
                        return Constants_fuckme.APP_CLOSED;
                    } else {
                        return application.getId();
                    }
                }
            } else {
                query = pm.newQuery(Admin.class);
                query.setFilter("username == usernameParam");
                query.declareParameters("String usernameParam");
                List<Admin> resultsAdmin = (List<Admin>) query.execute(usernameFromSession);
                if (resultsAdmin.size() > 0) {
                    return Constants_fuckme.ADMIN;
                } else {
                    query = pm.newQuery(Reviewer.class);
                    query.setFilter("username == usernameParam");
                    query.declareParameters("String usernameParam");
                    List<Reviewer> resultsReviewer = (List<Reviewer>) query.execute(usernameFromSession);
                    if (resultsReviewer.size() > 0) {
                        return Constants_fuckme.REVIEWER;
                    }
                    return Constants_fuckme.WRONG_PASSWORD;
                }
            }
        } finally {
            query.closeAll();
        }
        return Constants_fuckme.WRONG_PASSWORD;
    }

    @Override
    public Long checkIfSessionIsValid(String sessionId) {
        if (this.getThreadLocalRequest().getSession().getId().equalsIgnoreCase(sessionId)) {
            return checkLoginFromSession();
        } else {
            return -31L;
        }
    }

    @Override
    public String saveAnswers(List<String> answerStringList, Long applicationId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            try {
                Application applicationFromDb = pm.getObjectById(Application.class, applicationId);
                applicationFromDb.setStartAnswering(true);
                Text[] textArray = new Text[answerStringList.size()];
                for (int i = 0; i < answerStringList.size(); i++) {
                    textArray[i] = new Text(answerStringList.get(i));
                }
                applicationFromDb.setOrtakSorularArray(textArray);
            } catch (Exception e) {
                System.out.println("yeap...");
            }
        } finally {
            pm.close();
        }
        return "OK";
    }

    @Override
    public String saveAnswersApplicant(List<String> answerStringList, Long applicationId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            try {
                Applicant applicantFromDb = pm.getObjectById(Applicant.class, applicationId);
                Text[] textArray = new Text[answerStringList.size()];
                for (int i = 0; i < answerStringList.size(); i++) {
                    textArray[i] = new Text(answerStringList.get(i));
                }
                applicantFromDb.setKisiselSorularArray(textArray);
            } catch (Exception e) {
                System.out.println("saveAnswersApplicant exception ... " + e.getMessage());
            }
        } finally {
            pm.close();
        }
        return "OK";
    }

    @Override
    public List<ApplicantDTO> getApplicantsInAnApplication(Long appId) {
        ApplicationDTO application = this.getApplication(appId);
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Applicant.class);
        query.setFilter("groupId == grpId");
        query.declareParameters("Long grpId");
        List<Applicant> results = null;
        List<ApplicantDTO> outgoingApplicants = new ArrayList<ApplicantDTO>();
        ApplicantDTO applicantDto = null;
        try {
            results = (List<Applicant>) query.execute(application.getGroupId());
            for (Applicant e : results) {
                applicantDto = DunyaTurkOlsunUtil.createApplicantDTO(e);
                outgoingApplicants.add(applicantDto);
            }
        } finally {
            query.closeAll();
        }
        return outgoingApplicants;
    }

    @Override
    public String addMemberToGroup(String username, String password, String email, Long groupId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Applicant applicant = new Applicant();
        if (usernameExist(username)) {
            return Messages.USER_NAME_EXIST;
        }
        try {
            applicant = pm.makePersistent(applicant);
            applicant.setUsername(username);
            applicant.setPassword(password);
            applicant.setEmail(email);
            applicant.setGroupId(groupId);
            Group group = pm.getObjectById(Group.class, groupId);
            group.getApplicantList().add(applicant.getId());
        } finally {
            pm.close();
        }
        MailUtil.sendNewUserMail(username, email);
        return "OK";
    }

    @Override
    public Boolean basvuruyuTamamla(Long appId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Application applicationFromDatabase = null;
        try {
            applicationFromDatabase = pm.getObjectById(Application.class, appId);
            applicationFromDatabase.setApplciationIsFinished(true);
            applicationFromDatabase.setBitisTarihi(new Date());
        } catch (Exception e) {
            return new Boolean(false);
        } finally {
            pm.close();
        }
        return new Boolean(true);
    }

    @Override
    public Boolean basvuruyuTamamlaMailleriniGonder(String email) {
        MailUtil.mailGonderGeneric(email, "Başvurunuz tamamlandı. Hesabınız değişikliğe kapatılmıştır. " + "\nBaşvurunuz incelendikten sonra sizinle bağlantıya geçilecektir." + MailUtil.etohum_dunya, "Etohum başvurunuz tamamlandı.");
        return true;
    }

    @Override
    public String yildizIsaretle(Long userId, Long incomingApplicationId, Boolean ekle) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            if (ekle) {
                StarInfo star = new StarInfo();
                star = pm.makePersistent(star);
                star.setAdminId(userId);
                star.setApplicationId(incomingApplicationId);
                Application application = pm.getObjectById(Application.class, incomingApplicationId);
                application.getStarList().add(userId);
                if (((String) this.getThreadLocalRequest().getSession().getAttribute("type")).equalsIgnoreCase("admin")) {
                    Admin admin = pm.getObjectById(Admin.class, userId);
                    admin.getStarList().add(incomingApplicationId);
                } else if (((String) this.getThreadLocalRequest().getSession().getAttribute("type")).equalsIgnoreCase("reviewer")) {
                    Reviewer reviewer = pm.getObjectById(Reviewer.class, userId);
                    reviewer.getStarList().add(incomingApplicationId);
                }
            } else {
                Query query = pm.newQuery(StarInfo.class);
                query.setFilter("applicationId == appId && adminId == admId");
                query.declareParameters("Long appId, Long admId");
                try {
                    List results = (List<StarInfo>) query.execute(incomingApplicationId, userId);
                    StarInfo vote = (StarInfo) results.get(0);
                    Application application = pm.getObjectById(Application.class, incomingApplicationId);
                    application.getStarList().remove(userId);
                    if (((String) this.getThreadLocalRequest().getSession().getAttribute("type")).equalsIgnoreCase("admin")) {
                        Admin admin = pm.getObjectById(Admin.class, userId);
                        admin.getStarList().remove(incomingApplicationId);
                    } else if (((String) this.getThreadLocalRequest().getSession().getAttribute("type")).equalsIgnoreCase("reviewer")) {
                        Reviewer reviewer = pm.getObjectById(Reviewer.class, userId);
                        reviewer.getStarList().remove(incomingApplicationId);
                    }
                    pm.deletePersistent(vote);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    query.closeAll();
                }
            }
        } finally {
            pm.close();
        }
        return "OK";
    }

    @Override
    public String vote(Long adminId, Long applicationId, Integer value) {
        double sum = 0;
        double avg = 0;
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            Query query = pm.newQuery(VoteInfo.class);
            query.setFilter("applicationId == appId && adminId == admId");
            query.declareParameters("Long appId, Long admId");
            try {
                List<VoteInfo> results = (List<VoteInfo>) query.execute(applicationId, adminId);
                if (results == null || results.size() == 0) {
                    VoteInfo vote = new VoteInfo();
                    vote = pm.makePersistent(vote);
                    vote.setAdminId(adminId);
                    vote.setApplicationId(applicationId);
                    vote.setVoteValue(value);
                } else {
                    VoteInfo vote = results.get(0);
                    vote.setVoteValue(value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                query.closeAll();
            }
        } finally {
            pm.close();
        }
        avg = calculateAverage(applicationId);
        return String.valueOf(avg);
    }

    private double calculateAverage(Long applicationId) {
        double avg = 0;
        double sum;
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            List<VoteInfo> votesOfAnApplication = this.getVotesOfAnApplication(applicationId);
            sum = 0;
            for (Iterator<VoteInfo> iterator = votesOfAnApplication.iterator(); iterator.hasNext(); ) {
                VoteInfo voteInfo = iterator.next();
                sum += voteInfo.getVoteValue();
            }
            avg = sum / votesOfAnApplication.size();
            Application objectById = pm.getObjectById(Application.class, applicationId);
            objectById.setOrtalamaPuan(avg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pm.close();
        }
        return avg;
    }

    @Override
    public String addAdminOrReviewer(String username, String password, String email, int role) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        if (usernameExist(username)) {
            return Messages.USER_NAME_EXIST;
        }
        if (role == 1) {
            Admin admin = new Admin();
            try {
                admin = pm.makePersistent(admin);
                admin.setUsername(username);
                admin.setPassword(password);
                admin.setEmail(email);
                MailUtil.sendNewUserMail(admin.getUsername(), admin.getEmail());
            } finally {
                pm.close();
            }
            return "OK";
        } else if (role == 2) {
            Reviewer reviewer = new Reviewer();
            try {
                reviewer = pm.makePersistent(reviewer);
                reviewer.setUsername(username);
                reviewer.setPassword(password);
                reviewer.setEmail(email);
                MailUtil.sendNewUserMail(reviewer.getUsername(), reviewer.getEmail());
            } finally {
                pm.close();
            }
            return "OK";
        } else {
            return "Hata";
        }
    }

    @Override
    public List<Admin> getAdminList() {
        List<Admin> result = new ArrayList<Admin>();
        List<Admin> resultFromDb = null;
        PersistenceManager pm = PMF.get().getPersistenceManager();
        String queryString = "select from " + Admin.class.getName() + " order by name asc";
        Query query = pm.newQuery(queryString);
        try {
            resultFromDb = (List<Admin>) query.execute();
            Admin app;
            for (Iterator<Admin> iterator = resultFromDb.iterator(); iterator.hasNext(); ) {
                Admin applicationFromDatabase = (Admin) iterator.next();
                app = DunyaTurkOlsunUtil.createAdminDto(applicationFromDatabase);
                result.add(app);
            }
        } finally {
            pm.close();
        }
        return result;
    }

    @Override
    public List<Reviewer> getReviewerList() {
        List<Reviewer> result = new ArrayList<Reviewer>();
        List<Reviewer> resultFromDb = null;
        PersistenceManager pm = PMF.get().getPersistenceManager();
        String queryString = "select from " + Reviewer.class.getName() + " order by name asc";
        Query query = pm.newQuery(queryString);
        try {
            resultFromDb = (List<Reviewer>) query.execute();
            Reviewer app;
            for (Iterator<Reviewer> iterator = resultFromDb.iterator(); iterator.hasNext(); ) {
                Reviewer reviewerFromDatabase = (Reviewer) iterator.next();
                app = DunyaTurkOlsunUtil.createReviewerDto(reviewerFromDatabase);
                result.add(app);
            }
        } finally {
            pm.close();
        }
        return result;
    }

    @Override
    public Admin getAdmin(Long id) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Admin admin = null;
        try {
            admin = pm.getObjectById(Admin.class, id);
            admin = DunyaTurkOlsunUtil.createAdminDto(admin);
        } finally {
            pm.close();
        }
        return admin;
    }

    @Override
    public Reviewer getReviewer(Long id) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Reviewer reviewer = null;
        try {
            reviewer = pm.getObjectById(Reviewer.class, id);
            reviewer = DunyaTurkOlsunUtil.createReviewerDto(reviewer);
        } finally {
            pm.close();
        }
        return reviewer;
    }

    /**
	 * Eger username null veriliyorsa, session icindeki admin donulur
	 * 
	 */
    @Override
    public Admin getAdminByUsername(String username) {
        if (username == null) {
            username = (String) this.getThreadLocalRequest().getSession().getAttribute("username");
        }
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Admin admin = null;
        try {
            Query query = pm.newQuery(Admin.class);
            query.setFilter("username == usr");
            query.declareParameters("String usr");
            List<Admin> results = (List<Admin>) query.execute(username);
            admin = results.get(0);
            admin = DunyaTurkOlsunUtil.createAdminDto(admin);
        } finally {
            pm.close();
        }
        return admin;
    }

    /**
	 * Eger username null veriliyorsa, session icindeki reviewer donulur
	 * 
	 */
    @Override
    public Reviewer getReviewerByUsername(String username) {
        if (username == null) {
            username = (String) this.getThreadLocalRequest().getSession().getAttribute("username");
        }
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Reviewer reviewer = null;
        try {
            Query query = pm.newQuery(Reviewer.class);
            query.setFilter("username == usr");
            query.declareParameters("String usr");
            List<Reviewer> results = (List<Reviewer>) query.execute(username);
            reviewer = results.get(0);
            reviewer = DunyaTurkOlsunUtil.createReviewerDto(reviewer);
        } finally {
            pm.close();
        }
        return reviewer;
    }

    @Override
    public String createSampleData() {
        long start = System.currentTimeMillis();
        if (DomParserExample.sampleDataCreatorIndex != 0) {
            int phase = DomParserExample.getInstance().runExample();
            long end = System.currentTimeMillis();
            System.out.println("Execution time was " + (end - start) + " ms.");
            return phase + "";
        }
        System.out.println("sample data is being created...");
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            if (DomParserExample.sampleDataCreatorIndex == 0) {
                DomParserExample.sampleDataCreatorIndex++;
                Admin admin = new Admin();
                pm.makePersistent(admin);
                admin.setUsername("bilisim@psmri.net");
                admin.setPassword("qwerty");
                admin.setName("Dogan Berktas");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            pm.close();
            long end = System.currentTimeMillis();
            System.out.println("Execution time was " + (end - start) + " ms.");
        }
        return "-1";
    }

    @Override
    public String markAsSpam(Long adminId, Long applicationId, Boolean spamDurumu) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            if (spamDurumu) {
                Application application = pm.getObjectById(Application.class, applicationId);
                application.setIsSpam(adminId);
            } else {
                Application application = pm.getObjectById(Application.class, applicationId);
                application.setIsSpam(-1L);
            }
        } finally {
            pm.close();
        }
        return "OK";
    }

    @Override
    public List<Comment> getCommentOfAnApplication(Long applicationId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Comment.class);
        query.setFilter("applicationId == appId");
        query.declareParameters("Long appId");
        query.setOrdering("date desc");
        List<Comment> results = null;
        List<Comment> outgoingResults = new ArrayList<Comment>();
        Comment temp = null;
        try {
            results = (List<Comment>) query.execute(applicationId);
            for (Comment e : results) {
                temp = DunyaTurkOlsunUtil.createCommentDTO(e);
                outgoingResults.add(temp);
            }
        } finally {
            query.closeAll();
        }
        return outgoingResults;
    }

    @Override
    public String comment(Long applicationId, Long adminId, String commentText) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Comment comment = null;
        try {
            YoneticiBaseInterface yonetici = pm.getObjectById(Admin.class, adminId);
            if (yonetici == null) {
                yonetici = pm.getObjectById(Reviewer.class, adminId);
            }
            comment = new Comment();
            comment.setApplicationId(applicationId);
            comment.setAdminId(adminId);
            comment.setDate(new Date());
            comment.setCommentText(commentText);
            pm.makePersistent(comment);
            ApplicationDTO app = this.getApplication(applicationId);
            for (Iterator iterator = app.getCommentTakipListesi().iterator(); iterator.hasNext(); ) {
                Long adminReviewerId = (Long) iterator.next();
                Admin followerAdmin = null;
                followerAdmin = pm.getObjectById(Admin.class, adminReviewerId);
                if (followerAdmin != null) {
                    MailUtil.sendNewCommentMail(followerAdmin, comment, app, yonetici);
                } else {
                    Reviewer followerReviewer = pm.getObjectById(Reviewer.class, adminReviewerId);
                    if (followerReviewer != null) {
                        MailUtil.sendNewCommentMail(followerReviewer, comment, app, yonetici);
                    }
                }
            }
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            pm.close();
        }
        return "OK";
    }

    @Override
    public ArrayList<ApplicationDTO> getTamamlanmisApplications() {
        long start = System.currentTimeMillis();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        ArrayList<ApplicationDTO> resultAppList = new ArrayList<ApplicationDTO>();
        List<Application> appListFromDB = null;
        String queryString = "select from " + Application.class.getName() + " where applciationIsFinished == true";
        Query query = pm.newQuery(queryString);
        query.setOrdering("bitisTarihi desc");
        ApplicationDTO app;
        try {
            appListFromDB = (List<Application>) query.executeWithArray();
            for (Iterator<Application> iterator = appListFromDB.iterator(); iterator.hasNext(); ) {
                Application applicationFromDatabase = (Application) iterator.next();
                app = DunyaTurkOlsunUtil.createApplicationDTO(applicationFromDatabase);
                resultAppList.add(app);
            }
        } catch (Exception e) {
            System.err.println("Exception fired : " + e.getStackTrace().toString());
        } finally {
            pm.close();
        }
        long end = System.currentTimeMillis();
        System.out.println("Execution time in getBareApplications was " + (end - start) + " ms.");
        return resultAppList;
    }

    @Override
    public ResultAppListWrapper getBareApplications(int from, int to, MyQuery myQuery) {
        long start = System.currentTimeMillis();
        ArrayList<ApplicationDTO> resultAppList = new ArrayList<ApplicationDTO>();
        List<Application> appListFromDB = null;
        PersistenceManager pm = PMF.get().getPersistenceManager();
        String queryString = "select from " + Application.class.getName();
        Query query = pm.newQuery(queryString);
        query.setOrdering("basvuruTarihi desc");
        String declareParameters = "";
        String filterString = "";
        List<Object> parameterList = new ArrayList<Object>();
        if (myQuery.getHideSpam()) {
            filterString += " isSpam ==  " + -1 + " && ";
        }
        if (myQuery.getStartAnswering() != null) {
            filterString += " startAnswering == " + myQuery.getStartAnswering() + " && ";
        }
        if (myQuery.getHasCompany() != null) {
            filterString += " hasCompany == true " + " && ";
        }
        if (myQuery.getIsApplicationFinished() != null) {
            filterString += " applciationIsFinished == " + myQuery.getIsApplicationFinished() + " && ";
        }
        if (myQuery.getStartDate() != null) {
            filterString += " basvuruTarihi > startDate " + " && ";
            declareParameters += " java.util.Date startDate ,";
            parameterList.add(myQuery.getStartDate());
        }
        if (myQuery.getEndDate() != null) {
            filterString += " basvuruTarihi < endDate " + " && ";
            declareParameters += " java.util.Date endDate ,";
            parameterList.add(myQuery.getEndDate());
        }
        if (!myQuery.getGroupName().equalsIgnoreCase("")) {
            filterString += " groupName == '" + myQuery.getGroupName() + "' && ";
        }
        if (myQuery.getGroupId() != null) {
            filterString += " groupId == " + myQuery.getGroupId() + " && ";
        }
        String params = "";
        if (declareParameters.indexOf(",") > -1) {
            int lastIndexOf = declareParameters.lastIndexOf(",");
            params = declareParameters.substring(0, lastIndexOf).trim();
        } else {
            params = declareParameters;
        }
        String filter = "";
        if (filterString.indexOf("&&") > -1) {
            int lastIndexOf = filterString.lastIndexOf("&&");
            filter = filterString.substring(0, lastIndexOf).trim();
        } else {
            filter = filterString;
        }
        if (!filter.equalsIgnoreCase("")) {
            query.setFilter(filter);
            query.declareParameters(declareParameters);
        }
        System.out.println("from: " + from + " to:" + to);
        ApplicationDTO app;
        try {
            appListFromDB = (List<Application>) query.executeWithArray(parameterList.toArray());
            GreetingServiceImpl.sizeOfApplications = appListFromDB.size();
            if (to > appListFromDB.size()) {
                appListFromDB = appListFromDB.subList(from, appListFromDB.size());
            } else {
                appListFromDB = appListFromDB.subList(from, to);
            }
            for (Iterator<Application> iterator = appListFromDB.iterator(); iterator.hasNext(); ) {
                Application applicationFromDatabase = (Application) iterator.next();
                app = DunyaTurkOlsunUtil.createApplicationDTO(applicationFromDatabase);
                resultAppList.add(app);
            }
        } catch (Exception e) {
            System.err.println("Exception fired : " + e.getStackTrace().toString());
        } finally {
            pm.close();
        }
        long end = System.currentTimeMillis();
        System.out.println("Execution time in getBareApplications was " + (end - start) + " ms.");
        return new ResultAppListWrapper(resultAppList, GreetingServiceImpl.sizeOfApplications);
    }

    @Override
    public VoteInfo getVote(Long applicationId, Long adminId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        VoteInfo voreInfoFromDatabase = null;
        VoteInfo resultVoteInfo = null;
        try {
            Query query = pm.newQuery(VoteInfo.class);
            query.setFilter("applicationId == appId && adminId == adId");
            query.declareParameters("Long appId, Long adId");
            List<VoteInfo> results = (List<VoteInfo>) query.execute(applicationId, adminId);
            if (results.size() == 0) {
                resultVoteInfo = null;
            } else {
                voreInfoFromDatabase = results.get(0);
                resultVoteInfo = DunyaTurkOlsunUtil.createVoteInfoDTO(voreInfoFromDatabase);
            }
        } finally {
            pm.close();
        }
        return resultVoteInfo;
    }

    @Override
    public List<VoteInfo> getVotesOfAnApplication(Long applicationId) {
        List<VoteInfo> dtoResultList = new ArrayList<VoteInfo>();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            Query query = pm.newQuery(VoteInfo.class);
            query.setFilter("applicationId == appId");
            query.declareParameters("Long appId");
            List<VoteInfo> resultFromDB = (List<VoteInfo>) query.execute(applicationId);
            for (Iterator<VoteInfo> iterator = resultFromDB.iterator(); iterator.hasNext(); ) {
                VoteInfo voteInfo = (VoteInfo) iterator.next();
                voteInfo = DunyaTurkOlsunUtil.createVoteInfoDTO(voteInfo);
                dtoResultList.add(voteInfo);
            }
        } finally {
            pm.close();
        }
        return dtoResultList;
    }

    @Override
    public String sifreHatirlatma(String username) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Applicant applicantFromDatabase = null;
        Applicant resultApplicant = null;
        try {
            Query query = pm.newQuery(Applicant.class);
            query.setFilter("username == usr");
            query.declareParameters("String usr");
            List<Applicant> results = (List<Applicant>) query.execute(username);
            if (results != null && results.size() > 0) {
                Date d = new Date();
                int pass = Math.abs(new Date().toGMTString().hashCode());
                results.get(0).setPassword(String.valueOf(pass));
                MailUtil.sendRecoverPassword(results.get(0).getUsername(), results.get(0).getEmail(), pass);
                return "okuzsun_sen";
            } else {
                query = pm.newQuery(Admin.class);
                query.setFilter("username == usr");
                query.declareParameters("String usr");
                List<Admin> admins = (List<Admin>) query.execute(username);
                if (admins != null && admins.size() > 0) {
                    Date d = new Date();
                    int pass = Math.abs(new Date().toGMTString().hashCode());
                    admins.get(0).setPassword(String.valueOf(pass));
                    MailUtil.sendRecoverPassword(admins.get(0).getUsername(), admins.get(0).getEmail(), pass);
                    return "okuzsun_sen";
                } else {
                    query = pm.newQuery(Reviewer.class);
                    query.setFilter("username == usr");
                    query.declareParameters("String usr");
                    List<Reviewer> reviewers = (List<Reviewer>) query.execute(username);
                    if (reviewers != null && reviewers.size() > 0) {
                        Date d = new Date();
                        int pass = Math.abs(new Date().toGMTString().hashCode());
                        reviewers.get(0).setPassword(String.valueOf(pass));
                        MailUtil.sendRecoverPassword(reviewers.get(0).getUsername(), reviewers.get(0).getEmail(), pass);
                        return "okuzsun_sen";
                    } else {
                        return "okuzsun_sen";
                    }
                }
            }
        } finally {
            pm.close();
        }
    }

    @Override
    public Boolean sendHaberVerEmail(String msgBody, String toEmailAddress, String commentText, int ApplicationId, int senderId) {
        String subject = commentText;
        MailUtil.mailGonderGeneric(toEmailAddress, msgBody, subject);
        return true;
    }

    @Override
    public String commentFollow(Long appId, Boolean follow, Long adminId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Application applicationFromDatabase = null;
        ApplicationDTO app = null;
        try {
            applicationFromDatabase = pm.getObjectById(Application.class, appId);
            if (follow && !applicationFromDatabase.getCommentTakipListesi().contains(adminId)) {
                applicationFromDatabase.getCommentTakipListesi().add(adminId);
            } else {
                applicationFromDatabase.getCommentTakipListesi().remove(adminId);
            }
        } finally {
            pm.close();
        }
        return null;
    }

    @Override
    public String changePassword(String sessionId, String oldPassword, String newPassword) {
        if (this.getThreadLocalRequest().getSession().getId().equals(sessionId)) {
            String userName = (String) this.getThreadLocalRequest().getSession().getAttribute("username");
            PersistenceManager pm = PMF.get().getPersistenceManager();
            Applicant applicantFromDatabase = null;
            try {
                Query query = pm.newQuery(Applicant.class);
                query.setFilter("username == usr");
                query.declareParameters("String usr");
                List<Applicant> results = (List<Applicant>) query.execute(userName);
                applicantFromDatabase = results.get(0);
                if (applicantFromDatabase.getPassword().equals(oldPassword)) {
                    applicantFromDatabase.setPassword(newPassword);
                    return "OK";
                } else {
                    return "passwords_dont_match";
                }
            } finally {
                pm.close();
            }
        } else {
            return "session_not_valid";
        }
    }

    @Override
    public String getAdminReviewerNameById(Long adminId) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            YoneticiBaseInterface resultYonetici = pm.getObjectById(Admin.class, adminId);
            if (resultYonetici != null) {
                return resultYonetici.getEmail();
            } else {
                resultYonetici = pm.getObjectById(Reviewer.class, adminId);
                if (resultYonetici != null) {
                    return resultYonetici.getEmail();
                } else {
                    return "isim bulunamadi";
                }
            }
        } finally {
            pm.close();
        }
    }

    @Override
    public String haberVer(Long applicationId, Long adminId, String email, String message, String sid) {
        if (this.getThreadLocalRequest().getSession().getId().equals(sid)) {
            ApplicationDTO application = this.getApplication(applicationId);
            String yonetici = getAdminReviewerNameById(adminId);
            String epostaMessage = "Merhaba,\n" + yonetici + " kullancı sizinle bir başvuru paylaştı.\n" + "Kullanıcı tarafından eklenen mesaj: " + message + "\n-----\n" + "Başvuru Bilgileri\n" + "Başvuru numarası: " + applicationId + "\nBaşvuru Tarihi: " + application.getBasvuruTarihi().toGMTString() + "\nŞu ana kadar aldığı ortalama puan: " + application.getOrtalamaPuan() + "\nŞirket: " + (application.getHasCompany() ? "var" : "yok") + MailUtil.etohum_dunya + "\n" + "E-tohum Ekibi";
            MailUtil.mailGonderGeneric(email, epostaMessage, "Etohum - Başvuru incelemeye davet edildiniz");
            return "OK";
        }
        return "not_good";
    }

    @Override
    public String deleteAll() {
        System.out.println("siliyor...");
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            Query query = pm.newQuery(Applicant.class);
            query.deletePersistentAll();
        } finally {
            pm.close();
            System.out.println("silindi.");
        }
        DomParserExample.phase = 0;
        return "OK";
    }

    @Override
    public String saveMonthDate(Date date, String value) {
        System.out.println("month in saveMonthDate " + date.getMonth());
        PersistenceManager pm = PMF.get().getPersistenceManager();
        String queryString = "select from " + MonthlyStat.class.getName();
        Query query = pm.newQuery(queryString);
        String filterString = "";
        filterString += "month == " + date.getMonth() + " && ";
        filterString += "year == " + date.getYear() + "";
        query.setFilter(filterString);
        List<MonthlyStat> results = (List<MonthlyStat>) query.execute();
        if (results.size() == 0) {
            MonthlyStat m = new MonthlyStat();
            m.setHowMany(value);
            m.setMonth(date.getMonth());
            m.setYear(date.getYear());
            try {
                pm.makePersistent(m);
            } finally {
                pm.close();
            }
            return "OK";
        } else {
            pm.close();
            return "NOP";
        }
    }

    @Override
    public Integer getApplicationCountInOneMount(Date date) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        String queryString = "select from " + MonthlyStat.class.getName();
        Query query = pm.newQuery(queryString);
        String filterString = "";
        filterString += "month == " + date.getMonth() + " && ";
        filterString += "year == " + date.getYear() + "";
        query.setFilter(filterString);
        List<Application> greetings = null;
        int resultCount = 0;
        int size = 0;
        try {
            date.setDate(1);
            Query q = pm.newQuery("select from " + Application.class.getName());
            q.setFilter("basvuruTarihi >= tarih");
            q.declareImports("import java.util.Date");
            q.declareParameters("Date tarih");
            greetings = (List<Application>) q.execute(date);
            for (Iterator iterator = greetings.iterator(); iterator.hasNext(); ) {
                Application application = (Application) iterator.next();
                Date basvuruTarihi = application.getBasvuruTarihi();
                if (basvuruTarihi.getMonth() == date.getMonth() && basvuruTarihi.getYear() == date.getYear()) {
                    resultCount++;
                }
            }
            long end = System.currentTimeMillis();
            List<MonthlyStat> results = (List<MonthlyStat>) query.execute();
            if (results.size() == 0) {
                MonthlyStat m = new MonthlyStat();
                m.setHowMany(String.valueOf(resultCount));
                m.setMonth(date.getMonth());
                m.setYear(date.getYear());
                pm.makePersistent(m);
            }
        } finally {
            pm.close();
        }
        long start = System.currentTimeMillis();
        return resultCount;
    }

    @Override
    public List<MonthlyStat> getAllMonthData() {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        String query = "select from " + MonthlyStat.class.getName();
        List<MonthlyStat> greetings = (List<MonthlyStat>) pm.newQuery(query).execute();
        List<MonthlyStat> results = new ArrayList<MonthlyStat>();
        for (Iterator<MonthlyStat> iterator = greetings.iterator(); iterator.hasNext(); ) {
            MonthlyStat monthlyStat = iterator.next();
            results.add(DunyaTurkOlsunUtil.createMonthlyStat(monthlyStat));
        }
        return results;
    }

    @Override
    public String topluEmail(String message) {
        try {
            String data = "message=" + URLEncoder.encode(message, "UTF-8");
            URL url = new URL("http://interrailmap.com:8080/MailUtil6/BatchEmail");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(data);
            writer.flush();
            StringBuffer answer = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                answer.append(line);
            }
            writer.close();
            reader.close();
            System.out.println(answer.toString());
            return "Toplu Mail gonderildi!";
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            return "HATA 10.09 URL yapisi hatasi";
        } catch (IOException ex) {
            ex.printStackTrace();
            return "HATA 10.09 Baglanti hatasi";
        }
    }

    @Override
    public String setApplicationGroupName(Long id, String name) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Application applicant = null;
        try {
            applicant = pm.getObjectById(Application.class, id);
            applicant.setGroupName(name.toLowerCase());
        } finally {
            pm.close();
        }
        return "OK";
    }

    @Override
    public ApplicantDTO getApplicantByEmail(String email) {
        if (email == null) {
            return null;
        }
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Applicant applicantFromDatabase = null;
        ApplicantDTO resultApplicant = null;
        try {
            Query query = pm.newQuery(Applicant.class);
            query.setFilter("email == eml");
            query.declareParameters("String eml");
            List<Applicant> results = (List<Applicant>) query.execute(email);
            applicantFromDatabase = results.get(0);
            resultApplicant = DunyaTurkOlsunUtil.createApplicantDTO(applicantFromDatabase);
        } catch (Exception e) {
        } finally {
            pm.close();
        }
        return resultApplicant;
    }

    @Override
    public ArrayList<Long> getGroupIdsByNameOrSurname(String name, String surname) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        ArrayList<Long> groupIdList = new ArrayList<Long>();
        try {
            String queryString = "select from " + Applicant.class.getName();
            Query query = pm.newQuery(queryString);
            String filterString = "";
            if (!name.equalsIgnoreCase("")) {
                filterString += "name == '" + name + "' && ";
            }
            if (!surname.equalsIgnoreCase("")) {
                filterString += "surname == '" + surname + "' &&";
            }
            String filter = "";
            if (filterString.indexOf("&&") > -1) {
                int lastIndexOf = filterString.lastIndexOf("&&");
                filter = filterString.substring(0, lastIndexOf).trim();
            } else {
                filter = filterString;
            }
            query.setFilter(filter);
            List<Applicant> results = (List<Applicant>) query.execute();
            for (Iterator iterator = results.iterator(); iterator.hasNext(); ) {
                Applicant applicant = (Applicant) iterator.next();
                groupIdList.add(applicant.getGroupId());
            }
        } finally {
            pm.close();
        }
        return groupIdList;
    }

    @Override
    public ArrayList<Long> getApplicationIdsByGroupIds(ArrayList<Long> groupIdList) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        ArrayList<Long> applicationIdList = new ArrayList<Long>();
        for (Iterator iterator = groupIdList.iterator(); iterator.hasNext(); ) {
            Long groupId = (Long) iterator.next();
            try {
                String queryString = "select from " + Group.class.getName();
                Query query = pm.newQuery(queryString);
                String filterString = "";
                filterString += "id == " + groupId;
                query.setFilter(filterString);
                List<Group> results = (List<Group>) query.execute();
                for (Iterator iterator2 = results.iterator(); iterator2.hasNext(); ) {
                    Group group = (Group) iterator2.next();
                    applicationIdList.add(group.getApplicationId());
                }
            } finally {
                pm.close();
            }
        }
        return applicationIdList;
    }
}

class Util {

    public static boolean contains(String baba, String tasinasiKisim) {
        if (baba.contains(tasinasiKisim)) {
            return true;
        }
        return false;
    }
}
