const resources = import.meta.glob<string>('/node_modules/pdfjs-dist/{cmaps,standard_fonts,wasm}/*.{bcmap,pfb,ttf,wasm}', {
  eager: true,
  query: '?url&no-inline',
  import: 'default',
})

const resourcesByName = new Map(Object.entries(resources).map(([path, url]) => [path.split('/').at(-1), url]))

/** Resolve PDF.js fonts, CMaps and decoders to the app's own built assets. */
export class PdfResources {
  async fetch({ filename }: { filename: string }): Promise<Uint8Array> {
    const url = resourcesByName.get(filename)
    if (!url) throw new Error('Missing PDF resource')
    const response = await fetch(url)
    if (!response.ok) throw new Error('Could not read PDF resource')
    return new Uint8Array(await response.arrayBuffer())
  }
}
