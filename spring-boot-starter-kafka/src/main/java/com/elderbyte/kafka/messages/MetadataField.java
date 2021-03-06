package com.elderbyte.kafka.messages;

import com.elderbyte.commons.exceptions.ArgumentNullException;
import com.elderbyte.messaging.annotations.MessageHeader;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

class MetadataField {

    public static MetadataField from(Field field, @Nullable MessageHeader messageHeaderAttr){

        String metadataKey = field.getName();
        var writeToMetadata = true;
        var populate = true;

        if(messageHeaderAttr != null){

            String key = null;

            if(StringUtils.hasText(messageHeaderAttr.key())) {
                key = messageHeaderAttr.key();
            }else{
                key = messageHeaderAttr.value();
            }

            if(StringUtils.hasText(key)){
                metadataKey = key;
                writeToMetadata = messageHeaderAttr.write();
                populate = messageHeaderAttr.read();
            }
        }

        return new MetadataField(field, metadataKey, writeToMetadata, populate);
    }

    private final Field field;
    private final String metadataKey;
    private final boolean writeToMetadata;
    private final boolean populate;

    public MetadataField(Field field, String metadataKey, boolean writeToMetadata, boolean populate) {

        if(field == null) throw new ArgumentNullException("field");
        if(!StringUtils.hasText(metadataKey)) throw new IllegalArgumentException("metadataKey must have text but was: '"+metadataKey+"'");

        this.field = field;
        this.metadataKey = metadataKey;
        this.writeToMetadata = writeToMetadata;
        this.populate = populate;
    }

    public Field getField() {
        return field;
    }

    public String getMetadataKey() {
        return metadataKey;
    }

    public boolean isWriteToMetadata() {
        return writeToMetadata;
    }

    public boolean isPopulate() {
        return populate;
    }
}
