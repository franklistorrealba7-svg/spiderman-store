/* Araña que camina por la pantalla (canvas 2D con sombreado para efecto 3D) */
(function(){
  if (matchMedia("(prefers-reduced-motion: reduce)").matches) return;
  const cv = document.createElement("canvas");
  cv.id = "spider"; cv.setAttribute("aria-hidden","true");
  cv.style.cssText = "position:fixed;inset:0;width:100vw;height:100vh;pointer-events:none;z-index:45";
  document.body.appendChild(cv);
  const x = cv.getContext("2d");
  let W=0,H=0,D=1;
  function size(){ D=Math.min(devicePixelRatio||1,2); W=innerWidth; H=innerHeight; cv.width=W*D; cv.height=H*D; x.setTransform(D,0,0,D,0,0); }
  size(); addEventListener("resize", size);

  const S = innerWidth < 600 ? 0.6 : 0.85;          // escala
  const L1 = 36*S, L2 = 46*S;                          // largo de segmentos de pata
  // ángulo de cadera (rad, relativo al frente) y alcance de reposo
  const HIPS = [-0.55,-1.15,-1.85,-2.45, 0.55,1.15,1.85,2.45];
  const REACH = [64,58,58,66].map(v=>v*S);
  const GROUP = [0,1,0,1, 1,0,1,0];                    // marcha alterna (trípode doble)

  const sp = { x:-120, y:H*0.6, a:0, speed:0, tx:0, ty:0, state:"walk", wait:0 };
  const feet = HIPS.map(()=>({x:0,y:0,fx:0,fy:0,tx:0,ty:0,t:1}));
  let stepGroup = 0, stepT = 1;

  function pick(){
    const m=60;
    sp.tx = m + Math.random()*(W-2*m); sp.ty = 120 + Math.random()*(H-240);
  }
  function offscreen(){
    const side=Math.floor(Math.random()*4);
    if(side===0){sp.tx=-160;sp.ty=Math.random()*H;} else if(side===1){sp.tx=W+160;sp.ty=Math.random()*H;}
    else if(side===2){sp.tx=Math.random()*W;sp.ty=-160;} else {sp.tx=Math.random()*W;sp.ty=H+160;}
  }
  function spawn(){
    const side=Math.floor(Math.random()*2);
    sp.x = side? W+120 : -120; sp.y = 160 + Math.random()*(H-320);
    sp.a = side? Math.PI : 0; sp.state="walk"; sp.legs=0; pick();
    feet.forEach((f,i)=>{ const r=rest(i); f.x=f.fx=f.tx=r.x; f.y=f.fy=f.ty=r.y; f.t=1; });
  }
  function rest(i){
    const k=i%4, ang=sp.a+HIPS[i];
    return { x: sp.x+Math.cos(ang)*REACH[k] + Math.cos(sp.a)*sp.speed*0.12,
             y: sp.y+Math.sin(ang)*REACH[k] + Math.sin(sp.a)*sp.speed*0.12 };
  }
  function hip(i){
    const k=i%4, side=i<4?-1:1;
    const off=[10,4,-3,-9][k]*S;                        // posición a lo largo del cefalotórax
    return { x: sp.x+Math.cos(sp.a)*off + Math.cos(sp.a+side*Math.PI/2)*5*S,
             y: sp.y+Math.sin(sp.a)*off + Math.sin(sp.a+side*Math.PI/2)*5*S };
  }

  let last=performance.now(), visits=0, jumpClock=0, fx=null;
  const JUMP_EVERY=30, JUMP_T=0.55, WEB_IN=0.7, WEB_HOLD=1.3, WEB_OUT=0.7;
  function startJump(){
    if (sp.state==="gone"){ spawn(); }
    fx={t:0, sx:sp.x, sy:sp.y, cx:W/2, cy:H*0.45, spokes:Array.from({length:22},(_,i)=>i/22*Math.PI*2+(Math.random()-.5)*.12)};
    sp.state="jump";
  }
  function drawWeb(cx,cy,prog,alpha,spokes){
    const R=Math.hypot(W,H)*0.75*prog;
    x.save(); x.globalAlpha=alpha; x.lineCap="round";
    x.shadowColor="rgba(255,255,255,.55)"; x.shadowBlur=6;
    x.strokeStyle="rgba(245,245,250,.9)"; x.lineWidth=1.6;
    spokes.forEach(a=>{ x.beginPath(); x.moveTo(cx,cy); x.lineTo(cx+Math.cos(a)*R,cy+Math.sin(a)*R); x.stroke(); });
    x.lineWidth=1.1;
    for(let k=1;k<40;k++){
      const rr=k*k*2.2+k*22; if(rr>R) break;
      x.strokeStyle=`rgba(245,245,250,${Math.max(.25,.9-k*.02)})`;
      x.beginPath();
      spokes.forEach((a,i)=>{ const a2=spokes[(i+1)%spokes.length]+(i+1===spokes.length?Math.PI*2:0);
        const p1=[cx+Math.cos(a)*rr,cy+Math.sin(a)*rr], p2=[cx+Math.cos(a2)*rr,cy+Math.sin(a2)*rr], am=(a+a2)/2;
        if(i===0) x.moveTo(p1[0],p1[1]); x.quadraticCurveTo(cx+Math.cos(am)*rr*.9,cy+Math.sin(am)*rr*.9,p2[0],p2[1]); });
      x.stroke();
    }
    x.restore();
  }
  spawn();
  function tick(now){
    const raw=(now-last)/1000; const dt=Math.min(0.05,raw); last=now;
    jumpClock+=Math.min(raw,0.5);
    if (!fx && jumpClock>=JUMP_EVERY){ jumpClock=0; startJump(); }
    if (fx){
      fx.t+=Math.min(raw,0.1); const t=fx.t;
      x.clearRect(0,0,W,H);
      if (t<JUMP_T){
        const e=t/JUMP_T, ee=e*e*(3-2*e);
        sp.x=fx.sx+(fx.cx-fx.sx)*ee; sp.y=fx.sy+(fx.cy-fx.sy)*ee - Math.sin(e*Math.PI)*60;
        sp.a+= (Math.atan2(fx.cy-fx.sy,fx.cx-fx.sx)-sp.a)*Math.min(1,dt*8);
        feet.forEach((f,i)=>{ const r=rest(i); f.x=r.x; f.y=r.y; f.t=1; f.lift=0; });
        draw(now, 1+ee*5);
      } else {
        const w=t-JUMP_T;
        let prog=Math.min(1,w/WEB_IN), alpha=1;
        if (w>WEB_IN+WEB_HOLD) alpha=Math.max(0,1-(w-WEB_IN-WEB_HOLD)/WEB_OUT);
        drawWeb(fx.cx,fx.cy,1-Math.pow(1-prog,3),alpha,fx.spokes);
        if (w<WEB_IN*0.6) draw(now, 6*(1-w/(WEB_IN*0.6))+0.01);
        if (alpha<=0){ fx=null; jumpClock=0; spawn(); }
      }
      requestAnimationFrame(tick); return;
    }
    // movimiento del cuerpo
    if (sp.state==="walk" || sp.state==="leave"){
      const dx=sp.tx-sp.x, dy=sp.ty-sp.y, d=Math.hypot(dx,dy);
      const want=Math.atan2(dy,dx); let da=want-sp.a; da=Math.atan2(Math.sin(da),Math.cos(da));
      sp.a += da*Math.min(1,dt*3.2);
      const target = Math.abs(da)>0.9 ? 25 : (d<80 ? 45 : 110)*S/0.9;
      sp.speed += (target-sp.speed)*Math.min(1,dt*3);
      sp.x += Math.cos(sp.a)*sp.speed*dt; sp.y += Math.sin(sp.a)*sp.speed*dt;
      if (d<18){
        if (sp.state==="leave"){ sp.state="gone"; sp.wait=6+Math.random()*10; }
        else { sp.state="pause"; sp.wait=0.8+Math.random()*2.2; }
      }
    } else if (sp.state==="pause"){
      sp.speed += (0-sp.speed)*Math.min(1,dt*6);
      sp.wait-=dt;
      if (sp.wait<=0){ visits++; if (visits%3===0){ sp.state="leave"; offscreen(); } else { sp.state="walk"; pick(); } }
    } else if (sp.state==="gone"){
      sp.wait-=dt; if (sp.wait<=0){ spawn(); }
      x.clearRect(0,0,W,H); requestAnimationFrame(tick); return;
    }
    // pasos
    stepT += dt/0.11;
    if (stepT>=1){
      // ¿alguna pata del grupo siguiente necesita moverse?
      const g = 1-stepGroup; let need=false;
      feet.forEach((f,i)=>{ if(GROUP[i]===g){ const r=rest(i); if(Math.hypot(r.x-f.x,r.y-f.y)>14*S || (sp.speed<5 && Math.hypot(r.x-f.x,r.y-f.y)>4)) need=true; }});
      if (need && sp.speed>2){
        stepGroup=g; stepT=0;
        feet.forEach((f,i)=>{ if(GROUP[i]===g){ const r=rest(i); f.fx=f.x; f.fy=f.y; f.tx=r.x+Math.cos(sp.a)*10*S; f.ty=r.y+Math.sin(sp.a)*10*S; f.t=0; }});
      }
    }
    feet.forEach(f=>{ if(f.t<1){ f.t=Math.min(1,stepT); const e=f.t<.5?2*f.t*f.t:1-Math.pow(-2*f.t+2,2)/2; f.x=f.fx+(f.tx-f.fx)*e; f.y=f.fy+(f.ty-f.fy)*e; f.lift=Math.sin(f.t*Math.PI); } else f.lift=0; });
    draw(now);
    requestAnimationFrame(tick);
  }

  function leg(i){
    const h=hip(i), f=feet[i], side=i<4?-1:1;
    const dx=f.x-h.x, dy=f.y-h.y; let d=Math.hypot(dx,dy);
    d=Math.min(d, L1+L2-1);
    const base=Math.atan2(dy,dx);
    const cosA=(L1*L1+d*d-L2*L2)/(2*L1*d); const A=Math.acos(Math.max(-1,Math.min(1,cosA)));
    const k=base - side*A;                                       // rodilla hacia afuera
    const knee={x:h.x+Math.cos(k)*L1, y:h.y+Math.sin(k)*L1};
    const lift=(f.lift||0)*10*S, kneeLift=14*S+lift*0.6;        // la rodilla sube (vista 3D)
    return {h, knee:{x:knee.x, y:knee.y-kneeLift}, foot:{x:f.x, y:f.y-lift}, footShadow:{x:f.x,y:f.y}, kneeShadow:knee};
  }

  function seg(a,b,w0,w1,c0,c1){
    const g=x.createLinearGradient(a.x,a.y,b.x,b.y); g.addColorStop(0,c0); g.addColorStop(1,c1);
    const ang=Math.atan2(b.y-a.y,b.x-a.x), nx=-Math.sin(ang), ny=Math.cos(ang);
    x.beginPath();
    x.moveTo(a.x+nx*w0,a.y+ny*w0); x.lineTo(b.x+nx*w1,b.y+ny*w1);
    x.lineTo(b.x-nx*w1,b.y-ny*w1); x.lineTo(a.x-nx*w0,a.y-ny*w0); x.closePath();
    x.fillStyle=g; x.fill();
    // pelos
    x.strokeStyle="rgba(40,30,25,.55)"; x.lineWidth=0.7;
    const n=6; for(let t=1;t<n;t++){ const px=a.x+(b.x-a.x)*t/n, py=a.y+(b.y-a.y)*t/n, w=w0+(w1-w0)*t/n;
      x.beginPath(); x.moveTo(px+nx*w,py+ny*w); x.lineTo(px+nx*(w+2.2*S)+Math.cos(ang)*1.5,py+ny*(w+2.2*S)+Math.sin(ang)*1.5); x.stroke();
      x.beginPath(); x.moveTo(px-nx*w,py-ny*w); x.lineTo(px-nx*(w+2.2*S)+Math.cos(ang)*1.5,py-ny*(w+2.2*S)+Math.sin(ang)*1.5); x.stroke(); }
  }

  function draw(now,k){
    if(!k){ x.clearRect(0,0,W,H); if (sp.x<-200||sp.x>W+200||sp.y<-200||sp.y>H+200) return; }
    x.save(); if(k&&k!==1){ x.translate(sp.x,sp.y); x.scale(k,k); x.translate(-sp.x,-sp.y); }
    const legs=HIPS.map((_,i)=>leg(i));
    const bob=Math.sin(now/90)*0.8*Math.min(1,sp.speed/40);
    // sombras
    x.save(); x.filter="blur(4px)"; x.fillStyle="rgba(0,0,0,.35)";
    x.beginPath(); x.ellipse(sp.x+6*S - Math.cos(sp.a)*14*S, sp.y+10*S - Math.sin(sp.a)*14*S, 26*S, 18*S, sp.a, 0, Math.PI*2); x.fill();
    x.strokeStyle="rgba(0,0,0,.25)"; x.lineWidth=2.5*S; x.lineCap="round";
    legs.forEach(l=>{ x.beginPath(); x.moveTo(l.h.x+6*S,l.h.y+10*S); x.lineTo(l.kneeShadow.x+6*S,l.kneeShadow.y+10*S); x.lineTo(l.footShadow.x+3*S,l.footShadow.y+4*S); x.stroke(); });
    x.restore();
    // patas
    legs.forEach(l=>{
      seg(l.h,l.knee,2.8*S,2.1*S,"#5b4638","#231a14");
      seg(l.knee,l.foot,2.0*S,0.8*S,"#2e231c","#6a5444");
      x.fillStyle="#120e0c"; x.beginPath(); x.arc(l.knee.x,l.knee.y,2.2*S,0,Math.PI*2); x.fill();
      x.fillStyle="rgba(255,255,255,.18)"; x.beginPath(); x.arc(l.knee.x-0.7*S,l.knee.y-0.8*S,0.9*S,0,Math.PI*2); x.fill();
    });
    x.save(); x.translate(sp.x, sp.y+bob); x.rotate(sp.a);
    // abdomen
    let g=x.createRadialGradient(-26*S,-6*S,2*S,-22*S,0,24*S);
    g.addColorStop(0,"#5a4438"); g.addColorStop(.35,"#22190f"); g.addColorStop(1,"#050403");
    x.fillStyle=g; x.beginPath(); x.ellipse(-24*S,0,22*S,16*S,0,0,Math.PI*2); x.fill(); x.strokeStyle="rgba(255,235,220,.16)"; x.lineWidth=1.2; x.stroke();
    // marca roja tipo reloj de arena
    x.fillStyle="rgba(200,16,40,.9)";
    x.beginPath(); x.moveTo(-14*S,0); x.lineTo(-22*S,-4*S); x.lineTo(-30*S,0); x.lineTo(-22*S,4*S); x.closePath(); x.fill();
    // brillo especular
    g=x.createRadialGradient(-28*S,-8*S,0,-28*S,-8*S,10*S); g.addColorStop(0,"rgba(255,255,255,.45)"); g.addColorStop(1,"rgba(255,255,255,0)");
    x.fillStyle=g; x.beginPath(); x.ellipse(-28*S,-8*S,10*S,6*S,-0.3,0,Math.PI*2); x.fill();
    // pedicelo
    x.fillStyle="#0b0807"; x.beginPath(); x.ellipse(-4*S,0,4*S,3*S,0,0,Math.PI*2); x.fill();
    // cefalotórax
    g=x.createRadialGradient(4*S,-4*S,1*S,6*S,0,14*S); g.addColorStop(0,"#4a382e"); g.addColorStop(.5,"#1c1510"); g.addColorStop(1,"#060504");
    x.fillStyle=g; x.beginPath(); x.ellipse(6*S,0,13*S,10.5*S,0,0,Math.PI*2); x.fill(); x.strokeStyle="rgba(255,235,220,.16)"; x.lineWidth=1.2; x.stroke();
    // quelíceros
    x.fillStyle="#0e0a08"; x.beginPath(); x.ellipse(19*S,-3*S,3.5*S,2.6*S,0.3,0,Math.PI*2); x.ellipse(19*S,3*S,3.5*S,2.6*S,-0.3,0,Math.PI*2); x.fill();
    // ojos
    [[13,-3,1.9],[13,3,1.9],[11,-6,1.2],[11,6,1.2],[15,-1.2,1],[15,1.2,1]].forEach(([ex,ey,r])=>{
      x.fillStyle="#000"; x.beginPath(); x.arc(ex*S,ey*S,r*S,0,Math.PI*2); x.fill();
      x.fillStyle="rgba(255,255,255,.7)"; x.beginPath(); x.arc(ex*S-0.4*S,ey*S-0.5*S,r*S*0.35,0,Math.PI*2); x.fill();
    });
    g=x.createRadialGradient(3*S,-5*S,0,3*S,-5*S,7*S); g.addColorStop(0,"rgba(255,255,255,.35)"); g.addColorStop(1,"rgba(255,255,255,0)");
    x.fillStyle=g; x.beginPath(); x.ellipse(3*S,-5*S,7*S,4*S,-0.2,0,Math.PI*2); x.fill();
    x.restore();
    x.restore();
  }
  setTimeout(()=>requestAnimationFrame(t=>{ last=t; tick(t); }), 1500);
})();
