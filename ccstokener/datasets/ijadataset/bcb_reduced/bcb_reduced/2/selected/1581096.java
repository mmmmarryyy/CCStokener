package alto.io;

import alto.lang.HttpRequest;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.StringTokenizer;

/**
 * Enumeration of authentication identifiers for quantifying the
 * production and consumption of authentication.
 *  
 * @author jdp
 */
public final class Authentication extends java.lang.Object {

    /**
     * Implementors are defined in
     * <code>"/META-INF/services/alto.io.Authentication"</code> as a
     * (properties) map from an {@link Authentication} name to the
     * fully qualified implementor class name.
     * 
     * Implementors are stateless.  These methods shall not employ
     * fields within their instances.  These methods may be invoked by
     * many independent threads concurrently.
     * 
     * @see alto.io.auth.SAuth
     * @see alto.io.auth.Facebook
     */
    public interface Method {

        /**
         * @return Mapped from
         */
        public Authentication getKind();

        /**
         * @param Mapped from (once only init)
         */
        public void setKind(Authentication from);

        /**
         * @return Request has info for verification in this authentication method
         */
        public boolean isVerifiable(HttpRequest request);

        /**
         * @return System UID from this authentication method
         */
        public String getUID(HttpRequest request);

        public boolean sign(Principal.Authentic keys, HttpRequest request);

        public boolean verify(Principal.Authentic keys, HttpRequest request);
    }

    /**
     * @see alto.lang.HttpMessage
     */
    public interface Container {

        /**
         * The principal is a placeholder ready for authentication
         */
        public boolean isAuthenticating();

        /**
         * The principal has been authenticated
         */
        public boolean isAuthenticated();

        /**
         * Convert a principal conditional to principal actual, or
         * return null.
         */
        public Principal.Authentic authenticate() throws java.io.IOException;

        public boolean hasAuthenticationMethod();

        public Authentication getAuthenticationMethod();

        public void setAuthenticationMethod(Authentication auth);

        /**
         * @return Has a defined authentication method on returning
         */
        public boolean maySetAuthenticationMethod(Authentication auth);

        /**
         * @return Has a defined authentication method on returning
         */
        public boolean maySetAuthenticationMethodStore();

        /**
         * Attempt to define the Thread IO Context.
         * @return Has an authentic principal on returning
         */
        public boolean maySetContext();

        public boolean isAuthVerifiable();

        public boolean authSign() throws java.io.IOException;

        public boolean authSign(Principal.Authentic principal) throws java.io.IOException;

        public boolean authVerify() throws java.io.IOException;

        public boolean authVerify(Principal.Authentic principal) throws java.io.IOException;
    }

    /**
     * This class is the package resource service for
     * <code>"/META-INF/services/alto.io.Authentication"</code>.
     * 
     * The services file is a properties map from an {@link
     * Authentication} name to a {@link Authentication$Method}
     * implementation class name.
     * 
     * @author jdp
     */
    public static final class Tools extends java.util.Properties {

        public static final String Resource = "/META-INF/services/alto.io.Authentication";

        private static final Tools Instance = new Tools();

        public static final boolean IsDefined(Authentication type) {
            return Instance.isDefined(type);
        }

        public static final boolean IsVerifiable(Authentication type, HttpRequest request) {
            return Instance.isVerifiable(type, request);
        }

        public static final boolean Sign(Authentication type, Principal.Authentic keys, HttpRequest request) {
            return Instance.sign(type, keys, request);
        }

        public static final boolean Verify(Authentication type, Principal.Authentic keys, HttpRequest request) {
            return Instance.verify(type, keys, request);
        }

        /**
         * @return Verification method
         */
        public static final Authentication.Method For(HttpRequest request) {
            return Instance.scanFor(request);
        }

        public static final Authentication.Method Lookup(Authentication auth) {
            return Instance.lookup(auth);
        }

        private final Method[] list;

