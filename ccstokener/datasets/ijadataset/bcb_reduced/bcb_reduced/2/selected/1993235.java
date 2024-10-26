package org.mss.quartzjobs.wizards;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.mss.quartzjobs.CorePlugin;

public class JobWizardPage2 extends WizardPage {

    private Button validatebutton;

    private Text jobtext;

    private Combo templatecombo;

    private Combo schedulecombo;

    private Combo scheduledescription;

    private Composite quartzcomposite;

    private Log log = LogFactory.getLog(getClass());

    private final String MOZILLA_TEMPLATE = "Select Mozilla Template";

    private final String SAMPLE_CRON_STRINGS = "Sample CRON Expression";

    private final String SAMPLE_CRON_STRING_DESCRIPTION = "Sample CRON Description";

    private final String VALIDATION_EMPTY_MESSAGE = "cron string cannot be empty, please enter a cronstring";

    private final String VALIDATION_ERROR_MESSAGE = "Cron Expression invalid, please follow cron job examples";

    public final String CRONSTRING_TEXT = "CRON String";

    public final String PATTERN_TEXT = "CRON PATTERN";

    public final String JOBTEXTLABEL_TOOLTIP = "Enter Schedule String, it is a cron string ";

    private String cronreg = "(((([0-9]|[0-5][0-9]),)*([0-9]|[0-5][0-9]))|(([0-9]|[0-5][0-9])(/|-)([0-9]|[0-5][0-9]))|([\\?])|([\\*]))[\\s](((([0-9]|[0-5][0-9]),)*([0-9]|[0-5][0-9]))|(([0-9]|[0-5][0-9])(/|-)([0-9]|[0-5][0-9]))|([\\?])|([\\*]))[\\s](((([0-9]|[0-1][0-9]|[2][0-3]),)*([0-9]|[0-1][0-9]|[2][0-3]))|(([0-9]|[0-1][0-9]|[2][0-3])(/|-)([0-9]|[0-1][0-9]|[2][0-3]))|([\\?])|([\\*]))[\\s](((([1-9]|[0][1-9]|[1-2][0-9]|[3][0-1]),)*([1-9]|[0][1-9]|[1-2][0-9]|[3][0-1])(C)?)|(([1-9]|[0][1-9]|[1-2][0-9]|[3][0-1])(/|-)([1-9]|[0][1-9]|[1-2][0-9]|[3][0-1])(C)?)|(L)|([\\?])|([\\*]))[\\s](((([1-9]|0[1-9]|1[0-2]),)*([1-9]|0[1-9]|1[0-2]))|(([1-9]|0[1-9]|1[0-2])(/|-)([1-9]|0[1-9]|1[0-2]))|(((JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC),)*(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))|((JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(-|/)(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))|([\\?])|([\\*]))[\\s]((([1-7],)*([1-7]))|([1-7](/|-)([1-7]))|(((MON|TUE|WED|THU|FRI|SAT|SUN),)*(MON|TUE|WED|THU|FRI|SAT|SUN)(C)?)|((MON|TUE|WED|THU|FRI|SAT|SUN)(-|/)(MON|TUE|WED|THU|FRI|SAT|SUN)(C)?)|(([1-7]|(MON|TUE|WED|THU|FRI|SAT|SUN))?(L)?)|([1-7]#([1-7])?)|([\\?])|([\\*]))([\\s]19[7-9][0-9]|20[0-9]{2})?";

    /**
		 * @since 3.2
		 * 
		 */
    private static final class CrossFieldValidator implements IValidator {

        /**
			 * 
			 */
        private final IObservableValue other;

        /**
			 * @param model
			 */
        private CrossFieldValidator(IObservableValue other) {
            this.other = other;
        }

        public IStatus validate(Object value) {
            if (!value.equals(other.getValue())) {
                return ValidationStatus.ok();
            }
            return ValidationStatus.error("values cannot be the same");
        }
    }

    class CronJobDigitValidator implements IValidator {

        public CronJobDigitValidator() {
        }

