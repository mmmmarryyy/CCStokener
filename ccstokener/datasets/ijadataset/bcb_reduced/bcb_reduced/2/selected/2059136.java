package br.com.petrobras.facade;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import br.com.petrobras.model.AgendamentoVO;
import br.com.petrobras.model.PessoaVO;
import br.com.petrobras.service.implementation.RotaException;
import br.com.petrobras.util.Constants;

public class GEOFacede {

    private static final Log logger = LogFactory.getLog(GEOFacede.class);

    private static final String STATUS_OK = "200";

    private static GEOFacede instance;

    private ResourceBundle resourceBundle;

    private XPath xpath;

    private GEOFacede() {
        super();
        this.resourceBundle = ResourceBundle.getBundle("google");
        this.xpath = XPathFactory.newInstance().newXPath();
    }

    private String getTagValue(Document document, String tagName) {
        try {
            String expr = String.format("//%s/text()", tagName);
            Node node = (Node) xpath.evaluate(expr, document, XPathConstants.NODE);
            if ((node != null) && (node.getNodeType() == Node.TEXT_NODE)) {
                return node.getNodeValue();
            } else {
                return null;
            }
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    private double getDistancia(PessoaVO pessoa1, PessoaVO pessoa2) {
        logger.debug(">>> getDistancia ");
        double latpont1 = pessoa1.getEndereco().getLatitude() * Math.PI / 180;
        double lngpont1 = pessoa1.getEndereco().getLongitude() * Math.PI / 180;
        double latpont2 = pessoa2.getEndereco().getLatitude() * Math.PI / 180;
        double lngpont2 = pessoa2.getEndereco().getLongitude() * Math.PI / 180;
        double difLatitude = latpont2 - latpont1;
        double difLongitude = lngpont2 - lngpont1;
        double a = Math.sin(difLatitude / 2) * Math.sin(difLatitude / 2) + Math.cos(latpont1) * Math.cos(latpont2) * Math.sin(difLongitude / 2) * Math.sin(difLongitude / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        logger.debug("<<< getDistancia ");
        final double RAIO_TERRA = 6371.0;
        return Math.round(RAIO_TERRA * c * 1000);
    }

    private boolean verificaStatusRequisicao(Document document) {
        String code = getTagValue(document, "code");
        return (code != null) && code.equals(STATUS_OK);
    }

    public static GEOFacede getInstance() {
        if (instance == null) {
            instance = new GEOFacede();
        }
        return instance;
    }

    public String getLogradouro(Document document) {
        String logradouro = getTagValue(document, "ThoroughfareName");
        if (logradouro != null) {
            int ini = logradouro.indexOf(".") + 2;
            int fim = logradouro.indexOf(",");
            if (ini <= logradouro.length() + 1) {
                return (ini <= fim) ? logradouro.substring(ini, fim) : logradouro.substring(ini);
            } else return null;
        } else {
            return null;
        }
    }

    public String getNumero(Document document) {
        String numero = getTagValue(document, "ThoroughfareName");
        if (numero != null) {
            int ini = numero.indexOf(",") + 2;
            return ((ini != 1) && (ini <= numero.length() + 1)) ? numero.substring(ini) : null;
        } else {
            return null;
        }
    }

    public String getBairro(Document document) {
        return getTagValue(document, "DependentLocalityName");
    }

    public String getCidade(Document document) {
        return getTagValue(document, "LocalityName");
    }

    public String getUf(Document document) {
        return getTagValue(document, "AdministrativeAreaName");
    }

    public Integer getCep(Document document) {
        String cep = getTagValue(document, "PostalCodeNumber");
        if (cep != null) {
            return Integer.valueOf(cep.replaceAll("-", ""));
        } else {
            return null;
        }
    }

    public Double getCoordenada(Document document, int indice) {
        String coord = getTagValue(document, "coordinates");
        return (coord != null) ? Double.parseDouble(coord.split(",")[indice]) : null;
    }

    public Double getLatitude(Document document) {
        return getCoordenada(document, 1);
    }

    public Double getLongitude(Document document) {
        return getCoordenada(document, 0);
    }

    public Document getKmlStream(String streetname, String number, String neighbourhood, String city, String state) throws RotaException {
        StringBuffer urlsb = new StringBuffer(resourceBundle.getString(Constants.URL_SEARCH));
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        InputStream in = null;
        HttpURLConnection httpConnection = null;
        Document doc = null;
        dbf.setValidating(false);
        String proxy = resourceBundle.getString(Constants.PROXY_HOST);
        String port = resourceBundle.getString(Constants.PROXY_PORT);
        try {
            String address = String.format("%s+%s+%s+%s+%s", URLEncoder.encode(streetname.trim(), Constants.URL_ENCODING), URLEncoder.encode(number.trim(), Constants.URL_ENCODING), URLEncoder.encode(neighbourhood.trim(), Constants.URL_ENCODING), URLEncoder.encode(city.trim(), Constants.URL_ENCODING), URLEncoder.encode(state.trim(), Constants.URL_ENCODING));
            DocumentBuilder df = dbf.newDocumentBuilder();
            urlsb.append(address);
            urlsb.append(resourceBundle.getString(Constants.GOOGLE_TYPE_OUTPUT));
            urlsb.append(resourceBundle.getString(Constants.SENSOR));
            urlsb.append(resourceBundle.getString(Constants.GOOGLE_KEY));
            urlsb.append(resourceBundle.getString(Constants.GOOGLE_KEY_VALUE));
            String addressUTF8 = urlsb.toString();
            URL url = new URL(addressUTF8);
            Properties systemproperties = System.getProperties();
            if (proxy != null && !proxy.equals("")) {
                systemproperties.setProperty("http.proxyHost", proxy);
                systemproperties.setProperty("http.proxyPort", port);
            }
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.connect();
            in = httpConnection.getInputStream();
            doc = df.parse(in);
            in.close();
            httpConnection.disconnect();
            if (doc == null || !verificaStatusRequisicao(doc)) {
                throw new RotaException("N�o foi poss�vel realizar a geodecodifica��o com o endere�o informado!");
            }
            return doc;
        } catch (UnsupportedEncodingException ue) {
            logger.error(ue);
            throw new RotaException("Encoding n�o suportado : " + ue.getMessage());
        } catch (MalformedURLException ma) {
            logger.error(ma);
            throw new RotaException("Erro na URL : " + ma.getMessage());
        } catch (ParserConfigurationException pe) {
            logger.error(pe);
            throw new RotaException("Erro ao realizar o parser da configura��o : " + pe.getMessage());
        } catch (SAXException sa) {
            logger.error(sa);
            throw new RotaException("Erro de SAX : " + sa.getMessage());
        } catch (ConnectException co) {
            logger.error(co);
            throw new RotaException("N�o foi poss�vel estabelecer a conex�o http : " + co.getMessage());
        } catch (IOException io) {
            logger.error(io);
            throw new RotaException("Erro de io : ", io);
        } catch (Exception ex) {
            throw new RotaException("N�o foi poss�vel gerar a rota  : " + ex.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ex) {
                    throw new RotaException("N�o foi poss�vel fechar o stream de dados ! : " + ex.getMessage());
                }
            }
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }

    public List<AgendamentoVO> gerarRota(List<AgendamentoVO> agendamento, PessoaVO pessoaOrigem) throws RotaException {
        logger.debug(">>> gerarRota ");
        if (agendamento == null) {
            throw new RotaException(resourceBundle.getString(Constants.ERROR_ROTA_AGENDAMENTO));
        }
        if (pessoaOrigem == null) {
            throw new RotaException(resourceBundle.getString(Constants.ERROR_ROTA_PESSOA_ORIGEM));
        }
        List<AgendamentoVO> rotaAgendamento = new ArrayList<AgendamentoVO>();
        AgendamentoVO agendamentoMenorDistancia = null;
        double menordistancia = Double.MAX_VALUE;
        for (AgendamentoVO currentAgendamento : agendamento) {
            double distancia2pontos = getDistancia(pessoaOrigem, currentAgendamento.getProvedor());
            logger.debug(">>>>  distância entre " + pessoaOrigem.getNome() + " e  " + currentAgendamento.getProvedor().getNome() + " é de : " + distancia2pontos);
            if (distancia2pontos < menordistancia) {
                menordistancia = distancia2pontos;
                agendamentoMenorDistancia = currentAgendamento;
            }
        }
        if (agendamentoMenorDistancia != null) {
            rotaAgendamento.add(agendamentoMenorDistancia);
        }
        if (agendamento.size() > 0) {
            agendamento.remove(agendamentoMenorDistancia);
            rotaAgendamento.addAll(gerarRota(agendamento, agendamentoMenorDistancia.getProvedor()));
        }
        logger.debug("<<< gerarRota ");
        return rotaAgendamento;
    }

    public boolean isEnderecoValido(Document KML) {
        return (getLogradouro(KML) != null) && (getNumero(KML) != null) && (getBairro(KML) != null) && (getCidade(KML) != null) && (getUf(KML) != null) && (getCep(KML) != null);
    }
}
