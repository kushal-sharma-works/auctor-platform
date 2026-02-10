import { onCLS, onLCP, onINP } from 'web-vitals'

export function reportWebVitals() {
  const handler = (metric: { name: string; value: number; rating?: string; id: string }) => {
    console.info('web_vital', {
      name: metric.name,
      value: metric.value,
      rating: metric.rating,
      id: metric.id,
    })
  }

  onCLS(handler)
  onLCP(handler)
  onINP(handler)
}
