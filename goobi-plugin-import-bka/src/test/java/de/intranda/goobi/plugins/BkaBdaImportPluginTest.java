package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.goobi.production.enums.ImportType;
import org.goobi.production.importer.Record;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ugh.dl.Prefs;

public class BkaBdaImportPluginTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File tempFolder;
    private String resourcesFolder;

    @Before
    public void setUp() throws Exception {
        tempFolder = folder.newFolder("tmp");

        resourcesFolder = "src/test/resources/"; // for junit tests in eclipse

        if (!Files.exists(Paths.get(resourcesFolder))) {
            resourcesFolder = "target/test-classes/"; // to run mvn test from cli or in jenkins
        }

    }

    @Test
    public void testConstructor() {
        BkaBdaImportPlugin plugin = new BkaBdaImportPlugin();
        assertNotNull(plugin);
        assertEquals(ImportType.FILE, plugin.getImportTypes().get(0));

        plugin.setImportFolder(tempFolder.getAbsolutePath());
    }

    @Test
    public void testUploadExcelFile() throws Exception {
        BkaBdaImportPlugin plugin = new BkaBdaImportPlugin();
        plugin.setImportFolder(tempFolder.getAbsolutePath());
        plugin.setTestMode(true); // ruleset
        Prefs prefs = new Prefs();
        prefs.loadPrefs(resourcesFolder + "ruleset.xml");
        plugin.setPrefs(prefs);

        File excelFile = new File(resourcesFolder + "metadaten.xlsx");
        assertTrue(excelFile.isFile());
        plugin.setFile(excelFile);
        List<Record> records = plugin.generateRecordsFromFile();
        Record record = records.get(0);
        String title = record.getId();

        List<Map<?, ?>> data = (List<Map<?, ?>>) record.getObject();
        Map<String, Integer> headerMap = (Map<String, Integer>) data.get(0);
        List<Map<?, ?>> rows = data.subList(1, data.size());

        assertEquals("Topographische Aufnahmen_digital_Michael AAAAAA_2015", title);
        assertEquals(21, headerMap.size());
        assertEquals(2, rows.size());
        assertEquals("Topographische Aufnahmen_digital", rows.get(0).get(headerMap.get("Objekttitel")));
    }

}
