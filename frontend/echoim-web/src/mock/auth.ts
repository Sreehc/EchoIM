import type { AuthSession } from '@/types/chat'

export const demoSession: AuthSession = {
  token: 'echoim-demo-token',
  tokenType: 'Bearer',
  expiresIn: 7200,
  expireAt: new Date(Date.now() + 7200 * 1000).toISOString(),
  refreshToken: 'echoim-demo-refresh-token',
  refreshTokenExpireAt: new Date(Date.now() + 30 * 24 * 3600 * 1000).toISOString(),
  userInfo: {
    userId: 10001,
    username: 'echo_demo_01',
    nickname: '林澈',
    avatarUrl: null,
  },
}

export async function mockLogin(username: string, password: string): Promise<AuthSession> {
  await new Promise((resolve) => window.setTimeout(resolve, 700))

  if (!username.trim() || !password.trim()) {
    throw new Error('请输入用户名和密码')
  }

  return demoSession
}
