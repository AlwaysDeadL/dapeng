/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.message.config;

/**
 * 描述:
 *
 * @author maple.lei
 * @date 2018年02月13日 上午11:52
 */
public class MessageInfo<T> {
    private String eventType;
    private T event;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public T getEvent() {
        return event;
    }

    public void setEvent(T event) {
        this.event = event;
    }

    public MessageInfo(String eventType, T event) {
        this.eventType = eventType;
        this.event = event;
    }

    @Override
    public String toString() {
        return "MessageInfo{" +
                "eventType='" + eventType + '\'' +
                ", event=" + event +
                '}';
    }
}
