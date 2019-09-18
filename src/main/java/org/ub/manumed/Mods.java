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
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.filter.Filters;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static org.ub.manumed.Importer.*;

class Mods {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

	private Element intern;
	private Element dmdPhys = getElement("dmdphys");
	private int dmd = 1;

	private String metadata = mapping.getProperty("meta");
	private String dmdID = mapping.getProperty("dmdid");
	private String val = mapping.getProperty("value");
	private String mdWr = mapping.getProperty("mdwrap");
	private String go = mapping.getProperty("go");
	private String dmdLog = mapping.getProperty("dmdlog");
	private String xmlDt = mapping.getProperty("xmldata");
	private String ext = mapping.getProperty("extension");
	private String link = mapping.getProperty("link");

	Mods() {

    }

	Mods(String meta) {
		create(meta);
	}

	/**
	 * return an Element from a specified XPAth
	 *
	 * @param definition String
	 * @return Element
	 */
	Element getElement(String definition) {
		String xPath = mapping.getProperty(definition);
		xpe = fact.compile(xPath, Filters.element(), null, spaceList);
		return (Element) xpe.evaluateFirst(docMeta);
	}

	/**
	 * add Mods metadata information
	 *
	 * @param definition String
	 * @param tag String
	 * @return String
	 */
	private String setElement(String definition, String tag) {
		String xPath = mapping.getProperty(definition);
		xpe = fact.compile(xPath, Filters.attribute(),null, spaceList);
		String value = "";
		if (xpe.evaluateFirst(docHida) != null) {
			value = ((Attribute) xpe.evaluateFirst(docHida)).getValue();
			Element ms = new Element(metadata, kitodo).setAttribute("name", tag).setText(value);
			intern.addContent(ms);
		}
		return value;
	}

	/**
	 * add Mods metadata and link to manuscripta mediaevalia
	 */
	private void setElement() {
		String xPath = mapping.getProperty("erschliessung");
		xpe = fact.compile(xPath, Filters.attribute(),null, spaceList);
		String value;
		if (xpe.evaluateFirst(docHida) != null) {
			value = ((Attribute) xpe.evaluateFirst(docHida)).getValue();
			Element ms = new Element(metadata, kitodo).setAttribute("name", "ms_record_id").setText("http://www.manuscripta-mediaevalia.de/dokumente/html/obj" + value);
			intern.addContent(ms);
		}
	}

	/**
	 * creates unique identifier containing place, institution and shelfmark
	 *
	 * @param place String
	 * @param institution String
	 * @param shelfmark String
	 */
	private void setIDShelfmark(String place, String institution, String shelfmark) {
		if (!place.isEmpty() && !institution.isEmpty() && !shelfmark.isEmpty()) {
			Element msIdentifyingShelfmark = new Element(metadata, kitodo).setAttribute("name","TitleDocMain").setText(place + ", " + institution + ", " + shelfmark);
			intern.addContent(msIdentifyingShelfmark);
		}
	}

	/**
	 * add unique Mods shelfmark
	 */
	private void setShelfmarkHash() {
		String hash = mapping.getProperty("hash");
		xpe = fact.compile(hash, Filters.text(), null, spaceList);
		String folderHash = ((Text) xpe.evaluateFirst(docMeta)).getText();
		Element msIdentifyingShelfmark = new Element(metadata, kitodo).setAttribute("name", "ms_identifying_shelfmark_hash").setText(folderHash.substring(folderHash.lastIndexOf('/') + 1, folderHash.lastIndexOf('_')));
		intern.addContent(msIdentifyingShelfmark);
	}

