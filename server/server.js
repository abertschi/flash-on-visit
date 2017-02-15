"use strict";

const app = require('express')();
const server = require('http').Server(app);
const io = require('socket.io')(server);
const log = require('debug')('flash-on-visit');
const unirest = require('unirest');

const SOCKET_PORT = process.env.PORT || 5001;

const GCS_API_KEY = require('./gcs_server_key.js');
const GCS_URL = 'https://fcm.googleapis.com/fcm/send';

server.listen(SOCKET_PORT);

log('Flash on visit server');
log('Running server at port %d', SOCKET_PORT);

app.use(function(req, res, next) {
  res.header("Access-Control-Allow-Origin", "*");
  res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
  next();
});

app.get('/channels/:channel', (req, res) => {
    let ip = req.ip;
    if (ip.lastIndexOf(':') > -1) {
        ip = ip.substring(ip.lastIndexOf(':') + 1, ip.length);
    }
    let host = req.headers.host;

    log('New notify request in %s on %s by %s', req.params.channel, host, ip);

    if (!req.params.channel) {
        res.status(500).send({
            error: 'No channel name set'
        });
        log('Error: %s: No channel name set', ip);
        return;
    }

    let payload = {
        channel: req.params.channel,
        ip: ip,
        host: host
    };

    // websocket new event
    io.to(payload.channel).emit('flash', payload);

    // google cloud services new event
    prepareGcsRequest()
        .send({
            to: `/topics/${payload.channel}`,
            data: payload
        })
        .end(response => {
            if (response.error) {
                res.status(500).send({
                    status: 500,
                    error: 'GCS: Invalid client registration token',
                    message: response.body,
                    channel: payload.channel
                });
                log('Error: Invalid client registration token');
            } else {
                res.send({
                  status: 200,
                  message: 'Clients notified',
                  channel: payload.channel
                });
            }
        });
});

io.on('connection', (socket) => {
    socket.on('regist', (data) => {
        let clientIp = socket.request.connection.remoteAddress;

        if (!data.channel) {
            socket.emit('failure', 'Channel missing. Please set a channel to join');
            log("Error - no channel name specified. %s can not join.", clientIp);
        } else {
            log('%s registered in channel %s', clientIp, data.channel);
            socket.join(data.channel);
        }
    });
});

//TODO: add post method to let android clients regist with GCS

let prepareGcsRequest = () => {
    return unirest.post(GCS_URL)
        .headers({
            'Content-Type': 'application/json',
            'Authorization': `key=${GCS_API_KEY}`
        });
};
