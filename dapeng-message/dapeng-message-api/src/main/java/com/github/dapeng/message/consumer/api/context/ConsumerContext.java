package com.github.dapeng.message.consumer.api.context;

import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.definition.SoaFunctionDefinition;


/**
 * Created by tangliu on 2016/8/4.
 */
public class ConsumerContext {

    public Object iface;

    public SoaFunctionDefinition<Object, Object, Object> soaFunctionDefinition;

    public Object getIface() {
        return iface;
    }

    public void setIface(Object iface) {
        this.iface = iface;
    }

    public SoaFunctionDefinition<Object, Object, Object> getSoaFunctionDefinition() {
        return soaFunctionDefinition;
    }

    public void setSoaFunctionDefinition(SoaFunctionDefinition<Object, Object, Object> soaFunctionDefinition) {
        this.soaFunctionDefinition = soaFunctionDefinition;
    }

    Object action;
    Object consumer;

    public Object getAction() {
        return action;
    }

    public void setAction(Object action) {
        this.action = action;
    }

    public Object getConsumer() {
        return consumer;
    }

    public void setConsumer(Object consumer) {
        this.consumer = consumer;
    }

    String groupId;

    String topic;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
