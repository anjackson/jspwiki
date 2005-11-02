// 004. Zebra tables  DF / May 2004
// %%zebra-table
//
var zebraCnt ;

/**
 ** Wiki functions
 **/
var Wiki = new Object();
Wiki.DELIM = "\u00A4"; //non-typable char - used as delimitter

Wiki.reImageTypes = new RegExp( '(.bmp|.gif|.png|.jpg|.jpeg|.tiff)$','i' );
Wiki.showImage = function( attachment, attDELIM, maxWidth, maxHeight )
{
  // contains Name, Link-url, Info-url 
  var attachArr = attachment.value.split( attDELIM );
  var attachImg  = document.getElementById("attachImg");

  if( !attachImg ) return true;

  if( attachArr.length == 1 ) //no image selected
  {
    return;
  }

  //not clean: should actually be read from the attachImg size - but dont know yet how xbrowser
  this.maxWidth = maxWidth;
  this.maxHeight = maxHeight;

  if( !this.reImageTypes.test( attachArr[0] ) ) 
  { 
    attachImg.innerHTML  = "No image selected"; 
    return;
  }  

  this.pic = new Image();
  this.pic.src = attachArr[1];
  if( this.pic.complete ) return Wiki.showLoadedImage() ; 

  this.countdown = 30;
  setTimeout( "Wiki.showLoadedImage()" , 200 ); 
  attachImg.innerHTML= "Loading image";
}

Wiki.showLoadedImage = function ()
{
  var attachImg  = document.getElementById("attachImg");

  if( this.pic.complete ) 
  { 
    var w = parseInt(this.pic.width);
    var h = parseInt(this.pic.height);

    if( w > this.maxWidth  ) { h *= this.maxWidth/w;  w = this.maxWidth; }
    if( h > this.maxHeight ) { w *= this.maxHeight/h; h = this.maxHeight; }
    attachImg.innerHTML = "<img src='" + this.pic.src + "' width='" + parseInt(w) + "' height='" + parseInt(h)
                        + "' style='margin-top:"+ parseInt((this.maxHeight-h)/2) +"px;' ></img>";
    this.countdown = 0; 
    this.pic = null;
    return;  
  }

  if( this.countdown <= 0 ) 
  {  
    attachImg.innerHTML = "Loading image expired<br />Try loading the image manually";
    return;
  } 

  this.countdown--;
  setTimeout( "Wiki.showLoadedImage()" , 200) ; 
  attachImg.innerHTML = "Loading image " + this.countdown
}

// called after loading the page
function validateZebraTable()
{
  if (!document.createElement) return;

  // find a <div class="zebra-table"> element
  var divArr = document.getElementsByTagName("div");
  if (! divArr) return; 

  for (var i=0; i<divArr.length; i++) 
  {
    if ( divArr[i].className == "zebra-table" )
    {
      zebraCnt = 0;
      validateZebraTableNode(divArr[i]);   
    } 
  }  

}

// returns an array of elements matching the classname
// returns only the first element matching the classname when matchFirst is true
// returns null when nothing found
function getElementsByClassName( node, clazz, matchFirst )
{
  var result = [];
  var re;
  if( clazz instanceof RegExp) { re = clazz; }
  else if( typeof clazz == 'string' ) { re = new RegExp ('(?:^| )'+clazz+'(?: |$)'); }
  else return null;

  var n = (node.all) ? node.all : node.getElementsByTagName("*");
  for( var i=0; i<n.length; i++ )
  {
    if( re.test(n[i].className) )
    {
      if( matchFirst ) return(n[i]);
      result.push(n[i]);
    }
  }
  return ( (result.length==0) ? null : result );
}

function getCookie( name )
{
  var reMatchCookie = new RegExp ( "(?:; )?" + name + "=([^;]*);?" );
  return( reMatchCookie.test( document.cookie ) ? decodeURIComponent(RegExp.$1) : null );
}

function setCookie( name, value, expires, path, domain, secure )
{
  var c = name + "=" + encodeURIComponent( value );
  if( expires ) { c += "; expires=" + expires.toGMTString(); } // Date()
  if( path    ) { c += "; path=" + path; }
  if( domain  ) { c += "; domain=" + domain; }
  if( secure  ) { c += "; secure"; } //true = only via https
  //alert("cookie: "+c);
  document.cookie = c;
}

function validateZebraTableNode(node)
{
  if ( node.nodeName == "TR") 
  {
     zebraCnt++;
     if (zebraCnt % 2 == 1) node.className = "odd";
  }
  
  if (node.hasChildNodes) 
  {
    for (var i=0; i<node.childNodes.length; i++) 
    { 
      validateZebraTableNode(node.childNodes[i]);
    }
  }

}

// Select skin
function skinSelect(skin)
{
  //var skin = document.forms["skinForm"].skinSelector;
  if (! skin) return; 

  for (var i=0; i<skin.length; i++)
  {
    if ( skin[i].selected )
    {
      document.cookie = "JspWikiSkin=" + skin[i].value + "#skin#" ;
    }
  }
  location.reload(); /* reload page */
}

/**
 ** Tabbed Section
 **/
