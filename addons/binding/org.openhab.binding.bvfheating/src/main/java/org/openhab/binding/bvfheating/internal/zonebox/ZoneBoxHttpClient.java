/**
 *
 */
package org.openhab.binding.bvfheating.internal.zonebox;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Fields;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rastiiik
 *
 */
public class ZoneBoxHttpClient implements ZoneBoxClient {
    private final Logger logger = LoggerFactory.getLogger(ZoneBoxHttpClient.class);

    // for example " 23.0 ℃"
    private final Pattern TEMP_PATTERN = Pattern.compile("(\\d+[,\\.]\\d+)");
    // like "stemp = 23.0;"
    private final Pattern SP_TEMP_JS_PATTERN = Pattern.compile("stemp\\s?=\\s?(\\d+[,\\.]\\d+)");

    private final String baseUri;
    private final HttpClient httpClient;

    public ZoneBoxHttpClient(@NonNull HttpClient httpClient, @NonNull String baseUri) {
        this.httpClient = httpClient;
        this.baseUri = baseUri;
    }

    private Optional<Integer> parseInt(String str) {
        return Optional.ofNullable(str).filter(x -> x.length() > 0).flatMap(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException exc) {
                logger.error("unable to convert string into an integer", exc);
                return Optional.empty();
            }
        });
    }

    private Optional<DecimalType> parseDecimal(String str) {
        return Optional.ofNullable(str).filter(x -> x.length() > 0).flatMap(s -> {
            try {
                return Optional.of(DecimalType.valueOf(s));
            } catch (NumberFormatException exc) {
                logger.error("unable to convert string into a decimal number", exc);
                return Optional.empty();
            }
        });
    }

    void parseResponse(@NonNull Document responseDoc, @NonNull ResponseHandler responseHandler) {
        // <form name="update_r"... -> extract room number, if set
        final Optional<Integer> roomNr = responseDoc.select("form[name=update_r] input[name=room]").stream()
                .map(roomInput -> parseInt(roomInput.attr("value"))).filter(tempOpt -> tempOpt.isPresent())
                .map(tempOpt -> tempOpt.get()).findFirst();

        logger.debug("parseResponse: room number resolved as: {}", roomNr);

        // <form name="update_v0"... -> extract actual room temperature from cur_temp div
        for (Element curTempDiv : responseDoc.select("form[name=update_v0] div[id=cur_temp]")) {
            logger.debug("parseResponse: found cur_temp div: {}", curTempDiv);
            Matcher tempMatcher = TEMP_PATTERN.matcher(curTempDiv.ownText());
            if (tempMatcher.find()) {
                parseDecimal(tempMatcher.group(1)).ifPresent(temp -> responseHandler.handleActualTemp(roomNr, temp));
            } else {
                logger.error("parseResponse: text in cur_temp div doesn't match the expected pattern: {}",
                        curTempDiv.ownText());
            }
        }

        // <form name="update_v1"... -> extract cmode and on/off from corresponding radio buttons
        for (Element v1RadioBtn : responseDoc.select("form[name=update_v1] input[checked]")) {
            logger.debug("parseResponse: found v1 radio button: {}", v1RadioBtn);
            final String elementName = v1RadioBtn.attr("name");
            final String elementValue = v1RadioBtn.attr("value");

            if ("cmode".equalsIgnoreCase(elementName)) {
                parseInt(elementValue).ifPresent(cmode -> responseHandler.handleCMode(roomNr, cmode));
            } else if ("onoff".equalsIgnoreCase(elementName)) {
                responseHandler.handleOnOff(roomNr, parseInt(elementValue).orElse(1) > 0);
            } else {
                logger.error("parseResponse: unknown input element/unknown name: {}", v1RadioBtn);
            }
        }

        // setpoint temperature value is within a script containing statement like "stemp = 23.0;"
        Matcher spMatcher = null;
        for (Element script : responseDoc.select("script")) {
            logger.trace("parseResponse: found script element: {}", script);
            if (spMatcher == null) {
                spMatcher = SP_TEMP_JS_PATTERN.matcher(script.data());
            } else {
                spMatcher.reset(script.data());
            }
            if (spMatcher.find()) {
                logger.debug("parseResponse: found script containing SP value");
                parseDecimal(spMatcher.group(1))
                        .ifPresent(spTemp -> responseHandler.handleSetPointTemp(roomNr, spTemp));
            } else {
                logger.trace("parseResponse: script element doesn't seem to contain SP value");
            }
        }
    }

    void parseResponse(@NonNull InputStream responseStream, @NonNull ResponseHandler responseHandler)
            throws IOException {
        parseResponse(Jsoup.parse(responseStream, null, baseUri), responseHandler);
    }

    InputStreamResponseListener postRequest(@NonNull Fields params) {
        final InputStreamResponseListener responseListener = new InputStreamResponseListener();
        httpClient.POST(baseUri).header("Content-Type", "application/x-www-form-urlencoded")
                .content(new FormContentProvider(params)).send(responseListener);
        return responseListener;
    }

    void safeClose(@Nullable Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException exc) {
        }
    }

    String dumpInputStream(@NonNull InputStream inputStream) {
        ByteArrayOutputStream result = new ByteArrayOutputStream(16384);
        byte[] buffer = new byte[1024];
        int length;

        try {
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            // StandardCharsets.UTF_8.name() > JDK 7
            return result.toString("UTF-8");
        } catch (IOException exc) {
            logger.error("dumpInputStream: unable to convert input stream", exc);
            return "N/A";
        }
    }

    void handleResponse(@NonNull InputStreamResponseListener responseListnr, @NonNull ResponseHandler responseHandler) {
        InputStream responseInputStream = null;
        try {
            Response response = responseListnr.get(20, TimeUnit.SECONDS);
            responseInputStream = responseListnr.getInputStream();

            if (response.getStatus() == HttpStatus.OK_200) {
                parseResponse(responseInputStream, responseHandler);
            } else {
                responseHandler.handleHttpError(response.getStatus(), dumpInputStream(responseInputStream));
            }
        } catch (InterruptedException | TimeoutException | ExecutionException | IOException exc) {
            logger.error("handleResponse: exception while processing response", exc);
            responseHandler.handleThrowable(exc);
        } finally {
            safeClose(responseInputStream);
        }
    }

    public void rForm(int newRoomNr, @NonNull ResponseHandler responseHandler) {
        final Fields params = new Fields(true);
        params.put("room", Integer.toString(newRoomNr));
        handleResponse(postRequest(params), responseHandler);
    }

    public void v0Form(@NonNull DecimalType setpointTemp, @NonNull ResponseHandler responseHandler) {
        final Fields params = new Fields(true);
        params.put("settemp", setpointTemp.toString());
        handleResponse(postRequest(params), responseHandler);
    }

    public void v1form(int cMode, boolean isOn, @NonNull ResponseHandler responseHandler) {
        final Fields params = new Fields(true);
        params.put("cmode", Integer.toString(cMode));
        params.put("onoff", isOn ? "1" : "0");
        handleResponse(postRequest(params), responseHandler);
    }
}
