window.tools = {}

window.largeSpinnerOptions =
	lines: 12
	length: 20
	width: 6
	radius: 22
	color: "#cedeef"
	speed: 1
	trail: 57
	top: "50%"
	left: "50%"
	position: "absolute"
	zIndex: 10

window.mediumSpinnerOptions =
	color: "#333"
	lines: 11
	length: 6
	width: 2
	radius: 6

window.topBarSpinnerOptions =
	lines: 9
	length: 6
	width: 3
	radius: 6
	color: "#ffffff"
	top: "-5px"
	left: "20px"
	position: "relative"

spinner = (options) ->
	options = options || {}

	defaultOptions =
		lines: 9
		length: 4
		width: 2
		radius: 2
		rotate: 0
		color: '#fff'
		speed: 1
		trail: 57
		shadow: false
		hwaccel: false
		className: 'spinner'
		zIndex: 2e9
		top: "auto"
		left: "auto"
		position: "relative"

	new Spinner(_.extend(defaultOptions, options)).spin()

clearSpinningTimeout = (elem) ->
	spinningTimeout = $(elem).data("timeoutId")
	if spinningTimeout
		window.clearTimeout(spinningTimeout)

$.fn.spinStart = (options) ->
	options = options || {}
	$(this).each ->
		$that = $(this)
		clearSpinningTimeout(this)
		$(this).data("timeoutId", window.setTimeout((-> $that.addClass("spinning").prepend(spinner(options).el)), 1))

$.fn.spinStop = ->
	$(this).each ->
		clearSpinningTimeout(this)
		$(this).removeClass("spinning").find(".spinner").remove()


tools.formatValue = (val, formatValue, dataRow) ->
	if formatValue
		formatValueFunction = -> ""
		eval("formatValueFunction = " + formatValue)
		formatValueFunction(val, dataRow)
	else
		val

tools.integerFormat = (value) ->
	value = value || 0
	valueInteger = Math.floor(value)
	valueIntegerString = valueInteger + ""
	valueIntegerArray = []
	length = valueIntegerString.length
	groups = Math.floor(length / 3)
	for group in [(groups + 1)..1]
		start = length - group * 3
		groupString = valueIntegerString.slice((if start <= 0 then 0 else start), start + 3)
		valueIntegerArray.push(groupString) if groupString.length > 0
	valueIntegerArray.join(",")

