char *controlwebpage = "<!DOCTYPE html>\
<html lang='en'>\
<head>\
<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\
<title>fan controller</title>\
</head>\
<body>\
\
<table><tr>\
\
<td>Power:</td>\
<td><input id='power' size='6' value='0'> (0 - 10000)</td></tr>\
\
<tr>\
<td>Airflow:</td>\
<td><input id='air' size='6' value='5000'> (0 - 10000)</td></tr>\
\
\
<tr><td colspan='2'>\
<button id='set' onclick='setParams()'>set power and flow</button>\
\
<tr>\
<td>P:</td>\
<td><input id='p' size='6' value='100'> (-∞ - ∞) * 1000</td></tr>\
\
<tr>\
<td>D:</td>\
<td><input id='d' size='6' value='100'> (-∞ - ∞) * 1000</td></tr>\
\
<tr>\
<td>I:</td>\
<td><input id='i' size='6' value='100'> (-∞ - ∞) * 1000</td></tr>\
\
<tr>\
<td>Temperature:</td>\
<td><input id='temp' size='6' value='200'> (0 - ∞)</td></tr>\
\
<tr>\
<td>Alpha:</td>\
<td><input id='alpha' size='6' value='100'> (0 - 1) * 1000</td></tr>\
\
<tr>\
<td>Delay:</td>\
<td><input id='del' size='6' value='100'> (0 - 1000)</td></tr>\
\
<tr><td colspan='2'>&nbsp;</td></tr>\
\
<tr><td colspan='2'>\
<button id='set' onclick='setPD()'>set PD</button>\
</td></tr>\
\
<tr><td colspan='2'>&nbsp;</td></tr>\
\
<tr>\
<td>Temperature:</td>\
<td><label id='temperature'>0</td>\
</tr>\
\
<tr>\
<td>Status:</td>\
<td><label id='status'>0</td>\
</tr>\
\
</table>\
<br><br>\
<canvas id='cnvs' width='1200' height='500' style='background-color:#333'></canvas>\
\
<script>\
function updateTemperature()\
{\
  console.log('tmp');\
  const xhttp = new XMLHttpRequest();\
  xhttp.onreadystatechange = function() {\
  if (this.readyState == 4 && this.status == 200) {\
     temperature.innerHTML = this.responseText;\
     draw(this.responseText);\
     console.log(this.responseText);\
   }\
  };\
  xhttp.open('GET', '/t');\
  xhttp.send(); \
}\
\
\
function setParams()\
{\
  var pow = parseInt(power.value);\
  var flow = parseInt(air.value);\
  if (isNaN(pow) || isNaN(flow) || (pow < 0) || (pow > 10000) || (flow < 0) || (flow > 10000)) \
  {\
    console.log('illegal input ' + pow + ', ' + flow);\
    return;\
  }\
  console.log('setting power=' + pow + ', airflow=' + flow);\
  const xhttp1 = new XMLHttpRequest();\
  const xhttp2 = new XMLHttpRequest();\
  xhttp1.onreadystatechange = function() {\
  if (this.readyState == 4 && this.status == 200) {\
     status1 ++;\
     if (status1 + status2 == 2)   \
        status.innerHTML = 'OK';\
   }\
  };\
  xhttp2.onreadystatechange = function() {\
  if (this.readyState == 4 && this.status == 200) {\
     status2 ++;\
     if (status1 + status2 == 2)   \
        status.innerHTML = 'OK';\
   }\
  };\
  status1 = 0;\
  status2 = 0;\
  xhttp1.open('GET', '/setdac?a=0`  &b=' + pow);\
  xhttp2.open('GET', '/setdac?a=1&b=' + flow);\
  status.innerHTML = 'SENT';\
  xhttp1.send(); \
  xhttp2.send(); \
  ctx.strokeStyle='#80EE40';\
  ctx.beginPath();\
      ctx.moveTo(cnvx, 0);\
      ctx.lineTo(cnvx, 499);\
      ctx.stroke();\
}\
\
function setPD()\
{\
  var pcoef = parseInt(p_value.value);\
  var dcoef = parseInt(d_value.value);\
  var icoef = parseInt(i_value.value);\
  var treq = parseInt(req_temp.value);\
  lasttreq = Number(treq);\
  var alp = parseInt(alpha.value);\
  var delaj = parseInt(del.value);\
\
  const xhttp1 = new XMLHttpRequest();\
  xhttp1.onreadystatechange = function() {\
  if (this.readyState == 4 && this.status == 200) {\
      status.innerHTML = 'OK';\
      cnvx = 1200;\
   }\
  };\
  \
  xhttp1.open('GET', '/setpd?a=' + pcoef + '&b=' + dcoef + '&c=' + treq + '&d=' + alp + '&e=' + delaj + '&f=' + icoef);\
  status.innerHTML = 'SENT';\
  xhttp1.send(); \
}\
\
function draw(tmpr) \
{\    
    if (cnvx >= 1197)\
    {\
      ctx.fillStyle='#203010';\
      ctx.fillRect(0, 0, 1200, 500);\
      cnvx = 0;\
      ctx.strokeStyle='#EE8040';\
      ctx.beginPath();\
      ctx.moveTo(0, 500 - lasttreq);\
      ctx.lineTo(1199, 500 - lasttreq);\
      ctx.stroke();\
      ctx.strokeStyle='#FFFFFF';\
    }\
    ctx.beginPath();\
    ctx.moveTo(cnvx, 500 - lasttmpr);\
    lasttmpr = Number(tmpr);\
    cnvx = cnvx + 3;\
    ctx.lineTo(cnvx, 500 - lasttmpr);\
    ctx.stroke();\
}\
console.log('hi');\
var power = document.getElementById('power');\
var air = document.getElementById('air');\
var temperature = document.getElementById('temperature');\
var status = document.getElementById('status');\
var p_value = document.getElementById('p');\
var d_value = document.getElementById('d');\
var i_value = document.getElementById('i');\
var req_temp = document.getElementById('temp');\
var alpha = document.getElementById('alpha');\
var del = document.getElementById('del');\
var cnvs = document.getElementById('cnvs');\
var ctx = cnvs.getContext('2d');\
ctx.strokeStyle='#FFFFFF';\
var cnvx = 0;\
ctx.fillStyle='#203010';\
ctx.fillRect(0, 0, 1200, 500);\
var lasttmpr = 0;\
var lasttreq = 0;\
var status1 = 0;\
var status2 = 0;\
setInterval(updateTemperature, 500); \
</script>\
</body>\
</html>";