	/**
	 * add specific attributes about the manuscript
	 *
	 * @param definition String
	 * @param tag String
	 */
	private void setAttribute(String definition, String tag) {
		String xPath = mapping.getProperty(definition);
		xpe = fact.compile(xPath, Filters.attribute(), null, spaceList);
		if (xpe.evaluateFirst(docHida) != null) {
			String value = ((Attribute) xpe.evaluateFirst(docHida)).getValue();
			if (value.contains("&")) {
				int i = 0;
				if (value.indexOf('&', i) == value.lastIndexOf('&')) {
					String child = value.substring(i, value.lastIndexOf('&') - 1);
					Element ms = new Element(metadata, kitodo).setAttribute("name", tag).setText(child);
					intern.addContent(ms);
				} else {
					while (value.indexOf('&', i) != value.lastIndexOf('&')) {
						int j = value.indexOf('&', i);
						String child = value.substring(i, j - 1);
						Element ms = new Element(metadata, kitodo).setAttribute("name", tag).setText(child);
						i = j + 2;
						intern.addContent(ms);
					}
				}
				String child = value.substring(value.lastIndexOf('&') + 2);
				Element ms = new Element(metadata, kitodo).setAttribute("name", tag).setText(child);
				intern.addContent(ms);
			} else {
				Element ms = new Element(metadata, kitodo).setAttribute("name", tag).setText(value);
				intern.addContent(ms);
			}
		}
	}

	/**
	 * add precise date of the manuscript
	 */
	private void setDate() {
		String xPath = mapping.getProperty("datiert");
		xpe = fact.compile(xPath, Filters.element(), null, spaceList);

		List<Element> listDates = xpe.evaluate(docHida);
		for (Element dat:listDates) {
			List<Element> listChilds = dat.getChildren();
			for (Element c:listChilds) {
				if (c.getAttributeValue("Type").equals("5064")) {
					intern.addContent(new Element(metadata, kitodo).setAttribute("name", "ms_dated").setText(c.getAttributeValue(val)));
				}
			}
		}
	}

	/**
	 * add period of the manuscript
	 */
	private void setDating() {
		String xPath = mapping.getProperty("datierung");
		xpe = fact.compile(xPath, Filters.element(), null, spaceList);

		List<Element> listDating = xpe.evaluate(docHida);
		for (Element dau:listDating) {
			List<Element> listChilds = dau.getChildren();
			for (Element c:listChilds) {
				if (c.getAttributeValue("Type").equals("5064")) {
					String value = c.getAttributeValue(val);

					if(mapping.containsKey(value))
					{
						intern.addContent(new Element(metadata, kitodo).setAttribute("name", "ms_dating_verbal").setText(mapping.getProperty(value)));
					}

					if (value.contains("/")) {
						intern.addContent(new Element(metadata, kitodo).setAttribute("name","ms_dating_not_before").setText(value.substring(0, value.indexOf('/'))));
						intern.addContent(new Element(metadata, kitodo).setAttribute("name","ms_dating_not_after").setText(value.substring(value.indexOf('/') + 1)));
					} else {
						intern.addContent(new Element(metadata, kitodo).setAttribute("name", "ms_dated").setText(value));
					}
				}
			}
		}
	}

	/**
	 * add names of an author or an institution
	 *
	 * @param in Element
	 * @param out Element
	 * @return Element
	 */
	private Element setName(Element in, Element out) {
		Element firstName = new Element("firstName", kitodo).setText(in.getAttributeValue(val));
		Element displayName = new Element("displayName", kitodo).setText(in.getAttributeValue(val));
		out.addContent(firstName);
		out.addContent(displayName);
		return out;
	}

	/**
	 * add specified links to the author or institution
	 *
	 * @param in Element
	 * @param out Element
	 * @return Element
	 */
	private Element setAuthority(Element in, Element out) {
		Element authorityID = new Element("authorityID", kitodo).setText("gnd");
		Element authorityURI = new Element("authorityURI", kitodo).setText(link);
		Element authorityValue = new Element("authorityValue", kitodo).setText(link + in.getAttributeValue(val));
		out.addContent(authorityID);
		out.addContent(authorityURI);
		out.addContent(authorityValue);
		return out;
	}

