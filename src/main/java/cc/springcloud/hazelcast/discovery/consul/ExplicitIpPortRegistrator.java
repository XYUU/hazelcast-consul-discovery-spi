package cc.springcloud.hazelcast.discovery.consul;

import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.DiscoveryNode;

/**
 * @author springcloud
 * @see BaseRegistrator
 * <p>
 * The IP/PORT that it registers with is whatever is specified by
 * in the `consul-registrator-config` config options `ipAddress` and `port`
 * described below.
 * <p>
 * Custom options (specified as JSON value for the 'consul-registrator-config')
 * These are in ADDITION to those commonly defined in BaseRegistrator (base-class)
 * <p>
 * <p>
 * - registerWithIpAddress: the explicit IP address that this node should be registered
 * with Consul as its ServiceAddress
 * <p>
 * - registerWithPort: the explicit PORT that this node should be registered
 * with Consul as its ServiceAddress
 */
public class ExplicitIpPortRegistrator extends BaseRegistrator {

    // properties that are supported in the JSON value for the 'consul-registrator-config' config property
    // in ADDITION to those defined in BaseRegistrator
    public static final String CONFIG_PROP_REGISTER_WITH_IP_ADDRESS = "registerWithIpAddress";
    public static final String CONFIG_PROP_REGISTER_WITH_PORT = "registerWithPort";

    @Override
    public Address determineMyLocalAddress(DiscoveryNode localDiscoveryNode, RegistratorConfig registratorConfig) throws Exception {
        if (registratorConfig == null) {
            String msg = "ExplicitIpPortRegistrator scenario 'consul-registrator-config' cannot be empty!";
            logger.warning(msg);
            throw new Exception(msg);
        }
        String registerWithIpAddress = registratorConfig.getRegisterWithIpAddress();
        Integer registerWithPort = registratorConfig.getRegisterWithPort();
        logger.info("Registrator config properties: " + CONFIG_PROP_REGISTER_WITH_IP_ADDRESS + ":" + registerWithIpAddress
                + " " + CONFIG_PROP_REGISTER_WITH_PORT + ":" + registerWithPort +
                ", will attempt to register with this IP/PORT...");

        return new Address(registerWithIpAddress, registerWithPort);
    }
}
