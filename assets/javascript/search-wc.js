var je=Object.create;var gt=Object.defineProperty;var Pt=Object.getOwnPropertyDescriptor;var Ie=Object.getOwnPropertyNames;var De=Object.getPrototypeOf,Ze=Object.prototype.hasOwnProperty;var v=(r,t)=>()=>(t||r((t={exports:{}}).exports,t),t.exports);var Be=(r,t,e,i)=>{if(t&&typeof t=="object"||typeof t=="function")for(let s of Ie(t))!Ze.call(r,s)&&s!==e&&gt(r,s,{get:()=>t[s],enumerable:!(i=Pt(t,s))||i.enumerable});return r};var Ot=(r,t,e)=>(e=r!=null?je(De(r)):{},Be(t||!r||!r.__esModule?gt(e,"default",{value:r,enumerable:!0}):e,r));var c=(r,t,e,i)=>{for(var s=i>1?void 0:i?Pt(t,e):t,n=r.length-1,o;n>=0;n--)(o=r[n])&&(s=(i?o(t,e,s):o(s))||s);return i&&s&&gt(t,e,s),s};var wt=v((Ps,ne)=>{function vi(r){var t=typeof r;return r!=null&&(t=="object"||t=="function")}ne.exports=vi});var ae=v((Os,oe)=>{var _i=typeof global=="object"&&global&&global.Object===Object&&global;oe.exports=_i});var Ct=v((Rs,le)=>{var yi=ae(),bi=typeof self=="object"&&self&&self.Object===Object&&self,$i=yi||bi||Function("return this")();le.exports=$i});var ce=v((Ns,he)=>{var xi=Ct(),Ai=function(){return xi.Date.now()};he.exports=Ai});var de=v((Us,ue)=>{var Si=/\s/;function Ei(r){for(var t=r.length;t--&&Si.test(r.charAt(t)););return t}ue.exports=Ei});var ge=v((Vs,pe)=>{var wi=de(),Ci=/^\s+/;function qi(r){return r&&r.slice(0,wi(r)+1).replace(Ci,"")}pe.exports=qi});var qt=v((zs,me)=>{var Li=Ct(),Ti=Li.Symbol;me.exports=Ti});var ye=v((js,_e)=>{var fe=qt(),ve=Object.prototype,ki=ve.hasOwnProperty,Hi=ve.toString,Q=fe?fe.toStringTag:void 0;function Mi(r){var t=ki.call(r,Q),e=r[Q];try{r[Q]=void 0;var i=!0}catch{}var s=Hi.call(r);return i&&(t?r[Q]=e:delete r[Q]),s}_e.exports=Mi});var $e=v((Is,be)=>{var Pi=Object.prototype,Oi=Pi.toString;function Ri(r){return Oi.call(r)}be.exports=Ri});var Ee=v((Ds,Se)=>{var xe=qt(),Ni=ye(),Ui=$e(),Vi="[object Null]",zi="[object Undefined]",Ae=xe?xe.toStringTag:void 0;function ji(r){return r==null?r===void 0?zi:Vi:Ae&&Ae in Object(r)?Ni(r):Ui(r)}Se.exports=ji});var Ce=v((Zs,we)=>{function Ii(r){return r!=null&&typeof r=="object"}we.exports=Ii});var Le=v((Bs,qe)=>{var Di=Ee(),Zi=Ce(),Bi="[object Symbol]";function Wi(r){return typeof r=="symbol"||Zi(r)&&Di(r)==Bi}qe.exports=Wi});var Me=v((Ws,He)=>{var Fi=ge(),Te=wt(),Gi=Le(),ke=NaN,Ji=/^[-+]0x[0-9a-f]+$/i,Ki=/^0b[01]+$/i,Xi=/^0o[0-7]+$/i,Yi=parseInt;function Qi(r){if(typeof r=="number")return r;if(Gi(r))return ke;if(Te(r)){var t=typeof r.valueOf=="function"?r.valueOf():r;r=Te(t)?t+"":t}if(typeof r!="string")return r===0?r:+r;r=Fi(r);var e=Ki.test(r);return e||Xi.test(r)?Yi(r.slice(2),e?2:8):Ji.test(r)?ke:+r}He.exports=Qi});var Tt=v((Fs,Oe)=>{var tr=wt(),Lt=ce(),Pe=Me(),er="Expected a function",ir=Math.max,rr=Math.min;function sr(r,t,e){var i,s,n,o,l,a,d=0,g=!1,h=!1,b=!0;if(typeof r!="function")throw new TypeError(er);t=Pe(t)||0,tr(e)&&(g=!!e.leading,h="maxWait"in e,n=h?ir(Pe(e.maxWait)||0,t):n,b="trailing"in e?!!e.trailing:b);function x(m){var w=i,I=s;return i=s=void 0,d=m,o=r.apply(I,w),o}function O(m){return d=m,l=setTimeout(et,t),g?x(m):o}function Ue(m){var w=m-a,I=m-d,Mt=t-w;return h?rr(Mt,n-I):Mt}function kt(m){var w=m-a,I=m-d;return a===void 0||w>=t||w<0||h&&I>=n}function et(){var m=Lt();if(kt(m))return Ht(m);l=setTimeout(et,Ue(m))}function Ht(m){return l=void 0,b&&i?x(m):(i=s=void 0,o)}function Ve(){l!==void 0&&clearTimeout(l),d=0,i=a=s=l=void 0}function ze(){return l===void 0?o:Ht(Lt())}function pt(){var m=Lt(),w=kt(m);if(i=arguments,s=this,a=m,w){if(l===void 0)return O(a);if(h)return clearTimeout(l),l=setTimeout(et,t),x(a)}return l===void 0&&(l=setTimeout(et,t)),o}return pt.cancel=Ve,pt.flush=ze,pt}Oe.exports=sr});var it=globalThis,rt=it.ShadowRoot&&(it.ShadyCSS===void 0||it.ShadyCSS.nativeShadow)&&"adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,mt=Symbol(),Rt=new WeakMap,D=class{constructor(t,e,i){if(this._$cssResult$=!0,i!==mt)throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=t,this.t=e}get styleSheet(){let t=this.o,e=this.t;if(rt&&t===void 0){let i=e!==void 0&&e.length===1;i&&(t=Rt.get(e)),t===void 0&&((this.o=t=new CSSStyleSheet).replaceSync(this.cssText),i&&Rt.set(e,t))}return t}toString(){return this.cssText}},q=r=>new D(typeof r=="string"?r:r+"",void 0,mt),L=(r,...t)=>{let e=r.length===1?r[0]:t.reduce((i,s,n)=>i+(o=>{if(o._$cssResult$===!0)return o.cssText;if(typeof o=="number")return o;throw Error("Value passed to 'css' function must be a 'css' function result: "+o+". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.")})(s)+r[n+1],r[0]);return new D(e,r,mt)},ft=(r,t)=>{if(rt)r.adoptedStyleSheets=t.map(e=>e instanceof CSSStyleSheet?e:e.styleSheet);else for(let e of t){let i=document.createElement("style"),s=it.litNonce;s!==void 0&&i.setAttribute("nonce",s),i.textContent=e.cssText,r.appendChild(i)}},st=rt?r=>r:r=>r instanceof CSSStyleSheet?(t=>{let e="";for(let i of t.cssRules)e+=i.cssText;return q(e)})(r):r;var{is:We,defineProperty:Fe,getOwnPropertyDescriptor:Ge,getOwnPropertyNames:Je,getOwnPropertySymbols:Ke,getPrototypeOf:Xe}=Object,nt=globalThis,Nt=nt.trustedTypes,Ye=Nt?Nt.emptyScript:"",Qe=nt.reactiveElementPolyfillSupport,Z=(r,t)=>r,B={toAttribute(r,t){switch(t){case Boolean:r=r?Ye:null;break;case Object:case Array:r=r==null?r:JSON.stringify(r)}return r},fromAttribute(r,t){let e=r;switch(t){case Boolean:e=r!==null;break;case Number:e=r===null?null:Number(r);break;case Object:case Array:try{e=JSON.parse(r)}catch{e=null}}return e}},ot=(r,t)=>!We(r,t),Ut={attribute:!0,type:String,converter:B,reflect:!1,hasChanged:ot};Symbol.metadata??=Symbol("metadata"),nt.litPropertyMetadata??=new WeakMap;var S=class extends HTMLElement{static addInitializer(t){this._$Ei(),(this.l??=[]).push(t)}static get observedAttributes(){return this.finalize(),this._$Eh&&[...this._$Eh.keys()]}static createProperty(t,e=Ut){if(e.state&&(e.attribute=!1),this._$Ei(),this.elementProperties.set(t,e),!e.noAccessor){let i=Symbol(),s=this.getPropertyDescriptor(t,i,e);s!==void 0&&Fe(this.prototype,t,s)}}static getPropertyDescriptor(t,e,i){let{get:s,set:n}=Ge(this.prototype,t)??{get(){return this[e]},set(o){this[e]=o}};return{get(){return s?.call(this)},set(o){let l=s?.call(this);n.call(this,o),this.requestUpdate(t,l,i)},configurable:!0,enumerable:!0}}static getPropertyOptions(t){return this.elementProperties.get(t)??Ut}static _$Ei(){if(this.hasOwnProperty(Z("elementProperties")))return;let t=Xe(this);t.finalize(),t.l!==void 0&&(this.l=[...t.l]),this.elementProperties=new Map(t.elementProperties)}static finalize(){if(this.hasOwnProperty(Z("finalized")))return;if(this.finalized=!0,this._$Ei(),this.hasOwnProperty(Z("properties"))){let e=this.properties,i=[...Je(e),...Ke(e)];for(let s of i)this.createProperty(s,e[s])}let t=this[Symbol.metadata];if(t!==null){let e=litPropertyMetadata.get(t);if(e!==void 0)for(let[i,s]of e)this.elementProperties.set(i,s)}this._$Eh=new Map;for(let[e,i]of this.elementProperties){let s=this._$Eu(e,i);s!==void 0&&this._$Eh.set(s,e)}this.elementStyles=this.finalizeStyles(this.styles)}static finalizeStyles(t){let e=[];if(Array.isArray(t)){let i=new Set(t.flat(1/0).reverse());for(let s of i)e.unshift(st(s))}else t!==void 0&&e.push(st(t));return e}static _$Eu(t,e){let i=e.attribute;return i===!1?void 0:typeof i=="string"?i:typeof t=="string"?t.toLowerCase():void 0}constructor(){super(),this._$Ep=void 0,this.isUpdatePending=!1,this.hasUpdated=!1,this._$Em=null,this._$Ev()}_$Ev(){this._$ES=new Promise(t=>this.enableUpdating=t),this._$AL=new Map,this._$E_(),this.requestUpdate(),this.constructor.l?.forEach(t=>t(this))}addController(t){(this._$EO??=new Set).add(t),this.renderRoot!==void 0&&this.isConnected&&t.hostConnected?.()}removeController(t){this._$EO?.delete(t)}_$E_(){let t=new Map,e=this.constructor.elementProperties;for(let i of e.keys())this.hasOwnProperty(i)&&(t.set(i,this[i]),delete this[i]);t.size>0&&(this._$Ep=t)}createRenderRoot(){let t=this.shadowRoot??this.attachShadow(this.constructor.shadowRootOptions);return ft(t,this.constructor.elementStyles),t}connectedCallback(){this.renderRoot??=this.createRenderRoot(),this.enableUpdating(!0),this._$EO?.forEach(t=>t.hostConnected?.())}enableUpdating(t){}disconnectedCallback(){this._$EO?.forEach(t=>t.hostDisconnected?.())}attributeChangedCallback(t,e,i){this._$AK(t,i)}_$EC(t,e){let i=this.constructor.elementProperties.get(t),s=this.constructor._$Eu(t,i);if(s!==void 0&&i.reflect===!0){let n=(i.converter?.toAttribute!==void 0?i.converter:B).toAttribute(e,i.type);this._$Em=t,n==null?this.removeAttribute(s):this.setAttribute(s,n),this._$Em=null}}_$AK(t,e){let i=this.constructor,s=i._$Eh.get(t);if(s!==void 0&&this._$Em!==s){let n=i.getPropertyOptions(s),o=typeof n.converter=="function"?{fromAttribute:n.converter}:n.converter?.fromAttribute!==void 0?n.converter:B;this._$Em=s,this[s]=o.fromAttribute(e,n.type),this._$Em=null}}requestUpdate(t,e,i){if(t!==void 0){if(i??=this.constructor.getPropertyOptions(t),!(i.hasChanged??ot)(this[t],e))return;this.P(t,e,i)}this.isUpdatePending===!1&&(this._$ES=this._$ET())}P(t,e,i){this._$AL.has(t)||this._$AL.set(t,e),i.reflect===!0&&this._$Em!==t&&(this._$Ej??=new Set).add(t)}async _$ET(){this.isUpdatePending=!0;try{await this._$ES}catch(e){Promise.reject(e)}let t=this.scheduleUpdate();return t!=null&&await t,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){if(!this.isUpdatePending)return;if(!this.hasUpdated){if(this.renderRoot??=this.createRenderRoot(),this._$Ep){for(let[s,n]of this._$Ep)this[s]=n;this._$Ep=void 0}let i=this.constructor.elementProperties;if(i.size>0)for(let[s,n]of i)n.wrapped!==!0||this._$AL.has(s)||this[s]===void 0||this.P(s,this[s],n)}let t=!1,e=this._$AL;try{t=this.shouldUpdate(e),t?(this.willUpdate(e),this._$EO?.forEach(i=>i.hostUpdate?.()),this.update(e)):this._$EU()}catch(i){throw t=!1,this._$EU(),i}t&&this._$AE(e)}willUpdate(t){}_$AE(t){this._$EO?.forEach(e=>e.hostUpdated?.()),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(t)),this.updated(t)}_$EU(){this._$AL=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this._$ES}shouldUpdate(t){return!0}update(t){this._$Ej&&=this._$Ej.forEach(e=>this._$EC(e,this[e])),this._$EU()}updated(t){}firstUpdated(t){}};S.elementStyles=[],S.shadowRootOptions={mode:"open"},S[Z("elementProperties")]=new Map,S[Z("finalized")]=new Map,Qe?.({ReactiveElement:S}),(nt.reactiveElementVersions??=[]).push("2.0.4");var At=globalThis,at=At.trustedTypes,Vt=at?at.createPolicy("lit-html",{createHTML:r=>r}):void 0,Bt="$lit$",C=`lit$${Math.random().toFixed(9).slice(2)}$`,Wt="?"+C,ti=`<${Wt}>`,H=document,F=()=>H.createComment(""),G=r=>r===null||typeof r!="object"&&typeof r!="function",St=Array.isArray,ei=r=>St(r)||typeof r?.[Symbol.iterator]=="function",vt=`[ 	
\f\r]`,W=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,zt=/-->/g,jt=/>/g,T=RegExp(`>|${vt}(?:([^\\s"'>=/]+)(${vt}*=${vt}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`,"g"),It=/'/g,Dt=/"/g,Ft=/^(?:script|style|textarea|title)$/i,Et=r=>(t,...e)=>({_$litType$:r,strings:t,values:e}),f=Et(1),cr=Et(2),ur=Et(3),E=Symbol.for("lit-noChange"),p=Symbol.for("lit-nothing"),Zt=new WeakMap,k=H.createTreeWalker(H,129);function Gt(r,t){if(!St(r)||!r.hasOwnProperty("raw"))throw Error("invalid template strings array");return Vt!==void 0?Vt.createHTML(t):t}var ii=(r,t)=>{let e=r.length-1,i=[],s,n=t===2?"<svg>":t===3?"<math>":"",o=W;for(let l=0;l<e;l++){let a=r[l],d,g,h=-1,b=0;for(;b<a.length&&(o.lastIndex=b,g=o.exec(a),g!==null);)b=o.lastIndex,o===W?g[1]==="!--"?o=zt:g[1]!==void 0?o=jt:g[2]!==void 0?(Ft.test(g[2])&&(s=RegExp("</"+g[2],"g")),o=T):g[3]!==void 0&&(o=T):o===T?g[0]===">"?(o=s??W,h=-1):g[1]===void 0?h=-2:(h=o.lastIndex-g[2].length,d=g[1],o=g[3]===void 0?T:g[3]==='"'?Dt:It):o===Dt||o===It?o=T:o===zt||o===jt?o=W:(o=T,s=void 0);let x=o===T&&r[l+1].startsWith("/>")?" ":"";n+=o===W?a+ti:h>=0?(i.push(d),a.slice(0,h)+Bt+a.slice(h)+C+x):a+C+(h===-2?l:x)}return[Gt(r,n+(r[e]||"<?>")+(t===2?"</svg>":t===3?"</math>":"")),i]},J=class r{constructor({strings:t,_$litType$:e},i){let s;this.parts=[];let n=0,o=0,l=t.length-1,a=this.parts,[d,g]=ii(t,e);if(this.el=r.createElement(d,i),k.currentNode=this.el.content,e===2||e===3){let h=this.el.content.firstChild;h.replaceWith(...h.childNodes)}for(;(s=k.nextNode())!==null&&a.length<l;){if(s.nodeType===1){if(s.hasAttributes())for(let h of s.getAttributeNames())if(h.endsWith(Bt)){let b=g[o++],x=s.getAttribute(h).split(C),O=/([.?@])?(.*)/.exec(b);a.push({type:1,index:n,name:O[2],strings:x,ctor:O[1]==="."?yt:O[1]==="?"?bt:O[1]==="@"?$t:N}),s.removeAttribute(h)}else h.startsWith(C)&&(a.push({type:6,index:n}),s.removeAttribute(h));if(Ft.test(s.tagName)){let h=s.textContent.split(C),b=h.length-1;if(b>0){s.textContent=at?at.emptyScript:"";for(let x=0;x<b;x++)s.append(h[x],F()),k.nextNode(),a.push({type:2,index:++n});s.append(h[b],F())}}}else if(s.nodeType===8)if(s.data===Wt)a.push({type:2,index:n});else{let h=-1;for(;(h=s.data.indexOf(C,h+1))!==-1;)a.push({type:7,index:n}),h+=C.length-1}n++}}static createElement(t,e){let i=H.createElement("template");return i.innerHTML=t,i}};function R(r,t,e=r,i){if(t===E)return t;let s=i!==void 0?e._$Co?.[i]:e._$Cl,n=G(t)?void 0:t._$litDirective$;return s?.constructor!==n&&(s?._$AO?.(!1),n===void 0?s=void 0:(s=new n(r),s._$AT(r,e,i)),i!==void 0?(e._$Co??=[])[i]=s:e._$Cl=s),s!==void 0&&(t=R(r,s._$AS(r,t.values),s,i)),t}var _t=class{constructor(t,e){this._$AV=[],this._$AN=void 0,this._$AD=t,this._$AM=e}get parentNode(){return this._$AM.parentNode}get _$AU(){return this._$AM._$AU}u(t){let{el:{content:e},parts:i}=this._$AD,s=(t?.creationScope??H).importNode(e,!0);k.currentNode=s;let n=k.nextNode(),o=0,l=0,a=i[0];for(;a!==void 0;){if(o===a.index){let d;a.type===2?d=new K(n,n.nextSibling,this,t):a.type===1?d=new a.ctor(n,a.name,a.strings,this,t):a.type===6&&(d=new xt(n,this,t)),this._$AV.push(d),a=i[++l]}o!==a?.index&&(n=k.nextNode(),o++)}return k.currentNode=H,s}p(t){let e=0;for(let i of this._$AV)i!==void 0&&(i.strings!==void 0?(i._$AI(t,i,e),e+=i.strings.length-2):i._$AI(t[e])),e++}},K=class r{get _$AU(){return this._$AM?._$AU??this._$Cv}constructor(t,e,i,s){this.type=2,this._$AH=p,this._$AN=void 0,this._$AA=t,this._$AB=e,this._$AM=i,this.options=s,this._$Cv=s?.isConnected??!0}get parentNode(){let t=this._$AA.parentNode,e=this._$AM;return e!==void 0&&t?.nodeType===11&&(t=e.parentNode),t}get startNode(){return this._$AA}get endNode(){return this._$AB}_$AI(t,e=this){t=R(this,t,e),G(t)?t===p||t==null||t===""?(this._$AH!==p&&this._$AR(),this._$AH=p):t!==this._$AH&&t!==E&&this._(t):t._$litType$!==void 0?this.$(t):t.nodeType!==void 0?this.T(t):ei(t)?this.k(t):this._(t)}O(t){return this._$AA.parentNode.insertBefore(t,this._$AB)}T(t){this._$AH!==t&&(this._$AR(),this._$AH=this.O(t))}_(t){this._$AH!==p&&G(this._$AH)?this._$AA.nextSibling.data=t:this.T(H.createTextNode(t)),this._$AH=t}$(t){let{values:e,_$litType$:i}=t,s=typeof i=="number"?this._$AC(t):(i.el===void 0&&(i.el=J.createElement(Gt(i.h,i.h[0]),this.options)),i);if(this._$AH?._$AD===s)this._$AH.p(e);else{let n=new _t(s,this),o=n.u(this.options);n.p(e),this.T(o),this._$AH=n}}_$AC(t){let e=Zt.get(t.strings);return e===void 0&&Zt.set(t.strings,e=new J(t)),e}k(t){St(this._$AH)||(this._$AH=[],this._$AR());let e=this._$AH,i,s=0;for(let n of t)s===e.length?e.push(i=new r(this.O(F()),this.O(F()),this,this.options)):i=e[s],i._$AI(n),s++;s<e.length&&(this._$AR(i&&i._$AB.nextSibling,s),e.length=s)}_$AR(t=this._$AA.nextSibling,e){for(this._$AP?.(!1,!0,e);t&&t!==this._$AB;){let i=t.nextSibling;t.remove(),t=i}}setConnected(t){this._$AM===void 0&&(this._$Cv=t,this._$AP?.(t))}},N=class{get tagName(){return this.element.tagName}get _$AU(){return this._$AM._$AU}constructor(t,e,i,s,n){this.type=1,this._$AH=p,this._$AN=void 0,this.element=t,this.name=e,this._$AM=s,this.options=n,i.length>2||i[0]!==""||i[1]!==""?(this._$AH=Array(i.length-1).fill(new String),this.strings=i):this._$AH=p}_$AI(t,e=this,i,s){let n=this.strings,o=!1;if(n===void 0)t=R(this,t,e,0),o=!G(t)||t!==this._$AH&&t!==E,o&&(this._$AH=t);else{let l=t,a,d;for(t=n[0],a=0;a<n.length-1;a++)d=R(this,l[i+a],e,a),d===E&&(d=this._$AH[a]),o||=!G(d)||d!==this._$AH[a],d===p?t=p:t!==p&&(t+=(d??"")+n[a+1]),this._$AH[a]=d}o&&!s&&this.j(t)}j(t){t===p?this.element.removeAttribute(this.name):this.element.setAttribute(this.name,t??"")}},yt=class extends N{constructor(){super(...arguments),this.type=3}j(t){this.element[this.name]=t===p?void 0:t}},bt=class extends N{constructor(){super(...arguments),this.type=4}j(t){this.element.toggleAttribute(this.name,!!t&&t!==p)}},$t=class extends N{constructor(t,e,i,s,n){super(t,e,i,s,n),this.type=5}_$AI(t,e=this){if((t=R(this,t,e,0)??p)===E)return;let i=this._$AH,s=t===p&&i!==p||t.capture!==i.capture||t.once!==i.once||t.passive!==i.passive,n=t!==p&&(i===p||s);s&&this.element.removeEventListener(this.name,this,i),n&&this.element.addEventListener(this.name,this,t),this._$AH=t}handleEvent(t){typeof this._$AH=="function"?this._$AH.call(this.options?.host??this.element,t):this._$AH.handleEvent(t)}},xt=class{constructor(t,e,i){this.element=t,this.type=6,this._$AN=void 0,this._$AM=e,this.options=i}get _$AU(){return this._$AM._$AU}_$AI(t){R(this,t)}};var ri=At.litHtmlPolyfillSupport;ri?.(J,K),(At.litHtmlVersions??=[]).push("3.2.1");var Jt=(r,t,e)=>{let i=e?.renderBefore??t,s=i._$litPart$;if(s===void 0){let n=e?.renderBefore??null;i._$litPart$=s=new K(t.insertBefore(F(),n),n,void 0,e??{})}return s._$AI(r),s};var $=class extends S{constructor(){super(...arguments),this.renderOptions={host:this},this._$Do=void 0}createRenderRoot(){let t=super.createRenderRoot();return this.renderOptions.renderBefore??=t.firstChild,t}update(t){let e=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(t),this._$Do=Jt(e,this.renderRoot,this.renderOptions)}connectedCallback(){super.connectedCallback(),this._$Do?.setConnected(!0)}disconnectedCallback(){super.disconnectedCallback(),this._$Do?.setConnected(!1)}render(){return E}};$._$litElement$=!0,$.finalized=!0,globalThis.litElementHydrateSupport?.({LitElement:$});var si=globalThis.litElementPolyfillSupport;si?.({LitElement:$});(globalThis.litElementVersions??=[]).push("4.1.1");var U=r=>(t,e)=>{e!==void 0?e.addInitializer(()=>{customElements.define(r,t)}):customElements.define(r,t)};var ni={attribute:!0,type:String,converter:B,reflect:!1,hasChanged:ot},oi=(r=ni,t,e)=>{let{kind:i,metadata:s}=e,n=globalThis.litPropertyMetadata.get(s);if(n===void 0&&globalThis.litPropertyMetadata.set(s,n=new Map),n.set(e.name,r),i==="accessor"){let{name:o}=e;return{set(l){let a=t.get.call(this);t.set.call(this,l),this.requestUpdate(o,a,r)},init(l){return l!==void 0&&this.P(o,void 0,r),l}}}if(i==="setter"){let{name:o}=e;return function(l){let a=this[o];t.call(this,l),this.requestUpdate(o,a,r)}}throw Error("Unsupported decorator location: "+i)};function u(r){return(t,e)=>typeof e=="object"?oi(r,t,e):((i,s,n)=>{let o=s.hasOwnProperty(n);return s.constructor.createProperty(n,o?{...i,wrapped:!0}:i),o?Object.getOwnPropertyDescriptor(s,n):void 0})(r,t,e)}function X(r){return u({...r,state:!0,attribute:!1})}var V=(r,t,e)=>(e.configurable=!0,e.enumerable=!0,Reflect.decorate&&typeof t!="object"&&Object.defineProperty(r,t,e),e);var ai;function Kt(r){return(t,e)=>V(t,e,{get(){return(this.renderRoot??(ai??=document.createDocumentFragment())).querySelectorAll(r)}})}var Xt={ATTRIBUTE:1,CHILD:2,PROPERTY:3,BOOLEAN_ATTRIBUTE:4,EVENT:5,ELEMENT:6},Yt=r=>(...t)=>({_$litDirective$:r,values:t}),lt=class{constructor(t){}get _$AU(){return this._$AM._$AU}_$AT(t,e,i){this._$Ct=t,this._$AM=e,this._$Ci=i}_$AS(t,e){return this.update(t,e)}update(t,e){return this.render(...e)}};var Y=class extends lt{constructor(t){if(super(t),this.it=p,t.type!==Xt.CHILD)throw Error(this.constructor.directiveName+"() can only be used in child bindings")}render(t){if(t===p||t==null)return this._t=void 0,this.it=t;if(t===E)return t;if(typeof t!="string")throw Error(this.constructor.directiveName+"() called with a non-string value");if(t===this.it)return this._t;this.it=t;let e=[t];return e.raw=e,this._t={_$litType$:this.constructor.resultType,strings:e,values:[]}}};Y.directiveName="unsafeHTML",Y.resultType=1;var z=Yt(Y);var ht='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M12.2,12.2V87.8H87.8V12.2ZM83,17V29.9H17V17ZM17,83V34.7H83V83Z"/><rect x="73.49" y="20.53" width="6.04" height="6.04"/><rect x="64.33" y="20.53" width="6.04" height="6.04"/><path d="M59.92,47.34a1.61,1.61,0,0,0,0,2.26l7.82,7.82-7.82,7.81a1.61,1.61,0,0,0,0,2.26,1.57,1.57,0,0,0,1.13.47,1.6,1.6,0,0,0,1.14-.47L72.26,57.42,62.19,47.34A1.62,1.62,0,0,0,59.92,47.34Z"/><path d="M40.08,47.34a1.62,1.62,0,0,0-2.27,0L27.74,57.42,37.81,67.49A1.6,1.6,0,0,0,39,68a1.56,1.56,0,0,0,1.13-.47,1.61,1.61,0,0,0,0-2.26l-7.82-7.81,7.82-7.82A1.61,1.61,0,0,0,40.08,47.34Z"/><path d="M53.55,47.38a1.61,1.61,0,0,0-2.06.94L45.21,65.4a1.61,1.61,0,0,0,.94,2.06,1.58,1.58,0,0,0,.56.1,1.6,1.6,0,0,0,1.5-1L54.5,49.43A1.6,1.6,0,0,0,53.55,47.38Z"/></svg>';var j='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="28.82" cy="16.34" r="4.6"/><path d="M98,17.22H40.41a6.79,6.79,0,0,0,.07-.88,11.66,11.66,0,1,0-23.32,0,6.73,6.73,0,0,0,.06.88H2V88.67H25.91l1,6.65,6.17-6.65H98ZM28.82,7.88a8.46,8.46,0,0,1,8.46,8.46c0,3.57-5.29,11.71-8.46,16.15-3.17-4.44-8.46-12.58-8.46-16.15A8.46,8.46,0,0,1,28.82,7.88Zm3.82,24.74C35.27,33.33,37,34.71,37,36c0,1.83-3.49,3.88-8.16,3.88s-8.16-2-8.16-3.88c0-1.28,1.71-2.66,4.34-3.37,1.25,1.81,2.24,3.13,2.54,3.54l1.28,1.68,1.28-1.68C30.4,35.75,31.39,34.43,32.64,32.62ZM28.17,82.09l-4.88-3.87,19.16-4.37L29.09,88.26Zm65.05,1.78H37.52L51.85,68.43,16.08,76.58l9.11,7.23v.06H6.78V22h12a61.44,61.44,0,0,0,4.39,7.82c-3.42,1.21-5.69,3.48-5.69,6.15,0,4,5,7.08,11.36,7.08S40.18,40,40.18,36c0-2.67-2.27-4.94-5.69-6.15A62.69,62.69,0,0,0,38.88,22H93.22Z"/><path d="M30.83,65.59H26.6V57.43c0-1,0-1.9.06-2.72a8.11,8.11,0,0,1-1,1l-1.75,1.45-2.16-2.66,5.29-4.31h3.76Z"/><path d="M80.19,41.54H69V38.8L72.76,35c1.08-1.13,1.78-1.89,2.11-2.31a4.91,4.91,0,0,0,.69-1.06,2.24,2.24,0,0,0,.2-.92,1.21,1.21,0,0,0-.39-.95,1.61,1.61,0,0,0-1.1-.35,2.94,2.94,0,0,0-1.47.42A9.66,9.66,0,0,0,71.12,31l-2.29-2.67a12.56,12.56,0,0,1,2-1.53,7.26,7.26,0,0,1,1.77-.7,8.75,8.75,0,0,1,2.14-.24,6.54,6.54,0,0,1,2.68.52,4.25,4.25,0,0,1,1.82,1.51,3.89,3.89,0,0,1,.65,2.19,5.91,5.91,0,0,1-.23,1.67,5.76,5.76,0,0,1-.7,1.52,10,10,0,0,1-1.26,1.56q-.78.83-3.35,3.12v.11h5.84Z"/><path d="M84.09,63.11a3.66,3.66,0,0,1-.87,2.46,5,5,0,0,1-2.54,1.5v.06c2.58.32,3.86,1.54,3.86,3.65a4,4,0,0,1-1.7,3.39,7.94,7.94,0,0,1-4.73,1.23,13.67,13.67,0,0,1-2.3-.18,12.67,12.67,0,0,1-2.3-.65V71.11a9.17,9.17,0,0,0,2.06.76,8.26,8.26,0,0,0,1.94.25,3.93,3.93,0,0,0,2-.39,1.3,1.3,0,0,0,.63-1.2,1.35,1.35,0,0,0-.33-1A2.14,2.14,0,0,0,78.75,69a8.71,8.71,0,0,0-1.89-.17h-1V65.73h1c2.11,0,3.17-.54,3.17-1.63a1,1,0,0,0-.47-.9,2.3,2.3,0,0,0-1.26-.3,5.8,5.8,0,0,0-3.06,1l-1.73-2.78A8.24,8.24,0,0,1,76,59.91,10.85,10.85,0,0,1,79,59.55a6.43,6.43,0,0,1,3.75,1A3,3,0,0,1,84.09,63.11Z"/><path d="M63.16,70.12a10.22,10.22,0,0,1-5.87-1.78l1.82-2.63a7.3,7.3,0,0,0,5,1.16l.34,3.18A12.1,12.1,0,0,1,63.16,70.12Zm-8.53-4.53a8.51,8.51,0,0,1-1-2.2,11.19,11.19,0,0,1-.41-3,9.45,9.45,0,0,1,.24-2.2l3.12.72a6,6,0,0,0-.16,1.48,8.11,8.11,0,0,0,.29,2.14,5.1,5.1,0,0,0,.63,1.35ZM35.13,57c-.27,0-.54,0-.8,0l-.06-3.21a10.12,10.12,0,0,0,5.5-1.37l1.61,2.77A12.49,12.49,0,0,1,35.13,57Zm22.51-.25-2.46-2.05a11.61,11.61,0,0,1,6.35-3.61l.77,3.11A8.55,8.55,0,0,0,57.64,56.71Zm7.59-3-.29-3.19a34.85,34.85,0,0,1,3.77-.12h.22a8.57,8.57,0,0,0,2.15-.25l.79,3.11a12,12,0,0,1-2.94.34h-.28A30.94,30.94,0,0,0,65.23,53.69Zm-21-.69-2.21-2.31A5.91,5.91,0,0,0,43.91,46l3.2.11A9.08,9.08,0,0,1,44.26,53Zm31.06-1.43L73.37,49a8.43,8.43,0,0,0,2.17-2.64l.19-.34c.28-.51.58-1.06.87-1.65l2.87,1.41c-.31.63-.64,1.23-.94,1.78l-.19.36A11.62,11.62,0,0,1,75.32,51.57ZM47.19,43,44,42.76a9.87,9.87,0,0,1,3.12-6.68l2.23,2.29A6.68,6.68,0,0,0,47.19,43Zm4.43-6.39-1.72-2.7a30.13,30.13,0,0,1,3.48-1.87,22.94,22.94,0,0,1,2.68-1l1,3a18.65,18.65,0,0,0-2.32.9A26.63,26.63,0,0,0,51.62,36.62ZM60,33.33l-.59-3.14a28.15,28.15,0,0,1,6.78-.45L66,32.93A25.34,25.34,0,0,0,60,33.33Z"/><path d="M68.14,69.17l-1.09-3a20.32,20.32,0,0,0,2.67-1.24l1.55,2.8A22.22,22.22,0,0,1,68.14,69.17Z"/></svg>';var Qt='data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.1" id="Layer_1" x="0px" y="0px" viewBox="0 0 100 100" style="enable-background:new 0 0 100 100;" xml:space="preserve"><path d="M80.8,34.7V30v-2.1L76,23.1L69.9,17l-4.8-4.8H63h-4.7l-0.1-0.1v0.1h-39v75.6h61.6V34.7L80.8,34.7L80.8,34.7z M76,83H24V17  h34.2v17.8H76V83z M76,30H63V17v-0.1l0.1,0.1L76,30L76,30L76,30z"/><path d="M35.1,59.7c-1.2,0-2.2-0.5-3.1-1.5c-1.5-1.7-1.6-3.9-0.1-5.7c1.2-1.5,3.5-2.8,6.7-3.9c1.2-0.3,2.4-0.8,3.7-1  c0.8-1.5,1.6-3.1,2.3-4.8c0.7-1.6,1.3-3.1,1.7-4.6c-1.8-3.1-3.6-6.8-3.5-9.7c0-2.5,1.5-4.2,3.9-4.5c2.2-0.2,3.8,0.8,4.5,2.9  c0.9,2.8,0,7-1,10.7c0.9,1.5,2,3,3.2,4.5c1,1.3,2,2.4,3,3.6c4,0,7.9,0.3,10.2,1.6c2.1,1.2,2.9,2.9,2.3,5c-0.7,2.3-2.5,3.5-4.8,3.1  c-3-0.3-6.5-3.2-9.1-6c-1.8,0.1-3.9,0.2-6,0.6c-1.5,0.2-3,0.5-4.4,0.8c-0.5,0.9-1,1.7-1.5,2.4c-2,2.9-3.8,4.8-5.5,5.9  C36.8,59.5,36,59.7,35.1,59.7L35.1,59.7z M39.6,52.1c-2.4,0.8-4.3,1.8-5.1,2.8c-0.3,0.3-0.3,0.6,0.1,1.2c0.2,0.2,0.3,0.5,1.3,0  C37,55.3,38.4,54,39.6,52.1L39.6,52.1z M60.2,49.5c1.7,1.5,3.2,2.4,4.4,2.5c0.5,0.1,0.7,0.1,1-0.7c0.1-0.2,0.1-0.3-0.6-0.8  C64,50,62.3,49.6,60.2,49.5L60.2,49.5z M48.9,41.9c-0.3,0.9-0.7,1.7-0.9,2.3c-0.3,0.9-0.8,1.7-1.2,2.5c0.6-0.1,1.2-0.2,1.7-0.2  c1.2-0.1,2.3-0.3,3.5-0.3c-0.6-0.7-1-1.3-1.3-1.6C50.3,43.9,49.6,43,48.9,41.9L48.9,41.9z M47.3,27.5c-0.5,0.1-0.8,0.1-0.8,1  c0,1.2,0.5,2.8,1.4,4.7c0.5-2.3,0.6-4.2,0.2-5.2C47.8,27.5,47.7,27.5,47.3,27.5L47.3,27.5z"/><g><path d="M35.9,67.1c0-0.9,0.6-1.3,1.3-1.3h3.3c2.1,0,4.1,1.3,4.1,4c0,2.3-1.4,4-4,4h-2v2.5c0,0.9-0.6,1.4-1.3,1.4   c-0.8,0-1.3-0.5-1.3-1.4L35.9,67.1L35.9,67.1z M38.6,71.4H40c1.1,0,1.8-0.8,1.8-1.6c0-1.1-0.7-1.6-1.8-1.6h-1.4V71.4z"/><path d="M45.9,67.1c0-0.9,0.6-1.3,1.3-1.3h3.2c3.5,0,5.5,2.8,5.5,5.8c0,3.9-2.6,6-5.5,6h-3.2c-0.7,0-1.3-0.5-1.3-1.3V67.1z    M48.6,75.1H50c2.1,0,3.1-1.4,3.1-3.5c0-2-1-3.3-3-3.3h-1.5V75.1z"/><path d="M57.5,67.1c0-0.9,0.6-1.3,1.3-1.3h4.1c0.8,0,1.3,0.5,1.3,1.2c0,0.8-0.5,1.2-1.3,1.2H60v2.1h2.4c0.8,0,1.3,0.5,1.3,1.2   c0,0.8-0.5,1.2-1.3,1.2H60V76c0,0.9-0.6,1.4-1.3,1.4c-0.8,0-1.3-0.5-1.3-1.4L57.5,67.1z"/></g></svg>';var te='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M61.05,69.88H55.54a13.46,13.46,0,0,1,4.87-10.37,15.61,15.61,0,1,0-19.93,0,13.46,13.46,0,0,1,4.87,10.37H39.84A7.94,7.94,0,0,0,37,63.75a21.11,21.11,0,1,1,27,0A7.94,7.94,0,0,0,61.05,69.88Z"/><path d="M60.71,75H40.18a1.84,1.84,0,0,1,0-3.67H60.71a1.84,1.84,0,0,1,0,3.67Z"/><path d="M60.71,79.93H40.18a1.84,1.84,0,0,1,0-3.67H60.71a1.84,1.84,0,0,1,0,3.67Z"/><path d="M60.71,84.87H40.18a1.84,1.84,0,0,1,0-3.67H60.71a1.84,1.84,0,0,1,0,3.67Z"/><path d="M56.72,89.82H44.17a1.84,1.84,0,1,1,0-3.67H56.72a1.84,1.84,0,1,1,0,3.67Z"/><polygon points="50.53 54.3 56.18 51.04 56.18 44.51 50.53 47.77 50.53 54.3"/><polygon points="51.07 54.61 56.18 57.56 56.18 51.66 51.07 54.61"/><polygon points="55.87 43.35 55.87 37.45 50.76 40.4 55.87 43.35"/><polygon points="50.22 40.72 44.57 43.98 50.22 47.24 55.87 43.98 50.22 40.72"/><polygon points="49.68 40.4 44.57 37.45 44.57 43.35 49.68 40.4"/><polygon points="56.72 44.83 56.72 50.73 61.83 47.77 56.72 44.83"/><polygon points="49.91 54.3 49.91 47.77 44.26 44.51 44.26 44.51 44.26 51.04 44.26 51.04 44.26 51.04 49.91 54.3"/><polygon points="43.72 44.83 38.61 47.77 43.72 50.73 43.72 44.83"/><polygon points="44.26 51.66 44.26 57.56 49.37 54.61 44.26 51.66"/><polygon points="24.4 47.66 12.48 52.76 12.48 42.56 24.4 47.66"/><polygon points="31.92 29.57 19.89 24.75 27.1 17.54 31.92 29.57"/><polygon points="50.04 22.1 44.94 10.18 55.14 10.18 50.04 22.1"/><polygon points="68.13 29.63 72.95 17.59 80.16 24.8 68.13 29.63"/><polygon points="75.6 47.74 87.52 42.64 87.52 52.84 75.6 47.74"/><polygon points="68.08 65.83 80.11 70.66 72.9 77.86 68.08 65.83"/><polygon points="31.87 65.78 27.05 77.81 19.84 70.6 31.87 65.78"/></svg>';var ee='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M50.09,25.2h-17a5,5,0,0,0-5,5v1.2a5,5,0,0,0,5,5h17a5,5,0,0,0,5-5V30.2A5,5,0,0,0,50.09,25.2Zm1.8,6.2a1.81,1.81,0,0,1-1.8,1.8h-17a1.8,1.8,0,0,1-1.8-1.8V30.2a1.8,1.8,0,0,1,1.8-1.8h17a1.81,1.81,0,0,1,1.8,1.8Z"/><path d="M52.49,44.88H29.89a1.6,1.6,0,0,0,0,3.2h22.6a1.6,1.6,0,1,0,0-3.2Z"/><path d="M52.49,39.47H29.89a1.6,1.6,0,0,0,0,3.2h22.6a1.6,1.6,0,0,0,0-3.2Z"/><path d="M52.49,50.29H29.89a1.6,1.6,0,0,0,0,3.2h22.6a1.6,1.6,0,1,0,0-3.2Z"/><path d="M73.3,30.68a16.42,16.42,0,0,0-3.81.46V23.4a9.21,9.21,0,0,0-9.2-9.2H22.89a12.61,12.61,0,0,0-12.6,12.6V73.2a12.61,12.61,0,0,0,12.6,12.6H64.2l.46-.21c1.14-.5,4.83-2.4,4.83-5.59V63a15.9,15.9,0,0,0,3.81.47,16.42,16.42,0,0,0,0-32.83ZM64.69,69.73H22.89a3.85,3.85,0,0,0,0,7.65h41.8v2.44A5.54,5.54,0,0,1,63.12,81H22.89a7.8,7.8,0,0,1-7.8-7.8V26.8a7.8,7.8,0,0,1,7.8-7.8h37.4a4.41,4.41,0,0,1,4.4,4.4v9.74a16.36,16.36,0,0,0,0,27.9Zm8.61-9.42A13.22,13.22,0,1,1,86.51,47.09,13.23,13.23,0,0,1,73.3,60.31Z"/><path d="M79.63,41.34,71.2,49.77l-3-3a1.59,1.59,0,0,0-2.26,0A1.61,1.61,0,0,0,66,49l5.25,5.25L81.89,43.6a1.59,1.59,0,0,0,0-2.26A1.61,1.61,0,0,0,79.63,41.34Z"/></svg>';var ie='data:image/svg+xml,<svg id="Layer_1" data-name="Layer 1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024"><defs><style>.cls-1{fill:%234695eb;}.cls-2{fill:%23ff004a;}.cls-3{fill:%23091313;}</style></defs><title>quarkus_icon_rgb_1024px_default</title><polygon class="cls-1" points="669.34 180.57 512 271.41 669.34 362.25 669.34 180.57"/><polygon class="cls-2" points="354.66 180.57 354.66 362.25 512 271.41 354.66 180.57"/><polygon class="cls-3" points="669.34 362.25 512 271.41 354.66 362.25 512 453.09 669.34 362.25"/><polygon class="cls-1" points="188.76 467.93 346.1 558.76 346.1 377.09 188.76 467.93"/><polygon class="cls-2" points="346.1 740.44 503.43 649.6 346.1 558.76 346.1 740.44"/><polygon class="cls-3" points="346.1 377.09 346.1 558.76 503.43 649.6 503.43 467.93 346.1 377.09"/><polygon class="cls-1" points="677.9 740.44 677.9 558.76 520.57 649.6 677.9 740.44"/><polygon class="cls-2" points="835.24 467.93 677.9 377.09 677.9 558.76 835.24 467.93"/><polygon class="cls-3" points="520.57 649.6 677.9 558.76 677.9 377.09 520.57 467.93 520.57 649.6"/><path class="cls-1" d="M853.47,1H170.53C77.29,1,1,77.29,1,170.53V853.47C1,946.71,77.29,1023,170.53,1023h467.7L512,716.39,420.42,910H170.53C139.9,910,114,884.1,114,853.47V170.53C114,139.9,139.9,114,170.53,114H853.47C884.1,114,910,139.9,910,170.53V853.47C910,884.1,884.1,910,853.47,910H705.28l46.52,113H853.47c93.24,0,169.53-76.29,169.53-169.53V170.53C1023,77.29,946.71,1,853.47,1Z"/></svg>';var re='data:image/svg+xml,<svg id="artwork" xmlns="http://www.w3.org/2000/svg" width="100%" height="auto" viewBox="0 0 1024 140"><defs><style>.cls-1{fill:%23ff004a} .cls-2{fill:%234695eb} .cls-3{fill:var(--text-color)}</style></defs><path fill="%23ff004a" d="M33.23 17.54v23.59a5.9 5.9 0 008.85 5.11l20.44-11.79a5.9 5.9 0 000-10.22l-20.44-11.8a5.9 5.9 0 00-8.85 5.11z"/><path fill="%234695eb" d="M109.5 17.54v23.59a5.9 5.9 0 01-8.85 5.11L80.22 34.45a5.9 5.9 0 010-10.22l20.43-11.8a5.9 5.9 0 018.85 5.11z"/><path fill="%23ff004a" d="M42.08 134.31l20.44-11.8a5.9 5.9 0 000-10.22L42.08 100.5a5.9 5.9 0 00-8.85 5.11v23.59a5.9 5.9 0 008.85 5.11z"/><path fill="%234695eb" d="M4 68.26l20.43-11.8a5.9 5.9 0 018.85 5.11v23.6a5.9 5.9 0 01-8.85 5.11L4 78.48a5.9 5.9 0 010-10.22z"/><path fill="%23ff004a" d="M138.79 68.26l-20.44-11.8a5.9 5.9 0 00-8.85 5.11v23.6a5.9 5.9 0 008.85 5.11l20.44-11.8a5.9 5.9 0 000-10.22z"/><path fill="%234695eb" d="M100.65 134.31l-20.43-11.8a5.9 5.9 0 010-10.22l20.43-11.79a5.89 5.89 0 018.85 5.11v23.59a5.9 5.9 0 01-8.85 5.11z"/><path fill="var(--main-text-color)" d="M45.35 61.29h-.05A3.14 3.14 0 0040.62 64v18.09a16.64 16.64 0 008.32 14.42l15.65 9a3.15 3.15 0 004.69-2.71V84.74A16.66 16.66 0 0061 70.32zm28.06 41.52a3.15 3.15 0 004.69 2.71h.05l15.65-9a16.64 16.64 0 008.32-14.42V64a3.15 3.15 0 00-4.69-2.71l-15.65 9a16.64 16.64 0 00-8.33 14.42zm22.23-45.32a3.16 3.16 0 000-5.43L80 43a16.62 16.62 0 00-16.65 0l-15.66 9a3.16 3.16 0 000 5.43l15.65 9a16.67 16.67 0 0016.65 0zm180.81 6.32q0 15.85-5.62 26.58a34.33 34.33 0 01-16.5 15.71L276 130.25h-24.4l-16.47-20.16h-1.41q-20.66 0-31.72-12t-11.1-34.4q0-22.44 11.09-34.26t31.81-11.84q20.72 0 31.68 11.93t10.97 34.29zm-65.58 0q0 15.06 5.72 22.68t17.09 7.62q22.8 0 22.8-30.3T233.8 33.45q-11.37 0-17.15 7.65t-5.78 22.71zM365.69 19v58.15a33.48 33.48 0 01-4.45 17.45 29.07 29.07 0 01-12.88 11.5q-8.42 4-19.91 4-17.34 0-26.92-8.88t-9.59-24.32V19h19v55q0 10.39 4.18 15.25t13.83 4.85q9.35 0 13.55-4.88t4.21-15.34V19zm74.5 89.86l-6.52-21.39h-32.76l-6.51 21.39h-20.53l31.71-90.22h23.3l31.83 90.22zm-11.07-37.37q-9-29.07-10.17-32.88c-.76-2.54-1.3-4.54-1.63-6q-2 7.88-11.61 38.9zm59.44 2.89v34.48H469.5V19h26.19q18.31 0 27.1 6.67t8.79 20.25A23.9 23.9 0 01527.22 60a28.82 28.82 0 01-12.36 9.68q20.28 30.3 26.43 39.15h-21.14L498.7 74.38zm0-15.48h6.14q9 0 13.34-3t4.3-9.46q0-6.39-4.39-9.1t-13.62-2.7h-5.77zm133.8 49.96h-21.64L577.18 71l-8 5.78v32.08h-19V19h19v41.12l7.5-10.57L601 19h21.14l-31.37 39.77zm8.79 0V19h19.05v89.85zM721.56 19h19.24l-30.55 89.85h-20.77L659 19h19.24l16.9 53.47q1.41 4.72 2.92 11c1 4.2 1.63 7.12 1.87 8.76q.69-5.65 4.61-19.79zm79.78 89.86h-51.75V19h51.75v15.62h-32.7v19.73h30.42V70h-30.42v23.13h32.7zm35.28-34.48v34.48h-19.06V19h26.19q18.32 0 27.1 6.67t8.79 20.25A23.9 23.9 0 01875.28 60a28.82 28.82 0 01-12.36 9.68q20.28 30.3 26.43 39.15h-21.14l-21.45-34.45zm0-15.48h6.14q9 0 13.34-3t4.3-9.46q0-6.39-4.39-9.1t-13.62-2.7h-5.77zm114.56 25.01q0 12.16-8.76 19.17t-24.37 7q-14.37 0-25.44-5.41V87A104.49 104.49 0 00908 92.7a45.91 45.91 0 0011.53 1.66q6.27 0 9.62-2.4a8.2 8.2 0 003.35-7.13 7.82 7.82 0 00-1.48-4.7 16.55 16.55 0 00-4.33-4A112.53 112.53 0 00915 70.08a54.87 54.87 0 01-12.35-7.44 27.82 27.82 0 01-6.58-8.29 23.68 23.68 0 01-2.46-11.07q0-11.91 8.09-18.74t22.34-6.82a52.53 52.53 0 0113.36 1.66 90.93 90.93 0 0113.31 4.67l-6.11 14.81a89.25 89.25 0 00-11.89-4.12 38.2 38.2 0 00-9.25-1.17q-5.4 0-8.3 2.52a8.34 8.34 0 00-2.88 6.58 8.19 8.19 0 001.16 4.39 12.9 12.9 0 003.72 3.63A114.3 114.3 0 00929.24 57q12.6 6 17.27 12.08a23.57 23.57 0 014.67 14.83zm65.52 24.95H965V19h51.75v15.62H984v19.73h30.43V70H984v23.13h32.7z"/></svg>%0A';var se='data:image/svg+xml,<svg width="57" height="57" viewBox="0 0 57 57" xmlns="http://www.w3.org/2000/svg" stroke="%234695EB">%0A    <g fill="none" fill-rule="evenodd">%0A        <g transform="translate(1 1)" stroke-width="2">%0A            <circle cx="5" cy="50" r="5">%0A                <animate attributeName="cy"%0A                     begin="0s" dur="2.2s"%0A                     values="50;5;50;50"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A                <animate attributeName="cx"%0A                     begin="0s" dur="2.2s"%0A                     values="5;27;49;5"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A            </circle>%0A            <circle cx="27" cy="5" r="5">%0A                <animate attributeName="cy"%0A                     begin="0s" dur="2.2s"%0A                     from="5" to="5"%0A                     values="5;50;50;5"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A                <animate attributeName="cx"%0A                     begin="0s" dur="2.2s"%0A                     from="27" to="27"%0A                     values="27;49;5;27"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A            </circle>%0A            <circle cx="49" cy="50" r="5">%0A                <animate attributeName="cy"%0A                     begin="0s" dur="2.2s"%0A                     values="50;50;5;50"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A                <animate attributeName="cx"%0A                     from="49" to="49"%0A                     begin="0s" dur="2.2s"%0A                     values="49;5;27;49"%0A                     calcMode="linear"%0A                     repeatCount="indefinite" />%0A            </circle>%0A        </g>%0A    </g>%0A</svg>';var fi={docs:{tutorial:ht,tutorials:ht,guide:j,guides:j,howto:j,pdf:Qt,concepts:te,reference:ee},origins:{quarkus:ie,quarkiverse:re},loading:se},M=fi;var _=class extends ${constructor(){super(...arguments);this.type="default";this.origin="quarkus";this.originsWithRelativeUrls=[]}connectedCallback(){if(this.data)for(let e in this.data)this.data.hasOwnProperty(e)&&(this[e]=this.data[e]);super.connectedCallback()}disconnectedCallback(){super.disconnectedCallback()}render(){return f`
      <div class="qs-hit qs-guide" aria-label="Guide Hit">
        <div class="qs-guide-icon">
          ${z(this.icon())}
        </div>
        <div>
          <h4>
            <a href="${this.relativizeUrl()}" target="_blank">${this._renderHTML(this.title)}</a>
            ${this.origin&&this.origin.toLowerCase()!=="quarkus"?f`<a href="${this._originLink()}" target="_blank" class="origin" title="${this._originTitle()}">${z(this._originIcon())}</a>`:""}
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
    `}relativizeUrl(){if(this.originsWithRelativeUrls.includes(this.origin)&&!this.url.startsWith("/"))try{return this.url.substring(new URL(this.url).origin.length)}catch{return this.url}else return this.url}icon(){let e=M.docs[this.type];return this._iconToSvg(e)}_renderHTML(e){return e&&(Array.isArray(e)?e.map(i=>f`<p>${this._renderHTML(i)}</p>`):z(e))}_originTitle(){return this.origin==="quarkiverse-hub"?"Quarkus extension project contributed by the community":this.origin}_originLink(){return this.origin==="quarkiverse-hub"?"https://github.com/quarkiverse":"#"}_originIcon(){let e=M.origins[this.origin==="quarkiverse-hub"?"quarkiverse":this.origin];return console.log(e),this._iconToSvg(e)}_iconToSvg(e){if(e){let i=e.match(/.*(<svg.*<\/svg>)/);if(i)return i[1].replaceAll("%23","#")}return""}};_.styles=L`
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
          color: var(--link-color, #1259A5);
      }

      .qs-guide a:hover {
          color: var(--link-color-hover, #c00);
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
          background-size: 100px 25px;
          background-repeat: no-repeat;
          background-position: center;
          width: 100px;
          height: 30px;
          display: block;
          vertical-align: middle;
      }

      .qs-guide .origin.quarkus {
          background-image: url('${q(M.origins.quarkus)}');
      }

      .qs-guide .origin.quarkiverse-hub {
          background-image: url('${q(M.origins.quarkiverse)}');
      }
    
      .qs-guide-icon svg {
        width: 70px;
        margin: 1rem 0 0 0;
        fill: var(--main-text-color);
      }

      .summary {
          min-height: 40px;
      }
  `,c([u({type:Object})],_.prototype,"data",2),c([u({type:String})],_.prototype,"type",2),c([u({type:String})],_.prototype,"url",2),c([u({type:String})],_.prototype,"title",2),c([u({type:String})],_.prototype,"summary",2),c([u({type:String})],_.prototype,"keywords",2),c([u({type:String})],_.prototype,"content",2),c([u({type:String})],_.prototype,"origin",2),c([u({type:String,attribute:"origins-with-relative-urls"})],_.prototype,"originsWithRelativeUrls",2),_=c([U("qs-guide")],_);var Re=Ot(Tt());var tt=class r{static{this.guides=null}static queryDocumentGuides(t="qs-target qs-guide"){let e=document.querySelectorAll(t),i=e?[]:null;for(let s=0;s<e.length;s++){let n=e[s];i.push({title:n.getAttribute("title"),categories:n.getAttribute("categories"),type:n.getAttribute("type"),url:n.getAttribute("url"),summary:n.getAttribute("summary"),keywords:n.getAttribute("keywords"),content:n.getAttribute("content"),origin:n.getAttribute("origin")})}return i}static enableLocalSearch(t){r.guides=r.queryDocumentGuides(t),r.guides!=null&&console.debug("LocalSearch is ready with "+r.guides.length+" guides found.")}static search(t){let e=r.guides;if(e==null)return null;let i=[];t.q&&i.push(...t.q.split(" ").map(n=>n.trim()));let s=[];return t.categories&&(Array.isArray(t.categories)?s.push(...t.categories):s.push(t.categories)),e.filter(n=>{let o=!0;return o&&s.length>0&&(o=r.containsAllCaseInsensitive(n.categories,s)),o&&i.length>0&&(o=r.containsAllCaseInsensitive(`${n.keywords}${n.summary}${n.title}${n.categories}`,i)),o})}static containsAllCaseInsensitive(t,e){let i=(t||"").toLowerCase();for(let s in e)if(i.indexOf(e[s].toLowerCase())<0)return!1;return!0}};var dt="qs-start",P="qs-result",ct="qs-next-page",ut="qs-query-suggestion",y=class extends ${constructor(){super();this.server="";this.minChars=3;this.initialTimeout=1500;this.moreTimeout=2500;this.language="en";this.localSearch=!1;this.originFilter="";this._page=0;this._currentHitCount=0;this._abortController=null;this._search=()=>{if(this._abortController&&this._abortController.abort(),!this._backendData){this._clearSearch();return}let e=new AbortController;if(this._abortController=e,this.dispatchEvent(new CustomEvent(dt,{detail:{page:this._page}})),this.localSearch){this._localSearch(),this._abortController==e&&(this._abortController=null);return}this._jsonFetch(e,"GET",this._backendData,this._page>0?this.initialTimeout:this.moreTimeout).then(i=>{this._page>0?this._currentHitCount+=i.hits.length:this._currentHitCount=i.hits.length;let s=i.total?.lowerBound,n=i.hits.length>0&&s>this._currentHitCount;this.dispatchEvent(new CustomEvent(P,{detail:{...i,search:this._backendData,page:this._page,hasMoreHits:n}}))}).catch(i=>{console.error("Could not run search: "+i),this._abortController==e&&(this._page=0,this._currentHitCount=0,this._localSearch())}).finally(()=>{this._abortController==e&&(this._abortController=null)})};this._searchDebounced=(0,Re.default)(this._search,300);this._handleInputChange=e=>{let i=this._getFormElements(),s={language:this.language,contentSnippets:2,contentSnippetsLength:120},n={};this.quarkusversion&&(s.version=this.quarkusversion),this.originFilter&&(s.origin=this.originFilter);var o=0;for(let l of i)this._isInput(l)&&(l.value.length===0||l.value.length<this.minChars)||l.value&&l.value.length>0&&l.name.length>0&&(s[l.name]=l.value,n[l.name]=l.value,o++);o==0?(this._backendData=null,this._browserData=null):(this._backendData=s,this._browserData=n)};this._handleNextPage=e=>{this._page++,this._search()};this._handleQuerySuggestion=e=>{this._getFormElements().forEach(i=>{this._isInput(i)&&i.name==="q"&&(i.value=e.detail.suggestion.query)}),this._handleInputChange(e)};let e=new URLSearchParams(window.location.hash.substring(1));if(e.size>0){this._initialQueryStringPresent=!0;let i=this._getFormElements();for(let s of i){let n=e.get(s.name);n&&(s.value=n)}}}render(){return f`
      <div id="qs-form">
        <slot></slot>
      </div>
    `}update(e){return this._initialQueryStringPresent&&(this._initialQueryStringPresent=!1,this._handleInputChange(null)),window.location.hash=this._browserData?new URLSearchParams(this._browserData).toString():"",this._backendData?this._searchDebounced():this._clearSearch(),super.update(e)}connectedCallback(){super.connectedCallback(),tt.enableLocalSearch();let e=this._getFormElements();this.addEventListener(ct,this._handleNextPage),this.addEventListener(ut,this._handleQuerySuggestion),e.forEach(i=>{let s=this._isInput(i)?"input":"change";i.addEventListener(s,this._handleInputChange)}),this._handleInputChange(null)}disconnectedCallback(){super.disconnectedCallback(),this.removeEventListener(ct,this._handleNextPage),this.removeEventListener(ut,this._handleQuerySuggestion),this._getFormElements().forEach(i=>{let s=this._isInput(i)?"input":"change";i.removeEventListener(s,this._handleInputChange)})}_getFormElements(){return this.querySelectorAll("input[name], select[name]")}_isInput(e){return e.tagName.toLowerCase()==="input"}async _jsonFetch(e,i,s,n){let o={...s,page:this._page.toString()},l=setTimeout(()=>e.abort(),n),a=await fetch(this.server+"/api/guides/search?"+new URLSearchParams(o).toString(),{method:i,signal:e.signal,body:null});if(clearTimeout(l),a.ok)return await a.json();throw"Response status is "+a.status+"; response: "+await a.text()}_clearSearch(){this._page=0,this._currentHitCount=0,this._abortController&&(this._abortController.abort(),this._abortController=null),this.dispatchEvent(new CustomEvent(P))}_localSearch(){let e=this._backendData,i=tt.search(e);if(i){let s={hits:i,total:i.length};this.dispatchEvent(new CustomEvent(P,{detail:{...s,search:e,page:0,hasMoreHits:!1}}));return}this.dispatchEvent(new CustomEvent(P))}};y.styles=L`

      ::slotted(*) {
          display: block !important;
      }

      .quarkus-search {
          display: block !important;
      }

      .d-none {
          display: none;
      }
  `,c([u({type:String})],y.prototype,"server",2),c([u({type:String,attribute:"min-chars"})],y.prototype,"minChars",2),c([u({type:String,attribute:"initial-timeout"})],y.prototype,"initialTimeout",2),c([u({type:String,attribute:"more-timeout"})],y.prototype,"moreTimeout",2),c([u({type:String})],y.prototype,"language",2),c([u({type:String,attribute:"quarkus-version"})],y.prototype,"quarkusversion",2),c([u({type:String,attribute:"local-search"})],y.prototype,"localSearch",2),c([u({type:String,attribute:"origin-filter"})],y.prototype,"originFilter",2),c([X({hasChanged(e,i){return JSON.stringify(e)!==JSON.stringify(i)}})],y.prototype,"_backendData",2),y=c([U("qs-form")],y);var Ne=Ot(Tt());var A=class extends ${constructor(){super(...arguments);this.searchResultsTitle="";this.type="guide";this.originsWithRelativeUrls=[];this._loading=!0;this._handleScroll=e=>{if(this._loading||!this._result)return;if(!this._result.hasMoreHits){console.debug("no more hits");return}let i=this._hits.length==0?null:this._hits[this._hits.length-1];if(!i)return;let s=document.documentElement,n=s.scrollTop+s.clientHeight,o=i.offsetTop;n>=o&&(this._loading=!0,this._form.dispatchEvent(new CustomEvent(ct)))};this._handleScrollDebounced=(0,Ne.default)(this._handleScroll,100);this._handleResult=e=>{if(console.debug("Received results in qs-target: ",e.detail),this._loadingEnd(),!this._result||!e.detail||!e.detail.hits||e.detail.page===0){e.detail?.hits?document.body.classList.add("qs-has-results"):document.body.classList.remove("qs-has-results"),this._result=e.detail;return}this._result.hits=this._result.hits.concat(e.detail.hits),console.debug(`${this._result.hits.length} results in qs-target: `),this._result.hasMoreHits=e.detail.hasMoreHits};this._loadingStart=e=>{this._loading=!0,e.detail.page===0&&(this._result=void 0)};this._loadingEnd=()=>{this._loading=!1}}connectedCallback(){super.connectedCallback(),this._form=document.querySelector("qs-form"),this._form.addEventListener(P,this._handleResult),this._form.addEventListener(dt,this._loadingStart),document.addEventListener("scroll",this._handleScrollDebounced)}disconnectedCallback(){this._form.removeEventListener(P,this._handleResult),this._form.removeEventListener(dt,this._loadingStart),document.removeEventListener("scroll",this._handleScrollDebounced),super.disconnectedCallback()}render(){if(this._result?.hits){if(this._result.hits.length===0)return this._result.suggestion?f`
            <div id="qs-target" class="no-hits">
              <p>Sorry, no ${this.type}s matched your search.
                Did you mean <span class="suggestion" @click=${this._querySuggestion}>${z(this._result.suggestion.highlighted)}</span>?</p>
            </div>
          `:f`
          <div id="qs-target" class="no-hits">
            <p>Sorry, no ${this.type}s matched your search. Please try again.</p>
          </div>
        `;let e=this._result.hits.map(i=>this._renderHit(i));return f`
        ${this.searchResultsTitle===""?"":f`<h1 class="search-result-title">${this.searchResultsTitle}</h1>`}
        <div id="qs-target" class="qs-hits" aria-label="Search Hits">
          ${e}
        </div>
        ${this._loading?this._renderLoading():""}
      `}return this._loading?f`
        <div id="qs-target">${this._renderLoading()}</div>`:f`
      <div id="qs-target">
        <slot></slot>
      </div>
    `}_renderLoading(){return f`
      <div class="loading">Searching...</div>
    `}_renderHit(e){switch(this.type){case"guide":return f`
          <qs-guide class="qs-hit" .data=${e} origins-with-relative-urls=${this.originsWithRelativeUrls}></qs-guide>`}return""}_querySuggestion(){this._form.dispatchEvent(new CustomEvent(ut,{detail:{suggestion:this._result.suggestion}}))}};A.styles=L`
    
    .loading {
      background-image: url('${q(M.loading)}');
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
      
      .suggestion {
        text-decoration: underline;
        cursor: pointer;
        .highlighted {
          font-weight: bold;
        }
      }
    }

    .search-result-title {
      margin-top: 2.5rem;
      font-weight: var(--heading-font-weight);
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
   
  `,c([u({type:String,attribute:"search-results-title"})],A.prototype,"searchResultsTitle",2),c([u({type:String})],A.prototype,"type",2),c([u({type:String,attribute:"origins-with-relative-urls"})],A.prototype,"originsWithRelativeUrls",2),c([X()],A.prototype,"_result",2),c([X()],A.prototype,"_loading",2),c([Kt(".qs-hit")],A.prototype,"_hits",2),A=c([U("qs-target")],A);export{tt as LocalSearch,ct as QS_NEXT_PAGE_EVENT,ut as QS_QUERY_SUGGESTION_EVENT,P as QS_RESULT_EVENT,dt as QS_START_EVENT,y as QsForm,_ as QsGuide,A as QsTarget};
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

