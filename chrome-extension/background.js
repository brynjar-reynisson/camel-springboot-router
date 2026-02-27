chrome.runtime.onMessage.addListener(
  function(request, sender, sendResponse) {
    if (request.type === "sendDocument") {
      console.log("Message from content script, source: " + request.source)
      //TODO, post to camel webservice
      const postResponse = fetch("http://localhost:8080/addContent", {
        method: "POST",
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(request),
      });
      console.log(postResponse);
      sendResponse(postResponse);
    }
  }
);