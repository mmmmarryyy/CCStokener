package org.fao.geonet.kernel.harvest.harvester.thredds;

import jeeves.exceptions.BadServerCertificateEx;
import jeeves.exceptions.BadXmlResponseEx;
import jeeves.interfaces.Logger;
import jeeves.resources.dbms.Dbms;
import jeeves.server.context.ServiceContext;
import jeeves.utils.Util;
import jeeves.utils.Xml;
import jeeves.utils.XmlRequest;
import jeeves.xlink.Processor;
import org.fao.geonet.GeonetContext;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.kernel.harvest.harvester.CategoryMapper;
import org.fao.geonet.kernel.harvest.harvester.GroupMapper;
import org.fao.geonet.kernel.harvest.harvester.Privileges;
import org.fao.geonet.kernel.harvest.harvester.RecordInfo;
import org.fao.geonet.kernel.harvest.harvester.UriMapper;
import org.fao.geonet.kernel.harvest.harvester.fragment.FragmentHarvester;
import org.fao.geonet.kernel.harvest.harvester.fragment.FragmentHarvester.FragmentParams;
import org.fao.geonet.kernel.harvest.harvester.fragment.FragmentHarvester.HarvestSummary;
import org.fao.geonet.kernel.setting.SettingInfo;
import org.fao.geonet.lib.Lib;
import org.fao.geonet.util.ISODate;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import thredds.catalog.InvAccess;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDataset;
import thredds.catalog.InvMetadata;
import thredds.catalog.InvService;
import thredds.catalog.ServiceType;
import thredds.catalog.ThreddsMetadata;
import thredds.catalog.dl.DIFWriter;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasetInfo;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.units.DateType;
import ucar.unidata.util.StringUtil;
import javax.net.ssl.SSLHandshakeException;
import java.io.DataInputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** 
 * A ThreddsHarvester is able to generate metadata for datasets and services
 * from a Thredds catalogue. Metadata for datasets are generated
 * using dataset information contained in the thredds catalogue document or
 * or from opening the dataset and retrieving variables, coordinate systems 
 * and/or global attributes.
 * 
 * Metadata produced are :
 * <ul>
 * 	<li>ISO19119 for service metadata (all services in the catalog)</li>
 * 	<li>ISO19139 (or profile) metadata for datasets in catalog</li>
 * </ul>
 * 
 * <pre>  
 * <nodes>
 *  <node type="thredds" id="114">
 *    <site>
 *      <name>TEST</name>
 *      <uuid>c1da2928-c866-49fd-adde-466fe36d3508</uuid>
 *      <account>
 *        <use>true</use>
 *        <username />
 *        <password />
 *      </account>
 *      <url>http://localhost:5556/thredds/catalog.xml</url>
 *      <icon>default.gif</icon>
 *    </site>
 *    <options>
 *      <every>90</every>
 *      <oneRunOnly>false</oneRunOnly>
 *      <status>active</status>
 *      <lang>eng</lang>
 *      <createThumbnails>false</createThumbnails>
 *      <createServiceMd>false</createServiceMd>
 *      <createCollectionDatasetMd>true</createCollectionDatasetMd>
 *      <createAtomicDatasetMd>false</createAtomicDatasetMd>
 *      <ignoreHarvestOnCollections>true</ignoreHarvestOnCollections>
 * Choice of {
 *      <outputSchemaOnCollectionsDIF>iso19139</outputSchemaOnCollectionsDIF>
 * } OR {
 *      <outputSchemaOnCollectionsFragments>iso19139</outputSchemaOnCollectionsFragments>
 *      <collectionFragmentStylesheet>collection_fragments.xsl</collectionFragmentStylesheet>
 *      <collectionMetadataTemplate>My template</collectionMetadataTemplate>
 *      <createCollectionSubtemplates>false</createCollectionSubtemplates>
 * }
 *      <ignoreHarvestOnAtomics>true</ignoreHarvestOnAtomics>
 * Choice of {
 *      <outputSchemaOnAtomicsDIF>iso19139.mcp</outputSchemaOnAtomicsDIF>
 * } OR {
 *      <outputSchemaOnAtomicsFragments>iso19139</outputSchemaOnAtomicsFragments>
 *      <atomicFragmentStylesheet>atomic_fragments.xsl</atomicFragmentStylesheet>
 *      <atomicMetadataTemplate>My template</atomicMetadataTemplate>
 *      <createAtomicSubtemplates>false</createAtomicSubtemplates>
 * }
 *      <modifiedOnly>true</modifiedOnly>
 *      <datasetCategory></datasetCategory>
 *    </options>
 *    <privileges>
 *      <group id="1">
 *        <operation name="view" />
 *      </group>
 *    </privileges>
 *    <categories>
 *      <category id="3" />
 *    </categories>
 *    <info>
 *      <lastRun>2007-12-05T16:17:20</lastRun>
 *      <running>false</running>
 *    </info>
 *  </node>
 * </nodes>
 * </pre>
 * 
 * @author Simon Pigot
 *   
 */
class Harvester {

