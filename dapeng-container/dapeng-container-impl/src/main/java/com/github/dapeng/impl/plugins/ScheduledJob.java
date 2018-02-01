package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.ContainerFactory;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by tangliu on 2016/8/17.
 */
public class ScheduledJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(TaskSchedulePlugin.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap data = context.getJobDetail().getJobDataMap();
        String serviceName = data.getString("serviceName");
        String versionName = data.getString("versionName");

//        if (!MasterHelper.isMaster(serviceName, versionName)) {
//            logger.info("--定时任务({}:{})不是Master，跳过--", serviceName, versionName);
//            return;
//        }

        logger.info("定时任务({})开始执行", context.getJobDetail().getKey().getName());
        Map<ProcessorKey, SoaServiceDefinition<?>> processorMap = ContainerFactory.getContainer().getServiceProcessors();
        SoaServiceDefinition soaServiceDefinition = processorMap.get(new ProcessorKey(serviceName, versionName));

//        SoaProcessFunction<Object, Object, Object, ? extends TCommonBeanSerializer<Object>, ? extends TCommonBeanSerializer<Object>> soaProcessFunction =
//                (SoaProcessFunction<Object, Object, Object, ? extends TCommonBeanSerializer<Object>, ? extends TCommonBeanSerializer<Object>>) data.get("function");

        Object iface = data.get("iface");

        try {
            if (soaServiceDefinition.isAsync) {
                SoaFunctionDefinition.Async<Object,Object, Object> functionDefinition = (SoaFunctionDefinition.Async<Object,Object, Object>) data.get("function");
                functionDefinition.apply(iface,new Object());
            } else {
                SoaFunctionDefinition.Sync<Object,Object, Object> functionDefinition = (SoaFunctionDefinition.Sync<Object,Object, Object>) data.get("function");
                functionDefinition.apply(iface,null);
            }
            logger.info("定时任务({})执行完成", context.getJobDetail().getKey().getName());
        } catch (Exception e) {
            logger.error("定时任务({})执行异常", context.getJobDetail().getKey().getName());
            logger.error(e.getMessage(), e);
        }

    }
}
