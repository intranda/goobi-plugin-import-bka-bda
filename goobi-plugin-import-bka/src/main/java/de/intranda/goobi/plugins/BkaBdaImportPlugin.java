package de.intranda.goobi.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.production.enums.ImportReturnValue;
import org.goobi.production.enums.ImportType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.importer.DocstructElement;
import org.goobi.production.importer.ImportObject;
import org.goobi.production.importer.Record;
import org.goobi.production.plugin.interfaces.IImportPluginVersion2;
import org.goobi.production.properties.ImportProperty;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.forms.MassImportForm;
import de.sub.goobi.helper.exceptions.ImportPluginException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.exceptions.UGHException;
import ugh.fileformats.mets.MetsMods;

@PluginImplementation
@Log4j2
public class BkaBdaImportPlugin implements IImportPluginVersion2 {

    @Getter
    private String title = "intranda_import_bka_bda";
    @Getter
    private PluginType type = PluginType.Import;

    @Getter
    private List<ImportType> importTypes;

    @Getter
    @Setter
    private Prefs prefs;
    @Getter
    @Setter
    private String importFolder;

    @Setter
    private MassImportForm form;

    @Setter
    private boolean testMode = false;

    @Getter
    @Setter
    private File file;

    @Setter
    private String workflowTitle;

    private int rowHeader;
    private int rowDataStart;
    private int rowDataEnd;
    private String publicationType;
    private String imageType;

    private String imageFolderPath;
    private String imageFolderHeaderName;

    private boolean runAsGoobiScript;

    private List<StringPair> mainMetadataList;
    private List<StringPair> imageMetadataList;

    private String processTitleColumn;
    private String collection;

    public BkaBdaImportPlugin() {
        importTypes = new ArrayList<>();
        importTypes.add(ImportType.FILE);
    }

