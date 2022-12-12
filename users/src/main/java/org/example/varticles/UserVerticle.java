package org.example.varticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import org.example.ClanEvent;
import org.example.dto.ClanDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import static org.example.MapsConstants.*;
import static org.example.MessageChannelsConstants.*;

public class UserVerticle extends AbstractVerticle {

    private ClanDTO clan;

    private final String name;

    public UserVerticle(String name) {
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
                        if (clan.result() != null) {
                            subscribeForInClan();
                        } else {
                            subscribeForNotInClan();
                        }
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


    private void subscribeForInClan() {
        vertx.setPeriodic(10000, event1 -> {
            vertx.sharedData().<String, List<String>>getAsyncMap(CLAN_USERS, event -> {
                if (event.succeeded()) {
                    event.result().get(clan.getName(), res -> {
                        if (res.succeeded()) {
                            List<String> members = res.result();
                                final ThreadLocalRandom random = ThreadLocalRandom.current();
                                String target = members.get(random.nextInt(members.size()));
                                while (target.equals(name)){
                                    target = members.get(random.nextInt(members.size()));
                                }
                                vertx.eventBus().send(CHAT + target, name);

                            }
                        });
                    }
                });
            });
            vertx.eventBus().consumer(CLAN_CHAT, event -> {
                System.out.println("I [" + name + "] am going out");
            });
        }

        private void subscribeForNotInClan () {
            vertx.setPeriodic(10000, timer -> {
                vertx.sharedData().<String, Boolean>getAsyncMap(CLAN_ACTIVE_MAP, map -> {
                    if (map.succeeded()) {
                        map
                                .result()
                                .entries()
                                .onComplete(res -> {
                                    List<String> activeClans = res.result()
                                            .entrySet()
                                            .stream()
                                            .filter(Map.Entry::getValue)
                                            .map(Map.Entry::getKey)
                                            .toList();

                                    System.out.println("All active clans: " + activeClans);
                                    final ThreadLocalRandom random = ThreadLocalRandom.current();

                                    if (activeClans.size() != 0) {
                                        final ThreadLocalRandom newRandom = ThreadLocalRandom.current();
                                        int number = newRandom.nextInt(100);
                                        if (number >= 50) {
                                            String clanName = activeClans.get(random.nextInt(activeClans.size()));
                                            System.out.println("Sending application for membership in " + clanName);
                                            vertx.eventBus().<Boolean>request(JOIN_REQUEST + clanName, name, new DeliveryOptions()
                                                            .setSendTimeout(1500),
                                                    reply -> {
                                                        if (reply.succeeded()) {
                                                            System.out.println("I have response form moderators of " + clanName + ", he said: " + reply.result().body());
                                                            if (reply.result().body()) {
                                                                vertx.cancelTimer(timer);
                                                                getClan();
                                                                System.out.println("Congratulations! A am in clan " + clanName + " now!");
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
        public void stop (Promise < Void > stopPromise)  {
            vertx.sharedData().<String, Boolean>getAsyncMap(USER_ONLINE_MAP, map -> {
                if (map.succeeded()) {
                    map.result().put(name, false);
                    stopPromise.complete();
                }
            });
        }
    }
