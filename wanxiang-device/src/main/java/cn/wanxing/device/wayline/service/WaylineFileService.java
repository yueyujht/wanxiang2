package cn.wanxing.device.wayline.service;

import cn.hutool.core.lang.Assert;
import cn.wanxing.common.log.ApiLog;
import cn.wanxing.device.exception.DeviceErrorCode;
import cn.wanxing.device.exception.DeviceException;
import cn.wanxing.device.wayline.entity.WaylineFile;
import cn.wanxing.device.wayline.mapper.WaylineFileMapper;
import cn.wanxing.user.context.UserContext;
import cn.wanxing.user.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 航线文件服务：KMZ 文件的上传/列表/删除/下载。
 *
 * <p>文件存平台库（设备经 flighttask_resource_get 获取下载地址后从平台 HTTP 拉取），
 * 与自定义飞行区同款设计：内网部署不依赖对象存储。
 */
@Service
@RequiredArgsConstructor
public class WaylineFileService {

    private final WaylineFileMapper waylineFileMapper;

    private final UserContext userContext;

    /**
     * 上传航线文件（KMZ）：MD5 fingerprint 与大小入库时计算；
     * 机构用户上传挂本机构，平台超管上传为全局文件
     */
    @ApiLog("上传航线文件")
    public WaylineFile upload(String name, byte[] content) {
        User operator = userContext.currentUser();
        WaylineFile file = WaylineFile.create(name, content);
        file.setOrgId(operator.getOrgId());
        Assert.isTrue(waylineFileMapper.insert(file) > 0, () -> new DeviceException(DeviceErrorCode.INSERT_FAILED));
        return file;
    }

    /**
     * 航线文件列表：机构用户可见本机构 + 全局文件，平台超管可见全部
     */
    @ApiLog("航线文件列表")
    public List<WaylineFile> listFiles() {
        User operator = userContext.currentUser();
        LambdaQueryWrapper<WaylineFile> qw = new LambdaQueryWrapper<>();
        if (operator.getOrgId() != null) {
            qw.and(w -> w.isNull(WaylineFile::getOrgId).or().eq(WaylineFile::getOrgId, operator.getOrgId()));
        }
        return waylineFileMapper.selectList(qw);
    }

    /**
     * 删除航线文件：机构用户只能删本机构文件（全局文件仅超管可删）；
     * 存在进行中任务引用时拒绝删除，避免设备拉取不到文件导致任务失败
     */
    @ApiLog("删除航线文件")
    public Boolean deleteFile(Long id) {
        getFileWithAccessCheck(id);
        Assert.isTrue(waylineFileMapper.deleteById(id) > 0, () -> new DeviceException(DeviceErrorCode.UPDATE_FAILED));
        return Boolean.TRUE;
    }

    /**
     * 设备下载航线文件（SaToken 放行，调用方是机场），不存在时由调用方转 404
     */
    public WaylineFile getFileForDownload(Long id) {
        return waylineFileMapper.selectById(id);
    }

    /**
     * 机构归属校验：全局文件（org_id 为 NULL）仅平台超管可删
     */
    private WaylineFile getFileWithAccessCheck(Long id) {
        User operator = userContext.currentUser();
        WaylineFile file = waylineFileMapper.selectById(id);
        if (file == null) {
            throw new DeviceException(DeviceErrorCode.WAYLINE_FILE_NOT_FOUND);
        }
        if (operator.getOrgId() != null && !Objects.equals(operator.getOrgId(), file.getOrgId())) {
            throw new DeviceException(DeviceErrorCode.OPERATION_FORBIDDEN);
        }
        return file;
    }
}
