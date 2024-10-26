package erepublik.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import erepublik.ERepublikException;
import erepublik.IERepublikService;
import erepublik.dto.Battle;
import erepublik.dto.Battles;
import erepublik.dto.Citizen;
import erepublik.dto.Citizens;
import erepublik.dto.Combatant;
import erepublik.dto.Combatants;
import erepublik.dto.Company;
import erepublik.dto.Countries;
import erepublik.dto.CountryDetails;
import erepublik.dto.Error;
import erepublik.dto.ExchangeOffer;
import erepublik.dto.ExchangeOffers;
import erepublik.dto.Industries;
import erepublik.dto.Industry;
import erepublik.dto.MarketOffer;
import erepublik.dto.MarketOffers;
import erepublik.dto.Region;
import erepublik.dto.RegionCitizens;
import erepublik.dto.Regions;
import erepublik.dto.War;
import erepublik.dto.Wars;
import static erepublik.ERepublikFeeds.*;

public class ERepublicServiceImpl implements IERepublikService {

    private static final Logger LOG = Logger.getLogger("erepublik");

    private static final String ERROR_NODE_NAME = "error";

    private static IERepublikService instance;

    public static IERepublikService getInstance() throws ERepublikException {
        if (instance == null) {
            try {
                LOG.info("Creating DOM parser");
                DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder parser = domFactory.newDocumentBuilder();
                LOG.info("Creating JAXB unmarshaller");
                JAXBContext context = JAXBContext.newInstance("erepublik.dto");
                Unmarshaller unmarshaller = context.createUnmarshaller();
                instance = new ERepublicServiceImpl(parser, unmarshaller);
            } catch (JAXBException e) {
                throw new ERepublikException(e);
            } catch (ParserConfigurationException e) {
                throw new ERepublikException(e);
            }
        }
        return instance;
    }

    private DocumentBuilder parser;

    private Unmarshaller unmarshaller;

    private ERepublicServiceImpl(DocumentBuilder parser, Unmarshaller unmarshaller) {
        this.parser = parser;
        this.unmarshaller = unmarshaller;
    }

    public <T> T invoke(Class<T> type, String urlSpec, Object... args) throws ERepublikException {
        String url = this.getURL(urlSpec, args);
        LOG.info("Loading data from " + url);
        return this.parse(type, new InputSource(url));
    }

    public <T> T invokeGZIP(Class<T> type, String urlSpec, Object... args) throws ERepublikException {
        try {
            URL url = new URL(this.getURL(urlSpec, args));
            LOG.info("Loading gziped data from " + url);
            InputStream in = new GZIPInputStream(url.openStream());
            return this.parse(type, new InputSource(in));
        } catch (MalformedURLException e) {
            throw new ERepublikException(e);
        } catch (IOException e) {
            throw new ERepublikException(e);
        }
    }

