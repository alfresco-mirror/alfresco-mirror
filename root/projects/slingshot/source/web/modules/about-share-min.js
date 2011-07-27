(function(){var c=YAHOO.util.Dom;Alfresco.module.AboutShare=function(e){var d=Alfresco.util.ComponentManager.get(this.id);if(d!==null){throw new Error("An instance of Alfresco.module.AboutShare already exists.")}Alfresco.module.AboutShare.superclass.constructor.call(this,"Alfresco.module.AboutShare",e,["container","connection","json"]);return this};YAHOO.extend(Alfresco.module.AboutShare,Alfresco.component.Base,{scrollpos:0,show:function a(){if(this.widgets.panel){this.widgets.panel.show();if(this.k3dmain!==null){this.k3dmain.canvas.startanim()}}else{Alfresco.util.Ajax.request({url:Alfresco.constants.URL_SERVICECONTEXT+"modules/about-share",dataObj:{htmlid:this.id},successCallback:{fn:this.onTemplateLoaded,scope:this},execScripts:true,failureMessage:"Could not load About Share template"})}},onTemplateLoaded:function b(d){var h=document.createElement("div");h.innerHTML=d.serverResponse.responseText;var e=c.getFirstChild(h);this.widgets.panel=Alfresco.util.createYUIPanel(e,{draggable:false});if(YAHOO.env.ua.ie===0||YAHOO.env.ua.ie>7){c.setStyle(this.id+"-contributions","display","block");var f=this;setInterval(function g(){var j=c.get(f.id+"-contributions");var i=f.scrollpos++;if(i>j.clientHeight){i=f.scrollpos=0}j.style.top="-"+i+"px"},80)}this.widgets.panel.show();
var me=this;this.widgets.panel.subscribe("hide",function(p_event,p_args){me.alfeehide()});if(window.addEventListener){var sequence=[],keySequence=[37,39],keySequenceStr=keySequence.toString();document.addEventListener("keydown",function(e){sequence.push(e.keyCode);while(sequence.length>keySequence.length){sequence.shift()}if(sequence.toString().indexOf(keySequenceStr)>=0){sequence=[];me.alfeeinit()}},true)}},k3dmain:null,alfeeinit:function(){if(this.k3dmain===null){var buf=document.createElement("canvas");if(!buf){return}buf.width=320;buf.height=245;buf.style.position="absolute";buf.style.left="60pt";buf.style.top="2pt";buf.style.zIndex=2;var p=c.get(this.id+"-logo");if(!p){return}p.appendChild(buf);var k3dmain=new ALFEE.Controller(buf);for(var i=0,j=24;i<j;i++){var obj=new ALFEE.ALFEEObject();obj.ophi=(360/j)*i;obj.otheta=(180/j)*i;with(obj){drawmode="wireframe";addgamma=0.5;addtheta=-1;addphi=-0.75;aboutx=75;abouty=-75;aboutz=0;offz=0;scale=10;init([{x:-1,y:1,z:-1},{x:1,y:1,z:-1},{x:1,y:-1,z:-1},{x:-1,y:-1,z:-1},{x:-1,y:1,z:1},{x:1,y:1,z:1},{x:1,y:-1,z:1},{x:-1,y:-1,z:1}],[{a:0,b:1},{a:1,b:2},{a:2,b:3},{a:3,b:0},{a:4,b:5},{a:5,b:6},{a:6,b:7},{a:7,b:4},{a:0,b:4},{a:1,b:5},{a:2,b:6},{a:3,b:7}],[{color:[255,0,0],vertices:[0,1,2,3]},{color:[0,255,0],vertices:[0,4,5,1]},{color:[0,0,255],vertices:[1,5,6,2]},{color:[255,255,0],vertices:[2,6,7,3]},{color:[0,255,255],vertices:[3,7,4,0]},{color:[255,0,255],vertices:[7,6,5,4]}])}k3dmain.addALFEEObject(obj)}this.k3dmain=k3dmain}this.k3dmain.canvas.startanim()},alfeehide:function(){if(this.k3dmain){this.k3dmain.canvas.stopanim()}}})})();Alfresco.module.getAboutShareInstance=function(){var a="alfresco-AboutShare-instance";return Alfresco.util.ComponentManager.get(a)||new Alfresco.module.AboutShare(a)};var RAD=Math.PI/180;var TWOPI=Math.PI*2;(function(){Vector3D=function(a,d,b){this.x=a;this.y=d;this.z=b;return this};Vector3D.prototype={x:0,y:0,z:0,clone:function(){return new Vector3D(this.x,this.y,this.z)},set:function(a){this.x=a.x;this.y=a.y;this.z=a.z;return this},add:function(a){this.x+=a.x;this.y+=a.y;this.z+=a.z;return this},sub:function(a){this.x-=a.x;this.y-=a.y;this.z-=a.z;return this},dot:function(a){return this.x*a.x+this.y*a.y+this.z*a.z},cross:function(a){return new Vector3D(this.y*a.z-this.z*a.y,this.z*a.x-this.x*a.z,this.x*a.y-this.y*a.x)},length:function(){return Math.sqrt(this.x*this.x+this.y*this.y+this.z*this.z)},distance:function(b){var d=this.x-b.x;var e=this.y-b.y;var a=this.z-b.z;return Math.sqrt(d*d+e*e+a*a)},thetaTo:function(b){var a=this.y*b.z-this.z*b.y,e=this.z*b.x-this.x*b.z,d=this.x*b.y-this.y*b.x;return Math.atan2(Math.sqrt(a*a+e*e+d*d),this.dot(b))},norm:function(){var a=this.length();this.x/=a;this.y/=a;this.z/=a;return this},scale:function(a){this.x*=a;this.y*=a;this.z*=a;return this}}})();if(typeof ALFEE=="undefined"||!ALFEE){var ALFEE={}}ALFEE.DEPTHCUE=new Array(256);for(var c=0;c<160;c++){ALFEE.DEPTHCUE[c]="rgb(0,"+(c+64)+",0)"}for(var c=160;c<192;c++){ALFEE.DEPTHCUE[c]="rgb(0,48,"+(c)+")"}for(var c=192;c<224;c++){ALFEE.DEPTHCUE[c]="rgb("+(c)+","+(c)+",0)"}for(var c=224;c<256;c++){ALFEE.DEPTHCUE[c]="rgb("+(c)+","+(c-64)+",0)"}(function(){ALFEE.Controller=function(a){this.canvas=a;var b=this;a.startanim=function(){if(b.interval===null){b.interval=setInterval(function(){b.tick()},25)}};a.stopanim=function(){if(b.interval!==null){clearInterval(b.interval);b.interval=null}};this.objects=[];this.lights=[];this.renderers=[];this.renderers.wireframe=new ALFEE.WireframeRenderer()};ALFEE.Controller.prototype={canvas:null,fillStyle:null,interval:null,renderers:null,objects:null,lights:null,addALFEEObject:function(a){a.setController(this,this.canvas.width,this.canvas.height);this.objects.push(a)},getRenderer:function(a){return this.renderers[a]},tick:function(){var d=this.canvas.getContext("2d");if(this.fillStyle!==null){d.fillStyle=this.fillStyle;d.fillRect(0,0,this.canvas.width,this.canvas.height)}else{d.clearRect(0,0,this.canvas.width,this.canvas.height)}var h=this.objects;for(var g=0,a=h.length;g<a;g++){h[g].executePipeline()}var f=this.lights;for(var g=0,a=f.length;g<a;g++){f[g].executePipeline()}h.forEach(function b(l,k,j){l.averagez=null});h.sort(function e(j,i){if(j.averagez===null){j.calculateAverageZ()}if(i.averagez===null){i.calculateAverageZ()}return(j.averagez<i.averagez?1:-1)});for(var g=0,a=h.length;g<a;g++){d.save();h[g].executeRenderer(d);d.restore()}}}})();(function(){ALFEE.BaseObject=function(){this.matrix=new Array(3);for(var a=0;a<3;a++){this.matrix[a]=new Array(3)}this.angles=new Array(6);return this};ALFEE.BaseObject.prototype={matrix:null,angles:null,offx:0,offy:0,offz:0,aboutx:0,abouty:0,aboutz:0,ogamma:0,otheta:0,ophi:0,addgamma:0,addtheta:0,addphi:0,velx:0,vely:0,velz:0,bminx:0,bminy:0,bminz:0,bmaxx:0,bmaxy:0,bmaxz:0,calcNormalVector:function(b,e,g,a,d,f){return new Vector3D((e*f)-(g*d),-((f*b)-(a*g)),(b*d)-(e*a)).norm()},calcMatrix:function(){var b=this.angles,a=this.matrix;b[0]=Math.sin(this.ogamma*RAD);b[1]=Math.cos(this.ogamma*RAD);b[2]=Math.sin(this.otheta*RAD);b[3]=Math.cos(this.otheta*RAD);b[4]=Math.sin(this.ophi*RAD);b[5]=Math.cos(this.ophi*RAD);a[0][0]=b[5]*b[1];a[1][0]=-(b[5]*b[0]);a[2][0]=b[4];a[0][1]=(b[2]*b[4]*b[1])+(b[3]*b[0]);a[1][1]=(b[3]*b[1])-(b[2]*b[4]*b[0]);a[2][1]=-(b[2]*b[5]);a[0][2]=(b[2]*b[0])-(b[3]*b[4]*b[1]);a[1][2]=(b[2]*b[1])+(b[3]*b[4]*b[0]);a[2][2]=b[3]*b[5]},transformToWorld:function(){},executePipeline:function(){this.ogamma+=this.addgamma;this.otheta+=this.addtheta;this.ophi+=this.addphi;this.offx+=this.velx;this.offy+=this.vely;this.offz+=this.velz;if(this.offx<this.bminx||this.offx>this.bmaxx){this.velx*=-1}if(this.offy<this.bminy||this.offy>this.bmaxy){this.vely*=-1}if(this.offz<this.bminz||this.offz>this.bmaxz){this.velz*=-1}this.calcMatrix();this.transformToWorld()}}})();(function(){ALFEE.ALFEEObject=function(){ALFEE.ALFEEObject.superclass.constructor.call(this);return this};YAHOO.extend(ALFEE.ALFEEObject,ALFEE.BaseObject,{controller:null,worldcoords:null,screenx:0,screeny:0,linescale:2,color:null,drawmode:"wireframe",shademode:"depthcue",perslevel:10,scale:1,points:null,edges:null,faces:null,screencoords:null,averagez:null,init:function(o,g,d){this.points=o;this.edges=g;this.faces=d;this.worldcoords=new Array(o.length+d.length);for(var k=0,f=this.worldcoords.length;k<f;k++){this.worldcoords[k]={x:0,y:0,z:0}}this.screencoords=new Array(o.length);for(var k=0,f=this.screencoords.length;k<f;k++){this.screencoords[k]={x:0,y:0}}if(this.scale!==0){for(var k=0,f=this.points.length;k<f;k++){o[k].x*=this.scale;o[k].y*=this.scale;o[k].z*=this.scale}}for(var k=0,f=d.length;k<f;k++){var l=d[k].vertices;var b=o[l[1]].x-o[l[0]].x;var n=o[l[1]].y-o[l[0]].y;var h=o[l[1]].z-o[l[0]].z;var a=o[l[2]].x-o[l[0]].x;var m=o[l[2]].y-o[l[0]].y;var e=o[l[2]].z-o[l[0]].z;d[k].normal=this.calcNormalVector(b,n,h,a,m,e);if(!d[k].color){d[k].color=[255,255,255]}}if(this.color===null){this.color=[255,255,255]}},setController:function(a,b,d){this.controller=a;this.screenx=b/2;this.screeny=d/2;this.bminx=-this.screenx;this.bminy=-this.screeny;this.bminz=-this.screenx;this.bmaxx=this.screenx;this.bmaxy=this.screeny;this.bmaxz=this.screenx},transformToWorld:function(){var l,j,g;var q=this.points,f=this.worldcoords,b=this.faces,o=this.matrix;var m=this.aboutx,k=this.abouty,h=this.aboutz,e=this.offx,d=this.offy,a=this.offz;var s=o[0],p=o[1],n=o[2];for(var r=0,t=q.length;r<t;r++){l=q[r].x+m;j=q[r].y+k;g=q[r].z+h;f[r].x=(s[0]*l)+(s[1]*j)+(s[2]*g)+e;f[r].y=(p[0]*l)+(p[1]*j)+(p[2]*g)+d;f[r].z=(n[0]*l)+(n[1]*j)+(n[2]*g)+a}for(var r=0,t=b.length,u;r<t;r++){u=b[r].normal;l=u.x;j=u.y;g=u.z;b[r].worldnormal=new Vector3D((s[0]*l)+(s[1]*j)+(s[2]*g)+e,(p[0]*l)+(p[1]*j)+(p[2]*g)+d,(n[0]*l)+(n[1]*j)+(n[2]*g)+a)}},transformToScreen:function(){var k,h,g;var b=this.worldcoords,j=this.screencoords;var m=this.screenx,l=this.screeny,f=this.perslevel;var a=(1<<f);for(var d=0,e=this.points.length;d<e;d++){k=b[d].x;h=b[d].y;g=b[d].z+a;if(g===0){g=1}j[d].x=((k<<f)/g)+m;j[d].y=l-((h<<f)/g)}},executePipeline:function(){ALFEE.ALFEEObject.superclass.executePipeline.call(this);this.transformToScreen();this.controller.getRenderer(this.drawmode).sortByDistance(this)},executeRenderer:function(a){this.controller.getRenderer(this.drawmode).renderObject(this,a)},calculateAverageZ:function(){var e=0;var d=this.worldcoords;for(var b=0,a=this.points.length;b<a;b++){e+=d[b].z}this.averagez=e/this.points.length}})})();(function(){ALFEE.Renderer=function(){};ALFEE.Renderer.prototype={sortByDistance:function(a){},renderObject:function(b,a){}}})();(function(){ALFEE.WireframeRenderer=function(){ALFEE.WireframeRenderer.superclass.constructor.call(this);return this};YAHOO.extend(ALFEE.WireframeRenderer,ALFEE.Renderer,{sortByDistance:function(a){this.quickSortObject(a.worldcoords,a.edges,0,a.edges.length-1)},quickSortObject:function(j,b,i,d){var e=i,h=d,f;var g;if(d>i){f=((j[(b[(i+d)>>1].a)].z)+(j[(b[(i+d)>>1].b)].z))/2;while(e<=h){while((e<d)&&((j[(b[e].a)].z+j[(b[e].b)].z)/2>f)){e++}while((h>i)&&((j[(b[h].a)].z+j[(b[h].b)].z)/2<f)){h--}if(e<=h){g=b[e];b[e]=b[h];b[h]=g;e++;h--}}if(i<h){this.quickSortObject(j,b,i,h)}if(e<d){this.quickSortObject(j,b,e,d)}}},renderObject:function(j,q){var l,o,n;var g=j.edges,m=j.screencoords,f=j.worldcoords;var d=j.screenx,e=d>>6,p=j.linescale/255;for(var h=0,k=g.length;h<k;h++){o=g[h].a;n=g[h].b;switch(j.shademode){case"plain":l=j.color;q.fillStyle="rgb("+l[0]+","+l[1]+","+l[2]+")";break;case"depthcue":l=((f[o].z+f[n].z)/2)+d;l=l/e;if(l<0){l=0}if(l>255){l=255}l=255-Math.ceil(l);q.strokeStyle=ALFEE.DEPTHCUE[l];q.lineWidth=p*l;break}q.beginPath();q.moveTo(m[o].x,m[o].y);q.lineTo(m[n].x,m[n].y);q.closePath();q.stroke()}
}})})();Alfresco.module.getAboutShareInstance=function(){var a="alfresco-AboutShare-instance";return Alfresco.util.ComponentManager.get(a)||new Alfresco.module.AboutShare(a)};