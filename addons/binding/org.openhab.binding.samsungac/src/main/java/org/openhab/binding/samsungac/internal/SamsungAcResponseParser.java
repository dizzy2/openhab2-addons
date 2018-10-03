/**
 *
 */
package org.openhab.binding.samsungac.internal;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.openhab.binding.samsungac.internal.StateMachineSaxHandler.ElementParserState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author rastiiik
 *
 */
public class SamsungAcResponseParser implements Runnable, Closeable {
    private static final Logger logger = LoggerFactory.getLogger(SamsungACHandler.class);

    private static final SAXParserFactory SAX_PARSER_FACTORY = SAXParserFactory.newInstance();

    public static interface ResponseHandler {
        void handleAuthOk(String startFrom) throws SAXException;

        void handleStatusAttribute(String devDuid, String devGroupId, String devModelId, String attrId, String attrType,
                String attrValue) throws SAXException;
    }

    private final ElementParserState documentParserState = new ElementParserState() {

        @Override
        public ElementParserState startElement(ElementParserState parentState, String name, Attributes attrs)
                throws SAXException {

            if ("Response".equals(name)) {
                return responseState.enterState(this, attrs);
            }

            return super.enterState(parentState, attrs);
        }

        // Response
        private final ElementParserState responseState = new ElementParserState() {

            @Override
            public ElementParserState enterState(ElementParserState parentState, Attributes attrs) throws SAXException {
                String responseStatus = attrs.getValue("Status");
                if("Okay".equals(responseStatus)) {
                    String responseType = attrs.getValue("Type");

                    if("AuthToken".equals(responseType)) {
                        logger.debug("received authOK response");
                        responseHandler.handleAuthOk(attrs.getValue("StartFrom"));
                    } else if("DeviceState".equals(responseType)) {
                        logger.debug("received device state response");
                    } else {
                        logger.warn("unknown response type/OK: {}", responseType);
                    }
                } else if("Error".equals(responseStatus)) {

                }
                return super.enterState(parentState, attrs);
            }


            @Override
            public ElementParserState startElement(
                    ElementParserState parentState, String name, Attributes attrs) throws SAXException {

                if("DeviceState".equals(name)) {
                    return deviceStateState.enterState(this, attrs);
                } else {
                    logger.error("{}: (ignoring) unexpected child element: {}", this, name);
                    return super.startElement(parentState, name, attrs);
                }
            }

            // Response/DeviceState
            private final ElementParserState deviceStateState = new ElementParserState() {

                @Override
                public ElementParserState startElement(
                        ElementParserState parentState, String name, Attributes attrs) throws SAXException {
                    if("Device".equals(name)) {
                        return deviceState.enterState(this, attrs);
                    } else {
                        logger.error("{}: (ignoring) unexpected child element: {}", this, name);
                        return super.startElement(parentState, name, attrs);
                    }
                }

                // Response/DeviceState/Device
                private final ElementParserState deviceState = new ElementParserState() {
                    public String duid;
                    public String groupId;
                    public String modelId;

                    @Override
                    public ElementParserState enterState(ElementParserState parentState, Attributes attrs) throws SAXException {
                        this.duid    = attrs.getValue("DUID");
                        this.groupId = attrs.getValue("GroupID");
                        this.modelId = attrs.getValue("ModelID");
                        return super.enterState(parentState, attrs);
                    }

                    @Override
                    public ElementParserState startElement(
                            ElementParserState parentState, String name, Attributes attrs) throws SAXException {
                        if ("Attr".equals(name)) {
                            return attrState.enterState(this, attrs);
                        } else {
                            logger.error("{}: (ignoring) unexpected child element: {}", this, name);
                            return super.startElement(parentState, name, attrs);
                        }
                    }

                    // Response/DeviceState/Device/Attr
                    private final ElementParserState attrState = new ElementParserState() {

                        @Override
                        public ElementParserState enterState(
                                ElementParserState parentState, Attributes attrs) throws SAXException {

                            responseHandler.handleStatusAttribute(duid, groupId,
                                    modelId, attrs.getValue("ID"), attrs.getValue("Type"), attrs.getValue("Value"));
                            return super.enterState(parentState, attrs);
                        }
                    };  // Attr
                };  // Device
            };  // DeviceState
        };  // Response
    };

    private final StateMachineSaxHandler saxHandler = new StateMachineSaxHandler(documentParserState);

    private final Reader responseInputReader;
    private final ResponseHandler responseHandler;

    public SamsungAcResponseParser(Reader responseInputReader, ResponseHandler responseHandler) {
        this.responseInputReader = responseInputReader;
        this.responseHandler = responseHandler;
    }

    @Override
    public void run() {
        try {
            SAXParser saxParser = SAX_PARSER_FACTORY.newSAXParser();

            for (;;) {
                saxParser.parse(new InputSource(responseInputReader), saxHandler);
                saxParser.reset();
            }
        } catch (EOFException eofExc) {
            logger.info("reached end of file: {}", this);
        } catch (ParserConfigurationException exc) {
            logger.error("error creating SAX parser", exc);
        } catch (IOException | SAXException exc) {
            logger.error("error reading/parsing AC response stream", exc);
        } finally {
            this.close();
        }
    }

    @Override
    public void close() {
        try {
            responseInputReader.close();
        } catch (IOException exc) {
            logger.debug("error closing stream", exc);
        }
    }
}
