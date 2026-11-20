package com.pawhome.adoption.service;

import com.pawhome.adoption.dto.AdoptionApplyRequest;
import com.pawhome.adoption.dto.AdoptionApplyVO;
import com.pawhome.adoption.dto.AuditRequest;
import com.pawhome.adoption.entity.AdoptionVisit;
import com.pawhome.common.core.PageResult;

import java.util.List;
import java.util.Map;

public interface AdoptionService {

    /** 提交领养申请（跨服务校验动物状态） */
    String submit(AdoptionApplyRequest request);

    /** 审核（通过则驱动动物状态变更） */
    void audit(AuditRequest request);

    /** 申请人取消 */
    void cancel(Long applyId, String reason);

    /** 分页查询 */
    PageResult<AdoptionApplyVO> page(long pageNum, long pageSize, String status, String keyword, Long applicantId);

    /** 详情 */
    AdoptionApplyVO detail(Long id);

    /** 我的申请 */
    List<AdoptionApplyVO> myApplications();

    /** 新增回访记录 */
    void addVisit(AdoptionVisit visit);

    /** 回访列表 */
    List<AdoptionVisit> visitList(Long applyId);

    /** 统计 */
    Map<String, Object> statistics();
}
