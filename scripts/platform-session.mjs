// Session client for trusted, configured local validation/benchmark API endpoints.
// Credentials stay in environment variables; cookies and CSRF stay in memory.
export function createPlatformSession(base){
  let cookie='',csrf='',ready;
  async function raw(route,options={}){
    const headers=new Headers(options.headers);if(cookie)headers.set('Cookie',cookie);
    const response=await fetch(base+route,{...options,headers,signal:options.signal??AbortSignal.timeout(30000)});
    for(const value of response.headers.getSetCookie())if(value.startsWith('ATLAS_SESSION='))cookie=value.split(';')[0];
    return response;
  }
  async function session(){const response=await raw('/auth/session');if(!response.ok)throw Error('Platform session unavailable');const value=await response.json();csrf=value.csrf_token;return value;}
  async function login(){
    const password=process.env.APP_LOGIN_PASSWORD,username=process.env.APP_LOGIN_USERNAME??'admin';
    if(!password)throw Error('APP_LOGIN_PASSWORD is required for platform tools');
    await session();
    const response=await raw('/auth/login',{method:'POST',headers:{'X-CSRF-TOKEN':csrf,'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({username,password}).toString()});
    if(response.status!==204)throw Error(`Platform login failed (${response.status})`);
    if(!(await session()).authenticated)throw Error('Platform authentication was not established');
  }
  return async(route,options={})=>{
    if(!route.startsWith('/')||route.startsWith('//')||route.includes('\\'))throw Error('Invalid platform route');
    ready??=login();await ready;
    const headers=new Headers(options.headers);if(!['GET','HEAD','OPTIONS'].includes(options.method??'GET'))headers.set('X-CSRF-TOKEN',csrf);
    // Never retry a write after a session or transport failure.
    return raw(route,{...options,headers});
  };
}
