package com.homeautomate.manager;

import java.net.URLEncoder;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import com.homeautomate.constant.I18NEnum;
import com.homeautomate.constant.ParameterEnum;

public class VocalManager implements IVocalManager {

    IParameterManager parameterManager;

    IJdbManager jdbManager;

    II18NManager i18nManager;

    /**
	 * Methode permettant de faire parler le système
	 */
    public void talk(String tts, Object... params) {
        String locale = parameterManager.getAParam(ParameterEnum.locale);
        String textToSpeech = i18nManager.getTraduction(tts);
        if (textToSpeech == null) textToSpeech = tts;
        String googleService = parameterManager.getAParam(ParameterEnum.googleSpeech);
        if (googleService == null) {
            googleService = ParameterEnum.googleSpeech.getDefaut();
        }
        try {
            HttpClient httpclient = new DefaultHttpClient();
            String encodeUrl = URLEncoder.encode(String.format(textToSpeech, params), "UTF-8");
            HttpGet httpget = new HttpGet(String.format(googleService, locale.substring(0, 2)) + encodeUrl);
            httpget.addHeader("User-Agent", "Mozilla/5.0");
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity googleVoiceResponse = response.getEntity();
            Player player = new Player(googleVoiceResponse.getContent());
            player.play();
            player.close();
        } catch (JavaLayerException e) {
            if (jdbManager != null) jdbManager.error(this.getClass(), I18NEnum.exceptionTechnique.name(), e.getCause(), e.getMessage());
        } catch (Exception e) {
            if (jdbManager != null) jdbManager.error(this.getClass(), I18NEnum.exceptionTechnique.name(), e.getCause(), e.getMessage());
        }
    }

    public IParameterManager getParameterManager() {
        return parameterManager;
    }

    public void setParameterManager(IParameterManager parameterManager) {
        this.parameterManager = parameterManager;
    }

    public II18NManager getI18nManager() {
        return i18nManager;
    }

    public void setI18nManager(II18NManager i18nManager) {
        this.i18nManager = i18nManager;
    }

    public IJdbManager getJdbManager() {
        return jdbManager;
    }

    public void setJdbManager(IJdbManager jdbManager) {
        this.jdbManager = jdbManager;
    }
}
