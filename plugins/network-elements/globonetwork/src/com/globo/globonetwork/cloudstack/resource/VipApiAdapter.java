package com.globo.globonetwork.cloudstack.resource;

import com.cloud.agent.api.Answer;
import com.cloud.network.lb.LoadBalancingRule;
import com.globo.globonetwork.client.api.GloboNetworkAPI;
import com.globo.globonetwork.client.exception.GloboNetworkException;
import com.globo.globonetwork.client.model.Vip;
import com.globo.globonetwork.client.model.VipEnvironment;
import com.globo.globonetwork.client.model.VipPoolMap;
import com.globo.globonetwork.client.model.VipV3;
import com.globo.globonetwork.client.model.OptionVipV3;
import com.globo.globonetwork.client.model.Ip;
import com.globo.globonetwork.cloudstack.commands.ApplyVipInGloboNetworkCommand;
import com.globo.globonetwork.cloudstack.response.GloboNetworkVipResponse;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

class VipApiAdapter {

    private Vip vip;
    private VipV3 vipV3;
    private Ip ip;
    private String version;
    private GloboNetworkAPI globoNetworkAPI;

    private static final Integer DEFAULT_TIMEOUT = 5;
    private static final String DEFAULT_CACHE = "(nenhum)";
    private static final Logger s_logger = Logger.getLogger(VipApiAdapter.class);

    VipApiAdapter(Long id, GloboNetworkAPI globoNetworkAPI, String version) throws GloboNetworkException {
        this.globoNetworkAPI = globoNetworkAPI;
        this.version = version;

        if(id != null){
            if(version.equals("3")){
                vipV3 = globoNetworkAPI.getVipV3API().getById(id);
            }else {
                vip = globoNetworkAPI.getVipAPI().getByPk(id);
            }
        }
    }

    Boolean hasVip(){
        return vip != null || vipV3 != null;
    }

    public VipApiAdapter save(ApplyVipInGloboNetworkCommand cmd, String host, VipEnvironment vipEnvironment, Ip ip, List<VipPoolMap> vipPoolMapping) throws GloboNetworkException {
        this.ip = ip;
        if(version.equals("3")){
            this.vipV3 = createVipV3(cmd, host, vipEnvironment, ip, vipPoolMapping);
        }else{
            this.vip = createVipV2(cmd, host, vipEnvironment, ip, vipPoolMapping);
        }
        return this;
    }

    public VipApiAdapter update(ApplyVipInGloboNetworkCommand cmd, Ip ip, List<VipPoolMap> vipPoolMapping) throws GloboNetworkException {
        this.ip = ip;
        if(vipV3 != null){
            this.vipV3 = updateVipV3(cmd);
        }else{
            this.vip = updateVipV2(cmd, ip, vipPoolMapping);
        }
        return this;
    }

    public VipApiAdapter validate(Ip ip) throws GloboNetworkException {
        if(this.vip != null) {
            s_logger.info("Validating VIP IP");
            this.vip = globoNetworkAPI.getVipAPI().getByPk(vip.getId());
            this.vip.setIpv4Id(ip.getId());
            globoNetworkAPI.getVipAPI().validate(vip.getId());
        }
        return this;
    }

    void deploy() throws GloboNetworkException {
        if(this.vipV3 != null){
            deployVipV3();
        }else{
            deployVipV2();
        }
    }

    Answer createVipResponse(ApplyVipInGloboNetworkCommand cmd) {
        if(vipV3 != null){
            return createVipV3Response(cmd);
        }else{
            return createVipV2Response(cmd);
        }
    }

    void undeploy() throws GloboNetworkException {
        if(vipV3 != null){
            undeployVipV3();
        }else{
            undeployVipV2();
        }
    }

    void delete(Boolean keepIp) throws GloboNetworkException {
        if(vipV3 != null){
            deleteVipV3(keepIp);
        }else{
            deleteVipV2(keepIp);
        }
    }