        private Tools() {
            super();
            URL url = this.getClass().getResource(Resource);
            if (null != url) {
                try {
                    InputStream in = url.openStream();
                    try {
                        super.load(in);
                    } finally {
                        in.close();
                    }
                } catch (IOException exc) {
                    throw new alto.sys.Error.State(Resource, exc);
                }
            }
            int count = this.size(), cc = 0;
            Method[] list = new Method[count];
            java.util.Enumeration keys = this.keys();
            while (keys.hasMoreElements()) {
                String authtname = (String) keys.nextElement();
                Authentication type = Authentication.Lookup(authtname);
                if (null != type) {
                    String classname = (String) this.get(authtname);
                    try {
                        Class clas = Class.forName(classname);
                        Method method = (Method) clas.newInstance();
                        method.setKind(type);
                        this.put(authtname, method);
                        list[cc++] = method;
                    } catch (ClassNotFoundException exc) {
                        throw new alto.sys.Error.State(classname, exc);
                    } catch (InstantiationException exc) {
                        throw new alto.sys.Error.State(classname, exc);
                    } catch (IllegalAccessException exc) {
                        throw new alto.sys.Error.State(classname, exc);
                    }
                } else throw new alto.sys.Error.State("Unrecognized authentication type '" + authtname + "'.");
            }
            this.list = list;
        }

        protected Method lookup(Authentication type) {
            if (null != type) {
                String name = type.getName();
                Object value = this.get(name);
                if (null == value) throw new alto.sys.Error.Argument("Undefined Authentication Type '" + name + "'"); else {
                    return (Method) value;
                }
            } else throw new alto.sys.Error.Argument("Missing Authentication Type");
        }

        public boolean isDefined(Authentication type) {
            try {
                this.lookup(type);
                return true;
            } catch (alto.sys.Error exc) {
                return false;
            }
        }

        public boolean isVerifiable(Authentication type, HttpRequest request) {
            Method method = this.lookup(type);
            return method.isVerifiable(request);
        }

        public boolean sign(Authentication type, Principal.Authentic keys, HttpRequest request) {
            Method method = this.lookup(type);
            return method.sign(keys, request);
        }

        public boolean verify(Authentication type, Principal.Authentic keys, HttpRequest request) {
            Method method = this.lookup(type);
            return method.verify(keys, request);
        }

        public Authentication.Method scanFor(HttpRequest request) {
            for (Method m : this.list) {
                if (m.isVerifiable(request)) return m;
            }
            return null;
        }
    }

    public static final class Names {

        public static final String SAuth = "SAuth";

        public static final String SSL = "SSL";

        public static final String NotApp = "NotApp";

        public static final String Facebook = "Facebook";

        public static final String AWS = "AWS";

        public static final String OpenID = "OpenID";

        public static final String OAuth = "OAuth";

        public static final String App = "App";

        public static final String Any = "*";
    }

    public static final class Constants {

        public static final int None = 0x0000;

        public static final int SAuth = 0x0001;

        public static final int SSL = 0x0002;

        public static final int NotApp = 0x00ff;

        public static final int Facebook = 0x1100;

        public static final int AWS = 0x3100;

        public static final int OpenID = 0x5100;

        public static final int OAuth = 0x7100;

        public static final int App = 0xff00;

        public static final int Any = 0xffff;
    }

    public static final class Properties {

        public static final int None = 0x0000;

        public static final int KeyAndSecret = 0x0001;

        public static final int Facebook = (KeyAndSecret);
    }

    /**
     * Note that the set of actual (and defined) authentication
     * methods does not include the descriptive (or mask) types
     * "None", "App" or "Any" or "NotApp".
     * 
     * @return One of the actual (and defined) authentication methods,
     * or null for not found.
     */
    public static final Authentication Lookup(int constant) {
        switch(constant) {
            case Constants.SAuth:
                return Authentication.SAuth;
            case Constants.SSL:
                return Authentication.SSL;
            case Constants.Facebook:
                return Authentication.Facebook;
            case Constants.AWS:
                return Authentication.AWS;
            case Constants.OpenID:
                return Authentication.OpenID;
            case Constants.OAuth:
                return Authentication.OAuth;
            default:
                return null;
        }
    }

    /**
     * Specify "any method, but some method"
     */
    public static final Authentication Any = new Authentication(Names.Any, Constants.Any, Properties.None);

    /**
     * Specify "not any app method"
     */
    public static final Authentication NotApp = new Authentication(Names.NotApp, Constants.NotApp, Properties.None);

    /**
     * "SAuth" is... strong but not private
     */
    public static final Authentication SAuth = new Authentication(Names.SAuth, Constants.SAuth, Properties.None);

    /**
     * "SSL" is... strong and private
     */
    public static final Authentication SSL = new Authentication(Names.SSL, Constants.SSL, Properties.None);

