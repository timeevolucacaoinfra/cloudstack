var globoNetworkAPI;
globoNetworkAPI = globoNetworkAPI || {};

(function(globoNetworkAPI, $) {

    // capabilities
    var capabilities = null;

    globoNetworkAPI.getCapability = function(name) {
        if (!capabilities) {
            $.ajax({
                url: createURL('listGloboNetworkCapabilities'),
                async: false,
                success: function(json) {
                    capabilities = json.listglobonetworkcapabilitiesresponse.globoNetworkCapability;
                }
            });
        }
        return capabilities[name];
    };

    // Network Dialog (used in system.js and network.js)
    globoNetworkAPI.networkDialog = {
        zoneObjs: [],
        physicalNetworkObjs: [],
        networkOfferingObjs: [],
        globoNetworkEnvironmentsObjs: [],
        def: {
            label: 'Add GloboNetwork',

            messages: {
                notification: function(args) {
                    return 'Add GloboNetwork';
                }
            },

            preFilter: function(args) {
                return globoNetworkAPI.getCapability('enabled');
            },

            createForm: {
                title: 'Add GloboNetwork',

                preFilter: function(args) {
                    if (!globoNetworkAPI.getCapability('supportCustomNetworkDomain')) {
                        args.$form.find('.form-item[rel=networkdomain]').hide();
                    }
                },

                fields: {
                    name: {
                        label: 'label.name',
                        validation: {
                            required: true
                        }
                    },
                    description: {
                        label: 'label.description',
                        validation: {
                            required: true
                        }
                    },

                    ipv6: {
                        label: 'IPv6',
                        isBoolean: true,
                        isChecked: false,
                    },
                    networkdomain: {
                        label: 'label.network.domain',
                        docID: 'helpGuestNetworkZoneNetworkDomain'
                    },
                    zoneId: {
                        label: 'label.zone',
                        validation: {
                            required: true
                        },
                        select: function(args) {
                            if ('zones' in args.context) {
                                globoNetworkAPI.networkDialog.zoneObjs = args.context.zones;
                            } else {
                                $.ajax({
                                    url: createURL('listZones'),
                                    data: {
                                        networktype: 'Advanced'
                                    },
                                    async: false,
                                    success: function(json) {
                                        globoNetworkAPI.networkDialog.zoneObjs = []; //reset
                                        var zones = json.listzonesresponse.zone;
                                        if (zones) {
                                            zones.forEach(function(zone) {
                                                globoNetworkAPI.networkDialog.zoneObjs.push(zone);
                                            });
                                        }
                                    }
                                });
                            }
                            args.response.success({
                                data: $.map(globoNetworkAPI.networkDialog.zoneObjs, function(zone) {
                                    return {
                                        id: zone.id,
                                        description: zone.name
                                    };
                                })
                            });
                        }
                    },
                    napiEnvironmentId: {
                        label: 'Environment',
                        dependsOn: 'zoneId',
                        select: function(args) {
                            if ('globoNetworkEnvironmentsObjs' in args.context) {
                                globoNetworkAPI.networkDialog.globoNetworkEnvironmentsObjs = args.context.globoNetworkEnvironmentsObjs;
                            } else {
                                var envObjs = [];
                                if (args.zoneId) {
                                    $.ajax({
                                        url: createURL('listGloboNetworkEnvironments'),
                                        data: {
                                            zoneid: args.zoneId
                                        },
                                        async: false,
                                        success: function(json) {
                                            envObjs = json.listglobonetworkenvironmentsresponse.globonetworkenvironment;
                                        }
                                    });
                                }
                                globoNetworkAPI.networkDialog.globoNetworkEnvironmentsObjs = envObjs;
                            }
                            args.response.success({
                                data: $.map(globoNetworkAPI.networkDialog.globoNetworkEnvironmentsObjs, function(env) {
                                    return {
                                        id: env.napienvironmentid,
                                        description: env.name
                                    };
                                })
                            });
                        }
                    },
                    networkOfferingId: {
                        label: 'label.network.offering',
                        dependsOn: ['zoneId', 'scope'],
                        select: function(args) {
                            var zoneId = args.zoneId;
                            var data = {
                                state: 'Enabled',
                                zoneid: zoneId
                            };

                            //Network tab in Guest Traffic Type in Infrastructure menu is only available when it's under Advanced zone.
                            //zone dropdown in add guest network dialog includes only Advanced zones.
                            if (args.scope == "zone-wide" || args.scope == "domain-specific") {
                                $.extend(data, {
                                    guestiptype: 'Shared'
                                });
                            }

                            var items = [];
                            if (zoneId) {
                                $.ajax({
                                    url: createURL('listNetworkOfferings'),
                                    data: data,
                                    async: false,
                                    success: function(json) {
                                        globoNetworkAPI.networkDialog.networkOfferingObjs = json.listnetworkofferingsresponse.networkoffering;
                                        if (globoNetworkAPI.networkDialog.networkOfferingObjs) {
                                            var selectedZoneObj = {};
                                            if (globoNetworkAPI.networkDialog.zoneObjs) {
                                                for (var index in globoNetworkAPI.networkDialog.zoneObjs) {
                                                    if (globoNetworkAPI.networkDialog.zoneObjs[index].id == zoneId) {
                                                        selectedZoneObj = globoNetworkAPI.networkDialog.zoneObjs[index];
                                                        break;
                                                    }
                                                }
                                            }
                                            for (var i = 0; i < globoNetworkAPI.networkDialog.networkOfferingObjs.length; i++) {
                                                //for zone-wide network in Advanced SG-enabled zone, list only SG network offerings
                                                if (selectedZoneObj.networktype === 'Advanced' && selectedZoneObj.securitygroupsenabled) {
                                                    if (args.scope == "zone-wide") {
                                                        var includingSecurityGroup = false;
                                                        var serviceObjArray = globoNetworkAPI.networkDialog.networkOfferingObjs[i].service;
                                                        for (var k = 0; k < serviceObjArray.length; k++) {
                                                            if (serviceObjArray[k].name == "SecurityGroup") {
                                                                includingSecurityGroup = true;
                                                                break;
                                                            }
                                                        }
                                                        if (!includingSecurityGroup)
                                                            continue; //skip to next network offering
                                                    }
                                                }
                                                items.push({
                                                    id: globoNetworkAPI.networkDialog.networkOfferingObjs[i].id,
                                                    description: globoNetworkAPI.networkDialog.networkOfferingObjs[i].displaytext
                                                });
                                            }
                                        }
                                    }
                                });
                            }
                            args.response.success({
                                data: items
                            });
                        }
                    },
                    isNetworkAdvanced: {
                                label: 'label.show.advanced.settings',
                                dependsOn: ['network'],
                                isBoolean: true,
                                defaultValue: false,
                                isChecked: false,
                    },
                    hostCount: {
                        label: 'Max hosts supported',
                        isHidden: function (args) {
                                    var isAdvancedChecked = $('input[name=isNetworkAdvanced]:checked').length > 0;
                                    return !isAdvancedChecked;
                                },
                        dependsOn: ['isNetworkAdvanced'],
                        docID: 'helpHostCount',
                        select: function(args) {
                            args.response.success({
                                data: [
                                    {id: '', name: '--- Select ---', description: '--- Select ---'},
                                    {id: '29', name: '2', description: '2'},
                                    {id: '28', name: '10', description: '10'},
                                    {id: '27', name: '26', description: '26'},
                                    {id: '26', name: '58', description: '58'},
                                    {id: '25', name: '122', description: '122'},
                                    {id: '24', name: '250', description: '250'}
                                    ]
                            })
                        }

                    }
                }
            },

            action: function(args) { //Add GloboNetwork network in advanced zone
                var $form = args.$form;

                var array1 = [];
                array1.push("&zoneId=" + args.data.zoneId);
                array1.push("&networkOfferingId=" + args.data.networkOfferingId);

                //Pass physical network ID to addNetworkViaGloboNetworkCmd API only when network offering's guestiptype is Shared.
                var selectedNetworkOfferingObj;
                if (globoNetworkAPI.networkDialog.networkOfferingObjs) {
                    for (var index in globoNetworkAPI.networkDialog.networkOfferingObjs) {
                        if (globoNetworkAPI.networkDialog.networkOfferingObjs[index].id == args.data.networkOfferingId) {
                            selectedNetworkOfferingObj = globoNetworkAPI.networkDialog.networkOfferingObjs[index];
                            break;
                        }
                    }
                }

                if (selectedNetworkOfferingObj.guestiptype == "Shared")
                    array1.push("&napienvironmentid=" + args.data.napiEnvironmentId);


                array1.push("&name=" + todb(args.data.name));
                array1.push("&displayText=" + todb(args.data.description));
                var useIpv6 = (args.data.ipv6 == "on");
                array1.push("&isipv6=" + useIpv6);

                if (globoNetworkAPI.getCapability('supportCustomNetworkDomain')) {
                    array1.push("&networkdomain=" + todb(args.data.networkdomain));
                }

                if (args.context.projects && args.context.projects[0].id) {
                    // Network is created in project, if it's in the context
                    // Otherwise, create in user's account
                    array1.push("&projectid=" + args.context.projects[0].id);
                } else {
                    array1.push("&domainId=" + args.context.users[0].domainid);
                    array1.push("&account=" + args.context.users[0].account);
                }

                array1.push("&acltype=account");

                if ( args.data.hostCount !== '') {
                    array1.push("&subnet=" + args.data.hostCount);
                }
                
                $.ajax({
                    url: createURL("addNetworkViaGloboNetwork" + array1.join(""), {
                        ignoreProject: true
                    }),
                    dataType: "json",
                    success: function(json) {
                        var item = json.addnetworkviaglobonetworkresponse.network;
                        args.response.success({
                            data: item
                        });
                    },
                    error: function(XMLHttpResponse) {
                        var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                        args.response.error(errorMsg);
                    }
                });
            },
            notification: {
                poll: function(args) {
                    args.complete();
                }
            }
        }
    };

    // GloboNetwork provider detail view (used in system.js)
    var addGloboNetworkHost = function(args, physicalNetworkObj, apiCmd, apiCmdRes) {
        var array1 = [];
        array1.push("&physicalnetworkid=" + physicalNetworkObj.id);
        array1.push("&username=" + todb(args.data.username));
        array1.push("&password=" + todb(args.data.password));
        array1.push("&url=" + todb(args.data.url));

        $.ajax({
            url: createURL(apiCmd + array1.join("")),
            dataType: "json",
            type: "POST",
            success: function(json) {
                var jid = json[apiCmdRes].jobid;
                args.response.success({
                    _custom: {
                        jobId: jid,
                    }
                });
            }
        });
    };

    var addGloboNetworkEnvironment = function(args, physicalNetworkObj, apiCmd, apiCmdRes, apiCmdObj) {
        var array1 = [];
        array1.push("&physicalnetworkid=" + physicalNetworkObj.id);
        array1.push("&napienvironmentid=" + todb(args.data.napiEnvironmentId));
        array1.push("&name=" + todb(args.data.name));

        $.ajax({
            url: createURL(apiCmd + array1.join("")),
            dataType: "json",
            type: "POST",
            success: function(json) {
                var jid = json[apiCmdRes].jobid;
                args.response.success({
                    _custom: {
                        jobId: jid,
                        getUpdatedItem: function(json) {
                            $(window).trigger('cloudStack.fullRefresh');
                        }
                    }
                });
            }
        });
    };

    globoNetworkAPI.provider = function(args) {
        var getNspMap = args.getNspMap,
            getNspHardcodingArray = args.getNspHardcodingArray,
            networkProviderActionFilter = args.networkProviderActionFilter,
            refreshNspData = args.refreshNspData,
            getSelectedPhysicalNetworkObj = args.getSelectedPhysicalNetworkObj;

        return {
            isMaximized: true,
            type: 'detailView',
            id: 'globoNetworkProvider',
            label: 'label.globoNetwork',
            tabs: {
                details: {
                    title: 'label.details',
                    fields: [{
                        name: {
                            label: 'label.name'
                        },
                        state: {
                            label: 'label.state'
                        }
                    }],
                    dataProvider: function(args) {
                        refreshNspData("GloboNetwork");
                        var providerObj;
                        $(getNspHardcodingArray()).each(function() {
                            if (this.id == "GloboNetwork") {
                                providerObj = this;
                                return false; //break each loop
                            }
                        });
                        args.response.success({
                            data: providerObj,
                            actionFilter: networkProviderActionFilter('GloboNetwork')
                        });
                    }
                },
                instances: {
                    title: 'Environments',
                    listView: {
                        label: 'Environments',
                        id: 'napienvironments',
                        fields: {
                            name: {
                                label: 'Local Name'
                            },
                            environmentid: {
                                label: 'GloboNetwork Environment ID'
                            }
                        },
                        dataProvider: function(args) {
                            var filter;
                            if (args.filterBy.search.value) {
                                filter = args.filterBy.search.value;
                            }

                            var items = [];
                            $.ajax({
                                url: createURL("listGloboNetworkEnvironments&physicalnetworkid=" + getSelectedPhysicalNetworkObj().id),
                                success: function(json) {
                                    $(json.listglobonetworkenvironmentsresponse.globonetworkenvironment).each(function() {
                                        if (this.name.match(new RegExp(filter, "i"))) {
                                            items.push({
                                                name: this.name,
                                                environmentid: this.napienvironmentid,
                                            });
                                        }
                                    });
                                    args.response.success({
                                        data: items
                                    });
                                }
                            });
                        },
                        actions: {
                            add: {
                                label: 'Add Environment',
                                createForm: {
                                    title: 'Add a GloboNetwork Environment',
                                    fields: {
                                        name: {
                                            label: 'Name',
                                            validation: {
                                                required: true
                                            }
                                        },
                                        napiEnvironmentId: {
                                            label: 'Environment',
                                            validation: {
                                                required: true
                                            },
                                            select: function(args) {
                                                $.ajax({
                                                    url: createURL("listAllEnvironmentsFromGloboNetwork&physicalnetworkid=" + getSelectedPhysicalNetworkObj().id),
                                                    dataType: "json",
                                                    async: false,
                                                    success: function(json) {
                                                        var items = [];
                                                        $(json.listallenvironmentsfromglobonetworkresponse.globonetworkenvironment).each(function() {
                                                            items.push({
                                                                id: this.environmentId,
                                                                description: this.environmentFullName
                                                            });
                                                        });
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    }
                                                });
                                            }
                                        }
                                    }
                                },
                                action: function(args) {
                                    addGloboNetworkEnvironment(args, getSelectedPhysicalNetworkObj(), "addGloboNetworkEnvironment", "addglobonetworkenvironmentresponse", "globonetworkenvironment");
                                },
                                messages: {
                                    notification: function(args) {
                                        return 'Environment added successfully';
                                    }
                                },
                                notification: {
                                    poll: pollAsyncJobResult
                                }
                            },
                            remove: {
                                label: 'label.remove',
                                messages: {
                                    confirm: function(args) {
                                        return 'Are you sure you want to remove environment ' + args.context.napienvironments[0].name + '(' + args.context.napienvironments[0].environmentid + ')?';
                                    },
                                    notification: function(args) {
                                        return 'Remove GloboNetwork environment';
                                    }
                                },
                                action: function(args) {
                                    var physicalnetworkid = args.context.physicalNetworks[0].id;
                                    var environmentid = args.context.napienvironments[0].environmentid;
                                    $.ajax({
                                        url: createURL("removeGloboNetworkEnvironment&physicalnetworkid=" + physicalnetworkid + "&environmentid=" + environmentid),
                                        dataType: "json",
                                        async: true,
                                        success: function(json) {
                                            args.response.success();
                                            $(window).trigger('cloudStack.fullRefresh');
                                        },
                                        error: function(XMLHttpResponse) {
                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                            args.response.error(errorMsg);
                                        }
                                    });
                                },
                                notification: {
                                    poll: function(args) {
                                        args.complete();
                                    }
                                }
                            },
                        },
                        detailView: {
                            tabs: {
                                lbenvironments: {
                                    title: 'LB Environments',
                                    listView: {
                                        label: 'LB Environments',
                                        id: 'globolbenvironments',
                                        fields: {
                                            name: {
                                                label: 'Local Name'
                                            },
                                            globonetworklbenvironmentid: {
                                                label: 'Load Balancer Environment ID'
                                            }
                                        },
                                        dataProvider: function(args) {
                                            var filter;
                                            if (args.filterBy.search.value) {
                                                filter = args.filterBy.search.value;
                                            }

                                            var items = [];
                                            $.ajax({
                                                url: createURL("listGloboNetworkLBEnvironments&physicalnetworkid=" + getSelectedPhysicalNetworkObj().id + "&environmentid=" + args.context.napienvironments[0].environmentid),
                                                success: function(json) {
                                                    $(json.listglobonetworklbenvironmentsresponse.globonetworklbenvironments).each(function() {
                                                        if (this.name.match(new RegExp(filter, "i"))) {
                                                            items.push({
                                                                name: this.name,
                                                                globonetworklbenvironmentid: this.globonetworklbenvironmentid,
                                                            });
                                                        }
                                                    });
                                                    args.response.success({
                                                        data: items
                                                    });
                                                }
                                            });
                                        },
                                        actions: {
                                            add: {
                                                label: 'Add LB Environment',
                                                createForm: {
                                                    title: 'Add LB Environment',
                                                    fields: {
                                                        name: {
                                                            label: 'Name',
                                                            validation: {
                                                                required: true
                                                            }
                                                        },
                                                        globolbenvironmentid: {
                                                            label: 'LB Environment ID',
                                                            validation: {
                                                                required: true
                                                            }
                                                        }
                                                    }
                                                },
                                                action: function(args) {
                                                    var physicalnetworkid = getSelectedPhysicalNetworkObj().id;
                                                    var environmentid = args.context.napienvironments[0].environmentid;
                                                    var globolbenvironmentid = args.data.globolbenvironmentid;
                                                    var name = args.data.name;
                                                    $.ajax({
                                                        url: createURL("addGloboNetworkLBEnvironment"),
                                                        data: {
                                                            physicalnetworkid: physicalnetworkid,
                                                            napienvironmentid: environmentid,
                                                            globolbenvironmentid: globolbenvironmentid,
                                                            name: name
                                                        },
                                                        dataType: "json",
                                                        async: true,
                                                        success: function(json) {
                                                            args.response.success();
                                                            setTimeout(function() {
                                                                $(window).trigger('cloudStack.fullRefresh');
                                                            }, 500);
                                                        },
                                                        error: function(errorMessage) {
                                                            args.response.error(parseXMLHttpResponse(errorMessage));
                                                        }
                                                    });
                                                },
                                                messages: {
                                                    notification: function(args) {
                                                        return 'Add LB Environment';
                                                    }
                                                },
                                                notification: {
                                                    poll: function(args) {
                                                        args.complete();
                                                    }
                                                }
                                            },
                                            remove: {
                                                label: 'label.remove',
                                                messages: {
                                                    confirm: function(args) {
                                                        return 'Are you sure you want to remove LB Environment ' + args.context.globolbenvironments[0].name + '(' + args.context.globolbenvironments[0].globonetworklbenvironmentid + ')?';
                                                    },
                                                    notification: function(args) {
                                                        return 'Remove Load Balancer Environment';
                                                    }
                                                },
                                                action: function(args) {
                                                    var physicalnetworkid = getSelectedPhysicalNetworkObj().id;
                                                    var environmentid = args.context.napienvironments[0].environmentid;
                                                    var globolbenvironmentid = args.context.globolbenvironments[0].globonetworklbenvironmentid;
                                                    $.ajax({
                                                        url: createURL("removeGloboNetworkLBEnvironment"),
                                                        data: {
                                                            physicalnetworkid: physicalnetworkid,
                                                            napienvironmentid: environmentid,
                                                            globolbenvironmentid: globolbenvironmentid
                                                        },
                                                        dataType: "json",
                                                        success: function(json) {
                                                            args.response.success();
                                                        },
                                                        error: function(XMLHttpResponse) {
                                                            var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                            args.response.error(errorMsg);
                                                        }
                                                    });
                                                },
                                                notification: {
                                                    poll: function(args) {
                                                        args.complete();
                                                    }
                                                }
                                            },
                                        },
                                    }
                                }
                            }
                        },
                    }
                }
            },
            actions: {
                add: {
                    label: 'GloboNetwork Configuration',
                    createForm: {
                        title: 'GloboNetwork Configuration',
                        fields: {
                            username: {
                                label: 'Username',
                                validation: {
                                    required: true
                                }
                            },
                            password: {
                                label: 'Password',
                                isPassword: true,
                                validation: {
                                    required: true
                                }
                            },
                            url: {
                                label: 'URL',
                                validation: {
                                    required: true
                                }
                            }
                        }
                    },
                    action: function(args) {
                        if (getNspMap()["GloboNetwork"] == null) {
                            $.ajax({
                                url: createURL("addNetworkServiceProvider&name=GloboNetwork&physicalnetworkid=" + getSelectedPhysicalNetworkObj().id),
                                dataType: "json",
                                async: true,
                                success: function(json) {
                                    var jobId = json.addnetworkserviceproviderresponse.jobid;
                                    var addGloboNetworkProviderIntervalID = setInterval(function() {
                                        $.ajax({
                                            url: createURL("queryAsyncJobResult&jobId=" + jobId),
                                            dataType: "json",
                                            success: function(json) {
                                                var result = json.queryasyncjobresultresponse;
                                                if (result.jobstatus == 0) {
                                                    return; //Job has not completed
                                                } else {
                                                    clearInterval(addGloboNetworkProviderIntervalID);
                                                    if (result.jobstatus == 1) {
                                                        getNspMap()["GloboNetwork"] = json.queryasyncjobresultresponse.jobresult.networkserviceprovider;
                                                        addGloboNetworkHost(args, getSelectedPhysicalNetworkObj(), "addGloboNetworkHost", "addglobonetworkhostresponse");
                                                    } else if (result.jobstatus == 2) {
                                                        alert("addNetworkServiceProvider&name=GloboNetwork failed. Error: " + _s(result.jobresult.errortext));
                                                    }
                                                }
                                            },
                                            error: function(XMLHttpResponse) {
                                                var errorMsg = parseXMLHttpResponse(XMLHttpResponse);
                                                alert("addNetworkServiceProvider&name=GloboNetwork failed. Error: " + errorMsg);
                                            }
                                        });
                                    }, g_queryAsyncJobResultInterval);
                                }
                            });
                        } else {
                            addGloboNetworkHost(args, getSelectedPhysicalNetworkObj(), "addGloboNetworkHost", "addglobonetworkhostresponse");
                        }
                    },
                    messages: {
                        notification: function(args) {
                            return 'label.globonetwork.add.device';
                        }
                    },
                    notification: {
                        poll: pollAsyncJobResult
                    }
                },
                enable: {
                    label: 'label.enable.provider',
                    action: function(args) {
                        $.ajax({
                            url: createURL("updateNetworkServiceProvider&id=" + getNspMap()["GloboNetwork"].id + "&state=Enabled"),
                            dataType: "json",
                            success: function(json) {
                                var jid = json.updatenetworkserviceproviderresponse.jobid;
                                args.response.success({
                                    _custom: {
                                        jobId: jid,
                                        getUpdatedItem: function(json) {
                                            $(window).trigger('cloudStack.fullRefresh');
                                        }
                                    }
                                });
                            }
                        });
                    },
                    messages: {
                        confirm: function(args) {
                            return 'message.confirm.enable.provider';
                        },
                        notification: function() {
                            return 'label.enable.provider';
                        }
                    },
                    notification: {
                        poll: pollAsyncJobResult
                    }
                },
                disable: {
                    label: 'label.disable.provider',
                    action: function(args) {
                        $.ajax({
                            url: createURL("updateNetworkServiceProvider&id=" + getNspMap()["GloboNetwork"].id + "&state=Disabled"),
                            dataType: "json",
                            success: function(json) {
                                var jid = json.updatenetworkserviceproviderresponse.jobid;
                                args.response.success({
                                    _custom: {
                                        jobId: jid,
                                        getUpdatedItem: function(json) {
                                            $(window).trigger('cloudStack.fullRefresh');
                                        }
                                    }
                                });
                            }
                        });
                    },
                    messages: {
                        confirm: function(args) {
                            return 'message.confirm.disable.provider';
                        },
                        notification: function() {
                            return 'label.disable.provider';
                        }
                    },
                    notification: {
                        poll: pollAsyncJobResult
                    }
                },
                destroy: {
                    label: 'label.shutdown.provider',
                    action: function(args) {
                        $.ajax({
                            url: createURL("deleteNetworkServiceProvider&id=" + getNspMap()["GloboNetwork"].id),
                            dataType: "json",
                            success: function(json) {
                                var jid = json.deletenetworkserviceproviderresponse.jobid;
                                args.response.success({
                                    _custom: {
                                        jobId: jid
                                    }
                                });

                                $(window).trigger('cloudStack.fullRefresh');
                            }
                        });
                    },
                    messages: {
                        confirm: function(args) {
                            return 'message.confirm.shutdown.provider';
                        },
                        notification: function(args) {
                            return 'label.shutdown.provider';
                        }
                    },
                    notification: {
                        poll: pollAsyncJobResult
                    }
                }
            }
        };
    };
})(globoNetworkAPI, jQuery);
