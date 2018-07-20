package cc.springcloud.hazelcast.discovery.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;

/**
 * Use this ConsulRegistrator if you manage the registration
 * of your hazelcast nodes manually/externally via a local
 * Consul agent or other means. No registration/deregistration
 * will occur if you use this implementation
 *
 * @author springcloud
 */
public class DoNothingRegistrator implements ConsulRegistrator {

    @Override
    public String getMyServiceId() {
        return null;
    }

    @Override
    public void init(ConsulClient consul,
                     String consulServiceName,
                     String[] consulTags,
                     boolean enableTagOverride,
                     String consulAclToken,
                     DiscoveryNode localDiscoveryNode,
                     RegistratorConfig registratorConfig,
                     ILogger logger) throws Exception {

    }


    @Override
    public void register() throws Exception {
    }

    @Override
    public void deregister() throws Exception {
    }

}
