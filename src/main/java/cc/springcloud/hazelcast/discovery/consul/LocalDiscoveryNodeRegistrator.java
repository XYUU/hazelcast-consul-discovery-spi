package cc.springcloud.hazelcast.discovery.consul;

import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.DiscoveryNode;

/**
 * @author springcloud
 * @see BaseRegistrator
 * <p>
 * The IP/PORT that it registers with is that auto detected/determined by Hazelcast
 * itself via Hazelcast's DiscoveryNode's Address that is passed to the ConsulDiscoveryStrategy
 * in its constructor.
 * <p>
 * Custom options (specified as JSON value for the 'consul-registrator-config')
 * These are in ADDITION to those commonly defined in BaseRegistrator (base-class)
 * <p>
 * - preferPublicAddress (true|false) : use the public IP determined by
 * hazelcast (if not null) over the private IP
 */
public class LocalDiscoveryNodeRegistrator extends BaseRegistrator {

    // properties that are supported in the JSON value for the 'consul-registrator-config' config property
    // in ADDITION to those defined in BaseRegistrator
    @Override
    public Address determineMyLocalAddress(DiscoveryNode localDiscoveryNode, RegistratorConfig registratorConfig) {

        Address myLocalAddress = localDiscoveryNode.getPrivateAddress();

        boolean usePublicAddress = registratorConfig != null && registratorConfig.isPreferPublicAddress();
        if (usePublicAddress) {
            logger.info("Registrator config property: preferPublicAddress:" + usePublicAddress + " attempting to use it...");
            Address publicAddress = localDiscoveryNode.getPublicAddress();
            if (publicAddress != null) {
                myLocalAddress = publicAddress;
            }
        }
        return myLocalAddress;
    }


}
