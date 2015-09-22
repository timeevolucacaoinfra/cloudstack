package com.globo.globonetwork.cloudstack.api;

import com.globo.globonetwork.cloudstack.manager.GloboNetworkManager;
import com.globo.globonetwork.cloudstack.response.GloboNetworkPoolResponse;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PoolResponse;

import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;


public class UpdateGloboNetworkPoolCmdTest extends TestCase {

    public void testExecute() throws Exception {
        UpdateGloboNetworkPoolCmd cmd = new UpdateGloboNetworkPoolCmd();

        List<GloboNetworkPoolResponse.Pool> lbResponses = new ArrayList<GloboNetworkPoolResponse.Pool>();
        GloboNetworkPoolResponse.Pool pool1 = new GloboNetworkPoolResponse.Pool();
        pool1.setId(12l);
        pool1.setHealthcheck("/heal.html");
        pool1.setHealthcheckType("HTTP");
        pool1.setExpectedHealthcheck("OK");
        lbResponses.add(pool1);

        GloboNetworkPoolResponse.Pool pool2 = new GloboNetworkPoolResponse.Pool();
        pool2.setId(13l);
        pool2.setHealthcheck("/heal.html");
        pool2.setHealthcheckType("HTTP");
        pool2.setExpectedHealthcheck("OK");
        lbResponses.add(pool2);


        List<Long> ids = new ArrayList<Long>();
        ids.add(12l);
        ids.add(13l);

        GloboNetworkManager mock = mock(GloboNetworkManager.class);
        when(mock.updatePools(ids, 123l, 10l, "HTTP", "/heal.html", "OK")).thenReturn(lbResponses);
        cmd._globoNetworkService = mock;


        cmd.setPoolIds(ids);
        cmd.setLbId(123l);
        cmd.setZoneId(10l);
        cmd.setHealthcheckType("HTTP");
        cmd.setHealthcheck("/heal.html");
        cmd.setExpectedHealthcheck("OK");
        cmd.execute();

        ListResponse list = (ListResponse) cmd.getResponseObject();
        assertEquals((Integer) 2, list.getCount());

        List<PoolResponse> pools = list.getResponses();

        PoolResponse pool11 = pools.get(0);
        assertEquals((Long)12l, pool11.getId());
        assertEquals("HTTP", pool11.getHealthcheckType());
        assertEquals("/heal.html", pool11.getHealthcheck());
        assertEquals("OK", pool11.getExpectedHealthcheck());

        PoolResponse pool21 = pools.get(1);
        assertEquals((Long)13l, pool21.getId());
        assertEquals("HTTP", pool21.getHealthcheckType());
        assertEquals("/heal.html", pool21.getHealthcheck());
        assertEquals("OK", pool21.getExpectedHealthcheck());


    }
}