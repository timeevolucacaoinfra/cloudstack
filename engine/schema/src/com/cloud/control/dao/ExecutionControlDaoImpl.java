package com.cloud.control.dao;

import com.cloud.control.ExecutionControlVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import org.springframework.stereotype.Component;
import javax.ejb.Local;

@Component
@Local(value = {ExecutionControlDao.class})
@DB
public class ExecutionControlDaoImpl extends GenericDaoBase<ExecutionControlVO, Long> implements ExecutionControlDao {

    protected final SearchBuilder<ExecutionControlVO> ExecutionControlFindByProcessAlias;

    public ExecutionControlDaoImpl() {
        ExecutionControlFindByProcessAlias = createSearchBuilder();
        ExecutionControlFindByProcessAlias.and("process_alias", ExecutionControlFindByProcessAlias.entity().getProcessAlias(), SearchCriteria.Op.IN);
        ExecutionControlFindByProcessAlias.done();
    }

    @Override
    public ExecutionControlVO findByProcessAlias(String processAlias) {
        SearchCriteria<ExecutionControlVO> sc = ExecutionControlFindByProcessAlias.create();
        sc.setParameters("process_alias", processAlias);
        return findOneBy(sc);
    }
}
