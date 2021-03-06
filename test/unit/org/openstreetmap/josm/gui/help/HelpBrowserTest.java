// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.tools.LanguageInfo.LocaleType;

/**
 * Unit tests of {@link HelpBrowser} class.
 */
public class HelpBrowserTest {

    static final String URL_1 = "https://josm.openstreetmap.de/wiki/Help";
    static final String URL_2 = "https://josm.openstreetmap.de/wiki/Introduction";
    static final String URL_3 = "https://josm.openstreetmap.de/javadoc";

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    static IHelpBrowser newHelpBrowser() {
        return new IHelpBrowser() {

            private final HelpBrowserHistory history = new HelpBrowserHistory(this);
            private String url;

            @Override
            public void openUrl(String url) {
                history.setCurrentUrl(url);
                this.url = url;
            }

            @Override
            public void openHelpTopic(String relativeHelpTopic) {
                openUrl(HelpUtil.getHelpTopicUrl(HelpUtil.buildAbsoluteHelpTopic(relativeHelpTopic, LocaleType.ENGLISH)));
            }

            @Override
            public String getUrl() {
                return url;
            }

            @Override
            public HelpBrowserHistory getHistory() {
                return history;
            }
        };
    }

    /**
     * Unit test of {@link HelpBrowser.BackAction} and {@link HelpBrowser.ForwardAction} classes.
     */
    @Test
    public void testBackAndForwardActions() {
        IHelpBrowser browser = newHelpBrowser();
        browser.openUrl(URL_1);
        assertEquals(URL_1, browser.getUrl());
        browser.openUrl(URL_2);
        assertEquals(URL_2, browser.getUrl());
        new HelpBrowser.BackAction(browser).actionPerformed(null);
        assertEquals(URL_1, browser.getUrl());
        new HelpBrowser.ForwardAction(browser).actionPerformed(null);
        assertEquals(URL_2, browser.getUrl());
    }

    /**
     * Unit test of {@link HelpBrowser.HomeAction} class.
     */
    @Test
    public void testHomeAction() {
        IHelpBrowser browser = newHelpBrowser();
        assertNull(browser.getUrl());
        new HelpBrowser.HomeAction(browser).actionPerformed(null);
        assertEquals(URL_1, browser.getUrl());
    }

    /**
     * Unit test of {@link HelpBrowser.EditAction} class.
     */
    @Test
    public void testEditAction() {
        IHelpBrowser browser = newHelpBrowser();
        assertNull(browser.getUrl());
        new HelpBrowser.EditAction(browser).actionPerformed(null);

        browser.openUrl(URL_2);
        assertEquals(URL_2, browser.getUrl());
        new HelpBrowser.EditAction(browser).actionPerformed(null);

        browser.openUrl(URL_3);
        assertEquals(URL_3, browser.getUrl());
        new HelpBrowser.EditAction(browser).actionPerformed(null);
    }

    /**
     * Unit test of {@link HelpBrowser.OpenInBrowserAction} class.
     */
    @Test
    public void testOpenInBrowserAction() {
        IHelpBrowser browser = newHelpBrowser();
        browser.openUrl(URL_1);
        assertEquals(URL_1, browser.getUrl());
        new HelpBrowser.OpenInBrowserAction(browser).actionPerformed(null);
    }

    /**
     * Unit test of {@link HelpBrowser.ReloadAction} class.
     */
    @Test
    public void testReloadAction() {
        IHelpBrowser browser = newHelpBrowser();
        browser.openUrl(URL_1);
        assertEquals(URL_1, browser.getUrl());
        new HelpBrowser.ReloadAction(browser).actionPerformed(null);
        assertEquals(URL_1, browser.getUrl());
    }
}
