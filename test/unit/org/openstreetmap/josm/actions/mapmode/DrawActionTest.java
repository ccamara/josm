// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JList;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests for class {@link DrawAction}.
 */
public class DrawActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Non regression test case for bug #12011.
     * Add a new node in the middle of way then undo. The rendering of the node, selected, must not cause any crash in OsmPrimitivRenderer.
     * @throws SecurityException see {@link Class#getDeclaredField} for details
     * @throws NoSuchFieldException see {@link Class#getDeclaredField} for details
     * @throws IllegalAccessException see {@link Field#set} for details
     * @throws IllegalArgumentException see {@link Field#set} for details
     */
    @Test
    public void testTicket12011() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        DataSet dataSet = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(dataSet, OsmDataLayer.createNewName(), null);

        Field mapView = MapFrame.class.getDeclaredField("mapView");
        mapView.setAccessible(true);
        mapView.set(Main.map, new MapViewMock(dataSet, layer));

        Node n1 = new Node(new EastNorth(0, 0));
        Node n2 = new Node(new EastNorth(100, 0));

        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);

        Way w = new Way();
        w.setNodes(Arrays.asList(new Node[] {n1, n2}));
        dataSet.addPrimitive(w);

        Main.main.addLayer(layer);
        try {
            assertTrue(Main.map.selectDrawTool(false));

            Main.map.mapModeDraw.mouseReleased(new MouseEvent(
                    Main.map,
                    MouseEvent.MOUSE_RELEASED,
                    2000,
                    InputEvent.BUTTON1_MASK,
                    50, 0,
                    2, false));

            JList<OsmPrimitive> lstPrimitives = new JList<>();
            OsmPrimitivRenderer renderer = new OsmPrimitivRenderer();

            assertEquals(3, w.getNodesCount());
            Collection<Node> sel = dataSet.getSelectedNodes();
            assertEquals(1, sel.size());

            Node n3 = sel.iterator().next();

            assertNotNull(renderer.getListCellRendererComponent(lstPrimitives, n3, 0, false, false));

            Main.main.undoRedo.undo();

            assertEquals(2, w.getNodesCount());
            assertTrue(dataSet.getSelectedNodes().isEmpty());

            assertNotNull(renderer.getListCellRendererComponent(lstPrimitives, n3, 0, false, false));
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            Main.main.removeLayer(layer);
        }
    }
}
