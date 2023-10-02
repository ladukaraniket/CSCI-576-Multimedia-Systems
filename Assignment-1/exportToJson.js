const { clear } = require('console');
const fs = require('fs');


const replacer = (key, value) => {
  if (typeof value === 'string') {
    return value; // Keep strings as they are (with quotations)
  } else if (typeof eval(value).length === 'number') {

    // console.log('~~~');
    // console.log(eval(value));
    s = `[`
    eval(value)?.forEach(item =>s=s+item+" ")
    s=s.slice(0,-1)+`]`

    console.log(s);
    return s;

  } else {

    return eval(value); // Convert other types to strings without quotations
  }
};

// Read the file asynchronously
fs.readFile('./log.txt', 'utf8', (err, data) => {
  if (err) {
    console.error('Error reading the file:', err);
    return;
  }

  let json = {

  };

  data.split("FILE END").forEach(filedata => {
    let [filename, data] = filedata.split("~~~");

    // console.log(filename);
    // console.log(data);
    if (!json[filename]) {
      json[filename] = {};
    }

    json[filename]["dropAxis"] = []
    json[filename]["errorAxis"] = []
    json[filename]["red"] = []
    json[filename]["green"] = []
    json[filename]["blue"] = []
    data?.split(":::").forEach(err => {
      let [dropPercent, erroRate,red,green,blue] = err.split("  ");

      dropPercent = dropPercent.split(": ")[1]
      erroRate = erroRate?.split(": ")[1]
      red = red?.split(": ")[1]
      green = green?.split(": ")[1]
      blue = blue?.split(": ")[1]

      if (dropPercent && erroRate) {

        json[filename]["dropAxis"].push(dropPercent)
        json[filename]["errorAxis"].push(erroRate)
        json[filename]["red"].push(red)
        json[filename]["green"].push(green)
        json[filename]["blue"].push(blue)
        // json[filename].push({
        //   dropPercent,
        //   val
        // })
      }

    })
  });

  // Object.keys(json).forEach(key=>{
  //   json[key]["dropAxis"]=json[key].map(item=>item.dropPercent)
  //   json[key]["errorAxis"]=json[key].map(item=>item.val)
  //   console.log(json[key].map(item=>item.dropPercent));

  // })


  json = JSON.stringify(json)

  // json.match
  // const unquoted = json.replace(/:"([^"]+)"/g, ':$1');
  // const unquoted = json.replace(/:\[(,*("([^"]+)")*)*\]/g, ':[$3]');

  fs.writeFileSync('errorData.json', json);

  // console.log(unquoted);

});