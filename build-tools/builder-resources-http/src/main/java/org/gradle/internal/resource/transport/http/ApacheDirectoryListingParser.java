package org.gradle.internal.resource.transport.http;

import com.google.common.base.Charsets;
import org.cyberneko.html.parsers.SAXParser;
import org.gradle.api.resources.ResourceException;
import org.gradle.internal.resource.UriTextResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ApacheDirectoryListingParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApacheDirectoryListingParser.class);

    public List<String> parse(URI baseURI, InputStream content, String contentType) throws Exception {
        baseURI = addTrailingSlashes(baseURI);
        if (contentType == null || !contentType.startsWith("text/html")) {
            throw new ResourceException(baseURI, String.format("Unsupported ContentType %s for directory listing '%s'", contentType, baseURI));
        }
        Charset contentEncoding = UriTextResource.extractCharacterEncoding(contentType, Charsets.UTF_8);
        final Reader htmlText = new InputStreamReader(content, contentEncoding);
        final InputSource inputSource = new InputSource(htmlText);
        final SAXParser htmlParser = new SAXParser();
        final AnchorListerHandler anchorListerHandler = new AnchorListerHandler();
        htmlParser.setContentHandler(anchorListerHandler);
        htmlParser.parse(inputSource);

        List<String> hrefs = anchorListerHandler.getHrefs();
        List<URI> uris = resolveURIs(baseURI, hrefs);
        return filterNonDirectChilds(baseURI, uris);
    }

    private URI addTrailingSlashes(URI uri) throws IOException, URISyntaxException {
        if(uri.getPath() == null){
            uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), "/", uri.getQuery(), uri.getFragment());
        }else if (!uri.getPath().endsWith("/") && !uri.getPath().endsWith(".html")) {
            uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath() + "/", uri.getQuery(), uri.getFragment());

        }
        return uri;
    }

    private List<String> filterNonDirectChilds(URI baseURI, List<URI> inputURIs) throws MalformedURLException {
        final int baseURIPort = baseURI.getPort();
        final String baseURIHost = baseURI.getHost();
        final String baseURIScheme = baseURI.getScheme();

        List<String> uris = new ArrayList<String>();
        final String prefixPath = baseURI.getPath();
        for (URI parsedURI : inputURIs) {
            if (parsedURI.getHost() != null && !parsedURI.getHost().equals(baseURIHost)) {
                continue;
            }
            if (parsedURI.getScheme() != null && !parsedURI.getScheme().equals(baseURIScheme)) {
                continue;
            }
            if (parsedURI.getPort() != baseURIPort) {
                continue;
            }
            if (parsedURI.getPath() != null && !parsedURI.getPath().startsWith(prefixPath)) {
                continue;
            }
            String childPathPart = parsedURI.getPath().substring(prefixPath.length(), parsedURI.getPath().length());
            if (childPathPart.startsWith("../")) {
                continue;
            }
            if (childPathPart.equals("") || childPathPart.split("/").length > 1) {
                continue;
            }

            String path = parsedURI.getPath();
            int pos = path.lastIndexOf('/');
            if (pos < 0) {
                uris.add(path);
            } else if (pos == path.length() - 1) {
                int start = path.lastIndexOf('/', pos - 1);
                if (start < 0) {
                    uris.add(path.substring(0, pos));
                } else {
                    uris.add(path.substring(start + 1, pos));
                }
            } else {
                uris.add(path.substring(pos + 1));
            }
        }
        return uris;
    }

    private List<URI> resolveURIs(URI baseURI, List<String> hrefs) {
        List<URI> uris = new ArrayList<URI>();
        for (String href : hrefs) {
            try {
                uris.add(baseURI.resolve(href));
            } catch (IllegalArgumentException ex) {
                LOGGER.debug("Cannot resolve anchor: {}", href);
            }
        }
        return uris;
    }

    private static class AnchorListerHandler extends DefaultHandler {
        List<String> hrefs = new ArrayList<String>();

        public List<String> getHrefs() {
            return hrefs;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if (qName.equalsIgnoreCase("A")) {
                final String href = atts.getValue("href");
                if (href != null) {
                    hrefs.add(href);
                }
            }
        }
    }
}
