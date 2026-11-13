import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import {
  ArrowLeft,
  FileText,
  CheckCircle2,
  XCircle,
  Banknote,
  ShieldCheck,
  MessageCircleQuestion,
  UserCheck,
  Send,
  type LucideIcon,
} from 'lucide-react'
import {
  getApplicationDetail,
  setDocumentStatus,
  recordCreditCheck,
  recordDecision,
  updateApplicationStatus,
  assignToMe,
  disburseLoan,
} from '@/api/applications'
import { isVersionConflict, newIdempotencyKey } from '@/api/client'
import { STATUS_META } from '@/lib/status'
import { useProducts } from '@/lib/useProducts'
import { useAuth } from '@/state/AuthContext'
import { formatUGX, formatDate, formatDateTime } from '@/lib/format'
import { colorForId } from '@/lib/avatarPalette'
import { cn } from '@/lib/cn'
import {
  decisionSchema,
  type DecisionFormValues,
  creditAssessmentSchema,
  type CreditAssessmentFormValues,
  infoRequestSchema,
  type InfoRequestFormValues,
} from '@/lib/schemas'
import type { LoanApplication, CreditCheck, Approval, Disbursement, StatusHistoryEntry } from '@/types'
import { PageSpinner } from '@/components/ui/PageSpinner'
import { Card, CardHeader, CardTitle } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { Button } from '@/components/ui/Button'
import { SelectField } from '@/components/ui/SelectField'
import { TextareaField } from '@/components/ui/TextareaField'
import { TextField } from '@/components/ui/TextField'
import { Avatar } from '@/components/ui/Avatar'

type DetailBundle = Awaited<ReturnType<typeof getApplicationDetail>>

