/*
 * Copyright (c) 2019. Leipzig University Library <it@ub.uni-leipzig.de>
 *
 * Licensed under the EUPL, Version 1.2 – as soon they will be approved by the European Commission subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-11-12
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under the Licence.
 */

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
	private int fnCount = 0;

	Mets(File rename, String imagePath, String meta) {
		File rootPath = new File(imagePath);
		HashMap<String, String> nameMap = doMapping(rename);
		ArrayList<File> fileList = getImageList(nameMap, rootPath);
		Element fileGrp = setFileGrp();
		Element dmdPhys = setDmdPhys();
		Element structMap = setStructMap(dmdPhys);
		Element structLink = setStructLink();
		Element fileSec = setFileSec(fileList, nameMap, fileGrp, dmdPhys, structLink);
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

	/**
	 * mapping between old file names and 'Kitodo' confirmed names
	 *
	 * @param renameFile File
	 * @return HashMap<String, String>
	 */
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

		return map;
	}

	/**
	 * creates Element dmdPhys
	 *
	 * @return Element
	 */
	private Element setDmdPhys() {
		return new Element("div", mets).setAttribute("DMDID","DMDPHYS_0000").setAttribute("ID","PHYS_0000").setAttribute("TYPE","BoundBook");
	}

	/**
	 * creates Element structMap
	 *
	 * @param dmdPhys Element
	 * @return Element
	 */
	private Element setStructMap(Element dmdPhys) {
		Element structMap = new Element(strMap, mets);
		structMap.setAttribute("TYPE","PHYSICAL");
		structMap.addContent(dmdPhys);

		return structMap;
	}

	/**
	 * creates Element fileGrp
	 *
	 * @return Element
	 */
	private Element setFileGrp() {
		Element fileGrp = new Element("fileGrp", mets);
		fileGrp.setAttribute("USE","LOCAL");

		return fileGrp;
	}

	/**
	 * creates Element structLink
	 *
	 * @return Element
	 */
	private Element setStructLink() {
		return new Element("structLink", mets);
	}

	/**
	 * fill content of Element structLink
	 *
	 * @param x int
	 * @param f File
	 * @param fileGrp Element
	 * @param oldInfo String
	 * @param dmdPhys Element
	 * @param structLink Element
	 */
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

	/**
	 * fill content of Element fileSec with file name mapping
	 *
	 * @param fileList ArrayList<File>
	 * @param nameMap HashMap<String,String>
	 * @param fileGrp Element
	 * @param dmdPhys Element
	 * @param structLink Element
	 * @return Element
	 */
	private Element setFileSec(ArrayList<File> fileList, HashMap<String, String> nameMap, Element fileGrp, Element dmdPhys, Element structLink) {
		Element fileSec = new Element("fileSec", mets);
		int x = 0;

		for(File f:fileList) {
			if(f.isFile() && f.getName().toLowerCase().endsWith(".tif")) {
				x++;

				String oldInfo = "";
				for(Map.Entry<String, String> entry:nameMap.entrySet()) {
					if(f.getName().equals(entry.getKey().substring(entry.getKey().lastIndexOf('/') + 1))) {
						String qualifiedName = entry.getValue();
						oldInfo = qualifiedName.substring(fnCount, qualifiedName.length() - 4);
						break;
					}
				}

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
				if(oldInfo.contains("_")) {
					oldInfo = oldInfo.substring(oldInfo.indexOf('_') + 1);
				}

				setFileID(x, f, fileGrp, oldInfo, dmdPhys, structLink);
			}
		}
		fileSec.addContent(fileGrp);

		return fileSec;
	}

	/**
	 * fill content of Element fileSec without file name mapping
	 *
	 * @param fileList ArrayList<File>
	 * @param fileGrp Element
	 * @param dmdPhys Element
	 * @param structLink Element
	 * @return Element
	 */
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

	/**
	 * create file path
	 *
	 * @param absolutePath String
	 * @return String
	 */
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

	/**
	 * fill integer with leading zeros
	 *
	 * @param i int
	 * @return String
	 */
	private String getIndex(int i) {
		return String.format("%04d", i);
	}

	/**
	 * sort list
	 *
	 * @param rootPath File
	 * @return ArrayList<File>
	 */
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

	/**
	 * get size to form order labels
	 *
	 * @param nameMap HashMap<String, String>
	 * @param rootPath File
	 * @return ArrayList<File>
	 */
	private ArrayList<File> getImageList(HashMap<String, String> nameMap, File rootPath) {
		ArrayList<File> fileList = sortList(rootPath);

		for(String oldFileName:nameMap.values()) {
			fnCount = oldFileName.lastIndexOf('/') + 1;
			String oldFN = oldFileName.substring(fnCount);
			String[] fnParts = oldFN.split("_");
			for(String fnPart:fnParts) {
				if(fnPart.matches("\\d*")) {
					fnCount += fnPart.length() + 1;
					break;
				} else {
					fnCount += fnPart.length() + 1;
				}
			}
		}

		return fileList;
	}

	/**
	 * output Mets part to xml file
	 *
	 * @param meta String
	 * @param structMap Element
	 * @param fileSec Element
	 * @param structLink Element
	 */
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