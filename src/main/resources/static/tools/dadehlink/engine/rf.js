/*
Copyright (c) 2016 Inigo del Portillo, Massachusetts Institute of Technology

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
/* DedehLink RF core. ITU-R P.676-13 Annex 1 dry-air component.
   Oxygen spectral line coefficients: ITU-Rpy (MIT), see bundled license.
   P.676-13 gas and P.838-3 rain + P.530-19 A0.01. No total link availability prediction. */
const RF = (()=> {
const C=299792458;
const OXYGEN_LINES=[[50.474214, 0.975, 9.651, 6.69, 0.0, 2.566, 6.85], [50.987745, 2.529, 8.653, 7.17, 0.0, 2.246, 6.8], [51.50336, 6.193, 7.709, 7.64, 0.0, 1.947, 6.729], [52.021429, 14.32, 6.819, 8.11, 0.0, 1.667, 6.64], [52.542418, 31.24, 5.983, 8.58, 0.0, 1.388, 6.526], [53.066934, 64.29, 5.201, 9.06, 0.0, 1.349, 6.206], [53.595775, 124.6, 4.474, 9.55, 0.0, 2.227, 5.085], [54.130025, 227.3, 3.8, 9.96, 0.0, 3.17, 3.75], [54.67118, 389.7, 3.182, 10.37, 0.0, 3.558, 2.654], [55.221384, 627.1, 2.618, 10.89, 0.0, 2.56, 2.952], [55.783815, 945.3, 2.109, 11.34, 0.0, -1.172, 6.135], [56.264774, 543.4, 0.014, 17.03, 0.0, 3.525, -0.978], [56.363399, 1331.8, 1.654, 11.89, 0.0, -2.378, 6.547], [56.968211, 1746.6, 1.255, 12.23, 0.0, -3.545, 6.451], [57.612486, 2120.1, 0.91, 12.62, 0.0, -5.416, 6.056], [58.323877, 2363.7, 0.621, 12.95, 0.0, -1.932, 0.436], [58.446588, 1442.1, 0.083, 14.91, 0.0, 6.768, -1.273], [59.164204, 2379.9, 0.387, 13.53, 0.0, -6.561, 2.309], [59.590983, 2090.7, 0.207, 14.08, 0.0, 6.957, -0.776], [60.306056, 2103.4, 0.207, 14.15, 0.0, -6.395, 0.699], [60.434778, 2438.0, 0.386, 13.39, 0.0, 6.342, -2.825], [61.150562, 2479.5, 0.621, 12.92, 0.0, 1.014, -0.584], [61.800158, 2275.9, 0.91, 12.63, 0.0, 5.014, -6.619], [62.41122, 1915.4, 1.255, 12.17, 0.0, 3.029, -6.759], [62.486253, 1503.0, 0.083, 15.13, 0.0, -4.499, 0.844], [62.997984, 1490.2, 1.654, 11.74, 0.0, 1.856, -6.675], [63.568526, 1078.0, 2.108, 11.34, 0.0, 0.658, -6.139], [64.127775, 728.7, 2.617, 10.88, 0.0, -3.036, -2.895], [64.67891, 461.3, 3.181, 10.38, 0.0, -3.968, -2.59], [65.224078, 274.0, 3.8, 9.96, 0.0, -3.528, -3.68], [65.764779, 153.0, 4.473, 9.55, 0.0, -2.548, -5.002], [66.302096, 80.4, 5.2, 9.06, 0.0, -1.66, -6.091], [66.836834, 39.8, 5.982, 8.58, 0.0, -1.68, -6.393], [67.369601, 18.56, 6.818, 8.11, 0.0, -1.956, -6.475], [67.900868, 8.172, 7.708, 7.64, 0.0, -2.216, -6.545], [68.431006, 3.397, 8.652, 7.17, 0.0, -2.492, -6.6], [68.960312, 1.334, 9.65, 6.69, 0.0, -2.773, -6.65], [118.750334, 940.3, 0.01, 16.64, 0.0, -0.439, 0.079], [368.498246, 67.4, 0.048, 16.4, 0.0, 0.0, 0.0], [424.76302, 637.7, 0.044, 16.4, 0.0, 0.0, 0.0], [487.249273, 237.4, 0.049, 16.0, 0.0, 0.0, 0.0], [715.392902, 98.1, 0.145, 16.0, 0.0, 0.0, 0.0], [773.83949, 572.3, 0.141, 16.2, 0.0, 0.0, 0.0], [834.145546, 183.1, 0.145, 14.7, 0.0, 0.0, 0.0]];
function oxygen(f, pressure=1013.25, tempC=15, vapourPressure=0) {
 const t=300/(tempC+273.15), p=pressure, e=vapourPressure;
 let n=0;
 for(const [fi,a1,a2,a3,a4,a5,a6] of OXYGEN_LINES){
  const w=Math.sqrt((a3*1e-4*(p*Math.pow(t,.8-a4)+1.1*e*t))**2+2.25e-6);
  const delta=(a5+a6*t)*1e-4*(p+e)*Math.pow(t,.8);
  const shape=f/fi*((w-delta*(fi-f))/((fi-f)**2+w*w)+(w-delta*(fi+f))/((fi+f)**2+w*w));
  n+=a1*1e-7*p*t**3*Math.exp(a2*(1-t))*shape;
 }
 const d=5.6e-4*(p+e)*Math.pow(t,.8);
 n+=f*p*t*t*(6.14e-5/(d*(1+(f/d)**2))+1.4e-12*p*Math.pow(t,1.5)/(1+1.9e-5*Math.pow(f,1.5)));
 return Math.max(0,.182*f*n);
}
function gain(f,diameterCm,eta=.75){return 10*Math.log10(eta*(Math.PI*diameterCm/100*f*1e9/C)**2)}
function roundedGain(f,d){return Math.floor((gain(f,d)+1e-10)*2)/2}
function fspl(f,d){return 20*Math.log10(4*Math.PI*d*1000*f*1e9/C)}
// RH is relative to liquid water (also below freezing); Buck saturation formula.
function vapourPressure(tempC,rh){return rh/100*6.1121*Math.exp((18.678-tempC/234.5)*tempC/(257.14+tempC))}
const WATER_LINES=[[22.23508, 0.1079, 2.144, 26.38, 0.76, 5.087, 1.0], [67.80396, 0.0011, 8.732, 28.58, 0.69, 4.93, 0.82], [119.99594, 0.0007, 8.353, 29.48, 0.7, 4.78, 0.79], [183.310087, 2.273, 0.668, 29.06, 0.77, 5.022, 0.85], [321.22563, 0.047, 6.179, 24.04, 0.67, 4.398, 0.54], [325.152888, 1.514, 1.541, 28.23, 0.64, 4.893, 0.74], [336.227764, 0.001, 9.825, 26.93, 0.69, 4.74, 0.61], [380.197353, 11.67, 1.048, 28.11, 0.54, 5.063, 0.89], [390.134508, 0.0045, 7.347, 21.52, 0.63, 4.81, 0.55], [437.346667, 0.0632, 5.048, 18.45, 0.6, 4.23, 0.48], [439.150807, 0.9098, 3.595, 20.07, 0.63, 4.483, 0.52], [443.018343, 0.192, 5.048, 15.55, 0.6, 5.083, 0.5], [448.001085, 10.41, 1.405, 25.64, 0.66, 5.028, 0.67], [470.888999, 0.3254, 3.597, 21.34, 0.66, 4.506, 0.65], [474.689092, 1.26, 2.379, 23.2, 0.65, 4.804, 0.64], [488.490108, 0.2529, 2.852, 25.86, 0.69, 5.201, 0.72], [503.568532, 0.0372, 6.731, 16.12, 0.61, 3.98, 0.43], [504.482692, 0.0124, 6.731, 16.12, 0.61, 4.01, 0.45], [547.67644, 0.9785, 0.158, 26.0, 0.7, 4.5, 1.0], [552.02096, 0.184, 0.158, 26.0, 0.7, 4.5, 1.0], [556.935985, 497.0, 0.159, 30.86, 0.69, 4.552, 1.0], [620.700807, 5.015, 2.391, 24.38, 0.71, 4.856, 0.68], [645.766085, 0.0067, 8.633, 18.0, 0.6, 4.0, 0.5], [658.00528, 0.2732, 7.816, 32.1, 0.69, 4.14, 1.0], [752.033113, 243.4, 0.396, 30.86, 0.68, 4.352, 0.84], [841.051732, 0.0134, 8.177, 15.9, 0.33, 5.76, 0.45], [859.965698, 0.1325, 8.055, 30.6, 0.68, 4.09, 0.84], [899.303175, 0.0547, 7.914, 29.85, 0.68, 4.53, 0.9], [902.611085, 0.0386, 8.429, 28.65, 0.7, 5.1, 0.95], [906.205957, 0.1836, 5.11, 24.08, 0.7, 4.7, 0.53], [916.171582, 8.4, 1.441, 26.73, 0.7, 5.15, 0.78], [923.112692, 0.0079, 10.293, 29.0, 0.7, 5.0, 0.8], [970.315022, 9.009, 1.919, 25.5, 0.64, 4.94, 0.67], [987.926764, 134.6, 0.257, 29.85, 0.68, 4.55, 0.9], [1780.0, 17506.0, 0.952, 196.3, 2.0, 24.15, 5.0]];
function gases(f,pressure,tempC,rh){
 const e=vapourPressure(tempC,rh),p=pressure-e,t=300/(tempC+273.15);
 if(p<=0)throw new RangeError('Vapour pressure exceeds total pressure');
 let n=0;
 for(const [fi,b1,b2,b3,b4,b5,b6] of WATER_LINES){
  const w0=b3*1e-4*(p*t**b4+b5*e*t**b6);
  const w=.535*w0+Math.sqrt(.217*w0*w0+2.1316e-12*fi*fi/t);
  const shape=f/fi*(w/((fi-f)**2+w*w)+w/((fi+f)**2+w*w));
  n+=b1*.1*e*t**3.5*Math.exp(b2*(1-t))*shape;
 }
 return {oxygen:oxygen(f,p,tempC,e),vapour:Math.max(0,.182*f*n)};
}
// P.838-3 coefficient fits; horizontal terrestrial path, H or V polarization.
function rainCoefficients(f,pol='H'){
 const fits={
 kh:[[-5.33980,-.35351,-.23789,-.94158],[-.10008,1.26970,.86036,.64552],[1.13098,.454,.15354,.16817],-.18961,.71147],
 kv:[[-3.80595,-3.44965,-.39902,.50167],[.56934,-.22911,.73042,1.07319],[.81061,.51059,.11899,.27195],-.16398,.63297],
 ah:[[-.14318,.29591,.32177,-5.37610,16.1721],[1.82442,.77564,.63773,-.96230,-3.29980],[-.55187,.19822,.13164,1.47828,3.43990],.67849,-1.95537],
 av:[[-.07771,.56727,-.20238,-48.2991,48.5833],[2.33840,.95545,1.14520,.791669,.791459],[-.76284,.54039,.26809,.116226,.116479],-.053739,.83433]
 };
 const fit=key=>{const[a,b,c,m,z]=fits[key],x=Math.log10(f);return a.reduce((sum,v,i)=>sum+v*Math.exp(-(((x-b[i])/c[i])**2)),0)+m*x+z};
 const vertical=pol==='V',k=10**fit(vertical?'kv':'kh'),alpha=fit(vertical?'av':'ah');
 return {k,alpha};
}
function rainSpecific(f,rate,pol='H'){const {k,alpha}=rainCoefficients(f,pol);return rate===0?0:k*rate**alpha}
// P.530-19 (2025), section 2.4.1, equations 32-33. R is R0.01.
function rainAttenuation(f,d,rate,pol='H'){
 if(![f,d,rate].every(Number.isFinite)||f<=0||d<0||rate<0||!['H','V'].includes(pol))throw new RangeError('Invalid rain parameters');
 if(d===0||rate===0)return 0;
 const {alpha}=rainCoefficients(f,pol),dc=Math.min(d,30),rc=Math.max(rate,3),fc=Math.max(f,6);
 const denominator=.477*dc**.633*rc**(.073*alpha)*fc**.123-10.579*(1-Math.exp(-.024*dc));
 if(denominator<=0)throw new RangeError('Invalid effective path length');
 return rainSpecific(f,rate,pol)*d/denominator;
}
function calculate(s){
 const ga=s.modeA==='diameter'?roundedGain(s.f,s.diaA):s.gainA;
 const gb=s.modeB==='diameter'?roundedGain(s.f,s.diaB):s.gainB;
 const free=fspl(s.f,s.distance),specific=oxygen(s.f,s.pressure,s.temp),dryGas=specific*s.distance;
 const enabled=s.weatherEnabled===true;
 const atm=gases(s.f,s.pressure,s.temp,enabled?(s.humidity??50):0);
 const oxygenLoss=atm.oxygen*s.distance,vapour=atm.vapour*s.distance;
 const rain=enabled&&s.rainEnabled?rainAttenuation(s.f,s.distance,s.rainRate??0,s.polarization??'H'):0;
 const gas=oxygenLoss+vapour,total=gas+rain,base=s.tx+ga+gb-s.lossA-s.lossB-free;
 const dryRx=base-dryGas,rx=enabled?base-total:dryRx;
 return {ga,gb,free,specific,dryGas,gas,oxygenLoss,vapour,rain,total,rx,dryRx,
 fresnel:Math.sqrt(C/(s.f*1e9)*s.distance*1000/4),margin:rx-s.sensitivity,eirp:s.tx+ga-s.lossA};
}
return {gain,roundedGain,fspl,oxygen,gases,vapourPressure,rainSpecific,rainCoefficients,rainAttenuation,calculate};
})();
if(typeof module!=='undefined')module.exports=RF;
