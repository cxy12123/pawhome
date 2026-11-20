package com.pawhome.volunteer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pawhome.volunteer.entity.Volunteer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VolunteerMapper extends BaseMapper<Volunteer> {
}
