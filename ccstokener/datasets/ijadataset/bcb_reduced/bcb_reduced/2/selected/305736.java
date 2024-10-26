package org.telscenter.sail.webapp.presentation.web.filters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import net.sf.sail.webapp.dao.ObjectNotFoundException;
import net.sf.sail.webapp.domain.User;
import net.sf.sail.webapp.presentation.web.filters.PasAuthenticationProcessingFilter;
import net.sf.sail.webapp.service.UserService;
import net.sf.sail.webapp.service.authentication.AuthorityNotFoundException;
import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaFactory;
import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.ui.AbstractProcessingFilter;
import org.springframework.security.ui.savedrequest.SavedRequest;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.telscenter.sail.webapp.domain.authentication.MutableUserDetails;
import org.telscenter.sail.webapp.domain.authentication.impl.StudentUserDetails;
import org.telscenter.sail.webapp.domain.authentication.impl.TeacherUserDetails;
import org.telscenter.sail.webapp.domain.portal.Portal;
import org.telscenter.sail.webapp.service.authentication.UserDetailsService;
import org.telscenter.sail.webapp.service.portal.PortalService;

/**
 * Custom AuthenticationProcessingFilter that subclasses Acegi Security. This
 * filter upon successful authentication will retrieve a <code>User</code> and
 * put it into the http session.
 *
 * @author Hiroki Terashima
 * @version $Id: TelsAuthenticationProcessingFilter.java 3144 2011-05-12 22:04:40Z honchikun@gmail.com $
 */
public class TelsAuthenticationProcessingFilter extends PasAuthenticationProcessingFilter {

    public static final String STUDENT_DEFAULT_TARGET_PATH = "/student/index.html";

    public static final String TEACHER_DEFAULT_TARGET_PATH = "/teacher/index.html";

    public static final String ADMIN_DEFAULT_TARGET_PATH = "/admin/index.html";

    public static final String RESEARCHER_DEFAULT_TARGET_PATH = "/teacher/index.html";

    public static final String LOGOUT_PATH = "pages/wiselogout.html";

    public static final Integer recentFailedLoginTimeLimit = 15;

    public static final Integer recentFailedLoginAttemptsLimit = 5;

    private UserDetailsService userDetailsService;

    private PortalService portalService;

    private Properties portalProperties;

