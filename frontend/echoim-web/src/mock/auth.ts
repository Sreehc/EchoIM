import type { AuthSession } from '@/types/chat'

export const demoSession: AuthSession = {
  token: 'echoim-demo-token',
  tokenType: 'Bearer',
  expiresIn: 7200,
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
