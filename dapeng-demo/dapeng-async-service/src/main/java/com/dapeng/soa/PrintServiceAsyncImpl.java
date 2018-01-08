package com.dapeng.soa;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.definition.AsyncService;
import com.github.dapeng.soa.account.enums.AccountType;
import com.github.dapeng.soa.info.Info;
import com.github.dapeng.soa.service.PrintServiceAsync;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Created by lihuimin on 2018/1/2.
 */
public class PrintServiceAsyncImpl implements PrintServiceAsync ,AsyncService {
    @Override
    public Future<Void> print(long timeout) throws SoaException {
        return null;
    }

    @Override
    public Future<String> printInfo(Info info, long timeout) throws SoaException {
        return null;
    }

    @Override
    public Future<String> printInfo2(String name, long timeout) throws SoaException {
        CompletableFuture<String> response = CompletableFuture.supplyAsync(()->{
            String result = "233333333";
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //返回结果
            return "result : " + result;

        });

        return  response;
    }

    @Override
    public Future<String> printInfo3(AccountType accountType, long timeout) throws SoaException {
        return null;
    }
}
