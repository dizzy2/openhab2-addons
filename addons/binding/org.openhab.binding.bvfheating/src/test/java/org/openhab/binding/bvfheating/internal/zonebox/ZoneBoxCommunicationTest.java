/**
 *
 */
package org.openhab.binding.bvfheating.internal.zonebox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import org.eclipse.jetty.client.HttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author rastiiik
 *
 */
public class ZoneBoxCommunicationTest {

    private String hostUrl = null;
    private int roomNr = 0;

    private HttpClient httpClient;
    private ZoneBoxHttpClient zoneBoxClient;

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

    @After
    public void cleanUp() throws Exception {
        if (httpClient != null) {
            httpClient.stop();
        }
        httpClient = null;
    }

    @Test
    public void testSwithingRoomNr() throws Exception {
        CollectingResponseHandler resultHandler = new CollectingResponseHandler();

        zoneBoxClient.rForm(roomNr, resultHandler);

        assertEquals("invalid room numner", Integer.valueOf(roomNr), resultHandler.roomNr);
    }
}
