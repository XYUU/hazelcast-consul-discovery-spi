package cc.springcloud.hazelcast.discovery.consul;

import com.ecwid.consul.json.GsonFactory;
import com.ecwid.consul.transport.TLSConfig;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.ecwid.consul.v1.health.model.HealthService;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * DiscoveryStrategy for Consul
 *
 * @author springcloud
 */
public class ConsulDiscoveryStrategy extends AbstractDiscoveryStrategy implements Runnable {

    // how we connect to consul
    private final String consulHost;
    private final Integer consulPort;
    private final String consulPath;

    // service name we will register under, and tags to apply
    private String[] consulServiceTags = null;
    private boolean enableTagOverride;
    private String consulServiceName = null;

    // if we only discover healthy nodes...
    private boolean consulHealthyOnly = true;

    // How we register with Consul
    private ConsulRegistrator registrator = null;

    // we set this to track if discoverNodes was ever invoked
    private boolean discoverNodesInvoked = false;

    // ACL token to be used for agent, health and catalog clients
    private String consulAclToken = null;

    private ConsulClient consul;

    /**
     * Constructor
     *
     * @param localDiscoveryNode
     * @param logger
     * @param properties
     */
    public ConsulDiscoveryStrategy(DiscoveryNode localDiscoveryNode, ILogger logger, Map<String, Comparable> properties) {
        super(logger, properties);
        // get basic properites for the strategy
        this.consulHost = getOrDefault("consul-host", ConsulDiscoveryConfiguration.CONSUL_HOST, "localhost");
        this.consulPort = getOrDefault("consul-port", ConsulDiscoveryConfiguration.CONSUL_PORT, 8500);
        this.consulPath = getOrDefault("consul-path", ConsulDiscoveryConfiguration.CONSUL_PATH, "");

        this.consulServiceTags = getOrDefault("consul-service-tags", ConsulDiscoveryConfiguration.CONSUL_SERVICE_TAGS, "").split(",");
        this.enableTagOverride = getOrDefault("consul-tag-override-enable", ConsulDiscoveryConfiguration.CONSUL_TAG_OVERRIDE_ENABLE, false);
        this.consulServiceName = getOrDefault("consul-service-name", ConsulDiscoveryConfiguration.CONSUL_SERVICE_NAME, "");
        this.consulHealthyOnly = getOrDefault("consul-healthy-only", ConsulDiscoveryConfiguration.CONSUL_HEALTHY_ONLY, true);
        long discoveryDelayMS = getOrDefault("consul-discovery-delay-ms", ConsulDiscoveryConfiguration.CONSUL_DISCOVERY_DELAY_MS, 30000);
        this.consulAclToken = getOrDefault("consul-acl-token", ConsulDiscoveryConfiguration.CONSUL_ACL_TOKEN, null);

        boolean consulSslEnabled = getOrDefault("consul-ssl-enabled", ConsulDiscoveryConfiguration.CONSUL_SSL_ENABLED, false);

        // our ConsulRegistrator default is DoNothingRegistrator
        String registratorClassName = getOrDefault("consul-registrator",
                ConsulDiscoveryConfiguration.CONSUL_REGISTRATOR,
                DoNothingRegistrator.class.getCanonicalName());

        // this is optional, custom properties to configure a registrator
        // @see the ConsulRegistrator for a description of supported options
        String registratorConfigJSON = getOrDefault("consul-registrator-config",
                ConsulDiscoveryConfiguration.CONSUL_REGISTRATOR_CONFIG,
                null);
        // if JSON config is present attempt to parse it into a map
        RegistratorConfig registratorConfig = null;
        if (registratorConfigJSON != null && !registratorConfigJSON.trim().isEmpty()) {
            try {
                registratorConfig = GsonFactory.getGson().fromJson(registratorConfigJSON, RegistratorConfig.class);
            } catch (Exception e) {
                logger.severe("Unexpected error parsing 'consul-registrator-config' JSON: " + registratorConfigJSON + " error=" + e.getMessage(), e);
            }
        }
        try {
            ConsulRawClient.Builder builder = ConsulRawClient.Builder.builder();
            builder.setHost(consulHost);
            builder.setPort(consulPort);
            builder.setPath(consulPath);
            if (consulSslEnabled) {
                String keyStoreInstanceType = getOrDefault("consul-key-store-instance-type", ConsulDiscoveryConfiguration.CONSUL_KEY_STORE_INSTANCE_TYPE, null);
                String certificatePath = getOrDefault("consul-cert-file-path", ConsulDiscoveryConfiguration.CONSUL_SSL_SERVER_CERT_FILE_PATH, null);
                String certificatePassword = getOrDefault("consul-cert-password", ConsulDiscoveryConfiguration.CONSUL_CERT_PASSWORD, null);
                String keyStorePath = getOrDefault("consul-key-store-path", ConsulDiscoveryConfiguration.CONSUL_KEY_STORE_PATH, null);
                String keyStorePassword = getOrDefault("consul-key-store-password", ConsulDiscoveryConfiguration.CONSUL_KEY_STORE_PASSWORD, null);
                TLSConfig tlsConfig = new TLSConfig(TLSConfig.KeyStoreInstanceType.valueOf(keyStoreInstanceType),
                        certificatePath,
                        certificatePassword,
                        keyStorePath,
                        keyStorePassword);
                builder.setTlsConfig(tlsConfig);
            }
            consul = new ConsulClient(builder.build());
            // build our clients
        } catch (Exception e) {
            String msg = "Unexpected error in configuring discovery: " + e.getMessage();
            logger.severe(msg, e);
        }
        // Ok, now construct our registrator and register with Consul
        try {
            registrator = (ConsulRegistrator) Class.forName(registratorClassName).newInstance();
            logger.info("Using ConsulRegistrator: " + registratorClassName);

            registrator.init(consul, consulServiceName, consulServiceTags, enableTagOverride, consulAclToken, localDiscoveryNode, registratorConfig, logger);
            registrator.register();
            logger.info("Registered with Consul[" + this.consulHost + ":" + this.consulPort + "] serviceId:" + registrator.getMyServiceId());
        } catch (Exception e) {
            logger.severe("Unexpected error attempting to init() ConsulRegistrator and register(): " + e.getMessage(), e);
        }
        // register our shutdown hook for deregisteration on shutdown...
        Thread shutdownThread = new Thread(this);
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        // finally sleep a bit according to the configured discoveryDelayMS
        try {
            logger.info("Registered our service instance w/ Consul OK.. delaying Hazelcast discovery, sleeping: " + discoveryDelayMS + "ms");
            Thread.sleep(discoveryDelayMS);
        } catch (Exception e) {
            logger.severe("Unexpected error sleeping prior to discovery: " + e.getMessage(), e);
        }
    }

