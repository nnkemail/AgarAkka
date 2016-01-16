var filterStrength = 20;
var frameTime = 0, lastLoop = new Date, thisLoop;





function Client(client_name, serverAdr, idRoom) {
this.client_name      = client_name;
this.tick_counter      = 0;
this.inactive_interval = 0;
this.balls             = {};
this.my_balls          = [];
this.myPoints          = [];
this.score             = 0;
this.ballList          = [];
this.ballsIds           = [];
    this.leaderBoard    = []
    this.serverAdress = serverAdr
    this.idRoom = idRoom

//==================

this.ratio = 1;
this.screenRenderSize = 10;
//var cx= 0;
//var cy= 0;
this.px = 0;
this.py = 0;

//PLAYER

this.positionX = 250;
this.positionY = 100;

//PLAYER

this.xoffset = 0;
this.yoffset = 0;
//writeToScreen("doszlo do przed window")
this.screenWidth = window.innerWidth;
this.screenHeight = window.innerHeight;
//writeToScreen("doszlo do po window")
this.canvas = document.getElementById('cvs');
this.context = this.canvas.getContext('2d');
    this.lbCanvas = null //dodac ddrawimage
//CANVAS

//MOUSE
this.mouseX = 250;
this.mouseY = 100;

this.mouseX2 = 250;
this.mouseY2 = 100;

this.keySpace = false;
this.keyW = false;
this.isTyping = false;

this.timestamp = +new Date;

this.canvas.width = this.screenWidth; this.canvas.height = this.screenHeight;
}

