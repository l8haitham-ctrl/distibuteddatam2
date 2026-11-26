package com.atlas.explorer.features.nativeingestion.dto;
import java.io.Serializable;
public record NativeField(String name,String type,boolean nullable) implements Serializable { }
