 package com.isuwang.soa.settle.scala.domain.serializer;

        import com.isuwang.soa.user.scala.domain.serializer._;import com.isuwang.soa.price.scala.domain.serializer._;import com.isuwang.soa.order.scala.domain.serializer._;import com.github.dapeng.soa.scala.domain.serializer._;import com.isuwang.soa.settle.scala.domain.serializer._;
        import com.github.dapeng.core._
        import com.github.dapeng.org.apache.thrift._
        import com.github.dapeng.org.apache.thrift.protocol._

        /**
        * Autogenerated by Dapeng-Code-Generator (2.0.0)
        *
        * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
        *  @generated
        **/

        class SettleSerializer extends BeanSerializer[com.isuwang.soa.settle.scala.domain.Settle]{
          
      @throws[TException]
      override def read(iprot: TProtocol): com.isuwang.soa.settle.scala.domain.Settle = {

        var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
        iprot.readStructBegin()

      var id: Int = 0
        var orderId: Int = 0
        var cash_debit: Double = 0.00
        var cash_credit: Double = 0.00
        var remark: Option[String] = None
        

      while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {

        schemeField = iprot.readFieldBegin

        schemeField.id match {
          
              case 1 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.I32 => id = iprot.readI32
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
            }
            
              case 2 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.I32 => orderId = iprot.readI32
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
            }
            
              case 3 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.DOUBLE => cash_debit = iprot.readDouble
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
            }
            
              case 4 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.DOUBLE => cash_credit = iprot.readDouble
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
            }
            
              case 5 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.STRING => remark = Option(iprot.readString)
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
            }
            
          case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
        }
      }

      iprot.readFieldEnd
      iprot.readStructEnd

      val bean = com.isuwang.soa.settle.scala.domain.Settle(id = id,orderId = orderId,cash_debit = cash_debit,cash_credit = cash_credit,remark = remark)
      validate(bean)

      bean
      }
    
      @throws[TException]
      override def write(bean: com.isuwang.soa.settle.scala.domain.Settle, oprot: TProtocol): Unit = {

      validate(bean)
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("Settle"))

      
            {
            val elem0 = bean.id 
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("id", com.github.dapeng.org.apache.thrift.protocol.TType.I32, 1.asInstanceOf[Short]))
            oprot.writeI32(elem0)
            oprot.writeFieldEnd
            
            }
            {
            val elem1 = bean.orderId 
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("orderId", com.github.dapeng.org.apache.thrift.protocol.TType.I32, 2.asInstanceOf[Short]))
            oprot.writeI32(elem1)
            oprot.writeFieldEnd
            
            }
            {
            val elem2 = bean.cash_debit 
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("cash_debit", com.github.dapeng.org.apache.thrift.protocol.TType.DOUBLE, 3.asInstanceOf[Short]))
            oprot.writeDouble(elem2)
            oprot.writeFieldEnd
            
            }
            {
            val elem3 = bean.cash_credit 
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("cash_credit", com.github.dapeng.org.apache.thrift.protocol.TType.DOUBLE, 4.asInstanceOf[Short]))
            oprot.writeDouble(elem3)
            oprot.writeFieldEnd
            
            }
            if(bean.remark.isDefined){
            val elem4 = bean.remark .get
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("remark", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, 5.asInstanceOf[Short]))
            oprot.writeString(elem4)
            oprot.writeFieldEnd
            
            }
      oprot.writeFieldStop
      oprot.writeStructEnd
    }
    
      @throws[TException]
      override def validate(bean: com.isuwang.soa.settle.scala.domain.Settle): Unit = {
      
    }
    

          @throws[TException]
          override def toString(bean: com.isuwang.soa.settle.scala.domain.Settle): String = if (bean == null) "null" else bean.toString

        }
        
      