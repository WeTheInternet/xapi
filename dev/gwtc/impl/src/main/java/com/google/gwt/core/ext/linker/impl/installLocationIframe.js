
var frameDoc;

function getInstallLocationDoc() {
  setupInstallLocation();
  return frameDoc;
}

function getInstallLocation() {
  setupInstallLocation();
  return frameDoc.getElementsByTagName('body')[0];
}

function setupInstallLocation() {
  if (frameDoc) { return; }
  // Create the script frame, making sure it's invisible, but not
  // "display:none", which keeps some browsers from running code in it.
  var scriptFrame = $doc.createElement('iframe');
  scriptFrame.src = 'javascript:""';
  scriptFrame.id = '__MODULE_NAME__';
  scriptFrame.sandbox = "allow-same-origin allow-scripts";
  scriptFrame.style.cssText = 'position:absolute; width:0; height:0; border:none; left: -1000px;'
    + ' top: -1000px;';
  scriptFrame.tabIndex = -1;
  $doc.body.appendChild(scriptFrame);

  frameDoc = scriptFrame.contentDocument;
  if (!frameDoc) {
    frameDoc = scriptFrame.contentWindow.document;
  }

  // The missing content has been seen on Safari 3 and firebug will
  // behave incorrectly on soft refresh unless we explicitly set the content
  // of the frame. However, we don't want to do this when runAsync calls
  // installCode, so we do it here when we create the iframe.
  frameDoc.open();
  var doctype = (document.compatMode == 'CSS1Compat') ? '<!doctype html>' : '';
  frameDoc.write(doctype + '<html><head></head><body></body></html>');
  frameDoc.close();
}
