package net.sf.evemsp.swa.ui;

import java.awt.BorderLayout;
import java.awt.Window;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import net.sf.evemsp.api.Account;
import net.sf.evemsp.api.CharImage;
import net.sf.evemsp.api.CharInfo;
import net.sf.evemsp.api.CharRecord;
import net.sf.evemsp.api.CharSkill;
import net.sf.evemsp.api.CharSkillGroup;
import net.sf.evemsp.api.Skill;
import net.sf.evemsp.api.SkillGroup;
import net.sf.evemsp.api.io.EveApiCall;
import net.sf.evemsp.api.io.EveApiResultListener;
import net.sf.evemsp.api.io.GetAccountCharacters;
import net.sf.evemsp.api.io.GetCharSkills;
import net.sf.evemsp.api.io.GetCharTraining;
import net.sf.evemsp.api.db.HibernateUtil;
import net.sf.evemsp.swa.EveSwissArmyKnife;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.hibernate.Query;
import org.hibernate.Session;

public class AccountLoadDlg extends JDialog implements EveApiResultListener {

    SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    private static final Log log = LogFactory.getLog(AccountLoadDlg.class);

    private class CharSheetLoader implements EveApiResultListener {

        Session session;

        CharRecord charRecord;

        CharInfo charInfo;

        CharSkillGroup skillGroup;

        CharSheetLoader(Session session, CharRecord charRecord) {
            this.session = session;
            this.charRecord = charRecord;
            GetCharSkills getCharSkills = new GetCharSkills();
            getCharSkills.setUserID(account.getAccountId());
            String key = Util.isEmptyString(account.getLimitedApiKey()) ? account.getFullApiKey() : account.getLimitedApiKey();
            getCharSkills.setApiKey(key);
            getCharSkills.setCharacterID(charRecord.getCharacterId());
            getCharSkills.getInformation(this);
        }

