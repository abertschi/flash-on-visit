"use strict";

const io = require('socket.io-client');
const exec = require('child_process').exec;
const argv = require('yargs').argv;
const log = require('debug')('flash-on-visit');
const error = require('debug')('flash-on-visit:error');


let channel = argv.channel || 'hack';
let serverUrl = argv.url || 'http://localhost:5001';
const conn = io.connect(serverUrl);

console.log('Running flash-on-visit client');
console.log('Trying to connect to ' + serverUrl);

exec('maclight', (error, stdout, stderr) => {
    if (error) {
        console.log('Please install maclight. maclight not found');
    }
});

conn.on('connect', () => {
    console.log(`Connected to ${serverUrl}. `);
    console.log(`Waiting for flash instructions in channel: ${channel}`);

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
