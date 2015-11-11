package com.globo.globoaclapi.cloudstack.manager;

import com.cloud.host.Host;
import com.cloud.network.Network;
import com.cloud.network.rules.FirewallRule;

import java.util.List;

public interface GloboACLService {

    List<FirewallRule> listACLRules(Network network);

    void createACLRule(Network network, FirewallRule rule);

    void removeACLRule(Network network, Long ruleId);

    Host addGloboAclApiHost(Long physicalNetworkId, String url, String username, String password);
}
