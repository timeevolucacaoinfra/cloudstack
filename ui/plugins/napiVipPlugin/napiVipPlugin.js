(function (cloudStack) {

  cloudStack.plugins.napiVipPlugin = function(plugin) {
    plugin.ui.addSection({
      id: 'napiVipPlugin',
      title: 'Network API VIPs',
      preFilter: function(args) {
        return true; // isAdmin();
      },

      listView: {
        id: 'vips',
        fields: {
          name: { label: 'label.name' },
          ip: { label: 'IP' },
          network: { label: 'label.network' },
        },
        actions: {
          add: {
            label: 'Create new VIP3',
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
                                var networks = json.listnetworksresponse.network;
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
              console.log('oi', args);
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
        },
        dataProvider: function(args) {
          plugin.ui.apiCall('listNetworkApiVips', {
            success: function(json) {
              var vips = json.listnetworkapivipsresponse.networkapivip;
              vips.forEach(function(vip) {
                  vip.ports = vip.ports.join("\n");
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
                  ports: { label: 'Ports' },
                  state: {
                    label: 'label.state',
                    indicator: {
                        'On': 'on',
                        'Off': 'off',
                    }
                  }
                },
                dataProvider: function(args) {
                  args.response.success({ data: args.context.vips[0].reals });
                },
                actions: {
                  add: {
                    label: 'Add Real',
                    createForm: {
                      title: 'Add Real to VIP',
                      fields: {
                        ip: {
                          label: 'IP',
                          validation: {
                            required: true
                          }
                        }
                      }
                    },
                    action: function(args) {
                      // FIXME
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
                  enable: {
                    label: 'Enable',
                    action: function(args) {
                      // FIXME
                    },
                    messages: {
                      // FIXME
                    },
                    notification: {
                      poll: pollAsyncJobResult
                    }
                  },
                  disable: {
                    label: 'Disable',
                    action: function(args) {
                      // FIXME
                    },
                    messages: {
                      // FIXME
                    },
                    notification: {
                      poll: pollAsyncJobResult
                    }
                  },
                  remove: {
                    label: 'Remove',
                    action: function(args) {
                      // FIXME
                    },
                    messages: {
                      // FIXME
                    },
                    notification: {
                      poll: pollAsyncJobResult
                    }
                  }
                }
              }
            },
          },
        },
      }
    });
  };
}(cloudStack));
