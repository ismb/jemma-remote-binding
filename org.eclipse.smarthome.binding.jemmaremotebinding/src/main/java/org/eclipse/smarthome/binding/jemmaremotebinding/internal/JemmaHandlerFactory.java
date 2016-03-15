/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.jemmaremotebinding.internal;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.binding.jemmaremotebinding.discovery.JemmaApplianceDiscoveryService;
import org.eclipse.smarthome.binding.jemmaremotebinding.handler.JemmaApplianceHandler;
import org.eclipse.smarthome.binding.jemmaremotebinding.handler.JemmaBridgeHandler;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.osgi.framework.ServiceRegistration;

import com.google.common.collect.Sets;

/**
 * The {@link JemmaHandlerFactory} is responsible for creating
 * things and thing handlers.
 *
 * @author Sandro Tassone - Initial contribution
 */
public class JemmaHandlerFactory extends BaseThingHandlerFactory
{

	private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets
			.union(JemmaBridgeHandler.SUPPORTED_THING_TYPES, JemmaApplianceHandler.SUPPORTED_THING_TYPES);

	private Map<ThingUID, ServiceRegistration<?>> regServices = new HashMap<>();

	@Override
	public boolean supportsThingType(ThingTypeUID thingTypeUID)
	{
		return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
	}

	@Override
	protected ThingHandler createHandler(Thing thing)
	{
		ThingTypeUID thingTypeUID = thing.getThingTypeUID();

		if (JemmaApplianceHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID))		
			return new JemmaApplianceHandler(thing);		
		else if (JemmaBridgeHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID))
		{
			JemmaBridgeHandler handler = new JemmaBridgeHandler(thing);
			registerApplianceDiscoveryService(handler);
			return handler;
		}
		return null;
	}

	protected void registerApplianceDiscoveryService(JemmaBridgeHandler jemmaBridgeJandler)
	{
		JemmaApplianceDiscoveryService discoveryService = new JemmaApplianceDiscoveryService(jemmaBridgeJandler);
		discoveryService.activate();
		this.regServices.put(jemmaBridgeJandler.getThing().getUID(), bundleContext
				.registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
	}
	
	@Override
	protected void removeHandler(ThingHandler thingHandler) 
	{
	    if(thingHandler instanceof JemmaBridgeHandler)
	    {
	        ServiceRegistration<?> discoveryServiceReg = this.regServices.get(thingHandler.getThing().getUID());
	        if(discoveryServiceReg != null)
	        {
	            JemmaApplianceDiscoveryService discoveryService = (JemmaApplianceDiscoveryService)bundleContext.getService(discoveryServiceReg.getReference());
	            discoveryService.deactivate();
	            discoveryServiceReg.unregister();
	            this.regServices.remove(discoveryServiceReg);
	        }	        
	    }	    
	}
}
