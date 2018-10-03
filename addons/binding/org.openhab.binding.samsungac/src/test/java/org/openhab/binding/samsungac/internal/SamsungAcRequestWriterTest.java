/**
 *
 */
package org.openhab.binding.samsungac.internal;

import static org.junit.Assert.*;

import java.io.StringWriter;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;

/**
 * @author rastiiik
 *
 */
public class SamsungAcRequestWriterTest {

    /**
     * Test method for {@link org.openhab.binding.samsungac.internal.SamsungAcRequestWriter#login(java.lang.String)}.
     *
     * @throws XMLStreamException
     */
    @Test
    public void testLogin() throws XMLStreamException {
        final String AUTH_TOKEN = "testAuthToken";
        StringWriter resultWriter = new StringWriter(256);
        SamsungAcRequestWriter testInstance = SamsungAcRequestWriter.createFromWriter(resultWriter);

        testInstance.login(AUTH_TOKEN);

        String loginRequest = resultWriter.toString();
        assertTrue("login request contains token string", loginRequest.contains(AUTH_TOKEN));
        assertEquals("login request contains single EOF (as last char)", loginRequest.length() - "\n".length(),
                loginRequest.indexOf("\n"));
    }

    /**
     * Test method for {@link org.openhab.binding.samsungac.internal.SamsungAcRequestWriter#getStatus()}.
     *
     * @throws XMLStreamException
     */
    @Test
    public void testGetStatus() throws XMLStreamException {
        final String DEVICE_ID = "testDeviceID";
        StringWriter resultWriter = new StringWriter(256);
        SamsungAcRequestWriter testInstance = SamsungAcRequestWriter.createFromWriter(resultWriter);

        testInstance.getStatus(DEVICE_ID);

        String getStatusRequest = resultWriter.toString();
        assertTrue("getStatus request contains deviceId", getStatusRequest.contains(DEVICE_ID));
        assertEquals("request contains single EOF (as last char)", getStatusRequest.length() - "\n".length(),
                getStatusRequest.indexOf("\n"));
    }

    @Test
    public void testMultipleCommands() throws XMLStreamException {
        StringWriter resultWriter = new StringWriter(256);
        SamsungAcRequestWriter testInstance = SamsungAcRequestWriter.createFromWriter(resultWriter);

        testInstance.login("test_auth_token");
        testInstance.getStatus("test_device_id");

        String multipleCommandsRequest = resultWriter.toString();
        String lines[] = multipleCommandsRequest.split("\\r?\\n");

        assertEquals("each request should be on separate line", 2, lines.length);
    }
}
