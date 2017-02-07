"use strict";

const io = require('socket.io-client');
const SERVER_URL = 'http://localhost:5001';
const exec = require('child_process').exec;
const conn = io.connect(SERVER_URL);

const args = process.argv;
let channel = args.length > 2 ? args[2] : 'hack';

console.log('Running flash-on-visit client');

exec('maclight', (error, stdout, stderr) => {
    if (error) {
        console.log('Please install maclight. maclight not found');
    }
});

conn.on('connect', () => {
    console.log(`Connected to ${SERVER_URL}. ` +
        `Waiting for flash instructions in channel: ${channel}`);

    let regist = {
        channel: channel
    };

    conn.emit('regist', regist, (resp, data) => {
        console.log('server sent resp code ' + resp);
    });

});

conn.on('disconnect', () => {
    console.log('Disconnected from server');
});

conn.on('flash', (data) => {
    let cmd = 'maclight keyboard blink -f 0 1:.4';

    exec(cmd, (error, stdout, stderr) => {
        if (error) {
            console.log(error);
        } else {
            console.log(`${data.ip} flashes LED`);
        }
    });
});
