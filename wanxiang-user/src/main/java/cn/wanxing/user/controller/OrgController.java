package cn.wanxing.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.Result;
import cn.wanxing.user.dto.request.CreateOrgRequest;
import cn.wanxing.user.dto.vo.OrgVO;
import cn.wanxing.user.service.OrgService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 机构（租户）管理接口（仅平台超管）
 */
@RestController
@RequestMapping("/org")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    /**
     * 机构列表
     */
    @SaCheckPermission(PermissionConst.ORG_READ)
    @GetMapping("/list")
    public Result<List<OrgVO>> list() {
        return Result.success(orgService.list());
    }

    /**
     * 查询机构信息
     */
    @SaCheckPermission(PermissionConst.ORG_READ)
    @GetMapping("/{id}")
    public Result<OrgVO> getById(@PathVariable Long id) {
        return Result.success(orgService.getById(id));
    }

    /**
     * 创建机构，返回新机构 id
     */
    @SaCheckPermission(PermissionConst.ORG_CREATE)
    @PostMapping("/create")
    public Result<OrgVO> create(@Valid @RequestBody CreateOrgRequest req) {
        return Result.success(orgService.create(req));
    }

    /**
     * 更新机构
     */
    @SaCheckPermission(PermissionConst.ORG_UPDATE)
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody CreateOrgRequest req) {
        Boolean response = orgService.update(id, req);
        return Result.success(response);
    }
}
