FROM node:20-alpine AS deps

WORKDIR /app
COPY package*.json ./
RUN npm ci

FROM node:20-alpine AS builder

WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:20-alpine AS runner

WORKDIR /app
ENV NODE_ENV=production

RUN addgroup -S app && adduser -S app -G app

COPY --from=builder /app/package*.json ./
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next ./.next

USER app

EXPOSE 3000

HEALTHCHECK --interval=10s --timeout=3s --retries=5 CMD wget -qO- http://localhost:3000/ || exit 1

CMD ["npm","run","start"]
