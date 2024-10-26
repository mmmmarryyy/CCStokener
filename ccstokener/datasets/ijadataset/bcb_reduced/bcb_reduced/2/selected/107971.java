package org.commonlibrary.lcms.support.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commonlibrary.lcms.curriculum.service.CurriculumService;
import org.commonlibrary.lcms.folder.service.FolderService;
import org.commonlibrary.lcms.learningobject.service.LearningObjectService;
import org.commonlibrary.lcms.model.*;
import org.commonlibrary.lcms.model.metadata.*;
import org.commonlibrary.lcms.security.service.UserService;
import org.commonlibrary.lcms.support.spring.beans.Property;
import org.commonlibrary.lcms.userProfile.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

/**
 * @author Jeff Wysong
 *         Date: Sep 19, 2008
 *         Time: 4:52:56 PM
 */
@Component("sampleDataPopulator")
public class SampleDataPopulator {

    protected final transient Log logger = LogFactory.getLog(getClass());

    @Property("clv2.create.sample.data")
    private boolean createSampleData;

    private static final int NUMBER_OF_SAMPLE_USERS_TO_CREATE = 31;

    @Autowired
    @Qualifier(value = "transactionTemplate")
    private TransactionTemplate transactionTemplate;

    @Autowired
    private LearningObjectService learningObjectService;

    @Autowired
    private UserService userService;

    @Autowired
    private FolderService folderService;

    @Autowired
    private CurriculumService curriculumService;

    @Autowired
    private UserProfileService userProfileService;

    private LearningObject[] learningObjectArray;

    public SampleDataPopulator() {
    }

