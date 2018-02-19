package com.github.dapeng.impl.plugins.netty;


import com.github.dapeng.api.Container;
import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.core.filter.*;
import com.github.dapeng.impl.filters.HeadFilter;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.RegistryAgentProxy;
import com.github.dapeng.util.ExceptionUtil;
import com.github.dapeng.util.SoaSystemEnvProperties;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.github.dapeng.util.SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;

/**
 * @author lihuimin
 * @date 2017/12/7
 */
@ChannelHandler.Sharable
public class SoaServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaServerHandler.class);

    private final Container container;

    SoaServerHandler(Container container) {
        this.container = container;
    }

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object msg) {
        final long invokeTime = System.currentTimeMillis();
        final TransactionContext transactionContext = TransactionContext.Factory.getCurrentInstance();
        try {
            SoaHeader soaHeader = transactionContext.getHeader();
            SoaServiceDefinition processor = container.getServiceProcessors().get(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

            container.getDispatcher().execute(() -> {
                try {
                    TransactionContext.Factory.setCurrentInstance(transactionContext);
                    processRequest(channelHandlerContext, processor, msg, transactionContext, invokeTime);
                } catch (Throwable e) {
                    writeErrorMessage(channelHandlerContext,
                            transactionContext,
                            ExceptionUtil.convertToSoaException(e));
                } finally {
                    TransactionContext.Factory.removeCurrentInstance();
                }
            });
        } catch (Throwable ex) {
            if (transactionContext.getHeader() == null) {
                transactionContext.setHeader(new SoaHeader());
            }
            writeErrorMessage(channelHandlerContext, transactionContext, new SoaException(SoaCode.UnKnown.getCode(), "读请求异常", ex));
        } finally {
            TransactionContext.Factory.removeCurrentInstance();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error(cause.getMessage(), cause);

        ctx.close();
    }

    private <I, REQ, RESP> void processRequest(ChannelHandlerContext channelHandlerContext,
                                               SoaServiceDefinition<I> serviceDef,
                                               REQ args,
                                               TransactionContext transactionContext,
                                               long invokeTime) throws TException {

        FilterContextImpl filterContext = new FilterContextImpl();
        try {
            SoaHeader soaHeader = transactionContext.getHeader();

            //check if request expired
            final long waitingTime = System.currentTimeMillis() - invokeTime;
            long timeout = getTimeout(soaHeader);
            if (waitingTime > timeout) {
                throw new SoaException(SoaCode.TimeOut, "服务端请求超时");
            }

            Application application = container.getApplication(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

            if (application == null) {
                throw new SoaException(SoaCode.NotMatchedService);
            }
            SoaFunctionDefinition<I, REQ, RESP> soaFunction = (SoaFunctionDefinition<I, REQ, RESP>) serviceDef.functions.get(soaHeader.getMethodName());

            if (soaFunction == null) {
                throw new SoaException(SoaCode.NotMatchedMethod);
            }

            I iface = serviceDef.iface;

            HeadFilter headFilter = new HeadFilter();
            Filter dispatchFilter = new Filter() {

                private FilterChain getPrevChain(FilterContext ctx) {
                    SharedChain chain = (SharedChain) ctx.getAttach(this, "chain");
                    return new SharedChain(chain.head, chain.shared, chain.tail, chain.size() - 2);
                }

                @Override
                public void onEntry(FilterContext filterContext, FilterChain next) {
                    try {
                        if (serviceDef.isAsync) {
                            SoaFunctionDefinition.Async asyncFunc = (SoaFunctionDefinition.Async) soaFunction;
                            CompletableFuture<RESP> future = (CompletableFuture<RESP>) asyncFunc.apply(iface, args);
                            future.whenComplete((realResult, ex) -> {
                                if (ex != null) {
                                    SoaException soaException = ExceptionUtil.convertToSoaException(ex);
                                    writeErrorMessage(channelHandlerContext, transactionContext, soaException);
                                } else {
                                    TransactionContext.Factory.setCurrentInstance(transactionContext);
                                    processResult(channelHandlerContext, soaFunction, transactionContext, realResult, filterContext);
                                }
                                onExit(filterContext, getPrevChain(filterContext));
                            });
                        } else {
                            SoaFunctionDefinition.Sync syncFunction = (SoaFunctionDefinition.Sync) soaFunction;
                            RESP result = (RESP) syncFunction.apply(iface, args);
                            processResult(channelHandlerContext, soaFunction, transactionContext, result, filterContext);
                            onExit(filterContext, getPrevChain(filterContext));
                        }
                    } catch (Throwable e) {
                        writeErrorMessage(channelHandlerContext, transactionContext, ExceptionUtil.convertToSoaException(e));
                    }
                }

                @Override
                public void onExit(FilterContext ctx, FilterChain prev) {
                    try {
                        prev.onExit(ctx);
                    } catch (TException e) {
                        writeErrorMessage(channelHandlerContext, transactionContext, ExceptionUtil.convertToSoaException(e));
                    }
                }
            };
            SharedChain sharedChain = new SharedChain(headFilter, container.getFilters(), dispatchFilter, 0);

            filterContext.setAttach(dispatchFilter, "chain", sharedChain);

            sharedChain.onEntry(filterContext);
        } catch (SoaException e) {
            // can't reach the headFilter
            writeErrorMessage(channelHandlerContext, transactionContext, e);
        } catch (Throwable e) {
            // can't reach the headFilter
            writeErrorMessage(channelHandlerContext, transactionContext, ExceptionUtil.convertToSoaException(e));
        }
    }

    private void processResult(ChannelHandlerContext channelHandlerContext,
                               SoaFunctionDefinition soaFunction,
                               TransactionContext transactionContext,
                               Object result,
                               FilterContext filterContext) {
        SoaHeader soaHeader = transactionContext.getHeader();
        soaHeader.setRespCode(Optional.of(SOA_NORMAL_RESP_CODE));
        soaHeader.setRespMessage(Optional.of("ok"));
        transactionContext.setHeader(soaHeader);
        try {
            filterContext.setAttribute("channelHandlerContext", channelHandlerContext);
            filterContext.setAttribute("context", transactionContext);
            filterContext.setAttribute("reqSerializer", soaFunction.reqSerializer);
            filterContext.setAttribute("respSerializer", soaFunction.respSerializer);
            filterContext.setAttribute("result", result);

        } catch (Throwable e) {
            writeErrorMessage(channelHandlerContext, transactionContext, ExceptionUtil.convertToSoaException(e));
        } finally {
            TransactionContext.Factory.removeCurrentInstance();
        }
    }

    /**
     * we can't handle this within HeadFilter as sometimes can't reach the headFilter
     *
     * @param ctx
     * @param transactionContext
     * @param e
     */
    private void writeErrorMessage(ChannelHandlerContext ctx, TransactionContext transactionContext, SoaException e) {

        SoaHeader soaHeader = transactionContext.getHeader();

        soaHeader.setRespCode(Optional.ofNullable(e.getCode()));
        soaHeader.setRespMessage(Optional.ofNullable(e.getMessage()));

        transactionContext.setSoaException(e);

        ctx.writeAndFlush(transactionContext);
    }

    /**
     * TODO
     * 获取timeout参数. 优先级如下:
     * 1. 如果invocationContext有设置的话, 那么用invocationContext的(这个值每次调用都可能不一样)
     * 2. invocationContext没有的话, 就拿Option的(命令行或者环境变量)
     * 3. 没设置Option的话, 那么取ZK的.
     * 4. ZK没有的话, 拿IDL的(暂没实现该参数)
     * 5. 都没有的话, 拿默认值.(这个值所有方法一致, 假设为50S)
     * <p>
     * 最后校验一下,拿到的值不能超过系统设置的最大值
     * <p>
     * 如果得到的数据超过最大值, 那么就用最大值.
     *
     * @param soaHeader
     * @return
     */
    private long getTimeout(SoaHeader soaHeader) {
        long timeout = 0L;
        String serviceKey = soaHeader.getServiceName() + "." + soaHeader.getVersionName() + "." + soaHeader.getMethodName() + ".producer";
        Map<ConfigKey, Object> configs = RegistryAgentProxy.getCurrentInstance(RegistryAgentProxy.Type.Server).getConfig(false, serviceKey);
        long envTimeout = SoaSystemEnvProperties.SOA_SERVICE_SERVER_TIMEOUT.longValue();
        if (null != configs) {
            Long timeoutConfig = (Long) configs.get(ConfigKey.ServerTimeout);
            timeout = (timeoutConfig != null) ? timeoutConfig.longValue() : envTimeout;
        }
        if (timeout == 0L) {
            timeout = (envTimeout == 0) ? SoaSystemEnvProperties.SOA_DEFAULT_TIMEOUT : envTimeout;
        }

        if (timeout > SoaSystemEnvProperties.SOA_MAX_TIMEOUT) {
            timeout = SoaSystemEnvProperties.SOA_MAX_TIMEOUT;
        }
        return timeout;
    }
}
