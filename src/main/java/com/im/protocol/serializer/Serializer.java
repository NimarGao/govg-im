package com.im.protocol.serializer;

import com.alibaba.fastjson.JSON;

public interface Serializer {
    
    byte JSON_SERIALIZER = 1;
    Serializer DEFAULT = new JSONSerializer();

    byte getSerializerAlgorithm();

    byte[] serialize(Object object);

    <T> T deserialize(Class<T> clazz, byte[] bytes);

    class JSONSerializer implements Serializer {
        
        @Override
        public byte getSerializerAlgorithm() {
            return JSON_SERIALIZER;
        }

        @Override
        public byte[] serialize(Object object) {
            return JSON.toJSONBytes(object);
        }

        @Override
        public <T> T deserialize(Class<T> clazz, byte[] bytes) {
            return JSON.parseObject(bytes, clazz);
        }
    }
}
