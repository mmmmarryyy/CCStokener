package yoichiro.eclipse.plugins.translationview.internal.core.translator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import yoichiro.eclipse.plugins.translationview.internal.core.TranslationViewPlugin;
import yoichiro.eclipse.plugins.translationview.internal.ui.OtherParameterInfo;

/**
 * Excite�T�C�g�𗘗p�����|�󏈗����s�������N���X�ł��B
 * @author Yoichiro Tanaka
 */
public class TranslatorExciteImpl implements ITranslator {

    /** �I���L�[���[�h���Ȃ������Ƃ��̃G���[�R�[�h */
    private static final int ERROR_END_KEYWORD_NOT_FOUND = 0;

    /** �J�n�L�[���[�h���Ȃ������Ƃ��̃G���[�R�[�h */
    private static final int ERROR_START_KEYWORD_NOT_FOUND = 1;

    /** �G���R�[�h��������Ȃ��������̃G���[�R�[�h */
    private static final int ERROR_UNSUPPORTED_ENCODING = 2;

    /** ��o�̓G���[�̃G���[�R�[�h */
    private static final int ERROR_IO = 4;

    /** �ݒ� */
    private TranslatorConfiguration config;

    /**
     * ���̃I�u�W�F�N�g�����������Ƃ��ɌĂяo����܂��B
     * @param config �ݒ�
     */
    public TranslatorExciteImpl(TranslatorConfiguration config) {
        super();
        this.config = config;
    }

    /**
     * �w�肳�ꂽ�������|�󂵂܂��B
     * @param before �|��O�̕�����
     * @param translateType �|��̕�@({@link ITranslatot#ENGLISH_TO_JAPANESE}���邢��{@link ITranslatot#JAPANESE_TO_ENGLISH})
     * @return �|���̕�����
     * @exception CoreException �|�󏈗����ɉ��炩�̃G���[�����������Ƃ�
     */
    public String translate(String before, int translateType) throws CoreException {
        if (before == null) throw new IllegalArgumentException("before is null.");
        if ((translateType != ENGLISH_TO_JAPANESE) && (translateType != JAPANESE_TO_ENGLISH)) {
            throw new IllegalArgumentException("Invalid translateType. value=" + translateType);
        }
        try {
            URL url = new URL(config.getTranslatorSiteUrl());
            URLConnection connection = url.openConnection();
            sendTranslateRequest(before, translateType, connection);
            String afterContents = receiveTranslatedResponse(connection);
            String afterStartKey = config.getTranslationResultStart();
            String afterEndKey = config.getTranslationResultEnd();
            int startLength = afterStartKey.length();
            int startPos = afterContents.indexOf(afterStartKey);
            if (startPos != -1) {
                int endPos = afterContents.indexOf(afterEndKey, startPos);
                if (endPos != -1) {
                    String after = afterContents.substring(startPos + startLength, endPos);
                    after = replaceEntities(after);
                    return after;
                } else {
                    throwCoreException(ERROR_END_KEYWORD_NOT_FOUND, "End keyword not found.", null);
                }
            } else {
                throwCoreException(ERROR_START_KEYWORD_NOT_FOUND, "Start keyword not found.", null);
            }
        } catch (IOException e) {
            throwCoreException(ERROR_IO, e.getMessage(), e);
        }
        throw new IllegalStateException("CoreException not occurd.");
    }

    /**
     * �|�󌋉ʂ���M���܂��B
     * @param connection �ڑ��I�u�W�F�N�g
     * @return ��M���ʂ̕�����
     * @throws CoreException ���炩�̗�O�����������Ƃ�
     */
    private String receiveTranslatedResponse(URLConnection connection) throws CoreException {
        BufferedReader bufferedReader = null;
        try {
            InputStreamReader isr = new InputStreamReader(connection.getInputStream(), config.getAfterEndoding());
            bufferedReader = new BufferedReader(isr);
            StringBuffer sb = new StringBuffer();
            String buf;
            while ((buf = bufferedReader.readLine()) != null) {
                sb.append(buf + "\n");
            }
            String afterContents = sb.toString();
            return afterContents;
        } catch (IOException e) {
            throwCoreException(ERROR_IO, e.getMessage(), e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        throw new IllegalStateException("CoreException not occurd.");
    }

    /**
     * �|��v���𑗐M���܂��B
     * @param before �|��Ώۂ̕�����
     * @param translateType �|��̕�@({@link ITranslatot#ENGLISH_TO_JAPANESE}���邢��{@link ITranslatot#JAPANESE_TO_ENGLISH})
     * @param connection �ڑ��I�u�W�F�N�g
     * @throws CoreException ���炩�̗�O�����������Ƃ�
     */
    private void sendTranslateRequest(String before, int translateType, URLConnection connection) throws CoreException {
        PrintStream printStream = null;
        try {
            String encodedBefore = URLEncoder.encode(before, config.getBeforeEndoding());
            String translateTypeStr = (translateType == ENGLISH_TO_JAPANESE) ? config.getEnglishToJapanese() : config.getJapaneseToEnglish();
            String parameter = config.getBeforeSource() + "=" + encodedBefore + "&" + config.getTranslationType() + "=" + translateTypeStr;
            Set otherParameters = config.getOtherParameters();
            for (Iterator iter = otherParameters.iterator(); iter.hasNext(); ) {
                OtherParameterInfo info = (OtherParameterInfo) iter.next();
                parameter += "&" + info.getName() + "=" + URLEncoder.encode(info.getValue(), config.getBeforeEndoding());
            }
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept-Language", "ja");
            printStream = new PrintStream(connection.getOutputStream());
            printStream.print(parameter);
            printStream.flush();
        } catch (UnsupportedEncodingException e) {
            throwCoreException(ERROR_UNSUPPORTED_ENCODING, e.getMessage(), e);
        } catch (IOException e) {
            throwCoreException(ERROR_IO, e.getMessage(), e);
        } finally {
            if (printStream != null) {
                printStream.close();
            }
        }
    }

    /**
     * �w�肳�ꂽ��������CoreException��O���X���[���܂��B
     * @param code �G���[�R�[�h
     * @param message �G���[���b�Z�[�W
     * @param exception �G���[�������ɐ�������O
     * @throws CoreException �������ꂽ��O
     */
    private void throwCoreException(int code, String message, Exception exception) throws CoreException {
        String pluginId = TranslationViewPlugin.getInstance().getUniqueIdentifier();
        IStatus status = new Status(IStatus.ERROR, pluginId, code, message, exception);
        throw new CoreException(status);
    }

    /**
     * �w�肳�ꂽ���������HTML�G���e�B�e�B�ɂ��ĕϊ��������s���C���̌��ʂ�Ԃ��܂��B
     * @param source �ϊ��O�̕�����
     * @return �ϊ���̕�����
     */
    private String replaceEntities(String source) {
        if ((source == null) || (source.length() == 0)) {
            return source;
        } else {
            String result;
            result = source.replaceAll("&lt;", "<");
            result = result.replaceAll("&gt;", ">");
            result = result.replaceAll("&amp;", "&");
            result = result.replaceAll("&quot;", "\"");
            result = result.replaceAll("&nbsp;", " ");
            return result;
        }
    }
}
