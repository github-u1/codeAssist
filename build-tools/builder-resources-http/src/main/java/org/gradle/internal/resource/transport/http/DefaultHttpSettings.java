package org.gradle.internal.resource.transport.http;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.gradle.authentication.Authentication;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.verifier.HttpRedirectVerifier;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import cz.msebera.android.httpclient.conn.ssl.StrictHostnameVerifier;

@SuppressWarnings("DEPRECATION")
public class DefaultHttpSettings implements HttpSettings {
    private final Collection<Authentication> authenticationSettings;
    private final SslContextFactory sslContextFactory;
    private final HostnameVerifier hostnameVerifier;
    private final HttpRedirectVerifier redirectVerifier;
    private final int maxRedirects;
    private final RedirectMethodHandlingStrategy redirectMethodHandlingStrategy;

    private HttpProxySettings proxySettings;
    private HttpProxySettings secureProxySettings;
    private HttpTimeoutSettings timeoutSettings;

    public static Builder builder() {
        return new Builder();
    }

    private DefaultHttpSettings(
        Collection<Authentication> authenticationSettings,
        SslContextFactory sslContextFactory,
        HostnameVerifier hostnameVerifier,
        HttpRedirectVerifier redirectVerifier,
        RedirectMethodHandlingStrategy redirectMethodHandlingStrategy, int maxRedirects
    ) {
        Preconditions.checkArgument(maxRedirects >= 0, "maxRedirects must be positive");
        Preconditions.checkNotNull(authenticationSettings, "authenticationSettings");
        Preconditions.checkNotNull(sslContextFactory, "sslContextFactory");
        Preconditions.checkNotNull(hostnameVerifier, "hostnameVerifier");
        Preconditions.checkNotNull(redirectVerifier, "redirectVerifier");
        Preconditions.checkNotNull(redirectMethodHandlingStrategy, "redirectMethodHandlingStrategy");

        this.maxRedirects = maxRedirects;
        this.authenticationSettings = authenticationSettings;
        this.sslContextFactory = sslContextFactory;
        this.hostnameVerifier = hostnameVerifier;
        this.redirectVerifier = redirectVerifier;
        this.redirectMethodHandlingStrategy = redirectMethodHandlingStrategy;
    }

    @Override
    public HttpProxySettings getProxySettings() {
        if (proxySettings == null) {
            proxySettings = new JavaSystemPropertiesHttpProxySettings();
        }
        return proxySettings;
    }

    @Override
    public HttpProxySettings getSecureProxySettings() {
        if (secureProxySettings == null) {
            secureProxySettings = new JavaSystemPropertiesSecureHttpProxySettings();
        }
        return secureProxySettings;
    }

    @Override
    public HttpTimeoutSettings getTimeoutSettings() {
        if (timeoutSettings == null) {
            timeoutSettings = new JavaSystemPropertiesHttpTimeoutSettings();
        }
        return timeoutSettings;
    }

    @Override
    public int getMaxRedirects() {
        return maxRedirects;
    }

    @Override
    public HttpRedirectVerifier getRedirectVerifier() {
        return redirectVerifier;
    }

    @Override
    public RedirectMethodHandlingStrategy getRedirectMethodHandlingStrategy() {
        return redirectMethodHandlingStrategy;
    }

    @Override
    public Collection<Authentication> getAuthenticationSettings() {
        return authenticationSettings;
    }

    @Override
    public SslContextFactory getSslContextFactory() {
        return sslContextFactory;
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public static class Builder {
        private Collection<Authentication> authenticationSettings;
        private SslContextFactory sslContextFactory;
        private HostnameVerifier hostnameVerifier;
        private HttpRedirectVerifier redirectVerifier;
        private int maxRedirects = 10;
        private RedirectMethodHandlingStrategy redirectMethodHandlingStrategy = RedirectMethodHandlingStrategy.ALWAYS_FOLLOW_AND_PRESERVE;

        public Builder withAuthenticationSettings(Collection<Authentication> authenticationSettings) {
            this.authenticationSettings = authenticationSettings;
            return this;
        }

        public Builder withSslContextFactory(SslContextFactory sslContextFactory) {
            this.sslContextFactory = sslContextFactory;
            this.hostnameVerifier = new StrictHostnameVerifier();
            return this;
        }

        public Builder withRedirectVerifier(HttpRedirectVerifier redirectVerifier) {
            this.redirectVerifier = redirectVerifier;
            return this;
        }

        public Builder allowUntrustedConnections() {
            this.sslContextFactory = ALL_TRUSTING_SSL_CONTEXT_FACTORY;
            this.hostnameVerifier = ALL_TRUSTING_HOSTNAME_VERIFIER;
            return this;
        }

        public Builder maxRedirects(int maxRedirects) {
            Preconditions.checkArgument(maxRedirects >= 0);
            this.maxRedirects = maxRedirects;
            return this;
        }

        public Builder withRedirectMethodHandlingStrategy(RedirectMethodHandlingStrategy redirectMethodHandlingStrategy) {
            this.redirectMethodHandlingStrategy = redirectMethodHandlingStrategy;
            return this;
        }

        public HttpSettings build() {
            return new DefaultHttpSettings(authenticationSettings, sslContextFactory, hostnameVerifier, redirectVerifier, redirectMethodHandlingStrategy, maxRedirects);
        }
    }

    private static final HostnameVerifier ALL_TRUSTING_HOSTNAME_VERIFIER = (hostname, session) -> true;

    private static final SslContextFactory ALL_TRUSTING_SSL_CONTEXT_FACTORY = new SslContextFactory() {
        private final java.util.function.Supplier<SSLContext> sslContextSupplier = Suppliers.memoize(new Supplier<SSLContext>() {
            @Override
            public SSLContext get() {
                try {
                    SSLContext sslcontext = SSLContext.getInstance("TLS");
                    sslcontext.init(null, allTrustingTrustManager, null);
                    return sslcontext;
                } catch (GeneralSecurityException e) {
                    throw UncheckedException.throwAsUncheckedException(e);
                }
            }
        });

        @Override
        public SSLContext createSslContext() {
            return sslContextSupplier.get();
        }

        private final TrustManager[] allTrustingTrustManager = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
        };
    };

}
