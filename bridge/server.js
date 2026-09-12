import express from 'express';
import QRCode from 'qrcode';
import pino from 'pino';
import makeWASocket,{useMultiFileAuthState,DisconnectReason,fetchLatestBaileysVersion} from '@whiskeysockets/baileys';

const app=express();app.use(express.json());const port=process.env.PORT||3000;const sessions=new Map();
async function start(phone){let s=sessions.get(phone);if(s?.sock)return s;const {state,saveCreds}=await useMultiFileAuthState('./auth/'+phone);const {version}=await fetchLatestBaileysVersion();const sock=makeWASocket({version,auth:state,logger:pino({level:'silent'}),printQRInTerminal:false});s={sock,code:null};sessions.set(phone,s);sock.ev.on('creds.update',saveCreds);sock.ev.on('connection.update',async u=>{if(u.qr)s.code=await QRCode.toDataURL(u.qr);if(u.connection==='close'){sessions.delete(phone);if(u.lastDisconnect?.error?.output?.statusCode!==DisconnectReason.loggedOut)setTimeout(()=>start(phone),1500);}});return s;}
app.get('/health',(req,res)=>res.json({ok:true,service:'jarvis-baileys-bridge'}));
app.post('/pair',async(req,res)=>{try{const phone=String(req.body.phone||'').replace(/\D/g,'');if(!phone)return res.status(400).json({error:'phone required'});const s=await start(phone);res.json({ok:true,phone,qr:s.code||null,message:s.code?'Scan QR in dashboard':'Session starting; poll /pair/status'});}catch(e){res.status(500).json({error:e.message});}});
app.post('/pair/code',async(req,res)=>{try{const phone=String(req.body.phone||'').replace(/\D/g,'');if(!phone)return res.status(400).json({error:'phone required'});const s=await start(phone);const code=await s.sock.requestPairingCode(phone);res.json({ok:true,phone,code});}catch(e){res.status(500).json({error:e.message});}});
app.get('/pair/status',(req,res)=>{const phone=String(req.query.phone||'').replace(/\D/g,'');const s=sessions.get(phone);res.json({ok:!!s,qr:s?.code||null,connected:!!s?.sock?.user});});
app.listen(port,()=>console.log('JARVIS Baileys bridge on '+port));
