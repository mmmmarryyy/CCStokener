package org.slasoi.gslam.templateregistry;

import java.io.IOException;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slasoi.gslam.core.context.SLAManagerContext;
import org.slasoi.gslam.core.negotiation.SLATemplateRegistry;
import org.slasoi.gslam.core.negotiation.ISyntaxConverter;
import org.slasoi.slamodel.primitives.UUID;
import org.slasoi.slamodel.service.Interface.Specification;

class __ref_resolver implements SLATemplateRegistry.ReferenceResolver {

    private SLAManagerContext context = null;

    __ref_resolver(SLAManagerContext context) throws Exception {
        this.context = context;
    }

    public Specification getInterfaceSpecification(UUID location) {
        if (context != null) try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(location.getValue());
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String wsdlString = EntityUtils.toString(entity);
                Map<ISyntaxConverter.SyntaxConverterType, ISyntaxConverter> scs = context.getSyntaxConverters();
                ISyntaxConverter syntaxConverter = scs.get(ISyntaxConverter.SyntaxConverterType.SLASOISyntaxConverter);
                return syntaxConverter.parseWSDL(wsdlString)[0];
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