    @Override
    public Iterable<DiscoveryNode> discoverNodes() {
        List<DiscoveryNode> toReturn = new ArrayList<DiscoveryNode>();
        try {
            // discover healthy nodes only? (and its NOT the first invocation...)
            if (this.consulHealthyOnly && discoverNodesInvoked) {
                List<HealthService> nodes = consul.getHealthServices(consulServiceName, true, null, consulAclToken).getValue();
                for (HealthService node : nodes) {
                    toReturn.add(new SimpleDiscoveryNode(new Address(node.getService().getAddress(), node.getService().getPort())));
                    getLogger().info("Discovered healthy node: " + node.getService().getAddress() + ":" + node.getService().getPort());
                }
                // discover all services, regardless of health or this is the first invocation
            } else {
                Response<List<CatalogService>> response = this.consul.getCatalogService(consulServiceName, null, this.consulAclToken);
                for (CatalogService service : response.getValue()) {

                    String discoveredAddress = null;
                    String rawServiceAddress = service.getServiceAddress();
                    String rawAddress = service.getAddress();

                    if (rawServiceAddress != null && !rawServiceAddress.trim().isEmpty()) {
                        discoveredAddress = rawServiceAddress;

                    } else if (rawAddress != null && !rawAddress.trim().isEmpty()) {
                        getLogger().warning("discoverNodes() ServiceAddress was null/blank! " +
                                "for service: " + service.getServiceName() +
                                " falling back to Address value");
                        discoveredAddress = rawAddress;

                    } else {
                        getLogger().warning("discoverNodes() could not discover an address, " +
                                "both ServiceAddress and Address were null/blank! " +
                                "for service: " + service.getServiceName());
                    }
                    toReturn.add(new SimpleDiscoveryNode(new Address(discoveredAddress, service.getServicePort())));
                    getLogger().info("Discovered healthy node: " + discoveredAddress + ":" + service.getServicePort());
                }
            }

        } catch (Exception e) {
            getLogger().severe("discoverNodes() unexpected error: " + e.getMessage(), e);
        }
        // flag we were invoked
        discoverNodesInvoked = true;
        return toReturn;
    }

    @Override
    public void run() {
        try {
            if (registrator != null) {
                getLogger().info("Deregistering myself from Consul: " + this.registrator.getMyServiceId());
                registrator.deregister();
            }
        } catch (Throwable e) {
            this.getLogger().severe("Unexpected error in ConsulRegistrator.deregister(): " + e.getMessage(), e);
        }
    }
}
