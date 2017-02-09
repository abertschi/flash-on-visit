"use strict";

const app = require('express')();
const server = require('http').Server(app);
const io = require('socket.io')(server);
const log = require('debug')('flash-on-visit');

const SOCKET_PORT = process.env.PORT || 5001;
server.listen(SOCKET_PORT);

log('Flash on visit server');
log('Running server at port %d', SOCKET_PORT);

app.get('/channels/:channel', (req, res) => {
    log('New flash request in %s by %s', req.params.channel, req.ip);

    if (!req.params.channel) res.status(400).end();
    let payload = {
        channel: req.params.channel,
        ip: req.ip
    };

    io.to(payload.channel).emit('flash', payload);
    res.send(JSON.stringify(payload));
});

io.on('connection', (socket) => {
    socket.on('regist', (data) => {
        if (!data.channel) {
            socket.emit('error', 'Channel missing. Please set a channel to join');
            log("Error - no channel name specified. Can not join.");
        } else {
          log('New client registered in channel %s', data.channel);
          socket.join(data.channel);
        }
    });
});
