package proyecto.twitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;
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
 * Clase que recoge toda la funcionalidad de la red de Twitter
 * 
 * @author moises
 * 
 */
public class TwitterRed implements RedInterfaz {

    private CommonsHttpOAuthConsumer consumer;

    private SharedPreferences datosTwitter;

    private Context apli;

    private ShowMessageHandler s;

    /**
	 * Constructor de la clase de Twitter
	 * 
	 * @param papli
	 * @param phandler
	 */
    public TwitterRed(Context papli, ShowMessageHandler phandler) {
        this.apli = papli;
        this.s = phandler;
        datosTwitter = apli.getSharedPreferences("datosTwitter", 0);
        consumer = new CommonsHttpOAuthConsumer(Constantes.CONSUMER_KEY_TWI, Constantes.CONSUMER_SECRET_TWI);
        consumer.setTokenWithSecret(getOpciones("twikey"), getOpciones("twisecret"));
    }

    /**
	 * Envio de mensaje a la red social Twitter
	 * 
	 * @param tweet
	 */
    public void enviarTweet(String tweet) {
        try {
            HttpPost post = new HttpPost("http://api.twitter.com/1/statuses/update.xml");
            final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("status", tweet));
            post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            post.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
            consumer.sign(post);
            HttpClient client = new DefaultHttpClient();
            final HttpResponse response = client.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            response.getEntity().consumeContent();
            if (statusCode != 200) {
                this.enviarMensaje("Error al enviar el Tweet a Twitter");
                return;
            }
            this.enviarMensaje("Enviado tweet a Twitter");
        } catch (UnsupportedEncodingException e) {
            this.enviarMensaje("Error al enviar el Tweet a Twitter");
        } catch (IOException e) {
            this.enviarMensaje("Error al enviar el Tweet a Twitter");
        } catch (OAuthMessageSignerException e) {
            this.enviarMensaje("Error al enviar el Tweet a Twitter");
        } catch (OAuthExpectationFailedException e) {
            this.enviarMensaje("Error al enviar el Tweet a Twitter");
        } catch (OAuthCommunicationException e) {
            this.enviarMensaje("Error al enviar el Tweet a Twitter");
        }
    }

    /**
	 * Retorna el timeline del usuario de la red social Twitter
	 * 
	 * @return ArrayList<Tweet>
	 */
    public ArrayList<Tweet> getTimeLine() {
        try {
            HttpGet get = new HttpGet("http://api.twitter.com/1/statuses/home_timeline.xml");
            consumer.sign(get);
            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(get);
            if (response != null) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    this.enviarMensaje("Problema al coger Timeline Twitter");
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
                try {
                    StringReader XMLout = new StringReader(sBuf.toString());
                    SAXParser sp = spf.newSAXParser();
                    XMLReader xr = sp.getXMLReader();
                    xmlParserTwitter gwh = new xmlParserTwitter();
                    xr.setContentHandler(gwh);
                    xr.parse(new InputSource(XMLout));
                    return gwh.getParsedData();
                } catch (ParserConfigurationException e) {
                    this.enviarMensaje("Problema al coger Timeline Twitter");
                } catch (SAXException e) {
                    this.enviarMensaje("Problema al coger Timeline Twitter");
                } catch (IOException e) {
                    this.enviarMensaje("Problema al coger Timeline Twitter");
                }
            }
        } catch (UnsupportedEncodingException e) {
            this.enviarMensaje("Problema al coger Timeline Twitter");
        } catch (IOException e) {
            this.enviarMensaje("Problema al coger Timeline Twitter");
        } catch (OAuthMessageSignerException e) {
            this.enviarMensaje("Problema al coger Timeline Twitter");
        } catch (OAuthExpectationFailedException e) {
            this.enviarMensaje("Problema al coger Timeline Twitter");
        } catch (OAuthCommunicationException e) {
            this.enviarMensaje("Problema al coger Timeline Twitter");
        }
        return null;
    }

    /**
	 * Devuelve el nombre del usuario auntenticado con el token.
	 */
    public String getUser() {
        try {
            HttpGet get = new HttpGet("http://api.twitter.com/1/account/verify_credentials.xml");
            consumer.sign(get);
            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(get);
            if (response != null) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    this.enviarMensaje("Problema al coger usuario Twitter");
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
                String user_name = salida.split("</screen_name>")[0].split("<screen_name>")[1];
                return user_name;
            }
        } catch (UnsupportedEncodingException e) {
            this.enviarMensaje("Problema al coger Timeline Twitter");
        } catch (IOException e) {
            this.enviarMensaje("Problema al coger Timeline Twitter");
        } catch (OAuthMessageSignerException e) {
            this.enviarMensaje("Problema al coger Timeline Twitter");
        } catch (OAuthExpectationFailedException e) {
            this.enviarMensaje("Problema al coger Timeline Twitter");
        } catch (OAuthCommunicationException e) {
            this.enviarMensaje("Problema al coger Timeline Twitter");
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
        SharedPreferences.Editor editor = datosTwitter.edit();
        editor.putString(clave, valor);
        editor.commit();
    }

    /**
	 * Devuelve las opciones almacenadas en la tabla Shared Preferences
	 * 
	 * @param clave
	 * @return
	 */
    public String getOpciones(String clave) {
        String d = datosTwitter.getString(clave, "not");
        return d;
    }

    /**************************************************************************
	 * Método privados
	 **************************************************************************/
    private void enviarMensaje(String mensaje) {
        Message msg = new Message();
        msg.obj = mensaje;
        this.s.sendMessage(msg);
    }
}
