package com.globo.globoaclapi.cloudstack.element;

import com.cloud.host.Host;
import com.cloud.network.Network;
import com.cloud.network.rules.FirewallRule;
import com.cloud.utils.component.PluggableService;

public interface GloboAclApiElementService extends PluggableService {

    Host addGloboAclApiHost(Long physicalNetworkId, String url, String username, String password);

    void removeFirewallRule(Network network, FirewallRule rule);

    void createFirewallRule(Network network, FirewallRule rule);
}
