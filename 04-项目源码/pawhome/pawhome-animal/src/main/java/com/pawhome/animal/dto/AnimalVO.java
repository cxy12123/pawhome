package com.pawhome.animal.dto;

import com.pawhome.animal.entity.AnimalMedical;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "动物详情")
public class AnimalVO {

    private Long id;
    private String animalNo;
    private String name;
    private String species;
    private String speciesText;
    private String breed;
    private String gender;
    private String genderText;
    private Integer ageMonth;
    private String ageText;
    private BigDecimal weight;
    private String healthStatus;
    private String healthStatusText;
    private String adoptStatus;
    private String adoptStatusText;
    private LocalDate rescueDate;
    private String rescueCity;
    private String rescueSource;
    private Long stationId;
    private Integer sterilized;
    private Integer vaccinated;
    private Integer dewormed;
    private String description;
    private String coverImage;
    private Integer viewCount;
    private LocalDateTime createTime;

    /** 诊疗记录，详情接口才返回 */
    private List<AnimalMedical> medicalList;
}
