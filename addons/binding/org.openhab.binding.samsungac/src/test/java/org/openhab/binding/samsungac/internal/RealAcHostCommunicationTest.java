/**
 *
 */
package org.openhab.binding.samsungac.internal;

import static org.junit.Assume.assumeTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

/**
 * @author rastiiik
 *
 */
public class RealAcHostCommunicationTest {
    private String host = null;
    private int port = 2878;
    private String certFileName = null;
    private String certPassword = null;

    @Before
    public void setUp() {
        host = System.getProperty("host");
        assumeTrue("missing host parameter", host != null);

        String port = System.getProperty("port");
        if (port != null) {
            this.port = Integer.parseInt(port);
        }

        certFileName = System.getProperty("certFileName");
        certPassword = System.getProperty("certPassword");
    }

    @Test
    public void establishConnection() throws IOException {
        if (certFileName == null || "".equals(certFileName)) {
            SslConnection.createTrustAll(host, port);
        } else {
            SslConnection.createFromCertFile(host, port, certFileName, certPassword);
        }
    }
}
