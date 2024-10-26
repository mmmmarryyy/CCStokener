package org.xi8ix.jms;

import javax.naming.spi.InitialContextFactory;
import javax.naming.*;
import javax.jms.ConnectionFactory;
import javax.jms.Connection;
import javax.jms.JMSException;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URI;

/**
 * @author Iain Shigeoka
 */
public class JMSInitialContextFactory implements InitialContextFactory {

    private static final Logger LOG = Logger.getLogger(JMSInitialContextFactory.class.getName());

    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return new JMSContext(environment);
    }

    class JMSContext implements Context {

        Hashtable<?, ?> environment;

        HashMap<String, Object> context = new HashMap<String, Object>();

        public JMSContext(Hashtable<?, ?> environment) {
            this.environment = environment;
            context.put("ConnectionFactory", new ConnectionFactory() {

                public Connection createConnection() throws JMSException {
                    return createConnection(null, null);
                }

                public Connection createConnection(String string, String string1) throws JMSException {
                    return new JMSConnection(JMSContext.this);
                }
            });
            Object url = environment.get(Context.PROVIDER_URL);
            if (url != null) {
                try {
                    Properties props = new Properties();
                    props.load(new URI(url.toString()).toURL().openStream());
                    for (Object key : props.keySet()) {
                        String name = key.toString();
                        if (name.startsWith("topic.")) {
                            context.put(name.substring(6), new JMSDestination(props.getProperty(name)));
                        } else if (name.startsWith("queue.")) {
                            context.put(name.substring(6), new JMSDestination(props.getProperty(name)));
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Trouble reading context environment from URL " + url, e);
                }
            } else {
                for (Object env : environment.keySet()) {
                    String name = env.toString();
                    if (name.startsWith("topic.")) {
                        context.put(name.substring(6), new JMSDestination(environment.get(env).toString()));
                    } else if (name.startsWith("queue.")) {
                        context.put(name.substring(6), new JMSDestination(environment.get(env).toString()));
                    }
                }
            }
        }

        public Object lookup(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public Object lookup(String name) throws NamingException {
            Object result;
            if (name.startsWith("dynamicQueue/")) {
                name = name.substring(13);
                result = context.get(name);
                if (result == null) {
                    result = new JMSDestination(name);
                    context.put(name, result);
                }
            } else if (name.startsWith("dynamicTopic/")) {
                name = name.substring(13);
                result = context.get(name);
                if (result == null) {
                    result = new JMSDestination(name);
                    context.put(name, result);
                }
            } else {
                result = context.get(name);
            }
            return result;
        }

        public void bind(Name name, Object obj) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public void bind(String name, Object obj) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public void rebind(Name name, Object obj) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public void rebind(String name, Object obj) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public void unbind(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public void unbind(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public void rename(Name oldName, Name newName) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public void rename(String oldName, String newName) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public void destroySubcontext(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public void destroySubcontext(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public Context createSubcontext(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public Context createSubcontext(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public Object lookupLink(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public Object lookupLink(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public NameParser getNameParser(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public NameParser getNameParser(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public Name composeName(Name name, Name prefix) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public String composeName(String name, String prefix) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public Object addToEnvironment(String propName, Object propVal) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public Object removeFromEnvironment(String propName) throws NamingException {
            throw new OperationNotSupportedException();
        }

        public Hashtable<?, ?> getEnvironment() throws NamingException {
            return environment;
        }

        public void close() throws NamingException {
        }

        public String getNameInNamespace() throws NamingException {
            throw new OperationNotSupportedException();
        }
    }
}
