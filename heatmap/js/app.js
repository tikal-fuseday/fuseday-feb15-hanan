function Application(lat, lng) {

    var serverURL = "http://192.168.1.85:8080/ci"

    var center = {
        lat: lat || 32,
        lng: lng || 32
    };

    var speed = 5000;

    var map;
    var heatMap;
    var when = 0;
    var loop;

    function request() {
        var mapBounds = map.getBounds();
        var requestData = {
            cb: Math.random(),
            ts: ((new Date().valueOf() - when )/ 5000).toFixed(0),
            lat2: mapBounds.getNorthEast().lat(),
            lat1: mapBounds.getSouthWest().lat(),
            long2: mapBounds.getNorthEast().lng(),
            long1: mapBounds.getSouthWest().lng()
        };
        $.ajax({
            url: serverURL,
            data: requestData,
            cache: false,
            success: recieve,
            error: function(error) {
                recieve(null);
            }
            });
        $("#loadingContainer").fadeIn(100);
        if (heatMap && map) {
            heatMap.setMap(null);
        }
        clearInterval(loop);
    }

    function timer() {
        clearInterval(loop);
        loop = setInterval(request, speed);
    }

    function recieve(data) {
        timer();
        var count = 0;
        $("#loadingContainer").fadeOut(100);
        if (data !== null) {
            var heatmapData = [];
            for (var latLng in data) {
                var lat = latLng.split('"').join('').split(":")[0];
                var lng = latLng.split('"').join('').split(":")[1];

                lat = parseFloat(lat) * 0.1;
                lng = parseFloat(lng) * 0.1;

                var volume = parseFloat(data[latLng]);


                heatmapData.push({
                    location: new google.maps.LatLng(lat, lng),
                    weight: volume
                });
            }

            $("#hotspot-count").text(count);
            if (heatmapData.length == 0) return;

            var pointArray = new google.maps.MVCArray(heatmapData);

            heatmap = new google.maps.visualization.HeatmapLayer({
                data: pointArray
            });

            heatmap.setMap(map);
        }
    }

    function createMap() {
        var mapOptions = {
            center: new google.maps.LatLng(center.lat, center.lng),
            mapTypeId: google.maps.MapTypeId.SATELLITE,
            zoom: 8
        };
        map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);
        google.maps.event.addListener(map, 'idle', function() {
            request();
        });
    }

    function updateSpeedToggle() {
        if (speed == 1000) {
            $("#speed-normal").removeClass("active");
            $("#speed-fast").addClass("active");
        } else {
            $("#speed-normal").addClass("active");
            $("#speed-fast").removeClass("active");
        }
    }

    this.init = function init() {
        google.maps.event.addDomListener(window, 'load', createMap);
        updateSpeedToggle();

        $("#btn-ok").click(function() {
            when = parseInt($("#txtTime")[0].value) * 1000;
        });

        $("#txtTime").blur(function () {
            $(this)[0].value = Math.max( $(this)[0].value, $(this)[0].value * -1 );
        });

        $("#btn-now").click(function() {
            when = 0;
            $("#txtTime")[0].value = 0;
        });

        $("#speed-normal").click(function() {
            speed = 5000;
            updateSpeedToggle();
        });

        $("#speed-fast").click(function() {
            speed = 1000;
            updateSpeedToggle();
        });
    };
}

var APP = new Application();
APP.init();

