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
        dataProvider: function(args) {
          plugin.ui.apiCall('listNetworkApiVips', {
            success: function(json) {
              var vips = json.listnetworkapivipsresponse.networkapivip;
              args.response.success({ data: vips });
            },
            error: function(errorMessage) {
              args.response.error(errorMessage)
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
                  plugin.ui.apiCall('listVirtualMachines', {
                    success: function(json) {
                      var reals = [{"vmname": "VM01", "ip": "200.0.0.1", "ports": "80:8080", "state": "On"}, {"vmname": "VM02", "ip": "200.0.0.2", "ports": "443:8443", "state": "Off"}];

                      args.response.success({ data: reals });
                    },
                    error: function(errorMessage) {
                      args.response.error(errorMessage)
                    }
                  });
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
