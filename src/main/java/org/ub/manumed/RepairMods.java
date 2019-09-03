package org.ub.manumed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

public class RepairMods {
	
	private static Document doc;
	private static XPathFactory xFactory = XPathFactory.instance();
	private static Namespace mets = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
	private static Namespace mods = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
	private static List<Namespace> lns = new ArrayList<>();
	private static Properties p = new Properties();
	private static XMLOutputter outxml = new XMLOutputter(Format.getPrettyFormat());
	
	/**
	 * initialize JSOM xml reader and konkordanz list
	 * 
	 * @param xml String
	 * @param kon String
	 */
	private static void init(String xml, String kon) {		
		try {
			SAXBuilder builder = new SAXBuilder();
			doc = builder.build(new File(xml));
			lns.add(mets);
			lns.add(mods);
			p.load(new FileInputStream(kon));
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void modifyVersion() {
		Element root = doc.getRootElement();
		Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		Attribute sl = root.getAttribute("schemaLocation", xsi);
		int v = sl.getValue().indexOf("http://www.loc.gov/standards/mods/v3/mods-3");
		String version = sl.getValue().substring(v, v + "http://www.loc.gov/standards/mods/v3/mods-3-7.xsd".length()); 
		sl.setValue(sl.getValue().replace(version, "http://www.loc.gov/standards/mods/v3/mods-3-7.xsd"));
	}
	
	/**
	 * change the text of the field <mods:typeOfResource> from 'Handschrift' to 'text'
	 */
	private static void modifyManuscript() {
		String tag = "//mods:typeOfResource[@manuscript='yes']";
		XPathExpression<Element> expr = xFactory.compile(tag, Filters.element(), null, lns);
		Element e = expr.evaluateFirst(doc);
		if(e != null && !e.getTextTrim().equals("text"))
			e.setText("text");
	}
	
	/**
	 * looking for attribute 'unit' from the field <mods:extent> and add it
	 */	
	private static void modifyExtent() {
		String tag = "//mods:physicalDescription/mods:extent";
		XPathExpression<Element> expr = xFactory.compile(tag, Filters.element(), null, lns);
		List<Element> le = expr.evaluate(doc);
		for(Element e:le) {
			if(!e.hasAttributes()) {
				if(e.getTextTrim().contains("cm"))
					e.setAttribute("unit", "cm");
				else
					e.setAttribute("unit", "leaves");
			}
		}
	}
	
	/**
	 * change 't' from subtitle to upper case
	 */
	private static void subtitleUp() {		
		String tag = "//mods:titleInfo/mods:subtitle";
		XPathExpression<Element> expr = xFactory.compile(tag, Filters.element(), null, lns);
		List<Element> le = expr.evaluate(doc);
		if(!le.isEmpty()) {
			for(Element e:le) {			
				e.setName("subTitle");
			}
		}
	}
	
	/**
	 * add missing parent element <mods:titleInfo> to <mods:title>
	 */
	private static void setTitleInfo() {
		String tag = "//mods:title";
		XPathExpression<Element> expr = xFactory.compile(tag, Filters.element(), null, lns);
		List<Element> le = expr.evaluate(doc);
		for(Element e:le) {
			Element p = e.getParentElement();
			if(!p.getName().equals("titleInfo")) {
				e.detach();
				Element ti = new Element("titleInfo", mods);
				ti.addContent(e);
				p.addContent(ti);
			}
		}
	}
	
	/**
	 * modify <mods:originInfo> and <mods:place> to adapt to the schema
	 */
	private static void modifyPlace() {
		String tag = "//mods:originInfo";
		XPathExpression<Element> expr = xFactory.compile(tag, Filters.element(), null, lns);
		Element e = expr.evaluateFirst(doc);
		if(e != null && e.getChild("place", mods) != null) {
			Element place = e.getChild("place", mods);
			if(place.hasAttributes() && place.getAttribute("eventType").getName().equals("eventType")) {
				e.setAttribute(place.getAttribute("eventType").getName(), place.getAttributeValue("eventType"));
				place.removeAttribute("eventType");
				if(place.getChildren().isEmpty()) {
					String placeContent = place.getTextTrim();
					place.setText(null);
					Element placeTerm = new Element("placeTerm", mods).setAttribute("type", "text").setText(placeContent);
					place.addContent(placeTerm);
				}
			}
		}
	}	
	
	/**
	 * add attribute 'type=text' to languageTerm and set language code
	 */
	private static void setLanguage() {
		String tag = "//mods:language";
		XPathExpression<Element> expr = xFactory.compile(tag, Filters.element(), null, lns);
		Element e = expr.evaluateFirst(doc);
		String language;
		ArrayList<String> ll = new ArrayList<>();
		if(e.getChildren().isEmpty()) {
			if(!e.getTextTrim().isEmpty()) {
				language = e.getTextTrim();
				e.setText(null);
				Element lt = new Element("languageTerm", mods).setAttribute("type", "text").setText(language);
				e.addContent(lt);
				ll.addAll(getISOcode(language.toLowerCase()));
				for(String iso:ll) {
					Element code = new Element("languageTerm", mods).setAttribute("type", "code").setAttribute("authority", "iso639-2b").setText(iso);
					e.addContent(code);
				}
			}
		} else {
			for(Element c:e.getChildren()) {
				if(!c.hasAttributes()) {
					c.setAttribute("type", "text");
				}
				ll.addAll(getISOcode(c.getTextTrim().toLowerCase()));			
			}			
			for(String iso:ll) {
				if(checkLanguage(e.getChildren(), iso)) {
					Element code = new Element("languageTerm", mods).setAttribute("type", "code").setAttribute("authority", "iso639-2b").setText(iso);				
					e.addContent(code);
				}
			}			
		}
	}
	
	/**
	 * test if element exists already
	 * 
	 * @param le List<Element>
	 * @param iso String
	 * @return boolean
	 */
	private static boolean checkLanguage(List<Element> le, String iso) {
		for(Element e:le) {
			if(e.getTextTrim().equals(iso)) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * test against konkordanz and get ISO codes
	 * 
	 * @param lang String
	 * @return ArrayList<String>
	 */
	private static ArrayList<String> getISOcode(String lang) {
		ArrayList<String> list  = new ArrayList<>();
		if(lang.contains("/") || lang.contains("und") || lang.contains(" ")) {
			String[] l = lang.split("\\s");
			for(String tag:l) {
				if(!tag.equals("/") && !tag.equals("und")) {
					list.add(p.getProperty(tag));
				}
			}
		} else {
			if(p.getProperty(lang) != null)
				list.add(p.getProperty(lang));
		}
		
		return list;
	}

	public static void main(String[] args) {
		
		Options options = new Options();
		
		Option path = new Option("p", "path", true, "path to the xml file");
		path.setRequired(true);
		options.addOption(path);
		
		Option konkordanz = new Option("k", "konkordanz", true, "path to the konkordanz list");
		konkordanz.setRequired(true);
		options.addOption(konkordanz);
		
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("utility-name", options);
			
			System.exit(1);
			return;
		}
		
		String xml = cmd.getOptionValue("path");
		String kon = cmd.getOptionValue("konkordanz");
		init(xml, kon);
		modifyVersion();
		modifyManuscript();
		modifyExtent();
		subtitleUp();
		setTitleInfo();
		modifyPlace();
		setLanguage();
		
		try {
			outxml.output(doc, new FileOutputStream(xml));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
