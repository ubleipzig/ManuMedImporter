package org.ub.manumed;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Properties;

public class Importer {

    private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    static Document docMeta;
    static Document docHida;
    static XMLOutputter outXML = new XMLOutputter(Format.getPrettyFormat());

    static Properties mapping = new Properties();

    static Namespace kitodo = Namespace.getNamespace("goobi","http://meta.goobi.org/v1.5.1/");
    static Namespace mets = Namespace.getNamespace("mets","http://www.loc.gov/METS/");
    static Namespace mods = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
    private static Namespace hd = Namespace.getNamespace("h1","http://www.startext.de/HiDA/DefService/XMLSchema");
    static Namespace xlink = Namespace.getNamespace("xlink","http://www.w3.org/1999/xlink");
    static ArrayList<Namespace> spaceList = new ArrayList<>();

    static XPathFactory fact = XPathFactory.instance();
    static XPathExpression xpe;

    /**
     * load Mets/Mods files and add namespaces
     *
     * @param meta String
     * @param hida String
     */
    private static void build(String meta, String hida) {
        try {
            docMeta = new SAXBuilder().build(new File(meta));
            docHida = new SAXBuilder().build(new File(hida));

            spaceList.add(kitodo);
            spaceList.add(mets);
            spaceList.add(mods);
            spaceList.add(hd);
            spaceList.add(xlink);
        } catch (JDOMException | IOException e) {
            logger.error("Could not get data from xml files.", e.fillInStackTrace());
        }
    }

    /**
     * load properties file
     *
     * @param map String
     */
    private static void loadProperties(String map) {
        try {
            mapping.load(new FileInputStream(map));
        } catch (IOException e) {
            logger.error("Could not get data from mapping file.", e.fillInStackTrace());
        }
    }

    public static void main(String[] args) {
        Options opts = new Options();

        Option metaFile = new Option("m", "meta", true, "path to the meta.xml file");
        metaFile.setRequired(true);
        opts.addOption(metaFile);

        Option imagePath = new Option("i", "images", true, "path to the images");
        imagePath.setRequired(true);
        opts.addOption(imagePath);

        Option hidaFile = new Option("h", "hida", true, "path to the hida xml file");
        hidaFile.setRequired(true);
        opts.addOption(hidaFile);

        Option mapFile = new Option("c", "conf-map", true, "path to the configuration mapping file");
        mapFile.setRequired(true);
        opts.addOption(mapFile);

        Option renameFile = new Option("r", "renaming", true, "path to the renaming file");
        renameFile.setRequired(false);
        opts.addOption(renameFile);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(opts, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", opts);

            System.exit(1);
            return;
        }

        String meta = cmd.getOptionValue("m");
        String images = cmd.getOptionValue("i");
        String hida = cmd.getOptionValue("h");
        String map = cmd.getOptionValue("c");
        build(meta, hida);
        loadProperties(map);
        if (cmd.hasOption("r")) {
            String rename = cmd.getOptionValue("r");
            new Mets(new File(rename), images, meta);
        } else {
            new Mets(images, meta);
        }
        new Mods(meta);
    }
}