    /** 
	 * Constructor
	 *  
	 * @param log		
	 * @param context		Jeeves context
	 * @param dbms 			Database
	 * @param params	Information about harvesting configuration for the node
	 * 
	 * @return null
     **/
    public Harvester(Logger log, ServiceContext context, Dbms dbms, ThreddsParams params) {
        this.log = log;
        this.context = context;
        this.dbms = dbms;
        this.params = params;
        result = new ThreddsResult();
        GeonetContext gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
        dataMan = gc.getDataManager();
        schemaMan = gc.getSchemamanager();
        SettingInfo si = new SettingInfo(context);
        String siteUrl = si.getSiteUrl() + context.getBaseUrl();
        metadataGetService = siteUrl + "/srv/en/xml.metadata.get";
        if (params.createAtomicDatasetMd && params.atomicMetadataGeneration.equals(ThreddsParams.FRAGMENTS)) {
            atomicFragmentHarvester = new FragmentHarvester(log, context, dbms, getAtomicFragmentParams());
        }
        if (params.createCollectionDatasetMd && params.collectionMetadataGeneration.equals(ThreddsParams.FRAGMENTS)) {
            collectionFragmentHarvester = new FragmentHarvester(log, context, dbms, getCollectionFragmentParams());
        }
    }

    /** 
     * Start the harvesting of a thredds catalog 
     **/
    public ThreddsResult harvest() throws Exception {
        Element xml = null;
        log.info("Retrieving remote metadata information for : " + params.name);
        localUris = new UriMapper(dbms, params.uuid);
        String url = params.url;
        try {
            XmlRequest req = new XmlRequest();
            req.setUrl(new URL(url));
            req.setMethod(XmlRequest.Method.GET);
            Lib.net.setupProxy(context, req);
            xml = req.execute();
        } catch (SSLHandshakeException e) {
            throw new BadServerCertificateEx("Most likely cause: The thredds catalog " + url + " does not have a " + "valid certificate. If you feel this is because the server may be " + "using a test certificate rather than a certificate from a well " + "known certification authority, then you can add this certificate " + "to the GeoNetwork keystore using bin/installCert");
        }
        harvestCatalog(xml);
        for (String localUri : localUris.getUris()) {
            if (!harvestUris.contains(localUri)) {
                for (RecordInfo record : localUris.getRecords(localUri)) {
                    log.debug("  - Removing deleted metadata with id: " + record.id);
                    dataMan.deleteMetadata(context, dbms, record.id);
                    if (record.isTemplate.equals("s")) {
                        Processor.uncacheXLinkUri(metadataGetService + "?uuid=" + record.uuid);
                        result.subtemplatesRemoved++;
                    } else {
                        result.locallyRemoved++;
                    }
                }
            }
        }
        dbms.commit();
        result.total = result.serviceRecords + result.collectionDatasetRecords + result.atomicDatasetRecords;
        return result;
    }

    /** 
     * Add metadata to GN for the services and datasets in a thredds 
	 * catalog
     *  
	 * 1. Open Catalog Document
	 * 2. Crawl the catalog processing datasets as ISO19139 records 
	 * and recording services (attach dataset ids to the services that deliver
	 * them)
	 * 3. Process services found as ISO19119 records
	 * 4. Create a service record for the thredds catalog service provided and 
	 * list service records as something that the thredds catalog provides
	 * 5. Save all
     *	
     * @param cata      Catalog document
     *                   
     **/
    private void harvestCatalog(Element cata) throws Exception {
        if (cata == null) return;
        localCateg = new CategoryMapper(dbms);
        localGroups = new GroupMapper(dbms);
        Lib.net.setupProxy(context);
        InvCatalogFactory factory = new InvCatalogFactory("default", true);
        catalog = (InvCatalogImpl) factory.readXML(params.url);
        StringBuilder buff = new StringBuilder();
        if (!catalog.check(buff, true)) {
            throw new BadXmlResponseEx("Invalid catalog " + params.url + "\n" + buff.toString());
        }
        log.info("Catalog read from " + params.url + " is \n" + factory.writeXML(catalog));
        String serviceStyleSheet = context.getAppPath() + Geonet.Path.IMPORT_STYLESHEETS + "/ThreddsCatalog-to-ISO19119_ISO19139.xsl";
        URL url = new URL(params.url);
        hostUrl = url.getProtocol() + "://" + url.getHost();
        if (url.getPort() != -1) hostUrl += ":" + url.getPort();
        log.info("Crawling the datasets in the catalog....");
        List<InvDataset> dsets = catalog.getDatasets();
        for (InvDataset ds : dsets) {
            crawlDatasets(ds);
        }
        int totalDs = result.collectionDatasetRecords + result.atomicDatasetRecords;
        log.info("Processed " + totalDs + " datasets.");
        if (params.createServiceMd) {
            log.info("Processing " + services.size() + " services...");
            processServices(cata, serviceStyleSheet);
            log.info("Creating service metadata for thredds catalog...");
            Map<String, String> param = new HashMap<String, String>();
            param.put("lang", params.lang);
            param.put("topic", params.topic);
            param.put("uuid", params.uuid);
            param.put("url", params.url);
            param.put("name", catalog.getName());
            param.put("type", "Thredds Data Service Catalog " + catalog.getVersion());
            param.put("version", catalog.getVersion());
            param.put("desc", Xml.getString(cata));
            param.put("props", catalog.getProperties().toString());
            param.put("serverops", "");
            log.debug("  - XSLT transformation using " + serviceStyleSheet);
            Element md = Xml.transform(cata, serviceStyleSheet, param);
            saveMetadata(md, Util.scramble(params.url), params.url);
            harvestUris.add(params.url);
            result.serviceRecords++;
        }
    }

