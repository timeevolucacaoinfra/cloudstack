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

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.as.dao.AutoScaleVmGroupDao;
import com.cloud.network.as.dao.AutoScaleVmGroupPolicyMapDao;
import com.cloud.network.as.dao.AutoScaleVmGroupVmMapDao;
import com.cloud.network.as.dao.AutoScaleVmProfileDao;
import com.cloud.network.lb.LoadBalancingRulesManagerImpl;
import com.cloud.user.Account;
import com.cloud.user.AccountManagerImpl;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.vm.UserVmVO;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AutoScaleMangerImplTest {

    private AutoScaleManagerImpl autoScaleManager;

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
    @Mock
    AutoScaleVmProfileDao autoScaleVmProfileDao;
    @Mock
    ScheduledExecutorService threadPool;

    private static final long AS_GROUP_ID = 10L;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDeleteEmptyAutoScaleGroup() throws ResourceUnavailableException {
        autoScaleManager = createMockedScaleDownAutoScaleManager();
        AutoScaleVmGroupVO asGroup = createAutoScaleGroup();
        mockAutoScaleGroupFindById(asGroup);
        mockLbRulesManagerConfigure(asGroup, true);
        mockAutoScaleVmMapCountBy(0);
        mockRemoveAutoScaleGroup();
        mockRemoveAutoScalePolicyByGroup();

        assertTrue(autoScaleManager.deleteAutoScaleVmGroup(AS_GROUP_ID));
        verify(autoScaleManager, times(0)).doScaleDown(anyLong(), anyInt());
        verify(autoScaleVmGroupDao, times(1)).remove(AS_GROUP_ID);
    }

    @Test
    public void testDeleteAutoScaleGroupWithThreeVms() throws ResourceUnavailableException {
        autoScaleManager = createMockedScaleDownAutoScaleManager();
        AutoScaleVmGroupVO asGroup = createAutoScaleGroup();
        mockAutoScaleGroupFindById(asGroup);
        mockLbRulesManagerConfigure(asGroup, true);
        mockAutoScaleVmMapCountBy(3);
        mockRemoveAutoScaleGroup();
        mockRemoveAutoScalePolicyByGroup();

        assertTrue(autoScaleManager.deleteAutoScaleVmGroup(AS_GROUP_ID));
        verify(autoScaleManager, times(1)).doScaleDown(anyLong(), eq(3));
        verify(autoScaleVmGroupDao, times(1)).remove(AS_GROUP_ID);
    }

    @Test
    public void testDeleteAutoScaleGroupGivenErrorInLbConfiguration() throws ResourceUnavailableException {
        autoScaleManager = createMockedScaleDownAutoScaleManager();
        AutoScaleVmGroupVO asGroup = createAutoScaleGroup();
        mockAutoScaleGroupFindById(asGroup);
        mockLbRulesManagerConfigure(asGroup, false);

        assertFalse(autoScaleManager.deleteAutoScaleVmGroup(AS_GROUP_ID));
    }

    @Test
    public void testDeleteAutoScaleGroupWithNullLbId() throws ResourceUnavailableException {
        autoScaleManager = createMockedScaleDownAutoScaleManager();
        AutoScaleVmGroupVO asGroup = createAutoScaleGroup();
        asGroup.setLoadBalancerId(null);
        mockAutoScaleGroupFindById(asGroup);
        mockLbRulesManagerConfigure(asGroup, false);

        assertFalse(autoScaleManager.deleteAutoScaleVmGroup(AS_GROUP_ID));
    }

    @Test
    public void testScaleUp() throws InsufficientCapacityException, ResourceUnavailableException {
        testScaleUpWith(1, true, true);
        verify(autoScaleVmGroupVmMapDao, times(1)).persist(any(AutoScaleVmGroupVmMapVO.class));
        verify(autoScaleManager, times(1)).createEvent(eq(AS_GROUP_ID), eq(EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEUP), anyString());
    }

    @Test
    public void testScaleUpThreeVms() throws InsufficientCapacityException, ResourceUnavailableException {
        testScaleUpWith(3, true, true);
        verify(autoScaleVmGroupVmMapDao, times(3)).persist(any(AutoScaleVmGroupVmMapVO.class));
        verify(autoScaleManager, times(3)).createEvent(eq(AS_GROUP_ID), eq(EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEUP), anyString());
    }

    @Test
    public void testScaleUpFailedToAssignToLB(){
        testScaleUpWith(1, true, false);
        verify(autoScaleVmGroupVmMapDao, times(0)).persist(any(AutoScaleVmGroupVmMapVO.class));
        verify(autoScaleManager, times(1)).createEvent(eq(AS_GROUP_ID), eq(EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEUP_FAILED), anyString());
    }

    @Test
    public void testScaleUpFailedToStartVm() throws InsufficientCapacityException, ResourceUnavailableException {
        testScaleUpWith(1, false, false);
        verify(autoScaleVmGroupVmMapDao, times(0)).persist(any(AutoScaleVmGroupVmMapVO.class));
    }

    private void testScaleUpWith(int vmCount, boolean startVmResult, boolean assignToLbResult){
        AutoScaleVmGroupVO asGroup = createAutoScaleGroup();
        autoScaleManager = spy(new AutoScaleManagerImpl());
        configureMocks(autoScaleManager);

        when(autoScaleVmGroupDao.findById(10L)).thenReturn(asGroup);
        //mocking createVm, assignLBRuleToNewVm and startVM so they can be tested in isolation
        doReturn(createUserVm(1L)).when(autoScaleManager).createNewVM(asGroup);
        doReturn(startVmResult).when(autoScaleManager).startNewVM(1L);
        doReturn(assignToLbResult).when(autoScaleManager).assignLBruleToNewVm(1L, asGroup);
        if(startVmResult && assignToLbResult){
            doNothing().when(autoScaleManager).createEvent(eq(AS_GROUP_ID), eq(EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEUP), anyString());
        }else {
            doNothing().when(autoScaleManager).createEvent(eq(AS_GROUP_ID), eq(EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEUP_FAILED), anyString());
        }
        autoScaleManager.doScaleUp(AS_GROUP_ID, vmCount);
    }

    @Test
    public void testScaleUpGivenServerApiException(){
        testScaleUpException(new ServerApiException(ApiErrorCode.ACCOUNT_RESOURCE_LIMIT_ERROR, "Account limit exceeded"));
    }

    @Test
    public void testScaleUpGivenGenericException(){
        testScaleUpException(new ConcurrentOperationException("Error"));
    }

    private void testScaleUpException(Exception exception){
        AutoScaleVmGroupVO asGroup = createAutoScaleGroup();
        autoScaleManager = spy(new AutoScaleManagerImpl());
        configureMocks(autoScaleManager);

        when(autoScaleVmGroupDao.findById(10L)).thenReturn(asGroup);
        //mocking createVm, assignLBRuleToNewVm and startVM so they can be tested in isolation
        doReturn(createUserVm(1L)).when(autoScaleManager).createNewVM(asGroup);
        doThrow(exception).when(autoScaleManager).startNewVM(1L);
        doNothing().when(autoScaleManager).createEvent(eq(AS_GROUP_ID), eq(EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEUP_FAILED), anyString());

        try {
            autoScaleManager.doScaleUp(AS_GROUP_ID, 1);
            fail();
        }catch(Exception ex){
            verify(autoScaleVmGroupVmMapDao, times(0)).persist(any(AutoScaleVmGroupVmMapVO.class));
            verify(autoScaleManager, times(1)).createEvent(eq(AS_GROUP_ID), eq(EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEUP_FAILED), anyString());
        }
    }

    @Test
    public void testScaleDown(){
        testScaleDownWith(1L, null);

        verify(autoScaleVmGroupVmMapDao, times(1)).remove(AS_GROUP_ID, 1L);
        verify(threadPool, times(1)).schedule(any(Runnable.class), eq(120L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testScaleDownGivenErrorRemovingFromLB(){
        testScaleDownWith(-1L, null);

        verify(autoScaleVmGroupVmMapDao, times(0)).remove(AS_GROUP_ID, 1L);
        verify(autoScaleManager, times(1)).createEvent(eq(AS_GROUP_ID), eq(EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEDOWN_FAILED), anyString());
    }

    @Test
    public void testScaleDownGivenError(){
        try {
            testScaleDownWith(1L, new RuntimeException());
            fail();
        }catch(Exception ex){
            verify(autoScaleVmGroupVmMapDao, times(0)).remove(AS_GROUP_ID, 1L);
            verify(autoScaleManager, times(1)).createEvent(eq(AS_GROUP_ID), eq(EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEDOWN_FAILED), anyString());
        }
    }

    private void testScaleDownWith(Long removeLbResult, Exception exception) {
        AutoScaleVmGroupVO asGroup = createAutoScaleGroup();
        autoScaleManager = spy(new AutoScaleManagerImpl());
        configureMocks(autoScaleManager);

        when(autoScaleVmGroupDao.findById(10L)).thenReturn(asGroup);
        doReturn(true).when(autoScaleManager).checkConditionDown(asGroup);
        if(exception != null){
            doThrow(exception).when(autoScaleManager).removeLBrule(asGroup);
        }else{
            doReturn(removeLbResult).when(autoScaleManager).removeLBrule(asGroup);
        }
        if(removeLbResult == -1 || exception != null){
            doNothing().when(autoScaleManager).createEvent(eq(AS_GROUP_ID), eq(EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEDOWN_FAILED), anyString());
        }
        when(autoScaleVmProfileDao.findById(asGroup.getProfileId())).thenReturn(new AutoScaleVmProfileVO());
        autoScaleManager.doScaleDown(AS_GROUP_ID, 1);
    }

    private void mockAutoScaleGroupFindById(AutoScaleVmGroupVO asGroup){
        when(autoScaleVmGroupDao.findById(asGroup.getId())).thenReturn(asGroup);
    }

    private void mockLbRulesManagerConfigure(AutoScaleVmGroupVO asGroup, boolean result) throws ResourceUnavailableException {
        when(loadBalancingRulesManager.configureLbAutoScaleVmGroup(asGroup.getId(), AutoScaleVmGroup.State_Enabled)).thenReturn(result);
    }

    private void mockAutoScaleVmMapCountBy(int vmCount){
        when(autoScaleVmGroupVmMapDao.countByGroup(AutoScaleMangerImplTest.AS_GROUP_ID)).thenReturn(vmCount);
    }

    private void mockRemoveAutoScaleGroup(){
        when(autoScaleVmGroupDao.remove(AutoScaleMangerImplTest.AS_GROUP_ID)).thenReturn(true);
    }

    private void mockRemoveAutoScalePolicyByGroup(){
        when(autoScaleVmGroupPolicyMapDao.removeByGroupId(AutoScaleMangerImplTest.AS_GROUP_ID)).thenReturn(true);
    }

    private AutoScaleVmGroupVO createAutoScaleGroup(){
        AutoScaleVmGroupVO asGroup = new AutoScaleVmGroupVO(1L,1l, 1L, 1L, 1, 3, 80, 30, new Date(), 1, "enabled");
        asGroup.id = AutoScaleMangerImplTest.AS_GROUP_ID;
        return asGroup;
    }

    private UserVmVO createUserVm(Long id) {
        return new UserVmVO(id, "test", "test", 1L, Hypervisor.HypervisorType.Any, 1L, false, false, 1L, 1L, 5L, "test", "test", 1L);
    }

    private AutoScaleManagerImpl createMockedScaleDownAutoScaleManager() {
        AutoScaleManagerImpl scaleManager = spy(new AutoScaleManagerImpl());
        configureMocks(scaleManager);
        doNothing().when(scaleManager).doScaleDown(anyLong(), anyInt());
        return scaleManager;
    }

    private void configureMocks(AutoScaleManagerImpl autoScaleManager) {
        autoScaleManager._lbRulesMgr = loadBalancingRulesManager;
        autoScaleManager._autoScaleVmGroupDao = autoScaleVmGroupDao;
        autoScaleManager._autoScaleVmGroupVmMapDao = autoScaleVmGroupVmMapDao;
        autoScaleManager._accountMgr = accountManager;
        autoScaleManager._autoScaleVmGroupPolicyMapDao = autoScaleVmGroupPolicyMapDao;
        autoScaleManager._autoScaleVmProfileDao = autoScaleVmProfileDao;
        autoScaleManager._executor = threadPool;

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
}