Client.prototype = {
connect: function() {
writeToScreen("connect begin");
//this.ws            = new WebSocket(jsRoutes.controllers.Application.socket().webSocketURL())
//this.ws.binaryType = "arraybuffer";
this.ws            = new WebSocket(this.serverAdress)
this.ws.onopen     = this.onConnect.bind(this);
this.ws.onmessage  = this.onMessage.bind(this);
this.ws.onclose    = this.onDisconnect.bind(this);
//this.ws.onerror    = this.onError.bind(this);
//=====================
//TODO PRZENIESC
writeToScreen("connect end");
},

sendWebSocket: function (object) {
    this.ws.send(JSON.stringify(object));
},

reset: function () {
    this.mouseX2 = (this.mouseX - this.screenWidth / 2) / this.ratio + this.px;
    this.mouseY2 = (this.mouseY - this.screenHeight / 2) / this.ratio + this.py;
},

sendNewName: function (newName) {
	this.sendWebSocket({type: "Name", name: newName})
},

    initializeInputCapture: function() {
        window.onkeydown   = this.onKeyDown.bind(this);
        window.onkeyup     = this.onKeyUp.bind(this);
//this.canvas.addEventListener('mousemove', this.getMousePos, false);
        this.canvas.addEventListener('mousemove', this.getMousePos.bind(this), false);
    },

getMousePos: function (evt) {
    this.mouseX = evt.clientX;
    this.mouseY = evt.clientY;
    this.reset();
    this.sendWebSocket({type: "Coord", x: this.mouseX2, y: this.mouseY2});
    //writeToScreen("getMousePos");
//resetGlobalMouse()
//sendWebSocket({type: "Coord", x: x1, y: y1})
//writeToScreen(JSON.stringify({type: "Coord", x: x1, y: y1}));
},

onConnect: function() {
    var client = this;
    this.initializeInputCapture();
    this.sendWebSocket({type: "Name", name: this.client_name});
    this.sendWebSocket({type: "JoinRoom", idRoom: this.idRoom});
    this.animloop();

//=====================
    writeToScreen("onConnect");
},

onDisconnect: function() {
    this.ws.close();
//writeToScreen("onDisconnect");
},
    drawLeaderBoard: function () {
        this.lbCanvas = null;
        if (0 != this.leaderBoard.length) {
            this.lbCanvas = document.createElement("canvas");
            var ctx = this.lbCanvas.getContext("2d");
            var boardLength = 60;
            boardLength = boardLength + 24 * this.leaderBoard.length;
            //var scaleFactor = Math.min(0.22*canvasHeight, Math.min(200, .3 * canvasWidth)) / 200;
            var scaleFactor = Math.min(200, 0.3 * this.canvas.width) / 200;
            this.lbCanvas.width = 200 * scaleFactor;
            this.lbCanvas.height = boardLength * scaleFactor;


            ctx.scale(scaleFactor, scaleFactor);
            ctx.globalAlpha = .4;
            ctx.fillStyle = "#000000";
            ctx.fillRect(0, 0, 200, boardLength);

            ctx.globalAlpha = 1;
            ctx.fillStyle = "#FFFFFF";
            var c = "Leaderboard";
            ctx.font = "30px Ubuntu";
            ctx.fillText(c, 100 - ctx.measureText(c).width / 2, 40);
            var b;
            //writeToScreen("Po leadeboard napis")

            for (ctx.font = "20px Ubuntu", b = 0; b < this.leaderBoard.length; ++b) {
                c = this.leaderBoard[b].name || "An unnamed cell";
                //if (!showName) {
                //    (c = "An unnamed cell");
                //}
                if (-1 != this.my_balls.indexOf(this.leaderBoard[b].id)) {
                    if (this.balls.hasOwnProperty(this.my_balls[this.leaderBoard[b].id].id)) {
                        this.balls[this.my_balls[0].id].name && (c = this.balls[this.my_balls[0].id].name);
                    }
                    ctx.fillStyle = "#FFAAAA";
                    //if (!noRanking) {
                    c = b + 1 + ". " + c;
                    //}
                    ctx.fillText(c, 100 - ctx.measureText(c).width / 2, 70 + 24 * b);
                } else {
                    ctx.fillStyle = "#FFFFFF";
                    //if (!noRanking) {
                    c = b + 1 + ". " + c;
                    //}
                    ctx.fillText(c, 100 - ctx.measureText(c).width / 2, 70 + 24 * b);
                }
            }

        }
    },

onMessage: function(evt) {
    //writeToScreen("onMessage");
//context.save()
    var coordinate_x;
    var coordinate_y;
    var size;
    var r;
    var g;
    var b;
//writeToScreen("onMa")
    data = JSON.parse(evt.data);
    if (data.type === "modf") {
        //writeToScreen("modf");
        //writeToScreen(data.toRemoveData)
        for (var i = 0; i < data.toRemoveData.length; i++) {
            //var killedBall = this.balls[i]

            //writeToScreen(i)
            var id = data.toRemoveData[i];
            if (this.balls.hasOwnProperty(id))
                this.balls[id].destroy();
            //if (typeof this.balls[data.toRemoveData[i]] !== 'undefined')
            //	this.balls[i].destroy()
            //if (killedBall)
            //killedBall.destroy()
        }

        //writeToScreen("po remove")

        var toUpdate = data.toUpdateData;
        for (var j in toUpdate) {
            if (toUpdate.hasOwnProperty(j)) {
                var ballId = toUpdate[j].id;

                var ball;
                var colorstr = (toUpdate[j].R << 16 | toUpdate[j].G << 8 | toUpdate[j].B).toString(16);
                colorstr = '#' + ('000000' + colorstr).substr(-6);

                //writeToScreen("po color")
                //writeToScreen(ballId)
                if (this.balls.hasOwnProperty(ballId)) {
                    //writeToScreen("gas owb property ballid")
                    ball = this.balls[ballId];
                    ball.updatePos();
                    ball.old_x = ball.x;
                    ball.old_y = ball.y;
                    ball.old_size = ball.size;
                    //ball.color = colorstr;
                    ball.update();

                    ball.new_x = toUpdate[j].x;
                    ball.new_y = toUpdate[j].y;
                    ball.new_size = toUpdate[j].size;
                } else {
                    //writeToScreen("nre bsll")
                    ball = new Ball(this, toUpdate[j].id, toUpdate[j].x, toUpdate[j].y, toUpdate[j].size, toUpdate[j].eType, toUpdate[j].name);
                    //ball.setName("Player")
                    //ball.color = colorstr;
                    //build()
                    //writeToScreen("po new ball")
                    //nodelist.push(ball);
                }
                ball.color = colorstr;

                //ball.virus = (toUpdate[j].eType === 2)
                //build();
                //writeToScreen("po balls")

                //data.updateCode = key;
                //ball.update()

                //writeToScreen("po balls 2")
                //if (-1 != this.my_balls.indexOf(id)) {
                //    this.px = toUpdate[j].x
                //    this.py = toUpdate[j].y
                //}

            }
        }

        //writeToScreen("po update")
    } else if (data.type === 'myId') {
        //writeToScreen("Przyszlo mid")
        //writeToScreen(data.nId)
        this.my_balls.push(data.nId);
        if (this.balls.hasOwnProperty(data.nId)) {
            this.px = this.balls[data.nId].x;
            this.py = this.balls[data.nId].y;
        }
        //writeToScreen("Wszystkie moje kulki po dodaniu ")
        //writeToScreen(this.my_balls);
        //build();
    } else if (data.type === "nLb") {
        //writeToScreen("Przyszlo nLib");
        this.leaderBoard = [];

        var newLB = data.lB
        for (var k in newLB) {
            if (newLB.hasOwnProperty(k)) {
                this.leaderBoard.push({
                    id: newLB[k].id,
                    name: newLB[k].name
                })
            }
        }
        //for (var l = 0; l < this.leaderBoard.length; l++) {
        //    writeToScreen(this.leaderBoard[l].id)
        //    writeToScreen(this.leaderBoard[l].name)
       // }
        this.drawLeaderBoard();
    }

    this.reset();
    this.sendWebSocket({type: "Coord", x: this.mouseX2, y: this.mouseY2});

//writeToScreen("poOnMessage") */
//context.restore()
},

onKeyDown: function (event) {
    switch (event.keyCode) {
        case 86: // split
            if ((!this.keySpace) && (!this.isTyping)) {
                this.reset();
                this.sendWebSocket({type: "Coord", x: this.mouseX2, y: this.mouseY2});
                this.sendWebSocket({type: "space"});
                this.keySpace = true;
            }
            break;
        case 87: // eject mass
            if ((!this.keyW) && (!this.isTyping)) {
                this.sendWebSocket({type: "Coord", x: this.mouseX2, y: this.mouseY2});
                this.sendWebSocket({type: "w"});
                this.keyW = true;
            }
    }
},

onKeyUp: function (event) {
    switch (event.keyCode) {
        case 86:
            this.keySpace = false;
            break;
        case 87:
            this.keyW = false;
            break;
    }
},

draw: function () {
	//writeToScreen("draw poczatek");
    this.timestamp = +new Date;
    if (0 < this.my_balls.length) {
        this.build();
        var w = 0;
        var d = 0;
        var i = 0;

        var counter  = 0
        for (;i < this.my_balls.length;i++) {
        	//writeToScreen("my ball id petla for");
            if (this.balls.hasOwnProperty(this.my_balls[i])) {
            	counter = counter + 1;
                this.balls[this.my_balls[i]].updatePos();
                w += this.balls[this.my_balls[i]].x / this.my_balls.length;
                d += this.balls[this.my_balls[i]].y / this.my_balls.length;
               // writeToScreen("my ball id");
                //writeToScreen(c.my_balls[i]);
                //writeToScreen(c.balls[c.my_balls[i]].x);
                //writeToScreen(c.balls[c.my_balls[i]].y);
            }
        }
        if (counter === this.my_balls.length) {
        this.px = w;
        this.py = d;
        //ratio1 = ratio;
        this.px = (this.px + w) / 2;
        this.py = (this.py + d) / 2;
        }

        //cx = px;
        //cy = py;
    }

//processData();
	//writeToScreen("draw przed reset")
    this.reset();
    this.context.clearRect(0, 0, this.screenWidth, this.screenHeight);
    this.context.fillStyle = "#F2FBFF";
    this.context.fillRect(0, 0, this.screenWidth, this.screenHeight);
    this.context.save();
    this.context.strokeStyle = "#000000";
    this.context.globalAlpha = 0.2;
    this.context.scale(this.ratio, this.ratio);
//w = width / ratio;
//d = height / ratio;
    w = this.screenWidth / this.ratio;
    d = this.screenHeight / this.ratio;
    //writeToScreen("draw przed i")
    i = -0.5 + ( w / 2 -this.px) % 50;
    for (;i < w;i += 50) {
        this.context.beginPath();
        this.context.moveTo(i, 0);
        this.context.lineTo(i, d);
        this.context.stroke();
    }
    i = -0.5 + (-this.py + d / 2) % 50;
    for (;i < d;i += 50) {
        this.context.beginPath();
        this.context.moveTo(0, i);
        this.context.lineTo(w, i);
        this.context.stroke();
    }
    this.context.restore();

//items.sort(function(a, b) {
//return a.size == b.size ? a.id - b.id : a.size - b.size;
//});
    this.context.save();
    this.context.translate(this.screenWidth / 2, this.screenHeight / 2);
    this.context.scale(this.ratio, this.ratio);
    this.context.translate(-this.px, -this.py);
    this.context.globalAlpha = 1;
    var ball;
    //writeToScreen("draw przed for ball")
    for(ball in this.balls) {
        if(this.balls.hasOwnProperty(ball)) {
        //writeToScreen("px");
        //writeToScreen(px);
        //writeToScreen("py");
        //writeToScreen(py);
        //writeToScreen("c.balls[ball].id");
        //writeToScreen(c.balls[ball].id);
        //writeToScreen("c.balls[ball].x");
        //writeToScreen(c.balls[ball].x);
        //writeToScreen("c.balls[ball].y");
        //writeToScreen(c.balls[ball].y);
        //writeToScreen(c.my_balls[0]);
        //writeToScreen(c.my_balls[1]);
        this.balls[ball].updatePos();
        this.balls[ball].draw();
        //writeToScreen("draw");
        }
    }


    this.context.restore();

    if (this.lbCanvas) {
        this.context.drawImage(this.lbCanvas, this.canvas.width - this.lbCanvas.width - 10, 10); }
},

build: function () {
    /*     if (myPoints.length != 0) {
     var score = 0;
     minMass = 10000000;
     for (var i = 0 ;i < myPoints.length;i++) {
     score += myPoints[i].size;
     if(minMass > myPoints[i].size){
     minMass = myPoints[i].size;
     }
     }
     score = Math.pow(Math.min(64 / score, 1), 0.4) * Math.max(height / 1080, width / 1920);
     ratio = (9 * ratio + score) / screenRenderSize ;
     }*/

    var score = 0;
    var minMass = 10000000;
    for (var i = 0; i < this.my_balls.length; i++) {
        var ballId  = this.my_balls[i];
        if (this.balls.hasOwnProperty(ballId)) {
            score += this.balls[this.my_balls[i]].size;
            if(minMass > this.balls[this.my_balls[i]].size){
                minMass = this.balls[this.my_balls[i]].size;
            }
        }
    }
    score = Math.pow(Math.min(64 / score, 1), 0.4) * Math.max(this.screenHeight / 1080, this.screenWidth / 1920);
    this.ratio = (9 * this.ratio + score) / this.screenRenderSize ;
},

mainLoop: function () {
    this.draw();
    //drawLeaderBoard()
},

animloop: function () {
    var animLoopHandle = window.requestAnimFrame(this.animloop.bind(this));
    this.mainLoop();
},

playAsGuest: function () {
    this.connect();
},

RGB2HTML: function (red, green, blue)
{
    var decColor =0x1000000+ blue + 0x100 * green + 0x10000 *red ;
    return '#'+decColor.toString(16).substr(1);
}

};

