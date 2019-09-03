package org.ub.manumed;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

public class Mets {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

	private Namespace mets = Namespace.getNamespace("mets","http://www.loc.gov/METS/");
	private Namespace xlink = Namespace.getNamespace("xlink","http://www.w3.org/1999/xlink");

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
		int id = 0;
		ArrayList<File> fileList = getImageList(rootPath);
		Element fileGrp = setFileGrp();
		Element dmdPhys = setDmdPhys();
		Element structMap = setStructMap(dmdPhys);
		Element structLink = setStructLink();
		Element fileSec = setFileSec(fileList, id, fileGrp, dmdPhys, structLink);
		print(meta, structMap, fileSec, structLink);
	}

	private HashMap<String, String> doMapping(File renameFile) {
		HashMap<String, String> map = new HashMap<>();
		try {
			FileInputStream fos = new FileInputStream(renameFile);
			byte[] buffer = new byte[fos.available()];
			fos.read(buffer);
			fos.close();

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
			e.printStackTrace();
		}

		return map;
	}

	private Element setDmdPhys() {
		return new Element("div", mets).setAttribute("DMDID","DMDPHYS_0000").setAttribute("ID","PHYS_0000").setAttribute("TYPE","BoundBook");
	}

	private Element setStructMap(Element dmdPhys) {
		Element structMap = new Element("structMap", mets);
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

				String fileid = getIndex(x);
				Element file = new Element("file", mets).setAttribute("ID","FILE_" + fileid).setAttribute("MIMETYPE","image/tiff");
				Element flocat = new Element("FLocat", mets).setAttribute("LOCTYPE","URL").setAttribute("href", makeURI(f.getAbsolutePath()), xlink).setNamespace(mets);
				file.addContent(flocat);
				fileGrp.addContent(file);

				Element div = new Element("div", mets).setAttribute("ID","PHYS_" + fileid).setAttribute("ORDER", String.valueOf(x)).setAttribute("ORDERLABEL", oldInfo).setAttribute("TYPE","page");
				Element fptr = new Element("fptr", mets).setAttribute("FILEID","FILE_" + fileid);
				div.addContent(fptr);
				dmdPhys.addContent(div);

				Element smLink = new Element("smLink", mets).setAttribute("to","PHYS_" + fileid, xlink).setAttribute("from","LOG_0000",xlink).setNamespace(mets);
				structLink.addContent(smLink);
			}
		}
		fileSec.addContent(fileGrp);

		return fileSec;
	}

	private Element setFileSec(ArrayList<File> fileList, int id, Element fileGrp, Element dmdPhys, Element structLink) {
		Element fileSec = new Element("fileSec", mets);
		int x = 0;

		for(File f:fileList) {
			if(f.isFile() && f.getName().toLowerCase().endsWith(".tif")) {
				x++;

				String fileID = getIndex(x);
				Element file = new Element("file", mets).setAttribute("ID","FILE_" + fileID).setAttribute("MIMETYPE","image/tiff");
				Element flocat = new Element("FLocat", mets).setAttribute("LOCTYPE","URL").setAttribute("href", makeURI(f.getAbsolutePath()), xlink).setNamespace(mets);
				file.addContent(flocat);
				fileGrp.addContent(file);

				Element div = new Element("div", mets).setAttribute("ID","PHYS_" + fileID).setAttribute("ORDER", String.valueOf(x)).setAttribute("ORDERLABEL", fileID).setAttribute("TYPE","page");
				Element fptr = new Element("fptr", mets).setAttribute("FILEID","FILE_" + fileID);
				div.addContent(fptr);
				dmdPhys.addContent(div);

				Element smLink = new Element("smLink", mets).setAttribute("to","PHYS_" + fileID, xlink).setAttribute("from","LOG_0000",xlink).setNamespace(mets);
				structLink.addContent(smLink);
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
		String index = String.valueOf(i);
		while(index.length()<4) {
			index = "0" + index;
		}

		return index;
	}

	private ArrayList<File> getImageList(HashMap<String, String> nameMap, File rootPath, int id) {
		ArrayList<File> fileList = new ArrayList<>();
		File[] files = rootPath.listFiles();
		for(File f:Objects.requireNonNull(files)) {
			if(f.isFile() && f.getName().toLowerCase().endsWith(".tif")) {
				fileList.add(f);
			}
		}

		Collections.sort(fileList);

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

		for(String part : counterTable.keySet())
		{
			if(counterTable.get(part) == nameMap.values().size())
				id += (part + "_").length();
		}

		return fileList;
	}

	private ArrayList<File> getImageList(File rootPath) {
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

	private void print(String meta, Element structMap, Element fileSec, Element structLink) {
		try {
			Document doc = new SAXBuilder().build(new File(meta));
			XMLOutputter outxml = new XMLOutputter(Format.getPrettyFormat());

			new XMLOutputter(Format.getPrettyFormat()).output(structMap, System.out);
			new XMLOutputter(Format.getPrettyFormat()).output(fileSec, System.out);
			new XMLOutputter(Format.getPrettyFormat()).output(structLink, System.out);

			int p = -1;
			for(Element sMap : doc.getRootElement().getChildren("structMap", mets)) {
				if(sMap.getAttributeValue("TYPE").equals("LOGICAL")) {
					p = doc.getRootElement().indexOf(sMap);

				}
			}

			doc.getRootElement().addContent(p - 1, fileSec);

			for(Element sMap : doc.getRootElement().getChildren("structMap", mets)) {
				if(sMap.getAttributeValue("TYPE").equals("PHYSICAL")) {
					p = doc.getRootElement().indexOf(sMap);
					doc.getRootElement().removeContent(sMap);
				}
			}

			doc.getRootElement().addContent(p, structMap);

			Element sLink = doc.getRootElement().getChild("structLink", mets);
			doc.getRootElement().removeContent(sLink);
			doc.getRootElement().addContent(structLink);

			outxml.output(doc, new FileOutputStream(meta));
			logger.debug("Finished.");
		} catch (IOException | JDOMException e) {
			e.printStackTrace();
		}
	}




/*
	private void init(String meta, String imagepath) {

		File renamefile = new File(rootpath.getAbsolutePath() + File.separator + "mapping.txt");
		logger.debug("Renamelist: " + renamefile);
		if(!renamefile.exists()) {
			logger.error("Renamefile doesn't exist!!!");
			System.exit(0);
		}

	}

 */

}