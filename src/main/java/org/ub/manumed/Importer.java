package org.ub.manumed;

import org.apache.commons.cli.*;

import java.io.File;

public class Importer {

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

        Option mapFile = new Option("m", "map", true, "path to the mapping file");
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

        String meta = cmd.getOptionValue("metaFile");
        String images = cmd.getOptionValue("imagePath");
        String hida = cmd.getOptionValue("hidaFile");
        String map = cmd.getOptionValue("mapFile");
        if (cmd.hasOption("renameFile")) {
            String rename = cmd.getOptionValue("renameFile");
            new Mets(new File(rename), images, meta);
        } else {
            new Mets(images, meta);
        }
        new Mods(meta, hida, map);
    }
}
