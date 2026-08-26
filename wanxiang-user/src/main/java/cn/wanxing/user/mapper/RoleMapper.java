package cn.wanxing.user.mapper;

import cn.wanxing.user.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 角色 Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 按角色编码 + 机构解析角色：全局角色（org_id 为 NULL）对所有机构可见，或本机构自定义角色
     */
    @Select("SELECT * FROM sys_role WHERE code = #{code} AND (org_id IS NULL OR org_id = #{orgId}) LIMIT 1")
    Role selectByCodeAndOrg(@Param("code") String code, @Param("orgId") Long orgId);
}
