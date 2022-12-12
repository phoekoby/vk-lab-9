package org.example.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import org.example.ClanEvent;
import org.example.dto.ClanDTO;

import java.util.concurrent.ThreadLocalRandom;

import static org.example.MapsConstants.*;
import static org.example.MessageChannelsConstants.*;

public class AdminVerticle extends AbstractVerticle {

    private final String name;

    private ClanDTO clan;

    public AdminVerticle(String name) {
        this.name = name;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        requestToCheckOnline(startPromise);
        vertx.eventBus().consumer(CHAT + name, event -> {
            System.out.println("I got message from " + event.body());
        });
        System.out.println("AdminVerticle with Name " + name + " deployed");
    }

    private void getClan(Promise<?> promise) {
        this.getVertx().sharedData().<String, ClanDTO>getAsyncMap(ALL_USERS_CLAN, map -> {
            if (map.succeeded()) {
                map.result().get(name, clan -> {
                    if (clan.succeeded()) {
                        this.clan = clan.result();
                        promise.complete();
                    }
                });
            }
        });
    }

    private void setTimerForChangingClanInformation() {
        final ThreadLocalRandom randomTimerDelay = ThreadLocalRandom.current();
        vertx.setTimer(randomTimerDelay.nextLong(10000L, 1000000000L), event -> {
            final DeliveryOptions deliveryOptions = new DeliveryOptions().setSendTimeout(1000);
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            ClanDTO newClan = new ClanDTO(clan.getName(), clan.getAdmin(), random.nextInt(100), random.nextInt(100), clan.getModerators());
            vertx.eventBus().<Boolean>request(CHANGE_CLAN_INFO_REQUEST + clan.getName(), newClan, deliveryOptions, reply -> {
                if (reply.succeeded()) {
                    if (reply.result().body()) {
                        vertx.eventBus().publish(CLAN_CHAT + clan.getName(), "Go out from Clan!");
                    }
                }
            });
        });
    }


    private void requestToCheckOnline(Promise<Void> startPromise) {
        vertx.sharedData().<String, Boolean>getAsyncMap(USER_ONLINE_MAP, map -> {
            if (map.succeeded()) {
                map.result().get(name, boolRes -> {
                    if (boolRes.result() == null || !boolRes.result()) {
                        vertx.sharedData().<String, Boolean>getAsyncMap(USER_ONLINE_MAP, map1 -> {
                            if (map1.succeeded()) {
                                map1.result().put(name, true);
                                vertx.executeBlocking(
                                        this::getClan,
                                        res -> {
                                            System.out.println("Clan: " + clan.getName());
                                            vertx.eventBus().send(ACTIVATE_CLAN + clan.getName(), ClanEvent.ACTIVATE.getValue());
                                            setTimerForChangingClanInformation();
                                            System.out.println("I [ " + name + " ] am online now!");
                                        }
                                );
                            }
                        });
                    } else {
                        startPromise.fail(new IllegalArgumentException("User with  name [" + name + "] is already online"));
                    }
                });
            }
        });
    }

    @Override
    public void stop() {
        vertx.sharedData().<String, Boolean>getAsyncMap(USER_ONLINE_MAP, map1 -> {
            if (map1.succeeded()) {
                map1.result().put(name, false);
                vertx.eventBus().send(ACTIVATE_CLAN + clan.getName(), ClanEvent.DISACTIVATE.getValue());
            }
        });
    }
}
