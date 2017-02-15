# flash-on-visit macOS client

This client flashes the capslock LED of your computer keyboard whenever you get a new visit on your site.

## Installation
Make sure you have [Node.js](https://nodejs.org/en/download/package-manager/), [npm](https://docs.npmjs.com/getting-started/installing-node) and [gem](https://rubygems.org/pages/download) properly installed.

You will need maclight installed on your system. Hook up gem and install maclight.
```sh
$ gem install maclight
```

After you have cloned the git repository and stepped into the macOS client folder, download the Node.js dependencies.

```sh
$ git clone https://github.com/abertschi/flash-on-visit.git
$ cd flash-on-visit/client_macos
$ npm install
```

Fire up flash-on-visit and get visitor notifications:

```sh
$ node client.js --help

 Usage: client [options] <file ...>

 Options:

   -h, --help         output usage information
   -V, --version      output the version number
   -h, --host <h>     The server adress
   -c, --channel <c>  The channel to subscribe
   -d, --debug        For debugging purpose

   ```

   ```sh
   $ node client.js --host http://213.136.81.179:3004 --channel abertschi
   ```
