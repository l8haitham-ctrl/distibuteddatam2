package com.atlas.explorer.features.nativeingestion.dto;
import java.io.Serializable;
import java.util.List;
public record NativeDataset(String source,String schema,String table,List<NativeField> fields) implements Serializable {
    public NativeDataset {fields=List.copyOf(fields);}
}
