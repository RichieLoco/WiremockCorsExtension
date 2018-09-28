package extension;

import java.util.Arrays;
import java.util.List;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

public class CORSResponseHeaderTransformer extends ResponseDefinitionTransformer {

    private static final List<String> EXCLUDE_HEADER_KEYS = Arrays.asList("Cache-Control", "Connection", "User-Agent",
            "Postman-Token", "Accept-Encoding", "Accept-Language");
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ACCEPT = "Accept";
    private static final String DEFAULT_CONTENT_TYPE = "application/json";

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files,
            Parameters parameters) {
        logToStdout("CORSResponseHeaderTransformer -- START");
        ResponseDefinitionBuilder resBuilder = ResponseDefinitionBuilder.like(responseDefinition);
        ResponseDefinition response = null;
        try {
            logRequestVariables(request);
            addAccessControlAllowHeaders(resBuilder, request);
            addContentTypeHeader(resBuilder, responseDefinition, request);
            addStaticCorsHeaders(resBuilder);
            response = resBuilder.build();
            logToStdout(" -- 'All Response Headers' --> " + response.getHeaders());
        } catch (Exception e) {
            logToStdout(" ERROR - " + e);
        }
        logToStdout("CORSResponseHeaderTransformer -- END");
        return response;
    }

    @Override
    public String getName() {
        return "CORSResponseHeaderTransformer";
    }

    private void addStaticCorsHeaders(ResponseDefinitionBuilder resBuilder) {
        resBuilder.withHeader("Access-Control-Allow-Origin", "*").withHeader("Access-Control-Allow-Methods", "*")
                .withHeader("X-Content-Type-Options", "nosniff").withHeader("x-frame-options", "DENY")
                .withHeader("x-xss-protection", "1; mode=block");
    }

    private void addContentTypeHeader(ResponseDefinitionBuilder resBuilder, ResponseDefinition responseDefinition,
            Request req) {

        // ...if mapping file contains Content-Type as part of the stubbed response header(s) then we pass that through
        if (null != responseDefinition.getHeaders() && null != responseDefinition.getHeaders().getContentTypeHeader()
                && responseDefinition.getHeaders().getContentTypeHeader().key().equalsIgnoreCase(CONTENT_TYPE)) {
            resBuilder.withHeader(CONTENT_TYPE, responseDefinition.getHeaders().getContentTypeHeader().mimeTypePart());
        }
        // ... else we just take in the Content-Type that might have been sent in as part of the request header
        else if (req.getHeader(CONTENT_TYPE) != null) {
            resBuilder.withHeader(CONTENT_TYPE, req.getHeader(CONTENT_TYPE));
        }
        // ... some specs by default won't have a Content-Type header (GET requests, etc...), so default it to the value
        // of the Accept header
        else if (req.getHeader(ACCEPT) != null && !req.getHeader(ACCEPT).equals("*/*")) {
            resBuilder.withHeader(CONTENT_TYPE, req.getHeader(ACCEPT));
        }
        // ... if a GET request and Content-Type not supplied then we have no way of knowing! So, default to
        // "text/plain"
        else {
            resBuilder.withHeader(CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
        }
    }

    private void addAccessControlAllowHeaders(ResponseDefinitionBuilder resBuilder, Request req) {
        HttpHeaders headers = req.getHeaders();
        if (headers != null) {
            StringBuilder sb = new StringBuilder();
            for (String key : headers.keys()) {
                if (!EXCLUDE_HEADER_KEYS.contains(key)) {
                    sb.append(key).append(",");
                }
            }

            logToStdout(" -- 'Access-Control-Request-Headers' --> " + req.getHeader("Access-Control-Request-Headers"));

            if (req.getHeader("Access-Control-Request-Headers") != null) {
                sb.append(req.getHeader("Access-Control-Request-Headers")).append(",");
            }
            sb.append("Content-Encoding, Server, Transfer-Encoding, Content-Type");
            resBuilder.withHeader("Access-Control-Allow-Headers", sb.toString());
            logToStdout(" -- 'Access-Control-Allow-Headers' --> " + sb.toString());
        }
    }

    private static void logRequestVariables(Request request) {
        logToStdout(" -- 'Request Method' --> " + request.getMethod().getName());
        logToStdout(" -- 'Origin Header' --> " + request.getHeader("Origin"));
        logToStdout(" -- 'Access-Control-Request-Method Header' --> "
                + request.getHeader("Access-Control-Request-Method"));
        logToStdout(" -- 'Access-Control-Request-Headers Header' --> "
                + request.getHeader("Access-Control-Request-Headers"));
        logToStdout(" -- 'All Request Headers' --> " + request.getAllHeaderKeys());
    }

    private static final void logToStdout(String message) {
        System.out.println(message);
    }
}