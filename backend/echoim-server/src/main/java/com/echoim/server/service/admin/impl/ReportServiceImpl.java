package com.echoim.server.service.admin.impl;

import com.echoim.server.common.PageResponse;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.entity.ImReportEntity;
import com.echoim.server.entity.ImUserEntity;
import com.echoim.server.mapper.ImReportMapper;
import com.echoim.server.mapper.ImUserMapper;
import com.echoim.server.service.admin.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    private final ImReportMapper reportMapper;
    private final ImUserMapper userMapper;

    public ReportServiceImpl(ImReportMapper reportMapper, ImUserMapper userMapper) {
        this.reportMapper = reportMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public void submitReport(Long reporterUserId, Integer targetType, Long targetId, String reason, String description) {
        if (targetType == null || (targetType != 1 && targetType != 2)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "举报目标类型无效");
        }
        if (targetId == null || targetId <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "举报目标无效");
        }
        if (reason == null || reason.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择举报原因");
        }
        if (targetType == 2) {
            ImUserEntity targetUser = userMapper.selectById(targetId);
            if (targetUser == null) {
                throw new BizException(ErrorCode.USER_NOT_FOUND, "被举报用户不存在");
            }
            if (targetUser.getId().equals(reporterUserId)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "不能举报自己");
            }
        }

        ImReportEntity report = new ImReportEntity();
        report.setReporterUserId(reporterUserId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setDescription(description);
        report.setStatus(0);
        reportMapper.insert(report);
    }

    @Override
    public Map<String, Object> listReports(Integer status, long pageNo, long pageSize) {
        long offset = (pageNo - 1) * pageSize;
        var list = reportMapper.selectReportPage(status, offset, pageSize);
        long total = reportMapper.countReports(status);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        result.put("total", total);
        return result;
    }

    @Override
    @Transactional
    public void handleReport(Long reportId, Long adminUserId, Integer action, String remark) {
        ImReportEntity report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "举报记录不存在");
        }
        if (report.getStatus() != 0) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "该举报已处理");
        }
        // action: 1=忽略, 2=警告, 3=禁言, 4=封号
        if (action == null || action < 1 || action > 4) {
            throw new BizException(ErrorCode.PARAM_ERROR, "处理操作无效");
        }

        report.setStatus(action);
        report.setHandledBy(adminUserId);
        report.setHandledAt(LocalDateTime.now());
        report.setHandleRemark(remark);
        reportMapper.updateById(report);

        // Apply action to the target user if applicable
        if (report.getTargetType() == 2 && action >= 3) {
            ImUserEntity targetUser = userMapper.selectById(report.getTargetId());
            if (targetUser != null) {
                if (action == 4) {
                    targetUser.setStatus(2); // disable
                    userMapper.updateById(targetUser);
                }
            }
        }
    }
}
