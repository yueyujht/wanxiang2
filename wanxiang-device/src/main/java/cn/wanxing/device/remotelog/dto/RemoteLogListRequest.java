package cn.wanxing.device.remotelog.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 查询设备可上传日志文件列表请求
 */
@Getter
@Setter
public class RemoteLogListRequest {

    /** 日志所属模块过滤：0 飞行器 / 3 机场 */
    private List<String> moduleList;
}