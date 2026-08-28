package cn.wanxing.device.remotelog.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 发起日志文件上传请求
 */
@Getter
@Setter
public class RemoteLogUploadRequest {

    /** 要上传的文件列表 */
    private List<UploadFile> files;

    @Getter
    @Setter
    public static class UploadFile {
        /** 日志所属模块：0 飞行器 / 3 机场 */
        private String module;

        /** 文件索引（boot_index） */
        private Integer bootIndex;
    }
}
