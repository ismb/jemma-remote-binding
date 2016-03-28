
package org.eclipse.smarthome.binding.jemmaremotebinding.handler;

import static org.eclipse.smarthome.binding.jemmaremotebinding.JemmaConstants.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.ismb.pert.jemma.jemmaDAL.Appliance;
import it.ismb.pert.jemma.jemmaDAL.Jemma;
import it.ismb.pert.jemma.jemmaDAL.Jemma.Authenticator;
import it.ismb.pert.jemma.jemmaDAL.JemmaRPCManager;
import it.ismb.pert.jemma.jemmaDAL.JemmaRPCManager.JemmaCredentialsProvider;

public class JemmaBridgeHandler extends BaseThingHandler {

    private static final Logger logger = LoggerFactory.getLogger(JemmaBridgeHandler.class);
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_BRIDGE);
    private static final int POLLING_INTERVAL = 10;

    private Set<ApplianceStatusListener> applianceStatusListeners = new HashSet<>();
    private Jemma jemma = null;
    private Set<Appliance> appliances = null;
    private URI jemmaURI = null;
    private ScheduledFuture<?> scheduledJob = null;

    public JemmaBridgeHandler(Thing thing) {
        super(thing);
        logger.debug("Initializing JEMMA handler.");

        try {
            jemma = getJemma();

        } catch (Exception e) {
            logger.error("Failed at creating JEMMA instance.");
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        updateStatus(ThingStatus.ONLINE);
        scheduledJob = scheduler.scheduleAtFixedRate(poller, 1, POLLING_INTERVAL, TimeUnit.SECONDS);
    }

    private void broadcastApplianceStatus(Set<Appliance> appliances, ThingStatus status) {
        for (ApplianceStatusListener listener : applianceStatusListeners) {
            dispatchApplianceStatus(listener, appliances, status);
        }
    }

    private void dispatchApplianceStatus(ApplianceStatusListener listener, Set<Appliance> appliances,
            ThingStatus status) {
        for (Appliance appliance : appliances) {
            switch (status) {
                case ONLINE:
                    listener.onApplianceAdded(jemma, appliance);
                    break;
                case OFFLINE:
                    listener.onApplianceRemoved(jemma, appliance);
                    break;
            }
        }
    }

    public boolean registerApplianceStatusListener(ApplianceStatusListener listener) {
        if (listener == null) {
            throw new NullPointerException("Invalid null listener");
        }

        boolean result = this.applianceStatusListeners.add(listener);
        if (result) {
            dispatchApplianceStatus(listener, getAppliances(), ThingStatus.ONLINE);
        }

        return result;
    }

    public boolean unregisterApplianceStatusListener(ApplianceStatusListener listener) {
        boolean result = this.applianceStatusListeners.remove(listener);
        return result;
    }

    private synchronized Jemma getJemma() {
        Jemma localJemma = jemma;
        if (localJemma == null) {
            final Configuration configuration = getThing().getConfiguration();
            final String username = (String) configuration.get(USERNAME_KEY);
            final String password = (String) configuration.get(PASSWORD_KEY);

            JemmaCredentialsProvider provider = new JemmaCredentialsProvider() {

                @Override
                public Authenticator submitCredential(URI jemmaURI) {
                    if (username.length() > 0 && password.length() > 0) {
                        return new Authenticator(username, password);
                    }
                    return new Authenticator(); // default
                }
            };
            localJemma = JemmaRPCManager.getInstance().getJemma(getJemmaURI(), provider);
            if (localJemma != null) {
                jemma = localJemma;
                broadcastApplianceStatus(getAppliances(), ThingStatus.ONLINE);
            }
        }

        return localJemma;
    }

    private URI getJemmaURI() {

        if (jemmaURI == null) {
            Configuration configuration = getThing().getConfiguration();

            try {
                String address = (String) configuration.get(ADDRESS_KEY);
                BigDecimal port = (BigDecimal) configuration.get(PORT_KEY);

                jemmaURI = new URIBuilder().setScheme("http").setHost(address).setPort(port.intValue())
                        .setPath("/demo/JSON-RPC").build();
            } catch (URISyntaxException e) {
            }
        }
        return jemmaURI;
    }

    public Set<Appliance> getAppliances() {
        Jemma localJemma = getJemma();
        if (localJemma != null) {
            return localJemma.getAppliances();
        }
        return new TreeSet<>();
    }

    public Appliance getApplianceById(String applianceId) {
        Jemma localJemma = getJemma();
        if (localJemma != null) {
            return localJemma.getAppliance(applianceId);
        }
        return null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // nothing to do be done here
    }

    private void updateAppliancesStatus(Set<Appliance> appliances) {
        for (Appliance appliance : appliances) {
            appliance.sync();
        }

    }

    @Override
    public void dispose() {
        super.dispose();
        JemmaRPCManager.getInstance().removeJemma(jemma);
        if (scheduledJob != null) {
            scheduledJob.cancel(false);
        }
        jemma = null;
        jemma = null;
        updateStatus(ThingStatus.OFFLINE);
    }

    private Runnable poller = new Runnable() {

        @Override
        public void run() {
            Jemma localJemma = getJemma();
            if (localJemma != null) {
                localJemma.refresh();

                Set<Appliance> remoteAppliances = getAppliances();

                Set<Appliance> localAppliances;
                // removed appliances
                if (appliances == null) {
                    appliances = new TreeSet<>();
                }

                localAppliances = new TreeSet<>(appliances);
                localAppliances.removeAll(remoteAppliances);
                broadcastApplianceStatus(localAppliances, ThingStatus.REMOVED);

                // new appliances
                localAppliances = new TreeSet<>(remoteAppliances);
                localAppliances.removeAll(appliances);
                broadcastApplianceStatus(localAppliances, ThingStatus.ONLINE);

                appliances.addAll(remoteAppliances);

                // set appliance status
                updateAppliancesStatus(appliances);
                logger.debug("Synchronization for jemma done");
            }

        }

    };

}
