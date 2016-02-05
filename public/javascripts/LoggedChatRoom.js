function LoggedRooms(serverAdress) {
    this.serverAdress = serverAdress;
    this.isConnected = false;
    this.mapCenter = new google.maps.LatLng(54.19265, 16.1779);
    this.markers = {};
    this.myMarker = null;
    this.choosenMarker = {marker: null}
    this.previousSelectedMarker = null;
    this.map = null;
    geocoder = new google.maps.Geocoder();
    this.connect();
    this.initialize();
}

LoggedRooms.prototype = {
    connect: function () {
        console.log("connect")
        this.ws = new WebSocket(this.serverAdress)
        this.ws.onopen = this.onConnect.bind(this);
        this.ws.onmessage = this.onMessage.bind(this);
        this.ws.onclose = this.onDisconnect.bind(this);
        this.ws.onerror = this.onError.bind(this);
    },

    onConnect: function () {
        this.isConnected = true;
    },

    onError: function (e) {
        console.log("On error" + e);
    },

    onDisconnect: function (e) {
        this.ws.close();
        this.isConnected = false;
        console.log("disconnect")
        console.log(e)
    },

    onMessage: function (evt) {
        var data = JSON.parse(evt.data);
        if (data.type === "AddedNewRoom") {
            var id = data.id;
            var title = data.title;
            var lat = data.lat;
            var lng = data.lng;
            var position = new google.maps.LatLng(lat, lng);
            this.addMarker(id, position, title);
        } 
        else if (data.type === "LoggedUserInfo") {
        	var result_holder = document.getElementById('myFacebook');
        	result_holder.innerHTML += '<img  style="vertical-align:middle;margin:10px; color:#000; margin-right:10px;border-radius: 25px" src=' + data.avatar + '>' + data.name + " TOTAL SCORE: " + data.score;
        }
        else if (data.type === "NewFriend") {
            var avatar = data.friend.avatar;
            var name = data.friend.name;
            var friendID = data.friend.friendID;
            var roomID = data.friend.roomID;
            this.addFriendToPage(friendID, name, avatar);
            this.changeFriendStatus(friendID, roomID);
        }
        else if (data.type === "FriendChangedRoom") {
            console.log("Przyszlo friendchangeroom")
            console.log(data)
            var friendID = data.friendID;
            var roomID = data.roomID;
            this.changeFriendStatus(friendID, roomID);
        }
    },

    sendWebSocket: function (object) {
        var objectToSend = JSON.stringify(object);
        console.log(objectToSend);
        this.ws.send(objectToSend);
    },

    toggleBounce: function (marker) {
        if (marker.getAnimation() !== null) {
            marker.setAnimation(null);
        } else {
            marker.setAnimation(google.maps.Animation.BOUNCE)

        }
    },

    clickMarkerFunction: function (marker) {
        if (this.choosenMarker.marker === null) {
            this.choosenMarker.marker = marker;
            this.choosenMarker.marker.setAnimation(google.maps.Animation.BOUNCE)
        } else if ((this.choosenMarker.marker.getAnimation() !== null) && (this.choosenMarker.marker.idRoom === marker.idRoom)) {
            this.choosenMarker.marker.setAnimation(null);
        } else if ((this.choosenMarker.marker.getAnimation() !== null) && (this.choosenMarker.marker.idRoom !== marker.idRoom)) {
            this.choosenMarker.marker.setAnimation(null);
            this.choosenMarker.marker = marker;
            this.choosenMarker.marker.setAnimation(google.maps.Animation.BOUNCE)
        } else if ((this.choosenMarker.marker.getAnimation() === null) && (this.choosenMarker.marker.idRoom === marker.idRoom)) {
            this.choosenMarker.marker.setAnimation(google.maps.Animation.BOUNCE)
        } else if ((this.choosenMarker.marker.getAnimation() === null) && (this.choosenMarker.marker.idRoom !== marker.idRoom)) {
            this.choosenMarker.marker = marker;
            this.choosenMarker.marker.setAnimation(google.maps.Animation.BOUNCE)
        }
        marker.infowindow.open(this.map, marker);
    },

    initialize: function ()  //function initializes Google map
    {
        var googleMapOptions =
        {
            center: this.mapCenter, // map center
            zoom: 4, //zoom level, 0 = earth view to higher value
            streetViewControl: false,
            panControl: true, //enable pan Control
            zoomControl: true, //enable zoom control
            zoomControlOptions: {
                style: google.maps.ZoomControlStyle.SMALL //zoom control size
            },
            scaleControl: true, // enable scale control
            mapTypeId: google.maps.MapTypeId.ROADMAP // google map type

        };
        this.map = new google.maps.Map(document.getElementById("map"), googleMapOptions);
    },

    addMarker: function (id, position, title) {
        var marker = new google.maps.Marker({
            position: position,
            map: this.map,
            draggable: false,
            title: title,
            icon: "http://maps.google.com/mapfiles/ms/micons/red.png"
        });
        marker.idRoom = id;
        
        this.markers[id] = marker;

        var contentString = '<p style="color:black;margin:0;padding:0">' + String(marker.title) + '</p>';
        marker.infowindow = new google.maps.InfoWindow({
            content: contentString
        });

        google.maps.event.addListener(marker, 'click', this.clickMarkerFunction.bind(this, marker));
    },

    addMyMarker: function () { //function that will add markers on button click
        var marker = new google.maps.Marker({
            position: this.mapCenter,
            map: this.map,
            draggable: true,
            animation: google.maps.Animation.DROP,
            title: "New room",
            icon: "http://maps.google.com/mapfiles/ms/micons/purple.png"
        });

        this.myMarker = marker;
        var total;
        google.maps.event.addListener(marker, "dragend", function () {
            var lat, lng, address;
            geocoder.geocode({'latLng': marker.getPosition()}, function (results, status) {
                if (status == google.maps.GeocoderStatus.OK) {
                    lat = marker.getPosition().lat();
                    lng = marker.getPosition().lng();
                    address = results[0].formatted_address;
                    total = ("<strong>\nAddress:</strong> " + address);
                    //return total;
                }
            });
            document.getElementById("RoomDescription").innerHTML = (total);
        });
    },

    deleteMyMarker: function () {
        this.myMarker.setMap(null);
    },

    confirmNewRoom: function () {
        if (this.myMarker != null) {
            var lat = this.myMarker.getPosition().lat();
            var lng = this.myMarker.getPosition().lng();
            var title = this.myMarker.getTitle();
            this.sendWebSocket({type: "AddNewRoom", lat: lat, lng: lng, title: title});
            this.deleteMyMarker()
        }
    },

    addNewFacebookFriend: function (userFacebookID, friendID) {
        this.sendWebSocket({type: "AddFacebookFriend", userFacebookID: userFacebookID, friendFacebookID: friendID});
    },

    addFriendToPage: function (friendID, name, avatar) {
        var resultFriend = '<div id = ' + friendID + ' style="margin:10px; color:#000;font-family:PT Sans, Helvetica, Arial, sans-serif"><img style="margin-right:10px;border-radius: 25px;vertical-align:middle" src=' + avatar + '>' + name + '<img style="float:right; margin-top:10px;" class = "statusImg"/></div><hr>';
        var result_holder_GameFriends = document.getElementById('gameFriends');
        result_holder_GameFriends.innerHTML += resultFriend;
    },
    
    showFriendRoomOnMap: function(roomID) {
    	if (this.markers.hasOwnProperty(roomID)) {
    		this.clickMarkerFunction(this.markers[roomID]);} 	
    },
    
    changeFriendStatus: function (friendID, roomID) {
    	var t = this;
    	elem = $("#gameFriends #" + friendID + " .statusImg")
        if (roomID === '0') {
            $("#gameFriends #" + friendID + " .statusImg").attr("src", "assets/img/status/google_map.png");
            elem.off('click');
        } else if (roomID == '' || roomID == "") {
            $("#gameFriends #" + friendID + " .statusImg").attr("src", "assets/img/status/open_door.jpg");
            elem.off('click');
        } else {
        	$("#gameFriends #" + friendID + " .statusImg").attr("src", "assets/img/status/game.png");
        	elem.click(function() {
        		t.showFriendRoomOnMap.call(t, roomID);
        	});  
        }

        //alert($("#gameFriends #" + friendID + " .statusImg").attr('src'));
    }
}