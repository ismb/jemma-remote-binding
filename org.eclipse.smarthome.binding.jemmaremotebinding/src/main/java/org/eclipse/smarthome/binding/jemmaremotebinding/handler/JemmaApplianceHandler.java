/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.jemmaremotebinding.handler;

import static org.eclipse.smarthome.binding.jemmaremotebinding.JemmaConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import it.ismb.pert.jemma.jemmaDAL.Appliance;
import it.ismb.pert.jemma.jemmaDAL.Appliance.UnsupportedActionException;
import it.ismb.pert.jemma.jemmaDAL.Jemma;


/**
 * The {@link JemmaApplianceHandler} is responsible for handling commands, which
 * are sent to one of the channels.
 *
 * @author Sandro Tassone - Initial contribution
 */
public class JemmaApplianceHandler extends BaseThingHandler
implements ApplianceStatusListener
{

    private Logger logger = LoggerFactory.getLogger(JemmaApplianceHandler.class);    
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_FLEXPLUG_A00500201, THING_TYPE_UNKNOWN);
    private String applianceId;
    private Appliance appliance = null;   

    public JemmaApplianceHandler(Thing thing)
    {
        super(thing);        
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command)
    {
        
        switch(channelUID.getId())
        {
            case CHANNEL_ONOFF:
                if(command instanceof OnOffType)
                    performOnOffAction((OnOffType)command);                
                break;
        }
    }
    
    private void performOnOffAction(OnOffType onOffType)
    {
        List<Object> params = new ArrayList<>();
        params.add(appliance.getId());
        if(onOffType == OnOffType.ON)
            params.add(1);
        else
            params.add(0);
        try
        {
            appliance.performAction(Appliance.ApplianceAction.OnOffServer,params);
        }
        catch(UnsupportedActionException e)
        {
            logger.info("Cannot perform action {} of type OnOffType on appliance {}",onOffType, appliance.getId());
        }
    }
            
    

    @Override
    public void initialize()
    {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate
        // proper working.
        // Long running initialization should be done asynchronously in
        // background.
        
        
        JemmaBridgeHandler jemmaHandler = getBridgeHandler();
        
        if(jemmaHandler != null)
        {            
        
            String applianceId = (String)getConfig().get(APPLIANCE_ID);
            appliance = jemmaHandler.getApplianceById(applianceId);        
            updateStatus(ThingStatus.ONLINE);
        }        

        // Note: When initialization can NOT be done set the status with more
        // details for further
        // analysis. See also class ThingStatusDetail for all available status
        // details.
        // Add a description to give user information to understand why thing
        // does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE,
        // ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }
        
    
    private JemmaBridgeHandler getBridgeHandler()
    {
        Bridge bridge;
        if((bridge = getBridge()) == null)
            return null;
        
        ThingHandler handler = bridge.getHandler();
        if(handler instanceof JemmaBridgeHandler)
            return (JemmaBridgeHandler)handler;
        return null;        
    }
 
    @Override
    public void onApplianceAdded(Jemma jemma, Appliance appliance)
    {
        if (appliance.getId().equals(applianceId))
        {
            updateStatus(ThingStatus.ONLINE);
            onApplianceStatusChanged(jemma, appliance);
        }
    }

    @Override
    public void onApplianceRemoved(Jemma jemma, Appliance appliance)
    {
        if (appliance.getId().equals(applianceId))
        {
            updateStatus(ThingStatus.OFFLINE);
        }

    }
    
    public static String getTypeId(Appliance appliance)
    {
        //TODO This is a temporary solution waiting for the actual module dedicated to appliance model derivation to be implemented   
        switch(appliance.getType())
        {
            case AH_EP_ZIGBEE_SMARTPLUG:
                return THING_TYPE_FLEXPLUG_A00500201.getId();
            case UNKNOWN_APPLIANCE_TYPE:
                return THING_TYPE_UNKNOWN.getId();                
        }
        return THING_TYPE_FLEXPLUG_A00500201.getId();
    }

    @Override
    public void onApplianceStatusChanged(Jemma jemma, Appliance appliance)
    {
        // TODO Auto-generated method stub

    }
}