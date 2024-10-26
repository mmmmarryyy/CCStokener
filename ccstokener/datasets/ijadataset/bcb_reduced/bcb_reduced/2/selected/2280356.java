package proyecto.linkedin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import proyecto.twemoi.Constantes;
import proyecto.twemoi.Tweet;
import proyecto.twemoi.RedInterfaz;
import proyecto.twemoi.twimoipro.ShowMessageHandler;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Message;

/**
 * Clase que recoge toda la funcionalidad de la red de Linkedin
 * 
 * @author moises
 * 
 */
public class LinkedinRed implements RedInterfaz {

    private CommonsHttpOAuthConsumer consumer;

    private SharedPreferences datosLinkedin;

    private Context apli;

    private ShowMessageHandler s;

    /**
	 * Constructor de la clase de Linkedin
	 * 
	 * @param papli
	 * @param phandler
	 */
    public LinkedinRed(Context papli, ShowMessageHandler phandler) {
        this.apli = papli;
        this.s = phandler;
        datosLinkedin = apli.getSharedPreferences("datosLinkedin", 0);
        consumer = new CommonsHttpOAuthConsumer(Constantes.CONSUMER_KEY_LIN, Constantes.CONSUMER_SECRET_LIN);
        consumer.setTokenWithSecret(getOpciones("linkedkey"), getOpciones("linkedsecret"));
    }

    /**
	 * Envio de mensaje a la red social Linkedin
	 * 
	 * @param tweet
	 */
    public void enviarTweet(String tweet) {
        HttpPut get = new HttpPut("http://api.linkedin.com/v1/people/~/current-status");
        StringEntity entidad;
        try {
            entidad = new StringEntity("<?xml version=\"1.0\" encoding=\"UTF-8\"?><current-status>" + tweet + "</current-status>");
            get.setEntity(entidad);
        } catch (UnsupportedEncodingException e2) {
            this.enviarMensaje("Error: No ha sido posible enviar el mensaje a la red de Linkedin");
            return;
        }
        get.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
        try {
            consumer.sign(get);
        } catch (OAuthMessageSignerException e) {
            this.enviarMensaje("Error: No ha sido posible enviar el mensaje a la red de Linkedin");
            return;
        } catch (OAuthExpectationFailedException e) {
            this.enviarMensaje("Error: No ha sido posible enviar el mensaje a la red de Linkedin");
            return;
        } catch (OAuthCommunicationException e) {
            this.enviarMensaje("Error: No ha sido posible enviar el mensaje a la red de Linkedin");
            return;
        }
        HttpClient client = new DefaultHttpClient();
        HttpResponse response = null;
        try {
            response = client.execute(get);
        } catch (ClientProtocolException e) {
            this.enviarMensaje("Error: No ha sido posible enviar el mensaje a la red de Linkedin");
            return;
        } catch (IOException e) {
            this.enviarMensaje("Error: No ha sido posible enviar el mensaje a la red de Linkedin");
            return;
        }
        int statusCode = response.getStatusLine().getStatusCode();
        if (!(statusCode >= 200 && statusCode < 300)) {
            this.enviarMensaje("Error: No ha sido posible enviar el mensaje a la red de Linkedin");
            return;
        }
        this.enviarMensaje("Enviado tweet a Linkedin");
    }

