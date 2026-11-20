package com.pawhome.animal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "动物建档/修改请求")
public class AnimalSaveRequest {

    @Schema(description = "主键，修改时必填")
    private Long id;

    @NotBlank(message = "动物昵称不能为空")
    @Schema(description = "昵称", example = "豆豆")
    private String name;

    @NotBlank(message = "请选择物种")
    @Schema(description = "物种：DOG/CAT/OTHER", example = "DOG")
    private String species;

    @Schema(description = "品种", example = "中华田园犬")
    private String breed;

    @NotBlank(message = "请选择性别")
    @Schema(description = "性别：M/F", example = "M")
    private String gender;

    @NotNull(message = "月龄不能为空")
    @Min(value = 0, message = "月龄不能为负数")
    private Integer ageMonth;

    @Schema(description = "体重(kg)")
    private BigDecimal weight;

    @Schema(description = "健康状态：HEALTHY/TREATING/RECOVERING/DISABLED")
    private String healthStatus;

    @Schema(description = "救助日期")
    private LocalDate rescueDate;

    @Schema(description = "救助城市")
    private String rescueCity;

    @Schema(description = "救助来源")
    private String rescueSource;

    @Schema(description = "所属救助站")
    private Long stationId;

    @Schema(description = "是否绝育 1/0")
    private Integer sterilized;

    @Schema(description = "是否接种疫苗 1/0")
    private Integer vaccinated;

    @Schema(description = "是否驱虫 1/0")
    private Integer dewormed;

    @Schema(description = "性格描述")
    private String description;

    @Schema(description = "封面图")
    private String coverImage;
}
