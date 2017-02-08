var fov = (function() {
    return {
        flash: flashOnVisit
    };

    function flashOnVisit(baseUrl, channel) {
        if (!baseUrl.endsWith('/')) {
          baseUrl = baseUrl + '/';
        }
        url = baseUrl + 'channels/' + channel;

        var xmlHttp = new XMLHttpRequest();
        xmlHttp.onreadystatechange = function() {
            if (xmlHttp.readyState == 4 && xmlHttp.status == 200)
                callback(xmlHttp.responseText);
        }
        xmlHttp.open("GET", url, true); // true for asynchronous
        xmlHttp.send(null);
    }
})();
