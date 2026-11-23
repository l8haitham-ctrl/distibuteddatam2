package com.atlas.explorer.features.quality.service;
import java.math.BigDecimal;
import java.util.*;
public final class QualityRules {
    private QualityRules(){}
    private static boolean same(String a,String b){return a!=null&&b!=null&&new BigDecimal(a).compareTo(new BigDecimal(b))==0;}
    public static List<Map<String,Object>> evaluate(Map<String,Map<String,String>> before,Map<String,Map<String,String>> after,Map<String,Map<String,String>> lake){
        var checks=new ArrayList<Map<String,Object>>();
        for(String table:List.of("customer","invoice","payment")){
            for(var side:Map.of("source",before,"lake",lake).entrySet()){
                var metrics=side.getValue().get(table);
                check(checks,side.getKey()+"."+table+".primary_key",metrics.get("distinct_ids"),metrics.get("row_count"));
                for(var metric:metrics.entrySet())if(metric.getKey().startsWith("invalid_")||Set.of("null_rows","duplicate_emails").contains(metric.getKey()))
                    check(checks,side.getKey()+"."+table+"."+metric.getKey(),metric.getValue(),"0");
            }
        }
        for(var query:before.entrySet())for(var metric:query.getValue().entrySet()){
            String id=query.getKey()+"."+metric.getKey();
            check(checks,"reconciliation."+id,lake.get(query.getKey()).get(metric.getKey()),metric.getValue());
            check(checks,"source_stability."+id,after.get(query.getKey()).get(metric.getKey()),metric.getValue());
        }
        for(String relation:List.of("orphan_invoices","orphan_payments","payment_dates","invoice_balances")){
            check(checks,"source."+relation,before.get(relation).get("violations"),"0");
            check(checks,"lake."+relation,lake.get(relation).get("violations"),"0");
        }
        return checks;
    }
    private static void check(List<Map<String,Object>> checks,String id,String observed,String expected){
        checks.add(Map.of("id",id,"observed",observed,"expected",expected,"status",same(observed,expected)?"passed":"failed"));
    }
}
