/**
 *
 */
package org.openhab.binding.samsungac.internal;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import org.openhab.binding.samsungac.internal.SamsungAcResponseParser.ResponseHandler;
import org.xml.sax.SAXException;

/**
 * @author rastiiik
 *
 */
public class SamsungAcResponseParserTest {

    private static class NoopResponseHandler implements ResponseHandler {
        public Object prm1 = null;

        @Override
        public void handleAuthOk(String startFrom) throws SAXException {
        }

        @Override
        public void handleStatusAttribute(String devDuid, String devGroupId,
                String devModelId, String attrId, String attrType, String attrValue) throws SAXException {
        }
    }

    /**
     * Test method testing parsing of AuthToken/Okay response.
     *
     * @throws XMLStreamException
     */
    @Test
    public void testAuthTokenOkResponse() throws XMLStreamException {
        final NoopResponseHandler handleAuthOkMock = new NoopResponseHandler() {
            @Override
            public void handleAuthOk(String startFrom) throws SAXException {
                assertEquals("startFrom should contain correct date", "2018-09-07/20:25:59", startFrom);
                prm1 = Boolean.TRUE;
            }
        };
        final String TEST_RESPONSE = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
                + "<Response Type=\"AuthToken\" Status=\"Okay\" StartFrom=\"2018-09-07/20:25:59\"/>";
        final SamsungAcResponseParser testInstance = new SamsungAcResponseParser(new StringReader(TEST_RESPONSE),
                handleAuthOkMock);

        testInstance.run();
        testInstance.close();

        assertTrue("ResponseHandler.handleAuthOk() should be called", Boolean.TRUE.equals(handleAuthOkMock.prm1));
    }

    /**
     * Test method testing parsing of DeviceState response.
     *
     * @throws XMLStreamException
     */
    @Test
    public void testDeviceStateResponse() throws XMLStreamException {
        final Map<String, String> resultAttrMap = new HashMap<>(50);
        final NoopResponseHandler handleDeviceStateMock = new NoopResponseHandler() {

            @Override
            public void handleStatusAttribute(String devDuid, String devGroupId,
                    String devModelId, String attrId, String attrType, String attrValue) throws SAXException {
                assertEquals("Device DUID should match",    "1234567890", devDuid);
                assertEquals("Device GroupID should match", "AC", devGroupId);
                assertEquals("Device ModelID should match", "AC", devModelId);
                resultAttrMap.put(attrId, attrValue);
            }

            @Override
            public void handleAuthOk(String startFrom) throws SAXException {
                assertEquals("startFrom should contain correct date", "2018-09-07/20:25:59", startFrom);
                prm1 = Boolean.TRUE;
            }
        };
        final String TEST_RESPONSE = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><Response Type=\"DeviceState\" Status=\"Okay\">"
                + "<DeviceState><Device DUID=\"1234567890\" GroupID=\"AC\" ModelID=\"AC\" >"
                + "<Attr ID=\"AC_FUN_ENABLE\" Type=\"RW\" Value=\"Enable\"/>"
                + "<Attr ID=\"AC_FUN_POWER\" Type=\"RW\" Value=\"Off\"/>"
                + "<Attr ID=\"AC_FUN_SUPPORTED\" Type=\"R\" Value=\"0\"/>"
                + "<Attr ID=\"AC_FUN_OPMODE\" Type=\"RW\" Value=\"Cool\"/>"
                + "<Attr ID=\"AC_FUN_TEMPSET\" Type=\"RW\" Value=\"26\"/>"
                + "<Attr ID=\"AC_FUN_COMODE\" Type=\"RW\" Value=\"Off\"/>"
                + "<Attr ID=\"AC_FUN_ERROR\" Type=\"RW\" Value=\"00000000\"/>"
                + "<Attr ID=\"AC_FUN_TEMPNOW\" Type=\"R\" Value=\"26\"/>"
                + "<Attr ID=\"AC_FUN_SLEEP\" Type=\"RW\" Value=\"0\"/>"
                + "<Attr ID=\"AC_FUN_WINDLEVEL\" Type=\"RW\" Value=\"Auto\"/>"
                + "<Attr ID=\"AC_FUN_DIRECTION\" Type=\"RW\" Value=\"SwingLR\"/>"
                + "<Attr ID=\"AC_ADD_AUTOCLEAN\" Type=\"RW\" Value=\"Off\"/>"
                + "<Attr ID=\"AC_ADD_APMODE_END\" Type=\"W\" Value=\"0\"/>"
                + "<Attr ID=\"AC_ADD_STARTWPS\" Type=\"RW\" Value=\"0\"/>"
                + "<Attr ID=\"AC_ADD_SPI\" Type=\"RW\" Value=\"Off\"/>"
                + "<Attr ID=\"AC_SG_WIFI\" Type=\"W\" Value=\"Connected\"/>"
                + "<Attr ID=\"AC_SG_INTERNET\" Type=\"W\" Value=\"Connected\"/>"
                + "<Attr ID=\"AC_ADD2_VERSION\" Type=\"RW\" Value=\"0\"/>"
                + "<Attr ID=\"AC_SG_MACHIGH\" Type=\"W\" Value=\"0\"/>"
                + "<Attr ID=\"AC_SG_MACMID\" Type=\"W\" Value=\"0\"/>"
                + "<Attr ID=\"AC_SG_MACLOW\" Type=\"W\" Value=\"0\"/>"
                + "<Attr ID=\"AC_SG_VENDER01\" Type=\"W\" Value=\"0\"/>"
                + "<Attr ID=\"AC_SG_VENDER02\" Type=\"W\" Value=\"0\"/>"
                + "<Attr ID=\"AC_SG_VENDER03\" Type=\"W\" Value=\"0\"/>"
                + "</Device></DeviceState></Response>";
        final SamsungAcResponseParser testInstance = new SamsungAcResponseParser(
                new StringReader(TEST_RESPONSE), handleDeviceStateMock);

        testInstance.run();
        testInstance.close();

        assertTrue("attributes have been collected", resultAttrMap.size() > 0);
        assertEquals("AC_FUN_ENABLE should match", "Enable", resultAttrMap.get("AC_FUN_ENABLE"));
        assertEquals("AC_FUN_TEMPSET should match", "26", resultAttrMap.get("AC_FUN_TEMPSET"));
    }
}
