package org.jbudget.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;
import javax.swing.BorderFactory;
import javax.swing.border.EtchedBorder;
import org.jbudget.Core.Account;
import org.jbudget.Core.Budget;
import org.jbudget.Core.Month;
import org.jbudget.Core.Transaction;
import org.jbudget.Core.User;
import org.jbudget.analysis.gui.AnalysisFrame;
import org.jbudget.gui.budget.AccountEditorDialog;
import org.jbudget.gui.budget.AccountSelectorDialog;
import org.jbudget.gui.budget.BudgetEditor;
import org.jbudget.gui.budget.BudgetSelectorDialog;
import org.jbudget.gui.components.HtmlDisplayDialog;
import org.jbudget.gui.month.DatesInfoTable;
import org.jbudget.gui.month.DatesInfoTableModel;
import org.jbudget.gui.month.MonthPanel;
import org.jbudget.gui.month.MonthSelectorDialog;
import org.jbudget.gui.trans.TransactionEditorDialog;
import org.jbudget.gui.trans.TransactionVerificationDialog;
import org.jbudget.gui.user.LoginDialog;
import org.jbudget.gui.user.NewUserDialog;
import org.jbudget.gui.user.UserPreferences;
import org.jbudget.gui.user.UserSelectorDialog;
import org.jbudget.io.DataManager;
import org.jbudget.io.DataManager.PrefKeys;
import org.jbudget.io.EventDispatcher.DataSourceListener;
import org.jbudget.util.DateFormater;
import org.jbudget.util.Pair;
import org.netbeans.api.wizard.WizardDisplayer;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;

/**
 * Main Window of the program.
 * @author petrov
 */
public class MainWindow extends MainWindowBase implements DataSourceListener {

    /** Status label */
    private final JLabel statusBarDataSourceLabel = new JLabel("");

    private final JLabel statusBarUserLabel = new JLabel("");

    /** Ananlysi frame. Only one instance of the frame can be created. */
    private AnalysisFrame analysisFrame = null;

    /** We assume that only one instance of MainWindow exists and it is
     * stored in this variable. This is mostly used to get the graphics
     * object that is used to paint the MainWindow and all its supbcomponents.
     */
    private static MainWindow instance;

