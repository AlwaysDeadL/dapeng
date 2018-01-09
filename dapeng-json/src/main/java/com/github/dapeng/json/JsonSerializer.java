package com.github.dapeng.json;

import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.enums.CodecProtocol;
import com.github.dapeng.core.metadata.*;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import static com.github.dapeng.util.MetaDataUtil.*;

public class JsonSerializer implements BeanSerializer<String> {
    private final Logger logger = LoggerFactory.getLogger(JsonSerializer.class);

    private final Struct struct;
    private ByteBuf requestByteBuf;
    private final Service service;
    private final Method method;
    private final InvocationContext invocationContext = InvocationContextImpl.Factory.getCurrentInstance();

    /**
     * for encode only
     * @param service
     * @param method
     * @param struct
     * @param jsonStr
     * @throws TException
     */
    public JsonSerializer(Service service, Method method, Struct struct, String jsonStr) throws TException {
        this.struct = struct;
        this.service = service;
        this.method = method;

        if (jsonStr != null) {

            try {
                invocationContext.setCodecProtocol(CodecProtocol.Binary);
                requestByteBuf = PooledByteBufAllocator.DEFAULT.buffer(8192);
                TSoaTransport transport = new TSoaTransport(requestByteBuf);
                TBinaryProtocol bodyProtocol = new TBinaryProtocol(transport);

                new JsonParser(jsonStr, new Json2ThriftCallback(bodyProtocol)).parseJsValue();
            } catch (Exception e) {
                if (requestByteBuf != null) {
                    requestByteBuf.release();
                }
                logger.error(e.getMessage(), e);
                throw e;
            }
        }
    }

    /**
     * for decode only
     * @param service
     * @param method
     * @param struct
     */
    public JsonSerializer(Service service, Method method, Struct struct) {
        this.struct = struct;
        this.service = service;
        this.method = method;
    }

    // thrift -> json
    private void read(TProtocol iproto, JsonCallback writer) throws TException {
        iproto.readStructBegin();
        writer.onStartObject();

        while (true) {
            TField field = iproto.readFieldBegin();
            if (field.type == TType.STOP)
                break;

            List<Field> flds = struct.getFields().stream().filter(_field -> _field.tag == field.id).collect(Collectors.toList()); // TODO get fld by field.id

            Field fld = flds.isEmpty() ? null : flds.get(0);

            boolean skip = fld == null;


            if (!skip) {
                writer.onStartField(fld.name);
                readField(iproto, fld.dataType, field.type, writer, skip);
                writer.onEndField();
            }

            iproto.readFieldEnd();
        }


        iproto.readStructEnd();
        writer.onEndObject();
    }

