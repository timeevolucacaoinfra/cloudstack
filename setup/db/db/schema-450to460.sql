-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

--
-- Schema upgrade from 4.5.0 to 4.6.0
--

INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Advanced", 'DEFAULT', 'management-server', "stats.output.uri", "", "URI to additionally send StatsCollector statistics to", "", NULL, NULL, 0);

-- globonetwork_environment_ref
CREATE TABLE `cloud`.`globonetwork_environment_ref` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `globonetwork_environment_id` bigint(20) unsigned DEFAULT NULL,
  `physical_network_id` bigint(20) unsigned NOT NULL COMMENT 'physical network id',
  `name` varchar(255) NOT NULL COMMENT 'name',
  PRIMARY KEY (`id`),
  KEY `fk_napi_network_ref__physical_network_id` (`physical_network_id`),
  KEY `fk_napi_network_ref__napi_environment_id` (`globonetwork_environment_id`),
  CONSTRAINT `fk_napi_environment_ref__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



-- globonetwork_network_ref
CREATE TABLE `cloud`.`globonetwork_network_ref` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `globonetwork_vlan_id` bigint(20) unsigned DEFAULT NULL,
  `network_id` bigint(20) unsigned NOT NULL COMMENT 'network id',
  `globonetwork_environment_id` bigint(20) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_napi_network_ref__network_id` (`network_id`),
  KEY `fk_napi_network_ref__napi_vlan_id` (`globonetwork_vlan_id`),
  KEY `napi_environment_index` (`globonetwork_environment_id`),
  CONSTRAINT `fk_napi_network_ref__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