    /**
	 * Retorna el timeline del usuario de la red social Facebook
	 * 
	 * @return ArrayList<Tweet>
	 */
    public ArrayList<Tweet> getTimeLine() {
        try {
            HttpGet get = new HttpGet("http://api.linkedin.com/v1/people/~/network/updates?scope=self");
            consumer.sign(get);
            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(get);
            if (response != null) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    this.enviarMensaje("Error: No ha sido posible recoger el timeline de Linkedin");
                    return null;
                }
                StringBuffer sBuf = new StringBuffer();
                String linea;
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                while ((linea = reader.readLine()) != null) {
                    sBuf.append(linea);
                }
                reader.close();
                response.getEntity().consumeContent();
                get.abort();
                SAXParserFactory spf = SAXParserFactory.newInstance();
                StringReader XMLout = new StringReader(sBuf.toString());
                SAXParser sp = spf.newSAXParser();
                XMLReader xr = sp.getXMLReader();
                xmlParserLinkedin gwh = new xmlParserLinkedin();
                xr.setContentHandler(gwh);
                xr.parse(new InputSource(XMLout));
                return gwh.getParsedData();
            }
        } catch (UnsupportedEncodingException e) {
            this.enviarMensaje("Error: No ha sido posible recoger el timeline de Linkedin");
        } catch (IOException e) {
            this.enviarMensaje("Error: No ha sido posible recoger el timeline de Linkedin");
        } catch (OAuthMessageSignerException e) {
            this.enviarMensaje("Error: No ha sido posible recoger el timeline de Linkedin");
        } catch (OAuthExpectationFailedException e) {
            this.enviarMensaje("Error: No ha sido posible recoger el timeline de Linkedin");
        } catch (OAuthCommunicationException e) {
            this.enviarMensaje("Error: No ha sido posible recoger el timeline de Linkedin");
        } catch (ParserConfigurationException e) {
            this.enviarMensaje("Error: No ha sido posible recoger el timeline de Linkedin");
        } catch (SAXException e) {
            this.enviarMensaje("Error: No ha sido posible recoger el timeline de Linkedin");
        }
        return null;
    }

    /**
	 * Devuelve el nombre del usuario auntenticado con el token.
	 */
    public String getUser() {
        try {
            HttpGet get = new HttpGet("http://api.linkedin.com/v1/people/~");
            consumer.sign(get);
            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(get);
            if (response != null) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    this.enviarMensaje("Error: Usuario no autenticado en la red de Linkedin");
                }
                StringBuffer sBuf = new StringBuffer();
                String linea;
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                while ((linea = reader.readLine()) != null) {
                    sBuf.append(linea);
                }
                reader.close();
                response.getEntity().consumeContent();
                get.abort();
                String salida = sBuf.toString();
                String user_firstname = salida.split("</first-name>")[0].split("<first-name>")[1];
                String user_lastname = salida.split("</last-name>")[0].split("<last-name>")[1];
                return user_firstname + " " + user_lastname;
            }
        } catch (UnsupportedEncodingException e) {
            this.enviarMensaje("Error: Usuario no autenticado en la red de Linkedin");
        } catch (IOException e) {
            this.enviarMensaje("Error: Usuario no autenticado en la red de Linkedin");
        } catch (OAuthMessageSignerException e) {
            this.enviarMensaje("Error: Usuario no autenticado en la red de Linkedin");
        } catch (OAuthExpectationFailedException e) {
            this.enviarMensaje("Error: Usuario no autenticado en la red de Linkedin");
        } catch (OAuthCommunicationException e) {
            this.enviarMensaje("Error: Usuario no autenticado en la red de Linkedin");
        }
        return null;
    }

    /**
	 * Establece registros en la tabla SharedPreferences
	 * 
	 * @param clave
	 * @param valor
	 */
    public void setOpciones(String clave, String valor) {
        SharedPreferences.Editor editor = datosLinkedin.edit();
        editor.putString(clave, valor);
        editor.commit();
    }

    /**
	 * Devuelve un regsitro almacenado en la tabla Shared Preferences
	 * 
	 * @param clave
	 * @return
	 */
    public String getOpciones(String clave) {
        String d = datosLinkedin.getString(clave, "not");
        return d;
    }

    /**
	 * Envio de mensaje al handler que muestra por pantalla información
	 * 
	 * @param mensaje
	 */
    private void enviarMensaje(String mensaje) {
        Message msg = new Message();
        msg.obj = mensaje;
        this.s.sendMessage(msg);
    }
}
