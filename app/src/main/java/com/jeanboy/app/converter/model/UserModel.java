package com.jeanboy.app.converter.model;


import com.jeanboy.converter.annotation.Field;
import com.jeanboy.converter.annotation.Product;

import java.util.List;

/**
 * @author caojianbo
 * @since 2020/1/16 16:46
 */
@Product("user")
public class UserModel {

    @Field(identity = "haha")
    private String name;

    @Field(identity = "can")
    private boolean isCan;

    @Field(identity = "age")
    private int age;

    @Field(identity = "updated")
    private boolean updated;

    @Field(identity = "createAt")
    private Long createAt;

    @Field(identity = "list")
    private List<String> strings;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCan() {
        return isCan;
    }

    public void setCan(boolean can) {
        isCan = can;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public Long getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Long createAt) {
        this.createAt = createAt;
    }

    public List<String> getStrings() {
        return strings;
    }

    public void setStrings(List<String> strings) {
        this.strings = strings;
    }
}