    private void readField(TProtocol iproto, DataType fieldDataType, byte fieldType,
                           JsonCallback writer, boolean skip) throws TException {
        switch (fieldType) {
            case TType.VOID:
                break;
            case TType.BOOL:
                boolean boolValue = iproto.readBool();
                if (!skip) {
                    writer.onBoolean(boolValue);
                }
                break;
            case TType.BYTE:
                // TODO
                break;
            case TType.DOUBLE:
                double dValue = iproto.readDouble();
                if (!skip) {
                    writer.onNumber(dValue);
                }
                break;
            case TType.I16:
                short sValue = iproto.readI16();
                if (!skip) {
                    writer.onNumber(sValue);
                }
                break;
            case TType.I32:
                int iValue = iproto.readI32();
                if (!skip) {
                    if (fieldDataType != null && fieldDataType.kind == DataType.KIND.ENUM) {
                        String enumLabel = findEnumItemLabel(findEnum(fieldDataType.qualifiedName, service), iValue);
                        writer.onString(enumLabel);
                    } else {
                        writer.onNumber(iValue);
                    }
                }
                break;
            case TType.I64:
                long lValue = iproto.readI64();
                if (!skip) {
                    writer.onNumber(lValue);
                }
                break;
            case TType.STRING:
                String strValue = iproto.readString();
                if (!skip) {
                    writer.onString(strValue);
                }
                break;
            case TType.STRUCT:
                if (!skip) {
                    String subStructName = fieldDataType.qualifiedName;
                    Struct subStruct = findStruct(subStructName, service);
                    new JsonSerializer(service, method, subStruct).read(iproto, writer);
                } else {
                    TProtocolUtil.skip(iproto, TType.STRUCT);
                }
                break;
            case TType.MAP:
                if (!skip) {
                    TMap map = iproto.readMapBegin();
                    writer.onStartObject();
                    for (int index = 0; index < map.size; index++) {
                        switch (map.keyType) {
                            case TType.STRING:
                                writer.onStartField(iproto.readString());
                                break;
                            case TType.I16:
                                writer.onStartField(String.valueOf(iproto.readI16()));
                                break;
                            case TType.I32:
                                writer.onStartField(String.valueOf(iproto.readI32()));
                                break;
                            case TType.I64:
                                writer.onStartField(String.valueOf(iproto.readI64()));
                                break;
                        }

                        readField(iproto, fieldDataType.valueType, map.valueType, writer, false);
                        writer.onEndField();
                    }
                    writer.onEndObject();
                } else {
                    TProtocolUtil.skip(iproto, TType.MAP);
                }
                break;
            case TType.SET:
                if (!skip) {
                    TSet set = iproto.readSetBegin();
                    writer.onStartArray();
                    readCollection(set.size, set.elemType, fieldDataType.valueType, fieldDataType.valueType.valueType, iproto, writer);
                    writer.onEndArray();
                } else {
                    TProtocolUtil.skip(iproto, TType.SET);
                }
                break;
            case TType.LIST:
                if (!skip) {
                    TList list = iproto.readListBegin();
                    writer.onStartArray();
                    readCollection(list.size, list.elemType, fieldDataType.valueType, fieldDataType.valueType.valueType, iproto, writer);
                    writer.onEndArray();
                } else {
                    TProtocolUtil.skip(iproto, TType.LIST);
                }
                break;
            default:

        }
    }

    /**
     * @param size
     * @param elemType     thrift的数据类型
     * @param metadataType metaData的DataType
     * @param iproto
     * @param writer
     * @throws TException
     */
    private void readCollection(int size, byte elemType, DataType metadataType, DataType subMetadataType, TProtocol iproto, JsonCallback writer) throws TException {
        Struct struct = null;
        if (metadataType.kind == DataType.KIND.STRUCT) {
            struct = findStruct(metadataType.qualifiedName, service);
        }
        for (int index = 0; index < size; index++) {
            if (!isComplexKind(metadataType.kind)) {//没有嵌套结构,也就是原始数据类型, 例如int, boolean,string等
                readField(iproto, metadataType, elemType, writer, false);
            } else {
                if (struct != null) {
                    new JsonSerializer(service, method, struct).read(iproto, writer);
                } else if (isCollectionKind(metadataType.kind)) {
                    //处理List<list<>>
                    TList list = iproto.readListBegin();
                    writer.onStartArray();
                    readCollection(list.size, list.elemType, subMetadataType, subMetadataType.valueType, iproto, writer);
                    writer.onEndArray();
                } else if (metadataType.kind == DataType.KIND.MAP) {
                    readField(iproto, metadataType, elemType, writer, false);
                }
            }
            writer.onEndField();
        }

    }

    @Override
    public String read(TProtocol iproto) throws TException {

        JsonWriter writer = new JsonWriter();
        read(iproto, writer);
        return writer.toString();
    }

    /**
     * format:
     * url:http://xxx/api/callService?serviceName=xxx&version=xx&method=xx
     * post body:
     * {
     * "header":{},
     * "body":{
     * ${structName}:{}
     * }
     * }
     * <p>
     * InvocationContext and SoaHeader should be ready before
     */
    enum ParsePhase {
        INIT, HEADER_BEGIN, HEADER, HEADER_END, BODY_BEGIN, BODY, BODY_END
    }

