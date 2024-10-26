package org.inigma.waragent.view;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.inigma.iniglet.Iniglet;
import org.inigma.iniglet.MessageType;
import org.inigma.iniglet.utils.DynamicListener;
import org.inigma.iniglet.utils.SwtTimerTask;
import org.inigma.utopia.Account;
import org.inigma.utopia.Army;
import org.inigma.utopia.Kingdom;
import org.inigma.utopia.Military;
import org.inigma.utopia.Province;
import org.inigma.utopia.Science;
import org.inigma.utopia.Survey;
import org.inigma.utopia.utils.CalendarUtils;
import org.inigma.waragent.crud.AccountCrud;
import org.inigma.waragent.crud.SynchronizationHandler;
import org.xml.sax.SAXException;

public class AccountComposite extends Composite {

    private static Log logger = LogFactory.getLog(AccountComposite.class);

    private Account account;

    private SwtTimerTask syncTask;

    private Text syncUrlText;

    private Text syncLoginText;

    private Text syncPasswordText;

    private Text lastSync;

    private Label activeAccountReference;

    public AccountComposite(Composite parent, Account refAccount, Label activeAccountReference) {
        super(parent, SWT.NONE);
        this.account = refAccount;
        this.activeAccountReference = activeAccountReference;
        setLayout(new GridLayout(2, false));
        Button switchAccount = new Button(this, SWT.PUSH);
        switchAccount.setText("Make Active Account");
        switchAccount.addListener(SWT.Selection, new DynamicListener(this, "onSwitchAccount"));
        Group syncGroup = new Group(this, SWT.SHADOW_ETCHED_IN);
        syncGroup.setText("Synchronization");
        syncGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        syncGroup.setLayout(new GridLayout(2, false));
        syncUrlText = addLabeledText(syncGroup, "Server URL: ", account.getSyncUrl());
        syncLoginText = addLabeledText(syncGroup, "Login: ", account.getSyncLogin());
        syncPasswordText = addLabeledText(syncGroup, "Password: ", account.getSyncPassword());
        lastSync = addLabeledText(syncGroup, "Last Sync: ", account.getLastSync().getTime().toString());
        lastSync.setEditable(false);
        lastSync.setEnabled(false);
        Composite buttonPanel = new Composite(syncGroup, SWT.NONE);
        buttonPanel.setLayout(new RowLayout(SWT.HORIZONTAL));
        buttonPanel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));
        Button apply = new Button(buttonPanel, SWT.PUSH);
        apply.setText("Apply Changes");
        apply.addListener(SWT.Selection, new DynamicListener(this, "onApplyChanges"));
        Button sync = new Button(buttonPanel, SWT.PUSH);
        sync.setText("Synchronize");
        sync.addListener(SWT.Selection, new DynamicListener(this, "onApplySync"));
        syncTask = new SwtTimerTask() {

            @Override
            public void task() {
                synchronize();
            }
        };
        syncTask.setEnabled((syncUrlText.getText().length() > 0));
    }

    @Override
    public void update() {
        syncUrlText.setText(account.getSyncUrl());
        syncLoginText.setText(account.getSyncLogin());
        syncPasswordText.setText(account.getSyncPassword());
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd @ hh:mm:ss");
        lastSync.setText(df.format(account.getLastSync().getTime()));
        super.update();
    }

    public SwtTimerTask getSyncTask() {
        return syncTask;
    }

    private Text addLabeledText(Composite parent, String label, String text) {
        Label l = new Label(parent, SWT.RIGHT | SWT.SHADOW_ETCHED_IN);
        l.setText(label);
        l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        Text t = new Text(parent, SWT.SINGLE | SWT.BORDER);
        t.setText(text);
        t.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        return t;
    }

    @SuppressWarnings("unused")
    private void onSwitchAccount(Event event) {
        AccountCrud.setActiveAccount(account);
        AccountCrud crud = new AccountCrud(account);
        try {
            activeAccountReference.setText(crud.getProvince().toString());
        } catch (SQLException e) {
            logger.error("Unable to retrieve account information!", e);
            Iniglet.displayNotification("Error", "Unable to retrieve account information!\n" + e.getMessage(), MessageType.Warn);
        }
    }

    @SuppressWarnings("unused")
    private void onApplyChanges(Event event) {
        try {
            AccountCrud crud = new AccountCrud(account);
            account.setSyncUrl(syncUrlText.getText());
            account.setSyncLogin(syncLoginText.getText());
            account.setSyncPassword(syncPasswordText.getText());
            crud.saveAccount();
            syncTask.setEnabled((syncUrlText.getText().length() > 0));
        } catch (SQLException e) {
            logger.error("Unable to retrieve account information!", e);
            Iniglet.displayNotification("Error", "Unable to retrieve account information!\n" + e.getMessage(), MessageType.Warn);
        }
    }

    @SuppressWarnings("unused")
    private void onApplySync(Event event) {
        synchronize();
    }

    private void synchronize() {
        Calendar syncTime = CalendarUtils.getCalendar();
        try {
            URL url = new URL(account.getSyncUrl());
            URLConnection connection = url.openConnection();
            connection.addRequestProperty("Accept-Encoding", "gzip,deflate");
            connection.setDoOutput(true);
            OutputStream out = connection.getOutputStream();
            out.write("login=".getBytes());
            out.write(account.getSyncLogin().getBytes());
            out.write("&password=".getBytes());
            out.write(account.getSyncPassword().getBytes());
            out.write("&lastSync=".getBytes());
            out.write(String.valueOf(account.getLastSync().getTimeInMillis()).getBytes());
            out.write("&data=".getBytes());
            try {
                out.write(toXml().getBytes());
            } catch (SQLException e) {
                logger.error("Unable to get account info!", e);
                Iniglet.displayNotification("Error", "Unable to sync data!\n" + e.getMessage(), MessageType.Error);
            }
            out.flush();
            out.close();
            String encoding = connection.getContentEncoding();
            InputStream in = null;
            if (encoding.contains("gzip")) {
                in = new GZIPInputStream(connection.getInputStream());
            } else if (encoding.contains("deflate")) {
                in = new InflaterInputStream(connection.getInputStream());
            } else {
                in = connection.getInputStream();
            }
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            AccountCrud crud = new AccountCrud(account);
            parser.parse(in, new SynchronizationHandler(crud));
            account.setLastSync(syncTime);
            crud.saveAccount();
            lastSync.setText(account.getLastSync().getTime().toString());
        } catch (IOException e) {
            logger.error("Synchronization Error", e);
            Iniglet.displayNotification("Error", e.getMessage(), MessageType.Error, true);
        } catch (ParserConfigurationException e) {
            logger.error("TODO", e);
            Iniglet.displayNotification("Error", e.getMessage(), MessageType.Error, true);
        } catch (SAXException e) {
            logger.error("TODO", e);
            Iniglet.displayNotification("Error", e.getMessage(), MessageType.Error, true);
        } catch (SQLException e) {
            logger.error("Unable to update account synchronization time!", e);
            Iniglet.displayNotification("Error", e.getMessage(), MessageType.Error, true);
        } catch (Exception e) {
            Iniglet.displayNotification("Error", e.getMessage(), MessageType.Error, true);
        }
    }

    private String toXml() throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("<data lastSync='").append(account.getLastSync().getTimeInMillis()).append("'>\n");
        AccountCrud crud = new AccountCrud(account);
        for (Kingdom kingdom : crud.getKingdomSyncList()) {
            sb.append(toXml(kingdom));
        }
        for (Province province : crud.getProvinceSyncList()) {
            sb.append(toXml(crud, province));
        }
        sb.append("</data>");
        return sb.toString();
    }

    private String toXml(AccountCrud crud, Province province) throws SQLException {
        Kingdom kingdom = crud.getKingdom(province.getKingdomId());
        StringBuilder sb = new StringBuilder("<province ");
        sb.append("lastUpdate='").append(province.getLastUpdate().getTimeInMillis()).append("' ");
        sb.append("kingdom='").append(kingdom.getLocation().getKingdom()).append("' ");
        sb.append("island='").append(kingdom.getLocation().getIsland()).append("' ");
        sb.append("name='").append(province.getName()).append("' ");
        sb.append("acres='").append(province.getAcres()).append("' ");
        sb.append("networth='").append(province.getNetworth()).append("' ");
        sb.append("race='").append(province.getRace()).append("' ");
        sb.append("rank='").append(province.getRank()).append("' ");
        if (province.getLeader() != null) {
            sb.append("gender='").append(province.isGender()).append("' ");
            sb.append("leader='").append(province.getLeader()).append("' ");
            sb.append("personality='").append(province.getPersonality()).append("' ");
            sb.append("peasants='").append(province.getPeasants()).append("' ");
            sb.append("gold='").append(province.getGold()).append("' ");
            sb.append("food='").append(province.getFood()).append("' ");
            sb.append("runes='").append(province.getRunes()).append("' ");
            sb.append("trade='").append(province.getTradeBalance()).append("' ");
            sb.append("thieves='").append(province.getThieves()).append("' ");
            sb.append("wizards='").append(province.getWizards()).append("' ");
            sb.append("soldiers='").append(province.getSoldiers()).append("' ");
            sb.append("offspecs='").append(province.getOffspecs()).append("' ");
            sb.append("defspecs='").append(province.getDefspecs()).append("' ");
            sb.append("elites='").append(province.getElites()).append("' ");
            sb.append("horses='").append(province.getHorses()).append("' ");
            sb.append("prisoners='").append(province.getPrisoners()).append("' ");
            sb.append("offense='").append(province.getOffense()).append("' ");
            sb.append("defense='").append(province.getDefense()).append("' ");
        }
        sb.append(">\n");
        Calendar sync = account.getLastSync();
        if (province.getScience().getLastUpdate().after(sync)) {
            sb.append(toXml(province.getScience()));
        }
        if (province.getSurvey().getLastUpdate().after(sync)) {
            sb.append(toXml(province.getSurvey()));
        }
        if (province.getMilitary().getLastUpdate().after(sync)) {
            sb.append(toXml(province.getMilitary()));
        }
        sb.append("</province>\n");
        return sb.toString();
    }

    private String toXml(Science science) {
        StringBuilder sb = new StringBuilder("<science ");
        sb.append("lastUpdate='").append(science.getLastUpdate().getTimeInMillis()).append("' ");
        sb.append("alchemy='").append(science.getAlchemy()).append("' ");
        sb.append("tools='").append(science.getTools()).append("' ");
        sb.append("housing='").append(science.getHousing()).append("' ");
        sb.append("food='").append(science.getFood()).append("' ");
        sb.append("military='").append(science.getMilitary()).append("' ");
        sb.append("crime='").append(science.getCrime()).append("' ");
        sb.append("channeling='").append(science.getChanneling()).append("' ");
        sb.append(" />\n");
        return sb.toString();
    }

    private String toXml(Military military) {
        StringBuilder sb = new StringBuilder("<military ");
        sb.append("lastUpdate='").append(military.getLastUpdate().getTimeInMillis()).append("' ");
        sb.append("raw='").append(military.isRaw()).append("' ");
        sb.append("offense='").append(military.getOffense()).append("' ");
        sb.append("defense='").append(military.getDefense()).append("' ");
        sb.append(">\n");
        for (Army army : military.getArmies()) {
            sb.append("<army ");
            sb.append("general='").append(army.getGenerals()).append("' ");
            sb.append("soldiers='").append(army.getSoldiers()).append("' ");
            sb.append("offspecs='").append(army.getOffspecs()).append("' ");
            sb.append("defspecs='").append(army.getDefspecs()).append("' ");
            sb.append("elites='").append(army.getElites()).append("' ");
            sb.append("horses='").append(army.getHorses()).append("' ");
            sb.append("spoils='").append(army.getSpoils()).append("' ");
            sb.append("eta='").append(army.getReturnTime().getTimeInMillis()).append("' ");
            sb.append("/>\n");
        }
        sb.append("</military>\n");
        return sb.toString();
    }

    private String toXml(Survey survey) {
        StringBuilder sb = new StringBuilder("<survey ");
        sb.append("lastUpdate='").append(survey.getLastUpdate().getTimeInMillis()).append("' ");
        sb.append("barrens='").append(survey.getBarren()).append("' ");
        sb.append("homes='").append(survey.getHomes()).append("' ");
        sb.append("farms='").append(survey.getFarms()).append("' ");
        sb.append("mills='").append(survey.getMills()).append("' ");
        sb.append("banks='").append(survey.getBanks()).append("' ");
        sb.append("trainingGrounds='").append(survey.getTrainingGrounds()).append("' ");
        sb.append("barracks='").append(survey.getBarracks()).append("' ");
        sb.append("armories='").append(survey.getArmories()).append("' ");
        sb.append("forts='").append(survey.getForts()).append("' ");
        sb.append("guardStations='").append(survey.getGuardStations()).append("' ");
        sb.append("hospitals='").append(survey.getHospitals()).append("' ");
        sb.append("guilds='").append(survey.getGuilds()).append("' ");
        sb.append("towers='").append(survey.getTowers()).append("' ");
        sb.append("thiefDens='").append(survey.getThievesDens()).append("' ");
        sb.append("watchtower='").append(survey.getWatchtowers()).append("' ");
        sb.append("libraries='").append(survey.getLibraries()).append("' ");
        sb.append("schools='").append(survey.getSchools()).append("' ");
        sb.append("stables='").append(survey.getStables()).append("' ");
        sb.append("dungeons='").append(survey.getDungeons()).append("' ");
        sb.append(" />\n");
        return sb.toString();
    }

    private String toXml(Kingdom kingdom) {
        StringBuilder sb = new StringBuilder("<kingdom ");
        sb.append("lastUpdate='").append(kingdom.getLastUpdate().getTimeInMillis()).append("' ");
        sb.append("kingdom='").append(kingdom.getLocation().getKingdom()).append("' ");
        sb.append("island='").append(kingdom.getLocation().getIsland()).append("' ");
        sb.append("name='").append(kingdom.getName()).append("' ");
        sb.append("relation='").append(kingdom.getRelation()).append("' ");
        sb.append("stance='").append(kingdom.getStance()).append("' ");
        sb.append("warCount='").append(kingdom.getWarCount()).append("' ");
        sb.append("warWins='").append(kingdom.getWarWins()).append("' ");
        sb.append("warNetworthDiff='").append(kingdom.getWarNetworthDiff()).append("' ");
        sb.append(" />\n");
        return sb.toString();
    }
}