    /** Creates a new instance of MainWindow */
    public MainWindow() {
        super();
        DataManager.getInstance().setMainWindow(this);
        initializeActions();
        SpringLayout statusBarLayout = new SpringLayout();
        JPanel statusBar = new JPanel();
        statusBar.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        statusBar.setLayout(statusBarLayout);
        statusBar.add(statusBarDataSourceLabel);
        statusBar.add(statusBarUserLabel);
        statusBarLayout.putConstraint(SpringLayout.WEST, statusBarDataSourceLabel, 3, SpringLayout.WEST, statusBar);
        statusBarLayout.putConstraint(SpringLayout.WEST, statusBarUserLabel, 22, SpringLayout.EAST, statusBarDataSourceLabel);
        statusBar.setPreferredSize(new Dimension(200, 20));
        add(statusBar, BorderLayout.SOUTH);
        statusBarDataSourceLabel.setText(DataManager.getInstance().getIdString());
        DataManager.getInstance().addDataSourceListener(this);
        setMinimumSize(new Dimension(400, 300));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setPreferredSize(new Dimension((int) (screenSize.getWidth() * 3 / 4), (int) (screenSize.getHeight() * 3 / 4)));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent ev) {
                exit();
            }
        });
        arrangeMenus();
        pack();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        instance = this;
        setVisible(true);
        if (DataManager.getInstance().isNewDataDir()) showGetStartedBox();
        changeUser();
    }

    /** Saves everything and exits */
    private void exit() {
        setVisible(false);
        User currentUser = DataManager.getInstance().getCurrentUser();
        if (currentUser != User.UNKNOWN) {
            UserPreferences preferences = currentUser.getUserPreferences();
            if (preferences == null) {
                preferences = new UserPreferences();
                currentUser.setUserPreferences(preferences);
            }
            preferences.setTableState(tableState);
            preferences.setRenderingState(renderingState);
            preferences.setColors(colors);
            preferences.setOpenMonths(displayedMonths);
            DataManager.getInstance().saveUserPreferences(currentUser);
        }
        for (int i = 0, s = dataPane.getTabCount(); i < s; i++) {
            MonthPanel mPanel = (MonthPanel) dataPane.getComponentAt(i);
            mPanel.dispose();
        }
        dispose();
        DataManager.getInstance().close();
        System.exit(0);
    }

    /** Initializes actions associated with menu items and buttons */
    private void initializeActions() {
        ActionMap keysActionMap = dataPane.getActionMap();
        InputMap keysInputMap = dataPane.getInputMap();
        keysActionMap.put("open_month", new AbstractAction("Open Month") {

            public void actionPerformed(ActionEvent ev) {
                openExistingMonth();
            }
        });
        openMonthBtn.setAction(keysActionMap.get("open_month"));
        keysInputMap.put(KeyStroke.getKeyStroke("control O"), "open_month");
        keysActionMap.put("new_month", new AbstractAction("New Month") {

            public void actionPerformed(ActionEvent ev) {
                createNewMonth();
            }
        });
        addMonthBtn.addActionListener(keysActionMap.get("new_month"));
        keysInputMap.put(KeyStroke.getKeyStroke("control N"), "new_month");
        keysActionMap.put("save_all", new AbstractAction("Save All") {

            public void actionPerformed(ActionEvent ev) {
                DataManager.getInstance().saveAllMonths();
            }
        });
        saveAllBtn.addActionListener(keysActionMap.get("save_all"));
        keysInputMap.put(KeyStroke.getKeyStroke("control S"), "save_all");
        closeAllBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                DataManager.getInstance().saveAllMonths();
                displayedMonths.clear();
                for (int i = 0, s = dataPane.getTabCount(); i < s; i++) {
                    MonthPanel mPanel = (MonthPanel) dataPane.getComponentAt(i);
                    mPanel.dispose();
                }
                dataPane.removeAll();
            }
        });
        keysActionMap.put("exit", new AbstractAction("Exit") {

            public void actionPerformed(ActionEvent ev) {
                exit();
            }
        });
        exitBtn.addActionListener(keysActionMap.get("exit"));
        keysInputMap.put(KeyStroke.getKeyStroke("control X"), "exit");
        collapseAccountsBtn.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent ev) {
                tableState.setExpandAccounts(ev.getStateChange() == ItemEvent.DESELECTED);
            }
        });
        collapseCategoriesBtn.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent ev) {
                tableState.setExpandCategories(ev.getStateChange() == ItemEvent.DESELECTED);
            }
        });
        hideEmptyAccountsBtn.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent ev) {
                tableState.setHideZeroBalanceAccounts(ev.getStateChange() == ItemEvent.SELECTED);
            }
        });
        hideEmptyCategoriesBtn.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent ev) {
                tableState.setHideZeroBalanceCategories(ev.getStateChange() == ItemEvent.SELECTED);
            }
        });
        hideIdleAccountsBtn.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent ev) {
                tableState.setHideInactiveAccounts(ev.getStateChange() == ItemEvent.SELECTED);
            }
        });
        hideIdleCategoriesBtn.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent ev) {
                tableState.setHideInactiveCategories(ev.getStateChange() == ItemEvent.SELECTED);
            }
        });
        hideSubcategoriesBtn.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent ev) {
                tableState.setCollapseSubcategories(ev.getStateChange() == ItemEvent.SELECTED);
            }
        });
        detailedRendererBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                renderingState.setRenderingType(DatesInfoTable.RenderingType.DETAILED);
            }
        });
        activityRendererBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                renderingState.setRenderingType(DatesInfoTable.RenderingType.ACTIVITY);
            }
        });
        balanceRendererBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                renderingState.setRenderingType(DatesInfoTable.RenderingType.BALANCE);
            }
        });
        actAndBalRendererBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                renderingState.setRenderingType(DatesInfoTable.RenderingType.ACTIVITY_BALANCE);
            }
        });
        plainBackgrBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                DatesInfoTable.BackgroundType bt = renderingState.getBackgroundType();
                renderingState.setBackgroundType(bt.getPlain());
            }
        });
        stripedBackgrBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                DatesInfoTable.BackgroundType bt = renderingState.getBackgroundType();
                renderingState.setBackgroundType(bt.getStriped());
            }
        });
        fullStructureBackgrBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                DatesInfoTable.BackgroundType bt = renderingState.getBackgroundType();
                renderingState.setBackgroundType(bt.getStructFull());
            }
        });
        simpleStructureBackgrBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                DatesInfoTable.BackgroundType bt = renderingState.getBackgroundType();
                renderingState.setBackgroundType(bt.getStructSimple());
            }
        });
        noStructureBackgrBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                DatesInfoTable.BackgroundType bt = renderingState.getBackgroundType();
                renderingState.setBackgroundType(bt.getStructOFF());
            }
        });
        highltOffBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                renderingState.setHighlightingType(DatesInfoTable.HighlightingType.NO_HIGHLIGHTING);
            }
        });
        highltUnderlBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                renderingState.setHighlightingType(DatesInfoTable.HighlightingType.UDERLINE);
            }
        });
        saveMonthBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                MonthPanel monthPanel = (MonthPanel) dataPane.getSelectedComponent();
                if (monthPanel == null) {
                    return;
                }
                DataManager.getInstance().saveMonth(monthPanel.getMonth());
            }
        });
        keysActionMap.put("close_month", new AbstractAction("Close") {

            public void actionPerformed(ActionEvent ev) {
                closeAndSaveSelectedMonth();
            }
        });
        closeMonthBtn.setAction(keysActionMap.get("close_month"));
        keysInputMap.put(KeyStroke.getKeyStroke("control W"), "close_month");
        showBudgetBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                MonthPanel monthPanel = (MonthPanel) dataPane.getSelectedComponent();
                if (monthPanel == null) {
                    return;
                }
                new BudgetEditor(MainWindow.this, monthPanel.getMonth().getBudget(), false);
            }
        });
        changeBudgetBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                MonthPanel monthPanel = (MonthPanel) dataPane.getSelectedComponent();
                if (monthPanel == null) {
                    return;
                }
                changeBudgetForMonth(monthPanel);
            }
        });
        addAllocationBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                showAddAllocationDialog();
            }
        });
        keysActionMap.put("add_transaction", new AbstractAction("Add Transaction") {

            public void actionPerformed(ActionEvent ev) {
                showAddTransactionDialog();
            }
        });
        addTransactionBtn.setAction(keysActionMap.get("add_transaction"));
        keysInputMap.put(KeyStroke.getKeyStroke("control T"), "add_transaction");
        viewBudgetBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                showBudget();
            }
        });
        newBudgetBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                createBudget();
            }
        });
        budgetWizardBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                runBudgetWisard();
            }
        });
        removeBudgetBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                removeUnusedBudget();
            }
        });
        removeUnusedBudgetsBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                removeUnusedBudgets();
            }
        });
        viewAccountBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                showAccount();
            }
        });
        editAccountBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                editAccount();
            }
        });
        newAccountBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                createAccount();
            }
        });
        removeAccountBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                removeUnusedAccount();
            }
        });
        removeUnusedAccountsBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                removeUnusedAccounts();
            }
        });
        analysisBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                if (analysisFrame != null) {
                    if ((analysisFrame.getExtendedState() & ICONIFIED) != 0) {
                        analysisFrame.setExtendedState(NORMAL);
                    }
                    analysisFrame.toFront();
                } else {
                    analysisFrame = new AnalysisFrame(MainWindow.this);
                    analysisFrame.addWindowListener(new WindowAdapter() {

                        @Override
                        public void windowClosing(WindowEvent e) {
                            analysisFrame = null;
                        }
                    });
                    analysisFrame.setVisible(true);
                }
            }
        });
        transactionVerifyBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                new TransactionVerificationDialog(MainWindow.this);
            }
        });
        preferencesBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                new PreferencesEditorDialog(MainWindow.this, true);
            }
        });
        keysActionMap.put("switch_user", new AbstractAction("Switch User") {

            public void actionPerformed(ActionEvent ev) {
                changeUser();
            }
        });
        switchUserBtn.setAction(keysActionMap.get("switch_user"));
        keysInputMap.put(KeyStroke.getKeyStroke("control U"), "switch_user");
        newUserBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                NewUserDialog dialog = new NewUserDialog(MainWindow.this, "New User", true);
                if (dialog.wasSelectionMade()) {
                    String userName = dialog.getUserName();
                    String passwd = dialog.getPasswd();
                    List<Pair<Integer, String>> userData = DataManager.getInstance().getValidUsers();
                    for (Pair<Integer, String> userInfo : userData) {
                        if (userInfo.second().equals(userName)) {
                            JOptionPane.showMessageDialog(MainWindow.this, "User with this name already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                    User newUser = new User(userName, passwd);
                    Account cashAccount = new Account("Cash");
                    cashAccount.addAccountHolder(newUser.getUID());
                    cashAccount.hide();
                    DataManager.getInstance().addAccount(cashAccount);
                    newUser.setCashAccountID(cashAccount.getID());
                    DataManager.getInstance().addUser(newUser);
                }
            }
        });
        removeUserBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                int uid = UserSelectorDialog.showDialog(MainWindow.this);
                User user = DataManager.getInstance().getUser(uid);
                if (user == null) {
                    return;
                }
                int cash_id = user.getCashAccountID();
                if (cash_id >= 0) {
                    DataManager.getInstance().removeAccount(cash_id);
                }
                if (uid > 1) {
                    DataManager.getInstance().removeUser(uid);
                }
            }
        });
        dataDirBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                File dir = DirectoryLocationDialog.showDialog(MainWindow.this);
                if (dir != null) {
                    List<Month> months = new ArrayList<Month>(displayedMonths.size());
                    months.addAll(displayedMonths);
                    for (Month month : months) {
                        closeAndSaveMonth(month);
                    }
                    String dataDir = dir.getAbsolutePath();
                    Preferences ioPreferences = Preferences.userNodeForPackage(DataManager.class);
                    ioPreferences.put(PrefKeys.MAIN_DIR, dataDir);
                    DataManager.getNewInstance();
                    changeUser();
                }
            }
        });
        getStartedBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                showGetStartedBox();
            }
        });
        aboutBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                showAboutBox();
            }
        });
    }

    /** Opens an existing month object and adds it to the data panel as a new
     * tab. First opens a month chooser dialog and then opens the month selected
     * by the dialog.
     */
    private void openExistingMonth() {
        Pair<Integer, Integer> monthIndex = MonthSelectorDialog.showDialog(this, true);
        if (monthIndex == null) return;
        int cMonth = monthIndex.first().intValue();
        int cYear = monthIndex.second().intValue();
        openMonth(cMonth, cYear);
    }

    /** Opens an existing month object and adds it to the data panel as a new
     * tab.
     */
    private void openMonth(int cMonth, int cYear) {
        Month month = DataManager.getInstance().getMonth(cMonth, cYear);
        if (month == null) return;
        int index = Collections.binarySearch(displayedMonths, month);
        if (index >= 0) {
            dataPane.setSelectedIndex(index);
        } else {
            index = -index - 1;
            displayedMonths.add(index, month);
            MonthPanel monthPanel = new MonthPanel(month, tableState, renderingState);
            monthPanel.setColumnColors(this.colors);
            String monthName = (new DateFormater()).formatMonth(month.getMonth()) + ", " + month.getYear();
            dataPane.insertTab(monthName, null, monthPanel, null, index);
            dataPane.setSelectedIndex(index);
            dataPane.revalidate();
        }
        synchronized (calendar) {
            calendar.setTimeInMillis(System.currentTimeMillis());
            int currentYear = calendar.get(Calendar.YEAR);
            int currentMonth = calendar.get(Calendar.MONTH);
            if ((cYear < currentYear) || (cYear == currentYear && cMonth < currentMonth)) {
                month.generateTransactions(31);
            } else if (cYear == currentYear && cMonth == currentMonth) {
                int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
                month.generateTransactions(currentDay);
            }
        }
    }

    /** creates a new month object and adds it to the data panel as a new
     * tab. First opens a month chooser dialog then opens the budget chooser
     * dialog and finally creates the new month using the budget selected by
     * the user.
     */
    private void createNewMonth() {
        Pair<Integer, Integer> monthIndex = MonthSelectorDialog.showDialog(this, false);
        if (monthIndex == null) return;
        int cMonth = monthIndex.first().intValue();
        int cYear = monthIndex.second().intValue();
        int budgetID = BudgetSelectorDialog.showDialog(this);
        if (budgetID == -1) return;
        Budget budget = DataManager.getInstance().getBudget(budgetID);
        assert budget != null;
        Month month = new Month(budget, cMonth, cYear);
        int response = JOptionPane.showConfirmDialog(this, "Should the new month carry over balances from the previous month?", "Carry balaces?", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) month.setCarryBalance(true);
        DataManager.getInstance().addMonth(month);
        synchronized (calendar) {
            calendar.setTimeInMillis(System.currentTimeMillis());
            int currentYear = calendar.get(Calendar.YEAR);
            int currentMonth = calendar.get(Calendar.MONTH);
            if ((cYear < currentYear) || (cYear == currentYear && cMonth < currentMonth)) {
                month.generateTransactions(31);
            } else if (cYear == currentYear && cMonth == currentMonth) {
                int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
                month.generateTransactions(currentDay);
            }
        }
        displayedMonths.add(month);
        MonthPanel monthPanel = new MonthPanel(month, tableState, renderingState);
        monthPanel.setColumnColors(colors);
        String monthName = (new DateFormater()).formatMonth(month.getMonth()) + ", " + month.getYear();
        dataPane.addTab(monthName, monthPanel);
        dataPane.setSelectedIndex(displayedMonths.size() - 1);
        dataPane.revalidate();
    }

    /** Returns the currently selected month (null if no moths are opened). */
    public Month getSelectedMonth() {
        if (displayedMonths.isEmpty()) return null;
        return displayedMonths.get(dataPane.getSelectedIndex());
    }

    /** changes the budget used with a given month. First opens a budget chooser
     *  dialog to choose a new budget.
     */
    private void changeBudgetForMonth(MonthPanel monthPanel) {
        if (monthPanel == null) return;
        Month month = monthPanel.getMonth();
        int budgetID = BudgetSelectorDialog.showDialog(this);
        if (budgetID == -1) return;
        Budget budget = DataManager.getInstance().getBudget(budgetID);
        assert budget != null;
        List<Transaction> incompatTrans = new ArrayList<Transaction>();
        if (!month.isCompatible(budget, incompatTrans)) {
            System.out.println(incompatTrans);
            int answer = JOptionPane.showConfirmDialog(this, "Changing the budget for this moth to the budget that \n" + "you have selected will cause some transactions to be lost.\n" + "Do you want to proceed?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer == JOptionPane.NO_OPTION) return;
        }
        Month newMonth = month.changeBudget(budget);
        DataManager.getInstance().removeMonth(month.getMonth(), month.getYear());
        DataManager.getInstance().addMonth(newMonth);
        int index = dataPane.indexOfComponent(monthPanel);
        dataPane.remove(index);
        displayedMonths.remove(index);
        String monthName = (new DateFormater()).formatMonth(newMonth.getMonth()) + ", " + newMonth.getYear();
        MonthPanel newPanel = new MonthPanel(newMonth, this.tableState, this.renderingState);
        newPanel.setColumnColors(this.colors);
        dataPane.add(newPanel, index);
        newPanel.validate();
        dataPane.setTitleAt(index, monthName);
        dataPane.setSelectedIndex(index);
        displayedMonths.add(index, newMonth);
    }

    private void closeAndSaveSelectedMonth() {
        MonthPanel monthPanel = (MonthPanel) dataPane.getSelectedComponent();
        if (monthPanel == null) {
            return;
        }
        closeAndSaveMonth(monthPanel.getMonth());
    }

    /** Closes the MonthPanel associated with a given month */
    private void closeAndSaveMonth(Month month) {
        int index = displayedMonths.indexOf(month);
        if (index == -1) {
            return;
        }
        MonthPanel monthPanel = (MonthPanel) dataPane.getComponentAt(index);
        DataManager.getInstance().saveMonth(month);
        monthPanel.dispose();
        dataPane.remove(monthPanel);
        displayedMonths.remove(index);
    }

    /** creates a new budget */
    private void createBudget() {
        String budgetName = JOptionPane.showInputDialog(MainWindow.this, "Enter name for the new budget.", "Budget Name", JOptionPane.QUESTION_MESSAGE);
        if (budgetName == null || budgetName.equals("") || budgetName.equals(" ")) {
            return;
        }
        int response = JOptionPane.NO_OPTION;
        if (DataManager.getInstance().getAllBudgetIDs().size() > 0) {
            response = JOptionPane.showConfirmDialog(MainWindow.this, "Do you want to initialize the new budget from an existing budget?", "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        }
        final Budget newBudget;
        if (response == JOptionPane.YES_OPTION) {
            int budgetID = BudgetSelectorDialog.showDialog(this);
            if (budgetID == -1) {
                return;
            }
            Budget budget = DataManager.getInstance().getBudget(budgetID);
            assert budget != null;
            newBudget = new Budget(budget, budgetName);
        } else {
            newBudget = new Budget(budgetName);
        }
        BudgetEditor editor = new BudgetEditor(MainWindow.this, newBudget, true);
        editor.addOkListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                DataManager.getInstance().addBudget(newBudget);
            }
        });
    }

    /** creates a new budget */
    private void runBudgetWisard() {
        Class[] pages = new Class[] { org.jbudget.gui.budget.wizard.IntroductionPage.class, org.jbudget.gui.budget.wizard.BudgetNamePage.class, org.jbudget.gui.budget.wizard.IncomesPage.class, org.jbudget.gui.budget.wizard.ExpenseCategoriesPage.class, org.jbudget.gui.budget.wizard.SummaryPage.class };
        Wizard wizard = WizardPage.createWizard(pages, new org.jbudget.gui.budget.wizard.ResultProducer());
        Rectangle parent_bounds = this.getBounds();
        final int wizard_width = 700;
        final int wizard_height = 400;
        int wizard_x = parent_bounds.x + parent_bounds.width / 2 - wizard_width / 2;
        int wizard_y = parent_bounds.y + parent_bounds.height / 2 - wizard_height / 2;
        if (wizard_x < 0) {
            wizard_x = 0;
        }
        if (wizard_y < 0) {
            wizard_y = 0;
        }
        Rectangle wizard_bounds = new Rectangle(wizard_x, wizard_y, wizard_width, wizard_height);
        WizardDisplayer.showWizard(wizard, wizard_bounds);
    }

    /** shows a budget selection dialog and displays a selected budget */
    private void showBudget() {
        int budgetID = BudgetSelectorDialog.showDialog(this);
        if (budgetID == -1) return;
        Budget budget = DataManager.getInstance().getBudget(budgetID);
        assert budget != null;
        new BudgetEditor(MainWindow.this, budget, false);
    }

    /** shows a budget selection dialog and removes the selected budget. */
    private void removeUnusedBudget() {
        if (DataManager.getInstance().getUnusedBudgets().isEmpty()) {
            JOptionPane.showMessageDialog(this, "There are no unused budgets.", "Message", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int budgetID = BudgetSelectorDialog.showDialog(this, true);
        if (budgetID == -1) return;
        assert DataManager.getInstance().isUnusedBudget(budgetID);
        DataManager.getInstance().removeBudget(budgetID);
    }

    /** removes all unused budgets */
    private void removeUnusedBudgets() {
        List<Pair<Integer, String>> unusedBudgets = DataManager.getInstance().getUnusedBudgets();
        if (unusedBudgets.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There are no unused budgets.", "Message", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int response;
        if (unusedBudgets.size() > 1) {
            response = JOptionPane.showConfirmDialog(this, "There are " + unusedBudgets.size() + " unused budgets. Do you want to remove" + " them?", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        } else {
            response = JOptionPane.showConfirmDialog(this, "There is one " + "unused budget. Do you want to remove it?", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        }
        if (response == JOptionPane.NO_OPTION) return;
        for (Pair<Integer, String> budgetInfo : unusedBudgets) DataManager.getInstance().removeBudget(budgetInfo.first().intValue());
    }

    /** creates a new budget */
    private void createAccount() {
        Account newAccount = AccountEditorDialog.showDilaog(this);
        if (newAccount != null) DataManager.getInstance().addAccount(newAccount);
    }

    /** shows a budget selection dialog and displays a selected budget */
    private void showAccount() {
        int accountID = AccountSelectorDialog.showDialog(this);
        if (accountID == -1) return;
        Account account = DataManager.getInstance().getAccount(accountID);
        assert account != null;
        new AccountEditorDialog(this, account, false);
    }

    /** shows a budget selection dialog and displays a selected budget */
    private void editAccount() {
        int accountID = AccountSelectorDialog.showDialog(this);
        if (accountID == -1) return;
        Account account = DataManager.getInstance().getAccount(accountID);
        assert account != null;
        new AccountEditorDialog(this, account, true);
    }

    /** shows a budget selection dialog and removes the selected budget. */
    private void removeUnusedAccount() {
        if (DataManager.getInstance().getUnusedAccounts().isEmpty()) {
            JOptionPane.showMessageDialog(this, "There are no unused accounts.", "Message", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int accountID = AccountSelectorDialog.showDialog(this, true);
        if (accountID == -1) return;
        assert DataManager.getInstance().isUnusedAccount(accountID);
        DataManager.getInstance().removeAccount(accountID);
    }

    /** removes all unused budgets */
    private void removeUnusedAccounts() {
        List<Pair<Integer, String>> unusedAccounts = DataManager.getInstance().getUnusedAccounts();
        if (unusedAccounts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There are no unused account records.", "Message", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int response;
        if (unusedAccounts.size() > 1) {
            response = JOptionPane.showConfirmDialog(this, "There are " + unusedAccounts.size() + " unused account records. Do you want to " + "remove them?", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        } else {
            response = JOptionPane.showConfirmDialog(this, "There is one " + "unused account record. Do you want to remove it?", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        }
        if (response == JOptionPane.NO_OPTION) return;
        for (Pair<Integer, String> accountInfo : unusedAccounts) {
            DataManager.getInstance().removeAccount(accountInfo.first().intValue());
        }
    }

    /** Prompts for a new user selection */
    private void changeUser() {
        User currentUser = DataManager.getInstance().getCurrentUser();
        if (currentUser != User.UNKNOWN) {
            UserPreferences preferences = currentUser.getUserPreferences();
            if (preferences == null) {
                preferences = new UserPreferences();
                currentUser.setUserPreferences(preferences);
            }
            preferences.setTableState(tableState);
            preferences.setRenderingState(renderingState);
            preferences.setColors(colors);
            preferences.setOpenMonths(displayedMonths);
            DataManager.getInstance().saveUserPreferences(currentUser);
        }
        List<Month> months = new ArrayList<Month>(displayedMonths.size());
        months.addAll(displayedMonths);
        for (Month month : months) closeAndSaveMonth(month);
        boolean validUser = false;
        DataManager.getInstance().setCurrentUser(User.UNKNOWN, null);
        while (!validUser) {
            LoginDialog loginDialog = new LoginDialog(MainWindow.this, "Welcome to jBudget!");
            if (loginDialog.getResult() == LoginDialog.Result.OK) {
                String userName = loginDialog.getUserName();
                String passwd = loginDialog.getPasswd();
                if (!DataManager.getInstance().setCurrentUser(userName, passwd)) {
                    JOptionPane.showMessageDialog(MainWindow.this, "Wrong user name or password", "Error", JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                validUser = true;
                GuiPreferences.setLastUser(userName);
                boolean flag = DataManager.getInstance().getCurrentUser() == User.SYSTEM;
                newUserBtn.setEnabled(flag);
                removeUserBtn.setEnabled(flag);
                editAccountBtn.setEnabled(flag);
                newAccountBtn.setEnabled(flag);
                removeAccountBtn.setEnabled(flag);
                removeUnusedAccountsBtn.setEnabled(flag);
                dataDirBtn.setEnabled(flag);
                UserPreferences prefs = DataManager.getInstance().getCurrentUser().getUserPreferences();
                DatesInfoTableModel.State newTableState = null;
                DatesInfoTable.RenderingState newRenderingState = null;
                DatesInfoTable.Colors newColors = null;
                if (prefs != null) {
                    newTableState = prefs.getTableState();
                    newRenderingState = prefs.getRenderingState();
                    newColors = prefs.getColors();
                }
                if (newTableState == null) newTableState = new DatesInfoTableModel.State();
                if (newRenderingState == null) newRenderingState = new DatesInfoTable.RenderingState();
                if (newColors == null) newColors = new DatesInfoTable.Colors();
                changeTableState(newTableState);
                changeRenderingState(newRenderingState);
                changeColors(newColors);
                synchronizeMenu();
                if (prefs != null) {
                    List<Pair<Integer, Integer>> monthsList = prefs.getOpenedMonths();
                    for (Pair<Integer, Integer> monthInfo : monthsList) openMonth(monthInfo.first().intValue(), monthInfo.second().intValue());
                }
            } else {
                Object options[] = { "Ok", "Exit" };
                if (JOptionPane.showOptionDialog(MainWindow.this, "You should login to work with jBudget. Try again?", loginDialog.getTitle(), JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]) == 1) {
                    exit();
                }
            }
        }
        userChanged();
    }

    public void userChanged() {
        statusBarUserLabel.setText("User:" + DataManager.getInstance().getCurrentUser().getName());
    }

    /** Creates a dialog for a new transaction */
    private void showAddTransactionDialog() {
        calendar.setTimeInMillis(System.currentTimeMillis());
        if (displayedMonths.size() > 0) {
            int index = dataPane.getSelectedIndex();
            Month month = displayedMonths.get(index);
            if (month.getMonth() != calendar.get(Calendar.MONTH) || month.getYear() != calendar.get(Calendar.YEAR)) {
                calendar.setTime(month.startingDate);
            }
            List<Integer> selectedDays = ((MonthPanel) dataPane.getSelectedComponent()).getSelectedDays();
            if (selectedDays.size() > 0) {
                int day = selectedDays.get(0).intValue();
                calendar.set(Calendar.DAY_OF_MONTH, day);
            }
        }
        TransactionEditorDialog dialog = new TransactionEditorDialog(MainWindow.this, calendar.getTime(), false);
        dialog.setTitle("New Transaction");
    }

    /** Creates a dialog for a new allocation */
    private void showAddAllocationDialog() {
        calendar.setTimeInMillis(System.currentTimeMillis());
        if (displayedMonths.size() > 0) {
            int index = dataPane.getSelectedIndex();
            Month month = displayedMonths.get(index);
            if (month.getMonth() != calendar.get(Calendar.MONTH) || month.getYear() != calendar.get(Calendar.YEAR)) {
                calendar.setTime(month.startingDate);
            }
            List<Integer> selectedDays = ((MonthPanel) dataPane.getSelectedComponent()).getSelectedDays();
            if (selectedDays.size() > 0) {
                int day = selectedDays.get(0).intValue();
                calendar.set(Calendar.DAY_OF_MONTH, day);
            }
        }
        TransactionEditorDialog dialog = new TransactionEditorDialog(MainWindow.this, calendar.getTime(), true);
        dialog.setTitle("New Allocation");
    }

    private void changeTableState(DatesInfoTableModel.State tableState) {
        this.tableState = tableState;
        int nTabs = dataPane.getTabCount();
        for (int i = 0; i < nTabs; i++) {
            ((MonthPanel) dataPane.getComponentAt(i)).setTableState(tableState);
        }
    }

    private void changeRenderingState(DatesInfoTable.RenderingState renderingState) {
        this.renderingState = renderingState;
        for (int i = 0, nTabs = dataPane.getTabCount(); i < nTabs; i++) {
            ((MonthPanel) dataPane.getComponentAt(i)).setRenderingState(renderingState);
        }
    }

    private void changeColors(DatesInfoTable.Colors colors) {
        this.colors = colors;
        for (int i = 0, nTabs = dataPane.getTabCount(); i < nTabs; i++) {
            ((MonthPanel) dataPane.getComponentAt(i)).setColumnColors(colors);
        }
    }

    /** Synchronizes the View menu with the state of the <b>renderingState</b> and
     *<b>tableState</b> objects.
     */
    private void synchronizeMenu() {
        DatesInfoTable.RenderingType renderingType = renderingState.getRenderingType();
        detailedRendererBtn.setSelected(renderingType == DatesInfoTable.RenderingType.DETAILED);
        activityRendererBtn.setSelected(renderingType == DatesInfoTable.RenderingType.ACTIVITY);
        balanceRendererBtn.setSelected(renderingType == DatesInfoTable.RenderingType.BALANCE);
        actAndBalRendererBtn.setSelected(renderingType == DatesInfoTable.RenderingType.ACTIVITY_BALANCE);
        DatesInfoTable.BackgroundType backgrType = renderingState.getBackgroundType();
        plainBackgrBtn.setSelected(backgrType.isPlain());
        stripedBackgrBtn.setSelected(backgrType.isStriped());
        fullStructureBackgrBtn.setSelected(backgrType.isStructureFull());
        simpleStructureBackgrBtn.setSelected(backgrType.isStructureSimple());
        noStructureBackgrBtn.setSelected(backgrType.isStructureOFF());
        DatesInfoTable.HighlightingType highltType = renderingState.getHighlightingType();
        highltOffBtn.setSelected(highltType == DatesInfoTable.HighlightingType.NO_HIGHLIGHTING);
        highltUnderlBtn.setSelected(highltType == DatesInfoTable.HighlightingType.UDERLINE);
        collapseAccountsBtn.setSelected(!tableState.getExpandAccounts());
        collapseCategoriesBtn.setSelected(!tableState.getExpandCategories());
        hideSubcategoriesBtn.setSelected(tableState.getCollapseSubcategories());
        hideEmptyAccountsBtn.setSelected(tableState.getHideZeroBalanceAccounts());
        hideEmptyCategoriesBtn.setSelected(tableState.getHideZeroBalanceCategories());
        hideIdleAccountsBtn.setSelected(tableState.getHideInactiveAccounts());
        hideIdleCategoriesBtn.setSelected(tableState.getHideInactiveCategories());
        hideSubcategoriesBtn.setSelected(tableState.getCollapseSubcategories());
        collapseAccountsBtn.setSelected(!tableState.getExpandAccounts());
        collapseCategoriesBtn.setSelected(!tableState.getExpandCategories());
    }

    public void showGetStartedBox() {
        String message = new String("Error: Resource Not Found.");
        java.net.URL url = ClassLoader.getSystemResource("docs/get_started.html");
        if (url != null) {
            try {
                StringBuffer buf = new StringBuffer();
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                while (reader.ready()) {
                    buf.append(reader.readLine());
                }
                message = buf.toString();
            } catch (IOException ex) {
                message = new String("IO Error.");
            }
        }
        new HtmlDisplayDialog(this, "Get Started", message);
    }

    private void showAboutBox() {
        String message = new String("Error: Resource Not Found.");
        java.net.URL url = ClassLoader.getSystemResource("docs/about.html");
        if (url != null) {
            try {
                StringBuffer buf = new StringBuffer();
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                while (reader.ready()) {
                    buf.append(reader.readLine());
                }
                message = buf.toString();
            } catch (IOException ex) {
                message = new String("IO Error.");
            }
        }
        JOptionPane.showOptionDialog(this, message, "About jBudget", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
    }

    /** Returns a reference to the last MainWindow object that was created. In
    general, there should be no reason to create more than one instance of
    the MainWindow class, so this method should return a reference to the
    only exiting instance.*/
    public static MainWindow getLastInstance() {
        return instance;
    }

    public void dataSourceChanged() {
        statusBarDataSourceLabel.setText("Data Source:" + DataManager.getInstance().getIdString() + "  ");
    }
}
