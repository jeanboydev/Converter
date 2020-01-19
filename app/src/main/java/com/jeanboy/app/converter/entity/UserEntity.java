package com.jeanboy.app.converter.entity;


import com.jeanboy.converter.annotation.Field;
import com.jeanboy.converter.annotation.Source;

import java.util.List;

/**
 * @author caojianbo
 * @since 2020/1/16 17:14
 */
@Source("user")
public class UserEntity {

    @Field(identity = "haha")
    private String userName;

    @Field(identity = "can")
    private boolean isCanDo;

    @Field(identity = "age")
    private int ageCount;

    @Field(identity = "updated")
    private boolean updatedState;

    @Field(identity = "createAt")
    private Long createAtTime;

    @Field(identity = "list")
    private List<String> stringList;

    public boolean isCanDo() {
        return isCanDo;
    }

    public void setCanDo(boolean canDo) {
        isCanDo = canDo;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getAgeCount() {
        return ageCount;
    }

    public void setAgeCount(int ageCount) {
        this.ageCount = ageCount;
    }

    public boolean isUpdatedState() {
        return updatedState;
    }

    public void setUpdatedState(boolean updatedState) {
        this.updatedState = updatedState;
    }

    public Long getCreateAtTime() {
        return createAtTime;
    }

    public void setCreateAtTime(Long createAtTime) {
        this.createAtTime = createAtTime;
    }

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }
}
