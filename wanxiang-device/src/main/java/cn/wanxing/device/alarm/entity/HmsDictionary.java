package cn.wanxing.device.alarm.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 健康告警（HMS）文案字典：加载 hms.json，按告警码查询文案并填充占位符。
 *
 * <p>hms.json 结构：{ "dock_tip_0x12040000": {"en": "...", "zh": "..."}, "fpv_tip_...": {...} }。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HmsDictionary {

    @Autowired
    private ObjectMapper objectMapper;

    /** 告警文案字典：key（如 dock_tip_0x16100083）→ {en, zh} */
    private final Map<String, Map<String, String>> dict = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/hms.json")) {
            if (is == null) {
                log.warn("未找到 hms.json 告警文案字典");
                return;
            }
            Map<String, Map<String, String>> loaded = objectMapper.readValue(is, new TypeReference<>() {});
            dict.putAll(loaded);
            log.info("加载告警文案字典 {} 条", dict.size());
        } catch (Exception e) {
            log.error("加载 hms.json 失败", e);
        }
    }

    /**
     * 解析告警文案：按设备类型选 key，用 args 填充占位符，返回中文文案（找不到返回 null）
     */
    public String resolveMessage(String deviceType, String code, Integer inTheSky, JsonNode args) {
        // 1.获取key
        String key = resolveTipKey(deviceType, code, inTheSky);
        if (key == null) {
            return null;
        }
        // 2.从json获取数据
        Map<String, String> tip = dict.get(key);
        if (tip == null) {
            return null;
        }

        return fill(tip.get("zh"), code, args);
    }

    /**
     * 按设备域选择文案 key：机场 dock_tip_{code}；飞行器/遥控器 fpv_tip_{code}（天上加 _in_the_sky）
     */
    private static String resolveTipKey(String deviceType, String code, Integer inTheSky) {
        if (deviceType == null || deviceType.isEmpty()) {
            return null;
        }
        String domain = deviceType.split("-")[0];
        if ("3".equals(domain)) {
            return "dock_tip_" + code;
        }
        return (inTheSky != null && inTheSky == 1)
                ? "fpv_tip_" + code + "_in_the_sky"
                : "fpv_tip_" + code;
    }

    /**
     * 填充文案占位符
     */
    private String fill(String template, String code, JsonNode args) {
        if (template == null) {
            return null;
        }
        int sensorIndex = args == null ? 0 : args.path("sensor_index").asInt(0);
        int componentIndex = args == null ? 0 : args.path("component_index").asInt(0);
        return template
                .replace("%alarmid", code == null ? "" : code)
                .replace("%component_index", String.valueOf(Math.max(1, componentIndex + 1)))
                .replace("%index", String.valueOf(sensorIndex + 1))
                .replace("%battery_index", sensorIndex == 0 ? "左" : "右")
                .replace("%dock_cover_index", sensorIndex == 0 ? "左" : "右")
                .replace("%charging_rod_index", chargingRod(sensorIndex));
    }

    private String chargingRod(int sensorIndex) {
        return switch (sensorIndex) {
            case 0 -> "前";
            case 1 -> "后";
            case 2 -> "左";
            case 3 -> "右";
            default -> "";
        };
    }
}