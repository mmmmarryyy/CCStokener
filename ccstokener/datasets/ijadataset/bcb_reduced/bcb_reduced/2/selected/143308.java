package com.atosorigin.nl.jspring2008.buzzword.quiz.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import org.mule.providers.sms.SmsMessage;
import org.mule.providers.sms.SmsTextMessage;
import com.atosorigin.nl.jspring2008.buzzword.quiz.domain.Answer;
import com.atosorigin.nl.jspring2008.buzzword.quiz.domain.ApproachQuestion;
import com.atosorigin.nl.jspring2008.buzzword.quiz.domain.Choice;
import com.atosorigin.nl.jspring2008.buzzword.quiz.domain.MultipleChoiceQuestion;
import com.atosorigin.nl.jspring2008.buzzword.quiz.domain.Participant;
import com.atosorigin.nl.jspring2008.buzzword.quiz.domain.Quiz;
import com.atosorigin.nl.jspring2008.buzzword.quiz.domain.QuizRound;
import com.thoughtworks.xstream.XStream;

/**
 * @author Jeroen Benckhuijsen (jeroen.benckhuijsen@gmail.com)
 * 
 */
public class XmlSerializer {

    public static final String QUIZ_IN = "quiz.in";

    public static final String QUIZ_OUT = "quiz.out";

    public static final String SMS_IN = "sms.in";

    public static final String SMS_OUT = "sms.out";

    private XStream xstream = new XStream();

    /**
	 * 
	 */
    public XmlSerializer() {
        super();
        xstream.alias("quiz", Quiz.class);
        xstream.alias("quizRound", QuizRound.class);
        xstream.alias("multipleChoiceQuestion", MultipleChoiceQuestion.class);
        xstream.alias("choice", Choice.class);
        xstream.alias("approachQuestion", ApproachQuestion.class);
        xstream.alias("smsTextMessage", SmsTextMessage.class);
        xstream.alias("smsMessage", SmsMessage.class);
        xstream.alias("participant", Participant.class);
        xstream.alias("answer", Answer.class);
    }

    /**
	 * @return
	 * @throws IOException
	 */
    public Quiz loadQuiz() throws IOException {
        return (Quiz) loadXml(QUIZ_IN);
    }

    /**
	 * @param quiz
	 * @throws IOException
	 */
    public void storeQuiz(Quiz quiz) throws IOException {
        storeXml(QUIZ_OUT, quiz);
    }

    /**
	 * @return
	 * @throws IOException
	 */
    @SuppressWarnings("unchecked")
    public Collection<SmsTextMessage> loadSmsMessages() throws IOException {
        return (Collection<SmsTextMessage>) loadXml(SMS_IN);
    }

    /**
	 * @param messages
	 * @throws IOException
	 */
    public void storeSmsMessages(Collection<SmsTextMessage> messages) throws IOException {
        storeXml(SMS_OUT, messages);
    }

    /**
	 * @param property
	 * @return
	 * @throws FileNotFoundException
	 */
    private Object loadXml(String property) throws IOException {
        URL url = new URL(System.getProperty(property));
        InputStream is = url.openConnection().getInputStream();
        return xstream.fromXML(is);
    }

    /**
	 * @param property
	 * @param object
	 * @throws FileNotFoundException
	 */
    private void storeXml(String property, Object object) throws IOException {
        String filepath = System.getProperty(property);
        OutputStream os = new FileOutputStream(new File(filepath));
        xstream.toXML(object, os);
    }
}
