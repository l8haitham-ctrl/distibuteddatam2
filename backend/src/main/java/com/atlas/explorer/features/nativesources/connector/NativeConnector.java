package com.atlas.explorer.features.nativesources.connector;

import com.atlas.explorer.features.nativesources.dto.NativeConnection;
import java.util.Map;

public interface NativeConnector {
    String engine();
    String displayName();
    int defaultPort();
    Map<String,Object> test(NativeConnection config);
    Map<String,Object> explore(NativeConnection config);
}
