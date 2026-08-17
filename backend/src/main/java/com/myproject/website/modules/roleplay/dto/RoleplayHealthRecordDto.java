package com.myproject.website.modules.roleplay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.myproject.website.modules.roleplay.entity.RoleplayHealthRecordEntity;
import lombok.Data;

@Data
public class RoleplayHealthRecordDto {

    private Integer day;
    private Integer cal;
    private Integer heart;
    private Integer count;
    private Integer duration;

    @JsonProperty("trigger")
    private String trigger;

    private String scene;
    private String thought;

    public static RoleplayHealthRecordDto from(RoleplayHealthRecordEntity entity) {
        RoleplayHealthRecordDto dto = new RoleplayHealthRecordDto();
        dto.setDay(entity.getDay());
        dto.setCal(entity.getCal());
        dto.setHeart(entity.getHeart());
        dto.setCount(entity.getCount());
        dto.setDuration(entity.getDuration());
        dto.setTrigger(entity.getTriggerText());
        dto.setScene(entity.getScene());
        dto.setThought(entity.getThought());
        return dto;
    }
}