    class Json2ThriftCallback implements JsonCallback {
        //是否已完成对body部分的解析
//        private boolean bodyParsed = false;
//        private boolean skipField = false;


        private final TProtocol oproto;
        private ParsePhase parsePhase = ParsePhase.INIT;

        class StackNode {
            final DataType dataType;
            /**
             * byteBuf position when this node created
             */
            final int byteBufPosition;

            //struct if dataType.kind==STRUCT
            final Struct struct;
            /**
             * if dataType is a Collection(such as LIST, MAP, SET etc), elCount represents the size of the Collection.
             */
            private int elCount = 0;

            StackNode(final DataType dataType, final int byteBufPosition, final Struct struct) {
                this.dataType = dataType;
                this.byteBufPosition = byteBufPosition;
                this.struct = struct;
            }

            void increaseElement() {
                elCount++;
            }
        }

        //当前处理数据节点
        StackNode current;
        String currentHeaderName;
        //onStartField的时候, 记录是否找到该Field. 如果没找到,那么需要skip这个field
        boolean foundField = true;

        /**
         * @param oproto
         */
        public Json2ThriftCallback(TProtocol oproto) {
            this.oproto = oproto;
        }


        /*  {  a:, b:, c: { ... }, d: [ { }, { } ]  }
         *
         *  init                -- [], topStruct
         *  onStartObject
         *    onStartField a    -- [topStruct], DataType a
         *    onEndField a
         *    ...
         *    onStartField c    -- [topStruct], StructC
         *      onStartObject
         *          onStartField
         *          onEndField
         *          ...
         *      onEndObject     -- [], topStruct
         *    onEndField c
         *
         *    onStartField d
         *      onStartArray    -- [topStruct] structD
         *          onStartObject
         *          onEndObject
         *      onEndArray      -- []
         *    onEndField d
         */
        Stack<StackNode> history = new Stack<>();

        @Override
        public void onStartObject() throws TException {
//            assert current.dataType.kind == DataType.KIND.STRUCT || current.dataType.kind == DataType.KIND.MAP;
            switch (parsePhase) {
                case INIT:
                    break;
                case HEADER_BEGIN:
                    parsePhase = ParsePhase.HEADER;
                    break;
                case HEADER:
                    logger.error("should not come here");
                case HEADER_END:
                    break;
                case BODY_BEGIN:
                    //初始化当前数据节点
                    DataType initDataType = new DataType();
                    initDataType.setKind(DataType.KIND.STRUCT);
                    initDataType.qualifiedName = struct.name;
                    current = new StackNode(initDataType, requestByteBuf.writerIndex(), struct);

                    oproto.writeStructBegin(new TStruct(current.struct.name));

                    parsePhase = ParsePhase.BODY;
                    break;
                case BODY:
                    if (peek() != null && isMultiElementKind(peek().dataType.kind)) {
                        peek().increaseElement();
                        //集合套集合的变态处理方式
                        current = new StackNode(peek().dataType.valueType, requestByteBuf.writerIndex(), current.struct);
                    }
                    switch (current.dataType.kind) {
                        case STRUCT:
                            Struct struct = current.struct;//findStruct(current.dataType.qualifiedName, service);
                            if (struct == null) {
                                logger.info("struct not found");
                            }
                            oproto.writeStructBegin(new TStruct(struct.name));
                            break;
                        case MAP:
                            assert isValidMapKeyType(current.dataType.keyType.kind);
                            // 压缩模式下, default size不能设置为0...
                            oproto.writeMapBegin(new TMap(dataType2Byte(current.dataType.keyType), dataType2Byte(current.dataType.valueType), 1));
                            break;
                    }
                    break;
                default:
                    logger.error("should not come here, current phase:" + parsePhase);
                    break;
            }
        }

