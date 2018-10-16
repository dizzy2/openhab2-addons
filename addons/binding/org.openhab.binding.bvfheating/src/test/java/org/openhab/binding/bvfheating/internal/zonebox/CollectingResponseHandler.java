/**
 *
 */
package org.openhab.binding.bvfheating.internal.zonebox;

import static org.junit.Assert.fail;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxHttpClient.ResponseHandler;

public class CollectingResponseHandler implements ResponseHandler {
    public Integer roomNr = null;
    public DecimalType temperature = null;
    public DecimalType spTemperature = null;
    public Integer cMode = null;
    public Boolean isOn = null;

    /*
     * (non-Javadoc)
     *
     * @see
     * org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxHttpClient.ResponseHandler#handleActualTemp(java.util.
     * Optional, org.eclipse.smarthome.core.library.types.DecimalType)
     */
    @Override
    public void handleActualTemp(@NonNull Optional<Integer> roomNr, @NonNull DecimalType temp) {
        roomNr.ifPresent(newRoomNr -> this.roomNr = newRoomNr);
        this.temperature = temp;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxHttpClient.ResponseHandler#handleSetPointTemp(java.util.
     * Optional, org.eclipse.smarthome.core.library.types.DecimalType)
     */
    @Override
    public void handleSetPointTemp(@NonNull Optional<Integer> roomNr, @NonNull DecimalType temp) {
        roomNr.ifPresent(newRoomNr -> this.roomNr = newRoomNr);
        this.spTemperature = temp;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxHttpClient.ResponseHandler#handleCMode(java.util.Optional,
     * int)
     */
    @Override
    public void handleCMode(@NonNull Optional<Integer> roomNr, int cMode) {
        roomNr.ifPresent(newRoomNr -> this.roomNr = newRoomNr);
        this.cMode = cMode;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxHttpClient.ResponseHandler#handleOnOff(java.util.Optional,
     * boolean)
     */
    @Override
    public void handleOnOff(@NonNull Optional<Integer> roomNr, boolean isOn) {
        roomNr.ifPresent(newRoomNr -> this.roomNr = newRoomNr);
        this.isOn = isOn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxHttpClient.ResponseHandler#handleHttpError(int,
     * java.lang.String)
     */
    @Override
    public void handleHttpError(int status, @NonNull String response) {
        fail("got an http error: " + status + " -> " + response);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxHttpClient.ResponseHandler#handleThrowable(java.lang.
     * Throwable)
     */
    @Override
    public void handleThrowable(@NonNull Throwable exc) {
        fail("got an exception: " + exc.getMessage());
    }
}