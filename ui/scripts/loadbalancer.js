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
            return isAdmin();
        },
        listView: {
            id: 'loadbalancers',
            fields: {
                name: { label: 'label.name' },
                publicip: { label: 'label.ip' },
                ports: { label: 'label.port' },
                algorithm: { label: 'label.algorithm' },
                state: {
                    converter: function(str) {
                        // For localization
                        return str;
                    },
                    label: 'label.state',
                    indicator: {
                        'Add': 'off',
                        'Active': 'on',
                        'Revoke': 'off',
                        'Deleting': 'off'
                    }
                },
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
                            healthchecktype: {
                                label: 'Healthcheck Type'
                            },
                            healthcheck: {
                                label: 'Healthcheck'
                            },
                        }],
                        dataProvider: function(args) {
                            $.ajax({
                                url: createURL("listLBStickinessPolicies"),
                                data: {
                                    lbruleid: args.jsonObj.id
                                },
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                    var response = json.listlbstickinesspoliciesresponse.stickinesspolicies[0];
                                    var stickiness = "";
                                    if (!response || !response.stickinesspolicy ||
                                        !response.stickinesspolicy[0] || !response.stickinesspolicy[0].name) {
                                        stickiness = "None";
                                    } else {
                                        stickiness = response.stickinesspolicy[0].name;
                                    }
                                    args.jsonObj.stickiness = stickiness;
                                },
                                error: function (errorMessage) {
                                    args.response.error(errorMessage);
                                }
                            });

                            $.ajax({
                                url: createURL("listLBHealthCheckPolicies"),
                                data: {
                                    lbruleid: args.jsonObj.id
                                },
                                dataType: "json",
                                async: false,
                                success: function(json) {
                                    var healthcheck = json.listlbhealthcheckpoliciesresponse.healthcheckpolicies[0].healthcheckpolicy;
                                    // This logic is for GloboNetwork!
                                    if (!healthcheck || !healthcheck[0] || !healthcheck[0].pingpath || healthcheck[0].pingpath === "") {
                                        // This means it's TCP
                                        args.jsonObj.healthchecktype = "TCP";
                                    } else {
                                        args.jsonObj.healthchecktype = "HTTP";
                                        args.jsonObj.healthcheck = healthcheck[0].pingpath;
                                    }
                                },
                                error: function (errorMessage) {
                                    args.response.error(errorMessage);
                                }
                            });

                            args.response.success({
                                data: args.jsonObj
                            });
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
                                // Make sure load balancer object is always up to date

                                $.ajax({
                                    url: createURL("listLoadBalancerRules"),
                                    data: {
                                        id: args.context.loadbalancers[0].id,
                                    },
                                    dataType: "json",
                                    async: true,
                                    success: function(json) {
                                        args.context.loadbalancers[0] = json.listloadbalancerrulesresponse.loadbalancerrule[0];
                                    },
                                    error: function(errorMessage) {
                                        args.response.error(errorMessage);
                                    }
                                });

                                var networkidslist = [];
                                networkidslist.push(args.context.loadbalancers[0].networkid);
                                networkidslist = networkidslist.concat(args.context.loadbalancers[0].additionalnetworkids);

                                var networks = [];
                                $(networkidslist).each(function() {
                                    $.ajax({
                                        url: createURL('listNetworks'),
                                        data: {
                                            id: this.valueOf()
                                        },
                                        async: false,
                                        success: function(json) {
                                            networks.push(json.listnetworksresponse.network[0]);
                                        },
                                        error: function(errorMessage) {
                                            args.response.error(errorMessage);
                                        }
                                    });
                                });
                                args.response.success({ data: networks });
                            },
                            actions: {
                                remove: {
                                    label: 'label.remove',
                                    messages: {
                                        confirm: function(args) {
                                            return 'Are you sure you want to disassociate network ' + args.context.networks[0].name + ' from load balancer ' + args.context.loadbalancers[0].name + '?';
                                        },
                                        notification: function(args) {
                                            return 'Remove Network From Load Balancer';
                                        }
                                    },
                                    action: function(args) {
                                        $.ajax({
                                            url: createURL("removeNetworksFromLoadBalancerRule"),
                                            data: {
                                                id: args.context.loadbalancers[0].id,
                                                networkids: args.context.networks[0].id
                                            },
                                            dataType: "json",
                                            async: true,
                                            success: function(json) {
                                                $(window).trigger('cloudStack.fullRefresh');
                                            },
                                            error: function(errorMessage) {
                                                args.response.error(errorMessage);
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
                                    label: 'Associate Network to Load Balancer',
                                    createForm: {
                                        title: 'Associate Network to Load Balancer',
                                        fields: {
                                            network: {
                                                label: 'label.network',
                                                validation: { required: true },
                                                select: function(args) {
                                                    var networks = [];
                                                    $.ajax({
                                                        url: createURL("listNetworks"),
                                                        data: {
                                                            supportedservices: 'lb'
                                                        },
                                                        dataType: "json",
                                                        async: false,
                                                        success: function(json) {
                                                            var lb = args.context.loadbalancers[0];
                                                            $(json.listnetworksresponse.network).each(function() {
                                                                // Remove those that are already associated to load balancer
                                                                if (lb.networkid != this.id && lb.additionalnetworkids.indexOf(this.id) === -1) {
                                                                    networks.push({id: this.id, description: this.name});
                                                                }
                                                            });
                                                        }
                                                    });
                                                    args.response.success({
                                                        data: networks
                                                    });
                                                }
                                            }
                                        },
                                    },
                                    action: function(args) {
                                        $.ajax({
                                            url: createURL("assignNetworksToLoadBalancerRule"),
                                            data: {
                                                id: args.context.loadbalancers[0].id,
                                                networkids: args.data.network
                                            },
                                            dataType: "json",
                                            async: true,
                                            success: function(json) {
                                                // Refresh load balancer list
                                                $(window).trigger('cloudStack.fullRefresh');
                                            },
                                            error: function(errorMessage) {
                                                args.response.error(errorMessage);
                                            }
                                        });
                                    },
                                    messages: {
                                        notification: function(args) {
                                            return 'Network associated to Load Balancer';
                                        }
                                    },
                                    notification: {
                                        poll: pollAsyncJobResult
                                    }
                                }
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
                                    url: createURL('listLoadBalancerRuleInstances'),
                                    data: {
                                        id: args.context.loadbalancers[0].id,
                                    },
                                    success: function(data) {
                                        var instances = [];
                                        response = data.listloadbalancerruleinstancesresponse.loadbalancerruleinstance ?
                                            data.listloadbalancerruleinstancesresponse.loadbalancerruleinstance : [];
                                            $(response).each(function() {
                                                var ipaddress;
                                                var networkname;
                                                $(this.nic).each(function() {
                                                    // Find the NIC that is in the load balancer
                                                    if (args.context.loadbalancers[0].networkid === this.networkid ||
                                                        args.context.loadbalancers[0].additionalnetworkids.indexOf(this.networkid) !== -1) {
                                                        ipaddress = this.ipaddress;
                                                        networkname = this.networkname;
                                                        return false; // break 'each' loop since we've found it
                                                    }
                                                });
                                                instances.push({id: this.id, name: this.name, ip: ipaddress, network: networkname });
                                            });
                                        args.response.success({
                                            data: instances
                                        });
                                    },
                                    error: function(errorMessage) {
                                        args.response.error(errorMessage);
                                    }
                                });
                            },
                            actions: {
                                remove: {
                                    label: 'label.remove',
                                    messages: {
                                        confirm: function(args) {
                                            return 'Are you sure you want to remove VM ' + args.context.vms[0].name + ' from load balancer ' + args.context.loadbalancers[0].name + '?';
                                        },
                                        notification: function(args) {
                                            return 'Remove VM From Load Balancer';
                                        }
                                    },
                                    action: function(args) {
                                        $.ajax({
                                            url: createURL("removeFromLoadBalancerRule"),
                                            data: {
                                                id: args.context.loadbalancers[0].id,
                                                virtualmachineids: args.context.vms[0].id
                                            },
                                            dataType: "json",
                                            async: true,
                                            success: function(data) {
                                                args.response.success({
                                                    _custom: {
                                                        jobId: data.removefromloadbalancerruleresponse.jobid,
                                                        notification: {
                                                            label: 'label.remove.vms.from.lb',
                                                            poll: pollAsyncJobResult
                                                        },
                                                        onComplete: function(args) {
                                                            $(window).trigger('cloudStack.fullRefresh');
                                                        }
                                                    }
                                                });
                                            },
                                            error: function(errorMessage) {
                                                args.response.error(errorMessage);
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
                                    label: 'Add VM to Load Balancer',
                                    createForm: {
                                        title: 'Add VM to Load Balancer',
                                        fields: {
                                            vm: {
                                                label: 'VM',
                                                validation: { required: true },
                                                select: function(args) {
                                                    var networks = [];
                                                    $.ajax({
                                                        url: createURL("listLoadBalancerRuleInstances"),
                                                        data: {
                                                            id: args.context.loadbalancers[0].id,
                                                            applied: false,
                                                        },
                                                        dataType: "json",
                                                        async: false,
                                                        success: function(json) {
                                                            var instances = [];
                                                            var lb = args.context.loadbalancers[0];
                                                            $(json.listloadbalancerruleinstancesresponse.loadbalancerruleinstance).each(function() {
                                                                instances.push({id: this.id, description: this.name });
                                                            });
                                                            args.response.success({
                                                                data: instances
                                                            });
                                                        }
                                                    });
                                                }
                                            }
                                        },
                                    },
                                    action: function(args) {
                                        $.ajax({
                                            url: createURL("assignToLoadBalancerRule"),
                                            data: {
                                                id: args.context.loadbalancers[0].id,
                                                virtualmachineids: args.data.vm
                                            },
                                            dataType: "json",
                                            async: true,
                                            success: function(data) {
                                                args.response.success({
                                                    _custom: {
                                                        jobId: data.assigntoloadbalancerruleresponse.jobid,
                                                        notification: {
                                                            label: 'label.add.vms.to.lb',
                                                            poll: pollAsyncJobResult
                                                        },
                                                        onComplete: function(args) {
                                                            $(window).trigger('cloudStack.fullRefresh');
                                                        }
                                                    },
                                                });
                                            },
                                            error: function(errorMessage) {
                                                args.response.error(errorMessage);
                                            }
                                        });
                                    },
                                    messages: {
                                        notification: function(args) {
                                            return 'VM added to Load Balancer';
                                        }
                                    },
                                    notification: {
                                        poll: pollAsyncJobResult
                                    }
                                }
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
                        var ipToBeReleased = args.context.loadbalancers[0].publicipid;
                        $.ajax({
                            url: createURL("deleteLoadBalancerRule"),
                            data: {
                                id: args.context.loadbalancers[0].id
                            },
                            dataType: "json",
                            async: true,
                            success: function(data) {
                                var jobID = data.deleteloadbalancerruleresponse.jobid;
                                args.response.success({
                                    _custom: {
                                        jobId: jobID,
                                        notification: {
                                            label: 'label.action.delete.load.balancer',
                                            poll: pollAsyncJobResult
                                        },
                                        onComplete: function(args) {
                                            $.ajax({
                                                url: createURL('disassociateIpAddress'),
                                                data: {
                                                    id: ipToBeReleased
                                                },
                                                dataType: 'json',
                                                async: true,
                                                success: function(data) {
                                                    $(window).trigger('cloudStack.fullRefresh');
                                                },
                                                error: function(data) {
                                                    args.response.error(parseXMLHttpResponse(data));
                                                }
                                            });
                                        }
                                    }
                                });
                            },
                            error: function(errorMessage) {
                                args.response.error(parseXMLHttpResponse(data));
                            }
                        });
                    },
                    notification: {
                        poll: pollAsyncJobResult
                    },
                },
                add: {
                    label: 'Create new Load Balancer',
                    createForm: {
                        fields: {
                            name: {
                                label: 'label.name',
                                validation: {
                                    required: true
                                }
                            },
                            publicport: {
                                label: 'label.public.port',
                                validation: {
                                    required: true
                                }
                            },
                            privateport: {
                                label: 'label.private.port',
                                validation: {
                                    required: true
                                }
                            },
                            isportable: {
                                label: 'label.cross.zones',
                                validation: {
                                    required: true
                                },
                                isHidden: true,
                                defaultValue: "true",
                                select: function(args) {
                                    var items = [];
                                    items.push({
                                        id: "false",
                                        description: _l('label.no')
                                    });
                                    items.push({
                                        id: "true",
                                        description: _l('label.yes')
                                    });
                                    args.response.success({
                                        data: items
                                    });
                                },
                            },
                            network: {
                                label: 'label.network',
                                validation: {
                                    required: true
                                },
                                select: function(args) {
                                    var networks = [];
                                    $.ajax({
                                        url: createURL("listNetworks"),
                                        data: {
                                            supportedservices: 'lb'
                                        },
                                        dataType: "json",
                                        async: false,
                                        success: function(json) {
                                            $(json.listnetworksresponse.network).each(function() {
                                                networks.push({id: this.id, description: this.name});
                                            });
                                        },
                                        error: function(json) {
                                            args.response.error(parseXMLHttpResponse(json));
                                        }
                                    });
                                    args.response.success({
                                        data: networks
                                    });
                                }
                            },
                            lbnetwork: {
                                label: 'LB Network',
                                validation: {
                                    required: true
                                },
                                dependsOn: ['network'],
                                select: function(args) {
                                    var network;
                                    $.ajax({
                                        url: createURL("listNetworks"),
                                        data: {
                                            id: args.data.network
                                        },
                                        dataType: "json",
                                        async: false,
                                        success: function(json) {
                                            network = json.listnetworksresponse.network[0];
                                        },
                                        error: function(json) {
                                            args.response.error(parseXMLHttpResponse(json));
                                        }
                                    });

                                    $.ajax({
                                        url: createURL("listGloboNetworkLBNetworks"),
                                        data: {
                                            physicalnetworkid: network.physicalnetworkid,
                                            networkid: network.id
                                        },
                                        dataType: "json",
                                        async: false,
                                        success: function(json) {
                                            var data = [];
                                            $(json.listglobonetworklbnetworksresponse.globonetworklbnetworks).each(function() {
                                                data.push({id: this.globonetworklbnetworkid, name: this.name, description: this.name});
                                            });
                                            args.response.success({
                                                data: data
                                            });
                                        },
                                        error: function(json) {
                                            args.response.error(parseXMLHttpResponse(json));
                                        }
                                    });
                                }
                            },
                            algorithm: {
                                label: 'label.algorithm',
                                validation: {
                                    required: true
                                },
                                dependsOn: ['network'],
                                select: function(args) {
                                    var network;
                                    $.ajax({
                                        url: createURL("listNetworks"),
                                        data: {
                                            id: args.data.network
                                        },
                                        dataType: "json",
                                        async: false,
                                        success: function(json) {
                                            network = json.listnetworksresponse.network[0];
                                        },
                                        error: function(json) {
                                            args.response.error(parseXMLHttpResponse(json));
                                        }
                                    });


                                    var lbService = $.grep(network.service, function(service) {
                                        return service.name == 'Lb';
                                    })[0];

                                    var algorithmCapabilities = $.grep(
                                        lbService.capability,
                                        function(capability) {
                                            return capability.name == 'SupportedLbAlgorithms';
                                        }
                                    )[0];

                                    var algorithms = algorithmCapabilities.value.split(',');
                                    var data = [];
                                    $(algorithms).each(function() {
                                        data.push({id: this.valueOf().trim(), name: this.valueOf().trim(), description: _l('label.lb.algorithm.' + this.valueOf().trim())});
                                    });
                                    args.response.success({
                                        data: data
                                    });
                                },
                            },

                            sticky: {
                                label: 'label.stickiness',
                                dependsOn: ['network'],
                                select: function(args) {
                                    var network;
                                    $.ajax({
                                        url: createURL("listNetworks"),
                                        data: {
                                            id: args.data.network
                                        },
                                        dataType: "json",
                                        async: false,
                                        success: function(json) {
                                            network = json.listnetworksresponse.network[0];
                                        },
                                        error: function(json) {
                                            args.response.error(parseXMLHttpResponse(json));
                                        }
                                    });


                                    var lbService = $.grep(network.service, function(service) {
                                        return service.name == 'Lb';
                                    })[0];

                                    var stickinessCapabilities = $.grep(
                                        lbService.capability,
                                        function(capability) {
                                            return capability.name == 'SupportedStickinessMethods';
                                        }
                                    )[0];

                                    var stickinessMethods = jQuery.parseJSON(stickinessCapabilities.value);
                                    var data = [];
                                    // Default None value
                                    data.push({id: 'None', name: 'None', description: 'None'});
                                    $(stickinessMethods).each(function() {
                                        data.push({id: this.methodname, name: this.methodname, description: this.methodname});
                                    });
                                    args.response.success({
                                        data: data
                                    });
                                },
                            },

                            healthcheck: {
                                label: 'Healthcheck',
                            },
                        },
                    },
                    action: function(args) {
                        var network;
                        $.ajax({
                            url: createURL("listNetworks"),
                            data: {
                                id: args.data.network
                            },
                            dataType: "json",
                            async: false,
                            success: function(json) {
                                network = json.listnetworksresponse.network[0];
                            }
                        });

                        var networkoffering;
                        $.ajax({
                            url: createURL("listNetworkOfferings"),
                            data: {
                                id: network.networkofferingid
                            },
                            dataType: "json",
                            async: false,
                            success: function(json) {
                                networkoffering = json.listnetworkofferingsresponse.networkoffering[0];
                            },
                            error: function(json) {
                                args.response.error(parseXMLHttpResponse(json));
                            }
                        });

                        var lbService = $.grep(networkoffering.service, function(service) {
                            return service.name == 'Lb';
                        })[0];

                        var provider = lbService.provider[0].name;

                        var url;
                        var data = {};
                        if (args.data.isportable === 'true' && provider === 'GloboNetwork') {
                            url = "acquireNewLBIp";
                            data = {
                                networkid: args.data.network,
                                lbnetworkid: args.data.lbnetwork
                            };
                        } else if (provider === 'GloboNetwork') {
                            // GloboNetwork should only work with portable IPs
                            args.response.error("Network is provided by GloboNetwork and can only manage cross zone IPs (portable)");
                            return;
                        } else {
                            url = "associateIpAddress";
                            data = {
                                isportable: args.data.isportable,
                                networkid: args.data.network
                            };
                        }

                        $.ajax({
                            url: createURL(url),
                            data: data,
                            dataType: "json",
                            async: true,
                            success: function(json) {
                                var ipId = json.associateipaddressresponse.id;

                                var data = {
                                    algorithm: args.data.algorithm,
                                    name: args.data.name,
                                    privateport: args.data.privateport,
                                    publicport: args.data.publicport,
                                    openfirewall: false,
                                    networkid: args.data.network,
                                    publicipid: ipId
                                };

                                var stickyData = {methodname: args.data.sticky.valueOf(), stickyName: args.data.sticky.valueOf()};

                                var healthcheckPingPath = args.data.healthcheck.valueOf().trim();

                                $.ajax({
                                    url: createURL('createLoadBalancerRule'),
                                    data: data,
                                    dataType: 'json',
                                    async: true,
                                    success: function(data) {
                                        var jobID = data.createloadbalancerruleresponse.jobid;
                                        var lbID = data.createloadbalancerruleresponse.id;
                                        // Create stickiness policy
                                        if (stickyData &&
                                            stickyData.methodname &&
                                            stickyData.methodname != 'None') {
                                            cloudStack.lbStickyPolicy.actions.add(lbID,
                                                stickyData,
                                                args.complete, args.error);
                                        }

                                        // Create healthcheck
                                        var datahealthcheck = {
                                            lbruleid: lbID,
                                            pingpath: healthcheckPingPath
                                        };

                                        $.ajax({
                                            url: createURL('createLBHealthCheckPolicy'),
                                            data: datahealthcheck,
                                            success: function(json) {
                                                args.response.success({
                                                    _custom: {
                                                        jobId: jobID,
                                                        getUpdatedItem: function(json) {
                                                            return json.queryasyncjobresultresponse.jobresult.loadbalancer;
                                                        },
                                                        notification: {
                                                            poll: pollAsyncJobResult
                                                        }
                                                    }
                                                });
                                            },
                                            error: function(json) {
                                                cloudStack.dialog.notice({
                                                    message: _s(json.responseText)
                                                });
                                            }

                                        });
                                    },
                                    error: function(json) {
                                        args.response.error(parseXMLHttpResponse(json));
                                    }
                                });
                            },
                            error: function(json) {
                                args.response.error(parseXMLHttpResponse(json));
                            }
                        });
                    },
                    messages: {
                        notification: function(args) {
                            return 'Load Balancer created';
                        }
                    },
                    notification: {
                        label: 'Load Balancer created',
                        poll: pollAsyncJobResult
                    }
                }
            }
        }
    };
})(cloudStack, jQuery);