        @Override
        public void onEndObject() throws TException {
//            assert current.dataType.kind == DataType.KIND.STRUCT || current.dataType.kind == DataType.KIND.MAP;

            switch (parsePhase) {
                case HEADER_BEGIN:
                    logger.error("should not come here");
                    break;
                case HEADER:
                    parsePhase = ParsePhase.HEADER_END;
                    break;
                case HEADER_END:
                    logger.error("should not come here");
                    break;
                case BODY_BEGIN:
                    logger.error("should not come here");
                    break;
                case BODY:
                    switch (current.dataType.kind) {
                        case STRUCT:
                            oproto.writeFieldStop();
                            oproto.writeStructEnd();
                            if (current.struct.name.equals(struct.name)) {
                                parsePhase = ParsePhase.BODY_END;
                            }
                            break;
                        case MAP:
                            oproto.writeMapEnd();

                            reWriteByteBuf();
                            break;
                    }
                    break;
            }
        }

        /**
         * 由于目前拿不到集合的元素个数, 暂时设置为0个
         *
         * @throws TException
         */
        @Override
        public void onStartArray() throws TException {
            assert isCollectionKind(current.dataType.kind);

            if (parsePhase != ParsePhase.BODY) return;

            if (peek() != null && isMultiElementKind(peek().dataType.kind)) {
                peek().increaseElement();
                //集合套集合的变态处理方式
                current = new StackNode(peek().dataType.valueType, requestByteBuf.writerIndex(), current.struct);
            }

            switch (current.dataType.kind) {
                case LIST:
                    //TODO 压缩模式下, size > 14的时候如何处理?
                    oproto.writeListBegin(new TList(dataType2Byte(current.dataType.valueType), 0));
                    break;
                case SET:
                    oproto.writeSetBegin(new TSet(dataType2Byte(current.dataType.valueType), 0));
                    break;
            }

            stackNew(new StackNode(current.dataType.valueType, requestByteBuf.writerIndex(), findStruct(current.dataType.valueType.qualifiedName, service)));
        }

        @Override
        public void onEndArray() throws TException {
            if (parsePhase != ParsePhase.BODY) return;

            pop();

            assert isCollectionKind(current.dataType.kind);

            switch (current.dataType.kind) {
                case LIST:
                    oproto.writeListEnd();
                    reWriteByteBuf();
                    break;
                case SET:
                    oproto.writeSetEnd();
                    reWriteByteBuf();
                    break;
            }
        }

        @Override
        public void onStartField(String name) throws TException {
            switch (parsePhase) {
                case INIT:
                    if (name.equals("header")) {
                        parsePhase = ParsePhase.HEADER_BEGIN;
                    } else if (name.equals("body")) {
                        parsePhase = ParsePhase.BODY_BEGIN;
                    } else {
                        logger.warn("skip field(" + name + ")@pase:" + parsePhase);
                    }
                    break;
                case HEADER:
                    currentHeaderName = name;
                    break;
                case HEADER_END:
                    if (name.equals("body")) {
                        parsePhase = ParsePhase.BODY_BEGIN;
                    } else {
                        logger.warn("skip field(" + name + ")@pase:" + parsePhase);
                    }
                    break;
                case BODY:
                    if (current.dataType.kind == DataType.KIND.MAP) {
                        stackNew(new StackNode(current.dataType.keyType, requestByteBuf.writerIndex(), null));
                        assert isValidMapKeyType(current.dataType.keyType.kind);
                        if (current.dataType.kind == DataType.KIND.STRING) {
                            oproto.writeString(name);
                        } else {
                            writeIntField(name, current.dataType.kind);
                        }
                        pop();
                        stackNew(new StackNode(current.dataType.valueType, requestByteBuf.writerIndex(), findStruct(current.dataType.valueType.qualifiedName, service)));
                    } else {
                        Field field = findField(name, current.struct);
                        if (field == null) {
                            foundField = false;
                            logger.info("field(" + name + ") not found. just skip");
                            return;
                        } else {
                            foundField = true;
                        }

                        oproto.writeFieldBegin(new TField(field.name, dataType2Byte(field.dataType), (short) field.getTag()));
                        stackNew(new StackNode(field.dataType, requestByteBuf.writerIndex(), findStruct(field.dataType.qualifiedName, service)));
                    }
                    break;
                case BODY_END:
                    logger.warn("skip field(" + name + ")@pase:" + parsePhase);
                    break;
            }

        }

