package cc.springcloud.hazelcast.discovery.consul;

import com.ecwid.consul.v1.agent.model.NewService;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by XYUU <xyuu@xyuu.net> on 2018/7/19.
 */
public class RegistratorConfig {

    @SerializedName("preferPublicAddress")
    private boolean preferPublicAddress;

    @SerializedName("registerWithIpAddress")
    private String registerWithIpAddress;

    @SerializedName("registerWithPort")
    private int registerWithPort;

    @SerializedName("healthCheckProvider")
    private String healthCheckProvider;

    @SerializedName("Check")
    private NewService.Check check;

    @SerializedName("Checks")
    private List<NewService.Check> checks;

    public boolean isPreferPublicAddress() {
        return preferPublicAddress;
    }

    public RegistratorConfig setPreferPublicAddress(boolean preferPublicAddress) {
        this.preferPublicAddress = preferPublicAddress;
        return this;
    }

    public String getRegisterWithIpAddress() {
        return registerWithIpAddress;
    }

    public RegistratorConfig setRegisterWithIpAddress(String registerWithIpAddress) {
        this.registerWithIpAddress = registerWithIpAddress;
        return this;
    }

    public int getRegisterWithPort() {
        return registerWithPort;
    }

    public RegistratorConfig setRegisterWithPort(int registerWithPort) {
        this.registerWithPort = registerWithPort;
        return this;
    }

    public String getHealthCheckProvider() {
        return healthCheckProvider;
    }

    public RegistratorConfig setHealthCheckProvider(String healthCheckProvider) {
        this.healthCheckProvider = healthCheckProvider;
        return this;
    }

    public NewService.Check getCheck() {
        return check;
    }

    public RegistratorConfig setCheck(NewService.Check check) {
        this.check = check;
        return this;
    }

    public List<NewService.Check> getChecks() {
        return checks;
    }

    public RegistratorConfig setChecks(List<NewService.Check> checks) {
        this.checks = checks;
        return this;
    }
}
