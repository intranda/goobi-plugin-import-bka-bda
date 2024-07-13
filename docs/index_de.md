---
title: Altdatenimport für das Bundesdenkmalamt Österreich
identifier: intranda_import_bka_bda
description: Import-Plugin für den Import von Altdaten für das Bundesdenkmalamt in Österreich
published: true
---

## Einführung
Die vorliegende Dokumentation beschreibt die Installation, die Konfiguration und den Einsatz des Plugins für den Massenimport von vorliegenden Altdaten des Bundesdenkmalamts in Österreich. Die Ausgangsbasis für den Import sind vorliegende Excel-Dateien sowie bereitgestellte Verzeichnisse mit Bilddateien. Der besondere Aufbau der Excel-Datei machte eine deutliche Überarbeitung des Standard-Excel-Import-Plugins notwendig, so dass dieses deutlich davon abweicht.


## Installation
Um das Plugin nutzen zu können, müssen folgende Dateien installiert werden:

```bash
/opt/digiverso/goobi/plugins/import/plugin_intranda_import_bka_bda.jar
/opt/digiverso/goobi/config/plugin_intranda_import_bka_bda.xml
```


## Überblick und Funktionsweise
Um den Import zu nutzen, muss in den Produktionsvorlagen der Massenimportbereich geöffnet werden und im Reiter Dateiupload-Import das Plugin `intranda_import_bka_bda` ausgewählt werden. Anschließend kann eine Excel-Datei hochgeladen und importiert werden.

Der Import erfolgt anschließend zeilenweise. Dabei wird für jedes Objekt ein neuer Vorgang erzeugt und die konfigurierten Regeln angewendet. Wenn dabei ein valider Datensatz erzeugt wurde und der generierte Vorgangstitel noch nicht vergeben wurde, wird der Vorgang tatsächlich erzeugt und gespeichert. Innerhalb der Excel-Datei nachfolgende Zeilen, die zu dem zu erzeugenden Goobi-Vorgang gehören, werden abhängig von der Konfiguration mit dem gewünschten Strukturtyp erzeugt. Zugehörige Bilder werden hierbei ebenfalls automatisch übernommen und den erzeugten Strukturelementen und Vorgängen zugeordnet.


## Konfiguration
Die Konfiguration erfolgt über die Datei `plugin_intranda_import_bka_bda.xml`. Diese Datei kann im laufenden Betrieb angepasst werden.

{{CONFIG_CONTENT}}

### Individuelle Konfigurierbarkeit
Es ist sowohl möglich, eine globale Konfiguration für alle Produktionsvorlagen, als auch individuelle Einstellungen für einzelne Produktionsvorlagen zu erstellen. Dazu kann das Element `config` in der XML Datei wiederholt werden. Wenn in Goobi der Massenimport ausgewählt wurde, wird jeweils derjenige Konfigurationsblock gesucht, bei dem im Element `template` der Name der ausgewählten Produktionsvorlage steht. Existiert solch ein Eintrag nicht, wird die `default`-Konfiguration verwendet. Diese ist durch `*` gekennzeichnet.

```xml
<!-- which workflow template shall be used -->
<template>*</template>
```

### Publikationstyp
Mit dem folgenden Parameter läßt sich der zu verwendende Publikationstyp global festlegen: 

```xml
<!-- publication type to create -->
<publicationType>Monograph</publicationType>
```

Jeder Vorgang, der in Goobi mit diesem Plugin angelegt wird, erhält den hier definierten Pulikationstyp.

### Strukturtypen
Die Besonderheit dieses Plugins liegt darin, dass aus den sich teilweise wiederholenden Excel-Tabellenzeilen Strukturlemente generiert werden sollen, die zu dem zuvor erzeugten Publikationstyp als Unterelemente angelegt werden sollen. Der hierfür zu verwendende Typ wird mit diesem Parameter festgelegt:

```xml
<imageType>Photograph</imageType>
```

### Sammlung
Mit dem optionalen Element `collection` ist es möglich, eine Sammlung zu definieren, die in alle Datensätze eingefügt werden soll. Daneben können aber auch Sammlungen aus der Oberfläche ausgewählt werden, oder die Sammlung kann als Teil der Excel-Datei oder aus dem Katalog mit importiert werden.

```xml
<!-- which digital collection to use -->
<collection>Example collection</collection>
```

### Zeilenbereich
Die folgenden Elemente beschreiben den Aufbau der zu importierenden Excel-Datei.

In `rowHeader` wird definiert, in welcher Zeile die Spaltenüberschriften eingetragen wurden, die später für das Mapping relevant sind. Üblicherweise ist dies die erste Zeile. Dies kann bei mehrzeiligen Angaben jedoch auch davon abweichen.

```xml
<!-- define in which row the header is written, usually 1 -->
<rowHeader>1</rowHeader>
```

Die Elemente `rowDataStart` und `rowDataEnd` beschreiben den Bereich, der die Daten enthält. Üblicherweise sind dies die Zeilen, die direkt dem `rowHeader` folgen, bei besonderen Formatierungen können jedoch auch Leerzeilen enthalten sein, die hierüber entfernt werden können.

```xml
<!-- define in which row the data starts, usually 2 -->
<rowDataStart>2</rowDataStart>
<!-- define in which row the data ends, usually 20000 -->
<rowDataEnd>20000</rowDataEnd>
```

