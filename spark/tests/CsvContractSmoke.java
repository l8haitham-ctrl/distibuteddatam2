import com.atlas.ingestion.IngestionJob;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import java.nio.file.*;
import java.util.List;
import static org.apache.spark.sql.functions.*;

/** Real CSV contract check, executed with the same Java/Spark image as ingestion. */
public class CsvContractSmoke {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        var values=IngestionJob.class.getDeclaredMethod("csvValues",Dataset.class);values.setAccessible(true);
        var options=IngestionJob.class.getDeclaredMethod("csvOptions",DataFrameWriter.class);options.setAccessible(true);
        try(var spark=SparkSession.builder().appName("csv-contract").config("spark.sql.session.timeZone","UTC").getOrCreate()) {
            spark.sparkContext().setLogLevel("OFF");
            var schema=new StructType().add("text",DataTypes.StringType).add("amount",DataTypes.createDecimalType(20,9))
                    .add("missing",DataTypes.StringType).add("date",DataTypes.DateType).add("timestamp",DataTypes.TimestampType);
            var source=spark.createDataFrame(List.of(RowFactory.create("  spaced \"value\"  ",new java.math.BigDecimal("0.000000100"),null,
                    java.sql.Date.valueOf("2025-03-01"),java.sql.Timestamp.valueOf("2025-03-01 12:13:14"))),schema);
            var root=Files.createTempDirectory("csv-contract-");
            var output=(Dataset<Row>)values.invoke(null,source);
            var writer=(DataFrameWriter<Row>)options.invoke(null,output.write());
            writer.csv(root.resolve("valid").toString());
            try(var paths=Files.list(root.resolve("valid"))) {
                var file=paths.filter(p->p.toString().endsWith(".csv")).findFirst().orElseThrow();
                var lines=Files.readAllLines(file);
                String expected="  spaced \"value\"  ,0.000000100,\\N,2025-03-01,2025-03-01 12:13:14";
                if(lines.size()!=1 || !lines.getFirst().equals(expected))throw new AssertionError("CSV contract mismatch: "+lines);
            }
            int index=0;
            for(String unsafe:List.of("comma,value","line\nbreak","\\N")) {
                boolean rejected=false;
                try {
                    var invalid=(Dataset<Row>)values.invoke(null,source.withColumn("text",lit(unsafe)));
                    var invalidWriter=(DataFrameWriter<Row>)options.invoke(null,invalid.write());
                    invalidWriter.csv(root.resolve("invalid-"+(index++)).toString());
                } catch(Exception exception) {
                    for(Throwable cause=exception;cause!=null;cause=cause.getCause())
                        if(cause.getMessage()!=null && cause.getMessage().contains("CSV TEXTFILE cannot represent"))rejected=true;
                }
                if(!rejected)throw new AssertionError("Unsafe CSV string was not rejected.");
            }
            System.out.println("CSV_CONTRACT_PASSED: whitespace, quotes, plain decimals, nulls, dates, timestamps and unsafe strings");
        }
    }
}
