var De=Object.create;var ft=Object.defineProperty;var Bt=Object.getOwnPropertyDescriptor;var Xe=Object.getOwnPropertyNames;var ze=Object.getPrototypeOf,Pe=Object.prototype.hasOwnProperty;var x=(s,t)=>()=>(t||s((t={exports:{}}).exports,t),t.exports);var Ye=(s,t,e,r)=>{if(t&&typeof t=="object"||typeof t=="function")for(let i of Xe(t))!Pe.call(s,i)&&i!==e&&ft(s,i,{get:()=>t[i],enumerable:!(r=Bt(t,i))||r.enumerable});return s};var Ut=(s,t,e)=>(e=s!=null?De(ze(s)):{},Ye(t||!s||!s.__esModule?ft(e,"default",{value:s,enumerable:!0}):e,s));var c=(s,t,e,r)=>{for(var i=r>1?void 0:r?Bt(t,e):t,o=s.length-1,n;o>=0;o--)(n=s[o])&&(i=(r?n(t,e,i):n(i))||i);return r&&i&&ft(t,e,i),i};var kt=x((Is,le)=>{function Ar(s){var t=typeof s;return s!=null&&(t=="object"||t=="function")}le.exports=Ar});var he=x((ks,ce)=>{var Cr=typeof global=="object"&&global&&global.Object===Object&&global;ce.exports=Cr});var qt=x((qs,de)=>{var $r=he(),Sr=typeof self=="object"&&self&&self.Object===Object&&self,wr=$r||Sr||Function("return this")();de.exports=wr});var ge=x((Ls,ue)=>{var Er=qt(),Ir=function(){return Er.Date.now()};ue.exports=Ir});var me=x((Rs,pe)=>{var kr=/\s/;function qr(s){for(var t=s.length;t--&&kr.test(s.charAt(t)););return t}pe.exports=qr});var be=x((Ts,fe)=>{var Lr=me(),Rr=/^\s+/;function Tr(s){return s&&s.slice(0,Lr(s)+1).replace(Rr,"")}fe.exports=Tr});var Lt=x((Ms,ve)=>{var Mr=qt(),Or=Mr.Symbol;ve.exports=Or});var Ae=x((Os,xe)=>{var ye=Lt(),_e=Object.prototype,Hr=_e.hasOwnProperty,Br=_e.toString,it=ye?ye.toStringTag:void 0;function Ur(s){var t=Hr.call(s,it),e=s[it];try{s[it]=void 0;var r=!0}catch{}var i=Br.call(s);return r&&(t?s[it]=e:delete s[it]),i}xe.exports=Ur});var $e=x((Hs,Ce)=>{var Nr=Object.prototype,Vr=Nr.toString;function Zr(s){return Vr.call(s)}Ce.exports=Zr});var Ie=x((Bs,Ee)=>{var Se=Lt(),Wr=Ae(),Gr=$e(),jr="[object Null]",Fr="[object Undefined]",we=Se?Se.toStringTag:void 0;function Dr(s){return s==null?s===void 0?Fr:jr:we&&we in Object(s)?Wr(s):Gr(s)}Ee.exports=Dr});var qe=x((Us,ke)=>{function Xr(s){return s!=null&&typeof s=="object"}ke.exports=Xr});var Re=x((Ns,Le)=>{var zr=Ie(),Pr=qe(),Yr="[object Symbol]";function Qr(s){return typeof s=="symbol"||Pr(s)&&zr(s)==Yr}Le.exports=Qr});var He=x((Vs,Oe)=>{var Jr=be(),Te=kt(),Kr=Re(),Me=NaN,ti=/^[-+]0x[0-9a-f]+$/i,ei=/^0b[01]+$/i,ri=/^0o[0-7]+$/i,ii=parseInt;function si(s){if(typeof s=="number")return s;if(Kr(s))return Me;if(Te(s)){var t=typeof s.valueOf=="function"?s.valueOf():s;s=Te(t)?t+"":t}if(typeof s!="string")return s===0?s:+s;s=Jr(s);var e=ei.test(s);return e||ri.test(s)?ii(s.slice(2),e?2:8):ti.test(s)?Me:+s}Oe.exports=si});var Tt=x((Zs,Ue)=>{var oi=kt(),Rt=ge(),Be=He(),ni="Expected a function",ai=Math.max,li=Math.min;function ci(s,t,e){var r,i,o,n,a,l,g=0,m=!1,u=!1,A=!0;if(typeof s!="function")throw new TypeError(ni);t=Be(t)||0,oi(e)&&(m=!!e.leading,u="maxWait"in e,o=u?ai(Be(e.maxWait)||0,t):o,A="trailing"in e?!!e.trailing:A);function $(f){var T=r,X=i;return r=i=void 0,g=f,n=s.apply(X,T),n}function W(f){return g=f,a=setTimeout(ot,t),m?$(f):n}function Ge(f){var T=f-l,X=f-g,Ht=t-T;return u?li(Ht,o-X):Ht}function Mt(f){var T=f-l,X=f-g;return l===void 0||T>=t||T<0||u&&X>=o}function ot(){var f=Rt();if(Mt(f))return Ot(f);a=setTimeout(ot,Ge(f))}function Ot(f){return a=void 0,A&&r?$(f):(r=i=void 0,n)}function je(){a!==void 0&&clearTimeout(a),g=0,r=l=i=a=void 0}function Fe(){return a===void 0?n:Ot(Rt())}function mt(){var f=Rt(),T=Mt(f);if(r=arguments,i=this,l=f,T){if(a===void 0)return W(l);if(u)return clearTimeout(a),a=setTimeout(ot,t),$(l)}return a===void 0&&(a=setTimeout(ot,t)),n}return mt.cancel=je,mt.flush=Fe,mt}Ue.exports=ci});var Nt='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M61.05,69.88H55.54a13.46,13.46,0,0,1,4.87-10.37,15.61,15.61,0,1,0-19.93,0,13.46,13.46,0,0,1,4.87,10.37H39.84A7.94,7.94,0,0,0,37,63.75a21.11,21.11,0,1,1,27,0A7.94,7.94,0,0,0,61.05,69.88Z"/><path d="M60.71,75H40.18a1.84,1.84,0,0,1,0-3.67H60.71a1.84,1.84,0,0,1,0,3.67Z"/><path d="M60.71,79.93H40.18a1.84,1.84,0,0,1,0-3.67H60.71a1.84,1.84,0,0,1,0,3.67Z"/><path d="M60.71,84.87H40.18a1.84,1.84,0,0,1,0-3.67H60.71a1.84,1.84,0,0,1,0,3.67Z"/><path d="M56.72,89.82H44.17a1.84,1.84,0,1,1,0-3.67H56.72a1.84,1.84,0,1,1,0,3.67Z"/><polygon points="50.53 54.3 56.18 51.04 56.18 44.51 50.53 47.77 50.53 54.3"/><polygon points="51.07 54.61 56.18 57.56 56.18 51.66 51.07 54.61"/><polygon points="55.87 43.35 55.87 37.45 50.76 40.4 55.87 43.35"/><polygon points="50.22 40.72 44.57 43.98 50.22 47.24 55.87 43.98 50.22 40.72"/><polygon points="49.68 40.4 44.57 37.45 44.57 43.35 49.68 40.4"/><polygon points="56.72 44.83 56.72 50.73 61.83 47.77 56.72 44.83"/><polygon points="49.91 54.3 49.91 47.77 44.26 44.51 44.26 44.51 44.26 51.04 44.26 51.04 44.26 51.04 49.91 54.3"/><polygon points="43.72 44.83 38.61 47.77 43.72 50.73 43.72 44.83"/><polygon points="44.26 51.66 44.26 57.56 49.37 54.61 44.26 51.66"/><polygon points="24.4 47.66 12.48 52.76 12.48 42.56 24.4 47.66"/><polygon points="31.92 29.57 19.89 24.75 27.1 17.54 31.92 29.57"/><polygon points="50.04 22.1 44.94 10.18 55.14 10.18 50.04 22.1"/><polygon points="68.13 29.63 72.95 17.59 80.16 24.8 68.13 29.63"/><polygon points="75.6 47.74 87.52 42.64 87.52 52.84 75.6 47.74"/><polygon points="68.08 65.83 80.11 70.66 72.9 77.86 68.08 65.83"/><polygon points="31.87 65.78 27.05 77.81 19.84 70.6 31.87 65.78"/></svg>';var G='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="28.82" cy="16.34" r="4.6"/><path d="M98,17.22H40.41a6.79,6.79,0,0,0,.07-.88,11.66,11.66,0,1,0-23.32,0,6.73,6.73,0,0,0,.06.88H2V88.67H25.91l1,6.65,6.17-6.65H98ZM28.82,7.88a8.46,8.46,0,0,1,8.46,8.46c0,3.57-5.29,11.71-8.46,16.15-3.17-4.44-8.46-12.58-8.46-16.15A8.46,8.46,0,0,1,28.82,7.88Zm3.82,24.74C35.27,33.33,37,34.71,37,36c0,1.83-3.49,3.88-8.16,3.88s-8.16-2-8.16-3.88c0-1.28,1.71-2.66,4.34-3.37,1.25,1.81,2.24,3.13,2.54,3.54l1.28,1.68,1.28-1.68C30.4,35.75,31.39,34.43,32.64,32.62ZM28.17,82.09l-4.88-3.87,19.16-4.37L29.09,88.26Zm65.05,1.78H37.52L51.85,68.43,16.08,76.58l9.11,7.23v.06H6.78V22h12a61.44,61.44,0,0,0,4.39,7.82c-3.42,1.21-5.69,3.48-5.69,6.15,0,4,5,7.08,11.36,7.08S40.18,40,40.18,36c0-2.67-2.27-4.94-5.69-6.15A62.69,62.69,0,0,0,38.88,22H93.22Z"/><path d="M30.83,65.59H26.6V57.43c0-1,0-1.9.06-2.72a8.11,8.11,0,0,1-1,1l-1.75,1.45-2.16-2.66,5.29-4.31h3.76Z"/><path d="M80.19,41.54H69V38.8L72.76,35c1.08-1.13,1.78-1.89,2.11-2.31a4.91,4.91,0,0,0,.69-1.06,2.24,2.24,0,0,0,.2-.92,1.21,1.21,0,0,0-.39-.95,1.61,1.61,0,0,0-1.1-.35,2.94,2.94,0,0,0-1.47.42A9.66,9.66,0,0,0,71.12,31l-2.29-2.67a12.56,12.56,0,0,1,2-1.53,7.26,7.26,0,0,1,1.77-.7,8.75,8.75,0,0,1,2.14-.24,6.54,6.54,0,0,1,2.68.52,4.25,4.25,0,0,1,1.82,1.51,3.89,3.89,0,0,1,.65,2.19,5.91,5.91,0,0,1-.23,1.67,5.76,5.76,0,0,1-.7,1.52,10,10,0,0,1-1.26,1.56q-.78.83-3.35,3.12v.11h5.84Z"/><path d="M84.09,63.11a3.66,3.66,0,0,1-.87,2.46,5,5,0,0,1-2.54,1.5v.06c2.58.32,3.86,1.54,3.86,3.65a4,4,0,0,1-1.7,3.39,7.94,7.94,0,0,1-4.73,1.23,13.67,13.67,0,0,1-2.3-.18,12.67,12.67,0,0,1-2.3-.65V71.11a9.17,9.17,0,0,0,2.06.76,8.26,8.26,0,0,0,1.94.25,3.93,3.93,0,0,0,2-.39,1.3,1.3,0,0,0,.63-1.2,1.35,1.35,0,0,0-.33-1A2.14,2.14,0,0,0,78.75,69a8.71,8.71,0,0,0-1.89-.17h-1V65.73h1c2.11,0,3.17-.54,3.17-1.63a1,1,0,0,0-.47-.9,2.3,2.3,0,0,0-1.26-.3,5.8,5.8,0,0,0-3.06,1l-1.73-2.78A8.24,8.24,0,0,1,76,59.91,10.85,10.85,0,0,1,79,59.55a6.43,6.43,0,0,1,3.75,1A3,3,0,0,1,84.09,63.11Z"/><path d="M63.16,70.12a10.22,10.22,0,0,1-5.87-1.78l1.82-2.63a7.3,7.3,0,0,0,5,1.16l.34,3.18A12.1,12.1,0,0,1,63.16,70.12Zm-8.53-4.53a8.51,8.51,0,0,1-1-2.2,11.19,11.19,0,0,1-.41-3,9.45,9.45,0,0,1,.24-2.2l3.12.72a6,6,0,0,0-.16,1.48,8.11,8.11,0,0,0,.29,2.14,5.1,5.1,0,0,0,.63,1.35ZM35.13,57c-.27,0-.54,0-.8,0l-.06-3.21a10.12,10.12,0,0,0,5.5-1.37l1.61,2.77A12.49,12.49,0,0,1,35.13,57Zm22.51-.25-2.46-2.05a11.61,11.61,0,0,1,6.35-3.61l.77,3.11A8.55,8.55,0,0,0,57.64,56.71Zm7.59-3-.29-3.19a34.85,34.85,0,0,1,3.77-.12h.22a8.57,8.57,0,0,0,2.15-.25l.79,3.11a12,12,0,0,1-2.94.34h-.28A30.94,30.94,0,0,0,65.23,53.69Zm-21-.69-2.21-2.31A5.91,5.91,0,0,0,43.91,46l3.2.11A9.08,9.08,0,0,1,44.26,53Zm31.06-1.43L73.37,49a8.43,8.43,0,0,0,2.17-2.64l.19-.34c.28-.51.58-1.06.87-1.65l2.87,1.41c-.31.63-.64,1.23-.94,1.78l-.19.36A11.62,11.62,0,0,1,75.32,51.57ZM47.19,43,44,42.76a9.87,9.87,0,0,1,3.12-6.68l2.23,2.29A6.68,6.68,0,0,0,47.19,43Zm4.43-6.39-1.72-2.7a30.13,30.13,0,0,1,3.48-1.87,22.94,22.94,0,0,1,2.68-1l1,3a18.65,18.65,0,0,0-2.32.9A26.63,26.63,0,0,0,51.62,36.62ZM60,33.33l-.59-3.14a28.15,28.15,0,0,1,6.78-.45L66,32.93A25.34,25.34,0,0,0,60,33.33Z"/><path d="M68.14,69.17l-1.09-3a20.32,20.32,0,0,0,2.67-1.24l1.55,2.8A22.22,22.22,0,0,1,68.14,69.17Z"/></svg>';var Vt='data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.1" id="Layer_1" x="0px" y="0px" viewBox="0 0 100 100" style="enable-background:new 0 0 100 100;" xml:space="preserve"><path d="M80.8,34.7V30v-2.1L76,23.1L69.9,17l-4.8-4.8H63h-4.7l-0.1-0.1v0.1h-39v75.6h61.6V34.7L80.8,34.7L80.8,34.7z M76,83H24V17  h34.2v17.8H76V83z M76,30H63V17v-0.1l0.1,0.1L76,30L76,30L76,30z"/><path d="M35.1,59.7c-1.2,0-2.2-0.5-3.1-1.5c-1.5-1.7-1.6-3.9-0.1-5.7c1.2-1.5,3.5-2.8,6.7-3.9c1.2-0.3,2.4-0.8,3.7-1  c0.8-1.5,1.6-3.1,2.3-4.8c0.7-1.6,1.3-3.1,1.7-4.6c-1.8-3.1-3.6-6.8-3.5-9.7c0-2.5,1.5-4.2,3.9-4.5c2.2-0.2,3.8,0.8,4.5,2.9  c0.9,2.8,0,7-1,10.7c0.9,1.5,2,3,3.2,4.5c1,1.3,2,2.4,3,3.6c4,0,7.9,0.3,10.2,1.6c2.1,1.2,2.9,2.9,2.3,5c-0.7,2.3-2.5,3.5-4.8,3.1  c-3-0.3-6.5-3.2-9.1-6c-1.8,0.1-3.9,0.2-6,0.6c-1.5,0.2-3,0.5-4.4,0.8c-0.5,0.9-1,1.7-1.5,2.4c-2,2.9-3.8,4.8-5.5,5.9  C36.8,59.5,36,59.7,35.1,59.7L35.1,59.7z M39.6,52.1c-2.4,0.8-4.3,1.8-5.1,2.8c-0.3,0.3-0.3,0.6,0.1,1.2c0.2,0.2,0.3,0.5,1.3,0  C37,55.3,38.4,54,39.6,52.1L39.6,52.1z M60.2,49.5c1.7,1.5,3.2,2.4,4.4,2.5c0.5,0.1,0.7,0.1,1-0.7c0.1-0.2,0.1-0.3-0.6-0.8  C64,50,62.3,49.6,60.2,49.5L60.2,49.5z M48.9,41.9c-0.3,0.9-0.7,1.7-0.9,2.3c-0.3,0.9-0.8,1.7-1.2,2.5c0.6-0.1,1.2-0.2,1.7-0.2  c1.2-0.1,2.3-0.3,3.5-0.3c-0.6-0.7-1-1.3-1.3-1.6C50.3,43.9,49.6,43,48.9,41.9L48.9,41.9z M47.3,27.5c-0.5,0.1-0.8,0.1-0.8,1  c0,1.2,0.5,2.8,1.4,4.7c0.5-2.3,0.6-4.2,0.2-5.2C47.8,27.5,47.7,27.5,47.3,27.5L47.3,27.5z"/><g><path d="M35.9,67.1c0-0.9,0.6-1.3,1.3-1.3h3.3c2.1,0,4.1,1.3,4.1,4c0,2.3-1.4,4-4,4h-2v2.5c0,0.9-0.6,1.4-1.3,1.4   c-0.8,0-1.3-0.5-1.3-1.4L35.9,67.1L35.9,67.1z M38.6,71.4H40c1.1,0,1.8-0.8,1.8-1.6c0-1.1-0.7-1.6-1.8-1.6h-1.4V71.4z"/><path d="M45.9,67.1c0-0.9,0.6-1.3,1.3-1.3h3.2c3.5,0,5.5,2.8,5.5,5.8c0,3.9-2.6,6-5.5,6h-3.2c-0.7,0-1.3-0.5-1.3-1.3V67.1z    M48.6,75.1H50c2.1,0,3.1-1.4,3.1-3.5c0-2-1-3.3-3-3.3h-1.5V75.1z"/><path d="M57.5,67.1c0-0.9,0.6-1.3,1.3-1.3h4.1c0.8,0,1.3,0.5,1.3,1.2c0,0.8-0.5,1.2-1.3,1.2H60v2.1h2.4c0.8,0,1.3,0.5,1.3,1.2   c0,0.8-0.5,1.2-1.3,1.2H60V76c0,0.9-0.6,1.4-1.3,1.4c-0.8,0-1.3-0.5-1.3-1.4L57.5,67.1z"/></g></svg>';var Zt='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M50.09,25.2h-17a5,5,0,0,0-5,5v1.2a5,5,0,0,0,5,5h17a5,5,0,0,0,5-5V30.2A5,5,0,0,0,50.09,25.2Zm1.8,6.2a1.81,1.81,0,0,1-1.8,1.8h-17a1.8,1.8,0,0,1-1.8-1.8V30.2a1.8,1.8,0,0,1,1.8-1.8h17a1.81,1.81,0,0,1,1.8,1.8Z"/><path d="M52.49,44.88H29.89a1.6,1.6,0,0,0,0,3.2h22.6a1.6,1.6,0,1,0,0-3.2Z"/><path d="M52.49,39.47H29.89a1.6,1.6,0,0,0,0,3.2h22.6a1.6,1.6,0,0,0,0-3.2Z"/><path d="M52.49,50.29H29.89a1.6,1.6,0,0,0,0,3.2h22.6a1.6,1.6,0,1,0,0-3.2Z"/><path d="M73.3,30.68a16.42,16.42,0,0,0-3.81.46V23.4a9.21,9.21,0,0,0-9.2-9.2H22.89a12.61,12.61,0,0,0-12.6,12.6V73.2a12.61,12.61,0,0,0,12.6,12.6H64.2l.46-.21c1.14-.5,4.83-2.4,4.83-5.59V63a15.9,15.9,0,0,0,3.81.47,16.42,16.42,0,0,0,0-32.83ZM64.69,69.73H22.89a3.85,3.85,0,0,0,0,7.65h41.8v2.44A5.54,5.54,0,0,1,63.12,81H22.89a7.8,7.8,0,0,1-7.8-7.8V26.8a7.8,7.8,0,0,1,7.8-7.8h37.4a4.41,4.41,0,0,1,4.4,4.4v9.74a16.36,16.36,0,0,0,0,27.9Zm8.61-9.42A13.22,13.22,0,1,1,86.51,47.09,13.23,13.23,0,0,1,73.3,60.31Z"/><path d="M79.63,41.34,71.2,49.77l-3-3a1.59,1.59,0,0,0-2.26,0A1.61,1.61,0,0,0,66,49l5.25,5.25L81.89,43.6a1.59,1.59,0,0,0,0-2.26A1.61,1.61,0,0,0,79.63,41.34Z"/></svg>';var nt='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M12.2,12.2V87.8H87.8V12.2ZM83,17V29.9H17V17ZM17,83V34.7H83V83Z"/><rect x="73.49" y="20.53" width="6.04" height="6.04"/><rect x="64.33" y="20.53" width="6.04" height="6.04"/><path d="M59.92,47.34a1.61,1.61,0,0,0,0,2.26l7.82,7.82-7.82,7.81a1.61,1.61,0,0,0,0,2.26,1.57,1.57,0,0,0,1.13.47,1.6,1.6,0,0,0,1.14-.47L72.26,57.42,62.19,47.34A1.62,1.62,0,0,0,59.92,47.34Z"/><path d="M40.08,47.34a1.62,1.62,0,0,0-2.27,0L27.74,57.42,37.81,67.49A1.6,1.6,0,0,0,39,68a1.56,1.56,0,0,0,1.13-.47,1.61,1.61,0,0,0,0-2.26l-7.82-7.81,7.82-7.82A1.61,1.61,0,0,0,40.08,47.34Z"/><path d="M53.55,47.38a1.61,1.61,0,0,0-2.06.94L45.21,65.4a1.61,1.61,0,0,0,.94,2.06,1.58,1.58,0,0,0,.56.1,1.6,1.6,0,0,0,1.5-1L54.5,49.43A1.6,1.6,0,0,0,53.55,47.38Z"/></svg>';var Wt='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024"><defs><style>.cls-1{fill:%234695eb;}.cls-2{fill:%23ff004a;}.cls-3{fill:%23091313;}</style></defs><title>quarkus_icon_rgb_1024px_default</title><polygon class="cls-1" points="669.34 180.57 512 271.41 669.34 362.25 669.34 180.57"/><polygon class="cls-2" points="354.66 180.57 354.66 362.25 512 271.41 354.66 180.57"/><polygon class="cls-3" points="669.34 362.25 512 271.41 354.66 362.25 512 453.09 669.34 362.25"/><polygon class="cls-1" points="188.76 467.93 346.1 558.76 346.1 377.09 188.76 467.93"/><polygon class="cls-2" points="346.1 740.44 503.43 649.6 346.1 558.76 346.1 740.44"/><polygon class="cls-3" points="346.1 377.09 346.1 558.76 503.43 649.6 503.43 467.93 346.1 377.09"/><polygon class="cls-1" points="677.9 740.44 677.9 558.76 520.57 649.6 677.9 740.44"/><polygon class="cls-2" points="835.24 467.93 677.9 377.09 677.9 558.76 835.24 467.93"/><polygon class="cls-3" points="520.57 649.6 677.9 558.76 677.9 377.09 520.57 467.93 520.57 649.6"/><path class="cls-1" d="M853.47,1H170.53C77.29,1,1,77.29,1,170.53V853.47C1,946.71,77.29,1023,170.53,1023h467.7L512,716.39,420.42,910H170.53C139.9,910,114,884.1,114,853.47V170.53C114,139.9,139.9,114,170.53,114H853.47C884.1,114,910,139.9,910,170.53V853.47C910,884.1,884.1,910,853.47,910H705.28l46.52,113H853.47c93.24,0,169.53-76.29,169.53-169.53V170.53C1023,77.29,946.71,1,853.47,1Z"/></svg>';var Gt='data:image/svg+xml,<svg id="artwork" xmlns="http://www.w3.org/2000/svg" width="100%" height="auto" viewBox="0 0 1024 140"><defs><style>.cls-1{fill:%23ff004a} .cls-2{fill:%234695eb} .cls-3{fill:var(--text-color)}</style></defs><path fill="%23ff004a" d="M33.23 17.54v23.59a5.9 5.9 0 008.85 5.11l20.44-11.79a5.9 5.9 0 000-10.22l-20.44-11.8a5.9 5.9 0 00-8.85 5.11z"/><path fill="%234695eb" d="M109.5 17.54v23.59a5.9 5.9 0 01-8.85 5.11L80.22 34.45a5.9 5.9 0 010-10.22l20.43-11.8a5.9 5.9 0 018.85 5.11z"/><path fill="%23ff004a" d="M42.08 134.31l20.44-11.8a5.9 5.9 0 000-10.22L42.08 100.5a5.9 5.9 0 00-8.85 5.11v23.59a5.9 5.9 0 008.85 5.11z"/><path fill="%234695eb" d="M4 68.26l20.43-11.8a5.9 5.9 0 018.85 5.11v23.6a5.9 5.9 0 01-8.85 5.11L4 78.48a5.9 5.9 0 010-10.22z"/><path fill="%23ff004a" d="M138.79 68.26l-20.44-11.8a5.9 5.9 0 00-8.85 5.11v23.6a5.9 5.9 0 008.85 5.11l20.44-11.8a5.9 5.9 0 000-10.22z"/><path fill="%234695eb" d="M100.65 134.31l-20.43-11.8a5.9 5.9 0 010-10.22l20.43-11.79a5.89 5.89 0 018.85 5.11v23.59a5.9 5.9 0 01-8.85 5.11z"/><path fill="var(--main-text-color)" d="M45.35 61.29h-.05A3.14 3.14 0 0040.62 64v18.09a16.64 16.64 0 008.32 14.42l15.65 9a3.15 3.15 0 004.69-2.71V84.74A16.66 16.66 0 0061 70.32zm28.06 41.52a3.15 3.15 0 004.69 2.71h.05l15.65-9a16.64 16.64 0 008.32-14.42V64a3.15 3.15 0 00-4.69-2.71l-15.65 9a16.64 16.64 0 00-8.33 14.42zm22.23-45.32a3.16 3.16 0 000-5.43L80 43a16.62 16.62 0 00-16.65 0l-15.66 9a3.16 3.16 0 000 5.43l15.65 9a16.67 16.67 0 0016.65 0zm180.81 6.32q0 15.85-5.62 26.58a34.33 34.33 0 01-16.5 15.71L276 130.25h-24.4l-16.47-20.16h-1.41q-20.66 0-31.72-12t-11.1-34.4q0-22.44 11.09-34.26t31.81-11.84q20.72 0 31.68 11.93t10.97 34.29zm-65.58 0q0 15.06 5.72 22.68t17.09 7.62q22.8 0 22.8-30.3T233.8 33.45q-11.37 0-17.15 7.65t-5.78 22.71zM365.69 19v58.15a33.48 33.48 0 01-4.45 17.45 29.07 29.07 0 01-12.88 11.5q-8.42 4-19.91 4-17.34 0-26.92-8.88t-9.59-24.32V19h19v55q0 10.39 4.18 15.25t13.83 4.85q9.35 0 13.55-4.88t4.21-15.34V19zm74.5 89.86l-6.52-21.39h-32.76l-6.51 21.39h-20.53l31.71-90.22h23.3l31.83 90.22zm-11.07-37.37q-9-29.07-10.17-32.88c-.76-2.54-1.3-4.54-1.63-6q-2 7.88-11.61 38.9zm59.44 2.89v34.48H469.5V19h26.19q18.31 0 27.1 6.67t8.79 20.25A23.9 23.9 0 01527.22 60a28.82 28.82 0 01-12.36 9.68q20.28 30.3 26.43 39.15h-21.14L498.7 74.38zm0-15.48h6.14q9 0 13.34-3t4.3-9.46q0-6.39-4.39-9.1t-13.62-2.7h-5.77zm133.8 49.96h-21.64L577.18 71l-8 5.78v32.08h-19V19h19v41.12l7.5-10.57L601 19h21.14l-31.37 39.77zm8.79 0V19h19.05v89.85zM721.56 19h19.24l-30.55 89.85h-20.77L659 19h19.24l16.9 53.47q1.41 4.72 2.92 11c1 4.2 1.63 7.12 1.87 8.76q.69-5.65 4.61-19.79zm79.78 89.86h-51.75V19h51.75v15.62h-32.7v19.73h30.42V70h-30.42v23.13h32.7zm35.28-34.48v34.48h-19.06V19h26.19q18.32 0 27.1 6.67t8.79 20.25A23.9 23.9 0 01875.28 60a28.82 28.82 0 01-12.36 9.68q20.28 30.3 26.43 39.15h-21.14l-21.45-34.45zm0-15.48h6.14q9 0 13.34-3t4.3-9.46q0-6.39-4.39-9.1t-13.62-2.7h-5.77zm114.56 25.01q0 12.16-8.76 19.17t-24.37 7q-14.37 0-25.44-5.41V87A104.49 104.49 0 00908 92.7a45.91 45.91 0 0011.53 1.66q6.27 0 9.62-2.4a8.2 8.2 0 003.35-7.13 7.82 7.82 0 00-1.48-4.7 16.55 16.55 0 00-4.33-4A112.53 112.53 0 00915 70.08a54.87 54.87 0 01-12.35-7.44 27.82 27.82 0 01-6.58-8.29 23.68 23.68 0 01-2.46-11.07q0-11.91 8.09-18.74t22.34-6.82a52.53 52.53 0 0113.36 1.66 90.93 90.93 0 0113.31 4.67l-6.11 14.81a89.25 89.25 0 00-11.89-4.12 38.2 38.2 0 00-9.25-1.17q-5.4 0-8.3 2.52a8.34 8.34 0 00-2.88 6.58 8.19 8.19 0 001.16 4.39 12.9 12.9 0 003.72 3.63A114.3 114.3 0 00929.24 57q12.6 6 17.27 12.08a23.57 23.57 0 014.67 14.83zm65.52 24.95H965V19h51.75v15.62H984v19.73h30.43V70H984v23.13h32.7z"/></svg>%0A';var jt='data:image/svg+xml,<svg width="57" height="57" viewBox="0 0 57 57" xmlns="http://www.w3.org/2000/svg" stroke="%234695EB">%0A    <g fill="none" fill-rule="evenodd">%0A        <g transform="translate(1 1)" stroke-width="2">%0A            <circle cx="5" cy="50" r="5">%0A                <animate attributeName="cy"%0A                     begin="0s" dur="2.2s"%0A                     values="50;5;50;50"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A                <animate attributeName="cx"%0A                     begin="0s" dur="2.2s"%0A                     values="5;27;49;5"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A            </circle>%0A            <circle cx="27" cy="5" r="5">%0A                <animate attributeName="cy"%0A                     begin="0s" dur="2.2s"%0A                     from="5" to="5"%0A                     values="5;50;50;5"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A                <animate attributeName="cx"%0A                     begin="0s" dur="2.2s"%0A                     from="27" to="27"%0A                     values="27;49;5;27"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A            </circle>%0A            <circle cx="49" cy="50" r="5">%0A                <animate attributeName="cy"%0A                     begin="0s" dur="2.2s"%0A                     values="50;50;5;50"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A                <animate attributeName="cx"%0A                     from="49" to="49"%0A                     begin="0s" dur="2.2s"%0A                     values="49;5;27;49"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A            </circle>%0A        </g>%0A    </g>%0A</svg>';var or={docs:{tutorial:nt,tutorials:nt,guide:G,guides:G,howto:G,pdf:Vt,concepts:Nt,reference:Zt},origins:{quarkus:Wt,quarkiverse:Gt},loading:jt},k=or;var z=class s{static{this.guides=null}static queryDocumentGuides(t="qs-target qs-guide"){let e=document.querySelectorAll(t),r=e?[]:null;for(let i=0;i<e.length;i++){let o=e[i];r.push({title:o.getAttribute("title"),categories:o.getAttribute("categories"),type:o.getAttribute("type"),url:o.getAttribute("url"),summary:o.getAttribute("summary"),keywords:o.getAttribute("keywords"),content:o.getAttribute("content"),origin:o.getAttribute("origin")})}return r}static enableLocalSearch(t){s.guides=s.queryDocumentGuides(t),s.guides!=null&&console.debug("LocalSearch is ready with "+s.guides.length+" guides found.")}static search(t){let e=s.guides;if(e==null)return null;let r=[];t.q&&r.push(...t.q.split(" ").map(o=>o.trim()));let i=[];return t.categories&&(Array.isArray(t.categories)?i.push(...t.categories):i.push(t.categories)),e.filter(o=>{let n=!0;return n&&i.length>0&&(n=s.containsAllCaseInsensitive(o.categories,i)),n&&r.length>0&&(n=s.containsAllCaseInsensitive(`${o.keywords}${o.summary}${o.title}${o.categories}`,r)),n})}static containsAllCaseInsensitive(t,e){let r=(t||"").toLowerCase();for(let i in e)if(r.indexOf(e[i].toLowerCase())<0)return!1;return!0}};var at=globalThis,lt=at.ShadowRoot&&(at.ShadyCSS===void 0||at.ShadyCSS.nativeShadow)&&"adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,bt=Symbol(),Ft=new WeakMap,P=class{constructor(t,e,r){if(this._$cssResult$=!0,r!==bt)throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=t,this.t=e}get styleSheet(){let t=this.o,e=this.t;if(lt&&t===void 0){let r=e!==void 0&&e.length===1;r&&(t=Ft.get(e)),t===void 0&&((this.o=t=new CSSStyleSheet).replaceSync(this.cssText),r&&Ft.set(e,t))}return t}toString(){return this.cssText}},q=s=>new P(typeof s=="string"?s:s+"",void 0,bt),S=(s,...t)=>{let e=s.length===1?s[0]:t.reduce((r,i,o)=>r+(n=>{if(n._$cssResult$===!0)return n.cssText;if(typeof n=="number")return n;throw Error("Value passed to 'css' function must be a 'css' function result: "+n+". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.")})(i)+s[o+1],s[0]);return new P(e,s,bt)},Dt=(s,t)=>{if(lt)s.adoptedStyleSheets=t.map(e=>e instanceof CSSStyleSheet?e:e.styleSheet);else for(let e of t){let r=document.createElement("style"),i=at.litNonce;i!==void 0&&r.setAttribute("nonce",i),r.textContent=e.cssText,s.appendChild(r)}},vt=lt?s=>s:s=>s instanceof CSSStyleSheet?(t=>{let e="";for(let r of t.cssRules)e+=r.cssText;return q(e)})(s):s;var{is:nr,defineProperty:ar,getOwnPropertyDescriptor:lr,getOwnPropertyNames:cr,getOwnPropertySymbols:hr,getPrototypeOf:dr}=Object,ct=globalThis,Xt=ct.trustedTypes,ur=Xt?Xt.emptyScript:"",gr=ct.reactiveElementPolyfillSupport,Y=(s,t)=>s,Q={toAttribute(s,t){switch(t){case Boolean:s=s?ur:null;break;case Object:case Array:s=s==null?s:JSON.stringify(s)}return s},fromAttribute(s,t){let e=s;switch(t){case Boolean:e=s!==null;break;case Number:e=s===null?null:Number(s);break;case Object:case Array:try{e=JSON.parse(s)}catch{e=null}}return e}},ht=(s,t)=>!nr(s,t),zt={attribute:!0,type:String,converter:Q,reflect:!1,useDefault:!1,hasChanged:ht};Symbol.metadata??=Symbol("metadata"),ct.litPropertyMetadata??=new WeakMap;var L=class extends HTMLElement{static addInitializer(t){this._$Ei(),(this.l??=[]).push(t)}static get observedAttributes(){return this.finalize(),this._$Eh&&[...this._$Eh.keys()]}static createProperty(t,e=zt){if(e.state&&(e.attribute=!1),this._$Ei(),this.prototype.hasOwnProperty(t)&&((e=Object.create(e)).wrapped=!0),this.elementProperties.set(t,e),!e.noAccessor){let r=Symbol(),i=this.getPropertyDescriptor(t,r,e);i!==void 0&&ar(this.prototype,t,i)}}static getPropertyDescriptor(t,e,r){let{get:i,set:o}=lr(this.prototype,t)??{get(){return this[e]},set(n){this[e]=n}};return{get:i,set(n){let a=i?.call(this);o?.call(this,n),this.requestUpdate(t,a,r)},configurable:!0,enumerable:!0}}static getPropertyOptions(t){return this.elementProperties.get(t)??zt}static _$Ei(){if(this.hasOwnProperty(Y("elementProperties")))return;let t=dr(this);t.finalize(),t.l!==void 0&&(this.l=[...t.l]),this.elementProperties=new Map(t.elementProperties)}static finalize(){if(this.hasOwnProperty(Y("finalized")))return;if(this.finalized=!0,this._$Ei(),this.hasOwnProperty(Y("properties"))){let e=this.properties,r=[...cr(e),...hr(e)];for(let i of r)this.createProperty(i,e[i])}let t=this[Symbol.metadata];if(t!==null){let e=litPropertyMetadata.get(t);if(e!==void 0)for(let[r,i]of e)this.elementProperties.set(r,i)}this._$Eh=new Map;for(let[e,r]of this.elementProperties){let i=this._$Eu(e,r);i!==void 0&&this._$Eh.set(i,e)}this.elementStyles=this.finalizeStyles(this.styles)}static finalizeStyles(t){let e=[];if(Array.isArray(t)){let r=new Set(t.flat(1/0).reverse());for(let i of r)e.unshift(vt(i))}else t!==void 0&&e.push(vt(t));return e}static _$Eu(t,e){let r=e.attribute;return r===!1?void 0:typeof r=="string"?r:typeof t=="string"?t.toLowerCase():void 0}constructor(){super(),this._$Ep=void 0,this.isUpdatePending=!1,this.hasUpdated=!1,this._$Em=null,this._$Ev()}_$Ev(){this._$ES=new Promise(t=>this.enableUpdating=t),this._$AL=new Map,this._$E_(),this.requestUpdate(),this.constructor.l?.forEach(t=>t(this))}addController(t){(this._$EO??=new Set).add(t),this.renderRoot!==void 0&&this.isConnected&&t.hostConnected?.()}removeController(t){this._$EO?.delete(t)}_$E_(){let t=new Map,e=this.constructor.elementProperties;for(let r of e.keys())this.hasOwnProperty(r)&&(t.set(r,this[r]),delete this[r]);t.size>0&&(this._$Ep=t)}createRenderRoot(){let t=this.shadowRoot??this.attachShadow(this.constructor.shadowRootOptions);return Dt(t,this.constructor.elementStyles),t}connectedCallback(){this.renderRoot??=this.createRenderRoot(),this.enableUpdating(!0),this._$EO?.forEach(t=>t.hostConnected?.())}enableUpdating(t){}disconnectedCallback(){this._$EO?.forEach(t=>t.hostDisconnected?.())}attributeChangedCallback(t,e,r){this._$AK(t,r)}_$ET(t,e){let r=this.constructor.elementProperties.get(t),i=this.constructor._$Eu(t,r);if(i!==void 0&&r.reflect===!0){let o=(r.converter?.toAttribute!==void 0?r.converter:Q).toAttribute(e,r.type);this._$Em=t,o==null?this.removeAttribute(i):this.setAttribute(i,o),this._$Em=null}}_$AK(t,e){let r=this.constructor,i=r._$Eh.get(t);if(i!==void 0&&this._$Em!==i){let o=r.getPropertyOptions(i),n=typeof o.converter=="function"?{fromAttribute:o.converter}:o.converter?.fromAttribute!==void 0?o.converter:Q;this._$Em=i;let a=n.fromAttribute(e,o.type);this[i]=a??this._$Ej?.get(i)??a,this._$Em=null}}requestUpdate(t,e,r,i=!1,o){if(t!==void 0){let n=this.constructor;if(i===!1&&(o=this[t]),r??=n.getPropertyOptions(t),!((r.hasChanged??ht)(o,e)||r.useDefault&&r.reflect&&o===this._$Ej?.get(t)&&!this.hasAttribute(n._$Eu(t,r))))return;this.C(t,e,r)}this.isUpdatePending===!1&&(this._$ES=this._$EP())}C(t,e,{useDefault:r,reflect:i,wrapped:o},n){r&&!(this._$Ej??=new Map).has(t)&&(this._$Ej.set(t,n??e??this[t]),o!==!0||n!==void 0)||(this._$AL.has(t)||(this.hasUpdated||r||(e=void 0),this._$AL.set(t,e)),i===!0&&this._$Em!==t&&(this._$Eq??=new Set).add(t))}async _$EP(){this.isUpdatePending=!0;try{await this._$ES}catch(e){Promise.reject(e)}let t=this.scheduleUpdate();return t!=null&&await t,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){if(!this.isUpdatePending)return;if(!this.hasUpdated){if(this.renderRoot??=this.createRenderRoot(),this._$Ep){for(let[i,o]of this._$Ep)this[i]=o;this._$Ep=void 0}let r=this.constructor.elementProperties;if(r.size>0)for(let[i,o]of r){let{wrapped:n}=o,a=this[i];n!==!0||this._$AL.has(i)||a===void 0||this.C(i,void 0,o,a)}}let t=!1,e=this._$AL;try{t=this.shouldUpdate(e),t?(this.willUpdate(e),this._$EO?.forEach(r=>r.hostUpdate?.()),this.update(e)):this._$EM()}catch(r){throw t=!1,this._$EM(),r}t&&this._$AE(e)}willUpdate(t){}_$AE(t){this._$EO?.forEach(e=>e.hostUpdated?.()),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(t)),this.updated(t)}_$EM(){this._$AL=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this._$ES}shouldUpdate(t){return!0}update(t){this._$Eq&&=this._$Eq.forEach(e=>this._$ET(e,this[e])),this._$EM()}updated(t){}firstUpdated(t){}};L.elementStyles=[],L.shadowRootOptions={mode:"open"},L[Y("elementProperties")]=new Map,L[Y("finalized")]=new Map,gr?.({ReactiveElement:L}),(ct.reactiveElementVersions??=[]).push("2.1.2");var St=globalThis,Pt=s=>s,dt=St.trustedTypes,Yt=dt?dt.createPolicy("lit-html",{createHTML:s=>s}):void 0,re="$lit$",M=`lit$${Math.random().toFixed(9).slice(2)}$`,ie="?"+M,pr=`<${ie}>`,N=document,K=()=>N.createComment(""),tt=s=>s===null||typeof s!="object"&&typeof s!="function",wt=Array.isArray,mr=s=>wt(s)||typeof s?.[Symbol.iterator]=="function",yt=`[ 	
\f\r]`,J=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,Qt=/-->/g,Jt=/>/g,B=RegExp(`>|${yt}(?:([^\\s"'>=/]+)(${yt}*=${yt}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`,"g"),Kt=/'/g,te=/"/g,se=/^(?:script|style|textarea|title)$/i,Et=s=>(t,...e)=>({_$litType$:s,strings:t,values:e}),h=Et(1),Hi=Et(2),Bi=Et(3),R=Symbol.for("lit-noChange"),p=Symbol.for("lit-nothing"),ee=new WeakMap,U=N.createTreeWalker(N,129);function oe(s,t){if(!wt(s)||!s.hasOwnProperty("raw"))throw Error("invalid template strings array");return Yt!==void 0?Yt.createHTML(t):t}var fr=(s,t)=>{let e=s.length-1,r=[],i,o=t===2?"<svg>":t===3?"<math>":"",n=J;for(let a=0;a<e;a++){let l=s[a],g,m,u=-1,A=0;for(;A<l.length&&(n.lastIndex=A,m=n.exec(l),m!==null);)A=n.lastIndex,n===J?m[1]==="!--"?n=Qt:m[1]!==void 0?n=Jt:m[2]!==void 0?(se.test(m[2])&&(i=RegExp("</"+m[2],"g")),n=B):m[3]!==void 0&&(n=B):n===B?m[0]===">"?(n=i??J,u=-1):m[1]===void 0?u=-2:(u=n.lastIndex-m[2].length,g=m[1],n=m[3]===void 0?B:m[3]==='"'?te:Kt):n===te||n===Kt?n=B:n===Qt||n===Jt?n=J:(n=B,i=void 0);let $=n===B&&s[a+1].startsWith("/>")?" ":"";o+=n===J?l+pr:u>=0?(r.push(g),l.slice(0,u)+re+l.slice(u)+M+$):l+M+(u===-2?a:$)}return[oe(s,o+(s[e]||"<?>")+(t===2?"</svg>":t===3?"</math>":"")),r]},et=class s{constructor({strings:t,_$litType$:e},r){let i;this.parts=[];let o=0,n=0,a=t.length-1,l=this.parts,[g,m]=fr(t,e);if(this.el=s.createElement(g,r),U.currentNode=this.el.content,e===2||e===3){let u=this.el.content.firstChild;u.replaceWith(...u.childNodes)}for(;(i=U.nextNode())!==null&&l.length<a;){if(i.nodeType===1){if(i.hasAttributes())for(let u of i.getAttributeNames())if(u.endsWith(re)){let A=m[n++],$=i.getAttribute(u).split(M),W=/([.?@])?(.*)/.exec(A);l.push({type:1,index:o,name:W[2],strings:$,ctor:W[1]==="."?xt:W[1]==="?"?At:W[1]==="@"?Ct:F}),i.removeAttribute(u)}else u.startsWith(M)&&(l.push({type:6,index:o}),i.removeAttribute(u));if(se.test(i.tagName)){let u=i.textContent.split(M),A=u.length-1;if(A>0){i.textContent=dt?dt.emptyScript:"";for(let $=0;$<A;$++)i.append(u[$],K()),U.nextNode(),l.push({type:2,index:++o});i.append(u[A],K())}}}else if(i.nodeType===8)if(i.data===ie)l.push({type:2,index:o});else{let u=-1;for(;(u=i.data.indexOf(M,u+1))!==-1;)l.push({type:7,index:o}),u+=M.length-1}o++}}static createElement(t,e){let r=N.createElement("template");return r.innerHTML=t,r}};function j(s,t,e=s,r){if(t===R)return t;let i=r!==void 0?e._$Co?.[r]:e._$Cl,o=tt(t)?void 0:t._$litDirective$;return i?.constructor!==o&&(i?._$AO?.(!1),o===void 0?i=void 0:(i=new o(s),i._$AT(s,e,r)),r!==void 0?(e._$Co??=[])[r]=i:e._$Cl=i),i!==void 0&&(t=j(s,i._$AS(s,t.values),i,r)),t}var _t=class{constructor(t,e){this._$AV=[],this._$AN=void 0,this._$AD=t,this._$AM=e}get parentNode(){return this._$AM.parentNode}get _$AU(){return this._$AM._$AU}u(t){let{el:{content:e},parts:r}=this._$AD,i=(t?.creationScope??N).importNode(e,!0);U.currentNode=i;let o=U.nextNode(),n=0,a=0,l=r[0];for(;l!==void 0;){if(n===l.index){let g;l.type===2?g=new rt(o,o.nextSibling,this,t):l.type===1?g=new l.ctor(o,l.name,l.strings,this,t):l.type===6&&(g=new $t(o,this,t)),this._$AV.push(g),l=r[++a]}n!==l?.index&&(o=U.nextNode(),n++)}return U.currentNode=N,i}p(t){let e=0;for(let r of this._$AV)r!==void 0&&(r.strings!==void 0?(r._$AI(t,r,e),e+=r.strings.length-2):r._$AI(t[e])),e++}},rt=class s{get _$AU(){return this._$AM?._$AU??this._$Cv}constructor(t,e,r,i){this.type=2,this._$AH=p,this._$AN=void 0,this._$AA=t,this._$AB=e,this._$AM=r,this.options=i,this._$Cv=i?.isConnected??!0}get parentNode(){let t=this._$AA.parentNode,e=this._$AM;return e!==void 0&&t?.nodeType===11&&(t=e.parentNode),t}get startNode(){return this._$AA}get endNode(){return this._$AB}_$AI(t,e=this){t=j(this,t,e),tt(t)?t===p||t==null||t===""?(this._$AH!==p&&this._$AR(),this._$AH=p):t!==this._$AH&&t!==R&&this._(t):t._$litType$!==void 0?this.$(t):t.nodeType!==void 0?this.T(t):mr(t)?this.k(t):this._(t)}O(t){return this._$AA.parentNode.insertBefore(t,this._$AB)}T(t){this._$AH!==t&&(this._$AR(),this._$AH=this.O(t))}_(t){this._$AH!==p&&tt(this._$AH)?this._$AA.nextSibling.data=t:this.T(N.createTextNode(t)),this._$AH=t}$(t){let{values:e,_$litType$:r}=t,i=typeof r=="number"?this._$AC(t):(r.el===void 0&&(r.el=et.createElement(oe(r.h,r.h[0]),this.options)),r);if(this._$AH?._$AD===i)this._$AH.p(e);else{let o=new _t(i,this),n=o.u(this.options);o.p(e),this.T(n),this._$AH=o}}_$AC(t){let e=ee.get(t.strings);return e===void 0&&ee.set(t.strings,e=new et(t)),e}k(t){wt(this._$AH)||(this._$AH=[],this._$AR());let e=this._$AH,r,i=0;for(let o of t)i===e.length?e.push(r=new s(this.O(K()),this.O(K()),this,this.options)):r=e[i],r._$AI(o),i++;i<e.length&&(this._$AR(r&&r._$AB.nextSibling,i),e.length=i)}_$AR(t=this._$AA.nextSibling,e){for(this._$AP?.(!1,!0,e);t!==this._$AB;){let r=Pt(t).nextSibling;Pt(t).remove(),t=r}}setConnected(t){this._$AM===void 0&&(this._$Cv=t,this._$AP?.(t))}},F=class{get tagName(){return this.element.tagName}get _$AU(){return this._$AM._$AU}constructor(t,e,r,i,o){this.type=1,this._$AH=p,this._$AN=void 0,this.element=t,this.name=e,this._$AM=i,this.options=o,r.length>2||r[0]!==""||r[1]!==""?(this._$AH=Array(r.length-1).fill(new String),this.strings=r):this._$AH=p}_$AI(t,e=this,r,i){let o=this.strings,n=!1;if(o===void 0)t=j(this,t,e,0),n=!tt(t)||t!==this._$AH&&t!==R,n&&(this._$AH=t);else{let a=t,l,g;for(t=o[0],l=0;l<o.length-1;l++)g=j(this,a[r+l],e,l),g===R&&(g=this._$AH[l]),n||=!tt(g)||g!==this._$AH[l],g===p?t=p:t!==p&&(t+=(g??"")+o[l+1]),this._$AH[l]=g}n&&!i&&this.j(t)}j(t){t===p?this.element.removeAttribute(this.name):this.element.setAttribute(this.name,t??"")}},xt=class extends F{constructor(){super(...arguments),this.type=3}j(t){this.element[this.name]=t===p?void 0:t}},At=class extends F{constructor(){super(...arguments),this.type=4}j(t){this.element.toggleAttribute(this.name,!!t&&t!==p)}},Ct=class extends F{constructor(t,e,r,i,o){super(t,e,r,i,o),this.type=5}_$AI(t,e=this){if((t=j(this,t,e,0)??p)===R)return;let r=this._$AH,i=t===p&&r!==p||t.capture!==r.capture||t.once!==r.once||t.passive!==r.passive,o=t!==p&&(r===p||i);i&&this.element.removeEventListener(this.name,this,r),o&&this.element.addEventListener(this.name,this,t),this._$AH=t}handleEvent(t){typeof this._$AH=="function"?this._$AH.call(this.options?.host??this.element,t):this._$AH.handleEvent(t)}},$t=class{constructor(t,e,r){this.element=t,this.type=6,this._$AN=void 0,this._$AM=e,this.options=r}get _$AU(){return this._$AM._$AU}_$AI(t){j(this,t)}};var br=St.litHtmlPolyfillSupport;br?.(et,rt),(St.litHtmlVersions??=[]).push("3.3.3");var ne=(s,t,e)=>{let r=e?.renderBefore??t,i=r._$litPart$;if(i===void 0){let o=e?.renderBefore??null;r._$litPart$=i=new rt(t.insertBefore(K(),o),o,void 0,e??{})}return i._$AI(s),i};var It=globalThis,b=class extends L{constructor(){super(...arguments),this.renderOptions={host:this},this._$Do=void 0}createRenderRoot(){let t=super.createRenderRoot();return this.renderOptions.renderBefore??=t.firstChild,t}update(t){let e=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(t),this._$Do=ne(e,this.renderRoot,this.renderOptions)}connectedCallback(){super.connectedCallback(),this._$Do?.setConnected(!0)}disconnectedCallback(){super.disconnectedCallback(),this._$Do?.setConnected(!1)}render(){return R}};b._$litElement$=!0,b.finalized=!0,It.litElementHydrateSupport?.({LitElement:b});var vr=It.litElementPolyfillSupport;vr?.({LitElement:b});(It.litElementVersions??=[]).push("4.2.2");var E=s=>(t,e)=>{e!==void 0?e.addInitializer(()=>{customElements.define(s,t)}):customElements.define(s,t)};var yr={attribute:!0,type:String,converter:Q,reflect:!1,hasChanged:ht},_r=(s=yr,t,e)=>{let{kind:r,metadata:i}=e,o=globalThis.litPropertyMetadata.get(i);if(o===void 0&&globalThis.litPropertyMetadata.set(i,o=new Map),r==="setter"&&((s=Object.create(s)).wrapped=!0),o.set(e.name,s),r==="accessor"){let{name:n}=e;return{set(a){let l=t.get.call(this);t.set.call(this,a),this.requestUpdate(n,l,s,!0,a)},init(a){return a!==void 0&&this.C(n,void 0,s,a),a}}}if(r==="setter"){let{name:n}=e;return function(a){let l=this[n];t.call(this,a),this.requestUpdate(n,l,s,!0,a)}}throw Error("Unsupported decorator location: "+r)};function d(s){return(t,e)=>typeof e=="object"?_r(s,t,e):((r,i,o)=>{let n=i.hasOwnProperty(o);return i.constructor.createProperty(o,r),n?Object.getOwnPropertyDescriptor(i,o):void 0})(s,t,e)}function w(s){return d({...s,state:!0,attribute:!1})}var D=(s,t,e)=>(e.configurable=!0,e.enumerable=!0,Reflect.decorate&&typeof t!="object"&&Object.defineProperty(s,t,e),e);var xr;function ae(s){return(t,e)=>D(t,e,{get(){return(this.renderRoot??(xr??=document.createDocumentFragment())).querySelectorAll(s)}})}var Ne=Ut(Tt());var V="qs-start",I="qs-result",O="qs-grouped-result",ut="qs-next-page",gt="qs-query-suggestion",_=class extends b{constructor(){super();this.server="";this.minChars=3;this.initialTimeout=1500;this.moreTimeout=2500;this.language="en";this.localSearch=!1;this.originFilter="";this.grouped=!1;this._page=0;this._currentHitCount=0;this._abortController=null;this._search=()=>{if(this._abortController&&this._abortController.abort(),!this._backendData){this._clearSearch();return}let e=new AbortController;if(this._abortController=e,this.dispatchEvent(new CustomEvent(V,{detail:{page:this._page}})),this.localSearch){this._localSearch(),this._abortController==e&&(this._abortController=null);return}this.grouped?this._groupedFetch(e).then(r=>{this.dispatchEvent(new CustomEvent(O,{detail:{...r,search:this._backendData,server:this.server}}))}).catch(r=>{console.error("Could not run grouped search: "+r),this._abortController==e&&this._localSearch()}).finally(()=>{this._abortController==e&&(this._abortController=null)}):this._jsonFetch(e,"GET",this._backendData,this._page>0?this.initialTimeout:this.moreTimeout).then(r=>{this._page>0?this._currentHitCount+=r.hits.length:this._currentHitCount=r.hits.length;let i=r.total?.lowerBound,o=r.hits.length>0&&i>this._currentHitCount;this.dispatchEvent(new CustomEvent(I,{detail:{...r,search:this._backendData,page:this._page,hasMoreHits:o}}))}).catch(r=>{console.error("Could not run search: "+r),this._abortController==e&&(this._page=0,this._currentHitCount=0,this._localSearch())}).finally(()=>{this._abortController==e&&(this._abortController=null)})};this._searchDebounced=(0,Ne.default)(this._search,300);this._handleInputChange=e=>{let r=this._getFormElements(),i={language:this.language},o={};this.quarkusversion&&(i.version=this.quarkusversion),this.originFilter&&(i.origin=this.originFilter);var n=0;for(let a of r)this._isInput(a)&&(a.value.length===0||a.value.length<this.minChars)||a.value&&a.value.length>0&&a.name.length>0&&(i[a.name]=a.value,o[a.name]=a.value,n++);n==0?(this._backendData=null,this._browserData=null):(this._backendData=i,this._browserData=o)};this._handleNextPage=e=>{this._page++,this._search()};this._handleQuerySuggestion=e=>{this._getFormElements().forEach(r=>{this._isInput(r)&&r.name==="q"&&(r.value=e.detail.suggestion.query)}),this._handleInputChange(e)};let e=new URLSearchParams(window.location.hash.substring(1));if(e.size>0){this._initialQueryStringPresent=!0;let r=this._getFormElements();for(let i of r){let o=e.get(i.name);o&&(i.value=o)}}}render(){return h`
      <div id="qs-form">
        <slot></slot>
      </div>
    `}update(e){return this._initialQueryStringPresent&&(this._initialQueryStringPresent=!1,this._handleInputChange(null)),this._updateHash(),this._backendData?this._searchDebounced():this._clearSearch(),super.update(e)}connectedCallback(){super.connectedCallback(),z.enableLocalSearch();let e=this._getFormElements();this.addEventListener(ut,this._handleNextPage),this.addEventListener(gt,this._handleQuerySuggestion),e.forEach(r=>{let i=this._isInput(r)?"input":"change";r.addEventListener(i,this._handleInputChange)}),this._handleInputChange(null)}disconnectedCallback(){super.disconnectedCallback(),this.removeEventListener(ut,this._handleNextPage),this.removeEventListener(gt,this._handleQuerySuggestion),this._getFormElements().forEach(r=>{let i=this._isInput(r)?"input":"change";r.removeEventListener(i,this._handleInputChange)})}_getFormElements(){return this.querySelectorAll("input[name], select[name]")}_isInput(e){return e.tagName.toLowerCase()==="input"}_updateHash(){let e=new URLSearchParams(window.location.hash.substring(1)),r=new Set;if(this._getFormElements().forEach(o=>r.add(o.name)),r.forEach(o=>e.delete(o)),this._browserData)for(let[o,n]of Object.entries(this._browserData))e.set(o,n);let i=e.toString();window.location.hash=i||""}async _groupedFetch(e){let r={};this._backendData.q&&(r.q=this._backendData.q),this._backendData.language&&(r.language=this._backendData.language),this._backendData.version&&(r.version=this._backendData.version),this._backendData.origin&&(r.origin=this._backendData.origin);let i=setTimeout(()=>e.abort(),this.initialTimeout),o=await fetch(this.server+"/api/guides/search/grouped?"+new URLSearchParams(r).toString(),{method:"GET",signal:e.signal,body:null});if(clearTimeout(i),o.ok)return await o.json();throw"Response status is "+o.status+"; response: "+await o.text()}async _jsonFetch(e,r,i,o){let n={...i,page:this._page.toString()},a=setTimeout(()=>e.abort(),o),l=await fetch(this.server+"/api/guides/search?"+new URLSearchParams(n).toString(),{method:r,signal:e.signal,body:null});if(clearTimeout(a),l.ok)return await l.json();throw"Response status is "+l.status+"; response: "+await l.text()}_clearSearch(){this._page=0,this._currentHitCount=0,this._abortController&&(this._abortController.abort(),this._abortController=null),this.grouped?this.dispatchEvent(new CustomEvent(O)):this.dispatchEvent(new CustomEvent(I))}_localSearch(){let e=this._backendData,r=z.search(e);if(r){let i={hits:r,total:r.length};this.dispatchEvent(new CustomEvent(I,{detail:{...i,search:e,page:0,hasMoreHits:!1}}));return}this.dispatchEvent(new CustomEvent(I))}};_.styles=S`

      #qs-form {
          display: contents;
      }

      ::slotted(section) {
          display: block !important;
      }

      .d-none {
          display: none;
      }
  `,c([d({type:String})],_.prototype,"server",2),c([d({type:String,attribute:"min-chars"})],_.prototype,"minChars",2),c([d({type:String,attribute:"initial-timeout"})],_.prototype,"initialTimeout",2),c([d({type:String,attribute:"more-timeout"})],_.prototype,"moreTimeout",2),c([d({type:String})],_.prototype,"language",2),c([d({type:String,attribute:"quarkus-version"})],_.prototype,"quarkusversion",2),c([d({type:String,attribute:"local-search"})],_.prototype,"localSearch",2),c([d({type:String,attribute:"origin-filter"})],_.prototype,"originFilter",2),c([d({type:Boolean})],_.prototype,"grouped",2),c([w({hasChanged(e,r){return JSON.stringify(e)!==JSON.stringify(r)}})],_.prototype,"_backendData",2),_=c([E("qs-form")],_);var H=class extends b{constructor(){super(...arguments);this.categories=[];this.categoriesMeta={};this._handleGroupedResult=e=>{this._groupedResult=e.detail};this._handleResult=e=>{this._groupedResult=void 0};this._loadingStart=()=>{}}connectedCallback(){super.connectedCallback(),this._form=document.querySelector("qs-form"),this._form&&(this._form.addEventListener(O,this._handleGroupedResult),this._form.addEventListener(I,this._handleResult),this._form.addEventListener(V,this._loadingStart))}disconnectedCallback(){this._form&&(this._form.removeEventListener(O,this._handleGroupedResult),this._form.removeEventListener(I,this._handleResult),this._form.removeEventListener(V,this._loadingStart)),super.disconnectedCallback()}get _isSearchMode(){return!!this._groupedResult?.categories?.length}render(){return this._isSearchMode?this._renderSearchToc():this.categories&&this.categories.length>0?this._renderBrowseToc():h`<slot></slot>`}_renderBrowseToc(){return h`
      <div class="toc">
        <h3>Categories</h3>
        <ul>
          ${this.categories.map(e=>h`
            <li>
              <a @click=${r=>this._scrollToCategory(r,e.id)}>${e.title}</a>
              ${e.subcategories&&e.subcategories.length>0?h`
                <ul>
                  ${e.subcategories.map(r=>h`
                    <li><a @click=${i=>this._scrollToCategory(i,r.id)}>${r.title}</a></li>
                  `)}
                </ul>
              `:""}
            </li>
          `)}
        </ul>
      </div>
    `}_renderSearchToc(){return h`
      <div class="toc">
        <h3>Categories</h3>
        <ul>
          ${this._groupedResult.categories.map(e=>{let i=(this.categoriesMeta?.[e.category]||{}).title||e.category;return h`
              <li>
                <a @click=${o=>this._scrollToCategory(o,e.category)}>
                  ${i} <span class="count">(${e.hitCount})</span>
                </a>
              </li>
            `})}
        </ul>
      </div>
    `}_scrollToCategory(e,r){e.preventDefault();let i=document.querySelector("qs-target");if(i?.shadowRoot){let o=i.shadowRoot.querySelector(`qs-guide-group[category="${CSS.escape(r)}"]`);o||(o=i.querySelector(`qs-guide-group[category="${CSS.escape(r)}"]`)),o&&o.scrollIntoView({behavior:"instant",block:"start"})}}};H.styles=S`
    :host {
      display: block;
    }

    h3 {
      margin: 0 0 0.4rem 0;
      font-size: 1.125rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--main-text-color);
    }

    ul {
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .toc > ul > li {
      display: block;
      margin-bottom: 0.35rem;
      padding: 0;
    }

    .toc > ul > li > a {
      display: block;
      padding: 0.15rem 0.4rem;
      font-size: 1rem;
      font-weight: 600;
      line-height: 1.5;
      color: var(--main-text-color);
      text-decoration: none;
      border-radius: 3px;
      cursor: pointer;
    }

    .toc > ul > li > a:hover {
      background-color: var(--hover-background-color, rgba(0, 0, 0, 0.05));
      color: var(--link-color, #1259A5);
    }

    /* Subcategory chips */
    .toc > ul > li > ul {
      padding: 0.1rem 0 0.2rem 0.6rem;
      display: flex;
      flex-wrap: wrap;
      gap: 0.25rem;
    }

    .toc > ul > li > ul > li {
      display: inline;
      line-height: 1;
    }

    .toc > ul > li > ul > li > a {
      display: inline-block;
      padding: 0.1rem 0.45rem;
      font-size: 0.9rem;
      line-height: 1.5;
      color: var(--main-text-color);
      opacity: 0.75;
      text-decoration: none;
      border-radius: 8px;
      cursor: pointer;
    }

    .toc > ul > li > ul > li > a:hover {
      opacity: 1;
      color: var(--link-hover-color, white);
      background-color: var(--link-color, #1259A5);
    }

    .count {
      font-weight: 400;
      font-size: 0.75rem;
      opacity: 0.7;
    }
  `,c([d({type:Array})],H.prototype,"categories",2),c([d({type:Object,attribute:"categories-meta"})],H.prototype,"categoriesMeta",2),c([w()],H.prototype,"_groupedResult",2),H=c([E("qs-categories-toc")],H);var Ve={ATTRIBUTE:1,CHILD:2,PROPERTY:3,BOOLEAN_ATTRIBUTE:4,EVENT:5,ELEMENT:6},Ze=s=>(...t)=>({_$litDirective$:s,values:t}),pt=class{constructor(t){}get _$AU(){return this._$AM._$AU}_$AT(t,e,r){this._$Ct=t,this._$AM=e,this._$Ci=r}_$AS(t,e){return this.update(t,e)}update(t,e){return this.render(...e)}};var st=class extends pt{constructor(t){if(super(t),this.it=p,t.type!==Ve.CHILD)throw Error(this.constructor.directiveName+"() can only be used in child bindings")}render(t){if(t===p||t==null)return this._t=void 0,this.it=t;if(t===R)return t;if(typeof t!="string")throw Error(this.constructor.directiveName+"() called with a non-string value");if(t===this.it)return this._t;this.it=t;let e=[t];return e.raw=e,this._t={_$litType$:this.constructor.resultType,strings:e,values:[]}}};st.directiveName="unsafeHTML",st.resultType=1;var Z=Ze(st);var v=class extends b{constructor(){super(...arguments);this.type="default";this.origin="quarkus";this.pinned=!1;this.originsWithRelativeUrls=[]}willUpdate(e){if(e.has("data")&&this.data)for(let r in this.data)this.data.hasOwnProperty(r)&&(this[r]=this.data[r])}render(){let e=this.pinned?"qs-guide--pinned":"";return h`
      <div class="qs-hit qs-guide ${e}" aria-label="Guide Hit" @click="${this._handleCardClick}" @auxclick="${this._handleCardAuxClick}">
        <div class="qs-guide-header">
          <h4>
            <a href="${this.relativizeUrl()}">${this._renderHTML(this.title)}</a>
          </h4>
          <div class="qs-guide-badges">
            ${this.status?h`<span class="status-tag status-${this.status}" title="${this._statusHint()}">${this.status}</span>`:""}
            ${this.origin&&this.origin.toLowerCase()!=="quarkus"?h`<a href="${this._originLink()}" target="_blank" class="origin ${this.origin}" title="${this._originTitle()}">${Z(this._originIcon())}</a>`:""}
          </div>
        </div>

        <div class="qs-guide-body">
          <div class="qs-guide-icon">
            ${Z(this.icon())}
          </div>
          <p class="qs-guide-summary">${this._renderHTML(this.summary)}</p>
        </div>
      </div>
    `}_handleCardClick(e){if(e.target.closest("a"))return;let r=e.ctrlKey||e.metaKey;window.open(this.relativizeUrl(),r?"_blank":"_self")}_handleCardAuxClick(e){e.button===1&&(e.target.closest("a")||(e.preventDefault(),window.open(this.relativizeUrl(),"_blank")))}_renderTags(){let e=this._parseTags();return e.length===0?"":h`
      <div class="qs-guide-tags">
        ${e.map(r=>h`<span class="qs-guide-tag">${r}</span>`)}
      </div>
    `}_parseTags(){let e=[];if(this.keywords){let r=this.keywords.replace(/<[^>]*>/g,"").split(/[,]+/).map(i=>i.trim()).filter(i=>i.length>0);e.push(...r)}if(this.categories)if(Array.isArray(this.categories))e.push(...this.categories);else{let r=this.categories.replace(/<[^>]*>/g,"").split(/[,]+/).map(i=>i.trim()).filter(i=>i.length>0);e.push(...r)}return e}relativizeUrl(){if(this.originsWithRelativeUrls.includes(this.origin)&&!this.url.startsWith("/"))try{return this.url.substring(new URL(this.url).origin.length)}catch{return this.url}else return this.url}icon(){let e=k.docs[this.type];return this._iconToSvg(e)}_renderHTML(e){return e&&(Array.isArray(e)?e.map(r=>h`<p>${this._renderHTML(r)}</p>`):Z(e))}_originTitle(){return this.origin==="quarkiverse-hub"?"Quarkus extension project contributed by the community":this.origin}_originLink(){return this.origin==="quarkiverse-hub"?"https://github.com/quarkiverse":"#"}_originIcon(){let e=k.origins[this.origin==="quarkiverse-hub"?"quarkiverse":this.origin];return this._iconToSvg(e)}_iconToSvg(e){if(e){let r=e.match(/.*(<svg.*<\/svg>)/);if(r)return r[1].replaceAll("%23","#")}return""}_statusHint(){switch(this.status){case"stable":return"Backward compatibility and presence in the ecosystem are taken very seriously";case"experimental":return"Early feedback is requested to mature the idea";case"preview":return"Backward compatibility and presence in the ecosystem is not guaranteed";case"deprecated":return"This extension is likely to be replaced or removed";default:return""}}};v.styles=S`
      :host {
          display: block;
      }
      .highlighted {
          font-weight: bold;
      }

      .qs-guide {
          display: flex;
          flex-direction: column;
          gap: 0.75rem;
          border: 1px solid var(--card-border-color);
          border-radius: 10px;
          padding: 1rem;
          cursor: pointer;
          position: relative;
          overflow: hidden;
          transition: border-color 0.2s ease, box-shadow 0.2s ease;
          box-sizing: border-box;
          height: 100%;
      }

      .qs-guide:hover {
          border-color: var(--card-border-hover-color);
          background-color: var(--card-border-hover-color);
      }

      .qs-guide--pinned {
          background-color: var(--card-pinned-bg-color);
          border-color: var(--card-pinned-border-color);
      }

      .qs-guide--pinned::before {
          opacity: 1;
      }

      .qs-guide--pinned:hover {
          border-color: var(--card-accent-color);
      }

      .qs-guide-header {
          display: flex;
          align-items: flex-start;
          justify-content: space-between;
          gap: 0.5rem;
      }

      .qs-guide-header h4 {
          margin: 0;
          font-size: 1.15rem;
          font-weight: 600;
          line-height: 1.375;
      }

      .qs-guide a {
          color: var(--title-text-color, #1259A5);
          text-decoration: none;
          transition: color 0.15s ease;
      }

      .qs-guide:hover a {
          color: var(--card-accent-color);
      }

      .qs-guide-badges {
          display: flex;
          align-items: center;
          gap: 0.375rem;
          flex-shrink: 0;
          margin-top: 0.125rem;
      }

      .status-tag {
          cursor: default;
          font-size: 0.625rem;
          line-height: 1;
          text-transform: uppercase;
          font-weight: 600;
          display: inline-block;
          padding: 0.25rem 0.5rem;
          border-radius: 9999px;
          border: 1px solid transparent;
          letter-spacing: 0.05em;
      }

      .status-stable {
          color: var(--tag-stable-text-color, #047857);
          background-color: var(--tag-stable-background-color, #ecfdf5);
          border-color: var(--tag-stable-border-color, #a7f3d0);
      }

      .status-preview {
          color: var(--tag-preview-text-color);
          background-color: var(--tag-preview-background-color);
      }

      .status-deprecated {
          color: var(--tag-deprecated-text-color);
          background-color: var(--tag-deprecated-background-color);
      }

      .status-experimental {
          color: var(--tag-experimental-text-color);
          background-color: var(--tag-experimental-background-color);
      }

      .qs-guide-body {
          display: flex;
          gap: 0.75rem;
          align-items: flex-start;
      }

      .qs-guide-icon {
          flex-shrink: 0;
          margin-top: 0.125rem;
      }

      .qs-guide-icon svg {
          width: 32px;
          height: 32px;
          fill: var(--main-text-color);
      }

      .qs-guide-summary {
          font-size: 0.9rem;
          line-height: 1.625;
          color: var(--main-text-color);
          margin: 0;
          display: -webkit-box;
          -webkit-box-orient: vertical;
          -webkit-line-clamp: 3;
          overflow: hidden;

          p {
              margin: 0;
          }
      }

      .qs-guide--pinned .qs-guide-summary {
          -webkit-line-clamp: 2;
      }

      .qs-guide-tags {
          display: flex;
          flex-wrap: wrap;
          gap: 0.375rem;
          margin-top: auto;
          padding-top: 0.25rem;
      }

      .qs-guide-tag {
          display: inline-block;
          padding: 0.125rem 0.5rem;
          border-radius: 0.25rem;
          font-size: 0.6875rem;
          font-weight: 500;
          background-color: var(--tag-chip-bg);
          color: var(--tag-chip-text);
          border: 1px solid var(--tag-chip-border);
          font-family: ui-monospace, monospace;
      }

      .qs-guide .origin {
          background-size: 100px 25px;
          background-repeat: no-repeat;
          background-position: center;
          width: 100px;
          height: 25px;
          display: inline-block;
          vertical-align: middle;
      }

      .qs-guide .origin.quarkus {
          background-image: url('${q(k.origins.quarkus)}');
      }

      .qs-guide .origin.quarkiverse-hub {
          background-image: url('${q(k.origins.quarkiverse)}');
      }
  `,c([d({type:Object})],v.prototype,"data",2),c([d({type:String})],v.prototype,"type",2),c([d({type:String})],v.prototype,"status",2),c([d({type:String})],v.prototype,"url",2),c([d({type:String})],v.prototype,"title",2),c([d({type:String})],v.prototype,"summary",2),c([d({type:String})],v.prototype,"keywords",2),c([d({type:String})],v.prototype,"origin",2),c([d({type:String})],v.prototype,"categories",2),c([d({type:Boolean})],v.prototype,"pinned",2),c([d({type:String,attribute:"origins-with-relative-urls"})],v.prototype,"originsWithRelativeUrls",2),v=c([E("qs-guide")],v);var y=class extends b{constructor(){super(...arguments);this.category="";this.title="";this.description="";this.hitCount=0;this.hits=[];this.searchContext=null;this.originsWithRelativeUrls=[];this.subgroup=!1;this.pinned=!1;this._additionalHits=[];this._loading=!1;this._handleShowMore=async()=>{if(!(this._loading||!this.searchContext)){this._loading=!0;try{let e=this._allHits,r=new URLSearchParams;this.searchContext.query&&r.append("q",this.searchContext.query),r.append("categories",this.category),r.append("language",this.searchContext.language||"en"),this.searchContext.version&&r.append("version",this.searchContext.version),r.append("contentSnippets","0");for(let l of e){let g=l.url||l.id;g&&r.append("excludeIds",g)}let i=`${this.searchContext.server||""}/api/guides/search?${r.toString()}`,o=new AbortController,n=setTimeout(()=>o.abort(),5e3),a=await fetch(i,{method:"GET",signal:o.signal});if(clearTimeout(n),a.ok){let l=await a.json();l.hits&&l.hits.length>0&&(this._additionalHits=[...this._additionalHits,...l.hits])}else console.error("Failed to fetch more guides:",a.status)}catch(e){console.error("Error fetching more guides:",e)}finally{this._loading=!1}}}}willUpdate(e){(e.has("hits")||e.has("category"))&&(this._additionalHits=[])}get _allHits(){return[...this.hits||[],...this._additionalHits]}get _isSearchMode(){return!!(this.hits&&this.hits.length>0)}get _displayedCount(){return this._allHits.length}get _hasMore(){return this.hitCount>this._displayedCount}get _remainingCount(){return Math.max(0,this.hitCount-this._displayedCount)}render(){return this._isSearchMode?this._renderSearchMode():this._renderBrowseMode()}_renderBrowseMode(){let e=this.pinned?"qs-guide-group-pinned":"";return h`
      <div class="qs-guide-group ${e}">
        ${this._renderHeader()}
        <slot></slot>
      </div>
    `}_renderSearchMode(){let e=this._allHits;return h`
      <div class="qs-guide-group">
        ${this._renderHeader()}
        <div class="qs-guide-group-content">
          ${e.map(r=>h`
            <qs-guide .data=${r} origins-with-relative-urls=${this.originsWithRelativeUrls}></qs-guide>
          `)}
        </div>
        ${this._hasMore?this._renderShowMore():""}
      </div>
    `}_renderHeader(){let e=this.subgroup,r=this.title||this.category;return h`
      <div class="qs-guide-group-header">
        ${e?h`<h3>${r}</h3>`:h`<h2>
                  ${r}
                  ${this.hitCount>0?h`<span class="count">(${this.hitCount})</span>`:""}
                  ${this.description?h`<span class="qs-guide-group-description">${this.description}</span>`:""}
                </h2>`}
      </div>
    `}_renderShowMore(){let e=this._remainingCount;return h`
      <div class="qs-guide-group-more">
        <button @click=${this._handleShowMore} ?disabled=${this._loading}>
          ${this._loading?h`Loading<span class="loading-spinner"></span>`:h`Show remaining ${e} guides <span class="chevron">▼</span>`}
        </button>
      </div>
    `}};y.styles=S`
    .qs-guide-group {
      margin-bottom: 1.5rem;
    }

    .qs-guide-group-header {
      display: flex;
      align-items: baseline;
      margin-bottom: 0.95rem;
    }

    .qs-guide-group-header h2,
    .qs-guide-group-header h3 {
      margin: 0;
      font-weight: 600;
      white-space: nowrap;
    }

    .qs-guide-group-header h2 {
        font-size: 1.875rem;
        margin: 2rem 0 .75rem;
    }

    .qs-guide-group-header h3 {
        font-size: 1.05rem;
        padding-bottom: .5rem;
        color: var(--sub-title-text-color);
    }

    .count {
      margin-left: 0.5rem;
      font-size: 1.5rem;
      color: var(--content-highlight-color, #777);
      white-space: nowrap;
    }

    .title-line {
      background-color: color-mix(in srgb, var(--card-border-color) 33%, transparent);
      flex: 1;
      height: 1px;
      margin-left: 10px;
    }

    .qs-guide-group-description {
      margin: 0 0 0.5rem 1rem;
      color: var(--content-highlight-color, #555555);
      font-size: 1rem;
    }

    .qs-guide-group-content {
      display: grid;
      grid-template-columns: repeat(12, 1fr);
      grid-gap: 0.75rem;
      margin-bottom: 0.5rem;
    }

    .qs-guide-group-content qs-guide {
      grid-column: span 4;
    }

    @media screen and (max-width: 1300px) {
      .qs-guide-group-content qs-guide {
        grid-column: span 6;
      }
    }

    @media screen and (max-width: 1200px) {
      .qs-guide-group-description {
        margin: 0.5rem 0 0.5rem 0;
        display: block;
        white-space: normal
      }
    }

    @media screen and (max-width: 768px) {
      .qs-guide-group-content qs-guide {
        grid-column: span 12;
      }
    }

    .qs-guide-group-more {
      text-align: end;
      padding: 0.5rem 0 1rem 0;
    }

    .qs-guide-group-more button {
      background: none;
      border: none;
      border-radius: 0.5rem;
      padding: 0.5rem 1.5rem;
      cursor: pointer;
      font-size: 0.85rem;
      color: var(--main-text-color, black);
      transition: background-color 0.2s ease, border-color 0.2s ease;
    }

    .qs-guide-group-more button:hover {
      border-color: var(--card-border-hover-color, var(--link-color, #1259A5));
      background-color: var(--tag-chip-bg, #eef2f8);
    }

    .qs-guide-group-more button:disabled {
      opacity: 0.6;
      cursor: wait;
    }

    .chevron {
      display: inline-block;
      margin-left: 0.5rem;
      font-size: 0.7rem;
    }

    .loading-spinner {
      display: inline-block;
      width: 16px;
      height: 16px;
      background-image: url('${q(k.loading)}');
      background-repeat: no-repeat;
      background-size: contain;
      vertical-align: middle;
      margin-left: 0.5rem;
    }
      
    .qs-guide-group-pinned .qs-guide-group-header h2 {
      color: var(--card-accent-color)
    }

    .qs-guide-group-pinned .qs-guide-group-header div.title-line {
      background-color: var(--card-accent-color)
    }
  `,c([d({type:String})],y.prototype,"category",2),c([d({type:String})],y.prototype,"title",2),c([d({type:String})],y.prototype,"description",2),c([d({type:Number,attribute:"hit-count"})],y.prototype,"hitCount",2),c([d({type:Array})],y.prototype,"hits",2),c([d({type:Object,attribute:!1})],y.prototype,"searchContext",2),c([d({type:String,attribute:"origins-with-relative-urls"})],y.prototype,"originsWithRelativeUrls",2),c([d({type:Boolean})],y.prototype,"subgroup",2),c([d({type:Boolean})],y.prototype,"pinned",2),c([w()],y.prototype,"_additionalHits",2),c([w()],y.prototype,"_loading",2),y=c([E("qs-guide-group")],y);var We=Ut(Tt());var C=class extends b{constructor(){super(...arguments);this.searchResultsTitle="";this.type="guide";this.originsWithRelativeUrls=[];this.categoriesMeta={};this._loading=!0;this._handleScroll=e=>{if(this._loading||!this._result)return;if(!this._result.hasMoreHits){console.debug("no more hits");return}let r=this._hits.length==0?null:this._hits[this._hits.length-1];if(!r)return;let i=document.documentElement,o=i.scrollTop+i.clientHeight,n=r.offsetTop;o>=n&&(this._loading=!0,this._form.dispatchEvent(new CustomEvent(ut)))};this._handleScrollDebounced=(0,We.default)(this._handleScroll,100);this._handleGroupedResult=e=>{console.debug("Received grouped results in qs-target: ",e.detail),this._loadingEnd(),this._result=void 0,e.detail?.categories?document.body.classList.add("qs-has-results"):document.body.classList.remove("qs-has-results"),this._groupedResult=e.detail};this._handleResult=e=>{if(console.debug("Received results in qs-target: ",e.detail),this._loadingEnd(),this._groupedResult=void 0,!this._result||!e.detail||!e.detail.hits||e.detail.page===0){e.detail?.hits?document.body.classList.add("qs-has-results"):document.body.classList.remove("qs-has-results"),this._result=e.detail;return}this._result.hits=this._result.hits.concat(e.detail.hits),console.debug(`${this._result.hits.length} results in qs-target: `),this._result.hasMoreHits=e.detail.hasMoreHits};this._loadingStart=e=>{this._loading=!0,e.detail.page===0&&(this._result=void 0)};this._loadingEnd=()=>{this._loading=!1}}connectedCallback(){super.connectedCallback(),this._form=document.querySelector("qs-form"),this._form.addEventListener(I,this._handleResult),this._form.addEventListener(O,this._handleGroupedResult),this._form.addEventListener(V,this._loadingStart),document.addEventListener("scroll",this._handleScrollDebounced)}disconnectedCallback(){this._form.removeEventListener(I,this._handleResult),this._form.removeEventListener(O,this._handleGroupedResult),this._form.removeEventListener(V,this._loadingStart),document.removeEventListener("scroll",this._handleScrollDebounced),super.disconnectedCallback()}render(){if(this._groupedResult?.categories)return this._groupedResult.categories.length===0?h`
          <div id="qs-target" class="no-hits">
            <p>Sorry, no ${this.type}s matched your search. Please try again.</p>
          </div>
        `:h`
        ${this.searchResultsTitle===""?"":h`<h1 class="search-result-title">${this.searchResultsTitle}</h1>`}
        ${this._groupedResult.suggestion?h`
          <div class="result-message">
            <p>No ${this.type}s matched your original search query.
                Showing results for <span class="suggestion" @click=${this._querySuggestion}>${Z(this._groupedResult.suggestion.highlighted)}</span> instead.</p>
          </div>
        `:""}
        <div id="qs-target" class="qs-grouped-hits" aria-label="Search Hits">
          ${this._groupedResult.categories.map(e=>this._renderGroup(e))}
        </div>
        ${this._loading?this._renderLoading():""}
      `;if(this._result?.hits){if(this._result.hits.length===0)return h`
          <div id="qs-target" class="no-hits">
            <p>Sorry, no ${this.type}s matched your search. Please try again.</p>
          </div>
        `;let e=this._result.hits.map(r=>this._renderHit(r));return this._result.suggestion?h`
          ${this.searchResultsTitle===""?"":h`<h1 class="search-result-title">${this.searchResultsTitle}</h1>`}
            <div class="result-message">
              <p class="">No ${this.type}s matched your original search query.
                  Showing results for <span class="suggestion" @click=${this._querySuggestion}>${Z(this._result.suggestion.highlighted)}</span> instead.</p>
            </div>
            <div id="qs-target" class="qs-hits" aria-label="Search Hits">
              ${e}
            </div>
            ${this._loading?this._renderLoading():""}
          `:h`
          ${this.searchResultsTitle===""?"":h`<h1 class="search-result-title">${this.searchResultsTitle}</h1>`}
          <div id="qs-target" class="qs-hits" aria-label="Search Hits">
            ${e}
          </div>
          ${this._loading?this._renderLoading():""}
        `}return this._loading?h`
        <div id="qs-target">${this._renderLoading()}</div>`:h`
      <div id="qs-target">
        <slot></slot>
      </div>
    `}_renderLoading(){return h`
      <div class="loading">Searching...</div>
    `}_renderGroup(e){let r=this.categoriesMeta?.[e.category]||{},i=r.title||e.category,o=r.description||"",n=this._groupedResult?{server:this._groupedResult.server||"",query:this._groupedResult.search?.q||"",language:this._groupedResult.search?.language||"en",version:this._groupedResult.search?.version||void 0}:null;return h`
      <qs-guide-group
        category=${e.category}
        title=${i}
        description=${o}
        hit-count=${e.hitCount}
        .hits=${e.hits}
        .searchContext=${n}
        origins-with-relative-urls=${this.originsWithRelativeUrls}
      ></qs-guide-group>
    `}_renderHit(e){switch(this.type){case"guide":return h`
          <qs-guide class="qs-hit" .data=${e} origins-with-relative-urls=${this.originsWithRelativeUrls}></qs-guide>`}return""}_querySuggestion(){let e=this._groupedResult?.suggestion||this._result?.suggestion;this._form.dispatchEvent(new CustomEvent(gt,{detail:{suggestion:e}}))}};C.styles=S`
    
    .loading {
      background-image: url('${q(k.loading)}');
      background-repeat: no-repeat;
      background-position: top;
      background-size: 45px;
      padding-top: 55px;
      text-align: center;
      padding-bottom: 70px;
    }
    
    .qs-hits {
      display: grid;
      grid-template-columns: repeat(12, 1fr);
      grid-gap: 0.75rem;
      clear: both;
      margin-bottom: 4em;
    }
    
    .no-hits {
      padding: 10px;
      margin: 6em 10px 6em 10px;
      font-size: 1.2rem;
      line-height: 1.5;
      font-weight: 400;
      font-style: italic;
      text-align: center;
      background: var(--empty-background-color, #F0CA4D);
      box-shadow: 0 4px 10px 0 rgba(0, 0, 0, 0.05), 0 4px 20px 0 rgba(0, 0, 0, 0.05);
    }

    .search-result-title {
      margin-top: 2.5rem;
      font-weight: var(--heading-font-weight);
    }

    .result-message {
      box-shadow: 0 4px 10px 0 rgba(0, 0, 0, 0.05), 0 4px 20px 0 rgba(0, 0, 0, 0.05);
      background: var(--empty-background-color, #F0CA4D);
      padding: 10px 10px 10px 90px;
      font-size: 0.9rem;
      
      .suggestion {
        text-decoration: underline;
        cursor: pointer;
        .highlighted {
          font-weight: bold;
        }
      }
    }

    qs-guide {
      grid-column: span 4;

      @media screen and (max-width: 1300px) {
        grid-column: span 6;
      }

      @media screen and (max-width: 768px) {
        grid-column: span 12;
      }
    }
   
  `,c([d({type:String,attribute:"search-results-title"})],C.prototype,"searchResultsTitle",2),c([d({type:String})],C.prototype,"type",2),c([d({type:String,attribute:"origins-with-relative-urls"})],C.prototype,"originsWithRelativeUrls",2),c([d({type:Object,attribute:"categories-meta"})],C.prototype,"categoriesMeta",2),c([w()],C.prototype,"_result",2),c([w()],C.prototype,"_groupedResult",2),c([w()],C.prototype,"_loading",2),c([ae(".qs-hit")],C.prototype,"_hits",2),C=c([E("qs-target")],C);export{z as LocalSearch,O as QS_GROUPED_RESULT_EVENT,ut as QS_NEXT_PAGE_EVENT,gt as QS_QUERY_SUGGESTION_EVENT,I as QS_RESULT_EVENT,V as QS_START_EVENT,H as QsCategoriesToc,_ as QsForm,v as QsGuide,y as QsGuideGroup,C as QsTarget};
/*! Bundled license information:

@lit/reactive-element/css-tag.js:
  (**
   * @license
   * Copyright 2019 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/reactive-element.js:
lit-html/lit-html.js:
lit-element/lit-element.js:
@lit/reactive-element/decorators/custom-element.js:
@lit/reactive-element/decorators/property.js:
@lit/reactive-element/decorators/state.js:
@lit/reactive-element/decorators/event-options.js:
@lit/reactive-element/decorators/base.js:
@lit/reactive-element/decorators/query.js:
@lit/reactive-element/decorators/query-all.js:
@lit/reactive-element/decorators/query-async.js:
@lit/reactive-element/decorators/query-assigned-nodes.js:
lit-html/directive.js:
lit-html/directives/unsafe-html.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

lit-html/is-server.js:
  (**
   * @license
   * Copyright 2022 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/query-assigned-elements.js:
  (**
   * @license
   * Copyright 2021 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/