    private VipV3 createVipV3(ApplyVipInGloboNetworkCommand cmd, String host, VipEnvironment vipEnvironment, Ip ip, List<VipPoolMap> vipPoolMapping) throws GloboNetworkException {
        VipV3 vip = new VipV3();
        vip.setName(host);
        vip.setService(cmd.getServiceName());
        vip.setBusiness(cmd.getBusinessArea());
        vip.setEnvironmentVipId(vipEnvironment.getId());
        vip.setIpv4Id(ip.getId());
        vip.setOptions(buildVipOptions(cmd));

        OptionVipV3 l7Rule = globoNetworkAPI.getOptionVipV3API().findOptionsByTypeAndName(vipEnvironment.getId(), "l7_rule", "default_vip").get(0);
        List<VipV3.Port> ports = new ArrayList<>();
        for(VipPoolMap vipPoolMap : vipPoolMapping){
            String l4ProtocolString;
            String l7ProtocolString = "Outros";
            if(cmd.getHealthcheckType().equals("UDP")){
                l4ProtocolString = "UDP";
            } else{
                l4ProtocolString = "TCP";
            }

            OptionVipV3 l4Protocol = globoNetworkAPI.getOptionVipV3API().findOptionsByTypeAndName(vipEnvironment.getId(), "l4_protocol", l4ProtocolString).get(0);
            OptionVipV3 l7Protocol = globoNetworkAPI.getOptionVipV3API().findOptionsByTypeAndName(vipEnvironment.getId(), "l7_protocol", l7ProtocolString).get(0);

            VipV3.Port port = new VipV3.Port();
            port.setPort(vipPoolMap.getPort());
            port.setOptions(new VipV3.PortOptions(l4Protocol.getId(), l7Protocol.getId()));

            VipV3.Pool pool = new VipV3.Pool(vipPoolMap.getPoolId(), l7Rule.getId(), null);
            port.setPools(Collections.singletonList(pool));
            ports.add(port);
        }
        vip.setPorts(ports);

        globoNetworkAPI.getVipV3API().save(vip);

        return globoNetworkAPI.getVipV3API().getById(vip.getId());
    }

    private Vip createVipV2(ApplyVipInGloboNetworkCommand cmd, String host, VipEnvironment vipEnvironment, Ip ip, List<VipPoolMap> vipPoolMapping) throws GloboNetworkException {
        String finality = vipEnvironment.getFinality();
        String client = vipEnvironment.getClient();
        String environment = vipEnvironment.getEnvironmentName();
        String cache = cmd.getCache() == null ? DEFAULT_CACHE : cmd.getCache();
        String lbPersistence = getPersistenceMethod(cmd.getPersistencePolicy());

        Vip response = globoNetworkAPI.getVipAPI().save(
            ip.getId(), null, finality, client, environment, cache,
            lbPersistence, DEFAULT_TIMEOUT, host, cmd.getBusinessArea(),
            cmd.getServiceName(), null, vipPoolMapping, null, null
        );

        return globoNetworkAPI.getVipAPI().getByPk(response.getId());
    }

    private Vip updateVipV2(ApplyVipInGloboNetworkCommand cmd, Ip ip, List<VipPoolMap> vipPoolMapping) throws GloboNetworkException {
        String lbPersistence = getPersistenceMethod(cmd.getPersistencePolicy());

        if (vip.getCreated()) {
            if (!lbPersistence.equals(vip.getPersistence())) {
                globoNetworkAPI.getVipAPI().alterPersistence(vip.getId(), lbPersistence);
            }
            return vip;
        }
        return globoNetworkAPI.getVipAPI().save(
            ip.getId(), null, vip.getFinality(), vip.getClient(), vip.getEnvironment(), vip.getCache(),
            lbPersistence, DEFAULT_TIMEOUT, vip.getHost(), cmd.getBusinessArea(),
            cmd.getServiceName(), null, vipPoolMapping, null, null
        );
    }

    private VipV3 updateVipV3(ApplyVipInGloboNetworkCommand cmd) throws GloboNetworkException {
        vipV3.setOptions(buildVipOptions(cmd));

        if (!vipV3.getCreated()) {
            return globoNetworkAPI.getVipV3API().save(vipV3);
        }else{
            globoNetworkAPI.getVipV3API().deployUpdate(vipV3);
        }
        return vipV3;
    }

    private VipV3.VipOptions buildVipOptions(ApplyVipInGloboNetworkCommand cmd) throws GloboNetworkException {
        String cache = cmd.getCache() == null ? DEFAULT_CACHE : cmd.getCache();
        String lbPersistence = getPersistenceMethod(cmd.getPersistencePolicy());
        Long environment = cmd.getVipEnvironmentId();

        OptionVipV3 cacheGroup = globoNetworkAPI.getOptionVipV3API().findOptionsByTypeAndName(environment, "cache", cache).get(0);
        OptionVipV3 trafficReturn = globoNetworkAPI.getOptionVipV3API().findOptionsByTypeAndName(environment, "Retorno de trafego", "Normal").get(0);
        OptionVipV3 timeout = globoNetworkAPI.getOptionVipV3API().findOptionsByTypeAndName(environment, "timeout", "5").get(0);
        OptionVipV3 persistence = globoNetworkAPI.getOptionVipV3API().findOptionsByTypeAndName(environment, "Persistencia", lbPersistence).get(0);

        return new VipV3.VipOptions(cacheGroup.getId(), trafficReturn.getId(), timeout.getId(), persistence.getId());
    }

