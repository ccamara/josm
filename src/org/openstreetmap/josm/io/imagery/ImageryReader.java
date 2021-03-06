// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryBounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.Shape;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ImageryReader implements Closeable {

    private final String source;
    private CachedFile cachedFile;
    private boolean fastFail;

    private enum State {
        INIT,               // initial state, should always be at the bottom of the stack
        IMAGERY,            // inside the imagery element
        ENTRY,              // inside an entry
        ENTRY_ATTRIBUTE,    // note we are inside an entry attribute to collect the character data
        PROJECTIONS,        // inside projections block of an entry
        MIRROR,             // inside an mirror entry
        MIRROR_ATTRIBUTE,   // note we are inside an mirror attribute to collect the character data
        MIRROR_PROJECTIONS, // inside projections block of an mirror entry
        CODE,
        BOUNDS,
        SHAPE,
        NO_TILE,
        NO_TILESUM,
        METADATA,
        UNKNOWN,            // element is not recognized in the current context
    }

    public ImageryReader(String source) {
        this.source = source;
    }

    public List<ImageryInfo> parse() throws SAXException, IOException {
        Parser parser = new Parser();
        try {
            cachedFile = new CachedFile(source);
            cachedFile.setFastFail(fastFail);
            try (BufferedReader in = cachedFile
                    .setMaxAge(CachedFile.DAYS)
                    .setCachingStrategy(CachedFile.CachingStrategy.IfModifiedSince)
                    .getContentReader()) {
                InputSource is = new InputSource(in);
                Utils.parseSafeSAX(is, parser);
                return parser.entries;
            }
        } catch (SAXException e) {
            throw e;
        } catch (ParserConfigurationException e) {
            Main.error(e); // broken SAXException chaining
            throw new SAXException(e);
        }
    }

    private static class Parser extends DefaultHandler {
        private StringBuilder accumulator = new StringBuilder();

        private Stack<State> states;

        private List<ImageryInfo> entries;

        /**
         * Skip the current entry because it has mandatory attributes
         * that this version of JOSM cannot process.
         */
        private boolean skipEntry;

        private ImageryInfo entry;
        /** In case of mirror parsing this contains the mirror entry */
        private ImageryInfo mirrorEntry;
        private ImageryBounds bounds;
        private Shape shape;
        // language of last element, does only work for simple ENTRY_ATTRIBUTE's
        private String lang;
        private List<String> projections;
        private MultiMap<String, String> noTileHeaders;
        private MultiMap<String, String> noTileChecksums;
        private Map<String, String> metadataHeaders;

        @Override
        public void startDocument() {
            accumulator = new StringBuilder();
            skipEntry = false;
            states = new Stack<>();
            states.push(State.INIT);
            entries = new ArrayList<>();
            entry = null;
            bounds = null;
            projections = null;
            noTileHeaders = null;
            noTileChecksums = null;
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            accumulator.setLength(0);
            State newState = null;
            switch (states.peek()) {
            case INIT:
                if ("imagery".equals(qName)) {
                    newState = State.IMAGERY;
                }
                break;
            case IMAGERY:
                if ("entry".equals(qName)) {
                    entry = new ImageryInfo();
                    skipEntry = false;
                    newState = State.ENTRY;
                    noTileHeaders = new MultiMap<>();
                    noTileChecksums = new MultiMap<>();
                    metadataHeaders = new HashMap<>();
                }
                break;
            case MIRROR:
                if (Arrays.asList(new String[] {
                        "type",
                        "url",
                        "min-zoom",
                        "max-zoom",
                        "tile-size",
                }).contains(qName)) {
                    newState = State.MIRROR_ATTRIBUTE;
                    lang = atts.getValue("lang");
                } else if ("projections".equals(qName)) {
                    projections = new ArrayList<>();
                    newState = State.MIRROR_PROJECTIONS;
                }
                break;
            case ENTRY:
                if (Arrays.asList(new String[] {
                        "name",
                        "id",
                        "type",
                        "description",
                        "default",
                        "url",
                        "eula",
                        "min-zoom",
                        "max-zoom",
                        "attribution-text",
                        "attribution-url",
                        "logo-image",
                        "logo-url",
                        "terms-of-use-text",
                        "terms-of-use-url",
                        "country-code",
                        "icon",
                        "tile-size",
                        "valid-georeference",
                        "epsg4326to3857Supported",
                }).contains(qName)) {
                    newState = State.ENTRY_ATTRIBUTE;
                    lang = atts.getValue("lang");
                } else if ("bounds".equals(qName)) {
                    try {
                        bounds = new ImageryBounds(
                                atts.getValue("min-lat") + ',' +
                                        atts.getValue("min-lon") + ',' +
                                        atts.getValue("max-lat") + ',' +
                                        atts.getValue("max-lon"), ",");
                    } catch (IllegalArgumentException e) {
                        break;
                    }
                    newState = State.BOUNDS;
                } else if ("projections".equals(qName)) {
                    projections = new ArrayList<>();
                    newState = State.PROJECTIONS;
                } else if ("mirror".equals(qName)) {
                    projections = new ArrayList<>();
                    newState = State.MIRROR;
                    mirrorEntry = new ImageryInfo();
                } else if ("no-tile-header".equals(qName)) {
                    noTileHeaders.put(atts.getValue("name"), atts.getValue("value"));
                    newState = State.NO_TILE;
                } else if ("no-tile-checksum".equals(qName)) {
                    noTileChecksums.put(atts.getValue("type"), atts.getValue("value"));
                    newState = State.NO_TILESUM;
                } else if ("metadata-header".equals(qName)) {
                    metadataHeaders.put(atts.getValue("header-name"), atts.getValue("metadata-key"));
                    newState = State.METADATA;
                }
                break;
            case BOUNDS:
                if ("shape".equals(qName)) {
                    shape = new Shape();
                    newState = State.SHAPE;
                }
                break;
            case SHAPE:
                if ("point".equals(qName)) {
                    try {
                        shape.addPoint(atts.getValue("lat"), atts.getValue("lon"));
                    } catch (IllegalArgumentException e) {
                        break;
                    }
                }
                break;
            case PROJECTIONS:
            case MIRROR_PROJECTIONS:
                if ("code".equals(qName)) {
                    newState = State.CODE;
                }
                break;
            }
            /**
             * Did not recognize the element, so the new state is UNKNOWN.
             * This includes the case where we are already inside an unknown
             * element, i.e. we do not try to understand the inner content
             * of an unknown element, but wait till it's over.
             */
            if (newState == null) {
                newState = State.UNKNOWN;
            }
            states.push(newState);
            if (newState == State.UNKNOWN && "true".equals(atts.getValue("mandatory"))) {
                skipEntry = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            accumulator.append(ch, start, length);
        }

        @Override
        public void endElement(String namespaceURI, String qName, String rqName) {
            switch (states.pop()) {
            case INIT:
                throw new RuntimeException("parsing error: more closing than opening elements");
            case ENTRY:
                if ("entry".equals(qName)) {
                    entry.setNoTileHeaders(noTileHeaders);
                    noTileHeaders = null;
                    entry.setNoTileChecksums(noTileChecksums);
                    noTileChecksums = null;
                    entry.setMetadataHeaders(metadataHeaders);
                    metadataHeaders = null;

                    if (!skipEntry) {
                        entries.add(entry);
                    }
                    entry = null;
                }
                break;
            case MIRROR:
                if ("mirror".equals(qName)) {
                    if (mirrorEntry != null) {
                        entry.addMirror(mirrorEntry);
                        mirrorEntry = null;
                    }
                }
                break;
            case MIRROR_ATTRIBUTE:
                if (mirrorEntry != null) {
                    switch(qName) {
                    case "type":
                        boolean found = false;
                        for (ImageryType type : ImageryType.values()) {
                            if (Objects.equals(accumulator.toString(), type.getTypeString())) {
                                mirrorEntry.setImageryType(type);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            mirrorEntry = null;
                        }
                        break;
                    case "url":
                        mirrorEntry.setUrl(accumulator.toString());
                        break;
                    case "min-zoom":
                    case "max-zoom":
                        Integer val = null;
                        try {
                            val = Integer.valueOf(accumulator.toString());
                        } catch (NumberFormatException e) {
                            val = null;
                        }
                        if (val == null) {
                            mirrorEntry = null;
                        } else {
                            if ("min-zoom".equals(qName)) {
                                mirrorEntry.setDefaultMinZoom(val);
                            } else {
                                mirrorEntry.setDefaultMaxZoom(val);
                            }
                        }
                        break;
                    case "tile-size":
                        Integer tileSize = null;
                        try {
                            tileSize = Integer.valueOf(accumulator.toString());
                        } catch (NumberFormatException e) {
                            tileSize = null;
                        }
                        if (tileSize == null) {
                            mirrorEntry = null;
                        } else {
                            entry.setTileSize(tileSize.intValue());
                        }
                        break;
                    }
                }
                break;
            case ENTRY_ATTRIBUTE:
                switch(qName) {
                case "name":
                    entry.setName(lang == null ? LanguageInfo.getJOSMLocaleCode(null) : lang, accumulator.toString());
                    break;
                case "description":
                    entry.setDescription(lang, accumulator.toString());
                    break;
                case "id":
                    entry.setId(accumulator.toString());
                    break;
                case "type":
                    boolean found = false;
                    for (ImageryType type : ImageryType.values()) {
                        if (Objects.equals(accumulator.toString(), type.getTypeString())) {
                            entry.setImageryType(type);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        skipEntry = true;
                    }
                    break;
                case "default":
                    switch (accumulator.toString()) {
                    case "true":
                        entry.setDefaultEntry(true);
                        break;
                    case "false":
                        entry.setDefaultEntry(false);
                        break;
                    default:
                        skipEntry = true;
                    }
                    break;
                case "url":
                    entry.setUrl(accumulator.toString());
                    break;
                case "eula":
                    entry.setEulaAcceptanceRequired(accumulator.toString());
                    break;
                case "min-zoom":
                case "max-zoom":
                    Integer val = null;
                    try {
                        val = Integer.valueOf(accumulator.toString());
                    } catch (NumberFormatException e) {
                        val = null;
                    }
                    if (val == null) {
                        skipEntry = true;
                    } else {
                        if ("min-zoom".equals(qName)) {
                            entry.setDefaultMinZoom(val);
                        } else {
                            entry.setDefaultMaxZoom(val);
                        }
                    }
                    break;
                case "attribution-text":
                    entry.setAttributionText(accumulator.toString());
                    break;
                case "attribution-url":
                    entry.setAttributionLinkURL(accumulator.toString());
                    break;
                case "logo-image":
                    entry.setAttributionImage(accumulator.toString());
                    break;
                case "logo-url":
                    entry.setAttributionImageURL(accumulator.toString());
                    break;
                case "terms-of-use-text":
                    entry.setTermsOfUseText(accumulator.toString());
                    break;
                case "terms-of-use-url":
                    entry.setTermsOfUseURL(accumulator.toString());
                    break;
                case "country-code":
                    entry.setCountryCode(accumulator.toString());
                    break;
                case "icon":
                    entry.setIcon(accumulator.toString());
                    break;
                case "tile-size":
                    Integer tileSize = null;
                    try {
                        tileSize = Integer.valueOf(accumulator.toString());
                    } catch (NumberFormatException e) {
                        tileSize = null;
                    }
                    if (tileSize == null) {
                        skipEntry = true;
                    } else {
                        entry.setTileSize(tileSize.intValue());
                    }
                    break;
                case "valid-georeference":
                    entry.setGeoreferenceValid(Boolean.valueOf(accumulator.toString()));
                    break;
                case "epsg4326to3857Supported":
                    entry.setEpsg4326To3857Supported(Boolean.valueOf(accumulator.toString()));
                    break;
                }
                break;
            case BOUNDS:
                entry.setBounds(bounds);
                bounds = null;
                break;
            case SHAPE:
                bounds.addShape(shape);
                shape = null;
                break;
            case CODE:
                projections.add(accumulator.toString());
                break;
            case PROJECTIONS:
                entry.setServerProjections(projections);
                projections = null;
                break;
            case MIRROR_PROJECTIONS:
                mirrorEntry.setServerProjections(projections);
                projections = null;
                break;
            /* nothing to do for these or the unknown type:
            case NO_TILE:
            case NO_TILESUM:
            case METADATA:
            case UNKNOWN:
                break;
            */
            }
        }
    }

    /**
     * Sets whether opening HTTP connections should fail fast, i.e., whether a
     * {@link HttpClient#setConnectTimeout(int) low connect timeout} should be used.
     * @param fastFail whether opening HTTP connections should fail fast
     * @see CachedFile#setFastFail(boolean)
     */
    public void setFastFail(boolean fastFail) {
        this.fastFail = fastFail;
    }

    @Override
    public void close() throws IOException {
        Utils.close(cachedFile);
    }
}
