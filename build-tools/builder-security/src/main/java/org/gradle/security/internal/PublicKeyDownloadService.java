package org.gradle.security.internal;

import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.io.ExponentialBackoff;
import org.gradle.internal.io.IOQuery;
import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.ExternalResourceReadResult;
import org.gradle.internal.resource.ExternalResourceRepository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.gradle.security.internal.SecuritySupport.toLongIdHexString;

public class PublicKeyDownloadService implements PublicKeyService {
    private final static Logger LOGGER = Logging.getLogger(PublicKeyDownloadService.class);

    private final List<URI> keyServers;
    private final ExternalResourceRepository client;

    public PublicKeyDownloadService(List<URI> keyServers, ExternalResourceRepository client) {
        this.keyServers = keyServers;
        this.client = client;
    }

    @Override
    public void findByLongId(long keyId, PublicKeyResultBuilder builder) {
        List<URI> servers = new ArrayList<>(keyServers);
        Collections.shuffle(servers);
        tryDownloadKeyFromServer(toLongIdHexString(keyId), servers, builder, keyring -> findMatchingKey(keyId, keyring, builder));
    }

    @Override
    public void findByFingerprint(byte[] fingerprint, PublicKeyResultBuilder builder) {
        List<URI> servers = new ArrayList<>(keyServers);
        Collections.shuffle(servers);
        tryDownloadKeyFromServer(Fingerprint.wrap(fingerprint).toString(), servers, builder, keyring -> findMatchingKey(fingerprint, keyring, builder));
    }

    @SuppressWarnings("OptionalAssignedToNull")
    private void tryDownloadKeyFromServer(String fingerprint, List<URI> baseUris, PublicKeyResultBuilder builder, Consumer<? super PGPPublicKeyRing> onKeyring) {
        Deque<URI> serversLeft = new ArrayDeque<>(baseUris);
        try {
            ExponentialBackoff<ExponentialBackoff.Signal> backoff = ExponentialBackoff.of(5, TimeUnit.SECONDS, 50, TimeUnit.MILLISECONDS);
            backoff.retryUntil(() -> {
                URI baseUri = serversLeft.poll();
                if (baseUri == null) {
                    // no more servers left despite retries
                    return IOQuery.Result.successful(false);
                }
                try {
                    ExternalResourceName query = toQuery(baseUri, fingerprint);
                    ExternalResourceReadResult<IOQuery.Result<Boolean>> response = client.resource(query).withContentIfPresent(inputStream -> {
                        extractKeyRing(inputStream, builder, onKeyring);
                        return IOQuery.Result.successful(true);
                    });
                    if (response != null) {
                        return response.getResult();
                    } else {
                        logKeyDownloadAttempt(fingerprint, baseUri);
                        // null means the resource is missing from this repo
                    }
                } catch (Exception e) {
                    logKeyDownloadAttempt(fingerprint, baseUri);
                    // add for retry
                    serversLeft.add(baseUri);
                }
                // retry
                return IOQuery.Result.notSuccessful(false);
            });
        } catch (InterruptedException | IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    /**
     * A response was sent from the server. This is a keyring, we need to find
     * within the keyring the matching key.
     */
    private void findMatchingKey(long id, PGPPublicKeyRing keyRing, PublicKeyResultBuilder builder) {
        for (PGPPublicKey publicKey : keyRing) {
            if (publicKey.getKeyID() == id) {
                builder.publicKey(publicKey);
                return;
            }
        }
    }

    /**
     * A response was sent from the server. This is a keyring, we need to find
     * within the keyring the matching key.
     */
    private void findMatchingKey(byte[] fingerprint, PGPPublicKeyRing keyRing, PublicKeyResultBuilder builder) {
        for (PGPPublicKey publicKey : keyRing) {
            if (Arrays.equals(publicKey.getFingerprint(), fingerprint)) {
                builder.publicKey(publicKey);
                return;
            }
        }
    }

    private void extractKeyRing(InputStream stream, PublicKeyResultBuilder builder, Consumer<? super PGPPublicKeyRing> onKeyring) throws IOException {
        try (InputStream decoderStream = PGPUtil.getDecoderStream(stream)) {
            PGPObjectFactory objectFactory = new PGPObjectFactory(
                decoderStream, new BcKeyFingerprintCalculator());
            PGPPublicKeyRing keyring = (PGPPublicKeyRing) objectFactory.nextObject();
            onKeyring.accept(keyring);
            builder.keyRing(keyring);
        }
    }

    private static void logKeyDownloadAttempt(String fingerprint, URI baseUri) {
        LOGGER.debug("Cannot download public key " + fingerprint + " from " + baseUri.getHost());
    }

    private ExternalResourceName toQuery(URI baseUri, String fingerprint) throws URISyntaxException {
        String scheme = baseUri.getScheme();
        int port = baseUri.getPort();
        if ("hkp".equals(scheme)) {
            scheme = "http";
            port = 11371;
        }
        return new ExternalResourceName(new URI(scheme, null, baseUri.getHost(), port, "/pks/lookup", "op=get&options=mr&search=0x" + fingerprint, null));
    }

    @Override
    public void close() {
    }
}
