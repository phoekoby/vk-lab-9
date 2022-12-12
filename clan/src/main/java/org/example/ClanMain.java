package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import org.example.dto.ClanDTO;
import org.example.verticles.ClanVerticle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public class ClanMain {
    private static final String IMPORT_CLANS_FILE = "/import-clans";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {

        Vertx.clusteredVertx(
                new VertxOptions(),
                vertxResult -> {
                    final var vertx = vertxResult.result();
                    try {
                        URL file = ClanMain.class.getResource(IMPORT_CLANS_FILE);
                        final var options = new DeploymentOptions().setWorker(true);
                        List<ClanDTO> clansDTOs = objectMapper.readValue(file,
                                objectMapper.getTypeFactory().constructCollectionType(List.class, ClanDTO.class));
                        for (ClanDTO clanDTO : clansDTOs) {
                            vertx.deployVerticle(new ClanVerticle(clanDTO), options);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}