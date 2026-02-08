"use client"

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <h2 className="text-lg font-semibold text-slate-900">Unable to load execution</h2>
      <p className="mt-2 text-sm text-slate-600">{error.message}</p>
      <button
        onClick={reset}
        className="mt-4 rounded-md bg-primary-600 px-4 py-2 text-sm font-medium text-white"
      >
        Try again
      </button>
    </div>
  )
}
