package sharedPhoto.server.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.StringTokenizer;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import sharedPhoto.client.model.User;
import sharedPhoto.client.services.Security;
import sharedPhoto.server.model.UserDAO;

public class SecurityImpl extends BaseService implements Security {

    private static final long serialVersionUID = 6469360822036193966L;

    private static final Logger logger = Logger.getLogger(SecurityImpl.class);

    public User authenticateUser(String username, String password) {
        logger.debug("INIT logging " + username);
        User user = getUser(username);
        if ((user != null) && user.getPassword().equals(password)) {
            HttpSession session = getThreadLocalRequest().getSession(true);
            session.setAttribute("user", user);
            logger.debug("User logged");
            return new User(user.getLogin(), "", user.getProfile());
        }
        logger.debug("Logging rejected");
        return null;
    }

    public User getUser(String userlogin) {
        UserDAO userDAO = new UserDAO();
        User user = null;
        try {
            user = userDAO.load(userlogin);
            if (user == null) {
                URL url = Thread.currentThread().getContextClassLoader().getResource("users.cfg");
                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                String linea = br.readLine();
                while (linea != null) {
                    StringTokenizer st = new StringTokenizer(linea, ":");
                    if (st.countTokens() == 3) {
                        String login = st.nextToken();
                        String password = st.nextToken();
                        String profile = st.nextToken();
                        if (login.equals(userlogin)) {
                            user = new User(login, password, profile);
                            userDAO.save(user);
                        }
                    } else {
                    }
                    linea = br.readLine();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }

    public List<User> getUserList() {
        try {
            return (new UserDAO()).getList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isSecured() {
        return false;
    }
}
