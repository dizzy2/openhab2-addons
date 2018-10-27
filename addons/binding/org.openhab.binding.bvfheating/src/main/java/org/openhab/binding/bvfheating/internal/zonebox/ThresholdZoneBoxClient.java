/**
 *
 */
package org.openhab.binding.bvfheating.internal.zonebox;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.library.types.DecimalType;

/**
 * @author rastiiik
 *
 */
public class ThresholdZoneBoxClient implements ZoneBoxClient {

    private final @NonNull ZoneBoxClient nestedClient;
    private final long msDelay;

    private long lastCall;

    public ThresholdZoneBoxClient(@NonNull ZoneBoxClient nestedClient, long msDelay) {
        this.nestedClient = nestedClient;
        this.msDelay = msDelay;
        this.lastCall = 0;
    }

    private void safeSleep(long untilMs) {
        for (;;) {
            long currentTime = System.currentTimeMillis();
            if (currentTime < untilMs) {
                try {
                    Thread.sleep(untilMs - currentTime);
                } catch (InterruptedException exc) {
                    // TODO: log...
                }
            } else {
                return;
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxClient#rForm(int,
     * org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxClient.ResponseHandler)
     */
    @Override
    public synchronized void rForm(int newRoomNr, @NonNull ResponseHandler responseHandler) {
        safeSleep(lastCall + msDelay);
        nestedClient.rForm(newRoomNr, responseHandler);
        lastCall = System.currentTimeMillis();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxClient#v0Form(org.eclipse.smarthome.core.library.types.
     * DecimalType, org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxClient.ResponseHandler)
     */
    @Override
    public synchronized void v0Form(@NonNull DecimalType setpointTemp, @NonNull ResponseHandler responseHandler) {
        safeSleep(lastCall + msDelay);
        nestedClient.v0Form(setpointTemp, responseHandler);
        lastCall = System.currentTimeMillis();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxClient#v1form(int, boolean,
     * org.openhab.binding.bvfheating.internal.zonebox.ZoneBoxClient.ResponseHandler)
     */
    @Override
    public void v1form(int cMode, boolean isOn, @NonNull ResponseHandler responseHandler) {
        safeSleep(lastCall + msDelay);
        nestedClient.v1form(cMode, isOn, responseHandler);
        lastCall = System.currentTimeMillis();
    }
}
