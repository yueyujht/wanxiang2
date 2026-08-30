package cn.wanxing.device.flightarea;

import cn.wanxing.device.flightarea.entity.FlightAreaFile;
import cn.wanxing.device.flightarea.message.FlightAreasDroneLocationMessage;
import cn.wanxing.device.flightarea.message.FlightAreasSyncProgressMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自定义飞行区消息解析测试：报文结构取自官方文档
 */
class FlightAreaMessageParseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseSyncProgressOfficialExample() throws Exception {
        // 官方文档 flight_areas_sync_progress 示例
        String json = """
                {
                    "bid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
                    "data": {
                        "file": { "checksum": "sha256", "name": "geofence_xxx.json" },
                        "reason": 0,
                        "status": "synchronized"
                    },
                    "method": "flight_areas_sync_progress",
                    "need_reply": 1,
                    "tid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
                    "timestamp": 16540709686556
                }
                """;
        FlightAreasSyncProgressMessage message = objectMapper.readValue(json, FlightAreasSyncProgressMessage.class);
        assertEquals("synchronized", message.getData().getStatus());
        assertEquals(0, message.getData().getReason());
        assertEquals("geofence_xxx.json", message.getData().getFile().getName());
        assertEquals("sha256", message.getData().getFile().getChecksum());
        assertEquals(1, message.getNeedReply());
    }

    @Test
    void parseDroneLocationOfficialExample() throws Exception {
        // 官方文档 flight_areas_drone_location 示例
        String json = """
                {
                    "bid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
                    "data": {
                        "drone_locations": [
                            { "area_distance": 100.11, "area_id": "d275c4e1-d864-4736-8b5d-5f5882ee9bdd", "is_in_area": true }
                        ]
                    },
                    "method": "flight_areas_drone_location",
                    "need_reply": 0,
                    "tid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
                    "timestamp": 16540709686556
                }
                """;
        FlightAreasDroneLocationMessage message = objectMapper.readValue(json, FlightAreasDroneLocationMessage.class);
        assertEquals("flight_areas_drone_location", message.getMethod());
        assertEquals(1, message.getData().getDroneLocations().size());
        assertEquals("d275c4e1-d864-4736-8b5d-5f5882ee9bdd", message.getData().getDroneLocations().get(0).getAreaId());
        assertEquals(100.11, message.getData().getDroneLocations().get(0).getAreaDistance(), 0.001);
        assertTrue(message.getData().getDroneLocations().get(0).getIsInArea());
    }

    @Test
    void entityComputesChecksumAndSize() {
        FlightAreaFile file = FlightAreaFile.create("geofence.json", "{\"areas\":[]}");
        // SHA256 固定长度 64 位十六进制
        assertEquals(64, file.getChecksum().length());
        // 大小按 UTF-8 字节计
        assertEquals("{\"areas\":[]}".getBytes().length, file.getSize());
    }
}
