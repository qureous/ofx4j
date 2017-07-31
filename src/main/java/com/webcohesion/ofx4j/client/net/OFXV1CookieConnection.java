package com.webcohesion.ofx4j.client.net;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

public class OFXV1CookieConnection extends OFXV1Connection {

    private static final Log LOG = LogFactory.getLog(OFXV1CookieConnection.class);

    private CloseableHttpClient httpClient;

    private Boolean initialized;

    public OFXV1CookieConnection() {
        RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build();
        CookieStore cookieStore = new BasicCookieStore();
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);

        httpClient = HttpClients.custom().setDefaultRequestConfig(globalConfig).setDefaultCookieStore(cookieStore).build();
        initialized = false;
    }

    /**
     * Send the specified buffer to the specified URL.
     *
     * @param url       The URL.
     * @param outBuffer The buffer.
     * @return The response.
     */
    @Override
    protected InputStream sendBuffer(URL url, ByteArrayOutputStream outBuffer) throws IOException, OFXConnectionException {
        HttpPost httpPost = new HttpPost(url.toString());
        httpPost.setHeader("Cache-Control", "no-cache");
        httpPost.setHeader("Content-Type", "application/x-ofx");
        httpPost.setHeader("Accept", "application/x-ofx");

        String request = outBuffer.toString(Charset.forName("UTF-8").name());

        StringEntity stringEntity = new StringEntity(request, ContentType.create(
                "application/x-ofx", Charset.forName("UTF-8")));
        httpPost.setEntity(stringEntity);

        ResponseHandler<String> handler = new BasicResponseHandler();

        // if not initialzed make an initial request to acquire cookies
        CloseableHttpResponse httpResponse;
        if (!initialized) {
            httpResponse = httpClient.execute(httpPost);
            initialized = true;
        }

        // make the request again with cookies...
        httpResponse = httpClient.execute(httpPost);
        String body = handler.handleResponse(httpResponse);

        InputStream in;
        int responseCode = httpResponse.getStatusLine().getStatusCode();
        if (responseCode >= 200 && responseCode < 300) {
            in = new ByteArrayInputStream(body.getBytes());
        } else if (responseCode >= 400 && responseCode < 500) {
            throw new OFXServerException("Error with client request: "
                    + httpResponse.getStatusLine().getReasonPhrase(), responseCode);
        } else {
            throw new OFXServerException("Invalid response code from OFX server: "
                    + httpResponse.getStatusLine().getReasonPhrase(), responseCode);
        }

        return in;
    }
}
