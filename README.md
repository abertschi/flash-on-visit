# flash-on-visit

> It's often the small things that matter most in life.  

**flash-on-visit** gives you visual feedback on your website traffic by flashing the caps lock LED of your computer keyboard and notification light of your Android phone :yellow_heart:


This app is in early development :feet:

## Android client

<p align="center">
  <img src='.github/phone_showcase.png' />
</p>

Download the App from Google Play Store.

## MacOS client
The MacOS client is a Node.js app.  
Check out [./client/README.md](client/README.md) for installation instructions.

### Credits
Thanks to these amazing people:
- TODO: Icons, LEDManager

## Integrate on your website
Perform an HTTP GET request on `http://213.136.81.179:3004/channels/<channel>` where `<channel>` is an identifier for your website. The most straigth forward approach to do that is to include the following script tag into the head of your website.

```html
<html>
<head>
  <script src="http://213.136.81.179:3004/channels/hack/channels/my-channel-identifier"/></script>
</head>
<body>
    <h1>My awesome website 🚀</h1>
</body>
</html>
```

This will notify clients listening for notifications in the set channel.

## Host your own backend
A current version of [./server/server.js](server/server.js) is running on my server.  
The backend is a Node.js app running `express` and `socket.io` and a features a hook to notify Android clients
with Google's Firebase Cloud Messaging (FCM) Service. 

In case you wanted to host your own server, you would have to setup a Firebase account and rebuild the Android client with your own credentials.

i.e. set these files:
- `./android/app/google-services.json` Credentials to receive Firebase Cloud Messaging notifications
- `./server/gcs_server_key.js`: API Key to send Firebase Cloud Messaging notifications

## Contributing

Help is always welcome :yellow_heart:. Contribute to the project by forking and submitting a pull request.

## Contact
[![twitter: @andrinbertschi]( https://img.shields.io/badge/twitter-andrinbertschi-yellow.svg?style=flat-square)](http://twitter.com/andrinbertschi)