    @Override
    public Fileformat convertData() throws ImportPluginException {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ImportObject> generateFiles(List<Record> records) {
        if (StringUtils.isBlank(workflowTitle)) {
            workflowTitle = form.getTemplate().getTitel();
        }
        readConfig();

        DocStructType physicalType = prefs.getDocStrctTypeByName("BoundBook");
        DocStructType imageDocstructType = prefs.getDocStrctTypeByName(imageType);
        DocStructType pageType = prefs.getDocStrctTypeByName("page");

        DocStructType logicalType = prefs.getDocStrctTypeByName(publicationType);
        MetadataType pathimagefilesType = prefs.getMetadataTypeByName("pathimagefiles");

        List<ImportObject> answer = new ArrayList<>();
        for (Record record : records) {
            ImportObject io = new ImportObject();

            String title = record.getId().replaceAll("\\W", "_");

            List<Map<?, ?>> data = (List<Map<?, ?>>) record.getObject();
            Map<String, Integer> headerMap = (Map<String, Integer>) data.get(0);
            List<Map<?, ?>> rows = data.subList(1, data.size());

            // create new mets file
            try {
                Fileformat fileformat = new MetsMods(prefs);
                DigitalDocument dd = new DigitalDocument();
                fileformat.setDigitalDocument(dd);

                DocStruct physical = dd.createDocStruct(physicalType);
                dd.setPhysicalDocStruct(physical);

                // imagepath
                Metadata newmd = new Metadata(pathimagefilesType);
                newmd.setValue("/images/");
                physical.addMetadata(newmd);

                DocStruct logical = dd.createDocStruct(logicalType);
                dd.setLogicalDocStruct(logical);
                // parse main metadata
                Map<Integer, String> firstRow = (Map<Integer, String>) rows.get(0);
                for (StringPair sp : mainMetadataList) {
                    String rulesetName = sp.getOne();
                    String columnName = sp.getTwo();
                    String metadataValue = firstRow.get(headerMap.get(columnName));
                    if (StringUtils.isNotBlank(metadataValue)) {
                        MetadataType type = prefs.getMetadataTypeByName(rulesetName);
                        Metadata md = new Metadata(type);
                        md.setValue(metadataValue);
                        logical.addMetadata(md);
                    }
                }

                int currentPhysicalOrder = 0;

                for (Map<?, ?> rawRow : rows) {
                    // create page element for each image
                    Map<Integer, String> row = (Map<Integer, String>) rawRow;

                    DocStruct page = dd.createDocStruct(pageType);
                    page.setImageName(row.get(headerMap.get(imageFolderHeaderName)));

                    MetadataType mdt = prefs.getMetadataTypeByName("physPageNumber");
                    Metadata mdTemp = new Metadata(mdt);
                    mdTemp.setValue(String.valueOf(++currentPhysicalOrder));
                    page.addMetadata(mdTemp);

                    // logical page no
                    mdt = prefs.getMetadataTypeByName("logicalPageNumber");
                    mdTemp = new Metadata(mdt);
                    mdTemp.setValue("uncounted");

                    page.addMetadata(mdTemp);
                    physical.addChild(page);
                    logical.addReferenceTo(page, "logical_physical");

                    // create logical element for each image
                    DocStruct ds = dd.createDocStruct(imageDocstructType);
                    logical.addChild(ds);
                    ds.addReferenceTo(page, "logical_physical");

                    // parse image metadata

                    for (StringPair sp : imageMetadataList) {
                        String rulesetName = sp.getOne();
                        String columnName = sp.getTwo();
                        String metadataValue = row.get(headerMap.get(columnName));
                        if (StringUtils.isNotBlank(metadataValue)) {
                            MetadataType type = prefs.getMetadataTypeByName(rulesetName);
                            Metadata md = new Metadata(type);
                            md.setValue(metadataValue);
                            ds.addMetadata(md);
                        }
                    }
                    // add collections if configured
                    MetadataType collectionType = prefs.getMetadataTypeByName("singleDigCollection");
                    if (StringUtils.isNotBlank(collection)) {
                        Metadata mdColl = new Metadata(collectionType);
                        mdColl.setValue(collection);
                        logical.addMetadata(mdColl);
                    }
                    // and add all collections that where selected
                    for (String colItem : form.getDigitalCollections()) {
                        if (!colItem.equals(collection.trim())) {
                            Metadata mdColl = new Metadata(collectionType);
                            mdColl.setValue(colItem);
                            logical.addMetadata(mdColl);
                        }
                    }

                }
                String fileName = getImportFolder() + File.separator + title + ".xml";
                io.setProcessTitle(title);
                io.setMetsFilename(fileName);
                fileformat.write(fileName);
                io.setImportReturnValue(ImportReturnValue.ExportFinished);
            } catch (UGHException e) {
                log.error(e);
                io.setImportReturnValue(ImportReturnValue.WriteError);
            }

            for (Map<?, ?> rawRow : rows) {
                Map<Integer, String> row = (Map<Integer, String>) rawRow;
                String imageName = row.get(headerMap.get(imageFolderHeaderName));
                // TODO copy images

            }
            io.setProcessTitle(title);
            answer.add(io);
        }

        return answer;
    }

    private void readConfig() {
        XMLConfiguration xmlConfig = ConfigPlugins.getPluginConfig(title);
        xmlConfig.setExpressionEngine(new XPathExpressionEngine());
        xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());

        SubnodeConfiguration myconfig = null;
        try {
            myconfig = xmlConfig.configurationAt("//config[./template = '" + workflowTitle + "']");
        } catch (IllegalArgumentException e) {
            myconfig = xmlConfig.configurationAt("//config[./template = '*']");
        }

        if (myconfig != null) {
            rowHeader = myconfig.getInt("/rowHeader", 1);
            rowDataStart = myconfig.getInt("/rowDataStart", 2);
            rowDataEnd = myconfig.getInt("/rowDataEnd", 20000);
            publicationType = myconfig.getString("/publicationType", "Monograph");
            imageType = myconfig.getString("/imageType", "Picture");

            imageFolderPath = myconfig.getString("/imageFolderPath", null);
            imageFolderHeaderName = myconfig.getString("/imageFolderHeaderName", null);

            runAsGoobiScript = myconfig.getBoolean("/runAsGoobiScript", true);

            collection = myconfig.getString("/collection", null);
            processTitleColumn = myconfig.getString("/processTitleColumn", null);

            mainMetadataList = new ArrayList<>();
            List<HierarchicalConfiguration> mml = myconfig.configurationsAt("/mainMetadata");
            for (HierarchicalConfiguration md : mml) {
                String rulesetName = md.getString("@rulesetName");
                String headerName = md.getString("@columnName", null);
                mainMetadataList.add(new StringPair(rulesetName, headerName));
            }

            imageMetadataList = new ArrayList<>();
            mml = myconfig.configurationsAt("/imageMetadata");
            for (HierarchicalConfiguration md : mml) {
                String rulesetName = md.getString("@rulesetName");
                String headerName = md.getString("@columnName", null);
                imageMetadataList.add(new StringPair(rulesetName, headerName));
            }

        }
    }

