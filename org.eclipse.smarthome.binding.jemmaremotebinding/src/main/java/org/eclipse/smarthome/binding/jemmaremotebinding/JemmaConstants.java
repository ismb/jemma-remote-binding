/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.jemmaremotebinding;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link JemmaRemoteBindingBinding} class defines common constants, which
 * are used across the whole binding.
 *
 * @author Sandro Tassone - Initial contribution
 */
public class JemmaConstants
{

	public static final String BINDING_ID = "jemmaremotebinding";

	// List of all Thing Type UIDs
	public final static ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "jemma");
	public final static ThingTypeUID THING_TYPE_FLEXPLUG_A00500201 = new ThingTypeUID(BINDING_ID, "FlexPlug_A00500201");
	public final static ThingTypeUID THING_TYPE_UNKNOWN = new ThingTypeUID(BINDING_ID, "unknown");

	// List of all Channel ids
	public final static String CHANNEL_ONOFF = "onoff";
	public final static String APPLIANCE_ID="applianceId";
	public final static String APPLIANCE_MODEL_ID="applianceModelId";
	public final static String APPLIANCE_TYPE="applianceType";
	public final static String ADDRESS_KEY="address";
	public final static String PORT_KEY="port";
	public final static String USERNAME_KEY="username";
	public final static String PASSWORD_KEY="password";
		

}
