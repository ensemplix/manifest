package ru.ensemplix.manifest;

import org.apache.commons.cli.ParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestManifest {

    @Test
    public void testManifest() throws IOException, ParseException {
        Manifest.Resource[] expected = new Manifest.Resource[] {
                new Manifest.Resource("/natives/libjinput-linux.so", "f2317f7c050cd441510423e90fb16dfd", 13824),
                new Manifest.Resource("/natives/libjinput-linux64.so", "23a6b611eaab617a9394f932b69ae034", 14512),
                new Manifest.Resource("/natives/other/jinput-dx8.dll", "ae25629d223b95f73f2f27800da6bbb3", 61952),
                new Manifest.Resource("/natives/other/jinput-dx8_64.dll", "f1a51706365a44ea21aa96a9a04bfb37", 65024)
        };

        String url = "http://resources.ensemplix.ru";

        File fileJson = new File("src/test/resources/natives.json");
        File fileUrl = new File("src/test/resources/natives.txt");

        String[] args = new String[] {
                "--folder=src/test/resources/natives",
                "--url=" + url,
                "--info"
        };

        Manifest.main(args);

        ObjectMapper mapper = new ObjectMapper();
        Manifest.Resource[] actual = mapper.readValue(fileJson, Manifest.Resource[].class);
        fileJson.delete();

        assertEquals(expected.length, actual.length);

        for(int i = 0; i < expected.length; i++) {
            boolean found = false;

            for(int y = 0; i < actual.length; y++) {
                if(actual[y].name.equalsIgnoreCase(expected[i].name)) {
                    assertEquals(expected[i].name, actual[y].name);
                    assertEquals(expected[i].hash, actual[y].hash);
                    assertEquals(expected[i].size, actual[y].size);

                    found = true;
                    break;
                }
            }

            if(!found) {
                fail("Not found expected " + expected[i].name);
            }
        }

        try(BufferedReader br = new BufferedReader(new FileReader(fileUrl))) {
            String line;
            int i = 0;

            while((line = br.readLine()) != null) {
                assertEquals(url + expected[i].name, line);
                i++;
            }
        } finally {
            fileUrl.delete();
        }
    }

}
