package com.atlas.explorer.features.connectiondiagnostics.service;
import com.atlas.explorer.features.connection.dto.ConnectionConfig;
import com.atlas.explorer.features.connectiondiagnostics.connector.DiagnosticProbe;
import com.atlas.explorer.features.connectiondiagnostics.dto.DiagnosticEvent;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.function.*;

@Service
public class ConnectionDiagnostics {
    private static final String[] STEPS={"dns","tcp","jdbc","validation"};
    private final DiagnosticProbe probe;
    public ConnectionDiagnostics(DiagnosticProbe probe){this.probe=probe;}
    public void run(ConnectionConfig config,Consumer<DiagnosticEvent> events,BooleanSupplier cancelled){
        long started=System.nanoTime(),stepStarted=started;int step=0;
        try{
            check(cancelled);events.accept(event("dns","running","Recherche de l’adresse du serveur.",0));
            var addresses=probe.resolve(config.host());events.accept(event("dns","passed","Adresse du serveur résolue.",elapsed(stepStarted)));
            check(cancelled);step=1;stepStarted=System.nanoTime();events.accept(event("tcp","running","Vérification de l’accès au port.",0));
            probe.reach(addresses,config.port());events.accept(event("tcp","passed","Port du serveur accessible.",elapsed(stepStarted)));
            check(cancelled);step=2;stepStarted=System.nanoTime();events.accept(event("jdbc","running","Connexion du pilote : TLS, authentification et ouverture de la base.",0));
            try(var connection=probe.open(config)){
                events.accept(event("jdbc","passed","Connexion à la base établie avec le pilote configuré.",elapsed(stepStarted)));
                check(cancelled);step=3;stepStarted=System.nanoTime();events.accept(event("validation","running","Validation de la session SQL.",0));
                probe.verify(connection);events.accept(event("validation","passed","Session SQL valide.",elapsed(stepStarted)));
            }
            check(cancelled);events.accept(event("complete","passed","Connexion réussie.",elapsed(started)));
        }catch(java.util.concurrent.CancellationException ignored){
            // Transport closed: no invented result is published.
        }catch(Exception failure){
            if(cancelled.getAsBoolean())return;
            String message=safeMessage(step,failure);
            events.accept(event(STEPS[step],"failed",message,elapsed(stepStarted)));
            for(int next=step+1;next<STEPS.length;next++)events.accept(event(STEPS[next],"skipped","Non exécutée après l’échec précédent.",0));
            events.accept(event("complete","failed",message,elapsed(started)));
        }
    }
    private void check(BooleanSupplier cancelled){if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();}
    private long elapsed(long start){return Math.max(0,(System.nanoTime()-start)/1_000_000L);}
    private DiagnosticEvent event(String step,String state,String message,long ms){return new DiagnosticEvent(step,state,message,ms);}
    private String safeMessage(int step,Exception failure){
        if(step==0)return "Impossible de résoudre le nom du serveur.";
        if(step==1)return "Port inaccessible : vérifier le serveur, le port et le réseau.";
        if(step==2&&failure instanceof SQLException sql&&(sql.getErrorCode()==18456||(sql.getSQLState()!=null&&sql.getSQLState().startsWith("28"))))return "Authentification refusée : vérifier les identifiants et l’accès à la base.";
        return step==2?"Connexion JDBC refusée ou indisponible : vérifier TLS, les identifiants et la base.":"La session SQL n’a pas pu être validée.";
    }
}
