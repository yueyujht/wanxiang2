package cn.wanxing.device.firmware.mapper;

import cn.wanxing.device.firmware.entity.FirmwareTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 固件升级任务 Mapper
 */
@Mapper
public interface FirmwareTaskMapper extends BaseMapper<FirmwareTask> {
}