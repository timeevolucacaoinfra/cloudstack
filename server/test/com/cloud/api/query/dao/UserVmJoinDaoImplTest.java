package com.cloud.api.query.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UserVmJoinDaoImplTest {

    @Test
    public void testBuildListVmsByProjectAndTags() {
        UserVmJoinDaoImpl dao = new UserVmJoinDaoImpl();

        List params = new ArrayList();
        Map<String, String > tags = new HashMap<>();

        String sql = dao.buildListVmsByProjectAndTags(123l, tags, params);

        assertEquals("SELECT id, uuid, name, display_name, instance_name, state, ha_enabled, " +
                            "project_name, project_id, account_id, domain_id, " +
                            "display_vm, service_offering_uuid, service_offering_name, " +
                            "os_id, os_name, host_name, data_center_id, data_center_name " +
                "FROM view_globo_vm vm WHERE 1=1 AND vm.project_id = ?", sql);
        assertEquals(1, params.size());
        Object[] values = {123l};
        assertEquals(values, params.toArray());

        //case 1 ( projectId + 1 tagkey + 1 tagvalue
        params = new ArrayList();
        tags = new HashMap<>();
        tags.put("monitoring", "1");
        sql = dao.buildListVmsByProjectAndTags(33l, tags, params);

        assertEquals("SELECT id, uuid, name, display_name, instance_name, state, " +
                "ha_enabled, project_name, project_id, account_id, " +
                "domain_id, display_vm, service_offering_uuid, service_offering_name, " +
                "os_id, os_name, host_name, data_center_id, data_center_name " +
                "FROM view_globo_vm vm WHERE 1=1" +
                " AND vm.project_id = ?" +
                " AND vm.uuid IN (SELECT tg.resource_uuid FROM resource_tags tg WHERE 1=1 AND (" +
                " (tg.key = ? AND tg.value = ?)" +
                ") GROUP BY tg.resource_uuid HAVING count(tg.resource_uuid) = ?)", sql);
        assertEquals(4, params.size());
        Object[] values2 = {33l, "monitoring", "1", 1};
        assertEquals(values2, params.toArray());

        //case 2 ( projectId + 2x tagkey + 2x tagvalue
        params = new ArrayList();
        tags = new HashMap<>();
        tags.put("monitoring", "1");
        tags.put("label", "ABC");
        sql = dao.buildListVmsByProjectAndTags(444l, tags, params);

        assertEquals("SELECT id, uuid, name, display_name, instance_name, state, ha_enabled, project_name, project_id, account_id, domain_id, display_vm, service_offering_uuid, service_offering_name, os_id, os_name, host_name, data_center_id, data_center_name" +
                " FROM view_globo_vm vm WHERE 1=1" +
                " AND vm.project_id = ?" +
                " AND vm.uuid IN (SELECT tg.resource_uuid FROM resource_tags tg WHERE 1=1 AND (" +
                " (tg.key = ? AND tg.value = ?)" +
                " OR" +
                " (tg.key = ? AND tg.value = ?)" +
                ") GROUP BY tg.resource_uuid HAVING count(tg.resource_uuid) = ?)", sql);
        assertEquals(6, params.size());
        Object[] values3 = {444l, "monitoring", "1", "label", "ABC", 2};
        assertEquals(values3, params.toArray());

    }
}