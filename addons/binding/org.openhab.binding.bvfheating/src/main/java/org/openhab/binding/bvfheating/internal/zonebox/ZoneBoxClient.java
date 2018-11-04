/**
 *
 */
package org.openhab.binding.bvfheating.internal.zonebox;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.library.types.DecimalType;

/**
 * Interface defining all BVF ZoneBox options offered via it's web-based interface. Methods are coupled according
 * to ZoneBox's web forms to allow sending several settings (from single form) in single http request.<br/>
 * Each method takes a reference to a {@link ResponseHandler} implementation, which will receive the actual
 * values returned in ZoneBox response.
 *
 * @author rastiiik
 *
 */
public interface ZoneBoxClient {
    /**
     * Interface which will receive thermostat values as parsed from ZoneBox response. The order of the methods
     * invocation is undefined and can change.
     */
    public static interface ResponseHandler {

        public void handleActualTemp(@NonNull Optional<Integer> roomNr, @NonNull DecimalType temp);

        public void handleSetPointTemp(@NonNull Optional<Integer> roomNr, @NonNull DecimalType temp);

        public void handleCMode(@NonNull Optional<Integer> roomNr, int cMode);

        public void handleOnOff(@NonNull Optional<Integer> roomNr, boolean isOn);

        public void handleHttpError(int status, @NonNull String response);

        public void handleThrowable(@NonNull Throwable exc);
    }

    public void rForm(int newRoomNr, @NonNull ResponseHandler responseHandler);

    public void v0Form(@NonNull DecimalType setpointTemp, @NonNull ResponseHandler responseHandler);

    public void v1form(int cMode, boolean isOn, @NonNull ResponseHandler responseHandler);
}
