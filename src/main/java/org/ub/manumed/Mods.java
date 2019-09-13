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

	Mods() {

    }

	Mods(String meta, String map) {
		create(meta);
	}

	Element getElement(String definition) {
		String xPath = mapping.getProperty(definition);
		xpe = fact.compile(xPath, Filters.element(), null, spaceList);
		return (Element) xpe.evaluateFirst(docMeta);
	}

	private String setElement(String definition, String tag) {
		String xPath = mapping.getProperty(definition);
		xpe = fact.compile(xPath, Filters.attribute(),null, spaceList);
		String value = "";
		if (xpe.evaluateFirst(docHida) != null) {
			value = ((Attribute) xpe.evaluateFirst(docHida)).getValue();
			Element ms = new Element("metadata", kitodo).setAttribute("name", tag).setText(value);
			intern.addContent(ms);
		}
		return value;
	}

	private void setElement() {
		String xPath = mapping.getProperty("erschliessung");
		xpe = fact.compile(xPath, Filters.attribute(),null, spaceList);
		String value;
		if (xpe.evaluateFirst(docHida) != null) {
			value = ((Attribute) xpe.evaluateFirst(docHida)).getValue();
			Element ms = new Element("metadata", kitodo).setAttribute("name", "ms_record_id").setText("http://www.manuscripta-mediaevalia.de/dokumente/html/obj" + value);
			intern.addContent(ms);
		}
	}

	private void setIDShelfmark(String place, String institution, String shelfmark) {
		if (!place.isEmpty() && !institution.isEmpty() && !shelfmark.isEmpty()) {
			Element msIdentifyingShelfmark = new Element("metadata", kitodo).setAttribute("name","TitleDocMain").setText(place + ", " + institution + ", " + shelfmark);
			intern.addContent(msIdentifyingShelfmark);
		}
	}

	private void setShelfmarkHash() {
		String hash = mapping.getProperty("hash");
		xpe = fact.compile(hash, Filters.text(), null, spaceList);
		String folderHash = ((Text) xpe.evaluateFirst(docMeta)).getText();
		Element msIdentifyingShelfmark = new Element("metadata", kitodo).setAttribute("name", "ms_identifying_shelfmark_hash").setText(folderHash.substring(folderHash.lastIndexOf('/') + 1, folderHash.lastIndexOf('_')));
		intern.addContent(msIdentifyingShelfmark);
	}

	private void setAttribute(String definition, String tag) {
		String xPath = mapping.getProperty(definition);
		xpe = fact.compile(xPath, Filters.attribute(), null, spaceList);
		if (xpe.evaluateFirst(docHida) != null) {
			String value = ((Attribute) xpe.evaluateFirst(docHida)).getValue();
			if (value.contains("&")) {
				int i = 0;
				if (value.indexOf('&', i) == value.lastIndexOf('&')) {
					String child = value.substring(i, value.lastIndexOf('&') - 1);
					Element ms = new Element("metadata", kitodo).setAttribute("name", tag).setText(child);
					intern.addContent(ms);
				} else {
					while (value.indexOf('&', i) != value.lastIndexOf('&')) {
						int j = value.indexOf('&', i);
						String child = value.substring(i, j - 1);
						Element ms = new Element("metadata", kitodo).setAttribute("name", tag).setText(child);
						i = j + 2;
						intern.addContent(ms);
					}
				}
				String child = value.substring(value.lastIndexOf('&') + 2);
				Element ms = new Element("metadata", kitodo).setAttribute("name", tag).setText(child);
				intern.addContent(ms);
			} else {
				Element ms = new Element("metadata", kitodo).setAttribute("name", tag).setText(value);
				intern.addContent(ms);
			}
		}
	}

	private void setDate(String definition) {
		String xPath = mapping.getProperty(definition);
		xpe = fact.compile(xPath, Filters.element(), null, spaceList);

		List<Element> listDates = xpe.evaluate(docHida);
		for (Element dat:listDates) {
			List<Element> listChilds = dat.getChildren();
			for (Element c:listChilds) {
				if (c.getAttributeValue("Type").equals("5064")) {
					intern.addContent(new Element("metadata", kitodo).setAttribute("name", "ms_dated").setText(c.getAttributeValue("Value")));
				}
			}
		}
	}

	private void setDating(String definition) {
		String xPath = mapping.getProperty(definition);
		xpe = fact.compile(xPath, Filters.element(), null, spaceList);

		List<Element> listDating = xpe.evaluate(docHida);
		for (Element dau:listDating) {
			List<Element> listChilds = dau.getChildren();
			for (Element c:listChilds) {
				if (c.getAttributeValue("Type").equals("5064")) {
					String value = c.getAttributeValue("Value");

					if(mapping.containsKey(value))
					{
						intern.addContent(new Element("metadata", kitodo).setAttribute("name", "ms_dating_verbal").setText(mapping.getProperty(value)));
					}

					if (value.contains("/")) {
						intern.addContent(new Element("metadata", kitodo).setAttribute("name","ms_dating_not_before").setText(value.substring(0, value.indexOf('/'))));
						intern.addContent(new Element("metadata", kitodo).setAttribute("name","ms_dating_not_after").setText(value.substring(value.indexOf('/') + 1)));
					} else {
						intern.addContent(new Element("metadata", kitodo).setAttribute("name", "ms_dated").setText(value));
					}
				}
			}
		}
	}

	private Element setName(Element in, Element out) {
		Element firstName = new Element("firstName", kitodo).setText(in.getAttributeValue("Value"));
		Element displayName = new Element("displayName", kitodo).setText(in.getAttributeValue("Value"));
		out.addContent(firstName);
		out.addContent(displayName);
		return out;
	}

	private Element setAuthority(Element in, Element out) {
		Element authorityID = new Element("authorityID", kitodo).setText("gnd");
		Element authorityURI = new Element("authorityURI", kitodo).setText("http://d-nb.info/gnd/");
		Element authorityValue = new Element("authorityValue", kitodo).setText("http://d-nb.info/gnd/" + in.getAttributeValue("Value"));
		out.addContent(authorityID);
		out.addContent(authorityURI);
		out.addContent(authorityValue);
		return out;
	}

	private void setResponsibility(String definition, String tag, List<String> listName, List<String> listAuthority) {
        String xPath = mapping.getProperty(definition);
        xpe = fact.compile(xPath, Filters.element(), null, spaceList);

        List<Element> listResponse = xpe.evaluate(docHida);
        for(Element r:listResponse) {
            Element msResponse = new Element("metadata", kitodo).setAttribute("name", tag).setAttribute("type", "person");
            List<Element> listChildren = r.getChildren();
            for(Element c:listChildren) {
                if(listName.size() == 1) {
                    if (c.getAttributeValue("Type").equals(listName.get(0))) {
                        msResponse = setName(c, msResponse);
                    }
                } else {
                    if (c.getAttributeValue("Type").equals(listName.get(0)) || c.getAttributeValue("Type").equals(listName.get(1))) {
						msResponse = setName(c, msResponse);
                    }
                }
                if(listAuthority.size() == 1) {
                    if (c.getAttributeValue("Type").equals(listAuthority.get(0))) {
                        msResponse = setAuthority(c, msResponse);
                    }
                } else {
                    if (c.getAttributeValue("Type").equals(listAuthority.get(0)) || c.getAttributeValue("Type").equals(listAuthority.get(1))) {
						msResponse = setAuthority(c, msResponse);
                    }
                }
            }
            intern.addContent(msResponse);
        }
    }

	private Element createDmdSec() {
		Element dmdSec = new Element("dmdSec", mets);
		Element mdWrap = new Element("mdWrap", mets);
		mdWrap.setAttribute("MDTYPE", "MODS");
		Element xmlData = new Element("xmlData", mets);
		Element mod = new Element("mods", mods);
		Element extension = new Element("extension", mods);
		Element goobi = new Element("goobi", kitodo);

		dmdSec.addContent(mdWrap);
		mdWrap.addContent(xmlData);
		xmlData.addContent(mod);
		mod.addContent(extension);
		extension.addContent(goobi);

		return dmdSec;
	}

    private Element createDmdSec(int dmd, Element child) {
        Element dmdSec = new Element("dmdSec", mets);
		dmdSec.setAttribute("ID", "DMDLOG_" + String.format("%04d", dmd));
        Element mdWrap = new Element("mdWrap", mets);
        mdWrap.setAttribute("MDTYPE", "MODS");
        Element xmlData = new Element("xmlData", mets);
        Element mod = new Element("mods", mods);
        Element extension = new Element("extension", mods);
        Element goobi = new Element("goobi", kitodo);

        dmdSec.addContent(mdWrap);
		mdWrap.addContent(xmlData);
		xmlData.addContent(mod);
		mod.addContent(extension);
		extension.addContent(goobi);
		goobi.addContent(child);

        return dmdSec;
    }

	private int setHost(String definition) {
		int dmd = 1;

		Element dmdSec = createDmdSec();

		String xPath = mapping.getProperty(definition);
		xpe = fact.compile(xPath, Filters.element(), null, spaceList);

		List<Element> listHosts = xpe.evaluate(docHida);

		Element msHostVolumeInstitution = null;
		Element msHostVolumeShelfmark = null;
		String host = "";
		String bezSignatur = "";

		for (Element v:listHosts) {
            Element goobi = dmdSec.getChild("mdWrap", mets).getChild("xmlData", mets).getChild("mods", mods).getChild("extension", mods).getChild("goobi", kitodo);
			if (v.getAttributeValue("Type").equals("501k")) {
				msHostVolumeInstitution = new Element("metadata", kitodo).setAttribute("name","ms_host_volume_institution").setText(v.getAttributeValue("Value"));
				host = v.getAttributeValue("Value");
			}
			if (v.getAttributeValue("Type").equals("501m")) {
				msHostVolumeShelfmark = new Element("metadata", kitodo).setAttribute("name","ms_host_volume_shelfmark").setText(v.getAttributeValue("Value"));
				bezSignatur = v.getAttributeValue("Value");
			}

			if (msHostVolumeInstitution != null) {
				goobi.addContent(msHostVolumeInstitution);
			}

			if (msHostVolumeShelfmark != null) {
				goobi.addContent(msHostVolumeShelfmark);
			}

			if (!host.isEmpty() && !bezSignatur.isEmpty()) {
				Element msHostIdentifyingShelfmark = new Element("metadata", kitodo).setAttribute("name","ms_host_identifying_shelfmark").setText(host + ", " + bezSignatur);
				goobi.addContent(msHostIdentifyingShelfmark);
			}
		}

		if ((msHostVolumeInstitution != null) || (msHostVolumeShelfmark != null)) {

			dmdSec.setAttribute("ID", "DMDLOG_" + String.format("%04d", dmd));
			docMeta.getRootElement().addContent(dmd + 1, dmdSec);
			dmd += 1;
		}

		return dmd;
	}

	private int setStructureElements(Element divRoot, Element field, int dmd) {
		if(field.getAttributeValue("Type").equals("5230") && field.getAttributeValue("Value").equals("Einband")) {
			Element div = new Element("div", mets).setAttribute("DMDID", "DMDLOG_" + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","slub_Binding");
			divRoot.addContent(div);
		} else if(field.getAttributeValue("Type").equals("5210") && field.getAttributeValue("Value").equals("Fragment")) {
			Element div = new Element("div", mets).setAttribute("DMDID", "DMDLOG_" + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","Fragment");
			divRoot.addContent(div);
		} else if(field.getAttributeValue("Type").equals("bezwrk") && field.getAttributeValue("Value").equals("Abschrift")) {
			Element div = new Element("div", mets).setAttribute("DMDID", "DMDLOG_" + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","ContainedWork");
			divRoot.addContent(div);
			if(!field.getChildren().isEmpty()) {
				for(Element script:field.getChildren()) {
					if (script.getAttributeValue("Type").equals("6930") || script.getAttributeValue("Type").equals("6930gi")) {
						Element msWorkTitle = new Element("metadata", kitodo).setAttribute("name", "TitleDocMain").setText(script.getAttributeValue("Value"));
						Element dmdSec = createDmdSec(dmd, msWorkTitle);
						docMeta.getRootElement().addContent(dmdSec);
						dmd++;
					}
					if (script.getAttributeValue("Type").equals("6998")) {
						Element msWorkAuthority = new Element("metadata", kitodo).setAttribute("name", "ms_work_authority").setText("http://d-nb.info/gnd/" + script.getAttributeValue("Value"));
						Element dmdSec = createDmdSec(dmd, msWorkAuthority);
						docMeta.getRootElement().addContent(dmdSec);
                        dmd++;
					}
					if (script.getAttributeValue("Type").equals("6922")) {
						Element msWorkSubject = new Element("metadata", kitodo).setAttribute("name", "ms_work_subject").setText(script.getAttributeValue("Value"));
						Element dmdSec = createDmdSec(dmd, msWorkSubject);
						docMeta.getRootElement().addContent(dmdSec);
                        dmd++;
					}
				}
			}
		} else if(field.getAttributeValue("Type").equals("bezper") && field.getAttributeValue("Value").equals("Autorschaft")) {
			Element div = new Element("div", mets).setAttribute("DMDID", "DMDLOG_" + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","ContainedWork");
			divRoot.addContent(div);
			if(!field.getChildren().isEmpty()) {
				for(Element author:field.getChildren()) {
					Element msWorkAuthor = new Element("metadata", kitodo).setAttribute("name", "ms_work_author").setAttribute("type", "person");
					if (author.getAttributeValue("Type").equals("4100") || author.getAttributeValue("Type").equals("4100gi")) {
                        setName(author, msWorkAuthor);
                    }
					if (author.getAttributeValue("Type").equals("z001")) {
                        setAuthority(author, msWorkAuthor);
                    }
					Element dmdSec = createDmdSec(dmd, msWorkAuthor);
					docMeta.getRootElement().addContent(dmdSec);
					dmd++;
				}
			}
		}
		return dmd;
	}

	private Element setStructure(String definition, int dmd) {
		String xPath = mapping.getProperty(definition);
		xpe = fact.compile(xPath, Filters.element(), null, spaceList);
		List<Element> listBlocks = xpe.evaluate(docHida);

		Element divRoot = new Element("div", mets).setAttribute("DMDID","DMDLOG_0000").setAttribute("ID","LOG_0000").setAttribute("TYPE","Manuscript");

		for(Element block:listBlocks) {
			for(Element field:block.getChildren()) {
				if(field.getName().equals("Block")) {
					for(Element child:field.getChildren()) {
						dmd = setStructureElements(divRoot, child, dmd);
						dmd++;
					}
				} else {
					dmd = setStructureElements(divRoot, field, dmd);
					dmd++;
				}
			}
		}
		return divRoot;
	}

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

	private void out(String meta) {
		try {
			outXML.output(docMeta, new FileOutputStream(meta));
			logger.debug("meta.xml successfully created.");
		} catch (IOException e) {
			logger.error("meta.xml could not created successfully.", e.fillInStackTrace());
		}
	}

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
		setDate("datiert");
		setDating("datierung");
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
		int dmd = setHost("verwalter");
		Element divRoot = setStructure("werk", dmd);
		setAttribute("quelle", "ms_record_origin");
		setElement();
		addLogical(divRoot);
		out(meta);
	}

}