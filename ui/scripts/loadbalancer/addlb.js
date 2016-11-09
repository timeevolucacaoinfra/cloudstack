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
    var healthcheckTypes = cloudStack.sections.loadbalancer.utils.healthcheck;
    var msg_validation_healthcheck_http = healthcheckTypes.validationMsg;

    var networks = [];

    cloudStack.sections.loadbalancer.listView.actions["add"] = {
       label: 'Create new Load Balancer',
       preAction: function() {
           var message;
           $.ajax({
               url: createURL('listGloboLbNetworks'),
               data: {
                   supportedservices: 'lb'
               },
               dataType: "json",
               async: false,
               success: function(json) {
                   $(json.listnetworksresponse.network).each(function() {
                       networks.push({id: this.id,
                                      description: this.name,
                                      service: this.service,
                                      physicalnetworkid: this.physicalnetworkid
                       });
                   });
               },
               error: function(json) {
                   message = parseXMLHttpResponse(json);
               }
           });

           if (networks.length === 0) {
               cloudStack.dialog.notice({
                   message: message || 'There are no networks. Please create a network before creating a load balancer.'
               });
               return false;
           }
           return true;
       },
       createForm: {
           fields: {
               name: {
                   label: 'label.name',
                   validation: {
                       required: true,
                       noUnderscore : true
                   }
               },
               lbdomain: {
                   label: 'LB Domain',
                   validation: {
                       required: true
                   },
                   select: function(args) {
                       var lbdomains = [];
                       $.ajax({
                           url: createURL("listGloboNetworkCapabilities"),
                           dataType: "json",
                           async: false,
                           success: function(json) {
                               var response = json.listglobonetworkcapabilitiesresponse;
                               $(json.listglobonetworkcapabilitiesresponse.globoNetworkCapability.allowedLbSuffixes).each(function() {
                                   lbdomains.push({id: this.valueOf(), description: this.valueOf()});
                               });
                           },
                           error: function(json) {
                               args.response.error(parseXMLHttpResponse(json));
                           }
                       });
                       args.response.success({
                           data: lbdomains
                       });
                   }
               },
               ports: {
                   label: 'label.port',
                   docID: 'helpLbPorts',
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

                       args.response.success({
                           data: networks
                       });
                   }
               },
               lbenvironment: {
                   label: 'LB Environment',
                   validation: {
                       required: true
                   },
                   dependsOn: ['network'],
                   select: function(args) {
                       var network = networks[0];

                       $.ajax({
                           url: createURL("listGloboNetworkLBEnvironments"),
                           data: {
                               physicalnetworkid: network.physicalnetworkid,
                               networkid: network.id
                           },
                           dataType: "json",
                           async: false,
                           success: function(json) {
                               var data = [];
                               $(json.listglobonetworklbenvironmentsresponse.globonetworklbenvironments).each(function() {
                                   data.push({id: this.id, name: this.name, description: this.name});
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
                       var network = networks[0];


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

               isLbAdvanced: {
                   label: 'label.show.advanced.settings',
                   dependsOn: ['network'],
                   isBoolean: true,
                   defaultValue: false,
                   isChecked: false,
               },
               cachegroup: {
                   label: 'Cache',
                   isHidden: function (args) {
                       var isAdvancedChecked = $('input[name=isLbAdvanced]:checked').length > 0;
                       return !isAdvancedChecked;
                   },
                   validation: {
                       required: true
                   },
                   defaultValue: "(nenhum)",
                   dependsOn: ['lbenvironment','isLbAdvanced'],
                   select: function(args) {
                       var network;
                       if ( args.data.lbenvironment !== '' ){
                           $.ajax({
                               url: createURL("listGloboNetworkLBCacheGroups"),
                               data: {
                                   lbenvironment: args.data.lbenvironment,
                                   networkid: args.data.network
                               },
                               dataType: "json",
                               async: false,
                               success: function(json) {
                                   var data = [];
                                   $(json.listglobonetworklbcachegroupsresponse.globonetworkcachegroups).each(function() {
                                       data.push({id: this.name, name: this.name, description: this.name});
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
                   }
               },

               sticky: {
                   label: 'label.stickiness',
                   isHidden: function (args) {
                       var isAdvancedChecked = $('input[name=isLbAdvanced]:checked').length > 0;
                       return !isAdvancedChecked;
                   },
                   dependsOn: ['network', 'isLbAdvanced'],
                   select: function(args) {
                       var network = networks[0];

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
               // #hlb
               healthchecktype: {
                   label: 'Health Check Type',
                   docID: 'helpHealthcheckType',
                   isHidden: function (args) {
                       var isAdvancedChecked = $('input[name=isLbAdvanced]:checked').length > 0;
                       return !isAdvancedChecked;
                   },
                   dependsOn: ['isLbAdvanced'],
                   select: function(args) {
                       args.response.success({
                           data: healthcheckTypes.values
                       });
                       args.$select.change(function() {
                           var type = $(this).val()
                           console.log('change type: ' + $(this).val())
                           if (healthcheckTypes.isLayer4(type)) {
                               $("div[rel='healthcheck']").hide()
                               $("div[rel='expectedhealthcheck']").hide()
                           } else {
                               $("div[rel='healthcheck']").show()
                               $("div[rel='expectedhealthcheck']").show()
                           }
                       })
                   }
               },
               healthcheck: {
                   label: 'Health Check'
               },
               expectedhealthcheck: {
                   label: 'Expected Health Check',
                   select: function(args) {
                       var expectedHealthcheck = [];
                       $.ajax({
                           url: createURL("listGloboNetworkExpectedHealthchecks"),
                           data: {
                           },
                           async: false,
                           success: function(json) {
                               expectedHealthchecksR = json.listgloboNetworkexpectedhealthchecksresponse.globonetworkexpectedhealthcheck
                               $(expectedHealthchecksR).each(function() {
                                   expectedHealthcheck.push({id: this.expected, name: this.expected, description: this.expected})
                               });
                               args.response.success({
                                   data: expectedHealthcheck
                               });

                           }
                       })


                   }
               }

           },
       },
       action: function(args) {
           if (args.data.name.indexOf("_") > 0) {
               args.response.error("Underscore(_) is not allowed in Load Balancer names");
               return;
           }
           var expectedhealthcheck = '';
           var healthcheckPingPath = '';

           if ( healthcheckTypes.isLayer7(args.data.healthchecktype) ) {
               healthcheckPingPath = args.data.healthcheck.valueOf().trim();
               expectedhealthcheck = args.data.expectedhealthcheck;
               if ( healthcheckPingPath === ''){
                   args.response.error(msg_validation_healthcheck_http);
                   return
               }

           }


           var network;
           $.ajax({
               url: createURL('listGloboLbNetworks'),
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

           // FIXME Check what happen if loadbalancer is not active in network

           var url;
           var data = {};
           if (args.data.isportable === 'true' && provider === 'GloboNetwork') {
               url = "acquireNewLBIp";
               data = {
                   networkid: args.data.network,
                   lbenvironmentid: args.data.lbenvironment
               };
           } else if (provider === 'GloboNetwork') {
               // GloboNetwork should only work with portable IPs
               // FIXME Test this
               args.response.error("Network is provided by GloboNetwork and can only manage cross zone IPs (portable)");
               return;
           } else {
               url = "associateIpAddress";
               data = {
                   isportable: args.data.isportable,
                   networkid: args.data.network
               };
           }

           var show_error_message = function(msg) {
               if (typeof(msg) == 'string') {
                   args.response.error(msg);
               } else if (typeof(msg) == 'object') {
                   args.response.error(parseXMLHttpResponse(msg));
               }
           };

           var disassociate_ip_address_with_message = function(ipid, msg) {
               show_error_message(msg);
               $.ajax({
                   url: createURL('disassociateIpAddressFromGloboNetwork'),
                   data: {
                       id: ipid
                   },
                   dataType: 'json',
                   success: function(data) {
                       return;
                   },
                   error: show_error_message
               });
           };

           $.ajax({
               url: createURL(url),
               data: data,
               dataType: "json",
               success: function(json) {
                   var ipId = json.associateipaddressresponse.id;

                   // This regexp removes every character except numbers, ':' and ','. It also removes the ',' at the end of the string
                   args.data.ports = args.data.ports.replace(/[^\d\:\,]/g, "").replace(/\,+$/g, "");

                   var portlist = args.data.ports.split(",");
                   if (portlist[0].split(":").length !== 2) {
                       disassociate_ip_address_with_message(ipId, "Invalid ports. It should in the form \"80:8080,443:8443\"");
                   }

                   // Variable to make sure that mapping is valid, public port shouldn't repeat
                   var alreadymappedports = [];

                   var publicport = portlist[0].split(":")[0].trim();
                   var privateport = portlist[0].split(":")[1].trim();

                   alreadymappedports.push(publicport);

                   var additionalportmap = [];
                   if (portlist.length > 1) {
                       additionalportmap = portlist.slice(1, portlist.length);
                   }

                   // Validation
                   $(additionalportmap).each(function() {
                       if (this.split(":").length != 2) {
                           disassociate_ip_address_with_message(ipId, "Invalid ports. It should be in the form \"80:8080,443:8443\"");
                       }
                       var pubport = this.split(":")[0].trim();
                       if ($.inArray(pubport, alreadymappedports) !== -1) {
                           disassociate_ip_address_with_message(ipId, "Invalid ports. Duplicated public (VIP) port.");
                       }
                       alreadymappedports.push(pubport);
                   });

                   var data = {
                       algorithm: args.data.algorithm,
                       name: args.data.name + args.data.lbdomain,
                       privateport: privateport,
                       publicport: publicport,
                       openfirewall: false,
                       networkid: args.data.network,
                       cache: args.data.cachegroup,
                       healthcheckType: args.data.healthchecktype,
                       publicipid: ipId,
                       additionalportmap: additionalportmap.join(),
                       skipdnserror: true,
                   };

                   if ( expectedhealthcheck !== '' ){
                       data['expectedhealthcheck'] = expectedhealthcheck;
                   }

                   var stickyData = {methodname: args.data.sticky.valueOf(), stickyName: args.data.sticky.valueOf()};



                   $.ajax({
                       url: createURL('createLoadBalancerRule'),
                       data: data,
                       dataType: 'json',
                       success: function(data) {
                           var jobID = data.createloadbalancerruleresponse.jobid;
                           var lbID = data.createloadbalancerruleresponse.id;

                           args.response.success({
                               _custom: {
                                   jobId: jobID,
                                   getUpdatedItem: function(json) {
                                       var lb = json.queryasyncjobresultresponse.jobresult.loadbalancer;
                                       lb.ports = lb.publicport + ':' + lb.privateport + ', ';
                                       $(lb.additionalportmap).each(function() {
                                           lb.ports += this + ', ';
                                       });
                                       lb.ports = lb.ports.substring(0, lb.ports.length - 2); // remove last ', '

                                       console.log(JSON.stringify(lb));

                                       isLoadBalancerDNSRegistered = true;
                                       function setIsLoadBalancerDNSRegistered(result){
                                           isLoadBalancerDNSRegistered = result;
                                       }
                                       $.ajax({
                                           url: createURL("getGloboResourceConfiguration"),
                                           data: {
                                               resourceid: lb.id,
                                               resourcetype: 'LOAD_BALANCER',
                                               resourcekey: 'isDNSRegistered'
                                           },
                                           dataType: "json",
                                           async: false,
                                               success: function(json) {
                                                   var conf = json.getgloboresourceconfigurationresponse.globoresourceconfiguration.configurationvalue
                                                   if(conf == undefined || conf == "true") {
                                                       setIsLoadBalancerDNSRegistered(true);
                                                   } else if (conf == "false") {
                                                       setIsLoadBalancerDNSRegistered(false);
                                                   }
                                           },
                                           error: function (errorMessage) {
                                               setIsLoadBalancerDNSRegistered(false);
                                               //args.response.error(errorMessage);
                                           }
                                       });

                                       if (isLoadBalancerDNSRegistered == false) {
                                           cloudStack.dialog.notice({message: "The LoadBalancer DNS was not registered!"});
                                       }

                                       return lb;
                                   }
                               }
                           });


                           // Create stickiness policy
                           if (stickyData &&
                               stickyData.methodname &&
                               stickyData.methodname != 'None') {
                               cloudStack.lbStickyPolicy.actions.add(lbID,
                                   stickyData,
                                   $.noop, $.noop);
                           }

                           if (healthcheckPingPath !== '') {
                               // Create healthcheck
                               var datahealthcheck = {
                                   lbruleid: lbID,
                                   pingpath: healthcheckPingPath
                               };

                               $.ajax({
                                   url: createURL('createLBHealthCheckPolicy'),
                                   data: datahealthcheck,
                                   success: function(json) {
                                       cloudStack.ui.notifications.add({
                                               desc: 'Add Healthcheck Policy',
                                               section: 'Network',
                                               poll: pollAsyncJobResult,
                                               _custom: {
                                                   jobId: json.createlbhealthcheckpolicyresponse.jobid
                                               }
                                           },
                                           $.noop, {},
                                           $.noop, {}
                                       );
                                   },
                                   error: show_error_message // healtcheck
                               });
                           }
                       },
                       error: function(json) {
                           disassociate_ip_address_with_message(ipId, json);
                       },
                   });
               },
               error: show_error_message // associateipaddress
           });
       },
       messages: {
           notification: function(args) {
               return 'Create Load Balancer';
           }
       },
       notification: {
           label: 'Load Balancer created',
           poll: pollAsyncJobResult
       }
    }





}(cloudStack, jQuery));