"use strict";

const io = require('socket.io-client');
const exec = require('child_process').exec;

const log = require('debug')('flash-on-visit');
const error = require('debug')('flash-on-visit:error');
var program = require('commander');


program
  .version('0.0.1')
  .usage('[options] <file ...>')
  .option('-h, --host <h>', 'The server adress')
  .option('-c, --channel <c>', 'The channel to subscribe')
  .option('-d, --debug', 'For debugging purpose')
  .parse(process.argv);

console.log('Running flash-on-visit client \n');

if (!program.debug) {
  let missing = false;
  if (!program.host) {
    console.log('No host name set.');
    missing = true;
  }
  if (!program.channel) {
    console.log('No channel set.');
    missing = true;
  }

  if (missing) {
      program.help();
      process.exit();
  }
}

let channel = program.channel || 'hack';
let serverUrl = program.host || 'http://localhost:5001';
const conn = io.connect(serverUrl);

exec('maclight', (error, stdout, stderr) => {
    if (error) {
        console.log('Please install maclight. maclight not found');
    }
});

console.log('Trying to connect to ' + serverUrl);
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
