var webpack = require('webpack');

module.exports = {
    entry: './src/app.js',
    output: {
        filename: 'web/bundle.js'
    },
    module: {
        loaders: [
            { test: /\.css$/, loader: 'style!css' },
            { test: /\.less$/, loader: 'style!css!less' },
            { include: /\.json$/, loader: 'json-loader'},
            { test: /\.html$/, loader: 'ng-cache' }
        ]
    },
    plugins: [
        new webpack.ProvidePlugin({
            $: 'jquery',
            jQuery: 'jquery',
            'window.jQuery': 'jquery'
        })
    ]
};
