import type { ConversationProfile } from '@/types/chat'

export const mockProfiles: Record<number, ConversationProfile> = {
  30001: {
    conversationId: 30001,
    conversationType: 1,
    subtitle: '前端工程师',
    signature: '把复杂交互压进秩序里，界面才会安静。',
    sharedFilesCount: 12,
    sharedMediaCount: 8,
    actions: [
      { key: 'remark', label: '好友备注', value: '周序' },
      { key: 'mute', label: '消息免打扰', value: '关闭' },
      { key: 'top', label: '会话置顶', value: '开启' },
    ],
  },
  30002: {
    conversationId: 30002,
    conversationType: 2,
    subtitle: '产品协作群',
    signature: '阶段一与阶段二界面评审',
    notice: '今天 15:00 统一过首页视觉与交互骨架。',
    sharedFilesCount: 26,
    sharedMediaCount: 17,
    members: [
      { id: 10001, name: '林澈', role: 'Owner' },
      { id: 10003, name: '宋眠', role: 'Design' },
      { id: 10004, name: '裴见', role: 'PM' },
      { id: 10005, name: '程原', role: 'FE' },
    ],
    actions: [
      { key: 'notice', label: '群公告', value: '已开启' },
      { key: 'mute', label: '消息免打扰', value: '关闭' },
      { key: 'top', label: '会话置顶', value: '关闭' },
      { key: 'members', label: '群成员', value: '12 人' },
    ],
  },
  30003: {
    conversationId: 30003,
    conversationType: 1,
    subtitle: '视觉设计师',
    signature: '留白是为了让界面呼吸，不是为了显得高级。',
    sharedFilesCount: 4,
    sharedMediaCount: 11,
    actions: [
      { key: 'remark', label: '好友备注', value: '宋眠' },
      { key: 'mute', label: '消息免打扰', value: '已开启' },
      { key: 'top', label: '会话置顶', value: '关闭' },
    ],
  },
  30004: {
    conversationId: 30004,
    conversationType: 2,
    subtitle: '运维与告警',
    signature: '凌晨窗口变更请提前十分钟到位。',
    notice: '灰度切换期间关注 ACK 失败率。',
    sharedFilesCount: 9,
    sharedMediaCount: 2,
    members: [
      { id: 10006, name: '沈曜', role: 'SRE' },
      { id: 10007, name: '顾野', role: 'Infra' },
      { id: 10008, name: '林澈', role: 'FE' },
    ],
    actions: [
      { key: 'notice', label: '群公告', value: '已开启' },
      { key: 'mute', label: '消息免打扰', value: '关闭' },
      { key: 'top', label: '会话置顶', value: '关闭' },
      { key: 'members', label: '群成员', value: '6 人' },
    ],
  },
  30005: {
    conversationId: 30005,
    conversationType: 1,
    subtitle: '本地演示会话',
    signature: '这里适合验证空消息状态、文件卡片和键盘交互。',
    sharedFilesCount: 0,
    sharedMediaCount: 0,
    actions: [
      { key: 'remark', label: '会话备注', value: '文件助手' },
      { key: 'mute', label: '消息免打扰', value: '关闭' },
      { key: 'top', label: '会话置顶', value: '关闭' },
    ],
  },
}
