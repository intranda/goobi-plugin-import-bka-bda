package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.goobi.production.enums.ImportType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ugh.dl.Prefs;

public class BkaBdaImportPluginTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File tempFolder;

    @Before
    public void setUp() throws Exception {
        tempFolder = folder.newFolder("tmp");

    }

    @Test
    public void testConstructor() {
        BkaBdaImportPlugin plugin = new BkaBdaImportPlugin();
        assertNotNull(plugin);
        assertEquals(ImportType.FILE, plugin.getImportTypes().get(0));

        plugin.setImportFolder(tempFolder.getAbsolutePath());
    }

    @Test
    public void testCreateMetsFiles() throws Exception {
        BkaBdaImportPlugin plugin = new BkaBdaImportPlugin();
        plugin.setImportFolder(tempFolder.getAbsolutePath());
        plugin.setTestMode(true); // ruleset
        Prefs prefs = new Prefs();
        prefs.loadPrefs("src/test/resources/ruleset.xml");
        plugin.setPrefs(prefs);

    }

}
