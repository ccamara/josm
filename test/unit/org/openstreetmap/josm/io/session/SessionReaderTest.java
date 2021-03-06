// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.NoteLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.io.IllegalDataException;

/**
 * Unit tests for Session reading.
 */
public class SessionReaderTest {

    /**
     * Setup tests.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    private static String getSessionDataDir() {
        return TestUtils.getTestDataRoot() + "/sessions";
    }

    private List<Layer> testRead(String sessionFileName) throws IOException, IllegalDataException {
        boolean zip = sessionFileName.endsWith(".joz");
        File file = new File(getSessionDataDir()+"/"+sessionFileName);
        SessionReader reader = new SessionReader();
        reader.loadSession(file, zip, null);
        return reader.getLayers();
    }

    /**
     * Tests to read an empty .jos or .joz file.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    public void testReadEmpty() throws IOException, IllegalDataException {
        assertTrue(testRead("empty.jos").isEmpty());
        assertTrue(testRead("empty.joz").isEmpty());
    }

    /**
     * Tests to read a .jos or .joz file containing OSM data.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    public void testReadOsm() throws IOException, IllegalDataException {
        for (String file : new String[]{"osm.jos", "osm.joz"}) {
            List<Layer> layers = testRead(file);
            assertSame(layers.size(), 1);
            assertTrue(layers.get(0) instanceof OsmDataLayer);
            OsmDataLayer osm = (OsmDataLayer) layers.get(0);
            assertEquals(osm.getName(), "OSM layer name");
        }
    }

    /**
     * Tests to read a .jos or .joz file containing GPX data.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    public void testReadGpx() throws IOException, IllegalDataException {
        for (String file : new String[]{"gpx.jos", "gpx.joz", "nmea.jos"}) {
            List<Layer> layers = testRead(file);
            assertSame(layers.size(), 1);
            assertTrue(layers.get(0) instanceof GpxLayer);
            GpxLayer gpx = (GpxLayer) layers.get(0);
            assertEquals(gpx.getName(), "GPX layer name");
        }
    }

    /**
     * Tests to read a .joz file containing GPX and marker data.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    public void testReadGpxAndMarker() throws IOException, IllegalDataException {
        List<Layer> layers = testRead("gpx_markers.joz");
        assertSame(layers.size(), 2);
        GpxLayer gpx = null;
        MarkerLayer marker = null;
        for (Layer layer : layers) {
            if (layer instanceof GpxLayer) {
                gpx = (GpxLayer) layer;
            } else if (layer instanceof MarkerLayer) {
                marker = (MarkerLayer) layer;
            }
        }
        assertNotNull(gpx);
        assertNotNull(marker);
        assertEquals(gpx.getName(), "GPX layer name");
        assertEquals(marker.getName(), "Marker layer name");
    }

    /**
     * Tests to read a .jos file containing Bing imagery.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    public void testReadImage() throws IOException, IllegalDataException {
        final List<Layer> layers = testRead("bing.jos");
        assertSame(layers.size(), 1);
        assertTrue(layers.get(0) instanceof ImageryLayer);
        final ImageryLayer image = (ImageryLayer) layers.get(0);
        assertEquals("Bing aerial imagery", image.getName());
        assertEquals(image.getDx(), 12.34, 1e-9);
        assertEquals(image.getDy(), -56.78, 1e-9);
    }

    /**
     * Tests to read a .joz file containing notes.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException is the test file is considered as invalid
     */
    @Test
    public void testReadNotes() throws IOException, IllegalDataException {
        if (Main.isDisplayingMapView()) {
            for (NoteLayer nl : Main.map.mapView.getLayersOfType(NoteLayer.class)) {
                Main.map.mapView.removeLayer(nl);
            }
        }
        final List<Layer> layers = testRead("notes.joz");
        assertSame(layers.size(), 1);
        assertTrue(layers.get(0) instanceof NoteLayer);
        final NoteLayer layer = (NoteLayer) layers.get(0);
        assertEquals("Notes", layer.getName());
        assertEquals(174, layer.getNoteData().getNotes().size());
    }
}
