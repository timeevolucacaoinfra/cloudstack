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

package com.cloud.network.router;

import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.server.ResourceTag;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.tags.dao.ResourceTagsDaoImpl;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class VirtualNetworkApplianceManagerImplTest {


    VirtualNetworkApplianceManagerImpl vnAM;

    @Before
    public void setup() {
        vnAM = new VirtualNetworkApplianceManagerImpl();
    }

    @Test
    public void testSetVmTags() {
        VmDataCommand cmd = new VmDataCommand("10.1.1.1", "vm-User", false);

        List<ResourceTag> tags = new ArrayList<ResourceTag>();
        tags.add(new ResourceTagVO("Key_1", "VALUE_11", 0l, 0l, 123l, ResourceTag.ResourceObjectType.UserVm, "user_1", "123-123"));
        tags.add(new ResourceTagVO("key_2", "", 0l, 0l, 123l, ResourceTag.ResourceObjectType.UserVm, "user_1", "123-123"));
        tags.add(new ResourceTagVO("KEY_3", null, 0l, 0l, 123l, ResourceTag.ResourceObjectType.UserVm, "user_1", "123-123"));

        ResourceTagDao dao = mock(ResourceTagsDaoImpl.class);
        Mockito.<List<? extends ResourceTag>>when(dao.listBy(123l, ResourceTag.ResourceObjectType.UserVm)).thenReturn(tags);
        vnAM._resourceTagDao = dao;

        vnAM.buildTagMetadata(cmd, 123);

        List<String[]> vmData = cmd.getVmData();
        assertEquals(4, vmData.size());

        String[] data =  vmData.get(0);
        assertEquals("metadata", data[0]);
        assertEquals("TAG_Key_1", data[1]);
        assertEquals("VALUE_11", data[2]);

        String[] data2 =  vmData.get(1);
        assertEquals("metadata", data2[0]);
        assertEquals("TAG_key_2", data2[1]);
        assertEquals("", data2[2]);

        String[] data3 =  vmData.get(2);
        assertEquals("metadata", data3[0]);
        assertEquals("TAG_KEY_3", data3[1]);
        assertEquals("", data3[2]);

        String[] dataKeys =  vmData.get(3);
        assertEquals("metadata", dataKeys[0]);
        assertEquals("TAGKEYS", dataKeys[1]);
        assertEquals("TAG_Key_1,TAG_key_2,TAG_KEY_3", dataKeys[2]);
    }

}
