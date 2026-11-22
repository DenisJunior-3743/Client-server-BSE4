import { useEffect, useRef, useState } from 'react'
import type { PDFDocumentLoadingTask, PDFDocumentProxy, RenderTask } from 'pdfjs-dist'
import { Button } from './Button'

export function PdfPreview({ file }: { file: Blob }) {
  const canvas = useRef<HTMLCanvasElement>(null)
  const [pdf, setPdf] = useState<PDFDocumentProxy | null>(null)
  const [pageNumber, setPageNumber] = useState(1)
  const [renderedPage, setRenderedPage] = useState(0)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let active = true
    let loading: PDFDocumentLoadingTask | undefined
    void Promise.all([
      import('pdfjs-dist'),
      import('pdfjs-dist/build/pdf.worker.min.mjs?url'),
      import('pdfjs-dist/build/pdf.renderer.min.mjs?url'),
      import('@/lib/pdfResources'),
      file.arrayBuffer(),
    ]).then(async ([pdfjs, worker, renderer, { PdfResources }, data]) => {
      if (!active) return
      pdfjs.GlobalWorkerOptions.workerSrc = worker.default
      pdfjs.GlobalWorkerOptions.rendererSrc = renderer.default
      loading = pdfjs.getDocument({
        data: new Uint8Array(data),
        useWorkerFetch: false,
        BinaryDataFactory: PdfResources,
      })
      const document = await loading.promise
      if (active) setPdf(document)
    }).catch(() => {
      if (active) setFailed(true)
    })
    return () => {
      active = false
      void loading?.destroy().catch(() => {})
    }
  }, [file])

  useEffect(() => {
    const element = canvas.current
    if (!pdf || !element) return
    let active = true
    let rendering: RenderTask | undefined
    void pdf.getPage(pageNumber).then(async (page) => {
      if (!active) return
      const original = page.getViewport({ scale: 1 })
      const viewport = page.getViewport({ scale: Math.min(1600 / original.width, 2400 / original.height) })
      element.width = Math.ceil(viewport.width)
      element.height = Math.ceil(viewport.height)
      rendering = page.render({ canvas: element, viewport })
      await rendering.promise
      if (active) setRenderedPage(pageNumber)
    }).catch(() => {
      if (active) setFailed(true)
    })
    return () => {
      active = false
      rendering?.cancel()
    }
  }, [pdf, pageNumber])

  if (failed) {
    return <p role="alert" className="p-6 text-sm text-danger-700">This PDF could not be previewed. Use Download to open it separately.</p>
  }

  const ready = renderedPage === pageNumber
  return (
    <div>
      {pdf && pdf.numPages > 1 && (
        <div className="sticky top-0 z-10 flex flex-wrap items-center justify-center gap-2 border-b border-neutral-200 bg-white px-3 py-2 sm:gap-4">
          <Button type="button" size="sm" variant="secondary" disabled={pageNumber === 1}
            onClick={() => setPageNumber(pageNumber - 1)}>Previous</Button>
          <span aria-live="polite" className="text-sm text-neutral-700">Page {pageNumber} of {pdf.numPages}</span>
          <Button type="button" size="sm" variant="secondary" disabled={pageNumber === pdf.numPages}
            onClick={() => setPageNumber(pageNumber + 1)}>Next</Button>
        </div>
      )}
      {!ready && <p role="status" className="p-6 text-sm text-neutral-600">Rendering PDF…</p>}
      <div className="p-3 sm:p-4">
        <canvas ref={canvas} hidden={!ready} role="img" aria-label={`PDF page ${pageNumber}`}
          className="mx-auto h-auto w-full max-w-[50rem] bg-white" />
      </div>
    </div>
  )
}
