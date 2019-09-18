## Manu-Med-Importer

A CLI tool that transforms raw data from Manuscripta Mediaevalia into internal METS/MODS format (based on Kitodo)

## Build
`$ ./gradlew importer`

## Usage
```bash
/usr/bin/java -Dlog4j.configurationFile=log4j2ConfigFile -jar Import.jar -m xmlOutputFile -i imageFolder -h hidaFile -c propertiesFile -r imagePathMapping
```

| Argument | Description | Example     |
| -------- | ----------- | ----------- |
| -m | A internal METS/MODS xml file path | /meta.xml |
| -i | An image folder path | /master_MS_1002_media |
| -h | A HIDA xml file path | /ms_1002.xml |
| -c, --config | a propteries configuration File | config/mapping.properties |
| -r | A mapping between original file names and internal Kitodo file names | /mapping.txt |

## Java
* This requires Java 8 or higher