    public String getURL(String urlSpec, Object... args) throws ERepublikException {
        Object[] params = new Object[args.length];
        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof String) {
                try {
                    params[i] = URLEncoder.encode((String) args[i], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    params[i] = args[i];
                }
            } else {
                params[i] = args[i];
            }
        }
        return BASE_URL + String.format(urlSpec, params);
    }

    public <T> T parse(Class<T> type, InputSource in) throws ERepublikException {
        try {
            LOG.info("Parsing input source");
            Document doc = parser.parse(in);
            Node node = doc.getFirstChild();
            if (ERROR_NODE_NAME.equals(node.getNodeName())) {
                Error error = unmarshaller.unmarshal(node, Error.class).getValue();
                LOG.warning("Received eRepublik error '" + error.getMessage() + "'.");
                throw new ERepublikException(error.getMessage(), error.getTime());
            }
            LOG.info("Unmashalling response data");
            return unmarshaller.unmarshal(node, type).getValue();
        } catch (SAXException e) {
            LOG.warning("Error parsing response " + e.getMessage());
            throw new ERepublikException(e);
        } catch (IOException e) {
            LOG.warning("IO Error during response parsing " + e.getMessage());
            throw new ERepublikException(e);
        } catch (JAXBException e) {
            LOG.warning("Error unmarshalling response " + e.getMessage());
            throw new ERepublikException(e);
        }
    }

    @Override
    public Battle getBattle(int id) throws ERepublikException {
        return this.invoke(Battle.class, BATTLE, id);
    }

    @Override
    public List<Battle> getBattles(int warId) throws ERepublikException {
        return this.invoke(Battles.class, BATTLES_BY_WAR, warId).getBattles();
    }

    @Override
    public Citizen getCitizen(int id) throws ERepublikException {
        return this.invoke(Citizen.class, CITIZEN, id);
    }

    @Override
    public Citizen getCitizen(String name) throws ERepublikException {
        return this.invoke(Citizen.class, CITIZEN_BY_NAME, name);
    }

    @Override
    public List<Integer> getCitizenIds(int regionId) throws ERepublikException {
        List<Integer> ids = new ArrayList<Integer>();
        for (int i = 1; ; i++) {
            RegionCitizens regionCitizens = this.invoke(RegionCitizens.class, CITIZENS_BY_REGION, regionId, i - 1);
            ids.addAll(regionCitizens.getCitizens());
            Integer pages = regionCitizens.getPageCount();
            if (pages == null || i >= pages) break;
        }
        return ids;
    }

    @Override
    public List<CountryDetails> getCountries() throws ERepublikException {
        return this.invoke(Countries.class, COUNTRIES).getCountries();
    }

    @Override
    public CountryDetails getCountry(int id) throws ERepublikException {
        return this.invoke(CountryDetails.class, COUNTRY, id);
    }

    @Override
    public Region getRegion(int id) throws ERepublikException {
        return this.invoke(Region.class, REGION, id);
    }

    @Override
    public List<Region> getRegions(int countryId) throws ERepublikException {
        return this.invoke(Regions.class, REGIONS_BY_COUNTRY, countryId).getRegions();
    }

    @Override
    public War getWar(int id) throws ERepublikException {
        return this.invoke(War.class, WAR, id);
    }

    @Override
    public List<War> getWars(int countryId) throws ERepublikException {
        return this.invoke(Wars.class, WARS_BY_COUNTRY, countryId).getWars();
    }

    @Override
    public List<Combatant> getCombatants(int battleId) throws ERepublikException {
        List<Combatant> combatants = new ArrayList<Combatant>();
        for (int i = 1; ; i++) {
            Combatants combatantsPage = this.invoke(Combatants.class, COMBATANTS_BY_BATTLE, battleId, i - 1);
            combatants.addAll(combatantsPage.getCombatants());
            Integer pages = combatantsPage.getPageCount();
            if (pages == null || i >= pages) break;
        }
        return combatants;
    }

    @Override
    public Company getCompany(int id) throws ERepublikException {
        return this.invoke(Company.class, COMPANY, id);
    }

    @Override
    public List<ExchangeOffer> getExchangeOffers(String buyCurrency, String sellCurrency) throws ERepublikException {
        return this.invoke(ExchangeOffers.class, EXCHANGE_OFFERS, buyCurrency, sellCurrency).getOffers();
    }

    @Override
    public List<MarketOffer> getMarketOffers(int industryId, int countryId, int attr1, int attr2) throws ERepublikException {
        return this.invoke(MarketOffers.class, MARKET_OFFERS, industryId, countryId, attr1, attr2).getOffers();
    }

    @Override
    public List<Industry> getIndustries() throws ERepublikException {
        return this.invoke(Industries.class, INDUSTRIES).getIndustries();
    }

    @Override
    public List<Citizen> getCitizens(int countryId) throws ERepublikException {
        return this.invokeGZIP(Citizens.class, CITIZENS_BY_COUNTRY, countryId).getCitizens();
    }
}
