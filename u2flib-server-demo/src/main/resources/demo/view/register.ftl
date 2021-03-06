<html>
<head>
<title>Java U2F Demo</title>

<script src="chrome-extension://pfboblefjcgdjicmnffhdgionmgcdmne/u2f-api.js"></script>

<script>
var request = ${data};
setTimeout(function() {
    u2f.register(request.registerRequests, request.authenticateRequests,
    function(data) {
        console.log(data);
        var form = document.getElementById('form');
        var reg = document.getElementById('tokenResponse');
        if(data.errorCode) {
            alert("U2F failed with error: " + data.errorMessage);
            return;
        }
        reg.value=JSON.stringify(data);
        form.submit();
    });
}, 10000);
</script>

</head>
    <body>
    <p>Touch your U2F token.</p>
        <form method="POST" action="finishRegistration" id="form" onsubmit="return false;">
            <input type="hidden" name="username" value="${username}"/>
            <input type="hidden" name="tokenResponse" id="tokenResponse"/>
        </form>
    </body>
</html>
