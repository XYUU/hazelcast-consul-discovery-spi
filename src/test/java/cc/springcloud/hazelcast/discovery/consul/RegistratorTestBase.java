package cc.springcloud.hazelcast.discovery.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.ecwid.consul.v1.health.model.HealthService;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.nio.Address;
import org.junit.Assert;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Base test class for the Hazelcast Consul Discovery SPI strategies
 * writing registrators
 *
 * @author springcloud
 */
public abstract class RegistratorTestBase {

    public String consulHost;
    public int consulPort;
    public String consulPath;
    public String consulAclToken;
    public boolean consulSslEnabled;
    public String consulSslServerCertFilePath;
    public String consulKeyStoreInstanceType;
    public String consulCertPassword;
    public String consulKeyStorePath;
    public String consulKeyStorePassword;

    private static final ILogger logger = Logger.getLogger(RegistratorTestBase.class);

    protected abstract void preConstructHazelcast(int instanceNumber) throws Exception;

    protected void testRegistrator(String hazelcastConfigXmlFilename, String serviceName) {

        try {

            initSystemProps();

            IMap<Object, Object> testMap1 = null;
            IMap<Object, Object> testMap2 = null;

            int totalInstancesToTest = 5;
            List<HazelcastInstanceMgr> instances = new ArrayList<HazelcastInstanceMgr>();

            System.out.println("#################### IS CONSUL RUNNING @ " +
                    consulHost + ":" + consulPort + "? IF NOT THIS TEST WILL FAIL! ####################");


            ConsulRawClient.Builder builder = ConsulRawClient.Builder.builder();
            builder.setHost(consulHost);
            builder.setPort(consulPort);
            builder.setPath(consulPath);
            ConsulClient consul = new ConsulClient(builder.build());

            for (int i = 0; i < totalInstancesToTest; i++) {

                preConstructHazelcast(i);

                HazelcastInstanceMgr mgr = new HazelcastInstanceMgr(hazelcastConfigXmlFilename);
                instances.add(mgr);
                mgr.start();

                // create testMap1 in first instance and populate it w/ 10 entries
                if (i == 0) {
                    testMap1 = mgr.getInstance().getMap("testMap1");
                    for (int j = 0; j < 10; j++) {
                        testMap1.put(j, j);
                    }
                }

            }

            Thread.currentThread().sleep(20000);

            // validate we have 5 registered...(regardless of health)
            Response<List<CatalogService>> response = consul.getCatalogService(serviceName, null, consulAclToken);
            Assert.assertEquals(totalInstancesToTest, response.getValue().size());

            // validate we have 5 healthy
            Response<List<HealthService>> response2 = consul.getHealthServices(serviceName, true, null, consulAclToken);
            Assert.assertEquals(totalInstancesToTest, response2.getValue().size());

            // get the map via each instance and
            // validate it ensuring they are all talking to one another
            for (HazelcastInstanceMgr mgr : instances) {
                Assert.assertEquals(10, mgr.getInstance().getMap("testMap1").size());
            }

            // pick random instance add new map, verify its everywhere
            Random rand = new Random();
            testMap2 = instances.get(rand.nextInt(instances.size() - 1)).getInstance().getMap("testMap2");
            for (int j = 0; j < 10; j++) {
                testMap2.put(j, j);
            }

            for (HazelcastInstanceMgr mgr : instances) {
                Assert.assertEquals(10, mgr.getInstance().getMap("testMap2").size());
            }


            // shutdown one node
            HazelcastInstanceMgr deadInstance = instances.iterator().next();
            deadInstance.shutdown();

            // let consul healthcheck fail
            Thread.currentThread().sleep(60000);

            // healthy is total -1 now...
            response2 = consul.getHealthServices(serviceName, true, null, consulAclToken);
            Assert.assertEquals((totalInstancesToTest - 1), response2.getValue().size());

            // pick a random instance, add some entries in map, verify
            instances.get(rand.nextInt(instances.size() - 1)).getInstance().getMap("testMap2").put("extra1", "extra1");

            // should be 11 now
            for (HazelcastInstanceMgr mgr : instances) {
                if (mgr != deadInstance) {
                    Assert.assertEquals((10 + 1), mgr.getInstance().getMap("testMap2").size());
                }
            }

            // shutdown everything
            for (HazelcastInstanceMgr instance : instances) {
                instance.shutdown();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertFalse("Unexpected error in test: " + e.getMessage(), false);
        }

    }


    private class HazelcastInstanceMgr {

        private HazelcastInstance hazelcastInstance = null;
        private Config conf = null;

        public HazelcastInstanceMgr(String hazelcastConfigFile) {
            this.conf = new ClasspathXmlConfig(hazelcastConfigFile);
        }

        public HazelcastInstance getInstance() {
            return hazelcastInstance;
        }

        public void start() {
            hazelcastInstance = Hazelcast.newHazelcastInstance(conf);
        }

        public void shutdown() {
            this.hazelcastInstance.shutdown();
        }

        public Address getAddress() {
            return this.hazelcastInstance.getCluster().getLocalMember().getAddress();
        }

    }

    protected String determineIpAddress() throws Exception {

        InetAddress addr = InetAddress.getLocalHost();
        String ipAdd = addr.getHostAddress();

        return ipAdd;

    }


    protected void initSystemProps() {

        consulHost = System.getProperty(ConsulConfig.CONSUL_HOST.getValue(), "192.168.1.90");
        consulPort = Integer.valueOf(System.getProperty(ConsulConfig.CONSUL_PORT.getValue(), "8500"));
        consulPath = System.getProperty(ConsulConfig.CONSUL_PATH.getValue(), "");
        consulAclToken = System.getProperty(ConsulConfig.CONSUL_ACL_TOKEN.getValue(), "");
        consulSslEnabled = Boolean.valueOf(System.getProperty(ConsulConfig.CONSUL_SSL_ENABLED.getValue(), "false"));
        consulSslServerCertFilePath = System.getProperty(ConsulConfig.CONSUL_SSL_SERVER_CERT_FILE_PATH.getValue(), "");
        consulKeyStoreInstanceType = System.getProperty(ConsulConfig.CONSUL_KEY_STORE_INSTANCE_TYPE.getValue(), "");
        consulCertPassword = System.getProperty(ConsulConfig.CONSUL_CERT_PASSWORD.getValue(), "");
        consulKeyStorePath = System.getProperty(ConsulConfig.CONSUL_KEY_STORE_PATH.getValue(), "");
        consulKeyStorePassword = System.getProperty(ConsulConfig.CONSUL_KEY_STORE_PASSWORD.getValue(), "");

        System.setProperty(ConsulConfig.CONSUL_HOST.getValue(), consulHost);
        System.setProperty(ConsulConfig.CONSUL_PORT.getValue(), String.valueOf(consulPort));
        System.setProperty(ConsulConfig.CONSUL_ACL_TOKEN.getValue(), consulAclToken);
        System.setProperty(ConsulConfig.CONSUL_SSL_ENABLED.getValue(), String.valueOf(consulSslEnabled));
        System.setProperty(ConsulConfig.CONSUL_SSL_SERVER_CERT_FILE_PATH.getValue(), consulSslServerCertFilePath);
        System.setProperty(ConsulConfig.CONSUL_KEY_STORE_INSTANCE_TYPE.getValue(), consulKeyStoreInstanceType);
        System.setProperty(ConsulConfig.CONSUL_CERT_PASSWORD.getValue(), consulCertPassword);
        System.setProperty(ConsulConfig.CONSUL_KEY_STORE_PATH.getValue(), consulKeyStorePath);
        System.setProperty(ConsulConfig.CONSUL_KEY_STORE_PASSWORD.getValue(), consulKeyStorePassword);

        System.out.println("***** USING SYSTEM PARAMS *****");
        System.out.println("consulHost : " + consulHost);
        System.out.println("consulPort : " + consulPort);
        System.out.println("consulAclToken : " + consulAclToken);
        System.out.println("consulSslEnabled : " + consulSslEnabled);
        System.out.println("consulSslServerCertFilePath : " + consulSslServerCertFilePath);
        System.out.println("consulKeyStoreInstanceType : " + consulKeyStoreInstanceType);
        System.out.println("consulCertPassword : " + consulCertPassword);
        System.out.println("consulKeyStorePath : " + consulKeyStorePath);
        System.out.println("consulKeyStorePassword : " + consulKeyStorePassword);

    }
}