        public IStatus validate(Object value) {
            String mystr = (String) value;
            if (mystr == null) {
                validatebutton.setEnabled(false);
                return ValidationStatus.info(VALIDATION_EMPTY_MESSAGE);
            }
            if (!mystr.matches(cronreg)) {
                log.debug("JobWizardPage 2: Cron Job String Invalid Error" + cronreg);
                validatebutton.setEnabled(false);
                setPageComplete(false);
                return ValidationStatus.error(VALIDATION_ERROR_MESSAGE);
            }
            if (mystr.matches(cronreg)) {
                log.debug("JobWizardPage 2: Cron Job String OK " + cronreg);
            }
            validatebutton.setEnabled(true);
            setPageComplete(true);
            ((JobWizard) getWizard()).getJobOverview().setSchedule_string(mystr);
            log.debug("Save jobstring " + mystr);
            return ValidationStatus.ok();
        }
    }

    static class Model {

        IObservableValue StringValue = new WritableValue(null, String.class);
    }

    public JobWizardPage2() {
        super("Select Job Details");
        setPageComplete(false);
    }

    /**
	     * Override Method; Eclipse RCP book page 448
	     * 
	     * Updates the content of the page when it becomes visible
	     * 
	     * @param visible
	     */
    public void setVisible(boolean visible) {
        String cronexpression = ((JobWizard) getWizard()).getJobOverview().getSchedule_string();
        jobtext.setText(cronexpression);
        templatecombo.setText(((JobWizard) getWizard()).getJobOverview().getTemplatename());
        initScheduleCombo(cronexpression);
        super.setVisible(visible);
    }

