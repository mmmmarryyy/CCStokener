package steveshrader.budget.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import steveshrader.budget.domain.BudgetUser;
import steveshrader.budget.domain.Expense;
import steveshrader.budget.domain.ExpenseType;
import steveshrader.budget.domain.PaymentType;
import steveshrader.budget.domain.Vendor;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * The server side service class.
 */
public class BudgetService implements Filter {

    /**
	 * all services require a User to be authenticated...this will return the id
	 * for the authenticated User or null if the user has not logged in.
	 */
    public static BudgetUser getBudgetUser() {
        User user = UserServiceFactory.getUserService().getCurrentUser();
        if (user != null) {
            return new BudgetUser(user.getNickname(), user.getUserId());
        } else return null;
    }

    /**
	 * return a Url that can be used to sign out of the system
	 */
    public static String getLogoutUrl() {
        return UserServiceFactory.getUserService().createLogoutURL("/");
    }

    /**
	 * return a Quote to display on the title page
	 */
    public static String getQuote() {
        String quote = "";
        try {
            URL url = new URL("http://quote-server.appspot.com/rest/tag/money");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            quote = reader.readLine();
            reader.close();
        } catch (Exception e) {
        }
        return quote;
    }

    /**
	 * Store an Expense in the DB
	 */
    public static String addExpense(Expense expense) {
        if (getBudgetUser() == null) {
            return "User is not Logged In";
        } else {
            PersistenceManager pm = PMF.get().getPersistenceManager();
            try {
                pm.makePersistent(expense);
                Vendor vendor = new Vendor(expense.getVendor(), expense.getExpenseType(), expense.getPaymentType());
                pm.makePersistent(vendor);
                ExpenseType expenseType = new ExpenseType(expense.getExpenseType());
                pm.makePersistent(expenseType);
                PaymentType paymentType = new PaymentType(expense.getPaymentType());
                pm.makePersistent(paymentType);
            } finally {
                pm.close();
            }
            return "Expense Added";
        }
    }

    /**
	 * Remove an Expense from the DB
	 */
    public static String deleteExpense(Expense expense) {
        if (getBudgetUser() == null) {
            return "User is not Logged In";
        } else {
            PersistenceManager pm = PMF.get().getPersistenceManager();
            try {
                Expense e = pm.getObjectById(Expense.class, expense.getId());
                pm.deletePersistent(e);
            } finally {
                pm.close();
            }
            return "Expense Deleted";
        }
    }

    /**
	 * Get a list of Vendors previously used
	 */
    @SuppressWarnings("unchecked")
    public static List<Vendor> getVendors() {
        List<Vendor> vendors = new ArrayList<Vendor>();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Vendor.class);
        vendors = (List<Vendor>) query.execute();
        vendors.size();
        pm.close();
        return vendors;
    }

    /**
	 * Get a list of ExpenseTypes previously used
	 */
    @SuppressWarnings("unchecked")
    public static List<ExpenseType> getExpenseTypes() {
        List<ExpenseType> expenseTypes = new ArrayList<ExpenseType>();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(ExpenseType.class);
        expenseTypes = (List<ExpenseType>) query.execute();
        expenseTypes.size();
        pm.close();
        return expenseTypes;
    }

    /**
	 * Get a list of PaymentTypes previously used
	 */
    @SuppressWarnings("unchecked")
    public static List<PaymentType> getPaymentTypes() {
        List<PaymentType> paymentTypes = new ArrayList<PaymentType>();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(PaymentType.class);
        paymentTypes = (List<PaymentType>) query.execute();
        paymentTypes.size();
        pm.close();
        return paymentTypes;
    }

    /**
	 * Retrieve a list of Expense depending on the Dates and Vendor passed in.
	 * There will be two execution paths because Vendor does not need to be
	 * passed in as a filter.
	 */
    @SuppressWarnings({ "unchecked", "deprecation" })
    public static List<Expense> getExpenses(Date filterStartDate, Date filterEndDate, String filterVendor) {
        filterStartDate.setHours(0);
        filterStartDate.setMinutes(0);
        filterStartDate.setSeconds(0);
        filterEndDate.setHours(23);
        filterEndDate.setMinutes(59);
        filterEndDate.setSeconds(59);
        List<Expense> expenses = new ArrayList<Expense>();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Expense.class);
        query.declareImports("import java.util.Date");
        query.setOrdering("date asc");
        String queryFilter = "date >= :startDateParam && date <= :endDateParam";
        @SuppressWarnings("rawtypes") Map queryParameters = new HashMap();
        queryParameters.put("startDateParam", filterStartDate);
        queryParameters.put("endDateParam", filterEndDate);
        if (filterVendor != null && filterVendor.trim().length() > 0) {
            queryFilter = queryFilter + " && vendor == vendorParam";
            queryParameters.put("vendorParam", filterVendor);
        }
        query.setFilter(queryFilter);
        expenses = (List<Expense>) query.executeWithMap(queryParameters);
        expenses.size();
        pm.close();
        List<Expense> filteredExpenses = new ArrayList<Expense>();
        for (Expense expense : expenses) {
            if (filterVendor == null || filterVendor.length() == 0 || filterVendor.equals(expense.getVendor())) {
                filteredExpenses.add(expense);
            }
        }
        return filteredExpenses;
    }

    public static Expense findExpense(Long id) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Expense expense = null;
        try {
            expense = pm.getObjectById(Expense.class, id);
        } catch (JDOObjectNotFoundException e) {
        }
        pm.close();
        return expense;
    }

    public static Vendor findVendor(Long id) {
        return null;
    }

    public static ExpenseType findExpenseType(Long id) {
        return null;
    }

    public static PaymentType findPaymentType(Long id) {
        return null;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(req, resp);
    }

    public void init(FilterConfig config) {
    }

    public void destroy() {
    }
}
