import "@testing-library/jest-dom"

const undici = require("next/dist/compiled/undici")

if (!global.Request) {
	global.Request = undici.Request
}
if (!global.Response) {
	global.Response = undici.Response
}
if (!global.Headers) {
	global.Headers = undici.Headers
}
