package net.sf.webwarp.modules.user.acegi.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.webwarp.modules.user.UserGroup;
import net.sf.webwarp.modules.user.acegi.AcegiUser;
import net.sf.webwarp.modules.user.acegi.AcegiUserDAO;
import net.sf.webwarp.modules.user.impl.UserDAOImpl;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.apache.commons.io.IOUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link UserDetailsService} from acegi, extending the {@link UserDAOImpl}.
 * 
 * @author mos
 * @param <U>
 *            The {@link AcegiUser} implementation.
 * @param <G>
 *            The {@link UserGroup} implementation.
 */
@Transactional
public class AcegiUserDAOImpl<U extends AcegiUser, G extends UserGroup> extends UserDAOImpl<U, G> implements AcegiUserDAO<U, G> {

    private static final String ROLE_FILE_LOCATION = "META-INF/roles.txt";

    public U loadUserByUsername(String userName) {
        U user = (U) super.getUser(userName, USER_USER_GROUP_ROLES);
        if (user == null) {
            throw new UsernameNotFoundException("user with name: " + userName + " not found");
        }
        return user;
    }

    public Set<String> getAvailableRoles() {
        if (availableRoles == null) {
            availableRoles = new HashSet<String>();
            try {
                Enumeration<URL> resources = org.springframework.util.ClassUtils.getDefaultClassLoader().getResources(ROLE_FILE_LOCATION);
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    InputStream is = null;
                    try {
                        URLConnection con = url.openConnection();
                        con.setUseCaches(false);
                        is = con.getInputStream();
                        List<String> lines = IOUtils.readLines(is, "ISO-8859-1");
                        if (lines != null) {
                            for (String line : lines) {
                                availableRoles.add(line.trim());
                            }
                        }
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return availableRoles;
    }
}
