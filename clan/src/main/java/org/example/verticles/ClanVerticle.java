package org.example.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.example.dto.ClanDTO;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.example.MapsConstants.*;
import static org.example.MessageChannelsConstants.*;


public class ClanVerticle extends AbstractVerticle {
    private final ClanDTO clan;



    public ClanVerticle(@NotNull ClanDTO clan) {
        this.clan = clan;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        System.out.println("ClanVerticle with Name " + clan.getName() + " deployed");
        if(clan.getAmountModerators() + 1 >= clan.getAmountUsers()){
            startPromise.fail("Amount of users can not be less than amount moderators");
        }
        if(clan.getAmountModerators() < clan.getModerators().size()){
            startPromise.fail("You set so many moderators");
        }

        putData();
        subscribe();
    }

    private void putData(){
        vertx.sharedData().<String, ClanDTO>getAsyncMap(CLAN_INFORMATION, map->{
            if(map.succeeded()){
                map.result().put(clan.getName(), clan);
            }
        });

        vertx.sharedData().<String, ClanDTO>getAsyncMap(ALL_USERS_CLAN, map->{
            if(map.succeeded()){
                map.result().put(clan.getAdmin(), clan);
                clan.getModerators().forEach(s -> map.result().put(s, clan));
            }
        });

        vertx.sharedData().<String, List<String>>getAsyncMap(CLAN_USERS, map->{
            if(map.succeeded()){
                List<String> users = new ArrayList<>(List.copyOf(clan.getModerators()));
                users.add(clan.getAdmin());
                map.result().put(clan.getName(), users);
            }
        });
        vertx.sharedData().<String, Boolean>getAsyncMap(CLAN_ACTIVE_MAP, map->{
            if(map.succeeded()){
                map.result().put(clan.getName(), false);
            }
        });
    }

    private void subscribe(){
        vertx.eventBus().<ClanDTO>consumer(CHANGE_CLAN_INFO_REQUEST + clan.getName(), event -> {
            if(!event.body().getName().equals(clan.getName())){
                event.fail(400, "clan name can not be changed");
            }
            if(event.body().getModerators().size() > event.body().getAmountModerators()){
                event.fail(400, "You set too many moderators");
            }
            if(clan.getAmountModerators() + 1 < clan.getAmountUsers()){
                event.fail(100,"Amount of users can not be less than amount moderators");
            }
            this.getVertx().sharedData().<String, ClanDTO>getAsyncMap(CLAN_INFORMATION, map -> {
                if (map.succeeded()) {
                    map.result().put(clan.getName(), event.body());
                    vertx.sharedData().<String, List<String>>getAsyncMap(CLAN_USERS, map1 -> {
                        if(map1.succeeded()){
                            map1.result().get(clan.getName(), res->{
                                if(res.succeeded()){
                                    if(res.result().size() > clan.getAmountUsers()){
                                        event.reply(true);
                                    }else {
                                        event.reply(false);
                                    }
                                }
                            });
                        }
                    });
                }
            });
        });
        vertx.eventBus().<Boolean>consumer(ACTIVATE_CLAN + clan.getName(), event -> {
            var clanEvent = event.body();
            vertx.sharedData().<String, Boolean>getAsyncMap(CLAN_ACTIVE_MAP, map->{
                if(map.succeeded()){
                    map.result().put(clan.getName(), clanEvent);
                    System.out.println("Clan " + clan.getName() + " is active now!");
                }
            });
        });

        vertx.eventBus().<String>consumer(ADD_OR_DENY_REQUEST + clan.getName(), event->
                vertx.sharedData().<String, List<String>>getAsyncMap(CLAN_USERS, map1 -> {
            if(map1.succeeded()){
                map1.result().get(clan.getName(), res->{
                    if(res.succeeded()){
                        if(res.result().size() < clan.getAmountUsers()){
                            res.result().add(event.body());
                            List<String> members = res.result();
                            members.add(event.body());
                            vertx.sharedData().<String, List<String>>getAsyncMap(CLAN_USERS, map -> {
                                if(map.succeeded()) {
                                    map.result().put(clan.getName(), members);
                                }
                            });

                            vertx.sharedData().<String, ClanDTO>getAsyncMap(ALL_USERS_CLAN, map->{
                                if(map.succeeded()){
                                    map.result().put(event.body(), clan);
                                    event.reply(true);
                                }
                            });
                        }else{
                            event.reply(false);
                        }
                    }
                });
            }
        }));

    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        super.stop(stopPromise);
    }

}
