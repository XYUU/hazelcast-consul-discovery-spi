package cc.springcloud.hazelcast.discovery.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;

/**
 * Defines an interface for an object who's responsibility
 * it is to register (and deregister) this hazelcast instance as a service
 * node with Consul.
 * 
 * @author springcloud
 *
 */
public interface ConsulRegistrator {

	/**
	 * Return the service id as registered with Consul
	 * 
	 * @return
	 */
	public String getMyServiceId();

	/**
	 * Initialize the registrator
	 *
	 * @param consulServiceName
	 * @param consulTags
	 * @param consulAclToken
	 * @param consul
	 * @param localDiscoveryNode
	 * @param registratorConfig
	 * @param logger
	 * @throws Exception
	 */
	public void init(ConsulClient consul,
					 String consulServiceName,
					 String[] consulTags,
                     boolean enableTagOverride,
					 String consulAclToken,
					 DiscoveryNode localDiscoveryNode,
					 RegistratorConfig registratorConfig,
					 ILogger logger) throws Exception;
	
	/**
	 * Register this hazelcast instance as a service node
	 * with Consul
	 * 
	 * @throws Exception
	 */
	public void register() throws Exception;
	
	/**
	 * Deregister this hazelcast instance as a service node
	 * with Consul
	 * 
	 * @throws Exception
	 */
	public void deregister() throws Exception;
	
}
