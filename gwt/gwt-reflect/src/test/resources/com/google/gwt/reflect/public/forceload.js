// Sometimes, in maven, junit compiles simply fail to start when a test would otherwise pass.
// This ensures that the document does not time-out, but may indicate that you have other errors.
setTimeout(function(){
  if (!document.readyState) {
	  document.readyState = 'complete';
	  console.log("Page did not load in under a second; check console log for details.\n");
  }
}, 1000);