    /** 
	 * Crawl all datasets in the catalog recursively
	 *
     * @param	catalogDs		the dataset being processed 
	 * @throws	Exception 
	 **/
    private void crawlDatasets(InvDataset catalogDs) throws Exception {
        log.info("Crawling through " + catalogDs.getName());
        InvDataset realDs = catalogDs;
        if (catalogDs instanceof InvCatalogRef) {
            InvDataset proxyDataset = ((InvCatalogRef) catalogDs).getProxyDataset();
            realDs = proxyDataset.getName().equals(catalogDs.getName()) ? proxyDataset : catalogDs;
        }
        if (realDs.hasNestedDatasets()) {
            List<InvDataset> dsets = realDs.getDatasets();
            for (InvDataset ds : dsets) {
                crawlDatasets(ds);
            }
        }
        if (harvestMetadata(realDs)) {
            log.info("Harvesting dataset: " + realDs.getName());
            harvest(realDs);
        } else {
            log.info("Skipping dataset: " + realDs.getName());
        }
        if (catalogDs instanceof InvCatalogRef) {
            ((InvCatalogRef) catalogDs).release();
        }
    }

    /** 
	 * Save the metadata to GeoNetwork's database 
	 *
     * @param md		the metadata being saved
     * @param uuid		the uuid of the metadata being saved
     * @param uri		the uri from which the metadata has been harvested
	 **/
    private void saveMetadata(Element md, String uuid, String uri) throws Exception {
        md.removeNamespaceDeclaration(invCatalogNS);
        String schema = dataMan.autodetectSchema(md);
        if (schema == null) {
            log.warning("Skipping metadata with unknown schema.");
            result.unknownSchema++;
        }
        log.info("  - Adding metadata with " + uuid + " schema is set to " + schema + "\n XML is " + Xml.getString(md));
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date date = new Date();
        deleteExistingMetadata(uri);
        int userid = 1;
        String group = null, isTemplate = null, docType = null, title = null, category = null;
        boolean ufo = false, indexImmediate = false;
        String id = dataMan.insertMetadata(context, dbms, schema, md, context.getSerialFactory().getSerial(dbms, "Metadata"), uuid, userid, group, params.uuid, isTemplate, docType, title, category, df.format(date), df.format(date), ufo, indexImmediate);
        int iId = Integer.parseInt(id);
        addPrivileges(id);
        addCategories(id);
        dataMan.setTemplateExt(dbms, iId, "n", null);
        dataMan.setHarvestedExt(dbms, iId, params.uuid, uri);
        boolean indexGroup = false;
        dataMan.indexMetadata(dbms, id, indexGroup);
        dbms.commit();
    }

    /** 
	 * Process one dataset generating metadata as per harvesting node settings
	 *
     * @param 	ds	the dataset to be processed 
	 * @throws	Exception 
	 **/
    private void harvest(InvDataset ds) throws Exception {
        if (!params.modifiedOnly || datasetChanged(ds)) {
            if (harvestMetadataUsingFragments(ds)) {
                createMetadataUsingFragments(ds);
            } else {
                createDIFMetadata(ds);
            }
        }
        harvestUris.add(getUri(ds));
        List<InvAccess> accesses = ds.getAccess();
        for (InvAccess access : accesses) {
            processService(access.getService(), getUuid(ds), ds);
        }
    }

    /** 
	 * Get dataset uri 
	 *
     * @param 	ds	the dataset to be processed
     *  
	 **/
    private String getUri(InvDataset ds) {
        if (ds.getID() == null) {
            return ds.getParentCatalog().getUriString() + "#" + ds.getName();
        } else {
            return getSubsetUrl(ds);
        }
    }

    /** 
	 * Has the dataset has been modified since its metadata was last
	 * harvested  
	 *
     * @param 	ds	the dataset to be processed
     *  
	 **/
    private boolean datasetChanged(InvDataset ds) {
        List<RecordInfo> localRecords = localUris.getRecords(getUri(ds));
        if (localRecords == null) return true;
        Date lastModifiedDate = null;
        List<DateType> dates = ds.getDates();
        for (DateType date : dates) {
            if (date.getType().equalsIgnoreCase("modified")) {
                lastModifiedDate = date.getDate();
            }
        }
        if (lastModifiedDate == null) return true;
        String datasetModifiedDate = new ISODate(lastModifiedDate.getTime()).toString();
        for (RecordInfo localRecord : localRecords) {
            if (localRecord.isOlderThan(datasetModifiedDate)) return true;
        }
        return false;
    }

    /** 
	 * Delete all metadata previously harvested for a particular uri 
	 *
     * @param 	uri		uri for which previously harvested metadata should be deleted
     *  
	 **/
    private void deleteExistingMetadata(String uri) throws Exception {
        List<RecordInfo> localRecords = localUris.getRecords(uri);
        if (localRecords == null) return;
        for (RecordInfo record : localRecords) {
            dataMan.deleteMetadata(context, dbms, record.id);
            if (record.isTemplate.equals("s")) {
                Processor.uncacheXLinkUri(metadataGetService + "?uuid=" + record.uuid);
            }
        }
    }

