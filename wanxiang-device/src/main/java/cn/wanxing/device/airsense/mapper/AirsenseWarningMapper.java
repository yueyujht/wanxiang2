package cn.wanxing.device.airsense.mapper;

import cn.wanxing.device.airsense.entity.AirsenseWarning;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AirSense 空域告警 Mapper
 */
@Mapper
public interface AirsenseWarningMapper extends BaseMapper<AirsenseWarning> {
}
