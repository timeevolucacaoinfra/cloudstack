# Licensed to the Apache Software Foundation (ASF) under one
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# import for DNS lookup
import dns.resolver
import requests
import os
import sys

#All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import addGloboDnsHost, createNetwork

#Import Integration Libraries
from marvin.lib.base import Account, VirtualMachine, ServiceOffering, Network, NetworkOffering, NetworkServiceProvider, PhysicalNetwork
from marvin.lib.utils import cleanup_resources
from marvin.lib.common import get_zone, get_domain, get_template

# get globodns endpoint
if os.environ.get('dns_api_host'):
    globodns_host = os.environ.get('dns_api_host')
else:
    sys.exit("The environment variable 'dns_api_host' was not found!")

if os.getenv('dns_api_resolver_nameserver'):
    resolver_nameserver = os.getenv('dns_api_resolver_nameserver')
else:
    sys.exit("The environment variable 'dns_api_resolver_nameserver' was not found!")

if os.getenv('dns_api_auth_token'):
    globodns_payload = {"auth_token": os.getenv('dns_api_auth_token'), "now": "true"}
else:
    sys.exit("The environment variable 'dns_api_auth_token' was not found!")

globodns_export_path = '/bind9/export'
globodns_headers = {"Content-type": "application/json", "Accept": "application/json"}


class Data(object):
    """Test data object that is required to create resources
    """
    def __init__(self):
        self.testdata = {
            # data to create an account
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                "password": "password",
            },
            # data reqd for virtual machine creation
            "virtual_machine": {
                "name": "testglobodns",
                "displayname": "testglobodns",
            },
            # service offering
            "service_offering": {
                "name": "Small Instance",
                "displaytext": "Small Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 100,
            },
            "network_offering": {
                "name": "DNS API Network Offering",
                "displaytext": "DNS API Network Offering",
                "guestiptype": "Isolated",
                "supportedservices": "Dhcp,Dns,SourceNat",
                "traffictype": "GUEST",
                "serviceProviderList": {
                    "Dhcp": "VirtualRouter",
                    "Dns": "GloboDns",
                    "SourceNat": "VirtualRouter"
                }
            },
            # network
            "network": {
                "name": "GloboDnsNetwork",
                "displaytext": "GloboDnsNetwork",
                "networkdomain": "integrationtest.globo.com"
            },
            "globodns_provider": {
                "url": globodns_host,
                "username": "admin@example.com",
                "password": "password"
            },
            "ostype": 'CentOS 5.6 (64-bit)',
        }


class TestVMGloboDns(cloudstackTestCase):
    """Test deploy a VM using DNS API
    """

    def setUp(self):
        self.testdata = Data().testdata
        self.apiclient = self.testClient.getApiClient()

        # Get Zone, Domain and Default Built-in template
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient)
        self.testdata["mode"] = self.zone.networktype
        self.template = get_template(self.apiclient, self.zone.id, self.testdata["ostype"])

        # create a user account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        # create a service offering
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offering"]
        )

        self.physical_network = PhysicalNetwork.list(
            self.apiclient,
            zoneid=self.zone.id
        )

        # set up DNS API Provider
        nw_service_providers = NetworkServiceProvider.list(
            self.apiclient,
            name='GloboDns',
            physicalnetworkid=self.physical_network[0].id
        )
        if isinstance(nw_service_providers, list):
            self.globodns_provider = nw_service_providers[0]
        else:
            self.globodns_provider = NetworkServiceProvider.add(
                self.apiclient,
                'GloboDns',
                self.physical_network[0].id,
                None
            )

        cmd = addGloboDnsHost.addGloboDnsHostCmd()
        cmd.username = self.testdata["globodns_provider"]["username"]
        cmd.password = self.testdata["globodns_provider"]["password"]
        cmd.url = self.testdata["globodns_provider"]["url"]
        cmd.physicalnetworkid = self.physical_network[0].id
        self.apiclient.addGloboDnsHost(cmd)

        if self.globodns_provider.state != 'Enabled':
            NetworkServiceProvider.update(self.apiclient, self.globodns_provider.id, state='Enabled')

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.testdata["network_offering"]
        )
        # Enable Net Offering
        self.network_offering.update(self.apiclient, state='Enabled')

        #build cleanup list
        self.cleanup = [
            self.service_offering,
            self.account,
            self.network_offering
        ]

        self.resolver = dns.resolver.Resolver()
        self.resolver.nameservers = [resolver_nameserver]  # Set nameserver to resolve hostnames

    def test_deploy_vm_with_globodns(self):
        """Test Deploy Virtual Machine with DNS API

        # Validate the following:
        # 1. Network domain is correctly filled
        # 2. VM is accessible through its DNS entry
        """
        # create a network
        cmd = createNetwork.createNetworkCmd()
        cmd.name = self.testdata["network"]["name"]
        cmd.displaytext = self.testdata["network"]["displaytext"]
        cmd.networkdomain = self.testdata["network"]["networkdomain"]
        cmd.account = self.account.name
        cmd.domainid = self.account.domainid
        cmd.networkofferingid = self.network_offering.id
        cmd.zoneid = self.zone.id
        self.network = self.apiclient.createNetwork(cmd)

        list_networks = Network.list(self.apiclient, id=self.network.id)

        self.debug(
            "Verify listNetworks response for network: %s" % self.network.id
        )

        self.assertEqual(
            isinstance(list_networks, list),
            True,
            "List networks response was not a valid list"
        )
        self.assertNotEqual(
            len(list_networks),
            0,
            "List networks response was empty"
        )

        network = list_networks[0]
        self.assertEqual(
            network.id,
            self.network.id,
            "Network ids do not match"
        )
        self.assertEqual(
            network.name,
            self.network.name,
            "Network names do not match"
        )
        self.assertEqual(
            network.networkdomain,
            self.network.networkdomain,
            "Network domains do not match"
        )

        # Throws exception if domain doesn't exist
        # self.resolver.query(network.networkdomain)

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[self.network.id]
        )

        list_vms = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)
        # force export & reload bind in dns-api
        requests.post(globodns_host + globodns_export_path, data=globodns_payload, headers=globodns_headers)

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s" % self.virtual_machine.id
        )

        self.assertEqual(
            isinstance(list_vms, list),
            True,
            "List VM response was not a valid list"
        )
        self.assertNotEqual(
            len(list_vms),
            0,
            "List VM response was empty"
        )

        vm = list_vms[0]
        self.assertEqual(
            vm.id,
            self.virtual_machine.id,
            "Virtual Machine ids do not match"
        )
        self.assertEqual(
            vm.name,
            self.virtual_machine.name,
            "Virtual Machine names do not match"
        )
        self.assertEqual(
            vm.state,
            "Running",
            msg="VM is not in Running state"
        )

        self.assertEqual(
            self.resolver.query(vm.name + '.' + network.networkdomain)[0].address,
            vm.nic[0].ipaddress,
            "Resolved IP address and VM IP address do not match"
        )

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
            self.globodns_provider.delete(self.apiclient)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)