    /** 
	 * Get uuid and change date for thredds dataset
	 *
     * @param ds     the dataset to be processed 
	 **/
    private RecordInfo getDatasetInfo(InvDataset ds) {
        Date lastModifiedDate = null;
        List<DateType> dates = ds.getDates();
        for (DateType date : dates) {
            if (date.getType().equalsIgnoreCase("modified")) {
                lastModifiedDate = date.getDate();
            }
        }
        String datasetChangeDate = null;
        if (lastModifiedDate != null) {
            DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
            datasetChangeDate = fmt.print(new DateTime(lastModifiedDate));
        }
        return new RecordInfo(getUuid(ds), datasetChangeDate);
    }

    /** 
     * Create metadata using fragments
     *
     * <ul>
     * <li>collect useful metadata for the dataset<li>
     * <li>use supplied stylesheet to convert collected metadata into fragments</li>
     * <li>harvest metadata from fragments as requested</li> 
     * </ul>
     * 
     * Metadata collected is as follows:
     *
     * <pre>
     * {@code
     * <root>
     *    <catalogUri>http://someserver.com/thredds/catalog.xml</catalog>
     *    <uuid>uuid-generated-for-dataset</uuid>
     *    <catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.0.1">
	 *		 ... subset of catalog containing dataset as the top dataset ...
	 *    </catalog>
	 *    <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"  location="example1.nc">
	 *       ... ncml generated for netcdf dataset ...
	 *       ... atomic datasets only ...
	 *    </netcdf>
	 * </root>
	 * }
	 * </pre>
     **/
    private void createMetadataUsingFragments(InvDataset ds) {
        try {
            log.info("Retrieving thredds/netcdf metadata...");
            Element dsMetadata = new Element("root");
            dsMetadata.addContent(new Element("catalogUri").setText(ds.getParentCatalog().getUriString()));
            dsMetadata.addContent(new Element("uuid").setText(getUuid(ds)));
            dsMetadata.addContent(new Element("fullName").setText(ds.getFullName()));
            dsMetadata.addContent(getDatasetSubset(ds));
            if (!ds.hasNestedDatasets()) {
                NetcdfDataset ncD = NetcdfDataset.openDataset("thredds:" + ds.getCatalogUrl());
                NcMLWriter ncmlWriter = new NcMLWriter();
                Element ncml = Xml.loadString(ncmlWriter.writeXML(ncD), false);
                dsMetadata.addContent(ncml);
            }
            log.debug("Thredds metadata and ncml is:" + Xml.getString(dsMetadata));
            String schema = ds.hasNestedDatasets() ? params.outputSchemaOnCollectionsFragments : params.outputSchemaOnAtomicsFragments;
            fragmentStylesheetDirectory = schemaMan.getSchemaDir(schema) + Geonet.Path.TDS_STYLESHEETS;
            String stylesheet = ds.hasNestedDatasets() ? params.collectionFragmentStylesheet : params.atomicFragmentStylesheet;
            Element fragments = Xml.transform(dsMetadata, fragmentStylesheetDirectory + "/" + stylesheet);
            log.debug("Fragments generated for dataset:" + Xml.getString(fragments));
            deleteExistingMetadata(getUri(ds));
            FragmentHarvester fragmentHarvester = ds.hasNestedDatasets() ? collectionFragmentHarvester : atomicFragmentHarvester;
            HarvestSummary fragmentResult = fragmentHarvester.harvest(fragments, getUri(ds));
            result.fragmentsReturned += fragmentResult.fragmentsReturned;
            result.fragmentsUnknownSchema += fragmentResult.fragmentsUnknownSchema;
            result.subtemplatesAdded += fragmentResult.fragmentsAdded;
            result.fragmentsMatched += fragmentResult.fragmentsMatched;
            if (ds.hasNestedDatasets()) {
                result.collectionDatasetRecords += fragmentResult.recordsBuilt;
            } else {
                result.atomicDatasetRecords += fragmentResult.recordsBuilt;
            }
        } catch (Exception e) {
            log.error("Thrown Exception " + e + " during dataset processing");
            e.printStackTrace();
        }
    }

    /** 
	 * Return a catalog having the specified dataset as the top dataset
	 * resolving inherited metadata and required services
	 *
     * @param ds     the dataset to be processed 
	 */
    private Element getDatasetSubset(InvDataset ds) throws Exception {
        String datasetSubsetUrl = getSubsetUrl(ds);
        return Xml.loadFile(new URL(datasetSubsetUrl));
    }

