/**
 *
 */
package org.openhab.binding.samsungac.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author rastiiik
 *
 */
public class SamsungAcRequestWriter implements Closeable {
    private static final XMLOutputFactory XML_OUTPUT_FACTORY;

    static {
        XML_OUTPUT_FACTORY = XMLOutputFactory.newInstance();
        XML_OUTPUT_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, false);
    }

    private final XMLStreamWriter xmlWriter;

    /**
     * @param xmlWriter
     */
    public SamsungAcRequestWriter(XMLStreamWriter xmlWriter) {
        this.xmlWriter = xmlWriter;
    }

    void login(String authToken) throws XMLStreamException {
        // "<Request Type=\"AuthToken\"><User Token=\"" + TOKEN_STRING + "\" /></Request>"
        this.xmlWriter.writeStartDocument();
        this.xmlWriter.writeStartElement("Request");
        this.xmlWriter.writeAttribute("Type", "AuthToken");
        this.xmlWriter.writeEmptyElement("User");
        this.xmlWriter.writeAttribute("Token", authToken);
        this.xmlWriter.writeEndElement();
        this.xmlWriter.writeEndDocument();
        this.xmlWriter.writeCharacters("\n");
        this.xmlWriter.flush();
    }

    void getStatus(String deviceId) throws XMLStreamException {
        // "<Request Type=\"DeviceState\" DUID=\"" + MAC + "\"></Request>"
        this.xmlWriter.writeStartDocument();
        this.xmlWriter.writeEmptyElement("Request");
        this.xmlWriter.writeAttribute("Type", "DeviceState");
        this.xmlWriter.writeAttribute("DUID", deviceId);
        this.xmlWriter.writeEndDocument();
        this.xmlWriter.writeCharacters("\n");
        this.xmlWriter.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            xmlWriter.close();
        } catch (XMLStreamException exc) {
            throw new IOException("unable to close underlying stream", exc);
        }
    }

    public static SamsungAcRequestWriter createFromWriter(Writer writer) throws XMLStreamException {
        return new SamsungAcRequestWriter(XML_OUTPUT_FACTORY.createXMLStreamWriter(writer));
    }
}