	/**
	 * create the whole Mods element of the author or institution
	 *
	 * @param definition String
	 * @param tag String
	 * @param listName List<String>
	 * @param listAuthority List<String>
	 */
	private void setResponsibility(String definition, String tag, List<String> listName, List<String> listAuthority) {
        String xPath = mapping.getProperty(definition);
        xpe = fact.compile(xPath, Filters.element(), null, spaceList);

        List<Element> listResponse = xpe.evaluate(docHida);
        for(Element r:listResponse) {
            Element msResponse = new Element(metadata, kitodo).setAttribute("name", tag).setAttribute("type", "person");
            List<Element> listChildren = r.getChildren();
            for(Element c:listChildren) {
                if(listName.size() == 1) {
                    if (c.getAttributeValue("Type").equals(listName.get(0))) {
                        setName(c, msResponse);
                    }
                } else {
                    if (c.getAttributeValue("Type").equals(listName.get(0)) || c.getAttributeValue("Type").equals(listName.get(1))) {
						setName(c, msResponse);
                    }
                }
                if(listAuthority.size() == 1) {
                    if (c.getAttributeValue("Type").equals(listAuthority.get(0))) {
                        setAuthority(c, msResponse);
                    }
                } else {
                    if (c.getAttributeValue("Type").equals(listAuthority.get(0)) || c.getAttributeValue("Type").equals(listAuthority.get(1))) {
						setAuthority(c, msResponse);
                    }
                }
            }
            intern.addContent(msResponse);
        }
    }

	/**
	 * create the plain element dmdSec
	 *
	 * @return Element
	 */
	private Element createDmdSec() {
		Element dmdSec = new Element("dmdSec", mets);
		Element mdWrap = new Element(mdWr, mets);
		mdWrap.setAttribute("MDTYPE", "MODS");
		Element xmlData = new Element(xmlDt, mets);
		Element mod = new Element("mods", mods);
		Element extension = new Element(ext, mods);
		Element goobi = new Element(go, kitodo);

		dmdSec.addContent(mdWrap);
		mdWrap.addContent(xmlData);
		xmlData.addContent(mod);
		mod.addContent(extension);
		extension.addContent(goobi);

		return dmdSec;
	}

	/**
	 * create the element dmdSec with specified ID
	 *
	 * @param dmd int
	 * @param child Element
	 * @return Element
	 */
    private Element createDmdSec(int dmd, Element child) {
        Element dmdSec = new Element("dmdSec", mets);
		dmdSec.setAttribute("ID", dmdLog + String.format("%04d", dmd));
        Element mdWrap = new Element(mdWr, mets);
        mdWrap.setAttribute("MDTYPE", "MODS");
        Element xmlData = new Element(xmlDt, mets);
        Element mod = new Element("mods", mods);
        Element extension = new Element(ext, mods);
        Element goobi = new Element("go", kitodo);

        dmdSec.addContent(mdWrap);
		mdWrap.addContent(xmlData);
		xmlData.addContent(mod);
		mod.addContent(extension);
		extension.addContent(goobi);
		goobi.addContent(child);

        return dmdSec;
    }

