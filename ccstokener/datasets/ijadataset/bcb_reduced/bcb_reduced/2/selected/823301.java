package org.fspmboard.server.rpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.fspmboard.client.rpc.UserServiceRemote;
import org.fspmboard.domain.User;
import org.fspmboard.server.AbstractIntegrationTest;
import org.fspmboard.server.service.UserService;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;

/**
 * 
 * 
 * @author Holz, Roberto
 * 04.12.2008 | 13:49:12
 * 
 * TASK Test specific methods valid password/username etc
 */
public class UserServiceRemoteTest extends AbstractIntegrationTest {

    private UserServiceRemote userServiceRemote;

    private UserService userService;

    @Required
    public void setUserServiceRemote(UserServiceRemote userServiceRemote) {
        this.userServiceRemote = userServiceRemote;
    }

    @Required
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public static final class UserMapper implements RowMapper {

        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setLogin(rs.getString("login"));
            user.setPassword(rs.getString("password"));
            user.setEmail(rs.getString("email"));
            user.setHash(rs.getString("hash"));
            user.setRegDate(rs.getDate("REGISTRATION_DATE"));
            user.setUniversity(rs.getString("university"));
            user.setForumID(rs.getInt("FORUM_ID"));
            return user;
        }
    }

    /**
	 * testing registering a new user from client side, includes email sending
	 * ATTENTION for proofing the email of hash code check down
	 */
    public void testRegister() throws IOException {
        User newUser = new User(false, "testregUser", "regUser");
        newUser.setEmail("eagle-r@gmx.de");
        newUser.setUniversity("uni");
        newUser.setFirstName("first");
        newUser.setLastName("last");
        User regUser = null;
        try {
            regUser = (User) getJdbcTemplate().queryForObject("select id, login, password, email, hash, REGISTRATION_DATE, university, FORUM_ID from USER where login = ?", new Object[] { newUser.getUsername() }, new UserMapper());
        } catch (EmptyResultDataAccessException e) {
        }
        assertNull("This test user already exists! Abort test", regUser);
        userServiceRemote.registrate(newUser);
        setComplete();
        endTransaction();
        regUser = (User) getJdbcTemplate().queryForObject("select id, login, password, email, hash, REGISTRATION_DATE, university, FORUM_ID from USER where login = ?", new Object[] { newUser.getUsername() }, new UserMapper());
        assertNotNull(regUser);
        assertNotNull(regUser.getId());
        assertNotNull(regUser.getHash());
        assertFalse(regUser.getHash().isEmpty());
        assertEquals(regUser.getLogin(), newUser.getLogin());
        assertEquals(regUser.getPassword(), newUser.getPassword());
        assertEquals(regUser.getUniversity(), newUser.getUniversity());
        assertEquals(regUser.getEmail(), newUser.getEmail());
        Integer id = newUser.getId();
        getJdbcTemplate().execute("DELETE FROM USER_AUTHORITIES WHERE USER_ID =" + id);
        getJdbcTemplate().execute("DELETE FROM USER WHERE ID = " + id);
        StringBuilder urlString = new StringBuilder(userService.getForumUrl());
        urlString.append("phpBB.php?action=remove").append("&id=").append(newUser.getForumID()).append("&mode=remove");
        logger.debug("Connecting to URL: " + urlString.toString());
        URL url = new URL(urlString.toString());
        URLConnection con = url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) logger.debug("Response: " + inputLine);
        in.close();
    }
}