    /** 
	 * Return url to a catalog having the specified dataset as the top dataset
	 *
     * @param ds     the dataset to be processed 
	 **/
    private String getSubsetUrl(InvDataset ds) {
        try {
            return ds.getParentCatalog().getUriString() + "?dataset=" + URLEncoder.encode(ds.getID(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Thrown Exception " + e + " during dataset processing");
            e.printStackTrace();
        }
        return null;
    }

    /** 
	 * Get uuid for dataset
	 *
     * @param ds     the dataset to be processed 
	 **/
    private String getUuid(InvDataset ds) {
        String uuid = ds.getUniqueID();
        if (uuid == null) {
            uuid = Util.scramble(ds.getCatalogUrl());
        } else {
            uuid = StringUtil.allow(uuid, "_-.", '-');
        }
        return uuid;
    }

    /** 
	 * Process one dataset by extracting its metadata, writing to DIF
	 * and using xslt to transform to the required ISO format. 
	 *
	 * @param ds     the dataset to be processed 
	 */
    private void createDIFMetadata(InvDataset ds) {
        try {
            boolean addCoordSys = false;
            List<InvMetadata> mds = ds.getMetadata();
            log.info("Dataset has " + mds.size() + " metadata elements");
            for (InvMetadata md : mds) {
                log.info("Found metadata " + md.toString());
            }
            DIFWriter difWriter = new DIFWriter();
            StringBuffer sBuff = new StringBuffer();
            Element dif = null;
            if (difWriter.isDatasetUseable(ds, sBuff)) {
                log.info("Yay! Dataset has DIF compatible metadata " + sBuff.toString());
                dif = difWriter.writeOneEntry(ds, sBuff);
            } else {
                log.info("Dataset does not have DIF compatible metadata so we will write a relaxed DIF entry\n" + sBuff.toString());
                dif = difWriter.writeOneRelaxedEntry(ds, sBuff);
                addCoordSys = true;
            }
            String uuid = dif.getChild("Entry_ID", difNS).getText();
            boolean isCollection = ds.hasNestedDatasets();
            log.info("Dataset is a collection dataset? " + isCollection);
            Element md = null;
            if (isCollection) {
                String difToIsoStyleSheet = schemaMan.getSchemaDir(params.outputSchemaOnCollectionsDIF) + Geonet.Path.DIF_STYLESHEETS + "/DIFToISO.xsl";
                log.info("Transforming collection dataset to " + params.outputSchemaOnCollectionsDIF);
                md = Xml.transform(dif, difToIsoStyleSheet);
            } else {
                String difToIsoStyleSheet = schemaMan.getSchemaDir(params.outputSchemaOnAtomicsDIF) + Geonet.Path.DIF_STYLESHEETS + "/DIFToISO.xsl";
                log.info("Transforming atomic dataset to " + params.outputSchemaOnAtomicsDIF);
                md = Xml.transform(dif, difToIsoStyleSheet);
            }
            if (addCoordSys) {
                boolean globalAttributes = false;
                if (!isCollection) {
                    log.info("Opening dataset to get global attributes");
                    try {
                        NetcdfDataset ncD = NetcdfDataset.openDataset("thredds:" + ds.getCatalogUrl());
                        Attribute mdCon = ncD.findGlobalAttributeIgnoreCase("metadata_conventions");
                        if (mdCon != null) {
                            List<Attribute> ga = ncD.getGlobalAttributes();
                            for (Attribute att : ga) {
                                log.debug("Attribute found " + att.toString());
                            }
                        } else {
                            log.debug("No global attribute with metadata conventions found");
                        }
                        ncD.close();
                    } catch (Exception e) {
                        log.info("Exception raised in netcdfDataset ops: " + e);
                        e.printStackTrace();
                    }
                }
                boolean foundNetcdfInfo = false;
                if (!globalAttributes && !isCollection) {
                    log.info("No global attributes describing metadata so opening dataset to get coordinate systems");
                    try {
                        NetcdfDatasetInfo ncDI = new NetcdfDatasetInfo("thredds:" + ds.getCatalogUrl());
                        log.info("Coordinate systems builder is " + ncDI.getConventionUsed());
                        if (!ncDI.getConventionUsed().equals("None")) {
                            Document doc = ncDI.makeDocument();
                            Element coords = doc.detachRootElement();
                            log.info("Coordinate systems of dataset are: \n" + Xml.getString(coords));
                            setCoordsStyleSheet(isCollection);
                            addKeywordsAndDataParams(coords, md);
                            foundNetcdfInfo = true;
                        } else {
                            log.debug("Coordinate system convention is not recognized");
                        }
                        ncDI.close();
                    } catch (Exception e) {
                        log.info("Exception raised in netcdfDatasetInfo ops: " + e);
                        e.printStackTrace();
                    }
                }
                if (!globalAttributes && !foundNetcdfInfo) {
                    List<ThreddsMetadata.Variables> vsL = ds.getVariables();
                    if (vsL != null && vsL.size() > 0) {
                        for (ThreddsMetadata.Variables vs : vsL) {
                            String vHref = vs.getVocabHref();
                            URI vUri = vs.getVocabUri();
                            String vocab = vs.getVocabulary();
                            Element coords = new Element("netcdfDatasetInfo");
                            for (ThreddsMetadata.Variable v : vs.getVariableList()) {
                                Element varX = new Element("variable");
                                varX.setAttribute("name", v.getName());
                                varX.setAttribute("decl", v.getDescription());
                                varX.setAttribute("units", v.getUnits());
                                varX.setAttribute("vocab", vocab);
                                varX.setAttribute("vocaburi", vUri.toString());
                                varX.setAttribute("vocabhref", vHref);
                                coords.addContent(varX);
                            }
                            log.info("Coordinate systems from ThreddsMetadata are: \n" + Xml.getString(coords));
                            setCoordsStyleSheet(isCollection);
                            addKeywordsAndDataParams(coords, md);
                        }
                    }
                }
            }
            saveMetadata(md, uuid, getUri(ds));
            if (isCollection) {
                result.collectionDatasetRecords++;
            } else {
                result.atomicDatasetRecords++;
            }
        } catch (Exception e) {
            log.error("Thrown Exception " + e + " during dataset processing");
            e.printStackTrace();
        }
    }

    /** 
	 * Create the coordinate stylesheet names that will be used to add 
	 * gmd:keywords and mcp:DataParameters if the output schema requires.
	 *
	 * @param	isCollection true if we are working with a collection dataset
	 */
    private void setCoordsStyleSheet(boolean isCollection) {
        String schemaDir;
        if (!isCollection) {
            schemaDir = schemaMan.getSchemaDir(params.outputSchemaOnAtomicsDIF);
        } else {
            schemaDir = schemaMan.getSchemaDir(params.outputSchemaOnCollectionsDIF);
        }
        cdmCoordsToIsoKeywordsStyleSheet = schemaDir + Geonet.Path.DIF_STYLESHEETS + "/CDMCoords-to-ISO19139Keywords.xsl";
        if (schemaDir.contains("iso19139.mcp")) {
            cdmCoordsToIsoMcpDataParametersStyleSheet = schemaDir + Geonet.Path.DIF_STYLESHEETS + "/CDMCoords-to-ISO19139MCPDataParameters.xsl";
        } else {
            cdmCoordsToIsoMcpDataParametersStyleSheet = null;
        }
    }

    /** 
	 * Process a netcdfinfo document - adding variables as keywords and 
	 * mcp:DataParameters if the output schema requires.
	 *
	 * @param	coords	the netcdfinfo document with coord systems embedded
	 * @param	md		ISO metadata record to add keywords and data params to
	 **/
    private void addKeywordsAndDataParams(Element coords, Element md) throws Exception {
        Element keywords = Xml.transform(coords, cdmCoordsToIsoKeywordsStyleSheet);
        addKeywords(md, keywords);
        if (cdmCoordsToIsoMcpDataParametersStyleSheet != null) {
            Element dataParameters = Xml.transform(coords, cdmCoordsToIsoMcpDataParametersStyleSheet);
            log.info("mcp:DataParameters are: \n" + Xml.getString(dataParameters));
            addDataParameters(md, dataParameters);
        }
    }

    /** 
	 * Process a service reference in a dataset - record details of the 
	 * service and add the details of a dataset to the list of datasets it
	 * serves - Note: compound services are expanded.
	 *
	 * @param serv     the service to be processed 
	 * @param uuid     uuid of the dataset that is delivered by this service
	 * @param ds		    dataset that is being delivered by this service
	 **/
    private void processService(InvService serv, String uuid, InvDataset ds) {
        List<InvService> servs = new ArrayList();
        if (serv.getServiceType() == ServiceType.COMPOUND) {
            servs.addAll(serv.getServices());
        } else {
            servs.add(serv);
        }
        for (InvService s : servs) {
            if (s.getServiceType().equals(ServiceType.RESOLVER)) continue;
            String sUrl = "";
            if (!s.isRelativeBase()) {
                sUrl = s.getBase();
            } else {
                sUrl = hostUrl + s.getBase();
            }
            ThreddsService ts = services.get(sUrl);
            if (ts == null) {
                ts = new ThreddsService();
                ts.service = s;
                ts.version = getVersion(serv, ds);
                ts.ops = getServerOperations(serv, ds);
                services.put(sUrl, ts);
            }
            ts.datasets.put(uuid, ds.getName());
        }
    }

    /** 
	 * Find the version of the service that delivers a particular dataset
	 * Handles OPeNDAP and HTTP only at present
	 *
	 * @param	serv	the service that delivers the dataset
	 * @param	ds		the dataset being delivered by the service
	 **/
    private String getVersion(InvService serv, InvDataset ds) {
        String result = "unknown";
        if (serv.getServiceType() == ServiceType.OPENDAP) {
            InvAccess access = ds.getAccess(ServiceType.OPENDAP);
            if (access != null) {
                String href = access.getStandardUrlName() + ".ver";
                String readResult = getResultFromHttpUrl(href);
                if (readResult != null) result = readResult;
            }
        } else if (serv.getServiceType() == ServiceType.HTTPServer) {
            result = "HTTP/1.1";
        }
        return result;
    }

    /** 
	 * Get the server operations 
	 * Applicable to OPeNDAP only at present
	 *
	 * @param	serv	the service that delivers the dataset
	 * @param	ds		the dataset being delivered by the service
	 **/
    private String getServerOperations(InvService serv, InvDataset ds) {
        String result = "none";
        if (serv.getServiceType() == ServiceType.OPENDAP) {
            InvAccess access = ds.getAccess(ServiceType.OPENDAP);
            if (access != null) {
                String href = access.getStandardUrlName() + ".help";
                String readResult = getResultFromHttpUrl(href);
                if (readResult != null) result = readResult;
            }
        }
        return result;
    }

    /** 
	 * Get a String result from an HTTP URL
	 *
	 * @param 	href		the URL to get the info from
	 **/
    private String getResultFromHttpUrl(String href) {
        String result = null;
        try {
            URL url = new URL(href);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Object o = conn.getContent();
            log.debug("Opened " + href + " and got class " + o.getClass().getName());
            StringBuffer version = new StringBuffer();
            String inputLine;
            DataInputStream dis = new DataInputStream(conn.getInputStream());
            while ((inputLine = dis.readLine()) != null) {
                version.append(inputLine + "\n");
            }
            result = version.toString();
            log.debug("Read from URL:\n" + result);
            dis.close();
        } catch (Exception e) {
            log.debug("Caught exception " + e + " whilst attempting to query URL " + href);
            e.printStackTrace();
        } finally {
            return result;
        }
    }

    /** 
	 * Process all services that serve datasets in the thredds catalog
	 *
	 * @param	cata				the XML of the catalog
	 * @param	serviceStyleSheet	name of the stylesheet to produce 19119
	 **/
    private void processServices(Element cata, String serviceStyleSheet) throws Exception {
        for (String sUrl : services.keySet()) {
            ThreddsService ts = services.get(sUrl);
            InvService serv = ts.service;
            log.debug("Processing Thredds service: " + serv.toString());
            String sUuid = Util.scramble(sUrl);
            ts.uuid = sUuid;
            log.debug("  - XSLT transformation using " + serviceStyleSheet);
            Map<String, String> param = new HashMap<String, String>();
            param.put("lang", params.lang);
            param.put("topic", params.topic);
            param.put("uuid", sUuid);
            param.put("url", sUrl);
            param.put("name", serv.getName());
            param.put("type", serv.getServiceType().toString().toUpperCase());
            param.put("version", ts.version);
            param.put("desc", serv.toString());
            param.put("props", serv.getProperties().toString());
            param.put("serverops", ts.ops);
            Element md = Xml.transform(cata, serviceStyleSheet, param);
            String schema = dataMan.autodetectSchema(md);
            if (schema == null) {
                log.warning("Skipping metadata with unknown schema.");
                result.unknownSchema++;
            } else {
                md = addOperatesOnUuid(md, ts.datasets);
                saveMetadata(md, sUuid, sUrl);
                harvestUris.add(sUrl);
                result.serviceRecords++;
            }
        }
    }

    /** 
     * Add an Element to a child list at index after specified element
     *  
     * @param md      		iso19139 metadata
     * @param theNewElem	the new element to be added
     * @param name				the name of the element to search for
     * @param ns					the namespace of the element to search for
     *                   
     **/
    boolean addAfter(Element md, Element theNewElem, String name, Namespace ns) throws Exception {
        Element chSet = md.getChild(name, ns);
        if (chSet != null) {
            int pos = md.indexOf(chSet);
            md.addContent(pos + 1, theNewElem);
            return true;
        }
        return false;
    }

    /** 
     * Add keywords generated from CDM coordinate systems to identificationInfo
       
       <gmd:descriptiveKeywords>
	   		<gmd:MD_Keywords>
		 			<gmd:keyword>
		 				<gco:CharacterString>
		 				</gco:CharacterString>
		 			</gmd:keyword>
		 			...
		 			...
		 			...
		 			<gmd:type>
		 				<gmd:MD_KeywordType codelist...>
		 			</gmd:type>
		 			<gmd:thesaurusName>
		 				<gmd:CI_Citation>
		 					....
		 				</gmd:CI_Citation>
		 			</gmd:thesaurusName>
	   		</gmd:MD_Keywords>
       </gmd:descriptiveKeywords>
     	
     * @param md        iso19139 metadata
     * @param keywords	gmd:keywords block to be added to metadata
     *                   
     **/
    private Element addKeywords(Element md, Element keywords) throws Exception {
        Element root = (Element) md.getChild("identificationInfo", gmd).getChildren().get(0);
        boolean ok = addAfter(root, keywords, "descriptiveKeywords", gmd);
        if (!ok) {
            throw new BadXmlResponseEx("The metadata did not have a descriptiveKeywords Element");
        }
        return md;
    }

    /** 
     * Add mcp:dataParameters created from CDM coordinate systems to 
	 * identificationInfo (mcp only)
       
       <mcp:dataParameters>
	   		<mcp:DP_DataParameters>
		 			...
		 			...
		 			...
	   		</mcp:DP_DataParameters>
       </mcp:dataParameters>
     	
     * @param md        			iso19139 MCP metadata
     * @param dataParameters	mcp:dataParameters block to be added to metadata
     *                   
     **/
    private Element addDataParameters(Element md, Element dataParameters) throws Exception {
        Element root = (Element) md.getChild("identificationInfo", gmd).getChildren().get(0);
        root.addContent(dataParameters);
        return md;
    }

    /** 
     * Add OperatesOn elements on an ISO19119 metadata
     *  
     *  <srv:operatesOn>
	 *		<gmd:MD_DataIdentification uuidref=""/>
	 *	</srv:operatesOn>
     *	
     * @param md        iso19119 metadata
     * @param datasets  HashMap of datasets with uuids to be added
     *                   
     **/
    private Element addOperatesOnUuid(Element md, Map<String, String> datasets) {
        Element root = md.getChild("identificationInfo", gmd).getChild("SV_ServiceIdentification", srv);
        Element co = root.getChild("containsOperations", srv);
        if (root != null) {
            log.debug("  - add operatesOn with uuid and other attributes");
            for (String dsUuid : datasets.keySet()) {
                Element op = new Element("operatesOn", srv);
                op.setAttribute("uuidref", dsUuid);
                op.setAttribute("href", context.getBaseUrl() + "/srv/en/metadata.show?uuid=" + dsUuid, xlink);
                op.setAttribute("title", datasets.get(dsUuid), xlink);
                root.addContent(op);
            }
        }
        return md;
    }

    /** 
     * Validates metadata according to the schema.
     * 
     *  
     * @param schema 	Usually iso19139
     * @param md		Metadata to be validated
     * 
     * @return			true or false
     *                   
     **/
    private boolean validates(String schema, Element md) {
        try {
            dataMan.validate(schema, md);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 
     * Add categories according to harvesting configuration
     *   
     * @param id		GeoNetwork internal identifier
     * 
     **/
    private void addCategories(String id) throws Exception {
        for (String catId : params.getCategories()) {
            String name = localCateg.getName(catId);
            if (name == null) {
                log.debug("    - Skipping removed category with id:" + catId);
            } else {
                dataMan.setCategory(context, dbms, id, catId);
            }
        }
    }

    /** 
     * Add privileges according to harvesting configuration
     *   
     * @param id		GeoNetwork internal identifier
     * 
     **/
    private void addPrivileges(String id) throws Exception {
        for (Privileges priv : params.getPrivileges()) {
            String name = localGroups.getName(priv.getGroupId());
            if (name == null) {
                log.debug("    - Skipping removed group with id:" + priv.getGroupId());
            } else {
                for (int opId : priv.getOperations()) {
                    name = dataMan.getAccessManager().getPrivilegeName(opId);
                    if (opId == 0 || opId == 5 || opId == 6) {
                        dataMan.setOperation(context, dbms, id, priv.getGroupId(), opId + "");
                    } else {
                        log.debug("       --> " + name + " (skipped)");
                    }
                }
            }
        }
    }

    /** 
     * Determine whether dataset metadata should be harvested  
     *
     * @param ds     the dataset to be checked
     **/
    private boolean harvestMetadata(InvDataset ds) {
        if (isCollection(ds)) {
            return params.createCollectionDatasetMd && (params.ignoreHarvestOnCollections || ds.isHarvest());
        } else {
            return params.createAtomicDatasetMd && (params.ignoreHarvestOnAtomics || ds.isHarvest());
        }
    }

    /** 
     * Determine whether dataset metadata should be harvested using fragments  
     *
     * @param ds     the dataset to be checked
     **/
    private boolean harvestMetadataUsingFragments(InvDataset ds) {
        if (isCollection(ds)) {
            return params.collectionMetadataGeneration.equals(ThreddsParams.FRAGMENTS);
        } else {
            return params.atomicMetadataGeneration.equals(ThreddsParams.FRAGMENTS);
        }
    }

    /** 
	 * Determine whether dataset is a collection i.e. has nested datasets
     *
     * @param ds     the dataset to be checked
     **/
    private boolean isCollection(InvDataset ds) {
        return ds.hasNestedDatasets();
    }

    /** 
     * Get fragment harvesting parameters for collection datasets
     *   
     * @return		fragment harvesting parameters for collection datasets
     * 
     **/
    private FragmentParams getCollectionFragmentParams() {
        FragmentParams collectionParams = new FragmentHarvester.FragmentParams();
        collectionParams.categories = params.getCategories();
        collectionParams.createSubtemplates = params.createCollectionSubtemplates;
        collectionParams.isoCategory = params.datasetCategory;
        collectionParams.privileges = params.getPrivileges();
        collectionParams.templateId = params.collectionMetadataTemplate;
        collectionParams.url = params.url;
        collectionParams.uuid = params.uuid;
        collectionParams.outputSchema = params.outputSchemaOnCollectionsFragments;
        return collectionParams;
    }

    /** 
     * Get fragment harvesting parameters for atomic datasets
     *   
     * @return		fragment harvesting parameters for atomic datasets
     * 
     **/
    private FragmentParams getAtomicFragmentParams() {
        FragmentParams atomicParams = new FragmentHarvester.FragmentParams();
        atomicParams.categories = params.getCategories();
        atomicParams.createSubtemplates = params.createAtomicSubtemplates;
        atomicParams.isoCategory = params.datasetCategory;
        atomicParams.privileges = params.getPrivileges();
        atomicParams.templateId = params.atomicMetadataTemplate;
        atomicParams.url = params.url;
        atomicParams.uuid = params.uuid;
        atomicParams.outputSchema = params.outputSchemaOnAtomicsFragments;
        return atomicParams;
    }

    private Logger log;

    private ServiceContext context;

    private Dbms dbms;

    private ThreddsParams params;

    private DataManager dataMan;

    private SchemaManager schemaMan;

    private CategoryMapper localCateg;

    private GroupMapper localGroups;

    private UriMapper localUris;

    private ThreddsResult result;

    private String hostUrl;

    private HashSet<String> harvestUris = new HashSet<String>();

    private String cdmCoordsToIsoKeywordsStyleSheet;

    private String cdmCoordsToIsoMcpDataParametersStyleSheet;

    private String fragmentStylesheetDirectory;

    private String metadataGetService;

    private Map<String, ThreddsService> services = new HashMap();

    private InvCatalogImpl catalog;

    private FragmentHarvester atomicFragmentHarvester;

    private FragmentHarvester collectionFragmentHarvester;

    private class ThreddsService {

        public String uuid;

        public Map<String, String> datasets = new HashMap();

        public InvService service;

        public String version;

        public String ops;
    }

    ;

    private static final Namespace difNS = Namespace.getNamespace("http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/");

    private static final Namespace invCatalogNS = Namespace.getNamespace("http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0");

    private static final Namespace gmd = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");

    private static final Namespace gco = Namespace.getNamespace("gco", "http://www.isotc211.org/2005/gco");

    private static final Namespace srv = Namespace.getNamespace("srv", "http://www.isotc211.org/2005/srv");

    private static final Namespace xlink = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
}
