package com.pawhome.animal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pawhome.animal.entity.Animal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnimalMapper extends BaseMapper<Animal> {

    /**
     * 按物种统计。
     * <p>
     * 注意：自己写的 SQL 不会被 MyBatis-Plus 自动加逻辑删除条件，
     * 必须手动写 deleted = 0，否则会把已删除的动物算进去。
     * 这是使用逻辑删除时最常见的坑。
     */
    @Select("SELECT species, COUNT(*) AS total FROM t_animal WHERE deleted = 0 GROUP BY species")
    List<Map<String, Object>> countBySpecies();

    /** 按领养状态统计 */
    @Select("SELECT adopt_status AS adoptStatus, COUNT(*) AS total FROM t_animal WHERE deleted = 0 GROUP BY adopt_status")
    List<Map<String, Object>> countByAdoptStatus();

    /** 原子累加浏览量，避免并发下读-改-写丢失更新 */
    @org.apache.ibatis.annotations.Update("UPDATE t_animal SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int increaseView(Long id);
}
