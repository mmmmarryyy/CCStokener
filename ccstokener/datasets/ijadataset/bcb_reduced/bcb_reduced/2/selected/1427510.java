package org.sqsh;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.digester.Digester;

/**
 * Used to manage general help topics.
 */
public class HelpManager {

    private static final String HELP_FILE = "org/sqsh/HelpTopics.xml";

    private Map<String, HelpTopic> helpMap = new HashMap<String, HelpTopic>();

    public HelpManager() {
        init();
    }

    /**
     * Adds a topic to the manager.
     * 
     * @param topic The new topic.
     */
    public void addTopic(HelpTopic topic) {
        helpMap.put(topic.getTopic(), topic);
    }

    /**
     * Looks up a topic.
     * 
     * @param topic The name of the topic to look up.
     * @return The topic or null if there is no help.
     */
    public HelpTopic getTopic(String topic) {
        return helpMap.get(topic);
    }

    /**
     * Returns the available help topics.
     * @return The available help topics.
     */
    public HelpTopic[] getTopics() {
        return helpMap.values().toArray(new HelpTopic[0]);
    }

    /**
     * Performs initialization of the commandMap by processing the XML
     * document in org/sqsh/commands/Commands.xml
     */
    private void init() {
        URL url = getClass().getClassLoader().getResource(HELP_FILE);
        if (url == null) {
            System.err.println("Cannot locate help definition resource " + HELP_FILE);
            return;
        }
        String path;
        Digester digester = new Digester();
        digester.setValidating(false);
        path = "Help/Topic";
        digester.addObjectCreate(path, "org.sqsh.HelpTopic");
        digester.addSetNext(path, "addTopic", "org.sqsh.HelpTopic");
        digester.addCallMethod(path, "setTopic", 1, new Class[] { java.lang.String.class });
        digester.addCallParam(path, 0, "name");
        path = "Help/Topic/Description";
        digester.addCallMethod(path, "setDescription", 1, new Class[] { java.lang.String.class });
        digester.addCallParam(path, 0);
        path = "Help/Topic/HelpText";
        digester.addCallMethod(path, "setHelp", 1, new Class[] { java.lang.String.class });
        digester.addCallParam(path, 0);
        digester.push(this);
        InputStream in = null;
        try {
            in = url.openStream();
            digester.parse(in);
        } catch (Exception e) {
            System.err.println("Failed to parse internal command file '" + HELP_FILE + "': " + e.getMessage());
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }
}
