package cn.wanxing.device.wayline;

import cn.wanxing.device.wayline.message.WaylineProgressMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 航线任务进度消息解析测试：报文结构取自官方文档 flighttask_progress 示例
 */
class WaylineProgressMessageParseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 官方文档 flighttask_progress 示例（status=ok 终态，带断点信息） */
    private static final String OFFICIAL_EXAMPLE = """
            {
                "bid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
                "data": {
                    "output": {
                        "ext": {
                            "break_point": {
                                "attitude_head": 30,
                                "break_reason": 1,
                                "height": 100.23,
                                "index": 1,
                                "latitude": 23.4,
                                "longitude": 113.99,
                                "progress": 0.34,
                                "state": 0,
                                "wayline_id": 0
                            },
                            "current_waypoint_index": 3,
                            "flight_id": "flight_id",
                            "media_count": 6,
                            "track_id": "track_id",
                            "wayline_id": 0,
                            "wayline_mission_state": 9
                        },
                        "progress": {
                            "current_step": 19,
                            "percent": 100
                        },
                        "status": "ok"
                    },
                    "result": 0
                },
                "tid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
                "timestamp": 1654070968655,
                "method": "flighttask_progress"
            }
            """;

    @Test
    void parseOfficialExample() throws Exception {
        WaylineProgressMessage message = objectMapper.readValue(OFFICIAL_EXAMPLE, WaylineProgressMessage.class);
        assertEquals("flighttask_progress", message.getMethod());
        assertEquals(0, message.getData().getResult());
        assertEquals("ok", message.getData().getOutput().getStatus());
        assertEquals(100, message.getData().getOutput().getProgress().getPercent());
        assertEquals(19, message.getData().getOutput().getProgress().getCurrentStep());
        assertEquals("flight_id", message.getData().getOutput().getExt().getFlightId());
        assertEquals(6, message.getData().getOutput().getExt().getMediaCount());
        assertEquals(3, message.getData().getOutput().getExt().getCurrentWaypointIndex());
    }

    @Test
    void parseBreakPoint() throws Exception {
        WaylineProgressMessage message = objectMapper.readValue(OFFICIAL_EXAMPLE, WaylineProgressMessage.class);
        WaylineProgressMessage.BreakPoint bp = message.getData().getOutput().getExt().getBreakPoint();
        assertNotNull(bp);
        assertEquals(1, bp.getIndex());
        assertEquals(0, bp.getState());
        assertEquals(0.34, bp.getProgress(), 0.001);
        assertEquals(23.4, bp.getLatitude(), 0.000001);
        assertEquals(113.99, bp.getLongitude(), 0.000001);
        assertEquals(100.23, bp.getHeight(), 0.001);
        assertEquals(30, bp.getAttitudeHead(), 0.001);
        assertEquals(1, bp.getBreakReason());
    }

    @Test
    void entityApplyProgressKeepsDefaults() {
        // 进度字段缺省时保持原值（增量语义，与 state 链路一致）
        cn.wanxing.device.wayline.entity.WaylineJob job = new cn.wanxing.device.wayline.entity.WaylineJob();
        job.setStatus("executing");
        job.setPercent(40);
        job.applyProgress("ok", null, null, 6, "{\"index\":1}");
        assertEquals("ok", job.getStatus());
        assertEquals(40, job.getPercent());
        assertEquals(6, job.getMediaCount());
        assertEquals("{\"index\":1}", job.getBreakpoint());
    }
}
