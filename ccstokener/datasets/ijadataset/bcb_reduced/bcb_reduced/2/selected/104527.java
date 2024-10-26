package org.fspmboard.server.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.NoResultException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.BCodec;
import org.fspmboard.domain.Authority;
import org.fspmboard.domain.User;
import org.fspmboard.server.controller.UserMailAction;
import org.fspmboard.server.service.base.BaseServericeImpl;
import org.springframework.mail.MailException;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.encoding.PasswordEncoder;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;

/**
 * Implementation class for dealing with the user object
 * 
 * @author Holz, Roberto 06.11.2008 | 12:47:33
 * 
 */
@SuppressWarnings("all")
public class UserServiceImpl extends BaseServericeImpl implements UserService {

    /**
	 * Acegi Encoder for the password to avoid Passwords as clear text in the db
	 */
    private PasswordEncoder passwordEncoder;

    /**
	 * Service to send register confirmation email with link</br> to activate
	 * the user account
	 */
    private MailService mailService;

    /**
	 * represents the HttpSession for the Clientside <br>
	 * it holds logged in user
	 */
    private HashMap<String, Object> session;

    private String forumUrl;

    public User findByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Finding User by Username ({})", username);
        if (username == null || username.isEmpty()) throw new IllegalArgumentException("username cannot be null or a empty string");
        try {
            return (User) entityManager.createQuery("select u from User u where u.login=:username").setParameter("username", username).getSingleResult();
        } catch (NoResultException nrse) {
            throw new UsernameNotFoundException("user '" + username + "' not found...");
        }
    }

    public User getLoggedInUser() {
        logger.debug("Getting User from the SecurityContextHolder");
        String username = null;
        Object obj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (obj instanceof UserDetails) {
            username = ((UserDetails) obj).getUsername();
        } else {
            username = obj.toString();
        }
        if (session.get("user") == null) {
            User user = findByUsername(username);
            session.put("user", user);
            logger.debug("Added user({}) to Session", user.toString());
            return user;
        } else {
            User sessionUser = (User) session.get("user");
            if (sessionUser.getLogin().equals(username)) {
                logger.debug("Found user({}) in Session", sessionUser.toString());
                return sessionUser;
            } else {
                User user = findByUsername(username);
                session.put("user", user);
                logger.debug("Added user({}) to Session", user.toString());
                return user;
            }
        }
    }

    public boolean register(Object o) {
        String passwordAsText;
        if (o == null) throw new IllegalArgumentException("object cannot be null");
        if (!(o instanceof User)) {
            throw new IllegalArgumentException("passed argument is not an instance of the User class");
        }
        User newUser = (User) o;
        passwordAsText = newUser.getPassword();
        newUser.setPassword(passwordEncoder.encodePassword(passwordAsText, null));
        newUser.setRegDate(new Date());
        logger.debug("Setting default Authority {} to new user!", Authority.DEFAULT_NAME);
        newUser.getAuthorities().add(super.find(Authority.class, 1));
        logger.debug("Creating hash from email address! using Base64");
        newUser.setHash(new String(Base64.encodeBase64(newUser.getEmail().getBytes())));
        logger.debug("Creating phpBB forum User, by calling URL: {}", forumUrl);
        try {
            StringBuilder urlString = new StringBuilder(forumUrl);
            urlString.append("phpBB.php?action=register").append("&login=").append(newUser.getLogin()).append("&password=").append(passwordAsText).append("&email=").append(newUser.getEmail());
            sqlInjectionPreventer(urlString.toString());
            logger.debug("Connecting to URL: {}", urlString.toString());
            URL url = new URL(urlString.toString());
            URLConnection urlCon = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) newUser.setForumID(Integer.valueOf(inputLine));
            in.close();
        } catch (IOException io) {
            logger.error("Connecting failed! Msg: {}", io.getMessage());
            throw new RuntimeException("Couldn't conntect to phpBB");
        } catch (NumberFormatException e) {
            logger.error("phpBB user generation failed! Msg: {}", e.getMessage());
            throw new RuntimeException("phpBB user generation failed!");
        }
        entityManager.persist(newUser);
        try {
            sendConfirmationEmail(newUser);
            return true;
        } catch (MailException ex) {
            return false;
        }
    }

    public boolean isPasswordValid(String password, String toCompare) {
        return passwordEncoder.isPasswordValid(password, toCompare, null);
    }

    public User findUserByEmail(String email) {
        return (User) entityManager.createQuery("select u from User u where u.email = :email").setParameter("email", email).getSingleResult();
    }

    @SuppressWarnings("unchecked")
    private void sendConfirmationEmail(final User user) throws MailException {
        final String subject = "Account Confirmation FSPM_Board";
        final String template = "mail/registration-confirmation.vm";
        final Map model = new HashMap();
        model.put("user", user);
        model.put("confirm", UserMailAction.CONFIRMATION);
        model.put("reject", UserMailAction.REJECT);
        mailService.sendMessage(user.getEmail(), subject, template, model);
    }

    public void setSession(HashMap<String, Object> session) {
        this.session = session;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setForumUrl(String forumUrl) {
        this.forumUrl = forumUrl;
    }

    public String getForumUrl() {
        return forumUrl;
    }
}
