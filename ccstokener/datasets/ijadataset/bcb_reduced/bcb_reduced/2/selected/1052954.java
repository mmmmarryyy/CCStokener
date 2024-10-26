package org.simpleframework.tool.cp;

import java.io.InputStream;
import java.net.URL;
import java.util.Set;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.util.Dictionary;
import org.simpleframework.xml.util.Entry;

@Root(name = "config")
public class Config {

    @ElementList(inline = true)
    Dictionary<Config.Environment> list;

    public Set<Config.Environment> getEnvironments() {
        return list;
    }

    public Config.Environment getEnvironment(String name) {
        return list.get(name);
    }

    @Root(name = "environment")
    public static class Environment implements Entry {

        @Attribute
        String name;

        @Attribute
        String server;

        @Element
        String fi_server_details;

        @Element
        String server_details;

        @Element
        String host_details;

        @Commit
        public void commit() {
            String delimeter = "/";
            if (server.endsWith("/")) {
                delimeter = "";
            }
            server_details = server + delimeter + server_details;
            fi_server_details = server + delimeter + fi_server_details;
            host_details = server + delimeter + host_details;
        }

        public String getName() {
            return name;
        }

        public ServerDetails load(Serializer serializer, boolean serverDetails, boolean fiServerDetails, boolean hostDetails) throws Exception {
            ServerDetails details = new ServerDetails();
            if (serverDetails) {
                details.setSource(server_details);
                try {
                    load(serializer, server_details, details);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid XML schema for " + server_details, e);
                }
            }
            if (fiServerDetails) {
                details.setSource(fi_server_details);
                try {
                    load(serializer, fi_server_details, details);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid XML schema for " + fi_server_details, e);
                }
            }
            if (hostDetails) {
                details.setSource(host_details);
                try {
                    load(serializer, host_details, details);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid XML schema for " + host_details, e);
                }
            }
            return details;
        }

        public void load(Serializer serializer, String target, ServerDetails existing) throws Exception {
            URL url = new URL(target);
            InputStream in = url.openStream();
            serializer.read(existing, in, false);
            in.close();
        }
    }
}
