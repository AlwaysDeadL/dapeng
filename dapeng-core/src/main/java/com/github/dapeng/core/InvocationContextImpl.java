package com.github.dapeng.core;

import com.github.dapeng.core.enums.CodecProtocol;

import java.util.Optional;

/**
 * 客户端上下文
 *
 * @author craneding
 * @date 15/9/24
 */

public class InvocationContextImpl implements  InvocationContext {

    private String serviceName;

    private String methodName;

    private String versionName;

    private CodecProtocol codecProtocol = CodecProtocol.CompressedBinary;

    private Optional<String> calleeIp;

    private Optional<Integer> calleePort;

    private Optional<String> callerFrom = Optional.empty();

    private Optional<String> callerIp = Optional.empty();

    private Optional<Integer> operatorId = Optional.empty();

    private Optional<Integer> customerId = Optional.empty();

    private Optional<String> customerName = Optional.empty();

    private Optional<Integer> transactionSequence = Optional.empty();

    private InvocationInfo invocationInfo;

    /**
     * 全局事务id
     */
    private Optional<Integer> transactionId = Optional.empty();

    // readonly
    private int seqid;

    // read/write
    @Override
    public CodecProtocol getCodecProtocol() {
        return codecProtocol;
    }

    @Override
    public Optional<String> getCalleeIp() {
        return this.calleeIp;
    }

    @Override
    public void setCalleeIp(Optional<String> calleeIp) {
        this.calleeIp = calleeIp;
    }

    @Override
    public Optional<Integer> getCalleePort() {
        return this.calleePort;
    }

    @Override
    public void setCalleePort(Optional<Integer> calleePort) {
        this.calleePort = calleePort;
    }

    @Override
    public InvocationInfo getLastInfo() {
        return this.invocationInfo;
    }

    @Override
    public void setLastInfo(InvocationInfo invocationInfo) {
        this.invocationInfo = invocationInfo;
    }

    @Override
    public Optional<Integer> getTransactionId() {
        return transactionId;
    }

    @Override
    public void setTransactionId(Optional<Integer> transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public void setCustomerId(Optional<Integer> customerId) {

    }

    @Override
    public Optional<Integer> getCustomerId() {
        return this.customerId;
    }

    @Override
    public void setCustomerName(Optional<String> customerName) {
        this.customerName = customerName;
    }

    @Override
    public Optional<String> getCustomerName() {
        return this.customerName;
    }

    @Override
    public void setOperatorId(Optional<Integer> operatorId) {
        this.operatorId = operatorId;
    }

    @Override
    public Optional<Integer> getOperatorId() {
        return this.operatorId;
    }

    @Override
    public void setCallerFrom(Optional<String> callerFrom) {
        this.callerFrom = callerFrom;
    }

    @Override
    public Optional<String> getCallerFrom() {
        return this.callerFrom;
    }

    @Override
    public void setCallerIp(Optional<String> callerIp) {
        this.callerIp = callerIp;
    }

    @Override
    public Optional<String> getCallerIp() {
        return this.callerIp;
    }

    @Override
    public void setTransactionSequence(Optional<Integer> transactionSequence) {
        this.transactionSequence = transactionSequence;
    }

    @Override
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getServiceName() {
        return this.serviceName;
    }

    @Override
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public String getMethodName() {
        return this.methodName;
    }

    @Override
    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    @Override
    public String getVersionName() {
        return this.versionName;
    }

    @Override
    public void setCodecProtocol(CodecProtocol codecProtocol) {
        this.codecProtocol = codecProtocol;
    }

    public int getSeqid() {
        return seqid;
    }

    public void setSeqid(int seqid) {
        this.seqid = seqid;
    }

    public static class Factory {
        private static ThreadLocal<InvocationContext> threadLocal = new ThreadLocal<>();

        private static InvocationContext createNewInstance() {
            InvocationContext context = new InvocationContextImpl();
            threadLocal.set(context);
            return context;
        }

        public static InvocationContext getCurrentInstance() {
            InvocationContext context = threadLocal.get();

            if (context == null) {
                context = createNewInstance();

                threadLocal.set(context);
            }

            return context;
        }

        public static void removeCurrentInstance() {
            threadLocal.remove();
        }
    }


}
