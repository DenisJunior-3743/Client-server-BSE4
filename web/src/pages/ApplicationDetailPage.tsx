import { useCallback, useEffect, useState } from 'react'
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
  type LucideIcon,
} from 'lucide-react'
import {
  getApplicationDetail,
  setDocumentStatus,
  recordCreditCheck,
  recordDecision,
  updateApplicationStatus,
} from '@/api/applications'
import { loanProducts } from '@/mocks/data'
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
import type { LoanApplication, CreditCheck, Approval, Disbursement } from '@/types'
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
  const [actionMessage, setActionMessage] = useState<string | null>(null)

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

  const { application, documents, creditCheck, approval, disbursement, applicantProfile } = bundle
  const product = loanProducts.find((p) => p.id === application.productId)

  function refreshWithMessage(message: string) {
    setActionMessage(message)
    load()
    window.setTimeout(() => setActionMessage(null), 3000)
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

      {actionMessage && (
        <div className="mb-4 rounded-xl bg-success-50 px-4 py-3 text-sm font-medium text-success-700 ring-1 ring-inset ring-success-200">
          {actionMessage}
        </div>
      )}

      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <Avatar name={application.applicantName} color={colorForId(application.applicantId)} size={52} />
          <div>
            <h1 className="font-display text-2xl font-extrabold text-neutral-900">{application.applicantName}</h1>
            <p className="text-sm text-neutral-500">
              {application.id} · {application.applicantPhone}
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
                            onClick={() => setDocumentStatus(doc.id, 'VERIFIED').then(() => refreshWithMessage('Document verified.'))}
                          >
                            <CheckCircle2 className="h-3.5 w-3.5" /> Verify
                          </Button>
                          <Button
                            type="button"
                            size="sm"
                            variant="danger"
                            onClick={() => setDocumentStatus(doc.id, 'REJECTED').then(() => refreshWithMessage('Document rejected.'))}
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

          <TimelineCard application={application} creditCheck={creditCheck} approval={approval} disbursement={disbursement} />
        </div>

        <div className="flex flex-col gap-5">
          <CreditAssessmentPanel applicationId={application.id} existing={creditCheck} onSaved={() => refreshWithMessage('Credit check recorded.')} />
          <DecisionPanel applicationId={application.id} existing={approval} onSaved={() => refreshWithMessage('Decision recorded.')} />
          <InfoRequestPanel applicationId={application.id} onSaved={() => refreshWithMessage('Request for more information sent to applicant.')} />
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
  existing,
  onSaved,
}: {
  applicationId: string
  existing?: CreditCheck
  onSaved: () => void
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
    await recordCreditCheck(applicationId, values.score, values.result)
    onSaved()
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
  existing,
  onSaved,
}: {
  applicationId: string
  existing?: Approval
  onSaved: () => void
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
    await recordDecision(applicationId, values.decision, values.comments)
    onSaved()
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

function InfoRequestPanel({ applicationId, onSaved }: { applicationId: string; onSaved: () => void }) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<InfoRequestFormValues>({
    resolver: zodResolver(infoRequestSchema),
    defaultValues: { message: '' },
  })

  async function onSubmit() {
    await updateApplicationStatus(applicationId, 'AWAITING_ADDITIONAL_INFO')
    reset()
    onSaved()
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
}: {
  application: LoanApplication
  creditCheck?: CreditCheck
  approval?: Approval
  disbursement?: Disbursement
}) {
  const events: TimelineEvent[] = [{ label: 'Application submitted', at: application.submittedAt, icon: FileText, tone: 'primary' }]
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
