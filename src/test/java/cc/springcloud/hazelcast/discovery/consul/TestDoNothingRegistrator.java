package cc.springcloud.hazelcast.discovery.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.Address;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the Hazelcast Consul Discovery SPI strategy
 * 
 * @author springcloud
 *
 */
public class TestDoNothingRegistrator {
	
	public static final String CONSUL_HOST = "192.168.1.90";
	public static final int CONSUL_PORT = 8500;

	/**
	 * Tests DoNothingRegistrator functionality
	 * 
	 * IMPORTANT:
	 * 
	 * Prior to this test you must register a service with
	 * your local Consul named 'test-DoNothingRegistrator' with
	 * 5 nodes/instances, and corresponding ports from 5701-5705 (same ip, your local ip)
	 * 
	 * You can use the Consul services def for this test located @ src/test/resources/consul.d/test-DoNothingRegistrator-services.json
	 * Then run this command to start your local consul
	 * 
	 * consul agent -server -bootstrap-expect 1 -data-dir /tmp/consul -config-dir src/test/resources/consul.d/ -ui-dir /path/to/consul-web-ui
	 * 
	 * This will startup consul appropriately for this test to work.
	 * 
	 */
	@Test
	public void testDoNothingRegistrator() {
		
		try {
			
			IMap<Object,Object> testMap1 = null;
			
			int totalInstancesToTest = 5;
			List<HazelcastInstanceMgr> instances = new ArrayList<HazelcastInstanceMgr>();

			System.setProperty(ConsulConfig.CONSUL_HOST.getValue(), CONSUL_HOST);
			System.setProperty(ConsulConfig.CONSUL_PORT.getValue(), String.valueOf(CONSUL_PORT));
			System.setProperty(ConsulConfig.CONSUL_ACL_TOKEN.getValue(), "");
			System.setProperty(ConsulConfig.CONSUL_SSL_ENABLED.getValue(), String.valueOf(false));
			System.setProperty(ConsulConfig.CONSUL_SSL_SERVER_CERT_FILE_PATH.getValue(), "");

			System.out.println("#################### IS CONSUL RUNNING @ " +
					CONSUL_HOST+":"+CONSUL_PORT+"? IF NOT THIS TEST WILL FAIL! ####################");

			ConsulRawClient.Builder builder = ConsulRawClient.Builder.builder();
			builder.setHost(CONSUL_HOST);
			builder.setPort(CONSUL_PORT);
			ConsulClient consul = new ConsulClient(builder.build());
	
			for (int i=0; i<totalInstancesToTest; i++) {
				HazelcastInstanceMgr mgr = new HazelcastInstanceMgr("test-DoNothingRegistrator.xml");
				instances.add(mgr);
				mgr.start();
				
				// create testMap1 in first instance and populate it w/ 10 entries
				if (i == 0) {
					testMap1 = mgr.getInstance().getMap("testMap1");
					for(int j=0; j<10; j++) {
						testMap1.put(j, j);
					}
				}
				
			}
			
			Thread.currentThread().sleep(20000);
			
			// validate we have 5 registered...(regardless of health)
			Response<List<CatalogService>> response = consul.getCatalogService("test-DoNothingRegistrator",null);
			Assert.assertEquals(totalInstancesToTest,response.getValue().size());

			// get the map via each instance and 
			// validate it ensuring they are all talking to one another
			for (HazelcastInstanceMgr mgr : instances) {
				Assert.assertEquals(10, mgr.getInstance().getMap("testMap1").size());
			}
		
			// shutdown everything
			for (HazelcastInstanceMgr instance : instances) {
				instance.shutdown();
			}
			
		} catch(Exception e) {
			e.printStackTrace();
			Assert.assertFalse("Unexpected error in test: " + e.getMessage(),false);
		}
		
	}
	
	
	private class HazelcastInstanceMgr {
		
		private HazelcastInstance hazelcastInstance = null;
		private Config conf = null;
		
		public HazelcastInstanceMgr(String hazelcastConfigFile) {
			this.conf =new ClasspathXmlConfig(hazelcastConfigFile);
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
}
