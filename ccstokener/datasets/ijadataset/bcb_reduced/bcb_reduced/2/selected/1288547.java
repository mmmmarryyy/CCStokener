package org.personalsmartspace.spm.policy.impl.policyGeneration.provider;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.personalsmartspace.spm.negotiation.api.platform.RequestItem;
import org.personalsmartspace.spm.negotiation.api.platform.RequestPolicy;
import org.personalsmartspace.spm.policy.api.platform.IPolicyManager;
import org.personalsmartspace.spm.policy.impl.ServiceRetriever;
import org.personalsmartspace.sre.ems.api.pss3p.EventListener;
import org.personalsmartspace.sre.ems.api.pss3p.IEventMgr;
import org.personalsmartspace.sre.ems.api.pss3p.PSSEvent;
import org.personalsmartspace.sre.ems.api.pss3p.PeerEvent;
import org.personalsmartspace.sre.ems.api.pss3p.PeerEventTypes;
import org.personalsmartspace.sre.slm.api.platform.EventServiceInfo;
import org.personalsmartspace.sre.slm.api.pss3p.ServiceState;

/**
 * class that registers and listens for SLM service deployed events and extracts the privacy policy from the bundle of the service that just started
 * @author Elizabeth
 *
 */
public class PolicyRetriever extends EventListener {

    private BundleContext context;

    private ServiceRetriever sr;

    private IPolicyManager policyMgr;

    public PolicyRetriever(BundleContext bc, IPolicyManager polMgr) {
        this.context = bc;
        this.sr = new ServiceRetriever(this.context);
        this.subscribe();
        this.policyMgr = polMgr;
    }

    @Override
    public void handlePSSEvent(PSSEvent arg0) {
    }

    private void subscribe() {
        IEventMgr eventMgr = this.getEventManager();
        String[] trackedEventTypes = new String[] { PeerEventTypes.SERVICE_LIFECYCLE_EVENT, PeerEventTypes.PEER_INITIALISED };
        String[] eventTypes = new String[] { PeerEventTypes.SERVICE_LIFECYCLE_EVENT };
        eventMgr.registerListener(this, eventTypes, null);
    }

    private IEventMgr getEventManager() {
        return (IEventMgr) this.sr.getService(IEventMgr.class.getName());
    }

    @Override
    public void handlePeerEvent(PeerEvent event) {
        if (event.geteventInfo() instanceof EventServiceInfo) {
            EventServiceInfo info = (EventServiceInfo) event.geteventInfo();
            if (info.getServiceState() != ServiceState.Deployed) return;
            long bid = info.getBundleId();
            Bundle bundle = context.getBundle(bid);
            Enumeration entries = bundle.findEntries("OSGI-INF/PrivacyPolicy/", "*.xml", true);
            if (entries != null) {
                if (entries.hasMoreElements()) {
                    try {
                        URL url = (URL) entries.nextElement();
                        BufferedInputStream in = new BufferedInputStream(url.openStream());
                        XMLPolicyReader reader = new XMLPolicyReader(this.context);
                        RequestPolicy policy = reader.readPolicyFromFile(in);
                        if (policy != null) {
                            this.policyMgr.addPrivacyPolicyForService(info.getServiceID(), policy);
                        }
                    } catch (IOException ioe) {
                    }
                }
            }
        }
    }
}