function Ball(client, id, newX, newY, newSize, newType, newName) {
if(client.balls[id]) return client.balls[id];

//this.id    = id;
//this.name  = null;
//this.x = this.old_x = this.new_x = newX;
//this.y = this.old_y = this.new_y = newY;
//this.size = this.old_size = this.new_size = newSize;
//this.mass  = 0;

this.id    = id;
this.name  = null;
this.x   = newX;
this.y   = newY;
this.size  = newSize;
this.mass  = 0;

this.old_x = newX;
this.old_y = newY;
this.old_size = newSize;
this.new_x = newX;
this.new_y = newY;
this.new_size = newSize;

this.virus = false
this.mine  = false;
this.color = 0;

this.client      = client;
this.destroyed   = false;
this.visible     = false;
this.last_update = (+new Date);
this.update_tick = 0;
this.points      = []

    this.ballType = newType;
    this.nameCache = null;
    if (this.ballType === 0) {this.setName(newName); }
    //if (this.type === 2) {this.createPoints();}

client.balls[id] = this;
return this;
}

Ball.prototype = {
destroy: function() {
    delete this.client.balls[this.id];
    var mine_ball_index = this.client.my_balls.indexOf(this.id);
    if(mine_ball_index > -1) {
        //writeToScreen("Do usuniecia ID");
        //writeToScreen(this.id);
        this.client.my_balls.splice(mine_ball_index, 1);
        //writeToScreen("Wszystkie moje kulki po odjeciu")
        //writeToScreen(this.client.my_balls)
    }
},

setCords: function(new_x, new_y) {
    //if(this.x == new_x && this.y == new_y) return;
    this.old_x = this.x;
    this.old_y = this.y;
    this.new_x = new_x;
    this.new_y = new_y;


    //if(!old_x && !old_y) return;
},

setSize: function(new_size) {
    if(this.size == new_size) return;
    this.old_size = this.size;
    this.new_size = new_size;
    this.mass     = parseInt(Math.pow(new_size/10, 2));

    //if(!old_size) return;
    //if(this.mine) this.client.updateScore();
},

//setName: function(name) {
//    if(this.name == name) return;
//    var old_name = this.name;
//    this.name    = name;
//},
    getNameSize : function() {
        return Math.max(~~(0.3 * this.size), 24);
    },

    setName : function(name) {
        //if (this.name === name) return
        if (this.name = name) {
            if (null == this.nameCache) {
                this.nameCache = new SVGPlotFunction(this.getNameSize(), "#FFFFFF", true, "#000000");
            } else {
                this.nameCache.setSize(this.getNameSize());
            }
            this.nameCache.setValue(this.name);
        }
    },

update: function() {
    var old_time     = this.last_update;
    this.last_update = (+new Date);
},

appear: function() {
    if(this.visible) return;
    this.visible = true;

    //if(this.mine) this.client.updateScore();
},

disappear: function() {
    if(!this.visible) return;
    this.visible = false;
},

    updatePos : function() {
        var A;
        //A = (timestamp - this.updateTime) / 120;
        A = (this.client.timestamp - this.last_update) / 150;
        //A = (timestamp - this.last_update) / 80;
        A = 0 > A ? 0 : 1 < A ? 1 : A;
        //A = A * A * (3 - 2 * A);
         var getNameSize = this.getNameSize();
        /*
         if (this.destroyed && 1 <= A) {
         var idx = sprites.indexOf(this);
         if (-1 != idx) {
         sprites.splice(idx, 1);
         }
         }*/
        this.x = A * (this.new_x - this.old_x) + this.old_x;
        this.y = A * (this.new_y - this.old_y) + this.old_y;
        this.size = A * (this.new_size - this.old_size) + this.old_size;
        return A;
    },
/*
createPoints: function() {
    var numOfPoints = this.getNumPoints();
    for (var i = 0; i < numOfPoints; i++) {
        var pt = this.points[i];
        this.points.splice(i, 0, {
            ref: this,
            v: this.size,
            x: this.x,
            y: this.y
        });
    }
},
*/
    getNumPoints: function() {
      return 60;
    },

draw: function() {
    /*

    var radius = this.size;

    this.client.context.beginPath();
    this.client.context.arc(this.x, this.y, radius, 0, 2 * Math.PI, false);
    this.client.context.fillStyle = this.color;
    this.client.context.fill();
    this.client.context.lineWidth = 0;
    this.client.context.strokeStyle = '#003300';
    this.client.context.stroke(); */



    //this.drawTime = timestamp;
    //c = this.updatePos();
    //this.destroyed && (ctx.globalAlpha *= 1 - c);
    this.updatePos();



    if (this.ballType === 2) {
        var numOfPoints = this.getNumPoints();
        var y = 2 * Math.PI / numOfPoints;
        var v = this.size
        this.client.context.lineJoin = "miter"
        this.client.context.lineWidth = 10;
        this.client.context.lineCap = "round";
        this.client.context.fillStyle = this.color;
        this.client.context.strokeStyle = this.color;
        this.client.context.beginPath();

        this.client.context.moveTo(this.x + Math.cos(0) * v, this.y + Math.sin(0) * v);
        for (var i = 1; i < numOfPoints; i++) {
            if (1 == i % 2) {
                v += 5;
            }
            else {
                v -= 5;
            }
            this.client.context.lineTo(this.x + Math.cos(y * i) * v, this.y + Math.sin(y * i) * v);
        }
        this.client.context.fill();
        this.client.context.closePath();
        this.client.context.stroke()
    } else if (this.ballType === 0){
        this.client.context.beginPath();
        this.client.context.lineJoin = "round"
        this.client.context.lineWidth = 10;
        this.client.context.lineCap = "round";
        this.client.context.fillStyle = this.color;
        this.client.context.strokeStyle = this.color;
        this.client.context.arc(this.x, this.y, this.size, 0, 2 * Math.PI, false);
        this.client.context.fill();
        this.client.context.closePath();
        this.client.context.stroke()
        var src = ~~this.y;
            if (this.name) {
                if (this.nameCache) {
                    this.client.context.save();
                    //writeToScreen("Po this name cache")
                    this.nameCache.setSize(this.getNameSize());
                    var i = this.nameCache.render();
                     //writeToScreen("Po this name cache render")
                    this.client.context.drawImage(i, ~~this.x - ~~(i.width / 2), src - ~~(i.height / 2));
                    src += i.height / 2 + 4;
                    this.client.context.restore();
                }
            }
    } else {
        this.client.context.beginPath();
        this.client.context.lineJoin = "round"
        this.client.context.lineWidth = 10;
        this.client.context.lineCap = "round";
        this.client.context.fillStyle = this.color;
        this.client.context.strokeStyle = this.color;
        this.client.context.arc(this.x, this.y, this.size, 0, 2 * Math.PI, false);
        this.client.context.fill();
        this.client.context.closePath();
        this.client.context.stroke()
    }



    //this.client.context.strokeStyle = '#003300';
    //this.client.context.stroke();



},

toString: function() {
    if(this.name) return this.id + '(' + this.name + ')';
    return this.id.toString();
}
};

