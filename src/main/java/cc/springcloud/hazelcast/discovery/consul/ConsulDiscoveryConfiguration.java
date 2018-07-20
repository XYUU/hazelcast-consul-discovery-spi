package cc.springcloud.hazelcast.discovery.consul;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.config.properties.PropertyTypeConverter;
import com.hazelcast.config.properties.SimplePropertyDefinition;

/**
 * Defines constants for our supported Properties
 *
 * @author springcloud
 */
public class ConsulDiscoveryConfiguration {

    public static final PropertyDefinition CONSUL_HOST =
            new SimplePropertyDefinition("consul-host", PropertyTypeConverter.STRING);

    public static final PropertyDefinition CONSUL_PORT =
            new SimplePropertyDefinition("consul-port", PropertyTypeConverter.INTEGER);

    //Optional Property defaults to empty string if empty or not provided
    public static final PropertyDefinition CONSUL_SERVICE_TAGS =
            new SimplePropertyDefinition("consul-service-tags", true, PropertyTypeConverter.STRING);

    public static final PropertyDefinition CONSUL_SERVICE_NAME =
            new SimplePropertyDefinition("consul-service-name", PropertyTypeConverter.STRING);

    public static final PropertyDefinition CONSUL_HEALTHY_ONLY =
            new SimplePropertyDefinition("consul-healthy-only", PropertyTypeConverter.BOOLEAN);

    //Optional Property defaults to DoNothingRegistrator if empty or not provided
    public static final PropertyDefinition CONSUL_REGISTRATOR =
            new SimplePropertyDefinition("consul-registrator", true, PropertyTypeConverter.STRING);

    //Optional Property defaults to NULL if empty or not provided
    public static final PropertyDefinition CONSUL_REGISTRATOR_CONFIG =
            new SimplePropertyDefinition("consul-registrator-config", true, PropertyTypeConverter.STRING);

    //Optional Property defaults to 30000 if empty or not provided
    public static final PropertyDefinition CONSUL_DISCOVERY_DELAY_MS =
            new SimplePropertyDefinition("consul-discovery-delay-ms", true, PropertyTypeConverter.INTEGER);

    //Optional Property defaults to NULL if empty or not provided
    public static final PropertyDefinition CONSUL_ACL_TOKEN =
            new SimplePropertyDefinition("consul-acl-token", true, PropertyTypeConverter.STRING);

    //Optional Property defaults to false if empty or not provided
    public static final PropertyDefinition CONSUL_SSL_ENABLED =
            new SimplePropertyDefinition("consul-ssl-enabled", true, PropertyTypeConverter.BOOLEAN);

    //Optional Property defaults to NULL if empty or not provided
    public static final PropertyDefinition CONSUL_SSL_SERVER_CERT_FILE_PATH =
            new SimplePropertyDefinition("consul-ssl-server-cert-file-path", true, PropertyTypeConverter.STRING);

    public static final PropertyDefinition CONSUL_PATH =
            new SimplePropertyDefinition("consul-path", true, PropertyTypeConverter.STRING);

    public static final PropertyDefinition CONSUL_TAG_OVERRIDE_ENABLE =
            new SimplePropertyDefinition("consul-tag-override-enable", true, PropertyTypeConverter.STRING);

    public static final PropertyDefinition CONSUL_KEY_STORE_INSTANCE_TYPE =
            new SimplePropertyDefinition("consul-key-store-instance-type", true, PropertyTypeConverter.STRING);

    public static final PropertyDefinition CONSUL_CERT_PASSWORD =
            new SimplePropertyDefinition("consul-cert-password", true, PropertyTypeConverter.STRING);

    public static final PropertyDefinition CONSUL_KEY_STORE_PATH =
            new SimplePropertyDefinition("consul-key-store-path", true, PropertyTypeConverter.STRING);

    public static final PropertyDefinition CONSUL_KEY_STORE_PASSWORD =
            new SimplePropertyDefinition("consul-key-store-password", true, PropertyTypeConverter.STRING);
}
