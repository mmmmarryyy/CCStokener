package edu.calpoly.csc.plantidentification;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Scanner;
import java.util.UUID;

public class UserManager {

    public String secret;

    public static enum Result {

        BAD_FORMAT, SERVER_CONNECTION_FAILED, SERVER_REJECT, OK
    }

    public Result registerUser(String username) {
        if (!username.matches("[a-zA-Z0-9]+")) {
            return Result.BAD_FORMAT;
        }
        return registerWithService(username);
    }

    private Result registerWithService(String username) {
        secret = UUID.randomUUID().toString();
        String text = "";
        try {
            URL url = new URL("http://cslvm157.csc.calpoly.edu/fieldguideservice/user/register.php?username=" + username + "&secret=" + secret);
            Scanner in = new Scanner(new InputStreamReader(url.openStream())).useDelimiter("\n");
            while (in.hasNext()) {
                text = in.next();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Result.SERVER_CONNECTION_FAILED;
        }
        if (text.equals("Okay")) return Result.OK; else return Result.SERVER_REJECT;
    }
}
