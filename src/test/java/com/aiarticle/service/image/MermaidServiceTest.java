package com.aiarticle.service.image;

import com.aiarticle.config.MermaidConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MermaidServiceTest {

    @Test
    void searchImage_returnsNullWhenSourceEmpty() {
        MermaidService service = new MermaidService(new MermaidConfig());
        assertNull(service.searchImage(ImageSearchRequest.builder().build()));
        assertEquals("MERMAID", service.getSearchMethod());
        assertNull(service.getFallbackImageUrl(1));
    }

    @Test
    void buildCommand_usesSizeFlagNotLegacyWidth() {
        MermaidConfig config = new MermaidConfig();
        config.setCliCommand("mmdc");
        config.setBackgroundColor("transparent");
        config.setWidth(1200);
        config.setPuppeteerConfigFile("");
        MermaidService service = new MermaidService(config);

        List<String> command = service.buildCommand(
                Path.of("/tmp/in.mmd"), Path.of("/tmp/out.svg"), "svg");

        assertTrue(command.contains("--size"));
        assertTrue(command.contains("1200"));
        assertFalse(command.contains("-w"));
        assertFalse(command.contains("-p"));
        assertEquals("svg", command.get(command.indexOf("-e") + 1));
    }

    @Test
    void buildCommand_includesPuppeteerConfigWhenFileExists() throws Exception {
        Path configFile = Files.createTempFile("mermaid-puppeteer-", ".json");
        Files.writeString(configFile, "{\"executablePath\":\"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome\"}");
        try {
            MermaidConfig config = new MermaidConfig();
            config.setPuppeteerConfigFile(configFile.toString());
            MermaidService service = new MermaidService(config);

            List<String> command = service.buildCommand(
                    Path.of("/tmp/in.mmd"), Path.of("/tmp/out.svg"), "svg");

            assertTrue(command.contains("-p"));
            assertEquals(configFile.toAbsolutePath().toString(),
                    command.get(command.indexOf("-p") + 1));
        } finally {
            Files.deleteIfExists(configFile);
        }
    }
}
