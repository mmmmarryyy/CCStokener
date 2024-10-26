package org.posper.hibernate.setup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import net.adrianromero.basic.BasicException;
import net.adrianromero.data.loader.ImageUtils;
import net.adrianromero.tpv.forms.AppConfig;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.posper.gui.AppView;
import org.posper.hibernate.Category;
import org.posper.hibernate.Customer;
import org.posper.hibernate.CustomerGroup;
import org.posper.hibernate.DiscountReason;
import org.posper.hibernate.Floor;
import org.posper.hibernate.HibDAOFactory;
import org.posper.hibernate.HibernateUtil;
import org.posper.hibernate.Image;
import org.posper.hibernate.Location;
import org.posper.hibernate.Permission;
import org.posper.hibernate.Place;
import org.posper.hibernate.Product;
import org.posper.hibernate.Property;
import org.posper.hibernate.Resource;
import org.posper.hibernate.Role;
import org.posper.hibernate.Tax;
import org.posper.hibernate.User;
import org.posper.resources.BasicProperties;

/**
 * 
 * Changes:
 * - added default posper.customer
 * @author Hans
 * @author Aaron Luchko<aaron.luchko@oxn.ca>
 * 
 */
public class DbSetup {

    private static Logger logger = Logger.getLogger("org.posper");

    private static final String POSPER_VERSION = "posper.version";

    private static final String POSPER_BUILD = "posper.build";

    /**
     * Given a current empty database insert all the default values.
     *
     * @throws BasicException
     *
     */
    public static void populateDatabase() throws BasicException {
        logger.debug("Populating database with default values");
        setDbVersionCurrent();
        if (HibernateUtil.getInstance().getDbType().equals("postgresql")) {
            try {
                HibernateUtil.getInstance().getSession().connection().createStatement().execute("create sequence posper_ticketnumber_sequence");
            } catch (HibernateException e) {
                logger.warn("Unhandled Exception" + e.getMessage());
            } catch (SQLException e) {
                logger.warn("Unhandled Exception" + e.getMessage());
            }
        }
        Property defaultCustomer = new Property("posper.customer");
        defaultCustomer.setValue("posper");
        defaultCustomer.save();
        populatePermissions();
        User user = new User();
        user.setName("Admin");
        user.setVisible(true);
        user.setRole(HibDAOFactory.getRoleDAO().get("admin"));
        user.save();
        user = new User();
        user.setName("Manager");
        user.setVisible(true);
        user.setRole(HibDAOFactory.getRoleDAO().get("manager"));
        user.save();
        user = new User();
        user.setName("Employee");
        user.setVisible(true);
        user.setRole(HibDAOFactory.getRoleDAO().get("employee"));
        user.save();
        user = new User();
        user.setName("Guest");
        user.setVisible(true);
        user.setRole(HibDAOFactory.getRoleDAO().get("guest"));
        user.save();
        Category root = new Category();
        root.setVisibleId(0);
        root.setName("root");
        root.save();
        Tax t = new Tax();
        t.setName("No Tax");
        t.setVisibleId(0);
        t.setRate(new Double(0));
        t.save();
        Location location = new Location();
        location.setVisibleId(0);
        location.setName("General");
        location.save();
        populateResources();
        HibernateUtil.getInstance().getSession().clear();
        logger.debug("Successfully populated database");
    }

