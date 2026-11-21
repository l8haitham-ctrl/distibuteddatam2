import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { AppComponent } from './app/app.component';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { routes } from './app/app.routes';

bootstrapApplication(AppComponent, { providers: [provideHttpClient(),provideRouter(routes,withInMemoryScrolling({scrollPositionRestoration:'enabled'}))] }).catch(() => {
  document.body.textContent = 'Impossible de démarrer l’interface. Veuillez actualiser la page.';
});
