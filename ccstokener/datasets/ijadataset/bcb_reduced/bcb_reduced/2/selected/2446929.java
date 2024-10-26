package br.com.petrobras.facade.directions;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.org.mozilla.javascript.internal.NativeArray;
import br.com.petrobras.model.AgendamentoVO;
import br.com.petrobras.model.EnderecoVO;
import br.com.petrobras.model.PessoaVO;
import br.com.petrobras.service.implementation.RotaException;
import br.com.petrobras.util.Constants;
import br.com.petrobras.util.Diretorio;
import br.com.petrobras.util.StringUtils;

public class DirectionsFacade {

    private static final Log logger = LogFactory.getLog(DirectionsFacade.class);

    private static DirectionsFacade instance;

    private ResourceBundle resourceBundle;

    public static DirectionsFacade getInstance() {
        if (instance == null) {
            instance = new DirectionsFacade();
        }
        return instance;
    }

    private DirectionsFacade() {
        super();
        this.resourceBundle = ResourceBundle.getBundle("google");
    }

    public List<AgendamentoVO> gerarRotaComGoogleDirections(List<AgendamentoVO> agendamento, PessoaVO pessoaOrigem) throws RotaException, ConnectException {
        logger.debug(">>> gerarRotaComGoogleDirections ");
        List<EnderecoVO> waypoints = new ArrayList<EnderecoVO>();
        for (AgendamentoVO a : agendamento) {
            waypoints.add(a.getProvedor().getEndereco());
        }
        List<Integer> resultadoAsIntegerArray = null;
        try {
            String jsonDirectionsFileAsString = getDirectionsFromGoogle(pessoaOrigem.getEndereco(), pessoaOrigem.getEndereco(), waypoints);
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("JavaScript");
            engine.eval(StringUtils.convertStreamToString(getClass().getResourceAsStream(Diretorio.SERVER_SIDE_JAVASCRIPT + "rota.js")));
            Invocable invocable = (Invocable) engine;
            NativeArray resultado;
            resultado = (NativeArray) invocable.invokeFunction("getRota", jsonDirectionsFileAsString);
            resultadoAsIntegerArray = new ArrayList<Integer>();
            for (int i = 0; i < resultado.getLength(); i++) {
                resultadoAsIntegerArray.add(((Double) resultado.get(i, resultado)).intValue());
            }
        } catch (ScriptException e) {
            logger.error(e);
            throw new RotaException("Erro de script : " + e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.error(e);
            throw new RotaException("M�todo n�o encontrado : " + e.getMessage());
        } catch (FileNotFoundException e) {
            logger.error(e);
            throw new RotaException("Arquivo n�o encontrado : " + e.getMessage());
        } catch (Exception e) {
            logger.error(e);
            throw new RotaException("Falha na obten��o da rota : " + e.getMessage());
        }
        List<AgendamentoVO> rota = new ArrayList<AgendamentoVO>();
        for (Integer i : resultadoAsIntegerArray) {
            rota.add(agendamento.get(i.intValue()));
        }
        logger.debug("<<< gerarRotaComGoogleDirections ");
        return rota;
    }

    private String retornaLatitudeLongitude(EnderecoVO endereco) {
        return endereco.getLatitude().toString() + "," + endereco.getLongitude().toString();
    }

    private String getDirectionsFromGoogle(EnderecoVO origin, EnderecoVO destination, List<EnderecoVO> waypoints) throws RotaException, ConnectException {
        String inputLine;
        StringBuffer urlsb = new StringBuffer("http://maps.google.com/maps/api/directions/json?");
        urlsb.append("origin=" + retornaLatitudeLongitude(origin));
        urlsb.append("&destination=" + retornaLatitudeLongitude(destination));
        if (waypoints.size() > 0) {
            urlsb.append("&waypoints=optimize:true");
            waypoints.iterator();
            for (EnderecoVO e : waypoints) {
                urlsb.append("|" + retornaLatitudeLongitude(e));
            }
        }
        urlsb.append("&sensor=false");
        logger.debug("URL directions, c�lculo da rota: " + urlsb.toString());
        HttpURLConnection httpConnection = null;
        String proxy = resourceBundle.getString(Constants.PROXY_HOST);
        String port = resourceBundle.getString(Constants.PROXY_PORT);
        try {
            String addressUTF8 = urlsb.toString();
            URL url = new URL(addressUTF8);
            Properties systemproperties = System.getProperties();
            if (proxy != null && !proxy.equals("")) {
                systemproperties.setProperty("http.proxyHost", proxy);
                systemproperties.setProperty("http.proxyPort", port);
            }
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.connect();
            inputLine = StringUtils.convertStreamToString(httpConnection.getInputStream());
            httpConnection.disconnect();
        } catch (UnsupportedEncodingException ue) {
            logger.error(ue);
            throw new RotaException("Encoding n�o suportado : " + ue.getMessage());
        } catch (MalformedURLException ma) {
            logger.error(ma);
            throw new RotaException("Erro na URL : " + ma.getMessage());
        } catch (IOException io) {
            logger.error(io);
            throw new RotaException("Erro de io : ", io);
        } catch (Exception ex) {
            throw new RotaException("N�o foi poss�vel gerar a rota  : " + ex.getMessage());
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
        return inputLine;
    }
}
