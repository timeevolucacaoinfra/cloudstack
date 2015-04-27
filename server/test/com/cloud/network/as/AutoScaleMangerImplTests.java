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
package com.cloud.network.as;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.as.dao.AutoScaleVmGroupDao;
import com.cloud.network.as.dao.AutoScaleVmGroupPolicyMapDao;
import com.cloud.network.as.dao.AutoScaleVmGroupVmMapDao;
import com.cloud.network.lb.LoadBalancingRulesManagerImpl;
import com.cloud.user.Account;
import com.cloud.user.AccountManagerImpl;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import org.apache.cloudstack.context.CallContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AutoScaleMangerImplTests {

    FakeAutoScaleManager autoScaleManager;

    @Mock
    LoadBalancingRulesManagerImpl loadBalancingRulesManager;

    @Mock
    AutoScaleVmGroupDao autoScaleVmGroupDao;

    @Mock
    AutoScaleVmGroupVmMapDao autoScaleVmGroupVmMapDao;

    @Mock
    AutoScaleVmGroupPolicyMapDao autoScaleVmGroupPolicyMapDao;

    @Mock
    AccountManagerImpl accountManager;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);

        createAutoScaleManager();

        autoScaleManager._lbRulesMgr = loadBalancingRulesManager;
        autoScaleManager._autoScaleVmGroupDao = autoScaleVmGroupDao;
        autoScaleManager._autoScaleVmGroupVmMapDao = autoScaleVmGroupVmMapDao;
        autoScaleManager._accountMgr = accountManager;
        autoScaleManager._autoScaleVmGroupPolicyMapDao = autoScaleVmGroupPolicyMapDao;

        AccountVO acct = new AccountVO(200L);
        acct.setType(Account.ACCOUNT_TYPE_NORMAL);
        acct.setAccountName("user");
        acct.setDomainId(1L);

        UserVO user = new UserVO();
        user.setUsername("user");
        user.setAccountId(acct.getAccountId());

        CallContext.register(user, acct);
        when(accountManager.getSystemAccount()).thenReturn(acct);
        when(accountManager.getSystemUser()).thenReturn(user);
    }

    @Test
    public void testDeleteEmptyAutoScaleGroup() throws ResourceUnavailableException {
        long AS_GROUP_ID = 10L;
        AutoScaleVmGroupVO asGroup = createAutoScaleGroup(AS_GROUP_ID);
        mockAutoScaleGroupFindById(asGroup);
        mockLbRulesManagerConfigure(asGroup, true);
        mockAutoScaleVmMapCountBy(AS_GROUP_ID, 0);
        mockRemoveAutoScaleGroup(AS_GROUP_ID);
        mockRemoveAutoScalePolicyByGroup(AS_GROUP_ID);

        assertTrue(autoScaleManager.deleteAutoScaleVmGroup(AS_GROUP_ID));
        autoScaleManager.verifyScaleDownCount(0);
        verify(autoScaleVmGroupDao, times(1)).remove(AS_GROUP_ID);
    }

    @Test
    public void testDeleteAutoScaleGroupWithThreeVms() throws ResourceUnavailableException {
        long AS_GROUP_ID = 10L;
        AutoScaleVmGroupVO asGroup = createAutoScaleGroup(AS_GROUP_ID);
        mockAutoScaleGroupFindById(asGroup);
        mockLbRulesManagerConfigure(asGroup, true);
        mockAutoScaleVmMapCountBy(AS_GROUP_ID, 0);
        mockRemoveAutoScaleGroup(AS_GROUP_ID);
        mockRemoveAutoScalePolicyByGroup(AS_GROUP_ID);

        assertTrue(autoScaleManager.deleteAutoScaleVmGroup(AS_GROUP_ID));
        autoScaleManager.verifyScaleDownCount(0);
        verify(autoScaleVmGroupDao, times(1)).remove(AS_GROUP_ID);
    }

    @Test
    public void testDeleteAutoScaleGroupGivenErrorInLbConfiguration() throws ResourceUnavailableException {
        long AS_GROUP_ID = 10L;
        AutoScaleVmGroupVO asGroup = createAutoScaleGroup(AS_GROUP_ID);
        mockAutoScaleGroupFindById(asGroup);
        mockLbRulesManagerConfigure(asGroup, false);

        assertFalse(autoScaleManager.deleteAutoScaleVmGroup(AS_GROUP_ID));
    }

    @Test
    public void testDeleteAutoScaleGroupWithNullLbId() throws ResourceUnavailableException {
        long AS_GROUP_ID = 10L;
        AutoScaleVmGroupVO asGroup = createAutoScaleGroup(AS_GROUP_ID);
        asGroup.setLoadBalancerId(null);
        mockAutoScaleGroupFindById(asGroup);
        mockLbRulesManagerConfigure(asGroup, false);

        assertFalse(autoScaleManager.deleteAutoScaleVmGroup(AS_GROUP_ID));
    }


    private void mockAutoScaleGroupFindById(AutoScaleVmGroupVO asGroup){
        when(autoScaleVmGroupDao.findById(asGroup.getId())).thenReturn(asGroup);
    }

    private void mockLbRulesManagerConfigure(AutoScaleVmGroupVO asGroup, boolean result) throws ResourceUnavailableException {
        when(loadBalancingRulesManager.configureLbAutoScaleVmGroup(asGroup.getId(), AutoScaleVmGroup.State_Enabled)).thenReturn(true);
    }

    private void mockAutoScaleVmMapCountBy(long asGroupId, int vmCount){
        when(autoScaleVmGroupVmMapDao.countByGroup(asGroupId)).thenReturn(vmCount);
    }

    private void mockRemoveAutoScaleGroup(long asGroupId){
        when(autoScaleVmGroupDao.remove(asGroupId)).thenReturn(true);
    }

    private void mockRemoveAutoScalePolicyByGroup(long asGroupId){
        when(autoScaleVmGroupPolicyMapDao.removeByGroupId(asGroupId)).thenReturn(true);
    }

    private AutoScaleVmGroupVO createAutoScaleGroup(long id){
        AutoScaleVmGroupVO asGroup = new AutoScaleVmGroupVO(1L,1l, 1L, 1L, 1, 3, 80, 30, new Date(), 1, "enabled");
        asGroup.id = id;
        return asGroup;
    }

    protected void createAutoScaleManager() {
        autoScaleManager = new FakeAutoScaleManager();
    }

    class FakeAutoScaleManager extends AutoScaleManagerImpl{
        int scaleDownCount;
        public void doScaleDown(long groupId, Integer numVm) { scaleDownCount++; } //mocked scaledown action
        public void verifyScaleDownCount(int count){ if(count != this.scaleDownCount) fail(); }
    }
}
