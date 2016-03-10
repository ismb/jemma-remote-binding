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
public class JemmaRemoteBindingBindingConstants
{

	public static final String BINDING_ID = "jemmaremotebinding";

	// List of all Thing Type UIDs
	public final static ThingTypeUID JEMMA_RPC_MANAGER_THING = new ThingTypeUID(BINDING_ID, "jemma_rpc_manager");

	// List of all Channel ids
	public final static String COMMAND_CHANNEL = "command";

}
