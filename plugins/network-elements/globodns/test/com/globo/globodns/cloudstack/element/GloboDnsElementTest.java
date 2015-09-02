package com.globo.globodns.cloudstack.element;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.globo.globodns.cloudstack.commands.CreateOrUpdateRecordAndReverseCommand;
import com.globo.globodns.cloudstack.commands.RemoveRecordCommand;

public class GloboDnsElementTest {

    private static long zoneId = 5L;
    private static long globoDnsHostId = 7L;
    private static long domainId = 10L;
    private AccountVO acct = null;
    private UserVO user = null;

    GloboDnsElement _globodnsElement;

    @Mock
    DataCenterDao _datacenterDao;

    @Mock
    HostDao _hostDao;

    @Mock
    AgentManager _agentMgr;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        _globodnsElement = new GloboDnsElement();

        _globodnsElement._dcDao = _datacenterDao;
        _globodnsElement._hostDao = _hostDao;
        _globodnsElement._agentMgr = _agentMgr;

        acct = new AccountVO(200L);
        acct.setType(Account.ACCOUNT_TYPE_NORMAL);
        acct.setAccountName("user");
        acct.setDomainId(domainId);

        user = new UserVO();
        user.setUsername("user");
        user.setAccountId(acct.getAccountId());

        CallContext.register(user, acct);
    }

    @After
    public void tearDown() throws Exception {
        CallContext.unregister();
        acct = null;
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpperCaseCharactersAreNotAllowed() throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        Network network = mock(Network.class);
        when(network.getDataCenterId()).thenReturn(zoneId);
        when(network.getId()).thenReturn(1l);
        NicProfile nic = new NicProfile();
        VirtualMachineProfile vm = mock(VirtualMachineProfile.class);
        when(vm.getHostName()).thenReturn("UPPERCASENAME");
        when(vm.getType()).thenReturn(VirtualMachine.Type.User);
        when(_datacenterDao.findById(zoneId)).thenReturn(mock(DataCenterVO.class));
        DeployDestination dest = new DeployDestination();
        ReservationContext context = new ReservationContextImpl(null, null, user);
        boolean result = _globodnsElement.prepare(network, nic, vm, dest, context);

        assertTrue(result);
        verify(_agentMgr, times(1)).easySend(eq(globoDnsHostId), isA(CreateOrUpdateRecordAndReverseCommand.class));
    }

    @Test
    public void testPrepareMethodCallGloboDnsToRegisterHostName() throws Exception {
        Network network = mock(Network.class);
        when(network.getDataCenterId()).thenReturn(zoneId);
        when(network.getId()).thenReturn(1l);
        NicProfile nic = new NicProfile();
        nic.setIp4Address("10.11.12.13");
        VirtualMachineProfile vm = mock(VirtualMachineProfile.class);
        when(vm.getHostName()).thenReturn("vm-name");
        when(vm.getType()).thenReturn(VirtualMachine.Type.User);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);
        when(dataCenterVO.getId()).thenReturn(zoneId);
        when(_datacenterDao.findById(zoneId)).thenReturn(dataCenterVO);
        DeployDestination dest = new DeployDestination();
        ReservationContext context = new ReservationContextImpl(null, null, user);

        HostVO hostVO = mock(HostVO.class);
        when(hostVO.getId()).thenReturn(globoDnsHostId);
        when(_hostDao.findByTypeNameAndZoneId(eq(zoneId), eq(Provider.GloboDns.getName()), eq(Type.L2Networking))).thenReturn(hostVO);

        when(_agentMgr.easySend(eq(globoDnsHostId), isA(CreateOrUpdateRecordAndReverseCommand.class))).then(new org.mockito.stubbing.Answer<Answer>() {

            @Override
            public Answer answer(InvocationOnMock invocation) throws Throwable {
                Command cmd = (Command)invocation.getArguments()[1];
                return new Answer(cmd);
            }
        });

        boolean result = _globodnsElement.prepare(network, nic, vm, dest, context);
        assertTrue(result);
        verify(_agentMgr, times(1)).easySend(eq(globoDnsHostId), isA(CreateOrUpdateRecordAndReverseCommand.class));
    }

    @Test
    public void testReleaseMethodCallResource() throws Exception {
        Network network = mock(Network.class);
        when(network.getDataCenterId()).thenReturn(zoneId);
        when(network.getId()).thenReturn(1l);
        NicProfile nic = new NicProfile();
        nic.setIp4Address("10.11.12.13");
        VirtualMachineProfile vm = mock(VirtualMachineProfile.class);
        when(vm.getHostName()).thenReturn("vm-name");
        when(vm.getType()).thenReturn(VirtualMachine.Type.User);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);
        when(dataCenterVO.getId()).thenReturn(zoneId);
        when(_datacenterDao.findById(zoneId)).thenReturn(dataCenterVO);
        ReservationContext context = new ReservationContextImpl(null, null, user);

        HostVO hostVO = mock(HostVO.class);
        when(hostVO.getId()).thenReturn(globoDnsHostId);
        when(_hostDao.findByTypeNameAndZoneId(eq(zoneId), eq(Provider.GloboDns.getName()), eq(Type.L2Networking))).thenReturn(hostVO);

        when(_agentMgr.easySend(eq(globoDnsHostId), isA(RemoveRecordCommand.class))).then(new org.mockito.stubbing.Answer<Answer>() {

            @Override
            public Answer answer(InvocationOnMock invocation) throws Throwable {
                Command cmd = (Command)invocation.getArguments()[1];
                return new Answer(cmd);
            }
        });

        boolean result = _globodnsElement.release(network, nic, vm, context);
        assertTrue(result);
        verify(_agentMgr, times(1)).easySend(eq(globoDnsHostId), isA(RemoveRecordCommand.class));
    }

    @Test(expected=InvalidParameterValueException.class)
    public void testUnderscoreInLoadBalancerNameNotAllowed() {
        String lbDomain = "lb.globo.com";
        String lbRecord = "test_underscore";
        String lbRecordContent = "10.0.0.1";
        boolean result = _globodnsElement.validateDnsRecordForLoadBalancer(lbDomain, lbRecord, lbRecordContent, zoneId);
    }
}
