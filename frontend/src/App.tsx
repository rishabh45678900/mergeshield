
import { useRef, useState } from 'react'

type Finding = {
  ruleId: string
  severity: string
  location: string
  summary: string
  oldValue: string
  newValue: string
  evidence: string
}

type Result = {
  status: string
  breakingCount: number
  warningCount: number
  findings: Finding[]
}

const oldDemo = JSON.stringify(
  {
    openapi: '3.0.3',
    info: { title: 'Store API', version: '1.0.0' },
    paths: {
      '/users': {
        get: { responses: { '200': { description: 'List users' } } },
      },
      '/orders': {
        get: { responses: { '200': { description: 'List orders' } } },
      },
    },
  },
  null,
  2,
)

const newDemo = JSON.stringify(
  {
    openapi: '3.0.3',
    info: { title: 'Store API', version: '2.0.0' },
    paths: {
      '/users': {
        get: { responses: { '200': { description: 'List users' } } },
      },
    },
  },
  null,
  2,
)

export default function App() {
  const [oldSpec, setOldSpec] = useState(oldDemo)
  const [newSpec, setNewSpec] = useState(newDemo)
  const [result, setResult] = useState<Result | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const oldFileRef = useRef<HTMLInputElement>(null)
  const newFileRef = useRef<HTMLInputElement>(null)

  async function loadFile(file: File | null, target: 'old' | 'new') {
    if (!file) return

    if (file.size > 1_000_000) {
      setError(`${file.name} is larger than the 1 MB limit.`)
      return
    }

    try {
      const text = await file.text()

      if (target === 'old') {
        setOldSpec(text)
      } else {
        setNewSpec(text)
      }

      setResult(null)
      setError('')
    } catch {
      setError(`Could not read ${file.name}. Try a UTF-8 JSON or YAML file.`)
    }
  }

  async function analyze() {
    setLoading(true)
    setError('')
    setResult(null)

    try {
      if (
        new Blob([oldSpec]).size > 1_000_000 ||
        new Blob([newSpec]).size > 1_000_000
      ) {
        throw new Error('Each specification must be 1 MB or smaller.')
      }

      const base = (
        import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
      ).replace(/\/$/, '')

      const response = await fetch(`${base}/api/analyze`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ oldSpec, newSpec }),
      })

      const raw = await response.text()

      let body: { message?: string; error?: string } & Partial<Result>

      try {
        body = JSON.parse(raw) as {
          message?: string
          error?: string
        } & Partial<Result>
      } catch {
        throw new Error(
          response.ok
            ? 'The API returned an unreadable response.'
            : `API request failed with HTTP ${response.status}. Check the backend logs.`,
        )
      }

      if (!response.ok) {
        throw new Error(
          body.message || body.error || `Analysis failed with HTTP ${response.status}`,
        )
      }

      if (!Array.isArray(body.findings)) {
        throw new Error('The API response is missing its findings list.')
      }

      setResult(body as Result)
    } catch (e) {
      setError(
        e instanceof Error
          ? e.message
          : 'Could not reach the API. Is the backend running?',
      )
    } finally {
      setLoading(false)
    }
  }

  function downloadReport() {
    if (!result) return

    const report = {
      product: 'MergeShield',
      generatedAt: new Date().toISOString(),
      limitations:
        'Specification-level findings only; not a guarantee of runtime compatibility.',
      ...result,
    }

    const blob = new Blob([JSON.stringify(report, null, 2)], {
      type: 'application/json;charset=utf-8',
    })

    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')

    link.href = url
    link.download = 'mergeshield-report.json'
    link.style.display = 'none'

    document.body.appendChild(link)
    link.click()
    link.remove()

    // Allow the browser to start the download before releasing the URL.
    window.setTimeout(() => URL.revokeObjectURL(url), 1000)
  }

  function resetDemo() {
    setOldSpec(oldDemo)
    setNewSpec(newDemo)
    setResult(null)
    setError('')
  }

  return (
    <main className="shell">
      <nav className="nav">
        <a className="brand" href="#">
          <span className="brand-mark">M</span> MergeShield
        </a>
        <span className="nav-label">
          <i className="pulse" /> API COMPATIBILITY ENGINE
        </span>
      </nav>

      <section className="hero">
        <div className="eyebrow">— &nbsp; SHIP WITH CONFIDENCE</div>
        <h1>
          Catch breaking API changes
          <br />
          <span>before they ship.</span>
        </h1>
        <p>
          Compare two OpenAPI specifications. Find compatibility risks with
          clear evidence before your next release.
        </p>
      </section>

      <section className="panel">
        <div className="section-heading">
          <div>
            <small>01 / INPUT</small>
            <h2>Compare specifications</h2>
          </div>

          <div className="input-actions">
            <button
              className="secondary"
              onClick={() => oldFileRef.current?.click()}
            >
              Import base
            </button>
            <button
              className="secondary"
              onClick={() => newFileRef.current?.click()}
            >
              Import proposed
            </button>
            <button className="secondary" onClick={resetDemo}>
              Load demo
            </button>

            <input
              ref={oldFileRef}
              className="hidden-file"
              type="file"
              accept=".json,.yaml,.yml,application/json,text/yaml"
              aria-label="Import base OpenAPI file"
              onChange={(e) => {
                void loadFile(e.target.files?.[0] || null, 'old')
                e.currentTarget.value = ''
              }}
            />

            <input
              ref={newFileRef}
              className="hidden-file"
              type="file"
              accept=".json,.yaml,.yml,application/json,text/yaml"
              aria-label="Import proposed OpenAPI file"
              onChange={(e) => {
                void loadFile(e.target.files?.[0] || null, 'new')
                e.currentTarget.value = ''
              }}
            />
          </div>
        </div>

        <div className="editors">
          <label className="editor">
            <span>
              ● &nbsp; BASE VERSION <small>OLD SPEC</small>
            </span>
            <textarea
              spellCheck={false}
              aria-label="Old OpenAPI specification"
              value={oldSpec}
              onChange={(e) => setOldSpec(e.target.value)}
            />
          </label>

          <label className="editor">
            <span>
              ● &nbsp; PROPOSED VERSION <small>NEW SPEC</small>
            </span>
            <textarea
              spellCheck={false}
              aria-label="New OpenAPI specification"
              value={newSpec}
              onChange={(e) => setNewSpec(e.target.value)}
            />
          </label>
        </div>

        <div className="action">
          <p>OpenAPI 3.x · JSON or YAML · Maximum 1 MB each</p>
          <button
            className="primary"
            disabled={loading}
            onClick={analyze}
          >
            {loading ? 'Analyzing…' : 'Run compatibility check'} <b>↗</b>
          </button>
        </div>
      </section>

      {error && (
        <section className="error" role="alert">
          <b>Analysis could not complete</b>
          <p>{error}</p>
          <small>
            Check the specification and confirm that the backend is running at
            the configured API URL.
          </small>
        </section>
      )}

      {result && (
        <section className="panel results">
          <div className="section-heading">
            <div>
              <small>02 / REPORT</small>
              <h2>Analysis results</h2>
            </div>

            <div className="result-actions">
              <span className="status">
                <i className="pulse" /> {result.status}
              </span>
              <button className="secondary" onClick={downloadReport}>
                Export JSON
              </button>
            </div>
          </div>

          <div className="stats">
            <div>
              <small>BREAKING FINDINGS</small>
              <strong className="red">{result.breakingCount}</strong>
            </div>
            <div>
              <small>WARNINGS</small>
              <strong className="amber">{result.warningCount}</strong>
            </div>
            <div>
              <small>TOTAL FINDINGS</small>
              <strong>{result.findings.length}</strong>
            </div>
          </div>

          {result.findings.length === 0 ? (
            <div className="empty">
              <h3>No findings under current rules</h3>
              <p>
                This does not guarantee full compatibility. Review supported
                rules and test the actual API.
              </p>
            </div>
          ) : (
            result.findings.map((f, i) => (
              <article className="finding" key={f.ruleId + f.location + i}>
                <span className={'severity ' + f.severity.toLowerCase()}>
                  {f.severity}
                </span>
                <code>{f.ruleId}</code>
                <h3>{f.summary}</h3>
                <p className="location">{f.location}</p>

                <div className="change">
                  <div>
                    <small>OLD</small>
                    <code>{f.oldValue}</code>
                  </div>
                  <div>
                    <small>NEW</small>
                    <code>{f.newValue}</code>
                  </div>
                </div>

                <p className="evidence">
                  <b>Evidence:</b> {f.evidence}
                </p>
              </article>
            ))
          )}

          <p className="disclaimer">
            Findings are specification-level signals, not proof of runtime
            failure. Referenced schemas are not fully diffed yet.
          </p>
        </section>
      )}

      <footer>
        <span>MERGESHIELD / EARLY MVP</span>
        <span>Built for safer API changes.</span>
      </footer>
    </main>
  )
}
