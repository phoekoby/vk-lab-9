package org.example.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.shareddata.AsyncMap;
import org.example.dto.ClanDTO;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.example.MapsConstants.*;
import static org.example.MessageChannelsConstants.ACTIVATE_CLAN;
import static org.example.MessageChannelsConstants.CHANGE_CLAN_INFO_REQUEST;


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

        putData(startPromise);
        subscribe();
    }

    private void putData(Promise<Void> startPromise){
        CompositeFuture.all(vertx.sharedData().<String, ClanDTO>getAsyncMap(CLAN_INFORMATION),
                        vertx.sharedData().<String, String>getAsyncMap(ALL_USERS_CLAN),
                        vertx.sharedData().<String, List<String>>getAsyncMap(CLAN_USERS),
                        vertx.sharedData().<String, Boolean>getAsyncMap(CLAN_ACTIVE_MAP))
                .onComplete(result->{
                    if(result.succeeded()){
                        List<Future> futureList = new ArrayList<>();
                        CompositeFuture res = result.result();
                        AsyncMap<String, ClanDTO> stringClanDTOAsyncMap = res.resultAt(0);
                        futureList.add(stringClanDTOAsyncMap.put(clan.getName(), clan));

                        AsyncMap<String, String> allUsersMap = res.resultAt(1);
                        futureList.add(allUsersMap.put(clan.getAdmin(), clan.getName()));
                        clan.getModerators().forEach(s -> futureList.add(allUsersMap.put(s, clan.getName())));

                        AsyncMap<String, List<String>> clanToUsersMap = res.resultAt(2);
                        List<String> users = new ArrayList<>(List.copyOf(clan.getModerators()));
                        users.add(clan.getAdmin());
                        futureList.add(clanToUsersMap.put(clan.getName(), users));

                        AsyncMap<String, Boolean> clanActiveMap = res.resultAt(3);
                        futureList.add(clanActiveMap.put(clan.getName(), false));


                        CompositeFuture
                                .all(futureList)
                                .onComplete(putsResult->{
                                    if(putsResult.failed()){
                                        startPromise.fail("Something wrong...");
                                    }
                                });
                    }else {
                        startPromise.fail("Something wrong...");
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
            this.getVertx().sharedData().<String, ClanDTO>getAsyncMap(CLAN_INFORMATION, clanInformationMap -> {
                if (clanInformationMap.succeeded()) {
                    clanInformationMap.result().put(clan.getName(), event.body());
                    vertx.sharedData().<String, List<String>>getAsyncMap(CLAN_USERS, clanToUsersMap -> {
                        if(clanToUsersMap.succeeded()){
                            clanToUsersMap.result().get(clan.getName(), res->{
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
            vertx.sharedData().<String, Boolean>getAsyncMap(CLAN_ACTIVE_MAP, clanIsActiveMap->{
                if(clanIsActiveMap.succeeded()){
                    clanIsActiveMap.result().put(clan.getName(), clanEvent, putResult -> System.out.println("Clan " + clan.getName() + " is active now!"));
                }
            });
        });
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        super.stop(stopPromise);
    }

}
