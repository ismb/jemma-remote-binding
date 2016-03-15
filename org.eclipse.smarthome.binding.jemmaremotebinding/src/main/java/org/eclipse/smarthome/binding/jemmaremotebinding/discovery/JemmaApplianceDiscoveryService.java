package org.eclipse.smarthome.binding.jemmaremotebinding.discovery;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.binding.jemmaremotebinding.JemmaConstants;
import org.eclipse.smarthome.binding.jemmaremotebinding.handler.ApplianceStatusListener;
import org.eclipse.smarthome.binding.jemmaremotebinding.handler.JemmaApplianceHandler;
import org.eclipse.smarthome.binding.jemmaremotebinding.handler.JemmaBridgeHandler;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.ismb.pert.jemma.jemmaDAL.Appliance;
import it.ismb.pert.jemma.jemmaDAL.Jemma;

public class JemmaApplianceDiscoveryService extends AbstractDiscoveryService implements ApplianceStatusListener
{
    private static final Logger logger = LoggerFactory.getLogger(JemmaApplianceDiscoveryService.class);
    private static final int TIMEOUT = 60;
    private JemmaBridgeHandler jemmaBridgeHandler;	

    public JemmaApplianceDiscoveryService(JemmaBridgeHandler jemmaBridgeHandler) throws IllegalArgumentException
    {
        super(TIMEOUT);
        this.jemmaBridgeHandler = jemmaBridgeHandler;
    }

    public void activate()
    {
        jemmaBridgeHandler.registerApplianceStatusListener(this);
    }

    public void deactivate()
    {
        removeOlderResults(new Date().getTime());
        jemmaBridgeHandler.unregisterApplianceStatusListener(this);
    }

    @Override
    protected void startScan()
    {	
        //Separate thread of execution 
        for(Appliance appliance : jemmaBridgeHandler.getAppliances())
            onApplianceAddedInternal(appliance);		
    }

    @Override
    protected synchronized void stopScan() 
    {
        super.stopScan();
        removeOlderResults(new Date().getTime());
    }        

    private void onApplianceAddedInternal(Appliance appliance)
    {
        ThingUID thingUID = getThingUID(appliance);
        if(thingUID != null)
        {
            ThingUID bridgeUID = jemmaBridgeHandler.getThing().getUID();
            Map<String,Object> properties = new HashMap<>(1);
            properties.put(JemmaConstants.APPLIANCE_ID, appliance.getId());            

            DiscoveryResult result = DiscoveryResultBuilder
                    .create(thingUID)
                    .withBridge(bridgeUID)
                    .withProperties(properties)
                    .withLabel(appliance.getName())
                    .build();

            thingDiscovered(result);
        }
        else
        {
            logger.debug("discovered unsupported appliance id {}", appliance.getId());
        }		
    }



    @Override
    public void onApplianceAdded(Jemma jemma, Appliance appliance)
    {
        onApplianceAddedInternal(appliance);

    }

    @Override
    public void onApplianceRemoved(Jemma jemma, Appliance appliance)
    {
        ThingUID thingUID = getThingUID(appliance);
        if(thingUID != null)
            thingRemoved(thingUID);
    }

    @Override
    public void onApplianceStatusChanged(Jemma jemma, Appliance appliance)
    {
    }

    private ThingUID getThingUID(Appliance appliance)
    {
        String typeId = JemmaApplianceHandler.getTypeId(appliance);
        ThingUID bridgeUID = jemmaBridgeHandler.getThing().getUID();
        ThingTypeUID thingTypeUID = new ThingTypeUID(JemmaConstants.BINDING_ID, typeId);

        if(JemmaApplianceHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID))
        {
            String applianceId = appliance.getId().replaceAll("[^a-zA-Z0-9_]", "_");
            ThingUID thingUID = new ThingUID(thingTypeUID, bridgeUID,  applianceId);
            return thingUID;
        }
        return null;        
    }

}
