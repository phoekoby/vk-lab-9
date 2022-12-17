package org.example.varticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import org.example.dto.ClanDTO;

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
        vertx.eventBus().consumer(CHAT + name, event -> System.out.println("I got message from " + event.body()));
    }

    private void getClan(Promise<Void> startPromise) {
        this.getVertx().sharedData().<String, String>getAsyncMap(ALL_USERS_CLAN, map -> {
            if (map.succeeded()) {
                map.result().get(name, clan -> {
                    if (clan.succeeded()) {
                        if(clan.result() != null) {
                            vertx.sharedData().<String, ClanDTO>getAsyncMap(CLAN_INFORMATION, resultClanMap -> {
                                if (resultClanMap.succeeded()) {
                                    resultClanMap.result().get(clan.result(), resultClanDTO -> {
                                        if (resultClanDTO.succeeded()) {
                                            this.clan = resultClanDTO.result();
                                            if (clan.result() != null) {
                                                startPromise.complete();
                                                subscribeForInClan();
                                            }
                                        } else {
                                            startPromise.fail("Clan not found");
                                        }
                                    });
                                } else {
                                    startPromise.fail("Map not found");
                                }
                            });
                        }else {
                            subscribeForNotInClan();
                        }
                    } else {
                        startPromise.fail("clan not found");
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
                        getClan(startPromise);
                    } else {
                        startPromise.fail(new IllegalArgumentException("User with  name [" + name + "] is already online"));
                    }
                });
            }
        });
    }


    private void subscribeForInClan() {
        vertx.setPeriodic(10000, timer -> vertx.sharedData().<String, List<String>>getAsyncMap(CLAN_USERS, clanUsersMap -> {
            if (clanUsersMap.succeeded()) {
                clanUsersMap.result().get(clan.getName(), res -> {
                    if (res.succeeded()) {
                        List<String> members = res.result();
                        final ThreadLocalRandom random = ThreadLocalRandom.current();
                        List<String> filteredMembers = members
                                .stream()
                                .filter(s -> !s.equals(name))
                                .toList();
                                String target = filteredMembers.get(random.nextInt(filteredMembers.size()));
                        vertx.eventBus().send(CHAT + target, name);

                    }
                });
            }
        }));
        vertx.eventBus().consumer(CLAN_CHAT, event -> {
            System.out.println("I [" + name + "] am going out");
            clan = null;
            vertx.sharedData().<String, String>getAsyncMap(ALL_USERS_CLAN, mapResult->{
                if(mapResult.succeeded()){
                    mapResult.result().put(name, null);
                }
            });
            vertx.sharedData().<String, List<String>>getAsyncMap(CLAN_USERS, mapClanUsersResult -> {
                if(mapClanUsersResult.succeeded()){
                    mapClanUsersResult.result().get(clan.getName(), getListOFUsers-> getListOFUsers.result().remove(name));
                }
            });
            subscribeForNotInClan();
        });
    }

    private void subscribeForNotInClan() {
        final int RANDOM_ROOF = 100;
        final int EXECUTE_REQUEST_RANDOM_ROOF = 50;
        final int TIME_OUT = 1500;
        final DeliveryOptions deliveryOptions = new DeliveryOptions();
        vertx.setPeriodic(10000, timer -> vertx.sharedData().<String, Boolean>getAsyncMap(CLAN_ACTIVE_MAP, clanIsActiveMap -> {
            if (clanIsActiveMap.succeeded()) {
                clanIsActiveMap
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
                                int number = random.nextInt(RANDOM_ROOF);
                                if (number >= EXECUTE_REQUEST_RANDOM_ROOF) {
                                    String clanName = activeClans.get(random.nextInt(activeClans.size()));
                                    System.out.println("Sending application for membership in " + clanName);
                                    vertx.eventBus().<Boolean>request(JOIN_REQUEST + clanName, name, deliveryOptions
                                                    .setSendTimeout(TIME_OUT),
                                            reply -> {
                                                if (reply.succeeded()) {
                                                    System.out.println("I have response form moderators of " + clanName + ", he said: " + reply.result().body());
                                                    if (reply.result().body()) {
                                                        vertx.cancelTimer(timer);
                                                        Promise<Void> promise = Promise.promise();
                                                        getClan(promise);
                                                        promise.future().onSuccess(success -> {
                                                            System.out.println("Congratulations! A am in clan " + clanName + " now!");
                                                        });
                                                    }
                                                }
                                            });
                                }
                            }
                        });
            }
        }));
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        vertx.sharedData().<String, Boolean>getAsyncMap(USER_ONLINE_MAP, map -> {
            if (map.succeeded()) {
                map.result().put(name, false);
                stopPromise.complete();
            }
        });
    }
}
