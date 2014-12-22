(function (cloudStack) {

    cloudStack.plugins.globoNetworkVipPlugin = function(plugin) {
        plugin.ui.addSection({
            id: 'globoNetworkVipPlugin',
            title: 'GloboNetwork VIPs',
            preFilter: function(args) {
                return true; // isAdmin();
            },
            listView: {
                id: 'vips',
                fields: {
                    name: { label: 'label.name' },
                    ip: { label: 'IP' },
                },
                dataProvider: function(args) {
                    plugin.ui.apiCall('listGloboNetworkVips', {
                        success: function(json) {
                            var vips = json.listglobonetworkvipsresponse.globonetworkvip || [];
                            vips.forEach(function(vip) {
                                vip.ports = vip.ports.join(", ");
                            });
                            args.response.success({ data: vips });
                        },
                        error: function(errorMessage) {
                            args.response.error(errorMessage);
                        }
                    });
                },
                detailView: {
                    name: 'VIP details',
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
                                ip: {
                                    label: 'IP'
                                },
                                cache: {
                                    label: 'Cache'
                                },
                                method: {
                                    label: 'Balancing method'
                                },
                                persistence: {
                                    label: 'Persistence'
                                },
                                healthchecktype: {
                                    label: 'Healthcheck Type'
                                },
                                healthcheck: {
                                    label: 'Healthcheck'
                                },
                                maxconn: {
                                    label: 'Max Connections'
                                },
                                ports: {
                                    label: 'Ports'
                                },
                            }],
                            dataProvider: function(args) {
                                args.response.success({ data: args.jsonObj });
                            }
                        },
                        reals: {
                            title: 'Reals',
                            listView: {
                                id: 'reals',
                                fields: {
                                    vmname: { label: 'VM' },
                                    ip: { label: 'IP' },
                                    network: { label: 'Network' },
                                    ports: { label: 'Ports' },
                                },
                                dataProvider: function(args) {
                                    plugin.ui.apiCall('listGloboNetworkReals', {
                                        data: {
                                            vipid: args.context.vips[0].id,
                                        },
                                        success: function(json) {
                                            var reals = json.listglobonetworkrealsresponse.globonetworkreal || [];
                                            args.response.success({ data: reals });
                                        },
                                        error: function(errorMessage) {
                                            args.response.error(errorMessage);
                                        }
                                    });
                                },
                                actions: {
                                    add: {
                                        label: 'Add Real',
                                        createForm: {
                                            title: 'Add Real to VIP',
                                            fields: {
                                                nic: {
                                                    label: 'VM',
                                                    validation: {
                                                        required: true
                                                    },
                                                    select: function(args) {
                                                        var items = [];
                                                        args.context.vips[0].networkids.forEach(function(networkid) {
                                                            $.ajax({
                                                                url: createURL("listVirtualMachines&networkid=" + networkid),
                                                                dataType: "json",
                                                                async: false,
                                                                success: function(json) {
                                                                    var vms = json.listvirtualmachinesresponse.virtualmachine;
                                                                    $(vms).each(function(indexvm, vm) {
                                                                        $(vm.nic).each(function(indexnic,nic) {
                                                                            if (nic.networkid === networkid) {
                                                                                items.push({
                                                                                    id: nic.id,
                                                                                    description: vm.name,
                                                                                });
                                                                            }
                                                                        });
                                                                    });
                                                                }
                                                            });
                                                        });
                                                        args.response.success({
                                                            data: items
                                                        });
                                                    }
                                                }
                                            }
                                        },
                                        action: function(args) {
                                            $.ajax({
                                                url: createURL("associateGloboNetworkRealToVip&vipid=" + args.context.vips[0].id + "&nicid=" + args.data.nic),
                                                dataType: "json",
                                                async: true,
                                                success: function(json) {
                                                    $(window).trigger('cloudStack.fullRefresh');
                                                },
                                                error: function(errorMessage) {
                                                    var error = (jQuery.parseJSON(errorMessage.responseText)).associateglobonetworkrealtovipresponse;
                                                    args.response.error(error.errorcode + " : " + error.errortext);
                                                }
                                            });
                                        },
                                        messages: {
                                            notification: function(args) {
                                                return 'Real added successfully';
                                            }
                                        },
                                        notification: {
                                            poll: pollAsyncJobResult
                                        }
                                    },
                                    // enable: {
                                    //     label: 'Enable',
                                    //     action: function(args) {
                                    //         // FIXME
                                    //     },
                                    //     messages: {
                                    //         // FIXME
                                    //     },
                                    //     notification: {
                                    //         poll: pollAsyncJobResult
                                    //     }
                                    // },
                                    // disable: {
                                    //     label: 'Disable',
                                    //     action: function(args) {
                                    //         // FIXME
                                    //     },
                                    //     messages: {
                                    //         // FIXME
                                    //     },
                                    //     notification: {
                                    //         poll: pollAsyncJobResult
                                    //     }
                                    // },
                                    remove: {
                                        label: 'label.remove',
                                        action: function(args) {
                                            $.ajax({
                                                url: createURL("disassociateGloboNetworkRealFromVip&vipid=" + args.context.vips[0].id + "&nicid=" + args.context.reals[0].nic),
                                                dataType: "json",
                                                async: true,
                                                success: function(json) {
                                                    $(window).trigger('cloudStack.fullRefresh');
                                                },
                                                error: function(errorMessage) {
                                                    var error = (jQuery.parseJSON(errorMessage.responseText)).disassociateglobonetworkrealfromvipresponse;
                                                    args.response.error(error.errorcode + " : " + error.errortext);
                                                }
                                            });
                                        },
                                        messages: {
                                            confirm: function(args) {
                                                return 'Are you sure you want to remove real ' + args.context.reals[0].vmname + ' (' + args.context.reals[0].ip + ') from VIP ' + args.context.vips[0].name + ' (' + args.context.vips[0].ip + ')?';
                                            },
                                            notification: function(args) {
                                                return 'Real removed successfully';
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
                                return 'Are you sure you want to remove VIP ' + args.context.vips[0].name + ' (' + args.context.vips[0].ip + ')?';
                            },
                            notification: function(args) {
                                return 'Remove GloboNetwork VIP';
                            }
                        },
                        action: function(args) {
                            $.ajax({
                                url: createURL("removeGloboNetworkVip&vipid=" + args.context.vips[0].id),
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
                        label: 'Create new VIP',
                        createForm: {
                            title: 'Real network',
                            fields: {
                                networkId: {
                                    label: 'label.network',
                                    validation: {
                                        required: true
                                    },
                                    select: function(args) {
                                        $.ajax({
                                            url: createURL('listNetworks'),
                                            async: false,
                                            data: {
                                                listAll: true
                                            },
                                            success: function(json) {
                                                var networks = json.listnetworksresponse.network || [];
                                                args.response.success({
                                                    data: $.map(networks, function(network) {
                                                        return {
                                                            id: network.id,
                                                            description: network.name
                                                        };
                                                    })
                                                });
                                            }
                                        });
                                    },
                                },
                            }
                        },
                        action: function(args) {
                            var message_vip_func = function(event) {
                                var message = null;
                                if (event && event.originalEvent && event.originalEvent.data) {
                                    message = $.parseJSON(event.originalEvent.data);
                                }
                                if (!message || message.type !== "requestVip") {
                                    // this is not my message
                                    return;
                                }

                                var vipId = message.data.requestVip;

                                $.ajax({
                                    url: createURL('addGloboNetworkVipToAccount&networkid=' + args.data.networkId + '&vipid=' + vipId),
                                    async: false,
                                    success: function(vipJson) {
                                        $(window).unbind('message.vip');
                                        $('#editing-vip').dialog("close");
                                        // args.response.success();
                                        $(window).trigger('cloudStack.fullRefresh');
                                    }
                                });
                            };

                            $(window).unbind('message.vip').bind('message.vip', message_vip_func);

                            $.ajax({
                                url: createURL('generateUrlForEditingVip&networkid=' + args.data.networkId),
                                async: false,
                                success: function(json) {
                                    var url = json.generateurlforeditingvipresponse.editingurl.url;
                              
                                    var $iframe = $("<iframe>").attr({
                                        'src': url,
                                        'width': '680px',
                                        'height': '100%',
                                        'scrolling': 'auto'
                                    });
                                    var $dialog = $("<div>");
                                    $dialog.attr('id', 'editing-vip');
                                    $dialog.append($iframe);
                              
                                    $dialog.dialog({
                                        modal: false,
                                        width: 700,
                                        height: 500,
                                        title: 'Create new VIP',
                                        closeOnEscape: true,
                                        buttons: {
                                            'Close': function() {
                                                $(this).dialog("close");
                                                $(window).trigger('cloudStack.fullRefresh');
                                            }
                                        },
                                        close: function(event, ui) {
                                            $(':ui-dialog, .overlay').remove();
                                        },
                                        open: function(event, ui) {
                                            $(window).scrollTop($dialog.scrollTop());
                                        }
                                    }).closest('.ui-dialog').overlay();

                                    // Do not remove lines below. We use it for testing purpose.
                                    // window.setTimeout(function() {
                                        // message_vip_func({'originalEvent': {'data': '{"type": "requestVip", "data": { "requestVip": "6525"}}'}});
                                    // }, 2000);

                                }
                            });
                        },
                        messages: {
                            notification: function(args) {
                                return 'Vip created';
                            }
                        },
                        notification: {
                            poll: pollAsyncJobResult
                        }
                    }
                }
            }
        });
    };
}(cloudStack));