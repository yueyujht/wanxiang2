package cn.wanxing.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.wanxing.common.constant.PermissionConst;
import cn.wanxing.common.request.PageRequest;
import cn.wanxing.common.result.MultiResult;
import cn.wanxing.common.result.Result;
import cn.wanxing.user.dto.request.AssignRoleRequest;
import cn.wanxing.user.dto.request.ChangeStatusRequest;
import cn.wanxing.user.dto.request.CreateUserRequest;
import cn.wanxing.user.dto.response.UserPageResponse;
import cn.wanxing.user.dto.vo.UserVO;
import cn.wanxing.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户分页列表（机构管理员只见本机构，超管可传 orgId 过滤）
     */
    @SaCheckPermission(PermissionConst.USER_READ)
    @GetMapping("/list")
    public MultiResult<UserVO> list(@Valid PageRequest req,
                                    @RequestParam(required = false) Long orgId) {
        UserPageResponse<UserVO> pageResponse = userService.page(req, orgId);
        return MultiResult.successMulti(pageResponse.getDatas(),pageResponse.getTotal(),pageResponse.getCurrentPage(),pageResponse.getPageSize());
    }

    /**
     * 后台建账号（录入手机号）
     */
    @SaCheckPermission(PermissionConst.USER_CREATE)
    @PostMapping("/create")
    public Result<UserVO> create(@Valid @RequestBody CreateUserRequest req) {
        log.info("初始化账号，手机号{}，角色{}，昵称{}", req.getPhone(),req.getRole(),req.getNickname());
        UserVO userVO = userService.create(req);
        return Result.success(userVO);
    }

    /**
     * 分配角色
     */
    @SaCheckPermission(PermissionConst.USER_UPDATE)
    @PutMapping("/{id}/role")
    public Result<Boolean> assignRole(@PathVariable Long id, @Valid @RequestBody AssignRoleRequest req) {
        Boolean response = userService.assignRole(id, req.getRole());
        return Result.success(response);
    }

    /**
     * 修改状态（启用/禁用/锁定）
     */
    @SaCheckPermission(PermissionConst.USER_DISABLE)
    @PutMapping("/{id}/status")
    public Result<Boolean> changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest req) {
        Boolean response = userService.changeStatus(id, req.getStatus());
        return Result.success(response);
    }
}
