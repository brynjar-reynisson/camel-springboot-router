if(document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded',afterDOMLoaded);
} else {
    afterDOMLoaded();
}

function afterDOMLoaded(){
    (async () => {
      const response = await chrome.runtime.sendMessage({
        type: "sendDocument",
        source: document.location.href,
        name: document.location.href,
        content: JSON.stringify(document.body.innerHTML)
      });
      console.log(response);
    })();
}