    /**
	 * @see org.acegisecurity.ui.AbstractProcessingFilter#successfulAuthentication(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse,
	 *      org.acegisecurity.Authentication)
	 */
    @Override
    protected void successfulAuthentication(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response, Authentication authResult) throws IOException, ServletException {
        UserDetails userDetails = (UserDetails) authResult.getPrincipal();
        boolean userIsAdmin = false;
        if (userDetails instanceof StudentUserDetails) {
            Date lastLoginTime = ((StudentUserDetails) userDetails).getLastLoginTime();
            long pLT = 0L;
            if (lastLoginTime != null) {
                pLT = lastLoginTime.getTime();
            }
            this.setDefaultTargetUrl(STUDENT_DEFAULT_TARGET_PATH + "?pLT=" + pLT);
        } else if (userDetails instanceof TeacherUserDetails) {
            this.setDefaultTargetUrl(TEACHER_DEFAULT_TARGET_PATH);
            GrantedAuthority researcherAuth = null;
            try {
                researcherAuth = userDetailsService.loadAuthorityByName(UserDetailsService.RESEARCHER_ROLE);
            } catch (AuthorityNotFoundException e) {
                e.printStackTrace();
            }
            GrantedAuthority authorities[] = userDetails.getAuthorities();
            for (int i = 0; i < authorities.length; i++) {
                if (researcherAuth.equals(authorities[i])) {
                    this.setDefaultTargetUrl(RESEARCHER_DEFAULT_TARGET_PATH);
                }
            }
            GrantedAuthority adminAuth = null;
            try {
                adminAuth = userDetailsService.loadAuthorityByName(UserDetailsService.ADMIN_ROLE);
            } catch (AuthorityNotFoundException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < authorities.length; i++) {
                if (adminAuth.equals(authorities[i])) {
                    this.setDefaultTargetUrl(ADMIN_DEFAULT_TARGET_PATH);
                    userIsAdmin = true;
                }
            }
        }
        try {
            Portal portal = portalService.getById(0);
            if (!userIsAdmin && !portal.isLoginAllowed()) {
                response.sendRedirect(LOGOUT_PATH);
                return;
            }
        } catch (ObjectNotFoundException e) {
        }
        String redirectUrl = request.getParameter("redirect");
        if (StringUtils.hasText(redirectUrl)) {
            this.setDefaultTargetUrl(redirectUrl);
        }
        HttpSession session = request.getSession();
        ApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(session.getServletContext());
        UserService userService = (UserService) springContext.getBean("userService");
        SavedRequest savedRequest = (SavedRequest) session.getAttribute(AbstractProcessingFilter.SPRING_SECURITY_SAVED_REQUEST_KEY);
        if (savedRequest != null) {
            String method = savedRequest.getMethod();
            if (method != null && method.equals("POST")) {
                session.setAttribute(AbstractProcessingFilter.SPRING_SECURITY_SAVED_REQUEST_KEY, null);
            } else if (method != null && method.equals("GET")) {
                if (userDetails instanceof TeacherUserDetails && savedRequest.getRequestURL().contains(STUDENT_DEFAULT_TARGET_PATH)) {
                    session.setAttribute(AbstractProcessingFilter.SPRING_SECURITY_SAVED_REQUEST_KEY, null);
                }
            }
        }
        User user = userService.retrieveUser(userDetails);
        session.setAttribute(User.CURRENT_USER_SESSION_KEY, user);
        String reCaptchaPublicKey = portalProperties.getProperty("recaptcha_public_key");
        String reCaptchaPrivateKey = portalProperties.getProperty("recaptcha_private_key");
        boolean reCaptchaKeyValid = isReCaptchaKeyValid(reCaptchaPublicKey, reCaptchaPrivateKey);
        if (user != null && reCaptchaPrivateKey != null && reCaptchaPublicKey != null && reCaptchaKeyValid) {
            MutableUserDetails mutableUserDetails = (MutableUserDetails) user.getUserDetails();
            Date currentTime = new Date();
            Date recentFailedLoginTime = mutableUserDetails.getRecentFailedLoginTime();
            if (recentFailedLoginTime != null) {
                long timeDifference = currentTime.getTime() - recentFailedLoginTime.getTime();
                if (timeDifference < (recentFailedLoginTimeLimit * 60 * 1000)) {
                    Integer numberOfRecentFailedLoginAttempts = mutableUserDetails.getNumberOfRecentFailedLoginAttempts();
                    if (numberOfRecentFailedLoginAttempts != null && numberOfRecentFailedLoginAttempts >= recentFailedLoginAttemptsLimit) {
                        String reCaptchaChallengeField = request.getParameter("recaptcha_challenge_field");
                        String reCaptchaResponseField = request.getParameter("recaptcha_response_field");
                        String remoteAddr = request.getRemoteAddr();
                        if (reCaptchaChallengeField != null && reCaptchaResponseField != null && remoteAddr != null) {
                            ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
                            reCaptcha.setPrivateKey(reCaptchaPrivateKey);
                            ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(remoteAddr, reCaptchaChallengeField, reCaptchaResponseField);
                            if (!reCaptchaResponse.isValid()) {
                                unsuccessfulAuthentication(request, response, new AuthenticationException(remoteAddr, mutableUserDetails) {
                                });
                                return;
                            }
                        } else {
                            AuthenticationException authenticationException = new AuthenticationException(remoteAddr, mutableUserDetails) {
                            };
                            unsuccessfulAuthentication(request, response, authenticationException);
                            return;
                        }
                    }
                }
            }
        }
        super.successfulAuthentication(request, response, authResult);
        ((MutableUserDetails) userDetails).incrementNumberOfLogins();
        ((MutableUserDetails) userDetails).setLastLoginTime(Calendar.getInstance().getTime());
        ((MutableUserDetails) userDetails).setNumberOfRecentFailedLoginAttempts(0);
        userDetailsService.updateUserDetails((MutableUserDetails) userDetails);
    }

    /**
	 * Called when the user fails to log in to the portal. This function updates
	 * the recent failed login time and recent failed number of attempts.
	 * @see org.springframework.security.ui.AbstractProcessingFilter#unsuccessfulAuthentication(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.springframework.security.AuthenticationException)
	 */
    @Override
    protected void unsuccessfulAuthentication(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        Authentication authentication = failed.getAuthentication();
        String userName = "";
        if (authentication != null) {
            userName = (String) authentication.getPrincipal();
        } else {
            Object extraInformation = failed.getExtraInformation();
            if (extraInformation instanceof MutableUserDetails) {
                MutableUserDetails extraInformationUserDetails = (MutableUserDetails) extraInformation;
                userName = extraInformationUserDetails.getUsername();
            }
        }
        HttpSession session = request.getSession();
        ApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(session.getServletContext());
        UserService userService = (UserService) springContext.getBean("userService");
        User user = userService.retrieveUserByUsername(userName);
        if (user != null) {
            MutableUserDetails userDetails = (MutableUserDetails) user.getUserDetails();
            Date recentFailedLoginTime = userDetails.getRecentFailedLoginTime();
            Date currentTime = new Date();
            if (recentFailedLoginTime != null) {
                long timeDifference = currentTime.getTime() - recentFailedLoginTime.getTime();
                if (timeDifference < (recentFailedLoginTimeLimit * 60 * 1000)) {
                    userDetails.incrementNumberOfRecentFailedLoginAttempts();
                } else {
                    userDetails.setRecentFailedLoginTime(currentTime);
                    userDetails.setNumberOfRecentFailedLoginAttempts(1);
                }
            } else {
                userDetails.setRecentFailedLoginTime(currentTime);
                userDetails.setNumberOfRecentFailedLoginAttempts(1);
            }
            userService.updateUser(user);
        }
        super.unsuccessfulAuthentication(request, response, failed);
    }