    private void deployVipV2() throws GloboNetworkException {
        if (!vip.getCreated()) {
            s_logger.info("Requesting GloboNetwork to create vip " + vip.getId());
            globoNetworkAPI.getVipAPI().create(vip.getId());
        }
    }

    private void deployVipV3() throws GloboNetworkException {
        if (!vipV3.getCreated()) {
            s_logger.info("Requesting GloboNetwork to create vipV3 " + vipV3.getId());
            globoNetworkAPI.getVipV3API().deploy(vipV3.getId());
        }
    }

    private Answer createVipV3Response(ApplyVipInGloboNetworkCommand cmd) {
        if (vipV3 == null || vipV3.getId() == null) {
            return new Answer(cmd, false, "Vip request was not created in GloboNetwork");
        }

        return new GloboNetworkVipResponse(
            cmd, vipV3.getId(), vipV3.getName(), ip.getIpString(), vipV3.getIpv4Id(), vipV3.getEnvironmentVipId(), null, cmd.getCache(),
            cmd.getMethodBal(), getPersistenceMethod(cmd.getPersistencePolicy()), cmd.getHealthcheckType(), cmd.getHealthcheck(),
            0, new ArrayList<String>(), new ArrayList<GloboNetworkVipResponse.Real>(), vipV3.getCreated()
        );
    }

    private Answer createVipV2Response(ApplyVipInGloboNetworkCommand cmd) {
        if (vip == null || vip.getId() == null) {
            return new Answer(cmd, false, "Vip request was not created in GloboNetwork");
        }

        return new GloboNetworkVipResponse(
            cmd, vip.getId(), vip.getHost(), null, ip.getId(), cmd.getVipEnvironmentId(), null, vip.getCache(), vip.getMethod(), vip.getPersistence(),
            vip.getHealthcheckType(), vip.getHealthcheck(), vip.getMaxConn(), new ArrayList<String>(), new ArrayList<GloboNetworkVipResponse.Real>(),
            vip.getCreated()
        );
    }

    private void undeployVipV2() throws GloboNetworkException {
        if (vip.getCreated()) {
            s_logger.info("Requesting GloboNetwork to undeploy vip from loadbalancer equipment vip_id=" + vip.getId());
            globoNetworkAPI.getVipAPI().removeScriptVip(vip.getId());
        }
    }

    private void undeployVipV3() throws GloboNetworkException {
        if (vipV3.getCreated()) {
            s_logger.info("Requesting GloboNetwork to undeploy vip from loadbalancer equipment vip_id=" + vipV3.getId());
            globoNetworkAPI.getVipV3API().undeploy(vipV3.getId());
        }
    }

    private void deleteVipV2(Boolean keepIp) throws GloboNetworkException {
        globoNetworkAPI.getVipAPI().removeVip(vip.getId(), keepIp);
    }

    private void deleteVipV3(Boolean keepIp) throws GloboNetworkException {
        globoNetworkAPI.getVipV3API().delete(vipV3.getId(), keepIp);
    }

    List<Long> getPoolIds(){
        List<Long> poolIds = new ArrayList<>();
        if(vipV3 != null){
            Set<Long> poolIdSet = new HashSet<>();
            for(VipV3.Port port : vipV3.getPorts()){
                for(VipV3.Pool pool : port.getPools()){
                    poolIdSet.add(pool.getPoolId());
                }
            }
            poolIds = new ArrayList<>(poolIdSet);
        }else{
            if(vip != null){
                for(com.globo.globonetwork.client.model.Pool pool : vip.getPools()){
                    poolIds.add(pool.getId());
                }
            }
        }
        return poolIds;
    }

    protected static String getPersistenceMethod(LoadBalancingRule.LbStickinessPolicy persistencePolicy) {
        return GloboNetworkResource.PersistenceMethod.fromPersistencePolicy(persistencePolicy);
    }

    @Override
    public String toString() {
        return "VipApiAdapter{" + "vip=" + vip + ", vipV3=" + vipV3 + ", version='" + version + '\'' + '}';
    }
}