	/**
	 * add Mods part for an host
	 */
	private void setHost() {
		Element dmdSec = createDmdSec();

		String xPath = mapping.getProperty("verwalter");
		xpe = fact.compile(xPath, Filters.element(), null, spaceList);

		List<Element> listHosts = xpe.evaluate(docHida);

		Element msHostVolumeInstitution = null;
		Element msHostVolumeShelfmark = null;
		String host = "";
		String bezSignatur = "";

		for (Element v:listHosts) {
            Element goobi = dmdSec.getChild(mdWr, mets).getChild(xmlDt, mets).getChild("mods", mods).getChild(ext, mods).getChild(go, kitodo);
			if (v.getAttributeValue("Type").equals("501k")) {
				msHostVolumeInstitution = new Element(metadata, kitodo).setAttribute("name","ms_host_volume_institution").setText(v.getAttributeValue(val));
				host = v.getAttributeValue(val);
			}
			if (v.getAttributeValue("Type").equals("501m")) {
				msHostVolumeShelfmark = new Element(metadata, kitodo).setAttribute("name","ms_host_volume_shelfmark").setText(v.getAttributeValue(val));
				bezSignatur = v.getAttributeValue(val);
			}

			if (msHostVolumeInstitution != null) {
				goobi.addContent(msHostVolumeInstitution);
			}

			if (msHostVolumeShelfmark != null) {
				goobi.addContent(msHostVolumeShelfmark);
			}

			if (!host.isEmpty() && !bezSignatur.isEmpty()) {
				Element msHostIdentifyingShelfmark = new Element(metadata, kitodo).setAttribute("name","ms_host_identifying_shelfmark").setText(host + ", " + bezSignatur);
				goobi.addContent(msHostIdentifyingShelfmark);
			}
		}

		if ((msHostVolumeInstitution != null) || (msHostVolumeShelfmark != null)) {
			dmdSec.setAttribute("ID", dmdLog + String.format("%04d", dmd));
			docMeta.getRootElement().addContent(docMeta.getRootElement().indexOf(dmdPhys), dmdSec);
			dmd += 1;
		}
	}

