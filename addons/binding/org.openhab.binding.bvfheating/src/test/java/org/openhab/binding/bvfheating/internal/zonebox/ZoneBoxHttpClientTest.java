/**
 *
 */
package org.openhab.binding.bvfheating.internal.zonebox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.junit.Test;

/**
 * @author rastiiik
 *
 */
public class ZoneBoxHttpClientTest {
    private final ZoneBoxHttpClient testInstance = new ZoneBoxHttpClient(new HttpClient(), "http://bvfbox");

    @Test
    public void parseResponseTest() throws IOException {
        InputStream roomResponse = getClass().getClassLoader().getResourceAsStream("eindex_room7.htm");
        CollectingResponseHandler resultHandler = new CollectingResponseHandler();

        assumeNotNull(roomResponse, "missing eindex_room7.htm");
        testInstance.parseResponse(roomResponse, resultHandler);

        assertEquals("roomNumber doesn't match", Integer.valueOf(6), resultHandler.roomNr);
        assertEquals("temperature doesn't match", DecimalType.valueOf("23.0"), resultHandler.temperature);
        assertEquals("setpoint temperature doesn't match", DecimalType.valueOf("23.0"), resultHandler.spTemperature);
        assertEquals("cMode doesn't match", Integer.valueOf(0), resultHandler.cMode);
        assertEquals("isOn doesn't match", Boolean.TRUE, resultHandler.isOn);
    }
}
