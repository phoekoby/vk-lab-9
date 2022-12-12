package org.example.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import org.example.ClanEvent;
import org.example.dto.ClanDTO;

import static org.example.MapsConstants.*;
import static org.example.MessageChannelsConstants.*;

public class ModeratorVerticle extends AbstractVerticle {
    private final String name;

    private ClanDTO clan;

    public ModeratorVerticle(String name) {
        this.name = name;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        requestToCheckOnline(startPromise);
        vertx.eventBus().consumer(CHAT + name, event -> {
            System.out.println("I got message from " + event.body());
        });
    }

    private void getClan() {
        this.getVertx().sharedData().<String, ClanDTO>getAsyncMap(ALL_USERS_CLAN, map -> {
            if (map.succeeded()) {
                map.result().get(name, clan -> {
                    if (clan.succeeded()) {
                        this.clan = clan.result();
                        subscribe();
                    }
                });
            }
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
                            }
                        });
                        System.out.println("I [ " + name + " ] am online now!");
                        getClan();


                    } else {
                        startPromise.fail(new IllegalArgumentException("User with  name [" + name + "] is already online"));
                    }
                });
            }
        });
    }

    private void subscribe() {
        vertx.eventBus().<String>consumer(JOIN_REQUEST + clan.getName(), event -> {
            String userName = event.body();
            vertx.sharedData().<String, ClanDTO>getAsyncMap(ALL_USERS_CLAN, map -> {
                if (map.succeeded()) {
                    map.result().get(userName, result -> {
                        if (result.succeeded()) {
                            if (result.result() != null) {
                                System.out.println("User [" + userName + "] is already in clan");
                                event.reply(false);
                            } else {
                                vertx.eventBus().<Boolean>request(ADD_OR_DENY_REQUEST + clan.getName(), userName, reply -> {
                                    if (reply.succeeded()) {
                                        if (reply.result().body()) {
                                            System.out.println("User [" + userName + "] was added in clan [" + clan.getName() + "] by Moderator [" + name + "]");
                                            event.reply(true);
                                        } else {
                                            System.out.println("Sorry, " + userName + " we can not add you in our clan :((");
                                            event.reply(false);
                                        }
                                    }
                                });
                            }
                        }
                    });

                }
            });
        });
    }

    @Override
    public void stop (Promise < Void > stopPromise) {
        vertx.sharedData().<String, Boolean>getAsyncMap(USER_ONLINE_MAP, map -> {
            if (map.succeeded()) {
                map.result().put(name, false);
                stopPromise.complete();
            }
        });
    }
}
