package edu.calpoly.csc.plantidentification;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.Scanner;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;

public class QuestionDownloadService extends Service {

    DBAdapter db;

    /** 
	 * A String containing the URL from which to download the list of all Questions. 
	 */
    public static final String GET_PLANTS_URL = "http://cslvm157.csc.calpoly.edu/fieldguideservice/plant/all.php";

    public static final String GET_PLANT_ANSWERS_URL = "http://cslvm157.csc.calpoly.edu/fieldguideservice/plantanswer/all.php";

    public static final String GET_QUESTIONS_URL = "http://cslvm157.csc.calpoly.edu/fieldguideservice/question/all.php";

    public static final String GET_ANSWERS_URL = "http://cslvm157.csc.calpoly.edu/fieldguideservice/answer/all.php";

    @Override
    public void onCreate() {
        db = new DBAdapter(this);
        db.open();
        Cursor c = db.getAllPlants();
        c.moveToNext();
        if (c.getCount() == 0) {
            getPlantsFromServer();
            getQuestionsFromServer();
            getAnswersFromServer();
            getPlantAnswersFromServer();
        }
        c.close();
        db.close();
    }

    private void getPlantAnswersFromServer() {
        String text = null;
        String[] answers;
        String[] answer;
        try {
            URL url = new URL(GET_PLANT_ANSWERS_URL);
            Scanner in = new Scanner(new InputStreamReader(url.openStream())).useDelimiter("\n");
            while (in.hasNext()) {
                text = in.next();
            }
        } catch (Exception e) {
            return;
        }
        answers = text.split(";");
        for (int i = 0; i < answers.length; i++) {
            answer = answers[i].split(",");
            db.insertPlantAnswer(answer[0], answer[1], answer[2], answer[3]);
        }
    }

    private void getAnswersFromServer() {
        String text = null;
        String[] answers;
        String[] answer;
        try {
            URL url = new URL(GET_ANSWERS_URL);
            Scanner in = new Scanner(new InputStreamReader(url.openStream())).useDelimiter("\n");
            while (in.hasNext()) {
                text = in.next();
            }
        } catch (Exception e) {
            return;
        }
        answers = text.split(";");
        for (int i = 0; i < answers.length; i++) {
            answer = answers[i].split("\\|");
            String icon = "";
            String description = "";
            if (answer.length > 3) icon = answer[3];
            if (answer.length > 4) description = answer[4];
            db.insertAnswer(answer[0], answer[1], answer[2], icon, description);
        }
    }

    private void getPlantsFromServer() {
        String text = null;
        String[] answers;
        String[] answer;
        try {
            URL url = new URL(GET_PLANTS_URL);
            Scanner in = new Scanner(new InputStreamReader(url.openStream())).useDelimiter("\n");
            while (in.hasNext()) {
                text = in.next();
            }
        } catch (Exception e) {
            return;
        }
        answers = text.split(";");
        for (int i = 0; i < answers.length; i++) {
            answer = answers[i].split("\\|");
            String category = "";
            String imageURL = "";
            String imageAttribution = "";
            String scientificName = "";
            if (answer.length > 2) category = answer[2];
            if (answer.length > 3) scientificName = answer[3];
            if (answer.length > 4) imageURL = answer[4];
            if (answer.length > 5) imageAttribution = answer[5];
            db.insertPlant(answer[0], answer[1], category, scientificName, imageURL, imageAttribution);
        }
    }

    /** Cleanup any and all resources here. **/
    @Override
    public void onDestroy() {
    }

    /** Ignore this method, you won't be using it. **/
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void getQuestionsFromServer() {
        String text = null;
        String[] questions;
        String[] question;
        try {
            URL url = new URL(GET_QUESTIONS_URL);
            Scanner in = new Scanner(new InputStreamReader(url.openStream())).useDelimiter("\n");
            while (in.hasNext()) {
                text = in.next();
            }
        } catch (Exception e) {
            return;
        }
        questions = text.split(";");
        for (int i = 0; i < questions.length; i++) {
            question = questions[i].split("\\|");
            db.insertQuestion(question[0], question[1], question[2], question[3]);
        }
    }
}
