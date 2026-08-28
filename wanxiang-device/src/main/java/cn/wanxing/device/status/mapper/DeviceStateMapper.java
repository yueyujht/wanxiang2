package cn.wanxing.device.status.mapper;

import cn.wanxing.device.status.entity.DeviceState;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备状态 Mapper
 */
@Mapper
public interface DeviceStateMapper extends BaseMapper<DeviceState> {

    /**
     * UPSERT：插入或更新设备最新状态（device_sn 唯一，冲突则更新覆盖）
     */
    @Insert("INSERT INTO sys_device_state (device_sn, state_json, firmware_version, alarm_state, drone_in_dock, cover_state, mode_code, firmware_upgrade_status, air_transfer_enable, silent_mode, user_experience_improvement, updated_at) "
            + "VALUES (#{deviceSn}, #{stateJson}, #{firmwareVersion}, #{alarmState}, #{droneInDock}, #{coverState}, #{modeCode}, #{firmwareUpgradeStatus}, #{airTransferEnable}, #{silentMode}, #{userExperienceImprovement}, NOW()) "
            + "ON DUPLICATE KEY UPDATE state_json = #{stateJson}, "
            + "firmware_version = COALESCE(#{firmwareVersion}, firmware_version), "
            + "alarm_state = COALESCE(#{alarmState}, alarm_state), "
            + "drone_in_dock = COALESCE(#{droneInDock}, drone_in_dock), "
            + "cover_state = COALESCE(#{coverState}, cover_state), "
            + "mode_code = COALESCE(#{modeCode}, mode_code), "
            + "firmware_upgrade_status = COALESCE(#{firmwareUpgradeStatus}, firmware_upgrade_status), "
            + "air_transfer_enable = COALESCE(#{airTransferEnable}, air_transfer_enable), "
            + "silent_mode = COALESCE(#{silentMode}, silent_mode), "
            + "user_experience_improvement = COALESCE(#{userExperienceImprovement}, user_experience_improvement), "
            + "updated_at = NOW()")
    void upsert(DeviceState state);
}