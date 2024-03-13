package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.easymock.EasyMock;
import org.goobi.production.enums.ImportType;
import org.goobi.production.importer.ImportObject;
import org.goobi.production.importer.Record;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import ugh.dl.Prefs;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigPlugins.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "jdk.internal.reflect.*" })
public class BkaBdaImportPluginTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File tempFolder;
    private static String resourcesFolder;


    @BeforeClass
    public static void setUpClass() throws Exception {
        resourcesFolder = "src/test/resources/"; // for junit tests in eclipse

        if (!Files.exists(Paths.get(resourcesFolder))) {
            resourcesFolder = "target/test-classes/"; // to run mvn test from cli or in jenkins
        }
        String log4jFile = resourcesFolder + "log4j2.xml"; // for junit tests in eclipse
        System.setProperty("log4j.configurationFile", log4jFile);

        Path goobiFolder = Paths.get(resourcesFolder + "config/goobi_config.properties");
        ConfigurationHelper.CONFIG_FILE_NAME = goobiFolder.toString();
        ConfigurationHelper.resetConfigurationFile();
        ConfigurationHelper.getInstance().setParameter("goobiFolder", goobiFolder.getParent().getParent().toString() + "/");
    }

    @Before
    public void setUp() throws Exception {
        tempFolder = folder.newFolder("tmp");



        PowerMock.mockStatic(ConfigPlugins.class);
        EasyMock.expect(ConfigPlugins.getPluginConfig(EasyMock.anyString())).andReturn(getConfig()).anyTimes();
        PowerMock.replay(ConfigPlugins.class);
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
        plugin.setWorkflowTitle("workflow");
        File excelFile = new File(resourcesFolder + "metadaten.xlsx");
        assertTrue(excelFile.isFile());
        plugin.setFile(excelFile);
        List<Record> records = plugin.generateRecordsFromFile();
        Record record = records.get(0);
        String title = record.getId();

        List<Map<?, ?>> data = (List<Map<?, ?>>) record.getObject();
        Map<String, Integer> headerMap = (Map<String, Integer>) data.get(0);
        List<Map<?, ?>> rows = data.subList(1, data.size());

        assertEquals("1376717487B__XXXXXX2014", title);
        assertEquals(21, headerMap.size());
        assertEquals(11, rows.size());
        assertEquals("Neue Burg, Nationalbibliothek (heutiger Lesesaal)", rows.get(0).get(headerMap.get("Objekttitel")));
    }



    @Test
    public void testImportFiles()  throws Exception {
        BkaBdaImportPlugin plugin = new BkaBdaImportPlugin();
        plugin.setImportFolder(tempFolder.getAbsolutePath());
        plugin.setTestMode(true); // ruleset
        Prefs prefs = new Prefs();
        prefs.loadPrefs(resourcesFolder + "ruleset.xml");
        plugin.setPrefs(prefs);
        plugin.setWorkflowTitle("workflow");
        File excelFile = new File(resourcesFolder + "metadaten.xlsx");
        plugin.setFile(excelFile);
        List<Record> records = plugin.generateRecordsFromFile();

        List<ImportObject> convertedFiles = plugin.generateFiles(records);

        ImportObject io =  convertedFiles.get(0);

        assertEquals("1376717487B__XXXXXX2014", io.getProcessTitle());

        // TODO open mets file, check metadata


    }

    private XMLConfiguration getConfig() {
        String file = "plugin_intranda_import_bka_bda.xml";
        XMLConfiguration config = new XMLConfiguration();
        config.setDelimiterParsingDisabled(true);
        try {
            config.load(resourcesFolder + file);
        } catch (ConfigurationException e) {
        }
        config.setReloadingStrategy(new FileChangedReloadingStrategy());
        return config;
    }

}
