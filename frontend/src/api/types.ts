// kiểu dữ liệu khớp với backend: AskResult, Citation, sự kiện SSE (api-hoi-dap_v1.0.md)

export type Citation = {
  id: string
  taiLieuId: string | null
  tenHienThi: string | null
  trang: number
  quote: string
  verified: boolean
}

export type AskResult = {
  question: string
  status: string
  answer: string
  citations: Citation[]
  missing: string
  unverifiedNumbers: string[]
  unknownIds: string[]
  toolRounds: number
  toolTrace: string[]
  timingsMs: Record<string, number>
  costUsd: Record<string, number>
  promptTokens: number
  completionTokens: number
  promptVersion: string
  model: string
}

export type Verification = {
  status: string
  citations: Citation[]
  unverifiedNumbers: string[]
  unknownIds: string[]
  missing: string
}

export type Source = { id: string; taiLieuId: string; title: string; page: number; score: number }

// một dòng trong dòng thời gian các bước
export type Step = { kind: string; text: string; sources?: Source[] }

export type Turn = {
  id: number
  question: string
  steps: Step[]
  answer: string
  verification?: Verification
  result?: AskResult
  error?: string
  running: boolean
  // mã câu trả lời đã lưu (sự kiện "saved"), dùng để gửi feedback
  messageId?: number
  feedback?: FeedbackLevel | null
}

export type AgentEvent = { type: string; data: Record<string, unknown> }

export type FeedbackLevel = 'dung' | 'sai_mot_phan' | 'sai_toan_bo'

export type Me = {
  id: string
  name: string
  // họ tên kiểu Việt Nam (họ trước); không có thì là tên đăng nhập
  displayName: string
  roles: string[]
  permissions: { ask: boolean; manageDocuments: boolean; manageUsers: boolean }
}

export type ConversationSummary = { id: number; title: string; updatedAt: string }

export type ConversationMessage = {
  id: number
  question: string
  answer: string
  status: string
  result: AskResult | null
  feedback: FeedbackLevel | null
}

export type ConversationDetail = { id: number; title: string; messages: ConversationMessage[] }
