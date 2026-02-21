import nextJest from "next/jest.js"

const createJestConfig = nextJest({
  dir: "./"
})

const customJestConfig = {
  testEnvironment: "jsdom",
  setupFilesAfterEnv: ["<rootDir>/jest.setup.ts"],
  moduleDirectories: ["node_modules", "<rootDir>/"],
  testMatch: ["<rootDir>/test/**/*.(test|spec).(ts|tsx|js|jsx)"]
}

export default createJestConfig(customJestConfig)
