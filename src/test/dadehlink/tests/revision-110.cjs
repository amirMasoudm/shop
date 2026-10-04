const assert=require('node:assert/strict'),R=require('../web/rf.js'),ref=require('./reference.json');
let checks=0;function near(a,b,t=1e-7){assert.ok(Math.abs(a-b)<=t,`${a} != ${b}`);checks++}
for(const x of ref.gases){const g=R.gases(x.f,x.pressure,x.temp,x.rh);near(g.oxygen,x.oxygen);near(g.vapour,x.vapour)}
for(const x of ref.rain)near(R.rainSpecific(x.f,x.rate,x.pol),x.value);
for(const [r,v,h]of [[10,16.13934404,17.83972881],[20,26.48721505,30.32221479],[40,44.28227360,52.54922535]]){near(R.rainAttenuation(23,20,r,'V'),v);near(R.rainAttenuation(23,20,r,'H'),h)}
near(R.gain(23,60),10*Math.log10(.75*(Math.PI*.6*23e9/299792458)**2));
for(const f of [2,5.5,6,23,60,80])for(const d of [.01,.1,1,5,20,30,60,110])for(const r of [0,1,3,10,20,40,150])for(const pol of ['H','V']){const x=R.rainAttenuation(f,d,r,pol);assert.ok(Number.isFinite(x)&&x>=0);checks++}
for(let f=2;f<=80;f+=.5){const g=R.roundedGain(f,60);assert.ok(g<=R.gain(f,60)+1e-9&&R.gain(f,60)-g<.5000001);checks++}
const s={f:23,distance:20,tx:20,sensitivity:-75,gainA:32,gainB:32,diaA:60,diaB:60,modeA:'manual',modeB:'manual',lossA:0,lossB:0,pressure:1013.25,temp:15,humidity:50,rainRate:40,rainEnabled:true,weatherEnabled:true,polarization:'H'};
const a=R.calculate(s);near(a.total,a.oxygenLoss+a.vapour+a.rain);near(a.margin,a.rx-s.sensitivity);near(R.calculate({...s,weatherEnabled:false}).rx,a.dryRx);near(R.calculate({...s,humidity:0,rainEnabled:false}).rx,a.dryRx);near(R.calculate({...s,lossA:1}).rx,a.rx-1);near(R.rainAttenuation(23,0,40),0);near(R.rainAttenuation(23,20,0),0);
for(const f of [2,5.5,23,60,80])for(const temp of [-50,15,60])for(const pressure of [300,1013.25,1100])for(const humidity of [0,50,100]){const v=R.calculate({...s,f,temp,pressure,humidity});assert.ok(Object.values(v).every(Number.isFinite));checks++}
console.log(JSON.stringify({checks,example:a},null,2));