export default function ApplicationDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [bundle, setBundle] = useState<DetailBundle | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [notice, setNotice] = useState<{ tone: 'success' | 'warning' | 'danger'; text: string } | null>(null)
  const loanProducts = useProducts()
  const { user } = useAuth()
  // One key per disbursement. It survives re-renders and failed attempts, so a retry after a lost
  // reply re-sends the same key and the server pays out at most once. Replaced only after success.
  const disbursementKey = useRef(newIdempotencyKey())

  const load = useCallback(() => {
    if (!id) return
    getApplicationDetail(id)
      .then(setBundle)
      .catch(() => setNotFound(true))
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  if (notFound) {
    return (
      <div className="flex flex-col items-center justify-center gap-3 py-20 text-center">
        <p className="text-lg font-semibold text-neutral-800">Application not found</p>
        <Button type="button" variant="secondary" onClick={() => navigate(-1)}>
          Go back
        </Button>
      </div>
    )
  }

  if (!bundle) return <PageSpinner />

  const { application, documents, creditCheck, approval, disbursement, applicantProfile, history } = bundle
  const product = loanProducts.find((p) => p.id === application.productId)

  function refreshWithMessage(message: string) {
    setNotice({ tone: 'success', text: message })
    load()
    window.setTimeout(() => setNotice((current) => (current?.text === message ? null : current)), 3000)
  }

  // Optimistic concurrency, client side: every command carries the `version` this screen loaded.
  // If a colleague changed the application first, the server answers 409 instead of overwriting
  // their work. We reload the latest state and ask the officer to look again before retrying.
  function handleActionError(err: unknown) {
    if (isVersionConflict(err)) {
      setNotice({
        tone: 'warning',
        text: 'A colleague updated this application while you had it open. The latest version is now shown. Review it, then repeat your action if it still applies.',
      })
    } else {
      setNotice({ tone: 'danger', text: err instanceof Error ? err.message : 'The action could not be completed.' })
    }
    load()
  }

  function run(action: () => Promise<unknown>, successMessage: string) {
    return action()
      .then(() => refreshWithMessage(successMessage))
      .catch(handleActionError)
  }

  return (
    <div className="mx-auto max-w-6xl">
      <button
        type="button"
        onClick={() => navigate(-1)}
        className="mb-4 inline-flex cursor-pointer items-center gap-1.5 text-sm font-medium text-neutral-500 hover:text-neutral-800"
      >
        <ArrowLeft className="h-4 w-4" /> Back
      </button>

      {notice && (
        <div
          role="status"
          className={cn(
            'mb-4 rounded-xl px-4 py-3 text-sm font-medium ring-1 ring-inset',
            notice.tone === 'success' && 'bg-success-50 text-success-700 ring-success-200',
            notice.tone === 'warning' && 'bg-warning-50 text-warning-700 ring-warning-200',
            notice.tone === 'danger' && 'bg-danger-50 text-danger-700 ring-danger-200',
          )}
        >
          {notice.text}
        </div>
      )}

      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <Avatar name={application.applicantName} color={colorForId(application.applicantId)} size={52} />
          <div>
            <h1 className="font-display text-2xl font-extrabold text-neutral-900">{application.applicantName}</h1>
            <p className="text-sm text-neutral-500">
              {application.reference ?? application.id} · {application.applicantPhone}
            </p>
          </div>
        </div>
        <StatusBadge status={application.status} />
      </div>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
        <div className="flex flex-col gap-5 lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>Loan Details</CardTitle>
            </CardHeader>
            <dl className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-3">
              <Field label="Product" value={product?.name ?? application.productType} />
              <Field label="Amount requested" value={formatUGX(application.amountRequested)} />
              <Field label="Term" value={`${application.termMonths} months`} />
              <Field label="Purpose" value={application.purpose} />
              <Field label="Submitted" value={formatDate(application.submittedAt)} />
              <Field label="Last updated" value={formatDate(application.updatedAt)} />
            </dl>
          </Card>

          {applicantProfile && (
            <Card>
              <CardHeader>
                <CardTitle>Applicant Profile (KYC)</CardTitle>
              </CardHeader>
              <dl className="grid grid-cols-2 gap-4 text-sm sm:grid-cols-3">
                <Field label="National ID" value={applicantProfile.nationalId} />
                <Field label="Date of birth" value={formatDate(applicantProfile.dateOfBirth)} />
                <Field label="Employer" value={applicantProfile.employmentInfo.employer} />
                <Field label="Job title" value={applicantProfile.employmentInfo.jobTitle} />
                <Field label="Monthly income" value={formatUGX(applicantProfile.incomeMonthly)} />
                <Field label="Address" value={`${applicantProfile.address.street}, ${applicantProfile.address.city}`} />
              </dl>
            </Card>
          )}

          <Card>
            <CardHeader>
              <CardTitle>Documents</CardTitle>
            </CardHeader>
            {documents.length === 0 ? (
              <p className="text-sm text-neutral-500">No documents uploaded yet.</p>
            ) : (
              <ul className="flex flex-col divide-y divide-neutral-100">
                {documents.map((doc) => (
                  <li key={doc.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                    <div className="flex items-center gap-3">
                      <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-neutral-100 text-neutral-500">
                        <FileText className="h-4 w-4" />
                      </div>
                      <div>
                        <p className="text-sm font-medium text-neutral-800">{doc.docType.replace(/_/g, ' ')}</p>
                        <p className="text-xs text-neutral-500">{doc.fileName}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge tone={doc.verificationStatus === 'VERIFIED' ? 'success' : doc.verificationStatus === 'REJECTED' ? 'danger' : 'warning'}>
                        {doc.verificationStatus}
                      </Badge>
                      {doc.verificationStatus === 'PENDING' && (
                        <>
                          <Button
                            type="button"
                            size="sm"
                            variant="success"
                            onClick={() => run(() => setDocumentStatus(doc.id, 'VERIFIED', doc.version), 'Document verified.')}
                          >
                            <CheckCircle2 className="h-3.5 w-3.5" /> Verify
                          </Button>
                          <Button
                            type="button"
                            size="sm"
                            variant="danger"
                            onClick={() => run(() => setDocumentStatus(doc.id, 'REJECTED', doc.version), 'Document rejected.')}
                          >
                            <XCircle className="h-3.5 w-3.5" /> Reject
                          </Button>
                        </>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <TimelineCard application={application} creditCheck={creditCheck} approval={approval} disbursement={disbursement} history={history} />
        </div>

        <div className="flex flex-col gap-5">
          {application.status === 'SUBMITTED' && (
            <Card>
              <CardHeader>
                <CardTitle>Officer Actions</CardTitle>
              </CardHeader>
              <div className="flex flex-col gap-3">
                {!application.assignedOfficerId && (
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => run(() => assignToMe(application.id, user?.id ?? '', application.version), 'Application assigned to you.')}
                  >
                    <UserCheck className="h-4 w-4" /> Pick up application
                  </Button>
                )}
                <Button
                  type="button"
                  onClick={() =>
                    run(
                      () => updateApplicationStatus(application.id, 'CREDIT_CHECK_PENDING', { version: application.version }),
                      'Forwarded for credit check.',
                    )
                  }
                >
                  <Send className="h-4 w-4" /> Forward for credit check
                </Button>
              </div>
            </Card>
          )}
          {application.status === 'PENDING_DISBURSEMENT' && (
            <Card>
              <CardHeader>
                <CardTitle>Disbursement</CardTitle>
              </CardHeader>
              <p className="mb-3 text-sm text-neutral-600">
                The borrower accepted the offer. Release {formatUGX(application.amountApproved ?? application.amountRequested)} by:
              </p>
              <div className="flex flex-col gap-3">
                {(['BANK_TRANSFER', 'MOBILE_MONEY'] as const).map((method) => (
                  <Button
                    key={method}
                    type="button"
                    variant={method === 'BANK_TRANSFER' ? 'success' : 'secondary'}
                    onClick={() =>
                      run(
                        () =>
                          disburseLoan(application.id, method, application.version, disbursementKey.current).then(() => {
                            disbursementKey.current = newIdempotencyKey()
                          }),
                        'Funds disbursed. The repayment schedule has started.',
                      )
                    }
                  >
                    <Banknote className="h-4 w-4" /> {method === 'BANK_TRANSFER' ? 'Bank transfer' : 'Mobile money'}
                  </Button>
                ))}
              </div>
            </Card>
          )}
          <CreditAssessmentPanel
            applicationId={application.id}
            version={application.version}
            existing={creditCheck}
            onSaved={() => refreshWithMessage('Credit check recorded.')}
            onError={handleActionError}
          />
          <DecisionPanel
            applicationId={application.id}
            version={application.version}
            existing={approval}
            onSaved={() => refreshWithMessage('Decision recorded.')}
            onError={handleActionError}
          />
          <InfoRequestPanel
            applicationId={application.id}
            version={application.version}
            onSaved={() => refreshWithMessage('Request for more information sent to applicant.')}
            onError={handleActionError}
          />
        </div>
      </div>
    </div>
  )
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-neutral-400">{label}</dt>
      <dd className="mt-0.5 font-medium text-neutral-800">{value}</dd>
    </div>
  )
}

function CreditAssessmentPanel({
  applicationId,
  version,
  existing,
  onSaved,
  onError,
}: {
  applicationId: string
  version?: number
  existing?: CreditCheck
  onSaved: () => void
  onError: (err: unknown) => void
}) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(creditAssessmentSchema),
    defaultValues: { score: existing?.score ?? 700, result: existing?.result ?? 'PASSED' },
  })

  async function onSubmit(values: CreditAssessmentFormValues) {
    try {
      await recordCreditCheck(applicationId, values.score, values.result, version)
      onSaved()
    } catch (err) {
      onError(err)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Credit Assessment</CardTitle>
      </CardHeader>
      {existing && (
        <div className="mb-4 rounded-lg bg-neutral-50 p-3 text-sm text-neutral-600">
          Last recorded: <span className="font-semibold text-neutral-800">{existing.score}</span> ({existing.result}) on{' '}
          {formatDate(existing.checkedAt)}
        </div>
      )}
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-3">
        <TextField label="Credit score" type="number" min={300} max={850} required error={errors.score?.message} {...register('score')} />
        <SelectField
          label="Result"
          required
          options={[
            { value: 'PASSED', label: 'Passed' },
            { value: 'FAILED', label: 'Failed' },
          ]}
          error={errors.result?.message}
          {...register('result')}
        />
        <Button type="submit" variant="secondary" isLoading={isSubmitting}>
          Save assessment
        </Button>
      </form>
    </Card>
  )
}

function DecisionPanel({
  applicationId,
  version,
  existing,
  onSaved,
  onError,
}: {
  applicationId: string
  version?: number
  existing?: Approval
  onSaved: () => void
  onError: (err: unknown) => void
}) {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<DecisionFormValues>({
    resolver: zodResolver(decisionSchema),
    defaultValues: { decision: existing?.decision ?? 'APPROVED', comments: existing?.comments ?? '' },
  })
  const decision = watch('decision')

  async function onSubmit(values: DecisionFormValues) {
    try {
      await recordDecision(applicationId, values.decision, values.comments, version)
      onSaved()
    } catch (err) {
      onError(err)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Final Decision</CardTitle>
      </CardHeader>
      {existing && (
        <div className="mb-4 rounded-lg bg-neutral-50 p-3 text-sm text-neutral-600">
          Previously {existing.decision.toLowerCase()} on {formatDate(existing.decidedAt)}.
        </div>
      )}
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-3">
        <SelectField
          label="Decision"
          required
          options={[
            { value: 'APPROVED', label: 'Approve' },
            { value: 'REJECTED', label: 'Reject' },
          ]}
          error={errors.decision?.message}
          {...register('decision')}
        />
        <TextareaField
          label="Remarks"
          required={decision === 'REJECTED'}
          hint={decision === 'REJECTED' ? 'Required when rejecting' : 'Optional notes for the file'}
          error={errors.comments?.message}
          {...register('comments')}
        />
        <Button type="submit" variant={decision === 'REJECTED' ? 'danger' : 'success'} isLoading={isSubmitting}>
          {decision === 'REJECTED' ? 'Reject application' : 'Approve application'}
        </Button>
      </form>
    </Card>
  )
}

function InfoRequestPanel({
  applicationId,
  version,
  onSaved,
  onError,
}: {
  applicationId: string
  version?: number
  onSaved: () => void
  onError: (err: unknown) => void
}) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<InfoRequestFormValues>({
    resolver: zodResolver(infoRequestSchema),
    defaultValues: { message: '' },
  })

  async function onSubmit(values: InfoRequestFormValues) {
    try {
      await updateApplicationStatus(applicationId, 'AWAITING_ADDITIONAL_INFO', { version, note: values.message })
      reset()
      onSaved()
    } catch (err) {
      onError(err)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Request More Information</CardTitle>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-3">
        <TextareaField
          label="Message to applicant"
          required
          placeholder="e.g. Please upload your latest 3-month bank statement."
          error={errors.message?.message}
          {...register('message')}
        />
        <Button type="submit" variant="secondary" isLoading={isSubmitting}>
          <MessageCircleQuestion className="h-4 w-4" /> Send request
        </Button>
      </form>
    </Card>
  )
}

interface TimelineEvent {
  label: string
  at: string
  icon: LucideIcon
  tone: 'primary' | 'success' | 'danger' | 'warning'
}

const TIMELINE_ICON_CLASSES: Record<TimelineEvent['tone'], string> = {
  primary: 'bg-primary-50 text-primary-600',
  success: 'bg-success-50 text-success-600',
  danger: 'bg-danger-50 text-danger-600',
  warning: 'bg-warning-50 text-warning-600',
}

function TimelineCard({
  application,
  creditCheck,
  approval,
  disbursement,
  history,
}: {
  application: LoanApplication
  creditCheck?: CreditCheck
  approval?: Approval
  disbursement?: Disbursement
  history?: StatusHistoryEntry[]
}) {
  // The real API returns the full status history, one entry per state change of the loan.
  // Mock mode has none, so it falls back to the milestones it can derive.
  const fromHistory: TimelineEvent[] = (history ?? []).map((entry) => {
    const meta = STATUS_META[entry.toStatus]
    const tone = meta.tone === 'neutral' ? 'primary' : meta.tone
    return {
      label: entry.note ? `${meta.label}: ${entry.note}` : meta.label,
      at: entry.changedAt,
      icon: tone === 'success' ? CheckCircle2 : tone === 'danger' ? XCircle : tone === 'warning' ? MessageCircleQuestion : FileText,
      tone,
    }
  })
  const events: TimelineEvent[] =
    fromHistory.length > 0
      ? fromHistory
      : [{ label: 'Application submitted', at: application.submittedAt, icon: FileText, tone: 'primary' }]
  if (fromHistory.length === 0) {
    if (creditCheck) {
      events.push({
        label: `Credit check ${creditCheck.result.toLowerCase()} (score ${creditCheck.score})`,
        at: creditCheck.checkedAt,
        icon: ShieldCheck,
        tone: creditCheck.result === 'PASSED' ? 'success' : 'danger',
      })
    }
    if (approval) {
      events.push({
        label: `Application ${approval.decision.toLowerCase()}`,
        at: approval.decidedAt,
        icon: approval.decision === 'APPROVED' ? CheckCircle2 : XCircle,
        tone: approval.decision === 'APPROVED' ? 'success' : 'danger',
      })
    }
    if (disbursement?.disbursedAt) {
      events.push({ label: `Funds disbursed (${formatUGX(disbursement.amount)})`, at: disbursement.disbursedAt, icon: Banknote, tone: 'success' })
    }
  }
  events.sort((a, b) => new Date(a.at).getTime() - new Date(b.at).getTime())

  return (
    <Card>
      <CardHeader>
        <CardTitle>Timeline</CardTitle>
      </CardHeader>
      <ol className="flex flex-col gap-4">
        {events.map((event) => (
          <li key={`${event.label}-${event.at}`} className="flex gap-3">
            <div className={cn('flex h-8 w-8 shrink-0 items-center justify-center rounded-full', TIMELINE_ICON_CLASSES[event.tone])}>
              <event.icon className="h-4 w-4" />
            </div>
            <div>
              <p className="text-sm font-medium text-neutral-800">{event.label}</p>
              <p className="text-xs text-neutral-500">{formatDateTime(event.at)}</p>
            </div>
          </li>
        ))}
      </ol>
    </Card>
  )
}
