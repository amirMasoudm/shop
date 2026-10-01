const assert=require('node:assert/strict');const R=require('../web/rf.js');
const near=(a,b,t=1e-9)=>assert.ok(Math.abs(a-b)<t,`${a} != ${b}`);
near(R.fspl(5.5,20),133.276,0.001);
for(const [f,d,g] of [[5,60,28.5],[5,100,33],[5,120,34.5],[8,60,32.5],[60,60,50]])near(R.roundedGain(f,d),g);
for(let f=2;f<=80;f+=.1){const g=R.oxygen(f);assert.ok(Number.isFinite(g)&&g>0);const gain=R.roundedGain(f,60);assert.ok(gain<=R.gain(f,60)+1e-9);near(gain*2,Math.round(gain*2));}
const s={f:5.5,distance:20,tx:20,sensitivity:-75,gainA:32,gainB:32,diaA:60,diaB:60,modeA:'manual',modeB:'manual',lossA:2,lossB:2,pressure:1013.25,temp:15};
const r=R.calculate(s);near(r.eirp,50);near(r.margin,r.rx+75);near(R.calculate({...s,tx:23}).rx-r.rx,3);near(R.calculate({...s,lossA:3}).rx-r.rx,-1);
near(R.calculate({...s,distance:40}).rx-r.rx,-20*Math.log10(2)-20*R.oxygen(s.f));
near(R.calculate({...s,gainA:28,gainB:36}).rx,r.rx);
assert.ok(R.oxygen(60)>14&&R.oxygen(60)<16);assert.ok(R.oxygen(60)>100*R.oxygen(5));
console.log('RF tests passed; fixture RX',r.rx,'dBm; oxygen at 60 GHz',R.oxygen(60),'dB/km');

near(R.calculate(s).fresnel,16.508743083039914);near(R.calculate({...s,distance:80}).fresnel,2*R.calculate(s).fresnel);