        @Override
        public void setEveApiError(EveApiCall eveApiCall, Exception error) {
            log.error(error);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setEveApiResult(EveApiCall eveApiCall, String result) {
            log.info("Received " + result.length() + " bytes.");
            SAXReader reader = new SAXReader();
            try {
                Document document = reader.read(new ByteArrayInputStream(result.getBytes()));
                charInfo = charRecord.getCharDetails();
                boolean update = charInfo != null;
                if (!update) {
                    charInfo = new CharInfo();
                    charRecord.setCharDetails(charInfo);
                    charInfo.setCharacterId(charRecord);
                    charInfo.setRace(((Element) document.selectSingleNode("//result/race")).getText());
                    charInfo.setBloodLine(((Element) document.selectSingleNode("//result/bloodLine")).getText());
                    charInfo.setGender(((Element) document.selectSingleNode("//result/gender")).getText());
                    charInfo.setIntelligence(new Integer(((Element) document.selectSingleNode("//result/attributes/intelligence")).getText()));
                    charInfo.setMemory(new Integer(((Element) document.selectSingleNode("//result/attributes/memory")).getText()));
                    charInfo.setPerception(new Integer(((Element) document.selectSingleNode("//result/attributes/perception")).getText()));
                    charInfo.setWillpower(new Integer(((Element) document.selectSingleNode("//result/attributes/willpower")).getText()));
                    charInfo.setCharisma(new Integer(((Element) document.selectSingleNode("//result/attributes/charisma")).getText()));
                    charInfo.setIntelligenceImplant(0);
                    charInfo.setMemoryImplant(0);
                    charInfo.setPerceptionImplant(0);
                    charInfo.setWillpowerImplant(0);
                    charInfo.setCharismaImplant(0);
                }
                charInfo.setBalance(Double.parseDouble(((Element) document.selectSingleNode("//result/balance")).getText()));
                Date cachedUntil;
                try {
                    cachedUntil = DF.parse(((Element) document.selectSingleNode("//cachedUntil")).getText() + " UTC");
                } catch (ParseException e) {
                    cachedUntil = new Date();
                }
                charInfo.setCacheTimestamp(cachedUntil);
                charRecord.setCorpId(Long.parseLong(((Element) document.selectSingleNode("//result/corporationID")).getText()));
                charRecord.setCorpName(((Element) document.selectSingleNode("//result/corporationName")).getText());
                if (update) {
                    session.update(charInfo);
                } else {
                    session.persist(charInfo);
                }
                List<Element> skills = document.selectNodes("//result/rowset[@name='skills']/*");
                Query sklQuery = session.createQuery("from " + Skill.class.getName() + " where skillId=?");
                Query chrSklGrpQuery = session.createQuery("from " + CharSkillGroup.class.getName() + " where characterId=? AND skillGroupId=?");
                Query chrSklQuery = session.createQuery("from " + CharSkill.class.getName() + " where characterId=? AND skillId=?");
                log.info("has " + skills.size() + " skills.");
                for (Element sklNode : skills) {
                    long sklId = Long.parseLong(sklNode.attributeValue("typeID"));
                    int sklPt = Integer.parseInt(sklNode.attributeValue("skillpoints"));
                    sklQuery.setLong(0, sklId);
                    List<Skill> tmp = sklQuery.list();
                    if (tmp.size() == 0) {
                        log.error("Skill with id \"" + sklId + "\" not found.");
                    } else {
                        Skill skl = tmp.get(0);
                        SkillGroup sklGrp = skl.getSkillGroup();
                        log.debug("Find " + sklGrp.getName() + "(" + skl.getName() + ")");
                        chrSklGrpQuery.setLong(0, charRecord.getCharacterId());
                        chrSklGrpQuery.setLong(1, sklGrp.getSkillGroupId());
                        List<CharSkillGroup> sklGrps = chrSklGrpQuery.list();
                        CharSkillGroup charSkillGroup = null;
                        update = sklGrps.size() == 1;
                        if (update) {
                            log.debug("update(" + sklGrp.getName() + ")");
                            charSkillGroup = sklGrps.get(0);
                        } else {
                            charSkillGroup = new CharSkillGroup(charRecord, sklGrp);
                            log.debug("persist(" + sklGrp.getName() + ")");
                            session.persist(charSkillGroup);
                            session.update(charSkillGroup);
                            charRecord.getCharSkillGroups().add(charSkillGroup);
                        }
                        int sklLvl;
                        if (sklPt == skl.getLevelFive().intValue()) {
                            sklLvl = 5;
                        } else {
                            if (sklPt >= skl.getLevelFour().intValue()) {
                                sklLvl = 4;
                            } else {
                                if (sklPt >= skl.getLevelThree().intValue()) {
                                    sklLvl = 3;
                                } else {
                                    if (sklPt >= skl.getLevelTwo().intValue()) {
                                        sklLvl = 2;
                                    } else {
                                        if (sklPt >= skl.getLevelOne().intValue()) {
                                            sklLvl = 1;
                                        } else {
                                            sklLvl = 0;
                                        }
                                    }
                                }
                            }
                        }
                        chrSklQuery.setLong(0, charRecord.getCharacterId());
                        chrSklQuery.setLong(1, skl.getSkillId());
                        List<CharSkill> chrSkls = chrSklQuery.list();
                        CharSkill chrSkl = null;
                        update = chrSkls.size() == 1;
                        if (update) {
                            log.debug("update(" + skl.getName() + ")");
                            chrSkl = chrSkls.get(0);
                            chrSkl.setSkillPoints(sklPt);
                            chrSkl.setSkillLevel(sklLvl);
                            session.update(chrSkl);
                        } else {
                            log.debug("persist(" + skl.getName() + ")");
                            chrSkl = new CharSkill(charSkillGroup, skl, sklPt, sklLvl);
                            session.persist(chrSkl);
                            charSkillGroup.getSkills().add(chrSkl);
                        }
                        session.update(charSkillGroup);
                    }
                }
                session.update(charRecord);
            } catch (DocumentException e) {
                log.error(e);
            } catch (RuntimeException re) {
                log.error("Argh", re);
            }
        }

        @Override
        public void setEveApiProgress(EveApiCall eveApiCall, int transferred, int size) {
        }
    }

    private class CharTrainingLoader implements EveApiResultListener {

        Session session;

        CharRecord charRecord;

        CharSkillGroup skillGroup;

        CharTrainingLoader(Session session, CharRecord charRecord) {
            this.session = session;
            this.charRecord = charRecord;
            GetCharTraining getCharTraining = new GetCharTraining();
            getCharTraining.setUserID(account.getAccountId());
            String key = Util.isEmptyString(account.getLimitedApiKey()) ? account.getFullApiKey() : account.getLimitedApiKey();
            getCharTraining.setApiKey(key);
            getCharTraining.setCharacterID(charRecord.getCharacterId());
            getCharTraining.getInformation(this);
        }

