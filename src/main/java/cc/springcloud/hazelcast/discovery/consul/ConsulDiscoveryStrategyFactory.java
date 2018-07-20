package cc.springcloud.hazelcast.discovery.consul;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class ConsulDiscoveryStrategyFactory implements DiscoveryStrategyFactory {

	private static final Collection<PropertyDefinition> PROPERTIES =
			Arrays.asList(ConsulDiscoveryConfiguration.CONSUL_HOST,
					ConsulDiscoveryConfiguration.CONSUL_PORT,
					ConsulDiscoveryConfiguration.CONSUL_SERVICE_NAME,
					ConsulDiscoveryConfiguration.CONSUL_HEALTHY_ONLY,
					ConsulDiscoveryConfiguration.CONSUL_SERVICE_TAGS,
					ConsulDiscoveryConfiguration.CONSUL_REGISTRATOR,
					ConsulDiscoveryConfiguration.CONSUL_REGISTRATOR_CONFIG,
					ConsulDiscoveryConfiguration.CONSUL_DISCOVERY_DELAY_MS,
					ConsulDiscoveryConfiguration.CONSUL_ACL_TOKEN,
					ConsulDiscoveryConfiguration.CONSUL_SSL_ENABLED,
					ConsulDiscoveryConfiguration.CONSUL_SSL_SERVER_CERT_FILE_PATH,
					ConsulDiscoveryConfiguration.CONSUL_PATH,
					ConsulDiscoveryConfiguration.CONSUL_KEY_STORE_INSTANCE_TYPE,
					ConsulDiscoveryConfiguration.CONSUL_CERT_PASSWORD,
					ConsulDiscoveryConfiguration.CONSUL_KEY_STORE_PATH,
					ConsulDiscoveryConfiguration.CONSUL_KEY_STORE_PASSWORD);

	public Class<? extends DiscoveryStrategy> getDiscoveryStrategyType() {
		// Returns the actual class type of the DiscoveryStrategy
		// implementation, to match it against the configuration
		return ConsulDiscoveryStrategy.class;
	}

	public Collection<PropertyDefinition> getConfigurationProperties() {
		return PROPERTIES;
	}

	public DiscoveryStrategy newDiscoveryStrategy(DiscoveryNode discoveryNode,
												  ILogger logger,
												  Map<String, Comparable> properties ) {

		return new ConsulDiscoveryStrategy( discoveryNode, logger, properties );                                      
	}   

}
