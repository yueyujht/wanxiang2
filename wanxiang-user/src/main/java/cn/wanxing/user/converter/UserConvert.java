package cn.wanxing.user.converter;

import cn.wanxing.user.dto.vo.UserVO;
import cn.wanxing.user.entity.Org;
import cn.wanxing.user.entity.Role;
import cn.wanxing.user.entity.User;

/**
 * 用户实体 → VO 转换
 */
public final class UserConvert {

    private UserConvert() {
    }

    /**
     * 组装用户 VO，角色/机构信息由调用方传入（避免重复查询）
     */
    public static UserVO toVO(User user, Role role, Org org) {
        return UserVO.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .roleName(role != null ? role.getName() : null)
                .orgId(user.getOrgId())
                .orgName(org != null ? org.getName() : null)
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
