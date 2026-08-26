package cn.wanxing.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色权限 Mapper
 */
@Mapper
public interface RolePermissionMapper {

    /**
     * 查某个角色的权限点列表
     */
    @Select("SELECT permission FROM sys_role_permission WHERE role_id = #{roleId}")
    List<String> selectPermissionsByRoleId(@Param("roleId") Long roleId);
}