    /**
	 * Get the failure url. This function checks if the public and private 
	 * keys for the captcha have been provided and if the user has failed
	 * to log in 5 or more times in the last 15 minutes. If so, it will
	 * require the failure url page to display a captcha.
	 * @see org.springframework.security.ui.AbstractProcessingFilter#determineFailureUrl(javax.servlet.http.HttpServletRequest, org.springframework.security.AuthenticationException)
	 */
    @Override
    protected String determineFailureUrl(javax.servlet.http.HttpServletRequest request, AuthenticationException failed) {
        String authenticationFailureUrl = getAuthenticationFailureUrl();
        String reCaptchaPublicKey = portalProperties.getProperty("recaptcha_public_key");
        String reCaptchaPrivateKey = portalProperties.getProperty("recaptcha_private_key");
        boolean reCaptchaKeyValid = isReCaptchaKeyValid(reCaptchaPublicKey, reCaptchaPrivateKey);
        if (reCaptchaPublicKey != null && reCaptchaPrivateKey != null && reCaptchaKeyValid) {
            Authentication authentication = failed.getAuthentication();
            String userName = "";
            if (authentication != null) {
                userName = (String) authentication.getPrincipal();
            } else {
                Object extraInformation = failed.getExtraInformation();
                if (extraInformation instanceof MutableUserDetails) {
                    MutableUserDetails extraInformationUserDetails = (MutableUserDetails) extraInformation;
                    userName = extraInformationUserDetails.getUsername();
                }
            }
            HttpSession session = request.getSession();
            ApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(session.getServletContext());
            UserService userService = (UserService) springContext.getBean("userService");
            User user = userService.retrieveUserByUsername(userName);
            if (user != null) {
                MutableUserDetails userDetails = (MutableUserDetails) user.getUserDetails();
                Integer numberOfRecentFailedLoginAttempts = userDetails.getNumberOfRecentFailedLoginAttempts();
                if (numberOfRecentFailedLoginAttempts != null && userDetails.getNumberOfRecentFailedLoginAttempts() >= recentFailedLoginAttemptsLimit) {
                    authenticationFailureUrl += "&requireCaptcha=true";
                }
            }
        }
        return authenticationFailureUrl;
    }

    /**
	 * Check to make sure the public key is valid. We can only check if the public
	 * key is valid. If the private key is invalid the admin will have to realize that.
	 * We also check to make sure the connection to the captcha server is working.
	 * @param reCaptchaPublicKey the public key
	 * @param recaptchaPrivateKey the private key
	 * @return whether the captcha is valid and should be used
	 */
    private boolean isReCaptchaKeyValid(String reCaptchaPublicKey, String recaptchaPrivateKey) {
        boolean isValid = false;
        if (reCaptchaPublicKey != null && recaptchaPrivateKey != null) {
            ReCaptcha c = ReCaptchaFactory.newReCaptcha(reCaptchaPublicKey, recaptchaPrivateKey, false);
            String recaptchaHtml = c.createRecaptchaHtml(null, null);
            Pattern pattern = Pattern.compile(".*src=\"(.*)\".*");
            Matcher matcher = pattern.matcher(recaptchaHtml);
            matcher.find();
            String match = matcher.group(1);
            try {
                URL url = new URL(match);
                URLConnection urlConnection = url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuffer text = new StringBuffer();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    text.append(inputLine);
                }
                in.close();
                String responseText = text.toString();
                if (!responseText.contains("Input error")) {
                    isValid = true;
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isValid;
    }

    /**
	 * @return the userDetailsService
	 */
    public UserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

    /**
	 * @param userDetailsService the userDetailsService to set
	 */
    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
	 * @param portalService the portalService to set
	 */
    public void setPortalService(PortalService portalService) {
        this.portalService = portalService;
    }

    /**
	 * 
	 * @return
	 */
    public Properties getPortalProperties() {
        return portalProperties;
    }

    /**
	 * 
	 * @param portalProperties
	 */
    public void setPortalProperties(Properties portalProperties) {
        this.portalProperties = portalProperties;
    }
}
