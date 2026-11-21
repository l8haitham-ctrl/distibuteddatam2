package com.atlas.explorer.features.catalog.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.regex.Pattern;
/** Builds SQL only from a validated manifest, never from user-provided SQL fragments. */
public class TableDefinition {
    public final String table,view,location,columns,properties,projection,runSchema;
    public final boolean parquet,partitioned;
    public TableDefinition(UUID id,Map<String,Object> manifest) {this(id,manifest,"");}
    public TableDefinition(UUID id,Map<String,Object> manifest,String suffix) {
        try {
            runSchema=identifier("runs"+suffix);
            table="hive."+runSchema+"."+identifier(manifest.get("table_name").toString())+"_"+id.toString().substring(0,8);
            view="hive."+identifier(manifest.get("source_id")+"_"+manifest.get("schema_name")+suffix)+"."+identifier(manifest.get("table_name").toString());
            String bucket=manifest.get("bucket").toString(),prefix=manifest.get("prefix").toString();
            if(!bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]") || !prefix.matches("[A-Za-z0-9_/-]+"))throw new IllegalArgumentException();
            location="s3://"+bucket+"/"+prefix+"/data/";
            parquet=manifest.get("format").equals("parquet");
            if(!parquet && !manifest.get("format").equals("csv"))throw new IllegalArgumentException();
            var fields=new ObjectMapper().readTree(manifest.get("schema_json").toString()).get("fields");
            @SuppressWarnings("unchecked") var parts=(List<Map<String,Object>>)manifest.getOrDefault("partition_columns",List.of());
            var partNames=new LinkedHashSet<String>();var derived=new HashSet<String>();
            for(var part:parts){String name=identifier(part.get("name").toString());partNames.add(name);if(Boolean.TRUE.equals(part.get("derived")))derived.add(name);}
            var dataColumns=new LinkedHashMap<String,String>();var seen=new HashSet<String>();var source=new ArrayList<String>();
            for(var field:fields){String name=identifier(field.get("name").asText());if(!seen.add(name))throw new IllegalArgumentException();
                dataColumns.put(name,sqlType(field.get("type").asText()));if(!derived.contains(name))source.add(quote(name));}
            if(!dataColumns.keySet().containsAll(partNames)||source.isEmpty())throw new IllegalArgumentException();
            var order=new ArrayList<>(dataColumns.keySet());order.removeAll(partNames);order.addAll(partNames);
            columns=String.join(",",order.stream().map(name->quote(name)+" "+dataColumns.get(name)).toList());
            projection=String.join(",",source);partitioned=!partNames.isEmpty();
            properties="external_location="+literal(location)+",format='"+(parquet?"PARQUET":"TEXTFILE")+"'"+
                (parquet?"":",textfile_field_separator=',',null_format='\\N'")+
                (partitioned?",partitioned_by=ARRAY["+String.join(",",partNames.stream().map(TableDefinition::literal).toList())+"]":"");
        } catch(Exception e){throw new CatalogException();}
    }
    public static String identifier(String value){if(!value.matches("[A-Za-z_][A-Za-z0-9_]{0,255}"))throw new IllegalArgumentException();return value.toLowerCase(Locale.ROOT);}
    public static String quote(String value){return "\""+identifier(value)+"\"";}
    public static String literal(String value){return "'"+value.replace("'","''")+"'";}
    public boolean matchesLocation(String ddl) {
        String normalized=ddl.replace("s3a://","s3://");
        return normalized.contains(literal(location)) || normalized.contains(literal(location.replaceFirst("/$","")));
    }
    public static String sqlType(String type){
        if(Pattern.matches("decimal\\(([1-9]|[12][0-9]|3[0-8]),[0-9]{1,2}\\)",type))return type.toUpperCase(Locale.ROOT);
        return switch(type){case "string"->"VARCHAR";case "integer"->"INTEGER";case "long"->"BIGINT";case "short"->"SMALLINT";case "byte"->"TINYINT";
            case "date"->"DATE";case "timestamp","timestamp_ntz"->"TIMESTAMP(3)";case "boolean"->"BOOLEAN";case "float"->"REAL";case "double"->"DOUBLE";case "binary"->"VARBINARY";default->throw new CatalogException();};
    }
}
