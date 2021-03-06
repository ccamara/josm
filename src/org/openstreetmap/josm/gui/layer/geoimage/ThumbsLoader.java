// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.imageio.ImageIO;

import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.tools.ExifReader;

public class ThumbsLoader implements Runnable {
    public static final int maxSize = 120;
    public static final int minSize = 22;
    public volatile boolean stop;
    private final Collection<ImageEntry> data;
    private final GeoImageLayer layer;
    private MediaTracker tracker;
    private ICacheAccess<String, BufferedImageCacheEntry> cache;
    private final boolean cacheOff = Main.pref.getBoolean("geoimage.noThumbnailCache", false);

    private ThumbsLoader(Collection<ImageEntry> data, GeoImageLayer layer) {
        this.data = data;
        this.layer = layer;
        initCache();
    }

    /**
     * Constructs a new thumbnail loader that operates on a geoimage layer.
     * @param layer geoimage layer
     */
    public ThumbsLoader(GeoImageLayer layer) {
        this(new ArrayList<>(layer.data), layer);
    }

    /**
     * Constructs a new thumbnail loader that operates on the image entries
     * @param entries image entries
     */
    public ThumbsLoader(Collection<ImageEntry> entries) {
        this(entries, null);
    }

    /**
     * Initialize the thumbnail cache.
     */
    private void initCache() {
        if (!cacheOff) {
            try {
                cache = JCSCacheManager.getCache("geoimage-thumbnails", 0, 120,
                        Main.pref.getCacheDirectory().getPath() + File.separator + "geoimage-thumbnails");
            } catch (IOException e) {
                Main.warn("Failed to initialize cache for geoimage-thumbnails");
                Main.warn(e);
            }
        }
    }

    @Override
    public void run() {
        Main.debug("Load Thumbnails");
        tracker = new MediaTracker(Main.map.mapView);
        for (ImageEntry entry : data) {
            if (stop) return;

            // Do not load thumbnails that were loaded before.
            if (!entry.hasThumbnail()) {
                entry.setThumbnail(loadThumb(entry));

                if (layer != null && Main.isDisplayingMapView()) {
                    layer.updateOffscreenBuffer = true;
                    Main.map.mapView.repaint();
                }
            }
        }
        if (layer != null) {
            layer.thumbsLoaded();
            layer.updateOffscreenBuffer = true;
            Main.map.mapView.repaint();
        }
    }

    private BufferedImage loadThumb(ImageEntry entry) {
        final String cacheIdent = entry.getFile()+":"+maxSize;

        if (!cacheOff && cache != null) {
            try {
                BufferedImageCacheEntry cacheEntry = cache.get(cacheIdent);
                if (cacheEntry != null && cacheEntry.getImage() != null) {
                    Main.debug(" from cache");
                    return cacheEntry.getImage();
                }
            } catch (IOException e) {
                Main.warn(e);
            }
        }

        Image img = Toolkit.getDefaultToolkit().createImage(entry.getFile().getPath());
        tracker.addImage(img, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException e) {
            Main.error(" InterruptedException while loading thumb");
            return null;
        }
        if (tracker.isErrorID(1) || img.getWidth(null) <= 0 || img.getHeight(null) <= 0) {
            Main.error(" Invalid image");
            return null;
        }

        final int w = img.getWidth(null);
        final int h = img.getHeight(null);
        final int hh, ww;
        final Integer exifOrientation = entry.getExifOrientation();
        if (exifOrientation != null && ExifReader.orientationSwitchesDimensions(exifOrientation)) {
            ww = h;
            hh = w;
        } else {
            ww = w;
            hh = h;
        }

        Rectangle targetSize = ImageDisplay.calculateDrawImageRectangle(
                new Rectangle(0, 0, ww, hh),
                new Rectangle(0, 0, maxSize, maxSize));
        BufferedImage scaledBI = new BufferedImage(targetSize.width, targetSize.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledBI.createGraphics();

        final AffineTransform scale = AffineTransform.getScaleInstance((double) targetSize.width / ww, (double) targetSize.height / hh);
        if (exifOrientation != null) {
            final AffineTransform restoreOrientation = ExifReader.getRestoreOrientationTransform(exifOrientation, w, h);
            scale.concatenate(restoreOrientation);
        }

        while (!g.drawImage(img, scale, null)) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ie) {
                Main.warn("InterruptedException while drawing thumb");
            }
        }
        g.dispose();
        tracker.removeImage(img);

        if (scaledBI.getWidth() <= 0 || scaledBI.getHeight() <= 0) {
            Main.error(" Invalid image");
            return null;
        }

        if (!cacheOff && cache != null) {
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                ImageIO.write(scaledBI, "png", output);
                cache.put(cacheIdent, new BufferedImageCacheEntry(output.toByteArray()));
            } catch (IOException e) {
                Main.warn("Failed to save geoimage thumb to cache");
                Main.warn(e);
            }
        }

        return scaledBI;
    }
}
