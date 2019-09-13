package org.ub.manumed;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.*;

import static org.ub.manumed.Importer.*;

class Mets {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

	private String strMap = "structMap";

	Mets(File rename, String imagePath, String meta) {
		File rootPath = new File(imagePath);
		HashMap<String, String> nameMap = doMapping(rename);
		int id = 0;
		ArrayList<File> fileList = getImageList(nameMap, rootPath, id);
		Element fileGrp = setFileGrp();
		Element dmdPhys = setDmdPhys();
		Element structMap = setStructMap(dmdPhys);
		Element structLink = setStructLink();
		Element fileSec = setFileSec(fileList, nameMap, id, fileGrp, dmdPhys, structLink);
		print(meta, structMap, fileSec, structLink);
	}

	Mets(String imagePath, String meta) {
		File rootPath = new File(imagePath);
		ArrayList<File> fileList = sortList(rootPath);
		Element fileGrp = setFileGrp();
		Element dmdPhys = setDmdPhys();
		Element structMap = setStructMap(dmdPhys);
		Element structLink = setStructLink();
		Element fileSec = setFileSec(fileList, fileGrp, dmdPhys, structLink);
		print(meta, structMap, fileSec, structLink);
	}

	private HashMap<String, String> doMapping(File renameFile) {
		HashMap<String, String> map = new HashMap<>();
        try {
            byte[] fileContent = Files.readAllBytes(renameFile.toPath());
            String data = new String(fileContent);
            for(String line:data.split("\n")) {
                line = line.substring(7).trim();
                String tifOld = line.split(" to ")[0];
                String tifNew = line.split(" to ")[1];

                tifOld = tifOld.substring(tifOld.lastIndexOf(File.separator) + 1);
                tifNew = tifNew.substring(tifNew.lastIndexOf(File.separator) + 1);

                map.put(tifNew, tifOld);
            }
        } catch (IOException e) {
            logger.error("Could not map images.", e.fillInStackTrace());
        }
        /*
		try (FileInputStream fos = new FileInputStream(renameFile)) {
			byte[] buffer = new byte[fos.available()];
			fos.read(buffer);

			String data = new String(buffer);
			for(String line:data.split("\n")) {
				line = line.substring(7).trim();
				String tifOld = line.split(" to ")[0];
				String tifNew = line.split(" to ")[1];

				tifOld = tifOld.substring(tifOld.lastIndexOf(File.separator) + 1);
				tifNew = tifNew.substring(tifNew.lastIndexOf(File.separator) + 1);

				map.put(tifNew, tifOld);
			}
		} catch (IOException e) {
			logger.error("Could not map images.", e.fillInStackTrace());
		}

         */
		return map;
	}

	private Element setDmdPhys() {
		return new Element("div", mets).setAttribute("DMDID","DMDPHYS_0000").setAttribute("ID","PHYS_0000").setAttribute("TYPE","BoundBook");
	}

	private Element setStructMap(Element dmdPhys) {
		Element structMap = new Element(strMap, mets);
		structMap.setAttribute("TYPE","PHYSICAL");
		structMap.addContent(dmdPhys);

		return structMap;
	}

	private Element setFileGrp() {
		Element fileGrp = new Element("fileGrp", mets);
		fileGrp.setAttribute("USE","LOCAL");

		return fileGrp;
	}

	private Element setStructLink() {
		return new Element("structLink", mets);
	}

	private void setFileID(int x, File f, Element fileGrp, String oldInfo, Element dmdPhys, Element structLink) {
		String fileID = getIndex(x);
		Element file = new Element("file", mets).setAttribute("ID","FILE_" + fileID).setAttribute("MIMETYPE","image/tiff");
		Element flocat = new Element("FLocat", mets).setAttribute("LOCTYPE","URL").setAttribute("href", makeURI(f.getAbsolutePath()), xlink).setNamespace(mets);
		file.addContent(flocat);
		fileGrp.addContent(file);

		Element div = new Element("div", mets).setAttribute("ID","PHYS_" + fileID).setAttribute("ORDER", String.valueOf(x)).setAttribute("ORDERLABEL", oldInfo).setAttribute("TYPE","page");
		Element fptr = new Element("fptr", mets).setAttribute("FILEID","FILE_" + fileID);
		div.addContent(fptr);
		dmdPhys.addContent(div);

		Element smLink = new Element("smLink", mets).setAttribute("to","PHYS_" + fileID, xlink).setAttribute("from","LOG_0000",xlink).setNamespace(mets);
		structLink.addContent(smLink);
	}