var TabbedSection = new Object();
TabbedSection.reMatchTabs = new RegExp( "(?:^| )tab-(\\S+)" );
TabbedSection.onPageLoad = function()
{
  var t = getElementsByClassName( "tabbedSection" );
  if( !t ) return;

  for( var i = 0; i<t.length; i++)
  {
    if( !t[i].hasChildNodes ) continue; //take next section

    t[i].className = t[i].className +" tabs";
    var tabmenu = [];
    var active = true; //first tab assumed to be the active one
    for( var n = t[i].firstChild; n ; n = n.nextSibling )
    {
      if( !this.reMatchTabs.test( n.className ) ) continue; // not a tab: take next element

      if ( (n.id==null) || (n.id=="") ) n.id = n.className;
      n.style.display = ( active ? "" : "none" );

      /* <span><a class="active" href="#" id="menu-tabID"
                 onclick="TabbedSection.onclick('tabID')" >xyz</a></span>
      */
      tabmenu.push( "<span><a class='" + ( active ? "activetab" : "" ) + "' " );
      tabmenu.push( "id='menu-" + n.id + "'" );
      tabmenu.push( "onclick='TabbedSection.onclick(\"" + n.id + "\")' >" );
      tabmenu.push( RegExp.$1.deCamelize() + "</a></span>" );
      active=false;
    }
    if( tabmenu.length == 0 ) continue; //take next section
    var e = document.createElement( "div" );
    e.className = "tabmenu" ;
    e.innerHTML = tabmenu.join( "" );
    t[i].parentNode.insertBefore( e, t[i] );

  } // take next section

}

TabbedSection.onclick = function ( tabId )
{
  var target = document.getElementById( tabId );
  var section = target.parentNode;

  for( var n = section.firstChild; n ; n = n.nextSibling )
  {
    var m = document.getElementById( "menu-" + n.id );
    if( m && m.className == "activetab" )
    {
      n.style.display = "none";
      m.className = "";
      target.style.display = "";
      document.getElementById( "menu-" + target.id ).className = "activetab";
      break;
    }
  }
  return false;
}

/*
 * Edit Find & Replace functionality
 */
Wiki.editReplace = function(form, dataField)
{
  if( !form ) return;
  var findText    = form.findText.value; if( findText == "") return;
  var replaceText = form.replaceText.value;
  var isRegExp    = form.regExp.checked;
  var reGlobal    = ((form.global.checked) ? "g" : "") ;
  var reMatchCase = ((form.matchCase.checked) ? "" : "i") ;
  var data = dataField.value;

  if( !isRegExp ) /* escape all special re characters */
  {
    var re = new RegExp( "([\.\*\\\?\+\[\^\$])", "gi");
    findText = findText.replace(re,"\\$1");
  }
  
  var re = new RegExp(findText, reGlobal+reMatchCase+"m"); //multiline
  if( !re.exec(data) )
  {
    alert("No match found!");
    return(true);
  } 
    
  data = data.replace(re,replaceText);  

  form.undoMemory.value = dataField.value; 
  var undoButton = document.getElementById("undoHideOrShow"); //!! to elements now
  undoButton.style.visibility = "visible";
  dataField.value = data;    
  if( dataField.onchange ) dataField.onchange();
  
  return(true);
}

Wiki.editUndo = function(form, dataField)
{
  var undoButton = document.getElementById("undoHideOrShow");
  if( undoButton.style.visibility == "hidden") return(true);
  undoButton.style.visibility = "hidden";
  dataField.value = form.undoMemory.value;
  if( dataField.onchange ) dataField.onchange();
  form.undoMemory.value = ""; 
  return(true);
}

/**
 ** Search stuff
 ** 301 Remember 10 most recent search topics
 **     Uses a cookie to store to 10 most recent search topics
 **/
var SearchBox = new Object();

SearchBox.submit = function ( queryValue )
{
  for(var i=0; i < this.recentSearches.length; i++)
  {
    if( this.recentSearches[i] == queryValue ) return;
  }

  if( !this.recentSearches ) this.recentSearches = new Array();
  if( this.recentSearches.length > 9 ) this.recentSearches.pop();
  this.recentSearches.unshift( queryValue );

  setCookie( "JSPWikiSearchBox", this.recentSearches.join( Wiki.DELIM) );
}


SearchBox.onPageLoad = function()
{
  this.searchForm = document.getElementById("searchForm");
  if( !this.searchForm ) return;

  this.recentSearchesDIV = document.getElementById("recentSearches");
  if( !this.recentSearchesDIV ) return;

  this.recentSearches = new Array();
  var c = getCookie( "JSPWikiSearchBox" );
  if( c ) this.recentSearches = c.split( Wiki.DELIM );

  var s = "";
  if( this.recentSearches.length == 0 ) return;

  var div1 = "<div onclick='SearchBox.doSearch(this)'>";
  var div2 = "</div>";
  var s = "Recent Searches: ";
  s += div1 + this.recentSearches.join( div2+div1 ) + div2;
  s += "<br /><div onclick='SearchBox.clearRecentSearches()'>Clear Recent Searches</div>";
  this.recentSearchesDIV.innerHTML = s;

}

SearchBox.doSearch = function ( searchDiv )
{
  this.searchForm.query.value = searchDiv.innerHTML; //nodeValue seems not to work
  this.searchForm.submit();
}

SearchBox.clearRecentSearches = function()
{
  setCookie( "JSPWikiSearchBox", "" );
  this.recentSearches = new Array();
  this.recentSearchesDIV.innerHTML = "";
}


function runOnLoad()
{ 
  TabbedSection.onPageLoad();
  SearchBox.onPageLoad();
  
  validateZebraTable();
  googleSearchHighlight();
}

window.onload = runOnLoad;
