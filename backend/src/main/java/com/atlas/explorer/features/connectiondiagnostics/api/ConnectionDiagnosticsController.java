package com.atlas.explorer.features.connectiondiagnostics.api;
import com.atlas.explorer.features.connection.service.SourceService;
import com.atlas.explorer.features.connectiondiagnostics.service.ConnectionDiagnostics;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class ConnectionDiagnosticsController {
    private final SourceService sources;private final ConnectionDiagnostics diagnostics;
    public ConnectionDiagnosticsController(SourceService sources,ConnectionDiagnostics diagnostics){this.sources=sources;this.diagnostics=diagnostics;}
    @PostMapping(value="/sources/{id}/test/events",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> test(@PathVariable String id){
        var config=sources.get(id);var emitter=new SseEmitter(90_000L);var cancelled=new AtomicBoolean();
        emitter.onCompletion(()->cancelled.set(true));emitter.onTimeout(()->cancelled.set(true));emitter.onError(error->cancelled.set(true));
        Thread.startVirtualThread(()->{
            try{
                diagnostics.run(config,event->{try{emitter.send(SseEmitter.event().name("progress").data(event));}catch(java.io.IOException|IllegalStateException error){cancelled.set(true);throw new java.util.concurrent.CancellationException();}},cancelled::get);
                if(!cancelled.get())emitter.complete();
            }catch(Exception ignored){if(!cancelled.get())emitter.completeWithError(new IllegalStateException("Diagnostic interrompu."));}
        });
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Accel-Buffering","no").body(emitter);
    }
}
