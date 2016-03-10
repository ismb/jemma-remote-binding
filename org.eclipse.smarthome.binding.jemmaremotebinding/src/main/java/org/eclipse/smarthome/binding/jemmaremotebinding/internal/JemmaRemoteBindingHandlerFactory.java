/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.jemmaremotebinding.internal;

import static org.eclipse.smarthome.binding.jemmaremotebinding.JemmaRemoteBindingBindingConstants.JEMMA_RPC_MANAGER_THING;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.binding.jemmaremotebinding.handler.JemmaRemoteBindingHandler;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;

/**
 * The {@link JemmaRemoteBindingHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Sandro Tassone - Initial contribution
 */
public class JemmaRemoteBindingHandlerFactory extends BaseThingHandlerFactory {

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(JEMMA_RPC_MANAGER_THING);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(JEMMA_RPC_MANAGER_THING)) {
            return new JemmaRemoteBindingHandler(thing);
        }

        return null;
    }
}