    @Override
    public List<Record> splitRecords(String string) {
        List<Record> answer = new ArrayList<>();

        return answer;
    }

    @Override
    public List<String> splitIds(String ids) {
        return null;
    }

    @Override
    public String addDocstruct() {
        return null;
    }

    @Override
    public String deleteDocstruct() {
        return null;
    }

    @Override
    public void deleteFiles(List<String> arg0) {
    }

    @Override
    public List<Record> generateRecordsFromFile() {
        if (StringUtils.isBlank(workflowTitle)) {
            workflowTitle = form.getTemplate().getTitel();
        }
        readConfig();

        List<Record> recordList = new ArrayList<>();
        Map<String, Integer> headerOrder = new HashMap<>();

        InputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            BOMInputStream in = new BOMInputStream(fileInputStream, false);
            Workbook wb = WorkbookFactory.create(in);
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.rowIterator();

            // get header and data row number from config first
            int rowCounter = 0;

            //  find the header row
            Row headerRow = null;
            while (rowCounter < rowHeader) {
                headerRow = rowIterator.next();
                rowCounter++;
            }

            //  read and validate the header row
            int numberOfCells = headerRow.getLastCellNum();
            for (int i = 0; i < numberOfCells; i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    cell.setCellType(CellType.STRING);
                    String value = cell.getStringCellValue();
                    headerOrder.put(value, i);
                }
            }

            // find out the first data row
            while (rowCounter < rowDataStart - 1) {
                headerRow = rowIterator.next();
                rowCounter++;
            }

            Map<String, List<Map<?, ?>>> processMap = new HashMap<>();

            // run through all the data rows
            while (rowIterator.hasNext() && rowCounter < rowDataEnd) {
                Map<Integer, String> map = new HashMap<>();
                Row row = rowIterator.next();
                rowCounter++;
                int lastColumn = row.getLastCellNum();
                if (lastColumn == -1) {
                    continue;
                }
                for (int cn = 0; cn < lastColumn; cn++) {
                    //                while (cellIterator.hasNext()) {
                    //                    Cell cell = cellIterator.next();
                    Cell cell = row.getCell(cn, MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String value = "";
                    switch (cell.getCellTypeEnum()) {
                        case BOOLEAN:
                            value = cell.getBooleanCellValue() ? "true" : "false";
                            break;
                        case FORMULA:
                            //                            value = cell.getCellFormula();
                            value = cell.getRichStringCellValue().getString();
                            break;
                        case NUMERIC:
                            value = String.valueOf((int) cell.getNumericCellValue());
                            break;
                        case STRING:
                            value = cell.getStringCellValue();
                            break;
                        default:
                            // none, error, blank
                            value = "";
                            break;
                    }
                    map.put(cn, value);
                }
                // id = Objekttitel + FotografIn + Aufnahmejahr
                String title = map.get(headerOrder.get(processTitleColumn));

                if (processMap.containsKey(title)) {
                    List<Map<?, ?>> rows = processMap.get(title);
                    rows.add(map);
                } else {
                    List<Map<?, ?>> rows = new ArrayList<>();
                    rows.add(headerOrder);
                    rows.add(map);
                    processMap.put(title, rows);
                }

            }
            for (String title : processMap.keySet()) {
                Record r = new Record();
                r.setId(title);
                r.setObject(processMap.get(title));
                recordList.add(r);

            }

        } catch (Exception e) {
            log.error(e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }

        return recordList;
    }

    @Override
    public List<Record> generateRecordsFromFilenames(List<String> arg0) {
        return null;
    }

    @Override
    public List<String> getAllFilenames() {
        return null;
    }

    @Override
    public List<? extends DocstructElement> getCurrentDocStructs() {
        return null;
    }

    @Override
    public DocstructElement getDocstruct() {
        return null;
    }

    @Override
    public List<String> getPossibleDocstructs() {
        return null;
    }

    @Override
    public String getProcessTitle() {
        return null;
    }

    @Override
    public List<ImportProperty> getProperties() {
        return null;
    }

    @Override
    public void setData(Record arg0) {
    }

    @Override
    public void setDocstruct(DocstructElement arg0) {
    }

    @Override
    public boolean isRunnableAsGoobiScript() {
        return runAsGoobiScript;
    }

}
