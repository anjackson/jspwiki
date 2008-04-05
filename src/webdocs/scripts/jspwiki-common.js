/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
 
 /**
  ** Javascript routines to support JSPWiki
  ** since v.2.6.0
  **
  ** Use mootools v1.1, with following components:  
  **   Core, Class,  Native, Element(ex. Dimensions), Window,
  **   Effects(ex. Scroll), Drag(Base), Remote, Plugins(Hash.Cookie, Tips, Accordion)
  **
  ** 100 Wiki object (page parms, UserPrefs and setting focus) 
  ** 105 WikiSlidingFavorites 
  ** 110 WikiSlimbox : attachment viewer 
  ** 114 Reflection (adds reflection to images) 
  ** 120 QuickLinks object 
  ** 130 TabbedSection object
  ** 132 Accordion object 
  ** 140 SearchBox object: remember 10 most recent search topics
  ** 150 Colors, GraphBar object: e.g. used on the findpage
  ** 
  ** 200 Collapsible list items
  ** 210 Collapsbile Trees
  ** 220 RoundedCorners ffs
  ** 230 Sortable (clever table-sort) 
  ** 240 Table-filter (excel like table filters 
  ** 250 Categories: turn wikipage link into AJAXed popup 
  ** 260 WikiTips 
  ** 270 WikiColumns 
  ** 280 ZebraTable: color odd/even row of a table 
  ** 290 HighlightWord: refactored
  ** 295 Typography
  ** 300 Prettify
  **/

/* extend mootools */
String.extend({
	deCamelize: function(){
		return this.replace(/([a-z])([A-Z])/g,"$1 $2");
	},
	trunc: function(size,elips){
		if( !elips ) elips="...";
		return (this.length<size) ? this : this.substring(0,size)+elips;
	}
})

// get text of a dhtml node
function $getText(el) {
	return el.innerText || el.textContent || '';
}
Element.extend({

	/* wrapper = new Element('div').injectWrapper(node); */
	injectWrapper: function(el){
		while( el.firstChild ) this.appendChild( el.firstChild );
		el.appendChild( this ) ;
		return this;
	},

	visible: function() {
		var el = this;
		while($type(el)=='element'){
			if(el.getStyle('visibility') == 'hidden') return false;
			if(el.getStyle('display') == 'none' ) return false;
			el = el.getParent();
		}
		return true;
	},

	hide: function() {
		return this.setStyle('display','none');
	},

	show: function() {
		return this.setStyle('display','');
	},

	toggle: function() {
		return this.visible() ? this.hide() : this.show();  
	},

	scrollTo: function(x, y){
		this.scrollLeft = x;
		this.scrollTop = y;
	},

	/* dimensions.js */
	getPosition: function(overflown){
		overflown = overflown || [];
		var el = this, left = 0, top = 0;
		do {
			left += el.offsetLeft || 0;
			top += el.offsetTop || 0;
			el = el.offsetParent;
		} while (el);
		overflown.each(function(element){
			left -= element.scrollLeft || 0;
			top -= element.scrollTop || 0;
		});
		return {'x': left, 'y': top};
	},

	getDefaultValue: function(){
		switch(this.getTag()){
			case 'select':
				var values = [];
				$each(this.options, function(option){
					if (option.defaultSelected) values.push($pick(option.value, option.text));
				});
				return (this.multiple) ? values : values[0];
			case 'input': if (!(this.defaultChecked && ['checkbox', 'radio'].contains(this.type)) && !['hidden', 'text', 'password'].contains(this.type)) break;
			case 'textarea': return this.defaultValue;
		}
		return false;
	}

});

var Observer = new Class({
	initialize: function(el, fn, options){
		this.options = Object.extend({
	   	    event: 'keyup',
			delay: 300
		}, options || {});
		this.element = $(el);
		this.callback = fn;
		this.timeout = null;
		this.listener = this.fired.bind(this);
		this.value = this.element.getValue();
		this.element.setProperty('autocomplete','off').addEvent(this.options.event, this.listener);
	},
	fired: function() {
		if (this.value == this.element.value) return;
		this.clear();
		this.value = this.element.value;
		this.timeout = this.callback.delay(this.options.delay, null, [this.element]);
	},
	clear: function() {
		this.timeout = $clear(this.timeout);
	},
	stop: function() {
		this.element.removeEvent(this.options.event, this.listener);
		this.clear();
	}
});

/* Observable class: observe any form element for changes */
Element.extend({
	observe: function(fn, options){
		return new Observer(this, fn, options);
	}
});


/* I18N Support
 * LocalizedStrings takes form { "javascript.some.resource.key":"localised resource key {0}" }
 * Examples:
 * "moreInfo".localize();
 * "imageInfo".localize(2,4); => "Image {0} of {1}" becomes "Image 2 of 4 
 */
var LocalizedStrings = LocalizedStrings || []; //defensive
String.extend({
	localize: function(){
		var s = LocalizedStrings["javascript."+this], 
			args = arguments;

		if(!s) return("???" + this + "???");

		return s.replace(/\{(\d)\}/g, function(m){ 
			return args[m.charAt(1)] || "???"+m.charAt(1)+"???";
		});
	}
});

/* FIXME parse number anywhere inside a string */
Number.REparsefloat = new RegExp( "([+-]?\\d+(:?\\.\\d+)?(:?e[-+]?\\d+)?)", "i");

/** TABLE stuff **/
function $T(el) {
	var t = $(el); 
	return (t && t.tBodies[0]) ? $(t.tBodies[0]) : t;
};

/* FIXME */
// find first ancestor element with tagName
function getAncestorByTagName( node, tagName ) {
	if( !node) return null;
	if( node.nodeType == 1 && (node.tagName.toLowerCase() == tagName.toLowerCase())){ 
		return node; 
	} else { 
		return getAncestorByTagName( node.parentNode, tagName ); 
	}
}


