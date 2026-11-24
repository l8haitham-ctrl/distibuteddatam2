import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createServer} from 'node:http';
import {once} from 'node:events';
import {createPlatformSession} from './platform-session.mjs';
test('tool session rotates cookies and csrf, scopes routes and never retries a failed write',async()=>{
  let logins=0,writes=0;
  const server=createServer(async(req,res)=>{
    if(req.url==='/auth/session'){
      const authenticated=req.headers.cookie==='ATLAS_SESSION=after';res.setHeader('Content-Type','application/json');
      res.setHeader('Set-Cookie',`ATLAS_SESSION=${authenticated?'after':'before'}; HttpOnly; Path=/; SameSite=Strict`);
      res.end(JSON.stringify({authenticated,csrf_token:authenticated?'fresh-token':'initial-token'}));return;
    }
    if(req.url==='/auth/login'){logins++;assert.equal(req.headers['x-csrf-token'],'initial-token');assert.equal(req.headers.cookie,'ATLAS_SESSION=before');res.setHeader('Set-Cookie','ATLAS_SESSION=after; HttpOnly; Path=/');res.writeHead(204);res.end();return;}
    writes++;assert.equal(req.headers['x-csrf-token'],'fresh-token');assert.equal(req.headers.cookie,'ATLAS_SESSION=after');res.writeHead(401);res.end();
  });
  server.listen(0,'127.0.0.1');await once(server,'listening');
  const previous=process.env.APP_LOGIN_PASSWORD;process.env.APP_LOGIN_PASSWORD='OnlyForToolTests2026!';
  try{
    const api=createPlatformSession(`http://127.0.0.1:${server.address().port}`);
    assert.equal((await api('/test-write',{method:'POST',body:'{}'})).status,401);
    assert.equal(logins,1);assert.equal(writes,1);
    await assert.rejects(()=>api('//external.example.test/action'),/Invalid platform route/);
    assert.equal(logins,1);assert.equal(writes,1);
  }finally{if(previous===undefined)delete process.env.APP_LOGIN_PASSWORD;else process.env.APP_LOGIN_PASSWORD=previous;server.closeAllConnections();await new Promise(resolve=>server.close(resolve));}
});
