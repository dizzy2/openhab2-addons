/**
 *
 */
package org.openhab.binding.samsungac.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.ssl.KeyMaterial;
import org.apache.commons.ssl.SSLClient;
import org.apache.commons.ssl.TrustMaterial;
import org.eclipse.jdt.annotation.NonNull;

/**
 * @author rastiiik
 *
 */
public class SslConnection implements Closeable {
    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[] { new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }
    } };

    private final SSLSocket sslSocket;

    public SslConnection(@NonNull SSLSocket sslSocket) throws IOException {
        this.sslSocket = sslSocket;
        this.sslSocket.setSoTimeout(2000);
        this.sslSocket.startHandshake();
    }

    /**
     * @return the inputStream
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        return this.sslSocket.getInputStream();
    }

    /**
     * @return the outputStream
     * @throws IOException
     */
    public OutputStream getOutputStream() throws IOException {
        return this.sslSocket.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        this.sslSocket.close();
    }

    @Override
    public String toString() {
        return this.sslSocket.toString();
    }

    public static SslConnection createFromCertFile(String host, int port, String certFileName, String certPassword)
            throws IOException {
        try {
            SSLClient client = new SSLClient();

            client.addTrustMaterial(TrustMaterial.DEFAULT);
            client.setCheckHostname(false);
            client.setCheckExpiry(false);
            client.setKeyMaterial(new KeyMaterial(certFileName, certPassword.toCharArray()));
            client.setConnectTimeout(10000);
            return new SslConnection((SSLSocket) client.createSocket(host, port));
        } catch (GeneralSecurityException | IOException exc) {
            throw new IOException("unable to connect to the remote host " + host + ":" + port + " (using certificate "
                    + certFileName + ")", exc);
        }
    }

    public static SslConnection createTrustAll(String host, int port) throws IOException {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");

            ctx.init(null, TRUST_ALL_CERTS, null);
            return new SslConnection((SSLSocket) ctx.getSocketFactory().createSocket(host, port));
        } catch (GeneralSecurityException | IOException exc) {
            throw new IOException("unable to connect to the remote host " + host + ":" + port, exc);
        }
    }
}
