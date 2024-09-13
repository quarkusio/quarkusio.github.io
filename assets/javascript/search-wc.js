var je=Object.create;var ut=Object.defineProperty;var qt=Object.getOwnPropertyDescriptor;var Ze=Object.getOwnPropertyNames;var De=Object.getPrototypeOf,Ie=Object.prototype.hasOwnProperty;var f=(i,t)=>()=>(t||i((t={exports:{}}).exports,t),t.exports);var ze=(i,t,e,r)=>{if(t&&typeof t=="object"||typeof t=="function")for(let s of Ze(t))!Ie.call(i,s)&&s!==e&&ut(i,s,{get:()=>t[s],enumerable:!(r=qt(t,s))||r.enumerable});return i};var Pt=(i,t,e)=>(e=i!=null?je(De(i)):{},ze(t||!i||!i.__esModule?ut(e,"default",{value:i,enumerable:!0}):e,i));var h=(i,t,e,r)=>{for(var s=r>1?void 0:r?qt(t,e):t,n=i.length-1,o;n>=0;n--)(o=i[n])&&(s=(r?o(t,e,s):o(s))||s);return r&&s&&ut(t,e,s),s};var St=f((Ki,Xt)=>{function ar(i){var t=typeof i;return i!=null&&(t=="object"||t=="function")}Xt.exports=ar});var Yt=f((Xi,Gt)=>{var lr=typeof global=="object"&&global&&global.Object===Object&&global;Gt.exports=lr});var Et=f((Gi,Qt)=>{var cr=Yt(),hr=typeof self=="object"&&self&&self.Object===Object&&self,dr=cr||hr||Function("return this")();Qt.exports=dr});var ee=f((Yi,te)=>{var ur=Et(),pr=function(){return ur.Date.now()};te.exports=pr});var ie=f((Qi,re)=>{var gr=/\s/;function mr(i){for(var t=i.length;t--&&gr.test(i.charAt(t)););return t}re.exports=mr});var ne=f((ts,se)=>{var fr=ie(),vr=/^\s+/;function yr(i){return i&&i.slice(0,fr(i)+1).replace(vr,"")}se.exports=yr});var wt=f((es,oe)=>{var _r=Et(),br=_r.Symbol;oe.exports=br});var he=f((rs,ce)=>{var ae=wt(),le=Object.prototype,$r=le.hasOwnProperty,xr=le.toString,G=ae?ae.toStringTag:void 0;function Ar(i){var t=$r.call(i,G),e=i[G];try{i[G]=void 0;var r=!0}catch{}var s=xr.call(i);return r&&(t?i[G]=e:delete i[G]),s}ce.exports=Ar});var ue=f((is,de)=>{var Sr=Object.prototype,Er=Sr.toString;function wr(i){return Er.call(i)}de.exports=wr});var fe=f((ss,me)=>{var pe=wt(),Cr=he(),Lr=ue(),Mr="[object Null]",Hr="[object Undefined]",ge=pe?pe.toStringTag:void 0;function Tr(i){return i==null?i===void 0?Hr:Mr:ge&&ge in Object(i)?Cr(i):Lr(i)}me.exports=Tr});var ye=f((ns,ve)=>{function kr(i){return i!=null&&typeof i=="object"}ve.exports=kr});var be=f((os,_e)=>{var qr=fe(),Pr=ye(),Or="[object Symbol]";function Rr(i){return typeof i=="symbol"||Pr(i)&&qr(i)==Or}_e.exports=Rr});var Se=f((as,Ae)=>{var Nr=ne(),$e=St(),Ur=be(),xe=NaN,Vr=/^[-+]0x[0-9a-f]+$/i,jr=/^0b[01]+$/i,Zr=/^0o[0-7]+$/i,Dr=parseInt;function Ir(i){if(typeof i=="number")return i;if(Ur(i))return xe;if($e(i)){var t=typeof i.valueOf=="function"?i.valueOf():i;i=$e(t)?t+"":t}if(typeof i!="string")return i===0?i:+i;i=Nr(i);var e=jr.test(i);return e||Zr.test(i)?Dr(i.slice(2),e?2:8):Vr.test(i)?xe:+i}Ae.exports=Ir});var Lt=f((ls,we)=>{var zr=St(),Ct=ee(),Ee=Se(),Br="Expected a function",Wr=Math.max,Fr=Math.min;function Jr(i,t,e){var r,s,n,o,l,a,d=0,g=!1,c=!1,y=!0;if(typeof i!="function")throw new TypeError(Br);t=Ee(t)||0,zr(e)&&(g=!!e.leading,c="maxWait"in e,n=c?Wr(Ee(e.maxWait)||0,t):n,y="trailing"in e?!!e.trailing:y);function x(m){var w=r,Z=s;return r=s=void 0,d=m,o=i.apply(Z,w),o}function P(m){return d=m,l=setTimeout(tt,t),g?x(m):o}function Ne(m){var w=m-a,Z=m-d,kt=t-w;return c?Fr(kt,n-Z):kt}function Ht(m){var w=m-a,Z=m-d;return a===void 0||w>=t||w<0||c&&Z>=n}function tt(){var m=Ct();if(Ht(m))return Tt(m);l=setTimeout(tt,Ne(m))}function Tt(m){return l=void 0,y&&r?x(m):(r=s=void 0,o)}function Ue(){l!==void 0&&clearTimeout(l),d=0,r=a=s=l=void 0}function Ve(){return l===void 0?o:Tt(Ct())}function dt(){var m=Ct(),w=Ht(m);if(r=arguments,s=this,a=m,w){if(l===void 0)return P(a);if(c)return clearTimeout(l),l=setTimeout(tt,t),x(a)}return l===void 0&&(l=setTimeout(tt,t)),o}return dt.cancel=Ue,dt.flush=Ve,dt}we.exports=Jr});var et=globalThis,rt=et.ShadowRoot&&(et.ShadyCSS===void 0||et.ShadyCSS.nativeShadow)&&"adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,pt=Symbol(),Ot=new WeakMap,D=class{constructor(t,e,r){if(this._$cssResult$=!0,r!==pt)throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=t,this.t=e}get styleSheet(){let t=this.o,e=this.t;if(rt&&t===void 0){let r=e!==void 0&&e.length===1;r&&(t=Ot.get(e)),t===void 0&&((this.o=t=new CSSStyleSheet).replaceSync(this.cssText),r&&Ot.set(e,t))}return t}toString(){return this.cssText}},L=i=>new D(typeof i=="string"?i:i+"",void 0,pt),M=(i,...t)=>{let e=i.length===1?i[0]:t.reduce((r,s,n)=>r+(o=>{if(o._$cssResult$===!0)return o.cssText;if(typeof o=="number")return o;throw Error("Value passed to 'css' function must be a 'css' function result: "+o+". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.")})(s)+i[n+1],i[0]);return new D(e,i,pt)},gt=(i,t)=>{if(rt)i.adoptedStyleSheets=t.map(e=>e instanceof CSSStyleSheet?e:e.styleSheet);else for(let e of t){let r=document.createElement("style"),s=et.litNonce;s!==void 0&&r.setAttribute("nonce",s),r.textContent=e.cssText,i.appendChild(r)}},it=rt?i=>i:i=>i instanceof CSSStyleSheet?(t=>{let e="";for(let r of t.cssRules)e+=r.cssText;return L(e)})(i):i;var{is:Be,defineProperty:We,getOwnPropertyDescriptor:Fe,getOwnPropertyNames:Je,getOwnPropertySymbols:Ke,getPrototypeOf:Xe}=Object,st=globalThis,Rt=st.trustedTypes,Ge=Rt?Rt.emptyScript:"",Ye=st.reactiveElementPolyfillSupport,I=(i,t)=>i,z={toAttribute(i,t){switch(t){case Boolean:i=i?Ge:null;break;case Object:case Array:i=i==null?i:JSON.stringify(i)}return i},fromAttribute(i,t){let e=i;switch(t){case Boolean:e=i!==null;break;case Number:e=i===null?null:Number(i);break;case Object:case Array:try{e=JSON.parse(i)}catch{e=null}}return e}},nt=(i,t)=>!Be(i,t),Nt={attribute:!0,type:String,converter:z,reflect:!1,hasChanged:nt};Symbol.metadata??=Symbol("metadata"),st.litPropertyMetadata??=new WeakMap;var S=class extends HTMLElement{static addInitializer(t){this._$Ei(),(this.l??=[]).push(t)}static get observedAttributes(){return this.finalize(),this._$Eh&&[...this._$Eh.keys()]}static createProperty(t,e=Nt){if(e.state&&(e.attribute=!1),this._$Ei(),this.elementProperties.set(t,e),!e.noAccessor){let r=Symbol(),s=this.getPropertyDescriptor(t,r,e);s!==void 0&&We(this.prototype,t,s)}}static getPropertyDescriptor(t,e,r){let{get:s,set:n}=Fe(this.prototype,t)??{get(){return this[e]},set(o){this[e]=o}};return{get(){return s?.call(this)},set(o){let l=s?.call(this);n.call(this,o),this.requestUpdate(t,l,r)},configurable:!0,enumerable:!0}}static getPropertyOptions(t){return this.elementProperties.get(t)??Nt}static _$Ei(){if(this.hasOwnProperty(I("elementProperties")))return;let t=Xe(this);t.finalize(),t.l!==void 0&&(this.l=[...t.l]),this.elementProperties=new Map(t.elementProperties)}static finalize(){if(this.hasOwnProperty(I("finalized")))return;if(this.finalized=!0,this._$Ei(),this.hasOwnProperty(I("properties"))){let e=this.properties,r=[...Je(e),...Ke(e)];for(let s of r)this.createProperty(s,e[s])}let t=this[Symbol.metadata];if(t!==null){let e=litPropertyMetadata.get(t);if(e!==void 0)for(let[r,s]of e)this.elementProperties.set(r,s)}this._$Eh=new Map;for(let[e,r]of this.elementProperties){let s=this._$Eu(e,r);s!==void 0&&this._$Eh.set(s,e)}this.elementStyles=this.finalizeStyles(this.styles)}static finalizeStyles(t){let e=[];if(Array.isArray(t)){let r=new Set(t.flat(1/0).reverse());for(let s of r)e.unshift(it(s))}else t!==void 0&&e.push(it(t));return e}static _$Eu(t,e){let r=e.attribute;return r===!1?void 0:typeof r=="string"?r:typeof t=="string"?t.toLowerCase():void 0}constructor(){super(),this._$Ep=void 0,this.isUpdatePending=!1,this.hasUpdated=!1,this._$Em=null,this._$Ev()}_$Ev(){this._$ES=new Promise(t=>this.enableUpdating=t),this._$AL=new Map,this._$E_(),this.requestUpdate(),this.constructor.l?.forEach(t=>t(this))}addController(t){(this._$EO??=new Set).add(t),this.renderRoot!==void 0&&this.isConnected&&t.hostConnected?.()}removeController(t){this._$EO?.delete(t)}_$E_(){let t=new Map,e=this.constructor.elementProperties;for(let r of e.keys())this.hasOwnProperty(r)&&(t.set(r,this[r]),delete this[r]);t.size>0&&(this._$Ep=t)}createRenderRoot(){let t=this.shadowRoot??this.attachShadow(this.constructor.shadowRootOptions);return gt(t,this.constructor.elementStyles),t}connectedCallback(){this.renderRoot??=this.createRenderRoot(),this.enableUpdating(!0),this._$EO?.forEach(t=>t.hostConnected?.())}enableUpdating(t){}disconnectedCallback(){this._$EO?.forEach(t=>t.hostDisconnected?.())}attributeChangedCallback(t,e,r){this._$AK(t,r)}_$EC(t,e){let r=this.constructor.elementProperties.get(t),s=this.constructor._$Eu(t,r);if(s!==void 0&&r.reflect===!0){let n=(r.converter?.toAttribute!==void 0?r.converter:z).toAttribute(e,r.type);this._$Em=t,n==null?this.removeAttribute(s):this.setAttribute(s,n),this._$Em=null}}_$AK(t,e){let r=this.constructor,s=r._$Eh.get(t);if(s!==void 0&&this._$Em!==s){let n=r.getPropertyOptions(s),o=typeof n.converter=="function"?{fromAttribute:n.converter}:n.converter?.fromAttribute!==void 0?n.converter:z;this._$Em=s,this[s]=o.fromAttribute(e,n.type),this._$Em=null}}requestUpdate(t,e,r){if(t!==void 0){if(r??=this.constructor.getPropertyOptions(t),!(r.hasChanged??nt)(this[t],e))return;this.P(t,e,r)}this.isUpdatePending===!1&&(this._$ES=this._$ET())}P(t,e,r){this._$AL.has(t)||this._$AL.set(t,e),r.reflect===!0&&this._$Em!==t&&(this._$Ej??=new Set).add(t)}async _$ET(){this.isUpdatePending=!0;try{await this._$ES}catch(e){Promise.reject(e)}let t=this.scheduleUpdate();return t!=null&&await t,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){if(!this.isUpdatePending)return;if(!this.hasUpdated){if(this.renderRoot??=this.createRenderRoot(),this._$Ep){for(let[s,n]of this._$Ep)this[s]=n;this._$Ep=void 0}let r=this.constructor.elementProperties;if(r.size>0)for(let[s,n]of r)n.wrapped!==!0||this._$AL.has(s)||this[s]===void 0||this.P(s,this[s],n)}let t=!1,e=this._$AL;try{t=this.shouldUpdate(e),t?(this.willUpdate(e),this._$EO?.forEach(r=>r.hostUpdate?.()),this.update(e)):this._$EU()}catch(r){throw t=!1,this._$EU(),r}t&&this._$AE(e)}willUpdate(t){}_$AE(t){this._$EO?.forEach(e=>e.hostUpdated?.()),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(t)),this.updated(t)}_$EU(){this._$AL=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this._$ES}shouldUpdate(t){return!0}update(t){this._$Ej&&=this._$Ej.forEach(e=>this._$EC(e,this[e])),this._$EU()}updated(t){}firstUpdated(t){}};S.elementStyles=[],S.shadowRootOptions={mode:"open"},S[I("elementProperties")]=new Map,S[I("finalized")]=new Map,Ye?.({ReactiveElement:S}),(st.reactiveElementVersions??=[]).push("2.0.4");var $t=globalThis,ot=$t.trustedTypes,Ut=ot?ot.createPolicy("lit-html",{createHTML:i=>i}):void 0,zt="$lit$",C=`lit$${Math.random().toFixed(9).slice(2)}$`,Bt="?"+C,Qe=`<${Bt}>`,k=document,W=()=>k.createComment(""),F=i=>i===null||typeof i!="object"&&typeof i!="function",xt=Array.isArray,tr=i=>xt(i)||typeof i?.[Symbol.iterator]=="function",mt=`[ 	
\f\r]`,B=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,Vt=/-->/g,jt=/>/g,H=RegExp(`>|${mt}(?:([^\\s"'>=/]+)(${mt}*=${mt}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`,"g"),Zt=/'/g,Dt=/"/g,Wt=/^(?:script|style|textarea|title)$/i,At=i=>(t,...e)=>({_$litType$:i,strings:t,values:e}),_=At(1),ci=At(2),hi=At(3),E=Symbol.for("lit-noChange"),p=Symbol.for("lit-nothing"),It=new WeakMap,T=k.createTreeWalker(k,129);function Ft(i,t){if(!xt(i)||!i.hasOwnProperty("raw"))throw Error("invalid template strings array");return Ut!==void 0?Ut.createHTML(t):t}var er=(i,t)=>{let e=i.length-1,r=[],s,n=t===2?"<svg>":t===3?"<math>":"",o=B;for(let l=0;l<e;l++){let a=i[l],d,g,c=-1,y=0;for(;y<a.length&&(o.lastIndex=y,g=o.exec(a),g!==null);)y=o.lastIndex,o===B?g[1]==="!--"?o=Vt:g[1]!==void 0?o=jt:g[2]!==void 0?(Wt.test(g[2])&&(s=RegExp("</"+g[2],"g")),o=H):g[3]!==void 0&&(o=H):o===H?g[0]===">"?(o=s??B,c=-1):g[1]===void 0?c=-2:(c=o.lastIndex-g[2].length,d=g[1],o=g[3]===void 0?H:g[3]==='"'?Dt:Zt):o===Dt||o===Zt?o=H:o===Vt||o===jt?o=B:(o=H,s=void 0);let x=o===H&&i[l+1].startsWith("/>")?" ":"";n+=o===B?a+Qe:c>=0?(r.push(d),a.slice(0,c)+zt+a.slice(c)+C+x):a+C+(c===-2?l:x)}return[Ft(i,n+(i[e]||"<?>")+(t===2?"</svg>":t===3?"</math>":"")),r]},J=class i{constructor({strings:t,_$litType$:e},r){let s;this.parts=[];let n=0,o=0,l=t.length-1,a=this.parts,[d,g]=er(t,e);if(this.el=i.createElement(d,r),T.currentNode=this.el.content,e===2||e===3){let c=this.el.content.firstChild;c.replaceWith(...c.childNodes)}for(;(s=T.nextNode())!==null&&a.length<l;){if(s.nodeType===1){if(s.hasAttributes())for(let c of s.getAttributeNames())if(c.endsWith(zt)){let y=g[o++],x=s.getAttribute(c).split(C),P=/([.?@])?(.*)/.exec(y);a.push({type:1,index:n,name:P[2],strings:x,ctor:P[1]==="."?vt:P[1]==="?"?yt:P[1]==="@"?_t:R}),s.removeAttribute(c)}else c.startsWith(C)&&(a.push({type:6,index:n}),s.removeAttribute(c));if(Wt.test(s.tagName)){let c=s.textContent.split(C),y=c.length-1;if(y>0){s.textContent=ot?ot.emptyScript:"";for(let x=0;x<y;x++)s.append(c[x],W()),T.nextNode(),a.push({type:2,index:++n});s.append(c[y],W())}}}else if(s.nodeType===8)if(s.data===Bt)a.push({type:2,index:n});else{let c=-1;for(;(c=s.data.indexOf(C,c+1))!==-1;)a.push({type:7,index:n}),c+=C.length-1}n++}}static createElement(t,e){let r=k.createElement("template");return r.innerHTML=t,r}};function O(i,t,e=i,r){if(t===E)return t;let s=r!==void 0?e.o?.[r]:e.l,n=F(t)?void 0:t._$litDirective$;return s?.constructor!==n&&(s?._$AO?.(!1),n===void 0?s=void 0:(s=new n(i),s._$AT(i,e,r)),r!==void 0?(e.o??=[])[r]=s:e.l=s),s!==void 0&&(t=O(i,s._$AS(i,t.values),s,r)),t}var ft=class{constructor(t,e){this._$AV=[],this._$AN=void 0,this._$AD=t,this._$AM=e}get parentNode(){return this._$AM.parentNode}get _$AU(){return this._$AM._$AU}u(t){let{el:{content:e},parts:r}=this._$AD,s=(t?.creationScope??k).importNode(e,!0);T.currentNode=s;let n=T.nextNode(),o=0,l=0,a=r[0];for(;a!==void 0;){if(o===a.index){let d;a.type===2?d=new K(n,n.nextSibling,this,t):a.type===1?d=new a.ctor(n,a.name,a.strings,this,t):a.type===6&&(d=new bt(n,this,t)),this._$AV.push(d),a=r[++l]}o!==a?.index&&(n=T.nextNode(),o++)}return T.currentNode=k,s}p(t){let e=0;for(let r of this._$AV)r!==void 0&&(r.strings!==void 0?(r._$AI(t,r,e),e+=r.strings.length-2):r._$AI(t[e])),e++}},K=class i{get _$AU(){return this._$AM?._$AU??this.v}constructor(t,e,r,s){this.type=2,this._$AH=p,this._$AN=void 0,this._$AA=t,this._$AB=e,this._$AM=r,this.options=s,this.v=s?.isConnected??!0}get parentNode(){let t=this._$AA.parentNode,e=this._$AM;return e!==void 0&&t?.nodeType===11&&(t=e.parentNode),t}get startNode(){return this._$AA}get endNode(){return this._$AB}_$AI(t,e=this){t=O(this,t,e),F(t)?t===p||t==null||t===""?(this._$AH!==p&&this._$AR(),this._$AH=p):t!==this._$AH&&t!==E&&this._(t):t._$litType$!==void 0?this.$(t):t.nodeType!==void 0?this.T(t):tr(t)?this.k(t):this._(t)}O(t){return this._$AA.parentNode.insertBefore(t,this._$AB)}T(t){this._$AH!==t&&(this._$AR(),this._$AH=this.O(t))}_(t){this._$AH!==p&&F(this._$AH)?this._$AA.nextSibling.data=t:this.T(k.createTextNode(t)),this._$AH=t}$(t){let{values:e,_$litType$:r}=t,s=typeof r=="number"?this._$AC(t):(r.el===void 0&&(r.el=J.createElement(Ft(r.h,r.h[0]),this.options)),r);if(this._$AH?._$AD===s)this._$AH.p(e);else{let n=new ft(s,this),o=n.u(this.options);n.p(e),this.T(o),this._$AH=n}}_$AC(t){let e=It.get(t.strings);return e===void 0&&It.set(t.strings,e=new J(t)),e}k(t){xt(this._$AH)||(this._$AH=[],this._$AR());let e=this._$AH,r,s=0;for(let n of t)s===e.length?e.push(r=new i(this.O(W()),this.O(W()),this,this.options)):r=e[s],r._$AI(n),s++;s<e.length&&(this._$AR(r&&r._$AB.nextSibling,s),e.length=s)}_$AR(t=this._$AA.nextSibling,e){for(this._$AP?.(!1,!0,e);t&&t!==this._$AB;){let r=t.nextSibling;t.remove(),t=r}}setConnected(t){this._$AM===void 0&&(this.v=t,this._$AP?.(t))}},R=class{get tagName(){return this.element.tagName}get _$AU(){return this._$AM._$AU}constructor(t,e,r,s,n){this.type=1,this._$AH=p,this._$AN=void 0,this.element=t,this.name=e,this._$AM=s,this.options=n,r.length>2||r[0]!==""||r[1]!==""?(this._$AH=Array(r.length-1).fill(new String),this.strings=r):this._$AH=p}_$AI(t,e=this,r,s){let n=this.strings,o=!1;if(n===void 0)t=O(this,t,e,0),o=!F(t)||t!==this._$AH&&t!==E,o&&(this._$AH=t);else{let l=t,a,d;for(t=n[0],a=0;a<n.length-1;a++)d=O(this,l[r+a],e,a),d===E&&(d=this._$AH[a]),o||=!F(d)||d!==this._$AH[a],d===p?t=p:t!==p&&(t+=(d??"")+n[a+1]),this._$AH[a]=d}o&&!s&&this.j(t)}j(t){t===p?this.element.removeAttribute(this.name):this.element.setAttribute(this.name,t??"")}},vt=class extends R{constructor(){super(...arguments),this.type=3}j(t){this.element[this.name]=t===p?void 0:t}},yt=class extends R{constructor(){super(...arguments),this.type=4}j(t){this.element.toggleAttribute(this.name,!!t&&t!==p)}},_t=class extends R{constructor(t,e,r,s,n){super(t,e,r,s,n),this.type=5}_$AI(t,e=this){if((t=O(this,t,e,0)??p)===E)return;let r=this._$AH,s=t===p&&r!==p||t.capture!==r.capture||t.once!==r.once||t.passive!==r.passive,n=t!==p&&(r===p||s);s&&this.element.removeEventListener(this.name,this,r),n&&this.element.addEventListener(this.name,this,t),this._$AH=t}handleEvent(t){typeof this._$AH=="function"?this._$AH.call(this.options?.host??this.element,t):this._$AH.handleEvent(t)}},bt=class{constructor(t,e,r){this.element=t,this.type=6,this._$AN=void 0,this._$AM=e,this.options=r}get _$AU(){return this._$AM._$AU}_$AI(t){O(this,t)}};var rr=$t.litHtmlPolyfillSupport;rr?.(J,K),($t.litHtmlVersions??=[]).push("3.2.0");var Jt=(i,t,e)=>{let r=e?.renderBefore??t,s=r._$litPart$;if(s===void 0){let n=e?.renderBefore??null;r._$litPart$=s=new K(t.insertBefore(W(),n),n,void 0,e??{})}return s._$AI(i),s};var b=class extends S{constructor(){super(...arguments),this.renderOptions={host:this},this.o=void 0}createRenderRoot(){let t=super.createRenderRoot();return this.renderOptions.renderBefore??=t.firstChild,t}update(t){let e=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(t),this.o=Jt(e,this.renderRoot,this.renderOptions)}connectedCallback(){super.connectedCallback(),this.o?.setConnected(!0)}disconnectedCallback(){super.disconnectedCallback(),this.o?.setConnected(!1)}render(){return E}};b._$litElement$=!0,b.finalized=!0,globalThis.litElementHydrateSupport?.({LitElement:b});var ir=globalThis.litElementPolyfillSupport;ir?.({LitElement:b});(globalThis.litElementVersions??=[]).push("4.1.0");var N=i=>(t,e)=>{e!==void 0?e.addInitializer(()=>{customElements.define(i,t)}):customElements.define(i,t)};var sr={attribute:!0,type:String,converter:z,reflect:!1,hasChanged:nt},nr=(i=sr,t,e)=>{let{kind:r,metadata:s}=e,n=globalThis.litPropertyMetadata.get(s);if(n===void 0&&globalThis.litPropertyMetadata.set(s,n=new Map),n.set(e.name,i),r==="accessor"){let{name:o}=e;return{set(l){let a=t.get.call(this);t.set.call(this,l),this.requestUpdate(o,a,i)},init(l){return l!==void 0&&this.P(o,void 0,i),l}}}if(r==="setter"){let{name:o}=e;return function(l){let a=this[o];t.call(this,l),this.requestUpdate(o,a,i)}}throw Error("Unsupported decorator location: "+r)};function u(i){return(t,e)=>typeof e=="object"?nr(i,t,e):((r,s,n)=>{let o=s.hasOwnProperty(n);return s.constructor.createProperty(n,o?{...r,wrapped:!0}:r),o?Object.getOwnPropertyDescriptor(s,n):void 0})(i,t,e)}function X(i){return u({...i,state:!0,attribute:!1})}var U=(i,t,e)=>(e.configurable=!0,e.enumerable=!0,Reflect.decorate&&typeof t!="object"&&Object.defineProperty(i,t,e),e);var or;function Kt(i){return(t,e)=>U(t,e,{get(){return(this.renderRoot??(or??=document.createDocumentFragment())).querySelectorAll(i)}})}var Ce=Pt(Lt());var Y=class i{static{this.guides=null}static queryDocumentGuides(t="qs-target qs-guide"){let e=document.querySelectorAll(t),r=e?[]:null;for(let s=0;s<e.length;s++){let n=e[s];r.push({title:n.getAttribute("title"),categories:n.getAttribute("categories"),type:n.getAttribute("type"),url:n.getAttribute("url"),summary:n.getAttribute("summary"),keywords:n.getAttribute("keywords"),content:n.getAttribute("content"),origin:n.getAttribute("origin")})}return r}static enableLocalSearch(t){i.guides=i.queryDocumentGuides(t),i.guides!=null&&console.debug("LocalSearch is ready with "+i.guides.length+" guides found.")}static search(t){let e=i.guides;if(e==null)return null;let r=[];t.q&&r.push(...t.q.split(" ").map(n=>n.trim()));let s=[];return t.categories&&(Array.isArray(t.categories)?s.push(...t.categories):s.push(t.categories)),e.filter(n=>{let o=!0;return o&&s.length>0&&(o=i.containsAllCaseInsensitive(n.categories,s)),o&&r.length>0&&(o=i.containsAllCaseInsensitive(`${n.keywords}${n.summary}${n.title}${n.categories}`,r)),o})}static containsAllCaseInsensitive(t,e){let r=(t||"").toLowerCase();for(let s in e)if(r.indexOf(e[s].toLowerCase())<0)return!1;return!0}};var lt="qs-start",q="qs-result",at="qs-next-page",$=class extends b{constructor(){super();this.server="";this.minChars=3;this.initialTimeout=1500;this.moreTimeout=2500;this.language="en";this.localSearch=!1;this._page=0;this._currentHitCount=0;this._abortController=null;this._search=()=>{if(this._abortController&&this._abortController.abort(),!this._backendData){this._clearSearch();return}let e=new AbortController;if(this._abortController=e,this.dispatchEvent(new CustomEvent(lt,{detail:{page:this._page}})),this.localSearch){this._localSearch(),this._abortController==e&&(this._abortController=null);return}this._jsonFetch(e,"GET",this._backendData,this._page>0?this.initialTimeout:this.moreTimeout).then(r=>{this._page>0?this._currentHitCount+=r.hits.length:this._currentHitCount=r.hits.length;let s=r.total?.lowerBound,n=r.hits.length>0&&s>this._currentHitCount;this.dispatchEvent(new CustomEvent(q,{detail:{...r,search:this._backendData,page:this._page,hasMoreHits:n}}))}).catch(r=>{console.error("Could not run search: "+r),this._abortController==e&&(this._page=0,this._currentHitCount=0,this._localSearch())}).finally(()=>{this._abortController==e&&(this._abortController=null)})};this._searchDebounced=(0,Ce.default)(this._search,300);this._handleInputChange=e=>{let r=this._getFormElements(),s={language:this.language,contentSnippets:2,contentSnippetsLength:120},n={};this.quarkusversion&&(s.version=this.quarkusversion);var o=0;for(let l of r)this._isInput(l)&&(l.value.length===0||l.value.length<this.minChars)||l.value&&l.value.length>0&&l.name.length>0&&(s[l.name]=l.value,n[l.name]=l.value,o++);o==0?(this._backendData=null,this._browserData=null):(this._backendData=s,this._browserData=n)};this._handleNextPage=e=>{this._page++,this._search()};let e=new URLSearchParams(window.location.hash.substring(1));if(e.size>0){let r=this._getFormElements();for(let s of r){let n=e.get(s.name);n&&(s.value=n)}}}render(){return _`
      <div id="qs-form">
        <slot></slot>
      </div>
    `}update(e){return window.location.hash=this._browserData?new URLSearchParams(this._browserData).toString():"",this._backendData?this._searchDebounced():this._clearSearch(),super.update(e)}connectedCallback(){super.connectedCallback(),Y.enableLocalSearch();let e=this._getFormElements();this.addEventListener(at,this._handleNextPage),e.forEach(r=>{let s=this._isInput(r)?"input":"change";r.addEventListener(s,this._handleInputChange)}),this._handleInputChange(null)}disconnectedCallback(){super.disconnectedCallback(),this.removeEventListener(at,this._handleNextPage),this._getFormElements().forEach(r=>{let s=this._isInput(r)?"input":"change";r.removeEventListener(s,this._handleInputChange)})}_getFormElements(){return this.querySelectorAll("input[name], select[name]")}_isInput(e){return e.tagName.toLowerCase()==="input"}async _jsonFetch(e,r,s,n){let o={...s,page:this._page.toString()},l=setTimeout(()=>e.abort(),n),a=await fetch(this.server+"/api/guides/search?"+new URLSearchParams(o).toString(),{method:r,signal:e.signal,body:null});if(clearTimeout(l),a.ok)return await a.json();throw"Response status is "+a.status+"; response: "+await a.text()}_clearSearch(){this._page=0,this._currentHitCount=0,this._abortController&&(this._abortController.abort(),this._abortController=null),this.dispatchEvent(new CustomEvent(q))}_localSearch(){let e=this._backendData,r=Y.search(e);if(r){let s={hits:r,total:r.length};this.dispatchEvent(new CustomEvent(q,{detail:{...s,search:e,page:0,hasMoreHits:!1}}));return}this.dispatchEvent(new CustomEvent(q))}};$.styles=M`

      ::slotted(*) {
          display: block !important;
      }

      .quarkus-search {
          display: block !important;
      }

      .d-none {
          display: none;
      }
  `,h([u({type:String})],$.prototype,"server",2),h([u({type:String,attribute:"min-chars"})],$.prototype,"minChars",2),h([u({type:String,attribute:"initial-timeout"})],$.prototype,"initialTimeout",2),h([u({type:String,attribute:"more-timeout"})],$.prototype,"moreTimeout",2),h([u({type:String})],$.prototype,"language",2),h([u({type:String,attribute:"quarkus-version"})],$.prototype,"quarkusversion",2),h([u({type:String,attribute:"local-search"})],$.prototype,"localSearch",2),h([X({hasChanged(e,r){return JSON.stringify(e)!==JSON.stringify(r)}})],$.prototype,"_backendData",2),$=h([N("qs-form")],$);var Le={ATTRIBUTE:1,CHILD:2,PROPERTY:3,BOOLEAN_ATTRIBUTE:4,EVENT:5,ELEMENT:6},Me=i=>(...t)=>({_$litDirective$:i,values:t}),ct=class{constructor(t){}get _$AU(){return this._$AM._$AU}_$AT(t,e,r){this.t=t,this._$AM=e,this.i=r}_$AS(t,e){return this.update(t,e)}update(t,e){return this.render(...e)}};var Q=class extends ct{constructor(t){if(super(t),this.it=p,t.type!==Le.CHILD)throw Error(this.constructor.directiveName+"() can only be used in child bindings")}render(t){if(t===p||t==null)return this._t=void 0,this.it=t;if(t===E)return t;if(typeof t!="string")throw Error(this.constructor.directiveName+"() called with a non-string value");if(t===this.it)return this._t;this.it=t;let e=[t];return e.raw=e,this._t={_$litType$:this.constructor.resultType,strings:e,values:[]}}};Q.directiveName="unsafeHTML",Q.resultType=1;var Mt=Me(Q);var ht='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M12.2,12.2V87.8H87.8V12.2ZM83,17V29.9H17V17ZM17,83V34.7H83V83Z"/><rect x="73.49" y="20.53" width="6.04" height="6.04"/><rect x="64.33" y="20.53" width="6.04" height="6.04"/><path d="M59.92,47.34a1.61,1.61,0,0,0,0,2.26l7.82,7.82-7.82,7.81a1.61,1.61,0,0,0,0,2.26,1.57,1.57,0,0,0,1.13.47,1.6,1.6,0,0,0,1.14-.47L72.26,57.42,62.19,47.34A1.62,1.62,0,0,0,59.92,47.34Z"/><path d="M40.08,47.34a1.62,1.62,0,0,0-2.27,0L27.74,57.42,37.81,67.49A1.6,1.6,0,0,0,39,68a1.56,1.56,0,0,0,1.13-.47,1.61,1.61,0,0,0,0-2.26l-7.82-7.81,7.82-7.82A1.61,1.61,0,0,0,40.08,47.34Z"/><path d="M53.55,47.38a1.61,1.61,0,0,0-2.06.94L45.21,65.4a1.61,1.61,0,0,0,.94,2.06,1.58,1.58,0,0,0,.56.1,1.6,1.6,0,0,0,1.5-1L54.5,49.43A1.6,1.6,0,0,0,53.55,47.38Z"/></svg>';var V='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="28.82" cy="16.34" r="4.6"/><path d="M98,17.22H40.41a6.79,6.79,0,0,0,.07-.88,11.66,11.66,0,1,0-23.32,0,6.73,6.73,0,0,0,.06.88H2V88.67H25.91l1,6.65,6.17-6.65H98ZM28.82,7.88a8.46,8.46,0,0,1,8.46,8.46c0,3.57-5.29,11.71-8.46,16.15-3.17-4.44-8.46-12.58-8.46-16.15A8.46,8.46,0,0,1,28.82,7.88Zm3.82,24.74C35.27,33.33,37,34.71,37,36c0,1.83-3.49,3.88-8.16,3.88s-8.16-2-8.16-3.88c0-1.28,1.71-2.66,4.34-3.37,1.25,1.81,2.24,3.13,2.54,3.54l1.28,1.68,1.28-1.68C30.4,35.75,31.39,34.43,32.64,32.62ZM28.17,82.09l-4.88-3.87,19.16-4.37L29.09,88.26Zm65.05,1.78H37.52L51.85,68.43,16.08,76.58l9.11,7.23v.06H6.78V22h12a61.44,61.44,0,0,0,4.39,7.82c-3.42,1.21-5.69,3.48-5.69,6.15,0,4,5,7.08,11.36,7.08S40.18,40,40.18,36c0-2.67-2.27-4.94-5.69-6.15A62.69,62.69,0,0,0,38.88,22H93.22Z"/><path d="M30.83,65.59H26.6V57.43c0-1,0-1.9.06-2.72a8.11,8.11,0,0,1-1,1l-1.75,1.45-2.16-2.66,5.29-4.31h3.76Z"/><path d="M80.19,41.54H69V38.8L72.76,35c1.08-1.13,1.78-1.89,2.11-2.31a4.91,4.91,0,0,0,.69-1.06,2.24,2.24,0,0,0,.2-.92,1.21,1.21,0,0,0-.39-.95,1.61,1.61,0,0,0-1.1-.35,2.94,2.94,0,0,0-1.47.42A9.66,9.66,0,0,0,71.12,31l-2.29-2.67a12.56,12.56,0,0,1,2-1.53,7.26,7.26,0,0,1,1.77-.7,8.75,8.75,0,0,1,2.14-.24,6.54,6.54,0,0,1,2.68.52,4.25,4.25,0,0,1,1.82,1.51,3.89,3.89,0,0,1,.65,2.19,5.91,5.91,0,0,1-.23,1.67,5.76,5.76,0,0,1-.7,1.52,10,10,0,0,1-1.26,1.56q-.78.83-3.35,3.12v.11h5.84Z"/><path d="M84.09,63.11a3.66,3.66,0,0,1-.87,2.46,5,5,0,0,1-2.54,1.5v.06c2.58.32,3.86,1.54,3.86,3.65a4,4,0,0,1-1.7,3.39,7.94,7.94,0,0,1-4.73,1.23,13.67,13.67,0,0,1-2.3-.18,12.67,12.67,0,0,1-2.3-.65V71.11a9.17,9.17,0,0,0,2.06.76,8.26,8.26,0,0,0,1.94.25,3.93,3.93,0,0,0,2-.39,1.3,1.3,0,0,0,.63-1.2,1.35,1.35,0,0,0-.33-1A2.14,2.14,0,0,0,78.75,69a8.71,8.71,0,0,0-1.89-.17h-1V65.73h1c2.11,0,3.17-.54,3.17-1.63a1,1,0,0,0-.47-.9,2.3,2.3,0,0,0-1.26-.3,5.8,5.8,0,0,0-3.06,1l-1.73-2.78A8.24,8.24,0,0,1,76,59.91,10.85,10.85,0,0,1,79,59.55a6.43,6.43,0,0,1,3.75,1A3,3,0,0,1,84.09,63.11Z"/><path d="M63.16,70.12a10.22,10.22,0,0,1-5.87-1.78l1.82-2.63a7.3,7.3,0,0,0,5,1.16l.34,3.18A12.1,12.1,0,0,1,63.16,70.12Zm-8.53-4.53a8.51,8.51,0,0,1-1-2.2,11.19,11.19,0,0,1-.41-3,9.45,9.45,0,0,1,.24-2.2l3.12.72a6,6,0,0,0-.16,1.48,8.11,8.11,0,0,0,.29,2.14,5.1,5.1,0,0,0,.63,1.35ZM35.13,57c-.27,0-.54,0-.8,0l-.06-3.21a10.12,10.12,0,0,0,5.5-1.37l1.61,2.77A12.49,12.49,0,0,1,35.13,57Zm22.51-.25-2.46-2.05a11.61,11.61,0,0,1,6.35-3.61l.77,3.11A8.55,8.55,0,0,0,57.64,56.71Zm7.59-3-.29-3.19a34.85,34.85,0,0,1,3.77-.12h.22a8.57,8.57,0,0,0,2.15-.25l.79,3.11a12,12,0,0,1-2.94.34h-.28A30.94,30.94,0,0,0,65.23,53.69Zm-21-.69-2.21-2.31A5.91,5.91,0,0,0,43.91,46l3.2.11A9.08,9.08,0,0,1,44.26,53Zm31.06-1.43L73.37,49a8.43,8.43,0,0,0,2.17-2.64l.19-.34c.28-.51.58-1.06.87-1.65l2.87,1.41c-.31.63-.64,1.23-.94,1.78l-.19.36A11.62,11.62,0,0,1,75.32,51.57ZM47.19,43,44,42.76a9.87,9.87,0,0,1,3.12-6.68l2.23,2.29A6.68,6.68,0,0,0,47.19,43Zm4.43-6.39-1.72-2.7a30.13,30.13,0,0,1,3.48-1.87,22.94,22.94,0,0,1,2.68-1l1,3a18.65,18.65,0,0,0-2.32.9A26.63,26.63,0,0,0,51.62,36.62ZM60,33.33l-.59-3.14a28.15,28.15,0,0,1,6.78-.45L66,32.93A25.34,25.34,0,0,0,60,33.33Z"/><path d="M68.14,69.17l-1.09-3a20.32,20.32,0,0,0,2.67-1.24l1.55,2.8A22.22,22.22,0,0,1,68.14,69.17Z"/></svg>';var He='data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.1" id="Layer_1" x="0px" y="0px" viewBox="0 0 100 100" style="enable-background:new 0 0 100 100;" xml:space="preserve"><path d="M80.8,34.7V30v-2.1L76,23.1L69.9,17l-4.8-4.8H63h-4.7l-0.1-0.1v0.1h-39v75.6h61.6V34.7L80.8,34.7L80.8,34.7z M76,83H24V17  h34.2v17.8H76V83z M76,30H63V17v-0.1l0.1,0.1L76,30L76,30L76,30z"/><path d="M35.1,59.7c-1.2,0-2.2-0.5-3.1-1.5c-1.5-1.7-1.6-3.9-0.1-5.7c1.2-1.5,3.5-2.8,6.7-3.9c1.2-0.3,2.4-0.8,3.7-1  c0.8-1.5,1.6-3.1,2.3-4.8c0.7-1.6,1.3-3.1,1.7-4.6c-1.8-3.1-3.6-6.8-3.5-9.7c0-2.5,1.5-4.2,3.9-4.5c2.2-0.2,3.8,0.8,4.5,2.9  c0.9,2.8,0,7-1,10.7c0.9,1.5,2,3,3.2,4.5c1,1.3,2,2.4,3,3.6c4,0,7.9,0.3,10.2,1.6c2.1,1.2,2.9,2.9,2.3,5c-0.7,2.3-2.5,3.5-4.8,3.1  c-3-0.3-6.5-3.2-9.1-6c-1.8,0.1-3.9,0.2-6,0.6c-1.5,0.2-3,0.5-4.4,0.8c-0.5,0.9-1,1.7-1.5,2.4c-2,2.9-3.8,4.8-5.5,5.9  C36.8,59.5,36,59.7,35.1,59.7L35.1,59.7z M39.6,52.1c-2.4,0.8-4.3,1.8-5.1,2.8c-0.3,0.3-0.3,0.6,0.1,1.2c0.2,0.2,0.3,0.5,1.3,0  C37,55.3,38.4,54,39.6,52.1L39.6,52.1z M60.2,49.5c1.7,1.5,3.2,2.4,4.4,2.5c0.5,0.1,0.7,0.1,1-0.7c0.1-0.2,0.1-0.3-0.6-0.8  C64,50,62.3,49.6,60.2,49.5L60.2,49.5z M48.9,41.9c-0.3,0.9-0.7,1.7-0.9,2.3c-0.3,0.9-0.8,1.7-1.2,2.5c0.6-0.1,1.2-0.2,1.7-0.2  c1.2-0.1,2.3-0.3,3.5-0.3c-0.6-0.7-1-1.3-1.3-1.6C50.3,43.9,49.6,43,48.9,41.9L48.9,41.9z M47.3,27.5c-0.5,0.1-0.8,0.1-0.8,1  c0,1.2,0.5,2.8,1.4,4.7c0.5-2.3,0.6-4.2,0.2-5.2C47.8,27.5,47.7,27.5,47.3,27.5L47.3,27.5z"/><g><path d="M35.9,67.1c0-0.9,0.6-1.3,1.3-1.3h3.3c2.1,0,4.1,1.3,4.1,4c0,2.3-1.4,4-4,4h-2v2.5c0,0.9-0.6,1.4-1.3,1.4   c-0.8,0-1.3-0.5-1.3-1.4L35.9,67.1L35.9,67.1z M38.6,71.4H40c1.1,0,1.8-0.8,1.8-1.6c0-1.1-0.7-1.6-1.8-1.6h-1.4V71.4z"/><path d="M45.9,67.1c0-0.9,0.6-1.3,1.3-1.3h3.2c3.5,0,5.5,2.8,5.5,5.8c0,3.9-2.6,6-5.5,6h-3.2c-0.7,0-1.3-0.5-1.3-1.3V67.1z    M48.6,75.1H50c2.1,0,3.1-1.4,3.1-3.5c0-2-1-3.3-3-3.3h-1.5V75.1z"/><path d="M57.5,67.1c0-0.9,0.6-1.3,1.3-1.3h4.1c0.8,0,1.3,0.5,1.3,1.2c0,0.8-0.5,1.2-1.3,1.2H60v2.1h2.4c0.8,0,1.3,0.5,1.3,1.2   c0,0.8-0.5,1.2-1.3,1.2H60V76c0,0.9-0.6,1.4-1.3,1.4c-0.8,0-1.3-0.5-1.3-1.4L57.5,67.1z"/></g></svg>';var Te='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M61.05,69.88H55.54a13.46,13.46,0,0,1,4.87-10.37,15.61,15.61,0,1,0-19.93,0,13.46,13.46,0,0,1,4.87,10.37H39.84A7.94,7.94,0,0,0,37,63.75a21.11,21.11,0,1,1,27,0A7.94,7.94,0,0,0,61.05,69.88Z"/><path d="M60.71,75H40.18a1.84,1.84,0,0,1,0-3.67H60.71a1.84,1.84,0,0,1,0,3.67Z"/><path d="M60.71,79.93H40.18a1.84,1.84,0,0,1,0-3.67H60.71a1.84,1.84,0,0,1,0,3.67Z"/><path d="M60.71,84.87H40.18a1.84,1.84,0,0,1,0-3.67H60.71a1.84,1.84,0,0,1,0,3.67Z"/><path d="M56.72,89.82H44.17a1.84,1.84,0,1,1,0-3.67H56.72a1.84,1.84,0,1,1,0,3.67Z"/><polygon points="50.53 54.3 56.18 51.04 56.18 44.51 50.53 47.77 50.53 54.3"/><polygon points="51.07 54.61 56.18 57.56 56.18 51.66 51.07 54.61"/><polygon points="55.87 43.35 55.87 37.45 50.76 40.4 55.87 43.35"/><polygon points="50.22 40.72 44.57 43.98 50.22 47.24 55.87 43.98 50.22 40.72"/><polygon points="49.68 40.4 44.57 37.45 44.57 43.35 49.68 40.4"/><polygon points="56.72 44.83 56.72 50.73 61.83 47.77 56.72 44.83"/><polygon points="49.91 54.3 49.91 47.77 44.26 44.51 44.26 44.51 44.26 51.04 44.26 51.04 44.26 51.04 49.91 54.3"/><polygon points="43.72 44.83 38.61 47.77 43.72 50.73 43.72 44.83"/><polygon points="44.26 51.66 44.26 57.56 49.37 54.61 44.26 51.66"/><polygon points="24.4 47.66 12.48 52.76 12.48 42.56 24.4 47.66"/><polygon points="31.92 29.57 19.89 24.75 27.1 17.54 31.92 29.57"/><polygon points="50.04 22.1 44.94 10.18 55.14 10.18 50.04 22.1"/><polygon points="68.13 29.63 72.95 17.59 80.16 24.8 68.13 29.63"/><polygon points="75.6 47.74 87.52 42.64 87.52 52.84 75.6 47.74"/><polygon points="68.08 65.83 80.11 70.66 72.9 77.86 68.08 65.83"/><polygon points="31.87 65.78 27.05 77.81 19.84 70.6 31.87 65.78"/></svg>';var ke='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M50.09,25.2h-17a5,5,0,0,0-5,5v1.2a5,5,0,0,0,5,5h17a5,5,0,0,0,5-5V30.2A5,5,0,0,0,50.09,25.2Zm1.8,6.2a1.81,1.81,0,0,1-1.8,1.8h-17a1.8,1.8,0,0,1-1.8-1.8V30.2a1.8,1.8,0,0,1,1.8-1.8h17a1.81,1.81,0,0,1,1.8,1.8Z"/><path d="M52.49,44.88H29.89a1.6,1.6,0,0,0,0,3.2h22.6a1.6,1.6,0,1,0,0-3.2Z"/><path d="M52.49,39.47H29.89a1.6,1.6,0,0,0,0,3.2h22.6a1.6,1.6,0,0,0,0-3.2Z"/><path d="M52.49,50.29H29.89a1.6,1.6,0,0,0,0,3.2h22.6a1.6,1.6,0,1,0,0-3.2Z"/><path d="M73.3,30.68a16.42,16.42,0,0,0-3.81.46V23.4a9.21,9.21,0,0,0-9.2-9.2H22.89a12.61,12.61,0,0,0-12.6,12.6V73.2a12.61,12.61,0,0,0,12.6,12.6H64.2l.46-.21c1.14-.5,4.83-2.4,4.83-5.59V63a15.9,15.9,0,0,0,3.81.47,16.42,16.42,0,0,0,0-32.83ZM64.69,69.73H22.89a3.85,3.85,0,0,0,0,7.65h41.8v2.44A5.54,5.54,0,0,1,63.12,81H22.89a7.8,7.8,0,0,1-7.8-7.8V26.8a7.8,7.8,0,0,1,7.8-7.8h37.4a4.41,4.41,0,0,1,4.4,4.4v9.74a16.36,16.36,0,0,0,0,27.9Zm8.61-9.42A13.22,13.22,0,1,1,86.51,47.09,13.23,13.23,0,0,1,73.3,60.31Z"/><path d="M79.63,41.34,71.2,49.77l-3-3a1.59,1.59,0,0,0-2.26,0A1.61,1.61,0,0,0,66,49l5.25,5.25L81.89,43.6a1.59,1.59,0,0,0,0-2.26A1.61,1.61,0,0,0,79.63,41.34Z"/></svg>';var qe='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024"><defs><style>.cls-1{fill:%234695eb;}.cls-2{fill:%23ff004a;}.cls-3{fill:%23091313;}</style></defs><title>quarkus_icon_rgb_1024px_default</title><polygon class="cls-1" points="669.34 180.57 512 271.41 669.34 362.25 669.34 180.57"/><polygon class="cls-2" points="354.66 180.57 354.66 362.25 512 271.41 354.66 180.57"/><polygon class="cls-3" points="669.34 362.25 512 271.41 354.66 362.25 512 453.09 669.34 362.25"/><polygon class="cls-1" points="188.76 467.93 346.1 558.76 346.1 377.09 188.76 467.93"/><polygon class="cls-2" points="346.1 740.44 503.43 649.6 346.1 558.76 346.1 740.44"/><polygon class="cls-3" points="346.1 377.09 346.1 558.76 503.43 649.6 503.43 467.93 346.1 377.09"/><polygon class="cls-1" points="677.9 740.44 677.9 558.76 520.57 649.6 677.9 740.44"/><polygon class="cls-2" points="835.24 467.93 677.9 377.09 677.9 558.76 835.24 467.93"/><polygon class="cls-3" points="520.57 649.6 677.9 558.76 677.9 377.09 520.57 467.93 520.57 649.6"/><path class="cls-1" d="M853.47,1H170.53C77.29,1,1,77.29,1,170.53V853.47C1,946.71,77.29,1023,170.53,1023h467.7L512,716.39,420.42,910H170.53C139.9,910,114,884.1,114,853.47V170.53C114,139.9,139.9,114,170.53,114H853.47C884.1,114,910,139.9,910,170.53V853.47C910,884.1,884.1,910,853.47,910H705.28l46.52,113H853.47c93.24,0,169.53-76.29,169.53-169.53V170.53C1023,77.29,946.71,1,853.47,1Z"/></svg>';var Pe='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">%0A  <defs>%0A    <style>%0A      .cls-1 {%0A        fill: %23ff004a;%0A      }%0A%0A      .cls-2 {%0A        fill: %234695eb;%0A      }%0A%0A      .cls-3 {%0A        fill: %23091313;%0A      }%0A    </style>%0A  </defs>%0A  <g>%0A    <g>%0A      <path class="cls-1" d="M235.08,104.91V276.25c0,33,35.7,53.6,64.27,37.11l148.38-85.67c28.56-16.5,28.56-57.72,0-74.22L299.35,67.8C270.78,51.31,235.08,71.93,235.08,104.91Z"/>%0A      <path class="cls-2" d="M788.92,104.91V276.25c0,33-35.7,53.6-64.27,37.11L576.27,227.69c-28.56-16.5-28.56-57.72,0-74.22L724.65,67.8C753.22,51.31,788.92,71.93,788.92,104.91Z"/>%0A      <path class="cls-1" d="M299.35,952.88l148.38-85.67c28.57-16.49,28.57-57.72,0-74.21L299.35,707.33c-28.57-16.49-64.27,4.12-64.27,37.11V915.78C235.08,948.76,270.78,969.37,299.35,952.88Z"/>%0A      <path class="cls-2" d="M22.42,473.24l148.39-85.67c28.56-16.49,64.27,4.12,64.27,37.1V596c0,33-35.71,53.6-64.27,37.11L22.42,547.45C-6.14,531-6.14,489.73,22.42,473.24Z"/>%0A      <path class="cls-1" d="M1001.58,473.24,853.19,387.57c-28.56-16.49-64.27,4.12-64.27,37.1V596c0,33,35.71,53.6,64.27,37.11l148.39-85.67C1030.14,531,1030.14,489.73,1001.58,473.24Z"/>%0A      <path class="cls-2" d="M724.65,952.88,576.27,867.21c-28.57-16.49-28.57-57.72,0-74.21l148.38-85.67c28.57-16.49,64.27,4.12,64.27,37.11V915.78C788.92,948.76,753.22,969.37,724.65,952.88Z"/>%0A    </g>%0A    <g>%0A      <g>%0A        <path class="cls-3" d="M323.05,422.59l-.32-.18c-15-8.59-34,2.42-34.05,19.71V573.68a120.91,120.91,0,0,0,60.45,104.71L462.79,744l.32.18c15,8.59,34-2.42,34.05-19.71V592.92a120.9,120.9,0,0,0-60.45-104.71Z"/>%0A        <path class="cls-3" d="M526.84,724.16v.32c0,17.29,19,28.3,34,19.71l.32-.18,113.66-65.62a120.91,120.91,0,0,0,60.45-104.71V442.12c-.05-17.29-19-28.3-34.05-19.71l-.32.18L587.29,488.21a120.9,120.9,0,0,0-60.45,104.71Z"/>%0A      </g>%0A      <path class="cls-3" d="M688.28,395l.28-.16c15-8.71,15-30.67,0-39.38l-.28-.16L574.63,289.71a120.9,120.9,0,0,0-120.91,0L340.06,355.33l-.28.16c-15,8.71-15,30.67,0,39.38l.28.16,113.66,65.62a121,121,0,0,0,120.91,0Z"/>%0A    </g>%0A  </g>%0A</svg>%0A';var Oe='data:image/svg+xml,<svg width="57" height="57" viewBox="0 0 57 57" xmlns="http://www.w3.org/2000/svg" stroke="%234695EB">%0A    <g fill="none" fill-rule="evenodd">%0A        <g transform="translate(1 1)" stroke-width="2">%0A            <circle cx="5" cy="50" r="5">%0A                <animate attributeName="cy"%0A                     begin="0s" dur="2.2s"%0A                     values="50;5;50;50"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A                <animate attributeName="cx"%0A                     begin="0s" dur="2.2s"%0A                     values="5;27;49;5"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A            </circle>%0A            <circle cx="27" cy="5" r="5">%0A                <animate attributeName="cy"%0A                     begin="0s" dur="2.2s"%0A                     from="5" to="5"%0A                     values="5;50;50;5"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A                <animate attributeName="cx"%0A                     begin="0s" dur="2.2s"%0A                     from="27" to="27"%0A                     values="27;49;5;27"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A            </circle>%0A            <circle cx="49" cy="50" r="5">%0A                <animate attributeName="cy"%0A                     begin="0s" dur="2.2s"%0A                     values="50;50;5;50"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A                <animate attributeName="cx"%0A                     from="49" to="49"%0A                     begin="0s" dur="2.2s"%0A                     values="49;5;27;49"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A            </circle>%0A        </g>%0A    </g>%0A</svg>';var ii={docs:{tutorial:ht,tutorials:ht,guide:V,guides:V,howto:V,pdf:He,concepts:Te,reference:ke},origins:{quarkus:qe,quarkiverse:Pe},loading:Oe},j=ii;var v=class extends b{constructor(){super(...arguments);this.type="default";this.origin="quarkus";this.originsWithRelativeUrls=[]}connectedCallback(){if(this.data)for(let e in this.data)this.data.hasOwnProperty(e)&&(this[e]=this.data[e]);super.connectedCallback()}disconnectedCallback(){super.disconnectedCallback()}render(){return _`
      <div class="qs-hit qs-guide" aria-label="Guide Hit">
        <div class="qs-guide-icon">
          ${Mt(this.icon())}
        </div>
        <div>
          <h4>
            <a href="${this.relativizeUrl()}" target="_blank">${this._renderHTML(this.title)}</a>
            ${this.origin&&this.origin.toLowerCase()!=="quarkus"?_`<span class="origin ${this.origin}" title="${this.origin}"></span>`:""}
          </h4>
          <div class="summary">
            <p>${this._renderHTML(this.summary)}</p>
          </div>
          <div class="keywords">${this._renderHTML(this.keywords)}</div>
          <div class="content-highlights">
            ${this._renderHTML(this.content)}
          </div>
        </div>
      </div>
    `}relativizeUrl(){if(this.originsWithRelativeUrls.includes(this.origin)&&!this.url.startsWith("/"))try{return this.url.substring(new URL(this.url).origin.length)}catch{return this.url}else return this.url}icon(){let e=j.docs[this.type];if(e){let r=e.match(/.*(<svg.*<\/svg>)/);if(r)return r[1]}return""}_renderHTML(e){return e&&(Array.isArray(e)?e.map(r=>_`<p>${this._renderHTML(r)}</p>`):Mt(e))}};v.styles=M`
      @media screen and (max-width: 1300px) {
          .qs-hit {
              grid-column: span 6;
          }
      }

      .highlighted {
          font-weight: bold;
      }

      .qs-guide {
          display: flex;
          column-gap: 20px;
      }

      .qs-guide a {
          line-height: 1.5rem;
          font-weight: 400;
          cursor: pointer;
          text-decoration: underline;
          color: var(--link-color);
      }

      .qs-guide a:hover {
          color: var(--link-color-hover);
      }

      .qs-guide h4 {
          margin: 1rem 0 0 0;
      }

      .qs-guide div {
          margin: 1rem 0 0 0;
          font-size: 1rem;
          line-height: 1.5rem;
          font-weight: 400;
      }

      .qs-guide .content-highlights {
          font-size: 0.7rem;
          line-height: 1rem;
          color: var(--content-highlight-color);

          p {
              margin: 0 0 .5rem;
          }
      }

      .qs-guide .origin {
          background-size: 20px 20px;
          background-repeat: no-repeat;
          background-position: center;
          margin-left: 5px;
          width: 20px;
          height: 20px;
          display: inline-block;
          vertical-align: middle;
      }

      .qs-guide .origin.quarkus {
          background-image: url('${L(j.origins.quarkus)}');
      }

      .qs-guide .origin.quarkiverse-hub {
          background-image: url('${L(j.origins.quarkiverse)}');
      }
    
      .qs-guide-icon svg {
        width: 70px;
        margin: 1rem 0 0 0;
        fill: var(--main-text-color);
      }

      .summary {
          min-height: 40px;
      }
  `,h([u({type:Object})],v.prototype,"data",2),h([u({type:String})],v.prototype,"type",2),h([u({type:String})],v.prototype,"url",2),h([u({type:String})],v.prototype,"title",2),h([u({type:String})],v.prototype,"summary",2),h([u({type:String})],v.prototype,"keywords",2),h([u({type:String})],v.prototype,"content",2),h([u({type:String})],v.prototype,"origin",2),h([u({type:String,attribute:"origins-with-relative-urls"})],v.prototype,"originsWithRelativeUrls",2),v=h([N("qs-guide")],v);var Re=Pt(Lt());var A=class extends b{constructor(){super(...arguments);this.type="guide";this.originsWithRelativeUrls=[];this._loading=!0;this._handleScroll=e=>{if(this._loading||!this._result)return;if(!this._result.hasMoreHits){console.debug("no more hits");return}let r=this._hits.length==0?null:this._hits[this._hits.length-1];if(!r)return;let s=document.documentElement,n=s.scrollTop+s.clientHeight,o=r.offsetTop;n>=o&&(this._loading=!0,this._form.dispatchEvent(new CustomEvent(at)))};this._handleScrollDebounced=(0,Re.default)(this._handleScroll,100);this._handleResult=e=>{if(console.debug("Received results in qs-target: ",e.detail),this._loadingEnd(),!this._result||!e.detail||!e.detail.hits||e.detail.page===0){e.detail?.hits?document.body.classList.add("qs-has-results"):document.body.classList.remove("qs-has-results"),this._result=e.detail;return}this._result.hits=this._result.hits.concat(e.detail.hits),console.debug(`${this._result.hits.length} results in qs-target: `),this._result.hasMoreHits=e.detail.hasMoreHits};this._loadingStart=e=>{this._loading=!0,e.detail.page===0&&(this._result=void 0)};this._loadingEnd=()=>{this._loading=!1}}connectedCallback(){super.connectedCallback(),this._form=document.querySelector("qs-form"),this._form.addEventListener(q,this._handleResult),this._form.addEventListener(lt,this._loadingStart),document.addEventListener("scroll",this._handleScrollDebounced)}disconnectedCallback(){this._form.removeEventListener(q,this._handleResult),this._form.removeEventListener(lt,this._loadingStart),document.removeEventListener("scroll",this._handleScrollDebounced),super.disconnectedCallback()}render(){if(this._result?.hits){if(this._result.hits.length===0)return _`
          <div id="qs-target" class="no-hits">
            <p>Sorry, no ${this.type}s matched your search. Please try again.</p>
          </div>
        `;let e=this._result.hits.map(r=>this._renderHit(r));return _`
        <div id="qs-target" class="qs-hits" aria-label="Search Hits">
          ${e}
        </div>
        ${this._loading?this._renderLoading():""}
      `}return this._loading?_`
        <div id="qs-target">${this._renderLoading()}</div>`:_`
      <div id="qs-target">
        <slot></slot>
      </div>
    `}_renderLoading(){return _`
      <div class="loading">Searching...</div>
    `}_renderHit(e){switch(this.type){case"guide":return _`
          <qs-guide class="qs-hit" .data=${e} origins-with-relative-urls=${this.originsWithRelativeUrls}></qs-guide>`}return""}};A.styles=M`
    
    .loading {
      background-image: url('${L(j.loading)}');
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
      grid-gap: 1em;
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
    }


    qs-guide {
      grid-column: span 4;
      margin: 1rem 0rem 1rem 0rem;

      @media screen and (max-width: 1300px) {
        grid-column: span 6;
      }

      @media screen and (max-width: 768px) {
        grid-column: span 12;
        margin: 1rem 0rem 1rem 0rem;
      }

      @media screen and (max-width: 480px) {
        grid-column: span 12;
      }
    }
   
  `,h([u({type:String})],A.prototype,"type",2),h([u({type:String,attribute:"origins-with-relative-urls"})],A.prototype,"originsWithRelativeUrls",2),h([X()],A.prototype,"_result",2),h([X()],A.prototype,"_loading",2),h([Kt(".qs-hit")],A.prototype,"_hits",2),A=h([N("qs-target")],A);export{Y as LocalSearch,at as QS_NEXT_PAGE_EVENT,q as QS_RESULT_EVENT,lt as QS_START_EVENT,$ as QsForm,v as QsGuide,A as QsTarget};
/*! Bundled license information:

@lit/reactive-element/css-tag.js:
  (**
   * @license
   * Copyright 2019 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/reactive-element.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

lit-html/lit-html.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

lit-element/lit-element.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/custom-element.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/property.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/state.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/event-options.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/base.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/query.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/query-all.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/query-async.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/query-assigned-elements.js:
  (**
   * @license
   * Copyright 2021 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/query-assigned-nodes.js:
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

lit-html/directive.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

lit-html/directives/unsafe-html.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/

