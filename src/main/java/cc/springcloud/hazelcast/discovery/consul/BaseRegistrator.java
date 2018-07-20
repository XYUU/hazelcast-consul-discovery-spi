package cc.springcloud.hazelcast.discovery.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.DiscoveryNode;

import java.util.Arrays;

/**
 * Use derivatives of this ConsulRegistrator if you don't have or don't want to
 * run a separate Consul agent process on the same box where this hazelcast
 * enabled application runs. This ConsulRegistrator will register this
 * hazelcast instance with Consul as a service and also optional
 * configure a health-check script if you defined it.
 * <p>
 * The IP/PORT that it registers with is generally dictated by classes
 * that derive from this class and override the determineMyLocalAddress()
 * method.
 * <p>
 * It will also de-register the service if invoked to do so.
 * <p>
 * Common custom options (specified as JSON value for the 'consul-registrator-config')
 * which are available to all derivative classes
 * <p>
 * - healthCheckScript: can be anything you want Consul to do to determine health.
 * Variables #MYIP/#MYPORT will be replaced. https://www.consul.io/docs/agent/checks.html
 * <p>
 * - healthCheckScriptIntervalSeconds: self explanatory
 * <p>
 * - healthCheckHttp: valid hostname and port to use for health check. You can provide optional
 * path to a specific script.
 * <p>
 * - healthCheckHttpIntervalSeconds: self explanatory
 *
 * @author springcloud
 */
public abstract class BaseRegistrator implements ConsulRegistrator {
    private static final String TEMPLATE_MYPORT = "#MYPORT";
    private static final String TEMPLATE_MYIP = "#MYIP";

    protected ILogger logger = null;
    protected Address myLocalAddress = null;
    protected String[] tags = null;
    protected boolean enableTagOverride;
    protected String consulServiceName = null;
    protected String consulAclToken = null;
    protected RegistratorConfig registratorConfig = null;

    private String myServiceId = null;

    private ConsulClient consul;

    protected abstract Address determineMyLocalAddress(DiscoveryNode localDiscoveryNode,
                                                       RegistratorConfig registratorConfig) throws Exception;

    @Override
    public void init(ConsulClient consul,
                     String consulServiceName,
                     String[] consulTags,
                     boolean enableTagOverride,
                     String consulAclToken,
                     DiscoveryNode localDiscoveryNode,
                     RegistratorConfig registratorConfig,
                     ILogger logger) throws Exception {

        this.logger = logger;
        this.tags = consulTags;
        this.enableTagOverride = enableTagOverride;
        this.consulServiceName = consulServiceName;
        this.consulAclToken = consulAclToken;
        this.registratorConfig = registratorConfig;
        try {
            /**
             * Determine my local address
             */
            this.myLocalAddress = determineMyLocalAddress(localDiscoveryNode, registratorConfig);
            logger.info("Determined local DiscoveryNode address to use: " + myLocalAddress);
            // build my Consul agent client that we will register with
            this.consul = consul;
        } catch (Exception e) {
            String msg = "Unexpected error in configuring LocalDiscoveryNodeRegistration: " + e.getMessage();
            logger.severe(msg, e);
            throw new Exception(msg, e);
        }

    }

    @Override
    public String getMyServiceId() {
        return this.myServiceId;
    }

    @Override
    public void register() throws Exception {
        try {
            String address = this.myLocalAddress.getInetAddress().getHostAddress();
            this.myServiceId = this.consulServiceName + "-" +
                    address + "-" +
                    this.myLocalAddress.getHost() + "-" +
                    this.myLocalAddress.getPort();
            NewService service = new NewService();
            service.setName(this.consulServiceName);
            service.setId(myServiceId);
            service.setAddress(address);
            service.setPort(this.myLocalAddress.getPort());
            service.setTags(Arrays.asList(tags));
            service.setEnableTagOverride(enableTagOverride);
            if (registratorConfig.getChecks() != null) {
                for (NewService.Check check : registratorConfig.getChecks()) {
                    replaceVar(check, address);
                }
                service.setChecks(registratorConfig.getChecks());
            } else {
                NewService.Check check = registratorConfig.getCheck();
                replaceVar(check, address);
                service.setCheck(check);
            }
            // register...
            this.consul.agentServiceRegister(service, this.consulAclToken);
        } catch (Exception e) {
            String msg = "Unexpected error in register(serviceId:" + myServiceId + "): " + e.getMessage();
            logger.severe(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * 替换变量
     *
     * @param check
     * @param address
     */
    private void replaceVar(NewService.Check check, String address) {
        String http = check.getHttp();
        if (http != null) {
            check.setHttp(http.replaceAll(TEMPLATE_MYIP, address)
                    .replaceAll(TEMPLATE_MYPORT, String.valueOf(myLocalAddress.getPort())));
        }
        String tcp = check.getTcp();
        if (tcp != null) {
            check.setTcp(tcp.replaceAll(TEMPLATE_MYIP, address)
                    .replaceAll(TEMPLATE_MYPORT, String.valueOf(myLocalAddress.getPort())));
        }
        String script = check.getScript();
        if (script != null) {
            check.setScript(script.replaceAll(TEMPLATE_MYIP, address)
                    .replaceAll(TEMPLATE_MYPORT, String.valueOf(myLocalAddress.getPort())));
        }
    }

    @Override
    public void deregister() throws Exception {
        try {
            this.consul.agentServiceDeregister(this.myServiceId);
        } catch (Exception e) {
            String msg = "Unexpected error in deregister(serviceId:" + myServiceId + "): " + e.getMessage();
            logger.severe(msg, e);
            throw new Exception(msg, e);
        }
    }

}
