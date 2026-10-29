import socket, threading, time, re, hashlib, random, sys
PORT=int(sys.argv[1]); USER="20060108"; PASS="secreto"; REALM="asterisk"; NONCE="5f3a"
srv=socket.socket(socket.AF_INET,socket.SOCK_DGRAM); srv.bind(("127.0.0.1",PORT))
stats={"reg_ok":0,"options_ok":0,"invites":0,"acks":0,"byes_rx":0,"cancels":0,"rtp_rx":0,"auth_bad":0,"ack_487":0}
calls={}
def hdr(m,n):
    r=re.search(rf"^{n}:\s*(.*)$",m,re.M|re.I); return r.group(1).strip() if r else ""
def resp(m,code,frase,extra="",tag=True,body=""):
    via="\r\n".join("Via: "+v for v in re.findall(r"^Via:\s*(.*)$",m,re.M))
    via=via.replace(";rport",";rport=%d;received=200.1.2.3"%cli[0][1],1) if cli[0] else via
    to=hdr(m,"To")+(";tag=ast99" if tag and "tag=" not in hdr(m,"To") else "")
    return (f"SIP/2.0 {code} {frase}\r\n{via}\r\nFrom: {hdr(m,'From')}\r\nTo: {to}\r\nCall-ID: {hdr(m,'Call-ID')}\r\nCSeq: {hdr(m,'CSeq')}\r\n{extra}"
            f"Content-Length: {len(body)}\r\n\r\n{body}").encode()
def check_auth(m,method):
    a=hdr(m,"Authorization")
    if not a: return None
    g=lambda k: re.search(k+r'="?([^",]+)',a).group(1)
    ha1=hashlib.md5(f"{USER}:{REALM}:{PASS}".encode()).hexdigest(); ha2=hashlib.md5(f"{method}:{g('uri')}".encode()).hexdigest()
    exp=hashlib.md5(f"{ha1}:{NONCE}:{g('nc')}:{g('cnonce')}:auth:{ha2}".encode()).hexdigest() if 'qop' in a else hashlib.md5(f"{ha1}:{NONCE}:{ha2}".encode()).hexdigest()
    return exp==g('response')
cli=[None]
CH='WWW-Authenticate: Digest algorithm=MD5, realm="asterisk", nonce="5f3a", qop="auth"\r\n'
rtp=socket.socket(socket.AF_INET,socket.SOCK_DGRAM); rtp.bind(("127.0.0.1",PORT+100)); rtp.settimeout(0.02)
rtp_dst=[None]
def rtp_loop():
    seq=0
    while True:
        try:
            d,a=rtp.recvfrom(2048); stats["rtp_rx"]+=1; rtp_dst[0]=a
        except socket.timeout: pass
        if rtp_dst[0]:
            ruido=(time.time()%3)<1.0
            payload=bytes(random.randint(0,255) for _ in range(160)) if ruido else b"\xd5"*160
            pkt=bytes([0x80,8])+seq.to_bytes(2,'big')+(seq*160).to_bytes(4,'big')+b"\x01\x02\x03\x04"+payload
            rtp.sendto(pkt,rtp_dst[0]); seq=(seq+1)&0xffff
threading.Thread(target=rtp_loop,daemon=True).start()
def opt(n):
    return (f"OPTIONS sip:{USER}@x SIP/2.0\r\nVia: SIP/2.0/UDP 127.0.0.1:{PORT};branch=z9hG4bKo{n}\r\nFrom: <sip:a@x>;tag=o{n}\r\nTo: <sip:{USER}@x>\r\n"
            f"Call-ID: o{n}\r\nCSeq: 1 OPTIONS\r\nContent-Length: 0\r\n\r\n").encode()
def spam():
    n=0
    while True:
        time.sleep(1)
        if cli[0]: n+=1; srv.sendto(opt(n),cli[0])
threading.Thread(target=spam,daemon=True).start()
while True:
    d,a=srv.recvfrom(65535); m=d.decode(errors="ignore"); cli[0]=a
    first=m.split("\r\n")[0]
    if len(d)<10: continue
    if first.startswith("REGISTER"):
        ok=check_auth(m,"REGISTER")
        if ok is None: srv.sendto(resp(m,401,"Unauthorized",CH),a)
        elif ok: stats["reg_ok"]+=1; srv.sendto(resp(m,200,"OK","Expires: 60\r\n"),a)
        else: stats["auth_bad"]+=1; srv.sendto(resp(m,401,"Unauthorized",CH),a)
    elif first.startswith("INVITE"):
        num=re.search(r"sip:(\d+)@",first).group(1)
        srv.sendto(resp(m,100,"Trying",tag=False),a)
        ok=check_auth(m,"INVITE")
        if ok is None: srv.sendto(resp(m,401,"Unauthorized",CH),a); continue
        if not ok: stats["auth_bad"]+=1; srv.sendto(resp(m,403,"Forbidden"),a); continue
        stats["invites"]+=1; calls[hdr(m,"Call-ID")]=m
        srv.sendto(resp(m,180,"Ringing"),a)
        if num.endswith("1"):
            def ans(m=m,a=a):
                time.sleep(0.8)
                sdp=f"v=0\r\no=- 1 1 IN IP4 127.0.0.1\r\ns=-\r\nc=IN IP4 127.0.0.1\r\nt=0 0\r\nm=audio {PORT+100} RTP/AVP 8 101\r\na=rtpmap:8 PCMA/8000\r\n"
                srv.sendto(resp(m,200,"OK",f"Contact: <sip:{num}@127.0.0.1:{PORT}>\r\nContent-Type: application/sdp\r\n",body=sdp),a)
            threading.Thread(target=ans).start()
        elif num.endswith("2"):
            srv.sendto(resp(m,486,"Busy Here"),a)
    elif first.startswith("ACK"):
        if "SIP/2.0" in first: stats["acks"]+=1
        if "CSeq" in m and calls and any(c in m for c in calls) and "tag=ast99" in m: pass
    elif first.startswith("BYE"):
        stats["byes_rx"]+=1; srv.sendto(resp(m,200,"OK"),a)
    elif first.startswith("CANCEL"):
        stats["cancels"]+=1; srv.sendto(resp(m,200,"OK"),a)
        inv=calls.get(hdr(m,"Call-ID"))
        if inv: srv.sendto(resp(inv,487,"Request Terminated"),a)
    elif first.startswith("SIP/2.0 200") and "OPTIONS" in m: stats["options_ok"]+=1
    open(f"/tmp/fake_stats_{PORT}","w").write(repr(stats))
