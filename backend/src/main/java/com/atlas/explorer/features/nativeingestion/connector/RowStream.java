package com.atlas.explorer.features.nativeingestion.connector;
import java.util.Iterator;
import java.util.List;
public interface RowStream extends Iterator<List<String>>,AutoCloseable { void close(); }
