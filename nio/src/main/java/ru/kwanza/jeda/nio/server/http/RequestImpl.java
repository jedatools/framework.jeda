package ru.kwanza.jeda.nio.server.http;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.attributes.AttributeHolder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.*;
import org.glassfish.grizzly.http.util.MimeHeaders;
import org.glassfish.grizzly.http.util.Parameters;
import org.glassfish.grizzly.utils.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.*;

import static org.glassfish.grizzly.http.util.Constants.FORM_POST_CONTENT_TYPE;

/**
 * @author Guzanov Alexander
 */
final class RequestImpl extends EmptyCompletionHandler<WriteResult> implements IHttpRequest {

    private static final Logger log = LoggerFactory.getLogger(RequestImpl.class);

    private static final Charset DEFAULT_HTTP_CHARSET = Charsets.lookupCharset("UTF-8");

    FilterChainContext context;
    ITimedOutHandler timedOutHandler;
    volatile boolean finished = false;
    long suspendedTimestamp = -1l;
    long timeout;
    private EntryPoint entryPoint;
    private HttpContent content;
    private HttpServer httpServer;
    private RequestID id;
    private String uri;
    private long timestamp;

    private String remoteAddress;

    private Map<String, String> requestHeaders;
    private Map<String, String> requestAttributes;
    private Map<String, String> requestParameters;

    private Parameters internalParameters = new Parameters();

    private Collection<Cookie> cookies;
    private Cookies rawCookies;

    private byte[] body;

    private boolean remoteAddressParsed;
    private boolean internalParametersParsed;
    private boolean requestParametersParsed;
    private boolean requestHeadersParsed;
    private boolean requestAttributesParsed;
    private boolean cookiesParsed;
    private boolean bodyParsed;

    RequestImpl(String uri, FilterChainContext context, HttpServer httpServer, EntryPoint entryPoint, HttpContent content,
                ITimedOutHandler timedOutHandler, long timeout) {
        this.id = new RequestID(httpServer.nextUID(), httpServer.getName());
        this.context = context;
        this.content = content;
        this.uri = uri;
        this.timestamp = System.currentTimeMillis();
        this.httpServer = httpServer;
        this.entryPoint = entryPoint;
        this.timeout = timeout;
        this.timedOutHandler = timedOutHandler;
    }

    @Override
    public void cancelled() {
        resume();
    }

    @Override
    public void failed(Throwable throwable) {
        resume();
    }

    @Override
    public void completed(WriteResult result) {
        resume();
    }

    public RequestID getID() {
        return id;
    }

