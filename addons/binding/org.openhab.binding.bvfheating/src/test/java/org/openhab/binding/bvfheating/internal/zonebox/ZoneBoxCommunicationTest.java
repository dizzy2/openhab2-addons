/**
 *
 */
package org.openhab.binding.bvfheating.internal.zonebox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.*;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author rastiiik
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ZoneBoxCommunicationTest {

    private String hostUrl = null;
    private int roomNr = 0;

    private HttpClient httpClient;
    private ZoneBoxHttpClient zoneBoxClient;

    private static CollectingResponseHandler initialParameters = null;

    @Before
    public void setUp() throws Exception {
        hostUrl = System.getProperty("bvfzonebox.url");
        assumeTrue("missing parameter: bvfzonebox.url", hostUrl != null);

        String roomNr = System.getProperty("bvfzonebox.room");
        if (roomNr != null) {
            this.roomNr = Integer.parseInt(roomNr);
        }

        httpClient = new HttpClient();
        httpClient.start();

        zoneBoxClient = new ZoneBoxHttpClient(httpClient, hostUrl);
    }

    @Test
    public void _testSwithingRoomNr() throws Exception {
        initialParameters = new CollectingResponseHandler();

        zoneBoxClient.rForm(roomNr, initialParameters);
        Thread.sleep(2000);

        assertEquals("invalid room numner", Integer.valueOf(roomNr), initialParameters.roomNr);
    }

    @Test
    public void testSettingSetpointTemp() throws Exception {
        final DecimalType newSetpointTemp = new DecimalType("25.5");
        final CollectingResponseHandler resultHandler = new CollectingResponseHandler();

        assumeNotNull("missing initial parameters from switchRoomNr test", initialParameters);

        try {
            Thread.sleep(2000);
            zoneBoxClient.v0Form(newSetpointTemp, resultHandler);
            Thread.sleep(2000);

            assertEquals("temperature doesn't match", newSetpointTemp, resultHandler.spTemperature);
        } finally {
            zoneBoxClient.v0Form(initialParameters.spTemperature, resultHandler);
        }
    }

    @Test
    public void testSettingCmodeOnOff() throws Exception {
        final CollectingResponseHandler resultHandler = new CollectingResponseHandler();
        final int newCMode = 1;
        final boolean newIsOn = true;

        assumeNotNull("missing initial parameters from switchRoomNr test", initialParameters);

        try {
            Thread.sleep(2000);
            zoneBoxClient.v1form(newCMode, newIsOn, resultHandler);
            Thread.sleep(2000);

            assertEquals("cmode doesn't match", Integer.valueOf(newCMode), resultHandler.cMode);
            assertEquals("on/off doesn't match", Boolean.valueOf(newIsOn), resultHandler.isOn);
        } finally {
            zoneBoxClient.v1form(initialParameters.cMode, initialParameters.isOn, resultHandler);
        }
    }

    @After
    public void cleanUp() throws Exception {
        if (httpClient != null) {
            httpClient.stop();
        }
        httpClient = null;
    }
}