    public void setCreateSampleData(boolean createSampleData) {
        if (logger.isDebugEnabled()) {
            logger.debug("setting createSampleData = " + createSampleData);
        }
        this.createSampleData = createSampleData;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void setLearningObjectService(LearningObjectService learningObjectService) {
        this.learningObjectService = learningObjectService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setFolderService(FolderService folderService) {
        this.folderService = folderService;
    }

    public void setCurriculumService(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    public void setUserProfileService(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    public void insertSampleData() {
        if (!createSampleData || sampleUserExists()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping sample data bootstrapping");
            }
            return;
        }
        if (sampleUserExists()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping sample data bootstrapping");
            }
            return;
        }
        logger.error("Creating USERS regardless!!!!!!!!!!!!!!!!!!");
        if (logger.isDebugEnabled()) {
            logger.debug("Number of first names is " + FIRST_NAMES.length);
            logger.debug("Number of last names is " + LAST_NAMES.length);
            logger.debug("Number of nouns is " + NOUNS.length);
            logger.debug("Number of adjectives is " + ADJECTIVES.length);
        }
        addCreatorUser();
        createSampleUser();
        createRealNamedSampleUsers();
        createCurriculumSampleData();
        if (logger.isInfoEnabled()) {
            logger.info("Inserting sample learning objects into user test0@commonlibrary.org");
        }
        insertLearningObjectsTest();
        logger.debug("DONE WITH SAMPLE DATA!!!");
    }

    private void addCreatorUser() {
        User creator = new User();
        creator.setEnabled(true);
        creator.setPassword("creator");
        creator.setUsername("creator");
        creator.setFirstName("creator");
        creator.setLastName("creator");
        creator.setEmail("creator@commonlibrary.org");
        UserProfile userProfile = new UserProfile();
        userProfile.setName(creator.getUsername() + "-Profile.html");
        String profileId = userProfileService.create(userProfile);
        UserProfileMetadata userProfileMetadata = new UserProfileMetadata();
        userProfileService.addMetadata(userProfile, userProfileMetadata);
        creator.setUserProfile(userProfile);
        userService.create(creator);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("creator", "creator"));
    }

    private void createSampleUser() {
        Random randomGen = new Random();
        for (int x = 0; x < NUMBER_OF_SAMPLE_USERS_TO_CREATE; x++) {
            logger.debug("x = " + x);
            User user = new User();
            user.setUsername("test" + x + "@commonlibrary.org");
            user.setEmail("test" + x + "@commonlibrary.org");
            user.setPassword("password");
            user.setFirstName("test" + x);
            user.setLastName("Testor");
            UserProfile userProfile = new UserProfile();
            userProfile.setName(user.getUsername() + "-Profile.html");
            String profileId = userProfileService.create(userProfile);
            UserProfileMetadata userProfileMetadata = new UserProfileMetadata();
            userProfileService.addMetadata(userProfile, userProfileMetadata);
            user.setUserProfile(userProfile);
            int randomCountryIndex = randomGen.nextInt(Country.values().length);
            user.setCountry(Country.values()[randomCountryIndex]);
            user.setZip("31318");
            int randomIndex = randomGen.nextInt(Language.values().length);
            user.setGender(Gender.values()[randomIndex % 2]);
            user.setNativeLanguage(Language.values()[randomIndex]);
            userService.create(user);
            logger.debug("user with name {" + user.getUsername() + "} created!!!");
        }
        logger.debug("done with loop.");
    }

    private void createRealNamedSampleUsers() {
        Random randomGen = new Random();
        for (int u = 0; u < 200; u++) {
            User user = new User();
            String firstName;
            String lastName;
            String usernameString;
            String email;
            do {
                int firstNameIndex = randomGen.nextInt(FIRST_NAMES.length);
                firstName = FIRST_NAMES[firstNameIndex];
                int lastNameIndex = randomGen.nextInt(LAST_NAMES.length);
                lastName = LAST_NAMES[lastNameIndex];
                usernameString = firstName + "." + lastName + "@commonlibrary.org";
                email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@commonlibrary.org";
            } while (userService.findUserByUsername(usernameString) != null);
            user.setUsername(usernameString);
            user.setPassword("password");
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            UserProfile userProfile = new UserProfile();
            userProfile.setName(user.getUsername() + "-Profile.html");
            String profileId = userProfileService.create(userProfile);
            UserProfileMetadata userProfileMetadata = new UserProfileMetadata();
            userProfileService.addMetadata(userProfile, userProfileMetadata);
            user.setUserProfile(userProfile);
            int randomCountryIndex = randomGen.nextInt(Country.values().length);
            user.setCountry(Country.values()[randomCountryIndex]);
            user.setZip("31318");
            int randomIndex = randomGen.nextInt(Language.values().length);
            user.setGender(Gender.values()[randomIndex % 2]);
            user.setNativeLanguage(Language.values()[randomIndex]);
            userService.create(user);
            if (logger.isDebugEnabled()) {
                logger.debug("user with name {" + user.getUsername() + "} created!!!");
            }
        }
    }

    private boolean sampleUserExists() {
        return (userService.findUserByUsername("creator") != null);
    }

    /**
     * Data for test learning Objects. Set all data for create LO's
     */
    private void insertLearningObjectsTest() {
        Throwable thrown = (Throwable) transactionTemplate.execute(new TransactionCallback() {

            public Object doInTransaction(TransactionStatus transactionStatus) {
                Throwable thrown = null;
                try {
                    User user = userService.findUserByUsername("creator");
                    learningObjectArray = new LearningObject[10];
                    Metadata metadata = new Metadata();
                    metadata.setAgeFrom(1);
                    metadata.setAgeTo(2);
                    metadata.setContext(Context.ContinuousFormation);
                    metadata.setCoverage("Full");
                    metadata.setDegreeOfInteractivity(DegreeOfInteractivity.High);
                    metadata.setDescription("Historia del mundo");
                    metadata.setDifficulty(Difficulty.Medium);
                    metadata.setEndUser(IntendedUser.Learners);
                    metadata.setFormat(Format.HTML_XHTML);
                    metadata.setInteractivityType(InteractivityType.Active);
                    metadata.setKeywords("keyworks");
                    metadata.setLanguage(Language.Spanish);
                    metadata.setStatus(Status.Draft);
                    metadata.setSubject("Subject Test");
                    metadata.setResourceType(LearningResourceType.Exam);
                    LearningObject learningObject = new LearningObject();
                    learningObject.setName("Historia.html");
                    learningObject.setOwner(user);
                    learningObject.setTotalRating(3);
                    learningObject.setCreator(user);
                    learningObject.setContentSize(1024);
                    learningObject.setViewCount(4);
                    learningObjectService.create(learningObject);
                    learningObjectService.addMetadata(learningObject, metadata);
                    URL url = new URL("http://es.wikipedia.org/wiki/Caballeros_Templarios");
                    learningObjectService.addContent(learningObject, url.openStream());
                    metadata = new Metadata();
                    metadata.setAgeFrom(3);
                    metadata.setAgeTo(4);
                    metadata.setContext(Context.PrimaryEducation);
                    metadata.setCoverage("Full");
                    metadata.setDegreeOfInteractivity(DegreeOfInteractivity.Any);
                    metadata.setDescription("only for dark minds");
                    metadata.setDifficulty(Difficulty.High);
                    metadata.setEndUser(IntendedUser.Learners);
                    metadata.setFormat(Format.HTML_XHTML);
                    metadata.setInteractivityType(InteractivityType.Expositive);
                    metadata.setKeywords("keyworks");
                    metadata.setLanguage(Language.English);
                    metadata.setStatus(Status.Draft);
                    metadata.setSubject("Subject Test");
                    metadata.setResourceType(LearningResourceType.Exam);
                    learningObjectArray[0] = learningObject;
                    learningObject = new LearningObject();
                    learningObject.setName("Dark Sciencist.html");
                    learningObject.setOwner(user);
                    learningObject.setTotalRating(1);
                    learningObject.setCreator(user);
                    learningObject.setContentSize(1024);
                    learningObject.setViewCount(4);
                    learningObjectService.create(learningObject);
                    learningObjectService.addMetadata(learningObject, metadata);
                    url = new URL("http://en.wikipedia.org/wiki/Mathematics");
                    learningObjectService.addContent(learningObject, url.openStream());
                    learningObjectArray[1] = learningObject;
                    metadata = new Metadata();
                    metadata.setAgeFrom(3);
                    metadata.setAgeTo(4);
                    metadata.setContext(Context.ProfessionalFormation);
                    metadata.setCoverage("Full");
                    metadata.setDegreeOfInteractivity(DegreeOfInteractivity.Medium);
                    metadata.setDescription("Sale campeon... sale sale sale campeon...");
                    metadata.setDifficulty(Difficulty.High);
                    metadata.setEndUser(IntendedUser.Any);
                    metadata.setFormat(Format.HTML_XHTML);
                    metadata.setInteractivityType(InteractivityType.Mixed);
                    metadata.setKeywords("keyworks");
                    metadata.setLanguage(Language.Spanish);
                    metadata.setStatus(Status.Final);
                    metadata.setSubject("Subject Test");
                    metadata.setResourceType(LearningResourceType.Exam);
                    learningObject = new LearningObject();
                    learningObject.setName("Deportes.html");
                    learningObject.setOwner(user);
                    learningObject.setTotalRating(1);
                    learningObject.setCreator(user);
                    learningObject.setContentSize(1024);
                    learningObject.setViewCount(4);
                    learningObjectService.create(learningObject);
                    learningObjectService.addMetadata(learningObject, metadata);
                    url = new URL("http://es.wikipedia.org/wiki/Futbol");
                    learningObjectService.addContent(learningObject, url.openStream());
                    learningObjectArray[2] = learningObject;
                    metadata = new Metadata();
                    metadata.setAgeFrom(3);
                    metadata.setAgeTo(4);
                    metadata.setContext(Context.ProfessionalFormation);
                    metadata.setCoverage("Full");
                    metadata.setDegreeOfInteractivity(DegreeOfInteractivity.Medium);
                    metadata.setDescription("Sale campeon... sale sale sale campeon...");
                    metadata.setDifficulty(Difficulty.High);
                    metadata.setEndUser(IntendedUser.Any);
                    metadata.setFormat(Format.HTML_XHTML);
                    metadata.setInteractivityType(InteractivityType.Mixed);
                    metadata.setKeywords("keyworks");
                    metadata.setLanguage(Language.Spanish);
                    metadata.setStatus(Status.Final);
                    metadata.setSubject("Subject Test");
                    metadata.setResourceType(LearningResourceType.Exam);
                    learningObject = new LearningObject();
                    learningObject.setName("Equipos grandes del mundo.html");
                    learningObject.setOwner(user);
                    learningObject.setTotalRating(1);
                    learningObject.setCreator(user);
                    learningObject.setContentSize(1024);
                    learningObject.setViewCount(4);
                    learningObjectService.create(learningObject);
                    learningObjectService.addMetadata(learningObject, metadata);
                    url = new URL("http://es.wikipedia.org/wiki/Deportivo_Saprissa");
                    learningObjectService.addContent(learningObject, url.openStream());
                    learningObjectArray[3] = learningObject;
                    metadata = new Metadata();
                    metadata.setAgeFrom(3);
                    metadata.setAgeTo(4);
                    metadata.setContext(Context.ProfessionalFormation);
                    metadata.setCoverage("Full");
                    metadata.setDegreeOfInteractivity(DegreeOfInteractivity.Medium);
                    metadata.setDescription("The world");
                    metadata.setDifficulty(Difficulty.High);
                    metadata.setEndUser(IntendedUser.Any);
                    metadata.setFormat(Format.HTML_XHTML);
                    metadata.setInteractivityType(InteractivityType.Mixed);
                    metadata.setKeywords("keyworks");
                    metadata.setLanguage(Language.English);
                    metadata.setStatus(Status.Final);
                    metadata.setSubject("Subject Test");
                    metadata.setResourceType(LearningResourceType.Exam);
                    learningObject = new LearningObject();
                    learningObject.setName("City.html");
                    learningObject.setOwner(user);
                    learningObject.setTotalRating(1);
                    learningObject.setCreator(user);
                    learningObject.setContentSize(1024);
                    learningObject.setViewCount(4);
                    learningObjectService.create(learningObject);
                    learningObjectService.addMetadata(learningObject, metadata);
                    url = new URL("http://en.wikipedia.org/wiki/Atlanta");
                    learningObjectService.addContent(learningObject, url.openStream());
                    learningObjectArray[4] = learningObject;
                    learningObject = new LearningObject();
                    learningObject.setName("Wikipedia 1.html");
                    learningObject.setCreator(user);
                    learningObject.setOwner(user);
                    learningObjectService.create(learningObject);
                    url = new URL("http://es.wikipedia.org/wiki/Isla_de_Flores_(Indonesia)");
                    learningObjectService.addContent(learningObject, url.openStream());
                    metadata = new Metadata();
                    metadata.setAgeFrom(10);
                    metadata.setAgeTo(15);
                    metadata.setContext(Context.SecondaryEducation);
                    metadata.setCoverage("coverage");
                    metadata.setDegreeOfInteractivity(DegreeOfInteractivity.Medium);
                    metadata.setDescription("description");
                    metadata.setDifficulty(Difficulty.Medium);
                    metadata.setEndUser(IntendedUser.Learners);
                    metadata.setFormat(Format.HTML_XHTML);
                    metadata.setInteractivityType(InteractivityType.Interactive);
                    metadata.setKeywords("keywords");
                    metadata.setLanguage(Language.Spanish);
                    metadata.setSubject("subject");
                    metadata.setResourceType(LearningResourceType.Exam);
                    learningObjectService.addMetadata(learningObject, metadata);
                    learningObjectArray[5] = learningObject;
                    learningObject = new LearningObject();
                    learningObject.setName("Amero.html");
                    learningObject.setCreator(user);
                    learningObject.setOwner(user);
                    learningObjectService.create(learningObject);
                    url = new URL("http://es.wikipedia.org/wiki/Amero");
                    learningObjectService.addContent(learningObject, url.openStream());
                    metadata = new Metadata();
                    metadata.setAgeFrom(15);
                    metadata.setAgeTo(80);
                    metadata.setContext(Context.ProfessionalFormation);
                    metadata.setCoverage("coverage");
                    metadata.setDegreeOfInteractivity(DegreeOfInteractivity.Low);
                    metadata.setDescription("description");
                    metadata.setDifficulty(Difficulty.Medium);
                    metadata.setEndUser(IntendedUser.Teachers);
                    metadata.setFormat(Format.HTML_XHTML);
                    metadata.setInteractivityType(InteractivityType.Expositive);
                    metadata.setKeywords("moneda");
                    metadata.setLanguage(Language.Spanish);
                    metadata.setSubject("subject");
                    metadata.setResourceType(LearningResourceType.Exam);
                    learningObjectService.addMetadata(learningObject, metadata);
                    learningObjectArray[6] = learningObject;
                    learningObject = new LearningObject();
                    learningObject.setName("Mitos y Leyendas.html");
                    learningObject.setCreator(user);
                    learningObject.setOwner(user);
                    learningObjectService.create(learningObject);
                    url = new URL("http://en.wikipedia.org/wiki/Differential_calculus");
                    learningObjectService.addContent(learningObject, url.openStream());
                    metadata = new Metadata();
                    metadata.setAgeFrom(8);
                    metadata.setAgeTo(12);
                    metadata.setContext(Context.PrimaryEducation);
                    metadata.setCoverage("coverage");
                    metadata.setDegreeOfInteractivity(DegreeOfInteractivity.Medium);
                    metadata.setDescription("description");
                    metadata.setDifficulty(Difficulty.Medium);
                    metadata.setEndUser(IntendedUser.Learners);
                    metadata.setFormat(Format.HTML_XHTML);
                    metadata.setInteractivityType(InteractivityType.Interactive);
                    metadata.setKeywords("leyendas");
                    metadata.setLanguage(Language.Spanish);
                    metadata.setSubject("subject");
                    metadata.setResourceType(LearningResourceType.Exam);
                    learningObjectService.addMetadata(learningObject, metadata);
                    learningObjectArray[7] = learningObject;
                    learningObject = new LearningObject();
                    learningObject.setName("National Parks.html");
                    learningObject.setCreator(user);
                    learningObject.setOwner(user);
                    learningObjectService.create(learningObject);
                    url = new URL("http://en.wikipedia.org/wiki/National_parks");
                    learningObjectService.addContent(learningObject, url.openStream());
                    metadata = new Metadata();
                    metadata.setAgeFrom(13);
                    metadata.setAgeTo(17);
                    metadata.setContext(Context.SecondaryEducation);
                    metadata.setCoverage("coverage");
                    metadata.setDegreeOfInteractivity(DegreeOfInteractivity.Medium);
                    metadata.setDescription("description");
                    metadata.setDifficulty(Difficulty.Medium);
                    metadata.setEndUser(IntendedUser.Learners);
                    metadata.setFormat(Format.HTML_XHTML);
                    metadata.setInteractivityType(InteractivityType.Interactive);
                    metadata.setKeywords("parks");
                    metadata.setLanguage(Language.English);
                    metadata.setSubject("National Parks");
                    metadata.setResourceType(LearningResourceType.Exam);
                    learningObjectService.addMetadata(learningObject, metadata);
                    learningObjectArray[8] = learningObject;
                    learningObject = new LearningObject();
                    learningObject.setName("Waterfalls.html");
                    learningObject.setCreator(userService.getSystemUser());
                    learningObject.setOwner(user);
                    learningObjectService.create(learningObject);
                    url = new URL("http://en.wikipedia.org/wiki/Waterfalls");
                    learningObjectService.addContent(learningObject, url.openStream());
                    metadata = new Metadata();
                    metadata.setAgeFrom(13);
                    metadata.setAgeTo(17);
                    metadata.setContext(Context.SecondaryEducation);
                    metadata.setCoverage("coverage");
                    metadata.setDegreeOfInteractivity(DegreeOfInteractivity.Medium);
                    metadata.setDescription("description");
                    metadata.setDifficulty(Difficulty.Medium);
                    metadata.setEndUser(IntendedUser.Learners);
                    metadata.setFormat(Format.HTML_XHTML);
                    metadata.setInteractivityType(InteractivityType.Interactive);
                    metadata.setKeywords("waterfalls");
                    metadata.setLanguage(Language.English);
                    metadata.setSubject("Waterfalls");
                    metadata.setResourceType(LearningResourceType.Exam);
                    learningObjectService.addMetadata(learningObject, metadata);
                    learningObjectArray[9] = learningObject;
                    learningObject = new LearningObject();
                    learningObject.setName("Diphysciaceae.html");
                    learningObject.setCreator(userService.getSystemUser());
                    learningObject.setOwner(user);
                    learningObjectService.create(learningObject);
                    url = new URL("http://en.wikipedia.org/wiki/Diphysciaceae");
                    learningObjectService.addContent(learningObject, url.openStream());
                    learningObjectService.addMetadata(learningObject, createMetadata("Diphysciaceae", "Diphysciaceae"));
                    learningObject = new LearningObject();
                    learningObject.setName("Dark Parks");
                    learningObject.setCreator(userService.getSystemUser());
                    learningObject.setOwner(user);
                    learningObjectService.create(learningObject);
                    metadata = new Metadata();
                    metadata.setAgeFrom(13);
                    metadata.setAgeTo(17);
                    metadata.setContext(Context.SecondaryEducation);
                    metadata.setCoverage("coverage");
                    metadata.setDegreeOfInteractivity(DegreeOfInteractivity.Medium);
                    metadata.setDescription("Liverpool");
                    metadata.setDifficulty(Difficulty.Medium);
                    metadata.setEndUser(IntendedUser.Learners);
                    metadata.setFormat(Format.HTML_XHTML);
                    metadata.setInteractivityType(InteractivityType.Interactive);
                    metadata.setKeywords("waterfalls");
                    metadata.setLanguage(Language.English);
                    metadata.setSubject("Waterfalls");
                    metadata.setResourceType(LearningResourceType.Exam);
                    learningObjectService.addMetadata(learningObject, metadata);
                } catch (Throwable t) {
                    transactionStatus.setRollbackOnly();
                    thrown = t;
                }
                return thrown;
            }
        });
        if (thrown != null) {
            if (logger.isErrorEnabled()) {
                StringWriter out = new StringWriter();
                thrown.printStackTrace(new PrintWriter(out));
                logger.error(thrown);
                logger.error(out.getBuffer());
            }
        }
    }

    private Metadata createMetadata(String description, String keywords) {
        Metadata metadata = new Metadata();
        metadata.setAgeFrom(13);
        metadata.setAgeTo(17);
        metadata.setContext(Context.SecondaryEducation);
        metadata.setCoverage("coverage");
        metadata.setDegreeOfInteractivity(DegreeOfInteractivity.Medium);
        metadata.setDescription(description);
        metadata.setDifficulty(Difficulty.Medium);
        metadata.setEndUser(IntendedUser.Learners);
        metadata.setFormat(Format.HTML_XHTML);
        metadata.setInteractivityType(InteractivityType.Interactive);
        metadata.setKeywords(keywords);
        metadata.setLanguage(Language.English);
        metadata.setSubject(keywords);
        metadata.setResourceType(LearningResourceType.Exam);
        return metadata;
    }

    private void createCurriculumSampleData() {
        Random randomGen = new Random();
        User ownerCreator = userService.findUserByUsername("creator");
        Folder parent = new Folder();
        parent.setName("parent-name");
        folderService.create(parent);
        Folder folder = new Folder();
        folder.setOwner(ownerCreator);
        folder.setName("sample_data_folder");
        folder.setContents(new ArrayList<Organizable>());
        folderService.create(parent, folder);
        insertLearningObjectsTest();
        for (int q = 0; q < 50; q++) {
            Curriculum curriculum = new Curriculum();
            curriculum.setCreator(ownerCreator);
            curriculum.setOwner(ownerCreator);
            curriculum.setName(ADJECTIVES[randomGen.nextInt(ADJECTIVES.length)] + " " + NOUNS[randomGen.nextInt(NOUNS.length)] + " " + NOUNS[randomGen.nextInt(NOUNS.length)]);
            curriculumService.create(curriculum, folder);
            Folder cfolder = new Folder();
            cfolder.setOwner(ownerCreator);
            cfolder.setName(NOUNS[randomGen.nextInt(NOUNS.length)] + Integer.toString(q));
            cfolder.setContents(new ArrayList<Organizable>());
            folderService.create(folder, cfolder);
            curriculumService.addFolder(curriculum, cfolder);
            curriculumService.addLearningObject(curriculum, learningObjectArray[randomGen.nextInt(9)]);
            folderService.addContent(cfolder, learningObjectArray[randomGen.nextInt(9)]);
        }
        Curriculum curriculum = new Curriculum();
        curriculum.setCreator(ownerCreator);
        curriculum.setOwner(ownerCreator);
        curriculum.setName("AdavanceSearch Curriculum");
        curriculumService.create(curriculum, folder);
        Folder cfolder = new Folder();
        cfolder.setOwner(ownerCreator);
        cfolder.setName("Curriculum Advance Search Folder");
        cfolder.setContents(new ArrayList<Organizable>());
        folderService.create(folder, cfolder);
        curriculumService.addFolder(curriculum, cfolder);
        curriculumService.addLearningObject(curriculum, learningObjectArray[randomGen.nextInt(9)]);
        folderService.addContent(cfolder, learningObjectArray[randomGen.nextInt(9)]);
        curriculum = new Curriculum();
        curriculum.setCreator(ownerCreator);
        curriculum.setOwner(ownerCreator);
        curriculum.setName("Tomorrow Never Dies");
        curriculumService.create(curriculum, folder);
        cfolder = new Folder();
        cfolder.setOwner(ownerCreator);
        cfolder.setName("Yes We Can");
        cfolder.setContents(new ArrayList<Organizable>());
        folderService.create(folder, cfolder);
        curriculumService.addFolder(curriculum, cfolder);
        curriculumService.addLearningObject(curriculum, learningObjectArray[randomGen.nextInt(9)]);
        folderService.addContent(cfolder, learningObjectArray[randomGen.nextInt(9)]);
        curriculumService.addMetadata(curriculum, createMetadata("Today is the day", "Firewall"));
        Metadata metadata = new Metadata();
        metadata.setAgeFrom(13);
        metadata.setAgeTo(17);
        metadata.setContext(Context.SecondaryEducation);
        metadata.setCoverage("coverage");
        metadata.setDegreeOfInteractivity(DegreeOfInteractivity.Medium);
        metadata.setDescription("Manchester");
        metadata.setDifficulty(Difficulty.Medium);
        metadata.setEndUser(IntendedUser.Learners);
        metadata.setFormat(Format.HTML_XHTML);
        metadata.setInteractivityType(InteractivityType.Interactive);
        metadata.setKeywords("Red Devils");
        metadata.setLanguage(Language.English);
        metadata.setSubject("Soccer");
        metadata.setResourceType(LearningResourceType.Exam);
        curriculum = new Curriculum();
        curriculum.setCreator(ownerCreator);
        curriculum.setOwner(ownerCreator);
        curriculum.setName("One option");
        curriculumService.create(curriculum, folder);
        curriculumService.addMetadata(curriculum, metadata);
    }

    private static final String[] LAST_NAMES = { "Alfort", "Allendorf", "Alsop", "Amaker", "Angus", "Annan", "Annesley", "Appleby", "Arbuthnot", "Armitage", "Artois", "Arundel", "Ashburton", "Astor", "Athill", "Athow", "Auchinleck", "Averill", "Ayres", "Balcombe", "Balfour", "Ballantine", "Ballantyne", "Bancroft", "Bannatyne", "Bar", "Barnum", "Barnwell", "Barringer", "Barrow", "Barry", "Barstow", "Barwick", "Beal", "Beckett", "Beckley", "Beckwith", "Bedford", "Beers", "Bellamy", "Bentley", "Bethune", "Beveridge", "Beverly", "Biggar", "Billings", "Bingham", "Binney", "Birch", "Birney", "Blackwood", "Blaisdale", "Blaney", "Bloss", "Bogue", "Bolingbroke", "Bolton", "Bonar", "Boswell", "Boughton", "Bourne", "Boynton", "Bradford", "Braine", "Brandon", "Breck", "Breckenridge", "Brentwood", "Briare", "Bridges", "Brienne", "Brierly", "Brighton", "Brocklesby", "Brodt", "Bromfield", "Bromley", "Brooks", "Brougham", "Broughton", "Bryne", "Buchan", "Buchanan", "Buckbee", "Buddington", "Buckley", "Bunnell", "Burbeck", "Burgos", "Burgoyne", "Burleigh", "Burnham", "Burns", "Burrard", "Burton", "Bushwell", "Butman", "Buxton", "Cadwell", "Calder", "Caldwell", "Carden", "Carey", "Carmichael", "Caw", "Caxton", "Cayly", "Chadwick", "Challoner", "Chetham", "Chatsey", "Chatsworth", "Chatterton", "Chedsey", "Chesebrough", "Chester", "Chichester", "Chilton", "Church", "Clauson", "Clavering", "Clay", "Cleveland", "Cliff", "Clifton", "Clum", "Cobb", "Cobern", "Cochran", "Cockburn", "Coffin", "Cogswell", "Colby", "Collamore", "Colley", "Colquite", "Colton", "Colven", "Conry", "Contin", "Conyers", "Coote", "Corbin", "Cornish", "Cornwallis", "Corrie", "Cotesworth", "Courtenay", "Coventry", "Cranston", "Crawford", "Crayford", "Croft", "Cross", "Crosswell", "Cullen", "Cummings", "Cunningham", "Cupar", "D'Oily", "Daggett", "Dalrymple", "Dalton", "Danforth", "Darby", "Davenport", "De Wilde", "Dearden", "Denio", "Derby", "Devenish", "Devlin", "Digby", "Dinsmor", "Dolbeer", "Dole", "Dorset", "Dudley", "Dumfries", "Dun", "Dunbar", "Dunham", "Dunipace", "Duppa", "Durban", "Durden", "Durham", "Dutton", "Dyke", "Eastcote", "Eberlee", "Edgecumbe", "Elphinstone", "Ely", "Emmet", "Esham", "Eton", "Eure", "Evelyn", "Ewell", "Fairholm", "Fales", "Falun", "Fanshaw", "Farnham", "Felton", "Ferrer", "Fife", "Fifield", "Filey", "Flanders", "Fleming", "Flint", "Foote", "Forbes", "Fordham", "Fosdyke", "Fotherby", "Fothergill", "Fotheringham", "Fremont", "Frothingham", "Fulham", "Fulsom", "Gano", "Garfield", "Garnet", "Garrison", "Gaston", "Gavet", "Geddes", "Gihon", "Gill", "Girdwood", "Girvan", "Glanville", "Glentworth", "Gliston", "Gloucester", "Goadby", "Goring", "Goudy", "Grasse", "Gray", "Greely", "Grimsby", "Hadley", "Haineau", "Halden", "Hales", "Halsey", "Halstead", "Ham", "Hamlin", "Hampton", "Hanley", "Hanna", "Harcourt", "Harding", "Hargill", "Harley", "Harrington", "Hartwell", "Hasbrouck", "Hastings", "Hatch", "Hatfield", "Hathaway", "Hatton", "Haverill", "Hayden", "Hedges", "Hedon", "Helling", "Henley", "Herndon", "Hernshaw", "Heyden", "Hinckley", "Hindon", "Hippisley", "Hipwood", "Holbech", "Holland", "Holmes", "Holsapple", "Holt", "Holywell", "Hope", "Horton", "Hosford", "Hough", "Houghton", "Houston", "Hubbell", "Huddleston", "Hull", "Hungerford", "Huntley", "Hutton", "Hyde", "Incledon", "Ingham", "Ingleby", "Ipres", "Ireton", "Irving", "Isham", "Islip", "Ives", "Kay", "Keith", "Kelsey", "Kelso", "Kendall", "Kent", "Kettle", "Kilburne", "Kilham", "Kinghorn", "Kingston", "Kinloch", "Kirby", "Kirkaldy", "Kirkham", "Kirkpatrick", "Knox", "Kyle", "Lacy", "Lancaster", "Langton", "Laycock", "Lechmere", "Leigh", "Leland", "Lester", "Leven", "Lewes", "Lewknor", "Lincoln", "Lindall", "Lindsay", "Linn", "Linton", "Lippencot", "Lismore", "Livingstone", "Lonsdale", "Ludlow", "Main", "Mansfield", "Mansle", "Mar", "Massey", "Mayo", "Mead", "Medcaf", "Menteth", "Merton", "Meyeul", "Middleditch", "Middleton", "Milbourne", "Mills", "Milthorpe", "Milton", "Moffatt", "Monroe", "Morley", "Moseley", "Moxley", "Mundy", "Munsel", "Nairne", "Nance", "Needham", "Newton", "Nisbett", "Nogent", "Norcutt", "Norfolk", "Northop", "Northumberland", "Norton", "Norwich", "Nottingham", "Oakham", "Ockham", "Ogilvie", "Ollendorff", "Olmstead", "Onslow", "Orchard", "Orr", "Orton", "Otter", "Ouseley", "Oxford", "Pangbourn", "Paris", "Parret", "Parsall", "Parshall", "Paxton", "Pedin", "Peebles", "Peele", "Pelham", "Pendleton", "Peney", "Pennington", "Percy", "Pevensey", "Pickering", "Pickersgill", "Playfair", "Pleasants", "Polley", "Pollock", "Poole", "Poulton", "Pressley", "Preston", "Pringle", "Radcliff", "Ralston", "Ramsey", "Ranney", "Rawdon", "Reddenhurst", "Rhodes", "Riddell", "Ripley", "Rochester", "Romanno", "Root", "Rowe", "Rowen", "Rue", "Rusbridge", "Rutherford", "Rynders", "Safford", "Sandford", "Saterlee", "Scarret", "Scroggs", "Seaford", "Seaforth", "Seaton", "Selby", "Selkirk", "Seton", "Severn", "Shaddock", "Shelley", "Sheppy", "Shiel", "Shrewsbury", "Shuckburgh", "Shurtliff", "Shute", "Slade", "Snodgrass", "Soule", "Southwell", "Spalding", "St. Albans", "Stanhope", "Stanley", "Stebbins", "Stein", "Stirling", "Stocking", "Stockton", "Stokes", "Stokesby", "Stone", "Stoughton", "Strain", "Sunderland", "Surtees", "Sutton", "Swift", "Swinburn", "Symington", "Tabor", "Tattersall", "Teddington", "Teesdale", "Tefft", "Telford", "Temes", "Thorn", "Thurston", "Thwaite", "Tichenor", "Tillinghast", "Tilton", "Ting", "Torry", "Toucey", "Tournay", "Tracy", "Troublefield", "Trowbridge", "Tuttle", "Twickenham", "Vesey", "Vielle", "Vine", "Waite", "Wakefield", "Wallop", "Walpole", "Walton", "Wample", "Wands", "Warburton", "Wardlaw", "Ware", "Warwick", "Washington", "Wassen", "Waters", "Way", "Wayland", "Weeden", "Welby", "Welden", "Wells", "Wemyss", "Wentworth", "Westmoreland", "Wetherby", "Witherspoon", "Wheaton", "Whittaker", "Wiggin", "Wilberforce", "Willoughby", "Wilton", "Wiltshire", "Winch", "Winchester", "Windham", "Windsor", "Wing", "Wingfield", "Winslow", "Winterton", "Winthrop", "Ware", "Wiswall", "Woolsey", "Woolley", "Worcester", "Wysong", "York" };

    public static final String[] FIRST_NAMES = { "Aidan", "Braden", "Kaden", "Ethan", "Caleb", "Noah", "Jaden", "Connor", "Landon", "Jacob", "Jackson", "Elijah", "Gavin", "Dylan", "Alexander", "Aaron", "Gabriel", "Tristan", "Benjamin", "Lucas", "Andrew", "Jack", "Nathan", "Logan", "Nicholas", "Joshua", "Owen", "Liam", "Matthew", "Cole", "Zachary", "William", "Dominic", "Adam", "Damien", "Isaac", "Ian", "Colin", "Brandon", "Adrian", "Chase", "Daniel", "James", "Sebastian", "Ryan", "Luke", "Seth", "Keagan", "Oliver", "Christopher", "Nathaniel", "Samuel", "Isaiah", "Hayden", "Anthony", "Mason", "Michael", "Jonathan", "Xander", "Evan", "Xavier", "Jace", "Brice", "Micah", "Justin", "Tyler", "Carter", "David", "Reece", "Brendan", "Cameron", "Wyatt", "Jake", "Thomas", "Blake", "Asher", "Maddox", "Jason", "Sean", "Parker", "Joseph", "Cody", "Riley", "Austin", "Finn", "Hunter", "Brody", "Garrett", "Jonah", "Christian", "Grayson", "Bradley", "Gage", "Trevor", "Miles", "Colton", "Brian", "Holden", "Henry", "Jared", "Ava", "Abigail", "Cailyn", "Madeline", "Isabella", "Emma", "Caitlyn", "Olivia", "Chloe", "Brianna", "Hannah", "Lilly", "Sophia", "Kaylee", "Haley", "Ella", "Adrianna", "Isabelle", "Arianna", "Grace", "Elizabeth", "Amelia", "Aaliyah", "Addison", "Alyssa", "Madison", "Audrey", "Kylie", "Keira", "Paige", "Allison", "Emily", "Savannah", "Abby", "Alanna", "Claire", "Layla", "Cadence", "Charlotte", "Annalise", "Bella", "Natalie", "Jadyn", "Maya", "Michaela", "Gabriella", "Sophie", "Sadie", "Sarah", "Faith", "Eva", "Riley", "Mia", "Scarlett", "Bailey", "Gabrielle", "Samantha", "Lauren", "Ashlyn", "Alexa", "Regan", "Rachel", "Bianca", "Zoe", "Kayla", "Anna", "Ashley", "Alexandra", "Amy", "Peyton", "Avery", "Carly", "Amber", "Alexis", "Autumn", "Catherine", "Leah", "Brooke", "Sienna", "Callie", "Mackenzie", "Erin", "Lucy", "Victoria", "Aubrey", "Julianna", "Jasmine", "Annabelle", "Lillian", "Jillian", "Aaralyn", "Megan", "Julia", "Aurora", "Abrianna", "Taylor", "Adrienne", "Violet", "Alexia", "Nicole" };

    public static final String[] NOUNS = { "able", "able", "account", "achieve", "acoustics", "act", "action", "activity", "actor", "addition", "adjustment", "advertisement", "advice", "aftermath", "afternoon", "afterthought", "agreement", "air", "airplane", "airport", "alarm", "amount", "amusement", "anger", "angle", "animal", "answer", "ant", "ants", "apparatus", "apparel", "apple", "apples", "appliance", "approval", "arch", "argument", "arithmetic", "arm", "army", "art", "attack", "attempt", "attention", "attraction", "aunt", "authority", "babies", "baby", "back", "badge", "bag", "bait", "balance", "ball", "balloon", "balls", "banana", "band", "base", "baseball", "basin", "basket", "basketball", "bat", "bath", "battle", "bead", "beam", "bean", "bear", "bears", "beast", "bed", "bedroom", "beds", "bee", "beef", "beetle", "beggar", "beginner", "behavior", "belief", "believe", "bell", "bells", "berry", "bike", "bikes", "bird", "birds", "birth", "birthday", "bit", "bite", "blade", "blood", "blow", "board", "boat", "boats", "body", "bomb", "bone", "book", "books", "boot", "border", "bottle", "boundary", "box", "boy", "boys", "brain", "brake", "branch", "brass", "bread", "breakfast", "breath", "brick", "bridge", "brother", "brothers", "brush", "bubble", "bucket", "building", "bulb", "bun", "burn", "burst", "bushes", "business", "butter", "button", "cabbage", "cable", "cactus", "cake", "cakes", "calculator", "calendar", "camera", "camp", "can", "cannon", "canvas", "cap", "caption", "car", "card", "care", "carpenter", "carriage", "cars", "cart", "cast", "cat", "cats", "cattle", "cause", "cave", "celery", "cellar", "cemetery", "cent", "chain", "chair", "chairs", "chalk", "chance", "change", "channel", "cheese", "cherries", "cherry", "chess", "chicken", "chickens", "children", "chin", "church", "circle", "clam", "class", "clock", "clocks", "cloth", "cloud", "clouds", "clover", "club", "coach", "coal", "coast", "coat", "cobweb", "coil", "collar", "color", "comb", "comfort", "committee", "company", "comparison", "competition", "condition", "connection", "control", "cook", "copper", "copy", "cord", "cork", "corn", "cough", "country", "cover", "cow", "cows", "crack", "cracker", "crate", "crayon", "cream", "creator", "creature", "credit", "crib", "crime", "crook", "crow", "crowd", "crown", "crush", "cry", "cub", "cup", "current", "curtain", "curve", "cushion", "dad", "daughter", "day", "death", "debt", "decision", "deer", "degree", "design", "desire", "desk", "destruction", "detail", "development", "digestion", "dime", "dinner", "dinosaurs", "direction", "dirt", "discovery", "discussion", "disease", "disgust", "distance", "distribution", "division", "dock", "doctor", "dog", "dogs", "doll", "dolls", "donkey", "door", "downtown", "drain", "drawer", "dress", "drink", "driving", "drop", "drug", "drum", "duck", "ducks", "dust", "ear", "earth", "earthquake", "edge", "education", "effect", "egg", "eggnog", "eggs", "elbow", "end", "engine", "error", "event", "example", "exchange", "existence", "expansion", "experience", "expert", "eye", "eyes", "face", "fact", "fairies", "fall", "family", "fan", "fang", "farm", "farmer", "father", "father", "faucet", "fear", "feast", "feather", "feeling", "feet", "fiction", "field", "fifth", "fight", "finger", "finger", "fire", "fireman", "fish", "flag", "flame", "flavor", "flesh", "flight", "flock", "floor", "flower", "flowers", "fly", "fog", "fold", "food", "foot", "force", "fork", "form", "fowl", "frame", "friction", "friend", "friends", "frog", "frogs", "front", "fruit", "fuel", "furniture", "game", "garden", "gate", "geese", "ghost", "giants", "giraffe", "girl", "girls", "glass", "glove", "glue", "goat", "gold", "goldfish", "good-bye", "goose", "government", "governor", "grade", "grain", "grandfather", "grandmother", "grape", "grass", "grip", "ground", "group", "growth", "guide", "guitar", "gun", "hair", "haircut", "hall", "hammer", "hand", "hands", "harbor", "harmony", "hat", "hate", "head", "health", "hearing", "heart", "heat", "help", "hen", "hill", "history", "hobbies", "hole", "holiday", "home", "honey", "hook", "hope", "horn", "horse", "horses", "hose", "hospital", "hot", "hour", "house", "houses", "humor", "hydrant", "ice", "icicle", "idea", "impulse", "income", "increase", "industry", "ink", "insect", "instrument", "insurance", "interest", "invention", "iron", "island", "jail", "jam", "jar", "jeans", "jelly", "jellyfish", "jewel", "join", "joke", "journey", "judge", "juice", "jump", "kettle", "key", "kick", "kiss", "kite", "kitten", "kittens", "kitty", "knee", "knife", "knot", "knowledge", "laborer", "lace", "ladybug", "lake", "lamp", "land", "language", "laugh", "lawyer", "lead", "leaf", "learning", "leather", "leg", "legs", "letter", "letters", "lettuce", "level", "library", "lift", "light", "limit", "line", "linen", "lip", "liquid", "list", "lizards", "loaf", "lock", "locket", "look", "loss", "love", "low", "lumber", "lunch", "lunchroom", "machine", "magic", "maid", "mailbox", "man", "manager", "map", "marble", "mark", "market", "mask", "mass", "match", "meal", "measure", "meat", "meeting", "memory", "men", "metal", "mice", "middle", "milk", "mind", "mine", "minister", "mint", "minute", "mist", "mitten", "mom", "money", "monkey", "month", "moon", "morning", "mother", "motion", "mountain", "mouth", "move", "muscle", "music", "nail", "name", "nation", "neck", "need", "needle", "nerve", "nest", "net", "news", "night", "noise", "north", "nose", "note", "notebook", "number", "nut", "oatmeal", "observation", "ocean", "offer", "office", "oil", "operation", "opinion", "orange", "oranges", "order", "organization", "ornament", "oven", "owl", "owner", "page", "pail", "pain", "paint", "pan", "pancake", "paper", "parcel", "parent", "park", "part", "partner", "party", "passenger", "paste", "patch", "payment", "peace", "pear", "pen", "pencil", "person", "pest", "pet", "pets", "pickle", "picture", "pie", "pies", "pig", "pigs", "pin", "pipe", "pizzas", "place", "plane", "planes", "plant", "plantation", "plants", "plastic", "plate", "play", "playground", "pleasure", "plot", "plough", "pocket", "point", "poison", "police", "polish", "pollution", "popcorn", "porter", "position", "pot", "potato", "powder", "power", "price", "print", "prison", "process", "produce", "profit", "property", "prose", "protest", "pull", "pump", "punishment", "purpose", "push", "quarter", "quartz", "queen", "question", "quicksand", "quiet", "quill", "quilt", "quince", "quiver", "rabbit", "rabbits", "rail", "railway", "rain", "rainstorm", "rake", "range", "rat", "rate", "ray", "reaction", "reading", "reason", "receipt", "recess", "record", "regret", "relation", "religion", "representative", "request", "respect", "rest", "reward", "rhythm", "rice", "riddle", "rifle", "ring", "rings", "river", "road", "robin", "rock", "rod", "roll", "roof", "room", "root", "rose", "route", "rub", "rule", "run", "sack", "sail", "salt", "sand", "scale", "scarecrow", "scarf", "scene", "scent", "school", "science", "scissors", "screw", "sea", "seashore", "seat", "secretary", "seed", "selection", "self", "sense", "servant", "shade", "shake", "shame", "shape", "sheep", "sheet", "shelf", "ship", "shirt", "shock", "shoe", "shoes", "shop", "show", "side", "sidewalk", "sign", "silk", "silver", "sink", "sister", "sisters", "size", "skate", "skin", "skirt", "sky", "slave", "sleep", "sleet", "slip", "slope", "smash", "smell", "smile", "smoke", "snail", "snails", "snake", "snakes", "sneeze", "snow", "soap", "society", "sock", "soda", "sofa", "son", "song", "songs", "sort", "sound", "soup", "space", "spade", "spark", "spiders", "sponge", "spoon", "spot", "spring", "spy", "square", "squirrel", "stage", "stamp", "star", "start", "statement", "station", "steam", "steel", "stem", "step", "stew", "stick", "sticks", "stitch", "stocking", "stomach", "stone", "stop", "store", "story", "stove", "stranger", "straw", "stream", "street", "stretch", "string", "structure", "substance", "sugar", "suggestion", "suit", "summer", "sun", "support", "surprise", "sweater", "swim", "swing", "system", "table", "tail", "talk", "tank", "taste", "tax", "teaching", "team", "teeth", "temper", "tendency", "tent", "territory", "test", "texture", "theory", "thing", "things", "thought", "thread", "thrill", "throat", "throne", "thumb", "thunder", "ticket", "tiger", "time", "tin", "title", "toad", "toe", "toes", "tomatoes", "tongue", "tooth", "toothbrush", "toothpaste", "top", "touch", "town", "toy", "toys", "trade", "trail", "train", "trains", "tramp", "transport", "tray", "treatment", "tree", "trees", "trick", "trip", "trouble", "trousers", "truck", "trucks", "tub", "turkey", "turn", "twig", "twist", "umbrella", "uncle", "underwear", "unit", "use", "vacation", "value", "van", "vase", "vegetable", "veil", "vein", "verse", "vessel", "vest", "view", "visitor", "voice", "volcano", "volleyball", "voyage", "walk", "wall", "war", "wash", "waste", "watch", "water", "wave", "waves", "wax", "way", "wealth", "weather", "week", "weight", "wheel", "whip", "whistle", "wilderness", "wind", "window", "wine", "wing", "winter", "wire", "wish", "woman", "women", "wood", "wool", "word", "work", "worm", "wound", "wren", "wrench", "wrist", "writer", "writing", "yak", "yam", "yard", "yarn", "year", "yoke", "zebra", "zephyr", "zinc", "zipper", "zoo" };

    public static final String[] ADJECTIVES = { "aback", "abaft", "abandoned", "abashed", "aberrant", "abhorrent", "abiding", "abject", "ablaze", "able", "abnormal", "aboard", "aboriginal", "abortive", "abounding", "abrasive", "abrupt", "absent", "absorbed", "absorbing", "abstracted", "absurd", "abundant", "abusive", "acceptable", "accessible", "accidental", "accurate", "acid", "acidic", "acoustic", "acrid", "actually", "ad hoc", "adamant", "adaptable", "addicted", "adhesive", "adjoining", "adorable", "adventurous", "afraid", "aggressive", "agonizing", "agreeable", "ahead", "ajar", "alcoholic", "alert", "alike", "alive", "alleged", "alluring", "aloof", "amazing", "ambiguous", "ambitious", "amuck", "amused", "amusing", "ancient", "angry", "animated", "annoyed", "annoying", "anxious", "apathetic", "aquatic", "aromatic", "arrogant", "ashamed", "aspiring", "assorted", "astonishing", "attractive", "auspicious", "automatic", "available", "average", "awake", "aware", "awesome", "awful", "axiomatic", "bad", "barbarous", "bashful", "bawdy", "beautiful", "befitting", "belligerent", "beneficial", "bent", "berserk", "best", "better", "bewildered", "big", "billowy", "bite-sized", "bitter", "bizarre", "black", "black-and-white", "bloody", "blue", "blue-eyed", "blushing", "boiling", "boorish", "bored", "boring", "bouncy", "boundless", "brainy", "brash", "brave", "brawny", "breakable", "breezy", "brief", "bright", "bright", "broad", "broken", "brown", "bumpy", "burly", "bustling", "busy", "cagey", "calculating", "callous", "calm", "capable", "capricious", "careful", "careless", "caring", "cautious", "ceaseless", "certain", "changeable", "charming", "cheap", "cheerful", "chemical", "chief", "childlike", "chilly", "chivalrous", "chubby", "chunky", "clammy", "classy", "clean", "clear", "clever", "cloistered", "cloudy", "closed", "clumsy", "cluttered", "coherent", "cold", "colorful", "colossal", "combative", "comfortable", "common", "complete", "complex", "concerned", "condemned", "confused", "conscious", "cooing", "cool", "cooperative", "coordinated", "courageous", "cowardly", "crabby", "craven", "crazy", "creepy", "crooked", "crowded", "cruel", "cuddly", "cultured", "cumbersome", "curious", "curly", "curved", "curvy", "cut", "cute", "cute", "cynical", "daffy", "daily", "damaged", "damaging", "damp", "dangerous", "dapper", "dark", "dashing", "dazzling", "dead", "deadpan", "deafening", "dear", "debonair", "decisive", "decorous", "deep", "deeply", "defeated", "defective", "defiant", "delicate", "delicious", "delightful", "demonic", "delirious", "dependent", "depressed", "deranged", "descriptive", "deserted", "detailed", "determined", "devilish", "didactic", "different", "difficult", "diligent", "direful", "dirty", "disagreeable", "disastrous", "discreet", "disgusted", "disgusting", "disillusioned", "dispensable", "distinct", "disturbed", "divergent", "dizzy", "domineering", "doubtful", "drab", "draconian", "dramatic", "dreary", "drunk", "dry", "dull", "dusty", "dusty", "dynamic", "dysfunctional", "eager", "early", "earsplitting", "earthy", "easy", "eatable", "economic", "educated", "efficacious", "efficient", "eight", "elastic", "elated", "elderly", "electric", "elegant", "elfin", "elite", "embarrassed", "eminent", "empty", "enchanted", "enchanting", "encouraging", "endurable", "energetic", "enormous", "entertaining", "enthusiastic", "envious", "equable", "equal", "erect", "erratic", "ethereal", "evanescent", "evasive", "even", "excellent", "excited", "exciting", "exclusive", "exotic", "expensive", "extra-large", "extra-small", "exuberant", "exultant", "fabulous", "faded", "faint", "fair", "faithful", "fallacious", "false", "familiar", "famous", "fanatical", "fancy", "fantastic", "far", "far-flung", "fascinated", "fast", "fat", "faulty", "fearful", "fearless", "feeble", "feigned", "female", "fertile", "festive", "few", "fierce", "filthy", "fine", "finicky", "first", "five", "fixed", "flagrant", "flaky", "flashy", "flat", "flawless", "flimsy", "flippant", "flowery", "fluffy", "fluttering", "foamy", "foolish", "foregoing", "forgetful", "fortunate", "four", "frail", "fragile", "frantic", "free", "freezing", "frequent", "fresh", "fretful", "friendly", "frightened", "frightening", "full", "fumbling", "functional", "funny", "furry", "furtive", "future", "futuristic", "fuzzy", "gabby", "gainful", "gamy", "gaping", "garrulous", "gaudy", "general", "gentle", "giant", "giddy", "gifted", "gigantic", "glamorous", "gleaming", "glib", "glistening", "glorious", "glossy", "godly", "good", "goofy", "gorgeous", "graceful", "grandiose", "grateful", "gratis", "gray", "greasy", "great", "greedy", "green", "grey", "grieving", "groovy", "grotesque", "grouchy", "grubby", "gruesome", "grumpy", "guarded", "guiltless", "gullible", "gusty", "guttural", "habitual", "half", "hallowed", "halting", "handsome", "handsomely", "handy", "hanging", "hapless", "happy", "hard", "hard-to-find", "harmonious", "harsh", "hateful", "heady", "healthy", "heartbreaking", "heavenly", "heavy", "hellish", "helpful", "helpless", "hesitant", "hideous", "high", "highfalutin", "high-pitched", "hilarious", "hissing", "historical", "holistic", "hollow", "homeless", "homely", "honorable", "horrible", "hospitable", "hot", "huge", "hulking", "humdrum", "humorous", "hungry", "hurried", "hurt", "hushed", "husky", "hypnotic", "hysterical", "icky", "icy", "idiotic", "ignorant", "ill", "illegal", "ill-fated", "ill-informed", "illustrious", "imaginary", "immense", "imminent", "impartial", "imperfect", "impolite", "important", "imported", "impossible", "incandescent", "incompetent", "inconclusive", "industrious", "incredible", "inexpensive", "infamous", "innate", "innocent", "inquisitive", "insidious", "instinctive", "intelligent", "interesting", "internal", "invincible", "irate", "irritating", "itchy", "jaded", "jagged", "jazzy", "jealous", "jittery", "jobless", "jolly", "joyous", "judicious", "juicy", "jumbled", "jumpy", "juvenile", "kaput", "keen", "kind", "kindhearted", "kindly", "knotty", "knowing", "knowledgeable", "known", "labored", "lackadaisical", "lacking", "lame", "lamentable", "languid", "large", "last", "late", "laughable", "lavish", "lazy", "lean", "learned", "left", "legal", "lethal", "level", "lewd", "light", "like", "likeable", "limping", "literate", "little", "lively", "lively", "living", "lonely", "long", "longing", "long-term", "loose", "lopsided", "loud", "loutish", "lovely", "loving", "low", "lowly", "lucky", "ludicrous", "lumpy", "lush", "luxuriant", "lying", "lyrical", "macabre", "macho", "maddening", "madly", "magenta", "magical", "magnificent", "majestic", "makeshift", "male", "malicious", "mammoth", "maniacal", "many", "marked", "massive", "married", "marvelous", "material", "materialistic", "mature", "mean", "measly", "meaty", "medical", "meek", "mellow", "melodic", "melted", "merciful", "mere", "messy", "mighty", "military", "milky", "mindless", "miniature", "minor", "miscreant", "misty", "mixed", "moaning", "modern", "moldy", "momentous", "motionless", "mountainous", "muddled", "mundane", "murky", "mushy", "mute", "mysterious", "naive", "nappy", "narrow", "nasty", "natural", "naughty", "nauseating", "near", "neat", "nebulous", "necessary", "needless", "needy", "neighborly", "nervous", "new", "next", "nice", "nifty", "nimble", "nine", "nippy", "noiseless", "noisy", "nonchalant", "nondescript", "nonstop", "normal", "nostalgic", "nosy", "noxious", "null", "numberless", "numerous", "nutritious", "nutty", "oafish", "obedient", "obeisant", "obese", "obnoxious", "obscene", "obsequious", "observant", "obsolete", "obtainable", "oceanic", "odd", "offbeat", "old", "old-fashioned", "omniscient", "one", "onerous", "open", "opposite", "optimal", "orange", "ordinary", "organic", "ossified", "outgoing", "outrageous", "outstanding", "oval", "overconfident", "overjoyed", "overrated", "overt", "overwrought", "painful", "painstaking", "pale", "paltry", "panicky", "panoramic", "parallel", "parched", "parsimonious", "past", "pastoral", "pathetic", "peaceful", "penitent", "perfect", "periodic", "permissible", "perpetual", "petite", "petite", "phobic", "physical", "picayune", "pink", "piquant", "placid", "plain", "plant", "plastic", "plausible", "pleasant", "plucky", "pointless", "poised", "polite", "political", "poor", "possessive", "possible", "powerful", "precious", "premium", "present", "pretty", "previous", "pricey", "prickly", "private", "probable", "productive", "profuse", "protective", "proud", "psychedelic", "psychotic", "public", "puffy", "pumped", "puny", "purple", "purring", "pushy", "puzzled", "puzzling", "quack", "quaint", "quarrelsome", "questionable", "quick", "quickest", "quiet", "quirky", "quixotic", "quizzical", "rabid", "racial", "ragged", "rainy", "rambunctious", "rampant", "rapid", "rare", "raspy", "ratty", "ready", "real", "rebel", "receptive", "recondite", "red", "redundant", "reflective", "regular", "relieved", "remarkable", "reminiscent", "repulsive", "resolute", "resonant", "responsible", "rhetorical", "rich", "right", "righteous", "rightful", "rigid", "ripe", "ritzy", "roasted", "robust", "romantic", "roomy", "rotten", "rough", "round", "royal", "ruddy", "rude", "rural", "rustic", "ruthless", "sable", "sad", "safe", "salty", "same", "sassy", "satisfying", "savory", "scandalous", "scarce", "scared", "scary", "scattered", "scientific", "scintillating", "scrawny", "screeching", "second", "second-hand", "secret", "secretive", "sedate", "seemly", "selective", "selfish", "separate", "serious", "shaggy", "shaky", "shallow", "sharp", "shiny", "shivering", "shocking", "short", "shrill", "shut", "shy", "sick", "silent", "silent", "silky", "silly", "simple", "simplistic", "sincere", "six", "skillful", "skinny", "sleepy", "slim", "slimy", "slippery", "sloppy", "slow", "small", "smart", "smelly", "smiling", "smoggy", "smooth", "sneaky", "snobbish", "snotty", "soft", "soggy", "solid", "somber", "sophisticated", "sordid", "sore", "sore", "sour", "sparkling", "special", "spectacular", "spicy", "spiffy", "spiky", "spiritual", "spiteful", "splendid", "spooky", "spotless", "spotted", "spotty", "spurious", "squalid", "square", "squealing", "squeamish", "staking", "stale", "standing", "statuesque", "steadfast", "steady", "steep", "stereotyped", "sticky", "stiff", "stimulating", "stingy", "stormy", "straight", "strange", "striped", "strong", "stupendous", "stupid", "sturdy", "subdued", "subsequent", "substantial", "successful", "succinct", "sudden", "sulky", "super", "superb", "superficial", "supreme", "swanky", "sweet", "sweltering", "swift", "symptomatic", "synonymous", "taboo", "tacit", "tacky", "talented", "tall", "tame", "tan", "tangible", "tangy", "tart", "tasteful", "tasteless", "tasty", "tawdry", "tearful", "tedious", "teeny", "teeny-tiny", "telling", "temporary", "ten", "tender", "tense", "tense", "tenuous", "terrible", "terrific", "tested", "testy", "thankful", "therapeutic", "thick", "thin", "thinkable", "third", "thirsty", "thirsty", "thoughtful", "thoughtless", "threatening", "three", "thundering", "tidy", "tight", "tightfisted", "tiny", "tired", "tiresome", "toothsome", "torpid", "tough", "towering", "tranquil", "trashy", "tremendous", "tricky", "trite", "troubled", "truculent", "true", "truthful", "two", "typical", "ubiquitous", "ugliest", "ugly", "ultra", "unable", "unaccountable", "unadvised", "unarmed", "unbecoming", "unbiased", "uncovered", "understood", "undesirable", "unequal", "unequaled", "uneven", "unhealthy", "uninterested", "unique", "unkempt", "unknown", "unnatural", "unruly", "unsightly", "unsuitable", "untidy", "unused", "unusual", "unwieldy", "unwritten", "upbeat", "uppity", "upset", "uptight", "used", "useful", "useless", "utopian", "utter", "uttermost", "vacuous", "vagabond", "vague", "valuable", "various", "vast", "vengeful", "venomous", "verdant", "versed", "victorious", "vigorous", "violent", "violet", "vivacious", "voiceless", "volatile", "voracious", "vulgar", "wacky", "waggish", "waiting", "wakeful", "wandering", "wanting", "warlike", "warm", "wary", "wasteful", "watery", "weak", "wealthy", "weary", "well-groomed", "well-made", "well-off", "well-to-do", "wet", "whimsical", "whispering", "white", "whole", "wholesale", "wicked", "wide", "wide-eyed", "wiggly", "wild", "willing", "windy", "wiry", "wise", "wistful", "witty", "woebegone", "womanly", "wonderful", "wooden", "woozy", "workable", "worried", "worthless", "wrathful", "wretched", "wrong", "wry", "yellow", "yielding", "young", "youthful", "yummy", "zany", "zealous", "zesty", "zippy", "zonked" };
}
