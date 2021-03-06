package org.codehaus.httpcache4j.resolver.ning;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Response;
import com.ning.http.client.generators.InputStreamBodyGenerator;
import org.codehaus.httpcache4j.*;
import org.codehaus.httpcache4j.auth.Authenticator;
import org.codehaus.httpcache4j.auth.ProxyAuthenticator;
import org.codehaus.httpcache4j.resolver.AbstractResponseResolver;
import org.codehaus.httpcache4j.resolver.ConnectionConfiguration;
import org.codehaus.httpcache4j.resolver.ResolverConfiguration;
import org.codehaus.httpcache4j.resolver.ResponseCreator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.codehaus.httpcache4j.HTTPMethod.*;

/**
 * @author <a href="mailto:erlend@escenic.com">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public class NingResponseResolver extends AbstractResponseResolver {
    private final AsyncHttpClient client;

    protected NingResponseResolver(ResolverConfiguration configuration, AsyncHttpClientConfig asyncConfig) {
        super(configuration);
        AsyncHttpClientConfig.Builder config = new AsyncHttpClientConfig.Builder(Objects.requireNonNull(asyncConfig, "Async config may not be null")).
                setUserAgent(configuration.getUserAgent());
        config.setAllowPoolingConnections(true);
        config.setFollowRedirect(false);
        ConnectionConfiguration connectionConfiguration = configureConnections(configuration, config);
        if (!connectionConfiguration.getConnectionsPerHost().isEmpty()) {
            throw new UnsupportedOperationException("This Resolver does not support connections per host");
        }
        client = new AsyncHttpClient(config.build());
    }

    private ConnectionConfiguration configureConnections(ResolverConfiguration configuration, AsyncHttpClientConfig.Builder config) {
        ConnectionConfiguration connectionConfiguration = configuration.getConnectionConfiguration();
        if (connectionConfiguration.getMaxConnections().isPresent()) {
            config.setMaxConnections(connectionConfiguration.getMaxConnections().get());
        }
        if (connectionConfiguration.getDefaultConnectionsPerHost().isPresent()) {
            config.setMaxConnectionsPerHost(connectionConfiguration.getDefaultConnectionsPerHost().get());
        }
        if (connectionConfiguration.getTimeout().isPresent()) {
            config.setReadTimeout(connectionConfiguration.getTimeout().get());
        }
        if (connectionConfiguration.getSocketTimeout().isPresent()) {
            config.setConnectTimeout(connectionConfiguration.getSocketTimeout().get());
        }
        return connectionConfiguration;
    }

    public NingResponseResolver(ResolverConfiguration configuration) {
        this(configuration, new AsyncHttpClientConfig.Builder().build());
    }

    public NingResponseResolver(ProxyAuthenticator proxyAuthenticator, Authenticator authenticator) {
        this(new ResolverConfiguration(proxyAuthenticator, authenticator, new ConnectionConfiguration()));
    }

    public static NingResponseResolver newInstance(ResolverConfiguration configuration) {
        return new NingResponseResolver(configuration);
    }

    public static NingResponseResolver newInstance(ConnectionConfiguration configuration) {
        return newInstance(new ResolverConfiguration().withConnectionConfiguration(configuration));
    }

    public static NingResponseResolver newInstance() {
        return newInstance(new ConnectionConfiguration());
    }

    @Override
    protected HTTPResponse resolveImpl(HTTPRequest request) throws IOException {
        Future<Response> responseFuture = execute(request);
        return translate(responseFuture);
    }

    public void shutdown() {
        client.close();
    }

    private HTTPResponse translate(Future<Response> responseFuture) throws IOException {
        try {
            Response response = responseFuture.get();
            StatusLine line = new StatusLine(Status.valueOf(response.getStatusCode()), response.getStatusText());
            FluentCaseInsensitiveStringsMap headers = response.getHeaders();
            Optional<InputStream> stream = Optional.ofNullable(response.getResponseBodyAsStream());
            return ResponseCreator.createResponse(line, new Headers(headers), stream);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new HTTPException(e.getCause());
        }

        throw new HTTPException("Not possible to get response");
    }

    private Future<Response> execute(final HTTPRequest request) throws IOException {
        AsyncHttpClient.BoundRequestBuilder builder = builder(request.getNormalizedURI(), request.getMethod());
        if (request.getMethod().canHavePayload()) {
            request.getPayload().ifPresent(p -> {
                if (getConfiguration().isUseChunked()) {
                    builder.setBody(new InputStreamBodyGenerator(p.getInputStream()));
                }
                else {
                    builder.setBody(p.getInputStream());
                }
            });
        }
        for (Header header : request.getAllHeaders()) {
            builder.addHeader(header.getName(), header.getValue());
        }
        return builder.execute();
    }

    private AsyncHttpClient.BoundRequestBuilder builder(URI uri, HTTPMethod method) {
        if (DELETE.equals(method)) {
            return client.prepareDelete(uri.toString());
        }
        else if (GET.equals(method)) {
            return client.prepareGet(uri.toString());
        }
        else if (HEAD.equals(method)) {
            return client.prepareHead(uri.toString());
        }
        else if (OPTIONS.equals(method)) {
            return client.prepareOptions(uri.toString());
        }
        else if (POST.equals(method)) {
            return client.preparePost(uri.toString());
        }
        else if (PUT.equals(method)) {
            return client.preparePut(uri.toString());
        }
        throw new IllegalArgumentException("Unable to create request for method " + method);
    }
}
