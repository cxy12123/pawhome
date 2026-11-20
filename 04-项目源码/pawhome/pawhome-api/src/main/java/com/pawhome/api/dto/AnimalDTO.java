package com.pawhome.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 动物信息传输对象。
 * <p>
 * 为什么不在服务间直接传 Entity？
 * 因为 Entity 绑定了数据库表结构，一旦某个服务改了表，所有调用方都要跟着改；
 * DTO 是对外契约，字段可以裁剪，表结构变更对调用方透明。
 * 这是微服务"契约优先"的基本要求。
 */
@Data
public class AnimalDTO implements Serializable {

    private Long id;
    private String animalNo;
    private String name;
    private String species;
    private String breed;
    private String gender;
    private Integer ageMonth;
    private String healthStatus;
    private String adoptStatus;
    private Long stationId;
    private String coverImage;
}
