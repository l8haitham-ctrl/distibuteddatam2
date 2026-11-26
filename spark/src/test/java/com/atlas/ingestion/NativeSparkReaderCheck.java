package com.atlas.ingestion;
import java.math.BigDecimal;
public class NativeSparkReaderCheck {
    public static void main(String[] args){
        if(!new BigDecimal("18446744073709551615").equals(NativeSparkReader.convert("18446744073709551615","decimal(20,0)")))throw new AssertionError();
        if(!new BigDecimal("1234567890.12").equals(NativeSparkReader.convert("1234567890.12","decimal(12,2)")))throw new AssertionError();
        if(NativeSparkReader.convert(null,"string")!=null)throw new AssertionError();
        if(!"é العربية".equals(NativeSparkReader.convert("é العربية","string")))throw new AssertionError();
        try{NativeSparkReader.convert("1.001","decimal(12,2)");throw new AssertionError();}catch(ArithmeticException expected){}
        try{NativeSparkReader.convert("184467440737095516150","decimal(20,0)");throw new AssertionError();}catch(ArithmeticException expected){}
        if(!NativeSparkReader.convert("2025-03-01 12:00:00","timestamp").toString().contains("12:00:00"))throw new AssertionError();
        System.out.println("Native scalar conversion checks passed.");
    }
}
