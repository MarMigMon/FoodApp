// Example express application adding the parse-server module to expose Parse
// compatible API routes.

var express = require('express');
var ParseServer = require('parse-server').ParseServer;
var path = require('path');

var databaseUri = process.env.DATABASE_URI || process.env.MONGODB_URI;

if (!databaseUri) {
  console.log('DATABASE_URI not specified, falling back to localhost.');
}

var pushConfig = {};

if (process.env.FCM_API_KEY) {
    pushConfig['android'] = { apiKey: process.env.FCM_API_KEY || 'AAAAnjkZYJ0:APA91bEKJtfA92rHuj-vfLXkdGApoqPQupTJlJr8Cv8nrXns11WrgoHnExBdhQmV24AenaqeuSlX5X0letOH29awMxOVKgpCBw5MGWxYGjzC6uZpgykwLwhIPXGkoUvwiJJF_tHtVKP7udvzi1Q_Hk1aLE9VSEKrAA'};
}

if (process.env.APNS_ENABLE) {
    pushConfig['ios'] = [
        {
            pfx: 'ParsePushDevelopmentCertificate.p12', // P12 file only
            bundleId: 'me.mvega.foodapp',  // change to match bundleId
            production: false // dev certificate
        }
    ]
}


var filesAdapter = null;  // enable Gridstore to be the default
if (process.env.S3_ENABLE) {
    var S3Adapter = require('parse-server').S3Adapter;

    filesAdapter = new S3Adapter(
        process.env.AWS_ACCESS_KEY,
        process.env.AWS_SECRET_ACCESS_KEY,
        {bucket: process.env.AWS_BUCKET_NAME, bucketPrefix: "", directAccess: true}
    );
}

var api = new ParseServer({
  databaseURI: databaseUri || 'mongodb://heroku_c0khlctw:5e29kqj8d807cig006l9gef5vf@ds133104.mlab.com:33104/heroku_c0khlctw',
  cloud: process.env.CLOUD_CODE_MAIN || __dirname + '/cloud/main.js',
  appId: process.env.APP_ID || 'fbufood',
  masterKey: process.env.MASTER_KEY || 'marmigmon', //Add your master key here. Keep it secret!
  push: pushConfig,
  filesAdapter: filesAdapter,
  liveQuery: { classNames: ["Message"]},
  serverURL: process.env.SERVER_URL || 'http://fbufood.herokuapp.com/parse'  // needed for Parse Cloud and push notifications
});
// Client-keys like the javascript key or the .NET key are not necessary with parse-server
// If you wish you require them, you can set them as options in the initialization above:
// javascriptKey, restAPIKey, dotNetKey, clientKey

var app = express();

// Serve static assets from the /public folder
app.use('/public', express.static(path.join(__dirname, '/public')));

// Serve the Parse API on the /parse URL prefix
var mountPath = process.env.PARSE_MOUNT || '/parse';
app.use(mountPath, api);

// Parse Server plays nicely with the rest of your web routes
app.get('/', function(req, res) {
  res.status(200).send('I dream of being a website.  Please star the parse-server repo on GitHub!');
});

var port = process.env.PORT || 1337;
var httpServer = require('http').createServer(app);
httpServer.listen(port, function() {
    console.log('parse-server-example running on port ' + port + '.');
});

// This will enable the Live Query real-time server
ParseServer.createLiveQueryServer(httpServer);