        @Override
        public void setEveApiError(EveApiCall eveApiCall, Exception error) {
            log.error(error);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setEveApiResult(EveApiCall eveApiCall, String result) {
            log.info("CharTrainingLoader(" + charRecord.getName() + ") received " + result.length() + " bytes.");
            SAXReader reader = new SAXReader();
            try {
                Document document = reader.read(new ByteArrayInputStream(result.getBytes()));
                Query queryTraining = session.createQuery("from " + CharSkill.class.getName() + " where characterId=? AND startPoints IS NOT NULL");
                queryTraining.setLong(0, charRecord.getCharacterId());
                List<CharSkill> chrTraining = queryTraining.list();
                CharSkill charTraining = chrTraining.size() == 0 ? null : chrTraining.get(0);
                boolean update = charTraining != null;
                int skillsTraining = Integer.parseInt(((Element) document.selectSingleNode("//result/skillInTraining")).getText());
                if (update) {
                    if (skillsTraining == 0) {
                        log.info(charRecord.getName() + " no longer training " + charTraining.getCharSkill().getName() + ".");
                        charTraining.setStartPoints(null);
                        charTraining.setStopPoints(null);
                        charTraining.setStartTimestamp(null);
                        charTraining.setStopTimestamp(null);
                        session.update(charTraining);
                    } else {
                        log.info("Update existing CharTraining record for " + charRecord.getName() + ".");
                        long trainingID = Long.parseLong(((Element) document.selectSingleNode("//result/trainingTypeID")).getText());
                        boolean skillChanged = trainingID != charTraining.getCharSkill().getSkillId();
                        if (skillChanged) {
                            log.info(charRecord.getName() + " training switched from " + charTraining.getCharSkill().getName() + " to ....");
                            charTraining.setStartPoints(null);
                            charTraining.setStopPoints(null);
                            charTraining.setStartTimestamp(null);
                            charTraining.setStopTimestamp(null);
                            session.update(charTraining);
                            Query queryCharSkill = session.createQuery("from " + CharSkill.class.getName() + " where characterId=? AND skillId=?");
                            queryCharSkill.setLong(0, charRecord.getCharacterId());
                            queryCharSkill.setLong(1, trainingID);
                            List<CharSkill> chrSkls = queryCharSkill.list();
                            charTraining = chrSkls.get(0);
                            log.info(charRecord.getName() + " training switched to " + charTraining.getCharSkill().getName() + ".");
                        }
                        charTraining.setStartPoints(new Integer(((Element) document.selectSingleNode("//result/trainingStartSP")).getText()));
                        charTraining.setStopPoints(new Integer(((Element) document.selectSingleNode("//result/trainingDestinationSP")).getText()));
                        Date start;
                        try {
                            start = DF.parse(((Element) document.selectSingleNode("//result/trainingStartTime")).getText() + " UTC");
                        } catch (ParseException e) {
                            start = new Date();
                        }
                        charTraining.setStartTimestamp(start);
                        Date stop;
                        try {
                            stop = DF.parse(((Element) document.selectSingleNode("//result/trainingEndTime")).getText() + " UTC");
                        } catch (ParseException e) {
                            stop = new Date();
                        }
                        charTraining.setStopTimestamp(stop);
                        session.update(charTraining);
                    }
                } else {
                    if (skillsTraining == 0) {
                        log.info(charRecord.getName() + " not training, nothing to do.");
                        update = false;
                    } else {
                        log.info("Set training details for " + charRecord.getName() + ".");
                        long trainingID = Long.parseLong(((Element) document.selectSingleNode("//result/trainingTypeID")).getText());
                        Query queryCharSkill = session.createQuery("from " + CharSkill.class.getName() + " where characterId=? AND skillId=?");
                        queryCharSkill.setLong(0, charRecord.getCharacterId());
                        queryCharSkill.setLong(1, trainingID);
                        List<CharSkill> chrSkls = queryCharSkill.list();
                        charTraining = chrSkls.get(0);
                        charTraining.setStartPoints(new Integer(((Element) document.selectSingleNode("//result/trainingStartSP")).getText()));
                        charTraining.setStopPoints(new Integer(((Element) document.selectSingleNode("//result/trainingDestinationSP")).getText()));
                        Date start;
                        try {
                            start = DF.parse(((Element) document.selectSingleNode("//result/trainingStartTime")).getText() + " UTC");
                        } catch (ParseException e) {
                            start = new Date();
                        }
                        charTraining.setStartTimestamp(start);
                        Date stop;
                        try {
                            stop = DF.parse(((Element) document.selectSingleNode("//result/trainingEndTime")).getText() + " UTC");
                        } catch (ParseException e) {
                            stop = new Date();
                        }
                        charTraining.setStopTimestamp(stop);
                        session.update(charTraining);
                    }
                }
            } catch (DocumentException e) {
                log.error(e);
            } catch (RuntimeException re) {
                log.error("Argh", re);
            }
        }

        @Override
        public void setEveApiProgress(EveApiCall eveApiCall, int transferred, int size) {
        }
    }

