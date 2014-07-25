package com.globo.globodns.cloudstack.resource;


import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.cloud.agent.api.Answer;
import com.cloud.utils.component.ComponentContext;
import com.globo.globodns.client.GloboDns;
import com.globo.globodns.client.api.DomainAPI;
import com.globo.globodns.client.api.ExportAPI;
import com.globo.globodns.client.api.RecordAPI;
import com.globo.globodns.client.model.Domain;
import com.globo.globodns.client.model.Record;
import com.globo.globodns.cloudstack.commands.CreateOrUpdateRecordAndReverseCommand;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class GloboDnsResourceTest {
	
	private GloboDnsResource _globoDnsResource;
	
	private GloboDns _globoDnsApi;
	private DomainAPI _domainApi;
	private RecordAPI _recordApi;
	private ExportAPI _exportApi;

	@Before
	public void setUp() throws Exception {
        ComponentContext.initComponentsLifeCycle();
        
        String name = "GloboDNS";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("zoneId", "1");
        params.put("guid", "globodns");
        params.put("name", name);
        params.put("url", "http://example.com");
        params.put("username", "username");
        params.put("password", "password");
        
        _globoDnsResource = new GloboDnsResource();
        _globoDnsResource.configure(name, params);
        
        _globoDnsApi = spy(_globoDnsResource._globoDns);
        _globoDnsResource._globoDns = _globoDnsApi;
        
        _domainApi = mock(DomainAPI.class);
        when(_globoDnsApi.getDomainAPI()).thenReturn(_domainApi);
        
        _recordApi = mock(RecordAPI.class);
        when(_globoDnsApi.getRecordAPI()).thenReturn(_recordApi);
        
        _exportApi = mock(ExportAPI.class);
        when(_globoDnsApi.getExportAPI()).thenReturn(_exportApi);
    }
    
    @After
	public void tearDown() throws Exception {
    }
    
    @Test
    public void testCreateRecordAndReverseWhenDomainExists() throws Exception {
    	String recordName = "recordname";
    	String recordIp = "10.10.10.10";
    	String domainName = "domain.name.com";
    	String reverseDomainName = "10.10.10.in-addr.arpa";
    	String reverseRecordName = "10";
    	String reverseRecordContent = recordName + "." + domainName;
    	
    	Domain domain = new Domain();
    	domain.getDomainAttributes().setId(1L);
    	domain.getDomainAttributes().setName(domainName);
    	List<Domain> domainList = new ArrayList<Domain>();
    	domainList.add(domain);
    	when(_domainApi.listByQuery(domainName)).thenReturn(domainList);

    	Domain reverseDomain = new Domain();
    	reverseDomain.getDomainAttributes().setId(2L);
    	reverseDomain.getDomainAttributes().setName(reverseDomainName);
    	List<Domain> reverseDomainList = new ArrayList<Domain>();
    	reverseDomainList.add(reverseDomain);
    	when(_domainApi.listByQuery(reverseDomainName)).thenReturn(reverseDomainList);
    	
    	Record record = new Record();
    	record.getTypeARecordAttributes().setName(recordName);
    	record.getTypeARecordAttributes().setContent(recordIp);
    	List<Record> recordList = new ArrayList<Record>();
    	recordList.add(record);
    	when(_recordApi.listByQuery(domain.getId(), recordName)).thenReturn(recordList);
    	when(_recordApi.createRecord(domain.getId(), recordName, recordIp, "A")).thenReturn(record);

    	Record reverseRecord = new Record();
    	reverseRecord.getTypePTRRecordAttributes().setName(recordIp);
    	record.getTypeARecordAttributes().setContent(reverseRecordName);
    	List<Record> reverseRecordList = new ArrayList<Record>();
    	reverseRecordList.add(reverseRecord);
    	when(_recordApi.listByQuery(domain.getId(), reverseRecordName)).thenReturn(reverseRecordList);
    	when(_recordApi.createRecord(reverseDomain.getId(), reverseRecordName, reverseRecordContent, "PTR")).thenReturn(reverseRecord);

    	Answer answer = _globoDnsResource.execute(new CreateOrUpdateRecordAndReverseCommand(recordName, recordIp, domainName));
    	assertNotNull(answer);
    	assertEquals(true, answer.getResult());
    	verify(_exportApi, times(1)).scheduleExport();
    }

    @Test
    public void testCreateRecordAndReverseWhenDomainDoesntExist() throws Exception {
    	String recordName = "recordname";
    	String recordIp = "10.10.10.10";
    	String domainName = "domain.name.com";
    	
    	when(_domainApi.listByQuery(domainName)).thenReturn(new ArrayList<Domain>());

    	Answer answer = _globoDnsResource.execute(new CreateOrUpdateRecordAndReverseCommand(recordName, recordIp, domainName));
    	assertNotNull(answer);
    	assertEquals(false, answer.getResult());
    	assertEquals("Invalid domain", answer.getDetails());
    	verify(_exportApi, never()).scheduleExport();
    }

    @Test
    public void testUpdateRecordAndReverseWhenDomainExists() throws Exception {
    	String recordName = "recordname";
    	String oldRecordIp = "10.10.10.10";
    	String newRecordIp = "20.20.20.20";
    	String domainName = "domain.name.com";
    	String reverseDomainName = "20.20.20.in-addr.arpa";
    	String reverseRecordName = "20";
    	String reverseRecordContent = recordName + "." + domainName;
    	
    	Domain domain = new Domain();
    	domain.getDomainAttributes().setId(1L);
    	domain.getDomainAttributes().setName(domainName);
    	List<Domain> domainList = new ArrayList<Domain>();
    	domainList.add(domain);
    	when(_domainApi.listByQuery(domainName)).thenReturn(domainList);

    	Domain reverseDomain = new Domain();
    	reverseDomain.getDomainAttributes().setId(2L);
    	reverseDomain.getDomainAttributes().setName(reverseDomainName);
    	List<Domain> reverseDomainList = new ArrayList<Domain>();
    	reverseDomainList.add(reverseDomain);
    	when(_domainApi.listByQuery(reverseDomainName)).thenReturn(reverseDomainList);
    	
    	Record record = new Record();
    	record.getTypeARecordAttributes().setName(recordName);
    	record.getTypeARecordAttributes().setContent(oldRecordIp);
    	List<Record> recordList = new ArrayList<Record>();
    	recordList.add(record);
    	when(_recordApi.listByQuery(domain.getId(), recordName)).thenReturn(recordList);    	

    	Record reverseRecord = new Record();
    	reverseRecord.getTypePTRRecordAttributes().setName(oldRecordIp);
    	record.getTypeARecordAttributes().setContent(reverseRecordName);
    	List<Record> reverseRecordList = new ArrayList<Record>();
    	reverseRecordList.add(reverseRecord);
    	when(_recordApi.listByQuery(domain.getId(), reverseRecordName)).thenReturn(reverseRecordList);
    	when(_recordApi.createRecord(reverseDomain.getId(), reverseRecordName, reverseRecordContent, "PTR")).thenReturn(reverseRecord);

    	Answer answer = _globoDnsResource.execute(new CreateOrUpdateRecordAndReverseCommand(recordName, newRecordIp, domainName));
    	verify(_recordApi, times(1)).updateRecord(record.getId(), domain.getId(), recordName, newRecordIp);
    	assertNotNull(answer);
    	assertEquals(true, answer.getResult());
    	verify(_exportApi, times(1)).scheduleExport();
    }
}