        private void writeIntField(String value, DataType.KIND kind) throws TException {
            switch (kind) {
                case SHORT:
                    oproto.writeI16(Short.valueOf(value));
                    break;
                case INTEGER:
                    oproto.writeI32(Integer.valueOf(value));
                    break;
                case LONG:
                    oproto.writeI64(Long.valueOf(value));
                    break;
                default:
                    //should not come here..
            }
        }

        @Override
        public void onEndField() throws TException {
            if (parsePhase != ParsePhase.BODY) return;

            if (!foundField) return;

            pop();
            if (current.dataType.kind == DataType.KIND.MAP) {

            } else {
                oproto.writeFieldEnd();
            }
        }

        @Override
        public void onBoolean(boolean value) throws TException {
            switch (parsePhase) {
                case HEADER:
                    logger.warn("skip boolean(" + value + ")@pase:" + parsePhase);
                    break;
                case BODY:
                    if (peek() != null && isMultiElementKind(peek().dataType.kind)) peek().increaseElement();
                    oproto.writeBool(value);
                    break;
                default:
                    logger.warn("skip boolean(" + value + ")@pase:" + parsePhase);
            }

        }

        @Override
        public void onNumber(double value) throws TException {
            switch (parsePhase) {
                case HEADER:
                    fillIntToInvocationCtx((int) value);
                    break;
                case BODY:
                    DataType.KIND currentType = current.dataType.kind;

                    if (peek() != null && isMultiElementKind(peek().dataType.kind)) peek().increaseElement();

                    switch (currentType) {
                        case SHORT:
                            oproto.writeI16((short) value);
                            break;
                        case INTEGER:
                            oproto.writeI32((int) value);
                            break;
                        case LONG:
                            oproto.writeI64((long) value);
                            break;
                        case DOUBLE:
                            oproto.writeDouble(value);
                            break;
                        case BIGDECIMAL:
                            //TODO
                            break;
                        case BYTE:
                            //TODO
                            break;
                        default:
                            throw new TException("DataType(" + current.dataType.kind + ") for " + current.dataType.qualifiedName + " is not a Number");

                    }
                    break;
                default:
                    logger.warn("skip number(" + value + ")@pase:" + parsePhase);
            }
        }

        @Override
        public void onNull() throws TException {
            switch (parsePhase) {
                case HEADER:
                    break;
                case BODY:
                    //重置writerIndex
                    requestByteBuf.writerIndex(current.byteBufPosition);
                    break;
            }
        }

        @Override
        public void onString(String value) throws TException {
            switch (parsePhase) {
                case HEADER:
                    fillStringToInvocationCtx(value);
                    break;
                case BODY:
                    if (peek() != null && isMultiElementKind(peek().dataType.kind)) peek().increaseElement();

                    if (current.dataType.kind == DataType.KIND.ENUM) {
                        TEnum tEnum = findEnum(current.dataType.qualifiedName, service);
                        oproto.writeI32(findEnumItemValue(tEnum, value));
                        return;
                    }
                    oproto.writeString(value);
                    break;
                default:
                    logger.warn("skip boolean(" + value + ")@pase:" + parsePhase);
            }
        }

        private void stackNew(StackNode node) {
            history.push(this.current);
            this.current = node;
        }

        private StackNode pop() {
            return this.current = history.pop();
        }

        private StackNode peek() {
            return history.empty() ? null : history.peek();
        }

