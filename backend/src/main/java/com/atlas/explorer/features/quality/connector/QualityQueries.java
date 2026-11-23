package com.atlas.explorer.features.quality.connector;
import java.util.*;
import java.util.function.Function;
/** Business profile for the project's Customer/Invoice/Payment model, not arbitrary schemas. */
public final class QualityQueries {
    private QualityQueries(){}
    public static Map<String,String> build(Function<String,String> table){
        var queries=new LinkedHashMap<String,String>();
        String customer=table.apply("Customer"),invoice=table.apply("Invoice"),payment=table.apply("Payment");
        queries.put("customer",profile(customer,"customer_id",List.of("customer_id","first_name","last_name","email","city","created_at","updated_at"),
            ",COUNT(email)-COUNT(DISTINCT email) AS duplicate_emails,"+bad("TRIM(email)='' OR email NOT LIKE '%@%'","invalid_emails")+","+bad("updated_at<created_at","invalid_timestamps")));
        queries.put("invoice",profile(invoice,"invoice_id",List.of("invoice_id","customer_id","invoice_date","amount","status","updated_at"),
            ",COALESCE(SUM(amount),0) AS amount_sum,"+bad("amount<=0","invalid_amounts")+","+bad("status NOT IN ('paid','partial','unpaid')","invalid_statuses")+","+bad("updated_at<invoice_date","invalid_timestamps")));
        queries.put("payment",profile(payment,"payment_id",List.of("payment_id","invoice_id","payment_date","amount","payment_method","updated_at"),
            ",COALESCE(SUM(amount),0) AS amount_sum,"+bad("amount<=0","invalid_amounts")+","+bad("payment_method NOT IN ('card','transfer','cash')","invalid_methods")+","+bad("updated_at<payment_date","invalid_timestamps")));
        queries.put("orphan_invoices","SELECT COUNT(*) AS violations FROM "+invoice+" i LEFT JOIN "+customer+" c ON i.customer_id=c.customer_id WHERE c.customer_id IS NULL");
        queries.put("orphan_payments","SELECT COUNT(*) AS violations FROM "+payment+" p LEFT JOIN "+invoice+" i ON p.invoice_id=i.invoice_id WHERE i.invoice_id IS NULL");
        queries.put("payment_dates","SELECT COUNT(*) AS violations FROM "+payment+" p JOIN "+invoice+" i ON p.invoice_id=i.invoice_id WHERE p.payment_date<i.invoice_date");
        queries.put("invoice_balances","SELECT COUNT(*) AS violations FROM "+invoice+" i LEFT JOIN (SELECT invoice_id,SUM(amount) AS paid FROM "+payment+" GROUP BY invoice_id) p ON p.invoice_id=i.invoice_id WHERE (i.status='paid' AND COALESCE(p.paid,0)<>i.amount) OR (i.status='partial' AND (COALESCE(p.paid,0)<=0 OR p.paid>=i.amount)) OR (i.status='unpaid' AND COALESCE(p.paid,0)<>0)");
        return queries;
    }
    private static String bad(String condition,String alias){return "COALESCE(SUM(CASE WHEN "+condition+" THEN 1 ELSE 0 END),0) AS "+alias;}
    private static String profile(String table,String id,List<String> required,String extra){
        return "SELECT COUNT(*) AS row_count,COUNT(DISTINCT "+id+") AS distinct_ids,COALESCE(SUM(CAST("+id+" AS BIGINT)),0) AS id_sum,"
            +bad(String.join(" OR ",required.stream().map(c->c+" IS NULL").toList()),"null_rows")+extra+" FROM "+table;
    }
}
