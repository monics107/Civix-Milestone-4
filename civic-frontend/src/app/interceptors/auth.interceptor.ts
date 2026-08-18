import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError(err => {
      const isAuthenticationRequest =
        req.url.includes('/auth/login') || req.url.includes('/auth/register');

      // A token can expire while the user is already on a protected page.
      // Clear that stale session and send the user back to login instead of
      // continuing to issue unauthorised dashboard, petition, and poll calls.
      if (err.status === 401 && !isAuthenticationRequest) {
        auth.logout();
      }

      return throwError(() => err);
    })
  );
};
