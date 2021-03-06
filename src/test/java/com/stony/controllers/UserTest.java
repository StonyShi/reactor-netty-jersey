package com.stony.controllers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p>reactor-netty-jersey
 * <p>com.stony.controllers
 *
 * @author stony
 * @version 下午6:32
 * @since 2018/12/11
 */
public class UserTest {
    String name;
    int id;
    @JsonProperty("first_name")
    private String firstName;

    @JsonCreator
    public UserTest(@JsonProperty("name") String name, @JsonProperty("id") int id, @JsonProperty("first_name") String firstName) {
        this.name = name;
        this.id = id;
        this.firstName = firstName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public String toString() {
        return "UserTest{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", firstName='" + firstName + '\'' +
                '}';
    }
}
