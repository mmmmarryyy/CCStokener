package com.migazzi.dm4j.repository.m2.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.migazzi.dm4j.cache.Cache;
import com.migazzi.dm4j.cache.CacheAccessException;
import com.migazzi.dm4j.common.Artifact;
import com.migazzi.dm4j.common.Dependency;
import com.migazzi.dm4j.common.Type;
import com.migazzi.dm4j.pom.CannotParsePomFileException;
import com.migazzi.dm4j.pom.PomDependenciesParser;
import com.migazzi.dm4j.repository.Repository;
import com.migazzi.dm4j.repository.RepositoryAccessException;
import com.migazzi.dm4j.spring.BeanInitializationException;
import com.migazzi.dm4j.spring.Factory;

/**
 * @author Pascal Migazzi implementation of the Maven 2 repository
 */
public class M2Repository implements Repository {

    private static final Log log = LogFactory.getLog(M2Repository.class);

    private static final int DOWNLOAD___BUFFER_SIZE = 1024;

    /**
     * base url of the repository.
     */
    private String baseUrl;

    /**
     * cache of this repository
     */
    private Cache cache;

    /**
     * expiration date of the local cache
     */
    private Date cacheExpirationDate;

    private static final SimpleDateFormat mavenDateFormateur = new SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.US);

    /**
     * constructor.
     * 
     * @param url
     *            the base url of the maven repository
     */
    public M2Repository(final String url) {
        if (!url.endsWith("/")) {
            baseUrl = url + "/";
        } else {
            baseUrl = url;
        }
        try {
            cache = Factory.getCache(this, baseUrl);
            final Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -1);
            cacheExpirationDate = cal.getTime();
        } catch (BeanInitializationException e) {
            log.warn("cannot initialize cache", e);
            cache = null;
            cacheExpirationDate = null;
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getArtifactNames(String organisationName) throws RepositoryAccessException {
        if (cache != null) {
            final Date d = cache.getArtifactNamesLastUpdate(organisationName);
            if (d.compareTo(cacheExpirationDate) >= 0) {
                return cache.getArtifactNames(organisationName);
            }
        }
        List<String> artifactNames = null;
        try {
            URL organisationUrl = new URL(baseUrl + organisationName.replaceAll("\\.", "/") + "/");
            artifactNames = getTokenList(organisationUrl);
        } catch (MalformedURLException e) {
            throw new RepositoryAccessException("cannot get artifact names for organisation " + organisationName, e);
        } catch (TokenisingException e) {
            throw new RepositoryAccessException("cannot get artifact names for organisation " + organisationName, e);
        }
        if (cache != null) {
            try {
                cache.updateArtifactNames(organisationName, artifactNames);
            } catch (CacheAccessException e) {
                log.warn("cannot update cache with artifact names", e);
            }
        }
        return artifactNames;
    }

    @Override
    public List<String> getArtifactVersions(String organisationName, String artifactName) throws RepositoryAccessException {
        if (cache != null) {
            final Date d = cache.getVersionsLastUpdate(organisationName, artifactName);
            if (d.compareTo(cacheExpirationDate) >= 0) {
                return cache.getVersions(organisationName, artifactName);
            }
        }
        List<String> versions = null;
        try {
            URL artifactUrl = new URL(baseUrl + organisationName.replaceAll("\\.", "/") + "/" + artifactName.replaceAll("\\.", "/") + "/");
            versions = getTokenList(artifactUrl);
        } catch (MalformedURLException e) {
            throw new RepositoryAccessException("cannot get artifact versions", e);
        } catch (TokenisingException e) {
            throw new RepositoryAccessException("cannot get artifact versions", e);
        }
        if (cache != null) {
            try {
                cache.updateVersions(organisationName, artifactName, versions);
            } catch (CacheAccessException e) {
                log.warn("cannot write artifact versions to cache", e);
            }
        }
        return versions;
    }

    @Override
    public List<Dependency> getDependencies(String organisationName, String artifactName, String version, Type type) throws RepositoryAccessException {
        if (cache != null) {
            try {
                Date d = cache.getDependenciesLastUpdate(organisationName, artifactName, version, type);
                if (d.compareTo(cacheExpirationDate) >= 0) {
                    return cache.getDependencies(organisationName, artifactName, version, type);
                }
            } catch (CacheAccessException e) {
                log.warn("cannot read cached informations", e);
            }
        }
        if (Type.JAR.equals(type)) {
            URL pomURL = null;
            try {
                pomURL = new URL(baseUrl + organisationName.replaceAll("\\.", "/") + "/" + artifactName.replaceAll("\\.", "/") + "/" + version + "/" + artifactName + "-" + version + ".pom");
            } catch (MalformedURLException e) {
                throw new RepositoryAccessException("cannot get dependencies for artifact " + artifactName, e);
            }
            PomDependenciesParser parser = Factory.getPomDependenciesParser();
            List<Dependency> dependencies;
            try {
                dependencies = parser.parse(pomURL.openStream());
            } catch (CannotParsePomFileException e) {
                throw new RepositoryAccessException(e);
            } catch (IOException e) {
                throw new RepositoryAccessException(e);
            }
            if (cache != null) {
                try {
                    cache.updateDependencies(organisationName, artifactName, version, type, dependencies);
                } catch (CacheAccessException e) {
                    log.warn("cannot update cached information", e);
                }
            }
            return dependencies;
        } else {
            return new ArrayList<Dependency>();
        }
    }

    @Override
    public List<Dependency> getDependencies(Artifact artifact) throws RepositoryAccessException {
        return getDependencies(artifact.getOrganisationName(), artifact.getName(), artifact.getVersion(), artifact.getType());
    }

    @Override
    public String download(Artifact artifact) throws RepositoryAccessException {
        String filename;
        if (cache == null) {
            try {
                filename = File.createTempFile(artifact.getName() + "-" + artifact.getVersion(), artifact.getType().toString()).getAbsolutePath();
            } catch (IOException e) {
                throw new RepositoryAccessException("cannot generate temp file", e);
            }
        } else {
            filename = cache.getArtifactLocalFilename(artifact);
            try {
                Date d = cache.getArtifactFileLastUpdate(artifact);
                if (d.compareTo(cacheExpirationDate) >= 0) {
                    return filename;
                } else {
                    Date serverFileModificationDate = getArtifactFileLastUpdate(artifact);
                    if (serverFileModificationDate.compareTo(d) <= 0) {
                        cache.updateArtifactFileTimeStamp(artifact);
                        return filename;
                    }
                }
            } catch (CacheAccessException e) {
                log.warn("cannot update cached information", e);
            }
        }
        String urlString = generateUrlString(artifact.getOrganisationName(), artifact.getName(), artifact.getVersion(), artifact.getType());
        if (urlString == null) {
            throw new RepositoryAccessException("cannot generate url for artifact :" + artifact);
        }
        URL pomURL = null;
        try {
            pomURL = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RepositoryAccessException("cannot download artifact " + artifact, e);
        }
        InputStream in = null;
        OutputStream out = null;
        Throwable t = null;
        try {
            byte[] buff = new byte[DOWNLOAD___BUFFER_SIZE];
            in = pomURL.openStream();
            out = new FileOutputStream(filename);
            int length = 0;
            while ((length = in.read(buff)) > 0) {
                out.write(buff, 0, length);
            }
        } catch (IOException e) {
            t = e;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.warn("cannot close stream", e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.warn("cannot close stream", e);
                }
            }
            if (t != null) {
                throw new RepositoryAccessException("cannot download artifact", t);
            }
        }
        if (cache != null) {
            try {
                cache.updateArtifactFileTimeStamp(artifact);
            } catch (CacheAccessException e) {
                log.warn("cannot update cached information", e);
            }
        }
        return filename;
    }

    private String generateUrlString(String organisationName, String name, String version, Type type) {
        String urlString = null;
        organisationName = organisationName.replaceAll("\\.", "/");
        name = name.replaceAll("\\.", "/");
        switch(type) {
            case JAR:
                urlString = baseUrl + organisationName + "/" + name + "/" + version + "/" + name + "-" + version + ".jar";
                break;
            case SRC:
                urlString = baseUrl + organisationName + "/" + name + "/" + version + "/" + name + "-" + version + "-sources.jar";
                break;
            case WAR:
                urlString = baseUrl + organisationName + "/" + name + "/" + version + "/" + name + "-" + version + ".war";
                break;
            case ZIP:
                urlString = baseUrl + organisationName + "/" + name + "/" + version + "/" + name + "-" + version + ".zip";
                break;
            default:
        }
        return urlString;
    }

    /**
     * retrieve the last modification date of the artifact from repository
     * 
     * @param artifact
     *            the tested artifact
     * @return the last modification date of the artifact or <code>null</code> if the last modification date cannot be retrieve;
     */
    private Date getArtifactFileLastUpdate(Artifact artifact) {
        URL url = null;
        try {
            url = new URL(baseUrl + artifact.getOrganisationName().replaceAll("\\.", "/") + "/" + artifact.getName().replaceAll("\\.", "/") + "/" + artifact.getVersion() + "/");
        } catch (MalformedURLException e) {
            log.warn("cannot retrieve last modifcation date", e);
            return null;
        }
        URLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = url.openConnection();
            inputStream = urlConnection.getInputStream();
        } catch (FileNotFoundException e) {
            log.warn("cannot retrieve last modifcation date", e);
            return null;
        } catch (IOException e) {
            log.warn("cannot retrieve last modifcation date", e);
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            log.warn("cannot retrieve last modifcation date", e);
            return new Date(0);
        }
        Pattern pattern = Pattern.compile("<a href=\"" + artifact.getName() + "-" + artifact.getVersion() + ".jar\">" + artifact.getName() + "-" + artifact.getVersion() + ".jar</a> *(\\d{2}-[a-zA-Z]{3}-\\d{4} \\d{2}:\\d{2})");
        Matcher m = pattern.matcher(buffer);
        if (m.find()) {
            String dateStr = m.group(1);
            try {
                return mavenDateFormateur.parse(dateStr);
            } catch (ParseException e) {
                log.warn("cannot retrieve last modifcation date", e);
                return new Date(0);
            }
        }
        log.warn("cannot retrieve last modifcation date");
        return new Date(0);
    }

    @Override
    public List<Type> getArtifactTypes(String organisationName, String artifactName, String version) {
        if (cache != null) {
            Date d;
            try {
                d = cache.getTypesLastUpdate(organisationName, artifactName, version);
                if (d.compareTo(cacheExpirationDate) >= 0) {
                    return cache.getTypes(organisationName, artifactName, version);
                }
            } catch (CacheAccessException e) {
                log.warn("cannot access cache", e);
            }
        }
        List<Type> types = new ArrayList<Type>();
        String urlString = generateUrlString(organisationName, artifactName, version, Type.JAR);
        try {
            new URL(urlString).openStream();
            types.add(Type.JAR);
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        urlString = generateUrlString(organisationName, artifactName, version, Type.SRC);
        try {
            new URL(urlString).openStream();
            types.add(Type.SRC);
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        urlString = generateUrlString(organisationName, artifactName, version, Type.WAR);
        try {
            new URL(urlString).openStream();
            types.add(Type.WAR);
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        urlString = generateUrlString(organisationName, artifactName, version, Type.ZIP);
        try {
            new URL(urlString).openStream();
            types.add(Type.ZIP);
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        if (cache != null) {
            try {
                cache.updateTypes(organisationName, artifactName, version, types);
            } catch (CacheAccessException e) {
                log.warn("cannot access cache", e);
            }
        }
        return types;
    }

    @Override
    public List<String> getOrganisationNames() throws RepositoryAccessException {
        if (cache != null) {
            Date d = cache.getOrganisationNamesLastUpdate();
            if (d.compareTo(cacheExpirationDate) >= 0) {
                return cache.getOrganisationNames();
            }
        }
        List<String> organisationNames = null;
        try {
            URL url = new URL(baseUrl);
            organisationNames = getTokenList(url);
        } catch (MalformedURLException e) {
            throw new RepositoryAccessException("cannot get organisation names", e);
        } catch (TokenisingException e) {
            throw new RepositoryAccessException("cannot get organisation names", e);
        }
        if (cache != null) {
            try {
                cache.updateOrganisationNames(organisationNames);
            } catch (CacheAccessException e) {
                log.warn("cannot update cached information");
            }
        }
        return organisationNames;
    }

    /**
     * return a list of token which are corresponding to all link present in the page excepted ./ and ../ which are not representing child element. This method is used because the metadata of maven
     * repository are not up-to-date.
     * 
     * @param url
     *            the url to parse
     * @return the list of token extracted
     * @throws TokenisingException
     */
    private List<String> getTokenList(URL url) throws TokenisingException {
        URLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = url.openConnection();
            inputStream = urlConnection.getInputStream();
        } catch (FileNotFoundException e) {
            return new ArrayList<String>();
        } catch (IOException e) {
            throw new TokenisingException("cannot retrieve tokens", e);
        }
        StringBuffer buffer = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            throw new TokenisingException("cannot retrieve tokens", e);
        }
        Pattern pattern = Pattern.compile("<a href=\"([^\"]*)/\">[^<]*</a>");
        Matcher m = pattern.matcher(buffer);
        List<String> result = new ArrayList<String>();
        while (m.find()) {
            String value = m.group(1);
            if (!"..".equals(value)) {
                result.add(value);
            }
        }
        return result;
    }

    @Override
    public String getSourcesFileExtension() {
        return "jar";
    }

    @Override
    public boolean exists(Artifact artifact) {
        return exists(artifact.getOrganisationName(), artifact.getName(), artifact.getVersion(), artifact.getType());
    }

    @Override
    public boolean exists(String organisationName, String artifactName, String version, Type type) {
        return getArtifactTypes(organisationName, artifactName, version).contains(type);
    }
}
