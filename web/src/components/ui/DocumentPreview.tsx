import { useEffect, useId, useRef, useState } from 'react'
import { X } from 'lucide-react'
import { ApiError, fetchDocumentContent } from '@/api/client'
import type { LoanDocument } from '@/types'
import { PdfPreview } from './PdfPreview'

type PreviewContent =
  | { kind: 'image'; url: string }
  | { kind: 'pdf'; file: Blob }
  | { kind: 'error'; message: string }

export function DocumentPreview({ document, onClose }: { document: LoanDocument; onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const title = useId()
  const [content, setContent] = useState<PreviewContent | null>(null)
  const { id, fileName } = document

  useEffect(() => {
    if (dialog.current && !dialog.current.open) dialog.current.showModal()
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    let imageUrl: string | undefined
    void fetchDocumentContent(id, controller.signal).then((file) => {
      if (controller.signal.aborted) return
      const mime = file.type.split(';')[0]
      if (mime === 'image/jpeg' || mime === 'image/png') {
        imageUrl = URL.createObjectURL(file)
        setContent({ kind: 'image', url: imageUrl })
      } else if (mime === 'application/pdf') {
        setContent({ kind: 'pdf', file })
      } else {
        setContent({ kind: 'error', message: 'This file type cannot be previewed. Use Download to open it separately.' })
      }
    }).catch((err: unknown) => {
      if (!controller.signal.aborted) {
        setContent({ kind: 'error', message: err instanceof ApiError ? err.message : 'Could not load the document. Try opening it again.' })
      }
    })
    return () => {
      controller.abort()
      if (imageUrl) URL.revokeObjectURL(imageUrl)
    }
  }, [id])

  return (
    <dialog
      ref={dialog}
      aria-labelledby={title}
      onClose={onClose}
      onClick={(event) => { if (event.currentTarget === event.target) event.currentTarget.close() }}
      className="m-auto max-h-[calc(100dvh-1rem)] w-[calc(100%-1rem)] max-w-4xl overflow-hidden rounded-2xl border border-neutral-200 bg-white p-0 shadow-2xl backdrop:bg-neutral-950/65"
    >
      <div className="flex max-h-[calc(100dvh-1.25rem)] flex-col">
        <header className="flex shrink-0 items-center justify-between gap-2 border-b border-neutral-200 px-4 py-2">
          <h2 id={title} className="min-w-0 break-words text-base font-semibold text-neutral-900">{fileName}</h2>
          <button
            type="button"
            aria-label="Close preview"
            onClick={() => dialog.current?.close()}
            className="grid size-11 shrink-0 place-items-center rounded-lg text-neutral-700 hover:bg-neutral-100 focus-visible:outline-2 focus-visible:outline-primary-500"
          >
            <X aria-hidden="true" size={20} />
          </button>
        </header>
        <div className="min-h-0 overflow-auto bg-neutral-100">
          {content === null && <p role="status" className="p-6 text-sm text-neutral-600">Loading document…</p>}
          {content?.kind === 'error' && <p role="alert" className="p-6 text-sm text-danger-700">{content.message}</p>}
          {content?.kind === 'image' && (
            <img
              src={content.url}
              alt={fileName}
              onError={() => setContent({ kind: 'error', message: 'This image could not be displayed. Use Download to open it separately.' })}
              className="mx-auto block h-auto max-w-full p-3 sm:p-4"
            />
          )}
          {content?.kind === 'pdf' && <PdfPreview file={content.file} />}
        </div>
      </div>
    </dialog>
  )
}
