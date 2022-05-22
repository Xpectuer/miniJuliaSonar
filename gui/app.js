const express = require('express');
const multer = require('multer');
const path = require('path');
var crypto = require('crypto');


const {exec, spawn, execSync} = require('node:child_process')
const fs = require('fs')
const bodyParser = require("express");
const os = require("os");
const cors = require('cors');


const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, 'sources/');
    },

    filename: function (req, file, cb) {
        cb(null, file.originalname);
    }
});

var upload = multer({storage: storage})

function writeSonarLog(data) {
    let log = `====================================` +
        `\n[${new Date()}]\n ${data}`
    let flag = 'w'
    let path = './log/sonar.log'

    // if(fs.existsSync(path) && fs.statSync(path).size > LOG_LIMIT) {
    //     flag = 'w'
    // }

    fs.writeFile(path, log, {flag: flag}, err => {
        if (err) {
            console.error("write log error.");
            console.error(err);
        }
    })
}

function writeExchange(data) {
    let base = Date.now() + ".jl";
    let path = norm_path(INDIR, base)
    fs.writeFileSync(path, data);
    return base;
}

function readExchange(path) {
    return fs.readFileSync(norm_path(OUTDIR, path), {encoding: "utf8"});
}


const norm_path = (dir, base) => {
    return path.normalize(path.format({
        root: '.',
        dir: dir,
        base: base
    }));
}

const JAVA = "java"
const OPT = "-jar"
const SONARDIR = "../target/"
const SONAR = norm_path(SONARDIR, "miniJuliaSonar-1.0.jar");
const INDIR = path.join(os.tmpdir(), '/juliaSonarT/gui_in');
const OUTDIR = path.join(os.tmpdir(), '/juliaSonarT/gui_out');
const LOG_LIMIT = 5 * 1024;

let SESSION = [];

function lookUpSession(key) {
    let hash = crypto.createHash('md5').update(key).digest('hex');

    if (SESSION[hash] === undefined) {
        return undefined;
    } else {
        return SESSION[hash];
    }
}

function putSession(key, value) {
    let hash = crypto.createHash('md5').update(key).digest('hex');
    SESSION[hash] = value;
}

// ================ UTILITIES =====================

const currentTime = () => {
    return new Date().getTime();
}

const infoLog = (...msg) => {
    console.log("[INFO]:", msg)
}

const debugLog = (...msg) => {
    if (debug) {
        console.log("[DEBUG]:", msg)
    }
}

// second f argument1 argument2
const doWithTimeout2 = (t, f, a1, a2) => {
    // 30 seconds
    const TIMEOUT = t * 1000;

    let timeStart = currentTime();
    let timeNow = timeStart;
    // retry and try to open the html file
    while (true) {
        timeNow = currentTime();
        debugLog(timeNow - timeStart)
        if (timeNow - timeStart > TIMEOUT) {
            debugLog("consume timeout!!!");
            throw new Error('read output timeout');
        }

        try {
            f(a1, a2);
            debugLog("successful!!!");
            break;
        } catch (e) {
            debugLog("retrying ...");
        } finally {
            debugLog("continue retrying");
        }
    }

    debugLog("done!!!");
}


function clean() {
    cleanDIR(INDIR);
    cleanDIR(OUTDIR);
    if (debug) {
        SESSION = []
    }
}

function cleanDIR(dir) {
    let files = fs.readdirSync(dir);
    files.forEach((file) => {
        fs.unlinkSync(path.join(dir, file));
    })
}

const corsOptions = {
    "origin": "*",
    "methods": "GET,HEAD,PUT,PATCH,POST,DELETE",
    "preflightContinue": false,
    "optionsSuccessStatus": 204
}


// =============== HANDLERS ====================

function consume(base) {
    let path = norm_path(OUTDIR, base)
    doWithTimeout2(30, fs.openSync, path, "r");
    return readExchange(base);
}

async function activate_analysis_async() {
    const {spawn} = require('child_process');
    infoLog("start analysis...")

    let cmd = `${OPT} ${SONAR} ${INDIR} ${OUTDIR}`
    debugLog("cmd: ", cmd)
    const child = spawn(JAVA, [OPT, SONAR, INDIR, OUTDIR])
    let data = "";
    for await (const chunk of child.stdout) {
        debugLog(chunk.toString());
        data += chunk;
    }
    let error = "";
    for await (const chunk of child.stderr) {
        debugLog(chunk.toString());
        error += chunk;
    }

    const exitCode = await new Promise((resolve, reject) => {
        child.on('close', resolve);
    });

    if (exitCode) {
        writeSonarLog(`${new Date} \n[INFO] ${data},\n [ERROR] ${error}`);
        throw new Error(`subprocess error exit ${exitCode}, ${error}`);
    }

    return data;
}

const trimChar = (s) => {

    let c = s.split('')
    while (c.length > 0 && (c[0] === ' ' || c[0] === '\n' || c[0] === '\t')) {
        c.shift()
    }
    while (c.length > 0 && (c[c.length - 1] === ' ' || c[c.length - 1] === '\n' || c[c.length - 1] === '\t')) {
        c.pop()
    }
    return c.join('')
}

const format_out = (path) => {
    return path + ".html";
}


async function produce(code) {
    infoLog("Please Wait For Analysis Done.")
    // sync
    let base = writeExchange(code);
    debugLog("file_name:" + base)
    await activate_analysis_async()

    return format_out(base);
}

/*
  ================== WEB CODES ===============
 */
const app = express();
app.use(bodyParser.text({type: "text/plain"}));
app.use(cors(corsOptions))

app.get('/', (req, res) => {
    res.sendFile(__dirname + '/index.html');
});

app.post('/', upload.array('multi-files'), (req, res) => {
    res.redirect('/result');
});


app.post('/result', (req, res) => {
    debugLog("processing source:\n " + req.body);
    debugLog("cleaning: \n " + req.body);
    clean();
    let code = req.body;

    let tcode = trimChar(code);
    let hit = lookUpSession(tcode);
    if (hit) {
        debugLog("hit cache");
        res.send(hit);
    } else {
        produce(code)
            .then((base) => {
                let data = consume(base)
                infoLog("analysis finish.")
                debugLog(data);
                putSession(tcode, data);
                res.send(data);
                infoLog("data sent.")
            })
            .catch((err) => {
                res.send(err.stack);
                console.error("catch error", err);
            });
    }
});

const debug = false;

const port = 8000;
app.listen(port, 'localhost', () => {
    infoLog("LISTENING:", port)
    if (!fs.existsSync(INDIR)) {
        fs.mkdirSync(INDIR, {recursive: true})
        debugLog("tmp in dir" + INDIR + " created!!!")
    } else {
        debugLog("tmp in dir exists!!!")
    }

    if (!fs.existsSync(OUTDIR)) {
        fs.mkdirSync(OUTDIR, {recursive: true})
        debugLog("tmp out " + OUTDIR + "dir created!!!")
    } else {
        debugLog("tmp out " + OUTDIR + "dir exists!!!")
    }
});