	/**
	 * create a single element in the Mods structure
	 *
	 * @param divRoot Element
	 * @param field Element
	 */
	private void setStructureElements(Element divRoot, Element field) {
		if(field.getAttributeValue("Type").equals("5230") && field.getAttributeValue(val).equals("Einband")) {
			Element div = new Element("div", mets).setAttribute(dmdID, dmdLog + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","slub_Binding");
			divRoot.addContent(div);
		} else if(field.getAttributeValue("Type").equals("5210") && field.getAttributeValue(val).equals("Fragment")) {
			Element div = new Element("div", mets).setAttribute(dmdID, dmdLog + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","Fragment");
			divRoot.addContent(div);
		} else if(field.getAttributeValue("Type").equals("bezwrk") && field.getAttributeValue(val).equals("Abschrift")) {
			Element div = new Element("div", mets).setAttribute(dmdID, dmdLog + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","ContainedWork");
			divRoot.addContent(div);
			if(!field.getChildren().isEmpty()) {
				for(Element script:field.getChildren()) {
					if (script.getAttributeValue("Type").equals("6930") || script.getAttributeValue("Type").equals("6930gi")) {
						Element msWorkTitle = new Element(metadata, kitodo).setAttribute("name", "TitleDocMain").setText(script.getAttributeValue(val));
						Element dmdSec = createDmdSec(dmd, msWorkTitle);
						docMeta.getRootElement().addContent(docMeta.getRootElement().indexOf(dmdPhys), dmdSec);
						dmd++;
					}
					if (script.getAttributeValue("Type").equals("6998")) {
						Element msWorkAuthority = new Element(metadata, kitodo).setAttribute("name", "ms_work_authority").setText(link + script.getAttributeValue(val));
						Element dmdSec = createDmdSec(dmd, msWorkAuthority);
						docMeta.getRootElement().addContent(docMeta.getRootElement().indexOf(dmdPhys), dmdSec);
                        dmd++;
					}
					if (script.getAttributeValue("Type").equals("6922")) {
						Element msWorkSubject = new Element(metadata, kitodo).setAttribute("name", "ms_work_subject").setText(script.getAttributeValue(val));
						Element dmdSec = createDmdSec(dmd, msWorkSubject);
						docMeta.getRootElement().addContent(docMeta.getRootElement().indexOf(dmdPhys), dmdSec);
                        dmd++;
					}
				}
			}
		} else if(field.getAttributeValue("Type").equals("bezper") && field.getAttributeValue(val).equals("Autorschaft")) {
			Element div = new Element("div", mets).setAttribute(dmdID, dmdLog + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","ContainedWork");
			divRoot.addContent(div);
			if(!field.getChildren().isEmpty()) {
				for(Element author:field.getChildren()) {
					Element msWorkAuthor = new Element(metadata, kitodo).setAttribute("name", "ms_work_author").setAttribute("type", "person");
					if (author.getAttributeValue("Type").equals("4100") || author.getAttributeValue("Type").equals("4100gi")) {
                        setName(author, msWorkAuthor);
                    }
					if (author.getAttributeValue("Type").equals("z001")) {
                        setAuthority(author, msWorkAuthor);
                    }
					Element dmdSec = createDmdSec(dmd, msWorkAuthor);
					docMeta.getRootElement().addContent(docMeta.getRootElement().indexOf(dmdPhys), dmdSec);
					dmd++;
				}
			}
		}
	}

	/**
	 * create first element of the table of content
	 *
	 * @return Element
	 */
	private Element setStructure() {
		String xPath = mapping.getProperty("werk");
		xpe = fact.compile(xPath, Filters.element(), null, spaceList);
		List<Element> listBlocks = xpe.evaluate(docHida);

		Element divRoot = new Element("div", mets).setAttribute(dmdID,"DMDLOG_0000").setAttribute("ID","LOG_0000").setAttribute("TYPE","Manuscript");

		for(Element block:listBlocks) {
			for(Element field:block.getChildren()) {
				if(field.getName().equals("Block")) {
					for(Element child:field.getChildren()) {
						setStructureElements(divRoot, child);
					}
				} else {
					setStructureElements(divRoot, field);
				}
			}
		}
		return divRoot;
	}

	/**
	 * create logical structure
	 *
	 * @param div0 Element
	 */
	private void addLogical(Element div0) {
		for(Element sM: docMeta.getRootElement().getChildren("structMap", mets))
		{
			if(sM.getAttributeValue("TYPE").equals("LOGICAL"))
			{
				sM.removeChildren("div", mets);
				sM.addContent(div0);
			}
		}
	}

	/**
	 * print metadata to the meta.xml
	 *
	 * @param meta String
	 */
	private void out(String meta) {
		try {
			outXML.output(docMeta, new FileOutputStream(meta));
			logger.debug("meta.xml successfully created.");
		} catch (IOException e) {
			logger.error("meta.xml could not created successfully.", e.fillInStackTrace());
		}
	}

	/**
	 * add all elements to the JDOM structure
	 *
	 * @param meta String
	 */
	private void create(String meta) {
		intern = getElement("mods");
		String place = setElement("ort", "ms_place");
		String institution = setElement("institution", "ms_institution");
		String shelfmark = setElement("signatur", "ms_shelfmark");
		setIDShelfmark(place, institution, shelfmark);
		setShelfmarkHash();
		setElement("objekttitel", "ms_manuscript_title");
		setAttribute("medium", "ms_medium");
		setElement("formtyp", "ms_type");
		setAttribute("beschreibstoff", "ms_material");
		setElement("umfang", "ms_extent");
		setElement("abmessungen", "ms_dimensions");
		setAttribute("sprache", "ms_language");
		setElement("lokalisierung", "ms_place_of_origin");
		setDate();
		setDating();
		List<String> listName = new ArrayList<>();
		listName.add("4100");
		listName.add("4100gi");
		List<String> listAuthority = new ArrayList<>();
		listAuthority.add("z001");
		setResponsibility("autor", "ms_author", listName, listAuthority);
		listAuthority.add("y001");
		setResponsibility("personen", "ms_previous_owner_person", listName, listAuthority);
		listName.clear();
		listAuthority.clear();
		listName.add("4600");
		listAuthority.add("4998");
		setResponsibility("sozietaet", "ms_previous_owner_institution", listName, listAuthority);
		setHost();
		Element divRoot = setStructure();
		setAttribute("quelle", "ms_record_origin");
		setElement();
		addLogical(divRoot);
		out(meta);
	}

}