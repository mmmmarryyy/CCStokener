package com.sri.scenewiz.gaedb;

import com.google.appengine.api.users.User;
import com.sri.scenewiz.controller.SitesHelper2;
import com.sri.scenewiz.persist.PersistFactory;
import com.sri.scenewiz.persist.Rating;
import com.sri.scenewiz.persist.Scenario;
import com.sri.scenewiz.persist.Task;
import com.sri.scenewiz.persist.Token;
import com.sri.scenewiz.persist.UserProps;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Repository;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class ScenarioServiceImpl implements ScenarioService {

    private static Logger logger = Logger.getLogger(ScenarioServiceImpl.class.getName());

    private static Pattern SHORTNAME_EXTRACTION = Pattern.compile("^http.*//sites.google.com/site/(.*)$");

    @Override
    public Scenario add(Scenario scenario) {
        boolean success = false;
        PersistenceManager pm = null;
        Transaction tx = null;
        try {
            pm = PersistFactory.get().getPersistenceManager();
            tx = pm.currentTransaction();
            tx.begin();
            scenario = pm.makePersistent(scenario);
            tx.commit();
            success = true;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Cannot add scenario: ", t);
        } finally {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            if (pm != null && !pm.isClosed()) pm.close();
        }
        if (success) updateUsername(scenario);
        return scenario;
    }

    private void updateUsername(Scenario scenario) {
        PersistenceManager pm = null;
        try {
            pm = PersistFactory.get().getPersistenceManager();
            Query query = pm.newQuery(UserProps.class);
            query.declareParameters("String myUserId");
            query.setFilter("userId == myUserId");
            @SuppressWarnings({ "unchecked" }) List<UserProps> uprops = (List<UserProps>) query.execute(scenario.getAuthor().getUserId());
            UserProps up;
            if (uprops.size() > 0) {
                up = uprops.get(0);
                up.setName(scenario.getAuthorName());
            } else {
                up = new UserProps(scenario.getAuthor(), scenario.getAuthorName(), false);
            }
            pm.makePersistent(up);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "cannot update username: ", e);
        } finally {
            if (pm != null && !pm.isClosed()) pm.close();
        }
    }

    /**
     * @param scenario incoming information; may or may not include original ID from persistence; NEVER includes task updates
     */
    @Override
    public Scenario update(Scenario scenario) {
        boolean success = false;
        PersistenceManager pm = null;
        Transaction tx = null;
        Scenario existingEntity = null;
        try {
            pm = PersistFactory.get().getPersistenceManager();
            tx = pm.currentTransaction();
            tx.begin();
            try {
                existingEntity = pm.getObjectById(Scenario.class, scenario.getId());
            } catch (Exception e) {
                throw new IllegalArgumentException("cannot find scenario with id: " + scenario.getId());
            }
            Map map;
            try {
                scenario.setTasks(null);
                map = BeanUtils.describe(scenario);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
            map.remove("id");
            map.remove("author");
            map.remove("ratings");
            map.remove("tasks");
            map.put("touched", new Date());
            try {
                BeanUtils.populate(existingEntity, map);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
            tx.commit();
            success = true;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Cannot update scenario: ", t);
        } finally {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            if (pm != null && !pm.isClosed()) pm.close();
        }
        if (success) updateUsername(scenario);
        return existingEntity;
    }

    @Override
    public Scenario findById(Long key) {
        if (key == null) return null;
        Scenario detachedCopy = null, object;
        PersistenceManager pm = null;
        try {
            pm = PersistFactory.get().getPersistenceManager();
            object = pm.getObjectById(Scenario.class, key);
            object.getRatings().size();
            detachedCopy = pm.detachCopy(object);
        } catch (Exception e) {
            return null;
        } finally {
            if (pm != null && !pm.isClosed()) pm.close();
        }
        return detachedCopy;
    }

    public List<Scenario> getAll(User optionalUser) {
        PersistenceManager pm = null;
        List<Scenario> list;
        try {
            pm = PersistFactory.get().getPersistenceManager();
            Query q = pm.newQuery("select from " + Scenario.class.getName());
            q.setOrdering("scenTitle");
            list = (List<Scenario>) q.execute();
            for (Scenario scenario : list) {
                Float sum = 0f;
                int count = 0;
                for (Rating rating : scenario.getRatings()) {
                    count++;
                    sum += rating.getRating();
                    if (optionalUser != null && rating.getRaterId().equals(optionalUser.getUserId())) {
                        scenario.setMyRating(rating.getRating());
                    }
                }
                if (sum > 0) scenario.setAvgRating(sum / count);
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Cannot list scenarios: ", t);
            list = new ArrayList<Scenario>();
        } finally {
            if (pm != null && !pm.isClosed()) pm.close();
        }
        return list;
    }

    /**
     *
     * @return rating by this user for this scenario; null if not found
     */
    @Override
    public Rating getRating(User user, Long scenarioId) {
        if (user == null) return null;
        Scenario scenario = findById(scenarioId);
        if (scenario == null) {
            logger.log(Level.SEVERE, "cannot find scenario for id: " + scenarioId);
            return null;
        }
        Rating result = null;
        if (scenario.getRatings() != null) {
            for (Rating rating : scenario.getRatings()) {
                if (rating.getRaterId() != null && rating.getRaterId().equals(user.getUserId())) {
                    result = rating;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public void addRating(User user, Long scenarioId, Integer rating) {
        PersistenceManager pm = null;
        Transaction tx = null;
        try {
            pm = PersistFactory.get().getPersistenceManager();
            tx = pm.currentTransaction();
            tx.begin();
            Scenario scenario = findById(scenarioId);
            if (scenario == null) {
                throw new IllegalArgumentException("cannot find scenario for id: " + scenarioId);
            }
            Rating ratingObj = new Rating(user, scenario, rating);
            scenario.getRatings().add(ratingObj);
            pm.makePersistent(scenario);
            tx.commit();
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Cannot add rating: ", t);
        } finally {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            if (pm != null && !pm.isClosed()) pm.close();
        }
    }

    /**
     * save to db only; does not affect Google Site pages
     */
    @Override
    public void addTask(Task task) throws Exception {
        PersistenceManager pm = null;
        Transaction tx = null;
        try {
            pm = PersistFactory.get().getPersistenceManager();
            tx = pm.currentTransaction();
            tx.begin();
            pm.makePersistent(task);
            task.getScenario().incTasks();
            task.getScenario().setTouched(new Date());
            pm.makePersistent(task.getScenario());
            tx.commit();
        } catch (Exception t) {
            logger.log(Level.SEVERE, "Cannot add task: ", t);
            throw t;
        } finally {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            if (pm != null && !pm.isClosed()) pm.close();
        }
    }

    @Override
    public Rating updateRating(Rating rating) {
        Rating existingEntity = null;
        PersistenceManager pm = null;
        try {
            pm = PersistFactory.get().getPersistenceManager();
            try {
                existingEntity = pm.getObjectById(Rating.class, rating.getKey());
            } catch (Exception e) {
                throw new IllegalArgumentException("cannot find rating with id: " + rating.getKey());
            }
            existingEntity.setRating(rating.getRating());
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Cannot update rating: ", t);
        } finally {
            if (pm != null && !pm.isClosed()) pm.close();
        }
        return existingEntity;
    }

    @Override
    public void touch(Scenario scenario) {
        PersistenceManager pm = null;
        try {
            pm = PersistFactory.get().getPersistenceManager();
            Scenario existingEntity;
            try {
                existingEntity = pm.getObjectById(Scenario.class, scenario.getId());
            } catch (Exception e) {
                throw new IllegalArgumentException("cannot find scenario with id: " + scenario.getId());
            }
            try {
                existingEntity.setTouched(new Date());
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Cannot touch scenario date: ", t);
        } finally {
            if (pm != null && !pm.isClosed()) pm.close();
        }
    }

    @Override
    public Scenario findByScenarioUrl(String myUrl) {
        PersistenceManager pm = null;
        Scenario result = null;
        try {
            pm = PersistFactory.get().getPersistenceManager();
            Query query = pm.newQuery(Scenario.class);
            query.declareParameters("String myUrl");
            query.setFilter("url == myUrl");
            @SuppressWarnings({ "unchecked" }) List<Scenario> scenarios = (List<Scenario>) query.execute(myUrl);
            if (scenarios.size() > 0) {
                result = scenarios.get(0);
            }
        } finally {
            if (pm != null && !pm.isClosed()) pm.close();
        }
        return result;
    }

    @Override
    public void delete(Scenario scenario) {
        PersistenceManager pm = null;
        Transaction tx = null;
        try {
            pm = PersistFactory.get().getPersistenceManager();
            tx = pm.currentTransaction();
            tx.begin();
            Scenario existingEntity;
            try {
                existingEntity = pm.getObjectById(Scenario.class, scenario.getId());
            } catch (Exception e) {
                throw new IllegalArgumentException("cannot find scenario with id: " + scenario.getId());
            }
            pm.deletePersistent(existingEntity);
            tx.commit();
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Cannot delete scenario: ", t);
        } finally {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            if (pm != null && !pm.isClosed()) pm.close();
        }
    }

    @Override
    public Token getOauthToken(String userId) {
        Token result = null;
        Token found = null;
        PersistenceManager pm = null;
        try {
            pm = PersistFactory.get().getPersistenceManager();
            Query query = pm.newQuery(Token.class);
            query.declareParameters("String myUserId");
            query.setFilter("userId == myUserId");
            @SuppressWarnings({ "unchecked" }) List<Token> tokens = (List<Token>) query.execute(userId);
            if (tokens.size() > 0) {
                found = tokens.get(0);
            }
            if (found != null) result = pm.detachCopy(found);
        } catch (Exception e) {
        } finally {
            if (pm != null && !pm.isClosed()) pm.close();
        }
        return result;
    }

    @Override
    public void saveOauthToken(Token token) {
        String userId = null;
        if (token != null) userId = token.getUserId();
        saveOauthToken(token, userId);
    }

    public void saveOauthToken(Token token, String userId) {
        PersistenceManager pm = null;
        try {
            pm = PersistFactory.get().getPersistenceManager();
            Query query = pm.newQuery(Token.class);
            query.declareParameters("String myUserId");
            query.setFilter("userId == myUserId");
            @SuppressWarnings({ "unchecked" }) List<Token> tokens = (List<Token>) query.execute(userId);
            for (Token token1 : tokens) {
                pm.deletePersistent(token1);
            }
            if (token != null) pm.makePersistent(token);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "cannot save token: ", e);
        } finally {
            if (pm != null && !pm.isClosed()) pm.close();
        }
    }

    @Override
    public OAuthConsumer getOauthConsumer() {
        return new DefaultOAuthConsumer(SitesHelper2.CONSUMER_KEY, SitesHelper2.CONSUMER_SECRET);
    }

    @Override
    public OAuthProvider getOauthProvider() throws UnsupportedEncodingException {
        StringBuffer buf = new StringBuffer(OAUTH_SCOPE_PREFIX);
        buf.append(URLEncoder.encode(OAUTH_SCOPE, "utf-8"));
        return new DefaultOAuthProvider(buf.toString(), GOOGLE_OAUTH_GET_ACCESS_TOKEN, GOOGLE_OAUTH_AUTHORIZE);
    }

    private boolean validateToken(Token token, String shortname) throws Exception {
        logger.info("validating token: " + token.getToken());
        OAuthConsumer oauthConsumer = getOauthConsumer();
        oauthConsumer.setTokenWithSecret(token.getToken(), token.getSecret());
        return canAccessGoogleSites(oauthConsumer, shortname);
    }

    @Override
    public boolean validateToken(Token token) throws Exception {
        return validateToken(token, null);
    }

    /**
      * TODO determine if failure is due to something besides user-caused problem 
      * @return true if sites access ok
      */
    private boolean canAccessGoogleSites(OAuthConsumer consumer, String shortname) throws Exception {
        String target = ScenarioService.GOOGLE_SITES_FEED;
        if (shortname != null) target += shortname + "/";
        URL url = new URL(target);
        HttpURLConnection urlRequest = (HttpURLConnection) url.openConnection();
        consumer.sign(urlRequest);
        urlRequest.connect();
        boolean result = 200 == urlRequest.getResponseCode();
        if (!result) {
            logger.warning("failed request code, msg: " + urlRequest.getResponseCode() + ", " + urlRequest.getResponseMessage());
        }
        return result;
    }

    @Override
    public void deleteToken(String userId) {
        saveOauthToken(null, userId);
    }

    /**
     * @return shortname from Sites URL
     * @throws Exception if URL does not validate
     */
    @Override
    public String extractShortname(String url) throws Exception {
        if (url == null) throw new Exception("Cannot match submitted URL: '" + url + "' to expected Google Sites URL like \'http://sites.google.com/site/mygreatnewscenario/\'");
        Matcher shortNameMatch = SHORTNAME_EXTRACTION.matcher(url);
        if (!shortNameMatch.matches() || shortNameMatch.groupCount() <= 0) {
            throw new Exception("Cannot match submitted URL: '" + url + "' to expected Google Sites URL like \'http://sites.google.com/site/mygreatnewscenario/\'");
        }
        String shortName = shortNameMatch.group(1);
        int firstSlash = shortName.indexOf("/");
        if (firstSlash != -1) {
            shortName = shortName.substring(0, firstSlash);
        }
        if (shortName.length() == 0) throw new Exception("Submitted URL: '" + url + "' does not match expected URL for " + "Google Sites like \'http://sites.google.com/site/mygreatnewscenario/\'");
        return shortName;
    }

    public String getUsername(User user) {
        String result = "";
        if (user != null) {
            result = user.getNickname();
            PersistenceManager pm = null;
            try {
                pm = PersistFactory.get().getPersistenceManager();
                Query query = pm.newQuery(UserProps.class);
                query.declareParameters("String myUserId");
                query.setFilter("userId == myUserId");
                @SuppressWarnings({ "unchecked" }) List<UserProps> props = (List<UserProps>) query.execute(user.getUserId());
                if (props.size() > 0) {
                    UserProps found = props.get(0);
                    String name = found.getName();
                    if (name != null && name.length() > 0) result = name;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "cannot get username: ", e);
            } finally {
                if (pm != null && !pm.isClosed()) pm.close();
            }
        }
        return result;
    }

    @Override
    public Scenario findByIdWithTasks(Long key) {
        if (key == null) return null;
        Scenario detachedCopy = null, object;
        PersistenceManager pm = null;
        try {
            pm = PersistFactory.get().getPersistenceManager();
            object = pm.getObjectById(Scenario.class, key);
            object.getRatings().size();
            object.getTasks().size();
            detachedCopy = pm.detachCopy(object);
        } catch (Exception e) {
            return null;
        } finally {
            if (pm != null && !pm.isClosed()) pm.close();
        }
        return detachedCopy;
    }
}
