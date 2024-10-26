package de.icehorsetools.dataImport;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ugat.dataAccess.UnitOfWork;
import org.ugat.interfaces.IUnTransaction;
import org.ugat.utils.DateTimeUtils;
import org.ugat.wiser.language.Lang;
import de.icehorsetools.constants.ConfigCo;
import de.icehorsetools.constants.EntryStatusCo;
import de.icehorsetools.constants.FinancepaymentTypeCo;
import de.icehorsetools.constants.HorseSexCo;
import de.icehorsetools.constants.PersonSexCo;
import de.icehorsetools.dataAccess.objects.Entry;
import de.icehorsetools.dataAccess.objects.Financepayment;
import de.icehorsetools.dataAccess.objects.Horse;
import de.icehorsetools.dataAccess.objects.Participant;
import de.icehorsetools.dataAccess.objects.Person;
import de.icehorsetools.dataAccess.objects.Role;
import de.icehorsetools.dataAccess.objects.Test;
import de.icehorsetools.exeption.IcehorsetoolsRuntimeException;
import de.icehorsetools.exeption.NoNumberFormatException;
import de.icehorsetools.iceoffice.service.DefaultSvFactory;
import de.icehorsetools.iceoffice.service.configuration.IConfigurationSv;
import de.icehorsetools.interfaces.IIcehorestoolsDataAccess;

/**
 * imports the german IPZV online entries into the "FEIF Sport Database
 * Structure"
 *
 * @author kruegertom
 * @version $Id: OnlineEntriesDE.java 351 2009-07-27 16:05:06Z kruegertom $
 */
public class OnlineEntriesDE {

    private static final String AREA_NENNUNG = "nennung";

    private static final String AREA_TEILNEHMER = "teilnehmer";

    private static final String STATUS_SUCCESS = "7";

    private static Logger logger = Logger.getLogger(OnlineEntriesDE.class);

    private String configCompetitionNumber;

    private String configCompetitionPw;

    private String configUrlParticipants;

    private String configUrlEntries;

    private String configUrlSuccess;

    private String configProxyIp;

    private int configProxyPort;

    private UnitOfWork uow;

    private IIcehorestoolsDataAccess da;

    private IUnTransaction t;

