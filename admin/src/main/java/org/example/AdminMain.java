package org.example;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.example.verticles.AdminVerticle;
import org.jetbrains.annotations.NotNull;

public class AdminMain {
    public static void main(@NotNull String[] args) {
        if (args.length != 2 || !args[0].equals("--name")) {
            throw new IllegalArgumentException("Should be --name \"value\"");
        } else {
            @NotNull String name = args[1];
            Vertx.clusteredVertx(
                    new VertxOptions(),
                    vertxResult -> {
                        final var vertx = vertxResult.result();
                        final var options = new DeploymentOptions().setWorker(true);
                        final var adminVerticle = new AdminVerticle(name);
                        vertx.deployVerticle(adminVerticle, options, result -> {
                            if (result.failed()) {
                                vertx.undeploy(adminVerticle.deploymentID());
                                System.err.println(result.cause().getMessage());
                                System.exit(1);
                            }
                        });
                    }
            );
        }


    }
}