package cn.wanxing.device.config;

import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.util.UUID;

/**
 * MQTT 接入配置：建立与 DJI 设备网关的连接，订阅主题接收消息，并向外发布消息（如回复 requests_reply）。
 *
 * <p>仅当配置了 {@code wanxiang.mqtt.broker-url} 才生效，未配置则不建立连接（应用正常启动）。
 */
@Configuration
@RequiredArgsConstructor
@EnableIntegration
@EnableConfigurationProperties(MqttProperties.class)
@ConditionalOnProperty(prefix = "wanxiang.mqtt", name = "broker-url")
public class MqttConfig {

    /** 入站通道名：设备消息统一先进这里 */
    public static final String INBOUND_CHANNEL = "wanxiangMqttInboundChannel";

    /** 出站通道名：我们发往设备的消息从这里出去 */
    public static final String OUTBOUND_CHANNEL = "wanxiangMqttOutboundChannel";

    private final MqttProperties properties;

    /**
     * MQTT 客户端工厂：持有网关地址 + 账号密码
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{properties.getBrokerUrl()});
        options.setUserName(properties.getUsername());
        options.setPassword(properties.getPassword() == null ? new char[0] : properties.getPassword().toCharArray());
        factory.setConnectionOptions(options);
        return factory;
    }

    /**
     * 入站通道
     */
    @Bean
    public MessageChannel wanxiangMqttInboundChannel() {
        return new DirectChannel();
    }

    /**
     * 入站适配器：订阅主题，收到的消息以字节数组形式写入入站通道
     */
    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInbound(MqttPahoClientFactory factory,
                                                           MessageChannel wanxiangMqttInboundChannel) {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                properties.getClientId() + "-" + UUID.randomUUID(),
                factory,
                properties.getInboundTopic().split(","));
        // 统一按字节数组传输消息，避免编码问题
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(true);
        adapter.setConverter(converter);
        adapter.setQos(1);
        adapter.setOutputChannel(wanxiangMqttInboundChannel);
        return adapter;
    }

    /**
     * 出站通道
     */
    @Bean
    public MessageChannel wanxiangMqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * 出站处理器：把发到出站通道的消息发布到指定主题（主题取自消息头 MqttHeaders.TOPIC）
     */
    @Bean
    @ServiceActivator(inputChannel = OUTBOUND_CHANNEL)
    public MessageHandler mqttOutbound(MqttPahoClientFactory factory) {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(
                properties.getClientId() + "-out-" + UUID.randomUUID(), factory);
        // 统一按字节数组传输消息，避免编码问题
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(true);
        handler.setConverter(converter);
        handler.setDefaultQos(0);
        return handler;
    }
}
