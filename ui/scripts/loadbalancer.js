// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

(function(cloudStack, $) {

    cloudStack.sections.loadbalancer = {
        title: 'label.load.balancer',
        id: 'loadbalancer',
        preFilter: function(args) {
            return true; // isAdmin();
        },
        listView: {
            id: 'loadbalancers',
            fields: {
                name: { label: 'label.name' },
                publicip: { label: 'label.ip' },
                ports: { label: 'label.port' },
                algorithm: { label: 'label.algorithm' },
                state: { label: 'label.state' },
            },
            dataProvider: function(args) {
                $.ajax({
                    url: createURL("listLoadBalancerRules"),
                    data: {
                        listAll: true,
                    },
                    dataType: "json",
                    async: true,
                    success: function(data) {
                        var loadBalancerData = data.listloadbalancerrulesresponse.loadbalancerrule;
                        $(loadBalancerData).each(function() {
                            this.ports = this.publicport + ':' + this.privateport;
                        });
                        args.response.success({ data: loadBalancerData });
                    },
                    error: function(errorMessage) {
                        args.response.error(errorMessage);
                    }
                });
            },
            detailView: {
                name: 'Load Balancer Details',
                isMaximized: true,
                noCompact: true,
                tabs: {
                    details: {
                        title: 'label.details',
                        fields: [{
                            id: {
                                label: 'label.id'
                            },
                            name: {
                                label: 'label.name'
                            },
                            publicip: {
                                label: 'label.ip'
                            },
                            ports: {
                                label: 'label.port'
                            },
                            algorithm: {
                                label: 'label.algorithm'
                            },
                            stickiness: {
                                label: 'label.stickiness'
                            },
                            healthcheck: {
                                label: 'Healthcheck'
                            },
                        }],
                        dataProvider: function(args) {
                            args.response.success({ data: args.jsonObj });
                        }
                    },
                    networks: {
                        title: 'Networks',
                        listView: {
                            id: 'networks',
                            fields: {
                                name: { label: 'label.name' },
                                cidr: { label: 'label.cidr' },
                            },
                            dataProvider: function(args) {
                                $.ajax({
                                    url: createURL('listNetworks'),
                                    data: {
                                        lbruleid: args.context.loadbalancers[0].id
                                    },
                                    async: false,
                                    success: function(json) {
                                        var networks = json.listnetworksresponse.network;
                                        args.response.success({
                                            data: networks
                                        });
                                    },
                                    error: function(errorMessage) {
                                        args.response.error(errorMessage);
                                    }
                                });
                            },
                            actions: {

                            }
                        }
                    },
                    vms: {
                        title: 'label.virtual.machines',
                        listView: {
                            id: 'vms',
                            fields: {
                                name: { label: 'label.name' },
                                ip: { label: 'label.ip' },
                                network: { label: 'label.network' },
                            },
                            dataProvider: function(args) {
                                $.ajax({
                                    url: createURL('listVirtualMachines'),
                                    data: {
                                        lbruleid: args.context.loadbalancers[0].id,
                                    },
                                    success: function(data) {
                                        args.response.success({
                                            data: $.grep(
                                                data.listvirtualmachinesresponse.virtualmachine ?
                                                data.listvirtualmachinesresponse.virtualmachine : [],
                                                function(instance) {
                                                    return $.inArray(instance.state, [
                                                        'Destroyed', 'Expunging'
                                                    ]) == -1;
                                                }
                                            )
                                        });
                                    },
                                    error: function(errorMessage) {
                                        args.response.error(errorMessage);
                                    }
                                });
                            },
                            actions: {
                            },
                        }
                    },
                },
            },
            actions: {
                remove: {
                    label: 'label.remove',
                    messages: {
                        confirm: function(args) {
                            return 'Are you sure you want to remove load balancer ' + args.context.loadbalancers[0].name + '?';
                        },
                        notification: function(args) {
                            return 'Remove Load Balancer Rule';
                        }
                    },
                    action: function(args) {
                        $.ajax({
                            url: createURL("deleteLoadBalancerRule"),
                            data: {
                                id: args.context.loadbalancers[0].id
                            },
                            dataType: "json",
                            async: true,
                            success: function(json) {
                                $(window).trigger('cloudStack.fullRefresh');
                            },
                            error: function(errorMessage) {
                                var error = (jQuery.parseJSON(errorMessage.responseText)).removeglobonetworkvipresponse;
                                args.response.error(error.errorcode + " : " + error.errortext);
                            }
                        });
                    },
                    notification: {
                        poll: function(args) {
                            args.complete();
                        }
                    }
                },
                add: {
                    label: 'Create new Load Balancer',
                    action: function(args) {
                    },
                    messages: {
                        notification: function(args) {
                            return 'Load balancer created';
                        }
                    },
                    notification: {
                        poll: pollAsyncJobResult
                    }
                }
            }
        }
    };
})(cloudStack, jQuery);
