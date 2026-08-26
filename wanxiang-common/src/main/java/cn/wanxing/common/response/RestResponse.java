package cn.wanxing.common.response;

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.Setter;

/**
 * 第三方（DJI 云 API）响应封装
 */
@Setter
@Getter
public class RestResponse {

    private JSONObject data;

    private JSONObject error;

    public Boolean getSuccess() {
        return data != null;
    }

    public String getResponseCode() {
        return error != null ? error.getString("code") : null;
    }

    public String getResponseMessage() {
        return error != null ? error.getString("message") : null;
    }
}