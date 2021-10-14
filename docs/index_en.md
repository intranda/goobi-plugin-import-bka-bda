---
description: >-
  This is an import plugin for the Goobi workflow that was needed to import legacy data for the Federal Monuments Office in Vienna.
  
---

# Import of data records of the BDA Austria

## Introduction

This documentation describes the installation, configuration and use of the plugin for the mass import of existing old data of the Federal Monuments Office in Austria. The starting point for the import are existing Excel files as well as provided directories with image files. The special structure of the Excel file made a significant revision of the standard Excel import plugin necessary, so that this plugin differs significantly from it.

| Details |  |
| :--- | :--- |
| Identifier | intranda_import_bka_bda |
| Source code | [https://github.com/intranda/goobi-plugin-import-bka-bda](https://github.com/intranda/goobi-plugin-import-bka-bda) |
| Licence | GPL 2.0 or newer |
| Compatibility | Goobi workflow 2021.09 |
| Documentation date | 30.09.2021 |

## Installation

The plugin must be installed in the following folder:

```text
/opt/digiverso/goobi/plugins/import/plugin_intranda_import_bka_bda.jar
```

In addition, there is a configuration file that must be located in the following place:

```text
/opt/digiverso/goobi/config/plugin_intranda_import_bka_bda.xml
```

## Configuration

The configuration is done via the file `plugin_intranda_import_bka_bda.xml`. This file can be adapted during operation.

```xml
<config_plugin>
	<config>
		<!-- which workflow template shall be used -->
		<template>*</template>
		<!-- publication type to create -->
		<publicationType>Document</publicationType>
        <imageType>Photograph</imageType>
		<!-- which digital collection to use
		<collection>General</collection>
		 -->
		<processTitleColumn>Vorgang</processTitleColumn>
		<!-- define in which row the header is written, usually 1 -->
		<rowHeader>1</rowHeader>
		<!-- define in which row the data starts, usually 2 -->
		<rowDataStart>1</rowDataStart>
		<!-- define in which row the data ends, usually 20000 -->
		<rowDataEnd>10000</rowDataEnd>
		<!-- define if import shall use GoobiScript to run in the background -->
		<runAsGoobiScript>true</runAsGoobiScript>
        
        <!-- column for the path to the images -->
        <imageFolderHeaderName>Importpfad Bildobjekt</imageFolderHeaderName>

		<!-- individual metadata fields read from the excel columns -->
		<mainMetadata rulesetName="CatalogIDDigital" columnName="DMDB-ID" />
		<mainMetadata rulesetName="HerisID" columnName="HERIS-ID" />
		<mainMetadata rulesetName="FederalState" columnName="Bundesland" />
		<mainMetadata rulesetName="Location" columnName="Ort" />
		<mainMetadata rulesetName="Address" columnName="Adresse" />
		<mainMetadata rulesetName="TitleDocMain" columnName="Objekttitel" />
		<mainMetadata rulesetName="Photographer" columnName="FotografIn" />
		<mainMetadata rulesetName="DateRecorded" columnName="Aufnahmejahr" />
		<mainMetadata rulesetName="DateRecordedInformation" columnName="Aufnahmedatum Zusatzinfo" />
		<mainMetadata rulesetName="KeyNumber" columnName="GZ" />
		<mainMetadata rulesetName="Condition" columnName="Erhaltungszustand des Objekts" />
		<mainMetadata rulesetName="Remarks" columnName="Sonstiges/Anmerkungen" />
		<mainMetadata rulesetName="ObjectType" columnName="Objekttyp" />
		<mainMetadata rulesetName="Series" columnName="Serie" />
		<mainMetadata rulesetName="SubSeries" columnName="Unterserie" />

		<imageMetadata rulesetName="View" columnName="Ansicht" />
		<imageMetadata rulesetName="shelfmarksource" columnName="Signatur" />
		<imageMetadata rulesetName="FormerFilePath" columnName="Ursprünglicher Dateipfad" />
		<imageMetadata rulesetName="Copyright" columnName="Rechteinhaber" />
		<imageMetadata rulesetName="ImportPath" columnName="Importpfad Bildobjekt" />

	</config>
</config_plugin>
```

### Individual configurability

It is possible to create a global configuration for all production templates as well as individual settings for single production templates. To do this, the `config` element can be repeated in the XML file. If mass import is selected in Goobi, the system searches for the configuration block that contains the name of the selected production template in the `template` element. If such an entry does not exist, the `default` configuration is used. This is indicated by `*`.

```xml
<!-- which workflow template shall be used -->
<template>*</template>
```


### Publication type
The following parameter can be used to globally define the publication type to be used: 

```xml
<!-- publication type to create -->
<publicationType>Monograph</publicationType>
```

Every process that is created in Goobi with this plugin receives the application type defined here.

### Structure types
The special feature of this plugin is that structural elements are to be generated from the partially repeating Excel table rows, which are to be created as sub-elements for the previously created publication type. The type to be used for this is specified with this parameter:

```xml
<imageType>Photograph</imageType>
```

### Collection

With the optional element `collection` it is possible to define a collection to be inserted in all records. In addition, collections can also be selected from the interface, or the collection can be imported as part of the Excel file or from the catalogue.

```xml
<!-- which digital collection to use -->
<collection>Example collection</collection>
```

### Row range

The following elements describe the structure of the Excel file to be imported.

In `rowHeader` it is defined in which row the column headers are entered that are later relevant for the mapping. Usually this is the first line. However, this can also deviate in the case of multi-line entries.

```xml
<!-- define in which row the header is written, usually 1 -->
<rowHeader>1</rowHeader>
```

The elements `rowDataStart` and `rowDataEnd` describe the area that contains the data. Usually these are the lines that directly follow the `rowHeader`, but in the case of special formatting there may also be empty lines that can be removed via this.

```xml
<!-- define in which row the data starts, usually 2 -->
<rowDataStart>2</rowDataStart>
<!-- define in which row the data ends, usually 20000 -->
<rowDataEnd>20000</rowDataEnd>
```

### Identifier

The entry `identifierHeaderName` contains the heading of the column that contains an identifier. This field is used internally to identify the rows. In an OPAC query, this value is used. In addition, this value is also used for generating the case title if no other generation for case titles has been specified.

```xml
<!-- define which column is the one to use for catalogue requests and to identify the row during the import -->
<identifierHeaderName>Identifier</identifierHeaderName>
```

### Process title

The `processTitleRule` element is used to generate the process title. The same options are available here that can be used in the Goobi configuration file `goobi_projects.xml`.

```xml
<!-- Rules to generate the process title, the same syntax as in goobi_projects.xml can be used.
     Use the column names to get the right metadata values.
     If the field is missing or empty, the value of the identifier column is used.
-->
<processTitleRule>'StaticPrefix_'+Identifier</processTitleRule>
```

### Importing images

With the help of the elements `imageFolderHeaderName`, `imageFolderPath` and `moveImages`, images can be imported in addition to the metadata. In `imageFolderHeaderName` the column name is entered for this purpose, in which the folder names containing the images can be found in the Excel file. Either an absolute path or a relative path can be entered. If a relative path is specified, the element `imageFolderPath` must contain the `root` path to the images.

The element `moveImages` can be used to control whether the images are to be copied or moved.

```xml
<!-- define which column contains the image folder name. Can be combined with <imageFolderPath> prefix or an absolute path.
      If the field is missing, empty or does not contain an existing directory, no images will be imported -->
<imageFolderHeaderName>image folder</imageFolderHeaderName>

<!-- prefix path to the image folder. Can be empty or missing if the import doesn't contain images or if the excel field contains absolute path  -->
<imageFolderPath>/mnt/images/</imageFolderPath>

<!-- defines, if images are moved from the source folder to the destination (true) or copied (false) -->
<moveImages>true</moveImages>
```

### Execution via GoobiScript

The element `runAsGoobiScript` controls whether an import should be processed asynchronously in the background via the GoobiScript queue or whether the import should be processed directly within the user session. Here you have to decide which setting makes sense. If an import is to include images or if the Excel file contains a large number of data records, it is probably more sensible to perform this import as a GoobiScript.

```xml
<!-- Run the import as GoobiScript -->
<runAsGoobiScript>true</runAsGoobiScript>
```

{% hint style="info" %}
**Attention:** If the column `identifierHeaderName` does not contain a unique identifier or has not been configured, the option `runAsGoobiScript` cannot be used.
{% endhint %}

### Configuration of the individual Excel columns

The fields `metadata`, `person` and `group` can be used to import individual columns as metadata or as transaction properties. For this purpose, each field contains a number of attributes and sub-elements.

#### Importing metadata

The element `metadata` is used to create descriptive metadata.

| Name | Type | Description |
| :--- | :--- | :--- |
| `headerName` | Attribut | Column title in the Excel file |
| `ugh` | Attribut | Metadate name |
| `property` | Attribut | Property name |
| `docType` | Attribut | `anchor` or `child` |
| `normdataHeaderName` | Attribut | Column title of a column with associated identifiers |
| `opacSearchField` | Attribut | Definition of which search field is to be used for the catalogue query. This is necessary for the use of the JSON Opac plugin. |

The attribute `headerName` contains the column title. The rule only applies if the Excel file contains a column with this title and the cell is not empty. At least one of the two attributes `ugh` and `name` must exist. The field `ugh` can contain the name of a metadatum. If this is the case \(and the metadatum is allowed for the configured publication type\), a new metadatum is created. A property with this name is created using `name`.

The attribute `docType` becomes relevant if a multi-volume work or a journal has been imported from the catalogue. It can be used to control whether the field should belong to the complete record or to the volume.

If, in addition to the content, another column with standard data identifiers or URIs exists, this column can be added in the attribute `normdataHeaderName`.

## Use in Goobi

To use the import, the mass import area must be opened in the production templates and the plugin `intranda_import_bka_bda` must be selected in the File Upload Import tab. An Excel file can then be uploaded and imported.

The import is then carried out line by line. A new process is created for each object and the configured rules are applied. If a valid data record has been created and the generated process title has not yet been assigned, the process is actually created and saved. Within the Excel file, subsequent lines belonging to the Goobi process to be generated are created with the desired structure type depending on the configuration. Associated images are also automatically transferred and assigned to the generated structural elements and processes.