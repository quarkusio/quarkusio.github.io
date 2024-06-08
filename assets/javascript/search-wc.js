var Ze=Object.create;var ut=Object.defineProperty;var Tt=Object.getOwnPropertyDescriptor;var je=Object.getOwnPropertyNames;var De=Object.getPrototypeOf,Ie=Object.prototype.hasOwnProperty;var f=(r,t)=>()=>(t||r((t={exports:{}}).exports,t),t.exports);var ze=(r,t,e,i)=>{if(t&&typeof t=="object"||typeof t=="function")for(let s of je(t))!Ie.call(r,s)&&s!==e&&ut(r,s,{get:()=>t[s],enumerable:!(i=Tt(t,s))||i.enumerable});return r};var qt=(r,t,e)=>(e=r!=null?Ze(De(r)):{},ze(t||!r||!r.__esModule?ut(e,"default",{value:r,enumerable:!0}):e,r));var h=(r,t,e,i)=>{for(var s=i>1?void 0:i?Tt(t,e):t,n=r.length-1,o;n>=0;n--)(o=r[n])&&(s=(i?o(t,e,s):o(s))||s);return i&&s&&ut(t,e,s),s};var At=f((qs,se)=>{function fi(r){var t=typeof r;return r!=null&&(t=="object"||t=="function")}se.exports=fi});var oe=f((ks,ne)=>{var vi=typeof global=="object"&&global&&global.Object===Object&&global;ne.exports=vi});var St=f((Ps,ae)=>{var yi=oe(),_i=typeof self=="object"&&self&&self.Object===Object&&self,bi=yi||_i||Function("return this")();ae.exports=bi});var ce=f((Ns,le)=>{var $i=St(),xi=function(){return $i.Date.now()};le.exports=xi});var de=f((Os,he)=>{var Ai=/\s/;function Si(r){for(var t=r.length;t--&&Ai.test(r.charAt(t)););return t}he.exports=Si});var pe=f((Rs,ue)=>{var Ei=de(),wi=/^\s+/;function Ci(r){return r&&r.slice(0,Ei(r)+1).replace(wi,"")}ue.exports=Ci});var Et=f((Us,ge)=>{var Li=St(),Mi=Li.Symbol;ge.exports=Mi});var ye=f((Vs,ve)=>{var me=Et(),fe=Object.prototype,Hi=fe.hasOwnProperty,Ti=fe.toString,Y=me?me.toStringTag:void 0;function qi(r){var t=Hi.call(r,Y),e=r[Y];try{r[Y]=void 0;var i=!0}catch{}var s=Ti.call(r);return i&&(t?r[Y]=e:delete r[Y]),s}ve.exports=qi});var be=f((Zs,_e)=>{var ki=Object.prototype,Pi=ki.toString;function Ni(r){return Pi.call(r)}_e.exports=Ni});var Se=f((js,Ae)=>{var $e=Et(),Oi=ye(),Ri=be(),Ui="[object Null]",Vi="[object Undefined]",xe=$e?$e.toStringTag:void 0;function Zi(r){return r==null?r===void 0?Vi:Ui:xe&&xe in Object(r)?Oi(r):Ri(r)}Ae.exports=Zi});var we=f((Ds,Ee)=>{function ji(r){return r!=null&&typeof r=="object"}Ee.exports=ji});var Le=f((Is,Ce)=>{var Di=Se(),Ii=we(),zi="[object Symbol]";function Bi(r){return typeof r=="symbol"||Ii(r)&&Di(r)==zi}Ce.exports=Bi});var qe=f((zs,Te)=>{var Wi=pe(),Me=At(),Fi=Le(),He=NaN,Ji=/^[-+]0x[0-9a-f]+$/i,Ki=/^0b[01]+$/i,Xi=/^0o[0-7]+$/i,Gi=parseInt;function Yi(r){if(typeof r=="number")return r;if(Fi(r))return He;if(Me(r)){var t=typeof r.valueOf=="function"?r.valueOf():r;r=Me(t)?t+"":t}if(typeof r!="string")return r===0?r:+r;r=Wi(r);var e=Ki.test(r);return e||Xi.test(r)?Gi(r.slice(2),e?2:8):Ji.test(r)?He:+r}Te.exports=Yi});var Ct=f((Bs,Pe)=>{var Qi=At(),wt=ce(),ke=qe(),tr="Expected a function",er=Math.max,ir=Math.min;function rr(r,t,e){var i,s,n,o,l,a,d=0,g=!1,c=!1,y=!0;if(typeof r!="function")throw new TypeError(tr);t=ke(t)||0,Qi(e)&&(g=!!e.leading,c="maxWait"in e,n=c?er(ke(e.maxWait)||0,t):n,y="trailing"in e?!!e.trailing:y);function x(m){var w=i,j=s;return i=s=void 0,d=m,o=r.apply(j,w),o}function P(m){return d=m,l=setTimeout(tt,t),g?x(m):o}function Re(m){var w=m-a,j=m-d,Ht=t-w;return c?ir(Ht,n-j):Ht}function Lt(m){var w=m-a,j=m-d;return a===void 0||w>=t||w<0||c&&j>=n}function tt(){var m=wt();if(Lt(m))return Mt(m);l=setTimeout(tt,Re(m))}function Mt(m){return l=void 0,y&&i?x(m):(i=s=void 0,o)}function Ue(){l!==void 0&&clearTimeout(l),d=0,i=a=s=l=void 0}function Ve(){return l===void 0?o:Mt(wt())}function dt(){var m=wt(),w=Lt(m);if(i=arguments,s=this,a=m,w){if(l===void 0)return P(a);if(c)return clearTimeout(l),l=setTimeout(tt,t),x(a)}return l===void 0&&(l=setTimeout(tt,t)),o}return dt.cancel=Ue,dt.flush=Ve,dt}Pe.exports=rr});var et=globalThis,it=et.ShadowRoot&&(et.ShadyCSS===void 0||et.ShadyCSS.nativeShadow)&&"adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,pt=Symbol(),kt=new WeakMap,D=class{constructor(t,e,i){if(this._$cssResult$=!0,i!==pt)throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=t,this.t=e}get styleSheet(){let t=this.o,e=this.t;if(it&&t===void 0){let i=e!==void 0&&e.length===1;i&&(t=kt.get(e)),t===void 0&&((this.o=t=new CSSStyleSheet).replaceSync(this.cssText),i&&kt.set(e,t))}return t}toString(){return this.cssText}},L=r=>new D(typeof r=="string"?r:r+"",void 0,pt),M=(r,...t)=>{let e=r.length===1?r[0]:t.reduce((i,s,n)=>i+(o=>{if(o._$cssResult$===!0)return o.cssText;if(typeof o=="number")return o;throw Error("Value passed to 'css' function must be a 'css' function result: "+o+". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.")})(s)+r[n+1],r[0]);return new D(e,r,pt)},gt=(r,t)=>{if(it)r.adoptedStyleSheets=t.map(e=>e instanceof CSSStyleSheet?e:e.styleSheet);else for(let e of t){let i=document.createElement("style"),s=et.litNonce;s!==void 0&&i.setAttribute("nonce",s),i.textContent=e.cssText,r.appendChild(i)}},rt=it?r=>r:r=>r instanceof CSSStyleSheet?(t=>{let e="";for(let i of t.cssRules)e+=i.cssText;return L(e)})(r):r;var{is:Be,defineProperty:We,getOwnPropertyDescriptor:Fe,getOwnPropertyNames:Je,getOwnPropertySymbols:Ke,getPrototypeOf:Xe}=Object,st=globalThis,Pt=st.trustedTypes,Ge=Pt?Pt.emptyScript:"",Ye=st.reactiveElementPolyfillSupport,I=(r,t)=>r,z={toAttribute(r,t){switch(t){case Boolean:r=r?Ge:null;break;case Object:case Array:r=r==null?r:JSON.stringify(r)}return r},fromAttribute(r,t){let e=r;switch(t){case Boolean:e=r!==null;break;case Number:e=r===null?null:Number(r);break;case Object:case Array:try{e=JSON.parse(r)}catch{e=null}}return e}},nt=(r,t)=>!Be(r,t),Nt={attribute:!0,type:String,converter:z,reflect:!1,hasChanged:nt};Symbol.metadata??=Symbol("metadata"),st.litPropertyMetadata??=new WeakMap;var S=class extends HTMLElement{static addInitializer(t){this._$Ei(),(this.l??=[]).push(t)}static get observedAttributes(){return this.finalize(),this._$Eh&&[...this._$Eh.keys()]}static createProperty(t,e=Nt){if(e.state&&(e.attribute=!1),this._$Ei(),this.elementProperties.set(t,e),!e.noAccessor){let i=Symbol(),s=this.getPropertyDescriptor(t,i,e);s!==void 0&&We(this.prototype,t,s)}}static getPropertyDescriptor(t,e,i){let{get:s,set:n}=Fe(this.prototype,t)??{get(){return this[e]},set(o){this[e]=o}};return{get(){return s?.call(this)},set(o){let l=s?.call(this);n.call(this,o),this.requestUpdate(t,l,i)},configurable:!0,enumerable:!0}}static getPropertyOptions(t){return this.elementProperties.get(t)??Nt}static _$Ei(){if(this.hasOwnProperty(I("elementProperties")))return;let t=Xe(this);t.finalize(),t.l!==void 0&&(this.l=[...t.l]),this.elementProperties=new Map(t.elementProperties)}static finalize(){if(this.hasOwnProperty(I("finalized")))return;if(this.finalized=!0,this._$Ei(),this.hasOwnProperty(I("properties"))){let e=this.properties,i=[...Je(e),...Ke(e)];for(let s of i)this.createProperty(s,e[s])}let t=this[Symbol.metadata];if(t!==null){let e=litPropertyMetadata.get(t);if(e!==void 0)for(let[i,s]of e)this.elementProperties.set(i,s)}this._$Eh=new Map;for(let[e,i]of this.elementProperties){let s=this._$Eu(e,i);s!==void 0&&this._$Eh.set(s,e)}this.elementStyles=this.finalizeStyles(this.styles)}static finalizeStyles(t){let e=[];if(Array.isArray(t)){let i=new Set(t.flat(1/0).reverse());for(let s of i)e.unshift(rt(s))}else t!==void 0&&e.push(rt(t));return e}static _$Eu(t,e){let i=e.attribute;return i===!1?void 0:typeof i=="string"?i:typeof t=="string"?t.toLowerCase():void 0}constructor(){super(),this._$Ep=void 0,this.isUpdatePending=!1,this.hasUpdated=!1,this._$Em=null,this._$Ev()}_$Ev(){this._$ES=new Promise(t=>this.enableUpdating=t),this._$AL=new Map,this._$E_(),this.requestUpdate(),this.constructor.l?.forEach(t=>t(this))}addController(t){(this._$EO??=new Set).add(t),this.renderRoot!==void 0&&this.isConnected&&t.hostConnected?.()}removeController(t){this._$EO?.delete(t)}_$E_(){let t=new Map,e=this.constructor.elementProperties;for(let i of e.keys())this.hasOwnProperty(i)&&(t.set(i,this[i]),delete this[i]);t.size>0&&(this._$Ep=t)}createRenderRoot(){let t=this.shadowRoot??this.attachShadow(this.constructor.shadowRootOptions);return gt(t,this.constructor.elementStyles),t}connectedCallback(){this.renderRoot??=this.createRenderRoot(),this.enableUpdating(!0),this._$EO?.forEach(t=>t.hostConnected?.())}enableUpdating(t){}disconnectedCallback(){this._$EO?.forEach(t=>t.hostDisconnected?.())}attributeChangedCallback(t,e,i){this._$AK(t,i)}_$EC(t,e){let i=this.constructor.elementProperties.get(t),s=this.constructor._$Eu(t,i);if(s!==void 0&&i.reflect===!0){let n=(i.converter?.toAttribute!==void 0?i.converter:z).toAttribute(e,i.type);this._$Em=t,n==null?this.removeAttribute(s):this.setAttribute(s,n),this._$Em=null}}_$AK(t,e){let i=this.constructor,s=i._$Eh.get(t);if(s!==void 0&&this._$Em!==s){let n=i.getPropertyOptions(s),o=typeof n.converter=="function"?{fromAttribute:n.converter}:n.converter?.fromAttribute!==void 0?n.converter:z;this._$Em=s,this[s]=o.fromAttribute(e,n.type),this._$Em=null}}requestUpdate(t,e,i){if(t!==void 0){if(i??=this.constructor.getPropertyOptions(t),!(i.hasChanged??nt)(this[t],e))return;this.P(t,e,i)}this.isUpdatePending===!1&&(this._$ES=this._$ET())}P(t,e,i){this._$AL.has(t)||this._$AL.set(t,e),i.reflect===!0&&this._$Em!==t&&(this._$Ej??=new Set).add(t)}async _$ET(){this.isUpdatePending=!0;try{await this._$ES}catch(e){Promise.reject(e)}let t=this.scheduleUpdate();return t!=null&&await t,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){if(!this.isUpdatePending)return;if(!this.hasUpdated){if(this.renderRoot??=this.createRenderRoot(),this._$Ep){for(let[s,n]of this._$Ep)this[s]=n;this._$Ep=void 0}let i=this.constructor.elementProperties;if(i.size>0)for(let[s,n]of i)n.wrapped!==!0||this._$AL.has(s)||this[s]===void 0||this.P(s,this[s],n)}let t=!1,e=this._$AL;try{t=this.shouldUpdate(e),t?(this.willUpdate(e),this._$EO?.forEach(i=>i.hostUpdate?.()),this.update(e)):this._$EU()}catch(i){throw t=!1,this._$EU(),i}t&&this._$AE(e)}willUpdate(t){}_$AE(t){this._$EO?.forEach(e=>e.hostUpdated?.()),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(t)),this.updated(t)}_$EU(){this._$AL=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this._$ES}shouldUpdate(t){return!0}update(t){this._$Ej&&=this._$Ej.forEach(e=>this._$EC(e,this[e])),this._$EU()}updated(t){}firstUpdated(t){}};S.elementStyles=[],S.shadowRootOptions={mode:"open"},S[I("elementProperties")]=new Map,S[I("finalized")]=new Map,Ye?.({ReactiveElement:S}),(st.reactiveElementVersions??=[]).push("2.0.4");var $t=globalThis,ot=$t.trustedTypes,Ot=ot?ot.createPolicy("lit-html",{createHTML:r=>r}):void 0,Dt="$lit$",C=`lit$${Math.random().toFixed(9).slice(2)}$`,It="?"+C,Qe=`<${It}>`,q=document,W=()=>q.createComment(""),F=r=>r===null||typeof r!="object"&&typeof r!="function",zt=Array.isArray,ti=r=>zt(r)||typeof r?.[Symbol.iterator]=="function",mt=`[ 	
\f\r]`,B=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,Rt=/-->/g,Ut=/>/g,H=RegExp(`>|${mt}(?:([^\\s"'>=/]+)(${mt}*=${mt}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`,"g"),Vt=/'/g,Zt=/"/g,Bt=/^(?:script|style|textarea|title)$/i,Wt=r=>(t,...e)=>({_$litType$:r,strings:t,values:e}),_=Wt(1),cr=Wt(2),E=Symbol.for("lit-noChange"),p=Symbol.for("lit-nothing"),jt=new WeakMap,T=q.createTreeWalker(q,129);function Ft(r,t){if(!Array.isArray(r)||!r.hasOwnProperty("raw"))throw Error("invalid template strings array");return Ot!==void 0?Ot.createHTML(t):t}var ei=(r,t)=>{let e=r.length-1,i=[],s,n=t===2?"<svg>":"",o=B;for(let l=0;l<e;l++){let a=r[l],d,g,c=-1,y=0;for(;y<a.length&&(o.lastIndex=y,g=o.exec(a),g!==null);)y=o.lastIndex,o===B?g[1]==="!--"?o=Rt:g[1]!==void 0?o=Ut:g[2]!==void 0?(Bt.test(g[2])&&(s=RegExp("</"+g[2],"g")),o=H):g[3]!==void 0&&(o=H):o===H?g[0]===">"?(o=s??B,c=-1):g[1]===void 0?c=-2:(c=o.lastIndex-g[2].length,d=g[1],o=g[3]===void 0?H:g[3]==='"'?Zt:Vt):o===Zt||o===Vt?o=H:o===Rt||o===Ut?o=B:(o=H,s=void 0);let x=o===H&&r[l+1].startsWith("/>")?" ":"";n+=o===B?a+Qe:c>=0?(i.push(d),a.slice(0,c)+Dt+a.slice(c)+C+x):a+C+(c===-2?l:x)}return[Ft(r,n+(r[e]||"<?>")+(t===2?"</svg>":"")),i]},J=class r{constructor({strings:t,_$litType$:e},i){let s;this.parts=[];let n=0,o=0,l=t.length-1,a=this.parts,[d,g]=ei(t,e);if(this.el=r.createElement(d,i),T.currentNode=this.el.content,e===2){let c=this.el.content.firstChild;c.replaceWith(...c.childNodes)}for(;(s=T.nextNode())!==null&&a.length<l;){if(s.nodeType===1){if(s.hasAttributes())for(let c of s.getAttributeNames())if(c.endsWith(Dt)){let y=g[o++],x=s.getAttribute(c).split(C),P=/([.?@])?(.*)/.exec(y);a.push({type:1,index:n,name:P[2],strings:x,ctor:P[1]==="."?vt:P[1]==="?"?yt:P[1]==="@"?_t:O}),s.removeAttribute(c)}else c.startsWith(C)&&(a.push({type:6,index:n}),s.removeAttribute(c));if(Bt.test(s.tagName)){let c=s.textContent.split(C),y=c.length-1;if(y>0){s.textContent=ot?ot.emptyScript:"";for(let x=0;x<y;x++)s.append(c[x],W()),T.nextNode(),a.push({type:2,index:++n});s.append(c[y],W())}}}else if(s.nodeType===8)if(s.data===It)a.push({type:2,index:n});else{let c=-1;for(;(c=s.data.indexOf(C,c+1))!==-1;)a.push({type:7,index:n}),c+=C.length-1}n++}}static createElement(t,e){let i=q.createElement("template");return i.innerHTML=t,i}};function N(r,t,e=r,i){if(t===E)return t;let s=i!==void 0?e._$Co?.[i]:e._$Cl,n=F(t)?void 0:t._$litDirective$;return s?.constructor!==n&&(s?._$AO?.(!1),n===void 0?s=void 0:(s=new n(r),s._$AT(r,e,i)),i!==void 0?(e._$Co??=[])[i]=s:e._$Cl=s),s!==void 0&&(t=N(r,s._$AS(r,t.values),s,i)),t}var ft=class{constructor(t,e){this._$AV=[],this._$AN=void 0,this._$AD=t,this._$AM=e}get parentNode(){return this._$AM.parentNode}get _$AU(){return this._$AM._$AU}u(t){let{el:{content:e},parts:i}=this._$AD,s=(t?.creationScope??q).importNode(e,!0);T.currentNode=s;let n=T.nextNode(),o=0,l=0,a=i[0];for(;a!==void 0;){if(o===a.index){let d;a.type===2?d=new K(n,n.nextSibling,this,t):a.type===1?d=new a.ctor(n,a.name,a.strings,this,t):a.type===6&&(d=new bt(n,this,t)),this._$AV.push(d),a=i[++l]}o!==a?.index&&(n=T.nextNode(),o++)}return T.currentNode=q,s}p(t){let e=0;for(let i of this._$AV)i!==void 0&&(i.strings!==void 0?(i._$AI(t,i,e),e+=i.strings.length-2):i._$AI(t[e])),e++}},K=class r{get _$AU(){return this._$AM?._$AU??this._$Cv}constructor(t,e,i,s){this.type=2,this._$AH=p,this._$AN=void 0,this._$AA=t,this._$AB=e,this._$AM=i,this.options=s,this._$Cv=s?.isConnected??!0}get parentNode(){let t=this._$AA.parentNode,e=this._$AM;return e!==void 0&&t?.nodeType===11&&(t=e.parentNode),t}get startNode(){return this._$AA}get endNode(){return this._$AB}_$AI(t,e=this){t=N(this,t,e),F(t)?t===p||t==null||t===""?(this._$AH!==p&&this._$AR(),this._$AH=p):t!==this._$AH&&t!==E&&this._(t):t._$litType$!==void 0?this.$(t):t.nodeType!==void 0?this.T(t):ti(t)?this.k(t):this._(t)}S(t){return this._$AA.parentNode.insertBefore(t,this._$AB)}T(t){this._$AH!==t&&(this._$AR(),this._$AH=this.S(t))}_(t){this._$AH!==p&&F(this._$AH)?this._$AA.nextSibling.data=t:this.T(q.createTextNode(t)),this._$AH=t}$(t){let{values:e,_$litType$:i}=t,s=typeof i=="number"?this._$AC(t):(i.el===void 0&&(i.el=J.createElement(Ft(i.h,i.h[0]),this.options)),i);if(this._$AH?._$AD===s)this._$AH.p(e);else{let n=new ft(s,this),o=n.u(this.options);n.p(e),this.T(o),this._$AH=n}}_$AC(t){let e=jt.get(t.strings);return e===void 0&&jt.set(t.strings,e=new J(t)),e}k(t){zt(this._$AH)||(this._$AH=[],this._$AR());let e=this._$AH,i,s=0;for(let n of t)s===e.length?e.push(i=new r(this.S(W()),this.S(W()),this,this.options)):i=e[s],i._$AI(n),s++;s<e.length&&(this._$AR(i&&i._$AB.nextSibling,s),e.length=s)}_$AR(t=this._$AA.nextSibling,e){for(this._$AP?.(!1,!0,e);t&&t!==this._$AB;){let i=t.nextSibling;t.remove(),t=i}}setConnected(t){this._$AM===void 0&&(this._$Cv=t,this._$AP?.(t))}},O=class{get tagName(){return this.element.tagName}get _$AU(){return this._$AM._$AU}constructor(t,e,i,s,n){this.type=1,this._$AH=p,this._$AN=void 0,this.element=t,this.name=e,this._$AM=s,this.options=n,i.length>2||i[0]!==""||i[1]!==""?(this._$AH=Array(i.length-1).fill(new String),this.strings=i):this._$AH=p}_$AI(t,e=this,i,s){let n=this.strings,o=!1;if(n===void 0)t=N(this,t,e,0),o=!F(t)||t!==this._$AH&&t!==E,o&&(this._$AH=t);else{let l=t,a,d;for(t=n[0],a=0;a<n.length-1;a++)d=N(this,l[i+a],e,a),d===E&&(d=this._$AH[a]),o||=!F(d)||d!==this._$AH[a],d===p?t=p:t!==p&&(t+=(d??"")+n[a+1]),this._$AH[a]=d}o&&!s&&this.j(t)}j(t){t===p?this.element.removeAttribute(this.name):this.element.setAttribute(this.name,t??"")}},vt=class extends O{constructor(){super(...arguments),this.type=3}j(t){this.element[this.name]=t===p?void 0:t}},yt=class extends O{constructor(){super(...arguments),this.type=4}j(t){this.element.toggleAttribute(this.name,!!t&&t!==p)}},_t=class extends O{constructor(t,e,i,s,n){super(t,e,i,s,n),this.type=5}_$AI(t,e=this){if((t=N(this,t,e,0)??p)===E)return;let i=this._$AH,s=t===p&&i!==p||t.capture!==i.capture||t.once!==i.once||t.passive!==i.passive,n=t!==p&&(i===p||s);s&&this.element.removeEventListener(this.name,this,i),n&&this.element.addEventListener(this.name,this,t),this._$AH=t}handleEvent(t){typeof this._$AH=="function"?this._$AH.call(this.options?.host??this.element,t):this._$AH.handleEvent(t)}},bt=class{constructor(t,e,i){this.element=t,this.type=6,this._$AN=void 0,this._$AM=e,this.options=i}get _$AU(){return this._$AM._$AU}_$AI(t){N(this,t)}};var ii=$t.litHtmlPolyfillSupport;ii?.(J,K),($t.litHtmlVersions??=[]).push("3.1.3");var Jt=(r,t,e)=>{let i=e?.renderBefore??t,s=i._$litPart$;if(s===void 0){let n=e?.renderBefore??null;i._$litPart$=s=new K(t.insertBefore(W(),n),n,void 0,e??{})}return s._$AI(r),s};var b=class extends S{constructor(){super(...arguments),this.renderOptions={host:this},this._$Do=void 0}createRenderRoot(){let t=super.createRenderRoot();return this.renderOptions.renderBefore??=t.firstChild,t}update(t){let e=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(t),this._$Do=Jt(e,this.renderRoot,this.renderOptions)}connectedCallback(){super.connectedCallback(),this._$Do?.setConnected(!0)}disconnectedCallback(){super.disconnectedCallback(),this._$Do?.setConnected(!1)}render(){return E}};b._$litElement$=!0,b.finalized=!0,globalThis.litElementHydrateSupport?.({LitElement:b});var ri=globalThis.litElementPolyfillSupport;ri?.({LitElement:b});(globalThis.litElementVersions??=[]).push("4.0.5");var Kt={ATTRIBUTE:1,CHILD:2,PROPERTY:3,BOOLEAN_ATTRIBUTE:4,EVENT:5,ELEMENT:6},Xt=r=>(...t)=>({_$litDirective$:r,values:t}),at=class{constructor(t){}get _$AU(){return this._$AM._$AU}_$AT(t,e,i){this._$Ct=t,this._$AM=e,this._$Ci=i}_$AS(t,e){return this.update(t,e)}update(t,e){return this.render(...e)}};var X=class extends at{constructor(t){if(super(t),this.it=p,t.type!==Kt.CHILD)throw Error(this.constructor.directiveName+"() can only be used in child bindings")}render(t){if(t===p||t==null)return this._t=void 0,this.it=t;if(t===E)return t;if(typeof t!="string")throw Error(this.constructor.directiveName+"() called with a non-string value");if(t===this.it)return this._t;this.it=t;let e=[t];return e.raw=e,this._t={_$litType$:this.constructor.resultType,strings:e,values:[]}}};X.directiveName="unsafeHTML",X.resultType=1;var xt=Xt(X);var R=r=>(t,e)=>{e!==void 0?e.addInitializer(()=>{customElements.define(r,t)}):customElements.define(r,t)};var si={attribute:!0,type:String,converter:z,reflect:!1,hasChanged:nt},ni=(r=si,t,e)=>{let{kind:i,metadata:s}=e,n=globalThis.litPropertyMetadata.get(s);if(n===void 0&&globalThis.litPropertyMetadata.set(s,n=new Map),n.set(e.name,r),i==="accessor"){let{name:o}=e;return{set(l){let a=t.get.call(this);t.set.call(this,l),this.requestUpdate(o,a,r)},init(l){return l!==void 0&&this.P(o,void 0,r),l}}}if(i==="setter"){let{name:o}=e;return function(l){let a=this[o];t.call(this,l),this.requestUpdate(o,a,r)}}throw Error("Unsupported decorator location: "+i)};function u(r){return(t,e)=>typeof e=="object"?ni(r,t,e):((i,s,n)=>{let o=s.hasOwnProperty(n);return s.constructor.createProperty(n,o?{...i,wrapped:!0}:i),o?Object.getOwnPropertyDescriptor(s,n):void 0})(r,t,e)}function G(r){return u({...r,state:!0,attribute:!1})}var U=(r,t,e)=>(e.configurable=!0,e.enumerable=!0,Reflect.decorate&&typeof t!="object"&&Object.defineProperty(r,t,e),e);var oi;function Gt(r){return(t,e)=>U(t,e,{get(){return(this.renderRoot??(oi??=document.createDocumentFragment())).querySelectorAll(r)}})}var lt="/static/bundle/assets/docsicon-tutorials-NASJKIV7.svg";var V="/static/bundle/assets/docsicon-guides-J4TJ4ROM.svg";var Yt="/static/bundle/assets/docsicon-pdf-6MMHYQNH.svg";var Qt="/static/bundle/assets/docsicon-concepts-KZZ2X25M.svg";var te="/static/bundle/assets/docsicon-reference-QE5L5QYD.svg";var ee="/static/bundle/assets/quarkus_icon_default-2JCEOIDL.svg";var ie="/static/bundle/assets/quarkiverse_icon_default-REXGUNGY.svg";var re="/static/bundle/assets/loading-VOBOF5NI.svg";var mi={docs:{tutorial:lt,tutorials:lt,guide:V,guides:V,howto:V,pdf:Yt,concepts:Qt,reference:te},origins:{quarkus:ee,quarkiverse:ie},loading:re},Z=mi;var v=class extends b{constructor(){super(...arguments);this.type="default";this.origin="quarkus";this.originsWithRelativeUrls=[]}connectedCallback(){if(this.data)for(let e in this.data)this.data.hasOwnProperty(e)&&(this[e]=this.data[e]);super.connectedCallback()}disconnectedCallback(){super.disconnectedCallback()}render(){return _`
      <div class="qs-hit qs-guide" aria-label="Guide Hit">
        <div class="qs-guide-icon">
          ${xt(this.icon())}
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
    `}relativizeUrl(){return this.originsWithRelativeUrls.includes(this.origin)?this.url.substring(new URL(this.url).origin.length):this.url}icon(){let e=Z.docs[this.type];if(e){let i=e.match(/.*(<svg.*<\/svg>)/);if(i)return i[1]}return""}_renderHTML(e){return e&&(Array.isArray(e)?e.map(i=>_`<p>${this._renderHTML(i)}</p>`):xt(e))}};v.styles=M`
      :host {
          --link-color: #1259A5;
          --link-hover-color: #c00;
          --content-highlight-color: #777;
      }

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
          color: var(--link-hover-color);
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
          background-image: url('${L(Z.origins.quarkus)}');
      }

      .qs-guide .origin.quarkiverse-hub {
          background-image: url('${L(Z.origins.quarkiverse)}');
      }
    
      .qs-guide-icon svg {
        width: 70px;
        margin: 1rem 0 0 0;
        fill: var(--main-text-color);
      }

      .summary {
          min-height: 40px;
      }
  `,h([u({type:Object})],v.prototype,"data",2),h([u({type:String})],v.prototype,"type",2),h([u({type:String})],v.prototype,"url",2),h([u({type:String})],v.prototype,"title",2),h([u({type:String})],v.prototype,"summary",2),h([u({type:String})],v.prototype,"keywords",2),h([u({type:String})],v.prototype,"content",2),h([u({type:String})],v.prototype,"origin",2),h([u({type:String,attribute:"origins-with-relative-urls"})],v.prototype,"originsWithRelativeUrls",2),v=h([R("qs-guide")],v);var Ne=qt(Ct());var Q=class r{static{this.guides=null}static queryDocumentGuides(t="qs-target qs-guide"){let e=document.querySelectorAll(t),i=e?[]:null;for(let s=0;s<e.length;s++){let n=e[s];i.push({title:n.getAttribute("title"),categories:n.getAttribute("categories"),type:n.getAttribute("type"),url:n.getAttribute("url"),summary:n.getAttribute("summary"),keywords:n.getAttribute("keywords"),content:n.getAttribute("content"),origin:n.getAttribute("origin")})}return i}static enableLocalSearch(t){r.guides=r.queryDocumentGuides(t),r.guides!=null&&console.debug("LocalSearch is ready with "+r.guides.length+" guides found.")}static search(t){let e=r.guides;if(e==null)return null;let i=[];t.q&&i.push(...t.q.split(" ").map(n=>n.trim()));let s=[];return t.categories&&(Array.isArray(t.categories)?s.push(...t.categories):s.push(t.categories)),e.filter(n=>{let o=!0;return o&&s.length>0&&(o=r.containsAllCaseInsensitive(n.categories,s)),o&&i.length>0&&(o=r.containsAllCaseInsensitive(`${n.keywords}${n.summary}${n.title}${n.categories}`,i)),o})}static containsAllCaseInsensitive(t,e){let i=(t||"").toLowerCase();for(let s in e)if(i.indexOf(e[s].toLowerCase())<0)return!1;return!0}};var ht="qs-start",k="qs-result",ct="qs-next-page",$=class extends b{constructor(){super();this.server="";this.minChars=3;this.initialTimeout=1500;this.moreTimeout=2500;this.language="en";this.localSearch=!1;this._page=0;this._currentHitCount=0;this._abortController=null;this._search=()=>{if(this._abortController&&this._abortController.abort(),!this._backendData){this._clearSearch();return}let e=new AbortController;if(this._abortController=e,this.dispatchEvent(new CustomEvent(ht,{detail:{page:this._page}})),this.localSearch){this._localSearch(),this._abortController==e&&(this._abortController=null);return}this._jsonFetch(e,"GET",this._backendData,this._page>0?this.initialTimeout:this.moreTimeout).then(i=>{this._page>0?this._currentHitCount+=i.hits.length:this._currentHitCount=i.hits.length;let s=i.total?.lowerBound,n=i.hits.length>0&&s>this._currentHitCount;this.dispatchEvent(new CustomEvent(k,{detail:{...i,search:this._backendData,page:this._page,hasMoreHits:n}}))}).catch(i=>{console.error("Could not run search: "+i),this._abortController==e&&(this._page=0,this._currentHitCount=0,this._localSearch())}).finally(()=>{this._abortController==e&&(this._abortController=null)})};this._searchDebounced=(0,Ne.default)(this._search,300);this._handleInputChange=e=>{let i=this._getFormElements(),s={language:this.language,contentSnippets:2,contentSnippetsLength:120},n={};this.quarkusversion&&(s.version=this.quarkusversion);var o=0;for(let l of i)this._isInput(l)&&(l.value.length===0||l.value.length<this.minChars)||l.value&&l.value.length>0&&l.name.length>0&&(s[l.name]=l.value,n[l.name]=l.value,o++);o==0?(this._backendData=null,this._browserData=null):(this._backendData=s,this._browserData=n)};this._handleNextPage=e=>{this._page++,this._search()};let e=new URLSearchParams(window.location.hash.substring(1));if(e.size>0){let i=this._getFormElements();for(let s of i){let n=e.get(s.name);n&&(s.value=n)}}}render(){return _`
      <div id="qs-form">
        <slot></slot>
      </div>
    `}update(e){return window.location.hash=this._browserData?new URLSearchParams(this._browserData).toString():"",this._backendData?this._searchDebounced():this._clearSearch(),super.update(e)}connectedCallback(){super.connectedCallback(),Q.enableLocalSearch();let e=this._getFormElements();this.addEventListener(ct,this._handleNextPage),e.forEach(i=>{let s=this._isInput(i)?"input":"change";i.addEventListener(s,this._handleInputChange)}),this._handleInputChange(null)}disconnectedCallback(){super.disconnectedCallback(),this.removeEventListener(ct,this._handleNextPage),this._getFormElements().forEach(i=>{let s=this._isInput(i)?"input":"change";i.removeEventListener(s,this._handleInputChange)})}_getFormElements(){return this.querySelectorAll("input[name], select[name]")}_isInput(e){return e.tagName.toLowerCase()==="input"}async _jsonFetch(e,i,s,n){let o={...s,page:this._page.toString()},l=setTimeout(()=>e.abort(),n),a=await fetch(this.server+"/api/guides/search?"+new URLSearchParams(o).toString(),{method:i,signal:e.signal,body:null});if(clearTimeout(l),a.ok)return await a.json();throw"Response status is "+a.status+"; response: "+await a.text()}_clearSearch(){this._page=0,this._currentHitCount=0,this._abortController&&(this._abortController.abort(),this._abortController=null),this.dispatchEvent(new CustomEvent(k))}_localSearch(){let e=this._backendData,i=Q.search(e);if(i){let s={hits:i,total:i.length};this.dispatchEvent(new CustomEvent(k,{detail:{...s,search:e,page:0,hasMoreHits:!1}}));return}this.dispatchEvent(new CustomEvent(k))}};$.styles=M`

      ::slotted(*) {
          display: block !important;
      }

      .quarkus-search {
          display: block !important;
      }

      .d-none {
          display: none;
      }
  `,h([u({type:String})],$.prototype,"server",2),h([u({type:String,attribute:"min-chars"})],$.prototype,"minChars",2),h([u({type:String,attribute:"initial-timeout"})],$.prototype,"initialTimeout",2),h([u({type:String,attribute:"more-timeout"})],$.prototype,"moreTimeout",2),h([u({type:String})],$.prototype,"language",2),h([u({type:String,attribute:"quarkus-version"})],$.prototype,"quarkusversion",2),h([u({type:String,attribute:"local-search"})],$.prototype,"localSearch",2),h([G({hasChanged(e,i){return JSON.stringify(e)!==JSON.stringify(i)}})],$.prototype,"_backendData",2),$=h([R("qs-form")],$);var Oe=qt(Ct());var A=class extends b{constructor(){super(...arguments);this.type="guide";this.originsWithRelativeUrls=[];this._loading=!0;this._handleScroll=e=>{if(this._loading||!this._result)return;if(!this._result.hasMoreHits){console.debug("no more hits");return}let i=this._hits.length==0?null:this._hits[this._hits.length-1];if(!i)return;let s=document.documentElement,n=s.scrollTop+s.clientHeight,o=i.offsetTop;n>=o&&(this._loading=!0,this._form.dispatchEvent(new CustomEvent(ct)))};this._handleScrollDebounced=(0,Oe.default)(this._handleScroll,100);this._handleResult=e=>{if(console.debug("Received results in qs-target: ",e.detail),this._loadingEnd(),!this._result||!e.detail||!e.detail.hits||e.detail.page===0){e.detail?.hits?document.body.classList.add("qs-has-results"):document.body.classList.remove("qs-has-results"),this._result=e.detail;return}this._result.hits=this._result.hits.concat(e.detail.hits),console.debug(`${this._result.hits.length} results in qs-target: `),this._result.hasMoreHits=e.detail.hasMoreHits};this._loadingStart=e=>{this._loading=!0,e.detail.page===0&&(this._result=void 0)};this._loadingEnd=()=>{this._loading=!1}}connectedCallback(){super.connectedCallback(),this._form=document.querySelector("qs-form"),this._form.addEventListener(k,this._handleResult),this._form.addEventListener(ht,this._loadingStart),document.addEventListener("scroll",this._handleScrollDebounced)}disconnectedCallback(){this._form.removeEventListener(k,this._handleResult),this._form.removeEventListener(ht,this._loadingStart),document.removeEventListener("scroll",this._handleScrollDebounced),super.disconnectedCallback()}render(){if(this._result?.hits){if(this._result.hits.length===0)return _`
          <div id="qs-target" class="no-hits">
            <p>Sorry, no ${this.type}s matched your search. Please try again.</p>
          </div>
        `;let e=this._result.hits.map(i=>this._renderHit(i));return _`
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
      background-image: url('${L(Z.loading)}');
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
   
  `,h([u({type:String})],A.prototype,"type",2),h([u({type:String,attribute:"origins-with-relative-urls"})],A.prototype,"originsWithRelativeUrls",2),h([G()],A.prototype,"_result",2),h([G()],A.prototype,"_loading",2),h([Gt(".qs-hit")],A.prototype,"_hits",2),A=h([R("qs-target")],A);export{Q as LocalSearch,ct as QS_NEXT_PAGE_EVENT,k as QS_RESULT_EVENT,ht as QS_START_EVENT,$ as QsForm,v as QsGuide,A as QsTarget};
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
*/

