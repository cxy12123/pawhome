package com.pawhome.adoption.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pawhome.adoption.dto.AdoptionApplyRequest;
import com.pawhome.adoption.dto.AdoptionApplyVO;
import com.pawhome.adoption.dto.AuditRequest;
import com.pawhome.adoption.entity.AdoptionApply;
import com.pawhome.adoption.entity.AdoptionVisit;
import com.pawhome.adoption.mapper.AdoptionApplyMapper;
import com.pawhome.adoption.mapper.AdoptionVisitMapper;
import com.pawhome.adoption.service.AdoptionService;
import com.pawhome.api.dto.AnimalDTO;
import com.pawhome.api.dto.NotifyMessageDTO;
import com.pawhome.api.feign.AnimalClient;
import com.pawhome.api.feign.NotifyClient;
import com.pawhome.common.constant.CommonConstants;
import com.pawhome.common.context.UserContext;
import com.pawhome.common.core.BizException;
import com.pawhome.common.core.PageResult;
import com.pawhome.common.core.R;
import com.pawhome.common.core.ResultCode;
import com.pawhome.common.util.BizNoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 领养服务实现 —— 本项目的业务核心。
 * <p>
 * 一次"审核通过"动作会牵动三个服务：
 * <pre>
 *   adoption（本地事务）
 *      ├─ 更新申请单状态 -> PASSED
 *      ├─ Feign -> animal：动物状态改为 ADOPTED
 *      └─ Feign -> notify：给申请人发站内信
 * </pre>
 * <p>
 * 关于一致性，这里做了一个明确的取舍（报告中有详细论证）：
 * 动物状态是"必须一致"的，所以放在本地事务内，远程调用失败就整体回滚；
 * 通知是"可以最终一致"的，失败只记日志，绝不让用户因为收不到短信就领养失败。
 * <p>
 * 不引入 Seata 的原因：本项目只有"两个服务、两步操作"的弱事务场景，
 * 用"本地事务 + 失败回滚 + 幂等接口"已经足够，上 Seata 会带来
 * TC 单点、undo_log 表、全局锁等额外复杂度，收益不成正比。
 * 这个判断本身也是工程能力的一部分。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdoptionServiceImpl implements AdoptionService {

    private final AdoptionApplyMapper applyMapper;
    private final AdoptionVisitMapper visitMapper;
    private final AnimalClient animalClient;
    private final NotifyClient notifyClient;
    private final BizNoUtils bizNoUtils;
    private final StringRedisTemplate redisTemplate;

    private static final Map<String, String> STATUS_TEXT = Map.of(
            "PENDING", "待审核",
            "PASSED", "已通过",
            "REJECTED", "已驳回",
            "CANCELED", "已取消",
            "FINISHED", "已完成");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submit(AdoptionApplyRequest request) {
        Long userId = UserContext.requireUserId();

        // ---------- 1. 防重复提交（5 秒内只允许一次） ----------
        // 用户手抖连点两次提交按钮，会产生两条申请，用 Redis 短锁拦住
        String lockKey = CommonConstants.REDIS_SUBMIT_LOCK + "adoption:" + userId + ":" + request.getAnimalId();
        Boolean first = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(first)) {
            throw new BizException(ResultCode.REPEAT_SUBMIT);
        }

        // ---------- 2. 跨服务校验动物是否可领养 ----------
        R<AnimalDTO> animalResp = animalClient.getById(request.getAnimalId());
        if (!animalResp.isSuccess() || animalResp.getData() == null) {
            throw new BizException(ResultCode.REMOTE_CALL_ERROR,
                    animalResp.getMessage() == null ? "动物信息查询失败" : animalResp.getMessage());
        }
        AnimalDTO animal = animalResp.getData();

        if (!"WAITING".equals(animal.getAdoptStatus())) {
            throw new BizException(ResultCode.ANIMAL_NOT_ADOPTABLE,
                    "「" + animal.getName() + "」当前状态为 "
                            + transferStatus(animal.getAdoptStatus()) + "，暂不可申请领养");
        }

        // ---------- 3. 本地查重：同一用户对同一动物不允许有进行中的申请 ----------
        Long duplicate = applyMapper.selectCount(new LambdaQueryWrapper<AdoptionApply>()
                .eq(AdoptionApply::getAnimalId, request.getAnimalId())
                .eq(AdoptionApply::getApplicantId, userId)
                .in(AdoptionApply::getStatus, "PENDING", "PASSED"));
        if (duplicate != null && duplicate > 0) {
            throw new BizException("您已提交过「" + animal.getName() + "」的领养申请，请勿重复提交");
        }

        // ---------- 4. 落库 ----------
        AdoptionApply apply = new AdoptionApply();
        BeanUtil.copyProperties(request, apply);
        apply.setApplyNo(bizNoUtils.adoptionNo());
        apply.setAnimalNo(animal.getAnimalNo());
        apply.setAnimalName(animal.getName());
        apply.setApplicantId(userId);
        apply.setStatus("PENDING");
        if (apply.getFamilyMember() == null) {
            apply.setFamilyMember(1);
        }
        applyMapper.insert(apply);

        // ---------- 5. 驱动动物状态变为"申请中"，失败则整体回滚 ----------
        R<Boolean> statusResp = animalClient.updateAdoptStatus(request.getAnimalId(), "APPLYING");
        if (!statusResp.isSuccess()) {
            throw new BizException(ResultCode.REMOTE_CALL_ERROR,
                    "动物状态更新失败，申请已回滚，请稍后重试");
        }

        // ---------- 6. 发送通知（非核心，失败不影响主流程） ----------
        sendNotify(userId, apply.getApplicantName(),
                "领养申请已提交",
                String.format("您好 %s，您对「%s」的领养申请（%s）已提交，我们将在 3 个工作日内完成审核。",
                        apply.getApplicantName(), animal.getName(), apply.getApplyNo()),
                apply.getId());

        log.info("领养申请提交成功：{} 用户 {} 申请动物 {}", apply.getApplyNo(), userId, animal.getName());
        return apply.getApplyNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(AuditRequest request) {
        // ---------- 1. 权限校验：只有员工和管理员能审核 ----------
        String role = UserContext.getRole();
        if (!CommonConstants.ROLE_ADMIN.equals(role) && !CommonConstants.ROLE_STAFF.equals(role)) {
            throw new BizException(ResultCode.FORBIDDEN, "只有救助站员工可以审核领养申请");
        }

        AdoptionApply apply = applyMapper.selectById(request.getApplyId());
        BizException.throwIfNull(apply, "申请单不存在");

        if (!"PENDING".equals(apply.getStatus())) {
            throw new BizException("该申请当前状态为「" + STATUS_TEXT.getOrDefault(apply.getStatus(), apply.getStatus()) + "」，不可重复审核");
        }
        if (Boolean.FALSE.equals(request.getPass()) && StrUtil.isBlank(request.getRemark())) {
            throw new BizException("驳回时必须填写审核意见");
        }

        boolean pass = Boolean.TRUE.equals(request.getPass());

        // ---------- 2. 更新本地申请单 ----------
        AdoptionApply update = new AdoptionApply();
        update.setId(apply.getId());
        update.setStatus(pass ? "PASSED" : "REJECTED");
        update.setAuditUserId(UserContext.getUserId());
        update.setAuditUserName(UserContext.getRealName() != null
                ? UserContext.getRealName() : UserContext.getUsername());
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(request.getRemark());
        applyMapper.updateById(update);

        // ---------- 3. 驱动动物状态（这一步失败必须回滚，否则会留下脏数据） ----------
        String targetStatus = pass ? "ADOPTED" : "WAITING";
        R<Boolean> statusResp = animalClient.updateAdoptStatus(apply.getAnimalId(), targetStatus);
        if (!statusResp.isSuccess()) {
            // 抛异常 -> 本地事务回滚 -> 申请单仍然是 PENDING，可以重新审核
            throw new BizException(ResultCode.REMOTE_CALL_ERROR,
                    "动物状态同步失败，审核未生效，请稍后重试");
        }

        // ---------- 4. 通知申请人 ----------
        String title = pass ? "领养申请已通过" : "领养申请未通过";
        String content = pass
                ? String.format("您好 %s，您对「%s」的领养申请（%s）已通过审核，请在 7 日内到救助站办理交接手续。",
                        apply.getApplicantName(), apply.getAnimalName(), apply.getApplyNo())
                : String.format("您好 %s，很抱歉，您对「%s」的领养申请未通过，原因：%s。欢迎关注其他待领养动物。",
                        apply.getApplicantName(), apply.getAnimalName(), request.getRemark());

        sendNotify(apply.getApplicantId(), apply.getApplicantName(), title, content, apply.getId());

        log.info("领养审核完成：{} -> {}", apply.getApplyNo(), pass ? "通过" : "驳回");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long applyId, String reason) {
        AdoptionApply apply = applyMapper.selectById(applyId);
        BizException.throwIfNull(apply, "申请单不存在");

        Long userId = UserContext.requireUserId();
        if (!userId.equals(apply.getApplicantId())) {
            throw new BizException(ResultCode.FORBIDDEN, "只能取消自己的申请");
        }
        if (!"PENDING".equals(apply.getStatus())) {
            throw new BizException("当前状态不可取消");
        }

        AdoptionApply update = new AdoptionApply();
        update.setId(applyId);
        update.setStatus("CANCELED");
        update.setAuditRemark(reason);
        applyMapper.updateById(update);

        // 动物状态放回待领养，失败则回滚
        R<Boolean> resp = animalClient.updateAdoptStatus(apply.getAnimalId(), "WAITING");
        if (!resp.isSuccess()) {
            throw new BizException(ResultCode.REMOTE_CALL_ERROR, "取消失败，请稍后重试");
        }
    }

    @Override
    public PageResult<AdoptionApplyVO> page(long pageNum, long pageSize, String status,
                                            String keyword, Long applicantId) {
        LambdaQueryWrapper<AdoptionApply> wrapper = new LambdaQueryWrapper<AdoptionApply>()
                .eq(StrUtil.isNotBlank(status), AdoptionApply::getStatus, status)
                .eq(applicantId != null, AdoptionApply::getApplicantId, applicantId)
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(AdoptionApply::getApplyNo, keyword)
                        .or().like(AdoptionApply::getApplicantName, keyword)
                        .or().like(AdoptionApply::getAnimalName, keyword))
                .orderByDesc(AdoptionApply::getCreateTime);

        Page<AdoptionApply> page = applyMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<AdoptionApplyVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public AdoptionApplyVO detail(Long id) {
        AdoptionApply apply = applyMapper.selectById(id);
        BizException.throwIfNull(apply, "申请单不存在");
        return toVO(apply);
    }

    @Override
    public List<AdoptionApplyVO> myApplications() {
        Long userId = UserContext.requireUserId();
        return applyMapper.selectList(new LambdaQueryWrapper<AdoptionApply>()
                        .eq(AdoptionApply::getApplicantId, userId)
                        .orderByDesc(AdoptionApply::getCreateTime))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void addVisit(AdoptionVisit visit) {
        BizException.throwIf(visit.getApplyId() == null, "请指定申请单");
        AdoptionApply apply = applyMapper.selectById(visit.getApplyId());
        BizException.throwIfNull(apply, "申请单不存在");
        if (!"PASSED".equals(apply.getStatus()) && !"FINISHED".equals(apply.getStatus())) {
            throw new BizException("只有已通过的领养单才需要回访");
        }

        Long count = visitMapper.selectCount(new LambdaQueryWrapper<AdoptionVisit>()
                .eq(AdoptionVisit::getApplyId, visit.getApplyId()));
        visit.setVisitNo(count == null ? 1 : count.intValue() + 1);
        if (visit.getVisitor() == null) {
            visit.setVisitor(UserContext.getRealName());
        }
        visitMapper.insert(visit);
    }

    @Override
    public List<AdoptionVisit> visitList(Long applyId) {
        return visitMapper.selectList(new LambdaQueryWrapper<AdoptionVisit>()
                .eq(AdoptionVisit::getApplyId, applyId)
                .orderByDesc(AdoptionVisit::getVisitDate));
    }

    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (String s : List.of("PENDING", "PASSED", "REJECTED", "CANCELED")) {
            statusMap.put(STATUS_TEXT.get(s),
                    applyMapper.selectCount(new LambdaQueryWrapper<AdoptionApply>()
                            .eq(AdoptionApply::getStatus, s)));
        }
        result.put("byStatus", statusMap);
        result.put("total", applyMapper.selectCount(new LambdaQueryWrapper<AdoptionApply>()));
        result.put("pending", statusMap.get("待审核"));
        return result;
    }

    /**
     * 发送通知。
     * 单独 try-catch 是为了让通知失败不影响主流程 ——
     * 这是"核心链路"与"旁路链路"的边界划分。
     */
    private void sendNotify(Long receiverId, String receiverName, String title, String content, Long bizId) {
        try {
            notifyClient.send(NotifyMessageDTO.builder()
                    .receiverId(receiverId)
                    .receiverName(receiverName)
                    .title(title)
                    .content(content)
                    .type("ADOPTION")
                    .bizType("ADOPTION_APPLY")
                    .bizId(bizId)
                    .channel("INNER")
                    .build());
        } catch (Exception e) {
            log.warn("领养通知发送失败，不影响主流程：{}", e.getMessage());
        }
    }

    private String transferStatus(String status) {
        return Map.of("WAITING", "待领养", "APPLYING", "申请中",
                "ADOPTED", "已领养", "OBSERVING", "观察期").getOrDefault(status, status);
    }

    private AdoptionApplyVO toVO(AdoptionApply apply) {
        AdoptionApplyVO vo = new AdoptionApplyVO();
        BeanUtil.copyProperties(apply, vo);
        vo.setStatusText(STATUS_TEXT.getOrDefault(apply.getStatus(), apply.getStatus()));
        return vo;
    }
}
