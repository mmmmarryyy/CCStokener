package com.rapidminer.operator.io.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import com.rapidminer.tools.Tools;

/**
 * <p>Provides static methods to get the default user agent
 * or a random user agent from ~7000 browser user-agents.</p> 
 * 
 * @author Marcin Skirzynski
 *
 */
public class UserAgent {

    /**
	 * The default user agent
	 */
    public static final String DEFAULT_USER_AGENT = "RapidMiner";

    /**
	 * Cache for the ~7000 browser user-agents so that it is
	 * not necessary to read the file every time
	 */
    private static String[] USER_AGENT_CACHE;

    /**
	 * <p>Returns a random browser user-agent from about 7000
	 * user agents.</p>
	 * 
	 * @return	a randomly chosen user-agent
	 */
    public static String getRandomUserAgent() {
        if (USER_AGENT_CACHE == null) {
            Collection<String> userAgentsCache = new ArrayList<String>();
            try {
                URL url = Tools.getResource(UserAgent.class.getClassLoader(), "user-agents-browser.txt");
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                String str;
                while ((str = in.readLine()) != null) {
                    userAgentsCache.add(str);
                }
                in.close();
                USER_AGENT_CACHE = userAgentsCache.toArray(new String[userAgentsCache.size()]);
            } catch (Exception e) {
                System.err.println("Can not read file; using default user-agent; error message: " + e.getMessage());
                return DEFAULT_USER_AGENT;
            }
        }
        return USER_AGENT_CACHE[new Random().nextInt(USER_AGENT_CACHE.length)];
    }
}
