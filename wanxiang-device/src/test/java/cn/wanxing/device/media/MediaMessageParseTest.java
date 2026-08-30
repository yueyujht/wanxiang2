package cn.wanxing.device.media;

import cn.wanxing.device.media.entity.MediaFile;
import cn.wanxing.device.media.message.MediaFileInfo;
import cn.wanxing.device.media.message.MediaUploadMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 媒体上传消息解析测试：报文结构取自官方文档 file_upload_callback 示例
 */
class MediaMessageParseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 官方文档示例报文（dock file.md，file_upload_callback） */
    private static final String OFFICIAL_EXAMPLE = """
            {
                "bid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
                "data": {
                    "file": {
                        "cloud_to_cloud_id": "DEFAULT",
                        "ext": {
                            "drone_model_key": "0-67-0",
                            "flight_id": "xxx",
                            "is_original": true,
                            "payload_model_key": "0-67-0"
                        },
                        "metadata": {
                            "absolute_altitude": 56.311,
                            "create_time": "2021-05-10 16:04:20",
                            "gimbal_yaw_degree": "-91.40",
                            "relative_altitude": 41.124,
                            "shoot_position": {
                                "lat": 22.1,
                                "lng": 122.5
                            }
                        },
                        "name": "dog.jpeg",
                        "object_key": "object_key",
                        "path": "xxx"
                    },
                    "flight_task": {
                        "expected_file_count": 14,
                        "flight_type": 0,
                        "uploaded_file_count": 12
                    }
                },
                "gateway": "xxx",
                "method": "file_upload_callback",
                "need_reply": 1,
                "tid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
                "timestamp": 1654070968655
            }
            """;

    @Test
    void parseOfficialExample() throws Exception {
        MediaUploadMessage message = objectMapper.readValue(OFFICIAL_EXAMPLE, MediaUploadMessage.class);
        assertEquals("file_upload_callback", message.getMethod());
        assertEquals(1, message.getNeedReply());
        assertEquals("dog.jpeg", message.getData().getFile().getName());
        assertEquals("xxx", message.getData().getFile().getExt().getFlightId());
        assertTrue(message.getData().getFile().getExt().getIsOriginal());
        assertEquals(22.1, message.getData().getFile().getMetadata().getShootPosition().getLat());
        assertEquals(14, message.getData().getFlightTask().getExpectedFileCount());
    }

    @Test
    void entityMappingFromOfficialExample() throws Exception {
        MediaUploadMessage message = objectMapper.readValue(OFFICIAL_EXAMPLE, MediaUploadMessage.class);
        MediaFile media = MediaFile.create("DOCK001", message.getData().getFile(), message.getData().getFlightTask());

        assertEquals("DOCK001", media.getDeviceSn());
        assertEquals("object_key", media.getObjectKey());
        assertEquals("dog.jpeg", media.getFileName());
        assertEquals("0-67-0", media.getDroneModelKey());
        assertEquals(Boolean.TRUE, media.getIsOriginal());
        assertEquals(-91.40, media.getGimbalYaw(), 0.001);
        assertEquals(56.311, media.getAbsoluteAltitude(), 0.001);
        assertEquals(22.1, media.getShootLat(), 0.001);
        assertEquals(LocalDateTime.of(2021, 5, 10, 16, 4, 20), media.getShootTime());
        assertEquals(12, media.getUploadedCount());
        assertEquals(14, media.getExpectedCount());
        assertEquals(0, media.getFlightType());
    }

    @Test
    void shootTimeFallbacks() {
        // ISO8601 兜底可解析
        assertEquals(LocalDateTime.of(2021, 5, 10, 16, 4, 20),
                MediaFile.create("sn", fileWithCreateTime("2021-05-10T16:04:20"), null).getShootTime());
        // 无法解析时不抛异常、不丢记录
        assertNull(MediaFile.create("sn", fileWithCreateTime("garbage"), null).getShootTime());
        assertNull(MediaFile.create("sn", fileWithCreateTime(null), null).getShootTime());
    }

    private MediaFileInfo fileWithCreateTime(String createTime) {
        MediaFileInfo file = new MediaFileInfo();
        file.setObjectKey("k");
        MediaFileInfo.Metadata metadata = new MediaFileInfo.Metadata();
        metadata.setCreateTime(createTime);
        file.setMetadata(metadata);
        return file;
    }
}
