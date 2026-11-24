import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './app/features/auth/auth.interceptor';
import { AppComponent } from './app/app.component';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { routes } from './app/app.routes';

bootstrapApplication(AppComponent, { providers: [provideHttpClient(withInterceptors([authInterceptor])),provideRouter(routes,withInMemoryScrolling({scrollPositionRestoration:'enabled'}))] }).catch(() => {
  document.body.textContent = 'Impossible de démarrer l’interface. Veuillez actualiser la page.';
});
