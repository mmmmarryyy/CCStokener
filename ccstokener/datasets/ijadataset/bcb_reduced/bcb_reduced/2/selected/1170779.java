package com.pavco.caribbeanvisit.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.JDOUserException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.pavco.caribbeanvisit.client.RpcService;
import com.pavco.caribbeanvisit.client.objects.LoginInfo;
import com.pavco.caribbeanvisit.shared.models.Attraction;
import com.pavco.caribbeanvisit.shared.models.Country;
import com.pavco.caribbeanvisit.shared.models.Tag;

@SuppressWarnings("serial")
public class GenericServiceImpl extends RemoteServiceServlet implements RpcService {

    @Override
    public String getFeedFeed(String sUrl) {
        try {
            URL url = new URL(sUrl);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String result = "";
            String line;
            for (; (line = reader.readLine()) != null; result += line) {
            }
            reader.close();
            return result;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return null;
    }

    public HashMap<String, String> getPageDefaultData() {
        HashMap<String, String> result = new HashMap<String, String>();
        result.put("CurrentUser", getCurrentUser());
        return result;
    }

    private String getCurrentUser() {
        return null;
    }

    @Override
    public ArrayList<Country> getAllCountries() {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Country.class);
        query.setOrdering("name asc");
        try {
            List<Country> results = (List<Country>) query.execute();
            ArrayList<Country> countries = new ArrayList<Country>(results.size());
            for (Country country : results) {
                Country c = new Country(country.getName(), country.getLatitude(), country.getLongitude());
                countries.add(c);
            }
            return countries;
        } catch (Exception e) {
            System.out.println(e.toString());
        } finally {
            query.closeAll();
            pm.close();
        }
        return null;
    }

    @Override
    public void addCountry(Country country) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Country c = new Country(country.getName(), country.getLatitude(), country.getLongitude());
        try {
            pm.makePersistent(c);
        } catch (Exception e) {
            System.out.println(e.toString());
        } finally {
            pm.close();
        }
    }

    @Override
    public Country getCountry(String countryName) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        pm.setDetachAllOnCommit(true);
        Country country = null;
        try {
            country = pm.getObjectById(Country.class, countryName);
            Country c = new Country(country.getName(), country.getLatitude(), country.getLongitude());
            return c;
        } catch (JDOObjectNotFoundException e) {
            return null;
        } catch (JDOUserException e) {
            return null;
        } finally {
            pm.close();
        }
    }

    @Override
    public ArrayList<Country> getCountriesWithAttractionsWithTags() {
        ArrayList<Country> countries = new ArrayList<Country>();
        HashSet<String> uniqueCountries = new HashSet<String>();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Attraction.class);
        try {
            ArrayList<Attraction> results = new ArrayList<Attraction>((List<Attraction>) query.execute());
            if (results.iterator().hasNext()) {
                for (Attraction attraction : results) {
                    if (attraction.getTags().size() > 0) {
                        String countryId = attraction.getCountryName();
                        uniqueCountries.add(countryId);
                    }
                }
            }
            if (uniqueCountries.size() == 0) {
                return countries;
            }
            for (String countryId : uniqueCountries) {
                Country country = pm.getObjectById(Country.class, countryId);
                Country c = new Country(country.getName(), country.getLatitude(), country.getLongitude());
                countries.add(c);
            }
            return countries;
        } catch (JDOObjectNotFoundException jdoonfe) {
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            query.closeAll();
            pm.close();
        }
    }

    @Override
    public ArrayList<Attraction> getAttraction(String attractionName) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Attraction.class);
        query.setFilter("name == attractionNameParam");
        query.setOrdering("name desc");
        query.declareParameters("String attractionNameParam");
        ArrayList<Attraction> results = null;
        try {
            results = (ArrayList<Attraction>) query.execute(attractionName);
        } finally {
            query.closeAll();
            pm.close();
        }
        return results;
    }

    @Override
    public ArrayList<Attraction> getAttractions(ArrayList<String> countries, ArrayList<String> tags) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Attraction.class);
        try {
            List<Attraction> results = (List<Attraction>) query.execute();
            ArrayList<Attraction> attractions = new ArrayList<Attraction>(results.size());
            for (Attraction attraction : results) {
                Attraction a = new Attraction();
                a.setAttractionName(attraction.getName());
                a.setLocation(attraction.getLatitude(), attraction.getLongitude());
                a.setCountryName(attraction.getCountryName());
                for (Tag t : attraction.getTags()) {
                    a.addTag(t);
                }
                attractions.add(a);
                return attractions;
            }
        } finally {
            query.closeAll();
            pm.close();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<String> getAllTags() {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Tag.class);
        ArrayList<String> tags = new ArrayList<String>();
        try {
            List<Tag> results = (List<Tag>) query.execute();
            for (Tag t : results) {
                tags.add(t.getName());
            }
            return tags;
        } finally {
            query.closeAll();
            pm.close();
        }
    }

    @Override
    public LoginInfo login(String requestUri) {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        LoginInfo loginInfo = new LoginInfo();
        if (user != null) {
            loginInfo.setLoggedIn(true);
            loginInfo.setEmailAddress(user.getEmail());
            loginInfo.setNickname(user.getNickname());
            loginInfo.setLogoutUrl(userService.createLogoutURL(requestUri));
        } else {
            loginInfo.setLoggedIn(false);
            loginInfo.setLoginUrl(userService.createLoginURL(requestUri));
        }
        return loginInfo;
    }
}
