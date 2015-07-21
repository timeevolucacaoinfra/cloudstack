package org.apache.cloudstack.api.command.user.network;

import com.cloud.network.NetworkService;
import com.cloud.utils.exception.CloudRuntimeException;
import junit.framework.TestCase;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DeleteNetworkCmdTest extends TestCase {

    private static final Logger s_logger = Logger.getLogger(DeleteNetworkCmdTest.class);

    private DeleteNetworkCmd cmd;

    @Before
    public void setUp() {
       cmd = new DeleteNetworkCmd() {
            public Long getId() {
                return 1l;
            }
            public boolean isForced() {
                return false;
            }
        };
    }

    @Test
    public void testExecute() {
        NetworkService netService = Mockito.mock(NetworkService.class);
        Mockito.when(netService.deleteNetwork(1l, false)).thenReturn(true);
        cmd._networkService = netService;

        cmd.execute();

        SuccessResponse response = (SuccessResponse) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("deletenetworkresponse", response.getResponseName());
    }

    @Test
    public void testExecute_not_created() {
        NetworkService netService = Mockito.mock(NetworkService.class);
        Mockito.when(netService.deleteNetwork(1l, false)).thenReturn(false);
        cmd._networkService = netService;

        try {
            cmd.execute();
            fail("expected exception ");
        }catch (ServerApiException e) {
            assertEquals("Failed to delete network", e.getMessage());
        }catch (Exception e) {
            fail("expected ServerApiException " + e.getClass() + " " + e.getMessage());
        } finally {
            Mockito.verify(netService, Mockito.times(1)).deleteNetwork(1l, false);
        }
    }

    @Test
    public void testExecute_fail() {
        NetworkService netService = Mockito.mock(NetworkService.class);
        Mockito.when(netService.deleteNetwork(1l, false)).thenThrow(new CloudRuntimeException("not deleted"));
        cmd._networkService = netService;

        try {
            cmd.execute();
        }catch (ServerApiException e) {
            assertEquals("not deleted", e.getMessage());
        }catch (Exception e) {
            fail("should be ServerApiException " + e.getClass() + "  " + e.getMessage());
        }finally {
            Mockito.verify(netService, Mockito.times(1)).deleteNetwork(1l, false);
        }
    }
}