package org.example.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;


@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClanDTO implements Serializable {
    @JsonProperty("name")
    private final String name;

    @JsonProperty("admin")
    private final String admin;

    @JsonProperty("amountModerators")
    private final Integer amountModerators;
    @JsonProperty("amountUsers")
    private final Integer amountUsers;
    @JsonProperty("moderators")
    private final List<String> moderators;


    @JsonCreator
    public ClanDTO(@JsonProperty("name") String name,
                   @JsonProperty("admin") String admin,
                   @JsonProperty("amountModerators") Integer amountModerators,
                   @JsonProperty("amountUsers") Integer amountUsers,
                   @JsonProperty("moderators") List<String> moderators) {
        this.name = name;
        this.admin = admin;
        this.amountModerators = amountModerators == null ? 20 : amountModerators;
        this.amountUsers = amountUsers == null ? 40 : amountUsers;
        this.moderators = moderators;
    }


}
