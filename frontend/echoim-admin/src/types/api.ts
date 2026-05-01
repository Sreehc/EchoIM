export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  requestId?: string
}

export interface PageResponse<T> {
  list: T[]
  pageNo: number
  pageSize: number
  total: number
}

export interface AdminLoginRequest {
  username: string
  password: string
}

export interface AdminLoginResult {
  token: string
  tokenType: string
  adminInfo: AdminInfo
}

export interface AdminInfo {
  adminUserId: number
  username: string
  nickname: string
  roleCode: string
}

export interface AdminUserItem {
  userId: number
  username: string
  nickname: string
  avatarUrl?: string
  phone?: string
  email?: string
  status: number
  createdAt?: string
}

export interface AdminGroupItem {
  groupId: number
  groupName: string
  ownerUserId: number
  memberCount?: number
  status: number
  createdAt?: string
}

export interface BeautyNoItem {
  beautyNoId: number
  beautyNo: string
  levelType: number
  status: number
  remark?: string
}

export interface BeautyNoRequest {
  beautyNo: string
  levelType: number
  remark?: string
}

export interface ConfigItem {
  configId: number
  configKey: string
  configValue: string
  configName: string
  remark?: string
  status: number
}

export interface ConfigRequest {
  configKey: string
  configValue: string
  configName: string
  remark?: string
}

export interface VersionItem {
  versionId: number
  versionCode: string
  versionName: string
  platform: string
  releaseNote?: string
  forceUpdate?: number
  grayPercent?: number
  publishStatus: number
  createdAt?: string
}

export interface VersionRequest {
  versionCode: string
  versionName: string
  platform: string
  releaseNote?: string
  forceUpdate: number
  grayPercent: number
}
