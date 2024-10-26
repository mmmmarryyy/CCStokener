package icescrum2.service.impl;

import icescrum2.dao.model.IUser;
import icescrum2.dao.model.impl.User;
import icescrum2.service.BusinessRulesService;
import icescrum2.service.ConfigurationService;
import icescrum2.service.ProductService;
import icescrum2.service.SpringApplicationContext;
import icescrum2.service.UserService;
import icescrum2.service.beans.Base64;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.util.ResourceUtils;

public class ConfigurationServiceImpl implements ConfigurationService {

    private BusinessRulesService rulesService;

    private ProductService productService;

    private PropertyPlaceholderConfigurer propertyPlaceholderConfigurer;

    private UserService userService;

    public Properties icescrum2Properties = new Properties();

    private static Log log = LogFactory.getLog(ConfigurationServiceImpl.class);

    public static final String defaultPropertiesPath = "/META-INF/configuration.properties";

    public static String userPropertiesPath;

    public static String homePath = null;

    public static String filesPath;

    public static String contextRoot = null;

    public ConfigurationServiceImpl() {
        userPropertiesPath = "file:" + System.getProperty("user.home") + "/.icescrum/" + ConfigurationServiceImpl.contextRoot + "/configuration.properties";
        defConf();
        userConf();
        ConfigurationServiceImpl.homePath = System.getProperty("user.home") + File.separator + ".icescrum" + File.separator + contextRoot;
        ConfigurationServiceImpl.filesPath = ConfigurationServiceImpl.homePath + File.separator + icescrum2Properties.get("file.directory");
    }

    private void defConf() {
        try {
            InputStream in = ConfigurationServiceImpl.class.getResourceAsStream(defaultPropertiesPath);
            icescrum2Properties.load(in);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void userConf() {
        try {
            FileInputStream in = new FileInputStream(ResourceUtils.getFile(userPropertiesPath));
            icescrum2Properties.load(in);
            in.close();
        } catch (Exception e) {
            log.info("No User Configuration found");
        }
    }

    public void setRulesService(BusinessRulesService rulesService) {
        this.rulesService = rulesService;
    }

    public void changeAdminAccess(String log, String pwd) {
    }

    private boolean simpleCheck(String login, String pwd) {
        return login != null && pwd != null && login.equals(icescrum2Properties.get("icescrum2.admin.login")) && pwd.equals(icescrum2Properties.get("icescrum2.admin.pwd"));
    }

    public IUser checkAdmin(String login, String pwd) {
        IUser u = null;
        if (simpleCheck(login, pwd)) {
            u = new User(true);
            u.setFirstName("IceScrum2");
            u.setLastName("Administration");
            u.setLogin(login);
            u.setPassword(pwd);
            u.setLanguage(1);
        }
        return u;
    }

    public void setProductService(ProductService productService) {
        this.productService = productService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public Properties getConfiguration() {
        return (Properties) this.icescrum2Properties.clone();
    }

    public boolean storeProperties(Properties configuration) {
        File f;
        try {
            f = ResourceUtils.getFile(userPropertiesPath);
            FileOutputStream out = new FileOutputStream(f);
            configuration.store(out, "IceScrum2 Configuration");
            out.close();
            this.icescrum2Properties = configuration;
            return true;
        } catch (FileNotFoundException e) {
            log.info("No User Configuration found");
        } catch (IOException e) {
            if (!configuration.equals(icescrum2Properties)) this.storeProperties(icescrum2Properties);
            e.printStackTrace();
        }
        return false;
    }

    public Properties getIcescrum2Properties() {
        return icescrum2Properties;
    }

    public static Object getValue(String key) {
        return ((ConfigurationService) SpringApplicationContext.getBean("ConfigurationService")).getIcescrum2Properties().get(key);
    }

    public void setContextRoot(String contextRoot) {
        ConfigurationServiceImpl.contextRoot = contextRoot;
    }

    public String getLastVersion() {
        try {
            String server = icescrum2Properties.get("check.url").toString();
            Boolean useProxy = new Boolean(icescrum2Properties.get("proxy.active").toString());
            Boolean authProxy = new Boolean(icescrum2Properties.get("proxy.auth.active").toString());
            URL url = new URL(server);
            if (useProxy) {
                String proxy = icescrum2Properties.get("proxy.url").toString();
                String port = icescrum2Properties.get("proxy.port").toString();
                Properties systemProperties = System.getProperties();
                systemProperties.setProperty("http.proxyHost", proxy);
                systemProperties.setProperty("http.proxyPort", port);
            }
            URLConnection connection = url.openConnection();
            if (authProxy) {
                String username = icescrum2Properties.get("proxy.auth.username").toString();
                String password = icescrum2Properties.get("proxy.auth.password").toString();
                String login = username + ":" + password;
                String encodedLogin = Base64.base64Encode(login);
                connection.setRequestProperty("Proxy-Authorization", "Basic " + encodedLogin);
            }
            connection.setConnectTimeout(Integer.parseInt(icescrum2Properties.get("check.timeout").toString()));
            InputStream input = connection.getInputStream();
            StringWriter writer = new StringWriter();
            InputStreamReader streamReader = new InputStreamReader(input);
            BufferedReader buffer = new BufferedReader(streamReader);
            String value = "";
            while (null != (value = buffer.readLine())) {
                writer.write(value);
            }
            return writer.toString();
        } catch (IOException e) {
        }
        return null;
    }
}