function SVGPlotFunction(n, Var, stroke, plot) {
    if (n) {
        this._size = n;
    }
    if (Var) {
        this._color = Var;
    }
    this._stroke = stroke;
    if (plot) {
        this._strokeColor = plot;
    }
}

SVGPlotFunction.prototype = {
    _value : "",
    _color : "#000000",
    _stroke : false,
    _strokeColor : "#000000",
    _size : 16,
    _canvas : null,
    _ctx : null,
    _dirty : false,
    _scale : 1,
    setSize : function(size) {
        if (this._size != size) {
            this._size = size;
            this._dirty = true;
        }
    },
    setColor : function(color) {
        if (this._color != color) {
            this._color = color;
            this._dirty = true;
        }
    },
    setStroke : function(stroke) {
        if (this._stroke != stroke) {
            this._stroke = stroke;
            this._dirty = true;
        }
    },
    setStrokeColor : function(b) {
        if (this._strokeColor != b) {
            this._strokeColor = b;
            this._dirty = true;
        }
    },
    setValue : function(value) {
        if (value != this._value) {
            this._value = value;
            this._dirty = true;
        }
    },
    render : function() {
        if (null == this._canvas) {
            this._canvas = document.createElement("canvas");
            this._ctx = this._canvas.getContext("2d");
        }
        if (this._dirty) {
            this._dirty = false;
            var canvas = this._canvas;
            var ctx = this._ctx;
            var mass = this._value;
            var scale = this._scale;
            var fontSize = this._size;
            var font = fontSize + "px Ubuntu";
            ctx.font = font;
            var parentWidth = ctx.measureText(mass).width;
            var PX = ~~(0.2 * fontSize);
            canvas.width = (parentWidth + 6) * scale;
            canvas.height = (fontSize + PX) * scale;
            ctx.font = font;
            ctx.scale(scale, scale);
            ctx.globalAlpha = 1;
            ctx.lineWidth = 3;
            //ctx.strokeStyle = this._strokeColor;
            ctx.fillStyle = this._color;
            //if (this._stroke) {
            //    ctx.strokeText(mass, 3, fontSize - PX / 2);
            //}
            ctx.fillText(mass, 3, fontSize - PX / 2);
        }
        return this._canvas;
    }
};

