package org.ub.manumed;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mods {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

	private Document docMeta;
	private Document docHida;
	private XMLOutputter outXML = new XMLOutputter(Format.getPrettyFormat());

	private Namespace kitodo = Namespace.getNamespace("goobi","http://meta.goobi.org/v1.5.1/");
	private Namespace mets = Namespace.getNamespace("mets","http://www.loc.gov/METS/");
	private Namespace mods = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
	private Namespace hd = Namespace.getNamespace("h1","http://www.startext.de/HiDA/DefService/XMLSchema");
	private ArrayList<Namespace> spaceList;

	private Properties mapping = new Properties();

	private XPathFactory fact = XPathFactory.instance();
	private XPathExpression xpe;

	private Element intern;

	Mods(String meta, String hida, String map) {
		build(meta, hida);
		loadProperties(map);
		create(meta);
	}

	private void build(String meta, String hida) {
		try {
			docMeta = new SAXBuilder().build(new File(meta));
			docHida = new SAXBuilder().build(new File(hida));

			spaceList.add(kitodo);
			spaceList.add(mets);
			spaceList.add(mods);
			spaceList.add(hd);
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
	}

	private void loadProperties(String map) {
		try {
			mapping.load(new FileInputStream(map));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Element getElement() {
		String xPath = mapping.getProperty("mods");
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

    private Element createElements() {
        Element dmdSec = new Element("dmdSec", mets);
        Element mdWrap = new Element("mdWrap", mets);
        mdWrap.setAttribute("MDTYPE", "MODS");
        Element xmlData = new Element("xmlData", mets);
        Element mod = new Element("mods", mods);
        Element extension = new Element("extension", mods);
        Element goobi = new Element("goobi", kitodo);

        extension.addContent(goobi);
        mod.addContent(extension);
        xmlData.addContent(mod);
        mdWrap.addContent(xmlData);
        dmdSec.addContent(mdWrap);

        return dmdSec;
    }

	private int setHost(String definition) {
		int dmd = 1;

		Element dmdSec = createElements();

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

	private void setStructureElements(Element divRoot, Element field, int dmd) {
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
					}
					if (script.getAttributeValue("Type").equals("6998")) {
						Element msWorkAuthority = new Element("metadata", kitodo).setAttribute("name", "ms_work_authority").setText("http://d-nb.info/gnd/" + script.getAttributeValue("Value"));
					}
					if (script.getAttributeValue("Type").equals("6922")) {
						Element msWorkSubject = new Element("metadata", kitodo).setAttribute("name", "ms_work_subject").setText(script.getAttributeValue("Value"));
					}
				}
			}
		} else if(field.getAttributeValue("Type").equals("bezper") && field.getAttributeValue("Value").equals("Autorschaft")) {
			Element div = new Element("div", mets).setAttribute("DMDID", "DMDLOG_" + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","ContainedWork");
			divRoot.addContent(div);
			if(!field.getChildren().isEmpty()) {
				Element msWorkAuthor = new Element("metadata", kitodo).setAttribute("name", "ms_work_author").setAttribute("type", "person");
				for(Element author:field.getChildren()) {
					if (author.getAttributeValue("Type").equals("4100") || author.getAttributeValue("Type").equals("4100gi")) {
						msWorkAuthor = setName(author, msWorkAuthor);
					}
					if (author.getAttributeValue("Type").equals("z001")) {
						msWorkAuthor = setAuthority(author, msWorkAuthor);
					}
				}
			}
		}
	}

	private void setStructure(String definition, int dmd) {
		String xPath = mapping.getProperty(definition);
		xpe = fact.compile(xPath, Filters.element(), null, spaceList);
		List<Element> listBlocks = xpe.evaluate(docHida);

		Element divRoot = new Element("div", mets).setAttribute("DMDID","DMDLOG_0000").setAttribute("ID","LOG_0000").setAttribute("TYPE","Manuscript");

		for(Element block:listBlocks) {
			for(Element field:block.getChildren()) {
				if(field.getName().equals("Block")) {
					for(Element child:field.getChildren()) {
						setStructureElements(divRoot, field, dmd);
						dmd++;
					}
				} else {
					setStructureElements(divRoot, field, dmd);
					dmd++;
				}
			}
		}
	}

	/*
	private Element setStructMap(String definition, int dmd) {
		String xPath = mapping.getProperty(definition);
		xpe = fact.compile(xPath, Filters.element(), null, spaceList);

		Element div0 = new Element("div", mets).setAttribute("DMDID","DMDLOG_0000").setAttribute("ID","LOG_0000").setAttribute("TYPE","Manuscript");
		List<Element> listWritings = xpe.evaluate(docHida);
		for(Element w:listWritings) {

			boolean found_bezwerk = false, found_bezpers = false, binding = false;

			List<Element> fields = w.getChildren();
			for(Element f:fields) {
				if(f.getAttributeValue("Type").equals("5230") && f.getAttributeValue("Value").equals("Einband")) {
					binding = true;
					break;
				}
			}

			if (binding) {
				Element dmdSec = createElements();

				//Element ms_binding = new Element("metadata", kitodo).setAttribute("name", "TitleDocMain").setText("Einband");
				//we_goobii.addContent(ms_binding);

				Element dive = new Element("div", mets).setAttribute("DMDID", "DMDLOG_" + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","slub_Binding");

				if (div0.getContentSize() > 0) {
					if (!(div0.getChildren().get(div0.getChildren().size() - 1).getAttributeValue("ID").equals("LOG_" + String.format("%04d", dmd)))) {
						div0.addContent(dive);
					}
				}

				if (div0.getContentSize() == 0) {
					div0.addContent(dive);
				}

                dmdSec.setAttribute("ID", "DMDLOG_" + String.format("%04d", dmd++));
				docMeta.getRootElement().addContent(dmd, dmdSec);

				List<Element> fragments = w.getChildren("Block", hd);
				if(!fragments.isEmpty()) {
					for(Element frag:fragments) {
						List<Element> fragContent = frag.getChildren();
						boolean containFragment = false;
						for(Element fragElement:fragContent) {
							if(fragElement.getAttributeValue("Type").equals("5210") && fragElement.getAttributeValue("Value").equals("Fragment")) {
								containFragment = true;
							}
						}
						if(containFragment) {

							dmdSec = createElements();

							//Element ms_fragment = new Element("metadata", kitodo).setAttribute("name", "TitleDocMain").setText("Fragment");
							//wf_goobii.addContent(ms_fragment);

							Element div = new Element("div", mets).setAttribute("DMDID", "DMDLOG_" + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","Fragment");

							if (dive.getContentSize() > 0) {
								if (!(dive.getChildren().get(dive.getChildren().size() - 1).getAttributeValue("ID").equals("LOG_" + String.format("%04d", dmd)))) {
									dive.addContent(div);
								}
							}

							if (dive.getContentSize() == 0) {
								dive.addContent(div);
							}

                            dmdSec.setAttribute("ID", "DMDLOG_" + String.format("%04d", dmd++));
							docMeta.getRootElement().addContent(dmd, dmdSec);

							for(Element fragElement:fragContent) {
								if (fragElement.getAttributeValue("Type").equals("bezwrk") && fragElement.getAttributeValue("Value").equals("Abschrift")) {
									dmdSec = createElements();


									found_bezwerk = true;
									List<Element> subfields = fragElement.getChildren();
									for (Element c : subfields) {
									    Element goobi = dmdSec.getChild("mdWrap", mets).getChild("xmlData", mets).getChild("mods", mods).getChild("extension", mods).getChild("goobi", kitodo);
										if (c.getAttributeValue("Type").equals("6930") || c.getAttributeValue("Type").equals("6930gi")) {
											Element ms_work_title = new Element("metadata", kitodo).setAttribute("name", "TitleDocMain").setText(c.getAttributeValue("Value"));
											goobi.addContent(ms_work_title);
										}
										if (c.getAttributeValue("Type").equals("6998")) {
											Element ms_work_authority = new Element("metadata", kitodo).setAttribute("name", "ms_work_authority").setText("http://d-nb.info/gnd/" + c.getAttributeValue("Value"));
                                            goobi.addContent(ms_work_authority);
										}
										if (c.getAttributeValue("Type").equals("6922")) {
											Element ms_work_subject = new Element("metadata", kitodo).setAttribute("name", "ms_work_subject").setText(c.getAttributeValue("Value"));
                                            goobi.addContent(ms_work_subject);
										}
									}

									for(Element af:fragContent) {
										if (af.getAttributeValue("Type").equals("bezper") && af.getAttributeValue("Value").equals("Autorschaft")) {
											found_bezpers = true;
											Element ms_work_author = new Element("metadata", kitodo).setAttribute("name", "ms_work_author").setAttribute("type", "person");
											List<Element> subautfields = af.getChildren();
                                            Element goobi = dmdSec.getChild("mdWrap", mets).getChild("xmlData", mets).getChild("mods", mods).getChild("extension", mods).getChild("goobi", kitodo);
											for (Element c : subautfields) {
												if (c.getAttributeValue("Type").equals("4100") || c.getAttributeValue("Type").equals("4100gi")) {
													Element firstName = new Element("firstName", kitodo).setText(c.getAttributeValue("Value"));
													Element displayName = new Element("displayName", kitodo).setText(c.getAttributeValue("Value"));
													ms_work_author.addContent(firstName);
													ms_work_author.addContent(displayName);
												}
												if (c.getAttributeValue("Type").equals("z001")) {
													Element authorityID = new Element("authorityID", kitodo).setText("gnd");
													Element authorityURI = new Element("authorityURI", kitodo).setText("http://d-nb.info/gnd/");
													Element authorityValue = new Element("authorityValue", kitodo).setText("http://d-nb.info/gnd/" + c.getAttributeValue("Value"));
													ms_work_author.addContent(authorityID);
													ms_work_author.addContent(authorityURI);
													ms_work_author.addContent(authorityValue);
												}
											}
											goobi.addContent(ms_work_author);
										}
									}


									Element div1 = new Element("div", mets).setAttribute("DMDID", "DMDLOG_" + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","ContainedWork");

									if (div.getContentSize() > 0) {
										if (!(div.getChildren().get(div.getChildren().size() - 1).getAttributeValue("ID").equals("LOG_" + String.format("%04d", dmd)))) {
											div.addContent(div1);
										}
									}

									if (div.getContentSize() == 0) {
										div.addContent(div1);
									}


									if(found_bezwerk||found_bezpers) {
                                        dmdSec.setAttribute("ID", "DMDLOG_" + String.format("%04d", dmd++));
										docMeta.getRootElement().addContent(dmd, dmdSec);
									}
								}
							}
						}
					}
				}

			} else {
				for(Element f:fields) {

					if (f.getAttribute("Type") != null && f.getAttribute("Value") != null) {
						if (f.getAttributeValue("Type").equals("bezwrk") && f.getAttributeValue("Value").equals("Abschrift")) {
							Element dmdSec = createElements();


							found_bezwerk = true;
							List<Element> subfields = f.getChildren();
							for (Element c : subfields) {
                                Element goobi = dmdSec.getChild("mdWrap", mets).getChild("xmlData", mets).getChild("mods", mods).getChild("extension", mods).getChild("goobi", kitodo);
								if (c.getAttributeValue("Type").equals("6930") || c.getAttributeValue("Type").equals("6930gi")) {
									Element ms_work_title = new Element("metadata", kitodo).setAttribute("name", "TitleDocMain").setText(c.getAttributeValue("Value"));
									goobi.addContent(ms_work_title);
								}
								if (c.getAttributeValue("Type").equals("6998")) {
									Element ms_work_authority = new Element("metadata", kitodo).setAttribute("name", "ms_work_authority").setText("http://d-nb.info/gnd/" + c.getAttributeValue("Value"));
									goobi.addContent(ms_work_authority);
								}
								if (c.getAttributeValue("Type").equals("6922")) {
									Element ms_work_subject = new Element("metadata", kitodo).setAttribute("name", "ms_work_subject").setText(c.getAttributeValue("Value"));
									goobi.addContent(ms_work_subject);
								}
							}

							for(Element af:fields) {
								if (af.getAttributeValue("Type").equals("bezper") && af.getAttributeValue("Value").equals("Autorschaft")) {
									found_bezpers = true;
									Element ms_work_author = new Element("metadata", kitodo).setAttribute("name", "ms_work_author").setAttribute("type", "person");
									List<Element> subautfields = af.getChildren();
                                    Element goobi = dmdSec.getChild("mdWrap", mets).getChild("xmlData", mets).getChild("mods", mods).getChild("extension", mods).getChild("goobi", kitodo);
									for (Element c : subautfields) {
										if (c.getAttributeValue("Type").equals("4100") || c.getAttributeValue("Type").equals("4100gi")) {
											Element firstName = new Element("firstName", kitodo).setText(c.getAttributeValue("Value"));
											Element displayName = new Element("displayName", kitodo).setText(c.getAttributeValue("Value"));
											ms_work_author.addContent(firstName);
											ms_work_author.addContent(displayName);
										}
										if (c.getAttributeValue("Type").equals("z001")) {
											Element authorityID = new Element("authorityID", kitodo).setText("gnd");
											Element authorityURI = new Element("authorityURI", kitodo).setText("http://d-nb.info/gnd/");
											Element authorityValue = new Element("authorityValue", kitodo).setText("http://d-nb.info/gnd/" + c.getAttributeValue("Value"));
											ms_work_author.addContent(authorityID);
											ms_work_author.addContent(authorityURI);
											ms_work_author.addContent(authorityValue);
										}
									}
									goobi.addContent(ms_work_author);
								}
							}


							Element div = new Element("div", mets).setAttribute("DMDID", "DMDLOG_" + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","ContainedWork");

							if (div0.getContentSize() > 0) {
								if (!(div0.getChildren().get(div0.getChildren().size() - 1).getAttributeValue("ID").equals("LOG_" + String.format("%04d", dmd)))) {
									div0.addContent(div);
								}
							}

							if (div0.getContentSize() == 0) {
								div0.addContent(div);
							}


							if(found_bezwerk||found_bezpers) {
								dmdSec.setAttribute("ID", "DMDLOG_" + String.format("%04d", dmd++));
								docMeta.getRootElement().addContent(dmd, dmdSec);
							}
						}
					}
				}
			}
		}

		return div0;
	}

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

	private void out(String meta) {
		try {
			outXML.output(docMeta, new FileOutputStream(meta));
			logger.debug("meta.xml successfully created.");
		} catch (IOException e) {
		    e.printStackTrace();
			logger.error("meta.xml could not created successfully.", e.fillInStackTrace());
		}
	}

	private void create(String meta) {
		intern = getElement();
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
		setStructure("werk", dmd);
		setAttribute("quelle", "ms_record_origin");
		setElement();
		//addLogical(div0);
		out(meta);
	}






/*
	public static void main(String[] args) {

		try {
			
			//Pfad zum Einbinden	*****
			String p_mods = prop.getProperty("mods");
			xpe = fact.compile(p_mods, Filters.element(), null, kitodo);
			Element goobi = (Element)xpe.evaluateFirst(docMeta);


			//Ort	*****
			String p_ort = prop.getProperty("ort");
			xpe = fact.compile(p_ort,Filters.attribute(),null, hd);
			String ort = "";
			if (!((Attribute)xpe.evaluateFirst(docHida) == null)) {
				ort = ((Attribute)xpe.evaluateFirst(docHida)).getValue();
				
				//goobi-tag ms_place
				Element ms_place = new Element("metadata", kitodo).setAttribute("name","ms_place").setText(ort);
				
				goobi.addContent(ms_place);
				//new XMLOutputter(Format.getPrettyFormat()).output(ms_place, System.out);				
				//System.out.println();
			}
			
			//Institution	*****
			String p_institution = prop.getProperty("institution");
			xpe = fact.compile(p_institution,Filters.attribute(),null, hd);
			String institution = "";
			if (!((Attribute)xpe.evaluateFirst(docHida) == null)) {
				institution = ((Attribute)xpe.evaluateFirst(docHida)).getValue();
				
				//goobi-tag ms_institution
				Element ms_institution = new Element("metadata", kitodo).setAttribute("name","ms_institution").setText(institution);
				
				goobi.addContent(ms_institution);
			}
			
			//Signatur	*****
			String p_signatur = prop.getProperty("signatur");
			xpe = fact.compile(p_signatur,Filters.attribute(),null, hd);
			String signatur = "";
			if (!((Attribute)xpe.evaluateFirst(docHida) == null)) {
				signatur = ((Attribute)xpe.evaluateFirst(docHida)).getValue();

				//goobi-tag ms_shelfmark
				Element ms_shelfmark = new Element("metadata", kitodo).setAttribute("name","ms_shelfmark").setText(signatur);
				
				goobi.addContent(ms_shelfmark);				
			}
						
			//goobi-tag ms_identifying_shelfmark	*****
			if (!ort.isEmpty() && !institution.isEmpty() && !signatur.isEmpty()) {
				Element ms_identifying_shelfmark = new Element("metadata", kitodo).setAttribute("name","TitleDocMain").setText(ort + ", " + institution + ", " + signatur);
				
				//goobi-tag ms_identifying_shelfmark
				goobi.addContent(ms_identifying_shelfmark);
			}
			
			//goobi-tag ms_identifying_shelfmark_hash	*****
			//Element ms_identifying_shelfmark_hash = new Element("metadata", namespace).setAttribute("name", "ms_identifying_shelfmark_hash").setText(""+System.nanoTime());
			String hash = "/mets:mets/mets:dmdSec[@ID='DMDPHYS_0000']/mets:mdWrap[@MDTYPE='MODS']/mets:xmlData/mods:mods/mods:extension/goobi:goobi/goobi:metadata[@name='pathimagefiles']/text()";
			xpe = fact.compile(hash, Filters.text(), null, mets, mods, kitodo);
			String folderhash = ((Text)xpe.evaluateFirst(docMeta)).getText();
			Element ms_identifying_shelfmark_hash = new Element("metadata", kitodo).setAttribute("name", "ms_identifying_shelfmark_hash").setText(folderhash.substring(folderhash.lastIndexOf("/") + 1, folderhash.lastIndexOf("_")));
			
			//goobi-tag ms_identifying_shelfmark_hash
			goobi.addContent(ms_identifying_shelfmark_hash);
			
			//Objekttitel	*****
			String p_objekttitel = prop.getProperty("objekttitel");
			xpe = fact.compile(p_objekttitel,Filters.attribute(),null, hd);
			if (!((Attribute)xpe.evaluateFirst(docHida) == null)) {
				String objekttitel = ((Attribute)xpe.evaluateFirst(docHida)).getValue();

				//goobi-tag ms_manuscript_title
				Element ms_manuscript_title = new Element("metadata", kitodo).setAttribute("name","ms_manuscript_title").setText(objekttitel);
				
				goobi.addContent(ms_manuscript_title);
			}
			
			//Medium	*****
			String p_medium = prop.getProperty("medium");			
			xpe = fact.compile(p_medium,Filters.attribute(),null, hd);
			if (!((Attribute)xpe.evaluateFirst(docHida) == null)) {
				String medium = ((Attribute)xpe.evaluateFirst(docHida)).getValue();

				if (medium.contains("&")) {
					int i = 0;
					if (medium.indexOf("&", i) == medium.lastIndexOf("&")) {
						String submedium = medium.substring(i, medium.lastIndexOf("&") - 1);

						//goobi-tag ms_medium
						Element ms_medium = new Element("metadata", kitodo).setAttribute("name","ms_medium").setText(submedium);
						
						goobi.addContent(ms_medium);
					} else {				
						while (medium.indexOf("&", i) != medium.lastIndexOf("&")) {
							int j = medium.indexOf("&", i);
							String submedium = medium.substring(i, j - 1);

							//goobi-tag ms_medium
							Element ms_medium = new Element("metadata", kitodo).setAttribute("name","ms_medium").setText(submedium);
							i = j + 2;
							
							goobi.addContent(ms_medium);
						}
					}
					String submedium = medium.substring(medium.lastIndexOf("&") + 2);

					//goobi-tag ms_medium
					Element ms_medium = new Element("metadata", kitodo).setAttribute("name","ms_medium").setText(submedium);
					
					goobi.addContent(ms_medium);
				} else {					
					//goobi-tag ms_medium
					Element ms_medium = new Element("metadata", kitodo).setAttribute("name","ms_medium").setText(medium);
					
					goobi.addContent(ms_medium);
				}
			}
			
			//Formtyp	*****
			String p_formtyp = prop.getProperty("formtyp");			
			xpe = fact.compile(p_formtyp,Filters.attribute(),null, hd);
			if (!((Attribute)xpe.evaluateFirst(docHida) == null)) {
				String formtyp = ((Attribute)xpe.evaluateFirst(docHida)).getValue();

				//goobi-tag ms_type
				Element ms_type = new Element("metadata", kitodo).setAttribute("name","ms_type").setText(formtyp);
				
				goobi.addContent(ms_type);
			}
						
			//Beschreibstoff	*****
			String p_beschreibstoff = prop.getProperty("beschreibstoff");
			xpe = fact.compile(p_beschreibstoff,Filters.attribute(),null, hd);
			if (!((Attribute)xpe.evaluateFirst(docHida) == null)) {
				String beschreibstoff = ((Attribute)xpe.evaluateFirst(docHida)).getValue();

				if (beschreibstoff.contains("&")) {
					int i = 0;
					if (beschreibstoff.indexOf("&", i) == beschreibstoff.lastIndexOf("&")) {
						String subbeschreibstoff = beschreibstoff.substring(i, beschreibstoff.lastIndexOf("&") - 1);

						//goobi-tag ms_material
						Element ms_material = new Element("metadata", kitodo).setAttribute("name","ms_material").setText(subbeschreibstoff);
						
						goobi.addContent(ms_material);
					} else {				
						while (beschreibstoff.indexOf("&", i) != beschreibstoff.lastIndexOf("&")) {
							int j = beschreibstoff.indexOf("&", i);
							String subbeschreibstoff = beschreibstoff.substring(i, j - 1);

							//goobi-tag ms_material
							Element ms_material = new Element("metadata", kitodo).setAttribute("name","ms_material").setText(subbeschreibstoff);
							i = j + 2;
							
							goobi.addContent(ms_material);
						}
					}
					String subbeschreibstoff = beschreibstoff.substring(beschreibstoff.lastIndexOf("&") + 2);

					//goobi-tag ms_material
					Element ms_material = new Element("metadata", kitodo).setAttribute("name","ms_material").setText(subbeschreibstoff);
					
					goobi.addContent(ms_material);
				} else {					
					//goobi-tag ms_material
					Element ms_material = new Element("metadata", kitodo).setAttribute("name","ms_material").setText(beschreibstoff);
					
					goobi.addContent(ms_material);
				}
			}
			
			//Umfang	*****
			String p_umfang = prop.getProperty("umfang");
			xpe = fact.compile(p_umfang,Filters.attribute(),null, hd);
			if (!((Attribute)xpe.evaluateFirst(docHida) == null)) {
				String umfang = ((Attribute)xpe.evaluateFirst(docHida)).getValue();

				//goobi-tag ms_extent
				Element ms_extent = new Element("metadata", kitodo).setAttribute("name","ms_extent").setText(umfang);
				
				goobi.addContent(ms_extent);
			}
			
			//Abmessungen	*****
			String p_abmessungen = prop.getProperty("abmessungen");
			xpe = fact.compile(p_abmessungen,Filters.attribute(),null, hd);
			if (!((Attribute)xpe.evaluateFirst(docHida) == null)) {
				String abmessungen = ((Attribute)xpe.evaluateFirst(docHida)).getValue();

				//goobi-tag ms_dimensions
				Element ms_dimensions = new Element("metadata", kitodo).setAttribute("name","ms_dimensions").setText(abmessungen);
				
				goobi.addContent(ms_dimensions);
			}
			
			//Sprache	*****
			String p_sprache = prop.getProperty("sprache");
			xpe = fact.compile(p_sprache,Filters.attribute(),null, hd);
			if (!((Attribute)xpe.evaluateFirst(docHida) == null)) {
				String sprache = ((Attribute)xpe.evaluateFirst(docHida)).getValue();

				if (sprache.contains("&")) {							
					int i = 0;
					if (sprache.indexOf("&", i) == sprache.lastIndexOf("&")) {
						String subsprache = sprache.substring(i, sprache.lastIndexOf("&") - 1);

						//goobi-tag ms_language
						Element ms_language = new Element("metadata", kitodo).setAttribute("name","ms_language").setText(subsprache);
						
						goobi.addContent(ms_language);
					} else {				
						while (sprache.indexOf("&", i) != sprache.lastIndexOf("&")) {
							int j = sprache.indexOf("&", i);
							String subsprache = sprache.substring(i, j - 1);

							//goobi-tag ms_language
							Element ms_language = new Element("metadata", kitodo).setAttribute("name","ms_language").setText(subsprache);
							i = j + 2;
							
							goobi.addContent(ms_language);
						}
					}
					String subsprache = sprache.substring(sprache.lastIndexOf("&") + 2);

					//goobi-tag ms_language
					Element ms_language = new Element("metadata", kitodo).setAttribute("name","ms_language").setText(subsprache);
					
					goobi.addContent(ms_language);
				} else {
					//goobi-tag ms_language
					Element ms_language = new Element("metadata", kitodo).setAttribute("name","ms_language").setText(sprache);
					
					goobi.addContent(ms_language);
				}
			}
			
			//Lokalisierung	*****
			String p_lokalisierung = prop.getProperty("lokalisierung");
			xpe = fact.compile(p_lokalisierung,Filters.attribute(),null, hd);
			if (!((Attribute)xpe.evaluateFirst(docHida) == null)) {
				String lokalisierung = ((Attribute)xpe.evaluateFirst(docHida)).getValue();

				//goobi-tag ms_place_of_origin
				Element ms_place_of_origin = new Element("metadata", kitodo).setAttribute("name","ms_place_of_origin").setText(lokalisierung);
				
				goobi.addContent(ms_place_of_origin);
			}
			
			//datiert	*****
			String p_datiert = prop.getProperty("datiert");
			xpe = fact.compile(p_datiert,Filters.element(),null, hd);
			
			List<Element> datiertes = xpe.evaluate(docHida);
			for (Element dat:datiertes) {
				List<Element> childs = dat.getChildren();
				for (Element c:childs) {
					if (c.getAttributeValue("Type").equals("5064")) {
						Element ms_dated = new Element("metadata", kitodo).setAttribute("name", "ms_dated").setText(c.getAttributeValue("Value"));
						
						goobi.addContent(ms_dated);
					}					
				}				
			}			
			
			//Datierung	*****
			String p_datierung = prop.getProperty("datierung");			
			xpe = fact.compile(p_datierung,Filters.element(),null, hd);
			
			List<Element> datierungen = xpe.evaluate(docHida);
			for (Element dau:datierungen) {
				List<Element> childs = dau.getChildren();
				for (Element c:childs) {
					if (c.getAttributeValue("Type").equals("5064")) {
						String datierung = c.getAttributeValue("Value");
						
						if(konprop.containsKey(datierung))
						{
							//goobi-tag ms_dating_verbal
							Element ms_dating_verbal = new Element("metadata", kitodo).setAttribute("name", "ms_dating_verbal").setText(konprop.getProperty(datierung));
							
							goobi.addContent(ms_dating_verbal);							
						}
						
						if (datierung.contains("/")) {						
							//goobi-tag ms_dating_not_before
							Element ms_dating_not_before = new Element("metadata", kitodo).setAttribute("name","ms_dating_not_before").setText(datierung.substring(0, datierung.indexOf("/")));
						
							//goobi-tag ms_dating_not_after
							Element ms_dating_not_after = new Element("metadata", kitodo).setAttribute("name","ms_dating_not_after").setText(datierung.substring(datierung.indexOf("/") + 1));
						
							goobi.addContent(ms_dating_not_before);
							goobi.addContent(ms_dating_not_after);
						} else {
							//goobi-tag ms_dated
							Element ms_dated = new Element("metadata", kitodo).setAttribute("name", "ms_dated").setText(datierung);
							
							goobi.addContent(ms_dated);
						}
					}					
				}				
			}
			
			
			//Autor	*****
			String p_autor = prop.getProperty("autor");
			xpe = fact.compile(p_autor,Filters.element(),null, hd);
			
			List<Element> autors = xpe.evaluate(docHida);
			for(Element a:autors)
			{
				Element ms_author = new Element("metadata", kitodo).setAttribute("name","ms_author").setAttribute("type","person");
				
				List<Element> childs = a.getChildren();
				for (Element c:childs) {
					if (c.getAttributeValue("Type").equals("4100") || c.getAttributeValue("Type").equals("4100gi")) {
						Element firstName = new Element("firstName", kitodo).setText(c.getAttributeValue("Value"));
						Element displayName = new Element("displayName", kitodo).setText(c.getAttributeValue("Value"));
						ms_author.addContent(firstName);
						ms_author.addContent(displayName);
					}
					if (c.getAttributeValue("Type").equals("z001")) {
						Element authorityID = new Element("authorityID", kitodo).setText("gnd");
						Element authorityURI = new Element("authorityURI", kitodo).setText("http://d-nb.info/gnd/");
						Element authorityValue = new Element("authorityValue", kitodo).setText("http://d-nb.info/gnd/" + c.getAttributeValue("Value"));
						ms_author.addContent(authorityID);
						ms_author.addContent(authorityURI);
						ms_author.addContent(authorityValue);
					}
				}
				
				goobi.addContent(ms_author);
			}
			
			//Personen (Vorbesitz - PERSONEN)	*****
			String p_personen = prop.getProperty("personen");
			xpe = fact.compile(p_personen,Filters.element(),null, hd);
			
			List<Element> personens = xpe.evaluate(docHida);
			for (Element p:personens) {
				Element ms_previous_owner_person = new Element("metadata", kitodo).setAttribute("name","ms_previous_owner_person").setAttribute("type","person");
				
				List<Element> childs = p.getChildren();
				for(Element c:childs) {
					if(c.getAttributeValue("Type").equals("4100") || c.getAttributeValue("Type").equals("4100gi"))
					{
						Element firstName = new Element("firstName", kitodo).setText(c.getAttributeValue("Value"));
						Element displayName = new Element("displayName", kitodo).setText(c.getAttributeValue("Value"));
						ms_previous_owner_person.addContent(firstName);
						ms_previous_owner_person.addContent(displayName);
					}
					if(c.getAttributeValue("Type").equals("z001") || c.getAttributeValue("Type").equals("y001"))
					{
						Element authorityID = new Element("authorityID", kitodo).setText("gnd");
						Element authorityURI = new Element("authorityURI", kitodo).setText("http://d-nb.info/gnd/");
						Element authorityValue = new Element("authorityValue", kitodo).setText("http://d-nb.info/gnd/" + c.getAttributeValue("Value"));
						ms_previous_owner_person.addContent(authorityID);
						ms_previous_owner_person.addContent(authorityURI);
						ms_previous_owner_person.addContent(authorityValue);
					}					
				}
				
				goobi.addContent(ms_previous_owner_person);
			}
			
			//Sozietaet (Vorbesitz - INSTITUTION)	*****
			String p_sozietaet = prop.getProperty("sozietaet");
			xpe = fact.compile(p_sozietaet,Filters.element(),null, hd);
			
			List<Element> sozietaets = xpe.evaluate(docHida);
			for(Element s:sozietaets)
			{
				Element ms_previous_owner_institution = new Element("metadata", kitodo).setAttribute("name","ms_previous_owner_institution").setAttribute("type","person");
				
				List<Element> childs = s.getChildren();
				for(Element c:childs)
				{
					if(c.getAttributeValue("Type").equals("4600"))
					{
						Element firstName = new Element("firstName", kitodo).setText(c.getAttributeValue("Value"));
						Element displayName = new Element("displayName", kitodo).setText(c.getAttributeValue("Value"));
						ms_previous_owner_institution.addContent(firstName);
						ms_previous_owner_institution.addContent(displayName);
					}
					if(c.getAttributeValue("Type").equals("4998"))
					{
						Element authorityID = new Element("authorityID", kitodo).setText("gnd");
						Element authorityURI = new Element("authorityURI", kitodo).setText("http://d-nb.info/gnd/");
						Element authorityValue = new Element("authorityValue", kitodo).setText("http://d-nb.info/gnd/" + c.getAttributeValue("Value"));
						ms_previous_owner_institution.addContent(authorityID);
						ms_previous_owner_institution.addContent(authorityURI);
						ms_previous_owner_institution.addContent(authorityValue);
					}
				}
				
				goobi.addContent(ms_previous_owner_institution);
			}
			
			//dmdSec 0001 ...
			int dmd = 1;
			
			Element dmdSec = new Element("dmdSec", mets);
			Element mdWrap = new Element("mdWrap", mets);
			mdWrap.setAttribute("MDTYPE", "MODS");
			Element xmlData = new Element("xmlData", mets);
			Element mod = new Element("mods", mods);
			Element extension = new Element("extension", mods);
			Element goobii = new Element("goobi", kitodo);
			
			//Verwalter	*****
			String p_verwalter = prop.getProperty("verwalter");
			xpe = fact.compile(p_verwalter,Filters.element(),null, hd);
			
			List<Element> verwalters = xpe.evaluate(docHida);
			
			Element ms_host_volume_institution = null;
			Element ms_host_volume_shelfmark = null;
			String verwalter = "";
			String bez_signatur = "";
			
			for (Element v:verwalters) {				
				if (v.getAttributeValue("Type").equals("501k")) {
					//goobi-tag ms_host_volume_institution
					ms_host_volume_institution = new Element("metadata", kitodo).setAttribute("name","ms_host_volume_institution").setText(v.getAttributeValue("Value"));
					verwalter = v.getAttributeValue("Value");
				}
				if (v.getAttributeValue("Type").equals("501m")) {
					//goobi-tag ms_host_volume_shelfmark
					ms_host_volume_shelfmark = new Element("metadata", kitodo).setAttribute("name","ms_host_volume_shelfmark").setText(v.getAttributeValue("Value"));
					bez_signatur = v.getAttributeValue("Value");
				}
				
				if (ms_host_volume_institution != null) {
					goobii.addContent(ms_host_volume_institution);
				}
				
				if (ms_host_volume_shelfmark != null) {
					goobii.addContent(ms_host_volume_shelfmark);
				}
				
				if (!verwalter.isEmpty() && !bez_signatur.isEmpty()) {
					Element ms_host_identifying_shelfmark = new Element("metadata", kitodo).setAttribute("name","ms_host_identifying_shelfmark").setText(verwalter + ", " + bez_signatur);
					
					//goobi-tag ms_host_identifying_shelfmark
					goobii.addContent(ms_host_identifying_shelfmark);
				}
			}
			
			if ((ms_host_volume_institution != null) || (ms_host_volume_shelfmark != null)) {				
				dmdSec.setAttribute("ID", "DMDLOG_" + String.format("%04d", dmd));
				docMeta.getRootElement().addContent(dmd + 1, dmdSec);
				dmdSec.addContent(mdWrap);
				mdWrap.addContent(xmlData);
				xmlData.addContent(mod);
				mod.addContent(extension);				
				extension.addContent(goobii);
				dmd += 1;
			}

			
			//Werk	*****
			String p_werk = prop.getProperty("werk");
			xpe = fact.compile(p_werk, Filters.element(), null, hd);
			
			//structMap TYPE="LOGICAL" [Start]
			Element div0 = new Element("div", mets).setAttribute("DMDID","DMDLOG_0000").setAttribute("ID","LOG_0000").setAttribute("TYPE","Manuscript");
			//structMap TYPE="LOGICAL" [Ende]
			
			
			List<Element> werke = xpe.evaluate(docHida);
			for(Element w:werke) {
				
				boolean found_bezwerk = false, found_bezpers = false, binding = false;
				
				List<Element> fields = w.getChildren();
				for(Element f:fields)
				{
					if(f.getAttributeValue("Type").equals("5230") && f.getAttributeValue("Value").equals("Einband")) 
					{
						binding = true;
					}
				}
					
				if (binding)
				{
					Element we_dmdSec = new Element("dmdSec", mets);
					Element we_mdWrap = new Element("mdWrap", mets);
					we_mdWrap.setAttribute("MDTYPE", "MODS");
					Element we_xmlData = new Element("xmlData", mets);
					Element we_mod = new Element("mods", mods);
					Element we_extension = new Element("extension", mods);
					Element we_goobii = new Element("goobi", kitodo);
					
					we_extension.addContent(we_goobii);
					we_mod.addContent(we_extension);
					we_xmlData.addContent(we_mod);
					we_mdWrap.addContent(we_xmlData);
					we_dmdSec.addContent(we_mdWrap);
					
					Element ms_binding = new Element("metadata", kitodo).setAttribute("name", "TitleDocMain").setText("Einband");
					//we_goobii.addContent(ms_binding);
					
					Element dive = new Element("div", mets).setAttribute("DMDID", "DMDLOG_" + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","slub_Binding");
					
					if (div0.getContentSize() > 0)							
					{
						if (!(div0.getChildren().get(div0.getChildren().size() - 1).getAttributeValue("ID").equals("LOG_" + String.format("%04d", dmd))))
						{
							div0.addContent(dive);
						}
					} 
					
					if (div0.getContentSize() == 0)
					{
						div0.addContent(dive);
					}
					
					we_dmdSec.setAttribute("ID", "DMDLOG_" + String.format("%04d", dmd++));					
					docMeta.getRootElement().addContent(dmd, we_dmdSec);
					
					
					List<Element> fragments = w.getChildren("Block", hd);
					if(!fragments.isEmpty()) {						
						for(Element frag:fragments) {
							List<Element> fragContent = frag.getChildren();
							boolean containFragment = false;
							for(Element fragElement:fragContent) {
								if(fragElement.getAttributeValue("Type").equals("5210") && fragElement.getAttributeValue("Value").equals("Fragment")) 
								{
									containFragment = true;
								}
							}
							if(containFragment) {
								
								Element wf_dmdSec = new Element("dmdSec", mets);
								Element wf_mdWrap = new Element("mdWrap", mets);
								wf_mdWrap.setAttribute("MDTYPE", "MODS");
								Element wf_xmlData = new Element("xmlData", mets);
								Element wf_mod = new Element("mods", mods);
								Element wf_extension = new Element("extension", mods);
								Element wf_goobii = new Element("goobi", kitodo);
								
								wf_extension.addContent(wf_goobii);
								wf_mod.addContent(wf_extension);
								wf_xmlData.addContent(wf_mod);
								wf_mdWrap.addContent(wf_xmlData);
								wf_dmdSec.addContent(wf_mdWrap);
								
								Element ms_fragment = new Element("metadata", kitodo).setAttribute("name", "TitleDocMain").setText("Fragment");
								//wf_goobii.addContent(ms_fragment);
								
								Element div = new Element("div", mets).setAttribute("DMDID", "DMDLOG_" + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","Fragment");
								
								if (dive.getContentSize() > 0)							
								{
									if (!(dive.getChildren().get(dive.getChildren().size() - 1).getAttributeValue("ID").equals("LOG_" + String.format("%04d", dmd))))
									{
										dive.addContent(div);
									}
								} 
								
								if (dive.getContentSize() == 0)
								{
									dive.addContent(div);
								}
								
								wf_dmdSec.setAttribute("ID", "DMDLOG_" + String.format("%04d", dmd++));					
								docMeta.getRootElement().addContent(dmd, wf_dmdSec);
								
								for(Element fragElement:fragContent) 
								{
									if (fragElement.getAttributeValue("Type").equals("bezwrk") && fragElement.getAttributeValue("Value").equals("Abschrift"))
									{
										Element w_dmdSec = new Element("dmdSec", mets);
										Element w_mdWrap = new Element("mdWrap", mets);
										w_mdWrap.setAttribute("MDTYPE", "MODS");
										Element w_xmlData = new Element("xmlData", mets);
										Element w_mod = new Element("mods", mods);
										Element w_extension = new Element("extension", mods);
										Element w_goobii = new Element("goobi", kitodo);
										
										w_extension.addContent(w_goobii);
										w_mod.addContent(w_extension);
										w_xmlData.addContent(w_mod);
										w_mdWrap.addContent(w_xmlData);
										w_dmdSec.addContent(w_mdWrap);
										
										
										found_bezwerk = true;
										List<Element> subfields = fragElement.getChildren();
										for (Element c : subfields) {
											if (c.getAttributeValue("Type").equals("6930") || c.getAttributeValue("Type").equals("6930gi")) {
												Element ms_work_title = new Element("metadata", kitodo).setAttribute("name", "TitleDocMain").setText(c.getAttributeValue("Value"));
												w_goobii.addContent(ms_work_title);
											}
											if (c.getAttributeValue("Type").equals("6998")) {
												Element ms_work_authority = new Element("metadata", kitodo).setAttribute("name", "ms_work_authority").setText("http://d-nb.info/gnd/" + c.getAttributeValue("Value"));
												w_goobii.addContent(ms_work_authority);
											}
											if (c.getAttributeValue("Type").equals("6922")) {
												Element ms_work_subject = new Element("metadata", kitodo).setAttribute("name", "ms_work_subject").setText(c.getAttributeValue("Value"));
												w_goobii.addContent(ms_work_subject);
											}
										}
										
										for(Element af:fragContent) {
											if (af.getAttributeValue("Type").equals("bezper") && af.getAttributeValue("Value").equals("Autorschaft"))
											{
												found_bezpers = true;
												Element ms_work_author = new Element("metadata", kitodo).setAttribute("name", "ms_work_author").setAttribute("type", "person");
												List<Element> subautfields = af.getChildren();
												for (Element c : subautfields) {
													if (c.getAttributeValue("Type").equals("4100") || c.getAttributeValue("Type").equals("4100gi")) {
														Element firstName = new Element("firstName", kitodo).setText(c.getAttributeValue("Value"));
														Element displayName = new Element("displayName", kitodo).setText(c.getAttributeValue("Value"));
														ms_work_author.addContent(firstName);
														ms_work_author.addContent(displayName);								
													}
													if (c.getAttributeValue("Type").equals("z001")) {
														Element authorityID = new Element("authorityID", kitodo).setText("gnd");
														Element authorityURI = new Element("authorityURI", kitodo).setText("http://d-nb.info/gnd/");
														Element authorityValue = new Element("authorityValue", kitodo).setText("http://d-nb.info/gnd/" + c.getAttributeValue("Value"));
														ms_work_author.addContent(authorityID);
														ms_work_author.addContent(authorityURI);
														ms_work_author.addContent(authorityValue);
													}
												}						
												w_goobii.addContent(ms_work_author);
											}
										}
										
										
										Element div1 = new Element("div", mets).setAttribute("DMDID", "DMDLOG_" + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","ContainedWork");
																				
										if (div.getContentSize() > 0)							
										{
											if (!(div.getChildren().get(div.getChildren().size() - 1).getAttributeValue("ID").equals("LOG_" + String.format("%04d", dmd))))
											{
												div.addContent(div1);
											}
										} 
										
										if (div.getContentSize() == 0)
										{
											div.addContent(div1);
										}
										
										
										if(found_bezwerk||found_bezpers)				
										{
											w_dmdSec.setAttribute("ID", "DMDLOG_" + String.format("%04d", dmd++));					
											docMeta.getRootElement().addContent(dmd, w_dmdSec);
										}
									}
								}								
							}
						}
					}
					
				} else 
				{
					for(Element f:fields) {
						
						if (f.getAttribute("Type") != null && f.getAttribute("Value") != null) {
							if (f.getAttributeValue("Type").equals("bezwrk") && f.getAttributeValue("Value").equals("Abschrift"))
							{
								Element w_dmdSec = new Element("dmdSec", mets);
								Element w_mdWrap = new Element("mdWrap", mets);
								w_mdWrap.setAttribute("MDTYPE", "MODS");
								Element w_xmlData = new Element("xmlData", mets);
								Element w_mod = new Element("mods", mods);
								Element w_extension = new Element("extension", mods);
								Element w_goobii = new Element("goobi", kitodo);
								
								w_extension.addContent(w_goobii);
								w_mod.addContent(w_extension);
								w_xmlData.addContent(w_mod);
								w_mdWrap.addContent(w_xmlData);
								w_dmdSec.addContent(w_mdWrap);
								
								
								found_bezwerk = true;
								List<Element> subfields = f.getChildren();
								for (Element c : subfields) {
									if (c.getAttributeValue("Type").equals("6930") || c.getAttributeValue("Type").equals("6930gi")) {
										Element ms_work_title = new Element("metadata", kitodo).setAttribute("name", "TitleDocMain").setText(c.getAttributeValue("Value"));
										w_goobii.addContent(ms_work_title);
									}
									if (c.getAttributeValue("Type").equals("6998")) {
										Element ms_work_authority = new Element("metadata", kitodo).setAttribute("name", "ms_work_authority").setText("http://d-nb.info/gnd/" + c.getAttributeValue("Value"));
										w_goobii.addContent(ms_work_authority);
									}
									if (c.getAttributeValue("Type").equals("6922")) {
										Element ms_work_subject = new Element("metadata", kitodo).setAttribute("name", "ms_work_subject").setText(c.getAttributeValue("Value"));
										w_goobii.addContent(ms_work_subject);
									}
								}
								
								for(Element af:fields) {
									if (af.getAttributeValue("Type").equals("bezper") && af.getAttributeValue("Value").equals("Autorschaft"))
									{
										found_bezpers = true;
										Element ms_work_author = new Element("metadata", kitodo).setAttribute("name", "ms_work_author").setAttribute("type", "person");
										List<Element> subautfields = af.getChildren();
										for (Element c : subautfields) {
											if (c.getAttributeValue("Type").equals("4100") || c.getAttributeValue("Type").equals("4100gi")) {
												Element firstName = new Element("firstName", kitodo).setText(c.getAttributeValue("Value"));
												Element displayName = new Element("displayName", kitodo).setText(c.getAttributeValue("Value"));
												ms_work_author.addContent(firstName);
												ms_work_author.addContent(displayName);								
											}
											if (c.getAttributeValue("Type").equals("z001")) {
												Element authorityID = new Element("authorityID", kitodo).setText("gnd");
												Element authorityURI = new Element("authorityURI", kitodo).setText("http://d-nb.info/gnd/");
												Element authorityValue = new Element("authorityValue", kitodo).setText("http://d-nb.info/gnd/" + c.getAttributeValue("Value"));
												ms_work_author.addContent(authorityID);
												ms_work_author.addContent(authorityURI);
												ms_work_author.addContent(authorityValue);
											}
										}						
										w_goobii.addContent(ms_work_author);
									}
								}
								
								
								Element div = new Element("div", mets).setAttribute("DMDID", "DMDLOG_" + String.format("%04d", dmd)).setAttribute("ID", "LOG_" + String.format("%04d", dmd)).setAttribute("TYPE","ContainedWork");
														
								if (div0.getContentSize() > 0)							
								{
									if (!(div0.getChildren().get(div0.getChildren().size() - 1).getAttributeValue("ID").equals("LOG_" + String.format("%04d", dmd))))
									{
										div0.addContent(div);
									}
								} 
								
								if (div0.getContentSize() == 0)
								{
									div0.addContent(div);
								}
								
								
								if(found_bezwerk||found_bezpers)				
								{
									w_dmdSec.setAttribute("ID", "DMDLOG_" + String.format("%04d", dmd++));					
									docMeta.getRootElement().addContent(dmd, w_dmdSec);
								}
							}
						}
					}	
				}
			}
			
			//Quelle	*****
			String p_quelle = prop.getProperty("quelle");
			xpe = fact.compile(p_quelle,Filters.attribute(),null, hd);
			if (!((Attribute)xpe.evaluateFirst(docHida) == null)) {
				String quelle = ((Attribute)xpe.evaluateFirst(docHida)).getValue();

				if (quelle.contains("&")) {							
					int i = 0;
					if (quelle.indexOf("&", i) == quelle.lastIndexOf("&")) {
						String subquelle = quelle.substring(i, quelle.lastIndexOf("&") - 1);

						//goobi-tag ms_record_origin
						Element ms_record_origin = new Element("metadata", kitodo).setAttribute("name","ms_record_origin").setText(subquelle);
						
						goobi.addContent(ms_record_origin);
					} else {				
						while (quelle.indexOf("&", i) != quelle.lastIndexOf("&")) {
							int j = quelle.indexOf("&", i);
							String subquelle = quelle.substring(i, j - 1);

							//goobi-tag ms_record_origin
							Element ms_record_origin = new Element("metadata", kitodo).setAttribute("name","ms_record_origin").setText(subquelle);
							i = j + 2;
							
							goobi.addContent(ms_record_origin);
						}
					}
					String subquelle = quelle.substring(quelle.lastIndexOf("&") + 2);

					//goobi-tag ms_record_origin
					Element ms_record_origin = new Element("metadata", kitodo).setAttribute("name","ms_record_origin").setText(subquelle);
					
					goobi.addContent(ms_record_origin);
				} else {
					//goobi-tag ms_record_origin
					Element ms_record_origin = new Element("metadata", kitodo).setAttribute("name","ms_record_origin").setText(quelle);
					
					goobi.addContent(ms_record_origin);
				}
			}
			
			//Erschließung	*****
			String p_erschliessung = prop.getProperty("erschliessung");
			xpe = fact.compile(p_erschliessung,Filters.attribute(),null, hd);
			if (!((Attribute)xpe.evaluateFirst(docHida) == null)) {
				String erschliessung = ((Attribute)xpe.evaluateFirst(docHida)).getValue();

				//goobi-tag ms_record_id
				Element ms_record_id = new Element("metadata", kitodo).setAttribute("name","ms_record_id").setText("http://www.manuscripta-mediaevalia.de/dokumente/html/obj" + erschliessung);
				
				goobi.addContent(ms_record_id);
			}
			
			//Add structMap="LOGICAL"
			

			for(Element sM: docMeta.getRootElement().getChildren("structMap", mets))
			{
				if(sM.getAttributeValue("TYPE").equals("LOGICAL"))
				{
					sM.removeChildren("div", mets);
					sM.addContent(div0);
				}
			}
			
			//Ausgabe
			outXML.output(docMeta, new FileOutputStream(meta));
			//outxml.output(docmeta, new FileOutputStream("meta.xml"));
			System.out.println("Handschriftliche meta.xml erfolgreich erstellt.");
			
		} catch (IOException | JDOMException e){
			e.printStackTrace();
		}

	}

 */

}



