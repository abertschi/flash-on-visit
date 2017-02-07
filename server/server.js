"use strict";

const app = require('express')();
const server = require('http').Server(app);
const io = require('socket.io')(server);
const log = require('winston');

const SOCKET_PORT = 5001;

server.listen(SOCKET_PORT);

console.log('Flash on visit server');
console.log('Running server at port ' + SOCKET_PORT);

app.get('/channels/:channel', (req, res) => {
    log.info(`New flash request in channel: ${req.params.channel}`);

    if (!req.params.channel) res.status(400).end();
    let payload = {
        channel: req.params.channel,
        ip: req.ip
    };

    io.to(payload.channel).emit('flash', payload);
    res.send('flash on visit');
});

io.on('connection', (socket) => {
    socket.on('regist', (data) => {
        log.info('New client registered in channel: ' + data.channel);
        if (!data.channel) {
            socket.emit('error', 'No channel set to register');
        } else {
            socket.join(data.channel);
        }
    });
});
