package com.rif.client.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ServiceLoader;
import com.rif.client.service.impl.ServiceClientImpl;
import com.rif.client.service.parser.ClientModelParserFactory;
import com.rif.client.service.parser.IClientModelParser;
import com.rif.common.serializer.DataSerializerManager;
import com.rif.common.serializer.IDataSerializer;

/**
 * @author bruce.liu (mailto:jxta.liu@gmail.com)
 * 2011-7-28 下午11:00:08
 */
public class ServiceClientFactory {

    private String[] configPaths;

    private IServiceClient serviceClient;

    static {
        ServiceLoader<IDataSerializer> dataSerializerLoader = ServiceLoader.load(IDataSerializer.class);
        for (IDataSerializer dataSerializer : dataSerializerLoader) {
            DataSerializerManager.INSTANCE.regiest(dataSerializer);
        }
    }

    public ServiceClientFactory() {
    }

    public ServiceClientFactory(String[] configPaths) {
        this.configPaths = configPaths;
    }

    public IServiceClient getServiceClient() {
        return this.serviceClient;
    }

    public void init() {
        IClientModelParser parser = ClientModelParserFactory.INSTANCE.createParser();
        InputStream[] resources = new InputStream[this.configPaths.length];
        URL url = null;
        int i = 0;
        for (String configPath : configPaths) {
            try {
                url = new URL(configPath);
                resources[i] = url.openStream();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            i++;
        }
        parser.setResources(resources);
        parser.parser();
        this.serviceClient = new ServiceClientImpl();
    }

    public String[] getConfigPaths() {
        return configPaths;
    }

    public void setConfigPaths(String[] configPaths) {
        this.configPaths = configPaths;
    }
}
