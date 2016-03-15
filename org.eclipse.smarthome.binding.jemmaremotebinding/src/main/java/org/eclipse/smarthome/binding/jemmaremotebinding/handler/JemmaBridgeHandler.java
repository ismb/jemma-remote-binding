package org.eclipse.smarthome.binding.jemmaremotebinding.handler;

import static org.eclipse.smarthome.binding.jemmaremotebinding.JemmaConstants.*;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.ismb.pert.jemma.jemmaDAL.Appliance;
import it.ismb.pert.jemma.jemmaDAL.Jemma;
import it.ismb.pert.jemma.jemmaDAL.Jemma.JemmaAuthenticator;
import it.ismb.pert.jemma.jemmaDAL.JemmaRPCManager;
import it.ismb.pert.jemma.jemmaDAL.JemmaRPCManager.JemmaCredentialsProvider;

public class JemmaBridgeHandler extends BaseThingHandler {

    private static final Logger logger = LoggerFactory.getLogger(JemmaBridgeHandler.class);
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_BRIDGE);

    private Set<ApplianceStatusListener> applianceStatusListeners = new HashSet<>();
    private Jemma jemma = null;

    public JemmaBridgeHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing JEMMA handler.");
        super.initialize();

        if (jemma != null)
            dispose();        

        Configuration configuration = getThing().getConfiguration();

        try {
            String address = (String) configuration.get(ADDRESS_KEY);
            Integer port = Integer.parseInt(((String) configuration.get(PORT_KEY)));
            final String username = (String) configuration.get(USERNAME_KEY);
            final String password = (String) configuration.get(PASSWORD_KEY);

            URI jemmaURI = new URIBuilder().setScheme("http").setHost(address).setPort(port).setPath("/demo/JSON-RPC")
                    .build();

            JemmaCredentialsProvider provider = new JemmaCredentialsProvider() {

                @Override
                public JemmaAuthenticator submitCredential(URI jemmaURI) {
                    if (username.length() > 0 && password.length() > 0) {
                        return new Jemma.JemmaAuthenticator(username, password);
                    }
                    return new Jemma.JemmaAuthenticator(); // default
                }
            };
            jemma = JemmaRPCManager.getInstance().buildJemma(jemmaURI, provider);
            if(jemma != null)
                broadcastApplianceStatus();
        } catch (Exception e) 
        {
            logger.error("Failed at creating JEMMA instance.");
        }
    }
    
    private void broadcastApplianceStatus()
    {      
        for(ApplianceStatusListener listener : applianceStatusListeners)
            dispatchApplianceStatus(listener);
                     
    }
    private void dispatchApplianceStatus(ApplianceStatusListener listener)
    {
        if(jemma != null)
            for(Appliance appliance : jemma.getAppliances())
                listener.onApplianceAdded(jemma, appliance);
    }

    public boolean registerApplianceStatusListener(ApplianceStatusListener listener) {
        if (listener == null) {
            throw new NullPointerException("Invalid null listener");
        }

        boolean result = this.applianceStatusListeners.add(listener);
        if (result)
            dispatchApplianceStatus(listener);

        return result;
    }

    public boolean unregisterApplianceStatusListener(ApplianceStatusListener listener) {
        boolean result = this.applianceStatusListeners.remove(listener);
        return result;
    }

    public Set<Appliance> getAppliances() {
        if(jemma != null)
            return jemma.getAppliances();
        return new HashSet<>();
    }

    public Appliance getApplianceById(String applianceId) 
    {
        if(jemma != null)
            return jemma.getAppliance(applianceId);
        return null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // nothing to do be done here
    }

    @Override
    public void dispose() {
        super.dispose();
        JemmaRPCManager.getInstance().removeJemma(jemma);
        jemma = null;
    }

}
