import fs from "fs"
import path from "path"

const normalizeEmail = (value: string) => value.trim().toLowerCase()

const readAdminEmailsFile = (): string[] => {
  try {
    const filePath = path.join(process.cwd(), "resources", "admin-emails.json")
    const raw = fs.readFileSync(filePath, "utf8")
    const parsed = JSON.parse(raw) as { admins?: string[] }
    return Array.isArray(parsed.admins) ? parsed.admins : []
  } catch {
    return []
  }
}

export const getAdminEmails = (): string[] => {
  const fromEnv = (process.env.ADMIN_EMAILS || "")
    .split(",")
    .map(normalizeEmail)
    .filter(Boolean)

  const fromFile = readAdminEmailsFile().map(normalizeEmail)
  return Array.from(new Set([...fromFile, ...fromEnv]))
}

export const isAdminEmail = (email: string): boolean =>
  getAdminEmails().includes(normalizeEmail(email))
