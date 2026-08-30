package cn.wanxing.device.airsense;

import cn.wanxing.device.airsense.entity.AirsenseWarning;
import cn.wanxing.device.airsense.message.AirsenseWarningMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * AirSense 告警消息解析测试：报文结构取自官方文档 airsense_warning（data 为航班数组）
 */
class AirsenseMessageParseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 官方文档字段结构（thing/product/{sn}/events，method=airsense_warning） */
    private static final String OFFICIAL_EXAMPLE = """
            {
                "bid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
                "data": [
                    {
                        "icao": "79A8C2",
                        "warning_level": 3,
                        "latitude": 22.123456,
                        "longitude": 113.654321,
                        "altitude": 120,
                        "altitude_type": 1,
                        "heading": 273.5,
                        "relative_altitude": -35,
                        "vert_trend": 2,
                        "distance": 850
                    }
                ],
                "gateway": "xxx",
                "need_reply": 1,
                "tid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
                "timestamp": 1654070968655,
                "method": "airsense_warning"
            }
            """;

    @Test
    void parseOfficialExample() throws Exception {
        AirsenseWarningMessage message = objectMapper.readValue(OFFICIAL_EXAMPLE, AirsenseWarningMessage.class);
        assertEquals("airsense_warning", message.getMethod());
        assertEquals(1, message.getNeedReply());
        assertEquals(1, message.getData().size());
        assertEquals("79A8C2", message.getData().get(0).getIcao());
        assertEquals(3, message.getData().get(0).getWarningLevel());
        assertEquals(2, message.getData().get(0).getVertTrend());
        assertEquals(850, message.getData().get(0).getDistance());
    }

    @Test
    void entityMappingFromOfficialExample() throws Exception {
        AirsenseWarningMessage message = objectMapper.readValue(OFFICIAL_EXAMPLE, AirsenseWarningMessage.class);
        AirsenseWarning warning = AirsenseWarning.create("DOCK001", message.getData().get(0));

        assertEquals("DOCK001", warning.getDeviceSn());
        assertEquals("79A8C2", warning.getIcao());
        assertEquals(3, warning.getWarningLevel());
        assertEquals(22.123456, warning.getLatitude(), 0.000001);
        assertEquals(120, warning.getAltitude());
        assertEquals(1, warning.getAltitudeType());
        assertEquals(273.5, warning.getHeading(), 0.001);
        assertEquals(-35, warning.getRelativeAltitude());
        assertEquals(850, warning.getDistance());
        assertNotNull(warning.getCreatedAt(), "告警时间应在工厂方法中赋值");
    }
}