    /**
     * @throws BasicException
     * 
     */
    static void populatePermissions() throws BasicException {
        Permission rootPerm = new Permission("Menu.Root");
        rootPerm.save();
        Vector<Permission> allPerms = new Vector<Permission>();
        Permission systemPerm = new Permission("Menu.System", rootPerm);
        allPerms.add(systemPerm);
        Permission mainPerm = new Permission("Menu.Main", rootPerm);
        allPerms.add(mainPerm);
        allPerms.add(new Permission("Menu.Ticket", mainPerm));
        allPerms.add(new Permission("Menu.Login", systemPerm));
        allPerms.add(new Permission("Menu.Exit", systemPerm));
        int guestCutoff = allPerms.size();
        allPerms.add(new Permission("Menu.Order", mainPerm));
        allPerms.add(new Permission("Menu.Payments", mainPerm));
        allPerms.add(new Permission("Menu.Customers", mainPerm));
        allPerms.add(new Permission("Menu.ChangePassword", systemPerm));
        allPerms.add(new Permission("Menu.Keyboard", systemPerm));
        Permission ticketPerm = new Permission("Menu.TicketPerms", rootPerm);
        allPerms.add(ticketPerm);
        allPerms.add(new Permission("Perm.F1", ticketPerm));
        allPerms.add(new Permission("Perm.F2", ticketPerm));
        allPerms.add(new Permission("Perm.F3", ticketPerm));
        allPerms.add(new Permission("Perm.F4", ticketPerm));
        allPerms.add(new Permission("Perm.F5", ticketPerm));
        allPerms.add(new Permission("Perm.EditTicketline", ticketPerm));
        allPerms.add(new Permission("Perm.FilterCustomer", ticketPerm));
        allPerms.add(new Permission("Perm.LineDelete", ticketPerm));
        allPerms.add(new Permission("Perm.Minus", ticketPerm));
        allPerms.add(new Permission("Perm.Plus", ticketPerm));
        allPerms.add(new Permission("Perm.TicketDelete", ticketPerm));
        int employeeCutoff = allPerms.size();
        allPerms.add(new Permission("Menu.CloseTPV", mainPerm));
        allPerms.add(new Permission("Menu.TicketEdit", mainPerm));
        Permission productPerm = new Permission("Menu.Products", rootPerm);
        allPerms.add(productPerm);
        allPerms.add(new Permission("Menu.ProductsEdit", productPerm));
        allPerms.add(new Permission("Menu.ProductsWarehouse", productPerm));
        allPerms.add(new Permission("Menu.Categories", productPerm));
        allPerms.add(new Permission("Menu.StockDiary", productPerm));
        allPerms.add(new Permission("Menu.StockMovement", productPerm));
        Permission maintainPerm = new Permission("Menu.Maintenance", rootPerm);
        allPerms.add(maintainPerm);
        allPerms.add(new Permission("Menu.CustomerGroups", maintainPerm));
        allPerms.add(new Permission("Menu.Users", maintainPerm));
        Permission reports = new Permission("Menu.Reports", rootPerm);
        allPerms.add(reports);
        allPerms.add(new Permission("Menu.ProductsReport", reports));
        allPerms.add(new Permission("Menu.Inventory2", reports));
        allPerms.add(new Permission("Menu.ClosedProducts", reports));
        allPerms.add(new Permission("Menu.CloseCashTaxes", reports));
        allPerms.add(new Permission("Menu.CloseCashPayments", reports));
        int managerCutoff = allPerms.size();
        allPerms.add(new Permission("Menu.Configuration", systemPerm));
        allPerms.add(new Permission("Menu.Printer", systemPerm));
        allPerms.add(new Permission("Menu.Resources", maintainPerm));
        allPerms.add(new Permission("Menu.Roles", maintainPerm));
        allPerms.add(new Permission("Menu.Taxes", maintainPerm));
        allPerms.add(new Permission("Menu.Locations", maintainPerm));
        allPerms.add(new Permission("Menu.Floors", maintainPerm));
        allPerms.add(new Permission("Menu.Tables", maintainPerm));
        Map<String, Permission> adminPerms = new HashMap<String, Permission>();
        Map<String, Permission> managerPerms = new HashMap<String, Permission>();
        Map<String, Permission> employeePerms = new HashMap<String, Permission>();
        Map<String, Permission> guestPerms = new HashMap<String, Permission>();
        for (int i = 0; i < allPerms.size(); i++) {
            allPerms.get(i).save();
            adminPerms.put(allPerms.get(i).getName(), allPerms.get(i));
            if (i < managerCutoff) {
                managerPerms.put(allPerms.get(i).getName(), allPerms.get(i));
            }
            if (i < employeeCutoff) {
                employeePerms.put(allPerms.get(i).getName(), allPerms.get(i));
            }
            if (i < guestCutoff) {
                guestPerms.put(allPerms.get(i).getName(), allPerms.get(i));
            }
        }
        Permission rootDiscountPerm = new Permission("Discount");
        rootDiscountPerm.save();
        Permission discountPerm = new Permission("Discount.Max", rootDiscountPerm);
        discountPerm.save();
        Permission disPerm = new Permission("Discount.Max.Percent.1", discountPerm);
        disPerm.save();
        Permission noDisPerm = new Permission("Discount.Max.Percent.0", discountPerm);
        noDisPerm.save();
        adminPerms.put(disPerm.getName(), disPerm);
        managerPerms.put(disPerm.getName(), disPerm);
        employeePerms.put(disPerm.getName(), disPerm);
        guestPerms.put(noDisPerm.getName(), noDisPerm);
        Role admin = new Role();
        admin.setName("admin");
        admin.setPermissions(adminPerms);
        admin.save();
        Role manager = new Role();
        manager.setName("manager");
        manager.setPermissions(managerPerms);
        manager.save();
        Role employee = new Role();
        employee.setName("employee");
        employee.setPermissions(employeePerms);
        employee.save();
        Role guest = new Role();
        guest.setName("guest");
        guest.setPermissions(guestPerms);
        guest.save();
    }