/** 100 Wiki functions **/
var Wiki = {

	onPageLoad: function(){

		var meta = ['wikiPageName','wikiJsonUrl','wikiPageUrl','wikiEditUrl','wikiBaseUrl','wikiUserName','wikiTemplateUrl']
		$$('meta').each(function(el){
			var n = el.getProperty('name');
			if( meta.contains(n) ) this[n.substr(4)] = el.getProperty('content');
		},this);

		var h = location.host;
		this.BasePath = this.BaseUrl.slice(this.BaseUrl.indexOf(h)+h.length,-1);
		//this.ClientLanguage = navigator.language ? navigator.language : navigator.userLanguage;
		//this.ClientTimezone = new Date().getTimezoneOffset()/60;

		this.prefs = new Hash.Cookie('JSPWikiUserPrefs', {path:Wiki.BasePath, duration:20});
		
		this.PermissionEdit = !!$$('a.edit')[0]; //deduct permission level
		this.url = null;
		this.parseLocationHash.periodical(500);

		this.makeMenuFx('morebutton', 'morepopup');
	},

	setFocus: function(){
		/* plain.jsp,   login.jsp,   prefs/profile, prefs/prefs, find */
		['editorarea','j_username','loginname','assertedName','query2'].some(function(el){
			el = $(el);
			if(el && el.visible()) { el.focus(); return true; }
			return false;
		});
	},

	getUrl: function(pagename){
		return this.PageUrl.replace(/%23%24%25/, pagename);
	},	

	/* retrieve pagename from any wikipage url format */
	getPageName: function(url){
		var s = this.PageUrl.escapeRegExp().replace(/%23%24%25/, '(.+)'),
			res = url.match(new RegExp(s));
		return (res ? res[1] : false);
	},

	savePrefs: function(){
		if($('prefSkin')) this.prefs.set('SkinName', $('prefSkin').getValue());
		if($('prefTimeZone')) this.prefs.set('TimeZone', $('prefTimeZone').getValue());
		if($('prefTimeFormat')) this.prefs.set('DateFormat', $('prefTimeFormat').getValue());
		if($('prefOrientation')) this.prefs.set('Orientation', $('prefOrientation').getValue());
		if($('editor')) this.prefs.set('editor', $('editor').getValue()); 
		if($('prefLanguage')) this.prefs.set('Language', $('prefLanguage').getValue()); 
	},

	changeOrientation: function(){
		var fav = $('prefOrientation').getValue();
		$('wikibody')
			.removeClass('fav-left').removeClass('fav-right')
			.addClass(fav);
		//$('collapseFavs').fireEvent('click').fireEvent('click'); //refresh sliding favorites
	},

	/* make hover menu with fade effect */
	makeMenuFx: function(btn, menu){
		var btn = $(btn), menu = $(menu);
		if(!btn || !menu) return;

		var	popfx = menu.effect('opacity', {wait:false}).set(0);
		btn.adopt(menu).set({
			'href':'#',
			'events':{
				'mouseout': function(){ popfx.start(0) },
				'mouseover': function(){ Wiki.locatemenu(btn,menu); popfx.start(0.9) }
			}
		});
	},
	
	locatemenu: function(base,el){
		var win = {'x': window.getWidth(), 'y': window.getHeight()},
			scroll = {'x': window.getScrollLeft(), 'y': window.getScrollTop()},
			corner = base.getPosition(),
			offset = {'x': base.offsetWidth-el.offsetWidth, 'y': base.offsetHeight },
			popup = {'x': el.offsetWidth, 'y': el.offsetHeight},
			prop = {'x': 'left', 'y': 'top'};

		for (var z in prop){
			var pos = corner[z] + offset[z]; /*top-left corner of base */
			if ((pos + popup[z] - scroll[z]) > win[z]) pos = win[z] - popup[z] + scroll[z];
			el.setStyle(prop[z], pos);
		};
	},

	parseLocationHash: function(){
		if(this.url && this.url == location.href ) return;
		this.url = location.href;
		var h = location.hash; 
		if( h=="" ) return;
		h = h.replace(/^#/,'');

		var el = $(h);

		while( $type(el) == 'element' ){
			if( el.hasClass('hidetab') ){
				TabbedSection.clickTab.apply(el);
			} else if( el.hasClass('tab') ){
				/* accordion -- need to find accordion object */
			} else if( el.hasClass('collapsebody') ){
				/* collapsible box */
			} else if(!el.visible() ){
				//alert('not visible'+el.id);
				//fixme need to find the correct toggler
				//el.show(); //eg collapsedBoxes: fixme
			}
			el = el.getParent();
		}

		location = location.href; /* now jump to the #hash */
	},

	/* SubmitOnce: disable all buttons to avoid double submit */
	submitOnce: function(form){
		window.onbeforeunload = null; /* regular exit of this page -- see jspwiki-edit.js */
		(function(){ 
		$A(form.elements).each(function(e){
				if( (/submit|button/i).test(e.type)) e.disabled = true;
			});
		}).delay(10);
		return true;
	},

	submitUpload: function(form, progress){
		$('progressbar').setStyle('visibility','visible');
		this.progressbar =
		Wiki.jsonrpc.periodical(1000, this, ["progressTracker.getProgress",[progress],function(result){
			if(!result.code) $('progressbar').getFirst().setStyle('width',result+'%').setHTML(result+'%');
		}]);

		return Wiki.submitOnce(form);
	},

	JSONid : 10000,
	jsonrpc: function(method, params, fn) {	
		new Ajax( Wiki.JsonUrl, {
			postBody: Json.toString({"id":Wiki.JSONid++, "method":method, "params":params}), 
			method: 'post', 
			onComplete: function(result){ 
				var r = Json.evaluate(result,true);
				if(!r) return;
				if(r.result) fn(r.result);
				else if(r.error) fn(r.error);
			}
		}).request();
	}	
}

/** 105 WikiSlidingFavs
 ** Support sliding favorites menu
 ** Dirk Frederickx, Feb 2008
 */
var WikiSlidingFavs = 
{
	onPageLoad: function(){
		var tabs = $E('#page .tabs'); if( !tabs ) return;

		/* inject a wrapper div cause relative can not be set directly on the .tabs */
		tabs = new Element('div', { 
			'styles': { 
				'position':'relative', 
				'padding':'0.5em'
			} 
		}).injectWrapper(tabs.setStyle('padding','0'));	

		var body = $('wikibody'),
			page = $('page'),
			favs = $('favorites'),
			favsWidth = favs.offsetWidth,
			pageWidth = page.offsetWidth;

		var toggleFavs = function(){
			if( body.hasClass('fav-slide') ){ 
				toggler.set({'title': 'favs.show'.localize()});
				favsFx.set(favsHide);
				favsWrapper.setStyles({'width':favsWidth}).adopt(favs);
			} else {
				toggler.set({'title': 'favs.hide'.localize()});
				favs.injectAfter(page);
			}
		}
			
		var toggler = new Element('div', {
			'id':'collapseFavs',
			'events': {
				'click': function(){
					body.toggleClass('fav-slide');
					Wiki.prefs.set('slidingFav', body.hasClass('fav-slide') ? 'fav-slide' : '' );
					toggleFavs();					
				},
				'mouseenter': function(e){
					this.addClass('hover');
					var ppx = toggler.getPosition().x+"px";
					pointer.setStyles({ left: ppx, top: (e.pageY || e.clientY)+"px" }).show();
					if( body.hasClass('fav-slide') ){
						favsWrapper.inject(toggler).show();
						favsFx.start(favsShow);
					}
				},
				'mousemove': function(e){
					var ppx = toggler.getPosition().x+"px";
					pointer.setStyles({	left: ppx, top: (e.pageY || e.clientY)+"px" });
				},
				'mouseleave': function(){
					this.removeClass('hover');
					pointer.hide();
					if( body.hasClass('fav-slide') ){
						favsFx.start(favsHide).chain(function(){
						 	favsWrapper.hide().inject(document.body);
						});
					}
				}
			}
		}).injectTop(tabs);

		var pointer = new Element('div', {
			'id':'collapseFavsPointer'
		}).hide().inject(body);
			
		var favsWrapper = new Element('div', {
			'id':'collapseFavsWrapper',
			'events': {
				'click' : function(e){ e.stopPropagation();	},
				'mouseenter' : function(e){ pointer.hide();	},
				'mouseleave' : function(e){	pointer.show();	}
			} 
		}).hide().inject(document.body);

		var	favsFx = new Fx.Styles(favs,{wait:false}),
			favsHide = {'margin-left':-favsWidth, 'width':0, 'opacity':0},
			favsShow = {'margin-left':0, 'width':favsWidth, 'opacity':0.9};

		if( Wiki.prefs.get('slidingFav') == 'fav-slide') body.addClass('fav-slide');

		toggleFavs();
		//toggleFavs.delay(100);

	} 
}

/** 110 WikiSlimbox
 ** Inspired http://www.digitalia.be/software/slimbox by Christophe Bleys
 ** Dirk Frederickx, Mar 2007
 ** 	%%slimbox [...] %%
 ** 	%%slimbox-img  [some-image.jpg] %%
 ** 	%%slimbox-ajax [some-page links] %%
 **/
var WikiSlimbox = {

	onPageLoad: function(){
		var i = 0,
			lnk = new Element('a',{'class':'slimbox'}).setHTML('&raquo;');
			
		$$('*[class^=slimbox]').each(function(slim){
			var group = 'lightbox'+ i++,
				parm = slim.className.split('-')[1] || 'img ajax',
				filter = [];
			if(parm.test('img')) filter.extend(['img.inline', 'a.attachment']); 
			if(parm.test('ajax')) filter.extend(['a.wikipage', 'a.external']); 
			$ES(filter.join(','),slim).each(function(el){
				var href = el.src||el.href;
				var rel = (el.className.test('inline|attachment')) ? 'img' : 'ajax';
				if((rel=='img') && !href.test('(.bmp|.gif|.png|.jpg|.jpeg)(\\?.*)?$','i')) return;
				lnk.clone().setProperties({
					'href':href, 
					'rel':group+' '+rel,
					'title':el.alt||el.getText()
				}).injectBefore(el);
				if(el.src) el.replaceWith(new Element('a',{
					'class':'attachment',
					'href':el.src
				}).setHTML(el.alt||el.getText()));
			});
		});
		if(i) Lightbox.init();
		//new Asset.javascript(Wiki.TemplateUrl+'scripts/slimbox.js');
	}
}

/*
	Slimbox v1.31 - The ultimate lightweight Lightbox clone
	by Christophe Beyls (http://www.digitalia.be) - MIT-style license.
	Inspired by the original Lightbox v2 by Lokesh Dhakar.

	Updated by Dirk Frederickx to fit JSPWiki needs
	- minimum size of image canvas DONE
	- add maximum size of image w.r.t window size DONE
	- CLOSE icon -> close x text iso icon DONE
	- <<prev, next>> links added in visible part of screen DONE
	- add size of picture to info window DONE
	- spacebor, down arrow, enter : next image DONE
	- up arrow : prev image DONE
	- allow the same picture occuring several times DONE
	- add support for external page links  => slimbox_ex DONE
*/
var Lightbox = {

	init: function(options){
		this.options = $extend({
			resizeDuration: 400,
			resizeTransition: false, /*Fx.Transitions.sineInOut,*/
			initialWidth: 250,
			initialHeight: 250,
			animateCaption: true,
			errorMessage: "slimbox.error".localize()
		}, options || {});

		this.anchors=[];
		$each(document.links, function(el){
			if (el.rel && el.rel.test(/^lightbox/i)){
				el.onclick = this.click.pass(el, this);
				this.anchors.push(el);
			}
		}, this);
		this.eventKeyDown = this.keyboardListener.bindAsEventListener(this);
		this.eventPosition = this.position.bind(this);

		/*	Build float panel
			<div id="lbOverlay"></div>
			<div id="lbCenter">
				<div id="lbImage">
					<!-- img or iframe element is inserted here -->
				</div>
			</div>
			<div id="lbBottomContainer">
				<div id="lbBottom">
					<div id="lbCaption">
					<div id="lbNumber">
					<a id="lbCloseLink"></a>
					<div style="clear:both;"></div>
				</div>
			</div>
		*/
		this.overlay = new Element('div', {'id': 'lbOverlay'}).inject(document.body);

		this.center = new Element('div', {'id': 'lbCenter', 'styles': {'width': this.options.initialWidth, 'height': this.options.initialHeight, 'marginLeft': -(this.options.initialWidth/2), 'display': 'none'}}).inject(document.body);
		new Element('a', {'id': 'lbCloseLink', 'href':'#', 'title':'slimbox.close.title'.localize()}).inject(this.center).onclick = this.overlay.onclick = this.close.bind(this);
		this.image = new Element('div', {'id': 'lbImage'}).inject(this.center);

		this.bottomContainer = new Element('div', {'id': 'lbBottomContainer', 'styles': {'display': 'none'}}).inject(document.body);
		this.bottom = new Element('div', {'id': 'lbBottom'}).inject(this.bottomContainer);
		//new Element('a', {'id': 'lbCloseLink', 'href': '#', 'title':'slimbox.close.title'.localize()}).setHTML('slimbox.close'.localize()).inject(this.bottom).onclick = this.overlay.onclick = this.close.bind(this);
		this.caption = new Element('div', {'id': 'lbCaption'}).inject(this.bottom);

		var info = new Element('div').inject(this.bottom);  
		this.prevLink = new Element('a', {'id': 'lbPrevLink', 'href': '#', 'styles': {'display': 'none'}}).setHTML("slimbox.previous".localize()).inject(info);
		this.number = new Element('span', {'id': 'lbNumber'}).inject(info);
		this.nextLink = this.prevLink.clone().setProperties({'id': 'lbNextLink' }).setHTML("slimbox.next".localize()).inject(info);
		this.prevLink.onclick = this.previous.bind(this);
		this.nextLink.onclick = this.next.bind(this);

 		this.error = new Element('div').setProperty('id', 'lbError').setHTML(this.options.errorMessage);
		new Element('div', {'styles': {'clear': 'both'}}).inject(this.bottom);
		
		var nextEffect = this.nextEffect.bind(this);
		this.fx = {
			overlay: this.overlay.effect('opacity', {duration: 500}).hide(),
			resize: this.center.effects($extend({duration: this.options.resizeDuration, onComplete: nextEffect}, this.options.resizeTransition ? {transition: this.options.resizeTransition} : {})),
			image: this.image.effect('opacity', {duration: 500, onComplete: nextEffect}),
			bottom: this.bottom.effect('margin-top', {duration: 400, onComplete: nextEffect})
		};

		this.fxs = new Fx.Elements([this.center, this.image], $extend({duration: this.options.resizeDuration, onComplete: nextEffect}, this.options.resizeTransition ? {transition: this.options.resizeTransition} : {}));
		
		this.preloadPrev = new Image();
		this.preloadNext = new Image();
	},

	click: function(link){
		var rel = link.rel.split(' ');
		if (rel[0].length == 8) return this.open([[url, title, rel[1]]], 0);

		var imageNum=0, images = [];
		this.anchors.each(function(el){
			var elRel = el.rel.split(' ');
			if (elRel[0]!=rel[0]) return;
			if((el.href==link.href) && (el.title==link.title)) imageNum = images.length;
			images.push([el.href, el.title, elRel[1]]);
		});
		return this.open(images, imageNum);
	},

	open: function(images, imageNum){
		this.images = images;
		this.position();
		this.setup(true);
		this.top = window.getScrollTop() + (window.getHeight() / 15);
		this.center.setStyles({top: this.top, display: ''});
		this.fx.overlay.start(0.7);
		return this.changeImage(imageNum);
	},

	position: function(){
		this.overlay.setStyles({top: window.getScrollTop(), height: window.getHeight()});
	},

	setup: function(open){
		var elements = $A(document.getElementsByTagName('object'));
		elements.extend(document.getElementsByTagName(window.ie ? 'select' : 'embed'));
		elements.each(function(el){
			if (open) el.lbBackupStyle = el.style.visibility;
			el.style.visibility = open ? 'hidden' : el.lbBackupStyle;
		});
		var fn = open ? 'addEvent' : 'removeEvent';
		window[fn]('scroll', this.eventPosition)[fn]('resize', this.eventPosition);
		document[fn]('keydown', this.eventKeyDown);
		this.step = 0;
	},

	keyboardListener: function(event){
		switch (event.keyCode){
			case 27: case 88: case 67: this.close(); break;
			case 37: case 38: case 80: this.previous(); break;	
			case 13: case 32: case 39: case 40: case 78: this.next(); break;
			default: return;
		}
		new Event(event).stop();
	},

	previous: function(){
		return this.changeImage(this.activeImage-1);
	},

	next: function(){
		return this.changeImage(this.activeImage+1);
	},

	changeImage: function(imageNum){
		if (this.step || (imageNum < 0) || (imageNum >= this.images.length)) return false;
		this.step = 1;
		this.activeImage = imageNum;

		this.center.style.backgroundColor = '';
		this.bottomContainer.style.display = this.prevLink.style.display = this.nextLink.style.display = 'none';
		this.fx.image.hide();
		this.center.className = 'lbLoading';

		this.preload = new Image();
		this.image.empty().setStyle('overflow','hidden');
		if( this.images[imageNum][2] == 'img' ){
			this.preload.onload = this.nextEffect.bind(this);
			this.preload.src = this.images[imageNum][0];
		} else {			
			this.iframeId = "lbFrame_"+new Date().getTime();	// Safari would not update iframe content that has static id.
			this.so = new Element('iframe').setProperties({
				id: this.iframeId, 
//				width: this.contentsWidth, 
//				height: this.contentsHeight, 
				frameBorder:0, 
				scrolling:'auto', 
				src:this.images[imageNum][0]
			}).inject(this.image);
			this.nextEffect();	//asynchronous loading?

		}
		return false;
	},

	ajaxFailure: function (){
		this.ajaxFailed = true;
		this.image.setHTML('').adopt(this.error.clone());
		this.nextEffect();
	},
	
	nextEffect: function(){
		switch (this.step++){
		case 1:
			this.center.className = '';
			this.caption.empty().adopt(new Element('a', {
					'href':this.images[this.activeImage][0],
					'title':"slimbox.directLink".localize()
				}).setHTML(this.images[this.activeImage][1] || ''));
				
			var type = (this.images[this.activeImage][2]=='img') ? "slimbox.info" : "slimbox.remoteRequest";
			this.number.setHTML((this.images.length == 1) ? '' : type.localize(this.activeImage+1, this.images.length));
			this.image.style.backgroundImage = 'none';

			var w = Math.max(this.options.initialWidth,this.preload.width),
				h = Math.max(this.options.initialHeight,this.preload.height),
				ww = Window.getWidth()-10,
				wh = Window.getHeight()-120;
			if(this.images[this.activeImage][2]!='img' &&!this.ajaxFailed){ w = 6000; h = 3000; }
			if(w > ww) { h = Math.round(h * ww/w); w = ww; }
			if(h > wh) { w = Math.round(w * wh/h); h = wh; }

			this.image.style.width = this.bottom.style.width = w+'px';
			this.image.style.height = /*this.prevLink.style.height = this.nextLink.style.height = */ h+'px';
			
			if( this.images[this.activeImage][2]=='img') {
				this.image.style.backgroundImage = 'url('+this.images[this.activeImage][0]+')';

				if (this.activeImage) this.preloadPrev.src = this.images[this.activeImage-1][0];
				if (this.activeImage != (this.images.length - 1)) this.preloadNext.src = this.images[this.activeImage+1][0];
			
				this.number.setHTML(this.number.innerHTML+'&nbsp;&nbsp;['+this.preload.width+'&#215;'+this.preload.height+']');
			} else {
				this.so.style.width=w+'px';
				this.so.style.height=h+'px';
			}

			if (this.options.animateCaption) this.bottomContainer.setStyles({height: '0px', display: ''});

			this.fxs.start({
				'0': { height: [this.image.offsetHeight], width: [this.image.offsetWidth], marginLeft: [-this.image.offsetWidth/2] },
				'1': { opacity: [1] }
			});	

			break;
		case 2:
			//this.center.style.backgroundColor = '#000';
			this.image.setStyle('overflow','auto');
			this.bottomContainer.setStyles({ top: (this.top + this.center.clientHeight)+'px', marginLeft: this.center.style.marginLeft });
			if (this.options.animateCaption){
				this.fx.bottom.set(-this.bottom.offsetHeight);
				this.bottomContainer.style.height = '';
				this.fx.bottom.start(0);
				break;
			}
			this.bottomContainer.style.height = '';
		case 3:
			if (this.activeImage) this.prevLink.style.display = '';
			if (this.activeImage != (this.images.length - 1)) this.nextLink.style.display = '';
			this.step = 0;
		}
	},

	close: function(){
		if (this.step < 0) return;
		this.step = -1;
		if (this.preload){
			this.preload.onload = Class.empty;
			this.preload = null;
		}
		for (var f in this.fx) this.fx[f].stop();
		this.center.style.display = this.bottomContainer.style.display = 'none';
		this.fx.overlay.chain(this.setup.pass(false, this)).start(0);
		return false;
	}
};


/** 114 Reflection
 ** Inspired by Reflection.js at http://cow.neondragon.net/stuff/reflection/
 ** Freely distributable under MIT-style license.
 ** Adapted for JSPWiki/BrushedTemplate, D.Frederickx, Sep 06
 ** Use:
 ** 	%%reflection-height-opacity  [some-image.jpg] %%
 **/
var WikiReflection = {

	onPageLoad: function(){
		$$('*[class^=reflection]').each( function(w){
			var parms = w.className.split('-');
			$ES('img',w).each(function(img){
				Reflection.add(img, parms[1], parms[2]);
			}); 
		});
	}
}
/* FIXME : add delayed loading of reflection library */
var Reflection = {

	options: { height: 0.33, opacity: 0.5 },

	add: function(img, height, opacity) {
		//TODO Reflection.remove(image); --is this still needed?
		height  = (height ) ? height/100 : this.options.height;
		opacity = (opacity) ? opacity/100: this.options.opacity;

		var div = new Element('div').injectAfter(img).adopt(img),
			imgW = img.width,
			imgH = img.height,
			rH   = Math.floor(imgH * height); //reflection height

		div.className = img.className.replace(/\breflection\b/, "");
		div.style.cssText = img.backupStyle = img.style.cssText;
		//div.setStyles({'width':img.width, 'height':imgH +rH, "maxWidth": imgW });
		div.setStyles({'width':img.width, 'height':imgH +rH });
		img.style.cssText = 'vertical-align: bottom';
		//img.className = 'inline reflected';  //FIXME: is this still needed ??

		if( window.ie ){ 
			new Element('img', {'src': img.src, 'styles': {
				'width': imgW,
				'marginBottom': "-" + (imgH - rH) + 'px',
				'filter': 'flipv progid:DXImageTransform.Microsoft.Alpha(opacity='+(opacity*100)+', style=1, finishOpacity=0, startx=0, starty=0, finishx=0, finishy='+(height*100)+')'
			}}).inject(div);
		} else {
			var r = new Element('canvas', {'width':imgW, 'height':rH, 'styles': {'width':imgW, 'height': rH}}).inject(div);
			if( !r.getContext ) return;

			var ctx = r.getContext("2d");
			ctx.save();
			ctx.translate(0, imgH-1);
			ctx.scale(1, -1);
			ctx.drawImage(img, 0, 0, imgW, imgH);
			ctx.restore();
			ctx.globalCompositeOperation = "destination-out";

			var g = ctx.createLinearGradient(0, 0, 0, rH);
			g.addColorStop( 0, "rgba(255, 255, 255, " + (1 - opacity) + ")" );
			g.addColorStop( 1, "rgba(255, 255, 255, 1.0)" );
			ctx.fillStyle = g;
			ctx.rect( 0, 0, imgW, rH );
			ctx.fill(); 
		}
	}
}
 
/** 120 brushed quick links **/
var QuickLinks = {

	onPageLoad: function(){
		if( $("previewcontent") || !Wiki.PermissionEdit ) return;	

		var url = Wiki.EditUrl;
		url = url + (url.contains('?') ? '&' : '?') + 'section=';

		$$('#pagecontent *[id^=section]').each(function(el,i){
			new Element('span',{
				'class':'editsection'
			}).adopt( new Element('a', {
				'href' : url+i,
				'title' : 'quick.edit.title'.localize(el.getText())
				}).setHTML('quick.edit'.localize()) 
			).inject(el);
		});
	}
}

/** Class: Tabbed Section (130)
	Creates tabs, based on some css-class information
	Use in jspwiki: %%tabbedSection  %%tab-FirstTab .. %% %%

	Following markup:
	<div class="tabbedSection">
		<div class="tab-FirstTab">..<div>
		<div class="tab-SecondTab">..<div>
	</div>

	is changed into
	<div class="tabmenu"><span><a activetab>..</a></span>..</div>
	<div class="tabbedSection tabs">
		<div class="tab-firstTab ">
		<div class="tab-SecondTab hidetab">
	</div>
 **/
var TabbedSection = {

	onPageLoad: function(){

		// charge existing tabmenu's with click handlers
		$$('.tabmenu a').each(function(el){
			if(el.href) return;
			var tab = $(el.id.substr(5)); //drop 'menu-' prefix
			el.addEvent('click', this.clickTab.bind(tab) );
		},this);	
	
		// convert tabbedSections into tabmenu's with click handlers
		$$('.tabbedSection').each( function(tt){
			tt.addClass('tabs'); //css styling is on tabs
			var tabmenu = new Element('div',{'class':'tabmenu'}).injectBefore(tt);

			tt.getChildren().each(function(tab,i) {
				if( !tab.className.test('^tab-') ) return;

				if( !tab.id || (tab.id=="") ) tab.id = tab.className;
				var title = tab.className.substr(4).deCamelize(); //drop 'tab-' prefix

				(i==0) ? tab.removeClass('hidetab') : tab.addClass('hidetab');

				new Element('div',{'styles':{'clear':'both'}}).inject(tab);

				var menu = new Element('a', {
					'id':'menu-'+tab.id, 
					'events':{ 'click': this.clickTab.bind(tab)  }
				}).appendText(title).inject(tabmenu);
				if( i==0 ) menu.addClass('activetab');        
			},this);
		}, this);
	},

	clickTab: function(){
		var menu = $('menu-'+this.id);
		this.getParent().getChildren().some( function(el){
			if(el.id){
				var m = $('menu-'+el.id);
				if( m && m.hasClass('activetab') ) {
					if( el.id != this.id ) {
						m.removeClass('activetab');      
						menu.addClass('activetab');
						el.addClass('hidetab');
						this.removeClass('hidetab'); //.show();
					}
					return true;
				}
			}
			return false;
		},this);		
	}
	
}

/** 132 Accordion for Tabs, Accordeons, CollapseBoxes
 **
 ** Following markup:
 ** <div class="accordion">
 **		<div class="tab-FirstTab">...<div>
 **		<div class="tab-SecondTab">...<div>
 ** </div>
 **
 **	is changed into
 **	<div class="accordion">
 **		<div class="toggle active">First Tab</div>
 **		<div class="tab-FirstTab tab active">...</div>
 **		<div class="toggle">Second Tab</div>
 **		<div class="tab-SecondTab">...</div>
 **	</div>
 **/
var WikiAccordion = {

	onPageLoad: function(){
		$$('.accordion, .tabbedAccordion').each( function(tt){
			
			var toggles=[], contents=[], togglemenu=false;
			if(tt.hasClass('tabbedAccordion')) togglemenu = new Element('div',{'class':'togglemenu'}).injectBefore(tt);
			
			tt.getChildren().each(function(tab) {
				if( !tab.className.test('^tab-') ) return;

				//FIXME use class to make tabs visible during printing 
				//(i==0) ? tab.removeClass('hidetab'): tab.addClass('hidetab');

				var title = tab.className.substr(4).deCamelize();
				if(togglemenu) {
					toggles.push(new Element('div',{'class':'toggle'}).inject(togglemenu).appendText(title));
				} else {
					toggles.push(new Element('div',{'class':'toggle'}).injectBefore(tab).appendText(title));
				}        
				contents.push(tab.addClass('tab'));
			});
			new Accordion(toggles, contents, {     
				alwaysHide: !togglemenu,
				onComplete: function(){
					var el = $(this.elements[this.previous]);
					if (el.offsetHeight > 0) el.setStyle('height', 'auto');  
				},
				onActive: function(toggle,content){                          
					toggle.addClass('active'); 
					content.addClass('active').removeClass('xhidetab'); 
				},
				onBackground: function(toggle,content){ 
					content.setStyle('height', content['offsetHeight']);
					toggle.removeClass('active'); 
					content.removeClass('active').addClass('xhidetab');
				} 
			});
		});
	}
}


/* 140 SearchBox
 * FIXME: remember 10 most recent search topics (cookie based)
 * Extended with quick links for view, edit and clone (ref. idea of Ron Howard - Nov 05)
 * Refactored for mootools, April 07
 */
var SearchBox = {

	onPageLoad: function(){
		this.onPageLoadQuickSearch();
		this.onPageLoadFullSearch();
	},

	onPageLoadQuickSearch : function(){
		var q = $('query'); if( !q ) return;
		this.query = q; 
		q.observe(this.ajaxQuickSearch.bind(this) ); 

		this.hover = $('searchboxMenu').setProperty('visibility','visible')
			.effect('opacity', {wait:false}).set(0);
	
		$(q.form).addEvent('submit',this.submit.bind(this))
			//FIXME .addEvent('blur',function(){ this.hasfocus=false; this.hover.start(0) }.bind(this))
			//FIXME .addEvent('focus',function(){ this.hasfocus=true; this.hover.start(0.9) }.bind(this))
			  .addEvent('mouseout',function(){ this.hover.start(0) }.bind(this))
			  .addEvent('mouseover',function(){ Wiki.locatemenu(this.query, $('searchboxMenu') ); this.hover.start(0.9) }.bind(this));
		
		/* use advanced search-input on safari - experimental */
		if(window.xwebkit){
			q.setProperties({type:"search",autosave:q.form.action,results:"9",placeholder:q.defaultValue});
		} else {
			$('recentClear').addEvent('click', this.clear.bind(this));

			this.recent = Wiki.prefs.get('RecentSearch'); if(!this.recent) return;

			var ul = new Element('ul',{'id':'recentItems'}).inject($('recentSearches').show());
			this.recent.each(function(el){
				new Element('a',{
					'href':'#', 
					'events': {'click':function(){ q.value = el; q.form.submit(); }}
					}).setHTML(el).inject( new Element('li').inject(ul) );
			});
		}
	},

	onPageLoadFullSearch : function(){
		var q2 = $("query2"); if( !q2 ) return;
		this.query2 = q2;
		
		var changescope = function(){
			var qq = this.query2.value.replace(/^(?:author:|name:|contents:|attachment:)/,'');
			this.query2.value = $('scope').getValue() + qq;
			this.runfullsearch();
		}.bind(this);
		
		q2.observe( this.runfullsearch.bind(this) );
		
		$('scope').addEvent('change', changescope);
		$('details').addEvent('click', this.runfullsearch.bind(this));
	},

	runfullsearch : function(){
		var q2 = this.query2.value;
		if( !q2 || (q2.trim()=='')) { 
			$('searchResult2').empty();
			return; 
		}
		$('spin').show();

		var scope = $('scope'), 
			match= q2.match(/^(?:author:|name:|contents:|attachment:)/) ||"";
		$each(scope.options, function(option){
			if (option.value == match) option.selected = true;
		});

		new Ajax(Wiki.TemplateUrl+'AJAXSearch.jsp', {
			postBody: $('searchform2').toQueryString(),
			update: 'searchResult2', 
			method: 'post',
			onComplete: function() { 
				$('spin').hide(); 
				GraphBar.onPageLoad(); 
				Wiki.prefs.set('PrevQuery', q2); 
			} 
		}).request();
	},

	submit: function(){ 
		var v = this.query.value;
		if( v == this.query.defaultValue) this.query.value = '';
		if( !this.recent ) this.recent=[];
		if( !this.recent.test(v) ){
			if(this.recent.length > 9) this.recent.pop();
			this.recent.unshift(v);
			Wiki.prefs.set('RecentSearch', this.recent);
		}
	},

	clear: function(){		
		this.recent = [];
		Wiki.prefs.remove('RecentSearch');
		$('recentSearches','recentClear').hide();
	},

	ajaxQuickSearch: function(){
		var qv = this.query.value ;
		if( (qv==null) || (qv.trim()=="") || (qv==this.query.defaultValue) ) {
			$('searchOutput').empty();
			return;
		}
		$('searchTarget').setHTML('('+qv+') :');
		$('searchSpin').show();

		Wiki.jsonrpc('search.findPages', [qv,20], function(result){
				$('searchSpin').hide(); 
				if(!result.list) return;
				var frag = new Element('ul');
				
				result.list.each(function(el){
					new Element('li').adopt( 
						new Element('a',{'href':Wiki.getUrl(el.map.page) }).setHTML(el.map.page), 
						new Element('span',{'class':'small'}).setHTML(" ("+el.map.score+")")
					).inject(frag);
				});
				$('searchOutput').empty().adopt(frag);
				Wiki.locatemenu( $('query'), $('searchboxMenu') );
		});
	} ,

	/* navigate to url, after smart pagename handling */
	navigate: function(url, promptText, clone, search){
		var p = Wiki.PageName, s = this.query.value;
		if(s == this.query.defaultValue) s = '';

		if(s == ''){
			s = prompt(promptText, (clone) ? p+'sbox.clone.suffix'.localize() : p);
			if( !s || (s == '') ) return false;
		}
		//if(!search) s = s.replace(/[^A-Za-z0-9._\/]/g, ''); //valid pagename FIXME
		if( clone && (s != p) )  s += '&clone=' + p;

		if(s == '') return false; //dont exec the click
		location.href = url.replace('__PAGEHERE__', s);
	}
}


/**
 ** 150 GraphBar Object: also used on the findpage
 ** %%graphBars ... %%
 ** convert numbers inside %%gBar ... %% tags to graphic horizontal bars
 ** no img needed.
 ** supported parameters: bar-color and bar-maxsize
 ** e.g. %%graphBars-e0e0e0 ... %%  use color #e0e0e0, default size 120
 ** e.g. %%graphBars-blue-red ... %%  blend colors from blue to red
 ** e.g. %%graphBars-red-40 ... %%  use color red, maxsize 40 chars
 ** e.g. %%graphBars-vertical ... %%  vertical bars
 ** e.g. %%graphBars-progress ... %%  progress bars in 2 colors
 ** e.g. %%graphBars-gauge ... %%  gauge bars in gradient colors
 **/

/* minimal variant of the Color class, inspired by mootools */
var Color = new Class({

	_HTMLColors: {
		black  :"000000", green :"008000", silver :"c0c0c0", lime  :"00ff00",
		gray   :"808080", olive :"808000", white  :"ffffff", yellow:"ffff00",
		maroon :"800000", navy  :"000080", red    :"ff0000", blue  :"0000ff",
		purple :"800080", teal  :"008080", fuchsia:"ff00ff", aqua  :"00ffff" 
	},
	
	initialize: function(color, type){
		type = type || (color.push ? 'rgb' : 'hex');
		if(this._HTMLColors[color]) color = this._HTMLColors[color];
		var rgb = (type=='rgb') ? color : color.toString().hexToRgb(true);
		if(!rgb) return false;
		rgb.hex = rgb.rgbToHex();
		return $extend(rgb, Color.prototype);
	},

	mix: function(){
		var colors = $A(arguments),
			rgb = this.copy(),
			alpha = (($type(colors[colors.length - 1]) == 'number') ? colors.pop() : 50)/100,
			alphaI = 1-alpha;
		
		colors.each(function(color){
			color = new Color(color);
			for (var i = 0; i < 3; i++) rgb[i] = Math.round((rgb[i] * alphaI) + (color[i] * alpha));
		});
		return new Color(rgb, 'rgb');
	},

	invert: function(){
		return new Color(this.map(function(value){
			return 255 - value;
		}));
	}

});

var GraphBar =
{
	onPageLoad : function(){
		$$('*[class^=graphBars]').each( function(g){
			var lbound = 20,	//max - lowerbound size of bar
				ubound = 320,	//min - upperbound size of bar
				vwidth = 20,	//vertical bar width
				color1 = null,	// bar color
				color2 = null,	// 2nd bar color used depending on bar-type
				isGauge = false,	// gauge bar
				isProgress = false,	// progress bar
				isHorizontal = true,// horizontal or vertical orientation
				parms = g.className.substr(9).split('-'),
				barName = parms.shift(); //first param is optional barName
			
			parms.each(function(p){
				p = p.toLowerCase();
				if(p == "vertical") { isHorizontal = false; }
				else if(p == "progress") { isProgress = true;  }
				else if(p == "gauge") { isGauge = true; }
				else if(p.indexOf("min") == 0) { lbound = p.substr(3).toInt(); }
				else if(p.indexOf("max") == 0) { ubound = p.substr(3).toInt(); }
				else if(p != "") {
					p = new Color(p,'hex'); if(!p.hex) return;
					if(!color1) color1 = p; 
					else if(!color2) color2 = p;
				}
			});
			if( !color2 && color1) color2 = (isGauge || isProgress) ? color1.invert() : color1;

			if( lbound > ubound ) { var m = ubound; ubound=lbound; ubound=m; }
			var size = ubound-lbound;

			var bars = $ES('.gBar'+barName, g); //collect all gBar elements
			if( (bars.length==0) && barName && (barName!="")){  // check table data
				bars = this.getTableValues(g, barName);
			}
			if( !bars ) return;

			var barData = this.parseBarData( bars, lbound, size ),
				border = (isHorizontal ? 'borderLeft' : 'borderBottom');

			bars.each(function(b,j){
				var bar1 = $H().set(border+'Width',barData[j]), 
					bar2 = $H(), // 2nd bar only valid ico 'progress' 
					barEL = new Element('span',{'class':'graphBar'}),
					pb = b.getParent(); // parent of gBar element

				if(isHorizontal){
					barEL.setHTML('x');
					if(isProgress){	
						bar2.extend(bar1.obj);
						bar1.set(border+'Width',ubound-barData[j]).set('marginLeft','-1ex'); 
					}					
				} else { // isVertical
					if(pb.getTag()=='td') { pb = new Element('div').injectWrapper(pb); }

					pb.setStyles({'height':ubound+b.getStyle('lineHeight').toInt(), 'position':'relative'});
					b.setStyle('position', 'relative'); //needed for inserted spans ;-)) hehe
					if( !isProgress ) { b.setStyle('top', (ubound-barData[j])); }

					bar1.extend({'position':'absolute', 'width':vwidth, 'bottom':'0'});
					if(isProgress){ 
						bar2.extend(bar1.obj).set(border+'Width', ubound); 
					}
				}
				if(isProgress){
					if(color1){ bar1.set('borderColor', color1.hex); }
					if(color2){ 
						bar2.set('borderColor', color2.hex); 
					} else { 
						bar1.set('borderColor', 'transparent');
					}
				} else if(color1){
					var percent = isGauge ? (barData[j]-lbound)/size : j/(bars.length-1);
					bar1.set('borderColor', color1.mix(color2, 100 * percent).hex);
				}
				
				if(bar2.length > 0){ barEL.clone().setStyles(bar2.obj).injectBefore(b); };
				if(bar1.length > 0){ barEL.setStyles(bar1.obj).injectBefore(b); };

			},this);

		},this);
	},

	// parse bar data types and scale according to lbound and size
	parseBarData: function(nodes, lbound, size){
		var barData=[], 
			maxValue=Number.MIN_VALUE, 
			minValue=Number.MAX_VALUE,
			num=date=true;
	
		nodes.each(function(n,i){
			var s = n.getText();
			barData.push(s);
			if(num) num = !isNaN(parseFloat( s.match(Number.REparsefloat) ) );
			if(date) date = !isNaN(Date.parse(s));
		});
		barData = barData.map(function(b){
			if(date)     { b = new Date(Date.parse(b) ).valueOf();  }
			else if(num) { b = parseFloat( b.match(Number.REparsefloat) ); }
			
			maxValue = Math.max(maxValue, b);
			minValue = Math.min(minValue, b);
			return b;
		});		

		if(maxValue==minValue) maxValue=minValue+1; /* avoid div by 0 */
		size = size/(maxValue-minValue);
		return barData.map(function(b){
			return ( (size*(b-minValue)) + lbound).toInt();
		});
	},

	/* Fetch set of gBar values from a table
	 * Check first-row to match field-name: return array with col values
	 * Check first-column to match field-name: return array with row values
	 * insert SPANs as place-holder of the missing gBars
	 */
	getTableValues: function(node, fieldName){
		var table = $E('table', node); if(!table) return false;
		var tlen = table.rows.length;

		if( tlen > 1 ){ /* check for COLUMN based table */
			var r = table.rows[0];
			for( var h=0; h < r.cells.length; h++ ){
				if( $getText( r.cells[h] ).trim() == fieldName ){
					var result = [];
					for( var i=1; i< tlen; i++)
						result.push( new Element('span').injectWrapper(table.rows[i].cells[h]) );
					return result;
				}
			}
		}
		for( var h=0; h < tlen; h++ ){  /* check for ROW based table */
			var r = table.rows[h];
			if( $getText( r.cells[0] ).trim() == fieldName ){
				var result = [];
				for( var i=1; i< r.cells.length; i++)
					result.push( new Element('span').injectWrapper(r.cells[i]) );
				return result;
			}
		}
		return false;
	}
}


/** 200 Collapsible list and boxes
 ** See also David Lindquist <first name><at><last name><dot><net>
 ** See: http://www.gazingus.org/html/DOM-Scripted_Lists_Revisited.html
 **
 ** Add support for collabsable boxes, Nov 05, D.Frederickx
 ** Refactored on mootools, including effects, May 07, D.Frederickx
 **/
var Collapsible =
{
	pims : [], // all me cookies

	onPageLoad: function(){
		this.bullet = new Element('div',{'class':'collapseBullet'}).setHTML('&bull;');
		this.initialise( "favorites",   "JSPWikiCollapseFavorites" );
		this.initialise( "pagecontent", "JSPWikiCollapse" + Wiki.PageName );
		this.initialise( "previewcontent", "JSPWikiCollapse" + Wiki.PageName );
		this.initialise( "info" );
	},

	initialise: function( page, cookie){
		page = $(page); if(!page) return;

		this.pims.push({
			'name':cookie,
			'value':'',
			'initial': (cookie ? Cookie.get(cookie) : "") 
		});
		$ES('.collapse', page).each(function(el){ 
			if( $E('.collapseBullet',el) ) return; /* no nesting */
			this.collapseNode(el); 
		}, this);
		$ES('.collapsebox,.collapsebox-closed', page).each(function(el){ 
			this.collapseBox(el); 
		}, this);	
	},

	collapseBox: function(el){
		var title = el.getFirst(); if( !title ) return;
		var body = new Element('div', {'class':'collapsebody'}), 
			bullet  = this.bullet.clone(),
			isclosed = el.hasClass('collapsebox-closed');
		while(title.nextSibling) body.appendChild(title.nextSibling); // wrap other siblings
		el.appendChild(body);

		if(isclosed) el.removeClass('collapsebox-closed').addClass('collapsebox');
		bullet.injectTop( title.addClass('collapsetitle') );
		this.newBullet(bullet, body, !isclosed, title );
	},

	// Modifies the list such that sublists can be hidden/shown by clicking the listitem bullet
	// The listitem bullet is a node inserted into the DOM tree as the first child of the
	// listitem containing the sublist.
	collapseNode: function(node){
		$ES('li',node).each(function(li){
			var ulol = $E('ul',li) || $E('ol',li);
			
			//dont insert bullet when LI is 'empty': no text or no non-ul/ol tags			
			var emptyLI = true;
			for( var n = li.firstChild; n ; n = n.nextSibling ) {
				if((n.nodeType == 3 ) && ( n.nodeValue.trim() == "" ) ) continue; //keep searching
				if((n.nodeName == "UL") || (n.nodeName == "OL")) break; //seems like an empty li
				emptyLI = false;
				break;
			}
			if( emptyLI ) return;
			
			new Element('div',{'class':'collapsebody'}).injectWrapper(li);
			var bullet = this.bullet.clone().injectTop(li);
			if(ulol) this.newBullet(bullet, ulol, (ulol.getTag()=='ul'));
		},this);
	},

	newBullet: function(bullet, body, isopen, clicktarget){
		var ck = this.pims.getLast(); //read cookie
		isopen = this.parseCookie(isopen);
		if(!clicktarget) clicktarget = bullet;

		var bodyfx = body.setStyle('overflow','hidden')
			.effect('height', { 
				wait:false,
				onStart:this.renderBullet.bind(bullet),
				onComplete:function(){ if(bullet.hasClass('collapseOpen')) body.setStyle('height','auto'); } 
			});

		bullet.className = (isopen ? 'collapseClose' : 'collapseOpen'); //ready for rendering
		clicktarget.addEvent('click', this.clickBullet.bind(bullet, [ck, ck.value.length-1, bodyfx]))
			.addEvent('mouseenter', function(){ clicktarget.addClass('collapseHover')} )
			.addEvent('mouseleave', function(){ clicktarget.removeClass('collapseHover')} );
			  
		bodyfx.fireEvent('onStart');
		if(!isopen) bodyfx.set(0); //.set( isopen ? body.scrollHeight : 0 );	
	},

	renderBullet: function(){
		if(this.hasClass('collapseClose')){
			this.setProperties({'title':'collapse'.localize(), 'class':'collapseOpen'}).setHTML('-'); /* &raquo; */
		} else {
			this.setProperties({'title':'expand'.localize(), 'class':'collapseClose'}).setHTML('+'); /* &laquo; */
		}
	},

	clickBullet: function( ck, bulletidx, bodyfx){
		var collapse = this.hasClass('collapseOpen'),
			bodyHeight = bodyfx.element.scrollHeight; 

		if(collapse) bodyfx.start(bodyHeight, 0); else bodyfx.start(bodyHeight);
		
		ck.value = ck.value.substring(0,bulletidx) + (collapse ? 'c' : 'o') + ck.value.substring(bulletidx+1) ;
		if(ck.name) Cookie.set(ck.name, ck.value, {path:Wiki.BasePath, duration:20});
	},

	// parse initial cookie versus actual document 
	// returns true if collapse status is open
	parseCookie: function( isopen ){
		var ck = this.pims.getLast(),
			cursor = ck.value.length,
			token = (isopen ? 'o' : 'c');

		if(ck.initial && (ck.initial.length > cursor)){
			var cookieToken = ck.initial.charAt( cursor );

			if(  ( isopen && (cookieToken == 'c') )
			  || ( !isopen && (cookieToken == 'o') ) ) token = cookieToken ;

			if(token != cookieToken) ck.initial = null; //mismatch with initial cookie
		}
		ck.value += token; //append and save currentcookie

		return(token == 'o');
	}
}


/** 220 RoundedCorners --experimental
 ** based on Nifty corners by Allesandro Fulciniti
 ** www.pro.html.it
 ** Refactored for JSPWiki
 **
 ** JSPWiki syntax:
 **
 **  %%roundedCorners-<corners>-<color>-<borderColor>
 **  %%
 **
 **  roundedCorners-yyyy-ffc5ff-c0c0c0
 **
 **  corners: "yyyy" where first y: top-left,    2nd y: top-right,
 **                           3rd y: bottom-left; 4th y: bottom-right
 **     value can be: "y": Normal rounded corner (lowercase y)
 **                    "s": Small rounded corner (lowercase s)
 **                    "n": Normal square corner
 **
 **/
var RoundedCorners =
{
	/** Definition of CORNER dimensions
	 ** Normal    Normal+Border  Small  Small+Border
	 ** .....+++  .....BBB       ..+++  ..BBB
	 ** ...+++++  ...BB+++       .++++  .B+++
	 ** ..++++++  ..B+++++       +++++  B++++
	 ** .+++++++  .B++++++
	 ** .+++++++  .B++++++
	 ** ++++++++  B+++++++
	 **
	 ** legend: . background, B border, + forground color
	 **/
	NormalTop :
		 [ { margin: "5px", height: "1px", borderSide: "0", borderTop: "1px" }
		 , { margin: "3px", height: "1px", borderSide: "2px" }
		 , { margin: "2px", height: "1px", borderSide: "1px" }
		 , { margin: "1px", height: "2px", borderSide: "1px" }
		 ] ,
	SmallTop :
		 [ { margin: "2px", height: "1px", borderSide: "0", borderTop: "1px" }
		 , { margin: "1px", height: "1px", borderSide: "1px" }
		 ] ,
	//NormalBottom: see onPageLoad()
	//SmallBottom: see onPageLoad()

	/**
	 ** Usage:
	 ** RoundedCorners.register( "#header", ['yyyy', '00f000', '32cd32'] );
	 **/
	registry: {},
	register: function( selector, parameters )
	{
		this.registry[selector] = parameters;
		return this;
	},

	onPageLoad: function()
	{
		/* make reverse copies for bottom definitions */
		this.NormalBottom = this.NormalTop.slice(0).reverse();
		this.SmallBottom  = this.SmallTop.slice(0).reverse();

		for( selector in this.registry )  // CHECK NEEDED
		{
			var n = $$(selector); 
			var parms = this.registry[selector];
			this.exec( n, parms[0], parms[1], parms[2], parms[3] );
		}

		$$('#pagecontent *[class^=roundedCorners]').each(function(el){ 
			var parms = el.className.split('-');
			if( parms.length < 2 ) return;
			this.exec( [el], parms[1], parms[2], parms[3], parms[4] );
		},this);
	},

	exec: function( nodes, corners, color, borderColor, background )
	{
		corners = ( corners ? corners+"nnnn": "yyyy" );
		color   = new Color(color,'hex') || 'transparent';
		if(borderColor) borderColor = new Color(borderColor);
		if(background)  background  = new Color(background);

		var c = corners.split('');
		/* [0]=top-left; [1]=top-right; [2]=bottom-left; [3]=bottom-right; */

		var nodeTop = null;
		var nodeBottom = null;

		if( c[0]+c[1] != "nn" )  //add top rounded corners
		{
			nodeTop = document.createElement("b") ;
			nodeTop.className = "roundedCorners" ;

			if( (c[0] == "y") || (c[1] == "y") )
			{
				this.addCorner( nodeTop, this.NormalTop, c[0], c[1], color, borderColor );
			}
			else if( (c[0] == "s") || (c[1] == "s") )
			{
				this.addCorner( nodeTop, this.SmallTop, c[0], c[1], color, borderColor );
			}
		}

		if( c[2]+c[3] != "nn" ) //add bottom rounded corners
		{
			nodeBottom = document.createElement("b");
			nodeBottom.className = "roundedCorners";

			if( (c[2] == "y") || (c[3] == "y") )
			{
				this.addCorner( nodeBottom, this.NormalBottom, c[2], c[3], color, borderColor );
			}
			else if( (c[2] == "s") || (c[3] == "s") )
			{
				this.addCorner( nodeBottom, this.SmallBottom, c[2], c[3], color, borderColor );
			}
		}

		if( (!nodeTop) && (!borderColor) && (!nodeBottom) ) return;

		for( var i=0; i<nodes.length; i++)
		{
			if( !nodes[i] ) continue;
			this.addBody( nodes[i], color, borderColor );
			if( nodeTop     )  nodes[i].insertBefore( nodeTop.cloneNode(true), nodes[i].firstChild );
			if( nodeBottom  )  nodes[i].appendChild( nodeBottom.cloneNode(true) );
		}
	},

	addCorner: function( node, arr, left, right, color, borderColor )
	{
		for( var i=0; i< arr.length; i++ )
		{
			var n =  document.createElement("div");
			n.style.height = arr[i].height;
			n.style.overflow = "hidden";
			n.style.borderWidth = "0";
			n.style.backgroundColor = color.hex;

			if( borderColor )
			{
				n.style.borderColor = borderColor.hex;
				n.style.borderStyle = "solid";
				if(arr[i].borderTop)
				{
					n.style.borderTopWidth = arr[i].borderTop;
					n.style.height = "0";
				}
			}

			if( left != 'n' ) n.style.marginLeft = arr[i].margin;
			if( right != 'n' ) n.style.marginRight = arr[i].margin;
			if( borderColor )
			{
				n.style.borderLeftWidth  = ( left  == 'n' ) ? "1px": arr[i].borderSide;
				n.style.borderRightWidth = ( right == 'n' ) ? "1px": arr[i].borderSide;
			}
			node.appendChild( n );
		}
	},

	// move all children of the node inside a DIV and set color and bordercolor
	addBody: function( node, color, borderColor)
	{
		if( node.passed ) return;

		var container = new Element('div').injectWrapper(node);

		container.style.padding = "0 4px";
		container.style.backgroundColor = color.hex;
		if( borderColor )
		{
			container.style.borderLeft  = "1px solid " + borderColor.hex;
			container.style.borderRight = "1px solid " + borderColor.hex;
		}

		node.passed=true;
	}
}


/** 230 Sortable -- Sort tables **/
//TODO cache table ok, cache datatype for each column
var Sortable =
{
	onPageLoad: function(){
		this.DefaultTitle = "sort.click".localize();
		this.AscendingTitle = "sort.ascending".localize();
		this.DescendingTitle = "sort.descending".localize();
		
		$$('.sortable table').each(function(table){
			if( table.rows.length < 2 ) return;

			$A(table.rows[0].cells).each(function(th){
				th=$(th);
				if( th.getTag() != 'th' ) return;
				th.addEvent('click', function(){ Sortable.sort(th); })
					.addClass('sort')
					.title=Sortable.DefaultTitle;
			});
		},this);
	},

	sort: function(th){
		var table = getAncestorByTagName(th, "table" ),
			filter = (table.filterStack),
			rows = (table.sortCache || []),
			colidx = 0, //target column to sort
			body = $T(table); 
		th = $(th);

		//todo add spinner while sorting
		//validate header row
		$A(body.rows[0].cells).each(function(thi, i){
			if(thi.getTag() != 'th') return;
			if(th == thi) { colidx=i; return; }
			thi.removeClass('sortAscending').removeClass('sortDescending')
				.addClass('sort').title = Sortable.DefaultTitle;
		});

		if(rows.length == 0){  //if data not yet cached
			$A(body.rows).each(function(r,i){
				if((i==0) || ((i==1) && (filter))) return;
				rows.push( r );
			});
		};		
		var datatype = Sortable.guessDataType(rows,colidx);

		//do the actual sorting
		if(th.hasClass('sort')){ 
			rows.sort( Sortable.createCompare(colidx, datatype) )
		}
		else rows.reverse(); 
		
		var fl=th.hasClass('sortDescending');
		th.removeClass('sort').removeClass('sortAscending').removeClass('sortDescending');
		th.addClass(fl ? 'sortAscending': 'sortDescending')
			.title= fl ? Sortable.DescendingTitle: Sortable.AscendingTitle;
		
		var frag = document.createDocumentFragment();
		rows.each( function(r,i){ frag.appendChild(r); });
		body.appendChild(frag);
		table.sortCache = rows;
		if(table.zebra) table.zebra();
	},

	guessDataType: function(rows, colidx){
		var num=date=ip4=euro=true;
		rows.each(function(r,i){
			var v = $getText(r.cells[colidx]).clean().toLowerCase();
			if(num)  num  = !isNaN(parseFloat(v));
			if(date) date = !isNaN(Date.parse(v));
			if(ip4)  ip4  = v.test("(?:\\d{1,3}\\.){3}\\d{1,3}");
			if(euro) euro = v.test("^[£$€][0-9.,]+");
		});
		return (euro) ? 'euro': (ip4) ? 'ip4': (date) ? 'date': (num) ? 'num': 'string';
	},

	convert: function(val, datatype){
		switch(datatype){
			case "num" : return parseFloat( val.match( Number.REparsefloat ) );
			case "euro": return parseFloat( val.replace(/[^0-9.,]/g,'') );
			case "date": return new Date( Date.parse( val ) );
			case "ip4" : 
				var octet = val.split( "." );
				return parseInt(octet[0]) * 1000000000 + parseInt(octet[1]) * 1000000 + parseInt(octet[2]) * 1000 + parseInt(octet[3]);
			default    : return val.toString().toLowerCase();
		}
	},

	createCompare: function(i, datatype) {
		return function(row1, row2) {
			var val1 = Sortable.convert( $getText(row1.cells[i]), datatype );
			var val2 = Sortable.convert( $getText(row2.cells[i]), datatype );

			if(val1<val2){ return -1 } else if(val1>val2){ return 1 } else return 0;
		}
	}
}

/** 240 table-filters 
 ** inspired by http://www.codeproject.com/jscript/filter.asp
 **/
var TableFilter =
{
	onPageLoad: function(){
		this.All = "filter.all".localize();
		this.FilterRow = 1; //row number of filter dropdowns
		
		$$('.table-filter table').each( function(table){
			if( table.rows.length < 2 ) return;

			/*
			$A(table.rows[0].cells).each(function(e,i){
				var s = new Element('select',{ 
					'events': { 
						'click':function(event){ event.stop(); }.bindWithEvent(), 
						'change':TableFilter.filter 
					} 
				});
				s.fcol = i; //store index
				e.adopt(s);	        
			},this);
			*/
			
			var r = $(table.insertRow(TableFilter.FilterRow)).addClass('filterrow');
			for(var j=0; j < table.rows[0].cells.length; j++ ){
				var s = new Element('select',{ 
					'events': { 
						'change':TableFilter.filter 
					} 
				});
				s.fcol = j; //store index
				
				new Element('th').adopt(s).inject(r);
			}
			table.filterStack = [];
			TableFilter.buildEmptyFilters(table);
		});
	},

	buildEmptyFilters: function(table){
		for(var i=0; i < table.rows[0].cells.length; i++){
			var ff = table.filterStack.some(function(f){ return f.fcol==i });
			if(!ff) TableFilter.buildFilter(table, i);
		}
		if(table.zebra) table.zebra();			
	},

	// this function initialises a column dropdown filter
	buildFilter: function(table, col, selectedValue){
		// Get a reference to the dropdownbox.
		var select = table.rows[TableFilter.FilterRow].cells[col].firstChild;
		//var select = $(table.rows[0].cells[col]).getLast();
		if(!select) return; //empty dropdown
		select.options.length = 0;

		var rows=[];
		$A(table.rows).each(function(r,i){
			if((i==0) || (i==TableFilter.FilterRow)) return;
			if(r.style.display == 'none') return;
			rows.push( r );
		});
		rows.sort(Sortable.createCompare(col, Sortable.guessDataType(rows,col)));

		//add only unique strings to the dropdownbox
		select.options[0]= new Option(this.All, this.All);
		var value;
		rows.each(function(r,i){
			var v = $getText(r.cells[col]).clean().toLowerCase();
			if(v == value) return;
			value = v;
			//if(v.length > 32) v = v.substr(0,32)+ "...";
			//select.options[select.options.length] = new Option(v, value);
			select.options[select.options.length] = new Option(v.trunc(32), value);
		});
		(select.options.length <= 2) ? select.hide() : select.show();
		if(selectedValue != undefined) {
			select.value = selectedValue;
		} else {
			select.options[0].selected = true;
		}
	},

	filter: function(){ //onchange handler of filter dropdowns
		var col   = this.fcol,
			value = this.value,
			table = getAncestorByTagName(this, 'table');
		if( !table || table.style.display == 'none') return;

		// First check if the column is allready in the filter.
		if(table.filterStack.every(function(f,i){
			if(f.fcol != col) return true;
			if(value == TableFilter.All) table.filterStack.splice(i, 1);
			else f.fValue = value;
			return false;
		}) ) table.filterStack.push( {fValue:value, fcol:col} );

		$A(table.rows).each(function(r,i){ //show all
			r.style.display='';
		});

		table.filterStack.each(function(f){ //now filter the right rows
			var v = f.fValue, c = f.fcol;
			TableFilter.buildFilter(table, c, v);

			var j=0;
			$A(table.rows).each(function(r,i){
				if((i==0) || (i==TableFilter.FilterRow)) return;
				if(v != $getText(r.cells[c]).clean().toLowerCase()) r.style.display = 'none';
			});
		});
		TableFilter.buildEmptyFilters(table); //fill remaining dropdowns
	}
}


/** 250 Categories: turn wikipage link into AJAXed popup **/
var Categories =
{
	onPageLoad: function (){
		this.jsp = Wiki.TemplateUrl + '/AJAXCategories.jsp';

		$$('.category a.wikipage').each(function(link){
			var page = Wiki.getPageName(link.href); if(!page) return;
			var wrap = new Element('span').injectBefore(link).adopt(link),
				popup = new Element('div',{'class':'categoryPopup'}).inject(wrap),
				popfx = popup.effect('opacity',{wait:false}).set(0);

			link.addClass('categoryLink')
				.setProperties({ href:'#', title: "category.title".localize(page) })
				.addEvent('click', function(e){
				new Event(e).stop();  //dont jump to top of page ;-)
				new Ajax( Categories.jsp, { 
					postBody: '&page=' + page,
					update: popup,
					onComplete: function(){
						link.setProperty('title', '').removeEvent('click');
						wrap.addEvent('mouseover', function(e){ popfx.start(0.9); })
							.addEvent('mouseout', function(e){ popfx.start(0); });
						popup.setStyle('left', link.getPosition().x);
						popfx.start(0.9); 
					}
				}).request();
			});
		});
	} 
}

/**
 ** 260 Wiki Tips: 
 **/
var WikiTips =
{
	onPageLoad: function() {    
		var tips = [];
		$$('*[class^=tip]').each( function(t){
			var parms = t.className.split('-');
			if( parms.length<=0 || parms[0] != 'tip' ) return;
			t.className = "tip";

			var body = new Element('span').injectWrapper(t).hide(),
				caption = (parms[1]) ? parms[1].deCamelize(): "tip.default.title".localize();

			tips.push( 
				new Element('span',{
					'class': 'tip-anchor',
					'title': caption + '::' + body.innerHTML
				}).setHTML(caption).inject(t)
			);
		});
		if( tips.length>0 ) new Tips( tips , {'className':'tip'} );
	}
}


/**
 ** 270 Wiki Columns
 ** Dirk Frederickx, Mar 07
 **/
var WikiColumns =
{
	onPageLoad: function() {    
		var tips = [];
		$$('*[class^=columns]').each( function(t){
			var parms = t.className.split('-');
			t.className='columns';
			WikiColumns.buildColumns(t, parms[1] || 'auto');
		});
	},

	buildColumns: function( el, width){
		var breaks = $ES('hr',el);
		if(!breaks || breaks.length==0) return;

		var colCount = breaks.length+1;
		width = (width=='auto') ? 98/colCount+'%' : width/colCount+'px';

		var colDef = new Element('div',{'class':'col','styles':{'width':width}}),
			col = colDef.clone().injectBefore(el.getFirst()),
			n;
		while(n = col.nextSibling){
			if(n.tagName && n.tagName.toLowerCase() == 'hr'){
				col = colDef.clone();
				$(n).replaceWith(col);
				continue;
			}
			col.appendChild(n);
		}
		new Element('div',{'styles':{'clear':'both'}}).inject(el);
	}
}

/** 280 ZebraTable
 ** Color odd/even rows of table differently
 ** 1) odd rows get css class odd (ref. jspwiki.css )
 **   %%zebra-table ... %%
 **
 ** 2) odd rows get css style='background=<color>'
 ** %%zebra-<odd-color> ... %%
 **
 ** 3) odd rows get odd-color, even rows get even-color
 ** %%zebra-<odd-color>-<even-color> ... %%
 **
 ** colors are specified in HEX (without #) format or html color names (red, lime, ...)
 **/
var ZebraTable = {

	onPageLoad: function(){
		$$('*[class^=zebra]').each(function(z){
			var parms = z.className.split('-'), 
				isDefault = parms[1].test('table'),
				c1 = '', 
				c2 = '';
			if(parms[1]) c1= new Color(parms[1],'hex');
			if(parms[2]) c2= new Color(parms[2],'hex');
			$ES('table',z).each(function(t){
				t.zebra = this.zebrafy.pass([isDefault, c1,c2],t);
				t.zebra();
			},this);
		},this);
	},
	zebrafy: function(isDefault, c1,c2){
		var j=0;
		$A($T(this).rows).each(function(r,i){
			if(i==0 || (r.style.display=='none')) return;
			if(isDefault) (j++ % 2 == 0) ? $(r).addClass('odd') : $(r).removeClass('odd');
			else $(r).setStyle('background-color', (j++ % 2 == 0) ? c1 : c2 );
		});
	}
}


/** Highlight Word
 ** Inspired by http://www.kryogenix.org/code/browser/searchhi/
 ** Modified 21006 to fix query string parsing and add case insensitivity
 ** Modified 20030227 by sgala@hisitech.com to skip words
 **                   with "-" and cut %2B (+) preceding pages
 ** Refactored for JSPWiki -- now based on regexp, by D.Frederickx. Nov 2005
 **/
var HighlightWord =
{
	ReQuery: new RegExp( "(?:\\?|&)(?:q|query)=([^&]*)", "g" ),

	onPageLoad: function (){
		var q = Wiki.prefs.get('PrevQuery'); Wiki.prefs.set('PrevQuery', '');
		if( !q && this.ReQuery.test(document.referrer)) q = RegExp.$1; 
		if( !q ) return;

		var words = decodeURIComponent(q);
		words = words.replace( /\+/g, " " );
		words = words.replace( /\s+-\S+/g, "" );
		words = words.replace( /([\(\[\{\\\^\$\|\)\?\*\.\+])/g, "\\$1" ); //escape metachars
		words = words.trim().split(/\s+/).join("|");
		this.reMatch = new RegExp( "(" + words + ")" , "gi");

		this.walkDomTree( $("pagecontent") );
	},

	// recursive tree walk matching all text nodes
	walkDomTree: function( node )
	{
		if( !node ) return; /* bugfix */
		var nn = null;
		for( var n = node.firstChild; n ; n = nn ) {
			nn = n. nextSibling; /* prefetch nextSibling cause the tree will be modified */
			this.walkDomTree( n );
		}
		// continue on text-nodes, not yet highlighted, with a word match
		if( node.nodeType != 3 ) return;
		if( node.parentNode.className == "searchword" ) return;
		var s = node.innerText || node.textContent || '';
		if( !this.reMatch.test( s ) ) return;
		var tmp = new Element('span').setHTML(s.replace(this.reMatch,"<span class='searchword'>$1</span>"));

		var f = document.createDocumentFragment();
		while( tmp.firstChild ) f.appendChild( tmp.firstChild );

		node.parentNode.replaceChild( f, node );
	}
}


/* 300 Javascript Code Prettifier
 * based on http://google-code-prettify.googlecode.com/svn/trunk/README.html
 */
var WikiPrettify = {
	onPageLoad: function(){
		var els = $$('.prettify pre, .prettify code'); if(!els) return;

		//TODO: load assets .css and .js
		els.addClass('prettyprint');
		prettyPrint();
	}
}


window.addEvent('load', function(){

	Wiki.onPageLoad();
	WikiReflection.onPageLoad(); //before accordion cause impacts height!
	WikiAccordion.onPageLoad();

	TabbedSection.onPageLoad(); //after coordion or safari
	QuickLinks.onPageLoad();
	
	//GoogleChart.onPageLoad();

	//console.profile();
	Collapsible.onPageLoad();
	WikiSlidingFavs.onPageLoad();
	//console.profileEnd();

	SearchBox.onPageLoad();
	Sortable.onPageLoad();
	TableFilter.onPageLoad();
	RoundedCorners.onPageLoad();
	ZebraTable.onPageLoad();
	HighlightWord.onPageLoad();
	GraphBar.onPageLoad();
	Categories.onPageLoad();

	WikiSlimbox.onPageLoad();
	//WikiCoverflow.onPageLoad();
	WikiTips.onPageLoad();
	WikiColumns.onPageLoad();
	WikiPrettify.onPageLoad();

	Wiki.setFocus();
});