/**
 *
 */
package org.openhab.binding.bvfheating.internal;

import java.util.Optional;
import java.util.function.BiConsumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxClient;

/**
 * @author rastiiik
 *
 */
@NonNullByDefault
public class RoomStatus {
    public Optional<Integer> roomNr;
    public Optional<Command> actualTemp;
    public Optional<Command> setPointTemp;
    public Optional<Command> cMode;
    public Optional<Command> onOff;

    public Optional<Object> error;

    public RoomStatus() {
        this.roomNr = Optional.empty();
        this.actualTemp = Optional.empty();
        this.setPointTemp = Optional.empty();
        this.cMode = Optional.empty();
        this.onOff = Optional.empty();
        this.error = Optional.empty();
    }

    public ZoneBoxClient.ResponseHandler zoneboxResponseHandler = new ZoneBoxClient.ResponseHandler() {
        @Override
        public void handleActualTemp(Optional<Integer> roomNr, DecimalType temp) {
            roomNr.ifPresent(roomNrVal -> RoomStatus.this.roomNr = roomNr);
            RoomStatus.this.actualTemp = Optional.of(temp);
        }

        @Override
        public void handleSetPointTemp(Optional<Integer> roomNr, @NonNull DecimalType temp) {
            roomNr.ifPresent(roomNrVal -> RoomStatus.this.roomNr = roomNr);
            RoomStatus.this.setPointTemp = Optional.of(temp);
        }

        @Override
        public void handleCMode(Optional<Integer> roomNr, int cMode) {
            roomNr.ifPresent(roomNrVal -> RoomStatus.this.roomNr = roomNr);
            RoomStatus.this.cMode = Optional.of(new DecimalType(cMode));
        }

        @Override
        public void handleOnOff(Optional<Integer> roomNr, boolean isOn) {
            roomNr.ifPresent(roomNrVal -> RoomStatus.this.roomNr = roomNr);
            RoomStatus.this.onOff = Optional.of(isOn ? OnOffType.ON : OnOffType.OFF);
        }

        @Override
        public void handleHttpError(int status, @NonNull String response) {
            RoomStatus.this.error = Optional.of("http error: " + status + " -> " + response);
        }

        @Override
        public void handleThrowable(@NonNull Throwable exc) {
            RoomStatus.this.error = Optional.of(exc.getMessage());
        }
    };

    private static <T> Optional<T> optionalOfNullable(@Nullable T value) {
        return Optional.ofNullable(value);
    }

    public Optional<DecimalType> actualTemp() {
        return this.actualTemp.flatMap(actualTemp -> {
            if (actualTemp instanceof QuantityType<?>) {
                return optionalOfNullable(((QuantityType<?>) actualTemp).as(DecimalType.class));
            } else if (actualTemp instanceof DecimalType) {
                return Optional.of((DecimalType) actualTemp);
            } else {
                return Optional.empty();
            }
        });
    }

    public Optional<DecimalType> setpointTemp() {
        return this.setPointTemp.flatMap(setpointTemp -> {
            if (setpointTemp instanceof QuantityType<?>) {
                return optionalOfNullable(((QuantityType<?>) setpointTemp).as(DecimalType.class));
            } else if (setpointTemp instanceof DecimalType) {
                return Optional.of((DecimalType) setpointTemp);
            } else {
                return Optional.empty();
            }
        });
    }

    public Optional<DecimalType> cMode() {
        return this.cMode.flatMap(cMode -> {
            if (cMode instanceof DecimalType) {
                return Optional.of((DecimalType) cMode);
            } else {
                return Optional.empty();
            }
        });
    }

    public Optional<OnOffType> onOff() {
        return this.onOff.flatMap(onOff -> {
            if (onOff instanceof OnOffType) {
                return Optional.of((OnOffType) onOff);
            } else {
                return Optional.empty();
            }
        });
    }