    private class SkillsLoader implements Runnable {

        public void run() {
            loadSkills();
        }
    }

    private JLabel action = new JLabel("Retrieve data from CCP");

    private JProgressBar progressBar = new JProgressBar(0, 100);

    private JProgressBar progressBar2 = new JProgressBar(0, 100);

    private Account account;

    public AccountLoadDlg(Window parent, Account account) {
        super(parent, "Loading account");
        setModal(true);
        this.account = account;
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        add(action, BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);
        add(progressBar2, BorderLayout.SOUTH);
        pack();
        Util.centerDialogOnOwner(this);
        Thread t = new Thread(new SkillsLoader());
        t.start();
        setVisible(true);
    }

    public void loadSkills() {
        GetAccountCharacters getAccountCharacters = new GetAccountCharacters();
        getAccountCharacters.setUserID(account.getAccountId());
        String key = Util.isEmptyString(account.getLimitedApiKey()) ? account.getFullApiKey() : account.getLimitedApiKey();
        getAccountCharacters.setApiKey(key);
        getAccountCharacters.getInformation(this);
    }

    @Override
    public void setEveApiError(EveApiCall eveApiCall, Exception error) {
        log.error(error);
        dispose();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setEveApiResult(EveApiCall eveApiCall, String result) {
        log.info("Data received from API");
        SAXReader reader = new SAXReader();
        try {
            Document document = reader.read(new ByteArrayInputStream(result.getBytes()));
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            session.beginTransaction();
            try {
                session.update(account);
                Query query = session.createQuery("from " + CharRecord.class.getName() + " where characterId=?");
                List<Element> chrList = document.selectNodes("//result/rowset/*");
                for (Element chrNode : chrList) {
                    long charId = Long.parseLong(chrNode.attributeValue("characterID"));
                    query.setLong(0, charId);
                    CharRecord rec = null;
                    List<CharRecord> matchingChars = query.list();
                    boolean update = (matchingChars.size() == 1);
                    CharImage charImage = null;
                    if (update) {
                        rec = matchingChars.get(0);
                        charImage = rec.getCharImage();
                    } else {
                        rec = new CharRecord();
                        rec.setAccount(account);
                        rec.setCharacterId(charId);
                        rec.setName(chrNode.attributeValue("name"));
                        account.getCharacters().add(rec);
                    }
                    rec.setCorpId(Long.parseLong(chrNode.attributeValue("corporationID")));
                    rec.setCorpName(chrNode.attributeValue("corporationName"));
                    if (update) {
                        session.update(rec);
                    } else {
                        session.persist(rec);
                    }
                    if (charImage == null) {
                        byte[] imgData = getCharImage(rec.getCharacterId());
                        if (imgData != null) {
                            log.debug("char image raw bytes: " + imgData.length);
                            charImage = new CharImage(rec, imgData);
                            rec.setCharImage(charImage);
                            session.update(rec);
                        }
                    }
                    new CharSheetLoader(session, rec);
                    new CharTrainingLoader(session, rec);
                    ((EveSwissArmyKnife) getParent()).addUpdateCharacter(rec);
                }
            } finally {
                session.getTransaction().commit();
            }
        } catch (DocumentException e) {
            log.error(e);
        } catch (RuntimeException re) {
            log.error(re, re);
        }
        ((EveSwissArmyKnife) getParent()).characterUpdateCompleted();
        dispose();
    }

    @Override
    public void setEveApiProgress(EveApiCall eveApiCall, int transferred, int size) {
        System.out.println(transferred + "/" + size);
    }

    /**
	 * @return the account
	 */
    public Account getAccount() {
        return account;
    }

    private byte[] getCharImage(long chrId) {
        byte[] imgData = null;
        try {
            URL url = new URL("http://img.eve.is/serv.asp?s=256&c=" + chrId);
            URLConnection conn = url.openConnection();
            InputStream is = conn.getInputStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int data;
            try {
                while ((data = is.read()) >= 0) {
                    os.write(data);
                }
            } finally {
                is.close();
            }
            imgData = os.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imgData;
    }
}
