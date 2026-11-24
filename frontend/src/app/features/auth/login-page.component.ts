import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthStore } from './auth.store';
import { safeReturnUrl } from './auth.guard';
import { errorMessage } from '../../core/api.service';
import { IconComponent } from '../../shared/icon.component';
@Component({selector:'app-login-page',imports:[FormsModule,IconComponent],template:`
  <main class="login-page"><section class="login-card" aria-labelledby="login-title">
    <div class="login-brand"><span><app-icon name="Layers3" [size]="21"/></span>Data Platform</div>
    <p class="login-eyebrow">VOTRE ESPACE DE DONNÉES</p><h1 id="login-title">Connectez-vous à la plateforme</h1><p class="login-intro">Retrouvez vos sources, vos extractions et vos définitions métier.</p>
    @if(error()){<p class="login-error" role="alert">{{error()}}</p>}
    @if(loading()){<p role="status">Vérification de la session…</p>}
    <form #form="ngForm" (ngSubmit)="login()"><fieldset [disabled]="loading()||busy()||!ready()">
      <label for="login-user">Identifiant</label><input id="login-user" name="username" [(ngModel)]="username" required maxlength="100" autocomplete="username" autofocus/>
      <label for="login-password">Mot de passe</label><div class="password-field"><input id="login-password" name="password" [(ngModel)]="password" required maxlength="72" [type]="visible()?'text':'password'" autocomplete="current-password"/><button type="button" [attr.aria-label]="visible()?'Masquer le mot de passe':'Afficher le mot de passe'" [attr.aria-pressed]="visible()" (click)="visible.set(!visible())"><app-icon [name]="visible()?'EyeOff':'Eye'"/></button></div>
      <button class="wf-button wf-primary login-submit" type="submit" [disabled]="form.invalid||!username.trim()">{{busy()?'Connexion…':'Se connecter'}}<app-icon name="ArrowRight"/></button>
    </fieldset></form>
    @if(!ready()&&!loading()){<button class="wf-button" (click)="restore()">Réessayer la connexion au service</button>}
    <p class="login-footer"><app-icon name="LockKeyhole" [size]="13"/>Utilisez le compte fourni pour cette plateforme.</p>
  </section><p class="login-caption">Explorez. Comprenez. Analysez.</p></main>`,styles:`
  :host{display:block}.login-page{min-height:100dvh;display:flex;flex-direction:column;justify-content:center;align-items:center;background:#f4f4f5;padding:35px 20px;color:#27272a}.login-card{width:100%;max-width:460px;background:#fff;border:1px solid #c8c8ce;border-radius:5px;padding:38px;box-shadow:0 8px 30px #18181b07}.login-brand{display:flex;align-items:center;gap:10px;font-size:19px;font-weight:700;margin-bottom:38px}.login-brand>span{display:grid;place-items:center;background:#3f3f46;color:#fff;width:36px;height:36px;border-radius:4px}.login-eyebrow{font-size:10px;letter-spacing:1.5px;color:#71717a;margin-bottom:12px}.login-card h1{font-size:26px;line-height:1.3;margin-bottom:12px}.login-intro{font-size:14px;line-height:1.7;color:#63636d;margin-bottom:28px}.login-card fieldset{border:0;padding:0;min-width:0;display:grid;gap:10px}.login-card label{font-size:13px;font-weight:700}.login-card input{height:43px;padding:10px 12px;border:1px solid #b4b4bd;width:100%;background:white;color:#27272a}.login-card label:not(:first-child){margin-top:12px}.password-field{position:relative}.password-field input{padding-right:45px}.password-field button{position:absolute;right:1px;top:1px;width:42px;height:41px;display:grid;place-items:center}.login-submit{width:100%;margin-top:16px;min-height:43px;justify-content:space-between}.login-error{padding:12px;border:1px solid #c89e94;background:#fff8f6;font-size:13px;line-height:1.6;margin-bottom:20px}.login-footer{border-top:1px solid #e4e4e7;padding-top:20px;margin-top:26px;display:flex;align-items:center;gap:8px;font-size:12px;color:#71717a}.login-caption{font-size:12px;color:#71717a;margin-top:20px}@media(max-width:500px){.login-card{padding:28px 22px}.login-card h1{font-size:23px}.login-page{padding:24px 16px}}
`})
export class LoginPageComponent {
  private auth=inject(AuthStore);private router=inject(Router);private route=inject(ActivatedRoute);private pending=new Subscription();
  username='';password='';loading=signal(false);busy=signal(false);ready=signal(false);visible=signal(false);error=signal('');
  constructor(){inject(DestroyRef).onDestroy(()=>{this.pending.unsubscribe();this.password='';});this.restore();}
  restore(){this.loading.set(true);this.error.set('');this.pending.add(this.auth.restore().subscribe({next:s=>{this.loading.set(false);this.ready.set(true);if(s.authenticated)this.enter();},error:e=>{this.loading.set(false);this.ready.set(false);this.error.set(errorMessage(e));}}));}
  login(){if(this.busy()||!this.ready()||!this.username.trim()||!this.password)return;this.busy.set(true);this.error.set('');
    this.pending.add(this.auth.login(this.username.trim(),this.password).subscribe({next:s=>{this.password='';this.busy.set(false);if(s.authenticated)this.enter();else this.error.set('La session n’a pas pu être ouverte. Réessayez.');},error:e=>{this.password='';this.busy.set(false);this.error.set(errorMessage(e));this.pending.add(this.auth.restore().subscribe({next:()=>this.ready.set(true),error:()=>this.ready.set(false)}));}}));}
  private enter(){void this.router.navigateByUrl(safeReturnUrl(this.route.snapshot.queryParamMap.get('returnUrl')));}
}
