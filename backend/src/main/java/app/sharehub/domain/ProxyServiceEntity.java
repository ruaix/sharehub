package app.sharehub.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("proxy_services")
public class ProxyServiceEntity {
    @TableId
    private Long serviceId;
    private String panelUrlEncrypted;
    private String probeUrl;
    private Integer nodeTotal;
    private Integer trafficLimitGb;
    private Integer deviceLimit;

    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public String getPanelUrlEncrypted() { return panelUrlEncrypted; }
    public void setPanelUrlEncrypted(String panelUrlEncrypted) { this.panelUrlEncrypted = panelUrlEncrypted; }
    public String getProbeUrl() { return probeUrl; }
    public void setProbeUrl(String probeUrl) { this.probeUrl = probeUrl; }
    public Integer getNodeTotal() { return nodeTotal; }
    public void setNodeTotal(Integer nodeTotal) { this.nodeTotal = nodeTotal; }
    public Integer getTrafficLimitGb() { return trafficLimitGb; }
    public void setTrafficLimitGb(Integer trafficLimitGb) { this.trafficLimitGb = trafficLimitGb; }
    public Integer getDeviceLimit() { return deviceLimit; }
    public void setDeviceLimit(Integer deviceLimit) { this.deviceLimit = deviceLimit; }
}
