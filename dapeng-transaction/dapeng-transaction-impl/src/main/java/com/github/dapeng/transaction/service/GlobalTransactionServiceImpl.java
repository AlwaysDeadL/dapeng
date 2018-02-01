package com.github.dapeng.transaction.service;

import com.github.dapeng.transaction.api.domain.TGlobalTransaction;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionsStatus;
import com.github.dapeng.transaction.api.service.GlobalTransactionService;
import com.github.dapeng.transaction.dao.ITransactionDao;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.transaction.api.domain.TGlobalTransaction;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionsStatus;
import com.github.dapeng.transaction.api.service.GlobalTransactionService;
import com.github.dapeng.transaction.dao.ITransactionDao;
import com.github.dapeng.transaction.utils.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by tangliu on 2016/4/12.
 */
@Transactional(value = "globalTransaction", rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
public class GlobalTransactionServiceImpl implements GlobalTransactionService {

    public static void checkout(boolean assertion, String code, String message) throws SoaException {
        if (!assertion)
            throw new SoaException(code, message);
    }

    Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionServiceImpl.class);


    @Autowired
    ITransactionDao transactionDao;

    @Override
    public TGlobalTransaction create(TGlobalTransaction g) throws SoaException {

        checkout(g.getStatus() != null, ErrorCode.INPUTERROR.getCode(), "状态不能为空");
        checkout(g.getCurrSequence() != null, ErrorCode.INPUTERROR.getCode(), "当前过程序列号不能为空");

        Integer id = transactionDao.insert(g);
        g.setId(id);

        LOGGER.info("创建全局事务({}),状态为({}),当前过程序列号为({})", g.getId(), g.getStatus().getValue(), g.getCurrSequence());

        return g;
    }


    @Override
    public void update(Integer globalTransactionId, Integer currSequence, TGlobalTransactionsStatus status) throws SoaException {

        checkout(globalTransactionId > 0, ErrorCode.INPUTERROR.getCode(), "transactionId 错误");

        TGlobalTransaction globalTransaction = transactionDao.getGlobalByIdForUpdate(globalTransactionId);

        if (globalTransaction == null)
            throw new SoaException(ErrorCode.NOTEXIST.getCode(), ErrorCode.NOTEXIST.getMsg());

        LOGGER.info("更新全局事务({})前,状态({}),当前过程序列号({})", globalTransaction.getId(), globalTransaction.getStatus().getValue(), globalTransaction.getCurrSequence());

        transactionDao.updateGlobalTransactionStatusAndCurrSeq(status.getValue(), currSequence, globalTransactionId);

        LOGGER.info("更新全局事务({})后,状态({}),当前过程序列号({})", globalTransaction.getId(), status.getValue(), currSequence);

    }
}