    /**
     * @throws BasicException
     *
     */
    static void populateResources() throws BasicException {
        try {
            List<URL> templates = DatabaseValidator.listResources("/net/adrianromero/templates/" + Locale.getDefault().getLanguage());
            if (templates.size() == 0) {
                templates = DatabaseValidator.listResources("/net/adrianromero/templates/en");
            }
            for (URL url : templates) {
                String fileName = url.getFile();
                fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                if (fileName.endsWith(".xml") || fileName.endsWith(".txt")) {
                    Resource templateResource = new Resource(fileName.substring(0, fileName.length() - 4));
                    InputStream is = url.openStream();
                    StringBuffer strBuff = new StringBuffer();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String str;
                    while ((str = br.readLine()) != null) {
                        strBuff.append(str + "\n");
                    }
                    templateResource.setText(strBuff.toString());
                    templateResource.save();
                }
            }
        } catch (MalformedURLException e1) {
            logger.error("Unable to load templates", e1);
        } catch (IOException e1) {
            logger.error("Unable to load templates", e1);
        }
        String[][] images = new String[][] { { "default.user", "yast_sysadmin.png" }, { "default.product", "colorize.png" }, { "Window.Logo", "windowlogo.png" }, { "Image.Backarrow", "3backarrow.png" } };
        for (int i = 0; i < images.length; i++) {
            Image img = new Image();
            img.setBufferedImage(ImageUtils.readImage(DatabaseValidator.class.getResource("/net/adrianromero/images/" + images[i][1])));
            img.save();
            Property imgProperty = new Property(images[i][0]);
            imgProperty.setValue("" + img.getId());
            imgProperty.save();
        }
    }

    public static Product addSampleProduct(Category c, String ref, Tax tax, String prodName, String barcode, double buy, double sell, double volume, boolean instock, boolean com, boolean scale) throws BasicException {
        Product p = new Product();
        p.setCategory(c);
        p.setReference(ref);
        if (AppConfig.getInstance().useCombinedTax()) {
            List<Tax> taxes = new ArrayList<Tax>();
            taxes.add(tax);
            p.setTaxes(taxes);
        } else {
            p.setTax(tax);
        }
        p.setName(prodName);
        p.setCode(barcode);
        p.setPriceBuy(buy);
        p.setPriceSell(sell);
        p.setVolume(volume);
        p.setInStock(instock);
        p.setCom(com);
        p.setScale(scale);
        p.save();
        return p;
    }

    public static Category addSampleCategory(String name, int visibleId, Category parent) throws BasicException {
        Category c = new Category();
        c.setName(name);
        c.setVisibleId(visibleId);
        c.setParent(parent);
        c.save();
        return c;
    }