### Identifier
Der Eintrag `identifierHeaderName` enthält die Überschrift derjenigen Spalte, in der ein Identifier enthalten ist. Dieses Feld wird intern zur Identifikation der Zeilen genutzt. Bei einer OPAC Abfrage wird dieser Wert verwendet. Darüber hinaus wird dieser Wert ebenso für die Generierung des Vorgangstitels genutzt, wenn keine andere Generierung für Vorgangstitel angegeben wurde.

```xml
<!-- define which column is the one to use for catalogue requests and to identify the row during the import -->
<identifierHeaderName>Identifier</identifierHeaderName>
```

### Vorgangstitel
Das Element `processTitleRule` dient zur Generierung des Vorgangstitel. Hier stehen dieselben Möglichkeiten zur Verfügung, die auch in der Goobi-Konfigurationsdatei `goobi_projects.xml` genutzt werden können.

```xml
<!-- Rules to generate the process title, the same syntax as in goobi_projects.xml can be used.
     Use the column names to get the right metadata values.
     If the field is missing or empty, the value of the identifier column is used.
-->
<processTitleRule>'StaticPrefix_'+Identifier</processTitleRule>
```

### Übernahme von Bildern
Mit Hilfe der Elemente `imageFolderHeaderName`, `imageFolderPath` und `moveImages` können zusätzlich zu den Metadaten auch Bilder importiert werden. In `imageFolderHeaderName` wird hierfür der Spaltenname eingetragen, in dem in der Excel-Datei die Ordnernamen zu finden sind, die die Bilder enthalten. Dort kann entweder ein absoluter Pfad oder auch ein relativer Pfad angegeben werden. Wenn hierbei ein relativer Pfad angegeben wird, muss das Element `imageFolderPath` den `root` Pfad zu den Bildern enthalten.

Mittels des Elements`moveImages` kann gesteuert werden, ob die Bilder kopiert oder verschoben werden sollen.

```xml
<!-- define which column contains the image folder name. Can be combined with <imageFolderPath> prefix or an absolute path.
      If the field is missing, empty or does not contain an existing directory, no images will be imported -->
<imageFolderHeaderName>image folder</imageFolderHeaderName>

<!-- prefix path to the image folder. Can be empty or missing if the import doesn't contain images or if the excel field contains absolute path  -->
<imageFolderPath>/mnt/images/</imageFolderPath>

<!-- defines, if images are moved from the source folder to the destination (true) or copied (false) -->
<moveImages>true</moveImages>
```

### Ausführung mittels GoobiScript
Das Element `runAsGoobiScript` steuert, ob ein Import asynchron im Hintergrund über die GoobiScript Warteschlange abgearbeitet werden soll oder ob der Import direkt innerhalb der Nutzersession verarbeitet werden soll. Hier muss abgewägt werden, welche Einstellung sinnvoll ist. Soll ein ein Import inklusive Bildern erfolgen oder enthält die Excel-Datei sehr viele Datensätze, so ist es vermutlich sinnvoller, diesen Import als GoobiScript durchzuführen.

```markup
<!-- Run the import as GoobiScript -->
<runAsGoobiScript>true</runAsGoobiScript>
```

{% hint style="info" %}
**Achtung:** Wenn die Spalte `identifierHeaderName` keinen eindeutigen Identifier enthält oder nicht konfiguriert wurde, kann die Option `runAsGoobiScript` nicht genutzt werden.
{% endhint %}

### Konfiguration der einzelnen Excel-Spalten
Über die Felder `metadata`, `person` und `group` können einzelne Spalten als Metadatum oder als Vorgangseigenschaft importiert werden. Dazu enthält jedes Feld eine Reihe von Attributen und Unterelementen.

#### Import von Metadaten
Mit dem Element `metadata` werden deskriptive Metadaten erzeugt.

Name                 | Typ      | Beschreibung
---------------------|--------- |-------------------------------
`headerName`         | Attribut | Spaltentitel in der Exceldatei
`ugh`                | Attribut | Name des Metadatums
`property`           | Attribut | Name der Eigenschaft
`docType`            | Attribut | `anchor` oder `child`
`normdataHeaderName` | Attribut | Spaltentitel einer Spalte mit dazugehörigen Identifiern
`opacSearchField`    | Attribut | Definition, welches Suchfeld für die Katalogabfrage verwendet werden soll. Dies ist für den Einsatz des JSON-Opac-Plugins notwendig.

Das Attribut `headerName` enthält den Spaltentitel. Die Regel greift nur dann, wenn die Excel-Datei eine Spalte mit diesem Titel enthält und die Zelle nicht leer ist. Von den beiden Attributen `ugh` und `name` muss mindestens eines existieren. Das Feld `ugh` kann den Namen eines Metadatums enthalten. Wenn dies der Fall ist (und das Metadatum für den konfigurierten Publikationstyp erlaubt ist), wird ein neues Metadatum erzeugt. Mittels `name` wird eine Eigenschaft mit diesem Namen erstellt.

Das Attribut `docType` wird relevant, wenn aus dem Katalog ein mehrbändiges Werk oder eine Zeitschrift importiert wurde. Darüber kann gesteuert werden, ob das Feld zur Gesamtaufnahme oder zum Band gehören soll.

Falls zusätzlich zum Inhalt noch eine weitere Spalte mit Normdatenidentifiern oder URIs existiert, kann diese Spalte im Attribut `normdataHeaderName` hinzugefügt werden.