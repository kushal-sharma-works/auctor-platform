import "@testing-library/jest-dom"

// Polyfill fetch for jest environment
if (!global.Request) {
	global.Request = class Request {} as any
}
if (!global.Response) {
	global.Response = class Response {} as any
}
if (!global.Headers) {
	global.Headers = class Headers {} as any
}
if (!global.fetch) {
	global.fetch = jest.fn() as any
}

// Mock Next.js router
jest.mock("next/navigation", () => ({
	useRouter: jest.fn(() => ({
		push: jest.fn(),
		replace: jest.fn(),
		prefetch: jest.fn(),
		back: jest.fn(),
	})),
	usePathname: jest.fn(() => "/"),
	useSearchParams: jest.fn(() => new URLSearchParams()),
}))

// Suppress console errors in tests
const originalError = console.error
beforeAll(() => {
	console.error = (...args: any[]) => {
		if (
			typeof args[0] === "string" &&
			(args[0].includes("Warning: ReactDOM.render") ||
				args[0].includes("Not implemented: HTMLFormElement.prototype.submit"))
		) {
			return
		}
		originalError.call(console, ...args)
	}
})

afterAll(() => {
	console.error = originalError
})
