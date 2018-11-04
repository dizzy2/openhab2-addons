/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.bvfheating.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link BvfHeatingBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author dizzy - Initial contribution
 */
@NonNullByDefault
public class BvfHeatingBindingConstants {

    private static final String BINDING_ID = "bvfheating";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_ZONEBOX = new ThingTypeUID(BINDING_ID, "zonebox");

    // configuration parameters
    public static final String CONFIG_URL = "url";
    public static final String CONFIG_ACTIVE_ROOMS = "activeRooms";
    public static final String CONFIG_HTTP_POLLING_INTERVAL = "httpPollingInterval";

    // all channel groups (rooms)
    public static final String[] CHANNEL_GROUPS_ROOMS = { "room1", "room2", "room3", "room4", "room5", "room6", "room7", "room8" };
    public static final Map<String, Integer> CHANNEL_GROUP2INDEX;

    static {
	HashMap<String, Integer> room2index = new HashMap<>(CHANNEL_GROUPS_ROOMS.length);
	for(int i = 0; i < CHANNEL_GROUPS_ROOMS.length; i++) {
	    room2index.put(CHANNEL_GROUPS_ROOMS[i], i);
	}
	CHANNEL_GROUP2INDEX = Collections.unmodifiableMap(room2index);
    }

    // List of all Channel ids
    public static final String CHANNEL_ONOFF = "on_off";
    public static final String CHANNEL_TEMP = "temp";
    public static final String CHANNEL_SP_TEMP = "sp_temp";
    public static final String CHANNEL_MODE = "mode";
}
