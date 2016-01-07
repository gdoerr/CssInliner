var webpack = require('webpack');

module.exports = {
    entry: './app.js',
    output: {
        filename: 'web/bundle.js'
    },
    module: {
        loaders: [
            { test: /\.css$/, loader: 'style!css' },
            { include: /\.json$/, loader: 'json-loader'},
            {
                test: /jquery.+\.js$/,
                loader: 'expose?jQuery'
            }
        ]
    },
    plugins: [
        new webpack.ProvidePlugin({
            $: 'jquery',
            jQuery: 'jquery'
        })
    ]
};
