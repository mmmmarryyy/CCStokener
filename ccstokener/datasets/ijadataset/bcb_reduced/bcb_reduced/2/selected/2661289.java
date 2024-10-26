package aquest.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Random;
import java.util.Vector;
import aquest.model.reader.QuestionLoader;

/**
 * @author aurelio
 *
 */
public class QuestionBundle {

    public Vector<Question> questions;

    public String author;

    public String shortName;

    public String description;

    public URL url;

    public int total = 0;

    public int correct = 0;

    public int wrong = 0;

    public int general_wrong = 0;

    private static Random random = new Random();

    public QuestionBundle(URL questionsURL) throws IOException {
        this.url = questionsURL;
        questions = new Vector<Question>();
        questions = reload();
        for (Question q : questions) {
            q.setParent(this);
        }
    }

    public Vector<Question> reload() throws IOException {
        Vector<Question> questions = new Vector<Question>();
        InputStream is = url.openStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        shortName = br.readLine();
        if (shortName != null && shortName.equals("SHORTNAME")) {
            shortName = br.readLine();
            author = br.readLine();
            if (author != null && author.equals("AUTHOR")) {
                author = br.readLine();
                description = br.readLine();
                if (description != null && description.equals("DESCRIPTION")) {
                    description = br.readLine();
                    try {
                        questions = QuestionLoader.getQuestions(br);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                        throw ioe;
                    } finally {
                        br.close();
                        is.close();
                    }
                } else {
                    throw new IllegalArgumentException();
                }
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
        return questions;
    }

    public Question getRandomQuestion() {
        int sum = 0;
        for (int i = 0; i < questions.size(); ++i) {
            sum += questions.elementAt(i).getFactor();
        }
        System.out.println("bundle: " + this.shortName);
        System.out.println("sum: " + sum);
        System.out.println("questions: " + questions.size());
        int rnd = random.nextInt(sum);
        int upToSum = 0;
        for (int i = 0; i < questions.size(); ++i) {
            Question q = questions.elementAt(i);
            if (rnd >= upToSum && rnd < upToSum + q.getFactor()) {
                return q;
            }
            upToSum += q.getFactor();
        }
        return questions.lastElement();
    }

    public double getFactor() {
        if (total > 0) {
            return Math.pow(3, (general_wrong * 1.0) / total) * (1.0 / total);
        } else {
            return Double.POSITIVE_INFINITY;
        }
    }
}
