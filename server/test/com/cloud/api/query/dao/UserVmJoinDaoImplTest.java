package com.cloud.api.query.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UserVmJoinDaoImplTest {

    @Test
    public void testCreateQueryByTags() {
        UserVmJoinDaoImpl dao = new UserVmJoinDaoImpl();

        //1
        List params = new ArrayList();
        Map<String, String > tags = new HashMap<>();
        String queryByTags = dao.createQueryByTags(12L, tags, params);

        assertEquals("SELECT tg.resource_uuid as resource_uuid FROM resource_tag_view tg WHERE tg.resource_type = 'UserVm' AND tg.project_id = ? ", queryByTags);
        assertEquals(1, params.size());

        //2
        tags = new HashMap<>();
        params = new ArrayList();
        tags.put("monitoring", "1");
        queryByTags = dao.createQueryByTags(12L, tags, params);

        assertEquals("SELECT tg.resource_uuid as resource_uuid FROM resource_tag_view tg WHERE tg.resource_type = 'UserVm' AND tg.project_id = ? " +
                " AND ( (tg.key = ? AND tg.value = ?)) GROUP BY tg.resource_uuid HAVING count(tg.resource_uuid) = ?", queryByTags);
        assertEquals(4, params.size());


        //3
        tags = new HashMap<>();
        params = new ArrayList();
        tags.put("monitoring", "1");
        tags.put("abc", "123");
        queryByTags = dao.createQueryByTags(null, tags, params);

        assertEquals("SELECT tg.resource_uuid as resource_uuid FROM resource_tag_view tg WHERE tg.resource_type = 'UserVm'" +
                " AND ( (tg.key = ? AND tg.value = ?) OR (tg.key = ? AND tg.value = ?)) GROUP BY tg.resource_uuid HAVING count(tg.resource_uuid) = ?", queryByTags);
        assertEquals(5, params.size());

    }
}