    public String getUri() {
        return uri;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getRemoteAddress() {
        if (!remoteAddressParsed) {
            parseRemoteAddress();
        }
        return remoteAddress;
    }

    public HttpContent getContent() {
        return content;
    }

    public final boolean finish(HttpPacket result) {
        if (finished) {
            if (HttpServer.logger.isDebugEnabled()) {
                HttpServer.logger
                        .trace("HttpServer({},{}) : Can't send Response." + " Asynchronous request(id={},uri={}) is finished already!",
                                new Object[]{httpServer.getName(), entryPoint.getName(), id, uri});
            }
            return false;
        }
        if (HttpServer.logger.isDebugEnabled()) {
            if (HttpServer.logger.isTraceEnabled()) {
                HttpServer.logger.trace("HttpServer({},{}) : Response to  Asynchronous request(id={},uri={}) in {}, result: {}",
                        new Object[]{httpServer.getName(), entryPoint.getName(), id, uri, System.currentTimeMillis() - timestamp,
                                TraceUtil.getDescription(result)});
            } else {
                HttpServer.logger.debug("HttpServer({},{}) : Response to  Asynchronous request(id={},uri={}) in {}",
                        new Object[]{httpServer.getName(), entryPoint.getName(), id, uri, System.currentTimeMillis() - timestamp});
            }
        }

        httpServer.finish(this, result);
        return true;
    }

    public Map<String, String> getHeaderMap() {
        if (!requestHeadersParsed) {
            parseRequestHeaders();
        }
        return requestHeaders;
    }

    public String getHeader(String name) {
        if (!requestHeadersParsed) {
            parseRequestHeaders();
        }
        return requestHeaders.get(name);
    }

    public Map<String, String> getAttributeMap() {
        if (!requestAttributesParsed) {
            parseRequestAttributes();
        }
        return requestAttributes;
    }

    public String getAttribute(String name) {
        if (!requestAttributesParsed) {
            parseRequestAttributes();
        }
        return requestAttributes.get(name);
    }

    public Map<String, String> getParameterMap() {
        if (!requestParametersParsed) {
            parseRequestParameters();
        }
        return requestParameters;
    }

    public String getParameter(String name) {
        if (!internalParametersParsed) {
            parseInternalParameters();
        }
        return internalParameters.getParameter(name);
    }

    private Set<String> getParameterNames() {
        if (!internalParametersParsed) {
            parseInternalParameters();
        }
        return internalParameters.getParameterNames();
    }

    private String[] getParameterValues(String name) {
        if (!internalParametersParsed) {
            parseInternalParameters();
        }
        return internalParameters.getParameterValues(name);
    }

    public Collection<Cookie> getCookies() {
        if (!cookiesParsed) {
            parseCookies();
        }
        return Collections.unmodifiableCollection(cookies);
    }

    public byte[] getBody() {
        if (!bodyParsed) {
            parseBody();
        }
        return body;
    }

    private void parseRemoteAddress() {
        final Connection connection = context != null ? context.getConnection() : null;
        final Object peerAddress = connection != null ? connection.getPeerAddress() : null;
        InetSocketAddress inetSocketAddress = null;
        if (peerAddress instanceof InetSocketAddress) {
            inetSocketAddress = (InetSocketAddress) peerAddress;
        }
        remoteAddress = inetSocketAddress != null ? inetSocketAddress.getHostName() : null;
        remoteAddressParsed = true;
    }

    private void parseRequestHeaders() {
        final HttpHeader httpHeader = content != null ? content.getHttpHeader() : null;
        final MimeHeaders mimeHeaders = httpHeader != null ? httpHeader.getHeaders() : null;
        final Map<String, String> resultMap = new LinkedHashMap<String, String>();
        if (mimeHeaders != null) {
            for (String headerName : mimeHeaders.names()) {
                resultMap.put(headerName, httpHeader.getHeader(headerName));
            }
        }
        requestHeaders = Collections.unmodifiableMap(resultMap);
        requestHeadersParsed = true;
    }

    private void parseRequestAttributes() {
        final HttpHeader httpHeader = content != null ? content.getHttpHeader() : null;
        final AttributeHolder attributes = httpHeader != null ? httpHeader.getAttributes() : null;
        final Set<String> attributeSet = attributes != null ? attributes.getAttributeNames() : null;
        final Map<String, String> resultMap = new LinkedHashMap<String, String>();
        if (attributeSet != null) {
            for (String attributeName : attributeSet) {
                resultMap.put(attributeName, attributes.getAttribute(attributeName).toString());
            }
        }
        requestAttributes = Collections.unmodifiableMap(resultMap);
        requestAttributesParsed = true;
    }

    private void parseRequestParameters() {
        final Map<String, String> resultMap = new LinkedHashMap<String, String>();
        for (final String name : getParameterNames()) {
            final String[] values = getParameterValues(name);
            final String value = values != null ? (values.length == 0 ? "" : values[0]) : null;
            resultMap.put(name, value);
        }
        requestParameters = Collections.unmodifiableMap(resultMap);
        requestParametersParsed = true;
    }

    private void parseInternalParameters() {
        final HttpHeader httpHeader = content.getHttpHeader();
        HttpRequestPacket httpRequestPacket = null;
        if (httpHeader instanceof HttpRequestPacket) {
            httpRequestPacket = (HttpRequestPacket) httpHeader;
        }

        if (httpRequestPacket != null) {
            internalParameters.setQuery(httpRequestPacket.getQueryStringDC());
        }

        final String encoding = content.getHttpHeader().getCharacterEncoding();

        Charset charset;
        if (encoding != null) {
            try {
                charset = Charsets.lookupCharset(encoding);
            } catch (Exception e) {
                charset = DEFAULT_HTTP_CHARSET;
            }
        } else {
            charset = DEFAULT_HTTP_CHARSET;
        }

        internalParameters.setHeaders(content.getHttpHeader().getHeaders());
        internalParameters.setEncoding(charset);
        internalParameters.setQueryStringEncoding(charset);

        internalParameters.handleQueryParameters();

        if (httpRequestPacket != null && !Method.POST.equals(httpRequestPacket.getMethod())) {
            return;
        }

        final int length = (int) content.getHttpHeader().getContentLength();

        if (length > 0) {
            if (!checkPostContentType(content.getHttpHeader().getContentType())) {
                return;
            }

            try {
                final Buffer formData = content.getContent();
                internalParameters.processParameters(formData, formData.position(), length);
            } catch (Exception e) {
                log.error("Error while processing parameters", e);
            }
        }
        internalParametersParsed = true;
    }

    private void parseCookies() {
        cookies = new LinkedList<Cookie>(getRawCookies().get());
        cookiesParsed = true;
    }

    private Cookies getRawCookies() {
        if (rawCookies == null) {
            rawCookies = new Cookies();
        }
        if (!rawCookies.initialized()) {
            rawCookies.setHeaders(content.getHttpHeader().getHeaders());
        }
        return rawCookies;
    }

    private void parseBody() {
        final Buffer contentBuffer = content != null ? content.getContent() : null;
        final byte[] buffer = contentBuffer != null ? new byte[contentBuffer.capacity()] : null;
        if (buffer != null) {
            contentBuffer.get(buffer);
        }
        body = buffer;
        bodyParsed = true;
    }

    private boolean checkPostContentType(String contentType) {
        return contentType != null && contentType.trim().startsWith(FORM_POST_CONTENT_TYPE);
    }

    private void resume() {
        context.setMessage(this);
        context.resume();
    }
}
