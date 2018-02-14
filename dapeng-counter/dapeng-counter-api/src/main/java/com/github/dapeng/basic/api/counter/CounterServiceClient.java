package com.github.dapeng.basic.api.counter;

      import com.github.dapeng.core.*;
      import com.github.dapeng.org.apache.thrift.*;
      import java.util.ServiceLoader;
      import com.github.dapeng.basic.api.counter.CounterServiceCodec.*;
      import com.github.dapeng.basic.api.counter.service.CounterService;

      /**
       * Autogenerated by Dapeng-Code-Generator (2.0.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated

      **/
      public class CounterServiceClient implements CounterService{
      private final String serviceName;
      private final String version;

      private SoaConnectionPool pool;

      public CounterServiceClient() {
        this.serviceName = "com.github.dapeng.basic.api.counter.service.CounterService";
        this.version = "1.0.0";

        ServiceLoader<SoaConnectionPoolFactory> factories = ServiceLoader.load(SoaConnectionPoolFactory.class);
        for (SoaConnectionPoolFactory factory: factories) {
          this.pool = factory.getPool();
          break;
        }
        this.pool.registerClientInfo(serviceName,version);
      }

      
          
            /**
            * 
            **/
            
              public void submitPoint(com.github.dapeng.basic.api.counter.domain.DataPoint dataPoint) throws SoaException{

              String methodName = "submitPoint";

              submitPoint_args submitPoint_args = new submitPoint_args();
              submitPoint_args.setDataPoint(dataPoint);
                

              submitPoint_result response = pool.send(serviceName,version,"submitPoint",submitPoint_args, new SubmitPoint_argsSerializer(), new SubmitPoint_resultSerializer());

              
                  
                
            }
            
          

        
          
            /**
            * 
            **/
            
              public void submitPoints(java.util.List<com.github.dapeng.basic.api.counter.domain.DataPoint> dataPoints) throws SoaException{

              String methodName = "submitPoints";

              submitPoints_args submitPoints_args = new submitPoints_args();
              submitPoints_args.setDataPoints(dataPoints);
                

              submitPoints_result response = pool.send(serviceName,version,"submitPoints",submitPoints_args, new SubmitPoints_argsSerializer(), new SubmitPoints_resultSerializer());

              
                  
                
            }
            
          

        
          
            /**
            * 
            **/
            
              public java.util.List<com.github.dapeng.basic.api.counter.domain.DataPoint> queryPoints(com.github.dapeng.basic.api.counter.domain.DataPoint condition,String beginTimeStamp,String endTimeStamp) throws SoaException{

              String methodName = "queryPoints";

              queryPoints_args queryPoints_args = new queryPoints_args();
              queryPoints_args.setCondition(condition);
                queryPoints_args.setBeginTimeStamp(beginTimeStamp);
                queryPoints_args.setEndTimeStamp(endTimeStamp);
                

              queryPoints_result response = pool.send(serviceName,version,"queryPoints",queryPoints_args, new QueryPoints_argsSerializer(), new QueryPoints_resultSerializer());

              
                  
                      return response.getSuccess();
                    
                
            }
            
          

        

      /**
      * getServiceMetadata
      **/
      public String getServiceMetadata() throws SoaException {
        String methodName = "getServiceMetadata";
        getServiceMetadata_args getServiceMetadata_args = new getServiceMetadata_args();
        getServiceMetadata_result response = pool.send(serviceName,version,methodName,getServiceMetadata_args, new GetServiceMetadata_argsSerializer(), new GetServiceMetadata_resultSerializer());
        return response.getSuccess();
      }

    }
    