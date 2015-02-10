<html>
	<head>
		<title>DX Dashboard</title>
		<link rel="stylesheet" type="text/css" media="screen,print" href="/assets/${startId}/styles/dashboard.css">
	</head>
	<body>
		<h1>The sites</h1>
		<ul>
			<#list sites as site>
				<li>${site}</li>
			</#list>
		</ul>
	</body>
</html>