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
(function (cloudStack) {
  cloudStack.plugins.napiVipPlugin = function(plugin) {
    plugin.ui.addSection({
      id: 'napiVipPlugin',
      title: 'Network API VIPs',
      preFilter: function(args) {
        return isAdmin();
      },

      // Render page as a list view
      listView: {
        id: 'vips',
        fields: {
          id: { label: 'Id'},
          host: { label: 'Host' },
          ip: { label: 'IP' },
        },
        actions: {
          // The key/ID you specify here will determine what icon is
          // shown in the UI for this action,
          // and will be added as a CSS class to the action's element
          // (i.e., '.action.restart')
          //
          // -- here, 'restart' is a predefined name in CloudStack that will
          // automatically show a 'reboot' arrow as an icon;
          // this can be changed in csMyFirstPlugin.css
          restart: {
            label: 'Restart VM',
            messages: {
              confirm: function() { return 'Are you sure you want to restart this VM?' },
              notification: function() { return 'Rebooted VM' }
            },
            action: function(args) {
              // Get the instance object of the selected row from context
              //
              // -- all currently loaded state is stored in 'context' as objects,
              //    such as the selected list view row,
              //    the selected section, and active user
              //
              // -- for list view actions, the object's key will be the same as
              //    listView.id, specified above;
              //    always make sure you specify an 'id' for the listView,
              //     or else it will be 'undefined!'
              var instance = args.context.instances[0];

              plugin.ui.apiCall('rebootVirtualMachine', {
                // These will be appended to the API request
                //
                // i.e., rebootVirtualMachine&id=...
                data: {
                  id: instance.id
                },
                success: function(json) {
                  args.response.success({
                    // This is an async job, so success here only indicates
                    // that the job was initiated.
                    //
                    // To pass the job ID to the notification UI
                    // (for checking to see when action is completed),
                    // '_custom: { jobID: ... }' needs to always be passed on success,
                    // in the same format as below
                    _custom: { jobId: json.rebootvirtualmachineresponse.jobid }
                  });
                },


                error: function(errorMessage) {
                  args.response.error(errorMessage); // Cancel action, show error message returned
                }
              });
            },

            // Because rebootVirtualMachine is an async job, we need to add
            // a poll function, which will perodically check
            // the management server to see if the job is ready
            // (via pollAsyncJobResult API call)
            //
            // The plugin API provides a helper function, 'plugin.ui.pollAsyncJob',
            //  which will work for most jobs
            // in CloudStack
            notification: {
              poll: plugin.ui.pollAsyncJob
            }
          }
        },
        dataProvider: function(args) {
          // API calls go here, to retrive the data asynchronously
          //
          // On successful retrieval, call
          // args.response.success({ data: [data array] });
          plugin.ui.apiCall('listVirtualMachines', {
            success: function(json) {
              var vips = [{"id": "001", "host": "TESTEVIP1.GLOBO.COM", "ip": "10.0.0.1"}, {"id": "002", "host": "TESTEVIP2.GLOBO.COM", "ip": "10.0.0.2"}];

              args.response.success({ data: vips });
            },
            error: function(errorMessage) {
              args.response.error(errorMessage)
            }
          });
        }
      }
    });
  };
}(cloudStack));
