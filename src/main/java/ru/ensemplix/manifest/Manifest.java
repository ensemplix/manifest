package ru.ensemplix.manifest;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

public class Manifest {

    private final File folder;

    private final File output;

    private final boolean info;

    private final List<Resource> resources = new LinkedList<>();

    public Manifest(File folder, File output, boolean info) {
        this.folder = folder;
        this.output = output;
        this.info = info;
    }

    public List<Resource> getResources() {
        return resources;
    }

    private void fetch(File target) throws IOException {
        for(File file : target.listFiles()) {
            if(file.isDirectory()) {
                fetch(file);
                continue;
            }

            String name = file.getAbsolutePath();
            name = name.replace(folder.getParentFile().getAbsolutePath(), "");
            name = name.replaceAll("\\\\", "/");

            long size = file.length();
            String hash;

            try(FileInputStream input = new FileInputStream(file)) {
                hash = DigestUtils.md5Hex(input);
            }

            resources.add(new Resource(name, hash, size));

            if(info) {
                System.out.println("Name: " + name + ", Hash: " + hash +  ", Size: " + size);
            }
        }
    }

    public static void main(String[] args) throws ParseException, IOException {
        Options options = new Options();
        options.addOption("f", "folder", true, "folder that need to be patched");
        options.addOption("u", "url", true, "url where files will be hosted");
        options.addOption("h", "hash", false, "files will renamed to they hash");
        options.addOption("i", "info", false, "prints info during patch creation");

        CommandLine cmd = new GnuParser().parse(options, args);

        if(!cmd.hasOption("folder")) {
            System.out.println("Please provide folder that need to be patched.");
            return;
        }

        File folder = new File(cmd.getOptionValue("folder"));

        if(!folder.isDirectory()) {
            System.out.println("Please provide existing folder.");
            return;
        }

        File coreFolder = folder.getParentFile();
        File outputJson = new File(coreFolder, folder.getName() + ".json");

        System.out.println("Creating manifest.");

        Manifest manifest = new Manifest(folder, outputJson, cmd.hasOption("info"));
        manifest.fetch(folder);

        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(outputJson, manifest.getResources());

        if(cmd.hasOption("url")) {
            File outputUrl = new File(coreFolder, folder.getName() + ".txt");

            try (FileWriter writer = new FileWriter(outputUrl)) {
                for (Resource resource : manifest.getResources()) {
                    writer.write(cmd.getOptionValue("url") + resource.name + "\n");
                }
            }
        }

        if(cmd.hasOption("hash")) {
            File hashFolder = new File(coreFolder, "hashed");

            for (Resource resource : manifest.getResources()) {
                String fileName = resource.name.substring(resource.name.lastIndexOf("/") + 1);
                File hashFile = new File(hashFolder, resource.name.replaceAll(fileName, resource.hash));

                if(!hashFile.exists()) {
                    hashFile.getParentFile().mkdirs();
                    Files.copy(new File(coreFolder, resource.name).toPath(), hashFile.toPath());
                }
            }
        }

        System.out.println("Finished creating manifest patch from " + manifest.getResources().size() + " resources.");
    }

    public static class Resource {

        public String name;

        public String hash;

        public long size;

        public Resource() {

        }

        public Resource(String name, String hash, long size) {
            this.name = name;
            this.hash = hash;
            this.size = size;
        }

    }

}