    public void apply2client(ZoneBoxClient zoneBoxClient) {
        roomNr.ifPresent(roomNr -> {
            RoomStatus actualValues = new RoomStatus();

            try {
                zoneBoxClient.rForm(roomNr, actualValues.zoneboxResponseHandler);
                if (this.roomNr.equals(actualValues.roomNr)) {
                } else {
                    this.error = Optional.of("applyClient(): error setting roomNr: " + roomNr);
                    return;
                }

                this.setpointTemp().ifPresent(setpointTemp -> {
                    zoneBoxClient.v0Form(setpointTemp, actualValues.zoneboxResponseHandler);
                });

                if (this.cMode.isPresent() || this.onOff.isPresent()) {
                    zoneBoxClient.v1form(
                            cMode().orElseGet(() -> actualValues.cMode().get()).intValue(),
                            onOff().orElseGet(() -> actualValues.onOff().get()) == OnOffType.ON,
                            actualValues.zoneboxResponseHandler);
                }
            } finally {
                updateFrom(actualValues);
            }
        });
    }

    public void apply2handler(ThingUID thingUID, BiConsumer<ChannelUID, State> updateState) {
        this.roomNr.filter(roomNr -> roomNr < BvfHeatingBindingConstants.CHANNEL_GROUPS_ROOMS.length)
            .map(roomNr -> BvfHeatingBindingConstants.CHANNEL_GROUPS_ROOMS[roomNr])
            .ifPresent(roomGID -> {
                actualTemp().ifPresent(actualTemp -> {
                    updateState.accept(new ChannelUID(thingUID, roomGID, BvfHeatingBindingConstants.CHANNEL_TEMP), actualTemp);
                });

                setpointTemp().ifPresent(setpointTemp -> {
                    updateState.accept(new ChannelUID(thingUID, roomGID, BvfHeatingBindingConstants.CHANNEL_SP_TEMP), setpointTemp);
                });

                cMode().ifPresent(cMode -> {
                    updateState.accept(new ChannelUID(thingUID, roomGID, BvfHeatingBindingConstants.CHANNEL_MODE), cMode);
                });

                onOff().ifPresent(onOff -> {
                    updateState.accept(new ChannelUID(thingUID, roomGID, BvfHeatingBindingConstants.CHANNEL_ONOFF), onOff);
                });
            });
    }

    public boolean updateFromCommand(ChannelUID channelUID, Command command) {
        Optional.ofNullable(channelUID.getGroupId())
            .flatMap(groupId -> Optional.ofNullable(BvfHeatingBindingConstants.CHANNEL_GROUP2INDEX.get(groupId)))
            .ifPresent(roomNr -> this.roomNr = Optional.of(roomNr));

        switch (channelUID.getIdWithoutGroup()) {
            case BvfHeatingBindingConstants.CHANNEL_SP_TEMP:
                if (command instanceof QuantityType<?> || command instanceof DecimalType) {
                    this.setPointTemp = Optional.of(command);
                    return true;
                } else {
                    return false;
                }

            case BvfHeatingBindingConstants.CHANNEL_MODE:
                if (command instanceof DecimalType) {
                    this.cMode = Optional.of(command);
                    return true;
                } else {
                    return false;
                }

            case BvfHeatingBindingConstants.CHANNEL_ONOFF:
                if (command instanceof OnOffType) {
                    this.onOff = Optional.of(command);
                    return true;
                } else {
                    return false;
                }

            default:
                return false;
        }
    }

    public void updateFrom(RoomStatus other) {
        other.roomNr.ifPresent(otherRoomNr -> this.roomNr = other.roomNr);
        other.actualTemp.ifPresent(otherActualTemp -> this.actualTemp = other.actualTemp);
        other.setPointTemp.ifPresent(otherSetpointTemp -> this.setPointTemp = other.setPointTemp);
        other.cMode.ifPresent(otherCmode -> this.cMode = other.cMode);
        other.onOff.ifPresent(otherOnOff -> this.onOff = other.onOff);
        other.error.ifPresent(otherError -> this.error = other.error);
    }
}