    /**
     * App
     */
    public static final Authentication Facebook = new Authentication(Names.Facebook, Constants.Facebook, Properties.Facebook);

    /**
     * App
     */
    public static final Authentication AWS = new Authentication(Names.AWS, Constants.AWS, Properties.None);

    /**
     * App
     */
    public static final Authentication OpenID = new Authentication(Names.OpenID, Constants.OpenID, Properties.None);

    /**
     * App
     */
    public static final Authentication OAuth = new Authentication(Names.OAuth, Constants.OAuth, Properties.None);

    private static final alto.io.u.Objmap Map = new alto.io.u.Objmap();

    static {
        Map.put(SAuth.getName().toLowerCase(), SAuth);
        Map.put(SSL.getName().toLowerCase(), SSL);
        Map.put(Facebook.getName().toLowerCase(), Facebook);
        Map.put(AWS.getName().toLowerCase(), AWS);
        Map.put(OpenID.getName().toLowerCase(), OpenID);
        Map.put(OAuth.getName().toLowerCase(), OAuth);
    }

    public static final Authentication Lookup(String name) {
        return (Authentication) Map.get(name.trim().toLowerCase());
    }

    /**
     * Default authentication method for the store.
     */
    public static final Authentication DefaultStore = SAuth;

    public final String name;

    public final int constant;

    public final int properties;

    private Authentication.Method method;

    private Authentication(String name, int constant, int properties) {
        super();
        this.name = name;
        this.constant = constant;
        this.properties = properties;
    }

    public String getName() {
        return this.name;
    }

    public int getConstant() {
        return this.constant;
    }

    /**
     * @return True when the {@link alto.sec.Auth} object uses
     * a Key and Secret layout.
     */
    public boolean hasAuthKeyAndSecret() {
        return (Properties.KeyAndSecret == (Properties.KeyAndSecret & this.properties));
    }

    public Authentication.Method getMethod() {
        Authentication.Method method = this.method;
        if (null == method && Authentication.Tools.IsDefined(this)) {
            method = Authentication.Tools.Lookup(this);
            this.method = method;
        }
        return method;
    }

    /**
     * @return Whether sign and verify are available
     */
    public boolean isDefinedMethod() {
        return (null != this.getMethod());
    }

    /**
     * Throws an alto sys error exception when not a defined method.
     * @return Success
     */
    public boolean isVerifiable(HttpRequest request) {
        Authentication.Method method = this.getMethod();
        if (null != method) return method.isVerifiable(request); else return false;
    }

    /**
     * Throws an alto sys error exception when not a defined method.
     * @return Success
     */
    public boolean sign(Principal.Authentic keys, HttpRequest request) {
        Authentication.Method method = this.getMethod();
        if (null != method) return method.sign(keys, request); else return false;
    }

    /**
     * Throws an alto sys error exception when not a defined method.
     * @return Success
     */
    public boolean verify(Principal.Authentic keys, HttpRequest request) {
        Authentication.Method method = this.getMethod();
        if (null != method) return method.verify(keys, request); else return false;
    }

    public String getUID(HttpRequest request) {
        Authentication.Method method = this.getMethod();
        if (null != method) return method.getUID(request); else return null;
    }

    public String toString() {
        return this.name;
    }

    public int hashCode() {
        return this.constant;
    }

    public boolean equals(Object that) {
        if (this == that) return true; else if (that instanceof Authentication) {
            switch(this.constant) {
                case Constants.SAuth:
                    return (Constants.SAuth == (((Authentication) that).constant & Constants.SAuth));
                case Constants.SSL:
                    return (Constants.SSL == (((Authentication) that).constant & Constants.SSL));
                case Constants.NotApp:
                    return (Constants.None == (((Authentication) that).constant & Constants.App));
                case Constants.Facebook:
                    return (Constants.Facebook == (((Authentication) that).constant & Constants.Facebook));
                case Constants.AWS:
                    return (Constants.AWS == (((Authentication) that).constant & Constants.AWS));
                case Constants.OpenID:
                    return (Constants.OpenID == (((Authentication) that).constant & Constants.OpenID));
                case Constants.OAuth:
                    return (Constants.OAuth == (((Authentication) that).constant & Constants.OAuth));
                case Constants.App:
                    return (Constants.None != (((Authentication) that).constant & Constants.App));
                case Constants.Any:
                    return true;
                default:
                    throw new alto.sys.Error.Bug();
            }
        } else return false;
    }
}
