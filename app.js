/* =========================================================
   CONFIGURACIÓN — cambia aquí tu número y tus productos.
   Para poner la foto real: img: "nombre-archivo.jpg"
   ========================================================= */
const WHATSAPP = "51912762364";   // WhatsApp de la tienda: +51 912 762 364

const CATS = [
  {id:"crocs",    name:"Crocs"},
  {id:"mascaras", name:"Máscaras"},
  {id:"carros",   name:"Carros"}
];
const CROCS_SIZES = ["M4/W6|36","M5/W7|37","M6/W8|38","M7/W9|39-40","M8/W10|41-42","M9/W11|43","M10/W12|44","M11/W13|45"];
const sizeChip = s => { const [a,b]=s.split("|"); return b?`<b>${a}</b><small>EU ${b}</small>`:a; };
const sizeText = s => { const [a,b]=s.split("|"); return b?`${a} (${b})`:a; };
const CROCS_GIFT = "Regalo según promoción vigente: shopping bag, caja de Crocs y 5 Jibbitz";
const MASK_INFO  = "Ojos movibles, tela lycra spandex, faceshell incluido";

/* price: null → se muestra "Consultar precio" */
const PRODUCTS = [
  /* ---------- MÁSCARAS ---------- */
  {id:"m1", cat:"mascaras", name:"Máscara Tobey Maguire · Black Spider", price:269, old:320, info:MASK_INFO, badge:"Ojos movibles", imgs:["tobey-black-1.webp?v=3","tobey-black-2.webp?v=3"]},
  {id:"m2", cat:"mascaras", name:"Máscara Tobey Maguire · Red Spider",   price:269, old:320, info:MASK_INFO, badge:"Ojos movibles", imgs:["tobey-red-1.webp?v=3","tobey-red-2.webp?v=3"]},
  {id:"m3", cat:"mascaras", name:"Máscara Red Spider · Lentes dorados",  price:289, info:"Incluye caja y faceshell", imgs:["red-gold-1.webp?v=3","red-gold-2.webp?v=3","red-gold-3.webp?v=3"]},
  {id:"m4", cat:"mascaras", name:"Máscara Black Stealth",                price:315.90, info:"Incluye caja", imgs:["black-stealth-1.webp?v=3","black-stealth-2.webp?v=3","black-stealth-3.webp?v=3"]},
  {id:"m5", cat:"mascaras", name:"Máscara Red Hero",                     price:99.90, info:"Incluye caja", imgs:["red-hero-1.webp?v=3","red-hero-2.webp?v=3","red-hero-3.webp?v=3"]},
  {id:"m6", cat:"mascaras", name:"Lanza Telarañas Electrónico",          price:99.90, old:139, info:"Puntas imantadas. Caja, manual, cable de carga, desarmador y guante", imgs:[]},
  /* ---------- CARROS ---------- */
  {id:"c1", cat:"carros", name:"Set Cars · Camión Mack y autos", price:489.95, info:"Caja de colección y bolsa de regalo", badge:"Set completo", imgs:["cars-set-1.webp?v=3","cars-set-2.webp?v=3","cars-set-3.webp?v=3","cars-set-4.webp?v=3"]},
  /* ---------- CROCS ---------- */
  {id:"ks1", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs Spider-Man · Black", price:319, info:CROCS_GIFT, badge:"Más vendido", imgs:["o-spiderman-black-1.webp?v=3", "o-spiderman-black-2.webp?v=3"]},
  {id:"ks2", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs Spider-Man · Red", price:319, info:CROCS_GIFT, badge:"Más vendido", imgs:["o-spiderman-red-1.webp?v=3", "o-spiderman-red-2.webp?v=3", "o-spiderman-red-3.webp?v=3", "o-spiderman-red-4.webp?v=3"]},
  {id:"ks3", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs Spider-Man · Marvel", price:null, imgs:["o-spiderman-marvel-1.webp?v=3", "o-spiderman-marvel-2.webp?v=3", "o-spiderman-marvel-3.webp?v=3", "o-spiderman-marvel-4.webp?v=3", "o-spiderman-marvel-5.webp?v=3"]},
  {id:"k1", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Cars · Rayo McQueen", price:239.9, old:280, info:CROCS_GIFT, imgs:["o-rayo-mcqueen-1.webp?v=3", "o-rayo-mcqueen-2.webp?v=3", "o-rayo-mcqueen-3.webp?v=3", "o-rayo-mcqueen-4.webp?v=3", "o-rayo-mcqueen-5.webp?v=3"]},
  {id:"k2", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Cars · Rayo McQueen Full Set", price:299, old:339, info:CROCS_GIFT, badge:"Con alerones y luces", imgs:["o-rayo-full-set-1.webp?v=3", "o-rayo-full-set-2.webp?v=3", "o-rayo-full-set-3.webp?v=3", "o-rayo-full-set-4.webp?v=3", "o-rayo-full-set-5.webp?v=3"]},
  {id:"k4", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Rayo McQueen · Kids", price:199.9, old:240, info:CROCS_GIFT, imgs:["o-rayo-mcqueen-1.webp?v=3", "o-rayo-mcqueen-2.webp?v=3", "o-rayo-mcqueen-3.webp?v=3", "o-rayo-mcqueen-4.webp?v=3", "o-rayo-mcqueen-5.webp?v=3"]},
  {id:"k3", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Cars · Tow Mate", price:240, old:279, info:CROCS_GIFT, imgs:["o-tow-mate-1.webp?v=3", "o-tow-mate-2.webp?v=3", "o-tow-mate-3.webp?v=3"]},
  {id:"k5", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Hello Kitty", price:269, old:320, info:CROCS_GIFT, imgs:["o-hello-kitty-1.webp?v=3", "o-hello-kitty-2.webp?v=3"]},
  {id:"k6", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Hello Kitty · Blanco", price:240, old:290, info:CROCS_GIFT, imgs:["o-hello-kitty-blanco-1.webp?v=3", "o-hello-kitty-blanco-2.webp?v=3", "o-hello-kitty-blanco-3.webp?v=3", "o-hello-kitty-blanco-4.webp?v=3"]},
  {id:"k7", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Los Simpsons · Krusty", price:279, old:340, info:CROCS_GIFT, imgs:["o-krusty-4.webp?v=3", "o-krusty-2.webp?v=3", "o-krusty-1.webp?v=3", "o-krusty-3.webp?v=3"]},
  {id:"k8", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Chucky", price:269, old:320, info:CROCS_GIFT, imgs:["o-chucky-1.webp?v=3", "o-chucky-2.webp?v=3", "o-chucky-3.webp?v=3", "o-chucky-4.webp?v=3"]},
  {id:"k9", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Súper Girls · Bombón", price:249, old:280, info:CROCS_GIFT, imgs:["o-bombon-1.webp?v=3", "o-bombon-2.webp?v=3", "o-bombon-3.webp?v=3"]},
  {id:"k10", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Lotso", price:255, old:289, info:CROCS_GIFT, imgs:["o-lotso-1.webp?v=3", "o-lotso-2.webp?v=3", "o-lotso-3.webp?v=3"]},
  {id:"k11", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Barbie", price:259, old:320, info:CROCS_GIFT, imgs:["o-barbie-1.webp?v=3", "o-barbie-2.webp?v=3", "o-barbie-3.webp?v=3", "o-barbie-4.webp?v=3"]},
  {id:"k12", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Demon Slayer · Inosuke", price:269, old:340, info:CROCS_GIFT, imgs:["o-inosuke-1.webp?v=3", "o-inosuke-2.webp?v=3", "o-inosuke-3.webp?v=3", "o-inosuke-4.webp?v=3", "o-inosuke-5.webp?v=3"]},
  {id:"n1", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Stranger Things", price:null, imgs:["o-stranger-things-1.webp?v=3", "o-stranger-things-2.webp?v=3", "o-stranger-things-3.webp?v=3", "o-stranger-things-4.webp?v=3", "o-stranger-things-5.webp?v=3"]},
  {id:"n2", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Star Wars · Darth Vader", price:null, imgs:["o-darth-vader-1.webp?v=3", "o-darth-vader-2.webp?v=3", "o-darth-vader-3.webp?v=3"]},
  {id:"n3", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Star Wars · Grogu", price:null, imgs:["o-grogu-1.webp?v=3", "o-grogu-2.webp?v=3"]},
  {id:"n4", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Batman", price:null, imgs:["o-batman-1.webp?v=3", "o-batman-2.webp?v=3", "o-batman-3.webp?v=3", "o-batman-4.webp?v=3"]},
  {id:"n5", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Toy Story · Buzz Lightyear", price:null, imgs:["o-buzz-1.webp?v=3", "o-buzz-2.webp?v=3", "o-buzz-3.webp?v=3"]},
  {id:"n6", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Monsters Inc · Mike Wazowski", price:null, imgs:["o-mike-wazowski-1.webp?v=3", "o-mike-wazowski-2.webp?v=3"]},
  {id:"n7", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Los Simpsons", price:null, imgs:["o-simpsons-1.webp?v=3", "o-simpsons-2.webp?v=3", "o-simpsons-3.webp?v=3"]},
  {id:"n8", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Bob Esponja", price:null, imgs:["o-bob-esponja-1.webp?v=3", "o-bob-esponja-2.webp?v=3"]},
  {id:"n9", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Bob Esponja · Patricio", price:null, imgs:["o-patricio-1.webp?v=3", "o-patricio-2.webp?v=3"]},
  {id:"n10", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Dragon Ball · Goku", price:null, imgs:["o-goku-1.webp?v=3", "o-goku-2.webp?v=3"]},
  {id:"n11", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Dragon Ball · Cell", price:null, imgs:["o-cell-3.webp?v=3", "o-cell-2.webp?v=3", "o-cell-1.webp?v=3"]},
  {id:"n12", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x One Piece · Luffy", price:null, imgs:["o-luffy-1.webp?v=3", "o-luffy-2.webp?v=3"]},
  {id:"n13", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Naruto · Kakashi", price:null, imgs:["o-kakashi-1.webp?v=3", "o-kakashi-2.webp?v=3"]},
  {id:"n14", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Naruto · Akatsuki", price:null, imgs:["o-akatsuki-1.webp?v=3", "o-akatsuki-2.webp?v=3"]},
  {id:"n15", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Pokémon · Pikachu", price:null, imgs:["o-pikachu-1.webp?v=3", "o-pikachu-2.webp?v=3", "o-pikachu-3.webp?v=3"]},
  {id:"n16", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Pokémon · Charizard y Blastoise", price:null, imgs:["o-pokemon-1.webp?v=3"]},
  {id:"n17", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x BAPE · Camo Negro", price:null, imgs:["o-bape-negro-1.webp?v=3", "o-bape-negro-2.webp?v=3"]},
  {id:"n18", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x BAPE · Camo Azul", price:null, imgs:["o-bape-azul-1.webp?v=3"]},
  {id:"n19", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x BAPE · Camo Rosado", price:null, imgs:["o-bape-rosado-1.webp?v=3", "o-bape-rosado-2.webp?v=3"]},
  {id:"n20", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x BAPE · Camo Morado", price:null, imgs:["o-bape-morado-1.webp?v=3", "o-bape-morado-2.webp?v=3", "o-bape-morado-3.webp?v=3", "o-bape-morado-4.webp?v=3"]},
  {id:"n21", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x BAPE · Camo Verde", price:null, imgs:["o-bape-verde-1.webp?v=3"]},
  {id:"n22", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs x Sanrio · Rosado", price:null, imgs:["o-sanrio-rosado-1.webp?v=3", "o-sanrio-rosado-2.webp?v=3"]},
  {id:"n23", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs Crocband · Negro", price:null, imgs:["o-crocband-negro-1.webp?v=3", "o-crocband-negro-2.webp?v=3", "o-crocband-negro-3.webp?v=3"]},
  {id:"n24", cat:"crocs", sizes:CROCS_SIZES, name:"Crocs Crocband · Blanco", price:null, imgs:["o-crocband-blanco-1.webp?v=3", "o-crocband-blanco-2.webp?v=3", "o-crocband-blanco-3.webp?v=3"]}
];

/* Carrusel 3D: solo Crocs, primero las Spider-Man black y red */
const FEATURED = ["ks1", "ks2", "ks3", "k2", "k1", "k5", "k8", "k12", "k7", "n4"];

/* ========================================================= */
const $ = s => document.querySelector(s);
const fmt = n => "S/ " + n.toLocaleString("es-PE",{minimumFractionDigits:n%1?2:0,maximumFractionDigits:2});
const priceHTML = (p,short) => p.price==null ? `<span class="ask">${short?"A consultar":"Consultar precio"}</span>` : `${fmt(p.price)}${p.old?`<s>${fmt(p.old)}</s>`:""}`;
const catName = c => (CATS.find(x=>x.id===c)||{}).name||"";
const RM = matchMedia("(prefers-reduced-motion: reduce)");
const byId = id => PRODUCTS.find(p => p.id === id);
const esc = s => String(s).replace(/[&<>"]/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c]));

const ICON_SHOE = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="9" cy="10" r="1.6"/><path d="M21 16l-5-5-8 8"/></svg>';
function photo(p, small){
  const im = p.imgs||[];
  if (im.length) return `<div class="ph has"><img src="${esc(im[0])}" alt="${esc(p.name)}" loading="lazy">${!small&&im[1]?`<img class="alt" src="${esc(im[1])}" alt="" loading="lazy">`:""}</div>`;
  if (p.tone){ const t=p.name.replace(/^Crocs (x )?/,'').split(' · '); const light=/^#f/i.test(p.tone);
    return `<div class="ph tone${light?'':' dk'}" style="--tone:${p.tone}">${small?'':`<span class="tl">Crocs</span><span class="tn">${esc(t[0])}</span>${t[1]?`<span class="ts">${esc(t[1])}</span>`:''}`}</div>`; }
  return `<div class="ph"><div class="tag">${ICON_SHOE}${small?'':'<span class="label">Foto próximamente</span>'}</div></div>`;
}

{ const sp=$("#statProducts"); if(sp) sp.textContent = PRODUCTS.filter(p=>p.cat==="crocs").length; }
$("#waText").textContent = "+51 912 762 364";
$("#askLink").href = "https://wa.me/" + WHATSAPP + "?text=" + encodeURIComponent("Hola Spiderman Store, busco un modelo: ");

/* ---------- Web canvas ---------- */
(function web(){
  const c = $("#web"), x = c.getContext("2d");
  function draw(){
    const r = c.getBoundingClientRect(), d = Math.min(window.devicePixelRatio||1, 2);
    c.width = r.width*d; c.height = r.height*d; x.setTransform(d,0,0,d,0,0);
    x.clearRect(0,0,r.width,r.height);
    const anchors = [[r.width*0.92, -20, 34, 1.0],[ -30, r.height*0.15, 26, .7]];
    anchors.forEach(([cx,cy,spokes,a])=>{
      const R = Math.max(r.width, r.height)*0.75*a;
      x.strokeStyle = "rgba(201,201,211,.16)"; x.lineWidth = 1;
      const ang = []; for(let i=0;i<spokes;i++) ang.push(i/spokes*Math.PI*2 + (i%3)*0.02);
      ang.forEach(t=>{ x.beginPath(); x.moveTo(cx,cy); x.lineTo(cx+Math.cos(t)*R, cy+Math.sin(t)*R); x.stroke(); });
      for(let ring=1; ring<24; ring++){
        const rr = ring*ring*1.6 + ring*16;
        if (rr > R) break;
        x.beginPath();
        ang.forEach((t,i)=>{
          const sag = 0.92;
          const p1 = [cx+Math.cos(t)*rr, cy+Math.sin(t)*rr];
          const t2 = ang[(i+1)%ang.length], p2 = [cx+Math.cos(t2)*rr, cy+Math.sin(t2)*rr];
          const tm = (t + (i+1===ang.length ? t2+Math.PI*2 : t2))/2;
          const pm = [cx+Math.cos(tm)*rr*sag, cy+Math.sin(tm)*rr*sag];
          if (i===0) x.moveTo(p1[0],p1[1]);
          x.quadraticCurveTo(pm[0],pm[1],p2[0],p2[1]);
        });
        x.stroke();
      }
    });
  }
  draw(); let t; addEventListener("resize",()=>{clearTimeout(t);t=setTimeout(draw,120)});
})();

/* ---------- 3D Carousel ---------- */
const ring = $("#ring"), stage = $("#stage"), dotsEl = $("#dots");
/* Mientras las Crocs no tengan foto, el carrusel muestra los productos con foto */
let feat = FEATURED.map(byId).filter(Boolean);
const N = feat.length, STEP = 360/N;
let cur = 0, timer = null, paused = false;
ring.innerHTML = feat.map((p,i)=>`
  <article class="slide" data-i="${i}" aria-label="${esc(p.name)}">
    <div class="spin"><img src="${esc(p.imgs[0])}" alt="${esc(p.name)}" draggable="false"></div>
    <div class="shadow"></div>
  </article>`).join("");
const capEl=$("#featCap");
dotsEl.innerHTML = feat.map((p,i)=>`<button aria-label="Ir a ${esc(p.name)}" data-d="${i}"></button>`).join("");

function layout(){
  const w = innerWidth < 600 ? 250 : 360;
  ring.style.setProperty("--cw", w+"px");
  const radius = Math.round((w/2) / Math.tan(Math.PI/N)) + (innerWidth<600 ? 20 : 60);
  ring.dataset.r = radius;
  [...ring.children].forEach((s,i)=> s.style.transform = `rotateY(${i*STEP}deg) translateZ(${radius}px)`);
  spin.silent = true; spin(); spin.silent = false;
}
/* Telaraña que cae sobre la pantalla en cada cambio */
const shot = $("#shot"), sx2 = shot.getContext("2d");
let webRaf = null, firstSpin = true;
function webShot(){
  if (RM.matches) return;
  cancelAnimationFrame(webRaf);
  const r = shot.getBoundingClientRect(), d = Math.min(devicePixelRatio||1,2);
  shot.width = r.width*d; shot.height = r.height*d; sx2.setTransform(d,0,0,d,0,0);
  const W = r.width, H = r.height;
  const cx = W/2 + (Math.random()-.5)*Math.min(W*.35,260), cy = H*.42 + (Math.random()-.5)*60;
  const R = Math.min(Math.max(W,H)*.62, 520);
  const spokes = 16, ang = [];
  for (let i=0;i<spokes;i++) ang.push(i/spokes*Math.PI*2 + (Math.random()-.5)*.18);
  const len = ang.map(()=> R*(.75+Math.random()*.35));
  const t0 = performance.now(), DROP=260, OPEN=420, HOLD=650, FADE=520;
  function frame(now){
    const t = now - t0;
    sx2.clearRect(0,0,W,H);
    let alpha = 1;
    if (t > DROP+OPEN+HOLD) alpha = Math.max(0, 1-(t-DROP-OPEN-HOLD)/FADE);
    sx2.globalAlpha = alpha;
    sx2.lineCap = "round";
    // hilo que cae desde arriba
    const dropK = Math.min(1, t/DROP), ease = 1-Math.pow(1-dropK,3);
    const hy = -10 + (cy+10)*ease;
    sx2.strokeStyle = "rgba(243,243,246,.9)"; sx2.lineWidth = 1.4;
    sx2.beginPath(); sx2.moveTo(cx,-10); sx2.lineTo(cx,hy); sx2.stroke();
    if (t > DROP){
      const k = Math.min(1,(t-DROP)/OPEN), e = 1-Math.pow(1-k,4);
      // destello de impacto
      if (k < 1){ sx2.fillStyle = `rgba(229,18,47,${.35*(1-k)})`; sx2.beginPath(); sx2.arc(cx,cy,40+120*e,0,Math.PI*2); sx2.fill(); }
      // radios
      sx2.strokeStyle = "rgba(243,243,246,.85)"; sx2.lineWidth = 1.3;
      ang.forEach((a,i)=>{ sx2.beginPath(); sx2.moveTo(cx,cy); sx2.lineTo(cx+Math.cos(a)*len[i]*e, cy+Math.sin(a)*len[i]*e); sx2.stroke(); });
      // anillos con caída
      sx2.lineWidth = 1;
      for (let ring=1; ring<=9; ring++){
        const rr = (ring/9)*R*.9*e; if (rr < 6) continue;
        sx2.strokeStyle = `rgba(243,243,246,${.75 - ring*.05})`;
        sx2.beginPath();
        ang.forEach((a,i)=>{
          const a2 = ang[(i+1)%spokes] + (i+1===spokes?Math.PI*2:0);
          const r1 = Math.min(rr,len[i]*e), r2 = Math.min(rr,len[(i+1)%spokes]*e);
          const p1=[cx+Math.cos(a)*r1, cy+Math.sin(a)*r1], p2=[cx+Math.cos(a2)*r2, cy+Math.sin(a2)*r2];
          const am=(a+a2)/2, rm=Math.min(r1,r2)*.86;
          if(i===0) sx2.moveTo(p1[0],p1[1]);
          sx2.quadraticCurveTo(cx+Math.cos(am)*rm, cy+Math.sin(am)*rm, p2[0], p2[1]);
        });
        sx2.stroke();
      }
    }
    sx2.globalAlpha = 1;
    if (t < DROP+OPEN+HOLD+FADE) webRaf = requestAnimationFrame(frame); else sx2.clearRect(0,0,W,H);
  }
  webRaf = requestAnimationFrame(frame);
}
function spin(){
  if (firstSpin) firstSpin = false;
  ring.style.transform = `translateZ(${-ring.dataset.r}px) rotateY(${-cur*STEP}deg)`;
  const idx = ((cur % N) + N) % N;
  [...ring.children].forEach((s,i)=> s.classList.toggle("on", i===idx));
  if (typeof capEl!=="undefined" && capEl){ const p=feat[idx]; capEl.innerHTML=`<span class="label">${catName(p.cat)}</span><h3>${esc(p.name)}</h3><div class="cap-row"><span class="price">${priceHTML(p)}</span><button class="add" data-view="${p.id}">Ver producto</button></div><p class="tip">Toca la Croc para verla de cerca</p>`; }
  [...dotsEl.children].forEach((d,i)=>{ d.classList.remove("on"); if(i===idx){ void d.offsetWidth; d.classList.add("on"); } });
}
function go(n){ cur = n; spin(); restart(); }
let spinning = false;
function restart(){ clearInterval(timer); timer = setInterval(()=>{ if(!paused && !spinning){ cur++; spin(); } }, 3000); }
$("#featCap").addEventListener("click",e=>{ const v=e.target.closest("[data-view]"); if(v) openQuick(v.dataset.view); });
$("#prev").onclick = ()=> go(cur-1);
$("#next").onclick = ()=> go(cur+1);
dotsEl.onclick = e => { const d = e.target.closest("[data-d]"); if(!d) return; const target = +d.dataset.d, idx=((cur%N)+N)%N; let diff = target-idx; if(diff>N/2)diff-=N; if(diff<-N/2)diff+=N; go(cur+diff); };
[stage, $("#ctrls")].forEach(el=>{
  el.addEventListener("mouseenter",()=>{paused=true;document.querySelector(".hero").classList.add("paused")});
  el.addEventListener("mouseleave",()=>{paused=false;document.querySelector(".hero").classList.remove("paused")});
});
let sx=null; stage.addEventListener("touchstart",e=>sx=e.touches[0].clientX,{passive:true});
stage.addEventListener("touchend",e=>{ if(sx===null)return; const dx=e.changedTouches[0].clientX-sx; if(Math.abs(dx)>40) go(cur+(dx<0?1:-1)); sx=null; });
ring.addEventListener("click",e=>{
  const v = e.target.closest("[data-view]");
  if (v){ openQuick(v.dataset.view); return; }
  const s = e.target.closest(".slide"); if(!s) return;
  const target = +s.dataset.i, idx=((cur%N)+N)%N; let diff=target-idx; if(diff>N/2)diff-=N; if(diff<-N/2)diff+=N;
  if(diff){ go(cur+diff); return; }
  tapSpin(s);
});
/* Humo: partículas en canvas que salen de la base de la Croc */
const smoke = $("#smoke"), smx = smoke.getContext("2d");
const PUFF = (()=>{ // sprites pre-renderizados: gris y gris con toque rojo
  const mk = (r,g,b)=>{ const c=document.createElement("canvas"); c.width=c.height=64; const x=c.getContext("2d");
    const gr=x.createRadialGradient(32,32,0,32,32,32); gr.addColorStop(0,`rgba(${r},${g},${b},.55)`); gr.addColorStop(.45,`rgba(${r},${g},${b},.25)`); gr.addColorStop(1,`rgba(${r},${g},${b},0)`);
    x.fillStyle=gr; x.fillRect(0,0,64,64); return c; };
  return [mk(178,178,188), mk(150,150,160), mk(229,18,47)];
})();
let parts = [], smokeRaf = null, smokeEnd = 0, smokeLast = 0, emitBox = null;
function sizeSmoke(){
  const r = smoke.getBoundingClientRect(), d = Math.min(devicePixelRatio||1, 1.5);
  smoke.width = Math.round(r.width*d); smoke.height = Math.round(r.height*d); smx.setTransform(d,0,0,d,0,0);
}
function startSmoke(slide){
  if (RM.matches) return;
  const st = smoke.getBoundingClientRect(), im = slide.querySelector("img").getBoundingClientRect();
  emitBox = { x: im.left - st.left + im.width*.5, y: im.top - st.top + im.height*.8, w: im.width*.62 };
  sizeSmoke();
  smokeEnd = performance.now() + 1700; // emite mientras gira
  if (!smokeRaf){ smokeLast = performance.now(); smokeRaf = requestAnimationFrame(smokeFrame); }
}
function smokeFrame(now){
  const dt = Math.min(48, now - smokeLast) / 1000; smokeLast = now;
  const W = smoke.width, H = smoke.height;
  smx.clearRect(0,0,W,H);
  if (now < smokeEnd && parts.length < 90){
    const n = Math.round(60*dt*1.4) || 1;
    for (let i=0;i<n;i++){
      const red = Math.random() < .18;
      parts.push({ x: emitBox.x + (Math.random()-.5)*emitBox.w, y: emitBox.y + Math.random()*10,
        vx: (Math.random()-.5)*28, vy: -(40+Math.random()*55), r: 14+Math.random()*14, gr: 26+Math.random()*30,
        life: 0, max: 1.6+Math.random()*.6, spr: red ? PUFF[2] : PUFF[Math.random()<.5?0:1], a: red ? .4 : .6 });
    }
  }
  for (let i=parts.length-1;i>=0;i--){
    const p = parts[i]; p.life += dt;
    if (p.life >= p.max){ parts.splice(i,1); continue; }
    p.x += p.vx*dt; p.y += p.vy*dt; p.vy *= .985; p.r += p.gr*dt;
    const k = p.life/p.max, alpha = p.a * (k < .15 ? k/.15 : 1 - (k-.15)/.85);
    smx.globalAlpha = alpha; smx.drawImage(p.spr, p.x-p.r, p.y-p.r, p.r*2, p.r*2);
  }
  smx.globalAlpha = 1;
  if (parts.length || now < smokeEnd) smokeRaf = requestAnimationFrame(smokeFrame);
  else { smokeRaf = null; smx.clearRect(0,0,W,H); }
}
function tapSpin(s){
  const sp = s.querySelector(".spin");
  if (spinning) return;
  const cls = RM.matches ? "fade" : "go";
  spinning = true; document.querySelector(".hero").classList.add("paused");
  sp.classList.remove("go","fade"); void sp.offsetWidth; sp.classList.add(cls); if (cls==="go"){ s.classList.add("zooming"); ring.classList.add("zoom"); }
  sp.addEventListener("animationend", function done(e){
    if (e.target !== sp) return;
    sp.removeEventListener("animationend", done);
    sp.classList.remove(cls); s.classList.remove("zooming"); ring.classList.remove("zoom"); spinning = false;
    if (!paused) document.querySelector(".hero").classList.remove("paused");
    restart();
  });
}
addEventListener("resize", layout);
layout(); restart();

/* ---------- Catalog ---------- */
let fCat = "crocs";
const picked = {};
$("#tabs").innerHTML = CATS.map(c=>`<button role="tab" data-cat="${c.id}">${c.name}</button>`).join("");
const needsSize = p => p.sizes && p.sizes.length;
const off = p => p.old && p.price ? Math.round((1-p.price/p.old)*100) : 0;

function render(){
  document.querySelectorAll("#tabs button").forEach(b=>{ const on=b.dataset.cat===fCat; b.classList.toggle("on",on); b.setAttribute("aria-selected",on); });
  const list = PRODUCTS.filter(p => fCat==="todo"||p.cat===fCat)
    .map((p,i)=>({p,i})).sort((a,b)=> ((b.p.imgs.length?1:0)-(a.p.imgs.length?1:0)) || a.i-b.i).map(x=>x.p);
  $("#catTitle").textContent = fCat==="crocs" ? "Crocs" : catName(fCat);
  $("#count").textContent = list.length + (list.length===1?" producto":" productos");
  $("#grid").innerHTML = list.map(p=>`
    <article class="card" data-id="${p.id}">
      <button class="pic" data-quick aria-label="Ver fotos de ${esc(p.name)}">
        ${off(p)?`<span class="badge">-${off(p)}%</span>`:""}${p.badge?`<span class="badge alt r">${esc(p.badge)}</span>`:""}
        ${photo(p)}
        ${p.imgs.length>1?`<span class="angles">${p.imgs.length} ángulos</span>`:""}
      </button>
      <div class="body">
        <div class="meta"><span class="label">${catName(p.cat)}</span></div>
        <h3>${esc(p.name)}</h3>
        ${p.info?`<p class="info">${esc(p.info)}</p>`:""}
        ${needsSize(p)?`<div class="sizes" aria-label="Talla">${p.sizes.map(s=>`<button data-size="${s}" class="${picked[p.id]===s?'on':''}">${sizeChip(s)}</button>`).join("")}</div>`:""}
        <p class="hint" aria-live="polite"></p>
        <div class="foot"><span class="price">${priceHTML(p,true)}</span><button class="add" data-add>${p.price==null?"Consultar":"Agregar"}</button></div>
      </div>
    </article>`).join("");
}
function setFilter(cat){ fCat = cat||"crocs"; render(); $("#catalogo").scrollIntoView({behavior: RM.matches?"auto":"smooth"}); }
$("#tabs").onclick = e => { const b=e.target.closest("[data-cat]"); if(b){ fCat=b.dataset.cat; render(); } };
document.querySelectorAll("[data-go-cat]").forEach(b=> b.onclick = ()=> setFilter(b.dataset.goCat));
const BASE = /^https?:/.test(location.protocol) ? location.origin + location.pathname : "";
const prodLink = p => BASE ? BASE + "#p-" + p.id : "";
const photoLink = p => (BASE && p.imgs.length) ? new URL(p.imgs[0], BASE).href : "";
function itemLines(p){ return [prodLink(p)&&`  Ver producto: ${prodLink(p)}`, photoLink(p)&&`  Foto: ${photoLink(p)}`].filter(Boolean).join("\n"); }
function waLink(text){ return "https://wa.me/"+WHATSAPP+"?text="+encodeURIComponent(text); }
function askLink(p){ return waLink(`Hola Spiderman Store, quiero el precio de: ${p.name}\n${itemLines(p)}`); }
function buyLink(p, size){ return waLink(`Hola Spiderman Store, quiero comprar:\n• ${p.name}${p.cat==="crocs"&&size?` (talla ${sizeText(size)})`:""} — ${p.price!=null?fmt(p.price):"precio a consultar"}\n${itemLines(p)}`); }
function tryAdd(id, el, hintEl){
  const p = byId(id);
  if (p.price==null) return true; /* el botón es un enlace a WhatsApp */
  if (needsSize(p) && !picked[id]){ if(hintEl) hintEl.textContent="Elige tu talla"; if(el){el.classList.remove("need"); void el.offsetWidth; el.classList.add("need");} return false; }
  addToCart(id, picked[id] || (p.cat==="crocs"?"por confirmar":"única"));
  return true;
}
$("#grid").onclick = e => {
  const card = e.target.closest(".card"); if(!card) return;
  const id = card.dataset.id;
  if (e.target.closest("[data-quick]")){ openQuick(id); return; }
  const sz = e.target.closest("[data-size]");
  if (sz){ picked[id] = sz.dataset.size; card.querySelectorAll("[data-size]").forEach(b=>b.classList.toggle("on", b===sz)); card.querySelector(".hint").textContent=""; return; }
  const add = e.target.closest("[data-add]");
  if (add){ const p=byId(id); if(p.price!=null && needsSize(p) && !picked[id]){ openQuick(id); return; } if(p.price==null){ const a=document.createElement("a"); a.href=askLink(p); a.target="_blank"; a.rel="noopener"; document.body.appendChild(a); a.click(); a.remove(); return; } tryAdd(id, add, card.querySelector(".hint")); }
};

/* ---------- Quick view (galería de ángulos) ---------- */
let qId=null, qIdx=0, qReturn=null;
function openQuick(id){
  const p=byId(id); if(!p) return; qReturn=document.activeElement; qId=id; qIdx=0;
  $("#qName").textContent=p.name; $("#qCat").textContent=catName(p.cat);
  $("#qPrice").innerHTML=priceHTML(p); $("#qInfo").textContent=p.info||"";
  $("#qSizes").innerHTML = needsSize(p)? p.sizes.map(s=>`<button data-size="${s}" class="${picked[id]===s?'on':''}">${sizeChip(s)}</button>`).join("") : "";
  $("#qSizes").hidden = !needsSize(p);
  $("#qAdd").hidden = p.price==null; $("#qAsk").hidden = p.price!=null; $("#qAsk").href = askLink(p);
  $("#qBuy").hidden = p.price==null; $("#qBuy").href = buyLink(p, picked[id]);
  try{ history.replaceState(null,"","#p-"+id); }catch(e){}
  $("#qHint").textContent="";
  $("#qThumbs").innerHTML = p.imgs.length>1 ? p.imgs.map((src,i)=>`<button data-qi="${i}" aria-label="Ángulo ${i+1}"><img src="${esc(src)}" alt=""></button>`).join("") : "";
  showQ(); $("#quick").hidden=false; document.body.style.overflow="hidden"; $("#qClose").focus();
}
function showQ(){
  const p=byId(qId);
  $("#qMain").innerHTML = p.imgs.length ? `<img src="${esc(p.imgs[qIdx])}" alt="${esc(p.name)} · ángulo ${qIdx+1}">` : p.tone ? photo(p).replace(/^<div class="ph tone([^"]*)"([^>]*)>/,'<div class="tone$1 qtone"$2>') : `<div class="tag">${ICON_SHOE}<span class="label">Foto próximamente</span></div>`;
  $("#qMain").classList.toggle("has", !!p.imgs.length);
  [...$("#qThumbs").children].forEach((b,i)=>b.classList.toggle("on",i===qIdx));
  $("#qPrev").hidden = $("#qNext").hidden = p.imgs.length<2;
}
function closeQuick(){ if($("#quick").hidden) return; $("#quick").hidden=true; document.body.style.overflow=""; if(qReturn&&qReturn.focus) qReturn.focus(); try{ if(location.hash.startsWith("#p-")) history.replaceState(null,"",location.pathname+location.search); }catch(e){} }
function fromHash(){ const m=location.hash.match(/^#p-([a-z0-9]+)$/); if(m && byId(m[1])) openQuick(m[1]); }
addEventListener("hashchange", fromHash);
$("#qClose").onclick=closeQuick;
$("#quick").addEventListener("click",e=>{ if(e.target.id==="quick") closeQuick(); });
$("#quick").addEventListener("keydown",e=>{
  if(e.key!=="Tab") return;
  const f=[...$("#quick").querySelectorAll("button,a[href]")].filter(x=>!x.hidden && x.offsetParent!==null);
  if(!f.length) return; const first=f[0], last=f[f.length-1];
  if(e.shiftKey && document.activeElement===first){ e.preventDefault(); last.focus(); }
  else if(!e.shiftKey && document.activeElement===last){ e.preventDefault(); first.focus(); }
});
$("#qPrev").onclick=()=>{ const n=byId(qId).imgs.length; qIdx=(qIdx-1+n)%n; showQ(); };
$("#qNext").onclick=()=>{ const n=byId(qId).imgs.length; qIdx=(qIdx+1)%n; showQ(); };
$("#qThumbs").onclick=e=>{ const b=e.target.closest("[data-qi]"); if(b){ qIdx=+b.dataset.qi; showQ(); } };
$("#qSizes").onclick=e=>{ const b=e.target.closest("[data-size]"); if(!b) return; picked[qId]=b.dataset.size; [...$("#qSizes").children].forEach(x=>x.classList.toggle("on",x===b)); $("#qHint").textContent=""; $("#qBuy").href=buyLink(byId(qId),picked[qId]); render(); };
$("#qAdd").onclick=()=>{ if(tryAdd(qId, $("#qAdd"), $("#qHint"))) closeQuick(); };
render();
fromHash();

/* ---------- Cart ---------- */
let cart = [];
try {
  const raw = JSON.parse(localStorage.getItem("sm-cart")||"[]");
  cart = (Array.isArray(raw)?raw:[]).filter(l=>{
    const p = l && typeof l.id==="string" && byId(l.id); if(!p || p.price==null) return false;
    l.q = Math.min(20, Math.max(1, parseInt(l.q,10)||1));
    const okSize = needsSize(p) ? p.sizes.includes(l.size) : (l.size==="única");
    return okSize;
  }).slice(0,50);
} catch(e){ cart = []; }
function save(){ try{ localStorage.setItem("sm-cart", JSON.stringify(cart)); }catch(e){} }
function addToCart(id, size){
  const l = cart.find(x=>x.id===id && x.size===size);
  l ? (l.q=Math.min(20,l.q+1)) : cart.push({id,size,q:1});
  save(); drawCart(); toast(`Agregado: ${byId(id).name}${byId(id).cat==="crocs"?` · ${sizeText(size)}`:""}`);
}
function drawCart(){
  const n = cart.reduce((a,l)=>a+l.q,0), tot = cart.reduce((a,l)=>a+l.q*(byId(l.id).price||0),0);
  $("#cartN").textContent = n;
  $("#dTotal").textContent = fmt(tot);
  $("#dList").innerHTML = cart.length ? cart.map((l,i)=>{ const p=byId(l.id); return `
    <div class="line">${photo(p,true)}
      <div><h4>${esc(p.name)}</h4><small>${p.cat==='crocs'?'Talla '+esc(sizeText(l.size)):catName(p.cat)}</small>
        <div class="qty"><button data-q="-1" data-i="${i}" aria-label="Quitar uno">−</button><span>${l.q}</span><button data-q="1" data-i="${i}" aria-label="Agregar uno">+</button></div></div>
      <div class="lp">${fmt(p.price*l.q)}</div></div>`; }).join("")
    : `<div class="d-empty">Tu carrito está vacío.<br>Elige tu talla y agrega tus favoritos.</div>`;
  const wa = $("#waBtn");
  if (cart.length){
    const msg = "Hola Spiderman Store, quiero hacer este pedido:\n" +
      cart.map(l=>{const p=byId(l.id);const x=itemLines(p);return `• ${l.q} x ${p.name}${p.cat==='crocs'?` (talla ${sizeText(l.size)})`:''} — ${fmt(p.price*l.q)}`+(x?`\n${x}`:'')}).join("\n") +
      `\nTotal: ${fmt(tot)}`;
    wa.href = "https://wa.me/"+WHATSAPP+"?text="+encodeURIComponent(msg);
    wa.setAttribute("aria-disabled","false");
  } else { wa.href="#"; wa.setAttribute("aria-disabled","true"); }
}
$("#dList").onclick = e => { const b=e.target.closest("[data-q]"); if(!b) return; const l=cart[+b.dataset.i]; l.q=Math.min(20,l.q+(+b.dataset.q)); if(l.q<=0) cart.splice(+b.dataset.i,1); save(); drawCart(); };
function openCart(o){ $("#drawer").classList.toggle("on",o); $("#scrim").classList.toggle("on",o); $("#drawer").setAttribute("aria-hidden",!o); if(o) $("#closeCart").focus(); }
$("#openCart").onclick = ()=>openCart(true);
$("#closeCart").onclick = ()=>openCart(false);
$("#scrim").onclick = ()=>openCart(false);
addEventListener("keydown", e=>{ if(e.key==="Escape"){ openCart(false); closeQuick(); } });
let tt; function toast(m){ const t=$("#toast"); t.textContent=m; t.classList.add("on"); clearTimeout(tt); tt=setTimeout(()=>t.classList.remove("on"),2200); }
drawCart();

/* Volver arriba */
(function(){ const b=document.createElement("button"); b.className="to-top"; b.type="button"; b.setAttribute("aria-label","Volver arriba");
  b.innerHTML='<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M6 15l6-6 6 6"/></svg>';
  b.onclick=()=>scrollTo({top:0,behavior:RM.matches?"auto":"smooth"}); document.body.appendChild(b);
  const t=()=>b.classList.toggle("on", scrollY>innerHeight*0.8); addEventListener("scroll",t,{passive:true}); t(); })();
