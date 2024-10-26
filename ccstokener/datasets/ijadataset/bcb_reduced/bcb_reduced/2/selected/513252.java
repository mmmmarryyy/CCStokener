package com.bphx.android.network;

import android.content.Context;
import com.bphx.android.INetworkManager;
import com.bphx.android.IStorage;
import com.bphx.android.exception.BaseException;
import com.bphx.android.words.Word;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import static com.bphx.android.exception.ExceptionFactory.getException;
import static com.bphx.android.network.NetworkConstants.*;
import static com.bphx.android.utils.Utils.closeSafely;
import static java.net.URLEncoder.encode;

/**
 * @author Vyacheslav Kovalyov
 * @version 1.0
 */
class NetworkManager implements INetworkManager {

    public NetworkManager(String userName, String password, Context context) {
        this.userName = userName;
        this.password = password;
        this.context = context;
    }

    @Override
    public void mergeWithLocal(IStorage local, List<Word> newWords) throws BaseException {
        List<Word> localWords = local.loadWords();
        for (Word w : newWords) {
            Word existingWord = getContainedWord(localWords, w);
            if (existingWord == null) {
                try {
                    local.addWord(w);
                } catch (BaseException ignore) {
                }
            } else {
                local.replaceWord(w, existingWord);
            }
        }
    }

    private Word getContainedWord(List<Word> words, Word word) {
        for (Word w : words) {
            if (word.getOriginalWord().equals(w.getOriginalWord())) {
                return w;
            }
        }
        return null;
    }

    @Override
    public ArrayList<Word> loadWords() throws BaseException {
        return loadAndParse();
    }

    private ArrayList<Word> loadAndParse() throws BaseException {
        Document doc = getDocument(getConnection());
        NodeList nodes = doc.getElementsByTagName(TAG_WORDS);
        ArrayList<Word> wordsList = new ArrayList<Word>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node root = nodes.item(i);
            NodeList words = root.getChildNodes();
            for (int word_num = 0; word_num < words.getLength(); word_num++) {
                Word w = createWord(words.item(word_num).getChildNodes());
                if (w != null) {
                    wordsList.add(w);
                }
            }
        }
        return wordsList;
    }

    private Word createWord(NodeList attributes) {
        Word word = new Word();
        try {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                if (attribute.getNodeName().equals(TAG_ORIGINAL_WORD)) {
                    word.setOriginalWord(attribute.getFirstChild().getNodeValue());
                } else if (attribute.getNodeName().equals(TAG_TRANS)) {
                    word.addVariant(attribute.getFirstChild().getNodeValue());
                } else if (attribute.getNodeName().equals(TAG_INVALID_TRANS)) {
                    word.addInvalidVariant(attribute.getFirstChild().getNodeValue());
                }
            }
        } catch (BaseException e) {
            return null;
        }
        return word;
    }

    private Document getDocument(InputStream stream) throws BaseException {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(stream);
        } catch (Exception e) {
            throw getException(e, context);
        } finally {
            closeSafely(stream);
        }
        return document;
    }

    private InputStream getConnection() throws BaseException {
        OutputStreamWriter wr = null;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(getBaseString());
            sb.append(AND);
            sb.append(encode(ACTION, ENCODING));
            sb.append(EQUAL);
            sb.append(encode(ACTION_GET_ALL, ENCODING));
            URL url = new URL(SERVER_URL);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(sb.toString());
            wr.flush();
            return conn.getInputStream();
        } catch (Exception e) {
            throw getException(e, context);
        } finally {
            closeSafely(wr);
        }
    }

    private String getBaseString() throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append(encode(UN, ENCODING));
        sb.append(EQUAL);
        sb.append(encode(userName, ENCODING));
        sb.append(AND);
        sb.append(encode(PWD, ENCODING));
        sb.append(EQUAL);
        sb.append(encode(password, ENCODING));
        return sb.toString();
    }

    private String userName;

    private String password;

    private Context context;
}
