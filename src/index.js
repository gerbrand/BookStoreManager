// @flow
const csv = require('csv-parser')
const fs = require('fs')
const results = [];

fs.createReadStream('../Bol.Com huidig aanbod 23 oktober 2018.csv')
    .pipe(csv({ separator: ',' }))
    .on('data', results.push)
    .on('end', () => {
        console.log(results);
    });