        /**
         * 根据current 节点重写集合元素长度
         */
        private void reWriteByteBuf() throws TException {
            assert isMultiElementKind(current.dataType.kind);

            //拿到当前node的开始位置以及集合元素大小
            int beginPosition = current.byteBufPosition;
            int elCount = current.elCount;

            //备份最新的writerIndex
            int currentIndex = requestByteBuf.writerIndex();

            //reWriteListBegin
            requestByteBuf.writerIndex(beginPosition);

            switch (current.dataType.kind) {
                case MAP:
                    oproto.writeMapBegin(new TMap(dataType2Byte(current.dataType.keyType), dataType2Byte(current.dataType.valueType), elCount));
                    break;
                case SET:
                    oproto.writeSetBegin(new TSet(dataType2Byte(current.dataType.valueType), elCount));
                    break;
                case LIST:
                    oproto.writeListBegin(new TList(dataType2Byte(current.dataType.valueType), elCount));
                    break;
            }

            requestByteBuf.writerIndex(currentIndex);
        }

        private void fillStringToInvocationCtx(String value) {
            if ("serviceName".equals(currentHeaderName)) {
                invocationContext.setServiceName(value);
            } else if ("methodName".equals(currentHeaderName)) {
                invocationContext.setMethodName(value);
            } else if ("versionName".equals(currentHeaderName)) {
                invocationContext.setVersionName(value);
            } else if ("calleeIp".equals(currentHeaderName)) {
                invocationContext.setCalleeIp(Optional.of(value));
            } else if ("callerFrom".equals(currentHeaderName)) {
                invocationContext.setCallerFrom(Optional.of(value));
            } else if ("callerIp".equals(currentHeaderName)) {
                invocationContext.setCallerFrom(Optional.of(value));
            } else if ("customerName".equals(currentHeaderName)) {
                invocationContext.setCustomerName(Optional.of(value));
            } else {
                logger.warn("skip field(" + currentHeaderName + ")@pase:" + parsePhase);
            }
        }

        private void fillIntToInvocationCtx(int value) {
            if ("calleePort".equals(currentHeaderName)) {
                invocationContext.setCalleePort(Optional.of(value));
            } else if ("operatorId".equals(currentHeaderName)) {
                invocationContext.setOperatorId(Optional.of(value));
            } else if ("customerId".equals(currentHeaderName)) {
                invocationContext.setCustomerId(Optional.of(value));
            } else if ("transactionSequence".equals(currentHeaderName)) {
                invocationContext.setTransactionSequence(Optional.of(value));
            } else {
                logger.warn("skip field(" + currentHeaderName + ")@pase:" + parsePhase);
            }
        }
    }


    // json -> thrift

    /**
     * {
     * header:{
     * <p>
     * },
     * boday:{
     * ${struct.name}:{
     * <p>
     * }
     * }
     * }
     *
     * @param input
     * @param oproto
     * @throws TException
     */
    @Override
    public void write(String input, TProtocol oproto) throws TException {
        try {
            oproto.writeBinary(requestByteBuf.nioBuffer());
        } finally {
            requestByteBuf.release();
        }
    }

    @Override
    public void validate(String s) throws TException {

    }

    @Override
    public String toString(String s) {
        return s;
    }

    /**
     * 暂时只支持key为整形或者字符串的map
     *
     * @param kind
     * @return
     */
    private boolean isValidMapKeyType(DataType.KIND kind) {
        return kind == DataType.KIND.INTEGER || kind == DataType.KIND.LONG
                || kind == DataType.KIND.SHORT || kind == DataType.KIND.STRING;
    }

    /**
     * 是否集合类型
     *
     * @param kind
     * @return
     */
    private boolean isCollectionKind(DataType.KIND kind) {
        return kind == DataType.KIND.LIST || kind == DataType.KIND.SET;
    }

    /**
     * 是否容器类型
     *
     * @param kind
     * @return
     */
    private boolean isMultiElementKind(DataType.KIND kind) {
        return isCollectionKind(kind) || kind == DataType.KIND.MAP;
    }

    /**
     * 是否复杂类型
     *
     * @param kind
     * @return
     */
    private boolean isComplexKind(DataType.KIND kind) {
        return isMultiElementKind(kind) || kind == DataType.KIND.STRUCT;
    }
}
