package com.pawhome.animal.service;

import com.pawhome.animal.dto.AnimalSaveRequest;
import com.pawhome.animal.dto.AnimalVO;
import com.pawhome.common.core.PageResult;

import java.util.List;
import java.util.Map;

public interface AnimalService {

    /** 分页查询（支持关键字、物种、领养状态、健康状态过滤） */
    PageResult<AnimalVO> page(long pageNum, long pageSize, String keyword,
                              String species, String adoptStatus, String healthStatus);

    /** 详情（走 Redis 缓存） */
    AnimalVO detail(Long id, boolean withMedical);

    /** 建档 */
    Long create(AnimalSaveRequest request);

    /** 修改 */
    void update(AnimalSaveRequest request);

    /** 删除（逻辑删除） */
    void delete(Long id);

    /** 更新领养状态（供领养服务 Feign 调用） */
    boolean updateAdoptStatus(Long id, String adoptStatus);

    /** 浏览量 +1 */
    void increaseView(Long id);

    /** 可领养动物列表（公开接口，给领养页用） */
    List<AnimalVO> listAdoptable(int limit);

    /**
     * 在站待领养动物（含治疗中 / 康复中 / 残疾），供领养门户展示。
     *
     * <p>与 {@link #listAdoptable(int)} 的区别：不做健康状态过滤。
     * 这样门户的"待领养"数量与后台统计口径一致，
     * 由前端根据 healthStatus 决定该动物能否立即提交申请。
     */
    List<AnimalVO> listWaiting(int limit);

    /** 统计看板数据 */
    Map<String, Object> statistics();
}
