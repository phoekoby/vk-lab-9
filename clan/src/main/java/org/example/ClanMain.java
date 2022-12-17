package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.example.dto.ClanDTO;
import org.example.verticles.ClanVerticle;

import java.io.IOException;
import java.net.URL;
import java.util.List;

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
                        final var options = new DeploymentOptions();
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