    public void createControl(Composite parent) {
        log.debug("JobWizard Page 2: ViewPart enter createControl " + parent.toString());
        Realm realm = SWTObservables.getRealm(parent.getShell().getDisplay());
        final DataBindingContext dbc = new DataBindingContext(realm);
        WizardPageSupport.create(this, dbc);
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = gridLayout.marginHeight = 0;
        container.setLayout(gridLayout);
        container.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        setControl(container);
        Composite currentdatecomposite = new Composite(container, SWT.NONE);
        gridLayout = new GridLayout(1, false);
        gridLayout.numColumns = 2;
        gridLayout.marginWidth = gridLayout.marginHeight = 0;
        currentdatecomposite.setLayout(gridLayout);
        currentdatecomposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        Label calendarlabel = new Label(currentdatecomposite, SWT.NULL);
        calendarlabel.setText("Calendar");
        final DateTime calendar = new DateTime(currentdatecomposite, SWT.CALENDAR | SWT.BORDER);
        Label datelabel = new Label(currentdatecomposite, SWT.NULL);
        datelabel.setText("Date");
        final DateTime date = new DateTime(currentdatecomposite, SWT.DATE | SWT.SHORT);
        Label timelabel = new Label(currentdatecomposite, SWT.NULL);
        timelabel.setText("Time");
        final DateTime time = new DateTime(currentdatecomposite, SWT.TIME | SWT.SHORT);
        Composite cronjobcomposite = new Composite(container, SWT.NONE);
        gridLayout = new GridLayout(1, false);
        gridLayout.numColumns = 2;
        gridLayout.marginWidth = gridLayout.marginHeight = 0;
        cronjobcomposite.setLayout(gridLayout);
        GridData combotextgridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
        combotextgridData.widthHint = 400;
        combotextgridData.heightHint = 18;
        combotextgridData.horizontalAlignment = GridData.FILL;
        combotextgridData.verticalAlignment = GridData.FILL;
        combotextgridData.horizontalSpan = 1;
        Label emptylabel = new Label(cronjobcomposite, SWT.LEFT);
        validatebutton = new Button(cronjobcomposite, SWT.PUSH);
        validatebutton.setText("Cron Job String Valid");
        validatebutton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
            }
        });
        Label templatelabel = new Label(cronjobcomposite, SWT.LEFT);
        templatelabel.setText(MOZILLA_TEMPLATE);
        templatecombo = new Combo(cronjobcomposite, SWT.READ_ONLY);
        templatecombo.setLayoutData(combotextgridData);
        templatecombo.add("master_deutsche_boerse_etf");
        templatecombo.setText(templatecombo.getItem(0).toString());
        Label cronstringlabel = new Label(cronjobcomposite, SWT.LEFT);
        cronstringlabel.setText(CRONSTRING_TEXT);
        jobtext = new Text(cronjobcomposite, SWT.FILL | SWT.BORDER);
        jobtext.setLayoutData(combotextgridData);
        Label jobtextlabel = new Label(cronjobcomposite, SWT.LEFT);
        jobtextlabel.setText(PATTERN_TEXT);
        jobtextlabel.setToolTipText(JOBTEXTLABEL_TOOLTIP);
        quartzcomposite = new Composite(cronjobcomposite, SWT.NONE);
        GridLayout crongridlayout = new GridLayout();
        crongridlayout.numColumns = 14;
        quartzcomposite.setLayout(crongridlayout);
        GridData crongriddata = new GridData();
        crongriddata.horizontalAlignment = SWT.FILL;
        Label seclabel = new Label(quartzcomposite, SWT.LEFT);
        seclabel.setText("Sec ");
        Combo quartzcombo_sec = new Combo(quartzcomposite, SWT.NORMAL);
        quartzcombo_sec.setText("    ");
        quartzcombo_sec.add("-");
        quartzcombo_sec.add("*");
        quartzcombo_sec.add(",");
        quartzcombo_sec.add("/");
        quartzcombo_sec.add("0-59");
        Label minlabel = new Label(quartzcomposite, SWT.LEFT);
        minlabel.setText("Min ");
        Combo quartzcombo_min = new Combo(quartzcomposite, SWT.NORMAL);
        quartzcombo_min.setText("    ");
        quartzcombo_min.add("-");
        quartzcombo_min.add("*");
        quartzcombo_min.add(",");
        quartzcombo_min.add("/");
        quartzcombo_min.add("0-59");
        Label hrlabel = new Label(quartzcomposite, SWT.LEFT);
        hrlabel.setText("Hr  ");
        Combo quartzcombo_hour = new Combo(quartzcomposite, SWT.NORMAL);
        quartzcombo_hour.setText("    ");
        quartzcombo_hour.add("-");
        quartzcombo_hour.add("*");
        quartzcombo_hour.add(",");
        quartzcombo_hour.add("/");
        quartzcombo_hour.add("0-59");
        Label monthdaylabel = new Label(quartzcomposite, SWT.LEFT);
        monthdaylabel.setText("Day/Month");
        Combo quartzcombo_day_of_month = new Combo(quartzcomposite, SWT.NORMAL);
        quartzcombo_day_of_month.setText("    ");
        quartzcombo_day_of_month.add("1-31");
        quartzcombo_day_of_month.add(",");
        quartzcombo_day_of_month.add("-");
        quartzcombo_day_of_month.add("*");
        quartzcombo_day_of_month.add("?");
        quartzcombo_day_of_month.add("/");
        quartzcombo_day_of_month.add("L");
        quartzcombo_day_of_month.add("W");
        quartzcombo_day_of_month.add("C");
        Label monthlabel = new Label(quartzcomposite, SWT.LEFT);
        monthlabel.setText("Month  ");
        Combo quartzcombo_month = new Combo(quartzcomposite, SWT.NORMAL);
        quartzcombo_month.setText("    ");
        quartzcombo_month.add("1-12");
        quartzcombo_month.add("JAN-DEC");
        quartzcombo_month.add("-");
        quartzcombo_month.add("*");
        quartzcombo_month.add("/");
        quartzcombo_month.add(",");
        Label weekdaylabel = new Label(quartzcomposite, SWT.LEFT);
        weekdaylabel.setText("Weekday");
        Combo quartzcombo_day_of_week = new Combo(quartzcomposite, SWT.NORMAL);
        quartzcombo_day_of_week.setText("    ");
        quartzcombo_day_of_week.add("1-7");
        quartzcombo_day_of_week.add("SUN-SAT");
        quartzcombo_day_of_week.add(",");
        quartzcombo_day_of_week.add("-");
        quartzcombo_day_of_week.add("*");
        quartzcombo_day_of_week.add("?");
        quartzcombo_day_of_week.add("/");
        quartzcombo_day_of_week.add("L");
        quartzcombo_day_of_week.add("C");
        quartzcombo_day_of_week.add("#");
        Label yearlabel = new Label(quartzcomposite, SWT.LEFT);
        yearlabel.setText("Year ");
        Combo quartzcombo_year = new Combo(quartzcomposite, SWT.NORMAL);
        quartzcombo_year.setText("    ");
        quartzcombo_year.add("1970-2099");
        quartzcombo_year.add(",");
        quartzcombo_year.add("-");
        quartzcombo_year.add("*");
        quartzcombo_year.add("/");
        ResultSet myresultset = null;
        try {
            myresultset = CorePlugin.getDefault().getHibernateUtil().executeSQLQuery("show tables");
            while (myresultset.next()) {
                String tablename = myresultset.getString(1);
                templatecombo.add(tablename);
            }
        } catch (SQLException e1) {
            e1.printStackTrace();
        } finally {
            try {
                myresultset.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        Label jobschedulelabel = new Label(cronjobcomposite, SWT.LEFT);
        jobschedulelabel.setText(SAMPLE_CRON_STRINGS);
        schedulecombo = new Combo(cronjobcomposite, SWT.READ_ONLY);
        schedulecombo.setLayoutData(combotextgridData);
        Label jobscheduledescription = new Label(cronjobcomposite, SWT.LEFT);
        jobscheduledescription.setText(SAMPLE_CRON_STRING_DESCRIPTION);
        scheduledescription = new Combo(cronjobcomposite, SWT.READ_ONLY);
        scheduledescription.setLayoutData(combotextgridData);
        loadJobProperties();
        schedulecombo.setText(schedulecombo.getItem(0));
        scheduledescription.setText(scheduledescription.getItem(0));
        templatecombo.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                int selection = templatecombo.getSelectionIndex();
                String templatename = templatecombo.getItem(selection);
                ((JobWizard) getWizard()).getJobOverview().setTemplatename(templatename);
            }
        });
        schedulecombo.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                int index = schedulecombo.getSelectionIndex();
                scheduledescription.select(index);
                setComboSelection(index);
            }
        });
        scheduledescription.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                int index = scheduledescription.getSelectionIndex();
                schedulecombo.select(index);
                setComboSelection(index);
            }
        });
        CronJobDigitValidator myvalidator = new CronJobDigitValidator();
        final Model model = new Model();
        dbc.bindValue(SWTObservables.observeText(jobtext, SWT.Modify), model.StringValue, new UpdateValueStrategy().setAfterConvertValidator(new CronJobDigitValidator()), null);
    }

    private void loadJobProperties() {
        URL quartzwizardurl = CorePlugin.getDefault().getBundle().getResource("/org/mss/quartzjobs/wizards/quartzwizard.properties");
        log.debug(" Load Quarz Wizard Properties for QuartzWizardPage2: " + quartzwizardurl.toExternalForm());
        Properties props = new Properties();
        try {
            props.load(quartzwizardurl.openStream());
            for (Iterator it = props.keySet().iterator(); it.hasNext(); ) {
                String cronvalue = (String) it.next();
                String crondesc = props.getProperty(cronvalue);
                schedulecombo.add(cronvalue);
                scheduledescription.add(crondesc);
            }
        } catch (IOException e) {
            log.error("Could not load Quartz Wizard Properties " + quartzwizardurl.toExternalForm());
            e.printStackTrace();
        }
    }

    /**
	 * MK TODO regex funktioniert nicht mit jahr -> regex testen
	 * @param cronexpression
	 */
    private void initScheduleCombo(String cronexpression) {
        String[] cronexpressions = schedulecombo.getItems();
        List<String> cronlist = Arrays.asList(cronexpressions);
        Collections.sort(cronlist);
        int index = Collections.binarySearch(cronlist, cronexpression);
        if (index > 0) {
            schedulecombo.select(index);
            scheduledescription.select(index);
            setComboSelection(index);
        }
    }

    /**
	 * MK 04.01.2008
	 * @param parentcomposite
	 * @param incombo
	 * @param index
	 */
    private void setComboSelection(int index) {
        String crontext = schedulecombo.getItem(index);
        String[] splitcron = crontext.split(" ");
        if (splitcron.length <= 6) {
            crontext = crontext + " *";
            splitcron = crontext.split(" ");
        }
        Control controls[] = quartzcomposite.getChildren();
        int comboindex = 0;
        for (Control control : controls) {
            if (comboindex % 2 != 0) {
                Combo combo = (Combo) control;
                combo.setText(splitcron[(comboindex - 1) / 2]);
            }
            comboindex++;
        }
        if (crontext.split(" ").length >= 7) crontext = crontext.substring(0, crontext.lastIndexOf(" "));
        jobtext.setText(crontext);
    }
}
