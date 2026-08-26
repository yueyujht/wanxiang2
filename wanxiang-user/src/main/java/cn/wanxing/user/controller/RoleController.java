package cn.wanxing.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.result.Result;
import cn.wanxing.user.dto.vo.RoleVO;
import cn.wanxing.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色接口
 */
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 角色列表
     */
    @SaCheckPermission(PermissionConst.ROLE_READ)
    @GetMapping("/list")
    public Result<List<RoleVO>> list() {
        return Result.success(roleService.list());
    }
}