//======================================================================
/*
//var perf = performance.now();
//CANVAS
// writeToScreen("Poczatek");
var c = new Client("me");
c.connect();
var ratio = 1;
var screenRenderSize = 10;
//var cx= 0;
//var cy= 0;
var px = 0
var py = 0

//PLAYER

var positionX = 250;
var positionY = 100;

//PLAYER

var xoffset = 0;
var yoffset = 0;
var screenWidth = window.innerWidth;
var screenHeight = window.innerHeight;
var canvas = document.getElementById('cvs');
var context = canvas.getContext('2d');
canvas.width = screenWidth; canvas.height = screenHeight;
//CANVAS

//MOUSE
var mouseX = 250;
var mouseY = 100;

var mouseX2 = 250;
var mouseY2 = 100;

canvas.addEventListener('mousemove', getMousePos, false);

function getMousePos(evt) {
mouseX = evt.clientX
mouseY = evt.clientY
reset()
c.sendWebSocket({type: "Coord", x: mouseX2, y: mouseY2})
//resetGlobalMouse()
//sendWebSocket({type: "Coord", x: x1, y: y1})
//writeToScreen(JSON.stringify({type: "Coord", x: x1, y: y1}));
};

function reset() {
mouseX2 = (mouseX - screenWidth / 2) / ratio + px;
mouseY2 = (mouseY - screenHeight / 2) / ratio + py;
}

// KEYBOARD

var keySpace = false;
var keyW = false;
var isTyping = false;

window.onkeydown = function (event) {
switch (event.keyCode) {
case 32: // split
if ((!keySpace) && (!isTyping)) {
reset()
c.sendWebSocket({type: "Coord", x: mouseX2, y: mouseY2})
c.sendWebSocket({type: "space"})
keySpace = true;
}
break;
case 87: // eject mass
if ((!keyW) && (!isTyping)) {
c.sendWebSocket({type: "Coord", x: mouseX2, y: mouseY2})
c.sendWebSocket({type: "w"})
keyW = true;
}
}
};

window.onkeyup = function (event) {
switch (event.keyCode) {
case 32:
keySpace = false;
break;
case 87:
keyW = false;
break;
}
};
//KEYBOARD
//writeToScreen("Przed connect");
//writeToScreen("Po connect");
//var timestamp = +new Date;
//if (window.requestAnimationFrame) {
//    window.requestAnimationFrame(anim);
//} else {
//    setInterval(draw, 1E3 / 60);
//}

function draw() {
var ctx = context;
timestamp = +new Date;
if (0 < c.my_balls.length) {
build();
var w = 0;
var d = 0;
var i = 0;

for (;i < c.my_balls.length;i++) {
if (c.balls.hasOwnProperty(c.my_balls[i])) {
c.balls[c.my_balls[i]].updatePos()
w += c.balls[c.my_balls[i]].x / c.my_balls.length;
d += c.balls[c.my_balls[i]].y / c.my_balls.length;
//writeToScreen("my ball id");
//writeToScreen(c.my_balls[i]);
// writeToScreen(c.balls[c.my_balls[i]].x);
// writeToScreen(c.balls[c.my_balls[i]].y);
}
}
px = w;
py = d;
//ratio1 = ratio;
px = (px + w) / 2;
py = (py + d) / 2;

//cx = px;
//cy = py;
}

//processData();
reset();
ctx.clearRect(0, 0, screenWidth, screenHeight);
ctx.fillStyle = "#F2FBFF";
ctx.fillRect(0, 0, screenWidth, screenHeight);
ctx.save();
ctx.strokeStyle = "#000000";
ctx.globalAlpha = 0.2;
ctx.scale(ratio, ratio);
//w = width / ratio;
//d = height / ratio;
w = screenWidth / ratio;
d = screenHeight / ratio;
i = -0.5 + ( w / 2 -px) % 50;
for (;i < w;i += 50) {
ctx.beginPath();
ctx.moveTo(i, 0);
ctx.lineTo(i, d);
ctx.stroke();
}
i = -0.5 + (-py + d / 2) % 50;
for (;i < d;i += 50) {
ctx.beginPath();
ctx.moveTo(0, i);
ctx.lineTo(w, i);
ctx.stroke();
}
ctx.restore();

//items.sort(function(a, b) {
//    return a.size == b.size ? a.id - b.id : a.size - b.size;
//});
ctx.save();
ctx.translate(screenWidth / 2, screenHeight / 2);
ctx.scale(ratio, ratio);
ctx.translate(-px, -py);
ctx.globalAlpha = 1;
var ball = 0
for(var ball in c.balls) {
//if(c.balls.hasOwnProperty(ball)) {
//writeToScreen("px");
//writeToScreen(px);
//writeToScreen("py");
//writeToScreen(py);
//writeToScreen("c.balls[ball].id");
//writeToScreen(c.balls[ball].id);
//writeToScreen("c.balls[ball].x");
//writeToScreen(c.balls[ball].x);
//writeToScreen("c.balls[ball].y");
//writeToScreen(c.balls[ball].y);
//writeToScreen(c.my_balls[0]);
//writeToScreen(c.my_balls[1]);
c.balls[ball].updatePos()
c.balls[ball].draw();
//writeToScreen("draw");
//}
}


ctx.restore();
}

function build() {
/*     if (myPoints.length != 0) {
var score = 0;
minMass = 10000000;
for (var i = 0 ;i < myPoints.length;i++) {
score += myPoints[i].size;
if(minMass > myPoints[i].size){
minMass = myPoints[i].size;
}
}
score = Math.pow(Math.min(64 / score, 1), 0.4) * Math.max(height / 1080, width / 1920);
ratio = (9 * ratio + score) / screenRenderSize ;
}

var score = 0;
var minMass = 10000000;
for (var i = 0; i < c.my_balls.length; i++) {
var ballId  = c.my_balls[i]
if (c.balls.hasOwnProperty(ballId)) {
score += c.balls[c.my_balls[i]].size;
if(minMass > c.balls[c.my_balls[i]].size){
minMass = c.balls[c.my_balls[i]].size;
}
}
}
score = Math.pow(Math.min(64 / score, 1), 0.4) * Math.max(screenHeight / 1080, screenWidth / 1920);
ratio = (9 * ratio + score) / screenRenderSize ;
}




//writeToScreen("Poczatek");
//var websocket = new Object()
//startGame()
//writeToScreen("Po new client");
//writeToScreen("Po connect");



Array.prototype.diff = function(a) {
return this.filter(function(i) {return a.indexOf(i) < 0;});
};

var leaderBoard = ["Test"]

function drawLeaderBoard() {
lbCanvas = null;
if (0 != leaderBoard.length)
if (true) {
lbCanvas = document.createElement("canvas");
var ctx = lbCanvas.getContext("2d"),
boardLength = 60;
boardLength = boardLength + 24 * leaderBoard.length;
var scaleFactor = Math.min(0.22*canvasHeight, Math.min(200, .3 * canvasWidth)) / 200;
lbCanvas.width = 200 * scaleFactor;
lbCanvas.height = boardLength * scaleFactor;

ctx.scale(scaleFactor, scaleFactor);
ctx.globalAlpha = .4;
ctx.fillStyle = "#000000";
ctx.fillRect(0, 0, 200, boardLength);

ctx.globalAlpha = 1;
ctx.fillStyle = "#FFFFFF";
var c = "Leaderboard";
ctx.font = "30px Ubuntu";
ctx.fillText(c, 100 - ctx.measureText(c).width / 2, 40);
}
}

function arr_diff (a1, a2) {

var a = [], diff = [];

for (var i = 0; i < a1.length; i++) {
a[a1[i]] = true;
}

for (var i = 0; i < a2.length; i++) {
if (a[a2[i]]) {
delete a[a2[i]];
} else {
a[a2[i]] = true;
}
}

for (var k in a) {
diff.push(k);
}

return diff;
};


function sendMousePostion() {
//var scaledMouseX = Math.round(mouseX - screenWidth/2)
//var scaledMouseY = Math.round(mouseY - screenHeight/2)
c.sendWebSocket({type: "Coord", x: scaledMouseX, y: scaledMouseY})
}


function resetGlobalMouse() {
//mouseX2 = (mouseX - width / 2) / ratio + px;
//mouseY2 = (mouseY - height / 2) / ratio + py;
//mouseX2 = (mouseX - screenWidth / 2) / 1 + positionX;
//mouseY2 = (mouseY - screenHeight / 2) / 1 + positionY;
}

function RGB2HTML(red, green, blue)
{
var decColor =0x1000000+ blue + 0x100 * green + 0x10000 *red ;
return '#'+decColor.toString(16).substr(1);
}

//MOUSE



//function onOpen(evt) {
//writeToScreen("CONNECTED")
//    animloop()
//}

/*
var keySpace = false;
window.onkeydown = function(e) {
if (!(32 != e.keyCode)) { // space
if (!keySpace) {
//sendMousePosition();
c.sendWebSocket({type: "space"})
keySpace = true;
}
}
case 87: // eject mass
if ((!wPressed) && (!isTyping)) {
sendMouseMove();
sendUint8(21);
wPressed = true;
}
}

window.onkeyup = function(event) {
if (32 == event.keyCode) { // space
keySpace = false;
}
}*/

function writeToScreen(message) {
	var pre = document.createElement("p");
	var status = document.getElementById("status");
	pre.style.wordWrap = "break-word";
	pre.innerHTML = message + " " + new Date();
	status.appendChild(pre);
	}

	window.requestAnimFrame = (function() {
	return  window.requestAnimationFrame       ||
	    window.webkitRequestAnimationFrame ||
	    window.mozRequestAnimationFrame    ||
	    window.msRequestAnimationFrame     ||
	    function( callback ) {
	        window.setTimeout(callback, 1000 / 50);
	    };
	})();

	window.cancelAnimFrame = (function(handle) {
	return  window.cancelAnimationFrame     ||
	    window.mozCancelAnimationFrame;
	})();




