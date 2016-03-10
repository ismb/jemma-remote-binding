/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.jemmaremotebinding.handler;

import static org.eclipse.smarthome.binding.jemmaremotebinding.JemmaRemoteBindingBindingConstants.COMMAND_CHANNEL;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link JemmaRemoteBindingHandler} is responsible for handling commands,
 * which are sent to one of the channels.
 *
 * @author Sandro Tassone - Initial contribution
 */
public class JemmaRemoteBindingHandler extends BaseThingHandler implements JemmaRPCManager.JemmaCredentialsProvider
{

	private Logger logger = LoggerFactory.getLogger(JemmaRemoteBindingHandler.class);
	private JemmaRPCManager jemmaRPCManager = null;
	private List<URI> jemmaURIs = null;

	{
		jemmaURIs = new ArrayList<URI>();
		try
		{
			jemmaURIs.add(new URI("http://localhost:8080/demo/JSON-RPC"));
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
		}
	}

	public JemmaRemoteBindingHandler(Thing thing)
	{
		super(thing);
		jemmaRPCManager = new JemmaRPCManager(jemmaURIs, this);
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command)
	{
		if (channelUID.getId().equals(COMMAND_CHANNEL))
			if (command instanceof OnOffType)
			{
				List<Object> params = new ArrayList<Object>();
				for (Appliance appliance : jemmaRPCManager.getAllAppliances())
				{
					params.clear();
					params.add(appliance.getAppliancePid());
					switch ((OnOffType) command)
					{
					case ON:
						params.add(1);
						break;
					case OFF:
						params.add(0);
						break;
					}

					try
					{
						if (appliance.canDoThis(Appliance.ApplianceAction.OnOffServer))
							appliance.performAction(Appliance.ApplianceAction.OnOffServer, params);
					}
					catch (UnsupportedActionException e)
					{
						e.printStackTrace();
					}
				}
			}
	}

	@Override
	public void initialize()
	{
		// TODO: Initialize the thing. If done set status to ONLINE to indicate
		// proper working.
		// Long running initialization should be done asynchronously in
		// background.
		updateStatus(ThingStatus.ONLINE);

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

	@Override
	public JemmaAuthenticator submitCredential(URI jemmaURI)
	{
		// TODO Auto-generated method stub
		return new JemmaAuthenticator();
	}
}