    public OnlineEntriesDE() {
        IConfigurationSv cfg = DefaultSvFactory.getInstance().getConfigurationSv();
        this.configCompetitionNumber = cfg.getValueString(ConfigCo.GOE_COMPETITION_NUMBER);
        this.configCompetitionPw = cfg.getValueString(ConfigCo.GOE_COMPETITION_PASSWORD);
        this.configUrlParticipants = cfg.getValueString(ConfigCo.GOE_URL_PARTICIPANTS);
        this.configUrlEntries = cfg.getValueString(ConfigCo.GOE_URL_ENTRIES);
        this.configUrlSuccess = cfg.getValueString(ConfigCo.GOE_URL_SUCCESS);
        this.configProxyIp = cfg.getValueString(ConfigCo.COMMON_PROXY_IP);
        try {
            this.configProxyPort = cfg.getValueInt(ConfigCo.COMMON_PROXY_PORT);
        } catch (NoNumberFormatException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    public void startImport() {
        initialize();
        HashMap<String, String> dataListParticipant = getDataListParticipant();
        while (!dataListParticipant.isEmpty()) {
            Person person = getOrCreatePerson(dataListParticipant);
            da.insertOrUpdate(person);
            Horse horse = getOrCreateHorse(dataListParticipant);
            da.insertOrUpdate(horse);
            Participant participant = getOrCreateParticipant(dataListParticipant, person, horse);
            createFinancepayments(participant);
            da.insertOrUpdate(participant);
            String participantIPZV = dataListParticipant.get("NENNUNGSID");
            List<Entry> entriesList = getEntriesList(participantIPZV, participant);
            for (Entry entry : entriesList) {
                da.insertOrUpdate(entry);
            }
            boolean markRead = markEntriesRead(participantIPZV);
            if (markRead) {
                markRead = markParticipantRead(participantIPZV);
            }
            da.flush();
            dataListParticipant = getDataListParticipant();
        }
        terminate();
    }

    private void createFinancepayments(Participant participant) {
        Financepayment financepayment = null;
        if (participant.getPerbar() != 0.0) {
            financepayment = getFinancepayment(FinancepaymentTypeCo.CASH, participant.getPerbar(), participant);
            da.insertOrUpdate(financepayment);
        }
        if (participant.getPerscheck() != 0.0) {
            financepayment = getFinancepayment(FinancepaymentTypeCo.CHEQUE, participant.getPerscheck(), participant);
            da.insertOrUpdate(financepayment);
        }
        if (participant.getPerueberweisung() != 0.0) {
            financepayment = getFinancepayment(FinancepaymentTypeCo.BANK_TRANSACTION, participant.getPerueberweisung(), participant);
            da.insertOrUpdate(financepayment);
        }
        if (participant.getRueckerstattet() != 0.0) {
            financepayment = getFinancepayment(FinancepaymentTypeCo.BANK_TRANSACTION, -participant.getPerueberweisung(), participant);
            da.insertOrUpdate(financepayment);
        }
    }

    private Financepayment getFinancepayment(FinancepaymentTypeCo type, Double amount, Participant participant) {
        Financepayment financepayment = new Financepayment();
        DefaultSvFactory.getInstance().getFinancepaymentSv().initNewFinancepayment(financepayment);
        financepayment.setAmount(amount);
        financepayment.setType(type.toInteger());
        financepayment.setPersonPaidby(participant.getPerson());
        financepayment.setParticipant(participant);
        financepayment.setHorseRelated(participant.getHorse());
        financepayment.setPersonRelated(participant.getPerson());
        return financepayment;
    }

    /**
     * mark actual participant as read
     *
     * @param participantIPZV
     * @return
     */
    private boolean markParticipantRead(String participantIPZV) {
        boolean markReads = false;
        String strUrl = this.configUrlSuccess.replace("[competition]", this.configCompetitionNumber).replace("[password]", this.configCompetitionPw).replace("[participant]", participantIPZV).replace("[area]", AREA_TEILNEHMER).replace("[status]", STATUS_SUCCESS);
        Scanner scanner = getUrlScanner(strUrl);
        while (scanner.hasNextLine()) {
            String row = scanner.nextLine();
            if (!StringUtils.isBlank(row)) {
                logger.debug(row + " (mark actual participant as read)");
                markReads = "OK".equalsIgnoreCase(row);
                break;
            }
        }
        return markReads;
    }

    /**
     * mark entries for the actual participant as read
     *
     * @param participantIPZV
     * @return
     */
    private boolean markEntriesRead(String participantIPZV) {
        boolean markRead = false;
        {
            String strUrl = this.configUrlSuccess.replace("[competition]", this.configCompetitionNumber).replace("[password]", this.configCompetitionPw).replace("[participant]", participantIPZV).replace("[area]", AREA_NENNUNG).replace("[status]", STATUS_SUCCESS);
            Scanner scanner = getUrlScanner(strUrl);
            while (scanner.hasNextLine()) {
                String row = scanner.nextLine();
                if (!StringUtils.isBlank(row)) {
                    logger.debug(row + " (mark entries for the actual participant as read)");
                    markRead = "OK".equalsIgnoreCase(row);
                    break;
                }
            }
        }
        return markRead;
    }

    /**
     * @param dataList
     * @param person
     * @param horse
     * @return
     */
    private Participant getOrCreateParticipant(HashMap<String, String> dataList, Person person, Horse horse) {
        Participant participant = da.loadParticipantByPersonAndHorse(person, horse);
        if (participant == null) {
            participant = new Participant();
            DefaultSvFactory.getInstance().getParticipantSv().initNewParticipant(participant);
            participant.setPerson(person);
            participant.setHorse(horse);
            participant.setClub(dataList.get("VEREIN"));
            participant.setHorseId(horse.getHorseid());
            participant.setPersonId(person.getPersonid());
            participant.setSta(DefaultSvFactory.getInstance().getParticipantSv().getNextStartingNumber());
            participant.setStable(dataList.get("STALL"));
            participant.setExtra(Double.valueOf(dataList.get("EXTRA")));
            participant.setHelferfonds(Double.valueOf(dataList.get("HELFERFONDS")));
            participant.setNenngeld(Double.valueOf(dataList.get("NENNGELD")));
            participant.setPerbar(Double.valueOf(dataList.get("PERBAR")));
            participant.setPerscheck(Double.valueOf(dataList.get("PERSCHECK")));
            participant.setPerueberweisung(Double.valueOf(dataList.get("PER�BERWEISUNG")));
            participant.setProgrammheft(Double.valueOf(dataList.get("PROGRAMMHEFT")));
            participant.setRueckerstattet(Double.valueOf(dataList.get("R�CKERSTATTET")));
            participant.setSonstiges(Double.valueOf(dataList.get("SONSTIGES")));
            participant.setStallgeld(Double.valueOf(dataList.get("STALLGELD")));
            participant.setStartgeld(Double.valueOf(dataList.get("STARTGELD")));
            participant.setSumme(Double.valueOf(dataList.get("SUMME")));
        }
        Role role = DefaultSvFactory.getInstance().getRoleSv().getOrCreateRole("PARTICIPANT");
        if (!person.getRoles().contains(role)) {
            person.getRoles().add(role);
        }
        return participant;
    }

    /**
     * @param dataList
     * @return
     */
    private Horse getOrCreateHorse(HashMap<String, String> dataList) {
        Horse horse = da.loadHorseByHorseId(dataList.get("PBARCODE"));
        if (horse == null) {
            horse = new Horse();
            DefaultSvFactory.getInstance().getHorseSv().initNewHorse(horse);
            horse.setHorseid(dataList.get("PBARCODE"));
            horse.setBirthday(DateTimeUtils.stringToDate(dataList.get("PGEB")));
            horse.setBreeder(dataList.get("Z"));
            horse.setColor(dataList.get("PFARBE"));
            horse.setCountry(dataList.get("PLAND"));
            horse.setF(dataList.get("V"));
            horse.setFf(dataList.get("VV"));
            horse.setFm(dataList.get("VM"));
            horse.setM(dataList.get("M"));
            horse.setMf(dataList.get("MV"));
            horse.setMm(dataList.get("MM"));
            horse.setMarking(dataList.get("PABZ"));
            horse.setName(dataList.get("PFERD"));
            horse.setOwner(dataList.get("B"));
            if (dataList.get("PSEX").equalsIgnoreCase("W")) {
                horse.setSex((Integer) HorseSexCo.SEX_GELDING.toIdentifier());
            } else if (dataList.get("PSEX").equalsIgnoreCase("S")) {
                horse.setSex((Integer) HorseSexCo.SEX_MARE.toIdentifier());
            } else if (dataList.get("PSEX").equalsIgnoreCase("H")) {
                horse.setSex((Integer) HorseSexCo.SEX_STALLION.toIdentifier());
            }
        }
        return horse;
    }

    /**
     * @param dataList
     * @return
     */
    private Person getOrCreatePerson(HashMap<String, String> dataList) {
        Person person = da.loadPersonByPersonId(dataList.get("RBARCODE"));
        if (person == null) {
            person = new Person();
            DefaultSvFactory.getInstance().getPersonSv().initNewPerson(person);
            person.setPersonid(dataList.get("RBARCODE"));
            person.setNamefirst(dataList.get("VORNAME"));
            person.setNamelast(dataList.get("NACHNAME"));
            person.setAddress1(dataList.get("ANSCHRIFT1"));
            person.setAddress2(dataList.get("ANSCHRIFT2"));
            person.setZip(dataList.get("PLZ"));
            person.setCity(dataList.get("ORT"));
            person.setRegion(dataList.get("BUNDESLAND"));
            person.setCountry(dataList.get("STAAT"));
            person.setPhone(dataList.get("FON"));
            person.setFax(dataList.get("FAX"));
            person.setMobile(dataList.get("MOBIL"));
            person.setEmail(dataList.get("EMAIL"));
            person.setBirthday(DateTimeUtils.stringToDate(dataList.get("GEB")));
            person.setSex((Integer) (dataList.get("ANREDE").equalsIgnoreCase("F") ? PersonSexCo.SEX_FEMALE.toIdentifier() : PersonSexCo.SEX_MALE.toIdentifier()));
        }
        return person;
    }

    /**
     * @return
     */
    private HashMap<String, String> getDataListParticipant() {
        String strUrl = this.configUrlParticipants.replace("[competition]", this.configCompetitionNumber).replace("[password]", this.configCompetitionPw);
        Scanner scannerParticipants = getUrlScanner(strUrl);
        HashMap<String, String> dataList = new HashMap<String, String>();
        while (scannerParticipants.hasNextLine()) {
            String[] row = scannerParticipants.nextLine().split(":");
            if (row.length > 1) {
                dataList.put(row[0], row[1]);
                logger.debug(row[0] + "->" + row[1]);
            }
        }
        return dataList;
    }

    /**
     * @param participantIPZV
     * @param participant
     * @return
     */
    private List<Entry> getEntriesList(String participantIPZV, Participant participant) {
        String strUrl = this.configUrlEntries.replace("[competition]", this.configCompetitionNumber).replace("[password]", this.configCompetitionPw).replace("[participant]", participantIPZV);
        Scanner scannerEntries = getUrlScanner(strUrl);
        List<Entry> entryList = new ArrayList<Entry>();
        while (scannerEntries.hasNextLine()) {
            String[] row = scannerEntries.nextLine().split(";");
            if (row.length > 5) {
                entryList.add(getOrCreateEntry(row, participant));
                logger.debug(row[0] + "->" + row[1] + "->" + row[2] + "->" + row[3] + "->" + row[4] + "->" + row[5]);
            }
        }
        return entryList;
    }

    /**
     * @param row
     * @param participant
     * @return
     */
    private Entry getOrCreateEntry(String[] row, Participant participant) {
        String testCode = row[2];
        boolean rightRein = ("0".equalsIgnoreCase(row[3]) ? false : true);
        Integer entryStatus = 0;
        if ("VE".equalsIgnoreCase(row[4])) {
            entryStatus = (Integer) EntryStatusCo.PRELIMINARY.toIdentifier();
        } else if ("AF".equalsIgnoreCase(row[4])) {
            entryStatus = (Integer) EntryStatusCo.A_FINAL.toIdentifier();
        } else if ("BF".equalsIgnoreCase(row[4])) {
            entryStatus = (Integer) EntryStatusCo.B_FINAL.toIdentifier();
        } else if ("CF".equalsIgnoreCase(row[4])) {
            entryStatus = (Integer) EntryStatusCo.C_FINAL.toIdentifier();
        }
        Double qualification = Double.valueOf(row[5]);
        Test test = da.loadTestByCode(testCode);
        if (test == null) {
            test = new Test();
            DefaultSvFactory.getInstance().getTestSv().initNewTest(test);
            test.setCode(testCode);
            test.setNr(DefaultSvFactory.getInstance().getTestSv().getNextTestNumber());
            da.insertOrUpdate(test);
        }
        Entry entry = da.loadEntryByParticipantAndTest(participant, test);
        if (entry == null) {
            entry = new Entry();
            DefaultSvFactory.getInstance().getEntrySv().initNewEntry(entry);
            entry.setTest(test);
            entry.setCode(test.getCode());
            entry.setParticipant(participant);
            entry.setQualification(qualification);
            entry.setRr(rightRein);
            entry.setStatus(entryStatus);
        }
        return entry;
    }

    /**
     * get the content from the given url as {@link Scanner}
     *
     * @param strUrl
     * @return url-content as {@link Scanner}
     */
    private Scanner getUrlScanner(String strUrl) {
        URL urlParticipants = null;
        Scanner scannerParticipants;
        try {
            urlParticipants = new URL(strUrl);
            URLConnection connParticipants;
            if (StringUtils.isBlank(this.configProxyIp)) {
                connParticipants = urlParticipants.openConnection();
            } else {
                SocketAddress address = new InetSocketAddress(this.configProxyIp, this.configProxyPort);
                Proxy proxy = new Proxy(Proxy.Type.HTTP, address);
                connParticipants = urlParticipants.openConnection(proxy);
            }
            InputStream streamParticipant = connParticipants.getInputStream();
            String charSet = StringUtils.substringAfterLast(connParticipants.getContentType(), "charset=");
            scannerParticipants = new Scanner(streamParticipant, charSet);
        } catch (MalformedURLException e) {
            throw new IcehorsetoolsRuntimeException(MessageFormat.format(Lang.get(this.getClass(), "MalformedURLException"), new Object[] { urlParticipants.toString() }));
        } catch (IOException e) {
            throw new IcehorsetoolsRuntimeException(MessageFormat.format(Lang.get(this.getClass(), "IOException"), new Object[] { urlParticipants.toString() }));
        }
        return scannerParticipants;
    }

    /**
     *
     */
    private void initialize() {
        this.uow = UnitOfWork.Factory.createInstance();
        UnitOfWork.Factory.setCurrentUnitOfWork(uow);
        this.da = IIcehorestoolsDataAccess.Factory.getInstance(uow);
        this.da.setFlushModeToCommit();
        this.t = this.da.beginTransaction();
    }

    /**
     *
     */
    private void terminate() {
        this.t.commit();
        IIcehorestoolsDataAccess.Factory.getInstance().flush();
        this.uow.close();
        UnitOfWork.Factory.resetCurrentUnitOfWork();
    }
}