	private Element setFileSec(ArrayList<File> fileList, HashMap<String, String> nameMap, int id, Element fileGrp, Element dmdPhys, Element structLink) {
		Element fileSec = new Element("fileSec", mets);
		int x = 0;

		for(File f:fileList) {
			if(f.isFile() && f.getName().toLowerCase().endsWith(".tif")) {
				x++;

				String qualifiedName = nameMap.get(f.getName());
				String oldInfo = qualifiedName.substring(id, qualifiedName.length() - 4);

				int j = 0;
				String z = "0";
				String s = "_";
				while (oldInfo.charAt(j) == z.charAt(0)) {
					j++;
				}
				if (oldInfo.charAt(j) == s.charAt(0)) {
					j++;
				}
				oldInfo = oldInfo.substring(j);

				setFileID(x, f, fileGrp, oldInfo, dmdPhys, structLink);
			}
		}
		fileSec.addContent(fileGrp);

		return fileSec;
	}

	private Element setFileSec(ArrayList<File> fileList, Element fileGrp, Element dmdPhys, Element structLink) {
		Element fileSec = new Element("fileSec", mets);
		int x = 0;

		for(File f:fileList) {
			if(f.isFile() && f.getName().toLowerCase().endsWith(".tif")) {
				x++;

				setFileID(x, f, fileGrp, getIndex(x), dmdPhys, structLink);
			}
		}
		fileSec.addContent(fileGrp);

		return fileSec;
	}

	private String makeURI(String absolutePath) {
		String result = absolutePath;
		if(absolutePath.startsWith("/")) {
			result = "file://" + result;
		}
		else {
			result = "file://" + result.substring(2);

			if(System.getProperty("os.name").toLowerCase().contains("windows"))
				result = result.replaceAll("\\\\","/");
		}
		return result;
	}

	private String getIndex(int i) {
		return String.format("%04d", i);
	}

	private ArrayList<File> sortList(File rootPath) {
	    ArrayList<File> fileList = new ArrayList<>();
	    File[] files = rootPath.listFiles();
        for(File f:Objects.requireNonNull(files)) {
            if(f.isFile() && f.getName().toLowerCase().endsWith(".tif")) {
                fileList.add(f);
            }
        }

        Collections.sort(fileList);

        return fileList;
    }

	private ArrayList<File> getImageList(HashMap<String, String> nameMap, File rootPath, int id) {
		ArrayList<File> fileList = sortList(rootPath);

		HashMap<String, Integer> counterTable = new HashMap<>();
		for(String oldFileName : nameMap.values()) {
			String[] filenameParts = oldFileName.split("_");
			for (String filenamePart : filenameParts) {
				if (counterTable.containsKey(filenamePart)) {
					counterTable.put(filenamePart, counterTable.get(filenamePart) + 1);
				} else {
					counterTable.put(filenamePart, 1);
				}
			}
		}

		for(Map.Entry<String, Integer> entry : counterTable.entrySet()) {
		    if(entry.getValue() == nameMap.values().size()) {
		        id += (entry.getKey() + "_").length();
            }
        }
		/*
		for(String part : counterTable.keySet())
		{
			if(counterTable.get(part) == nameMap.values().size())
				id += (part + "_").length();
		}

		 */
		return fileList;
	}

	private void print(String meta, Element structMap, Element fileSec, Element structLink) {
		try {
			int p = -1;
			for(Element sMap : docMeta.getRootElement().getChildren(strMap, mets)) {
			    if(sMap.getAttributeValue("TYPE").equals("LOGICAL")) {
					p = docMeta.getRootElement().indexOf(sMap);
				}
			}

			Element dmdPhys = new Mods().getElement("dmdphys");
			docMeta.getRootElement().addContent(docMeta.getRootElement().indexOf(dmdPhys) + 1 , fileSec);
            //docMeta.getRootElement().addContent(p - 1, fileSec);

			for(Element sMap : docMeta.getRootElement().getChildren(strMap, mets)) {
				if(sMap.getAttributeValue("TYPE").equals("PHYSICAL")) {
					p = docMeta.getRootElement().indexOf(sMap);
                    docMeta.getRootElement().removeContent(sMap);
				}
			}

            docMeta.getRootElement().addContent(p, structMap);

			Element sLink = docMeta.getRootElement().getChild("structLink", mets);
            docMeta.getRootElement().removeContent(sLink);
            docMeta.getRootElement().addContent(structLink);

			outXML.output(docMeta, new FileOutputStream(meta));
			logger.debug("Finished.");
		} catch (IOException e) {
			logger.error("Could not print METS part.", e.fillInStackTrace());
		}
	}

}