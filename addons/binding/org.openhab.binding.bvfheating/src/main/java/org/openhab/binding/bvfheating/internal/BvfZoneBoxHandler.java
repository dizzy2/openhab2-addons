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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.bvfheating.internal.zonebox.ThresholdZoneBoxClient;
import org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxClient;
import org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BvfZoneBoxHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author dizzy - Initial contribution
 */
@NonNullByDefault
public class BvfZoneBoxHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(BvfZoneBoxHandler.class);

    private final HttpClient httpClient;

    private Optional<ZoneBoxClient> zoneBoxClient;
    private Optional<ScheduledFuture<?>> pollingTickFuture;
    private boolean isOnline = false;

    private class PollingTick implements Runnable {
        private final Iterable<Integer> rooms;
        private Iterator<Integer> roomIter;

        /**
         *
         */
        public PollingTick(Iterable<Integer> rooms) {
            this.rooms = rooms;
            this.roomIter = rooms.iterator();
        }

        @Override
        public void run() {
            RoomStatus roomStatus = new RoomStatus();
            if (!roomIter.hasNext()) {
                roomIter = rooms.iterator();
            }
            final int roomNr = roomIter.next() - 1;
            zoneBoxClientTransaction(zoneBoxClient ->
            	zoneBoxClient.rForm(roomNr, roomStatus.zoneboxResponseHandler), "polling tick, room ", roomNr);

            isOnline = roomStatus.error.map(err -> {
            	updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, err.toString());
            	return false;
            }).orElseGet(() -> {
                roomStatus.apply2handler(thing.getUID(), (channelID, state) -> updateState(channelID, state));
                if (!isOnline) {
                    updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "");
        		}
            	return true;
            });
        }
    }

    public BvfZoneBoxHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
        this.zoneBoxClient = Optional.empty();
        this.pollingTickFuture = Optional.empty();
    }

    private boolean zoneBoxClientTransaction(Consumer<ZoneBoxClient> transaction, String transactionName, Object prm) {
        return zoneBoxClient.map(zoneBoxClient -> {
            synchronized (zoneBoxClient) {
                logger.debug("{}: ZoneBoxClient transaction start: {} {}", this, transactionName, prm);
                transaction.accept(zoneBoxClient);
            }
            logger.debug("{}: ZoneBoxClient transaction finished: {} {}", this, transactionName, prm);
            return true;
        }).orElseGet(() -> {
            logger.error("{}: Illegal state - ZoneBoxClient is not initialized yet");
            return false;
        });
    }


    private static Optional<Integer> atoi(String number) {
        try {
            return Optional.of(Integer.parseInt(number));
        } catch (NumberFormatException exc) {
            return Optional.empty();
        }
    }


    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);
        this.isOnline = false;

        logger.debug("{}: dropping previous tick handler (if any)", this);
    	pollingTickFuture.ifPresent(pollingTick -> {
    	    if(!pollingTick.isDone()) {
    	        pollingTick.cancel(false);
    	    }
    	});
    	pollingTickFuture = Optional.empty();

    	logger.debug("{}: parsing configuration settings: {}", getConfig());
        Set<Integer> activeRooms = Optional.ofNullable(getConfig().get(BvfHeatingBindingConstants.CONFIG_ACTIVE_ROOMS)).map(activeRoomsObj -> {
            return Arrays.stream(activeRoomsObj.toString().split(","))
                .map(roomStr -> atoi(roomStr.trim()).orElse(-1))
                .collect(Collectors.toSet());
        }).orElse(Collections.emptySet());

        String url = getConfig().get(BvfHeatingBindingConstants.CONFIG_URL).toString();

        int httpPollingInterval = Optional.ofNullable(getConfig().get(BvfHeatingBindingConstants.CONFIG_HTTP_POLLING_INTERVAL))
                .flatMap(pollingIntervalObj -> atoi(pollingIntervalObj.toString())).orElse(10);

        logger.debug("{}: validating configuration: url = {}, activeRooms = {}, httpPollingInterval = {}", this, url, activeRooms, httpPollingInterval);
        if(activeRooms.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "active rooms is empty");
            return;
        }
        for(Integer roomNr: activeRooms) {
            if(roomNr < 1 || roomNr > 8) {
            	updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "invalid room numbers");
            	return;
            }
        }
        if(httpPollingInterval < 5) {
            logger.warn("{}: httpPollingInterval is too low - setting to 5 seconds: {}", httpPollingInterval);
            httpPollingInterval = 5;
        }

        logger.debug("{}: configuration looks good - starting client...", this);
        zoneBoxClient = Optional.of(new ThresholdZoneBoxClient(new ZoneBoxHttpClient(httpClient, url), 1000));
        pollingTickFuture = Optional.of(scheduler.scheduleAtFixedRate(new PollingTick(activeRooms), 1, httpPollingInterval, TimeUnit.SECONDS));
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("{}.handleCommand: groupId = {}, bindingId = {}, channel: {} -> {}",
                this, channelUID.getGroupId(), channelUID.getBindingId(), channelUID.getIdWithoutGroup(), command);

        RoomStatus roomStatus = new RoomStatus();

        if(roomStatus.updateFromCommand(channelUID, command)) {
            if(zoneBoxClientTransaction(zoneBoxClient -> roomStatus.apply2client(zoneBoxClient), "apply command: ", command)) {
                roomStatus.apply2handler(thing.getUID(), (channelID, state) -> updateState(channelID, state));
            } else {
                logger.debug("unable to apply command - transaction didn't pass");
            }
        } else {
            logger.debug("ignoring unknown command: {} -> {}", channelUID, command);
        }
    }


    @Override
    public void handleRemoval() {
        logger.debug("{} - handling removal...", this);
        pollingTickFuture.ifPresent(pollingTick -> {
            if(!pollingTick.isDone()) {
                pollingTick.cancel(false);
            }
        });
        pollingTickFuture = Optional.empty();
        super.handleRemoval();
    }

    @Override
    public void dispose() {
        logger.debug("{} - disposing...", this);
        pollingTickFuture.ifPresent(pollingTick -> {
            if(!pollingTick.isDone()) {
                pollingTick.cancel(false);
            }
        });
        pollingTickFuture = Optional.empty();
        super.dispose();
    }
}
