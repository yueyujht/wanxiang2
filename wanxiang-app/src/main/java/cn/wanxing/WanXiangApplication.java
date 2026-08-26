package cn.wanxing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 万象无人机云控平台 启动类
 *
 * <p>组件扫描基包为 cn.wanxing，自动覆盖 cn.wanxing.common.* 与各业务模块（cn.wanxing.user.*）。
 */
@SpringBootApplication
public class WanXiangApplication {

    public static void main(String[] args) {
        SpringApplication.run(WanXiangApplication.class, args);
    }
}