package be.kuleuven.cw.peno3.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import be.kuleuven.VTKfakbarCWA1.data.WebserviceAdressContainer;
import be.kuleuven.VTKfakbarCWA1.model.management.User;
import be.kuleuven.VTKfakbarCWA1.model.management.UserFunction;
import be.kuleuven.VTKfakbarCWA1.model.management.UserManager;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class UserDAO {

    private static final String BASEURL = "http://localhost:9876/";

    private static UserManager userManager = UserManager.getSingletonUserManager();

    public static void main(String[] args) {
        User newUser = userManager.newUser("Nathan", "Nathan", "Bekaert", "geheim", UserFunction.ADMIN);
        newUser.setId(91);
        deleteUser(newUser);
    }

    public static List<User> searchUsers(String searchString) {
        List<User> returner = new ArrayList<User>();
        if (searchString != null) {
            try {
                String url = BASEURL + "UserHandler/searchUsers?name=" + searchString;
                String json = stringOfUrl(url);
                returner = mapUsersToList(json);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return returner;
    }

    public static User getUser(Integer id) {
        if (id != null && id > 0) {
            try {
                String url = BASEURL + "UserHandler/getUser?id=" + id;
                String json = stringOfUrl(url);
                User user = mapUser(json);
                return user;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static List<User> listUsers() {
        List<User> returner = new ArrayList<User>();
        try {
            String json = stringOfUrl(BASEURL + "UserHandler/listUsers");
            returner = mapUsersToList(json);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returner;
    }

    public static List<User> listAll() {
        List<User> returner = new ArrayList<User>();
        try {
            String json = stringOfUrl(BASEURL + "UserHandler/listAll");
            returner = mapUsersToList(json);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returner;
    }

    public static boolean updateUser(User userToUpdate) {
        if (userToUpdate != null) {
            try {
                String jsonuser = new Gson().toJson(userToUpdate);
                printUser(userToUpdate);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(BASEURL + "UserHandler/updateUser");
                method.addParameter("user", jsonuser);
                int returnCode = client.executeMethod(method);
                String result = method.getResponseBodyAsString();
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean addUserSimple(User userToAdd) {
        if (userToAdd != null) {
            try {
                printUser(userToAdd);
                String url = BASEURL + "UserHandler/addUserLong?userName=" + userToAdd.getUserName().replace(" ", "%20") + "&passWord=" + userToAdd.getPassWord().replace(" ", "%20") + "&firstName=" + userToAdd.getFirstName().replace(" ", "%20") + "&lastName=" + userToAdd.getLastName().replace(" ", "%20") + "&function=" + userToAdd.getFunction();
                System.out.println(url);
                String resultString = stringOfUrl(url);
                System.out.println(resultString);
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean addUser(User userToAdd) {
        if (userToAdd != null) {
            try {
                String jsonuser = new Gson().toJson(userToAdd);
                printUser(userToAdd);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(BASEURL + "UserHandler/addUser");
                method.addParameter("user", jsonuser);
                int returnCode = client.executeMethod(method);
                System.out.println(method.getResponseBodyAsString());
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean deleteUserSimple(User userToDelete) {
        if (userToDelete != null) {
            try {
                printUser(userToDelete);
                String url = BASEURL + "UserHandler/deleteUserID?id=" + userToDelete.getId();
                System.out.println(url);
                String resultString = stringOfUrl(url);
                System.out.println(resultString);
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean deleteUser(User userToDelete) {
        if (userToDelete != null) {
            try {
                String jsonuser = new Gson().toJson(userToDelete);
                printUser(userToDelete);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(BASEURL + "UserHandler/deleteUser");
                method.addParameter("user", jsonuser);
                int returnCode = client.executeMethod(method);
                System.out.println(method.getResponseBodyAsString());
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean deactivateUser(User userToDeactivate) {
        if (userToDeactivate != null) {
            try {
                String url = BASEURL + "UserHandler/deactivateUser?id=" + userToDeactivate.getId();
                String resultString = stringOfUrl(url);
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static String stringOfUrl(String addr) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        URL url = new URL(addr);
        IOUtils.copy(url.openStream(), output);
        return output.toString();
    }

    public static String streamToString(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(stream, output);
        return output.toString();
    }

    private static List<User> arrayToList(User[] array) {
        List<User> returner = new ArrayList<User>();
        Collections.addAll(returner, array);
        return returner;
    }

    private static User mapUser(String jsonstring) {
        User[] usermapping = new Gson().fromJson(jsonstring, User[].class);
        return usermapping[0];
    }

    private static List<User> mapUsersToList(String jsonstring) {
        User[] usermapping = new Gson().fromJson(jsonstring.toString(), User[].class);
        return arrayToList(usermapping);
    }

    private static void printUsers(List<User> users) {
        for (User user : users) {
            printUser(user);
        }
    }

    private static void printUser(User userToPrint) {
        System.out.println(userToPrint);
    }
}
