package com.atlas.explorer.features.exploration.connector;
import com.atlas.explorer.features.connection.connector.JdbcConnections;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import com.atlas.explorer.features.connectors.connector.RelationalPlugin;
import com.atlas.explorer.shared.domain.ExplorerException;
import java.sql.*;
import java.util.*;
public final class JdbcSchemaConnector implements SchemaConnector {
    private final RelationalPlugin plugin;
    public JdbcSchemaConnector(RelationalPlugin plugin){this.plugin=plugin;}
    public String engine(){return plugin.engine();}
    @FunctionalInterface private interface Read<T>{T run(Connection c)throws SQLException;}
    private <T>T read(ConnectionConfig config,Read<T> op){try(var c=plugin.open(JdbcConnections.settings(config))){return op.run(c);}catch(SQLException e){throw JdbcConnections.sanitized(e);}}
    public List<Map<String,Object>> tables(ConnectionConfig config){return read(config,this::tables);}
    public Map<String,Object> describe(ConnectionConfig config,String schema,String table){return read(config,c->describe(c,schema,table));}
    public List<Map<String,Object>> explore(ConnectionConfig config){return read(config,c->{var result=new ArrayList<Map<String,Object>>();for(var table:tables(c))result.add(describe(c,table.get("schema_name").toString(),table.get("table_name").toString()));return result;});}
    private List<Map<String,Object>> tables(Connection c)throws SQLException{
        var result=new ArrayList<Map<String,Object>>();
        try(var rs=c.getMetaData().getTables(c.getCatalog(),null,"%",new String[]{"TABLE"})){
            while(rs.next()){String schema=rs.getString("TABLE_SCHEM");if(schema==null)schema=rs.getString("TABLE_CAT");
                if(plugin.userSchema(schema))result.add(Map.of("schema_name",schema,"table_name",rs.getString("TABLE_NAME")));}
        }
        result.sort(Comparator.comparing((Map<String,Object> r)->r.get("schema_name").toString()).thenComparing(r->r.get("table_name").toString()));return result;
    }
    private Map<String,Object> describe(Connection c,String schema,String table)throws SQLException{
        var meta=c.getMetaData();String catalog=plugin.metadataCatalog(c,schema),nativeSchema=plugin.metadataSchema(schema);
        String schemaPattern=pattern(meta,nativeSchema),tablePattern=pattern(meta,table);
        try(var rs=meta.getTables(catalog,schemaPattern,tablePattern,new String[]{"TABLE"})){if(!plugin.userSchema(schema)||!rs.next())throw new ExplorerException(ExplorerException.Code.TABLE_NOT_FOUND);}
        var columns=new ArrayList<Map<String,Object>>();
        try(var rs=meta.getColumns(catalog,schemaPattern,tablePattern,null)){
            while(rs.next()){
                int type=rs.getInt("DATA_TYPE"),size=rs.getInt("COLUMN_SIZE");var col=new LinkedHashMap<String,Object>();
                col.put("name",rs.getString("COLUMN_NAME"));col.put("data_type",rs.getString("TYPE_NAME").toLowerCase(Locale.ROOT));
                col.put("position",rs.getInt("ORDINAL_POSITION"));col.put("max_length_bytes",rs.getInt("CHAR_OCTET_LENGTH"));
                col.put("precision",size);col.put("scale",rs.getInt("DECIMAL_DIGITS"));col.put("nullable",rs.getInt("NULLABLE")!=DatabaseMetaData.columnNoNulls);
                col.put("is_identity","YES".equals(rs.getString("IS_AUTOINCREMENT")));col.put("default_value",rs.getString("COLUMN_DEF"));
                col.put("jdbc_type",type);col.put("character_length",size);
                col.put("temporal",Set.of(Types.DATE,Types.TIMESTAMP,Types.TIMESTAMP_WITH_TIMEZONE).contains(type));
                col.put("partitionable",Set.of(Types.CHAR,Types.VARCHAR,Types.LONGVARCHAR,Types.NCHAR,Types.NVARCHAR,Types.LONGNVARCHAR,Types.TINYINT,Types.SMALLINT,Types.INTEGER,Types.BIGINT,Types.NUMERIC,Types.DECIMAL,Types.REAL,Types.FLOAT,Types.DOUBLE,Types.BOOLEAN,Types.BIT,Types.DATE,Types.TIMESTAMP,Types.TIMESTAMP_WITH_TIMEZONE).contains(type));
                columns.add(col);
            }
        }
        columns.sort(Comparator.comparingInt(r->((Number)r.get("position")).intValue()));
        var primary=new ArrayList<Map<String,Object>>();
        try(var rs=meta.getPrimaryKeys(catalog,nativeSchema,table)){while(rs.next())primary.add(Map.of("constraint_name",Objects.toString(rs.getString("PK_NAME"),"primary_key"),"column_name",rs.getString("COLUMN_NAME"),"position",rs.getInt("KEY_SEQ")));}
        primary.sort(Comparator.comparingInt(r->((Number)r.get("position")).intValue()));
        var foreign=new ArrayList<Map<String,Object>>();
        try(var rs=meta.getImportedKeys(catalog,nativeSchema,table)){while(rs.next())foreign.add(Map.of("constraint_name",Objects.toString(rs.getString("FK_NAME"),"foreign_key"),"column_name",rs.getString("FKCOLUMN_NAME"),"position",rs.getInt("KEY_SEQ"),"referenced_schema",Objects.toString(rs.getString("PKTABLE_SCHEM"),Objects.toString(rs.getString("PKTABLE_CAT"),schema)),"referenced_table",rs.getString("PKTABLE_NAME"),"referenced_column",rs.getString("PKCOLUMN_NAME")));}
        foreign.sort(Comparator.comparing((Map<String,Object> r)->r.get("constraint_name").toString()).thenComparingInt(r->((Number)r.get("position")).intValue()));
        return Map.of("schema_name",schema,"table_name",table,"columns",columns,"primary_key",primary,"foreign_keys",foreign);
    }
    private String pattern(DatabaseMetaData meta,String value)throws SQLException{if(value==null)return null;String e=meta.getSearchStringEscape();return value.replace(e,e+e).replace("%",e+"%").replace("_",e+"_");}
}
