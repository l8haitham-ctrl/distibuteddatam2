import { AfterViewInit, Component, ElementRef, input, output, viewChild } from '@angular/core';
import { SourceEntry } from '../../core/sources.store';
import { DiagnosticStep, SourceTestRun } from './source-test.api';
import { IconComponent } from '../../shared/icon.component';
@Component({selector:'app-source-test-dialog',imports:[IconComponent],template:`
  <dialog #dialog class="source-test-dialog" aria-labelledby="source-test-title" (cancel)="$event.preventDefault();closed.emit()" (click)="$event.target===dialog&&closed.emit()">
    <header><div><span class="test-eyebrow">DIAGNOSTIC DE CONNEXION</span><h2 id="source-test-title">Tester {{source().name}}</h2></div><button class="icon-button" aria-label="Fermer le suivi" (click)="closed.emit()"><app-icon name="X" [size]="20"/></button></header>
    <p class="test-target">{{source().host}}:{{source().port}} · {{source().database}}</p>
    @if(run();as r){<p class="test-result" role="status"><app-icon [name]="r.state==='running'?'LoaderCircle':r.state==='passed'?'Check':'CircleAlert'" [class.spin]="r.state==='running'"/>{{r.message}}@if(r.duration_ms!==null){<span>{{r.duration_ms}} ms</span>}</p>
      <ol aria-label="Étapes du test">@for(step of steps;track step.id){<li [class.failed]="r.steps[step.id].state==='failed'"><span class="test-step-icon"><app-icon [name]="r.steps[step.id].state==='passed'?'Check':r.steps[step.id].state==='running'?'LoaderCircle':r.steps[step.id].state==='failed'?'X':'Minus'" [class.spin]="r.steps[step.id].state==='running'"/></span><div><strong>{{step.label}}</strong><p>{{r.steps[step.id].message||step.note}}</p></div><small>{{label(r.steps[step.id],r)}}@if(r.steps[step.id].state==='passed'||r.steps[step.id].state==='failed'){<span>{{r.steps[step.id].duration_ms}} ms</span>}</small></li>}</ol>
      <p class="test-note">TLS, authentification et ouverture de la base sont vérifiés ensemble par le pilote JDBC. Les étapes affichées proviennent du serveur.</p>
      @if(r.state==='running'){<p class="test-note">Vous pouvez fermer cette fenêtre ; le test continue et mettra à jour la liste.</p>}
      <footer><button class="wf-button" (click)="closed.emit()">Fermer</button><button class="wf-button wf-primary" [disabled]="r.state==='running'" (click)="retry.emit()">Relancer le test</button></footer>
    }
  </dialog>`,styles:`
  .source-test-dialog{position:fixed;width:min(620px,calc(100vw - 32px));max-height:calc(100dvh - 40px);overflow:auto;border:1px solid #a1a1aa;border-radius:5px;background:#fff;color:var(--dark);padding:26px;box-shadow:0 20px 80px #0002}.source-test-dialog::backdrop{background:#18181b55}header{display:flex;align-items:start;justify-content:space-between;gap:14px}.test-eyebrow{font-size:10px;letter-spacing:1px;color:var(--muted)}h2{font-size:23px;margin-top:10px;overflow-wrap:anywhere}.test-target{font-size:12px;color:var(--muted);margin:10px 0 20px;overflow-wrap:anywhere}.test-result{display:flex;gap:10px;align-items:center;flex-wrap:wrap;background:#f4f4f5;padding:14px;border:1px solid var(--line);font-size:13px;line-height:1.6}.test-result>span{margin-left:auto;font-size:12px}ol{list-style:none;padding:0;margin:16px 0}li{display:flex;align-items:start;gap:12px;padding:17px 0;border-bottom:1px solid var(--line)}li>div{flex:1;min-width:0}li strong{font-size:13px}li p{font-size:12px;line-height:1.6;color:var(--muted);margin:5px 0 0}.test-step-icon{border:1px solid var(--line);padding:6px;border-radius:50%}li small{text-align:right;font-size:11px;white-space:nowrap}li small span{display:block;color:var(--muted);margin-top:6px}.failed{color:#9b332d}.test-note{font-size:11px;line-height:1.7;color:var(--muted);margin:12px 0}footer{display:flex;gap:10px;justify-content:flex-end;margin-top:20px}@media(max-width:540px){.source-test-dialog{padding:20px 16px}li{gap:8px}}
`})
export class SourceTestDialogComponent implements AfterViewInit {
  source=input.required<SourceEntry>();run=input<SourceTestRun>();closed=output<void>();retry=output<void>();
  private dialog=viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  steps=[{id:'dns',label:'Résolution du serveur',note:'Recherche de l’adresse réseau.'},{id:'tcp',label:'Accès au port',note:'Vérification de la connexion réseau.'},{id:'jdbc',label:'Connexion à la base',note:'TLS, authentification et ouverture de la base.'},{id:'validation',label:'Validation de la session SQL',note:'Vérification de la session par le pilote.'}] as const;
  ngAfterViewInit(){this.dialog().nativeElement.showModal();}
  label(step:DiagnosticStep,run:SourceTestRun){if(run.state==='failed'&&step.state==='waiting')return 'Non confirmé';return {waiting:'En attente',running:'En cours',passed:'Réussi',failed:'Échec',skipped:'Ignoré',unknown:'Non confirmé'}[step.state];}
}
