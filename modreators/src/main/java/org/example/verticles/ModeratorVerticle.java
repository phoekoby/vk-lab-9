package org.example.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Promise;
import io.vertx.core.shareddata.AsyncMap;
import org.example.dto.ClanDTO;

import java.util.List;

import static org.example.MapsConstants.*;
import static org.example.MessageChannelsConstants.CHAT;
import static org.example.MessageChannelsConstants.JOIN_REQUEST;

public class ModeratorVerticle extends AbstractVerticle {
    private final String name;

    private ClanDTO clan;

    public ModeratorVerticle(String name) {
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
                map.result().get(name, clanName -> {
                    if (clanName.succeeded()) {
                        vertx.sharedData().<String, ClanDTO>getAsyncMap(CLAN_INFORMATION, resultClanMap -> {
                            if (resultClanMap.succeeded()) {
                                resultClanMap.result().get(clanName.result(), resultClanDTO -> {
                                    if (resultClanDTO.succeeded()) {
                                        this.clan = resultClanDTO.result();
                                        subscribe();
                                    }else {
                                        startPromise.fail("Clan not found");
                                    }
                                });
                            }else {
                                startPromise.fail("Map not found");
                            }
                        });
                    } else {
                        startPromise.fail("Clan not found");
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

    private void subscribe() {
        vertx.eventBus().<String>consumer(JOIN_REQUEST + clan.getName(), event -> {
            String userName = event.body();
            vertx.sharedData().<String, String>getAsyncMap(ALL_USERS_CLAN, allUsersMap -> {
                if (allUsersMap.succeeded()) {
                    allUsersMap.result().get(userName, result -> {
                        if (result.succeeded()) {
                            if (result.result() != null) {
                                System.out.println("User [" + userName + "] is already in clan");
                                event.reply(false);
                            } else {
                                vertx.sharedData().<String, List<String>>getAsyncMap(CLAN_USERS, clanUsersMap -> {
                                    if(clanUsersMap.succeeded()){
                                        clanUsersMap.result().get(clan.getName(), res->{
                                            if(res.succeeded()){
                                                if(res.result().size() < clan.getAmountUsers()){
                                                    res.result().add(event.body());
                                                    List<String> members = res.result();

                                                    CompositeFuture.all(vertx.sharedData().<String, List<String>>getAsyncMap(CLAN_USERS),
                                                                    vertx.sharedData().<String, String>getAsyncMap(ALL_USERS_CLAN))
                                                            .onComplete(compositeFutureResult->{
                                                                if(compositeFutureResult.succeeded()){
                                                                    CompositeFuture resultOmCompositeFuture = compositeFutureResult.result();
                                                                    AsyncMap<String, List<String>> mapClanToUsers = resultOmCompositeFuture.resultAt(0);
                                                                    AsyncMap<String, String> allUsersClan = resultOmCompositeFuture.resultAt(1);
                                                                    CompositeFuture.all(mapClanToUsers.put(clan.getName(), members),
                                                                                    allUsersClan.put(event.body(), clan.getName()))
                                                                            .onComplete(putResult -> {
                                                                                if(putResult.succeeded()){
                                                                                    System.out.println(name + " added " + event.body() + " to clan " + clan.getName());
                                                                                    event.reply(true);
                                                                                }else{
                                                                                    event.reply(false);
                                                                                }
                                                                            });
                                                                }else {
                                                                    event.reply(false);
                                                                }
                                                            });
                                                }else{
                                                    event.reply(false);
                                                }
                                            }
                                        });
                                    }
                                });
//
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
