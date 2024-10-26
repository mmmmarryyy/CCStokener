package org.droidtodoist.TodoistAPI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class TodoistAPI {

    private static String apiToken = "";

    private static LinkedList<TodoistProject> projects = null;

    /**
	 * @param email
	 *            User's email
	 * @param password
	 *            User's password
	 * @return apiToken on success, "ERROR" on failure
	 * @throws JSONException
	 * @throws IOException
	 */
    public static String login(String email, String password) throws JSONException, IOException {
        String jsonString = connect("login?email=" + email + "&password=" + password, true);
        if (jsonString.equals("\"LOGIN_ERROR\"")) return "ERROR";
        JSONObject json = (JSONObject) new JSONTokener(jsonString).nextValue();
        apiToken = json.getString("api_token");
        return apiToken;
    }

    /**
	 * @return number of projects
	 * @throws IOException
	 * @throws JSONException
	 */
    public static int getProjects() throws IOException, JSONException {
        String jsonString = connect("getProjects?token=" + apiToken, false);
        JSONArray json = (JSONArray) new JSONTokener(jsonString).nextValue();
        int length = json.length();
        if (length == 0) {
            projects.clear();
            return 0;
        }
        projects = new LinkedList<TodoistProject>();
        for (int i = 0; i < length; i++) {
            String newProject = json.getString(i).toString();
            projects.add(new TodoistProject(newProject));
        }
        return length;
    }

    /**
	 * @param project
	 *            the project to add
	 * @return the new project
	 * @throws IOException
	 * @throws JSONException
	 */
    public static TodoistProject addProject(TodoistProject project) throws IOException, JSONException {
        if (project.getName().equals("")) return null;
        String params = getParams(project);
        String jsonString = connect("addProject?token=" + apiToken + params, false);
        JSONObject json = (JSONObject) new JSONTokener(jsonString).nextValue();
        return new TodoistProject(json.toString());
    }

    public static TodoistProject updateProject(TodoistProject project) throws IOException, JSONException {
        String params = getParams(project);
        String jsonString = connect("updateProject?token=" + apiToken + "&project_id=" + project.getId() + params, false);
        JSONObject json = (JSONObject) new JSONTokener(jsonString).nextValue();
        String projectString = json.toString();
        if (projectString.equals("\"ERROR_PROJECT_NOT_FOUND\"")) return null; else return new TodoistProject(projectString);
    }

    /**
	 * @param id
	 *            the id of the project to delete
	 * @return true on success, false on failure
	 * @throws IOException
	 */
    public static boolean deleteProject(int id) throws IOException {
        String result = connect("deleteProject?token=" + apiToken + "&project_id=" + id, false);
        if (result.equals("\"ok\"")) return true; else return false;
    }

    /**
	 * @param apiURL
	 *            the Todoist API function to call with its parameters
	 * @param secure
	 *            if true, uses https over http
	 * @return a JSON response object in form of a string
	 * @throws IOException
	 */
    private static String connect(String apiURL, boolean secure) throws IOException {
        String baseUrl;
        if (secure) baseUrl = "https://todoist.com/API/"; else baseUrl = "http://todoist.com/API/";
        URL url = new URL(baseUrl + apiURL);
        URLConnection c = url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder toReturn = new StringBuilder("");
        String toAppend;
        while ((toAppend = in.readLine()) != null) toReturn.append(toAppend);
        return toReturn.toString();
    }

    private static String getParams(TodoistProject project) {
        StringBuilder params = new StringBuilder("&name=" + project.getName());
        params.append("&color=" + project.getColor());
        if (project.getIndent() > 0 && project.getIndent() <= 4) params.append("&indent=" + project.getIndent());
        if (project.getItemOrder() != -1) params.append("&order=" + project.getItemOrder());
        return params.toString();
    }
}