    public static Floor addSampleFloor(String name, int visibleId) throws BasicException {
        Floor floor = new Floor();
        floor.setName(name);
        floor.setVisibleId(visibleId);
        floor.save();
        return floor;
    }

    public static Place addSamplePlace(String name, Floor floor, int x, int y) throws BasicException {
        Place place = new Place();
        place.setFloor(floor);
        place.setName(name);
        place.setX(x);
        place.setY(y);
        place.save();
        return place;
    }

    public static Tax addSampleTax(String name, int visibleId, double rate) throws BasicException {
        Tax sampTax = new Tax();
        sampTax.setName(name);
        sampTax.setVisibleId(visibleId);
        sampTax.setRate(rate);
        sampTax.save();
        return sampTax;
    }

    public static CustomerGroup addSampleCustomerGroup(String name) throws BasicException {
        CustomerGroup group = new CustomerGroup();
        group.setName(name);
        group.save();
        return group;
    }

    public static Customer addSampleCustomer(String name, String code, String city, String provstate, String fax, String phone, String postal, String contact, String street, Tax tax, CustomerGroup group, int pin) throws BasicException {
        Customer cust = new Customer();
        cust.setCode(code);
        cust.setName(name);
        cust.setCity(city);
        cust.setProvState(provstate);
        cust.setFax(fax);
        cust.setPhone(phone);
        cust.setPostal(postal);
        cust.setContactName(contact);
        cust.setStreet(street);
        cust.setTax(tax);
        cust.setGroup(group);
        cust.setPin(pin);
        cust.save();
        return cust;
    }

    public static DiscountReason addSampleDiscountReason(String name) throws BasicException {
        DiscountReason reason = new DiscountReason();
        reason.setReason(name);
        reason.save();
        return reason;
    }

    /**
     * This method provides some sample data for use during testing.
     *
     * @param appView
     * @throws BasicException
     */
    public static void loadSampleData(AppView appView) throws BasicException {
        logger.debug("Loading sample data");
        Floor floor = addSampleFloor("Main", 1);
        addSamplePlace("Table1", floor, 100, 100);
        addSamplePlace("Table2", floor, 200, 200);
        Category c = addSampleCategory("SampleCategory", 1, Category.retrieveRoot());
        Tax sampTax = addSampleTax("SampleTax", 1, 0.05);
        Tax bigTax = addSampleTax("BiggerTax", 2, 0.10);
        addSampleProduct(c, "1", sampTax, "SampleProduct", "1hj32k32", 2.0, 3.0, 10.0, true, false, false);
        addSampleProduct(c, "2", bigTax, "Kryptonite", "314159265", 180.0, 300.0, 0.5, true, false, true);
        CustomerGroup group = addSampleCustomerGroup("Wholesale Customers");
        Customer cust = addSampleCustomer("Lex Luthor's Kryptonite Emporium", "12345678", "Smallville", "Yukon", "(555) 555-1234", "(555) 555-4321", "AAA 555", "Clark Kent", "small street", sampTax, group, 0);
        DiscountReason reason = addSampleDiscountReason("Generic Discount");
        HibernateUtil.getInstance().getSession().clear();
        logger.debug("Sample data loaded");
    }

    /**
     * @throws BasicException
     *
     */
    public static void setDbVersionCurrent() throws BasicException {
        Property versionProperty = HibDAOFactory.getPropertyDAO().get(POSPER_VERSION);
        if (versionProperty == null) {
            versionProperty = new Property(POSPER_VERSION);
        }
        versionProperty.setValue(BasicProperties.getProperty(POSPER_VERSION));
        versionProperty.save();
        versionProperty = HibDAOFactory.getPropertyDAO().get(POSPER_BUILD);
        if (versionProperty == null) {
            versionProperty = new Property(POSPER_BUILD);
        }
        versionProperty.setValue(BasicProperties.getProperty(POSPER_BUILD));
        versionProperty.save();
        DatabaseValidator.setSchema();
    }
}
